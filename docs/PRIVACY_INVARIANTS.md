# Local-first privacy invariants

These are release-blocking product invariants, not preferences.

1. User prompts, generated responses, screen trees, screenshots, OCR text, documents, embeddings, audio and credentials never leave the device.
2. All outbound network requests are denied by policy. LAI currently has no sync, analytics, crash upload, remote inference, advertising or telemetry endpoint.
3. Network permission and transport dependencies exist only in `platform:download`.
4. Network use is inbound-only, initiated by an explicit user tap, HTTPS-only, reviewed-host-only and used solely for public model/catalog artifacts.
5. Model artifacts require a SHA-256 before the request starts and must match before activation.
6. Redirect destinations are reviewed under the same host policy.
7. Downloaded bytes remain untrusted until digest and format validation pass.
8. Local telemetry is export-only; no automatic transmission path may exist.
9. Plugins are `LOCAL_ONLY`; a future plugin loader must reject any manifest requesting network authority.
10. Accessibility, Shizuku and native runtime authority stay in separate auditable modules.
11. Web catalog bytes are accepted only after ECDSA signature verification with the embedded public key.
12. A verified cache and embedded fallback preserve the supported list offline.
13. Local file imports store no provider URI and activate only exact reviewed digest/size/GGUF bytes.
14. Retained model export occurs only to a user-selected SAF document; LAI reopens and hashes the destination, stores no URI, and never exports prompts or app data with the public model weights.

## Mechanical enforcement

`scripts/check_architecture_boundaries.py` fails CI when:

- a network transport is imported outside `platform/download`;
- another manifest declares `INTERNET`;
- Android Accessibility APIs escape `platform/accessibility`;
- Shizuku APIs escape `platform/shizuku`;
- JNI entry points/native library loading escape runtime adapters;
- core modules import Android;
- a core/platform dependency points upward;
- a known outbound analytics SDK is referenced.

Source checks are defense in depth, not a proof. Release review also inspects the merged manifest, dependency graph and runtime traffic on a physical device.
