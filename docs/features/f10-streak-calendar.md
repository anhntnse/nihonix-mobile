# F10 — Learning Streak & Calendar

Status: Implemented and locally verified (Sprint 08; app/server totals cross-checked in the Sprint 09 emulator run); shared-environment and physical-device acceptance pending. Requirements FR-080–087. Updated 2026-11-16.
[Sprint08](../sprints/sprint-08.md) · [Contract](../contracts/sprint-08-api.md) · [Report](../sprints/sprint-08-report.md).

## Delivered frontend

- [x] Engaged foreground duration on loaded vocabulary/grammar detail, flashcard review and incomplete practice; background/navigation/idle pause.
- [x] Stable UUID/timestamp/timezone activity outbox, Room6 private snapshots and independent WorkManager synchronization.
- [x] Server daily minutes/goal, current/longest streak and monthly calendar with pending/error/retry/discard controls.
- [x] Account isolation, old-response guards, migration and process-restart fixture.

## Delivered backend

- [x] Authenticated immutable activity ingestion, canonical UUID replay/conflict and atomic batch validation.
- [x] Captured timezone/date slices with midnight/DST and late-offline recalculation.
- [x] User-approved60 seconds qualifies streak; daily_goal_minutes independently controls reminder suppression.
- [x] Practice completion counts without double timed credit; new completions capture timezone/day, legacy completion fallback UTC.
- [x] Legacy mutable count rows preserved in storage without fabricated timed history; they are not displayed as new timed activity.

Generation does not record time. The timer does not mark content learned. Process death can lose the last unchecked15-second interval/subsecond remainder. A failed initial preferences load currently needs lifecycle/navigation restart before tracking begins; this minor follow-up is recorded in the report.

## Acceptance

Local AC-012/013 evidence includes duplicate/lost replies, database reopen, timestamp/owner isolation, midnight/DST, goals, calendar and late data. Final consolidated regression is in the [report](../sprints/sprint-08-report.md): backend Jest and isolated fixtures PASS, instrumented suite 72 tests with 0 failures, same-APK restart PASS. The Sprint 09 emulator run (UTC) recorded about 66 s on a detail screen as 73 server seconds and showed streak 1, matching the server. See [acceptance matrix](../sprints/sprint-09-acceptance.md). Shared migration/HTTPS, physical/multi-device/Doze/network-transition acceptance and retention/scale policy remain rollout work.
