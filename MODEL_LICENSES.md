# Model license register

Last audited: 2026-08-17

LAI stores no model weights in Git. LLM model acquisition is an explicit user action. The offline OCR models below are bundled by GitHub CI with verified hashes after the owner authorized an openly licensed model choice. Artifact integrity/provenance metadata does not by itself prove model accuracy.

## Bundled OCR models

| Language | Source | SHA-256 | License | Status |
|---|---|---|---|---|
| Bengali (`ben`) | `tesseract-ocr/tessdata_fast` @ `87416418657359cb625c412a48b6e1d6d41c29bd` | `31163084c279aaebd376216f0c3d5c17ad4b5fee8db49dae79c20000b5de5964` | Apache-2.0 | Source integrated; named-device quality pending |
| English (`eng`) | same commit | `7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2` | Apache-2.0 | Source integrated; named-device quality pending |

The upstream [tessdata_fast license](https://github.com/tesseract-ocr/tessdata_fast/blob/main/LICENSE) applies to both files. Handwriting is not claimed.

## Reviewed catalog artifact

| Catalog ID | Upstream repository | Artifact | Catalog license field | Distribution state | Required user/release review |
|---|---|---|---|---|---|
| `qwen2.5-1.5b-instruct-q4-k-m` | `Qwen/Qwen2.5-1.5B-Instruct-GGUF` | `qwen2.5-1.5b-instruct-q4_k_m.gguf` | `Apache-2.0` | downloaded separately; not bundled | verify upstream model card/license at acquisition and before any redistribution; retain attribution/NOTICE as required |

Catalog trust currently records exact source URL, SHA-256, byte size, architecture, quantization, format, backend/ABI/context/memory compatibility, and review state. `banglaQualityValidated` remains false despite basic device observations; license metadata must never be interpreted as quality or safety validation.

## Local unreviewed models

User-imported or workspace-discovered unknown GGUF files are classified `LOCAL_UNREVIEWED`. LAI does not claim their license, provenance, safety, compatibility, or redistribution rights. Registration must not auto-load or redistribute them.

## Future model gate

Before adding a reviewed model: record upstream owner/repository, immutable artifact identity, exact license identifier/text source, usage and redistribution restrictions, attribution/NOTICE, dataset/use-policy restrictions where applicable, quantization/conversion provenance, backend requirements, and named-device/quality evidence. Unknown or incompatible terms block catalog inclusion and bundling.
