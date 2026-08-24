# Cross-Repository Tool Inventory Method

The detailed inventory must be evidence-backed. Do not count roadmap bullets as implemented tools.

For every tool/capability record:

```text
id
name
module/path
status
owner
purpose
inputs
outputs
side_effects
network dependency
AI dependency
privacy class
security risk
runtime dependency
automated tests
device evidence
quality/reference tools
reuse boundary
migration action
priority
```

Status vocabulary is aligned with `PROJECT_STATE.md`: Implemented, Build verified, Device validated, Scaffold, Pending/Planned.

Inventory order:

1. Contracts/interfaces
2. Concrete implementations
3. Policy/security gates
4. Runtime dependencies
5. Tests
6. Physical-device evidence
7. Overlap with GGEN
8. Canonical ownership
9. External quality research
10. Integration/migration action

This inventory is a planning artifact; it must not become an excuse to mark unimplemented capabilities as complete.
