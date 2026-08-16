#include "include/lai/backend.h"

#include <jni.h>

#include <algorithm>
#include <cstdint>
#include <filesystem>
#include <fstream>
#include <memory>
#include <mutex>
#include <sstream>
#include <string>
#include <string_view>
#include <unordered_map>
#include <utility>
#include <vector>

namespace {

std::mutex g_mutex;
std::string g_last_error;
long long g_next_handle = 1;
std::unordered_map<long long, std::shared_ptr<lai::BackendSession>> g_sessions;

void append_utf8(std::string& output, uint32_t codepoint) {
    if (codepoint <= 0x7F) {
        output.push_back(static_cast<char>(codepoint));
    } else if (codepoint <= 0x7FF) {
        output.push_back(static_cast<char>(0xC0U | (codepoint >> 6U)));
        output.push_back(static_cast<char>(0x80U | (codepoint & 0x3FU)));
    } else if (codepoint <= 0xFFFF) {
        output.push_back(static_cast<char>(0xE0U | (codepoint >> 12U)));
        output.push_back(static_cast<char>(0x80U | ((codepoint >> 6U) & 0x3FU)));
        output.push_back(static_cast<char>(0x80U | (codepoint & 0x3FU)));
    } else {
        output.push_back(static_cast<char>(0xF0U | (codepoint >> 18U)));
        output.push_back(static_cast<char>(0x80U | ((codepoint >> 12U) & 0x3FU)));
        output.push_back(static_cast<char>(0x80U | ((codepoint >> 6U) & 0x3FU)));
        output.push_back(static_cast<char>(0x80U | (codepoint & 0x3FU)));
    }
}

std::string from_jstring(JNIEnv* env, jstring value) {
    if (value == nullptr) return {};
    const jsize length = env->GetStringLength(value);
    const jchar* chars = env->GetStringChars(value, nullptr);
    if (chars == nullptr) return {};
    std::string result;
    result.reserve(static_cast<size_t>(length) * 2U);
    for (jsize index = 0; index < length; ++index) {
        uint32_t codepoint = chars[index];
        if (codepoint >= 0xD800U && codepoint <= 0xDBFFU && index + 1 < length) {
            const uint32_t low = chars[index + 1];
            if (low >= 0xDC00U && low <= 0xDFFFU) {
                codepoint = 0x10000U + ((codepoint - 0xD800U) << 10U) + (low - 0xDC00U);
                ++index;
            }
        }
        append_utf8(result, codepoint);
    }
    env->ReleaseStringChars(value, chars);
    return result;
}

std::u16string utf8_to_utf16(std::string_view value) {
    std::u16string result;
    result.reserve(value.size());
    size_t index = 0;
    while (index < value.size()) {
        const uint8_t first = static_cast<uint8_t>(value[index]);
        uint32_t codepoint = 0xFFFDU;
        size_t width = 1;
        if (first < 0x80U) {
            codepoint = first;
        } else if ((first & 0xE0U) == 0xC0U && index + 1 < value.size()) {
            codepoint = first & 0x1FU;
            width = 2;
        } else if ((first & 0xF0U) == 0xE0U && index + 2 < value.size()) {
            codepoint = first & 0x0FU;
            width = 3;
        } else if ((first & 0xF8U) == 0xF0U && index + 3 < value.size()) {
            codepoint = first & 0x07U;
            width = 4;
        }
        bool valid = width > 1;
        for (size_t part = 1; part < width && valid; ++part) {
            const uint8_t continuation = static_cast<uint8_t>(value[index + part]);
            valid = (continuation & 0xC0U) == 0x80U;
            codepoint = (codepoint << 6U) | (continuation & 0x3FU);
        }
        if (!valid && width > 1) {
            codepoint = 0xFFFDU;
            width = 1;
        }
        if (codepoint <= 0xFFFFU) {
            result.push_back(static_cast<char16_t>(codepoint));
        } else if (codepoint <= 0x10FFFFU) {
            codepoint -= 0x10000U;
            result.push_back(static_cast<char16_t>(0xD800U + (codepoint >> 10U)));
            result.push_back(static_cast<char16_t>(0xDC00U + (codepoint & 0x3FFU)));
        } else {
            result.push_back(u'\uFFFD');
        }
        index += width;
    }
    return result;
}

jstring to_jstring(JNIEnv* env, std::string_view value) {
    const std::u16string utf16 = utf8_to_utf16(value);
    return env->NewString(
        reinterpret_cast<const jchar*>(utf16.data()),
        static_cast<jsize>(utf16.size())
    );
}

size_t complete_utf8_prefix(std::string_view value) {
    size_t index = 0;
    size_t complete = 0;
    while (index < value.size()) {
        const uint8_t first = static_cast<uint8_t>(value[index]);
        size_t width = 1;
        if (first < 0x80U) width = 1;
        else if ((first & 0xE0U) == 0xC0U) width = 2;
        else if ((first & 0xF0U) == 0xE0U) width = 3;
        else if ((first & 0xF8U) == 0xF0U) width = 4;
        if (index + width > value.size()) break;
        bool valid = true;
        for (size_t part = 1; part < width; ++part) {
            if ((static_cast<uint8_t>(value[index + part]) & 0xC0U) != 0x80U) {
                valid = false;
                break;
            }
        }
        index += valid ? width : 1;
        complete = index;
    }
    return complete;
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

std::shared_ptr<lai::BackendSession> find_session(long long handle) {
    std::lock_guard<std::mutex> lock(g_mutex);
    const auto iterator = g_sessions.find(handle);
    return iterator == g_sessions.end() ? nullptr : iterator->second;
}

}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_dev_lai_runtime_inference_NativeBindings_runtimeInfo(JNIEnv* env, jclass) {
    const auto backends = lai::create_backends();
    std::ostringstream json;
    json << "{\"backends\":[";
    bool first = true;
    for (const auto& backend : backends) {
        if (!backend->available()) continue;
        if (!first) json << ',';
        json << '\"' << backend->name() << '\"';
        first = false;
    }
    json << "],\"detail\":\"";
    if (first) json << "JNI boundary ready; no concrete inference backend is compiled";
    else json << "llama.cpp CPU backend ready";
    json << "\"}";
    return to_jstring(env, json.str());
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
            g_sessions.emplace(handle, std::shared_ptr<lai::BackendSession>(std::move(session)));
            g_last_error.clear();
            return static_cast<jlong>(handle);
        }
        if (!error.empty()) set_error(error);
    }

    set_error("No requested inference backend is available in this artifact");
    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_dev_lai_runtime_inference_NativeBindings_generate(
    JNIEnv* env,
    jclass,
    jlong session_handle,
    jstring prompt_value,
    jint max_new_tokens,
    jfloat temperature,
    jfloat top_p,
    jlong seed,
    jobject callback
) {
    const auto session = find_session(static_cast<long long>(session_handle));
    if (session == nullptr) {
        set_error("Invalid or closed inference session");
        return -1;
    }
    if (callback == nullptr) {
        set_error("Token callback is required");
        return -1;
    }
    const jclass callback_class = env->GetObjectClass(callback);
    const jmethodID on_token_method = env->GetMethodID(callback_class, "onToken", "(Ljava/lang/String;)V");
    const jmethodID is_cancelled_method = env->GetMethodID(callback_class, "isCancelled", "()Z");
    if (on_token_method == nullptr || is_cancelled_method == nullptr) {
        set_error("Token callback contract is incompatible");
        return -1;
    }

    const std::string prompt = from_jstring(env, prompt_value);
    lai::GenerationOptions options{
        static_cast<int>(max_new_tokens),
        static_cast<float>(temperature),
        static_cast<float>(top_p),
        static_cast<long long>(seed),
    };
    std::string pending_utf8;
    auto cancelled = [&]() {
        const jboolean result = env->CallBooleanMethod(callback, is_cancelled_method);
        return env->ExceptionCheck() || result == JNI_TRUE;
    };
    auto emit = [&](std::string_view bytes) {
        pending_utf8.append(bytes);
        const size_t complete = complete_utf8_prefix(pending_utf8);
        if (complete == 0) return !cancelled();
        const jstring text = to_jstring(env, std::string_view(pending_utf8).substr(0, complete));
        env->CallVoidMethod(callback, on_token_method, text);
        env->DeleteLocalRef(text);
        pending_utf8.erase(0, complete);
        return !env->ExceptionCheck() && !cancelled();
    };

    try {
        const int generated = session->generate(prompt, options, emit, cancelled);
        if (!pending_utf8.empty() && !cancelled()) {
            const jstring text = to_jstring(env, pending_utf8);
            env->CallVoidMethod(callback, on_token_method, text);
            env->DeleteLocalRef(text);
        }
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            set_error("Kotlin token callback failed");
            return -1;
        }
        set_error("");
        return static_cast<jint>(generated);
    } catch (const std::exception& exception) {
        set_error(exception.what());
        return -1;
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
