package dev.lai.runtime.inference

import kotlinx.coroutines.flow.Flow

data class ProviderDescriptor(
    val id: String,
    val backends: Set<BackendDescriptor>,
    val evidence: ProviderEvidence,
    val networkCapable: Boolean = false,
)

data class InferenceProvenance(
    val providerId: String,
    val requestedBackend: BackendId?,
    val actualBackend: BackendId,
    val evidence: ProviderEvidence,
)

/**
 * Provider-neutral routing boundary. The gateway deliberately implements the stable
 * InferenceEngine contract so existing application callers can migrate without changing
 * streaming/cancellation semantics.
 */
class InferenceGateway(
    private val providers: List<ProviderRegistration>,
) : InferenceEngine {
    data class ProviderRegistration(
        val descriptor: ProviderDescriptor,
        val engine: InferenceEngine,
    )

    @Volatile
    var lastProvenance: InferenceProvenance? = null
        private set

    override val contextSize: Int
        get() = selectedProvider(null)?.engine?.contextSize
            ?: providers.firstOrNull()?.engine?.contextSize
            ?: 0

    override val capabilities: RuntimeCapabilities
        get() {
            val measured = providers
                .filter { it.descriptor.evidence == ProviderEvidence.MEASURED }
                .flatMap { it.descriptor.backends }
                .toSet()
            val details = if (measured.isEmpty()) {
                "No measured inference provider is registered"
            } else {
                measured.joinToString(", ") { it.id.value }
            }
            return RuntimeCapabilities(
                nativeLibraryLoaded = providers.any { it.engine.capabilities.nativeLibraryLoaded },
                compiledBackends = measured,
                detail = details,
            )
        }

    override suspend fun load(modelPath: String, backend: BackendId?): Result<Unit> {
        val provider = selectedProvider(backend)
            ?: return Result.failure(IllegalArgumentException("No measured provider supports backend ${backend?.value ?: "auto"}"))
        val actual = backend ?: provider.descriptor.backends.minByOrNull { it.defaultPriority }?.id
            ?: return Result.failure(IllegalStateException("Provider has no backend"))
        if (provider.descriptor.evidence != ProviderEvidence.MEASURED) {
            return Result.failure(IllegalStateException("Provider ${provider.descriptor.id} is not measured"))
        }
        val result = provider.engine.load(modelPath, actual)
        if (result.isSuccess) {
            lastProvenance = InferenceProvenance(
                providerId = provider.descriptor.id,
                requestedBackend = backend,
                actualBackend = actual,
                evidence = provider.descriptor.evidence,
            )
        }
        return result
    }

    override fun generate(
        conversation: List<ConversationMessage>,
        config: GenerationConfig,
    ): Flow<InferenceEvent> = selectedProvider(lastProvenance?.actualBackend)?.engine?.generate(conversation, config)
        ?: kotlinx.coroutines.flow.flowOf(InferenceEvent.Failed("No measured inference provider is loaded"))

    override suspend fun countTokens(conversation: List<ConversationMessage>): Result<Int> =
        selectedProvider(lastProvenance?.actualBackend)?.engine?.countTokens(conversation)
            ?: Result.failure(IllegalStateException("No measured inference provider is loaded"))

    override fun close() {
        providers.forEach { it.engine.close() }
        lastProvenance = null
    }

    private fun selectedProvider(backend: BackendId?): ProviderRegistration? =
        providers.firstOrNull { registration ->
            registration.descriptor.evidence == ProviderEvidence.MEASURED &&
                (backend == null || registration.descriptor.backends.any { it.id == backend })
        }
}
