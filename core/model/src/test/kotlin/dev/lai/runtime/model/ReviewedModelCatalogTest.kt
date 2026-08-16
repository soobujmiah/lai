package dev.lai.runtime.model

import dev.lai.runtime.core.LaiJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewedModelCatalogTest {
    @Test
    fun `recommended model is digest pinned but not falsely Bangla validated`() {
        val model = ReviewedModelCatalog.recommendedCpuBaseline
        assertEquals(64, model.sha256.length)
        assertTrue(ArtifactReviewState.METADATA_VERIFIED in model.reviewState)
        assertTrue(ArtifactReviewState.BUILD_COMPATIBLE in model.reviewState)
        assertFalse(ArtifactReviewState.DEVICE_VALIDATED in model.reviewState)
        assertFalse(model.banglaQualityValidated)
        assertEquals(model.sha256, model.toModelSpec().sha256)
        assertEquals(model.bytes, model.toModelSpec().expectedBytes)
        assertEquals(model.sha256, model.toImportSpec().sha256)
        assertEquals(model.bytes, model.toImportSpec().expectedBytes)
        val encoded = LaiJson.encodeToString(
            ReviewedModelCatalogDocument.serializer(),
            ReviewedModelCatalog.embeddedDocument,
        )
        val decoded = LaiJson.decodeFromString(ReviewedModelCatalogDocument.serializer(), encoded)
        assertEquals(ReviewedModelCatalog.embeddedDocument, decoded)
    }
}
