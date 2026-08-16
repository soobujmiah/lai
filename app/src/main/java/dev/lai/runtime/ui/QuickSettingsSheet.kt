package dev.lai.runtime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lai.runtime.R
import dev.lai.runtime.settings.LlmSettings
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Contextual ⚙ quick-settings sheet for Chat (Phase 2A item 6).
 *
 * Product rules encoded here:
 *
 * - **LLM controls only.** Image, voice and search ranges exist in the typed schema but have no
 *   real on-device adapter yet, so rendering them would promise a capability LAI cannot deliver.
 * - **Plain language, no jargon.** Each control says what it does for the user; the numeric value
 *   is shown but never a token/tensor explanation.
 * - **Apply once vs Save default are visibly different.** "Apply once" affects only the next reply
 *   and never rewrites the stored file; only "Save default" persists.
 * - **Bounded by construction.** Slider ranges mirror the validated ranges in `SettingsPolicy`, and
 *   the reply-length ceiling comes from the live runtime context, so the sheet cannot compose a
 *   document the store would reject.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSettingsSheet(
    current: LlmSettings,
    maxNewTokensCeiling: Int,
    statusLine: String,
    overrideArmed: Boolean,
    saving: Boolean,
    onApplyOnce: (LlmSettings) -> Unit,
    onSaveDefault: (LlmSettings) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Local editing state seeded from the effective settings; committed only by an explicit button.
    var temperature by remember(current) { mutableFloatStateOf(current.temperature) }
    var topP by remember(current) { mutableFloatStateOf(current.topP) }
    var maxNewTokens by remember(current, maxNewTokensCeiling) {
        mutableIntStateOf(current.maxNewTokens.coerceIn(MIN_NEW_TOKENS, maxNewTokensCeiling))
    }
    var keepLastTurns by remember(current) { mutableIntStateOf(current.context.keepLastTurns) }

    fun edited(): LlmSettings = current.copy(
        temperature = temperature,
        topP = topP,
        maxNewTokens = maxNewTokens.coerceIn(MIN_NEW_TOKENS, maxNewTokensCeiling),
        context = current.context.copy(keepLastTurns = keepLastTurns),
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.quick_settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                statusLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (overrideArmed) {
                Text(
                    stringResource(R.string.quick_settings_override_active),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            SettingSlider(
                label = stringResource(R.string.setting_creativity),
                help = stringResource(R.string.setting_creativity_help),
                valueLabel = decimal(temperature),
                value = temperature,
                range = TEMPERATURE_RANGE,
                steps = TEMPERATURE_STEPS,
                onValueChange = { temperature = round2(it) },
            )
            SettingSlider(
                label = stringResource(R.string.setting_focus),
                help = stringResource(R.string.setting_focus_help),
                valueLabel = decimal(topP),
                value = topP,
                range = TOP_P_RANGE,
                steps = TOP_P_STEPS,
                onValueChange = { topP = round2(it) },
            )
            SettingSlider(
                label = stringResource(R.string.setting_reply_length),
                help = stringResource(R.string.setting_reply_length_help),
                valueLabel = "$maxNewTokens",
                value = maxNewTokens.toFloat(),
                range = MIN_NEW_TOKENS.toFloat()..maxNewTokensCeiling.toFloat(),
                steps = 0,
                onValueChange = { maxNewTokens = it.roundToInt().coerceIn(MIN_NEW_TOKENS, maxNewTokensCeiling) },
            )
            SettingSlider(
                label = stringResource(R.string.setting_memory),
                help = stringResource(R.string.setting_memory_help),
                valueLabel = "$keepLastTurns",
                value = keepLastTurns.toFloat(),
                range = KEEP_TURNS_RANGE,
                steps = KEEP_TURNS_STEPS,
                onValueChange = { keepLastTurns = it.roundToInt().coerceIn(MIN_KEEP_TURNS, MAX_KEEP_TURNS) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { onApplyOnce(edited()) },
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.apply_once)) }
                OutlinedButton(
                    onClick = { onSaveDefault(edited()) },
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                ) { Text(if (saving) stringResource(R.string.saving) else stringResource(R.string.save_default)) }
            }
            TextButton(onClick = onReset, enabled = !saving) { Text(stringResource(R.string.reset_defaults)) }
            Text(
                stringResource(R.string.quick_settings_privacy_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    help: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.labelLarge)
        }
        Text(
            help,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
        )
    }
}

private fun round2(value: Float): Float = Math.round(value * 100f) / 100f

private fun decimal(value: Float): String = String.format(Locale.US, "%.2f", value)

// Mirrors of the validated ranges in SettingsPolicy; the sheet cannot produce a rejected document.
private val TEMPERATURE_RANGE = 0.0f..2.0f
private const val TEMPERATURE_STEPS = 19
private val TOP_P_RANGE = 0.0f..1.0f
private const val TOP_P_STEPS = 19
private const val MIN_NEW_TOKENS = 32
private const val MIN_KEEP_TURNS = 1
private const val MAX_KEEP_TURNS = 32
private val KEEP_TURNS_RANGE = MIN_KEEP_TURNS.toFloat()..MAX_KEEP_TURNS.toFloat()
private const val KEEP_TURNS_STEPS = MAX_KEEP_TURNS - MIN_KEEP_TURNS - 1
