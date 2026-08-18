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
        // Best-effort probe: does the device expose a Vulkan loader? SM8735 Adreno 750 should.
        void* handle = dlopen("libvulkan.so", RTLD_NOW);
        if (handle == nullptr) {
            __android_log_print(ANDROID_LOG_INFO, kLogTag, "vulkan: libvulkan.so not found — Adreno Vulkan unavailable");
            return false;
        }
        dlclose(handle);
        // Loader exists, but GGML Vulkan not yet compiled into this artifact (GGML_VULKAN=OFF).
        // Report unavailable until we compile with -DGGML_VULKAN=ON and qualify on SM8735.
        __android_log_print(ANDROID_LOG_INFO, kLogTag, "vulkan: loader found, backend scaffold present — awaiting GGML_VULKAN build");
        return false;
    }
    std::unique_ptr<BackendSession> open(const std::string&, int, std::string& error) override {
        error = "Vulkan backend scaffold: GGML_VULKAN not yet compiled — CPU fallback active";
        return nullptr;
    }
};

}  // namespace

std::unique_ptr<Backend> create_vulkan_backend() {
    return std::make_unique<VulkanBackend>();
}

}  // namespace lai
