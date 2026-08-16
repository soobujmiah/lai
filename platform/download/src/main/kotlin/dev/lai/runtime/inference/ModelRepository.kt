package dev.lai.runtime.inference

import android.content.Context
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
            validate(spec)
            modelDir.mkdirs()
            val finalFile = File(modelDir, "${spec.id}.gguf")
            val partialFile = File(modelDir, "${spec.id}.gguf.part")
            val existingBytes = partialFile.takeIf(File::exists)?.length() ?: 0L
            val request = Request.Builder()
                .url(spec.url)
                .header("User-Agent", "LAI-Android/0.1")
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
                    java.io.FileOutputStream(partialFile, append).buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var downloaded = base
                        var lastReport = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (downloaded - lastReport >= PROGRESS_STEP_BYTES) {
                                onProgress(DownloadProgress(downloaded, total))
                                lastReport = downloaded
                            }
                        }
                        output.flush()
                        onProgress(DownloadProgress(downloaded, total))
                    }
                } ?: error("Server returned an empty body")
            }

            val digest = sha256(partialFile)
            val expected = requireNotNull(spec.sha256).lowercase()
            check(digest == expected) { "SHA-256 mismatch; partial file retained for inspection" }
            check(partialFile.length() > 4) { "Downloaded model is empty" }
            FileInputStream(partialFile).use { stream ->
                val magic = ByteArray(4)
                check(stream.read(magic) == 4 && magic.contentEquals("GGUF".encodeToByteArray())) {
                    "Downloaded file is not a GGUF model"
                }
            }
            if (finalFile.exists()) check(finalFile.delete()) { "Could not replace existing model" }
            check(partialFile.renameTo(finalFile)) { "Could not finalize model file" }

            val installed = InstalledModel(
                id = spec.id,
                displayName = spec.displayName,
                fileName = finalFile.name,
                bytes = finalFile.length(),
                sha256 = digest,
                sourceUrl = spec.url,
                installedAtEpochMs = System.currentTimeMillis(),
            )
            writeRegistry(readRegistry().filterNot { it.id == installed.id } + installed)
            installed
        }
    }

    suspend fun resolve(model: InstalledModel): File = withContext(Dispatchers.IO) {
        File(modelDir, model.fileName).also { check(it.isFile) { "Model file is missing" } }
    }

    suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        val models = readRegistry()
        val target = models.firstOrNull { it.id == id } ?: return@withContext false
        File(modelDir, target.fileName).delete()
        File(modelDir, "${target.id}.gguf.part").delete()
        writeRegistry(models.filterNot { it.id == id })
        true
    }

    private fun validate(spec: ModelSpec) {
        require(ID.matches(spec.id)) { "Model id must contain lowercase letters, numbers, dot, underscore, or dash" }
        require(spec.displayName.isNotBlank()) { "Display name is required" }
        val expectedDigest = requireNotNull(spec.sha256) { "A reviewed SHA-256 is required" }
        require(expectedDigest.matches(SHA256)) { "Invalid SHA-256" }
        reviewArtifactNetwork(spec.url, expectedDigest)
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
        require(decision is NetworkDecision.Allow) {
            (decision as NetworkDecision.Deny).reason
        }
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

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        private val ID = Regex("^[a-z0-9][a-z0-9._-]{1,63}$")
        private val SHA256 = Regex("^[a-fA-F0-9]{64}$")
        private const val PROGRESS_STEP_BYTES = 512L * 1024L
    }
}
