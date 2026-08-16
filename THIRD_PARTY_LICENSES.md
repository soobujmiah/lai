# Third-party license register

Last audited: 2026-08-17

This file tracks license identifiers and required verification. It does not replace upstream license texts. A production release must generate and ship exact applicable texts/notices from the resolved dependency set.

| Dependency family | Current identifier | Source of truth to verify | Required action |
|---|---|---|---|
| AndroidX and Jetpack Compose | Apache-2.0 | resolved Maven POM/artifact license and NOTICE metadata | aggregate required notices |
| Kotlin and kotlinx | Apache-2.0 | resolved JetBrains/Kotlin artifact metadata | aggregate required notices |
| OkHttp/Okio | Apache-2.0 | resolved Square artifact metadata | include applicable notices |
| Shizuku API/provider | Apache-2.0 | pinned upstream release/source | preserve license/NOTICE obligations |
| llama.cpp | MIT | pinned commit root `LICENSE` | include MIT copyright/license text |
| JUnit 4 | EPL-1.0 | resolved test artifact metadata | build/test use; verify distribution scope |
| Android/Gradle/CMake toolchains | vendor/tool-specific | installed SDK/NDK/Gradle distributions | do not redistribute from source repository; review CI/release use |
| Future Qualcomm QAIRT/QNN | UNKNOWN until licensed acquisition | vendor agreement/SDK package | no source/binary distribution before legal review |
| Future plugin/model dependencies | UNKNOWN until proposed | signed manifest/catalog + upstream terms | block install/release when license is absent/unreviewed |

## Automated-control target

Production CI must create an SBOM, resolved dependency/license report, third-party notices artifact, release checksums, provenance/attestation, and a policy result for unknown/incompatible licenses. These controls are currently **MISSING/PARTIAL** and must not be described as implemented.
