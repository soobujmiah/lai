#include "include/lai/backend.h"

#include <memory>
#include <string>
#include <utility>
#include <vector>

namespace lai {
namespace {

class PlaceholderBackend final : public Backend {
public:
    explicit PlaceholderBackend(std::string backend_name) : name_(std::move(backend_name)) {}

    std::string name() const override { return name_; }
    bool available() const override { return false; }

    std::unique_ptr<BackendSession> open(
        const std::string&,
        int,
        std::string& error
    ) override {
        error = name_ + " adapter is not compiled into this artifact";
        return nullptr;
    }

private:
    std::string name_;
};

}  // namespace

std::vector<std::unique_ptr<Backend>> create_backends() {
    std::vector<std::unique_ptr<Backend>> backends;
#ifdef LAI_HAS_LLAMA_CPP
    backends.emplace_back(create_llama_cpu_backend());
#else
    backends.emplace_back(std::make_unique<PlaceholderBackend>("cpu"));
#endif
    backends.emplace_back(create_vulkan_backend());
    return backends;
}

}  // namespace lai
