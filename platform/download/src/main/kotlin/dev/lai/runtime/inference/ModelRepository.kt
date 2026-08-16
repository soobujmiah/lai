package dev.lai.runtime.inference

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.StatFs
import dev.lai.runtime.core.LaiJson
import dev.lai.runtime.privacy.DataClass
import dev.lai.runtime.privacy.DataFlowDirection
import dev.lai.runtime.privacy.LocalFirstPolicy
import dev.lai.runtime.privacy.NetworkDecision
import dev.lai.runtime.privacy.NetworkPurpose
import dev.lai.runtime.privacy.NetworkRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ModelRepository private constructor(
    context: Context,
    private val networkPolicy: LocalFirstPolicy,
    private val client: OkHttpClient,
) {
    constructor(context: Context) : this(context, LocalFirstPolicy(), defaultHttpClient())

    private val modelDir = File(context.noBackupFilesDir, "models").apply { mkdirs() }
    private val registryFile = File(modelDir, "registry.json")

    suspend fun list(): List<InstalledModel> = withContext(Dispatchers.IO) { readRegistry() }

    suspend fun download(
        spec: ModelSpec,
        onProgress: (DownloadProgress) -> Unit = {},
    ): Result<InstalledModel> = withContext(Dispatchers.IO) {
        runCatching {
            validateDownload(spec)
            modelDir.mkdirs()
            val partialFile = File(modelDir, "${spec.id}.gguf.part")
            var existingBytes = partialFile.takeIf(File::exists)?.length() ?: 0L
            if (existingBytes > MAX_MODEL_BYTES || spec.expectedBytes?.let { existingBytes > it } == true) {
                check(partialFile.delete()) { "Could not remove an oversized model staging file" }
                existingBytes = 0L
            }
            ensureStorageAvailable(spec.expectedBytes?.minus(existingBytes)?.coerceAtLeast(0))
            val request = Request.Builder()
                .url(spec.url)
                .header("User-Agent", "LAI-Android/0.4")
                .apply { if (existingBytes > 0) header("Range", "bytes=$existingBytes-") }
                .build()

            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Download failed with HTTP ${response.code}" }
                reviewArtifactNetwork(response.request.url.toString(), requireNotNull(spec.sha256))
                val append = existingBytes > 0 && response.code == 206
                if (!append && partialFile.exists()) partialFile.delete()
                val base = if (append) existingBytes else 0L
                val transferLimit = transferLimit(spec.expectedBytes, base)
                val responseBytes = response.body?.contentLength()?.takeIf { it >= 0 }
                responseBytes?.let { length ->
                    check(length <= transferLimit - base) {
                        "Model response exceeds the allowed artifact size"
                    }
                }
                val total = responseBytes?.plus(base) ?: spec.expectedBytes
                response.body?.byteStream()?.use { input ->
                    try {
                        streamToPartial(input, partialFile, append, base, total, transferLimit, onProgress)
                    } catch (error: StreamLimitExceededException) {
                        partialFile.delete()
                        throw error
                    }
                } ?: error("Server returned an empty body")
            }

            verifyAndActivate(
                id = spec.id,
                displayName = spec.displayName,
                expectedSha256 = requireNotNull(spec.sha256),
                expectedBytes = spec.expectedBytes,
                partialFile = partialFile,
                sourceLabel = spec.url,
            )
        }
    }

    suspend fun importModel(
        spec: ModelImportSpec,
        contentResolver: ContentResolver,
        uri: Uri,
        onProgress: (DownloadProgress) -> Unit = {},
    ): Result<InstalledModel> = withContext(Dispatchers.IO) {
        runCatching {
            validateIdentity(spec.id, spec.displayName, spec.sha256)
            validateExpectedBytes(spec.expectedBytes)
            modelDir.mkdirs()
            val partialFile = File(modelDir, "${spec.id}.gguf.part")
            if (partialFile.exists()) check(partialFile.delete()) { "Could not replace previous import staging file" }
            val providerLength = contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.length.takeIf { it >= 0 }
            }
            val total = spec.expectedBytes ?: providerLength
            validateExpectedBytes(total)
            ensureStorageAvailable(total)
            val limit = transferLimit(total, base = 0L)
            contentResolver.openInputStream(uri)?.use { input ->
                try {
                    streamToPartial(
                        input,
                        partialFile,
                        append = false,
                        base = 0,
                        total = total,
                        maxTotalBytes = limit,
                        onProgress = onProgress,
                    )
                } catch (error: StreamLimitExceededException) {
                    partialFile.delete()
                    throw error
                }
            } ?: error("Android could not open the selected model file")
            verifyAndActivate(
                id = spec.id,
                displayName = spec.displayName,
                expectedSha256 = spec.sha256,
                expectedBytes = spec.expectedBytes,
                partialFile = partialFile,
                sourceLabel = LOCAL_IMPORT_SOURCE,
            )
        }
    }

    suspend fun resolve(model: InstalledModel): File = withContext(Dispatchers.IO) {
        File(modelDir, model.fileName).also { check(it.isFile) { "Model file is missing" } }
    }

    suspend fun exportModel(
        model: InstalledModel,
        contentResolver: ContentResolver,
        destination: Uri,
        onProgress: (DownloadProgress) -> Unit = {},
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val source = File(modelDir, model.fileName)
            check(source.isFile && source.length() == model.bytes) { "Installed model file is missing or changed" }
            val digest = MessageDigest.getInstance("SHA-256")
            contentResolver.openOutputStream(destination, "w")?.buffered()?.use { output ->
                source.inputStream().buffered().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    var lastReport = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        copied += count
                        if (copied - lastReport >= PROGRESS_STEP_BYTES) {
                            onProgress(DownloadProgress(copied, model.bytes))
                            lastReport = copied
                        }
                    }
                    output.flush()
                    check(copied == model.bytes) { "Retained model copy is incomplete" }
                    onProgress(DownloadProgress(copied, model.bytes))
                }
            } ?: error("Android could not open the selected model destination")
            val streamedDigest = digest.digest().joinToString("") { "%02x".format(it) }
            check(streamedDigest == model.sha256) { "Installed model changed during export" }
            val retainedDigest = contentResolver.openInputStream(destination)?.buffered()?.use { input -> sha256(input) }
                ?: error("Android could not reopen the retained model copy for verification")
            check(retainedDigest == model.sha256) { "Retained model copy failed SHA-256 verification" }
        }
    }

    suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        val models = readRegistry()
        val target = models.firstOrNull { it.id == id } ?: return@withContext false
        File(modelDir, target.fileName).delete()
        File(modelDir, "${target.id}.gguf.part").delete()
        writeRegistry(models.filterNot { it.id == id })
        true
    }

    private fun ensureStorageAvailable(requiredBytes: Long?) {
        val available = StatFs(modelDir.absolutePath).availableBytes
        val required = requiredBytes?.let {
            if (it > Long.MAX_VALUE - STORAGE_RESERVE_BYTES) Long.MAX_VALUE else it + STORAGE_RESERVE_BYTES
        } ?: STORAGE_RESERVE_BYTES
        check(available >= required) {
            "Not enough storage: ${required / 1_048_576} MB required including safety reserve, " +
                "${available / 1_048_576} MB available"
        }
    }

    /** Maximum final staging size, preserving the storage reserve even when length metadata is absent. */
    private fun transferLimit(expectedBytes: Long?, base: Long): Long {
        validateExpectedBytes(expectedBytes)
        expectedBytes?.let { return it }
        val available = StatFs(modelDir.absolutePath).availableBytes
        val writable = (available - STORAGE_RESERVE_BYTES).coerceAtLeast(0L)
        check(writable > 0L) { "Not enough storage while preserving the safety reserve" }
        val storageBound = if (base > Long.MAX_VALUE - writable) Long.MAX_VALUE else base + writable
        return minOf(storageBound, MAX_MODEL_BYTES).also {
            check(it >= base) { "Existing model staging file exceeds the transfer limit" }
        }
    }

    private fun validateExpectedBytes(expectedBytes: Long?) {
        expectedBytes?.let {
            require(it in MIN_MODEL_BYTES..MAX_MODEL_BYTES) {
                "Model size must be between $MIN_MODEL_BYTES and $MAX_MODEL_BYTES bytes"
            }
        }
    }

    private fun streamToPartial(
        input: InputStream,
        partialFile: File,
        append: Boolean,
        base: Long,
        total: Long?,
        maxTotalBytes: Long,
        onProgress: (DownloadProgress) -> Unit,
    ) {
        FileOutputStream(partialFile, append).buffered().use { output ->
            copyBounded(
                input = input,
                output = output,
                base = base,
                maxTotalBytes = maxTotalBytes,
            ) { transferred ->
                onProgress(DownloadProgress(transferred, total))
            }
            output.flush()
        }
    }

    private fun verifyAndActivate(
        id: String,
        displayName: String,
        expectedSha256: String,
        expectedBytes: Long?,
        partialFile: File,
        sourceLabel: String,
    ): InstalledModel {
        val digest = sha256(partialFile)
        check(digest == expectedSha256.lowercase()) { "SHA-256 mismatch; staged file was not activated" }
        expectedBytes?.let { expected ->
            check(partialFile.length() == expected) {
                "Artifact size mismatch: expected $expected, received ${partialFile.length()}"
            }
        }
        check(partialFile.length() > 4) { "Selected model is empty" }
        FileInputStream(partialFile).use { stream ->
            val magic = ByteArray(4)
            check(stream.read(magic) == 4 && magic.contentEquals("GGUF".encodeToByteArray())) {
                "Selected file is not a GGUF model"
            }
        }
        val finalFile = File(modelDir, "$id.gguf")
        if (finalFile.exists()) check(finalFile.delete()) { "Could not replace existing model" }
        check(partialFile.renameTo(finalFile)) { "Could not atomically activate model file" }
        val installed = InstalledModel(
            id = id,
            displayName = displayName,
            fileName = finalFile.name,
            bytes = finalFile.length(),
            sha256 = digest,
            sourceUrl = sourceLabel,
            installedAtEpochMs = System.currentTimeMillis(),
        )
        writeRegistry(readRegistry().filterNot { it.id == installed.id } + installed)
        return installed
    }

    private fun validateDownload(spec: ModelSpec) {
        val expectedDigest = requireNotNull(spec.sha256) { "A reviewed SHA-256 is required" }
        validateIdentity(spec.id, spec.displayName, expectedDigest)
        validateExpectedBytes(spec.expectedBytes)
        reviewArtifactNetwork(spec.url, expectedDigest)
    }

    private fun validateIdentity(id: String, displayName: String, sha256: String) {
        require(ID.matches(id)) { "Model id must contain lowercase letters, numbers, dot, underscore, or dash" }
        require(displayName.isNotBlank()) { "Display name is required" }
        require(sha256.matches(SHA256)) { "Invalid SHA-256" }
    }

    private fun reviewArtifactNetwork(url: String, sha256: String) {
        val decision = networkPolicy.review(
            NetworkRequest(
                direction = DataFlowDirection.INBOUND,
                purpose = NetworkPurpose.MODEL_ARTIFACT,
                dataClass = DataClass.PUBLIC_ARTIFACT,
                url = url,
                explicitUserAction = true,
                expectedSha256 = sha256,
            ),
        )
        require(decision is NetworkDecision.Allow) { (decision as NetworkDecision.Deny).reason }
    }

    private fun readRegistry(): List<InstalledModel> = runCatching {
        if (!registryFile.exists()) emptyList()
        else LaiJson.decodeFromString(ListSerializer(InstalledModel.serializer()), registryFile.readText())
    }.getOrDefault(emptyList())

    private fun writeRegistry(models: List<InstalledModel>) {
        val temporary = File(modelDir, "registry.json.tmp")
        temporary.writeText(LaiJson.encodeToString(ListSerializer(InstalledModel.serializer()), models))
        if (registryFile.exists()) check(registryFile.delete())
        check(temporary.renameTo(registryFile))
    }

    private fun sha256(file: File): String = file.inputStream().buffered().use { input -> sha256(input) }

    private fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        private const val LOCAL_IMPORT_SOURCE = "local-import"
        private const val STORAGE_RESERVE_BYTES = 256L * 1024L * 1024L
        private const val MIN_MODEL_BYTES = 5L
        private const val MAX_MODEL_BYTES = 8L * 1024L * 1024L * 1024L
        private val ID = Regex("^[a-z0-9][a-z0-9._-]{1,63}$")
        private val SHA256 = Regex("^[a-fA-F0-9]{64}$")
    }
}

internal class StreamLimitExceededException(message: String) : IOException(message)

/**
 * Copies an untrusted stream without ever writing beyond [maxTotalBytes]. The bound is checked
 * before each write, so a missing or deceptive length cannot consume the device storage reserve.
 */
internal fun copyBounded(
    input: InputStream,
    output: OutputStream,
    base: Long,
    maxTotalBytes: Long,
    progressStepBytes: Long = 512L * 1024L,
    onProgress: (Long) -> Unit = {},
): Long {
    require(base >= 0L) { "Base byte count must not be negative" }
    require(maxTotalBytes >= base) { "Transfer limit must include existing bytes" }
    require(progressStepBytes > 0L) { "Progress step must be positive" }

    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var transferred = base
    var lastReport = base
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        if (read == 0) continue
        if (read.toLong() > maxTotalBytes - transferred) {
            throw StreamLimitExceededException("Model stream exceeded the $maxTotalBytes-byte transfer limit")
        }
        output.write(buffer, 0, read)
        transferred += read
        if (transferred - lastReport >= progressStepBytes) {
            onProgress(transferred)
            lastReport = transferred
        }
    }
    onProgress(transferred)
    return transferred
}
