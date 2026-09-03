package dev.lai.runtime

import android.content.Intent
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
        handleQualificationIntent(intent)
    }

    /**
     * ADB-first accelerator qualification trigger (docs/TESTING.md "Backend qualification").
     * MainActivity is already the app's only exported, always-present component, so it doubles
     * as the app-native control surface instead of adding a new one: a device-testing agent
     * qualifies a backend with one command and no UI navigation —
     *
     *   adb shell am start -n dev.lai.runtime/.MainActivity \
     *     --es qualify_backend llama-hexagon --es qualify_model qwen2.5-1.5b-instruct-q4-0
     *
     * [MainViewModel.runBackendQualification] independently re-verifies the backend is in this
     * build's BuildConfig.VALIDATED_ACCELERATORS before doing anything, so this is inert on
     * every ordinary signed release (which ships with that set empty) — it never changes
     * production backend-selection behavior.
     */
    private fun handleQualificationIntent(intent: Intent) {
        val backend = intent.getStringExtra(EXTRA_QUALIFY_BACKEND) ?: return
        if (intent.getBooleanExtra(EXTRA_QUALIFY_PROBE_ONLY, false)) {
            // Cheap, model-free capabilities probe (docs/TESTING.md "Backend qualification" —
            // diagnostic probe mode) — isolates whether backend *enumeration* hangs before
            // committing to a full, multi-minute qualification attempt. No BuildConfig gating
            // needed here: reading capabilities never forces a load.
            LaiLog.i("LAI-diag", "Probe intent received: backend=$backend")
            viewModel.runBackendProbe(backend)
            return
        }
        val modelId = intent.getStringExtra(EXTRA_QUALIFY_MODEL) ?: return
        val prompt = intent.getStringExtra(EXTRA_QUALIFY_PROMPT) ?: DEFAULT_QUALIFY_PROMPT
        val timeoutMs = intent.getLongExtra(EXTRA_QUALIFY_TIMEOUT_MS, MainViewModel.DEFAULT_QUALIFY_LOAD_TIMEOUT_MS)
        LaiLog.i("LAI-qualify", "Qualification intent received: model=$modelId backend=$backend")
        viewModel.runBackendQualification(modelId, backend, prompt, timeoutMs)
    }

    private companion object {
        const val EXTRA_QUALIFY_BACKEND = "qualify_backend"
        const val EXTRA_QUALIFY_MODEL = "qualify_model"
        const val EXTRA_QUALIFY_PROMPT = "qualify_prompt"
        const val EXTRA_QUALIFY_PROBE_ONLY = "qualify_probe"
        const val EXTRA_QUALIFY_TIMEOUT_MS = "qualify_timeout_ms"
        const val DEFAULT_QUALIFY_PROMPT = "Say hello in one short sentence."
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
