# Security and safety model

## Assets

- private prompts and generated text;
- visible screen content and screenshots;
- downloaded model files;
- accessibility authority;
- Shizuku shell/root authority;
- release signing keys and proprietary SDK credentials.

## Threats and controls

| Threat | Control now | Follow-up |
|---|---|---|
| Model emits malicious tool call | exact 16 KiB JSON envelope, per-tool schemas, fixed registry, second dispatch validation, one-time trusted review | expand parser/property fuzzing and persistent audit before autonomy |
| Screen prompt injection | screen data has no authority | provenance labels in model context |
| Shell injection | no raw shell; validated argv | fuzz every argument compiler |
| Password leakage in tree | password text/description omitted | screenshot redaction UX |
| Untrusted model file | HF HTTPS policy, optional hash, GGUF magic | mandatory signed manifest |
| Oversized output/DoS | 400 nodes, depth 24, 64 KiB shell output, timeout | model allocation preflight |
| Accessibility service retention | weak service ref, fresh roots | lifecycle/instrumentation tests |
| Secret committed to Git | validation regex, ignored key formats | GitHub secret scanning/protection |
| False acceleration claim | unavailable backends report empty capability | runtime self-test and benchmark proof |

## Fail-closed behavior

- Missing accessibility service → tool failure, no fallback gesture.
- Missing Shizuku permission → elevated tool failure.
- Model proposal mode is off by default; every accepted model proposal still requires one-time trusted UI review.
- Mixed prose/JSON, unknown fields (including model-authored confirmation), wrong types, unsafe selectors and unsupported tools → rejection.
- Consequential call without trusted confirmation → failure.
- Unknown shell operation/setting/key code → failure.
- Missing OCR model → typed unavailable error, no network OCR.
- Missing concrete LLM backend → empty capability set and load failure.
- Non-HTTPS/non-Hugging Face/non-GGUF model → download/install rejection.

## Accessibility disclosure

The service can read visible interface content and control other apps. It is disabled by default and can only be enabled by the user in Android Settings. LAI must not socially engineer permission, obscure the disclosure, automatically enable itself through Shizuku, or prevent revocation.

## Shizuku disclosure

Shizuku may run as UID 2000 (ADB shell) or UID 0 (root/Sui). The app surfaces UID in Developer Mode. Root does not relax policy. Operations remain allowlisted at either identity.

## Data handling

- outbound user-derived data is denied by `LocalFirstPolicy`;
- only `platform:download` owns network transport and Android network permission;
- supported-model catalog updates require an explicit refresh and valid ECDSA signature;
- downloads require explicit user action, HTTPS, reviewed hosts, mandatory SHA-256 and expected size;
- after model/component installation, inference and automation require no network;
- app-private model storage;
- screenshots in memory and recycled after OCR;
- no analytics or remote inference dependency;
- no broad storage permission;
- no logs of prompts, screen content, typed text, or command stdout by default;
- load/TTFT/token-rate/thermal samples remain in memory (maximum 20) and have no upload path;
- diagnostics leave app-private state only after an explicit Storage Access Framework export and exclude user content;
- Android critical-memory callbacks destroy the native session and notify UI state.

## Release supply chain

- pin toolchain/dependency versions;
- Dependabot proposes reviewed updates;
- compile only in GitHub Actions;
- signing secrets are environment-only and runner-temporary;
- future third-party action tags should be replaced by immutable commit SHAs before production;
- future releases should add SBOM, dependency verification, artifact attestations, and reproducible build comparison.

## Known Phase 1 gaps

- manual Developer Mode URLs still rely on user-supplied digest metadata rather than signed catalog provenance;
- the confirmation dialog supports one model-proposed action only; there is no autonomous result-feedback loop;
- the redacted audit is memory-only; there is no replay-resistant persistent audit log;
- OCR screenshot redaction is not implemented;
- dependency verification metadata and SBOM are not present;
- native adapters have not undergone memory-safety fuzzing.

These gaps prevent a production/autonomous designation.
