package dev.lai.runtime.shell

import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

class ShizukuController {
    private val _state = MutableStateFlow<ShizukuState>(ShizukuState.Unavailable)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }
    private val binderDead = Shizuku.OnBinderDeadListener { refresh() }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
        refresh()
    }

    fun refresh() {
        _state.value = runCatching {
            if (!Shizuku.pingBinder()) {
                ShizukuState.Unavailable
            } else if (Shizuku.isPreV11()) {
                ShizukuState.Error("This Shizuku version is too old")
            } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                ShizukuState.PermissionRequired
            } else {
                ShizukuState.Ready(Shizuku.getUid())
            }
        }.getOrElse { ShizukuState.Error(it.message ?: "Shizuku error") }
    }

    fun requestPermission(requestCode: Int = REQUEST_CODE) {
        runCatching {
            if (Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED &&
                !Shizuku.shouldShowRequestPermissionRationale()
            ) {
                Shizuku.requestPermission(requestCode)
            }
        }.onFailure { _state.value = ShizukuState.Error(it.message ?: "Permission request failed") }
    }

    fun close() {
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        Shizuku.removeRequestPermissionResultListener(permissionResult)
    }

    companion object {
        const val REQUEST_CODE = 4107
    }
}
