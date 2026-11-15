# F5 — Grammar Learning

Updated: 2026-11-15. Status: Sprint 04 implementation locally tested; shared-environment acceptance pending.
Requirements: FR-037–FR-044. Owners/reviewer: TBD.

## Frontend plan

- [x] Grammar sub-topic and pattern lists/details.
- [x] Topic/JLPT filters, multilingual keyword search and bounded page loading.
- [x] Structures, readings, explanations, examples, conjugation rules and grammar points.
- [x] Parent-to-child navigation, saved filters and loading/empty/error/retry states.
- [ ] Cached reads and completion events through F6/F7 in Sprint 05.

## Backend plan

- [x] Audit existing routes/relations and retain compatibility.
- [x] Publish validated catalog query/detail contracts and stable response versions.
- [x] Verify parent filters, Admin mutation gates and authoring level inheritance/conflicts.
- [x] Exercise real PostgreSQL fixtures without AI/provider calls or shared seeding.
- [ ] Shared-environment acceptance and performance verification.

[Canonical API](../contracts/sprint-04-api.md) · [Sprint 04 evidence](../sprints/sprint-04-report.md).

Local evidence covers grammar AC-004 and FR-037–FR-043. FR-044 offline behavior remains Sprint 05. Dependencies: F3 topics, F6 cache and F7 progress. Apply [delivery gates](../planning/delivery.md).
