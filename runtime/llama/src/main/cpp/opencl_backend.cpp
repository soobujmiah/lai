#include "include/lai/backend.h"

#include <android/log.h>

#include <string>

#ifdef LAI_HAS_OPENCL
#include "ggml-backend.h"
#include "llama_session.h"
#endif

namespace lai {
namespace {

constexpr const char* kLogTag = "LAI-llama";

#ifdef LAI_HAS_OPENCL

// Returns the GPU device registered by the ggml OpenCL backend (registered under the
// device name "GPUOpenCL"; its description carries the real device string, e.g.
// "Adreno (TM) 825"). ggml-opencl only registers CL_DEVICE_TYPE_GPU devices. Returns
// nullptr when no OpenCL device is registered (no vendor ICD on the device, or the
// driver refused to initialise), which makes this backend report unavailable and lets
// the app fall back to CPU without any acceleration claim.
ggml_backend_dev_t find_opencl_device() {
    const size_t count = ggml_backend_dev_count();
    for (size_t index = 0; index < count; ++index) {
        ggml_backend_dev_t device = ggml_backend_dev_get(index);
        if (device == nullptr) continue;
        const char* name = ggml_backend_dev_name(device);
        const enum ggml_backend_dev_type type = ggml_backend_dev_type(device);
        if (type != GGML_BACKEND_DEVICE_TYPE_GPU && type != GGML_BACKEND_DEVICE_TYPE_IGPU) continue;
        if (name != nullptr && std::string(name).find("OpenCL") != std::string::npos) {
            const char* description = ggml_backend_dev_description(device);
            __android_log_print(
                ANDROID_LOG_INFO, kLogTag,
                "opencl: device %zu type=%d name='%s' description='%s'",
                index, static_cast<int>(type), name,
                description != nullptr ? description : "?"
            );
            return device;
        }
    }
    return nullptr;
}

#endif  // LAI_HAS_OPENCL

// Adreno OpenCL track (docs/BUILD_AND_RELEASE.md). The Adreno 825 Vulkan driver crashes
// natively at vkCmdBindPipeline for MUL_MAT pipelines (addr2line-verified device evidence,
// release-183), and no env/patch combination on the pinned llama.cpp avoids it. Qualcomm's
// own acceleration path for Snapdragon GPUs is the OpenCL backend of llama.cpp: it ships
// Adreno-optimized matmul kernels, is maintained with Qualcomm/Codelinaro, and drives the
// same Adreno GPU through the mature OpenCL driver stack that every Android OpenCL app
// uses. This backend gives the scheduler that path, behind the same evidence gate as
// Vulkan: it is only selected when a qualification build grants DEVICE_VALIDATED evidence
// (-Plai.validatedAccelerators=llama-opencl) and the catalog declares it compatible.
class OpenCLBackend final : public Backend {
public:
    std::string name() const override { return "opencl"; }

    bool available() const override {
#ifdef LAI_HAS_OPENCL
        // Probe only: initializing llama.cpp registers the ggml OpenCL backend, which
        // enumerates OpenCL platforms/devices through the statically linked Khronos ICD
        // loader. On a device without a vendor OpenCL driver this yields zero devices and
        // we report unavailable — no crash, no acceleration claim.
        initialize_llama_once();
        const ggml_backend_dev_t device = find_opencl_device();
        if (device == nullptr) {
            __android_log_print(ANDROID_LOG_WARN, kLogTag, "opencl: compiled but no OpenCL GPU device registered (no vendor ICD?)");
            return false;
        }
        __android_log_print(ANDROID_LOG_INFO, kLogTag, "opencl: GGML_OPENCL compiled and GPU device probed — available");
        return true;
#else
        __android_log_print(ANDROID_LOG_INFO, kLogTag, "opencl: loader may exist but GGML_OPENCL is not compiled — CPU fallback active");
        return false;
#endif
    }

    std::unique_ptr<BackendSession> open(
        const std::string& model_path,
        int context_size,
        std::string& error
    ) override {
#ifdef LAI_HAS_OPENCL
        initialize_llama_once();
        const ggml_backend_dev_t device = find_opencl_device();
        if (device == nullptr) {
            error = "OpenCL compiled but no OpenCL GPU device is available on this device";
            return nullptr;
        }
        // Full layer offload to the Adreno GPU through OpenCL; build_llama_session pins
        // the model's device list to "GPUOpenCL" (+ CPU) so nothing can land on the
        // Vulkan device, keeps LOAD_MODE_NONE (weights must be copied into GPU buffers;
        // mmap would silently keep the integrated GPU at 0 offloaded layers) and disables
        // flash attention for the GPU path (the standard attention kernels are the
        // broadly exercised ones). The Kotlin scheduler only selects this backend after
        // physical-device validation has granted DEVICE_VALIDATED evidence.
        __android_log_print(ANDROID_LOG_INFO, kLogTag, "opencl: opening session with full layer offload");
        return build_llama_session(model_path, context_size, 999, "GPUOpenCL", error);
#else
        (void) model_path;
        (void) context_size;
        error = "OpenCL scaffold: GGML_OPENCL not compiled — CPU fallback active";
        return nullptr;
#endif
    }
};

}  // namespace

std::unique_ptr<Backend> create_opencl_backend() {
    return std::make_unique<OpenCLBackend>();
}

}  // namespace lai
