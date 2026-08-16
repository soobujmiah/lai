# ADR 0002: Modular local-first backbone

- Status: Accepted
- Date: 2026-08-16

## Context

LAI's Phase 1 and llama.cpp Phase 2 proved product and native-runtime paths in one Android module. That minimized initial build risk but allowed UI, network, Android authority and native adapter code to share one compile boundary. Comparison with NpuHub showed the value of pure contracts, adapter isolation, coverage ratchets, evidence-aware scheduling and source-level boundary enforcement.

## Decision

Adopt an eleven-module layered architecture:

- pure JVM: `core:contracts`, `core:policy`, `core:scheduler`, `plugins:api`;
- Android authority: `platform:download`, `platform:accessibility`, `platform:shizuku`;
- runtime adapters: `runtime:llama`, `runtime:ocr`, `runtime:orchestrator`;
- composition/product: `app`.

Require SHA-pinned artifact installation and prohibit all outbound user-data flows. Add a static architecture check and pure-JVM coverage ratchets to the first CI job.

## Consequences

Positive:

- future features cannot casually import privileged/network/native APIs;
- CPU/Vulkan/QNN routing is evidence-driven;
- core tests run without Android;
- plugin API can evolve independently;
- native and platform adapters are replaceable.

Costs:

- more Gradle modules and explicit dependencies;
- manifest/resource ownership must be maintained;
- initial CI configuration is more complex;
- feature UI remains in `app` until a later extraction to avoid premature fragmentation.

## Rejected alternatives

- Keep one module: fastest short term, unsafe for a global authority-bearing product.
- Copy all NpuHub modules: too much scope and several modules do not match LAI's product.
- Dynamic third-party plugin loading now: unacceptable supply-chain and authority risk before signing/sandbox policy exists.
