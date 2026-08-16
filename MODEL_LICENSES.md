# Model license register

Last audited: 2026-08-17

LAI stores no model weights in Git. Model acquisition is an explicit user action, and artifact integrity/provenance metadata does not by itself grant redistribution rights.

## Reviewed catalog artifact

| Catalog ID | Upstream repository | Artifact | Catalog license field | Distribution state | Required user/release review |
|---|---|---|---|---|---|
| `qwen2.5-1.5b-instruct-q4-k-m` | `Qwen/Qwen2.5-1.5B-Instruct-GGUF` | `qwen2.5-1.5b-instruct-q4_k_m.gguf` | `Apache-2.0` | downloaded separately; not bundled | verify upstream model card/license at acquisition and before any redistribution; retain attribution/NOTICE as required |

Catalog trust currently records exact source URL, SHA-256, byte size, architecture, quantization, format, backend/ABI/context/memory compatibility, and review state. `banglaQualityValidated` remains false despite basic device observations; license metadata must never be interpreted as quality or safety validation.

## Local unreviewed models

User-imported or workspace-discovered unknown GGUF files are classified `LOCAL_UNREVIEWED`. LAI does not claim their license, provenance, safety, compatibility, or redistribution rights. Registration must not auto-load or redistribute them.

## Future model gate

Before adding a reviewed model: record upstream owner/repository, immutable artifact identity, exact license identifier/text source, usage and redistribution restrictions, attribution/NOTICE, dataset/use-policy restrictions where applicable, quantization/conversion provenance, backend requirements, and named-device/quality evidence. Unknown or incompatible terms block catalog inclusion and bundling.
