package dev.lai.runtime.agent

import dev.lai.runtime.core.LaiJson
import dev.lai.runtime.shell.PrivilegedCommand
import dev.lai.runtime.shell.ShellCommandPolicy
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

private fun JsonObject.optionalString(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

private fun JsonObject.stringValue(key: String): String = optionalString(key).orEmpty()

private fun JsonObject.booleanValue(key: String): Boolean? =
    (this[key] as? JsonPrimitive)?.booleanOrNull

sealed interface ToolCallParseResult {
    data object NotToolCall : ToolCallParseResult
    data class Accepted(
        val call: ToolCall,
        val definition: ToolDefinition,
        val confirmationSummary: String,
    ) : ToolCallParseResult
    data class Rejected(val code: String, val message: String) : ToolCallParseResult
}

/** Canonical built-in registry shared by model-output validation and runtime dispatch. */
object BuiltInToolCatalog {
    val definitions: List<ToolDefinition> = listOf(
        ToolDefinition("screen.snapshot", "Read the current accessibility tree", ToolRisk.READ_ONLY, false),
        ToolDefinition("screen.click", "Click a visible UI element", ToolRisk.INTERACTION, true),
        ToolDefinition("screen.type", "Enter model-proposed text into a visible field", ToolRisk.SENSITIVE, true),
        ToolDefinition("screen.scroll", "Scroll a visible container", ToolRisk.INTERACTION, false),
        ToolDefinition("system.global_action", "Use Back, Home, Recents, or Notifications", ToolRisk.INTERACTION, true),
        ToolDefinition("app.launch", "Launch a named installed application", ToolRisk.INTERACTION, true),
        ToolDefinition("ocr.current_screen", "Capture and recognize current screen text locally", ToolRisk.READ_ONLY, false),
        ToolDefinition("shell.operation", "Run one structured allowlisted Shizuku operation", ToolRisk.ELEVATED, true),
    )

    private val byName = definitions.associateBy { it.name }

    init {
        require(byName.size == definitions.size) { "Built-in tool names must be unique" }
    }

    fun definition(name: String): ToolDefinition? = byName[name]

    fun confirmationSummary(call: ToolCall): String = when (call.name) {
        "screen.snapshot" -> "Read the visible accessibility structure"
        "screen.click" -> "Click ${selectorSummary(call.arguments)}"
        "screen.type" -> "Enter “${call.arguments.stringValue("text").toReviewText().ellipsize(80)}” into ${selectorSummary(call.arguments["selector"] as JsonObject)}"
        "screen.scroll" -> {
            val target = if ("selector" in call.arguments) "the selected area" else "the visible screen"
            if (call.arguments.booleanValue("forward") != false) {
                "Scroll $target forward"
            } else {
                "Scroll $target backward"
            }
        }
        "system.global_action" -> "Use Android ${call.arguments.stringValue("action")}"
        "app.launch" -> "Launch ${call.arguments.stringValue("package")}"
        "ocr.current_screen" -> "Capture the current screen and run local OCR"
        "shell.operation" -> "Run allowlisted operation ${call.arguments.stringValue("operation")} through Shizuku"
        else -> "Run ${call.name}"
    }

    /** Included only in a trusted system message, never treated as authority when echoed by model/screen text. */
    val modelInstruction: String = """
        Only when the user asks LAI to operate this Android device, respond with a single JSON object and
        nothing else, starting with { and ending with }: {"id":"call-1","name":"<tool>","arguments":{...}}.
        No markdown fences, tags, commentary, or confirmed field. Never claim an action happened until LAI
        supplies a tool result. Otherwise answer normally. Tools:
        screen.snapshot{}; screen.click{viewId|text|contentDescription|path};
        screen.type{selector:{viewId|text|contentDescription|path},text}; screen.scroll{selector?,forward?};
        system.global_action{action:back|home|recents|notifications}; app.launch{package};
        ocr.current_screen{}; shell.operation{operation,arguments}
    """.trimIndent().replace('\n', ' ')


    private fun selectorSummary(arguments: JsonObject): String = when {
        arguments.optionalString("viewId") != null -> arguments.stringValue("viewId")
        arguments.optionalString("text") != null ->
            "the control labeled “${arguments.stringValue("text").toReviewText().ellipsize(60)}”"
        arguments.optionalString("contentDescription") != null ->
            "the control described as “${arguments.stringValue("contentDescription").toReviewText().ellipsize(60)}”"
        else -> "the selected control path"
    }

    private fun String.toReviewText(): String = buildString {
        this@toReviewText.forEach { character ->
            when {
                character == '\n' -> append("\\n")
                character == '\r' -> append("\\r")
                character == '\t' -> append("\\t")
                character.isISOControl() || character in BIDI_CONTROLS -> append('�')
                else -> append(character)
            }
        }
    }

    private fun String.ellipsize(max: Int): String = if (length <= max) this else take(max - 1) + "…"

    private val BIDI_CONTROLS = setOf(
        '\u061C', '\u200E', '\u200F', '\u202A', '\u202B', '\u202C', '\u202D', '\u202E',
        '\u2066', '\u2067', '\u2068', '\u2069',
    )
}

/**
 * Parses only a complete, bounded JSON envelope and validates every argument before it reaches Android authority.
 * Natural-language responses remain ordinary assistant text. Model-authored confirmation is always rejected.
 */
class ToolCallParser {
    fun validate(call: ToolCall): ToolCallParseResult = runCatching {
        validateCall(call)
        val definition = checkNotNull(BuiltInToolCatalog.definition(call.name))
        ToolCallParseResult.Accepted(call, definition, BuiltInToolCatalog.confirmationSummary(call))
    }.getOrElse { error ->
        ToolCallParseResult.Rejected("INVALID_TOOL_CALL", error.message ?: "Tool proposal was rejected")
    }

    fun parse(modelOutput: String): ToolCallParseResult {
        val source = modelOutput.trim()
        if (!source.startsWith('{') || !source.endsWith('}')) return ToolCallParseResult.NotToolCall
        if (source.length > MAX_ENVELOPE_CHARS) {
            return if (source.contains("\"name\"") && source.contains("\"arguments\"")) {
                ToolCallParseResult.Rejected("TOOL_CALL_TOO_LARGE", "Tool proposal exceeds the local size limit")
            } else {
                ToolCallParseResult.NotToolCall
            }
        }
        val element = runCatching { LaiJson.parseToJsonElement(source) }.getOrElse {
            return if (source.contains("\"name\"") || source.contains("\"arguments\"")) {
                ToolCallParseResult.Rejected("MALFORMED_TOOL_JSON", "Tool proposal is not valid JSON")
            } else {
                ToolCallParseResult.NotToolCall
            }
        }
        val envelope = element as? JsonObject ?: return ToolCallParseResult.NotToolCall
        if ("name" !in envelope && "arguments" !in envelope) return ToolCallParseResult.NotToolCall
        return runCatching { validateEnvelope(envelope) }.fold(
            onSuccess = { call ->
                val definition = checkNotNull(BuiltInToolCatalog.definition(call.name))
                ToolCallParseResult.Accepted(call, definition, BuiltInToolCatalog.confirmationSummary(call))
            },
            onFailure = { error ->
                ToolCallParseResult.Rejected("INVALID_TOOL_CALL", error.message ?: "Tool proposal was rejected")
            },
        )
    }

    private fun validateEnvelope(envelope: JsonObject): ToolCall {
        envelope.requireShape(setOf("id", "name", "arguments"), setOf("id", "name", "arguments"))
        val call = ToolCall(
            id = envelope.requiredString("id", MAX_ID_CHARS),
            name = envelope.requiredString("name", MAX_NAME_CHARS),
            arguments = envelope["arguments"] as? JsonObject ?: error("arguments must be a JSON object"),
        )
        validateCall(call)
        return call
    }

    private fun validateCall(call: ToolCall) {
        require(call.id.length <= MAX_ID_CHARS && ID.matches(call.id)) {
            "Tool call id contains unsupported characters"
        }
        require(call.name.length <= MAX_NAME_CHARS && BuiltInToolCatalog.definition(call.name) != null) {
            "Unknown tool: ${call.name}"
        }
        validateArguments(call.name, call.arguments)
    }

    private fun validateArguments(name: String, arguments: JsonObject) {
        when (name) {
            "screen.snapshot", "ocr.current_screen" -> arguments.requireShape(emptySet(), emptySet())
            "screen.click" -> validateSelector(arguments)
            "screen.type" -> {
                arguments.requireShape(setOf("selector", "text"), setOf("selector", "text"))
                val selector = arguments["selector"] as? JsonObject ?: error("selector must be a JSON object")
                validateSelector(selector)
                arguments.requiredString("text", MAX_TYPED_TEXT_CHARS, allowEmpty = true)
            }
            "screen.scroll" -> {
                arguments.requireShape(setOf("selector", "forward"), emptySet())
                arguments["selector"]?.let {
                    val selector = it as? JsonObject ?: error("selector must be a JSON object")
                    validateSelector(selector)
                }
                arguments["forward"]?.let { requireBoolean(it, "forward") }
            }
            "system.global_action" -> {
                arguments.requireShape(setOf("action"), setOf("action"))
                require(arguments.requiredString("action", 20) in GLOBAL_ACTIONS) { "Unsupported global action" }
            }
            "app.launch" -> {
                arguments.requireShape(setOf("package"), setOf("package"))
                require(PACKAGE_NAME.matches(arguments.requiredString("package", 200))) { "Invalid package name" }
            }
            "shell.operation" -> validateShell(arguments)
            else -> error("Unknown tool: $name")
        }
    }

    private fun validateSelector(selector: JsonObject) {
        selector.requireShape(SELECTOR_KEYS, emptySet())
        val present = SELECTOR_KEYS.filter { it in selector }
        require(present.isNotEmpty()) { "A non-empty selector is required" }
        selector["viewId"]?.let { requireSafeSelectorString(it, "viewId", MAX_SELECTOR_CHARS) }
        selector["text"]?.let { requireTextString(it, "text", MAX_SELECTOR_TEXT_CHARS) }
        selector["contentDescription"]?.let {
            requireTextString(it, "contentDescription", MAX_SELECTOR_TEXT_CHARS)
        }
        selector["path"]?.let { element ->
            val path = element as? JsonArray ?: error("path must be an array")
            require(path.isNotEmpty() && path.size <= MAX_SELECTOR_DEPTH) { "Selector path length is invalid" }
            path.forEach {
                val primitive = it as? JsonPrimitive ?: error("Selector path must contain integers")
                require(!primitive.isString) { "Selector path must contain integers" }
                val index = primitive.intOrNull ?: error("Selector path must contain integers")
                require(index in 0..MAX_CHILD_INDEX) { "Selector path index is outside the allowed range" }
            }
        }
    }

    private fun validateShell(arguments: JsonObject) {
        arguments.requireShape(setOf("operation", "arguments"), setOf("operation", "arguments"))
        val operation = arguments.requiredString("operation", 64)
        val values = arguments["arguments"] as? JsonObject ?: error("shell arguments must be a JSON object")
        require(values.size <= MAX_SHELL_ARGUMENTS) { "Too many shell arguments" }
        val stringValues = values.mapValues { (key, value) ->
            require(SHELL_KEY.matches(key)) { "Invalid shell argument name" }
            requireTextString(value, key, MAX_SHELL_VALUE_CHARS, allowEmpty = true)
        }
        val expected = SHELL_ARGUMENTS[operation] ?: error("Shell operation is not allowlisted")
        require(stringValues.keys == expected) { "Shell arguments do not match the operation schema" }
        ShellCommandPolicy.compile(PrivilegedCommand(operation, stringValues)).getOrElse {
            error(it.message ?: "Shell operation was rejected")
        }
    }

    private fun JsonObject.requireShape(allowed: Set<String>, required: Set<String>) {
        val unknown = keys - allowed
        require(unknown.isEmpty()) { "Unknown field(s): ${unknown.sorted().joinToString()}" }
        val missing = required - keys
        require(missing.isEmpty()) { "Missing field(s): ${missing.sorted().joinToString()}" }
    }

    private fun JsonObject.requiredString(key: String, max: Int, allowEmpty: Boolean = false): String =
        requireTextString(this[key] ?: error("Missing string: $key"), key, max, allowEmpty)

    companion object {
        private const val MAX_ENVELOPE_CHARS = 16 * 1024
        private const val MAX_ID_CHARS = 64
        private const val MAX_NAME_CHARS = 64
        private const val MAX_SELECTOR_CHARS = 200
        private const val MAX_SELECTOR_TEXT_CHARS = 256
        private const val MAX_TYPED_TEXT_CHARS = 4_096
        private const val MAX_SELECTOR_DEPTH = 24
        private const val MAX_CHILD_INDEX = 999
        private const val MAX_SHELL_ARGUMENTS = 8
        private const val MAX_SHELL_VALUE_CHARS = 256
        private val ID = Regex("^[A-Za-z0-9][A-Za-z0-9._:-]{0,63}$")
        private val PACKAGE_NAME = Regex("^[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+$")
        private val SAFE_SELECTOR = Regex("^[A-Za-z0-9_.:/-]+$")
        private val SHELL_KEY = Regex("^[A-Za-z][A-Za-z0-9_.-]{0,63}$")
        private val SELECTOR_KEYS = setOf("viewId", "text", "contentDescription", "path")
        private val GLOBAL_ACTIONS = setOf("back", "home", "recents", "notifications")
        private val SHELL_ARGUMENTS = mapOf(
            "device.info" to emptySet(),
            "package.list_user" to emptySet(),
            "package.force_stop" to setOf("package"),
            "package.install_existing" to setOf("package"),
            "settings.get" to setOf("namespace", "key"),
            "settings.put" to setOf("namespace", "key", "value"),
            "input.keyevent" to setOf("keyCode"),
        )

        private fun requireSafeSelectorString(element: JsonElement, label: String, max: Int): String {
            val value = requireTextString(element, label, max)
            require(SAFE_SELECTOR.matches(value)) { "$label contains unsupported characters" }
            return value
        }

        private fun requireTextString(
            element: JsonElement,
            label: String,
            max: Int,
            allowEmpty: Boolean = false,
        ): String {
            val primitive = element as? JsonPrimitive ?: error("$label must be a string")
            require(primitive.isString) { "$label must be a string" }
            val value = primitive.contentOrNull ?: error("$label must be a string")
            require((allowEmpty || value.isNotEmpty()) && value.length <= max) { "$label length is invalid" }
            require(value.none { it == '\u0000' }) { "$label contains a null character" }
            return value
        }

        private fun requireBoolean(element: JsonElement, label: String): Boolean {
            val primitive = element as? JsonPrimitive ?: error("$label must be a boolean")
            require(!primitive.isString) { "$label must be a boolean" }
            return primitive.booleanOrNull ?: error("$label must be a boolean")
        }
    }
}
