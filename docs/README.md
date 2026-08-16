# LAI documentation map

Documentation follows the source-of-truth order: source → tests → accepted ADRs → architecture → specifications → roadmap → README → assumptions.

## Master audited documentation

- [Current implementation state](implementation/current-state.md)
- [Architecture overview](architecture/overview.md)
- [Module map](architecture/module-map.md)
- [AI architecture](architecture/ai-architecture.md)
- [Agent architecture](architecture/agent-architecture.md)
- [Security architecture](architecture/security-architecture.md)
- [Plugin architecture](architecture/plugin-architecture.md)
- [Feature matrix](product/feature-matrix.md)
- [Master roadmap](product/roadmap.md)
- [Implementation plan](implementation/implementation-plan.md)
- [Testing plan](implementation/testing-plan.md)
- [Master directive documentation coverage](implementation/directive-coverage.md)
- [PDF section-by-section compliance audit](implementation/pdf-compliance-audit.md)
- [Definition of done](product/definition-of-done.md)
- [Development policy](development/development-policy.md)
- [Licensing and distribution](legal/licensing.md)
- [ADR index and policy](decisions/README.md)

## Existing detailed documentation retained

- [System architecture blueprint](ARCHITECTURE.md)
- [Module ownership](MODULES.md)
- [Implementation status and evidence](STATUS.md)
- [Security and safety](SECURITY_AND_SAFETY.md)
- [Privacy invariants](PRIVACY_INVARIANTS.md)
- [Models and backends](MODELS_AND_BACKENDS.md)
- [Vendor backend strategy](VENDOR_BACKEND_STRATEGY.md)
- [Automation tools](AUTOMATION_TOOLS.md)
- [Bangla OCR](BANGLA_OCR.md)
- [Build and release](BUILD_AND_RELEASE.md)
- [Device testing](DEVICE_TESTING.md)
- [Diagnostics export](DIAGNOSTICS_EXPORT.md)
- [NpuHub comparison](ARCHITECTURE_COMPARISON_NPUHUB.md)
- [Recorded device evidence](device-results/2026-08-16-redmi-turbo-4-pro.md)

## Documentation rule

Do not create an empty roadmap-category file just to satisfy a desired tree. Add a document when it has audited content, and link or consolidate existing canonical material rather than duplicating it. Significant architecture changes require an ADR before implementation.
