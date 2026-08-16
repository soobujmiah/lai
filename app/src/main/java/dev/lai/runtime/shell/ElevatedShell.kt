package dev.lai.runtime.shell

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

class ElevatedShell(private val controller: ShizukuController) {
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

        return runCatching {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                val process = Shizuku.newProcess(approved.argv.toTypedArray(), null, null)
                try {
                    coroutineScope {
                        val stdout = async { process.inputStream.bufferedReader().use { it.readText().take(MAX_OUTPUT) } }
                        val stderr = async { process.errorStream.bufferedReader().use { it.readText().take(MAX_OUTPUT) } }
                        val exitCode = withTimeoutOrNull(timeoutMs) {
                            while (process.isAlive) delay(25)
                            process.exitValue()
                        }
                        if (exitCode == null) process.destroy()
                        ShellResult(
                            exitCode = exitCode ?: -1,
                            stdout = stdout.await(),
                            stderr = stderr.await(),
                            timedOut = exitCode == null,
                        )
                    }
                } finally {
                    if (process.isAlive) process.destroy()
                }
            }
        }
    }

    companion object {
        private const val MAX_OUTPUT = 64 * 1024
    }
}
