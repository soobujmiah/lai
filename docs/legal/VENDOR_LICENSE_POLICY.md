# Vendor license policy

**Status:** canonical vendor / accelerator gate  
**Date:** 2026-08-18  
**Architecture constraint:** ADR 0005 — Snapdragon-first, vendor-neutral backends  
**Related:** [`../VENDOR_BACKEND_STRATEGY.md`](../VENDOR_BACKEND_STRATEGY.md), [`../BUILD_AND_RELEASE.md`](../BUILD_AND_RELEASE.md)

Vendor SDK terms never become LAI’s project license. A vendor binary in an official APK does not relicense the public Apache-2.0 tree, and the public tree does not relicense the vendor SDK.

## In-scope families

| Family | Current tree | Gate |
|---|---|---|
| Qualcomm QNN / QAIRT | Absent. Empty adapters forbidden | Licensed acquisition, redistribution clause, SHA-256 pin, no public cache of proprietary payloads |
| Vulkan | Device driver on phone; CI uses distro SPIR-V/glslang/libvulkan-dev to compile | Build-package licenses UNVERIFIED; driver is not redistributed by LAI |
| Android NDK / SDK | CI only; not committed | Google/SDK terms; do not vendor into Git |
| GPU libraries (Adreno, future) | Not vendored | Vendor agreement before ship |
| NPU runtimes | Not present | Dedicated runtime module only; no vendor types in `core` |
| Future acceleration backends | Planned behind opaque backend IDs | Same intake as [`THIRD_PARTY_INTAKE.md`](THIRD_PARTY_INTAKE.md) |

## Rules

1. Vendor SDKs, headers that are under NDA, and proprietary `.so` files stay out of the public repository.
2. CI may fetch a vendor SDK only from an access-controlled, license-compliant URL into runner-temporary storage, then delete it.
3. Generic scheduler and contracts receive descriptors, capabilities, events, and metrics — not vendor types (ADR 0005).
4. A backend is added only with real integration, truthful probes, licensing review, and a physical-validation plan.
5. Production notes must not describe a stub as accelerated.
6. Combining a vendor SDK with a future proprietary LAI module requires **LEGAL REVIEW REQUIRED** of both the vendor agreement and Apache-2.0 obligations for any included public Work.

## llama.cpp / Vulkan note

llama.cpp is MIT and is fetched at a pinned commit; it is not a Qualcomm SDK. Vulkan *compilation* uses distro packages; Vulkan *execution* uses the device driver. Those are distinct from QAIRT. See [`THIRD_PARTY_COMPLIANCE.md`](THIRD_PARTY_COMPLIANCE.md) section D.

## Decision

No vendor SDK is added in this phase. QNN remains UNKNOWN until licensed acquisition.
