# Definition of done

A capability is complete only when every applicable item is checked with evidence. Compilation alone is not completion.

## Required checklist

- [ ] Current behavior and target behavior are documented.
- [ ] Architecture and interfaces were reviewed.
- [ ] Any required ADR is accepted.
- [ ] Implementation is complete without fake capability or placeholder success.
- [ ] Unit/integration/instrumentation/device tests exist as applicable.
- [ ] Relevant tests actually passed and the environment/command/result/limitations are recorded.
- [ ] Security boundaries, abuse cases, permission gates, and failure behavior were reviewed.
- [ ] Privacy/data-flow review confirms no hidden upload, telemetry, or cloud fallback.
- [ ] Licensing, model/dependency terms, attribution, and distribution obligations were reviewed.
- [ ] Accessibility, Bangla/English localization, and adaptive input/layout implications were considered.
- [ ] Cancellation, timeout, storage pressure, process death, and recovery behavior are documented/tested as applicable.
- [ ] User documentation is updated.
- [ ] Developer documentation, feature matrix, current state, and roadmap are updated.
- [ ] Known limitations and evidence level are explicit.
- [ ] Migration and rollback are documented and tested for stateful changes.
- [ ] CI/source/architecture/catalog/release gates pass.

## Evidence labels

Use precise evidence: **implemented but untested**, **tested in emulator**, **tested on named physical device**, or **production validated**. Do not convert build success, theoretical SDK compatibility, or one device result into broader claims.

## Phase transition gate

Before moving phases, record YES/NO for architecture, interfaces, security, privacy, tests, known limitations, user docs, developer docs, and ADR requirement/acceptance. Any required NO blocks advancement.

## Non-completing work

The following never counts as completed capability by itself: an interface with no adapter; a button with no real operation; a mocked backend presented as real; unverified hardware flags; a benchmark not run; a placeholder OCR/model/server; documentation describing future behavior as current; or security controls that are not enforced in code.
