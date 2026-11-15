# F4 — Vocabulary Learning

Updated: 2026-11-15. Status: Sprint 04 implementation locally tested; shared-environment acceptance pending.
Requirements: FR-028–FR-036. Owners/reviewer: TBD.

## Frontend plan

- [x] Set and word lists/details with topic/JLPT filters and debounced keyword search.
- [x] Bounded page loading, empty/error/retry states and stable list keys.
- [x] Japanese readings, localized meanings/examples and set-to-child navigation.
- [x] Saved filter/search/language state and stale-response protection.
- [ ] Persistent cached reads and learning completion events with F6/F7 in Sprint 05.

## Backend plan

- [x] Audit/reuse existing entities and retain legacy endpoints.
- [x] Provide bounded catalog page/detail contracts with deterministic ordering and version metadata.
- [x] Verify parent filters/references and Admin authoring; reject conflicting parent/word levels.
- [x] Test representative Japanese data in isolated PostgreSQL without changing shared seed data.
- [ ] Shared-environment acceptance and performance verification.

[Canonical API](../contracts/sprint-04-api.md) · [Sprint 04 evidence](../sprints/sprint-04-report.md).

Local evidence covers vocabulary AC-004 and FR-028–FR-035. FR-036 and AC-005/AC-006 offline behavior remain Sprint 05. Dependencies: F3 topics, F6 cache and F7 progress. Apply [delivery gates](../planning/delivery.md).
