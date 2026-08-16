package dev.lai.runtime.model

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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.concurrent.TimeUnit

enum class CatalogSource { EMBEDDED, VERIFIED_CACHE, SIGNED_WEB }

data class ModelCatalogSnapshot(
    val document: ReviewedModelCatalogDocument,
    val source: CatalogSource,
)

class RemoteModelCatalogRepository private constructor(
    context: Context,
    private val networkPolicy: LocalFirstPolicy,
    private val client: OkHttpClient,
) {
    constructor(context: Context) : this(context, LocalFirstPolicy(), defaultHttpClient())

    private val cacheDir = File(context.noBackupFilesDir, "catalog").apply { mkdirs() }
    private val cachedJson = File(cacheDir, "models-v1.json")
    private val cachedSignature = File(cacheDir, "models-v1.sig")

    suspend fun cachedOrEmbedded(): ModelCatalogSnapshot = withContext(Dispatchers.IO) {
        val embedded = ModelCatalogSnapshot(ReviewedModelCatalog.embeddedDocument, CatalogSource.EMBEDDED)
        val cached = runCatching {
            val json = cachedJson.readBytes()
            val signature = cachedSignature.readBytes()
            parseVerified(json, signature, CatalogSource.VERIFIED_CACHE)
        }.getOrNull()
        cached?.takeIf { it.document.revision >= embedded.document.revision } ?: embedded
    }

    suspend fun refresh(): Result<ModelCatalogSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            reviewCatalogUrl(CATALOG_URL)
            reviewCatalogUrl(SIGNATURE_URL)
            val json = fetchBounded(CATALOG_URL, MAX_CATALOG_BYTES)
            val signature = fetchBounded(SIGNATURE_URL, MAX_SIGNATURE_BYTES)
            val snapshot = parseVerified(json, signature, CatalogSource.SIGNED_WEB)
            check(snapshot.document.revision >= ReviewedModelCatalog.embeddedDocument.revision) {
                "Signed catalog revision is older than the embedded catalog"
            }
            writeCache(json, signature)
            snapshot
        }
    }

    private fun fetchBounded(url: String, limit: Int): ByteArray {
        val request = Request.Builder().url(url).header("User-Agent", "LAI-Android/0.6").build()
        return client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Catalog request failed with HTTP ${response.code}" }
            reviewCatalogUrl(response.request.url.toString())
            response.body?.contentLength()?.takeIf { it >= 0 }?.let {
                check(it <= limit) { "Catalog response exceeded its size limit" }
            }
            response.body?.byteStream()?.use { readBounded(it, limit) }
                ?: error("Catalog server returned an empty body")
        }
    }

    private fun readBounded(input: InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream(minOf(limit, 16 * 1024))
        val buffer = ByteArray(4 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            check(total <= limit) { "Catalog response exceeded its size limit" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun parseVerified(
        jsonBytes: ByteArray,
        signatureText: ByteArray,
        source: CatalogSource,
    ): ModelCatalogSnapshot {
        val signatureBytes = Base64.getMimeDecoder().decode(signatureText.toString(Charsets.US_ASCII).trim())
        val keyBytes = Base64.getDecoder().decode(CATALOG_PUBLIC_KEY_DER_BASE64)
        val publicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(keyBytes))
        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(publicKey)
        verifier.update(jsonBytes)
        check(verifier.verify(signatureBytes)) { "Catalog signature verification failed" }
        val document = LaiJson.decodeFromString(
            ReviewedModelCatalogDocument.serializer(),
            jsonBytes.toString(Charsets.UTF_8),
        )
        validateDocument(document)
        return ModelCatalogSnapshot(document, source)
    }

    private fun validateDocument(document: ReviewedModelCatalogDocument) {
        require(document.schemaVersion == 1) { "Unsupported catalog schema" }
        require(document.revision > 0 && document.generatedAt.isNotBlank()) { "Invalid catalog revision" }
        require(document.models.isNotEmpty() && document.models.size <= MAX_MODELS) { "Invalid catalog model count" }
        require(document.models.map { it.id }.distinct().size == document.models.size) { "Duplicate catalog model id" }
        document.models.forEach { model ->
            require(model.id.matches(MODEL_ID)) { "Invalid model id" }
            require(model.displayName.isNotBlank() && model.description.isNotBlank()) { "Incomplete model metadata" }
            require(model.sha256.matches(SHA256) && model.bytes > 0) { "Invalid artifact trust metadata" }
            require(model.modelFormat.isNotBlank() && model.contextSize in 256..131_072) {
                "Invalid model execution metadata"
            }
            require(model.estimatedPeakBytes >= model.bytes && model.requiredAbis.isNotEmpty()) {
                "Invalid model hardware metadata"
            }
            require(
                model.compatibleBackendIds.isNotEmpty() &&
                    model.compatibleBackendIds.distinct().size == model.compatibleBackendIds.size &&
                    model.compatibleBackendIds.all { it.matches(BACKEND_ID) } &&
                    model.preferredBackendId in model.compatibleBackendIds &&
                    model.fallbackBackendIds.distinct().size == model.fallbackBackendIds.size &&
                    model.fallbackBackendIds.all {
                        it in model.compatibleBackendIds && it != model.preferredBackendId
                    },
            ) { "Invalid model backend compatibility metadata" }
            val decision = networkPolicy.review(
                NetworkRequest(
                    direction = DataFlowDirection.INBOUND,
                    purpose = NetworkPurpose.MODEL_ARTIFACT,
                    dataClass = DataClass.PUBLIC_ARTIFACT,
                    url = model.url,
                    explicitUserAction = true,
                    expectedSha256 = model.sha256,
                ),
            )
            require(decision is NetworkDecision.Allow) { "Catalog contains a disallowed model host" }
        }
    }

    private fun reviewCatalogUrl(url: String) {
        val decision = networkPolicy.review(
            NetworkRequest(
                direction = DataFlowDirection.INBOUND,
                purpose = NetworkPurpose.REVIEWED_CATALOG,
                dataClass = DataClass.PUBLIC_CATALOG,
                url = url,
                explicitUserAction = true,
            ),
        )
        require(decision is NetworkDecision.Allow) {
            (decision as NetworkDecision.Deny).reason
        }
    }

    private fun writeCache(json: ByteArray, signature: ByteArray) {
        val jsonTemp = File(cacheDir, "models-v1.json.tmp")
        val signatureTemp = File(cacheDir, "models-v1.sig.tmp")
        jsonTemp.writeBytes(json)
        signatureTemp.writeBytes(signature)
        if (cachedJson.exists()) check(cachedJson.delete())
        if (cachedSignature.exists()) check(cachedSignature.delete())
        check(jsonTemp.renameTo(cachedJson))
        check(signatureTemp.renameTo(cachedSignature))
    }

    companion object {
        private const val CATALOG_URL =
            "https://github.com/soobujmiah/lai/releases/download/catalog-v1/models-v1.json"
        private const val SIGNATURE_URL =
            "https://github.com/soobujmiah/lai/releases/download/catalog-v1/models-v1.sig"
        private const val CATALOG_PUBLIC_KEY_DER_BASE64 =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE9XrZRI85NEmqNWxyWn73nKK39Fv6IGNQ4N2v2KpyHNBxK4vNsvhlZmjsJpiY42wBD3MWJST126YFRpfaFUOwgQ=="
        private const val MAX_CATALOG_BYTES = 512 * 1024
        private const val MAX_SIGNATURE_BYTES = 16 * 1024
        private const val MAX_MODELS = 100
        private val MODEL_ID = Regex("^[a-z0-9][a-z0-9._-]{1,63}$")
        private val BACKEND_ID = Regex("^[a-z0-9][a-z0-9._-]{1,63}$")
        private val SHA256 = Regex("^[a-f0-9]{64}$")

        private fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
}
