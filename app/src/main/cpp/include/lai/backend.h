#pragma once

#include <functional>
#include <memory>
#include <string>
#include <string_view>
#include <vector>

namespace lai {

struct GenerationOptions {
    int max_new_tokens = 512;
    float temperature = 0.7F;
    float top_p = 0.9F;
    long long seed = -1;
};

using TokenCallback = std::function<bool(std::string_view)>;
using CancelCallback = std::function<bool()>;

class BackendSession {
public:
    virtual ~BackendSession() = default;
    virtual int generate(
        std::string_view prompt,
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
