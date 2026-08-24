# GGEN ↔ LAI Boundary & Ownership Contract

**Status:** Canonical cross-repository architecture contract
**Last reviewed:** 2026-08-24

GGEN is the creative/document product layer. LAI is the Android AI execution/runtime layer. The repositories must complement each other rather than duplicate runtime infrastructure.

## Canonical ownership

| Capability | Canonical owner | Consumer/relationship |
|---|---|---|
| Creative/document UX, canvas, vector/raster, PDF, templates | GGEN | LAI provides runtime services only |
| Creative workflow and document representation | GGEN | LAI executes permitted AI/device steps |
| AI task UX | GGEN | LAI executes normalized requests |
| AI provider adapters | **LAI** | GGEN consumes runtime contract |
| Provider routing/failover | **LAI** | GGEN consumes |
| Provider credentials and cloud transport | **LAI** | Never duplicated in GGEN |
| Local LLM inference | **LAI** | CPU/GPU/NPU runtime |
| Model registry/runtime metadata | **LAI** | GGEN requests capabilities/models |
| Device-aware scheduling | **LAI** | Android runtime concern |
| Android tool registry | **LAI** | One authority |
| Accessibility automation | **LAI** | Android authority |
| Shizuku/elevated operations | **LAI** | Typed/policy-gated |
| Android permissions/consent | **LAI** | Security boundary |
| Runtime audit/security events | **LAI** | Device/runtime authority |
| OCR user workflow | GGEN | LAI may provide device OCR runtime |
| OCR runtime/backend | **LAI** when device/runtime capability | GGEN owns product presentation/data model |
| AI-generated creative result | GGEN owns resulting document/product state | LAI provides execution/result |

## Non-duplication rules

Do not create a second implementation in GGEN of LAI-owned:

- provider adapters;
- API-key/secret storage;
- retry/failover router;
- model execution scheduler;
- Android tool registry;
- Android permission authority;
- Accessibility executor;
- Shizuku authority;
- device/thermal backend selection;
- runtime audit chain.

Likewise, LAI must not absorb GGEN's document/canvas domain model or creative UX.

## Integration direction

```text
GGEN
Creative/Product Layer
       |
       | stable AI Runtime contract
       v
LAI
Android AI Runtime
       |
 +-----+------------------+
 |         |              |
Local     Cloud        Android
AI        providers    tools
CPU/GPU/  custom       Accessibility/
NPU       endpoints    Shizuku/etc.
```

GGEN should never depend on LAI implementation internals. LAI should never depend on GGEN's creative document implementation to remain a useful Android runtime.

## Security boundary

```text
GGEN intent
   ↓
LAI policy
   ↓
permission / consent
   ↓
provider or Android tool
   ↓
normalized result
   ↓
GGEN
```

A model response is never an authorization decision.

## AI modes

LAI is the runtime owner for local, cloud, custom-endpoint, and hybrid execution. GGEN may request an execution mode or capability but does not implement another provider runtime merely to expose that product option.

## Existing overlap migration

For every duplicated capability:

1. identify both implementations;
2. assign canonical ownership;
3. document compatibility requirements;
4. introduce/verify the integration contract;
5. migrate the consumer;
6. remove or freeze the duplicate;
7. add regression coverage preventing ownership drift.

Do not delete working functionality before a migration path is verified.

## Relationship to LAI roadmap

This boundary is consistent with LAI's existing runtime structure: the repository already contains provider/model/runtime, scheduler, policy, Accessibility, Shizuku, audit, and orchestrator boundaries. The goal is to make those runtime capabilities reusable by GGEN rather than duplicate them.

## Completion criterion

The architecture is considered aligned when each capability has one canonical owner, both repositories reference this same contract, GGEN remains the creative/document product, LAI remains the Android AI runtime, and future feature work uses this boundary before adding code.
