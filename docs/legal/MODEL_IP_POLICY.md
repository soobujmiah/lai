# Model IP policy

**Status:** canonical model and data IP policy  
**Date:** 2026-08-18  
**Register:** [`../../MODEL_LICENSES.md`](../../MODEL_LICENSES.md)  
**Pointer from:** [`MODEL_AND_DATA_LICENSE_POLICY.md`](MODEL_AND_DATA_LICENSE_POLICY.md)

A downloadable model is not a commercially redistributable model. Integrity metadata is not a license grant.

## Classes

| Class | Meaning | LAI action |
|---|---|---|
| A. User-downloaded model | User fetches bytes from upstream | LAI may assist download of a *reviewed* catalog artifact; LAI does not become the licensor |
| B. LAI-referenced model | Catalog / embedded metadata | Public URLs, hashes, license field only |
| C. LAI-distributed model | LAI ships weights in an APK, archive, or CDN | Not done. Requires completed record + **LEGAL REVIEW REQUIRED** |
| D. Commercially licensed model | Separate vendor contract | Not present |
| E. Unknown / unreviewed model | User import / workspace discovery | `LOCAL_UNREVIEWED`; no LAI license claim; no redistribution |

## Required record (reviewed models)

| Field | Required |
|---|---|
| Model name / catalog ID | Yes |
| Exact upstream repository | Yes |
| Exact version / revision | Yes (immutable artifact) |
| File name | Yes |
| Hash (SHA-256) | Yes |
| Size | Yes when known |
| License | Yes — text for that artifact |
| Copyright holder | Yes if stated upstream |
| Redistribution status | permitted / restricted / unknown |
| Commercial-use status | permitted / restricted / unknown |
| Modification / quantization status | Yes |
| Attribution requirements | Yes |
| Required notices | Yes |
| Restrictions (use, output, hosting) | Yes if any |
| Bundled / download-only / local-only | Yes |

Unknown commercial-use or redistribution **fails closed** for catalog inclusion, bundling, and LAI redistribution.

## Current reviewed artifact

| Field | Value |
|---|---|
| Catalog ID | `qwen2.5-1.5b-instruct-q4-k-m` |
| Upstream | `Qwen/Qwen2.5-1.5B-Instruct-GGUF` |
| File | `qwen2.5-1.5b-instruct-q4_k_m.gguf` |
| SHA-256 | `6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e` |
| Size | 1,117,320,736 bytes |
| License field | Apache-2.0; upstream LICENSE file is Apache License 2.0 (read 2026-08-18) |
| Class | B (referenced) + A (user download). Not C |
| Commercial-use / LAI redistribution | **LEGAL REVIEW REQUIRED** before any LAI redistribution |
| Bundled | No |

## Datasets

No training or evaluation dataset is in Git. Planned Bangla OCR / handwriting corpora remain blocked until licensed. Synthetic LAI-authored fixtures must be labeled and must not include uncleared third-party corpus text.

## Planned models (not licensed in-tree)

Tesseract/`ben.traineddata`, Granite Embedding 107M, Whisper large-v3 Q4, QNN context binaries: **PLANNED**, intake incomplete. See [`THIRD_PARTY_COMPLIANCE.md`](THIRD_PARTY_COMPLIANCE.md) section E.

## Cloud provider models

Remote model names are not LAI artifacts. Provider terms govern them. No prompt or document is sent to a provider unless a documented consent path exists. None exists in current source ([`../PRIVACY_INVARIANTS.md`](../PRIVACY_INVARIANTS.md)).
