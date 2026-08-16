#!/usr/bin/env python3
"""Enforce LAI's local-first module and authority boundaries without Android tooling."""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SKIP = {".git", ".gradle", ".idea", "build", ".cxx", "node_modules"}

NETWORK_TRANSPORT = (
    re.compile(r"^\s*import\s+okhttp3(\.|$)"),
    re.compile(r"^\s*import\s+retrofit2(\.|$)"),
    re.compile(r"^\s*import\s+io\.ktor\.client(\.|$)"),
    re.compile(r"^\s*import\s+java\.net\.(URL|URLConnection|HttpURLConnection|Socket)(\.|$)"),
)
ACCESSIBILITY_RUNTIME = (
    re.compile(r"^\s*import\s+android\.accessibilityservice(\.|$)"),
    re.compile(r"^\s*import\s+android\.view\.accessibility(\.|$)"),
)
SHIZUKU_RUNTIME = (re.compile(r"^\s*import\s+rikka\.shizuku(\.|$)"),)
NATIVE_ENTRY = (re.compile(r"\bexternal\s+fun\b"), re.compile(r"\bSystem\.loadLibrary\s*\("))
ANALYTICS_MARKERS = (
    "firebase-analytics",
    "com.google.firebase.analytics",
    "io.sentry",
    "com.amplitude",
    "com.mixpanel",
    "appcenter-analytics",
)


def source_files() -> list[Path]:
    return sorted(
        p for p in ROOT.rglob("*")
        if p.is_file() and p.suffix in {".kt", ".kts", ".java", ".xml"} and not any(part in SKIP for part in p.parts)
    )


def allowed(rel: str, prefixes: tuple[str, ...]) -> bool:
    return rel.startswith(prefixes)


def project_dependencies(build_file: Path) -> list[str]:
    text = build_file.read_text(encoding="utf-8")
    return re.findall(r'project\("(:[A-Za-z0-9:_-]+)"\)', text)


def main() -> None:
    violations: list[str] = []
    for path in source_files():
        rel = path.relative_to(ROOT).as_posix()
        text = path.read_text(encoding="utf-8", errors="replace")
        lines = text.splitlines()
        for number, line in enumerate(lines, 1):
            if not allowed(rel, ("platform/download/",)):
                for pattern in NETWORK_TRANSPORT:
                    if pattern.search(line):
                        violations.append(f"{rel}:{number}: network transport must stay in platform/download")
            if not allowed(rel, ("platform/accessibility/",)):
                for pattern in ACCESSIBILITY_RUNTIME:
                    if pattern.search(line):
                        violations.append(f"{rel}:{number}: Accessibility runtime must stay in platform/accessibility")
            if not allowed(rel, ("platform/shizuku/",)):
                for pattern in SHIZUKU_RUNTIME:
                    if pattern.search(line):
                        violations.append(f"{rel}:{number}: Shizuku API must stay in platform/shizuku")
            if not allowed(rel, ("runtime/",)):
                for pattern in NATIVE_ENTRY:
                    if pattern.search(line):
                        violations.append(f"{rel}:{number}: native entry points must stay in runtime adapters")
        lowered = text.lower()
        for marker in ANALYTICS_MARKERS:
            if marker in lowered:
                violations.append(f"{rel}: outbound analytics dependency/API is forbidden")
        if "android.permission.INTERNET" in text and rel != "platform/download/src/main/AndroidManifest.xml":
            violations.append(f"{rel}: INTERNET permission belongs only to platform/download")
        if rel.startswith("core/") and re.search(r"^\s*import\s+android\.", text, re.MULTILINE):
            violations.append(f"{rel}: pure core modules cannot import Android APIs")

    for build_file in ROOT.rglob("build.gradle.kts"):
        if any(part in SKIP for part in build_file.parts):
            continue
        rel = build_file.relative_to(ROOT).as_posix()
        module = rel.removesuffix("/build.gradle.kts").replace("/", ":")
        if module.startswith("core:"):
            for dep in project_dependencies(build_file):
                if dep.startswith((":platform:", ":runtime:", ":app", ":plugins:")):
                    violations.append(f"{rel}: core module cannot depend on {dep}")
        if module.startswith("platform:"):
            for dep in project_dependencies(build_file):
                if dep.startswith((":runtime:", ":app")):
                    violations.append(f"{rel}: platform module cannot depend on {dep}")

    if violations:
        print("Architecture boundary check failed:", file=sys.stderr)
        for item in sorted(set(violations)):
            print(f"  - {item}", file=sys.stderr)
        raise SystemExit(1)
    print("Architecture boundaries OK: network, Android authority, JNI, analytics, and module direction are isolated.")


if __name__ == "__main__":
    main()
