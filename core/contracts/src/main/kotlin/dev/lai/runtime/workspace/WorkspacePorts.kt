package dev.lai.runtime.workspace

import dev.lai.runtime.settings.SettingsDocumentV1

/**
 * Pure ports over the user-granted SAF workspace (Phase 2A item 7).
 *
 * The Android adapters in `platform:workspace` implement these; `app` and pure decision code
 * depend only on the interfaces, so the composition root can be exercised by fakes in ordinary
 * JVM unit tests without Robolectric, an emulator, or a real `content://` tree.
 *
 * Nothing here exposes a `Uri`, a raw path, or any Android type: taking the grant is an Android
 * authority action that stays inside the adapter and the Activity result, while everything the
 * product logic needs is the resulting [WorkspaceGrantState] plus bounded, typed data.
 */

/** Read-only view of the current SAF tree grant. */
interface WorkspaceGrantPort {
    /** Current grant state, recomputed from the persisted URI and held persistable permissions. */
    val state: WorkspaceGrantState
}

/**
 * Effective settings plus provenance after a bounded read.
 *
 * [document] is **always** safe to use: when the workspace is not granted, the file is absent,
 * oversized, or malformed, the adapter substitutes embedded defaults and explains why in
 * [warnings] instead of throwing.
 */
data class SettingsLoadOutcome(
    val document: SettingsDocumentV1,
    val fromFile: Boolean,
    val fellBackToDefaults: Boolean,
    val warnings: List<String>,
)

/** Bounded, non-secret settings persistence (`config/settings.json` inside the granted tree). */
interface SettingsStorePort {
    /** Never throws; falls back to embedded defaults with a reason. */
    suspend fun load(): SettingsLoadOutcome

    /** Verifies the candidate is exactly the v1 schema, then persists it atomically. */
    suspend fun save(document: SettingsDocumentV1): Result<Unit>
}

/**
 * Bounded model discovery inside the granted workspace.
 *
 * Discovery registers metadata only — it never allocates weights and never auto-loads inference.
 */
interface ModelDiscoveryPort {
    suspend fun discoverModels(
        reviewedBySha256: Map<String, String>,
        limits: DiscoveryLimits = DiscoveryLimits(),
    ): Result<List<DiscoveredModel>>
}
