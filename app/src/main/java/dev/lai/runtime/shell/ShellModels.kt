package dev.lai.runtime.shell

import kotlinx.serialization.Serializable

@Serializable
data class PrivilegedCommand(
    val operation: String,
    val arguments: Map<String, String> = emptyMap(),
)

@Serializable
data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean = false,
)

sealed interface ShizukuState {
    data object Unavailable : ShizukuState
    data object PermissionRequired : ShizukuState
    data class Ready(val uid: Int) : ShizukuState
    data class Error(val message: String) : ShizukuState
}
