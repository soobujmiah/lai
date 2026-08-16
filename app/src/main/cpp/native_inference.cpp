#include "include/lai/backend.h"

#include <jni.h>

#include <filesystem>
#include <fstream>
#include <memory>
#include <mutex>
#include <sstream>
#include <string>
#include <unordered_map>

namespace {

std::mutex g_mutex;
std::string g_last_error;
long long g_next_handle = 1;
std::unordered_map<long long, std::unique_ptr<lai::BackendSession>> g_sessions;

std::string from_jstring(JNIEnv* env, jstring value) {
    if (value == nullptr) return {};
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

jstring to_jstring(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}

void set_error(std::string error) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_last_error = std::move(error);
}

bool is_gguf(const std::string& path) {
    std::ifstream input(path, std::ios::binary);
    char magic[4]{};
    input.read(magic, 4);
    return input.gcount() == 4 && std::string(magic, 4) == "GGUF";
}

}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_dev_lai_runtime_inference_NativeBindings_runtimeInfo(JNIEnv* env, jclass) {
    // Do not claim placeholder adapters as compiled acceleration backends.
    return to_jstring(
        env,
        R"({"backends":[],"detail":"JNI boundary ready; no concrete inference backend is compiled in Phase 1"})"
    );
}

extern "C" JNIEXPORT jlong JNICALL
Java_dev_lai_runtime_inference_NativeBindings_createSession(
    JNIEnv* env,
    jclass,
    jstring model_path_value,
    jstring backend_value,
    jint context_size
) {
    const std::string model_path = from_jstring(env, model_path_value);
    const std::string requested = from_jstring(env, backend_value);
    if (context_size < 256 || context_size > 131072) {
        set_error("Invalid context size");
        return 0;
    }
    if (!std::filesystem::is_regular_file(model_path)) {
        set_error("Model file does not exist");
        return 0;
    }
    if (!is_gguf(model_path)) {
        set_error("Model file is not GGUF");
        return 0;
    }

    auto backends = lai::create_backends();
    for (auto& backend : backends) {
        if (requested != "auto" && requested != backend->name()) continue;
        if (!backend->available()) continue;
        std::string error;
        auto session = backend->open(model_path, context_size, error);
        if (session != nullptr) {
            std::lock_guard<std::mutex> lock(g_mutex);
            const long long handle = g_next_handle++;
            g_sessions.emplace(handle, std::move(session));
            return static_cast<jlong>(handle);
        }
        if (!error.empty()) set_error(error);
    }

    set_error("No requested inference backend is available in this artifact");
    return 0;
}

extern "C" JNIEXPORT jstring JNICALL
Java_dev_lai_runtime_inference_NativeBindings_generate(
    JNIEnv* env,
    jclass,
    jlong session_handle,
    jstring prompt_value,
    jstring
) {
    const std::string prompt = from_jstring(env, prompt_value);
    std::lock_guard<std::mutex> lock(g_mutex);
    const auto iterator = g_sessions.find(static_cast<long long>(session_handle));
    if (iterator == g_sessions.end()) {
        g_last_error = "Invalid or closed inference session";
        return to_jstring(env, "");
    }
    lai::GenerationOptions options;
    try {
        return to_jstring(env, iterator->second->generate(prompt, options));
    } catch (const std::exception& exception) {
        g_last_error = exception.what();
        return to_jstring(env, "");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_dev_lai_runtime_inference_NativeBindings_destroySession(JNIEnv*, jclass, jlong session_handle) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_sessions.erase(static_cast<long long>(session_handle));
}

extern "C" JNIEXPORT jstring JNICALL
Java_dev_lai_runtime_inference_NativeBindings_lastError(JNIEnv* env, jclass) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return to_jstring(env, g_last_error);
}
