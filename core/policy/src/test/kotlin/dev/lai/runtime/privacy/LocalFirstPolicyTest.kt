package dev.lai.runtime.privacy

import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFirstPolicyTest {
    private val policy = LocalFirstPolicy()

    @Test
    fun `all outbound data is denied`() {
        val decision = policy.review(
            NetworkRequest(
                DataFlowDirection.OUTBOUND,
                NetworkPurpose.REVIEWED_CATALOG,
                DataClass.USER_CONTENT,
                "https://huggingface.co/upload",
                explicitUserAction = true,
            ),
        )
        assertTrue(decision is NetworkDecision.Deny)
    }

    @Test
    fun `explicit digest pinned Hugging Face artifact is allowed`() {
        val decision = policy.review(
            NetworkRequest(
                DataFlowDirection.INBOUND,
                NetworkPurpose.MODEL_ARTIFACT,
                DataClass.PUBLIC_ARTIFACT,
                "https://huggingface.co/org/model/resolve/main/model.gguf",
                explicitUserAction = true,
                expectedSha256 = "a".repeat(64),
            ),
        )
        assertTrue(decision is NetworkDecision.Allow)
    }

    @Test
    fun `explicit public signed catalog host is allowed by transport policy`() {
        val decision = policy.review(
            NetworkRequest(
                DataFlowDirection.INBOUND,
                NetworkPurpose.REVIEWED_CATALOG,
                DataClass.PUBLIC_CATALOG,
                "https://github.com/soobujmiah/lai/releases/download/catalog-v1/models-v1.json",
                explicitUserAction = true,
            ),
        )
        assertTrue(decision is NetworkDecision.Allow)
    }

    @Test
    fun `catalog cannot be fetched from a model host`() {
        val decision = policy.review(
            NetworkRequest(
                DataFlowDirection.INBOUND,
                NetworkPurpose.REVIEWED_CATALOG,
                DataClass.PUBLIC_CATALOG,
                "https://huggingface.co/catalog.json",
                explicitUserAction = true,
            ),
        )
        assertTrue(decision is NetworkDecision.Deny)
    }

    @Test
    fun `model without digest is denied`() {
        val decision = policy.review(
            NetworkRequest(
                DataFlowDirection.INBOUND,
                NetworkPurpose.MODEL_ARTIFACT,
                DataClass.PUBLIC_ARTIFACT,
                "https://huggingface.co/org/model/resolve/main/model.gguf",
                explicitUserAction = true,
            ),
        )
        assertTrue(decision is NetworkDecision.Deny)
    }
}
