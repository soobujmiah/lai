#include "include/lai/backend.h"

#include "llama.h"

#include <android/log.h>

#include <algorithm>
#include <atomic>
#include <chrono>
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

using Clock = std::chrono::steady_clock;
constexpr const char* kLogTag = "LAI-llama";
constexpr const char* kSystemPrompt =
    "You are LAI, a private on-device assistant. Be concise and accurate. "
    "When the user writes in Bangla, reply in simple natural Bangla with short sentences. "
    "আপনি LAI। বাংলায় সহজ, ছোট বাক্যে উত্তর দিন।";

long long elapsed_us(Clock::time_point start, Clock::time_point end) {
    return std::chrono::duration_cast<std::chrono::microseconds>(end - start).count();
}

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

std::string normalized_role(const std::string& role) {
    if (role == "user" || role == "assistant" || role == "system") return role;
    throw std::invalid_argument("Conversation contains an unsupported role");
}

std::string apply_chat_template(const llama_model* model, const std::vector<ChatMessage>& conversation) {
    if (conversation.empty()) throw std::invalid_argument("Conversation cannot be empty");
    std::vector<std::string> roles;
    std::vector<std::string> contents;
    roles.reserve(conversation.size() + 1U);
    contents.reserve(conversation.size() + 1U);
    roles.emplace_back("system");
    contents.emplace_back(kSystemPrompt);
    for (const auto& message : conversation) {
        if (message.content.empty()) continue;
        const std::string role = normalized_role(message.role);
        if (role == "system") {
            contents.front().append("\n").append(message.content);
            continue;
        }
        roles.emplace_back(role);
        contents.emplace_back(message.content);
    }
    if (roles.size() == 1U) throw std::invalid_argument("Conversation has no non-empty messages");

    std::vector<llama_chat_message> messages;
    messages.reserve(roles.size());
    for (size_t index = 0; index < roles.size(); ++index) {
        messages.push_back({roles[index].c_str(), contents[index].c_str()});
    }

    const char* chat_template = llama_model_chat_template(model, nullptr);
    int32_t required = llama_chat_apply_template(
        chat_template, messages.data(), messages.size(), true, nullptr, 0
    );
    if (required <= 0) {
        std::string fallback = std::string(kSystemPrompt) + "\n";
        for (size_t index = 1; index < roles.size(); ++index) {
            fallback += roles[index] + ": " + contents[index] + "\n";
        }
        fallback += "assistant:";
        return fallback;
    }
    std::vector<char> formatted(static_cast<size_t>(required) + 1U);
    const int32_t written = llama_chat_apply_template(
        chat_template,
        messages.data(),
        messages.size(),
        true,
        formatted.data(),
        static_cast<int32_t>(formatted.size())
    );
    if (written < 0) throw std::runtime_error("Model chat template could not be applied");
    return {formatted.data(), static_cast<size_t>(written)};
}

std::vector<llama_token> tokenize(const llama_vocab* vocab, const std::string& text) {
    const int32_t required = -llama_tokenize(
        vocab, text.data(), static_cast<int32_t>(text.size()), nullptr, 0, true, true
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
    LlamaCpuSession(llama_model* model, llama_context* context, int baseline_threads)
        : model_(model),
          context_(context),
          vocab_(llama_model_get_vocab(model)),
          applied_threads_(baseline_threads) {}

    ~LlamaCpuSession() override {
        if (context_ != nullptr) llama_free(context_);
        if (model_ != nullptr) llama_model_free(model_);
    }

    int count_tokens(const std::vector<ChatMessage>& conversation) override {
        const auto lock_wait_start = Clock::now();
        std::lock_guard<std::mutex> lock(generation_mutex_);
        const auto start = Clock::now();
        // The shared generation_mutex_ is a suspected stall point (stage COUNTING_TOKENS in the
        // diagnostics export): if generate() holds it, this blocks with no visible progress.
        __android_log_print(
            ANDROID_LOG_INFO, kLogTag,
            "count_tokens: lock acquired after %lld us", elapsed_us(lock_wait_start, start)
        );
        const int count = static_cast<int>(tokenize(vocab_, apply_chat_template(model_, conversation)).size());
        __android_log_print(
            ANDROID_LOG_INFO, kLogTag,
            "count_tokens: %d tokens in %lld us", count, elapsed_us(start, Clock::now())
        );
        return count;
    }

    GenerationResult generate(
        const std::vector<ChatMessage>& conversation,
        const GenerationOptions& options,
        const TokenCallback& on_token,
        const CancelCallback& is_cancelled
    ) override {
        const auto lock_wait_start = Clock::now();
        std::lock_guard<std::mutex> lock(generation_mutex_);
        if (options.max_new_tokens < 1 || options.max_new_tokens > 4096) {
            throw std::invalid_argument("maxNewTokens is outside the supported range");
        }

        const auto total_start = Clock::now();
        __android_log_print(
            ANDROID_LOG_INFO, kLogTag,
            "generate: lock acquired after %lld us", elapsed_us(lock_wait_start, total_start)
        );
        const std::string templated = apply_chat_template(model_, conversation);
        const auto template_end = Clock::now();
        std::vector<llama_token> prompt_tokens = tokenize(vocab_, templated);
        const auto tokenize_end = Clock::now();
        __android_log_print(
            ANDROID_LOG_INFO, kLogTag,
            "generate: template %zu chars in %lld us, tokenize %zu tokens in %lld us",
            templated.size(), elapsed_us(total_start, template_end),
            prompt_tokens.size(), elapsed_us(template_end, tokenize_end)
        );
        const int32_t context_size = static_cast<int32_t>(llama_n_ctx(context_));
        if (static_cast<int64_t>(prompt_tokens.size()) + options.max_new_tokens > context_size) {
            throw std::runtime_error("Conversation and requested response exceed the loaded context size");
        }

        // Any exception below leaves the KV cache in an uncertain state relative to kv_tokens_,
        // so the reuse bookkeeping is invalidated wholesale rather than guessed at.
        try {

        // KV-prefix reuse. Device evidence (0.9.0, six generations): TTFT grew linearly from
        // 6.2 s to 17.0 s because every request cleared the memory and re-prefilled the whole
        // conversation at ~28 tok/s. A chat conversation only ever grows at the tail, so the
        // tokens already decoded for the previous request are byte-identical up to the point
        // where the new turn begins. Keep the longest common prefix, drop everything after it
        // ([reused, inf) via llama_memory_seq_rm), and prefill only the suffix.
        //
        // The reuse ceiling is prompt-size minus one: at least one prompt token must always be
        // decoded so the sampler has fresh logits for position -1.
        llama_memory_t memory = llama_get_memory(context_);
        size_t reused = 0;
        const size_t reuse_ceiling = std::min(kv_tokens_.size(), prompt_tokens.size() - 1U);
        while (reused < reuse_ceiling && kv_tokens_[reused] == prompt_tokens[reused]) ++reused;
        if (reused == 0) {
            llama_memory_clear(memory, true);
            kv_tokens_.clear();
        } else if (llama_memory_seq_rm(memory, 0, static_cast<llama_pos>(reused), -1)) {
            kv_tokens_.resize(reused);
        } else {
            // The memory backend refused a partial removal; fall back to a full re-prefill.
            llama_memory_clear(memory, true);
            kv_tokens_.clear();
            reused = 0;
        }
        __android_log_print(
            ANDROID_LOG_INFO, kLogTag,
            "generate: reusing %zu of %zu prompt tokens from the KV cache",
            reused, prompt_tokens.size()
        );

        const auto prompt_start = Clock::now();
        // Hotfix-2 on SM8735 heating: prefill must use big cluster even if governor throttled to 2.
        if (applied_threads_ < 4) {
            llama_set_n_threads(context_, 4, 4);
            __android_log_print(ANDROID_LOG_INFO, kLogTag, "hotfix: force prefill threads %d -> 4", applied_threads_);
            applied_threads_ = 4;
            requested_threads_.store(4, std::memory_order_relaxed);
        }
        // Smaller prompt chunks bound how long a single uninterruptible llama_decode call runs,
        // so Stop is observed promptly instead of after a whole 512-token batch.
        // Hotfix 2026-08-17 (run 28671: 128/334 in 179 sec = 0.7 tok/s, device hot). Halve again to 32
        // and shorten prompt 334→~180 tokens to keep each decode bounded and cut heat.
        const int32_t batch_size = std::max(1, std::min(32, context_size));
        size_t offset = reused;
        while (offset < prompt_tokens.size()) {
            if (is_cancelled()) throw std::runtime_error("Generation cancelled");
            apply_thread_limit_locked();
            const int32_t count = static_cast<int32_t>(
                std::min(prompt_tokens.size() - offset, static_cast<size_t>(batch_size))
            );
            llama_batch batch = llama_batch_get_one(prompt_tokens.data() + offset, count);
            if (llama_decode(context_, batch) != 0) throw std::runtime_error("Prompt evaluation failed");
            // kv_tokens_ mirrors exactly what has entered the KV cache, chunk by chunk, so a
            // cancellation between chunks still leaves the bookkeeping truthful.
            kv_tokens_.insert(
                kv_tokens_.end(),
                prompt_tokens.begin() + static_cast<std::ptrdiff_t>(offset),
                prompt_tokens.begin() + static_cast<std::ptrdiff_t>(offset + static_cast<size_t>(count))
            );
            offset += static_cast<size_t>(count);
            // Per-chunk progress: four device reports could not distinguish "prefill is slow"
            // from "prefill is wedged". This names the exact chunk where progress stops.
            __android_log_print(
                ANDROID_LOG_INFO, kLogTag,
                "generate: prefill %zu/%zu tokens at %lld us",
                offset, prompt_tokens.size(), elapsed_us(prompt_start, Clock::now())
            );
        }
        const auto prompt_end = Clock::now();
        const long long prefill_us = elapsed_us(prompt_start, prompt_end);
        const size_t evaluated = prompt_tokens.size() - reused;
        __android_log_print(
            ANDROID_LOG_INFO, kLogTag,
            "generate: prefill done, %zu new of %zu total tokens in %lld us (%.1f tok/s)",
            evaluated, prompt_tokens.size(), prefill_us,
            prefill_us > 0 ? static_cast<double>(evaluated) * 1e6 / static_cast<double>(prefill_us) : 0.0
        );

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
            // Mild repetition penalty: 1.5B-class models loop visibly, and worst in low-resource
            // languages (device report: partly incoherent, repetitive Bangla). Applied AFTER
            // top-p on purpose — the pinned header warns the penalty scan is slow on a full
            // (151k Qwen) candidate list. 1.1 over the last 64 tokens is the conservative
            // llama.cpp community default; freq/present stay off. llama_sampler_sample accepts
            // each sampled token into the chain, so the penalty window tracks automatically.
            llama_sampler_chain_add(sampler, llama_sampler_init_penalties(
                llama_vocab_n_tokens(vocab_), 64, 1.1F, 0.0F, 0.0F
            ));
            llama_sampler_chain_add(sampler, llama_sampler_init_temp(std::clamp(options.temperature, 0.05F, 2.0F)));
            const uint32_t seed = options.seed < 0
                ? LLAMA_DEFAULT_SEED
                : static_cast<uint32_t>(options.seed & 0xFFFFFFFFLL);
            llama_sampler_chain_add(sampler, llama_sampler_init_dist(seed));
        }

        const auto decode_start = Clock::now();
        Clock::time_point first_token_time{};
        int generated = 0;
        while (generated < options.max_new_tokens) {
            if (is_cancelled()) break;
            apply_thread_limit_locked();
            llama_token token = llama_sampler_sample(sampler, context_, -1);
            if (llama_vocab_is_eog(vocab_, token)) break;
            const std::string piece = token_piece(vocab_, token);
            if (generated == 0) {
                first_token_time = Clock::now();
                __android_log_print(
                    ANDROID_LOG_INFO, kLogTag,
                    "generate: first token sampled at %lld us from start",
                    elapsed_us(total_start, first_token_time)
                );
            }
            if (!piece.empty() && !on_token(piece)) break;
            ++generated;
            llama_batch batch = llama_batch_get_one(&token, 1);
            if (llama_decode(context_, batch) != 0) throw std::runtime_error("Generated-token evaluation failed");
            // The generated token is now part of the KV cache; recording it means the NEXT
            // request's templated prompt (which embeds this reply as assistant text) can match
            // it in the common-prefix scan instead of re-prefilling the whole reply.
            kv_tokens_.push_back(token);
        }
        const auto end = Clock::now();
        __android_log_print(
            ANDROID_LOG_INFO, kLogTag,
            "generate: done, %d tokens in %lld us total",
            generated, elapsed_us(total_start, end)
        );
        return GenerationResult{
            static_cast<int>(prompt_tokens.size()),
            generated,
            elapsed_us(prompt_start, prompt_end),
            generated > 0 ? elapsed_us(total_start, first_token_time) : elapsed_us(total_start, end),
            elapsed_us(decode_start, end),
            elapsed_us(total_start, end),
            static_cast<int>(evaluated),
        };

        } catch (...) {
            // The KV cache may hold a partial or failed decode; never let kv_tokens_ overstate
            // what is actually cached. Next request pays a full prefill, which is always correct.
            llama_memory_clear(llama_get_memory(context_), true);
            kv_tokens_.clear();
            throw;
        }
    }

    void set_thread_limit(int decode_threads) override {
        // Recorded from the UI thread; consumed by the decode loop at its next safe point.
        requested_threads_.store(decode_threads, std::memory_order_relaxed);
    }

private:
    /** Applies a pending thermal thread budget between llama_decode calls; never during one. */
    void apply_thread_limit_locked() {
        const int requested = requested_threads_.load(std::memory_order_relaxed);
        if (requested > 0 && requested != applied_threads_) {
            llama_set_n_threads(context_, requested, requested);
            __android_log_print(
                ANDROID_LOG_INFO, kLogTag,
                "thermal: decode threads %d -> %d", applied_threads_, requested
            );
            applied_threads_ = requested;
        }
    }

    llama_model* model_;
    llama_context* context_;
    const llama_vocab* vocab_;
    std::mutex generation_mutex_;
    std::atomic<int> requested_threads_{0};
    int applied_threads_;
    // Exact token sequence currently resident in the KV cache (prompt + generated tokens),
    // maintained strictly after each successful llama_decode. Guarded by generation_mutex_.
    std::vector<llama_token> kv_tokens_;
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
        model_params.load_mode = LLAMA_LOAD_MODE_MMAP;
        llama_model* model = llama_model_load_from_file(model_path.c_str(), model_params);
        if (model == nullptr) {
            error = "llama.cpp could not load the GGUF model";
            return nullptr;
        }

        llama_context_params context_params = llama_context_default_params();
        context_params.n_ctx = static_cast<uint32_t>(context_size);
        context_params.n_batch = static_cast<uint32_t>(std::min(context_size, 256));
        context_params.n_ubatch = context_params.n_batch;
        // Thermal budget, not raw throughput.
        //
        // Field report (Redmi Turbo 4 Pro / SM8735, 8 cores): the previous policy used
        // hardware_concurrency-2 for decode and ALL 8 cores for prompt batches. On a 1+3+4
        // big.LITTLE layout that saturates the little cores too, and they finish their share
        // late, so every batch waits on the slowest core while the whole SoC heats. The device
        // became hot enough to notice within one reply.
        //
        // Half the cores keeps work on the performance cluster, cuts sustained package power
        // roughly in half, and on a 1.5B Q4_K_M model costs little real decode speed because
        // decode is memory-bandwidth bound long before it is core bound.
        const unsigned int hardware_threads = std::max(2U, std::thread::hardware_concurrency());
        const int32_t worker_threads = static_cast<int32_t>(std::clamp(hardware_threads / 2U, 2U, 4U));
        context_params.n_threads = worker_threads;
        context_params.n_threads_batch = worker_threads;
        context_params.no_perf = false;

        llama_context* context = llama_init_from_model(model, context_params);
        if (context == nullptr) {
            llama_model_free(model);
            error = "llama.cpp could not allocate the inference context";
            return nullptr;
        }
        return std::make_unique<LlamaCpuSession>(model, context, static_cast<int>(worker_threads));
    }
};

}  // namespace

std::unique_ptr<Backend> create_llama_cpu_backend() {
    return std::make_unique<LlamaCpuBackend>();
}

}  // namespace lai
