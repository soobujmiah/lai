# Legal, licensing, and commercial documentation

This directory is the legal and commercialization map for LAI. It does not replace [`../../LICENSE`](../../LICENSE), the root third-party registers, or the current-state audit in [`../LEGAL_AND_LICENSING.md`](../LEGAL_AND_LICENSING.md).

This material is engineering and product-planning documentation. It is not legal advice. Interpretive conclusions are marked **LEGAL REVIEW REQUIRED**. No item in this tree changes the current Apache License 2.0 grant.

## Canonical policies (one source each)

| Policy | File |
|---|---|
| Commercial ownership and IP | [`COMMERCIAL_IP_POLICY.md`](COMMERCIAL_IP_POLICY.md) |
| IP decision register | [`OWNERSHIP_DECISIONS.md`](OWNERSHIP_DECISIONS.md) |
| Contributor rights | [`CONTRIBUTOR_RIGHTS.md`](CONTRIBUTOR_RIGHTS.md) |
| AI code provenance | [`AI_CODE_PROVENANCE.md`](AI_CODE_PROVENANCE.md) |
| Third-party intake | [`THIRD_PARTY_INTAKE.md`](THIRD_PARTY_INTAKE.md) |
| Model IP | [`MODEL_IP_POLICY.md`](MODEL_IP_POLICY.md) |
| Vendor SDK / NPU / GPU | [`VENDOR_LICENSE_POLICY.md`](VENDOR_LICENSE_POLICY.md) |
| Trademark | [`TRADEMARK_POLICY.md`](TRADEMARK_POLICY.md) |
| Release compliance | [`RELEASE_COMPLIANCE.md`](RELEASE_COMPLIANCE.md) |
| Proprietary / premium boundaries | [`PROPRIETARY_BOUNDARIES.md`](PROPRIETARY_BOUNDARIES.md) |
| Distribution map to root registers | [`licensing.md`](licensing.md) |

Older filenames in this directory are **pointers** to the canonical file of the same topic. Do not add a second conflicting policy.

## Binding facts and inventories

| Topic | Location |
|---|---|
| Project license text | [`../../LICENSE`](../../LICENSE) |
| Current license audit (informational; not a relicensing change) | [`../LEGAL_AND_LICENSING.md`](../LEGAL_AND_LICENSING.md) |
| Ownership facts (no invented entity) | [`OWNERSHIP_MODEL.md`](OWNERSHIP_MODEL.md) |
| Licensing strategy options | [`LICENSING_STRATEGY.md`](LICENSING_STRATEGY.md) |
| Public vs commercial layers | [`PUBLIC_VS_COMMERCIAL_BOUNDARY.md`](PUBLIC_VS_COMMERCIAL_BOUNDARY.md) |
| Third-party inventory | [`THIRD_PARTY_COMPLIANCE.md`](THIRD_PARTY_COMPLIANCE.md) |
| Third-party notices / licenses | [`../../THIRD_PARTY_NOTICES.md`](../../THIRD_PARTY_NOTICES.md), [`../../THIRD_PARTY_LICENSES.md`](../../THIRD_PARTY_LICENSES.md) |
| Model register | [`../../MODEL_LICENSES.md`](../../MODEL_LICENSES.md) |
| SBOM design | [`SBOM_AND_PROVENANCE.md`](SBOM_AND_PROVENANCE.md) |

## Related architecture and product

[`../architecture/ENTITLEMENT_ARCHITECTURE.md`](../architecture/ENTITLEMENT_ARCHITECTURE.md) · [`../architecture/COMMERCIAL_MODULE_BOUNDARIES.md`](../architecture/COMMERCIAL_MODULE_BOUNDARIES.md) · [`../architecture/HYBRID_AND_PROVIDER_ARCHITECTURE.md`](../architecture/HYBRID_AND_PROVIDER_ARCHITECTURE.md) · [`../product/COMMERCIAL_FEATURE_MATRIX.md`](../product/COMMERCIAL_FEATURE_MATRIX.md) · [`../product/COMMERCIAL_PRODUCT_STRUCTURE.md`](../product/COMMERCIAL_PRODUCT_STRUCTURE.md) · [`../security/COMMERCIAL_SECRET_POLICY.md`](../security/COMMERCIAL_SECRET_POLICY.md) · [`../governance/PUBLIC_DEVELOPMENT_POLICY.md`](../governance/PUBLIC_DEVELOPMENT_POLICY.md)

## Operating rule

```text
DOCUMENTATION FIRST → ARCHITECTURE REVIEW → LICENSING/IP REVIEW
→ IMPLEMENTATION → TESTING → RELEASE REVIEW
```

Documentation existence is not implementation authorization. Agents read this index and [`OWNERSHIP_DECISIONS.md`](OWNERSHIP_DECISIONS.md) before adding a dependency, model, SDK, or commercial module.
