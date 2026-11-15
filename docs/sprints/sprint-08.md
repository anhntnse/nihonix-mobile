# Sprint08 — Streak, calendar and notifications

Status: Implemented; review fixes applied; final verification mostly complete, 2 Sprint06 flashcard UI test failures and release/device/rollout acceptance open. Date2026-11-15.
Scope F10/F11, F1 device logout, F2 reminder settings and F9 activity integration.

User approved60 engaged seconds/day for streak. Daily goal separately uses daily_goal_minutes; captured timezone preserves original event dates.

## Frontend

- [x] Foreground/idle study timer, immutable offline activity producer and Room5→6 migration.
- [x] Server totals/streak and full monthly calendar with cache/pending/retry/discard.
- [x] Firebase Messaging permission/token/session binding and safe notification destination.
- [x] Cold-start binding, reminder opt-out and session-safe logout regression fixes.

## Backend

- [x] Idempotent activity batches, captured timezone dates, late-event summaries/calendar.
- [x] Practice completion count/date integration without duplicate duration.
- [x] Session-bound device rotation/removal/cascade.
- [x] Deduplicated, timezone-aware goal/opt-out/session reminder checks and bounded FCM provider.

## Verification and rollout

- [x] Policy and core integration/native target tests witnessed RED→GREEN.
- [x] Fresh read-only review,5 Important findings enter one regression fix pass.
- [x] Backend final regression (2026-11-15): build, Jest 16 suites/95 tests, Sprint05–08 isolated fixtures PASS.
- [x] Android unit tests, lintDebug and assembleDebug PASS; same-APK force-stop restart (seed→force-stop→verify) PASS for Activity, Flashcard, Offline and Practice.
- [x] Android instrumented suite: 72 tests, 66 pass, 2 fail, 4 are restart tests run manually (PASS). The 2 failures are Sprint06 `FlashcardScreensTest`; the same 2 fail 3/3 on Sprint07 baseline ed4aed2, so they are pre-existing, not a Sprint08 regression. Fixed in Sprint09 (test used `performScrollTo` on lazy items); full suite then 72 tests, 0 failures.
- [ ] Release build: `assembleRelease` blocked by guard until `NIHONIX_RELEASE_API_BASE_URL` is a deployed HTTPS URL; not run.
- [ ] Actual recipient and physical/multi-device/Doze/shared HTTPS acceptance.
- [ ] Shared migration/deployment and explicit live scheduler enablement.

No automatic commit/push/deployment. [Report](sprint-08-report.md) · [Contract](../contracts/sprint-08-api.md) · [Ledger](sprint-08-implementation.md) · [Tasks](sprint-08-tasks.md).
