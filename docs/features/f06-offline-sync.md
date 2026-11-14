# F6 — Offline Content & Synchronization

Status: Planned delivery; source baseline reviewed 2026-11-14.
Requirements: FR-045–FR-051.
Primary sprint: [Sprint 05](../sprints/sprint-05.md).
Frontend owner: TBD. Backend owner: TBD. QA/reviewer: TBD.

## Baseline and outcome

Planned; etag field/display is not cache validation.

Deliver the behavior specified in [SRS](../SRS.md), Section 4.6. “Frontend” means the Android client in this repository. Backend tasks are proposed work until the backend repository and existing implementation have been audited.

## Dependencies

- [F3 — Learning Roadmap: Levels & Topics](f03-learning-roadmap.md)
- [F4 — Vocabulary Learning](f04-vocabulary.md)
- [F5 — Grammar Learning](f05-grammar.md)
- Agreed API/error/ownership contract in [API decisions](../architecture/api-decisions.md).
- Existing source presence does not mean release or backend acceptance has passed.

## Frontend work

- [ ] In Sprint 01 design account-scoped content/cache and pending-operation schemas, migrations and repository boundaries.
- [ ] In Sprint 05 implement persistent catalog reads and explicit loading/offline/stale states.
- [ ] Implement ETag conditional requests and preserve cached data on 304.
- [ ] Add durable queue/retry and connectivity/background sync; integrate user-data producers in F7/F8/F10.
- [ ] Clear/isolate per-user data on logout/account changes and expose sync errors.

## Backend work

- [ ] Define manifest/entity sync contracts, version scope, deletion markers and pagination.
- [ ] Return consistent ETags and correct If-None-Match/304 behavior.
- [ ] Define idempotency, server/client timestamps and conflict responses for user mutations.
- [ ] Validate ownership, retention/tombstone lifetime and safe retry after partial failures.

## API and data contract

Proposed /entity-sync and /sync groups; exact routes and DTOs are unverified. Coordinate progress batch/delta and flashcard/activity synchronization.

Data/persistence: Server-owned catalog cache, sync cursors/ETags, account-scoped outbox, idempotency IDs, tombstones and retry state. Select Room/WorkManager or equivalent via architecture decision before adding dependencies.

Before implementation, record exact methods, fields, nullability, errors, authentication, ownership, pagination/versioning and representative fixtures in the API decision log. Preserve existing client contracts or document an explicit migration.

## Verification and acceptance

- [ ] Cold-start cached content in airplane mode after process termination.
- [ ] 304 keeps cached payload; changed/deleted server entities update correctly.
- [ ] Interrupt/retry sync without duplicate writes; test account switching, migration and partial failure.

Acceptance mapping: AC-005, AC-006; infrastructure for AC-007/AC-008/AC-012; feature-specific sync acceptance follows producers. These are targets, not passing test results. Apply the shared [delivery gates](../planning/delivery.md).

## Delivery handoff

- [ ] Backend contract audited and agreed; existing endpoints reused where suitable.
- [ ] Frontend and backend changes reviewed with migration/configuration notes.
- [ ] Integration verified against a shared environment; failures and remaining work recorded.
- [ ] SRS source-review status, API decisions and sprint review updated with evidence.
