package dev.lai.runtime

import android.app.Application
import android.content.ComponentCallbacks2
import dev.lai.runtime.core.AppContainer
import dev.lai.runtime.core.LaiLog

class LaiApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Centralized diagnostics are configured before anything else can log: debug builds log
        // at DEBUG, signed release builds at INFO (see docs/LOGGING.md). A crash handler records
        // fatal stack traces into the diagnostic log file for later extraction.
        LaiLog.configure(this, debugBuild = BuildConfig.DEBUG)
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
            Thread.UncaughtExceptionHandler { thread, throwable ->
                LaiLog.e("LAI-crash", "Uncaught exception on ${thread.name}", throwable)
                previousHandler?.uncaughtException(thread, throwable)
            },
        )
        LaiLog.i("LAI-lifecycle", "LaiApplication onCreate (process start)")
        container = AppContainer(this)
        // Native (GPU/driver) crashes are uncatchable by Kotlin; install the native signal
        // handler so any such crash writes a backtrace into the diagnostic log file.
        container.inferenceEngine.installNativeCrashHandler(LaiLog.logFilePath())
        // Adreno OpenCL track: point the statically linked Khronos ICD loader at a vendor
        // directory that resolves the Adreno OpenCL driver on Qualcomm devices (the loader's
        // default Android path is usually empty). Must run before any backend probe — see
        // docs/BUILD_AND_RELEASE.md "GPU enablement — Adreno OpenCL track".
        container.inferenceEngine.configureOpenCLVendors(filesDir.absolutePath)
        LaiLog.i("LAI-lifecycle", "OpenCL vendor discovery configured under ${filesDir.name}")
        // Hexagon DSP-side skel discovery (docs/device-results/
        // 2026-09-03-redmi-turbo-4-pro-hexagon-session-open-diagnosis.md): must also run before
        // any backend probe, same requirement as the OpenCL vendor setup above.
        container.inferenceEngine.configureHexagonAdspPath(applicationInfo.nativeLibraryDir)
        LaiLog.i("LAI-lifecycle", "Hexagon ADSP_LIBRARY_PATH configured under ${applicationInfo.nativeLibraryDir}")
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            LaiLog.w("LAI-mem", "onTrimMemory level=$level")
        } else {
            LaiLog.d("LAI-mem", "onTrimMemory level=$level")
        }
        container.onTrimMemory(level)
        super.onTrimMemory(level)
    }

    @Suppress("DEPRECATION")
    override fun onLowMemory() {
        LaiLog.w("LAI-mem", "onLowMemory")
        container.onLowMemory()
        super.onLowMemory()
    }
}
