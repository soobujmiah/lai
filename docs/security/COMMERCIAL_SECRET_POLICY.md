# Commercial secret policy

**Status:** operating policy  
**Date:** 2026-08-18  
**Existing secret rules:** [`../development/development-policy.md`](../development/development-policy.md), [`../SECURITY_AND_SAFETY.md`](../SECURITY_AND_SAFETY.md), [`../../SECURITY.md`](../../SECURITY.md), [`../BUILD_AND_RELEASE.md`](../BUILD_AND_RELEASE.md)

Public development must not expose future commercial assets. This policy lists classes that must never enter the public repository.

## Forbidden in the public tree

| Class | Examples | Current handling |
|---|---|---|
| API keys | Cloud provider keys, Hugging Face tokens, GitHub PATs | `validate_repo.sh` scans some GitHub token patterns |
| Signing keys | Upload keystore, Play App Signing, catalog ECDSA private key | Catalog private key is a GitHub secret; keystore is a secret |
| Private certificates | TLS client certs, enterprise MDM certs | Not present |
| Production credentials | Store consoles, billing, license-server admin | Not present |
| License-server secrets | Entitlement signing keys, revocation HMAC | Not present; must stay out of Git if created |
| Proprietary model weights | Unreleased fine-tunes, vendor contexts | `*.gguf` and related binaries forbidden |
| Commercial backend credentials | QNN download credentials, vendor portals | Documented as secrets-only if ever used |
| Customer data | Prompts, chats, device identifiers collected from users | Product must not collect them into Git or CI logs |
| Private datasets | Licensed Bangla corpora, customer documents | Fail closed until licensed and stored off-Git |
| Private business logic intentionally kept proprietary | Premium module source if a private tree is chosen | Must not be committed to `soobujmiah/lai` |
| Unreleased commercial algorithms | Unpublished ranking, entitlement crypto | Keep in the private tree or do not exist yet |
| Private infrastructure configuration | Production hosts, internal URLs with credentials | Not present |

## Allowed in the public tree

- Catalog **public** key (`catalog/catalog-public-key.pem`)
- Reviewed model **hashes and public URLs**
- Architecture and entitlement **designs**
- Dependency coordinates
- Debug-signing discussion (not the production keystore)

## Agent rules

AI agents must not request, print, or commit the forbidden classes. If a secret appears in chat or logs, the credential is rotated. Agents must not invent placeholder private keys that look real.

## Decision

Policy is documentation. No new scanners are added in this phase.
