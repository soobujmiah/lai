package dev.lai.runtime.shell

import android.content.Context
import androidx.annotation.Keep
import org.json.JSONObject
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Runs in Shizuku's shell/root UserService process, not the application process. */
@Keep
class PrivilegedUserService : IPrivilegedService.Stub() {
    constructor()

    @Keep
    constructor(@Suppress("UNUSED_PARAMETER") context: Context)

    override fun execute(argv: Array<out String>, timeoutMs: Long, outputLimit: Int): String {
        require(argv.isNotEmpty()) { "Empty command" }
        require(argv.size <= 16) { "Too many command arguments" }
        val limit = outputLimit.coerceIn(1_024, 64 * 1_024)
        val process = ProcessBuilder(argv.toList()).start()
        val stdout = BoundedBuffer(limit)
        val stderr = BoundedBuffer(limit)
        val outThread = drainAsync(process.inputStream, stdout)
        val errThread = drainAsync(process.errorStream, stderr)
        val completed = process.waitFor(timeoutMs.coerceIn(1_000, 30_000), TimeUnit.MILLISECONDS)
        if (!completed) process.destroyForcibly()
        outThread.join(1_000)
        errThread.join(1_000)
        return JSONObject()
            .put("exitCode", if (completed) process.exitValue() else -1)
            .put("stdout", stdout.value())
            .put("stderr", stderr.value())
            .put("timedOut", !completed)
            .toString()
    }

    override fun destroy() {
        System.exit(0)
    }

    private fun drainAsync(input: InputStream, destination: BoundedBuffer): Thread = thread(
        start = true,
        isDaemon = true,
        name = "lai-command-stream",
    ) {
        input.use {
            val buffer = ByteArray(4 * 1024)
            while (true) {
                val count = it.read(buffer)
                if (count < 0) break
                destination.append(buffer, count)
            }
        }
    }

    private class BoundedBuffer(private val limit: Int) {
        private val data = java.io.ByteArrayOutputStream(limit)

        @Synchronized
        fun append(bytes: ByteArray, count: Int) {
            val accepted = minOf(count, limit - data.size())
            if (accepted > 0) data.write(bytes, 0, accepted)
        }

        @Synchronized
        fun value(): String = data.toString(Charsets.UTF_8.name())
    }
}
