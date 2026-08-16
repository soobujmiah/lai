package dev.lai.runtime.settings

/**
 * Pure session semantics for typed settings (Phase 2A items 6–7).
 *
 * The product has two distinct notions of "current settings" and confusing them would be a
 * privacy/UX bug, so they are modelled explicitly here rather than inside the ViewModel:
 *
 * - **Saved defaults** — the document persisted in the granted workspace (or embedded defaults
 *   when nothing is granted). Only an explicit "Save default" changes them.
 * - **One-request override** — what the Chat ⚙ quick sheet's "Apply once" sets. It affects the
 *   next generation only and is consumed by it; it must never silently mutate saved defaults.
 *
 * Everything in this file is pure: no Android, no I/O, no coroutines. That keeps the rules
 * unit-testable on the JVM and identical for the Chat sheet, the future Tools Dashboard, and
 * any other surface that eventually edits the same contracts.
 */

/** Immutable settings session: persisted defaults plus an optional single-request LLM override. */
data class SettingsSession(
    val saved: SettingsDocumentV1 = SettingsDocumentV1(),
    val pendingLlmOverride: LlmSettings? = null,
    val loadedFromFile: Boolean = false,
    val fellBackToDefaults: Boolean = false,
    val warnings: List<String> = emptyList(),
) {
    /** LLM parameters the next generation must use. */
    val effectiveLlm: LlmSettings get() = pendingLlmOverride ?: saved.llm

    /** `true` while a quick-sheet override is armed for exactly one request. */
    val hasPendingOverride: Boolean get() = pendingLlmOverride != null
}

/** Outcome of a session transition. A [Rejected] transition leaves the session untouched. */
sealed interface SettingsSessionResult {
    val session: SettingsSession

    data class Applied(override val session: SettingsSession, val message: String) : SettingsSessionResult

    data class Rejected(
        override val session: SettingsSession,
        val issues: List<SettingsIssue>,
    ) : SettingsSessionResult {
        /** Short, user-facing reason; never echoes stored values. */
        val message: String
            get() = issues.firstOrNull()?.let { "${it.path}: ${it.message}" } ?: "Settings rejected"
    }
}

/** LLM parameters resolved for one request, plus the session with the override consumed. */
data class ResolvedRequestSettings(
    val llm: LlmSettings,
    val session: SettingsSession,
    val usedOverride: Boolean,
)

/**
 * Applies [SettingsPolicy] to session transitions. Every transition validates first: an invalid
 * candidate is rejected with typed issues instead of being sanitized behind the user's back, so
 * the UI can explain exactly which control is out of range.
 */
class SettingsSessionPolicy(private val policy: SettingsPolicy = SettingsPolicy()) {

    /** Session used before anything is loaded: embedded defaults, no override. */
    fun initial(): SettingsSession = SettingsSession(saved = policy.defaults())

    /**
     * Builds a session from a bounded read. A document that somehow fails validation is replaced
     * by defaults with an explanatory warning — the app must start even with a hostile file.
     */
    fun fromLoad(
        document: SettingsDocumentV1,
        fromFile: Boolean,
        fellBackToDefaults: Boolean,
        warnings: List<String> = emptyList(),
    ): SettingsSession {
        val validation = policy.validate(document)
        return if (validation.isValid) {
            SettingsSession(
                saved = document,
                pendingLlmOverride = null,
                loadedFromFile = fromFile,
                fellBackToDefaults = fellBackToDefaults,
                warnings = warnings,
            )
        } else {
            SettingsSession(
                saved = policy.defaults(),
                pendingLlmOverride = null,
                loadedFromFile = fromFile,
                fellBackToDefaults = true,
                warnings = warnings + validation.errors.map { "${it.path}: ${it.message}" },
            )
        }
    }

    /**
     * Arms a one-request override. Saved defaults are deliberately left untouched, so closing the
     * app or pressing Reset returns to exactly what the user last chose to persist.
     */
    fun applyOnce(session: SettingsSession, llm: LlmSettings): SettingsSessionResult {
        val candidate = session.saved.copy(llm = llm)
        val validation = policy.validate(candidate)
        if (!validation.isValid) return SettingsSessionResult.Rejected(session, validation.errors)
        return SettingsSessionResult.Applied(
            session = session.copy(pendingLlmOverride = llm),
            message = "Applied to the next reply only; saved defaults are unchanged",
        )
    }

    /**
     * Validates a candidate document for persistence. The caller writes it only on [Applied];
     * the pending override is cleared because the user just promoted their choice to a default.
     */
    fun prepareSave(session: SettingsSession, document: SettingsDocumentV1): SettingsSessionResult {
        val validation = policy.validate(document)
        if (!validation.isValid) return SettingsSessionResult.Rejected(session, validation.errors)
        return SettingsSessionResult.Applied(
            session = session.copy(
                saved = document,
                pendingLlmOverride = null,
                warnings = validation.warnings.map { "${it.path}: ${it.message}" },
            ),
            message = "Saved as the default for new replies",
        )
    }

    /** Returns the embedded defaults and drops any override. */
    fun reset(session: SettingsSession): SettingsSession = session.copy(
        saved = policy.defaults(),
        pendingLlmOverride = null,
        warnings = emptyList(),
    )

    /** Resolves the parameters for one request and consumes the override, if any. */
    fun resolveForRequest(session: SettingsSession): ResolvedRequestSettings = ResolvedRequestSettings(
        llm = session.effectiveLlm,
        session = session.copy(pendingLlmOverride = null),
        usedOverride = session.hasPendingOverride,
    )

    /** Drops an armed override without running a request (for example when Chat is cleared). */
    fun discardOverride(session: SettingsSession): SettingsSession = session.copy(pendingLlmOverride = null)

    /**
     * Ceiling the quick sheet must apply to "max new tokens".
     *
     * The reply budget can never be the *whole* context: the prompt has to fit alongside it.
     * `prepareConversation` enforces `promptTokens + maxNewTokens <= contextSize`, so a ceiling
     * equal to the context size makes that condition unsatisfiable — the trim loop strips every
     * message and then throws, which the user experiences as "the app just never replies".
     *
     * A share of the context is therefore always reserved for the prompt. The reply may claim at
     * most [REPLY_CONTEXT_NUMERATOR]/[REPLY_CONTEXT_DENOMINATOR] of the smaller of the configured
     * budget and the context the loaded runtime actually reports.
     */
    fun maxNewTokensCeiling(session: SettingsSession, runtimeContextTokens: Int?): Int {
        val configured = session.saved.llm.context.maxContextTokens
        val runtime = runtimeContextTokens?.takeIf { it > 0 } ?: configured
        val usable = minOf(configured, runtime)
        val replyBudget = usable * REPLY_CONTEXT_NUMERATOR / REPLY_CONTEXT_DENOMINATOR
        return minOf(ABSOLUTE_MAX_NEW_TOKENS, replyBudget).coerceAtLeast(MIN_REPLY_TOKENS)
    }

    private companion object {
        /** Mirrors the absolute `maxNewTokens` range enforced by [SettingsPolicy]. */
        const val ABSOLUTE_MAX_NEW_TOKENS = 4096

        /** At most half the context may be spent on the reply; the rest is reserved for the prompt. */
        const val REPLY_CONTEXT_NUMERATOR = 1
        const val REPLY_CONTEXT_DENOMINATOR = 2

        /** Even a tiny context must still allow a usable reply. */
        const val MIN_REPLY_TOKENS = 32
    }
}
