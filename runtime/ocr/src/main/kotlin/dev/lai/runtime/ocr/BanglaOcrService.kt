package dev.lai.runtime.ocr

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer
import java.nio.file.Files
import java.nio.file.StandardCopyOption

interface OcrEngine {
    val id: String
    suspend fun recognize(bitmap: Bitmap, request: OcrRequest): Result<OcrResult>
}

/** Offline OCR over the user-authorized, bundled Apache-2.0 Bengali/English models. */
class BanglaOcrService(context: Context, engine: OcrEngine = TesseractBanglaOcrEngine(context)) {
    private val selectedEngine = engine

    suspend fun recognize(bitmap: Bitmap, request: OcrRequest = OcrRequest()): Result<OcrResult> =
        withContext(Dispatchers.Default) { selectedEngine.recognize(bitmap, request) }
}

class TesseractBanglaOcrEngine(private val context: Context) : OcrEngine {
    override val id: String = "tesseract-fast-ben-eng-v1"

    override suspend fun recognize(bitmap: Bitmap, request: OcrRequest): Result<OcrResult> =
        withContext(Dispatchers.Default) {
            runCatching {
                require(!bitmap.isRecycled) { "OCR image is no longer available" }
                require(bitmap.width.toLong() * bitmap.height <= MAX_PIXELS) { "OCR image exceeds the pixel limit" }
                val languages = OcrLanguagePolicy.select(request)
                val models = languages.map { if (it.first == "bn") BENGALI else ENGLISH }
                val dataDir = ensureModels(models)
                val start = SystemClock.elapsedRealtime()
                val tess = TessBaseAPI()
                try {
                    check(tess.init(dataDir.absolutePath, models.joinToString("+") { it.tessLanguage })) {
                        "Bundled OCR model could not be initialized"
                    }
                    tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
                    tess.setImage(bitmap)
                    val fullText = Normalizer.normalize(tess.getUTF8Text().orEmpty(), Normalizer.Form.NFC)
                    val lineLevel = TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE
                    val blocks = buildList {
                        val iterator = tess.resultIterator
                        if (iterator != null) {
                            try {
                                iterator.begin()
                                do {
                                    val line = Normalizer.normalize(
                                        iterator.getUTF8Text(lineLevel).orEmpty().trim(),
                                        Normalizer.Form.NFC,
                                    )
                                    if (line.isNotEmpty()) {
                                        val bounds = iterator.getBoundingRect(lineLevel)
                                        add(OcrBlock(
                                            text = line,
                                            language = if (languages.size == 1) languages.first().first else null,
                                            confidence = if (request.includeConfidence) {
                                                (iterator.confidence(lineLevel) / 100f).coerceIn(0f, 1f)
                                            } else null,
                                            polygon = listOf(
                                                OcrPoint(bounds.left, bounds.top),
                                                OcrPoint(bounds.right, bounds.top),
                                                OcrPoint(bounds.right, bounds.bottom),
                                                OcrPoint(bounds.left, bounds.bottom),
                                            ),
                                        ))
                                    }
                                } while (iterator.next(lineLevel))
                            } finally {
                                iterator.delete()
                            }
                        }
                    }
                    OcrResult(
                        fullText = fullText,
                        blocks = blocks,
                        processingTimeMs = SystemClock.elapsedRealtime() - start,
                        engine = id,
                        warning = "Printed-text OCR candidate; handwriting and accuracy on this device are not validated",
                    )
                } finally {
                    tess.recycle()
                }
            }
        }

    private fun ensureModels(models: List<Model>): File = synchronized(MODEL_LOCK) {
        val base = File(context.filesDir, "ocr/tesseract")
        val data = File(base, "tessdata")
        check(data.isDirectory || data.mkdirs()) { "Cannot create private OCR model directory" }
        for (model in models) {
            val target = File(data, "${model.tessLanguage}.traineddata")
            if (target.isFile && sha256(target) == model.sha256) continue
            val temporary = File(data, ".${model.tessLanguage}.traineddata.tmp")
            try {
                context.assets.open("tessdata/${model.tessLanguage}.traineddata").use { input ->
                    temporary.outputStream().use { output -> input.copyTo(output) }
                }
                check(sha256(temporary) == model.sha256) { "Bundled OCR model integrity check failed" }
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } finally {
                temporary.delete()
            }
        }
        base
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private data class Model(val tessLanguage: String, val sha256: String)

    private companion object {
        const val MAX_PIXELS = 16_000_000L
        val MODEL_LOCK = Any()
        val BENGALI = Model("ben", "31163084c279aaebd376216f0c3d5c17ad4b5fee8db49dae79c20000b5de5964")
        val ENGLISH = Model("eng", "7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2")
    }
}
