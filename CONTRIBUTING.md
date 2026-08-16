# Contributing to LAI

## Documentation is part of the implementation

Every behavioral change must update documentation in the same pull request:

- architecture/data flow → `docs/ARCHITECTURE.md`;
- implementation boundary → `docs/STATUS.md`;
- tool schema/policy → `docs/AUTOMATION_TOOLS.md`;
- threat/control → `docs/SECURITY_AND_SAFETY.md`;
- build/secrets/dependency → `docs/BUILD_AND_RELEASE.md`;
- device behavior → `docs/DEVICE_TESTING.md` and redacted evidence.

A feature without current docs is incomplete. Do not describe scaffolding as working acceleration or recognition.

## Source-only rule

Never commit SDKs, model weights, APK/AAB/AAR/SO files, QNN contexts, keystores, generated build output, or Gradle wrapper JAR. Run:

```bash
bash scripts/validate_repo.sh
```

Compilation and lint run in GitHub Actions.

## Engineering rules

1. Preserve module boundaries and inject implementations through interfaces.
2. No raw shell API or `sh -c` from model/user strings.
3. New consequential tools require trusted confirmation and threat review.
4. Bound depth, count, time, output, allocation, and network size where applicable.
5. Keep telemetry and model internals behind Developer Mode.
6. Add tests for protocol changes, Bangla UTF-8, rejection paths, and injection attempts.
7. Include physical-device evidence for hardware/performance claims.
8. Never log prompts, entered text, screen trees, captures, tokens, or secrets by default.

## Commit style

Use focused conventional commits, for example:

```text
feat(automation): add confirmed long-click tool
fix(models): preserve partial file on HTTP range resume
docs(qnn): document HTP context compatibility key
```

## Pull-request review gates

- CI green;
- docs updated;
- no repository size/binary/secret violation;
- explicit real/scaffold/planned status;
- safety review for authority changes;
- license review for a new dependency/model/runtime;
- device evidence when platform behavior is involved.
