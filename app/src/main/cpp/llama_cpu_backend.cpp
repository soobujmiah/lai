#include "include/lai/backend.h"

#include "llama.h"

#include <android/log.h>

#include <algorithm>
#include <cstdint>
#include <cstdio>
#include <mutex>
#include <stdexcept>
#include <string>
#include <string_view>
#include <thread>
#include <utility>
#include <vector>

namespace lai {
namespace {

constexpr const char* kLogTag = "LAI-llama";
constexpr const char* kSystemPrompt =
    "You are LAI, a private on-device assistant. Respond in natural Bangla when the user writes in "
    "Bangla, otherwise use the user's language. Be concise, accurate, and never claim that an Android "
    "action happened unless a tool result confirms it. আপনি একটি ব্যক্তিগত অফলাইন সহকারী। ব্যবহারকারী বাংলায় "
    "লিখলে স্বাভাবিক বাংলায় উত্তর দিন।";

void initialize_llama_once() {
    static std::once_flag once;
    std::call_once(once, [] {
        llama_log_set([](enum ggml_log_level level, const char* text, void*) {
            const int priority = level >= GGML_LOG_LEVEL_ERROR ? ANDROID_LOG_ERROR : ANDROID_LOG_DEBUG;
            __android_log_write(priority, kLogTag, text);
        }, nullptr);
        ggml_backend_load_all();
    });
}

std::string apply_chat_template(const llama_model* model, std::string_view user_prompt) {
    const std::string user(user_prompt);
    const llama_chat_message messages[] = {
        {"system", kSystemPrompt},
        {"user", user.c_str()},
    };
    const char* chat_template = llama_model_chat_template(model, nullptr);
    int32_t required = llama_chat_apply_template(chat_template, messages, 2, true, nullptr, 0);
    if (required <= 0) {
        return std::string(kSystemPrompt) + "\nUser: " + user + "\nAssistant:";
    }
    std::vector<char> formatted(static_cast<size_t>(required) + 1U);
    const int32_t written = llama_chat_apply_template(
        chat_template,
        messages,
        2,
        true,
        formatted.data(),
        static_cast<int32_t>(formatted.size())
    );
    if (written < 0) throw std::runtime_error("Model chat template could not be applied");
    return {formatted.data(), static_cast<size_t>(written)};
}

std::vector<llama_token> tokenize(const llama_vocab* vocab, const std::string& text) {
    const int32_t required = -llama_tokenize(
        vocab,
        text.data(),
        static_cast<int32_t>(text.size()),
        nullptr,
        0,
        true,
        true
    );
    if (required <= 0) throw std::runtime_error("Prompt tokenization failed");
    std::vector<llama_token> tokens(static_cast<size_t>(required));
    const int32_t written = llama_tokenize(
        vocab,
        text.data(),
        static_cast<int32_t>(text.size()),
        tokens.data(),
        static_cast<int32_t>(tokens.size()),
        true,
        true
    );
    if (written < 0) throw std::runtime_error("Prompt tokenization buffer was rejected");
    tokens.resize(static_cast<size_t>(written));
    return tokens;
}

std::string token_piece(const llama_vocab* vocab, llama_token token) {
    std::vector<char> buffer(256);
    int32_t written = llama_token_to_piece(vocab, token, buffer.data(), static_cast<int32_t>(buffer.size()), 0, true);
    if (written < 0) {
        buffer.resize(static_cast<size_t>(-written));
        written = llama_token_to_piece(vocab, token, buffer.data(), static_cast<int32_t>(buffer.size()), 0, true);
    }
    if (written < 0) throw std::runtime_error("Generated token could not be converted to UTF-8 bytes");
    return {buffer.data(), static_cast<size_t>(written)};
}

class LlamaCpuSession final : public BackendSession {
public:
    LlamaCpuSession(llama_model* model, llama_context* context)
        : model_(model), context_(context), vocab_(llama_model_get_vocab(model)) {}

    ~LlamaCpuSession() override {
        if (context_ != nullptr) llama_free(context_);
        if (model_ != nullptr) llama_model_free(model_);
    }

    int generate(
        std::string_view prompt,
        const GenerationOptions& options,
        const TokenCallback& on_token,
        const CancelCallback& is_cancelled
    ) override {
        std::lock_guard<std::mutex> lock(generation_mutex_);
        if (prompt.empty()) throw std::invalid_argument("Prompt cannot be empty");
        if (options.max_new_tokens < 1 || options.max_new_tokens > 4096) {
            throw std::invalid_argument("maxNewTokens is outside the supported range");
        }

        llama_memory_clear(llama_get_memory(context_), true);
        const std::string formatted = apply_chat_template(model_, prompt);
        std::vector<llama_token> prompt_tokens = tokenize(vocab_, formatted);
        const int32_t context_size = static_cast<int32_t>(llama_n_ctx(context_));
        if (static_cast<int64_t>(prompt_tokens.size()) + options.max_new_tokens > context_size) {
            throw std::runtime_error("Prompt and requested response exceed the loaded context size");
        }

        const int32_t batch_size = std::max(1, std::min(512, context_size));
        size_t offset = 0;
        while (offset < prompt_tokens.size()) {
            if (is_cancelled()) throw std::runtime_error("Generation cancelled");
            const int32_t count = static_cast<int32_t>(
                std::min(prompt_tokens.size() - offset, static_cast<size_t>(batch_size))
            );
            llama_batch batch = llama_batch_get_one(prompt_tokens.data() + offset, count);
            if (llama_decode(context_, batch) != 0) throw std::runtime_error("Prompt evaluation failed");
            offset += static_cast<size_t>(count);
        }

        llama_sampler* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
        if (sampler == nullptr) throw std::runtime_error("Sampler allocation failed");
        struct SamplerGuard {
            llama_sampler* value;
            ~SamplerGuard() { llama_sampler_free(value); }
        } sampler_guard{sampler};

        if (options.temperature <= 0.0F) {
            llama_sampler_chain_add(sampler, llama_sampler_init_greedy());
        } else {
            llama_sampler_chain_add(sampler, llama_sampler_init_top_p(std::clamp(options.top_p, 0.05F, 1.0F), 1));
            llama_sampler_chain_add(sampler, llama_sampler_init_temp(std::clamp(options.temperature, 0.05F, 2.0F)));
            const uint32_t seed = options.seed < 0
                ? LLAMA_DEFAULT_SEED
                : static_cast<uint32_t>(options.seed & 0xFFFFFFFFLL);
            llama_sampler_chain_add(sampler, llama_sampler_init_dist(seed));
        }

        int generated = 0;
        while (generated < options.max_new_tokens) {
            if (is_cancelled()) break;
            llama_token token = llama_sampler_sample(sampler, context_, -1);
            if (llama_vocab_is_eog(vocab_, token)) break;
            const std::string piece = token_piece(vocab_, token);
            if (!piece.empty() && !on_token(piece)) break;
            ++generated;
            llama_batch batch = llama_batch_get_one(&token, 1);
            if (llama_decode(context_, batch) != 0) throw std::runtime_error("Generated-token evaluation failed");
        }
        return generated;
    }

private:
    llama_model* model_;
    llama_context* context_;
    const llama_vocab* vocab_;
    std::mutex generation_mutex_;
};

class LlamaCpuBackend final : public Backend {
public:
    std::string name() const override { return "cpu"; }
    bool available() const override { return true; }

    std::unique_ptr<BackendSession> open(
        const std::string& model_path,
        int context_size,
        std::string& error
    ) override {
        initialize_llama_once();
        llama_model_params model_params = llama_model_default_params();
        model_params.n_gpu_layers = 0;
        model_params.use_mmap = true;
        model_params.use_mlock = false;
        llama_model* model = llama_model_load_from_file(model_path.c_str(), model_params);
        if (model == nullptr) {
            error = "llama.cpp could not load the GGUF model";
            return nullptr;
        }

        llama_context_params context_params = llama_context_default_params();
        context_params.n_ctx = static_cast<uint32_t>(context_size);
        context_params.n_batch = static_cast<uint32_t>(std::min(context_size, 512));
        context_params.n_ubatch = context_params.n_batch;
        const unsigned int hardware_threads = std::max(2U, std::thread::hardware_concurrency());
        context_params.n_threads = static_cast<int32_t>(std::clamp(hardware_threads > 2 ? hardware_threads - 2 : 2U, 2U, 8U));
        context_params.n_threads_batch = static_cast<int32_t>(std::clamp(hardware_threads, 2U, 8U));
        context_params.no_perf = false;
        llama_context* context = llama_init_from_model(model, context_params);
        if (context == nullptr) {
            llama_model_free(model);
            error = "llama.cpp could not allocate the inference context";
            return nullptr;
        }
        return std::make_unique<LlamaCpuSession>(model, context);
    }
};

}  // namespace

std::unique_ptr<Backend> create_llama_cpu_backend() {
    return std::make_unique<LlamaCpuBackend>();
}

}  // namespace lai
