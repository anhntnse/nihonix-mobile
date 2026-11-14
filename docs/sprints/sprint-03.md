# Sprint 03 — Roadmap and application configuration

Status: Android/backend implementation delivered and locally verified; shared-environment rollout pending.
Updated: 2026-11-14. Owners, capacity and calendar dates remain unassigned.

Scope: F3 and F13. Persistent topic caching remains Sprint 05.

## Delivered

- [x] Level topic/vocabulary/grammar counts, preserving existing localized search and level changes.
- [x] Complete topic bundles and content browsing with loading/error/retry/empty states.
- [x] Saved filter/search state and restored bundle navigation.
- [x] Configuration retrieval, maintenance/minimum-version gates and cached-config failure policy.
- [x] Diagnostic health check and server request IDs.
- [x] Backend counts, deterministic ordering/versions and Admin-only catalog mutation gates.
- [x] Representative data and real PostgreSQL/HTTP integration in a temporary isolated schema.
- [ ] Restart/deploy matching backend and demonstrate against the shared environment.
- [ ] Resolve the stricter catalog-read product policy (D02) and production HTTPS deployment.
- [ ] FR-027 persistent topic cache acceptance in Sprint 05.

Local evidence covers AC-003 and the topic-browsing portion of AC-004, configuration fallback and authorization. Full vocabulary/grammar learning remains Sprint 04. No shared seeding, migration, server restart or deployment was performed.

See [execution report](sprint-03-report.md), [canonical contract](../contracts/sprint-03-api.md) and [delivery gates](../planning/delivery.md).
