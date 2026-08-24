# GGEN Capability Contract for LAI

## Purpose

This is the initial contract boundary for serving GGEN without coupling GGEN to LAI internals.

## Envelope

```text
protocol_version
request_id
capability
provider_id
provider_version
model_id
input
options
privacy
limits
```

Response:

```text
request_id
capability
status
result
usage
execution_evidence
error
```

## Evidence fields

At minimum:

```text
api_available
backend_available
backend_accepted
operations_delegated
execution_completed
device_validated
performance_measured
backend_id
model_id
device_id
```

A field being false/unknown is valid and must not be upgraded by the client.

## Capability semantics

### text.generate
Input: conversation/messages plus generation constraints.
Output: text or normalized stream events and optional generation metrics.

### ocr.extract
Input: image/document reference plus language/configuration.
Output: versioned OCR blocks, confidence, geometry and language metadata.

### image.generate / image.edit
These remain provider capability contracts. The result must identify output media, dimensions, format and provenance metadata where available.

### embedding.create
Input: text/multimodal content.
Output: bounded embedding vector metadata or provider-defined reference when the vector itself is not transferred.

### tool.execute / agent.run
These are high-risk capabilities. LAI must apply policy, risk checks, user confirmation and audit before authority is exercised.

## Transport neutrality

The contract is independent of HTTP, Binder, Unix sockets or another transport. Transport selection is an implementation concern and must not leak into GGEN's core domain model.

## Compatibility

Unknown capabilities must return an explicit unsupported result. Unknown optional fields must be safely ignored. Breaking schema changes require a new protocol version.
