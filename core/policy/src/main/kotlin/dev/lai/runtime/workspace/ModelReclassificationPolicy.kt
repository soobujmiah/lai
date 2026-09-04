package dev.lai.runtime.workspace

import dev.lai.runtime.inference.InstalledModel

/**
 * Upgrades an already-installed model's registered id to the reviewed catalog's canonical id
 * once its SHA-256 matches an entry the catalog didn't recognize at import time.
 *
 * Root cause (docs/device-results/2026-09-04-redmi-turbo-4-pro-opencl-revalidation.md): the
 * workspace auto-importer ([WorkspacePolicy.classify]) already matches by SHA-256 correctly for
 * *new* imports — but a file imported by an older build, before the catalog recognized its
 * digest, falls back to a filename-derived id (see `MainViewModel.importWorkspaceModels`'s
 * `ModelImportSpec` fallback) and stays registered under that id forever: nothing re-checks an
 * already-installed model against catalog updates. This policy is that re-check, applied
 * idempotently every time the installed model list is refreshed, so a model imported before its
 * catalog entry existed self-heals to the canonical id the next time the app runs, with no
 * reinstall or manual re-import required.
 *
 * Pure and Android-free: no file I/O, no persistence — [dev.lai.runtime.inference.ModelRepository]
 * (`platform:download`) owns applying [Plan.updatedRegistry] back to the persisted registry.
 */
class ModelReclassificationPolicy {

    /**
     * @param updatedRegistry the full registry, in the same order as [installed], with any
     *   reclassified entries' `id` replaced by their canonical catalog id and every other field
     *   (fileName, bytes, sha256, sourceUrl, installedAtEpochMs, displayName) left untouched —
     *   the file on disk is never moved or renamed, since [InstalledModel.fileName] (not `id`) is
     *   what resolves it, so a pure id rewrite is always safe.
     * @param idRemap old id -> new canonical id, containing only entries that were actually
     *   reclassified this pass. Callers with live state keyed by the old id (a currently active
     *   model, an in-flight generation) must apply this same remap to that state in the same
     *   update, or it will silently point at an id the registry no longer has.
     */
    data class Plan(
        val updatedRegistry: List<InstalledModel>,
        val idRemap: Map<String, String>,
    )

    /**
     * @param installed the current registry, in persisted order.
     * @param reviewedBySha256 lowercased-hex SHA-256 -> canonical reviewed catalog model id (the
     *   same map [WorkspacePolicy.classify] takes, so callers can build it once and share it).
     */
    fun reclassify(
        installed: List<InstalledModel>,
        reviewedBySha256: Map<String, String>,
    ): Plan {
        val reviewed = reviewedBySha256.mapKeys { it.key.lowercase() }
        // Tracks ids as they're claimed during this pass (starting from every id already present)
        // so two stale entries that happen to share a canonical target can never both be renamed
        // onto it: id is treated as a stable key everywhere else in the app (delete/load/dedup all
        // do `firstOrNull { it.id == ... }`), so producing a duplicate id would make those silently
        // ambiguous. The first entry in registry order claims the canonical id; any other entry
        // whose digest matches the same canonical id is left exactly as it was — never merged,
        // never deleted, never silently dropped.
        val claimedIds = installed.mapTo(HashSet()) { it.id }
        val remap = LinkedHashMap<String, String>()
        val updated = installed.map { model ->
            val canonicalId = reviewed[model.sha256.lowercase()]
            if (canonicalId == null || canonicalId == model.id || canonicalId in claimedIds) {
                model
            } else {
                claimedIds.remove(model.id)
                claimedIds.add(canonicalId)
                remap[model.id] = canonicalId
                model.copy(id = canonicalId)
            }
        }
        return Plan(updated, remap)
    }
}
