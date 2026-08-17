package dev.lai.runtime.agent

/**
 * Decides whether the tool-proposal instruction is worth its prefill cost for one request.
 *
 * Field data (device reports `0.6.78`–`0.6.84`): while proposals were enabled the full tool
 * instruction was prepended to *every* message, so even "hi" paid a several-hundred-token prompt
 * prefill — many seconds of CPU work before the first streamed token on 4 threads. The
 * instruction only earns that cost when the user's text plausibly asks LAI to operate the
 * device, so inclusion is now gated on the latest user message.
 *
 * The heuristic is deliberately recall-biased. A false positive costs prefill time only; a false
 * negative means the model answers in plain text and the user can rephrase with an action verb.
 * Nothing here grants or removes authority: proposals are still parsed, validated twice, and
 * explicitly confirmed by the user before any dispatch, exactly as before.
 *
 * Pure JVM by design (core:policy) so it is unit-testable without Android.
 */
object ToolInstructionGate {

    /**
     * Returns true when [latestUserText] plausibly requests an Android action and the tool
     * instruction should therefore be included in the system prompt for this request.
     */
    fun shouldIncludeInstruction(latestUserText: String): Boolean {
        val text = latestUserText.trim()
        if (text.isEmpty()) return false
        if (ENGLISH_ACTION_WORDS.containsMatchIn(text)) return true
        return BANGLA_ACTION_STEMS.any { stem -> text.contains(stem) }
    }

    /**
     * English action verbs and device nouns, matched on word boundaries and case-insensitively,
     * so "open Settings." matches but "happened" does not.
     */
    private val ENGLISH_ACTION_WORDS = Regex(
        pattern = "\\b(" +
            "open|launch|start|run|click|tap|press|push|type|enter|write|fill|input|" +
            "scroll|swipe|screenshot|screen|read|back|home|notifications?|recents|" +
            "install|uninstall|stop|close|shut|turn|enable|disable|" +
            "settings?|wi-?fi|bluetooth|brightness|volume|apps?|application|ocr" +
            ")\\b",
        options = setOf(RegexOption.IGNORE_CASE),
    )

    /**
     * Bangla action stems, matched as substrings on purpose: Bangla verbs inflect by suffix
     * (খুলুন / খুলে / খোলো all contain খুল), so stems are inflection-tolerant where an exact-word
     * match would silently miss common requests.
     */
    private val BANGLA_ACTION_STEMS: List<String> = listOf(
        "খুল", "খোল", "চালু", "চালা", "চালিয়ে", "ক্লিক", "ট্যাপ", "চাপ", "টিপ",
        "টাইপ", "লেখ", "লিখ", "ঢোক", "ঢুক",
        "স্ক্রল", "সোয়াইপ", "স্ক্রিন", "স্ক্রীন", "স্ক্রিনশট",
        "পেছনে", "ব্যাক", "হোম", "নোটিফিকেশন",
        "ইনস্টল", "আনইনস্টল", "বন্ধ", "থামা",
        "সেটিংস", "সেটিং", "ওয়াইফাই", "ব্লুটুথ", "উজ্জ্বলতা", "ভলিউম",
        "অ্যাপ", "পড়ে", "পড়ো", "পড়ুন", "দেখাও", "দেখান", "দেখতে",
    )
}
