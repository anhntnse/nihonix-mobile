# F3 — Learning Roadmap: Levels & Topics

Status: Sprint 03 implementation locally verified; shared-environment rollout pending.
Requirements: FR-021–FR-027. Primary sprint: [03](../sprints/sprint-03.md). Owners/reviewer: TBD.

## Frontend plan and delivery

- [x] Retain N5–N1 browsing, localized title/slug search and user-level changes.
- [x] Display per-level topic/vocabulary/grammar counts.
- [x] Retrieve complete bundles and browse vocabulary/grammar content.
- [x] Handle loading, missing/empty data and retry; save filters/search/navigation state.
- [x] Persist previously fetched content with F6 in Sprint 05 (FR-027); [evidence](../sprints/sprint-05-report.md).
- [x] Complete F4/F5 learning interactions in Sprint 04; explicit progress actions added in Sprint 05.

## Backend plan and delivery

- [x] Audit/reuse levels/topics/user-level contracts.
- [x] Add aggregate counts, complete topic bundles and deterministic ordering/version metadata.
- [x] Enforce Admin-only catalog mutations with guest/user negative tests.
- [x] Exercise representative N5–N1 and empty data in isolated PostgreSQL.
- [ ] Finalize D02 public-versus-authenticated catalog product policy; existing public reads retained.
- [ ] Deploy matching backend and demonstrate shared-environment data.

## Contract and acceptance

[Canonical API](../contracts/sprint-03-api.md) defines counts, bundle arrays, version hashes and access rules. Existing GET/PATCH users/me/level remains. Preserve vi/en/jp content and stable IDs; translated application UI remains out of scope.

AC-003 and topic-browsing AC-004 have local automated evidence in the [Sprint 03 report](../sprints/sprint-03-report.md); shared-environment acceptance is pending. Bundle version is not a persistent-sync cursor. Offline AC-005/AC-006 has local Sprint 05 evidence; shared-environment acceptance remains open.

Dependencies: F1/F2 sessions/profile; F6 cache/sync; [API decisions](../architecture/api-decisions.md). Apply [delivery gates](../planning/delivery.md).
