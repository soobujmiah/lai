# ADR 0004: One local-first application with signed web catalog

- Status: Accepted
- Date: 2026-08-16
- Supersedes: ADR 0003

## Context

Separate connected and air-gapped application IDs fragmented the product and did not match the intended user experience. LAI should be one app that can acquire reviewed components when requested and then run all intelligence offline.

## Decision

Ship one `dev.lai.runtime` application. Network authority remains isolated to `platform:download` and is limited to:

1. an explicitly refreshed, detached-signature-verified supported-model catalog;
2. explicit downloads of catalog artifacts with mandatory SHA-256 and exact-size verification.

The app embeds the catalog verification public key and a minimal fallback catalog. GitHub Actions stores the private ECDSA P-256 key as an encrypted secret and publishes signed catalog assets. A verified cache keeps the list available offline. Local file import remains an alternative path through Android's file picker.

Prompts, screen data, OCR, documents, generations, automation data and telemetry never enter a network request contract. Once models are installed, chat, OCR, RAG, speech and automation must work without connectivity.

## Consequences

- One product, one application ID and one upgrade path.
- Model support can expand without an APK release while remaining signature-gated.
- Catalog refresh and model download require internet; inference does not.
- The app still has Android network permission, so module boundaries, dependency checks, signed metadata and runtime traffic tests are defense in depth.
- Absolute air-gap users may import local files and deny network access through Android/firewall controls, but there is no separate LAI application.
