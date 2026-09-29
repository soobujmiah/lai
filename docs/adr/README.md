# Architecture Decision Records

Decision records for this repository, using the format in
`standards/repository-research-and-adr.md` (`soobujmiah/skb`).

The `android-app` type requires ADRs for decisions that are costly to reverse:
architecture boundaries, data storage, external dependencies, and security or
privacy posture.

## Naming

`NNNN-short-title.md`, numbered sequentially, never reused or renumbered.

## Required fields

- **Status** — `proposed`, `accepted`, `superseded`, or `rejected`
- **Context** — the forces at work, including constraints
- **Decision** — what was decided
- **Consequences** — what becomes easier and harder, honestly

An ADR is not a changelog entry and is not rewritten after the fact to make a
past decision look better. Supersede it with a new record instead.
