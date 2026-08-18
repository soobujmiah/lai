# Public repository versus commercial product

**Status:** planning document  
**Date:** 2026-08-18  
**License of the public Work:** Apache License 2.0  
**Decision:** **DECISION REQUIRED** for future commercial SKUs  
**Legal interpretation of derivative-work boundaries:** **LEGAL REVIEW REQUIRED**

## Principle

Public GitHub availability is a development and distribution fact. It is not equivalent to commercial product entitlement.

Code already distributed under Apache-2.0 remains subject to the rights granted by [`../../LICENSE`](../../LICENSE). Future capabilities that are separately authored, separately licensed, or offered only as services are not automatically released under that same grant. Whether a given future file is a Derivative Work of the public Work is **LEGAL REVIEW REQUIRED**.

The public repository must not be treated as the complete commercial product.

## Layer map

| Layer | Definition | Current state | Governing texts |
|---|---|---|---|
| Public repository | Source, docs, tests, catalog metadata, and CI descriptions on `https://github.com/soobujmiah/lai` | Public; Apache-2.0; not a fork | [`../../LICENSE`](../../LICENSE), [`../LEGAL_AND_LICENSING.md`](../LEGAL_AND_LICENSING.md) |
| Commercial product | Packaged application SKUs, store listings, paid capabilities, support entitlements | Not defined as SKUs; one application ID `dev.lai.runtime` (ADR 0004) | [`../product/COMMERCIAL_FEATURE_MATRIX.md`](../product/COMMERCIAL_FEATURE_MATRIX.md) |
| Commercial services | Hosted inference, sync, catalog extras, organization controls, usage billing | **MISSING**; current product is local-only | [`../CLOUD.md`](../CLOUD.md), [`../architecture/HYBRID_AND_PROVIDER_ARCHITECTURE.md`](../architecture/HYBRID_AND_PROVIDER_ARCHITECTURE.md) |
| Third-party components | Libraries, native engines, SDKs, fonts, icons | Declared in Gradle/CI; some registers stale | [`THIRD_PARTY_COMPLIANCE.md`](THIRD_PARTY_COMPLIANCE.md) |
| User data | Prompts, generations, screens, OCR, documents, embeddings, credentials, audit content | Must remain on device unless an explicit future consent path exists | [`../PRIVACY_INVARIANTS.md`](../PRIVACY_INVARIANTS.md) |
| User-provided models | Imported or workspace-discovered GGUF and future formats | Classified `LOCAL_UNREVIEWED` unless catalog-matched | [`MODEL_AND_DATA_LICENSE_POLICY.md`](MODEL_AND_DATA_LICENSE_POLICY.md), [`../../MODEL_LICENSES.md`](../../MODEL_LICENSES.md) |
| Premium modules | Future separately licensed capabilities | Not present | [`../architecture/COMMERCIAL_MODULE_BOUNDARIES.md`](../architecture/COMMERCIAL_MODULE_BOUNDARIES.md) |

## Public repository

The public repository is the development and reference tree. It currently contains LAI-authored source and documentation under Apache-2.0, plus references to third-party works that are not vendored.

Facts:

- Visibility: public.
- Default branch: `main`.
- First commit: `f0f1b81` (2026-08-16).
- Audit commit referenced by the license audit: `94d88aa`.
- Tags exist from `v0.1.0` through at least `v0.9.7`.
- Releases publish APK artifacts. Some historical releases used debug signing; later notes record a production keystore held outside Git.

The public repository does not contain model weights, vendor SDKs, keystores, or customer data. See [`../security/COMMERCIAL_SECRET_POLICY.md`](../security/COMMERCIAL_SECRET_POLICY.md).

## Commercial product

Commercial product entitlement is a product and contract concept. It is not created by cloning the repository.

A recipient of the public Work may build and run that Work under Apache-2.0. That recipient does not automatically receive:

- a paid SKU,
- a store listing identity,
- hosted services,
- proprietary modules that were never published under Apache-2.0,
- trademarks,
- production signing identity,
- support obligations,
- catalog signing private keys.

Conversely, paying for a future SKU does not remove Apache-2.0 rights the customer already has in public copies.

## Commercial services

Services are offered under separate terms. A service may refuse service to a particular account without relicensing the client. Service access is not a substitute for the Apache-2.0 grant on published source.

Current source has no remote inference, sync, analytics, or crash-upload endpoint ([`../PRIVACY_INVARIANTS.md`](../PRIVACY_INVARIANTS.md)). Introducing a service requires an ADR, privacy review, and explicit user consent. Silent cloud fallback is forbidden by existing product docs.

## Third-party components

Third-party licenses travel with the corresponding work. LAI product licensing cannot relicense AndroidX, Kotlin, OkHttp, Shizuku, llama.cpp, Qwen weights, or future SDKs.

## User data

User data is not a LAI-licensed work. It is not an entitlement object. It must not be uploaded, sold, or used for training by LAI unless a future, separately documented, explicit consent path exists. No such path exists in current source.

## User-provided models

Users may import models they already have rights to use. LAI does not acquire those rights. LAI does not redistribute user-imported unknown GGUF files. Catalog metadata is not a license grant. See [`MODEL_AND_DATA_LICENSE_POLICY.md`](MODEL_AND_DATA_LICENSE_POLICY.md).

## Premium modules

Future premium modules may be:

- developed in a non-public tree,
- distributed as signed plugins,
- enabled by entitlement,
- offered only as services.

None of those mechanisms converts already published Apache-2.0 files into proprietary files.

A module that merely wraps, modifies, or is based on the public Work may still be a Derivative Work under Apache-2.0. **LEGAL REVIEW REQUIRED** before any “proprietary” label is applied.

## Architectural implication

Design future proprietary or commercial functionality so that it can be separated at an API, process, artifact, or service boundary. Do not assume every module can automatically be proprietary.

Candidate boundaries are listed in [`../architecture/COMMERCIAL_MODULE_BOUNDARIES.md`](../architecture/COMMERCIAL_MODULE_BOUNDARIES.md). Public-development rules are in [`../governance/PUBLIC_DEVELOPMENT_POLICY.md`](../governance/PUBLIC_DEVELOPMENT_POLICY.md).

## Decision

**DECISION REQUIRED** for SKU naming and which capabilities leave the public tree.

**LEGAL REVIEW REQUIRED** for derivative-work classification of any proposed premium module.
