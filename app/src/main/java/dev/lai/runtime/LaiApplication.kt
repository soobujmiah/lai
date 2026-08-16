package dev.lai.runtime

import android.app.Application
import dev.lai.runtime.core.AppContainer

class LaiApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun onTrimMemory(level: Int) {
        container.onTrimMemory(level)
        super.onTrimMemory(level)
    }

    @Suppress("DEPRECATION")
    override fun onLowMemory() {
        container.onLowMemory()
        super.onLowMemory()
    }
}
