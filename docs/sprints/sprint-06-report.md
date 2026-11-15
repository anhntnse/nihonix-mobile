# Sprint 06 execution report

Date: 2026-11-15. Manual personal flashcards (FR-059–068/070), offline review and synchronization are implemented and locally verified in Android and the sibling NestJS backend. FR-069 remains Sprint07. Shared rollout remains pending. The user subsequently authorized grouped Sprint06 commits and push for both repositories; deployment remains separate.

## Delivered behavior

Topics & Levels → Flashcards provides private folders → sets → cards, folder editing, complete-set bulk editing and selected-card deletion. Destructive actions require confirmation. Review supports flip/previous/next/restart; stable card IDs/side/position survive ViewModel recreation. Editor drafts live in Room, with only IDs/navigation state in saved state.

The user selected preserving local edits on conflict. CAS folder/set versions, dependent immutable UUID mutations and canonical server copies retain unsent content; choose server copy or save a new private copy. Child writes bump folder versions to reject stale folder cascades. Progress and flashcard queues remain independent. Account-pinned actions reject stale editors after account changes.

Backend legacy CRUD delegates to one transactional owner-aware writer. Nullable ownership preserves/hides old unowned rows. Receipts, tombstone heads and ordered deltas support retries/deletions. Conditional upserts prevent concurrent UUID collisions from overwriting another owner's content. PostgreSQL bounds delta snapshot bytes before materializing responses. No new AI call or SMTP change was introduced.

## Verification

| Check | Result |
| --- | --- |
| Android unit suite | Passed:114 tests, including model validation and Retrofit payload/dependency/cursor/owner contracts |
| Android ordinary instrumented suite |45 tests passed on emulator-5556; final Gradle invocation excludes the two phase-only fixtures |
| Direct AndroidJUnitRunner full suite |OK (47 tests):45 ordinary tests plus2 phase-only assumptions |
| Sprint06 same-APK process restart |seed passed → force-stop → verify passed; different PID, identical pending UUID/payloads, drafts/cards/deletion retained and subsequent sync succeeds |
| Sprint05 process-restart regression |seed/force-stop/verify passed with v4 DB; cache/progress/queue retained |
| Android lint/debug/release packaging |BUILD SUCCESSFUL; reserved example HTTPS endpoint used only for packaging |
| Backend build/Jest |Passed;71 tests across11 suites |
| PostgreSQL/HTTP Sprint04/05 regressions |Passed; isolated schemas removed |
| PostgreSQL/HTTP Sprint06 integration |Passed; migration preservation, JWT ownership, bulk rollback, replay/concurrency/dependencies, cascades, delta byte budget, large Japanese payload/413 and account deletion |
| Source/document checks |git diff --check passed in both repositories;105 FR,19 BR,26 NFR and17 AC identifiers preserved |

Native unit validation first witnessed RED→GREEN for5 new model tests; backend policy first witnessed RED→GREEN for11 tests. A fresh read-only review found cross-owner fresh-UUID upserts and retry row reordering. Both were reproduced before fixes: conditional owner/parent SQL upserts and UPDATE-only retry metadata now pass their regressions.

Large200-card Japanese data reproduced SQLite CursorWindow overflow. Room v4 adds bounded32,000-character chunks for outbox/canonical/draft JSON, retaining surrogate pairs at boundaries. Tests cover database reopen, upload, canonical restoration, greater-than1MiB draft restoration, supplementary Unicode and account purge. Migration2→3→4 preserves prior cache/progress/outbox. Explicit KSP→test-assets ordering ensures the current schema is packaged on first generation.

Two large server snapshots first exceeded8MiB in one delta page; a PostgreSQL byte budget now splits the page while preserving cursor/has_more. A one-second UI wait also timed out under the combined build; asynchronous UI waits now allow10 seconds, and the final ordinary UI run passes. Emulator-5554 property discovery emitted a ddmlib timeout; final verification explicitly uses working emulator-5556 and exits successfully.

## Reproduction

Run commands sequentially using Android Studio's JBR for JAVA_HOME. The connected test excludes fixtures that require explicit process phases:

```text
gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notClass=com.nihonix.mobile.FlashcardProcessRestartTest,com.nihonix.mobile.OfflineProcessRestartTest
gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease -PNIHONIX_RELEASE_API_BASE_URL=https://api.example.test/api/
npm run build
npm test -- --runInBand
npm run test:sprint04
npm run test:sprint05
npm run test:sprint06
```

Backend commands run in ../nihonix-backend. In PowerShell, quote each complete -P argument. ANDROID_SERIAL=emulator-5556 scopes the connected test.

Install the same built app/test APKs once, then run:

```text
adb -s emulator-5556 shell am instrument -w -e class com.nihonix.mobile.FlashcardProcessRestartTest -e restartPhase seed com.nihonix.mobile.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5556 shell am force-stop com.nihonix.mobile
adb -s emulator-5556 shell am instrument -w -e class com.nihonix.mobile.FlashcardProcessRestartTest -e restartPhase verify com.nihonix.mobile.test/androidx.test.runner.AndroidJUnitRunner
```

Repeat with OfflineProcessRestartTest for the Sprint05 regression. Neither sequence reinstalls/clears app data between seed and verify. Both assert a different process PID and remove their fixture DB after verification.

## Rollout limits

Local PostgreSQL fixtures use temporary schemas removed in finally, actual JWT registration/verification/login and stubbed email/AI. They do not contact real providers. No manual shared migration/deployment was run; the pre-existing local DATABASE_SYNCHRONIZE=true means a running development watcher may synchronize entities.

Migration1794786400000 preserves unowned legacy rows without guessing ownership. Explicit ownership mapping requires evidence. Server receipt/head retention lasts for account lifetime; future compaction needs a resync contract. Rollback down removes ownership metadata and can restore the old authorization gap; it is not a safe production downgrade procedure.

Up to200 text cards/set,4000 characters per face/meaning,7MiB native aggregate and8MiB flashcard API body. Delta pages have an8MiB cumulative snapshot budget, always allowing one event to advance. Offline review/editing covers locally available private content; first-device import needs online delta sync. WorkManager timing remains subject to Doze/OEM/force-stop restrictions.

A deployed HTTPS URL, shared migration and physical-device/multi-device/background acceptance remain rollout work. Release packaging uses reserved https://api.example.test/api/ only. No SM-2, AI/quota generation, inferred completion or activity-duration recording is delivered. User-local .idea files remain outside the commits. Delivery is grouped into Android data/sync, Android UI, backend schema/contracts, backend API/ownership, and documentation commits on the existing branches.

[Contract](../contracts/sprint-06-api.md) · [Sprint](sprint-06.md) · [Implementation ledger](sprint-06-implementation.md).
