package dev.lai.runtime.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import dev.lai.runtime.scheduler.BackendCapability
import dev.lai.runtime.scheduler.DeviceProfile
import dev.lai.runtime.scheduler.RuntimeEnvironment
import dev.lai.runtime.scheduler.ThermalState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Android observations only; runtime adapters remain responsible for truthful accelerator probes. */
class AndroidRuntimeEnvironmentProvider(context: Context) {
    private val appContext = context.applicationContext

    fun snapshot(): RuntimeEnvironment {
        val memoryInfo = ActivityManager.MemoryInfo()
        appContext.getSystemService(ActivityManager::class.java)?.getMemoryInfo(memoryInfo)
        val batteryIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else null
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status?.let {
            it == BatteryManager.BATTERY_STATUS_CHARGING || it == BatteryManager.BATTERY_STATUS_FULL
        }
        return RuntimeEnvironment(
            availableMemoryBytes = memoryInfo.availMem.takeIf { it > 0 },
            batteryPercent = batteryPercent,
            charging = charging,
            thermalState = thermalState(),
        )
    }

    fun profile(backends: List<BackendCapability>): DeviceProfile = DeviceProfile(
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER else null,
        socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else null,
        androidSdk = Build.VERSION.SDK_INT,
        supportedAbis = Build.SUPPORTED_ABIS.toList(),
        cpuCoreCount = Runtime.getRuntime().availableProcessors().takeIf { it > 0 },
        environment = snapshot(),
        backends = backends,
    )

    private fun thermalState(): ThermalState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ThermalState.UNKNOWN
        return statusToState(appContext.getSystemService(PowerManager::class.java)?.currentThermalStatus)
    }

    /**
     * Live thermal status stream for the closed-loop governor. Emits the current state
     * immediately, then every change. Below Android Q there is no callback API: a single
     * UNKNOWN is emitted and the governor simply never intervenes.
     */
    fun thermalStates(): Flow<ThermalState> = callbackFlow {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            trySend(ThermalState.UNKNOWN)
            awaitClose {}
            return@callbackFlow
        }
        val powerManager = appContext.getSystemService(PowerManager::class.java)
        if (powerManager == null) {
            trySend(ThermalState.UNKNOWN)
            awaitClose {}
            return@callbackFlow
        }
        trySend(statusToState(powerManager.currentThermalStatus))
        val listener = PowerManager.OnThermalStatusChangedListener { status ->
            trySend(statusToState(status))
        }
        powerManager.addThermalStatusListener(listener)
        awaitClose { powerManager.removeThermalStatusListener(listener) }
    }.distinctUntilChanged()

    private fun statusToState(status: Int?): ThermalState = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> ThermalState.NOMINAL
        PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.LIGHT
        PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.MODERATE
        PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.SEVERE
        PowerManager.THERMAL_STATUS_CRITICAL,
        PowerManager.THERMAL_STATUS_EMERGENCY,
        PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState.CRITICAL
        else -> ThermalState.UNKNOWN
    }
}
