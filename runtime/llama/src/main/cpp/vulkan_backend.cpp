#include "include/lai/backend.h"

#include <android/log.h>
#include <dlfcn.h>

namespace lai {
namespace {

constexpr const char* kLogTag = "LAI-llama";

class VulkanBackend final : public Backend {
public:
    std::string name() const override { return "vulkan"; }
    bool available() const override {
        void* handle = dlopen("libvulkan.so", RTLD_NOW);
        if (handle == nullptr) {
            __android_log_print(ANDROID_LOG_INFO, kLogTag, "vulkan: libvulkan.so not found — Adreno 825 unavailable");
            return false;
        }
        dlclose(handle);
        __android_log_print(ANDROID_LOG_INFO, kLogTag, "vulkan: loader found, Adreno 825 — scaffold, GGML_VULKAN=OFF (install SPIRV-Headers on CI first)");
        return false;
    }
    std::unique_ptr<BackendSession> open(const std::string&, int, std::string& error) override {
        error = "Vulkan scaffold: GGML_VULKAN not compiled — CPU fallback active";
        return nullptr;
    }
};

}  // namespace

std::unique_ptr<Backend> create_vulkan_backend() {
    return std::make_unique<VulkanBackend>();
}

}  // namespace lai
