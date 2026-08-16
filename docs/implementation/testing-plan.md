# Testing plan

Last updated: 2026-08-17

## Purpose and reporting rule

Tests establish evidence; they do not upgrade implementation status by implication. Every recorded run must state test, environment, command/procedure, result, failure, and limitation. Use evidence levels: implemented but untested, emulator tested, physical-device tested, production validated.

## Current automated baseline

CI command:

```bash
gradle --no-daemon --stacktrace \
  coverageCheck \
  :platform:audit:testDebugUnitTest \
  :platform:download:testDebugUnitTest \
  :platform:workspace:testDebugUnitTest \
  :app:testDebugUnitTest \
  :app:lintDebug \
  -Plai.enableLlamaCpp=ON \
  -Plai.llamaCppDir="$LAI_LLAMA_CPP_DIR"
```

CI then assembles debug/release APK with pinned llama.cpp. `scripts/validate_repo.sh` performs source size/binary/token/document checks plus architecture and catalog validation; it does not compile Kotlin.

## Test pyramid

### Pure unit and property tests

Target contracts, serialization, parsers, canonicalization, policy, routing, migrations, state reducers, diff algorithms, and permission decisions. Tests must be deterministic and avoid Android where possible.

### Android/JVM fake-boundary tests

Use fake `ContentResolver`/SAF providers, fake repositories, fake clocks, fake device profiles, fake authority adapters, and temporary files. Cover interrupted writes, deceptive metadata, revocation, lifecycle, and concurrency.

### Instrumentation/emulator tests

Cover Compose navigation/state restoration, ActivityResult flows, storage grants, process recreation, configuration changes, service binding, and accessibility semantics where emulator support is meaningful.

### Native/integration tests

Compile pinned llama.cpp; test JNI handle lifecycle, invalid model paths/magic, cancellation, concurrent close/generate, UTF-8 fragmentation, context limits, allocation failure, and sanitizer/fuzz builds where toolchains permit.

### Physical-device evidence

Use named device, SoC, Android/build, ABI, app commit/APK hash, model/digest, environment, thermal/battery state, procedure, raw measurements, result, and limitation. Never generalize one device result to all Android hardware.

## Immediate Phase 0/1 gates

1. Documentation link and status-vocabulary validation.
2. Full CI rerun on the documentation commit.
3. Add app/ViewModel fake tests for startup, cancellation, tool approval, audit failure, model state, malformed settings, and process recreation.
4. Add download/catalog tests with mock HTTP: redirect host validation, no length, chunked overrun, Range mismatch, digest/size/magic failure, cache interruption.
5. Add SAF tests: lying/unknown size, in-stream byte ceiling, deadline/cancellation, partial output, rename/delete failure, permission revocation.
6. Split release permission and add workflow policy tests/action pin review.

## Subsystem plans

| Subsystem | Required automated tests | Required device/integration tests | Acceptance evidence |
|---|---|---|---|
| Model/catalog | signatures, downgrade, redirects, byte limits, resume, atomic registry/cache | interruption, storage full, uninstall/restore | exact digest/bytes, recovery result |
| Inference | config bounds, scheduler, JNI lifecycle, cancel/close races, UTF-8 | sustained multi-turn, stop/reload, context trim, memory pressure, thermal | model/device/backend/metrics/limitations |
| Accessibility | selector determinism, bounds, password omission | each action, service death/rebind, screenshot lifecycle | harmless harness and authority state |
| Shizuku | operation/argument fuzzing, timeout/output bounds | UID 2000/root variants, binder death/recovery | no raw shell, exact argv/result |
| Tool agent | parser/property fuzz, consent, replay/transitions, budgets | model compliance, foreground binding, denial/cancel/restart | complete task/audit trace without content |
| Workspace/settings | schema/migration, provider deception, atomic recovery | grant/revoke/reinstall/removable provider | no broad storage; deterministic recovery |
| OCR/multimodal | schema, bitmap lifecycle, model errors | quality dataset by script/language, latency/memory | versioned dataset and measured quality |
| Plugin future | manifest/schema fuzz, permission denial, compatibility | isolation/crash/update/rollback | signed package and capability audit |
| Localhost future | auth, bind address, ports, streaming, limits, kill switch | process death/reboot/LAN opt-in/client visibility | loopback default and no unauthenticated access |
| Project/workstation future | path traversal, diff correctness, rollback, trust transitions | large tree, process kill, interrupted build, Git conflict | checkpoint restore and no secret leakage |

## Reliability and recovery matrix

Every stateful subsystem must test app kill, process death, reboot where relevant, cancellation, timeout, storage full, permission revocation, corrupt metadata, interrupted write/download/build, and version migration. A passing happy path is insufficient.

## Security and privacy testing

- Fuzz all untrusted JSON, URL, selector, package, key/value, manifest, catalog, and workspace parsers.
- Verify outbound traffic absence with network inspection during prompts, automation, OCR, diagnostics, and plugin execution.
- Assert diagnostics/logs exclude prompts, screen content, typed text, command output, credentials, and raw project content.
- Scan source/history/artifacts for secrets and prohibited binaries.
- Add dependency vulnerability, SBOM, checksum, provenance, and signing verification before production designation.

## Performance testing

Record real measurements only: load, prefill, TTFT, decode, task duration, peak estimate/observed memory, storage throughput, battery, and thermal trajectory. Specify warm/cold state, thread/batch/context, model hash, and sample count. Performance regressions need thresholds only after a stable baseline exists.

## Accessibility and localization testing

Test TalkBack semantics, focus order, touch targets, dynamic type, keyboard/mouse navigation, RTL where relevant, Bangla/English truncation, error clarity, permission disclosures, and non-color-only status. Future workstation surfaces require phone/tablet/foldable test matrices.

## Test record template

```markdown
### Test ID and title
- Commit/APK hash:
- Environment/device:
- Backend/model/digest:
- Command or procedure:
- Expected:
- Actual result:
- Failure/artifacts:
- Evidence level:
- Limitation:
- Date/reviewer:
```

Store device records under `docs/device-results/` and do not edit old evidence to imply a newer commit was tested.

## Current limitations

This documentation audit ran `scripts/validate_repo.sh` successfully but did not run the Android/NDK build locally because the audit environment lacks the required JDK 17/Android toolchain/Gradle setup. The next CI run is therefore an explicit acceptance gate.
