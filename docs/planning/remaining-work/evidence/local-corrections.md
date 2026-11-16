# Local corrections — execution ledger

Plan: [01-local-corrections.md](../01-local-corrections.md). Started 2026-11-16. Status: **Verified locally** — final regression complete, no commit/push.

Baseline: branch `feat/sprint-08-09-streak-notifications`, HEAD `78626b9a1b0faa54bb8f35f42bca94b25962d9a3`; existing docs changes and untracked `.idea/` left intact. Android unit baseline: 132 passed, 0 failed. Working changes are uncommitted.

## Rulings and scope

- Work in the current IDE feature checkout to preserve IDE buffer safety and existing uncommitted plans; do not switch branches, commit or push. No shared interfaces between FIX-01/02/03 except final validation.
- Exclude the entire sharedpref domain in all backup modes. Existing token/user/push preferences were already excluded; cached app configuration and installation-local preferences should not transfer. Keep database exclusion. No wildcard paths or config migration. Future portable preferences will need an explicit backup-policy decision.
- Keep this screen's existing English UI. English plural rules are applied specifically to streak resources so a Vietnamese/Japanese device does not select English “days” for 1. This does not change device locale or learning-content language. Revisit this choice when translating the full application UI.
- Add clock-driven quota expiry with server `resets_at` as an earlier deadline; future/zero timestamps and malformed reset are stale. Expiry performs no network calls. Recheck cache/owner/bucket before a new generation, including after flashcard sync; receipt recovery/retry paths remain available.
- No deploy, release build, shared database mutation or real provider operation.

## FIX-01: Backup config

Files: backup_rules.xml, data_extraction_rules.xml; new phased BackupConfigTest.

RED: existing rules restored `app_config_358159305` after a real backup, app-data clear and restore on emulator-5554/API37. Positive-control file was present; verify failed with “Restored private preference”. Initial harness attempt returned restoreFinished -1000 because package-manager metadata was missing; repeating backup with `@pm@` resolved the harness problem before assessing product behavior.

GREEN: same-APK LocalTransport backup/restore passed in both default/cloud mode and `is_device_transfer=true` mode. Each seed and verify phase: 1/1. Verify checks:
- ordinary unexcluded file restored (positive control);
- actual config filename and alternative positive/negative endpoint hashes do not restore;
- token/fallback/user/push probe preferences and test database do not restore;
- no old maintenance/min-version restriction; unavailable network gives UNAVAILABLE; fresh config loads READY and replaces cache.

Commands per mode (PowerShell; APKs built/installed beforehand):

```powershell
adb shell am instrument -w -e class com.nihonix.mobile.BackupConfigTest -e backupPhase seed com.nihonix.mobile.test/androidx.test.runner.AndroidJUnitRunner
adb shell bmgr backupnow --non-incremental '@pm@' com.nihonix.mobile
adb shell pm clear com.nihonix.mobile
adb shell bmgr restore 1 com.nihonix.mobile
adb shell am instrument -w -e class com.nihonix.mobile.BackupConfigTest -e backupPhase verify com.nihonix.mobile.test/androidx.test.runner.AndroidJUnitRunner
```

Do not run clear/restore on an install containing user data. This emulator had no Nihonix package before this task; all app data used here was synthetic. Transport mode was set only for this test. Local package backup was wiped; original GMS transport, absent local parameters and disabled backup state restored afterward. No cloud-provider backup occurred.

Device-transfer simulation follows [AOSP CTS backup-rule testing](https://android.googlesource.com/platform/cts/+/e5ca08da528acb7740f7b0508dab7625497bbd74%5E2..e5ca08da528acb7740f7b0508dab7625497bbd74/). This proves API37 LocalTransport behavior, not OEM migration or a pre-Android12 device run. Legacy XML is aligned and lint-checked.

## FIX-02: Quota freshness

RED: real Room repository retained quotaFresh=true after 61 seconds without a database write; separate future-timestamp fixture also failed before the fix.

Implementation: QuotaFreshness.kt contains deterministic TTL/reset validation and a cold cancellable flow. PracticeRepository replaces the timer on cache/account changes; parsing sets/drafts is not repeated on every clock check. No timer survives cancellation or runs after expiry. PracticeViewModel rechecks authoritative local cache readiness before sending new practice/cards; backend remains the quota authority.

Tests: 59,999/60,000 ms, future/zero timestamps, earlier reset, malformed reset, idle expiry, resume clock jump, cancellation/new collector; Room tests for idle/future, account switch and pre-generation check before an observer update. Fixture quota reset now uses its current Vietnam day instead of a hardcoded expired date.

## FIX-03: Streak

RED: existing screen failed singular expectation. Fresh review identified locale-specific plural selection; Vietnamese-context UI regression also failed before the final fix.

Implementation: current/longest streak use English quantity resources, scoped English resource configuration and unchanged server counts. UI cases cover 0/1/2, font scale 2 and Vietnamese configuration.

## Verification

- Baseline: `:app:testDebugUnitTest` — 132 passed.
- First implementation run: `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` — successful, 137 unit tests passed.
- Backup cloud/default and transfer-mode phased tests — PASS as above.
- Final latest-source `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` — successful; 137 unit tests passed, 0 failed.
- `:app:connectedDebugAndroidTest` on emulator-5554/API37 — 74 passed, 0 failed, 5 intentionally ignored (4 restart-phase fixtures plus BackupConfigTest).
- All four restart fixtures (Offline/Flashcard/Practice/Activity): explicit `restartPhase=seed` 4/4 PASS → force-stop → `restartPhase=verify` 4/4 PASS, same installed APK and different process PID. No reinstall or app-data clear between phases.
- BackupConfigTest explicit phases already PASS in both modes above; the five skips are not missing validation.
- The whole suite includes real idle expiry (61 seconds without DB emissions), future timestamp, just-in-time generation readiness, account isolation, streak 0/1/2, font scale 2 and Vietnamese-context singular regression.
- New unit tests use virtual time for reset/resume/cancellation; no physical-device sleep/resume, pre-Android12 backup or OEM migration acceptance is claimed.
- Fresh read-only review: one P2 locale issue, reproduced RED and addressed; no other actionable quota/account/backup findings.
- No commit/push; changes remain in the existing feature branch. Existing `.idea/` and other plan files are preserved.

## Completion ledger

- FIX-01: complete locally — real backup regression RED→GREEN, cloud/default and device-transfer simulation, lint PASS.
- FIX-02: complete locally — idle/future regressions RED→GREEN; 137-unit green suite and Room/account/restart regression PASS.
- FIX-03: complete locally — singular and reviewer locale regression RED→GREEN; font-scale/0/1/2 UI checks PASS.
- Final review: independent read-only reviewer found the locale issue; addressed in one fix pass and covered by the final green suite. No deferred minor findings.
