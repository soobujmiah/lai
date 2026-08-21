package dev.lai.runtime.ui.screens

import dev.lai.runtime.inference.InstalledModel
import dev.lai.runtime.model.ReviewedModel
import dev.lai.runtime.ui.MainUiState

/**
 * Presentation adapters keep Material 3 surfaces independent from runtime/domain models.
 * No inference, storage, network, or platform authority belongs in these adapters.
 */
fun MainUiState.toModelsUi(): List<LaiModelUi> = buildList {
    installedModels.forEach { model -> add(model.toUi(activeModelId == model.id)) }
    supportedModels
        .filterNot { reviewed -> installedModels.any { it.id == reviewed.id } }
        .forEach { add(it.toUi()) }
}

private fun InstalledModel.toUi(active: Boolean): LaiModelUi = LaiModelUi(
    id = id,
    name = displayName,
    detail = "Installed local model",
    status = if (active) "Active" else "Installed",
)

private fun ReviewedModel.toUi(): LaiModelUi = LaiModelUi(
    id = id,
    name = displayName,
    detail = "$architecture • $quantization • ${formatBytes(bytes)}",
    status = if (reviewState.isNotEmpty()) "Reviewed" else "Available",
)

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> "%.0f MB".format(bytes / 1_048_576.0)
    else -> "%.0f KB".format(bytes / 1024.0)
}

/**
 * Workspace intentionally exposes only coarse, non-identifying status. Raw paths and document
 * names stay behind the workspace authority boundary.
 */
fun MainUiState.toWorkspaceUi(): List<LaiProjectUi> = listOf(
    LaiProjectUi(
        id = "workspace",
        name = if (workspace.granted) "Workspace" else "Workspace access",
        detail = when {
            workspace.granted -> "${workspace.reviewedModelCount} reviewed • " +
                "${workspace.localUnreviewedModelCount} local unreviewed"
            else -> workspace.discoveryStatus
        },
    ),
)
