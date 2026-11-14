# Sprint 08 — Streak, calendar and notifications

Status: Proposed, not started.
Duration assumption: Two weeks.
Start/end dates: TBD. Capacity/estimate: TBD.
Sprint owner: TBD. Frontend owner: TBD. Backend owner: TBD. QA: TBD.

## Goal and scope

F10 and F11; complete F2 reminders, F1 device cleanup and F9 activity integration

Prerequisites: Sprint 02 settings, Sprint 05 durable sync and Sprint 07 practice completion events.

This sprint is a planning increment, not a delivery commitment. Split scope or adjust duration after team estimation. Existing source is a starting point, not proof of completed acceptance.

## Frontend backlog

- [ ] Wire study/practice activity duration recording and offline queue.
- [ ] Build daily totals/streak/monthly calendar and reminder settings integration.
- [ ] Integrate FCM permission/token lifecycle and notification destinations.

## Backend backlog

- [ ] Deliver idempotent activity ingestion and timezone-aware summaries.
- [ ] Implement device registration/removal/rotation and invalid-token cleanup.
- [ ] Schedule deduplicated reminders respecting timezone, opt-out and daily-goal completion.

## Integration and acceptance

- [ ] AC-012–AC-015 pass, including timezone/day-boundary and late offline events.
- [ ] Permission denial does not block study; logout/account switch prevents wrong-user delivery.
- [ ] Notification launch respects authentication and valid destinations.

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
