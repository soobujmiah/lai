package dev.lai.runtime.ocr

/** Stable Tesseract model ordering, independent of caller order or duplicate hints. */
internal object OcrLanguagePolicy {
    fun select(request: OcrRequest): List<Pair<String, String>> {
        val requested = request.languages.toSet()
        require(requested.isNotEmpty() && requested.all { it == "bn" || it == "en" }) {
            "Only Bangla (bn) and English (en) OCR are bundled"
        }
        return buildList {
            if ("bn" in requested) add("bn" to "ben")
            if ("en" in requested) add("en" to "eng")
        }
    }
}
