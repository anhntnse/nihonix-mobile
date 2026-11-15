# Sprint08 execution report

Date2026-11-15. Implementation and review fixes complete; final verification recorded below (2 open flashcard UI failures); no commit/push/deployment requested.

## Delivered implementation

User confirmed60 engaged seconds qualifies a streak day; daily goal separately uses daily_goal_minutes. Native records foreground vocabulary/grammar detail, flashcard review and incomplete practice time with60-second idle bound and15-second checkpoints. Immutable activity outbox and Room6 cache preserve queued UUID/timestamp/timezone. Server user-locked event ingestion, canonical hashes and captured date slices support duplicate retries and late/offline events without rewriting history after timezone changes.

Summary/current/longest streak and full monthly calendar use backend data. Practice completion events supply counts without duplicating native timed seconds. Legacy count-only rows are preserved, with no invented duration. Counts, minutes, qualification and daily goal have explicit distinct meanings.

FCM configuration uses the user's existing project the-discipline-476506-q7. CLI confirmed com.nihonix.mobile and retrieved app/google-services.json. Google-services/debug packaging passed. Backend ADC obtained a valid access token; Service Usage metadata was403, but FCM validate-only request reached expected invalid-token validation without delivering a notification. Private key remains outside both repositories; local ignored environment points to it.

Device registration binds installation/token to validated JWT session and rotates binding on account/session/token changes. Reminders claim once per owner/local date before network, skip opt-out/goal/session expiry, and clean unregistered tokens. Scheduler is opt-in and remains disabled for real recipients. Native data-only messages and taps check owner/session/binding and allowlisted destination. Permission is requested from profile controls; denial does not block study. Binding preferences and databases are excluded from backup.

## Evidence ledger

- Backend activity policy7 tests witnessed assertion RED, then GREEN; initial fixture401-vs404 RED, thenGREEN.
- Device registration404 RED→GREEN; reminder policy2 assertion failures RED→GREEN.
- Backend build/Jest95 and isolated activity/device/reminder fixture passed before final expanded regressions.
- Native clock/payload3 tests RED→GREEN; repository2 tests RED with missing queue, thenGREEN.
- Calendar UI placeholder test RED→GREEN. Full native unit and ordinary instrumented suite shell210 BUILD SUCCESSFUL.
- Room5→6 migration tests preserve prior practice/outbox and composite account cache keys.
- Review-fix regressions: Android binding/opt-out/logout-rotation/same-owner-session tests GREEN; backend practice capture date fixed.

### Final consolidated verification (2026-11-15, emulator-5554, Medium_Phone_API_37.0, cold boot, animations off)

- Backend: `nest build` OK; Jest 16 suites/95 tests PASS; `test:sprint05`–`test:sprint08` isolated-schema fixtures PASS (fake provider, no real delivery).
- Android: `testDebugUnitTest`, `lintDebug`, `assembleDebug` PASS. Only new compiler warning: deprecated `Task.token` at `NotificationsManager.kt:71`.
- Same-APK restart (`am instrument` seed → `force-stop` → verify, without reinstall): `ActivityProcessRestartTest`, `FlashcardProcessRestartTest`, `OfflineProcessRestartTest`, `PracticeProcessRestartTest` all PASS. Activity UUID, occurrence time and captured timezone survive; outbox sentinel preserved; sync yields one receipt.
- Instrumented suite (`connectedDebugAndroidTest`): 72 tests, 2 failures, 4 skipped (restart tests need `restartPhase`, covered above). All Sprint08 tests PASS.
- Open failures, both Sprint06 `FlashcardScreensTest`: `createFolderAndBulkSetValidateThenPersistJapaneseCards` and `largeTextDarkEditorKeepsSaveActionVisibleWithJapaneseInput` (second card row 'Front 2'/'Back 2' not found). `largeTextDark…` failed every run; others varied between runs (`replayedAiPreviewDoesNotCreateAnotherSet` failed once). Flashcard code is unchanged by Sprint08. Baseline comparison (detached worktree at Sprint07 commit ed4aed2, debug+androidTest APKs, same emulator, 3 runs): the same 2 tests (`createFolderAndBulkSet…`, `largeTextDark…`) failed in 3/3 runs, so they are pre-existing and not caused by Sprint08. Likely an API 37 emulator/layout issue in the Sprint06 flashcard editor; root cause not investigated. Tracked as a Sprint09 defect. Baseline worktree removed afterwards.
- Not run: `assembleRelease` (guard `validateReleaseEnvironment` requires `NIHONIX_RELEASE_API_BASE_URL` deployed HTTPS URL; no placeholder used by decision).
- Earlier emulators (5554/5556) were hung (`adb shell` never returned) and were restarted; the first instrumented run failed with 'No compatible devices' for that reason.

## Rollout boundaries

No real notification, SMTP or AI generation sent; fixtures use isolated schemas/fake provider. No shared manual migration/deployment or scheduler activation. Existing local synchronize=true development watcher may synchronize new entities.

Migration1794959200000 adds activity/device/reminder tables and preserves legacy data. Its down loses event/idempotency/dedup metadata, so it is not a safe production rollback. Lifetime event/cache/receipt growth, physical devices/Doze/network transitions, cross-device acceptance and actual HTTPS deployment remain rollout work.

Reminder attempts deliberately do not retry ambiguous external acceptance; crash/timeout can lose a reminder. Offline logout cannot revoke a server session without connectivity; local filtering suppresses stale-account display and session expiry/rebind later removes its validity. Process death can lose the last uncheckpointed study interval/subsecond remainder. These limits are explicit rather than implying exact duration or exactly-once FCM delivery.

Dependency installation reported69 aggregate npm audit findings; no broad or breaking automatic fixes were applied. .idea remains user-local.

[Contract](../contracts/sprint-08-api.md) · [Design/ledger](sprint-08-implementation.md) · [Tasks](sprint-08-tasks.md) · [Firebase setup](../troubleshooting/firebase-fcm.md).
