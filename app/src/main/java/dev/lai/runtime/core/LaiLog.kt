package dev.lai.runtime.core

import android.content.Context
import android.os.Build
import android.util.Log
import dev.lai.runtime.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Centralized diagnostic logger for LAI.
 *
 * Every call is mirrored to two sinks:
 *  - Android Logcat (caller-supplied subsystem tag, conventionally prefixed `LAI-`);
 *  - a bounded, app-private diagnostic log file, plus a bounded in-memory ring buffer that is
 *    included in the privacy-filtered diagnostics export (see docs/LOGGING.md).
 *
 * Verbosity is controlled per build type via [minLevel]: the app sets DEBUG for debug builds
 * and INFO for signed release builds in [dev.lai.runtime.LaiApplication.onCreate]. Levels are
 * DEBUG < INFO < WARN < ERROR.
 *
 * Privacy: every line passes through [LaiLogRedactor] before leaving this object, so
 * credentials/tokens are never written to logcat or the file even if a call site is careless.
 * Call sites must still never log prompt/response text, screens, OCR text, tool arguments or
 * file contents — see docs/LOGGING.md for the exact contract.
 */
object LaiLog {

    enum class Level(val order: Int) {
        DEBUG(0),
        INFO(1),
        WARN(2),
        ERROR(3),
        ;

        companion object {
            /** Accepts DEBUG/INFO/WARN/ERROR (case-insensitive); unknown values fall back to INFO. */
            fun parse(raw: String?): Level = when (raw?.trim()?.uppercase()) {
                "DEBUG" -> DEBUG
                "WARN" -> WARN
                "ERROR" -> ERROR
                else -> INFO
            }
        }
    }

    /** One buffered log entry; [message] may contain a stack trace for [Level.ERROR]. */
    data class Entry(
        val timestampEpochMs: Long,
        val level: String,
        val tag: String,
        val message: String,
    )

    private const val MAX_FILE_BYTES = 512 * 1024
    private const val MAX_RING_ENTRIES = 300
    private const val MAX_EXPORT_ENTRIES = 200

    @Volatile
    var minLevel: Level = Level.INFO
        private set

    private val lock = Any()
    private var logFile: File? = null
    private val ring = ArrayDeque<Entry>()
    private val started = AtomicBoolean(false)

    /**
     * Initializes the logger once from the application context. Picks the log level from the
     * build type (debug = DEBUG, signed release = INFO), creates the log file in app-specific
     * external storage (falling back to internal storage) and writes a context header.
     * Safe to call more than once; only the first call has any effect.
     */
    fun configure(context: Context, debugBuild: Boolean) {
        if (!started.compareAndSet(false, true)) return
        minLevel = if (debugBuild) Level.DEBUG else Level.INFO
        val dir = context.getExternalFilesDir(null)?.let { File(it, "logs") }
            ?: File(context.filesDir, "logs")
        dir.mkdirs()
        logFile = File(dir, "lai-${if (debugBuild) "debug" else "release"}.log")
        append(Level.INFO, "LAI-app", headerLine(if (debugBuild) "debug" else "release"))
    }

    fun d(tag: String, message: String) = log(Level.DEBUG, tag, message, null)
    fun i(tag: String, message: String) = log(Level.INFO, tag, message, null)
    fun w(tag: String, message: String) = log(Level.WARN, tag, message, null)
    fun e(tag: String, message: String) = log(Level.ERROR, tag, message, null)
    fun e(tag: String, message: String, throwable: Throwable) = log(Level.ERROR, tag, message, throwable)

    /** Bounded recent entries for the diagnostics export; newest last. */
    fun recentEntries(limit: Int = MAX_EXPORT_ENTRIES): List<Entry> = synchronized(lock) {
        ring.toList().takeLast(limit)
    }

    /** Absolute path of the current diagnostic log file (for the native crash handler). */
    fun logFilePath(): String? = synchronized(lock) { logFile?.absolutePath }

    /**
     * Complete log text (header + file content) for explicit user export via SAF. Falls back to
     * the in-memory ring buffer when no file exists yet.
     */
    fun exportText(): String {
        val file = logFile
        val fromFile = try {
            if (file != null && file.exists() && file.length() <= MAX_FILE_BYTES * 2L) {
                file.readText(Charsets.UTF_8)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
        if (!fromFile.isNullOrBlank()) return fromFile
        return synchronized(lock) {
            buildString {
                if (logFile != null) appendLine("# No log file yet — showing the in-memory ring buffer")
                ring.joinToString("\n") { "${isoTimestamp(it.timestampEpochMs)} ${it.level} ${it.tag}: ${it.message}" }
            }
        }
    }

    private fun log(level: Level, tag: String, message: String, throwable: Throwable?) {
        if (level.order < minLevel.order) return
        val safeTag = sanitizeTag(tag)
        val safeMessage = LaiLogRedactor.redact(message.ifBlank { "empty message" })
        val stack = throwable?.let { LaiLogRedactor.redact(Log.getStackTraceString(it)) }
        val logcatText = if (stack.isNullOrBlank()) safeMessage else "$safeMessage\n$stack"
        when (level) {
            Level.DEBUG -> Log.d(safeTag, logcatText)
            Level.INFO -> Log.i(safeTag, logcatText)
            Level.WARN -> Log.w(safeTag, logcatText)
            Level.ERROR -> Log.e(safeTag, logcatText)
        }
        append(level, safeTag, logcatText)
    }

    private fun append(level: Level, tag: String, text: String) {
        val entry = Entry(System.currentTimeMillis(), level.name, tag, text)
        synchronized(lock) {
            ring.addLast(entry)
            if (ring.size > MAX_RING_ENTRIES) ring.removeFirst()
            writeFileEntry(entry)
        }
    }

    private fun writeFileEntry(entry: Entry) {
        val file = logFile ?: return
        try {
            if (file.exists() && file.length() > MAX_FILE_BYTES) rotate(file)
            FileOutputStream(file, true).use { output ->
                output.write((formatEntry(entry) + "\n").toByteArray(Charsets.UTF_8))
            }
        } catch (_: Exception) {
            // Logging must never crash the app; on failure the ring buffer still holds the entry.
        }
    }

    private fun rotate(file: File) {
        val backup = File(file.parentFile, file.name + ".1")
        backup.delete()
        file.renameTo(backup)
    }

    private fun formatEntry(entry: Entry): String =
        "${isoTimestamp(entry.timestampEpochMs)} ${entry.level} ${entry.tag}: ${entry.message}"

    private fun isoTimestamp(epochMs: Long): String =
        TIMESTAMP_FORMAT.get().format(Date(epochMs))

    private fun headerLine(buildType: String): String = buildString {
        append("LAI diagnostic log ")
        append("($buildType build)")
        append(" · app version ").append(BuildConfig.VERSION_NAME)
        append(" (").append(BuildConfig.VERSION_CODE).append(')')
        append(" · android sdk ").append(Build.VERSION.SDK_INT)
        append(" · device ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
        append(" · abi ").append(Build.SUPPORTED_ABIS.joinToString(","))
    }

    private fun sanitizeTag(tag: String): String {
        // android.util.Log truncates to 23 chars; keep tags clean and predictable.
        val clean = tag.ifBlank { "LAI" }
        return clean.take(23)
    }

    private val TIMESTAMP_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
    }
}
