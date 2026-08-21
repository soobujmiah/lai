#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>

#include <cstdint>
#include <cstring>
#include <string>
#include <vector>

namespace {
constexpr const char* kTag = "LAI-GPU-Probe";

using cl_int = int32_t;
using cl_uint = uint32_t;
using cl_ulong = uint64_t;
using cl_platform_id = void*;
using cl_device_id = void*;
using cl_context = void*;
using cl_command_queue = void*;
using cl_mem = void*;
using cl_kernel = void*;
using cl_program = void*;
using cl_event = void*;
using cl_bitfield = uint64_t;
using cl_device_type = cl_bitfield;
using cl_mem_flags = cl_bitfield;

constexpr cl_int CL_SUCCESS = 0;
constexpr cl_device_type CL_DEVICE_TYPE_GPU = (1u << 2);
constexpr cl_uint CL_DEVICE_NAME = 0x102B;
constexpr cl_uint CL_DEVICE_VENDOR = 0x102C;
constexpr cl_uint CL_DEVICE_VERSION = 0x102F;
constexpr cl_uint CL_PLATFORM_NAME = 0x0902;
constexpr cl_mem_flags CL_MEM_READ_ONLY = (1ull << 2);
constexpr cl_mem_flags CL_MEM_WRITE_ONLY = (1ull << 1);
constexpr cl_mem_flags CL_MEM_COPY_HOST_PTR = (1ull << 5);
constexpr cl_uint CL_TRUE = 1;
constexpr cl_uint CL_FALSE = 0;

using FnGetPlatformIDs = cl_int (*)(cl_uint, cl_platform_id*, cl_uint*);
using FnGetPlatformInfo = cl_int (*)(cl_platform_id, cl_uint, size_t, void*, size_t*);
using FnGetDeviceIDs = cl_int (*)(cl_platform_id, cl_device_type, cl_uint, cl_device_id*, cl_uint*);
using FnGetDeviceInfo = cl_int (*)(cl_device_id, cl_uint, size_t, void*, size_t*);
using FnCreateContext = cl_context (*)(const void*, cl_uint, const cl_device_id*, void*, void*, cl_int*);
using FnReleaseContext = cl_int (*)(cl_context);
using FnCreateCommandQueue = cl_command_queue (*)(cl_context, cl_device_id, cl_bitfield, cl_int*);
using FnReleaseCommandQueue = cl_int (*)(cl_command_queue);
using FnCreateBuffer = cl_mem (*)(cl_context, cl_mem_flags, size_t, void*, cl_int*);
using FnReleaseMemObject = cl_int (*)(cl_mem);
using FnEnqueueWriteBuffer = cl_int (*)(cl_command_queue, cl_mem, cl_uint, size_t, size_t, const void*, cl_uint, const cl_event*, cl_event*);
using FnEnqueueReadBuffer = cl_int (*)(cl_command_queue, cl_mem, cl_uint, size_t, size_t, void*, cl_uint, const cl_event*, cl_event*);
using FnFinish = cl_int (*)(cl_command_queue);

struct Api {
    void* handle = nullptr;
    FnGetPlatformIDs get_platform_ids = nullptr;
    FnGetPlatformInfo get_platform_info = nullptr;
    FnGetDeviceIDs get_device_ids = nullptr;
    FnGetDeviceInfo get_device_info = nullptr;
    FnCreateContext create_context = nullptr;
    FnReleaseContext release_context = nullptr;
    FnCreateCommandQueue create_command_queue = nullptr;
    FnReleaseCommandQueue release_command_queue = nullptr;
    FnCreateBuffer create_buffer = nullptr;
    FnReleaseMemObject release_mem_object = nullptr;
    FnEnqueueWriteBuffer enqueue_write_buffer = nullptr;
    FnEnqueueReadBuffer enqueue_read_buffer = nullptr;
    FnFinish finish = nullptr;
};

template <typename T>
bool load_symbol(void* handle, const char* name, T& out) {
    out = reinterpret_cast<T>(dlsym(handle, name));
    if (!out) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "missing OpenCL symbol: %s", name);
        return false;
    }
    return true;
}

std::string query_string(FnGetDeviceInfo fn, cl_device_id device, cl_uint param) {
    size_t size = 0;
    if (fn(device, param, 0, nullptr, &size) != CL_SUCCESS || size == 0) return {};
    std::vector<char> value(size);
    if (fn(device, param, value.size(), value.data(), nullptr) != CL_SUCCESS) return {};
    return std::string(value.data());
}

std::string platform_string(FnGetPlatformInfo fn, cl_platform_id platform, cl_uint param) {
    size_t size = 0;
    if (fn(platform, param, 0, nullptr, &size) != CL_SUCCESS || size == 0) return {};
    std::vector<char> value(size);
    if (fn(platform, param, value.size(), value.data(), nullptr) != CL_SUCCESS) return {};
    return std::string(value.data());
}

bool load_api(Api& api) {
    // Prefer the Android public soname. The vendor library itself is intentionally not
    // executed as a program; it is loaded as a shared object and resolved by the linker.
    const char* candidates[] = {"libOpenCL.so", "/vendor/lib64/libOpenCL_adreno.so"};
    for (const char* candidate : candidates) {
        api.handle = dlopen(candidate, RTLD_NOW | RTLD_LOCAL);
        if (api.handle) {
            __android_log_print(ANDROID_LOG_INFO, kTag, "OpenCL library loaded: %s", candidate);
            break;
        }
        __android_log_print(ANDROID_LOG_WARN, kTag, "dlopen('%s') failed: %s", candidate, dlerror());
    }
    if (!api.handle) return false;

#define LOAD(name) if (!load_symbol(api.handle, #name, api.name)) return false
    LOAD(get_platform_ids);
    LOAD(get_platform_info);
    LOAD(get_device_ids);
    LOAD(get_device_info);
    LOAD(create_context);
    LOAD(release_context);
    LOAD(create_command_queue);
    LOAD(release_command_queue);
    LOAD(create_buffer);
    LOAD(release_mem_object);
    LOAD(enqueue_write_buffer);
    LOAD(enqueue_read_buffer);
    LOAD(finish);
#undef LOAD
    return true;
}

} // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_dev_lai_runtime_inference_NativeBindings_qualifyOpenCL(JNIEnv* env, jclass) {
    Api api;
    if (!load_api(api)) {
        return env->NewStringUTF("{\"status\":\"UNAVAILABLE\",\"reason\":\"dlopen failed\"}");
    }

    cl_uint platform_count = 0;
    cl_int rc = api.get_platform_ids(0, nullptr, &platform_count);
    if (rc != CL_SUCCESS || platform_count == 0) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "clGetPlatformIDs: rc=%d count=%u", rc, platform_count);
        dlclose(api.handle);
        return env->NewStringUTF("{\"status\":\"NO_PLATFORM\"}");
    }

    std::vector<cl_platform_id> platforms(platform_count);
    rc = api.get_platform_ids(platform_count, platforms.data(), nullptr);
    if (rc != CL_SUCCESS) {
        dlclose(api.handle);
        return env->NewStringUTF("{\"status\":\"PLATFORM_ENUM_FAILED\"}");
    }

    for (cl_platform_id platform : platforms) {
        const std::string platform_name = platform_string(api.get_platform_info, platform, CL_PLATFORM_NAME);
        __android_log_print(ANDROID_LOG_INFO, kTag, "OpenCL platform: %s", platform_name.c_str());

        cl_uint device_count = 0;
        rc = api.get_device_ids(platform, CL_DEVICE_TYPE_GPU, 0, nullptr, &device_count);
        if (rc != CL_SUCCESS || device_count == 0) continue;

        std::vector<cl_device_id> devices(device_count);
        rc = api.get_device_ids(platform, CL_DEVICE_TYPE_GPU, device_count, devices.data(), nullptr);
        if (rc != CL_SUCCESS) continue;

        for (cl_device_id device : devices) {
            const std::string name = query_string(api.get_device_info, device, CL_DEVICE_NAME);
            const std::string vendor = query_string(api.get_device_info, device, CL_DEVICE_VENDOR);
            const std::string version = query_string(api.get_device_info, device, CL_DEVICE_VERSION);
            __android_log_print(ANDROID_LOG_INFO, kTag,
                "OpenCL GPU: name='%s' vendor='%s' version='%s'", name.c_str(), vendor.c_str(), version.c_str());

            cl_int err = CL_SUCCESS;
            cl_context context = api.create_context(nullptr, 1, &device, nullptr, nullptr, &err);
            if (!context || err != CL_SUCCESS) continue;
            cl_command_queue queue = api.create_command_queue(context, device, 0, &err);
            if (!queue || err != CL_SUCCESS) {
                api.release_context(context);
                continue;
            }

            // Conservative sanity test: write/read a small buffer. This validates actual
            // command submission and memory access without introducing kernel compilation.
            constexpr size_t kBytes = 4096;
            std::vector<uint8_t> input(kBytes), output(kBytes, 0);
            for (size_t i = 0; i < input.size(); ++i) input[i] = static_cast<uint8_t>(i * 31u + 7u);
            cl_mem buffer = api.create_buffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, kBytes, input.data(), &err);
            if (!buffer || err != CL_SUCCESS) {
                api.release_command_queue(queue);
                api.release_context(context);
                continue;
            }
            err = api.enqueue_read_buffer(queue, buffer, CL_TRUE, 0, kBytes, output.data(), 0, nullptr, nullptr);
            const bool io_ok = err == CL_SUCCESS && std::memcmp(input.data(), output.data(), kBytes) == 0;
            err = api.finish(queue);
            api.release_mem_object(buffer);
            api.release_command_queue(queue);
            api.release_context(context);
            dlclose(api.handle);

            if (io_ok && err == CL_SUCCESS) {
                std::string json = "{\"status\":\"COMPUTE_IO_VALIDATED\",\"platform\":\"" + platform_name +
                    "\",\"device\":\"" + name + "\",\"vendor\":\"" + vendor + "\",\"version\":\"" + version + "\"}";
                return env->NewStringUTF(json.c_str());
            }
        }
    }

    dlclose(api.handle);
    return env->NewStringUTF("{\"status\":\"GPU_DEVICE_FOUND_BUT_IO_FAILED\"}");
}
