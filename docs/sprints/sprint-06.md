# Sprint 06 — Personal flashcards and synchronization

Status: Proposed, not started.
Duration assumption: Two weeks.
Start/end dates: TBD. Capacity/estimate: TBD.
Sprint owner: TBD. Frontend owner: TBD. Backend owner: TBD. QA: TBD.

## Goal and scope

F8 manual CRUD/bulk/review/sync; AI generation follows Sprint 07

Prerequisites: Sprint 05 durable outbox, stable IDs and account isolation.

This sprint is a planning increment, not a delivery commitment. Split scope or adjust duration after team estimation. Existing source is a starting point, not proof of completed acceptance.

## Frontend backlog

- [ ] Deliver folder/set/card CRUD, bulk edit/delete and review screens.
- [ ] Persist/synchronize offline changes and expose conflict/retry states.
- [ ] Prepare generated-card preview/save integration boundary.

## Backend backlog

- [ ] Deliver owner-scoped folder/set/card and bulk contracts.
- [ ] Implement transactional validation, delta/deleted-ID sync and conflict policy.
- [ ] Test malicious cross-user parent-child references and duplicate mutations.

## Integration and acceptance

- [ ] AC-009 passes; offline edits/deletions reconcile across restart and devices.
- [ ] Cross-user access and invalid bulk requests are rejected without unintended partial writes.
- [ ] FR-069 remains open until AI integration in Sprint 07.

Apply the [shared delivery gates](../planning/delivery.md). Use the [feature index](../features/README.md) to find exact FR ranges and feature-specific checks.

## Execution and review

| Item | Value |
| --- | --- |
| Committed task IDs / estimates | TBD during sprint planning |
| API/schema decisions | Link accepted entries in [API decisions](../architecture/api-decisions.md) |
| Dependencies / blockers | None assessed yet; record owner and next action when identified |
| Demo / test evidence | Pending |
| Completed acceptance | None recorded |
| Carryover / defects | Pending review |
| Retrospective / next actions | Pending review |

Update this record during delivery; only mark completed after integrated acceptance evidence is linked.
