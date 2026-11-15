# Sprint 09 — Integration, acceptance and release readiness

Status: In progress (started 2026-11-16). Done locally: acceptance matrix, backend route-guard audit, cross-feature ownership fixture, emulator journey against an isolated local backend, release configuration review, flashcard test fix. Open: release-build run, shared-environment and physical-device acceptance, extending the journey to practice/flashcards/progress.
Duration assumption: Two weeks.
Start/end dates: TBD. Capacity/estimate: TBD.
Sprint owner: TBD. Frontend owner: TBD. Backend owner: TBD. QA: TBD.

## Goal and scope

F1–F13, all AC-001–AC-017 and non-functional requirements

Prerequisites: Sprint 02–08 increments integrated; remaining gaps explicitly tracked.

This sprint is a planning increment, not a delivery commitment. Split scope or adjust duration after team estimation. Existing source is a starting point, not proof of completed acceptance.

## Frontend backlog

- [~] Run complete registration → study → practice → progress → reminder journeys. (Done on emulator against a local isolated backend: sign up, verify, sign in, browse, study 66 s, streak/calendar, reminder settings, FCM token registration, sign out, session revocation → sign in. Practice, flashcards and progress screens not driven manually; reminder delivery not exercised. See [acceptance](sprint-09-acceptance.md).)
- [ ] Exercise offline/process-death/account-switch scenarios and screen accessibility/layout checks.
- [~] Verify release endpoint/logging/storage configuration; fix regression defects. (Static review done: findings R-1 low, R-2 medium unverified, R-3 info. Release build not run, needs deployed HTTPS URL. Flashcard UI test defects D-09-01/02 fixed in the test.)

## Backend backlog

- [~] Run ownership/Admin authorization, token replay, quota concurrency and idempotency checks. (`npm run test:sprint09` PASS: cross-feature ownership, users self/admin, catalog admin-only, token replay and revocation, deletion cascade. Quota concurrency and idempotency remain covered by the Sprint 05–08 fixtures only.)
- [ ] Verify migrations, retention/deletion, backup/recovery procedure and operational monitoring.
- [ ] Exercise reminder scheduling, provider failure and API compatibility under expected load.

## Integration and acceptance

- [~] Every SRS acceptance criterion has a test result or a named release-blocking issue. (Draft matrix in [sprint-09-acceptance.md](sprint-09-acceptance.md): 0 fully accepted; 15 local pass, 2 partial (AC-003, AC-015); shared-environment acceptance pending for all.)
- [ ] No critical auth/ownership/data-loss defects remain; non-functional targets have agreed measurements.
- [ ] Release candidate, rollback instructions, known issues and updated SRS are reviewed; no automatic production deployment is implied.

Apply the [shared delivery gates](../planning/delivery.md). Use the [feature index](../features/README.md) to find exact FR ranges and feature-specific checks.

## Execution and review

| Item | Value |
| --- | --- |
| Committed task IDs / estimates | TBD during sprint planning |
| API/schema decisions | Link accepted entries in [API decisions](../architecture/api-decisions.md) |
| Dependencies / blockers | Release-build run needs a deployed HTTPS `NIHONIX_RELEASE_API_BASE_URL`; shared migration/deployment; real FCM recipient and physical-device access need explicit authorization. Owners TBD. |
| Demo / test evidence | [Acceptance matrix](sprint-09-acceptance.md); backend route-guard audit; backend and Android regressions re-run 2026-11-15/16 |
| Completed acceptance | None recorded |
| Carryover / defects | See defect table in [acceptance](sprint-09-acceptance.md). Fixed: D-09-01/02 (Sprint06 `FlashcardScreensTest`, brittle `performScrollTo` on lazy items; full instrumented suite 72 tests, 0 failures, 4 restart tests run manually). Open: D-09-03 release build, D-09-04 shared migrations, D-09-05 real FCM/device, D-09-06 backup rule for `app_config_*`, D-09-07 global guard and `ai/health`. Shared migration/HTTPS/physical-device items from Sprints 02–08 remain open. |
| Retrospective / next actions | Pending review |

Update this record during delivery; only mark completed after integrated acceptance evidence is linked.
