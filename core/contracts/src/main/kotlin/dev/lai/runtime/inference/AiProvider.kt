package dev.lai.runtime.inference

/** Local/remote classification used by routing and egress policy. */
enum class ProviderKind { LOCAL, REMOTE }

enum class ProviderEvidence { UNQUALIFIED, MEASURED, FAILED }

enum class NetworkPolicy { LOCAL_ONLY, CLOUD_ONLY, PREFER_LOCAL, PREFER_CLOUD, AUTO }

data class AiProviderDescriptor(
    val id: String,
    val kind: ProviderKind,
    val backends: Set<BackendDescriptor> = emptySet(),
    val evidence: ProviderEvidence,
    val networkRequired: Boolean,
)

/** Stable application-facing provider contract. Provider SDK/native details stay behind adapters. */
interface AiProvider {
    val descriptor: AiProviderDescriptor
    val inference: InferenceEngine
}

/** Explicit routing request. A requested provider/backend is never silently replaced. */
data class AiRouteRequest(
    val providerId: String? = null,
    val backendId: BackendId? = null,
    val networkPolicy: NetworkPolicy = NetworkPolicy.LOCAL_ONLY,
)

data class AiProvenance(
    val providerId: String,
    val providerKind: ProviderKind,
    val requestedProviderId: String?,
    val requestedBackendId: BackendId?,
    val actualBackendId: BackendId?,
    val evidence: ProviderEvidence,
    val fallbackApplied: Boolean,
)

/**
 * Provider registry/router. It does not perform implicit cloud fallback and never treats
 * discovery/presence as qualification.
 */
class AiGateway(private val providers: List<AiProvider>) {
    @Volatile
    var lastProvenance: AiProvenance? = null
        private set

    fun select(request: AiRouteRequest): Result<AiProvider> {
        val candidates = providers.filter { it.descriptor.evidence == ProviderEvidence.MEASURED }
        val policyCandidates = when (request.networkPolicy) {
            NetworkPolicy.LOCAL_ONLY -> candidates.filter { it.descriptor.kind == ProviderKind.LOCAL }
            NetworkPolicy.CLOUD_ONLY -> candidates.filter { it.descriptor.kind == ProviderKind.REMOTE }
            NetworkPolicy.PREFER_LOCAL, NetworkPolicy.AUTO -> candidates.sortedBy { it.descriptor.kind != ProviderKind.LOCAL }
            NetworkPolicy.PREFER_CLOUD -> candidates.sortedBy { it.descriptor.kind != ProviderKind.REMOTE }
        }
        val provider = policyCandidates.firstOrNull { candidate ->
            (request.providerId == null || candidate.descriptor.id == request.providerId) &&
                (request.backendId == null || candidate.descriptor.backends.any { it.id == request.backendId })
        } ?: return Result.failure(
            IllegalStateException(
                "No measured provider satisfies provider=${request.providerId ?: "auto"}, " +
                    "backend=${request.backendId?.value ?: "auto"}, policy=${request.networkPolicy}",
            ),
        )
        return Result.success(provider)
    }

    fun recordSuccess(request: AiRouteRequest, provider: AiProvider, actualBackendId: BackendId?) {
        lastProvenance = AiProvenance(
            providerId = provider.descriptor.id,
            providerKind = provider.descriptor.kind,
            requestedProviderId = request.providerId,
            requestedBackendId = request.backendId,
            actualBackendId = actualBackendId,
            evidence = provider.descriptor.evidence,
            fallbackApplied = request.providerId != null && request.providerId != provider.descriptor.id,
        )
    }
}
