package dev.lai.runtime.workspace

import kotlinx.serialization.Serializable

/**
 * SAF tree grant state for the user-owned LAI workspace.
 *
 * The workspace is granted through Android's `ACTION_OPEN_DOCUMENT_TREE`; LAI never requests
 * `MANAGE_EXTERNAL_STORAGE` and never translates an arbitrary `content://` URI into a raw path.
 * The Android adapter owns the persistable-permission bookkeeping; this enum is the pure state
 * surfaced to UI, diagnostics, and policy.
 */
@Serializable
enum class WorkspaceGrantState { NOT_GRANTED, GRANTED, REVOKED }

/**
 * Canonical directory/file layout relative to the granted tree root. Pure constants shared by
 * the SAF adapter, the discovery classifier, the settings store, and diagnostics so no path
 * string is hard-coded in more than one place.
 */
object WorkspaceLayout {
    const val MODELS_DIRECTORY = "models"
    const val TOOLS_DIRECTORY = "tools"
    const val CONFIG_DIRECTORY = "config"
    const val CACHE_DIRECTORY = "cache"
    const val SETTINGS_FILE_NAME = "settings.json"

    /** Relative path segments of the settings file from the tree root (e.g. `["config","settings.json"]`). */
    val settingsRelativeSegments: List<String> = listOf(CONFIG_DIRECTORY, SETTINGS_FILE_NAME)

    /** Directories LAI is allowed to create/read inside the granted tree. */
    val managedDirectories: List<String> = listOf(MODELS_DIRECTORY, TOOLS_DIRECTORY, CONFIG_DIRECTORY, CACHE_DIRECTORY)
}

/**
 * A raw file observed during a bounded SAF traversal, before classification. The Android adapter
 * supplies these values (relative path, exact size, lowercased-hex SHA-256, and a magic-byte
 * format such as `gguf`); the pure classifier decides their status.
 */
@Serializable
data class ModelCandidate(
    val relativePath: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String?,
    val modelFormat: String?,
)

/** Status assigned to a discovered model file by [WorkspacePolicy.classify]. */
@Serializable
enum class ModelDiscoveryStatus { REVIEWED, LOCAL_UNREVIEWED, REJECTED }

/**
 * A classified model file. `REVIEWED` and `LOCAL_UNREVIEWED` entries may be registered; `REJECTED`
 * entries never are. **Registration never allocates weights or auto-loads inference** — loading
 * still copies/verifies into app-private runtime storage.
 */
@Serializable
data class DiscoveredModel(
    val relativePath: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String?,
    val modelFormat: String?,
    val status: ModelDiscoveryStatus,
    val reviewedCatalogId: String? = null,
    val rejectionReason: String? = null,
)

/**
 * Bounded discovery limits. `maxRegisteredFiles` and `maxFileBytes` are enforced by the pure
 * classifier; depth/time bounds are enforced by the SAF adapter during traversal.
 */
@Serializable
data class DiscoveryLimits(
    val maxRegisteredFiles: Int = DEFAULT_MAX_REGISTERED_FILES,
    val maxFileBytes: Long = DEFAULT_MAX_FILE_BYTES,
    val supportedFormats: Set<String> = DEFAULT_SUPPORTED_FORMATS,
) {
    companion object {
        const val DEFAULT_MAX_REGISTERED_FILES = 64

        /** A single discovered model may not exceed 4 GiB. */
        const val DEFAULT_MAX_FILE_BYTES: Long = 4L * 1024 * 1024 * 1024

        val DEFAULT_SUPPORTED_FORMATS: Set<String> = setOf("gguf")
    }
}
