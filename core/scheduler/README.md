# `core:scheduler`

Pure-JVM, vendor-neutral backend routing. A backend is eligible only when compile and runtime-probe evidence exist, model-format compatibility is declared, and memory policy passes. Any non-CPU accelerator additionally requires physical-device validation and can be blocked by thermal or low-battery policy. Real measured performance and adapter/model preference guide selection; synthetic benchmarks are not accepted.

Backend IDs and static facts are supplied by runtime adapters. This module must not contain hardware-vendor names, SDK terminology, or vendor-specific scheduling branches.
