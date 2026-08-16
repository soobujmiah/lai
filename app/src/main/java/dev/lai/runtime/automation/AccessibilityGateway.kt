package dev.lai.runtime.automation

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

object AccessibilityGateway {
    private var serviceRef = WeakReference<AccessibilityAutomationService>(null)
    private val _connected = MutableStateFlow(false)
    private val _foregroundPackage = MutableStateFlow<String?>(null)

    val connected: StateFlow<Boolean> = _connected.asStateFlow()
    val foregroundPackage: StateFlow<String?> = _foregroundPackage.asStateFlow()

    internal fun attach(service: AccessibilityAutomationService) {
        serviceRef = WeakReference(service)
        _connected.value = true
    }

    internal fun detach(service: AccessibilityAutomationService) {
        if (serviceRef.get() === service) serviceRef.clear()
        _connected.value = false
        _foregroundPackage.value = null
    }

    internal fun updateForegroundPackage(packageName: String?) {
        _foregroundPackage.value = packageName
    }

    suspend fun execute(command: AutomationCommand): AutomationResult = withContext(Dispatchers.Main.immediate) {
        serviceRef.get()?.execute(command)
            ?: AutomationResult(false, "LAI accessibility service is not enabled")
    }

    suspend fun captureScreen(): Result<Bitmap> = withContext(Dispatchers.Main.immediate) {
        serviceRef.get()?.captureScreen()
            ?: Result.failure(IllegalStateException("LAI accessibility service is not enabled"))
    }
}
