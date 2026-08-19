#include "native_crash_handler.h"

#include <android/log.h>
#include <dlfcn.h>
#include <unwind.h>

#include <fcntl.h>
#include <signal.h>
#include <string.h>
#include <unistd.h>

#include <cstdint>
#include <cstdio>

namespace lai {
namespace {

constexpr const char* kCrashTag = "LAI-crash";

char g_log_path[512] = {0};

struct BacktraceState {
    void** current;
    void** end;
};

_Unwind_Reason_Code unwind_callback(struct _Unwind_Context* ctx, void* arg) {
    BacktraceState* state = static_cast<BacktraceState*>(arg);
    const uintptr_t pc = _Unwind_GetIP(ctx);
    if (pc != 0) {
        if (state->current == state->end) return _URC_END_OF_STACK;
        *state->current++ = reinterpret_cast<void*>(pc);
    }
    return _URC_NO_REASON;
}

size_t capture_backtrace(void** buffer, size_t max) {
    BacktraceState state = {buffer, buffer + max};
    _Unwind_Backtrace(unwind_callback, &state);
    return state.current - buffer;
}

// Async-signal-safe line writer: logcat + append to the diagnostic log file.
void write_crash_line(const char* line) {
    __android_log_write(ANDROID_LOG_FATAL, kCrashTag, line);
    if (g_log_path[0] == '\0') return;
    const int fd = open(g_log_path, O_WRONLY | O_CREAT | O_APPEND, 0644);
    if (fd < 0) return;
    const size_t len = strlen(line);
    const char* nl = "\n";
    write(fd, line, len);
    write(fd, nl, 1);
    close(fd);
}

const char* signal_name(int sig) {
    switch (sig) {
        case SIGSEGV: return "SIGSEGV (invalid memory access)";
        case SIGABRT: return "SIGABRT (abort)";
        case SIGBUS: return "SIGBUS (bus error)";
        case SIGILL: return "SIGILL (illegal instruction)";
        case SIGFPE: return "SIGFPE (float exception)";
        default: return "unknown signal";
    }
}

void crash_handler(int sig) {
    // Restore defaults first: no recursion, and re-raising below delivers to the OS.
    signal(sig, SIG_DFL);

    char head[256];
    snprintf(head, sizeof head, "NATIVE CRASH signal=%d (%s)", sig, signal_name(sig));
    write_crash_line(head);
    write_crash_line("Backtrace (crashing thread):");

    void* frames[64];
    const size_t count = capture_backtrace(frames, 64);
    for (size_t i = 0; i < count; ++i) {
        char line[640];
        Dl_info info;
        if (dladdr(frames[i], &info) != 0 && info.dli_sname != nullptr) {
            const uintptr_t offset =
                reinterpret_cast<const char*>(frames[i]) - reinterpret_cast<const char*>(info.dli_saddr);
            const uintptr_t base =
                reinterpret_cast<const char*>(frames[i]) - reinterpret_cast<const char*>(info.dli_fbase);
            snprintf(
                line, sizeof line,
                "  #%02zu pc=0x%lx(+0x%lx) %s+0x%lx  [%s]",
                i,
                static_cast<unsigned long>(base), static_cast<unsigned long>(frames[i] - info.dli_fbase),
                info.dli_sname, static_cast<unsigned long>(offset),
                info.dli_fname != nullptr ? info.dli_fname : "?"
            );
        } else {
            // Unsymbolized (hidden-visibility) frame: still record the load-base-relative offset
            // so it can be mapped offline with addr2line against an unstripped build.
            void* base = nullptr;
            Dl_info libinfo;
            if (dladdr(frames[i], &libinfo) != 0) {
                base = libinfo.dli_fbase;
                snprintf(
                    line, sizeof line,
                    "  #%02zu pc=0x%lx(+0x%lx)  [%s]",
                    i,
                    static_cast<unsigned long>(reinterpret_cast<const char*>(frames[i]) -
                                               reinterpret_cast<const char*>(base)),
                    static_cast<unsigned long>(frames[i] - base),
                    libinfo.dli_fname != nullptr ? libinfo.dli_fname : "?"
                );
            } else {
                snprintf(line, sizeof line, "  #%02zu %p", i, frames[i]);
            }
        }
        write_crash_line(line);
    }

    raise(sig);
}

}  // namespace

void install_native_crash_handler(const char* log_file_path) {
    if (log_file_path != nullptr) {
        strncpy(g_log_path, log_file_path, sizeof g_log_path - 1);
        g_log_path[sizeof g_log_path - 1] = '\0';
    }
    struct sigaction sa {};
    sa.sa_handler = crash_handler;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGSEGV, &sa, nullptr);
    sigaction(SIGABRT, &sa, nullptr);
    sigaction(SIGBUS, &sa, nullptr);
    sigaction(SIGILL, &sa, nullptr);
    sigaction(SIGFPE, &sa, nullptr);
    __android_log_write(ANDROID_LOG_INFO, kCrashTag, "Native crash handler installed");
}

}  // namespace lai
