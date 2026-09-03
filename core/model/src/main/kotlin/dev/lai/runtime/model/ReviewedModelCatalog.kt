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
    val modelFormat: String,
    val contextSize: Int,
    val compatibleBackendIds: List<String>,
    val preferredBackendId: String,
    val fallbackBackendIds: List<String>,
    val estimatedPeakBytes: Long,
    val requiredAbis: List<String>,
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
        description = "Device-validated Snapdragon CPU baseline; llama-opencl (Adreno OpenCL track) and " +
            "llama-vulkan declared compatible as fallbacks pending device qualification; " +
            "broader Bangla quality evaluation pending.",
        sourceRepository = "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
        fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
        url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/" +
            "qwen2.5-1.5b-instruct-q4_k_m.gguf",
        sha256 = "6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e",
        bytes = 1_117_320_736,
        license = "Apache-2.0",
        architecture = "qwen2",
        quantization = "Q4_K_M",
        modelFormat = "gguf",
        contextSize = 4_096,
        // llama-opencl (Adreno OpenCL track) and llama-vulkan are declared compatible
        // (fallbacks) so the scheduler may select the Adreno GPU once a backend has
        // DEVICE_VALIDATED evidence; until then the accelerator gate keeps CPU active.
        // CPU remains the reviewed baseline and preferred backend.
        compatibleBackendIds = listOf("llama-cpu", "llama-vulkan", "llama-opencl"),
        preferredBackendId = "llama-cpu",
        fallbackBackendIds = listOf("llama-opencl", "llama-vulkan"),
        estimatedPeakBytes = 1_933_521_832,
        requiredAbis = listOf("arm64-v8a"),
        reviewState = setOf(
            ArtifactReviewState.METADATA_VERIFIED,
            ArtifactReviewState.BUILD_COMPATIBLE,
            ArtifactReviewState.DEVICE_VALIDATED,
        ),
        banglaQualityValidated = false,
    )

    // Same Qwen 2.5 1.5B Instruct weights as recommendedCpuBaseline, quantized Q4_0 instead of
    // Q4_K_M specifically to qualify llama-hexagon (NPU): ggml-hexagon's MUL_MAT kernel only
    // accepts Q4_0/Q4_1/Q8_0/IQ4_NL, not Q4_K (docs/HANDOFF-2026-09-03-npu-hexagon-scoping.md).
    // An alternate entry, not the recommended baseline -- CPU stays preferred until device
    // evidence says otherwise for this file too.
    val hexagonQualificationVariant = ReviewedModel(
        id = "qwen2.5-1.5b-instruct-q4-0",
        displayName = "Qwen 2.5 1.5B Instruct (Q4_0)",
        description = "Same Qwen 2.5 1.5B Instruct weights as the CPU baseline, quantized Q4_0 instead of " +
            "Q4_K_M specifically to qualify llama-hexagon (NPU): ggml-hexagon's MUL_MAT kernel only accepts " +
            "Q4_0/Q4_1/Q8_0/IQ4_NL, not Q4_K. Also kernel-compatible with llama-vulkan/llama-opencl. CPU " +
            "remains the reviewed, preferred backend until device evidence says otherwise; no backend has " +
            "DEVICE_VALIDATED evidence for this file yet.",
        sourceRepository = "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
        fileName = "qwen2.5-1.5b-instruct-q4_0.gguf",
        url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/" +
            "qwen2.5-1.5b-instruct-q4_0.gguf",
        sha256 = "dcd819ff094852c38faba6873d8ff0c9d51eadb2844539e52042ae5d647bbfdb",
        bytes = 1_066_227_232,
        license = "Apache-2.0",
        architecture = "qwen2",
        quantization = "Q4_0",
        modelFormat = "gguf",
        contextSize = 4_096,
        compatibleBackendIds = listOf("llama-cpu", "llama-vulkan", "llama-opencl", "llama-hexagon"),
        preferredBackendId = "llama-cpu",
        fallbackBackendIds = listOf("llama-hexagon", "llama-opencl", "llama-vulkan"),
        estimatedPeakBytes = 1_882_428_328,
        requiredAbis = listOf("arm64-v8a"),
        reviewState = setOf(
            ArtifactReviewState.METADATA_VERIFIED,
        ),
        banglaQualityValidated = false,
    )

    val all: List<ReviewedModel> = listOf(recommendedCpuBaseline, hexagonQualificationVariant)
    val embeddedDocument = ReviewedModelCatalogDocument(
        schemaVersion = 1,
        revision = 6,
        generatedAt = "2026-09-03T14:00:00+06:00",
        models = all,
    )

    init {
        require(all.map { it.id }.distinct().size == all.size) { "Reviewed model IDs must be unique" }
        require(all.all { it.sha256.matches(Regex("^[a-f0-9]{64}$")) }) { "Every model requires SHA-256" }
        require(all.all { it.url.startsWith("https://") && it.bytes > 0 }) { "Invalid reviewed artifact" }
        require(all.all { it.modelFormat.isNotBlank() && it.contextSize >= 256 && it.estimatedPeakBytes > 0 }) {
            "Invalid model compatibility metadata"
        }
        require(all.all { model ->
            model.compatibleBackendIds.isNotEmpty() &&
                model.preferredBackendId in model.compatibleBackendIds &&
                model.fallbackBackendIds.all { it in model.compatibleBackendIds } &&
                model.requiredAbis.isNotEmpty()
        }) { "Invalid backend or hardware compatibility metadata" }
    }
}
