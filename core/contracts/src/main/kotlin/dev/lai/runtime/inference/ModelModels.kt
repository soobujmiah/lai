package dev.lai.runtime.inference

import kotlinx.serialization.Serializable

@Serializable
data class ModelSpec(
    val id: String,
    val displayName: String,
    val url: String,
    val sha256: String? = null,
    val expectedBytes: Long? = null,
)

@Serializable
data class ModelImportSpec(
    val id: String,
    val displayName: String,
    val sha256: String,
    val expectedBytes: Long? = null,
)

@Serializable
data class InstalledModel(
    val id: String,
    val displayName: String,
    val fileName: String,
    val bytes: Long,
    val sha256: String,
    val sourceUrl: String,
    val installedAtEpochMs: Long,
)

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long?,
) {
    val fraction: Float? = totalBytes?.takeIf { it > 0 }?.let {
        (downloadedBytes.toDouble() / it.toDouble()).coerceIn(0.0, 1.0).toFloat()
    }
}
