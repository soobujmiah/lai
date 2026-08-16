package dev.lai.runtime.ocr

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface OcrEngine {
    val id: String
    suspend fun recognize(bitmap: Bitmap, request: OcrRequest): Result<OcrResult>
}

/**
 * Stable OCR boundary. A future downloadable TFLite/QNN plugin can replace the
 * placeholder without changing UI or tool-calling code.
 */
class BanglaOcrService(private val engine: OcrEngine = PlaceholderBanglaOcrEngine()) {
    suspend fun recognize(bitmap: Bitmap, request: OcrRequest = OcrRequest()): Result<OcrResult> =
        withContext(Dispatchers.Default) { engine.recognize(bitmap, request) }
}

class PlaceholderBanglaOcrEngine : OcrEngine {
    override val id: String = "bangla-ocr-placeholder-v1"

    override suspend fun recognize(bitmap: Bitmap, request: OcrRequest): Result<OcrResult> =
        Result.failure(
            OcrModelRequiredException(
                "Bangla OCR model is not installed. The capture and JSON pipeline is ready; " +
                    "install a licensed OCR plugin before recognition.",
            ),
        )
}

class OcrModelRequiredException(message: String) : IllegalStateException(message)
