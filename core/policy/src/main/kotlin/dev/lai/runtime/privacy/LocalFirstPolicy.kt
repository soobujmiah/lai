package dev.lai.runtime.privacy

import java.net.URI

enum class DataFlowDirection { INBOUND, OUTBOUND }
enum class NetworkPurpose { MODEL_ARTIFACT, REVIEWED_CATALOG }
enum class DataClass { PUBLIC_ARTIFACT, PUBLIC_CATALOG, USER_CONTENT, SCREEN_CONTENT, GENERATED_CONTENT, CREDENTIAL }

data class NetworkRequest(
    val direction: DataFlowDirection,
    val purpose: NetworkPurpose,
    val dataClass: DataClass,
    val url: String,
    val explicitUserAction: Boolean,
    val expectedSha256: String? = null,
)

sealed interface NetworkDecision {
    data class Allow(val normalizedHost: String) : NetworkDecision
    data class Deny(val code: String, val reason: String) : NetworkDecision
}

/**
 * Product-wide network policy. LAI never authorizes user-derived outbound data.
 * The only current network path is an explicitly requested, digest-pinned model download.
 */
class LocalFirstPolicy(
    private val reviewedHostSuffixes: Set<String> = setOf("huggingface.co", "hf.co"),
) {
    fun review(request: NetworkRequest): NetworkDecision {
        if (request.direction == DataFlowDirection.OUTBOUND) {
            return NetworkDecision.Deny("OUTBOUND_DENIED", "LAI does not transmit data off-device")
        }
        if (request.dataClass !in setOf(DataClass.PUBLIC_ARTIFACT, DataClass.PUBLIC_CATALOG)) {
            return NetworkDecision.Deny("PRIVATE_DATA_NETWORK_DENIED", "Private or generated content cannot use the network")
        }
        if (!request.explicitUserAction) {
            return NetworkDecision.Deny("USER_ACTION_REQUIRED", "Network access requires an explicit user action")
        }
        val uri = runCatching { URI(request.url) }.getOrNull()
            ?: return NetworkDecision.Deny("INVALID_URL", "URL could not be parsed")
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            return NetworkDecision.Deny("HTTPS_REQUIRED", "Only HTTPS is allowed")
        }
        val host = uri.host?.lowercase()?.trimEnd('.')
            ?: return NetworkDecision.Deny("INVALID_HOST", "URL has no valid host")
        if (reviewedHostSuffixes.none { host == it || host.endsWith(".$it") }) {
            return NetworkDecision.Deny("HOST_NOT_REVIEWED", "Host is outside the reviewed artifact allowlist")
        }
        if (request.purpose == NetworkPurpose.MODEL_ARTIFACT &&
            request.expectedSha256?.matches(SHA256) != true
        ) {
            return NetworkDecision.Deny("DIGEST_REQUIRED", "Model downloads require a reviewed SHA-256")
        }
        return NetworkDecision.Allow(host)
    }

    companion object {
        private val SHA256 = Regex("^[a-fA-F0-9]{64}$")
    }
}
