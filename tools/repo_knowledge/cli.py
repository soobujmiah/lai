#!/usr/bin/env python3
"""repo-knowledge -- deterministic, non-LLM repository state sync CLI.

See governance/DETERMINISTIC_STATE_SYNC_POLICY.md and governance/REPO_STATE_PROTOCOL.md in the
SKB repository. Every subcommand here is pure Git/filesystem derivation; none of them call an
LLM, and none of them write status/priority/maturity/next_gate -- those remain human-authored.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

from . import core, validate

REPO_DIR_NAME = ".repo"


def _repo_dir(root: Path) -> Path:
    return root / REPO_DIR_NAME


def _project_yaml(root: Path) -> Path:
    return _repo_dir(root) / "project.yaml"


def _infer_repository(root: Path) -> str | None:
    url = core.run_git(["remote", "get-url", "origin"], root)
    if not url:
        return None
    match = re.search(r"[:/]([^/]+/[^/]+?)(?:\.git)?$", url)
    return match.group(1) if match else None


def _resolve_identity(root: Path, args: argparse.Namespace) -> tuple[str, str]:
    repository = args.repository or _infer_repository(root)
    if not repository:
        raise SystemExit("could not infer repository (no origin remote); pass --repository owner/repo")
    project_id = args.project_id or repository.split("/", 1)[1]
    return project_id, repository


def cmd_init(args: argparse.Namespace) -> int:
    root = Path(args.root).resolve()
    project_id, repository = _resolve_identity(root, args)
    (_repo_dir(root) / "events").mkdir(parents=True, exist_ok=True)
    project_yaml = _project_yaml(root)
    if project_yaml.exists():
        print(f"already initialized: {project_yaml}")
        return 0
    state = core.build_project_state(
        root, project_id, repository, generated_by="tools/repo_knowledge init",
        sync_source="local", existing=None,
    )
    state["sync"]["status"] = "pending"
    core.dump_yaml(project_yaml, state)
    print(f"initialized {project_yaml}")
    return 0


def cmd_collect(args: argparse.Namespace) -> int:
    root = Path(args.root).resolve()
    project_id, repository = _resolve_identity(root, args)
    existing = core.load_yaml(_project_yaml(root))
    previous_commit = existing.get("head", {}).get("commit") if existing else None

    state = core.build_project_state(
        root, project_id, repository,
        generated_by="tools/repo_knowledge collect",
        sync_source="ci" if args.ci else "local",
        existing=existing,
        build_status=args.build_status, build_run_id=args.build_run_id,
        test_status=args.test_status, test_run_id=args.test_run_id,
        test_summary=args.test_summary,
    )
    core.dump_yaml(_project_yaml(root), state)

    head_commit = state["head"]["commit"]
    events = []
    if head_commit and head_commit != previous_commit:
        events.append(core.make_event(
            repository, head_commit, "commit", status="completed",
            summary=core.get_commit_summary(root, head_commit),
        ))
    if args.build_status:
        events.append(core.make_event(
            repository, head_commit, "build", status=args.build_status, run_id=args.build_run_id,
        ))
    if args.test_status:
        events.append(core.make_event(
            repository, head_commit, "test", status=args.test_status, run_id=args.test_run_id,
            summary=args.test_summary,
        ))
    for event in events:
        path = core.write_event(root, event)
        print(f"event: {path}")

    print(f"collected {_project_yaml(root)}")
    return 0


def cmd_sync(args: argparse.Namespace) -> int:
    # sync performs the same collection as `collect`, then always records a sync.completed
    # event -- it must succeed even when the build/test it is reporting on failed (policy
    # rule 4/7): a failed build is not a sync failure.
    rc = cmd_collect(args)
    if rc != 0:
        return rc
    root = Path(args.root).resolve()
    project_id, repository = _resolve_identity(root, args)
    state = core.load_yaml(_project_yaml(root))
    head_commit = state["head"]["commit"]
    event = core.make_event(repository, head_commit, "sync", status="completed")
    core.write_event(root, event)

    status_md = core.render_status_markdown(state)
    (_repo_dir(root) / "STATUS.md").write_text(status_md, encoding="utf-8")
    print(f"synced ({state['sync']['source']}); wrote {_repo_dir(root) / 'STATUS.md'}")
    return 0


def cmd_verify(args: argparse.Namespace) -> int:
    root = Path(args.root).resolve()
    schemas_dir = root / "schemas"
    findings: list[str] = []

    project_yaml = _project_yaml(root)
    state = core.load_yaml(project_yaml)
    if state is None:
        print(f"VERIFY: {project_yaml} does not exist (run `repo-knowledge init` first)")
        return 1
    registry_schema = validate.load_schema(schemas_dir / "project-registry.schema.json")
    findings += [f"project.yaml {e}" for e in validate.validate(state, registry_schema)]

    event_schema = validate.load_schema(schemas_dir / "project-event.schema.json")
    events_dir = _repo_dir(root) / "events"
    for event_path in sorted(events_dir.glob("*.yaml")) if events_dir.exists() else []:
        event = core.load_yaml(event_path)
        findings += [f"{event_path.name} {e}" for e in validate.validate(event, event_schema)]

    if findings:
        print(f"VERIFY: {len(findings)} finding(s)")
        for f in findings:
            print(f"- {f}")
        return 1
    print("VERIFY: 0 findings")
    return 0


def cmd_status(args: argparse.Namespace) -> int:
    root = Path(args.root).resolve()
    state = core.load_yaml(_project_yaml(root))
    if state is None:
        print("no .repo/project.yaml -- run `repo-knowledge init` first")
        return 1
    print(core.render_status_markdown(state))
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="repo-knowledge", description=__doc__)
    parser.add_argument("--root", default=".", help="repository root (default: cwd)")
    parser.add_argument("--project-id", default=None)
    parser.add_argument("--repository", default=None, help="owner/repo, inferred from origin if omitted")
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("init", help="create the .repo/ skeleton").set_defaults(func=cmd_init)

    for name, func in (("collect", cmd_collect), ("sync", cmd_sync)):
        p = sub.add_parser(name)
        p.add_argument("--ci", action="store_true", help="mark this run's sync.source as ci")
        p.add_argument("--build-status", choices=core.STATUS_VALUES, default=None)
        p.add_argument("--build-run-id", default=None)
        p.add_argument("--test-status", choices=core.STATUS_VALUES, default=None)
        p.add_argument("--test-run-id", default=None)
        p.add_argument("--test-summary", default=None)
        p.set_defaults(func=func)

    sub.add_parser("verify", help="validate .repo/ against the schemas; report-only").set_defaults(func=cmd_verify)
    sub.add_parser("status", help="print a human summary of .repo/project.yaml").set_defaults(func=cmd_status)
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
