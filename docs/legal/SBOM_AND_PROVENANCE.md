# SBOM and provenance

**Status:** future production requirement  
**Date:** 2026-08-18  
**Current implementation:** lightweight `gradle :app:dependencies` text dump uploaded as a CI artifact; not CycloneDX/SPDX JSON  
**Related:** [`../release/COMMERCIAL_RELEASE_COMPLIANCE.md`](../release/COMMERCIAL_RELEASE_COMPLIANCE.md), [`../BENCHMARKING.md`](../BENCHMARKING.md)

The goal is reproducible commercial release compliance. This phase does not add generators.

## Required artifacts for a production-labeled release

| Artifact | Contents | Current |
|---|---|---|
| Dependency SBOM | Resolved Maven coordinates, versions, licenses, hashes where available | Text dump only |
| Native dependency SBOM | llama.cpp commit, ggml subcomponents actually linked, NDK, CMake, SPIR-V packages used to compile | Pin recorded; component inventory MISSING |
| Model provenance | Catalog ID, URL, SHA-256, size, license, signature revision | Present for the reviewed Qwen artifact; weights not in Git |
| Dataset provenance | Source, license, hash | None in product |
| Build provenance | git SHA, tag, workflow run, runner image, unsigned vs signed | Partially in GitHub Actions metadata |
| Source revision | Full commit SHA of the tree that produced the APK | Available from the tag |
| Build environment | JDK distribution/version, Gradle version, AGP, compile/target SDK | Partially in workflow env; docs drift exists |
| Compiler versions | Kotlin, NDK clang | Recorded in catalog / `ndkVersion` |
| SDK versions | Android SDK/NDK/CMake | Workflow env |
| License metadata | SPDX per shipped component | Registers PARTIAL / stale |
| Artifact hashes | SHA-256 of APK/AAB and native `.so` | Not systematically published |

Target formats, when implemented: SPDX or CycloneDX JSON plus a human-readable NOTICE. The feature matrix already lists CycloneDX as a target, not a current capability.

## Provenance of native inference

When `LAI_ENABLE_LLAMA_CPP=ON`:

- record llama.cpp commit `ad1de39e0708e3ced9c71bb3c82d93a2c046a73f` (or the then-current pin),
- record whether Vulkan was compiled in,
- include the MIT notice in the release NOTICE set,
- inventory ggml third-party files that actually linked.

When the flag is OFF, the APK must not be described as containing llama.cpp.

## Provenance of models

Models are user-acquired. Release notes must not imply that weights are part of the APK. Catalog revision, public key identity, and reviewed SHA-256 belong in the SBOM or a companion provenance file as *references*, not as embedded blobs.

## Policy result

CI should eventually emit `pass` / `fail` / `unknown-license`. `unknown-license` fails a production-labeled job.

Those controls are currently **MISSING/PARTIAL** and must not be described as implemented.

## Decision

**DECISION REQUIRED** for SBOM format and the production-fail policy threshold.  
No generator is added in this phase.
