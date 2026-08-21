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
        // Native crash diagnostics are optional: logging may not have a file yet during startup.
        LaiLog.logFilePath()?.let { path ->
            container.inferenceEngine.installNativeCrashHandler(path)
        }
        container.inferenceEngine.configureOpenCLVendors(filesDir.absolutePath)
        LaiLog.i("LAI-lifecycle", "OpenCL vendor discovery configured under ${filesDir.name}")
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
