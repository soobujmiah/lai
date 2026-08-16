#!/usr/bin/env python3
"""Validate the source catalog before its detached signature is published."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "catalog/models-v1.json"
ID = re.compile(r"^[a-z0-9][a-z0-9._-]{1,63}$")
SHA256 = re.compile(r"^[a-f0-9]{64}$")
ALLOWED_HOST_SUFFIXES = ("huggingface.co", "hf.co")
REQUIRED_FIELDS = {
    "id", "displayName", "description", "sourceRepository", "fileName", "url", "sha256",
    "bytes", "license", "architecture", "quantization", "reviewState", "banglaQualityValidated",
}


def main() -> None:
    raw = CATALOG.read_bytes()
    if len(raw) > 512 * 1024:
        raise SystemExit("Catalog exceeds 512 KiB")
    document = json.loads(raw)
    if document.get("schemaVersion") != 1:
        raise SystemExit("Unsupported catalog schema")
    if not isinstance(document.get("revision"), int) or document["revision"] < 1:
        raise SystemExit("Invalid catalog revision")
    models = document.get("models")
    if not isinstance(models, list) or not 1 <= len(models) <= 100:
        raise SystemExit("Catalog must contain 1..100 models")
    ids: set[str] = set()
    for index, model in enumerate(models):
        missing = REQUIRED_FIELDS - set(model)
        unknown = set(model) - REQUIRED_FIELDS
        if missing or unknown:
            raise SystemExit(f"Model {index}: missing={sorted(missing)} unknown={sorted(unknown)}")
        if not ID.fullmatch(model["id"]) or model["id"] in ids:
            raise SystemExit(f"Model {index}: invalid or duplicate id")
        ids.add(model["id"])
        if not SHA256.fullmatch(model["sha256"]):
            raise SystemExit(f"Model {index}: invalid SHA-256")
        if not isinstance(model["bytes"], int) or model["bytes"] <= 0:
            raise SystemExit(f"Model {index}: invalid byte size")
        parsed = urlparse(model["url"])
        host = (parsed.hostname or "").lower()
        if parsed.scheme != "https" or not any(host == suffix or host.endswith("." + suffix) for suffix in ALLOWED_HOST_SUFFIXES):
            raise SystemExit(f"Model {index}: disallowed artifact URL")
        if not isinstance(model["reviewState"], list) or "METADATA_VERIFIED" not in model["reviewState"]:
            raise SystemExit(f"Model {index}: metadata review evidence missing")
        if model["banglaQualityValidated"] is not False and "DEVICE_VALIDATED" not in model["reviewState"]:
            raise SystemExit(f"Model {index}: Bangla validation claim lacks device evidence")
    fallback_source = (
        ROOT / "core/model/src/main/kotlin/dev/lai/runtime/model/ReviewedModelCatalog.kt"
    ).read_text(encoding="utf-8")
    for model in models:
        for value in (model["id"], model["sourceRepository"], model["fileName"], model["sha256"]):
            if value not in fallback_source:
                raise SystemExit(f"Embedded fallback is out of sync for {model['id']}: {value}")
        if str(model["bytes"]) not in fallback_source.replace("_", ""):
            raise SystemExit(f"Embedded fallback byte size is out of sync for {model['id']}")
    print(f"Model catalog OK: revision={document['revision']} models={len(models)} bytes={len(raw)}")


if __name__ == "__main__":
    main()
