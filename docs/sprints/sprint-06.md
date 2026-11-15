# Sprint 06 — Personal flashcards and synchronization

Status: Implemented and locally verified. Shared deployment/physical-device acceptance pending.
Implementation date:2026-11-15. Team estimates and calendar capacity were not supplied.
[Contract](../contracts/sprint-06-api.md) · [Report](sprint-06-report.md) · [Execution plan](sprint-06-implementation.md).

## Goal

F8 manual CRUD/bulk/review/sync (FR-059–068/070) on the Sprint05 outbox. AI FR-069 follows Sprint07. User selected preserving local edits with explicit server-copy/save-copy conflict resolution.

## Android

- [x] S6-FE01: Private folder/set/card CRUD, bulk editor/selected delete and confirmation.
- [x] S6-FE02: Room migrations2→3→4, drafts/canonical copies, bounded large-JSON storage and immutable dependent outbox.
- [x] S6-FE03: Independent progress/flashcard WorkManager producers, account-pinned edits and conflict recovery.
- [x] S6-FE04: Review flip/previous/next/restart, stable position/side and draft recreation.
- [x] S6-FE05: Generated-card preview/explicit-save boundary; no AI provider UI enabled.

## Backend

- [x] S6-BE01: Nullable owner migration preserving/hiding legacy rows; owner-safe legacy CRUD adapters.
- [x] S6-BE02: Atomic complete-set validation/replacement and cascading deletion.
- [x] S6-BE03: Durable receipt hashes, CAS/dependency versions, parent-version updates, tombstones and scoped deltas.
- [x] S6-QA01: PostgreSQL/JWT malicious parent/card tests, concurrent fresh UUID collisions, stale deletes, replay and migration preservation.

## Acceptance and review

- [x] Local CRUD/bulk/review/conflict tests, including large Japanese drafts/aggregates.
- [x] Fresh read-only review; important ownership/retry-order findings reproduced RED→GREEN.
- [x] Backend build/Jest and PostgreSQL Sprint04/05/06 regressions;45 ordinary Android instrumented tests; both same-APK process-restart fixtures. Final native unit/lint/packaging result recorded in report.
- [ ] Shared migration/HTTPS deployment, real multi-device and physical-device Doze/network-transition acceptance.
- [ ] FR-069 AI/quota integration remains Sprint07; AC-010 is not claimed.

AC-009 has local automated evidence; whole-product/shared-environment acceptance remains open. The subsequent user request authorizes grouped commits and push on both existing repository branches. Deployment remains separate.
