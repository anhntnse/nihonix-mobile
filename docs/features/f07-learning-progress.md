# F7 — Learning Progress

Status: Planned delivery; source baseline reviewed 2026-11-14.
Requirements: FR-052–FR-058.
Primary sprint: [Sprint 05](../sprints/sprint-05.md).
Frontend owner: TBD. Backend owner: TBD. QA/reviewer: TBD.

## Baseline and outcome

Planned; no progress implementation.

Deliver the behavior specified in [SRS](../SRS.md), Section 4.7. “Frontend” means the Android client in this repository. Backend tasks are proposed work until the backend repository and existing implementation have been audited.

## Dependencies

- [F4 — Vocabulary Learning](f04-vocabulary.md)
- [F5 — Grammar Learning](f05-grammar.md)
- [F6 — Offline Content & Synchronization](f06-offline-sync.md)
- Agreed API/error/ownership contract in [API decisions](../architecture/api-decisions.md).
- Existing source presence does not mean release or backend acceptance has passed.

## Frontend work

- [ ] Define supported completion events and item types with product/backend before recording.
- [ ] Persist progress locally and enqueue batches through F6.
- [ ] Display per-topic/per-level summaries and reconcile server deltas.
- [ ] Use stable mutation IDs and client_updated_at according to agreed conflict policy.

## Backend work

- [ ] Audit/provide progress batch, delta and summary contracts.
- [ ] Enforce item-type/status validity, ownership and idempotency.
- [ ] Define Last-Write-Wins timestamp/tie-break rules and cursor semantics.
- [ ] Recompute summaries consistently and handle deleted/unknown catalog references.

## API and data contract

Proposed /progress batch, delta and summary operations; exact methods and schemas need confirmation.

Data/persistence: User/item/type/status, client_updated_at, server version/cursor and stable mutation ID. Completion thresholds remain an explicit decision.

Before implementation, record exact methods, fields, nullability, errors, authentication, ownership, pagination/versioning and representative fixtures in the API decision log. Preserve existing client contracts or document an explicit migration.

## Verification and acceptance

- [ ] Progress survives restart and offline study; reconnect reconciles state.
- [ ] Duplicate and reordered batches do not create duplicate progress.
- [ ] Two-device conflict, pagination and summary consistency tests pass.

Acceptance mapping: AC-007, AC-008; progress portion of AC-012. These are targets, not passing test results. Apply the shared [delivery gates](../planning/delivery.md).

## Delivery handoff

- [ ] Backend contract audited and agreed; existing endpoints reused where suitable.
- [ ] Frontend and backend changes reviewed with migration/configuration notes.
- [ ] Integration verified against a shared environment; failures and remaining work recorded.
- [ ] SRS source-review status, API decisions and sprint review updated with evidence.
