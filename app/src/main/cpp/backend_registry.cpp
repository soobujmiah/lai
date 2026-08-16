#include "include/lai/backend.h"

#include <memory>
#include <string>
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
        error = name_ + " adapter is not compiled into this Phase 1 artifact";
        return nullptr;
    }

private:
    std::string name_;
};

}  // namespace

std::vector<std::unique_ptr<Backend>> create_backends() {
    std::vector<std::unique_ptr<Backend>> backends;
#ifdef LAI_HAS_LLAMA_CPP
    // The pinned llama.cpp adapter is introduced in Phase 2. This flag is never
    // presented as available until that concrete adapter is linked.
#endif
#ifdef LAI_HAS_QNN
    // QNN runtime loading and graph adapters require QAIRT headers/libraries.
#endif
    backends.emplace_back(std::make_unique<PlaceholderBackend>("cpu"));
    backends.emplace_back(std::make_unique<PlaceholderBackend>("vulkan"));
    backends.emplace_back(std::make_unique<PlaceholderBackend>("qnn"));
    return backends;
}

}  // namespace lai
