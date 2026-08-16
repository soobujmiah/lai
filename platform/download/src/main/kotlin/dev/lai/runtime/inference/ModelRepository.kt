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
            val existingBytes = partialFile.takeIf(File::exists)?.length() ?: 0L
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
                val total = response.body?.contentLength()?.takeIf { it >= 0 }?.plus(base)
                    ?: spec.expectedBytes
                response.body?.byteStream()?.use { input ->
                    streamToPartial(input, partialFile, append, base, total, onProgress)
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
            modelDir.mkdirs()
            val partialFile = File(modelDir, "${spec.id}.gguf.part")
            ensureStorageAvailable(spec.expectedBytes)
            if (partialFile.exists()) check(partialFile.delete()) { "Could not replace previous import staging file" }
            val providerLength = contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.length.takeIf { it >= 0 }
            }
            val total = spec.expectedBytes ?: providerLength
            contentResolver.openInputStream(uri)?.use { input ->
                streamToPartial(input, partialFile, append = false, base = 0, total = total, onProgress = onProgress)
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

    private fun streamToPartial(
        input: InputStream,
        partialFile: File,
        append: Boolean,
        base: Long,
        total: Long?,
        onProgress: (DownloadProgress) -> Unit,
    ) {
        FileOutputStream(partialFile, append).buffered().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var transferred = base
            var lastReport = base
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
                transferred += read
                if (transferred - lastReport >= PROGRESS_STEP_BYTES) {
                    onProgress(DownloadProgress(transferred, total))
                    lastReport = transferred
                }
            }
            output.flush()
            onProgress(DownloadProgress(transferred, total))
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
        private val ID = Regex("^[a-z0-9][a-z0-9._-]{1,63}$")
        private val SHA256 = Regex("^[a-fA-F0-9]{64}$")
        private const val PROGRESS_STEP_BYTES = 512L * 1024L
    }
}
