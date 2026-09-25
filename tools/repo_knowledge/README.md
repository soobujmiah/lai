# tools/repo_knowledge

Deterministic, non-LLM repository state sync CLI. Implements
`governance/DETERMINISTIC_STATE_SYNC_POLICY.md` and `governance/REPO_STATE_PROTOCOL.md`.

## Usage

```bash
python3 -m tools.repo_knowledge init                       # create .repo/ skeleton
python3 -m tools.repo_knowledge collect \
  --build-status passed --build-run-id "$GITHUB_RUN_ID" \
  --test-status passed --test-summary "237/237 passed"
python3 -m tools.repo_knowledge sync --ci                  # writes .repo/project.yaml + STATUS.md
python3 -m tools.repo_knowledge verify                      # report-only schema check, exit 1 on findings
python3 -m tools.repo_knowledge status                       # human-readable summary
```

Run from the repository root (the module infers `--repository owner/repo` from `git remote get-url origin`
and `--project-id` from the repo name; override either flag if that inference is wrong).

## What it does not do

- It never writes `status`, `strategic_priority`, `maturity`, or `next_gate` — those stay
  human-authored in `projects/state/*.md` per `governance/PROJECT_STATE_SCHEMA.md`.
- It never calls an LLM.
- It never deletes an event or overwrites `last_successful_build`/`last_failed_build` with a
  run that didn't actually report that outcome (see the merge rules in
  `governance/DETERMINISTIC_STATE_SYNC_POLICY.md`).

## Tests

```bash
python3 -m unittest discover -s tools/repo_knowledge/tests -p 'test_*.py' -v
```
