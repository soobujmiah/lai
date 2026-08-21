package dev.lai.runtime.ui.screens

import dev.lai.runtime.inference.DownloadProgress

data class LaiModelUi(
    val id: String,
    val name: String,
    val description: String,
    val detail: String,
    val status: String,
    val architecture: String,
    val quantization: String,
    val format: String,
    val sizeLabel: String,
    val backendLabel: String,
    val reviewLabel: String,
    val installed: Boolean,
    val active: Boolean,
    val recommended: Boolean,
    val loading: Boolean,
    val installing: Boolean,
    val deletingEnabled: Boolean,
    val loadEnabled: Boolean,
    val installEnabled: Boolean,
    val deleteEnabled: Boolean,
    val progress: DownloadProgress? = null,
)

data class LaiWorkspaceUi(
    val granted: Boolean,
    val grantStatus: String,
    val settingsStatus: String,
    val discoveryStatus: String,
    val reviewedModelCount: Int,
    val localUnreviewedModelCount: Int,
    val discovering: Boolean,
    val busy: Boolean,
)

data class LaiProviderUi(
    val id: String,
    val name: String,
    val kind: String,
    val status: String,
    val backendLabel: String,
    val networkPolicy: String,
    val selected: Boolean,
)
