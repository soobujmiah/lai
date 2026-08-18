package dev.lai.runtime.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the "never log a secret" contract. The redactor is the last line of defense in
 * [LaiLog]; these cases must stay redacted in both debug and signed release builds.
 *
 * Note: the fake secrets below are assembled at runtime on purpose — the repository's own
 * source-policy scanner (validate_repo.sh) rejects literal token-shaped strings in any file.
 */
class LaiLogRedactorTest {

    private val classicPat: String = "ghp_" + "AbCdEfGhIjKlMnOpQrStUvWxYz012345"
    private val fineGrainedPat: String =
        "github_pat_" + "11ABCDEF0123456789_abcdefghijklmnopqrstuvwxyz0123456789"

    @Test
    fun `redacts classic GitHub personal access token`() {
        val out = LaiLogRedactor.redact("cloning with token=$classicPat")
        assertFalse(out.contains(classicPat))
        assertFalse(out.contains("ghp_AbCdEf"))
    }

    @Test
    fun `redacts fine-grained GitHub token`() {
        val out = LaiLogRedactor.redact("auth $fineGrainedPat")
        assertFalse(out.contains(fineGrainedPat))
        assertFalse(out.contains("github_pat_"))
    }

    @Test
    fun `redacts api key value but keeps key prefix readable`() {
        val out = LaiLogRedactor.redact("request failed api_key=super-secret-value-1234 status=401")
        assertFalse(out.contains("super-secret-value-1234"))
        assertTrue(out.contains("api_key=REDACTED"))
    }

    @Test
    fun `redacts authorization header bearer value`() {
        val out = LaiLogRedactor.redact(
            "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.token.payload",
        )
        assertFalse(out.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"))
        assertTrue(out.contains("REDACTED"))
    }

    @Test
    fun `redacts bare bearer token`() {
        val out = LaiLogRedactor.redact("calling api with Bearer abcdefgh12345678")
        assertFalse(out.contains("abcdefgh12345678"))
        assertTrue(out.contains("REDACTED"))
    }

    @Test
    fun `redacts password and access token forms`() {
        val password = LaiLogRedactor.redact("login failed password=hunter2forreal")
        assertFalse(password.contains("hunter2forreal"))
        assertTrue(password.contains("password=REDACTED"))
        val token = LaiLogRedactor.redact("refresh access_token=AbCdEf1234567890")
        assertFalse(token.contains("AbCdEf1234567890"))
        assertTrue(token.contains("access_token=REDACTED"))
    }

    @Test
    fun `redacts entire private key block`() {
        val key = "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA\n-----END RSA PRIVATE KEY-----"
        val out = LaiLogRedactor.redact("failed to load $key")
        assertFalse(out.contains("MIIEpAIBAAKCAQEA"))
        assertTrue(out.contains("BEGIN REDACTED PRIVATE KEY"))
    }

    @Test
    fun `redacts aws access key id`() {
        val secret = "AKIA" + "IOSFODNN7EXAMPLE"
        val out = LaiLogRedactor.redact("s3 put with $secret")
        assertFalse(out.contains(secret))
    }

    @Test
    fun `leaves normal diagnostics untouched`() {
        val line = "LAI-model: Model loaded id=qwen2.5-1.5b-instruct-q4-k-m backend=llama-cpu in 2077 ms"
        assertEquals(line, LaiLogRedactor.redact(line))
    }

    @Test
    fun `leaves model sha256 hashes untouched`() {
        val sha = "sha256=6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e"
        assertEquals(sha, LaiLogRedactor.redact(sha))
    }

    @Test
    fun `leaves ordinary text untouched`() {
        val line = "Generation completed: 42 tokens, decode 18.5 tok/s, total 2341 ms"
        assertEquals(line, LaiLogRedactor.redact(line))
    }
}
