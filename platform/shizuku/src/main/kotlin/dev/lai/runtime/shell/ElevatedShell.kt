package dev.lai.runtime.shell

import android.content.Context

class ElevatedShell(context: Context, private val controller: ShizukuController) {
    private val userService = ShizukuUserServiceClient(context)

    suspend fun execute(
        request: PrivilegedCommand,
        confirmationGranted: Boolean,
        timeoutMs: Long = 10_000,
    ): Result<ShellResult> {
        val approved = ShellCommandPolicy.compile(request).getOrElse { return Result.failure(it) }
        if (approved.requiresConfirmation && !confirmationGranted) {
            return Result.failure(SecurityException("Explicit user confirmation is required"))
        }
        if (controller.state.value !is ShizukuState.Ready) {
            return Result.failure(IllegalStateException("Shizuku is not connected and authorized"))
        }
        return runCatching { userService.execute(approved.argv, timeoutMs) }
    }
}
