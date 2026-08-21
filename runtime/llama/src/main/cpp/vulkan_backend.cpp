#include "include/lai/backend.h"

#include <android/log.h>
#include <dlfcn.h>

#include <cstdlib>
#include <string>

#ifdef LAI_HAS_VULKAN
#include "ggml-backend.h"
#include "llama_session.h"
#endif

namespace lai {
namespace {

constexpr const char* kLogTag = "LAI-llama";

#ifdef LAI_HAS_VULKAN

void configure_adreno_vulkan_env() {
    // Adreno driver workarounds (SM8735 / Adreno 825, device evidence in SKB and LAI).
    // Keep these identical for availability probing and session open; ggml-vulkan reads several
    // switches while the Vulkan device is first created, so inconsistent setup can accidentally
    // re-enable an unsafe shader path.
    setenv("GGML_VK_DISABLE_COOPMAT", "1", 1);
    setenv("GGML_VK_DISABLE_COOPMAT2", "1", 1);
    setenv("GGML_VK_DISABLE_MMVQ", "1", 1);
    setenv("GGML_VK_DISABLE_INTEGER_DOT_PRODUCT", "1", 1);
    setenv("GGML_VK_DISABLE_F16", "1", 1);
    setenv("GGML_VK_DISABLE_ASYNC", "1", 1);
    // Route through the graphics queue on Adreno. SKB records compute-queue sensitivity and a
    // vendor-driver crash at vkCmdBindPipeline during ggml MUL_MAT.
    setenv("GGML_VK_ALLOW_GRAPHICS_QUEUE", "1", 1);
    setenv("GGML_VK_DISABLE_MULTI_ADD", "1", 1);
    setenv("GGML_VK_DISABLE_FUSION", "1", 1);
    setenv("GGML_VK_DISABLE_GRAPH_OPTIMIZE", "1", 1);
}

// Returns the first GPU-class device registered by ggml — on this build that is the
// Vulkan backend driving the Adreno 825. ggml-vulkan classifies an integrated GPU
// (every Android Adreno/Mali) as GGML_BACKEND_DEVICE_TYPE_IGPU, not GPU — both must be
// accepted or the only accelerator on the device is skipped. Prefers a device whose name
// contains "Vulkan"; otherwise falls back to any GPU/IGPU-class device. Returns nullptr
// when no GPU device is registered (loader present but driver/backend failed to init).
ggml_backend_dev_t find_gpu_device() {
    const size_t count = ggml_backend_dev_count();
    ggml_backend_dev_t fallback = nullptr;
    for (size_t index = 0; index < count; ++index) {
        ggml_backend_dev_t device = ggml_backend_dev_get(index);
        if (device == nullptr) continue;
        const char* name = ggml_backend_dev_name(device);
        __android_log_print(
            ANDROID_LOG_INFO, kLogTag,
            "vulkan: device %zu type=%d name='%s'",
            index, static_cast<int>(ggml_backend_dev_type(device)),
            name != nullptr ? name : "?"
        );
        const enum ggml_backend_dev_type type = ggml_backend_dev_type(device);
        // Integrated GPUs (Adreno 825 included) report IGPU; discrete ones report GPU.
        if (type != GGML_BACKEND_DEVICE_TYPE_GPU && type != GGML_BACKEND_DEVICE_TYPE_IGPU) continue;
        if (fallback == nullptr) fallback = device;
        if (name != nullptr && std::string(name).find("Vulkan") != std::string::npos) {
            return device;
        }
    }
    return fallback;
}

#endif  // LAI_HAS_VULKAN

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
#ifdef LAI_HAS_VULKAN
        configure_adreno_vulkan_env();
        initialize_llama_once();
        const ggml_backend_dev_t device = find_gpu_device();
        if (device == nullptr) {
            __android_log_print(ANDROID_LOG_WARN, kLogTag, "vulkan: loader present but no GPU device registered by ggml");
            return false;
        }
        __android_log_print(ANDROID_LOG_INFO, kLogTag, "vulkan: GGML_VULKAN compiled and GPU device probed — available");
        return true;
#else
        __android_log_print(ANDROID_LOG_INFO, kLogTag, "vulkan: loader found but GGML_VULKAN is not compiled — CPU fallback active");
        return false;
#endif
    }

    std::unique_ptr<BackendSession> open(
        const std::string& model_path,
        int context_size,
        std::string& error
    ) override {
#ifdef LAI_HAS_VULKAN
        configure_adreno_vulkan_env();
        initialize_llama_once();
        const ggml_backend_dev_t device = find_gpu_device();
        if (device == nullptr) {
            error = "Vulkan compiled but no GPU device is available";
            return nullptr;
        }
        // Offload every model layer to the Adreno device; llama.cpp keeps the CPU for
        // prompt/output glue. The Kotlin scheduler only selects this backend after
        // physical-device validation has granted DEVICE_VALIDATED evidence. The "Vulkan"
        // pin keeps the offload on the Vulkan device even though the OpenCL backend is
        // compiled into the same artifact (Adreno track).
        __android_log_print(ANDROID_LOG_INFO, kLogTag, "vulkan: opening session with full layer offload");
        return build_llama_session(model_path, context_size, 999, "Vulkan", error);
#else
        (void) model_path;
        (void) context_size;
        error = "Vulkan scaffold: GGML_VULKAN not compiled — CPU fallback active";
        return nullptr;
#endif
    }
};

}  // namespace

std::unique_ptr<Backend> create_vulkan_backend() {
    return std::make_unique<VulkanBackend>();
}

}  // namespace lai
