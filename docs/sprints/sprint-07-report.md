# Sprint07 execution report

Date2026-11-15. AI quota, practice and generated-flashcard preview/save are implemented in Android and the sibling NestJS backend. Local persistence, lint and debug/release verification passed. User subsequently authorized grouped Sprint07 commits and push for both repositories; deployment remains separate.

## Delivered behavior

User-approved quota is flashcards2/day, fill_blank+multiple_choice shared3/day, max15 items, reset00:00 Asia/Ho_Chi_Minh. Backend locks serialize reservation and success accounting. Stable UUID/payload receipts replay exact results; failed, cancelled and expired attempts do not consume successful allowance. Two-minute leases recover crashed work; a separate2-worker limit prevents cancellation abuse.

Existing Gemini integration performs one bounded attempt with strict complete-output validation. Practice supports both types, private paged/filterable history, detail, offline answer drafts, immutable queued submissions and backend results. Answers/explanations remain hidden before completion. Grading, receipt and one completion event commit together. No catalog item is automatically marked learned and no activity duration is invented.

AI flashcards remain a preview until explicit editor save. Stable generated IDs and canonical/local history prevent duplicate sets on replay. Room4→5 preserves prior progress/flashcards and stores account-private practice/request data. Background sync submits saved answers; it never automatically starts a paid generation.

## Verification evidence

| Check | Result |
| --- | --- |
| Native unit + ordinary instrumented regression | Passed:121 unit tests and full ordinary instrumented suite after review fixes; shell166 BUILD SUCCESSFUL |
| Backend build/Jest | Passed:84 tests,14 suites; shell167 |
| Sprint07 PostgreSQL/HTTP | Passed after cancelled-worker regression fix; shell165 |
| Sprint05/06/07 combined PostgreSQL regression | Passed, shell168; isolated schemas cleaned up |
| Same-APK process restart | Passed:3 seed tests → force-stop →3 verify tests, shells170/171; changed PID and stable persisted payloads |
| Android lint/debug/release packaging | BUILD SUCCESSFUL, shell172; reserved example HTTPS URL for packaging only |
| Source/document checks | git diff --check passed in both repositories;105 FR,19 BR,26 NFR,17 AC identifiers retained |

The fresh read-only review identified2 Important defects. Cancelled requests whose provider failed retained active slots until lease expiry; a new fixture first returned429 instead of201, then passed after releasing the cancelled lease under the account lock. Legacy completed sessions without new submission receipts showed editable questions; the UI regression first lacked the stored score, then passed with a read-only session/exercise projection. No completion event is invented for legacy results. One fix pass completed; no second review loop.

Earlier RED→GREEN evidence covers strict output/grading, Vietnam quota boundaries, provider truncation/retry bounds, stable native payloads, lost replies, migration/account isolation, Gson explicit result:null, stale quota refresh, option labels, request ordering and replayed preview saves. Expanded Compose tests cover429 form retention and dark/large-text/small-screen scrolling.

## Reproduction

Run commands sequentially, using Android Studio JBR for JAVA_HOME and ANDROID_SERIAL=emulator-5556. In PowerShell quote each whole dotted -P argument.

```text
gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notClass=com.nihonix.mobile.FlashcardProcessRestartTest,com.nihonix.mobile.OfflineProcessRestartTest,com.nihonix.mobile.PracticeProcessRestartTest --console=plain
gradlew.bat :app:lintDebug :app:assembleDebug :app:assembleRelease -PNIHONIX_RELEASE_API_BASE_URL=https://api.example.test/api/ --console=plain
npm run build
npm test -- --runInBand
npm run test:sprint05
npm run test:sprint06
npm run test:sprint07
```

Phase-only process fixtures require installing the same app/test APKs once, running seed, force-stopping com.nihonix.mobile and running verify without reinstalling or clearing data. PracticeProcessRestartTest checks changed PID and exact persisted generation/submission UUID/payload, then submission recovery without another generation call. Prior Sprint05/06 fixtures cover catalog/progress/flashcard persistence.

## Remaining rollout and minor follow-ups

No real provider/SMTP call or manual shared migration/deployment was performed. PostgreSQL fixtures use isolated schemas removed in finally, actual JWT HTTP and deterministic provider/mail doubles. Existing local synchronize=true can synchronize a running development watcher. Provider configured metadata means key presence, not proof of actual key/model acceptance.

Shared migration/HTTPS, actual configured Gemini acceptance, physical/multi-device/network-transition/Doze checks remain rollout gates. Release packaging uses a reserved example endpoint only. Migration1794872800000 down drops ledger/receipt/event metadata and is not a safe production quota rollback. Retention/cache compaction remains future work.

Minor review follow-ups: idle quota freshness expires only on cache emissions; an alternate submission UUID returning an existing canonical result is not separately registered; POST retry readiness checks run before receipt lookup (GET recovery remains available). Backend enforcement still prevents extra charges and duplicate completion events. Track these before broad rollout.

FR-078 has durable completion records; Sprint08 owns activity/streak/calendar aggregation. No FCM, SM-2 or inferred learning tracking is included. User-local .idea files remain untouched.

[Contract](../contracts/sprint-07-api.md) · [Sprint](sprint-07.md) · [Execution ledger](sprint-07-implementation.md).
