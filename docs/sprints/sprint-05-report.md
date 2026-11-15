# Sprint 05 execution report

Date: 2026-11-15. Scope: F6/F7 and F3/F4/F5 persistent catalog caching. Android and sibling NestJS backend implementation. The user subsequently authorized grouped commits and push; deployment remains separate.

## Delivered behavior

Previously fetched level/topic/bundle and vocabulary/grammar pages/details persist in environment-specific Room storage. Public GET requests validate ETags; 304 restores saved payloads. Network/5xx can display saved content with an offline notice. A complete six-collection inventory removes projections containing deleted references; synchronized request ordering prevents delayed responses from overwriting newer cache or restoring removed content.

Vocabulary and grammar detail screens expose **Đã học/Bỏ đánh dấu**, explicitly confirmed by the user. Merely opening content does not change completion. Local progress and UUID outbox operation commit atomically. Pending/blocked status stays visible; retries keep the same mutation ID unless the user explicitly creates a corrected edit.

Topics & Levels → **Tiến độ** opens topic/JLPT summaries with pending/error counts and sync/retry/discard controls. Counts are optimistic local state against the downloaded authoritative inventory; deleted items stop contributing. Server summaries count current catalog references.

Backend provides JWT-protected progress/batch, progress/changes and progress/summary. Account-row locking, durable receipt outcomes and unique keys make retries idempotent. Timestamp then UUID resolves conflicts; future clocks and missing items produce per-item rejections. A JWT/expected-account mismatch fails before applying data.

WorkManager jobs require network and use backoff/periodic recovery. Logout cancels protected work and hides progress while retaining that account's queue. Acknowledgements remove their exact ID and preserve newer edits. Account deletion invalidates the active session before the final local purge, preventing late sync from restoring private rows.

## Verification evidence

| Check | Result |
| --- | --- |
| Android unit suite | 106 passed, zero failures |
| Complete emulator suite (emulator-5554) | 25 passed; phase-specific process-restart fixture intentionally ignored in the ordinary suite |
| Same-APK process-restart fixture | seed passed; force-stop; verify passed in a different PID with cache/progress/stable mutation intact |
| Android lint/debug/release packaging | Passed; release used reserved example HTTPS endpoint only |
| Backend build/Jest | Passed; 60 tests across 10 suites |
| PostgreSQL/HTTP Sprint04 regression | Passed in isolated temporary schema |
| PostgreSQL/HTTP Sprint05 integration | Passed; temporary schema removed in finally |
| Live localhost:3010 smoke | catalog/manifest200, schema1, six collections and ETag; anonymous progress/summary401 |
| Git/SRS checks | diff --check passed in both repositories; all 105 FR IDs retained |
| Fresh review | Retry/account and delayed-cache races reproduced RED→GREEN and fixed |
| Deletion lifecycle regression | Failing session-before-purge assertion reproduced, then full unit suite GREEN |

Android commands:

```text
:app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug :app:assembleRelease -PNIHONIX_RELEASE_API_BASE_URL=https://api.example.test/api/
:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease -PNIHONIX_RELEASE_API_BASE_URL=https://api.example.test/api/
```

The full emulator suite ran before the final two-line deletion-order correction. The final 106-test unit/lint/debug/release run includes that correction; the final targeted AccountScreensTest run also passed all four tests after that correction.

Process-death verification installs the same built app/test APKs once, runs:

```text
adb -s emulator-5554 shell am instrument -w -e class com.nihonix.mobile.OfflineProcessRestartTest -e restartPhase seed com.nihonix.mobile.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5554 shell am force-stop com.nihonix.mobile
adb -s emulator-5554 shell am instrument -w -e class com.nihonix.mobile.OfflineProcessRestartTest -e restartPhase verify com.nihonix.mobile.test/androidx.test.runner.AndroidJUnitRunner
```

Do not substitute two separate Gradle connected-test invocations: the test infrastructure reinstalls/clears the target package, which tests installation rather than process-only recovery. The seed intentionally leaves its Room connection open; verification asserts a different process PID and removes its fixture database.

Backend commands: npm run build; npm test -- --runInBand; npm run test:sprint04; npm run test:sprint05. Fixtures exercise real local PostgreSQL, actual JWT registration/verification/login with stubbed email, account isolation, payload-reuse409, simultaneous conflicting edits, partial rejection, cursor paging, current-catalog summaries, stable/changed/deleted inventory, HTTP304, successful legacy entity-sync JSON/stable fallback ETag/304 (with response deadline) and durable replay after service recreation. No real SMTP/AI/media provider is contacted.

Room tests also cover migration1→2 preserving cache/outbox, failed local transaction rollback, retry after reopen, HTTP429 retention, rejected-edit retry/discard, account switch while retrying, and topic/level summary updates after inventory deletion. UI tests cover explicit marking/unmarking and pending state. Retrofit tests verify exact wire fields, cursor precision and owner pinning.

Migration testing initially exposed serialization runtime inconsistency (app core1.7.3 versus test JSON1.8.1). Aligning serialization1.8.1 fixed the ABI failure; migration tests then passed. Cache race tests failed on the original implementation before synchronized generation/sequence guards; retry ownership failed before captured-owner/expected-mutation transactions.

## Rollout and limitations

- Offline covers previously fetched URLs/pages/details. Inventory metadata is not a full download of every body. A deleted reference invalidates its saved projection; reopening online fetches a corrected projection.
- The complete inventory targets the current small catalog. Each background run validates at most 30 oldest saved projections. Large catalogs need snapshot pagination/completeness semantics before scaling.
- Progress receipt retention lasts for account lifetime; storage grows until a future compaction/full-resync contract. Flashcard/activity producer columns exist, but F8/F10 implementations remain future work.
- Android controls background timing; no immediate-sync promise under Doze, force-stop or OEM restrictions. Physical-device airplane-mode, background restrictions and real multi-device shared-environment acceptance remain rollout checks.
- Backend migration1794700000000 creates progress/receipt tables; no manual shared-schema migration/deployment was run. Existing local .env.develop already has DATABASE_SYNCHRONIZE=true, so a development watcher/restart may synchronize new entities automatically. Controlled migration up/down tests operate only in temporary schemas.
- A real deployed HTTPS API URL is still required. https://api.example.test/api/ verified packaging only and was not saved as a deployment endpoint. SMTP configuration was unchanged.
- The user authorized grouping the reviewed changes into feature-related commits and pushing the current Android/backend branches. User-local .idea files are outside these commits.

[API contract](../contracts/sprint-05-api.md) · [Sprint record](sprint-05.md) · [Decisions/ledger](sprint-05-implementation.md).
