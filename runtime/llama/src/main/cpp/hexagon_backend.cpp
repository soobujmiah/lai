#include "include/lai/backend.h"

#include <android/log.h>

#include <chrono>
#include <string>

#ifdef LAI_HAS_HEXAGON
#include "ggml-backend.h"
#include "llama_session.h"
#endif

namespace lai {
namespace {

constexpr const char* kLogTag = "LAI-llama";
// See llama_session.cpp's own kDiagTag doc comment: same distinct, permanent diagnostic tag,
// duplicated locally (rather than shared via the header) to avoid coupling this translation
// unit's diagnostics to llama_session.cpp's internals for a handful of log-only helpers.
constexpr const char* kDiagTag = "LAI-diag";
using DiagClock = std::chrono::steady_clock;

[[maybe_unused]] long long diag_elapsed_us(DiagClock::time_point start, DiagClock::time_point end) {
    return std::chrono::duration_cast<std::chrono::microseconds>(end - start).count();
}

#ifdef LAI_HAS_HEXAGON

// Returns the first Hexagon HTP device registered by ggml (named "HTP0", "HTP1", ... one per
// DSP session; see ggml-hexagon.cpp). ggml-hexagon classifies the HTP the same as any other
// accelerator, GGML_BACKEND_DEVICE_TYPE_GPU (upstream's own doc: "Hexagon NPU behaves as a
// 'GPU' device when it comes to -ngl and other offload-related options") -- name match is
// what actually distinguishes it from Vulkan/OpenCL, which also register as GPU-type devices
// in the same compiled artifact. Returns nullptr when no HTP device is registered (SDK/skel
// mismatch, or the driver refused to initialise), which makes this backend report
// unavailable and lets the app fall back to CPU without any acceleration claim.
ggml_backend_dev_t find_htp_device() {
    const size_t count = ggml_backend_dev_count();
    ggml_backend_dev_t match = nullptr;
    for (size_t index = 0; index < count; ++index) {
        ggml_backend_dev_t device = ggml_backend_dev_get(index);
        if (device == nullptr) continue;
        const char* name = ggml_backend_dev_name(device);
        const char* description = ggml_backend_dev_description(device);
        __android_log_print(
            ANDROID_LOG_INFO, kLogTag,
            "hexagon probe: device %zu type=%d name='%s' description='%s'",
            index, static_cast<int>(ggml_backend_dev_type(device)),
            name != nullptr ? name : "?",
            description != nullptr ? description : "?"
        );
        if (match != nullptr || name == nullptr) continue;
        if (std::string(name).rfind("HTP", 0) == 0) match = device;
    }
    return match;
}

#endif  // LAI_HAS_HEXAGON

// Hexagon NPU track (docs/HANDOFF-2026-09-03-npu-hexagon-scoping.md). SM8735's Hexagon HTP is
// confirmed v73 (device evidence: only libQnnHtpV73*.so present under
// /vendor/lib/rfsa/adsp/). ggml-hexagon is compiled from upstream's own already-vendored
// backend against the Hexagon SDK 6.6.0.0 extracted from the public snapdragon-toolchain
// image in CI; it cross-compiles all four HTP skels (v73/v75/v79/v81) unconditionally, so no
// per-chip build flag is needed -- the runtime picks the one matching this device. Same
// evidence gate as Vulkan/OpenCL: selected only after a qualification build grants
// DEVICE_VALIDATED (-Plai.validatedAccelerators=llama-hexagon) and the catalog declares it
// compatible. No real-device generation has been measured yet -- this adapter exists so that
// qualification can happen the same way Vulkan/OpenCL's did, not as an acceleration claim.
class HexagonBackend final : public Backend {
public:
    std::string name() const override { return "hexagon"; }

    bool available() const override {
#ifdef LAI_HAS_HEXAGON
        __android_log_print(ANDROID_LOG_INFO, kDiagTag, "hexagon: available() ENTER");
        initialize_llama_once();
        const auto probe_start = DiagClock::now();
        const ggml_backend_dev_t device = find_htp_device();
        __android_log_print(
            ANDROID_LOG_INFO, kDiagTag, "hexagon: available() find_htp_device() returned %p after %lld us",
            static_cast<void*>(device), diag_elapsed_us(probe_start, DiagClock::now())
        );
        if (device == nullptr) {
            __android_log_print(ANDROID_LOG_WARN, kLogTag, "hexagon: compiled but no HTP device registered (skel/session init failed?)");
            return false;
        }
        __android_log_print(ANDROID_LOG_INFO, kLogTag, "hexagon: GGML_HEXAGON compiled and HTP device probed — available");
        return true;
#else
        __android_log_print(ANDROID_LOG_INFO, kLogTag, "hexagon: GGML_HEXAGON is not compiled — CPU fallback active");
        return false;
#endif
    }

    std::unique_ptr<BackendSession> open(
        const std::string& model_path,
        int context_size,
        std::string& error
    ) override {
#ifdef LAI_HAS_HEXAGON
        __android_log_print(ANDROID_LOG_INFO, kDiagTag, "hexagon: open() ENTER");
        initialize_llama_once();
        __android_log_print(ANDROID_LOG_INFO, kDiagTag, "hexagon: open() calling find_htp_device()");
        const auto probe_start = DiagClock::now();
        const ggml_backend_dev_t device = find_htp_device();
        __android_log_print(
            ANDROID_LOG_INFO, kDiagTag, "hexagon: open() find_htp_device() returned %p after %lld us",
            static_cast<void*>(device), diag_elapsed_us(probe_start, DiagClock::now())
        );
        if (device == nullptr) {
            error = "Hexagon compiled but no HTP device is available on this device";
            return nullptr;
        }
        // Full layer offload to the single HTP session. GGML_HEXAGON_NDEV (default 1) governs
        // how many HTP sessions ggml-hexagon allocates; a single Qwen-1.5B-class model fits one
        // per upstream's own sizing guidance (docs/backend/snapdragon/README.md), so pin to the
        // first session only -- do not raise NDEV without new evidence this model needs it.
        __android_log_print(ANDROID_LOG_INFO, kLogTag, "hexagon: opening session with full layer offload");
        __android_log_print(ANDROID_LOG_INFO, kDiagTag, "hexagon: open() calling build_llama_session()");
        return build_llama_session(model_path, context_size, 999, "HTP0", error);
#else
        (void) model_path;
        (void) context_size;
        error = "Hexagon scaffold: GGML_HEXAGON not compiled — CPU fallback active";
        return nullptr;
#endif
    }
};

}  // namespace

std::unique_ptr<Backend> create_hexagon_backend() {
    return std::make_unique<HexagonBackend>();
}

}  // namespace lai
