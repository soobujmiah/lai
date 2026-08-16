# ADR 0003: Connected and air-gapped editions

- Status: Accepted
- Date: 2026-08-16

## Context

LAI needs user-initiated model downloads and also a distribution with a mechanically enforceable promise that the app cannot access the network. A runtime toggle cannot remove an Android permission and is therefore insufficient for high-assurance users.

## Decision

Build two product flavors from the same reviewed source:

- `connected`: `dev.lai.runtime`, with network permission isolated to digest-pinned artifact downloads;
- `airgap`: `dev.lai.runtime.airgap`, whose flavor manifest removes `INTERNET` and `ACCESS_NETWORK_STATE` from the merged APK.

The air-gapped edition imports the immutable reviewed model through Android's Storage Access Framework. LAI stores no source URI and activates bytes only after SHA-256, exact-size and GGUF validation.

GitHub Actions builds both APKs and uses `apkanalyzer` on final merged artifacts. CI fails if the connected artifact lacks its intended download permission or the air-gapped artifact contains a network permission.

## Consequences

- Users can install both editions side by side.
- The air-gapped edition cannot download models, sync, upload or use remote services even if future code attempts a network call.
- A model must be transferred to the device through a user-controlled external channel before import.
- Test/release workflows and artifact naming are more complex.
- Both editions still require the same local inference, policy and device validation.
