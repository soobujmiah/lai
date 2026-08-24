# LAI ↔ GGEN Integration Boundary

**Status:** Architecture decision baseline
**Date:** 2026-08-24

## Principle

LAI and GGEN are independent products with a capability contract between them.

- GGEN is the creative/document application.
- LAI is the intelligence/runtime/agent/device platform.

LAI must not require GGEN to operate. GGEN must not require LAI to operate.

## LAI-facing contract

LAI may expose capabilities such as:

- `text.generate`
- `vision.analyze`
- `ocr.extract`
- `image.generate`
- `image.edit`
- `embedding.create`
- `agent.run`
- `tool.execute`

The contract must specify capability identity, request schema, response schema, streaming/cancellation behavior, error classes, provider metadata, privacy/egress policy, authorization requirements and audit correlation identifiers.

## Separation of concerns

GGEN owns creative/document semantics. LAI owns intelligence and execution semantics.

LAI must not become a graphics editor merely because GGEN requests image or document intelligence. GGEN remains responsible for presenting and applying returned results to its artifact model.

## Provider neutrality

A GGEN request can be served by:

- LAI local CPU runtime;
- LAI GPU/NPU runtime;
- a cloud provider;
- an OpenAI-compatible service;
- a custom endpoint;
- a remote LAI instance.

The LAI contract must not require one model family or accelerator.

## Security boundary

GGEN's capability request is not equivalent to permission to automate Android. Any action that touches Accessibility, Shizuku, files outside the application sandbox, shell execution or other consequential device operations remains subject to LAI's typed-operation policy, confirmation and audit mechanisms.

## Protocol layering

MCP is a suitable tool/resource interoperability layer. A2A is a suitable agent-to-agent delegation layer. LAI policy, consent, audit and replay protection remain above these protocols. Protocol adoption does not remove LAI's governance layer.

## Implementation order

1. Freeze capability names and schemas.
2. Freeze error and cancellation semantics.
3. Define provider discovery and health model.
4. Define local/remote transport adapters.
5. Implement a minimal LAI provider.
6. Implement a cloud provider without LAI.
7. Test GGEN with LAI absent.
8. Test GGEN with LAI present.
9. Add MCP/A2A adapters only where their boundaries are justified.
