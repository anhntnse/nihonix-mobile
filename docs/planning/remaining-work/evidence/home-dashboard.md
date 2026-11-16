# Group03 — Home Dashboard execution ledger

Plan: ../03-home-dashboard.md. Baseline Android c868848 (feat/sprint-08-09-streak-notifications), 2026-11-16. Status: **Verified locally** — HOME-01/02/03 complete. No commit/push/deploy/release/provider operations.

## HOME-01 design and mapping

User requested execution of the existing group03 plan; keep Compose Material3/MVVM/ServiceLocator. Continue in the current IDE feature checkout to preserve buffer safety, .idea untouched. No new dependencies or backend endpoint.

| Widget | Real source | Semantics |
| --- | --- | --- |
| Greeting/JLPT | AuthRepository.currentUserFlow + getCurrentUser | Saved profile remains visible on refresh error; absent level prompts Topics selection |
| Daily goal/today/streak | ActivityRepository summary + preferences | Accepted server seconds, server timezone/day; no percentage-to-minutes conversion. Pending activities displayed separately |
| Freshness | ActivityState.cachedAt + summary.today | Label last saved time; a summary from a different local date in its timezone is not today's total |
| Learning progress | account-specific Room progress/outbox through ProgressRepository.database | Learned vocabulary/grammar counts on this device, including optimistic edits; explicit pending/blocked count. No invented catalog-wide denominator |
| Shortcuts | Existing Vocabulary/Grammar/Practice/Flashcards/Progress/Activity/Topics/Profile routes | Explicit navigation only; no AI generation or completion writes |
| Recent/recommended | No trustworthy source currently exists | Omit recommendations and keep Browse topics shortcut |

Layout: greeting and current level, daily activity card with independent Refresh/error/cache notice, local learning counts, responsive shortcut grid. Semantic theme colors; bounded readable width, vertical scroll, no fixed-height text cards; 48dp actions. Home becomes login/session landing; Topics remains separate; feature Back returns Home; notification allowlist still opens activity above Home. Retain lazy scroll via rememberLazyListState; refresh on resume for changed profile/level/day.

Ruling: execute the existing requested design in place rather than restart approval questions — user authorized group03 and existing layout order; risk is visual preference changes can require polish.
Ruling: keep current English UI and Material3 theme — consistent with existing screens; full localization remains separate.
Ruling: show local learned counts and pending events separately from accepted server time — existing source has no trustworthy recommendation or complete catalog denominator; cost is less personalized overview until such a contract exists.
Ruling: tooling uses studio read/write/build instead of shell workspace scripts — protects unsaved IDE buffers; record evidence here rather than generated scratch.

Preflight: HOME-01 defines owner-tagged HomeDataSource and HomeUiState for HOME-02; HOME-03 consumes typed HomeDestination mapped through existing route builders. Account/auth state must gate all snapshots; section observers/refreshes must cancel and reject stale generations independently.

## Verification

- HOME-01 complete: mapping and layout recorded above; API source sufficient, no backend endpoint/migration/dependency additions.
- HOME-02 RED: 6/8 ViewModel tests failed against a minimal state shell (missing aggregates, offline errors, account switching and timezone-day projection). After implementation 8/8 passed.
- Follow-up progress-observer recovery RED: failed observer remained dead after Refresh; restarted scoped observers on refresh. Clock/day-boundary test already passed. Full latest unit run **156 passed, 0 failed** (10 Home cases).
- HOME-03 RED: all 6 HomeScreenTest cases failed against the empty composable. After implementation **6 passed**: source labels, independent errors/retry, 8 typed shortcuts, 320dp/2x font/dark reachability, lazy state restoration, stale-day treatment.
- Navigation RED: saved/login landing remained Topics; cold notification Back returned Topics. Login test initially used wrong label Login; corrected fixture to existing Sign In before assessing landing. Updated actual NavHost now has Home landing and protected routing. **5 navigation tests passed** including all shortcut routes and N5 filters, post-login stack, session loss, cold notification/back, rejected notification.
- Home summary refresh fetches preferences/summary and syncs pending events, without loading a calendar merely to show Home. Navigation DI defaults preserve existing ServiceLocator production flow; tests substitute finite fake API providers.
- Full `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` successful. Debug only.
- Full connected suite before review fix: **87 passed, 0 failed, 5 intentionally ignored** on emulator-5554/API37. Includes 6 HomeScreen and 7 actual HomeNavigation tests (all eight routes + N5 filters, login/back/session, notifications, real Room owner counts, accepted summary without calendar, navigation saved-state restoration).
- Fresh independent whole-change review completed: one P2 timezone finding, no other actionable or deferred minor findings. A settings update can replace cached preferences without replacing summary; old-timezone totals must not be called today's totals. Added regression: **11 passed / 1 failed** against pre-fix source, preserving the separate goal-only preference behavior.
- Final: fixed P2 timezone mismatch — `changedPreferenceTimezoneCannotReusePreviousTimezoneTotals` RED→GREEN. Require current cached preference timezone to match summary timezone; keep historical summary through saved-data fallback. Goal-only change regression remains GREEN.
- Final full unit/lint/debug build successful: **158 passed, 0 failed**, including 12 Home unit tests.
- First post-fix connected run: 86 passed, 5 intentionally ignored, 1 timeout in existing `PracticeScreensTest.generatedCardsRequireReviewAndSaveAndReplayDoesNotDuplicate` at preview wait line126. All Home cases passed. That exact existing case then passed in isolation on unchanged source. No speculative product/test changes; async quota/Room/UI generation path inspected, timeout not yet reproducible deterministically. Final full same-source rerun PASS: **87 passed, 0 failed, 5 intentional phase skips**. No product or fixture changes were made between the timeout, isolated pass and full rerun; root cause remains unconfirmed. Retain this intermittent failure for quality follow-up rather than claim it fixed.
- Four DB process-restart fixtures and BackupConfigTest intentionally require explicit phase arguments; not rerun in group03 (no DB schema/backup changes). Earlier phase evidence remains group01/group02. New Home saved-state restoration tests run in the regular suite.

- Final handoff: tracked diff check PASS; backend checkout unchanged/clean. New Home source/tests and documentation remain uncommitted on the existing Android feature branch. Existing .idea untouched. No deferred review minors; no provider/deploy/release operations.
- Limits: fake providers/local emulator only; state-restoration harness covers saved scroll/navigation/filter, not physical-device rotation/OEM behavior or measured NFRs. Existing BackupConfigTest is unchanged and its explicit cloud/transfer run remains group01 evidence.
