package dev.lai.runtime.shell

/**
 * Converts structured operations into argument arrays. It intentionally has no
 * "raw command" escape hatch: model-authored strings never reach a shell parser.
 */
object ShellCommandPolicy {
    private val packageName = Regex("^[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+$")
    private val settingKey = Regex("^[a-zA-Z0-9_.-]{1,80}$")
    private val safeSettingKeys = setOf(
        "screen_brightness",
        "screen_brightness_mode",
        "accelerometer_rotation",
        "user_rotation",
        "font_scale",
    )
    private val safeKeyCodes = setOf("3", "4", "24", "25", "26", "85", "87", "88")

    data class ApprovedCommand(
        val argv: List<String>,
        val requiresConfirmation: Boolean,
        val auditLabel: String,
    )

    fun compile(request: PrivilegedCommand): Result<ApprovedCommand> = runCatching {
        when (request.operation) {
            "device.info" -> ApprovedCommand(
                argv = listOf("getprop"),
                requiresConfirmation = false,
                auditLabel = "Read device properties",
            )

            "package.list_user" -> ApprovedCommand(
                argv = listOf("pm", "list", "packages", "-3"),
                requiresConfirmation = false,
                auditLabel = "List user applications",
            )

            "package.force_stop" -> {
                val packageValue = requirePackage(request.arguments["package"])
                ApprovedCommand(
                    argv = listOf("am", "force-stop", packageValue),
                    requiresConfirmation = true,
                    auditLabel = "Force-stop $packageValue",
                )
            }

            "package.install_existing" -> {
                val packageValue = requirePackage(request.arguments["package"])
                ApprovedCommand(
                    argv = listOf("cmd", "package", "install-existing", packageValue),
                    requiresConfirmation = true,
                    auditLabel = "Enable existing package $packageValue",
                )
            }

            "settings.get" -> {
                val namespace = requireNamespace(request.arguments["namespace"])
                val key = request.arguments["key"]?.takeIf(settingKey::matches)
                    ?: error("Invalid settings key")
                ApprovedCommand(
                    argv = listOf("settings", "get", namespace, key),
                    requiresConfirmation = false,
                    auditLabel = "Read $namespace/$key",
                )
            }

            "settings.put" -> {
                val namespace = requireNamespace(request.arguments["namespace"])
                val key = request.arguments["key"]?.takeIf { it in safeSettingKeys }
                    ?: error("Setting is not in the writable allowlist")
                val value = request.arguments["value"]?.take(80) ?: error("Missing value")
                require(!value.contains('\u0000')) { "Invalid value" }
                ApprovedCommand(
                    argv = listOf("settings", "put", namespace, key, value),
                    requiresConfirmation = true,
                    auditLabel = "Change $namespace/$key",
                )
            }

            "input.keyevent" -> {
                val keyCode = request.arguments["keyCode"]?.takeIf { it in safeKeyCodes }
                    ?: error("Key code is not allowed")
                ApprovedCommand(
                    argv = listOf("input", "keyevent", keyCode),
                    requiresConfirmation = true,
                    auditLabel = "Send key event $keyCode",
                )
            }

            else -> error("Unsupported privileged operation: ${request.operation}")
        }
    }

    private fun requirePackage(value: String?): String =
        value?.takeIf(packageName::matches) ?: error("Invalid package name")

    private fun requireNamespace(value: String?): String =
        value?.takeIf { it in setOf("system", "secure", "global") }
            ?: error("Invalid settings namespace")
}
