package dev.lai.runtime.settings

import dev.lai.runtime.core.LaiJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull

enum class SettingsSeverity { WARNING, ERROR }

/** A single validation finding rooted at a JSON path (for example `llm.temperature`). */
data class SettingsIssue(
    val path: String,
    val severity: SettingsSeverity,
    val code: String,
    val message: String,
)

/** Aggregate result of validating a settings document or raw JSON object. */
class SettingsValidation(val issues: List<SettingsIssue>) {
    val errors: List<SettingsIssue> get() = issues.filter { it.severity == SettingsSeverity.ERROR }
    val warnings: List<SettingsIssue> get() = issues.filter { it.severity == SettingsSeverity.WARNING }

    /** `true` when no hard (ERROR) issue was found; warnings do not invalidate the document. */
    val isValid: Boolean get() = errors.isEmpty()

    override fun toString(): String =
        if (isValid) "Settings valid (${warnings.size} warning(s))"
        else "Settings invalid: ${errors.size} error(s), ${warnings.size} warning(s)"
}

/** Outcome of loading/migrating a raw settings document. */
data class SettingsMigration(
    val document: SettingsDocumentV1,
    val warnings: List<String>,
    /** `true` when the input was unsupported/malformed and safe defaults were substituted. */
    val fellBackToDefaults: Boolean,
)

/**
 * Pure validation, default-merging and migration for [SettingsDocumentV1].
 *
 * Design rules (PROJECT_STATE.md §4.1 / §4.2):
 *
 * - Unknown fields are **forward-compatible warnings**, never silently dropped and never fatal,
 *   so a newer app can still read an older document and vice-versa.
 * - Range, finite-number, enum and context-dependent checks are **errors**.
 * - `maxNewTokens` is validated against `maxContextTokens` (a generation cannot exceed the
 *   context window) in addition to its absolute range — the context-dependent max-token limit.
 * - Any hard failure (unknown schema version, wrong type, out of range, malformed JSON) falls
 *   back to embedded safe defaults rather than crashing or persisting an unsafe document.
 * - The schema has no text fields, so it cannot store prompts, documents, or secrets.
 *
 * Only schema version 1 exists today; the [migrate] seam is deterministic so future versions
 * plug in without touching callers.
 */
class SettingsPolicy {

    /** Embedded safe defaults; also the reset baseline. */
    fun defaults(): SettingsDocumentV1 = SettingsDocumentV1()

    /** Range/finite/context checks over an already-typed document (e.g. after a code-side build). */
    fun validate(document: SettingsDocumentV1): SettingsValidation {
        val issues = mutableListOf<SettingsIssue>()
        checkLlm(document.llm, "llm", issues)
        checkImage(document.imageGeneration, "imageGeneration", issues)
        checkVoice(document.voice, "voice", issues)
        checkSearch(document.search, "search", issues)
        return SettingsValidation(issues)
    }

    /** Structural, unknown-field, schema-version and range checks over raw JSON. */
    fun validate(raw: JsonObject): SettingsValidation {
        val issues = mutableListOf<SettingsIssue>()
        reportUnknown(raw.keys, KNOWN_TOP_LEVEL, "$", issues)
        checkSchemaVersion(raw, issues)
        reportSectionUnknowns(raw, issues)
        // A bad or unsupported schema version means the document cannot be trusted as v1;
        // do not attempt to decode/range-check it (that would only duplicate the finding).
        if (issues.any { it.code == "schema_version_type" || it.code == "unsupported_schema_version" }) {
            return SettingsValidation(issues)
        }

        val decoded = runCatching {
            LaiJson.decodeFromJsonElement(SettingsDocumentV1.serializer(), raw)
        }.getOrElse {
            issues += error("$", "malformed_schema", "Settings are not valid typed JSON: ${reason(it)}")
            return SettingsValidation(issues)
        }
        // Re-run range/finite/context checks on the decoded values.
        issues += validate(decoded).issues
        return SettingsValidation(issues)
    }

    /**
     * Returns a valid document, substituting safe defaults for any unsupported/malformed input.
     * Missing fields are always filled from defaults; explicit valid values are preserved.
     */
    fun sanitize(raw: JsonObject): SettingsDocumentV1 {
        if (!validate(raw).isValid) return defaults()
        return runCatching {
            LaiJson.decodeFromJsonElement(SettingsDocumentV1.serializer(), raw)
        }.getOrDefault(defaults())
    }

    /**
     * Deterministic version-aware load. Today only schema version 1 is supported; an absent
     * version is treated as 1. Older/unknown or future versions fall back to defaults with a
     * clear warning rather than guessing a transform.
     */
    fun migrate(raw: JsonObject): SettingsMigration {
        val warnings = mutableListOf<String>()
        val explicitVersion = raw["schemaVersion"]
        val version = (explicitVersion as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull

        val supported = when {
            explicitVersion == null -> true
            version == null -> {
                warnings += "schemaVersion is not an integer; using safe defaults."
                false
            }
            version == SettingsDocumentV1.SCHEMA_VERSION -> true
            else -> {
                warnings += "Unsupported schemaVersion $version; only " +
                    "${SettingsDocumentV1.SCHEMA_VERSION} is supported. Using safe defaults."
                false
            }
        }
        if (!supported) return SettingsMigration(defaults(), warnings, fellBackToDefaults = true)

        val validation = validate(raw)
        validation.warnings.forEach { warnings += "${it.path}: ${it.message}" }
        if (!validation.isValid) {
            validation.errors.forEach { warnings += "${it.path}: ${it.message}" }
            return SettingsMigration(defaults(), warnings, fellBackToDefaults = true)
        }
        val document = runCatching {
            LaiJson.decodeFromJsonElement(SettingsDocumentV1.serializer(), raw)
        }.getOrElse {
            warnings += "Document could not be decoded; using safe defaults."
            return SettingsMigration(defaults(), warnings, fellBackToDefaults = true)
        }
        return SettingsMigration(document, warnings, fellBackToDefaults = false)
    }

    private fun checkLlm(s: LlmSettings, path: String, issues: MutableList<SettingsIssue>) {
        if (checkFinite(s.temperature, "$path.temperature", issues) && s.temperature !in TEMPERATURE_RANGE) {
            issues += error("$path.temperature", "temperature_range", "temperature must be in [0.0, 2.0]")
        }
        if (checkFinite(s.topP, "$path.topP", issues) && s.topP !in TOP_P_RANGE) {
            issues += error("$path.topP", "top_p_range", "topP must be in [0.0, 1.0]")
        }
        if (s.maxNewTokens !in MAX_NEW_TOKENS_RANGE) {
            issues += error("$path.maxNewTokens", "max_new_tokens_range", "maxNewTokens must be in [1, 4096]")
        }
        if (s.seed != RANDOM_SEED && s.seed !in SEED_RANGE) {
            issues += error("$path.seed", "seed_range", "seed must be -1 (random) or a non-negative integer")
        }
        val contextPath = "$path.context"
        if (s.context.maxContextTokens !in MAX_CONTEXT_RANGE) {
            issues += error("$contextPath.maxContextTokens", "max_context_range", "maxContextTokens must be in [512, 32768]")
        }
        if (s.context.keepLastTurns !in KEEP_LAST_TURNS_RANGE) {
            issues += error("$contextPath.keepLastTurns", "keep_last_turns_range", "keepLastTurns must be in [1, 64]")
        }
        // Context-dependent ceiling: a generation budget cannot exceed the context window.
        if (s.maxNewTokens in MAX_NEW_TOKENS_RANGE &&
            s.context.maxContextTokens in MAX_CONTEXT_RANGE &&
            s.maxNewTokens > s.context.maxContextTokens
        ) {
            issues += error(
                "$path.maxNewTokens",
                "max_new_tokens_exceeds_context",
                "maxNewTokens cannot exceed maxContextTokens (${s.context.maxContextTokens})",
            )
        }
    }

    private fun checkImage(s: ImageGenerationSettings, path: String, issues: MutableList<SettingsIssue>) {
        if (s.steps !in IMAGE_STEPS_RANGE) {
            issues += error("$path.steps", "image_steps_range", "steps must be in [1, 100]")
        }
        if (checkFinite(s.guidanceScale, "$path.guidanceScale", issues) && s.guidanceScale !in GUIDANCE_RANGE) {
            issues += error("$path.guidanceScale", "guidance_range", "guidanceScale must be in [0.0, 30.0]")
        }
        checkImageDimension(s.width, "$path.width", issues)
        checkImageDimension(s.height, "$path.height", issues)
    }

    private fun checkImageDimension(value: Int, path: String, issues: MutableList<SettingsIssue>) {
        if (value !in IMAGE_DIMENSION_RANGE || value % IMAGE_DIMENSION_STEP != 0) {
            issues += error(path, "image_dimension", "$path must be in [64, 1024] and a multiple of 64")
        }
    }

    private fun checkVoice(s: VoiceSettings, path: String, issues: MutableList<SettingsIssue>) {
        if (checkFinite(s.speechRate, "$path.speechRate", issues) && s.speechRate !in SPEECH_RATE_RANGE) {
            issues += error("$path.speechRate", "speech_rate_range", "speechRate must be in [0.5, 2.0]")
        }
    }

    private fun checkSearch(s: SearchSettings, path: String, issues: MutableList<SettingsIssue>) {
        if (s.maxResults !in MAX_RESULTS_RANGE) {
            issues += error("$path.maxResults", "max_results_range", "maxResults must be in [1, 50]")
        }
        if (checkFinite(s.minScore, "$path.minScore", issues) && s.minScore !in MIN_SCORE_RANGE) {
            issues += error("$path.minScore", "min_score_range", "minScore must be in [0.0, 1.0]")
        }
    }

    private fun checkFinite(value: Float, path: String, issues: MutableList<SettingsIssue>): Boolean {
        if (!value.isFinite()) {
            issues += error(path, "non_finite_number", "$path must be a finite number")
            return false
        }
        return true
    }

    private fun reportUnknown(actual: Set<String>, known: Set<String>, path: String, issues: MutableList<SettingsIssue>) {
        actual.filter { it !in known }.sorted().forEach { key ->
            issues += SettingsIssue("$path.$key", SettingsSeverity.WARNING, "unknown_field", "Unknown field '$key' will be ignored")
        }
    }

    private fun reportSectionUnknowns(raw: JsonObject, issues: MutableList<SettingsIssue>) {
        SECTION_KEYS.forEach { (section, known) ->
            val fields = raw[section] as? JsonObject ?: return@forEach
            reportUnknown(fields.keys, known, section, issues)
        }
    }

    private fun checkSchemaVersion(raw: JsonObject, issues: MutableList<SettingsIssue>) {
        val element = raw["schemaVersion"] ?: return
        val primitive = element as? JsonPrimitive
        val value = primitive?.takeIf { !it.isString }?.intOrNull
        when {
            primitive == null || primitive.isString ->
                issues += error("$", "schema_version_type", "schemaVersion must be an integer")
            value != SettingsDocumentV1.SCHEMA_VERSION ->
                issues += error("$", "unsupported_schema_version", "Only schemaVersion ${SettingsDocumentV1.SCHEMA_VERSION} is supported (got $value)")
        }
    }

    private fun error(path: String, code: String, message: String) =
        SettingsIssue(path, SettingsSeverity.ERROR, code, message)

    private fun reason(throwable: Throwable): String =
        throwable.message?.take(200) ?: throwable::class.simpleName ?: "decode error"

    companion object {
        private const val RANDOM_SEED = -1L
        private val TEMPERATURE_RANGE = 0.0f..2.0f
        private val TOP_P_RANGE = 0.0f..1.0f
        private val MAX_NEW_TOKENS_RANGE = 1..4096
        private val SEED_RANGE = 0L..Long.MAX_VALUE
        private val MAX_CONTEXT_RANGE = 512..32768
        private val KEEP_LAST_TURNS_RANGE = 1..64
        private val IMAGE_STEPS_RANGE = 1..100
        private val GUIDANCE_RANGE = 0.0f..30.0f
        private val IMAGE_DIMENSION_RANGE = 64..1024
        private const val IMAGE_DIMENSION_STEP = 64
        private val SPEECH_RATE_RANGE = 0.5f..2.0f
        private val MAX_RESULTS_RANGE = 1..50
        private val MIN_SCORE_RANGE = 0.0f..1.0f

        private val KNOWN_TOP_LEVEL = setOf("schemaVersion", "llm", "imageGeneration", "voice", "search")
        private val SECTION_KEYS: Map<String, Set<String>> = mapOf(
            "llm" to setOf("temperature", "topP", "maxNewTokens", "seed", "context"),
            "imageGeneration" to setOf("steps", "guidanceScale", "width", "height"),
            "voice" to setOf("speechRate", "bargeIn"),
            "search" to setOf("maxResults", "minScore"),
        )
    }
}
