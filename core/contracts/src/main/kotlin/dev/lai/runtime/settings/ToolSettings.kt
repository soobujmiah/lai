package dev.lai.runtime.settings

import kotlinx.serialization.Serializable

/**
 * Versioned, typed, non-secret product settings.
 *
 * Phase 2A foundation (see `PROJECT_STATE.md` §4.1). This is the dependency for the
 * contextual Chat ⚙ quick-sheet, the Model Center, the Dashboard/Chat tool parity,
 * and the future SAF workspace store.
 *
 * Privacy invariant: every field is a bounded numeric or boolean value. The schema
 * intentionally has **no free-text field**, so a settings document can never absorb a
 * prompt, conversation, document, model output, selector, package name, or credential.
 * Validation, defaults, and migration live in
 * [dev.lai.runtime.settings.SettingsPolicy] (pure `core:policy`).
 *
 * Defaults preserve the current reviewed Qwen 2.5 1.5B product state
 * (temperature `0.7`, top-P `0.9`, maximum new tokens `256`, random seed sentinel).
 */
@Serializable
data class SettingsDocumentV1(
    val schemaVersion: Int = SCHEMA_VERSION,
    val llm: LlmSettings = LlmSettings(),
    val imageGeneration: ImageGenerationSettings = ImageGenerationSettings(),
    val voice: VoiceSettings = VoiceSettings(),
    val search: SearchSettings = SearchSettings(),
) {
    companion object {
        /** Current and only supported wire schema version. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Local LLM generation parameters. Mirrors [dev.lai.runtime.inference.GenerationConfig]
 * plus a small context policy. `seed = -1` is the "let the runtime choose" sentinel;
 * any other value must be non-negative for deterministic sampling.
 */
@Serializable
data class LlmSettings(
    val temperature: Float = DEFAULT_TEMPERATURE,
    val topP: Float = DEFAULT_TOP_P,
    val maxNewTokens: Int = DEFAULT_MAX_NEW_TOKENS,
    val seed: Long = DEFAULT_SEED,
    val context: ContextPolicy = ContextPolicy(),
) {
    companion object {
        const val DEFAULT_TEMPERATURE = 0.7f
        const val DEFAULT_TOP_P = 0.9f
        const val DEFAULT_MAX_NEW_TOKENS = 256

        /** Sentinel meaning "do not pin a seed"; the native runtime selects one. */
        const val DEFAULT_SEED = -1L
    }
}

/**
 * Multi-turn context handling. `keepLastTurns` bounds how many completed turns are
 * retained before the rolling-window/summary work lands; `maxContextTokens` is the
 * hard native context budget and also the context-dependent ceiling for
 * [LlmSettings.maxNewTokens].
 */
@Serializable
data class ContextPolicy(
    val keepLastTurns: Int = DEFAULT_KEEP_LAST_TURNS,
    val maxContextTokens: Int = DEFAULT_MAX_CONTEXT_TOKENS,
) {
    companion object {
        const val DEFAULT_KEEP_LAST_TURNS = 8
        const val DEFAULT_MAX_CONTEXT_TOKENS = 4096
    }
}

/**
 * Image-generation parameter ranges. These are **inert** until a real compatible
 * on-device adapter capability exists; no `enabled` toggle is persisted because
 * availability is a runtime capability fact, not a user setting.
 */
@Serializable
data class ImageGenerationSettings(
    val steps: Int = DEFAULT_STEPS,
    val guidanceScale: Float = DEFAULT_GUIDANCE_SCALE,
    val width: Int = DEFAULT_WIDTH,
    val height: Int = DEFAULT_HEIGHT,
) {
    companion object {
        const val DEFAULT_STEPS = 20
        const val DEFAULT_GUIDANCE_SCALE = 7.5f
        const val DEFAULT_WIDTH = 512
        const val DEFAULT_HEIGHT = 512
    }
}

/**
 * Voice (STT/TTS) parameter ranges. Inert until a real on-device speech adapter exists.
 */
@Serializable
data class VoiceSettings(
    val speechRate: Float = DEFAULT_SPEECH_RATE,
    val bargeIn: Boolean = DEFAULT_BARGE_IN,
) {
    companion object {
        const val DEFAULT_SPEECH_RATE = 1.0f
        const val DEFAULT_BARGE_IN = false
    }
}

/**
 * Local vector-search parameter ranges. Inert until the SQLCipher vector DB / RAG
 * adapter exists.
 */
@Serializable
data class SearchSettings(
    val maxResults: Int = DEFAULT_MAX_RESULTS,
    val minScore: Float = DEFAULT_MIN_SCORE,
) {
    companion object {
        const val DEFAULT_MAX_RESULTS = 5
        const val DEFAULT_MIN_SCORE = 0.3f
    }
}
