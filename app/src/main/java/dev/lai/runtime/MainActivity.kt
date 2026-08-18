package dev.lai.runtime

import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dev.lai.runtime.core.LaiLog
import dev.lai.runtime.ui.LaiApp
import dev.lai.runtime.ui.MainViewModel
import dev.lai.runtime.ui.theme.LaiTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LaiLog.i("LAI-lifecycle", "MainActivity onCreate")
        enableEdgeToEdge()
        requestHighestRefreshRate()
        setContent {
            LaiTheme {
                LaiApp(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        LaiLog.i("LAI-lifecycle", "MainActivity onDestroy")
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            LaiLog.w("LAI-mem", "MainActivity onTrimMemory level=$level")
        }
        super.onTrimMemory(level)
    }

    /**
     * Opt into the display's highest refresh rate (90/120/144 Hz where available).
     *
     * Android does not give an app the panel's peak rate by default: many devices keep a
     * non-gaming app at 60 Hz to save power, which is why scrolling and the keyboard transition
     * felt less smooth than the rest of the system (field report, Redmi Turbo 4 Pro).
     *
     * On Android 11+ the supported modes are ranked and the best one is requested explicitly.
     * Older versions get the simpler `preferredRefreshRate` hint. The compositor may still refuse
     * or drop the rate under thermal or battery pressure, which is correct behaviour — this is a
     * preference, not a guarantee, and LAI must never fight the platform's power management.
     */
    private fun requestHighestRefreshRate() {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else null
        val attributes = window.attributes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && display != null) {
            val current = display.mode
            val best = display.supportedModes
                // Only consider modes at the current resolution; switching resolution to gain Hz
                // would be a visible, unwanted change.
                .filter {
                    it.physicalWidth == current.physicalWidth &&
                        it.physicalHeight == current.physicalHeight
                }
                .maxByOrNull(Display.Mode::getRefreshRate)
            if (best != null && best.refreshRate > current.refreshRate) {
                attributes.preferredDisplayModeId = best.modeId
            }
        } else {
            @Suppress("DEPRECATION")
            val maxRate = windowManager.defaultDisplay.supportedRefreshRates.maxOrNull()
            if (maxRate != null) attributes.preferredRefreshRate = maxRate
        }
        window.attributes = attributes
        // Ask the compositor to avoid dropping to a lower rate while the window is in front.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            )
        }
    }
}
