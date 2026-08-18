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
