#include "include/lai/backend.h"

#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>

#include <cstdlib>
#include <filesystem>
#include <fstream>
#include <string>
#include <system_error>

#ifdef LAI_HAS_OPENCL
#include "ggml-backend.h"
#include "llama_session.h"
#endif

namespace lai {
namespace {

constexpr const char* kLogTag = "LAI-llama";

std::string jstring_to_utf8(JNIEnv* env, jstring value) {
    if (value == nullptr) return {};
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

#ifdef LAI_HAS_OPENCL

// True when `dir` exists and contains at least one .icd vendor file.
bool dir_has_icd_files(const char* dir) {
    std::error_code ec;
    if (!std::filesystem::is_directory(dir, ec)) return false;
    for (auto it = std::filesystem::directory_iterator(dir, ec);
         !ec && it != std::filesystem::directory_iterator(); it.increment(ec)) {
        if (it->path().extension() == ".icd") return true;
    }
    return false;
}

// Returns the first GPU device registered by the ggml OpenCL backend (registered under the
// device name "GPUOpenCL"; its description carries the real device string, e.g.
// "Adreno (TM) 825"). ggml-opencl only registers CL_DEVICE_TYPE_GPU devices. Returns
// nullptr when no OpenCL device is registered (no vendor ICD on the device, or the
// driver refused to initialise), which makes this backend report unavailable and lets
// the app fall back to CPU without any acceleration claim. Logs every registered ggml
// device so a failed probe is fully diagnosable from logcat.
ggml_backend_dev_t find_opencl_device() {
    const size_t count = ggml_backend_dev_count();
    ggml_backend_dev_t match = nullptr;
    for (size_t index = 0; index < count; ++index) {
        ggml_backend_dev_t device = ggml_backend_dev_get(index);
        if (device == nullptr) continue;
        const char* name = ggml_backend_dev_name(device);
        const char* description = ggml_backend_dev_description(device);
        __android_log_print(
            ANDROID_LOG_INFO, kLogTag,
            "opencl probe: device %zu type=%d name='%s' description='%s'",
            index, static_cast<int>(ggml_backend_dev_type(device)),
            name != nullptr ? name : "?",
            description != nullptr ? description : "?"
        );
        if (match != nullptr || name == nullptr) continue;
        const enum ggml_backend_dev_type type = ggml_backend_dev_type(device);
        if (type != GGML_BACKEND_DEVICE_TYPE_GPU && type != GGML_BACKEND_DEVICE_TYPE_IGPU) continue;
        if (std::string(name).find("OpenCL") != std::string::npos) match = device;
    }
    return match;
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
        // Direct namespace probe: can this app process even dlopen the vendor OpenCL
        // library? Qualcomm exposes it through the public-library namespace on most
        // devices; a failure here names the exact linker error in logcat.
        void* probe = dlopen("libOpenCL.so", RTLD_NOW);
        if (probe == nullptr) {
            __android_log_print(ANDROID_LOG_WARN, kLogTag, "opencl: dlopen(libOpenCL.so) failed: %s", dlerror());
        } else {
            __android_log_print(ANDROID_LOG_INFO, kLogTag, "opencl: dlopen(libOpenCL.so) OK");
            dlclose(probe);
        }
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

// Android OpenCL vendor discovery (device evidence 2026-08-20: the Khronos ICD loader
// compiled into liblai_runtime.so found zero platforms on the Redmi Turbo 4 Pro because
// its default Android search path is ONLY /system/vendor/Khronos/OpenCL/vendors, while
// Qualcomm devices expose the Adreno OpenCL driver elsewhere — typically resolvable as
// the plain soname "libOpenCL.so" through the app's public-library namespace, or under
// /vendor/lib64). The loader honours OCL_ICD_VENDORS, but as a SINGLE directory that it
// opens with opendir(), and reads one library name per *.icd file. So when no system
// vendor directory with .icd files exists, synthesize one in app-private storage listing
// every plausible driver location, and point OCL_ICD_VENDORS at it. Must run before the
// first ggml backend probe (initialize_llama_once), i.e. from LaiApplication.onCreate.
void prepare_opencl_vendor_dir(const std::string& base_dir) {
#ifdef LAI_HAS_OPENCL
    namespace fs = std::filesystem;
    static const char* kSystemVendorDirs[] = {
        "/system/vendor/Khronos/OpenCL/vendors",
        "/vendor/Khronos/OpenCL/vendors",
    };
    for (const char* system_dir : kSystemVendorDirs) {
        if (dir_has_icd_files(system_dir)) {
            __android_log_print(
                ANDROID_LOG_INFO, kLogTag,
                "opencl: system vendor ICD directory present (%s) — using loader default",
                system_dir
            );
            return;
        }
    }
    std::error_code ec;
    const fs::path vendors_dir = fs::path(base_dir) / "lai-opencl-vendors";
    fs::create_directories(vendors_dir, ec);
    if (ec) {
        __android_log_print(ANDROID_LOG_WARN, kLogTag, "opencl: cannot create vendor dir (%s)", ec.message().c_str());
        return;
    }
    // One candidate per .icd file; the loader dlopens each and skips failures. The bare
    // soname first: that is how Android resolves the vendor's Adreno OpenCL driver through
    // the public-library namespace on Qualcomm devices.
    static const char* kCandidates[][2] = {
        {"10-libOpenCL.so.icd", "libOpenCL.so"},
        {"20-vendor-lib64.icd", "/vendor/lib64/libOpenCL.so"},
        {"30-system-vendor-lib64.icd", "/system/vendor/lib64/libOpenCL.so"},
        {"40-system-lib64.icd", "/system/lib64/libOpenCL.so"},
    };
    for (const auto& candidate : kCandidates) {
        std::ofstream out(vendors_dir / candidate[0], std::ios::trunc);
        if (out) out << candidate[1] << "\n";
    }
    setenv("OCL_ICD_VENDORS", vendors_dir.c_str(), 1);
    __android_log_print(
        ANDROID_LOG_INFO, kLogTag,
        "opencl: no system ICD directory — synthesized %zu vendor entries at %s (OCL_ICD_VENDORS set)",
        sizeof(kCandidates) / sizeof(kCandidates[0]), vendors_dir.c_str()
    );
#else
    (void) base_dir;
    __android_log_print(ANDROID_LOG_INFO, kLogTag, "opencl: GGML_OPENCL not compiled — vendor discovery skipped");
#endif
}

}  // namespace lai

extern "C" JNIEXPORT void JNICALL
Java_dev_lai_runtime_inference_NativeBindings_configureOpenCLVendors(JNIEnv* env, jclass, jstring base_dir) {
    lai::prepare_opencl_vendor_dir(lai::jstring_to_utf8(env, base_dir));
}
