package dev.lai.runtime.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import dev.lai.runtime.scheduler.RuntimeEnvironment
import dev.lai.runtime.scheduler.ThermalState

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

    private fun thermalState(): ThermalState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ThermalState.UNKNOWN
        return when (appContext.getSystemService(PowerManager::class.java)?.currentThermalStatus) {
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
}
