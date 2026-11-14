# Sprint 07 — AI quota, practice and generated flashcards

Status: Proposed, not started.
Duration assumption: Two weeks.
Start/end dates: TBD. Capacity/estimate: TBD.
Sprint owner: TBD. Frontend owner: TBD. Backend owner: TBD. QA: TBD.

## Goal and scope

F12 first, then F9 and AI portion of F8

Prerequisites: Sprint 05 progress and Sprint 06 flashcard contracts; provider/quota decisions resolved.

This sprint is a planning increment, not a delivery commitment. Split scope or adjust duration after team estimation. Existing source is a starting point, not proof of completed acceptance.

## Frontend backlog

- [ ] Build quota display/reset/429 states before wiring generation.
- [ ] Deliver both exercise types, practice history/detail, submission and results.
- [ ] Connect generated-flashcard preview/save and emit idempotent completion events.

## Backend backlog

- [ ] Implement atomic quota reservation/consumption, reset and retry/refund policy.
- [ ] Integrate AI provider with validated output and owner-scoped persistence.
- [ ] Deliver grading/submission/idempotency and structured failures; keep provider keys server-side.

## Integration and acceptance

- [ ] AC-010/AC-011 pass including concurrent quota exhaustion, malformed output and timeouts.
- [ ] Generated cards are saved according to the agreed confirmation policy.
- [ ] Progress events are deduplicated; activity/streak acceptance completes in Sprint 08.

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
