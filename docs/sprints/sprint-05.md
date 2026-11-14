# Sprint 05 — Offline catalog and learning progress

Status: Proposed, not started.
Duration assumption: Two weeks.
Start/end dates: TBD. Capacity/estimate: TBD.
Sprint owner: TBD. Frontend owner: TBD. Backend owner: TBD. QA: TBD.

## Goal and scope

F6 and F7; complete F3/F4/F5 caching

Prerequisites: Sprint 04 stable catalog schemas plus Sprint 01 persistence/sync contract decisions.

This sprint is a planning increment, not a delivery commitment. Split scope or adjust duration after team estimation. Existing source is a starting point, not proof of completed acceptance.

## Frontend backlog

- [ ] Implement persistent content repositories, ETag validation and durable outbox/retry foundation.
- [ ] Implement learning-progress event persistence, batch/delta reconciliation and summaries.
- [ ] Handle restart, no connectivity, account switch and partial-sync error states.

## Backend backlog

- [ ] Deliver manifest/entity version, conditional request and deletion contracts.
- [ ] Deliver idempotent progress batch/delta/summary and timestamp conflict rules.
- [ ] Provide deterministic conflict, 304 and interrupted-sync test fixtures.

## Integration and acceptance

- [ ] AC-005–AC-008 pass with process death, reconnect, duplicate retries and updated/deleted catalog data.
- [ ] Pending progress survives restart and is isolated per account.
- [ ] Outbox supports later flashcard/activity producers; those features are not marked done yet.

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
