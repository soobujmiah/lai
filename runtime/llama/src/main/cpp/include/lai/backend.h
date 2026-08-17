#pragma once

#include <functional>
#include <memory>
#include <string>
#include <string_view>
#include <vector>

namespace lai {

struct ChatMessage {
    std::string role;
    std::string content;
};

struct GenerationOptions {
    int max_new_tokens = 512;
    float temperature = 0.7F;
    float top_p = 0.9F;
    long long seed = -1;
};

struct GenerationResult {
    int prompt_tokens = 0;
    int generated_tokens = 0;
    long long prompt_eval_us = 0;
    long long time_to_first_token_us = 0;
    long long decode_us = 0;
    long long total_us = 0;
    // Prompt tokens actually decoded this request (total minus the KV-cache prefix that was
    // reused). prompt_eval_us measures ONLY these, so tok/s must divide by this count.
    int evaluated_prompt_tokens = 0;
};

using TokenCallback = std::function<bool(std::string_view)>;
using CancelCallback = std::function<bool()>;

class BackendSession {
public:
    virtual ~BackendSession() = default;
    virtual int count_tokens(const std::vector<ChatMessage>& conversation) = 0;
    virtual GenerationResult generate(
        const std::vector<ChatMessage>& conversation,
        const GenerationOptions& options,
        const TokenCallback& on_token,
        const CancelCallback& is_cancelled
    ) = 0;
};

class Backend {
public:
    virtual ~Backend() = default;
    virtual std::string name() const = 0;
    virtual bool available() const = 0;
    virtual std::unique_ptr<BackendSession> open(
        const std::string& model_path,
        int context_size,
        std::string& error
    ) = 0;
};

std::vector<std::unique_ptr<Backend>> create_backends();

#ifdef LAI_HAS_LLAMA_CPP
std::unique_ptr<Backend> create_llama_cpu_backend();
#endif

}  // namespace lai
