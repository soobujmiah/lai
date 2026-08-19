#pragma once

namespace lai {

/**
 * Installs signal handlers (SIGSEGV/SIGABRT/SIGBUS/SIGILL/SIGFPE) that dump a backtrace of the
 * crashing thread to logcat (LAI-crash) and append it to the app diagnostic log file, then
 * re-raise with the default handler so the OS still reports the crash.
 *
 * This exists because GPU/driver crashes are NATIVE (uncatchable by Kotlin): with the handler,
 * a crash during Vulkan compute writes the offending native frames into the same log file the
 * user exports from Settings — no adb needed.
 *
 * Must be called once after the diagnostic log file path is known. Async-signal-safe: uses only
 * open/write/__android_log_write/dladdr/_Unwind_Backtrace.
 */
void install_native_crash_handler(const char* log_file_path);

}  // namespace lai
