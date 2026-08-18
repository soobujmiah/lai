# Knowledge Graph / Mind Map

LAI shall provide a **modular, interactive knowledge graph** — inspired by Obsidian usability, without complicating the core.

## Current

**Missing** — spec only in `MASTER_ROADMAP` Phase 19.

## Target (future, modular, after RAG)

*   **Nodes (future):** `documents`, `concepts`, `code`/`functions`, `projects`, `tasks`, `memories`, `entities` — each with `id`, `label`, `type`, `source` (`sha256` where appropriate), `timestamp`.
*   **Relationships:** `references`, `related-to`, `depends-on`, `derived-from`, `belongs-to`, `modifies`, `created-from` — each with `source`, `target`, `type`, `weight`, `provenance`.
*   **Source:** Derived from `core:rag` citations + `core:agent` `Task` graph + `core:pipeline` DAG + `MEMORY` + `FILES_AND_DOCUMENTS` metadata — never manual-only.
*   **UI (future):** `Knowledge` tab (like `Chat/Agent/Tools`), `RecyclerView`/`Compose` `Canvas` with `zoom/pan/search/filters/node expansion/node details/relationship inspection`. Uses `RAG` vector search + `MEMORY` retrieval. **Must not complicate core** — separate `features:knowledge` module, composed by `app`, isolated behind `plugins/api`-like contract.
*   **Storage:** App-private `SQLCipher` graph store (nodes/edges) — versioned, bounded, explicit `export/delete`, survives `install -r` via `storage/LAI/knowledge` (SAF optional).

## Privacy and commercial boundary

*   **Local-first:** graph bytes, node text, and edges stay on device. Optional later cloud intelligence requires explicit consent and must not auto-upload the graph ([`architecture/HYBRID_AND_PROVIDER_ARCHITECTURE.md`](architecture/HYBRID_AND_PROVIDER_ARCHITECTURE.md)).
*   **Module:** `features:knowledge` after RAG. Must not be implemented inside `core` or `runtime:llama`.
*   **Premium possibility:** advanced visualization, large-graph interaction, and hosted graph features are premium *candidates* only ([`legal/PROPRIETARY_BOUNDARIES.md`](legal/PROPRIETARY_BOUNDARIES.md)). Not decided (IP-006).
*   **Not implemented.** Documentation of this module is not implementation authorization.

## Non-goals

No auto-creation of `5B` model-sized graphs, no implicit cloud sync, no lock-in, no copy of another product’s protected visual language.

## Testing

`gen_knowledge_*` fixtures, `zoom/pan` on 1k nodes, `install -r` persistence, `RAG` citation → graph edge.

## Status

`FUTURE` — spec only, no code, no `MEASURED`.
