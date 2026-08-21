package dev.lai.runtime.ui.screens

import dev.lai.runtime.inference.ComputeClass
import dev.lai.runtime.inference.InstalledModel
import dev.lai.runtime.model.ArtifactReviewState
import dev.lai.runtime.model.ReviewedModel
import dev.lai.runtime.ui.MainUiState
import dev.lai.runtime.ui.RuntimeOperation
import dev.lai.runtime.workspace.WorkspaceGrantState

/**
 * Presentation adapters keep Material 3 surfaces independent from runtime/domain models.
 * No inference, storage, network, or platform authority belongs in these adapters.
 */
fun MainUiState.toModelsUi(): List<LaiModelUi> = buildList {
    val reviewedById = supportedModels.associateBy { it.id }
    installedModels.forEach { model ->
        add(model.toUi(
            reviewed = reviewedById[model.id],
            active = activeModelId == model.id,
            recommended = model.id == recommendedModel.id,
            state = this@toModelsUi,
        ))
    }
    supportedModels
        .filterNot { reviewed -> installedModels.any { it.id == reviewed.id } }
        .forEach { reviewed -> add(reviewed.toUi(this@toModelsUi)) }
}

fun MainUiState.toWorkspaceUi(): LaiWorkspaceUi = LaiWorkspaceUi(
    granted = workspace.granted,
    grantStatus = when (workspace.grantState) {
        WorkspaceGrantState.GRANTED -> "Workspace connected"
        WorkspaceGrantState.REVOKED -> "Workspace permission was revoked"
        WorkspaceGrantState.NOT_GRANTED -> "No workspace connected"
    },
    settingsStatus = workspace.settingsStatus,
    discoveryStatus = workspace.discoveryStatus,
    reviewedModelCount = workspace.reviewedModelCount,
    localUnreviewedModelCount = workspace.localUnreviewedModelCount,
    discovering = workspace.discovering,
    busy = busy,
)

fun MainUiState.toProvidersUi(): List<LaiProviderUi> {
    val backendSummary = runtimeDetail.ifBlank { "Runtime not probed" }
    val cpuReady = runtimeDetail.contains("cpu", ignoreCase = true) ||
        schedulerDetail.contains("llama-cpu", ignoreCase = true)
    val gpuCompiled = runtimeDetail.contains("vulkan", ignoreCase = true) ||
        runtimeDetail.contains("opencl", ignoreCase = true)
    return listOf(
        LaiProviderUi(
            id = "local-runtime",
            name = "Local runtime",
            kind = "Local",
            status = if (activeModelId != null) "Loaded" else if (installedModels.isNotEmpty()) "Available" else "Needs model",
            backendLabel = backendSummary,
            networkPolicy = "Local-only by default",
            selected = true,
        ),
        LaiProviderUi(
            id = "local-cpu",
            name = "CPU backend",
            kind = "Local",
            status = if (cpuReady) "Measured/available" else "Unavailable until native runtime is present",
            backendLabel = "llama-cpu • safest fallback",
            networkPolicy = "Offline",
            selected = schedulerDetail.contains("llama-cpu", ignoreCase = true),
        ),
        LaiProviderUi(
            id = "local-gpu",
            name = "GPU acceleration",
            kind = "Local",
            status = if (gpuCompiled) "Compiled; requires device validation" else "Not available in this build",
            backendLabel = "llama-opencl / llama-vulkan • never assumed stable",
            networkPolicy = "Offline",
            selected = schedulerDetail.contains("llama-vulkan", ignoreCase = true) ||
                schedulerDetail.contains("llama-opencl", ignoreCase = true),
        ),
        LaiProviderUi(
            id = "cloud-providers",
            name = "Cloud providers",
            kind = "Cloud",
            status = "Configuration UI only; no provider adapter active",
            backendLabel = "OpenAI, Anthropic, OpenAI-compatible, custom endpoint planned behind gateway",
            networkPolicy = "No implicit cloud fallback",
            selected = false,
        ),
    )
}

private fun InstalledModel.toUi(
    reviewed: ReviewedModel?,
    active: Boolean,
    recommended: Boolean,
    state: MainUiState,
): LaiModelUi {
    val busyForThis = state.operation == RuntimeOperation.LOADING && state.activeModelId == id
    return LaiModelUi(
        id = id,
        name = displayName,
        description = reviewed?.description ?: "User-installed local model. Compatibility facts are limited to verified file identity.",
        detail = reviewed?.let { "${it.architecture} • ${it.quantization} • ${formatBytes(it.bytes)}" }
            ?: "Installed • ${formatBytes(bytes)}",
        status = when {
            active -> "Active"
            state.operation == RuntimeOperation.LOADING -> "Loading"
            else -> "Installed"
        },
        architecture = reviewed?.architecture ?: "Unknown",
        quantization = reviewed?.quantization ?: "Unknown",
        format = reviewed?.modelFormat ?: fileName.substringAfterLast('.', "unknown"),
        sizeLabel = formatBytes(bytes),
        backendLabel = reviewed?.backendLabel() ?: "Runtime will verify backend compatibility before load",
        reviewLabel = reviewed?.reviewLabel() ?: "Local verified file",
        installed = true,
        active = active,
        recommended = recommended,
        loading = busyForThis,
        installing = false,
        deletingEnabled = !active,
        loadEnabled = !state.busy && !active,
        installEnabled = false,
        deleteEnabled = !state.busy && !active,
    )
}

private fun ReviewedModel.toUi(state: MainUiState): LaiModelUi {
    val downloading = state.downloadingModelId == id
    return LaiModelUi(
        id = id,
        name = displayName,
        description = description,
        detail = "$architecture • $quantization • ${formatBytes(bytes)}",
        status = when {
            downloading -> "Installing"
            id == state.recommendedModel.id -> "Recommended"
            else -> "Reviewed"
        },
        architecture = architecture,
        quantization = quantization,
        format = modelFormat.uppercase(),
        sizeLabel = formatBytes(bytes),
        backendLabel = backendLabel(),
        reviewLabel = reviewLabel(),
        installed = false,
        active = false,
        recommended = id == state.recommendedModel.id,
        loading = false,
        installing = downloading,
        deletingEnabled = false,
        loadEnabled = false,
        installEnabled = !state.busy,
        deleteEnabled = false,
        progress = if (downloading) state.downloadProgress else null,
    )
}

private fun ReviewedModel.backendLabel(): String = buildString {
    append(preferredBackendId)
    if (fallbackBackendIds.isNotEmpty()) append(" fallback: ${fallbackBackendIds.joinToString()}")
    append(" • ${compatibleBackendIds.size} compatible")
}

private fun ReviewedModel.reviewLabel(): String {
    val states = reviewState.map {
        when (it) {
            ArtifactReviewState.METADATA_VERIFIED -> "metadata"
            ArtifactReviewState.BUILD_COMPATIBLE -> "build"
            ArtifactReviewState.DEVICE_VALIDATED -> "device"
        }
    }
    return if (states.isEmpty()) "Unreviewed" else "Reviewed: ${states.joinToString(" + ")}"
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> "%.0f MB".format(bytes / 1_048_576.0)
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private fun computeClassLabel(computeClass: ComputeClass): String = when (computeClass) {
    ComputeClass.CPU -> "CPU"
    ComputeClass.GPU -> "GPU"
    ComputeClass.NPU -> "NPU"
    ComputeClass.DSP -> "DSP"
    ComputeClass.OTHER -> "Other"
}
