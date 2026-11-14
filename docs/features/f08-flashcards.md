# F8 — Personal Flashcards

Status: Planned delivery; source baseline reviewed 2026-11-14.
Requirements: FR-059–FR-070.
Primary sprint: [Sprint 06](../sprints/sprint-06.md).
Frontend owner: TBD. Backend owner: TBD. QA/reviewer: TBD.

## Baseline and outcome

Planned; folders, sets, cards, bulk edits and sync absent.

Deliver the behavior specified in [SRS](../SRS.md), Section 4.8. “Frontend” means the Android client in this repository. Backend tasks are proposed work until the backend repository and existing implementation have been audited.

## Dependencies

- [F1 — Authentication & Account Management](f01-authentication.md)
- [F6 — Offline Content & Synchronization](f06-offline-sync.md)
- Agreed API/error/ownership contract in [API decisions](../architecture/api-decisions.md).
- Existing source presence does not mean release or backend acceptance has passed.

## Frontend work

- [ ] Build folder and set/card CRUD, bulk actions and review screens.
- [ ] Persist account-scoped edits/tombstones and synchronize through F6.
- [ ] Handle edit/delete conflicts without losing user changes.
- [ ] Prepare AI-generation preview/confirm-save entry; wire it with F9/F12 in Sprint 07.

## Backend work

- [ ] Audit folder/set/card CRUD and bulk routes; enforce user ownership at every level.
- [ ] Define transactional bulk validation, partial-failure policy and set deletion semantics.
- [ ] Provide delta/deleted-ID sync and a documented multi-device conflict policy.
- [ ] Agree AI card generation and save contract with F9/F12.

## API and data contract

Proposed /flashcard-folders and /flashcards groups; verify set routes, bulk body shape, deletion and sync endpoints.

Data/persistence: user_id, folders, sets, cards, stable local/server IDs, versions, tombstones and pending operations. No SM-2 spaced repetition in this release.

Before implementation, record exact methods, fields, nullability, errors, authentication, ownership, pagination/versioning and representative fixtures in the API decision log. Preserve existing client contracts or document an explicit migration.

## Verification and acceptance

- [ ] Create/edit/delete folders, sets and cards; bulk edits remain consistent.
- [ ] Reject cross-user parent/child references and data access.
- [ ] Offline edits, deletion and multi-device conflicts reconcile; AI saving tested in Sprint 07.

Acceptance mapping: AC-009; flashcard portions of AC-005/AC-010 after AI integration. These are targets, not passing test results. Apply the shared [delivery gates](../planning/delivery.md).

## Delivery handoff

- [ ] Backend contract audited and agreed; existing endpoints reused where suitable.
- [ ] Frontend and backend changes reviewed with migration/configuration notes.
- [ ] Integration verified against a shared environment; failures and remaining work recorded.
- [ ] SRS source-review status, API decisions and sprint review updated with evidence.
