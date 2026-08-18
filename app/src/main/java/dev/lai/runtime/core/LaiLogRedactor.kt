package dev.lai.runtime.core

/**
 * Pure string redaction used by [LaiLog] before any line is written to logcat or the
 * diagnostic log file. Kept free of Android imports so it is directly unit-testable on a
 * plain JVM (app module unit tests).
 *
 * Conservative by design: when in doubt a line is over-redacted rather than risking a secret
 * reaching a log. Only the patterns below are applied; no message content is otherwise
 * inspected, parsed, or retained.
 */
object LaiLogRedactor {

    private val SECRET_PATTERNS = listOf(
        // GitHub tokens: ghp_ (classic PAT), gho_/ghu_/ghs_/ghr_ (OAuth/app tokens).
        Regex("""\b(?:ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9]{20,}\b"""),
        // GitHub fine-grained PATs.
        Regex("""\bgithub_pat_[A-Za-z0-9_]{20,}\b"""),
        // AWS access key id.
        Regex("""\bAKIA[0-9A-Z]{16}\b"""),
        // PEM private key blocks — redact the whole block between BEGIN and END.
        Regex(
            "-----BEGIN (?:RSA |EC |OPENSSH |DSA |PGP |ENCRYPTED )?PRIVATE KEY-----.*?" +
                "-----END (?:RSA |EC |OPENSSH |DSA |PGP |ENCRYPTED )?PRIVATE KEY-----",
            RegexOption.DOT_MATCHES_ALL,
        ),
        // Authorization header values (covers "Authorization: Bearer ..." and proxy form).
        Regex("""(?i)\b(authorization|proxy-authorization)\s*[:=]\s*(?:bearer\s+)?\S+"""),
        // key=value / key:value with a secret-ish key name: api_key, access_token,
        // refresh_token, client_secret, password, passwd, secret, token, apikey, auth.
        Regex(
            """(?i)\b((?:access|refresh|client|api)[_-]?(?:token|key)|password|passwd|pwd|secret|token|apikey|auth)\b\s*[:=]\s*[^\s,;"']+""",
        ),
        // Bare "Bearer <token>" when it is not part of an Authorization header line.
        Regex("""(?i)\bbearer\s+[A-Za-z0-9._~+/=-]{8,}"""),
    )

    fun redact(line: String): String {
        var result = line
        for (pattern in SECRET_PATTERNS) {
            result = pattern.replace(result) { match ->
                val value = match.value
                when {
                    "PRIVATE KEY" in value -> "-----BEGIN REDACTED PRIVATE KEY-----"
                    value.startsWith("authorization", ignoreCase = true) ||
                        value.startsWith("proxy-authorization", ignoreCase = true) ->
                        value.substringBefore(' ') + " REDACTED"
                    else -> {
                        // Keep a readable "key=REDACTED" / "key:REDACTED" prefix, never the value.
                        val separator = value.indexOfFirst { it == '=' || it == ':' }
                        if (separator >= 0) {
                            value.substring(0, separator + 1) + "REDACTED"
                        } else {
                            "REDACTED"
                        }
                    }
                }
            }
        }
        return result
    }
}
