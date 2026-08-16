package dev.lai.runtime.ocr

import kotlinx.serialization.Serializable

@Serializable
data class OcrRequest(
    val languages: List<String> = listOf("bn", "en"),
    val includeConfidence: Boolean = true,
)

@Serializable
data class OcrPoint(val x: Int, val y: Int)

@Serializable
data class OcrBlock(
    val text: String,
    val language: String?,
    val confidence: Float?,
    val polygon: List<OcrPoint>,
    val handwritten: Boolean? = null,
)

@Serializable
data class OcrResult(
    val schemaVersion: Int = 1,
    val fullText: String,
    val blocks: List<OcrBlock>,
    val processingTimeMs: Long,
    val engine: String,
    val warning: String? = null,
)
