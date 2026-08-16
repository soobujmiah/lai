#!/usr/bin/env python3
"""Validate the master-directive documentation contract without compiling Android source."""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

REQUIRED = (
    "docs/README.md",
    "docs/implementation/current-state.md",
    "docs/architecture/overview.md",
    "docs/architecture/system-architecture.md",
    "docs/architecture/module-map.md",
    "docs/architecture/ai-architecture.md",
    "docs/architecture/agent-architecture.md",
    "docs/architecture/security-architecture.md",
    "docs/architecture/plugin-architecture.md",
    "docs/product/feature-matrix.md",
    "docs/product/roadmap.md",
    "docs/product/definition-of-done.md",
    "docs/implementation/implementation-plan.md",
    "docs/implementation/testing-plan.md",
    "docs/implementation/directive-coverage.md",
    "docs/implementation/pdf-compliance-audit.md",
    "docs/decisions/README.md",
    "docs/development/development-policy.md",
    "docs/ROADMAP.md",
    "THIRD_PARTY_NOTICES.md",
    "THIRD_PARTY_LICENSES.md",
    "MODEL_LICENSES.md",
)

ALLOWED_STATUSES = {
    "IMPLEMENTED",
    "PARTIAL",
    "EXPERIMENTAL",
    "PLANNED",
    "MISSING",
    "DEPRECATED",
    "UNKNOWN",
}

TARGET_FEATURES = (
    "Managed localhost LLM server",
    "Universal AI Gateway",
    "Multi-backend inference",
    "Multi-step agent runtime",
    "Agent task center",
    "Diff/checkpoint/rollback",
    "Project-centric system",
    "Code editor/LSP",
    "Full terminal",
    "Git workbench",
    "Build center",
    "Linux runtime manager",
    "Local RAG",
    "Multimodal architecture",
    "Project Trust",
    "Backup and recovery",
    "Storage-pressure manager",
    "Performance center",
    "Vector Studio",
    "Paint Studio",
    "3D Studio",
    "Plugin API",
    "Plugin Manager",
    "Remote development",
    "Remote server management",
    "Local API/integrations",
)


def fail(message: str) -> None:
    print(f"Documentation policy failure: {message}", file=sys.stderr)
    raise SystemExit(1)


def validate_required() -> None:
    for relative in REQUIRED:
        path = ROOT / relative
        if not path.is_file() or path.stat().st_size < 200:
            fail(f"required substantive document missing or too small: {relative}")


def validate_architecture_documents() -> None:
    documents = (
        "overview.md",
        "system-architecture.md",
        "module-map.md",
        "ai-architecture.md",
        "agent-architecture.md",
        "security-architecture.md",
        "plugin-architecture.md",
    )
    required_topics = (
        "purpose",
        "responsibilit",
        "interface",
        "dependenc",
        "lifecycle",
        "data flow",
        "security",
        "failure",
        "testing",
        "extension",
    )
    for name in documents:
        text = (ROOT / "docs/architecture" / name).read_text(encoding="utf-8").lower()
        missing = [topic for topic in required_topics if topic not in text]
        if missing:
            fail(f"architecture/{name} missing directive topics: {', '.join(missing)}")


def validate_roadmap() -> None:
    text = (ROOT / "docs/ROADMAP.md").read_text(encoding="utf-8")
    phases = [int(value) for value in re.findall(r"^## Phase (\d+) —", text, re.MULTILINE)]
    if phases != list(range(15)):
        fail(f"canonical roadmap phases must be exactly 0..14; found {phases}")
    required_fields = (
        "Objective",
        "Current",
        "Target",
        "Dependencies",
        "Deliverables",
        "Architecture",
        "Security/privacy",
        "Tests",
        "Documentation",
        "Acceptance",
        "Rollback/migration",
    )
    phase_blocks = re.split(r"^## Phase \d+ —.*$", text, flags=re.MULTILINE)[1:16]
    for index, block in enumerate(phase_blocks):
        missing = [field for field in required_fields if f"**{field}" not in block]
        if missing:
            fail(f"roadmap Phase {index} is missing fields: {', '.join(missing)}")


def validate_feature_matrix() -> None:
    text = (ROOT / "docs/product/feature-matrix.md").read_text(encoding="utf-8")
    expected_columns = (
        "Feature",
        "Current status",
        "Existing implementation",
        "Target state",
        "Priority",
        "Dependencies",
        "Security impact",
        "Privacy impact",
        "Testing requirement",
        "Documentation",
    )
    header = next((line for line in text.splitlines() if line.startswith("| Feature |")), "")
    for column in expected_columns:
        if column not in header:
            fail(f"feature matrix header missing column: {column}")
    statuses = re.findall(r"^\| [^|]+ \| ([A-Z]+) \|", text, re.MULTILINE)
    unknown = sorted(set(statuses) - ALLOWED_STATUSES)
    if unknown:
        fail(f"feature matrix uses invalid statuses: {unknown}")
    for feature in TARGET_FEATURES:
        if f"| {feature} |" not in text:
            fail(f"feature matrix missing directive target: {feature}")


def validate_compliance_rows() -> None:
    text = (ROOT / "docs/implementation/pdf-compliance-audit.md").read_text(encoding="utf-8")
    for section in range(1, 49):
        if not re.search(rf"^\| {section} (?:[^0-9]|$)", text, re.MULTILINE):
            fail(f"PDF compliance audit missing section {section}")


def validate_local_links() -> None:
    markdown_files = [
        ROOT / "README.md",
        ROOT / "PROJECT_STATE.md",
        ROOT / "CONTRIBUTING.md",
        ROOT / "SECURITY.md",
        ROOT / "THIRD_PARTY_NOTICES.md",
        ROOT / "THIRD_PARTY_LICENSES.md",
        ROOT / "MODEL_LICENSES.md",
        *(ROOT / "docs").rglob("*.md"),
    ]
    pattern = re.compile(r"\[[^\]]*\]\(([^)]+)\)")
    for path in markdown_files:
        text = path.read_text(encoding="utf-8")
        for match in pattern.finditer(text):
            target = match.group(1).split("#", 1)[0]
            if not target or "://" in target or target.startswith("mailto:"):
                continue
            destination = (path.parent / target).resolve()
            if not destination.exists():
                fail(f"broken local link in {path.relative_to(ROOT)}: {target}")


def main() -> None:
    validate_required()
    validate_architecture_documents()
    validate_roadmap()
    validate_feature_matrix()
    validate_compliance_rows()
    validate_local_links()
    print(
        "Documentation policy OK: required files, architecture topics, Phase 0-14 roadmap, "
        "feature matrix, PDF sections 1-48, and local links"
    )


if __name__ == "__main__":
    main()
