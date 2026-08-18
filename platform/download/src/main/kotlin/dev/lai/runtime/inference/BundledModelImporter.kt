package dev.lai.runtime.inference

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Copies prebundled GGUFs from APK assets (built from local `model/` folder) into the
 * app-private model store on first launch. No network, no Git history — the 1.1 GB blob
 * stays only in the signed APK you built on CI/your machine.
 *
 * Assets come from `model/` → `assets/` (via app/build.gradle assets.srcDirs). They may be at
 * `assets/*.gguf` or `assets/models/*.gguf` — both are checked.
 */
object BundledModelImporter {
    private const val TAG = "LAI-bundled"

    fun importIfNeeded(context: Context) {
        try {
            val assetManager = context.assets
            val candidates = mutableSetOf<String>()
            // Root assets
            assetManager.list("")?.forEach { if (it.endsWith(".gguf", true)) candidates.add(it) }
            // Subfolder assets/models
            assetManager.list("models")?.forEach { if (it.endsWith(".gguf", true)) candidates.add("models/$it") }
            // Fallback: assetManager.list("model") if someone put files under model/models
            assetManager.list("model")?.forEach { if (it.endsWith(".gguf", true)) candidates.add("model/$it") }

            if (candidates.isEmpty()) return

            val modelDir = File(context.noBackupFilesDir, "models").apply { mkdirs() }
            val registryFile = File(modelDir, "registry.json")

            for (assetPath in candidates) {
                val fileName = assetPath.substringAfterLast('/')
                val dest = File(modelDir, fileName)
                if (dest.exists() && dest.length() > 0) {
                    Log.i(TAG, "Bundled model already present: $fileName")
                    continue
                }
                Log.i(TAG, "Copying bundled model $assetPath → ${dest.absolutePath}")
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(dest).use { out -> input.copyTo(out) }
                }
                // Register in registry.json if not already there — minimal entry so ModelRepository.list() sees it.
                // Full metadata (sha, bytes) will be filled on next list/import via verification if needed.
                // We keep it simple: write file and let ModelRepository discover it; if registry missing, create stub.
                try {
                    val sha = sha256(dest)
                    val bytes = dest.length()
                    // Append to registry if not present
                    val existing = if (registryFile.exists()) registryFile.readText() else "[]"
                    if (!existing.contains(fileName) && !existing.contains(dest.nameWithoutExtension)) {
                        // Create a stub InstalledModel entry — reuse ModelRepository's JSON shape via plain text patch.
                        // If registry is empty, write a single-entry array.
                        val stub = """{"id":"${dest.nameWithoutExtension}","displayName":"${dest.nameWithoutExtension}","fileName":"$fileName","bytes":$bytes,"sha256":"$sha","active":false}"""
                        val newText = if (existing.trim() == "[]" || existing.isBlank()) "[$stub]" else existing.trimEnd().removeSuffix("]") + ",$stub]"
                        registryFile.writeText(newText)
                        Log.i(TAG, "Registered bundled model $fileName ($bytes bytes, $sha)")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Bundled model $fileName copied but registry update failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Bundled import skipped: ${e.message}")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            var r: Int
            while (input.read(buf).also { r = it } != -1) digest.update(buf, 0, r)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
