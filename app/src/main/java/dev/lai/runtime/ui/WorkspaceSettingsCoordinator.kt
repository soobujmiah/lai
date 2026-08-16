package dev.lai.runtime.ui

import dev.lai.runtime.settings.LlmSettings
import dev.lai.runtime.settings.SettingsDocumentV1
import dev.lai.runtime.settings.SettingsSession
import dev.lai.runtime.settings.SettingsSessionPolicy
import dev.lai.runtime.settings.SettingsSessionResult
import dev.lai.runtime.workspace.DiscoveryLimits
import dev.lai.runtime.workspace.ModelDiscoveryPort
import dev.lai.runtime.workspace.ModelDiscoveryStatus
import dev.lai.runtime.workspace.SettingsStorePort
import dev.lai.runtime.workspace.WorkspaceGrantPort
import dev.lai.runtime.workspace.WorkspaceGrantState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Immutable workspace + settings slice of the UI state (Phase 2A items 6–7).
 *
 * Only **coarse** discovery counts are surfaced. Raw digests, file names, and file contents are
 * deliberately absent so nothing user-identifying can reach the UI, a screenshot, or a
 * diagnostics export.
 */
data class WorkspaceUiState(
    val grantState: WorkspaceGrantState = WorkspaceGrantState.NOT_GRANTED,
    val session: SettingsSession = SettingsSession(),
    val settingsStatus: String = "Using built-in defaults",
    val quickSettingsVisible: Boolean = false,
    val savingSettings: Boolean = false,
    val discovering: Boolean = false,
    val discoveryStatus: String = "No workspace scan yet",
    val reviewedModelCount: Int = 0,
    val localUnreviewedModelCount: Int = 0,
) {
    val granted: Boolean get() = grantState == WorkspaceGrantState.GRANTED

    /** LLM parameters the next reply will use (quick-sheet override, else saved defaults). */
    val effectiveLlm: LlmSettings get() = session.effectiveLlm

    /** `true` while an "Apply once" override is armed for exactly one request. */
    val overrideArmed: Boolean get() = session.hasPendingOverride
}

/**
 * Owns the typed-settings session and the SAF workspace status for the product shell.
 *
 * It depends only on the pure ports in `core:contracts` (`WorkspaceGrantPort`,
 * `SettingsStorePort`, `ModelDiscoveryPort`) and pure policy in `core:policy`, never on Android
 * classes. That keeps every rule that matters — defaults on an absent workspace, fallback on a
 * malformed or oversized file, "apply once" not mutating saved defaults, save/reset round-trips —
 * verifiable in ordinary JVM unit tests with fakes, and keeps `MainViewModel` a thin binder.
 *
 * Taking or releasing the SAF grant is an Android authority action and stays in
 * `platform:workspace` + the Activity result; this class only reacts to the resulting state.
 */
class WorkspaceSettingsCoordinator(
    private val grant: WorkspaceGrantPort,
    private val store: SettingsStorePort,
    private val discovery: ModelDiscoveryPort,
    private val policy: SettingsSessionPolicy = SettingsSessionPolicy(),
) {
    private val _state = MutableStateFlow(WorkspaceUiState(session = policy.initial()))
    val state: StateFlow<WorkspaceUiState> = _state.asStateFlow()

    /**
     * Reads the grant state and loads settings. Safe to call on start-up and after every grant
     * change; it never throws and always leaves a usable document in place.
     */
    suspend fun refresh(): String {
        val grantState = grant.state
        val outcome = store.load()
        val session = policy.fromLoad(
            document = outcome.document,
            fromFile = outcome.fromFile,
            fellBackToDefaults = outcome.fellBackToDefaults,
            warnings = outcome.warnings,
        )
        val status = describe(grantState, session)
        _state.update {
            it.copy(
                grantState = grantState,
                session = session,
                settingsStatus = status,
                // A revoked or re-granted tree invalidates any previous scan result.
                reviewedModelCount = if (grantState == WorkspaceGrantState.GRANTED) it.reviewedModelCount else 0,
                localUnreviewedModelCount = if (grantState == WorkspaceGrantState.GRANTED) {
                    it.localUnreviewedModelCount
                } else {
                    0
                },
                discoveryStatus = if (grantState == WorkspaceGrantState.GRANTED) {
                    it.discoveryStatus
                } else {
                    "No workspace scan yet"
                },
            )
        }
        return status
    }

    fun setQuickSettingsVisible(visible: Boolean) = _state.update { it.copy(quickSettingsVisible = visible) }

    /**
     * Arms a one-request override. Saved defaults are never touched, so an experiment cannot
     * silently become the user's permanent configuration.
     */
    fun applyOnce(llm: LlmSettings): String {
        val result = policy.applyOnce(_state.value.session, llm)
        val message = when (result) {
            is SettingsSessionResult.Applied -> result.message
            is SettingsSessionResult.Rejected -> result.message
        }
        _state.update {
            it.copy(
                session = result.session,
                settingsStatus = describe(it.grantState, result.session),
                quickSettingsVisible = result !is SettingsSessionResult.Applied,
            )
        }
        return message
    }

    /**
     * Validates then persists a document as the new default. A document that fails validation is
     * rejected before any write, and a write failure (for example a revoked grant) keeps the
     * previous saved defaults instead of leaving the UI lying about what is stored.
     */
    suspend fun saveDefaults(document: SettingsDocumentV1): String =
        when (val prepared = policy.prepareSave(_state.value.session, document)) {
            is SettingsSessionResult.Rejected -> prepared.message
            is SettingsSessionResult.Applied -> {
                _state.update { it.copy(savingSettings = true) }
                val result = store.save(document)
                _state.update {
                    val session = if (result.isSuccess) prepared.session else it.session
                    it.copy(
                        session = session,
                        savingSettings = false,
                        settingsStatus = describe(it.grantState, session),
                    )
                }
                result.fold(
                    onSuccess = { prepared.message },
                    onFailure = { error ->
                        "Settings were not saved: ${error.message ?: "workspace write failed"}"
                    },
                )
            }
        }

    /**
     * Returns to embedded defaults. When a workspace is granted the reset is persisted too, so
     * the on-disk file and the in-app state cannot drift apart.
     */
    suspend fun resetDefaults(): String {
        val reset = policy.reset(_state.value.session)
        val granted = _state.value.grantState == WorkspaceGrantState.GRANTED
        val persisted = if (granted) store.save(reset.saved) else Result.success(Unit)
        val message = when {
            persisted.isSuccess && granted -> "Settings reset to defaults and saved"
            persisted.isSuccess -> "Settings reset to defaults for this device"
            else -> "Reset applied in-app but not written: ${persisted.exceptionOrNull()?.message ?: "write failed"}"
        }
        _state.update { it.copy(session = reset, settingsStatus = describe(it.grantState, reset)) }
        return message
    }

    /**
     * Resolves the LLM parameters for exactly one request and consumes any armed override.
     * Called immediately before generation so "Apply once" means precisely one reply.
     */
    fun consumeForRequest(): LlmSettings {
        val resolved = policy.resolveForRequest(_state.value.session)
        _state.update {
            it.copy(session = resolved.session, settingsStatus = describe(it.grantState, resolved.session))
        }
        return resolved.llm
    }

    /** Drops an armed override without spending it (for example when the conversation is cleared). */
    fun discardOverride() = _state.update {
        val session = policy.discardOverride(it.session)
        it.copy(session = session, settingsStatus = describe(it.grantState, session))
    }

    /** Ceiling the quick sheet applies to "max new tokens" for the loaded runtime. */
    fun maxNewTokensCeiling(runtimeContextTokens: Int?): Int =
        policy.maxNewTokensCeiling(_state.value.session, runtimeContextTokens)

    /**
     * Scans the granted workspace for model files and publishes **coarse counts only**.
     * Registration is metadata-only: nothing is copied, verified into runtime storage, or loaded.
     */
    suspend fun discoverModels(
        reviewedBySha256: Map<String, String>,
        limits: DiscoveryLimits = DiscoveryLimits(),
    ): String {
        if (_state.value.grantState != WorkspaceGrantState.GRANTED) {
            val message = "Grant a workspace folder first"
            _state.update { it.copy(discoveryStatus = message) }
            return message
        }
        _state.update { it.copy(discovering = true, discoveryStatus = "Scanning workspace…") }
        val result = discovery.discoverModels(reviewedBySha256, limits)
        val message = result.fold(
            onSuccess = { models ->
                val reviewed = models.count { it.status == ModelDiscoveryStatus.REVIEWED }
                val local = models.count { it.status == ModelDiscoveryStatus.LOCAL_UNREVIEWED }
                _state.update {
                    it.copy(reviewedModelCount = reviewed, localUnreviewedModelCount = local)
                }
                "$reviewed reviewed • $local local unreviewed • nothing was loaded"
            },
            onFailure = { error -> "Workspace scan failed: ${error.message ?: "unknown error"}" },
        )
        _state.update { it.copy(discovering = false, discoveryStatus = message) }
        return message
    }

    private fun describe(grantState: WorkspaceGrantState, session: SettingsSession): String {
        val source = when {
            session.hasPendingOverride -> "Custom settings apply to the next reply only"
            grantState != WorkspaceGrantState.GRANTED -> "Using built-in defaults (no workspace folder)"
            session.fellBackToDefaults && session.loadedFromFile -> "Saved settings were unreadable; using defaults"
            session.loadedFromFile -> "Loaded from your workspace folder"
            else -> "Using built-in defaults"
        }
        val llm = session.effectiveLlm
        return "$source • temp ${format(llm.temperature)} • top-P ${format(llm.topP)} • " +
            "${llm.maxNewTokens} max tokens"
    }

    private fun format(value: Float): String {
        val rounded = Math.round(value * 100f) / 100f
        return if (rounded == rounded.toInt().toFloat()) "${rounded.toInt()}.0" else rounded.toString()
    }
}
