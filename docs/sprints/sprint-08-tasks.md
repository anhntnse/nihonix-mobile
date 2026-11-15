# Sprint08 implementation tasks

> Execute inline using executing-plans and test-driven-development; one fresh whole-change review. Existing user instruction is to implement Sprint08; explicit quota/streak decision and Firebase setup authorize continuing without another design ceremony. Spec: sprint-08-implementation.md, sprint-08.md, F10/F11/SRS.

Goal: durable engaged-time activity, timezone-aware streak/calendar and account/session-safe FCM reminders.
Architecture: existing Nest/TypeORM/PostgreSQL and Android Compose/Room/Retrofit/WorkManager. Firebase Messaging Android + firebase-admin backend. No replacement auth or shared deployment.

## Rulings and constraints

- Keep existing checkouts so Android Studio unsaved buffers remain authoritative. No commit/push/deployment without a new request.
- User confirmed60 seconds/day qualifies streak; daily goal uses settings. Capture event timezone; changing timezone does not move historical days. Display today in current settings timezone.
- Timer idle threshold60 seconds; start engagement on screen entry/touch/key, pause background; checkpoints every15 seconds. Each immutable segment1–300 seconds, batch≤50, event no more than365 days old or5 minutes future. Preserve date by per-second timezone splitting at midnight/DST.
- Practice foreground time uses the same timer producer; unique Sprint07 completion events contribute completion counts only, never duplicate timed seconds.
- Reminders claim once per owner/local date; do not retry ambiguous provider acceptance. At-most-one attempt (crash may lose delivery), client notification ID dedups repeats. Enabled scheduler is opt-in via FCM_REMINDERS_ENABLED=true; no automatic live broadcasts in verification.
- Device bindings use installation UUID, FCM token, owner and validated JWT session ID. Token rebind/rotation is transactional; logout deletes session so old binding cannot deliver. Offline logout disables token locally and ignores old-account payloads; server relies on session revocation on next successful cleanup/expiry.
- In-app data-only notifications require owner/binding and allowlisted destination; no server notification block that bypasses local auth filtering. Permissions requested from settings, never blocks study.

## Task1 backend activity contracts

Files: modules/learning-streaks/activity-policy.ts/.spec.ts, activity.entity.ts, learning-streaks.service/controller/module and DTOs; migration1794959200000-ActivityDevices.ts; data-source.ts.
Interfaces: splitActivity(event)->{date,seconds}[]; summarizeDays(rows,today,goal)->summary; ActivityService.recordBatch(owner,events), summary(owner), calendar(owner,year,month).
- [x] Tests first: Vietnam midnight30/30 split, New York DST,60-second threshold versus goal, yesterday/current and gap reset, invalid/future duration.
- [x] Witness RED with placeholder policies; implement and run full Jest. (Jest 16 suites/95 tests PASS, re-run 2026-11-15)
- [x] Isolated PostgreSQL/HTTP tests (`test:sprint08` PASS, re-run 2026-11-15): JWT ownership, same UUID replay/hash conflict, concurrent writes, late events, unchanged historical dates after timezone change, practice completion count without timed double-count, migration preservation.

## Task2 backend device/reminder contracts

Files: modules/notifications/device.entity.ts, notifications.controller/service/module, fcm.provider.ts, reminder.service.ts and policy specs; JWT strategy surfaces validated sid.
Interfaces: PUT /devices/:installationId body token/enabled, DELETE binding; scheduler run(now) claims and sends through provider abstraction; owner summary supplies goal.
- [x] Tests first: opt-out/completed-goal/session-expiry skips, local-time window/DST, claim dedup, invalid-token cleanup and provider ambiguity.
- [x] Register/rebind/rotate tokens under durable ownership/version lock and expected-account header. Unique token, owner/session binding, cascade cleanup.
- [x] Isolated fixture provider verifies cross-account/multi-session/logout behavior without real sends; FCM credential readiness checked separately.

## Task3 native activity/recovery

Files: feature/activity data/domain; Room6 migration; ServiceLocator, SyncWorker; lifecycle/interaction study tracker.
Interfaces: ActivityRepository.record(segment), sync(owner), summaries/month; EngagedTimer foreground/interaction/checkpoint.
- [x] RED tests: monotonic foreground/idle boundaries, checkpoints/recreation, no background overlap, timezone snapshots.
- [x] Room tests: atomic enqueue/reopen, stable payload retries, independent producers, account isolation/purge and5→6 migration.
- [~] Wire vocabulary/grammar detail, flashcard review and practice; no learned-status side effects, no duplicate completion time. (Implemented per report; no dedicated automated test per screen. Backend completion-count-only rule is covered by the Sprint08 fixture.)

## Task4 native calendar/FCM/settings

Files: feature/activity presentation, feature/notifications service/repository/permission UI, MainActivity/AuthNavGraph/profile settings; factory/DI.
- [x] RED Compose tests: totals/streak/month navigation, cached/pending state, permission denial/error, reminder opt-out; typed payload parse/account check tests.
- [x] Firebase service token lifecycle and data-only notification; cold/warm safe destinations and logout/account-switch cleanup.
- [x] FCM project processing/debug build and read-only credential validation; actual recipient delivery only when explicitly authorized. (Debug build PASS; validate-only probe done; no real delivery, still pending authorization.)

## Task5 final verification/handoff

- [~] Android unit/lint/debug + same-APK restart PASS; instrumented 66/72 pass (2 Sprint06 FlashcardScreensTest failures are pre-existing: same 2 fail 3/3 on Sprint07 baseline ed4aed2); release not run (needs deployed HTTPS URL). Backend build/Jest + Sprint05–08 fixtures PASS.
- [x] One fresh read-only reviewer, 5 Important findings reproduced RED→GREEN; deferred minors in ledger. Final full-suite rerun recorded in report.
- [ ] Contracts, SRS, F10/F11, sprint report/backend handoff and Firebase setup notes; preserve original requirement IDs.
- [ ] Shared rollout, live scheduler/device acceptance, HTTPS and config limitations explicit.
