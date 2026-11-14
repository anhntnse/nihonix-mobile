# Sprint 09 — Integration, acceptance and release readiness

Status: Proposed, not started.
Duration assumption: Two weeks.
Start/end dates: TBD. Capacity/estimate: TBD.
Sprint owner: TBD. Frontend owner: TBD. Backend owner: TBD. QA: TBD.

## Goal and scope

F1–F13, all AC-001–AC-017 and non-functional requirements

Prerequisites: Sprint 02–08 increments integrated; remaining gaps explicitly tracked.

This sprint is a planning increment, not a delivery commitment. Split scope or adjust duration after team estimation. Existing source is a starting point, not proof of completed acceptance.

## Frontend backlog

- [ ] Run complete registration → study → practice → progress → reminder journeys.
- [ ] Exercise offline/process-death/account-switch scenarios and screen accessibility/layout checks.
- [ ] Verify release endpoint/logging/storage configuration; fix regression defects.

## Backend backlog

- [ ] Run ownership/Admin authorization, token replay, quota concurrency and idempotency checks.
- [ ] Verify migrations, retention/deletion, backup/recovery procedure and operational monitoring.
- [ ] Exercise reminder scheduling, provider failure and API compatibility under expected load.

## Integration and acceptance

- [ ] Every SRS acceptance criterion has a test result or a named release-blocking issue.
- [ ] No critical auth/ownership/data-loss defects remain; non-functional targets have agreed measurements.
- [ ] Release candidate, rollback instructions, known issues and updated SRS are reviewed; no automatic production deployment is implied.

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
