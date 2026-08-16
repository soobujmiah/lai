package dev.lai.runtime.model

import dev.lai.runtime.inference.ModelImportSpec
import dev.lai.runtime.inference.ModelSpec
import kotlinx.serialization.Serializable

@Serializable
enum class ArtifactReviewState { METADATA_VERIFIED, BUILD_COMPATIBLE, DEVICE_VALIDATED }

@Serializable
data class ReviewedModel(
    val id: String,
    val displayName: String,
    val description: String,
    val sourceRepository: String,
    val fileName: String,
    val url: String,
    val sha256: String,
    val bytes: Long,
    val license: String,
    val architecture: String,
    val quantization: String,
    val reviewState: Set<ArtifactReviewState>,
    val banglaQualityValidated: Boolean,
) {
    fun toModelSpec(): ModelSpec = ModelSpec(
        id = id,
        displayName = displayName,
        url = url,
        sha256 = sha256,
        expectedBytes = bytes,
    )

    fun toImportSpec(): ModelImportSpec = ModelImportSpec(
        id = id,
        displayName = displayName,
        sha256 = sha256,
        expectedBytes = bytes,
    )
}

@Serializable
data class ReviewedModelCatalogDocument(
    val schemaVersion: Int,
    val revision: Int,
    val generatedAt: String,
    val models: List<ReviewedModel>,
)

/** Immutable fallback catalog shipped with the APK and used whenever the signed web catalog is unavailable. */
object ReviewedModelCatalog {
    val recommendedCpuBaseline = ReviewedModel(
        id = "qwen2.5-1.5b-instruct-q4-k-m",
        displayName = "Qwen 2.5 1.5B Instruct",
        description = "Official multilingual Q4_K_M CPU baseline; Bangla quality evaluation pending.",
        sourceRepository = "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
        fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
        url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/" +
            "qwen2.5-1.5b-instruct-q4_k_m.gguf",
        sha256 = "6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e",
        bytes = 1_117_320_736,
        license = "Apache-2.0",
        architecture = "qwen2",
        quantization = "Q4_K_M",
        reviewState = setOf(ArtifactReviewState.METADATA_VERIFIED, ArtifactReviewState.BUILD_COMPATIBLE),
        banglaQualityValidated = false,
    )

    val all: List<ReviewedModel> = listOf(recommendedCpuBaseline)
    val embeddedDocument = ReviewedModelCatalogDocument(
        schemaVersion = 1,
        revision = 1,
        generatedAt = "2026-08-16T00:00:00Z",
        models = all,
    )

    init {
        require(all.map { it.id }.distinct().size == all.size) { "Reviewed model IDs must be unique" }
        require(all.all { it.sha256.matches(Regex("^[a-f0-9]{64}$")) }) { "Every model requires SHA-256" }
        require(all.all { it.url.startsWith("https://") && it.bytes > 0 }) { "Invalid reviewed artifact" }
    }
}
