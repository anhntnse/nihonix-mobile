# Sprint 02 execution report

Date: 2026-11-14. Status: implemented locally; shared-environment rollout and provider acceptance pending.

## Delivered

Android registration leads to email verification with resend countdown. Password recovery can restart with a new code/email. Password validation aligns with the backend. Refresh handles rotation, outages, revocation and account changes. Profile editing, photo-picker avatar upload, learning preferences, logout and confirmed deletion are wired through repositories and ViewModels. Existing username login, manual dependency injection and JLPT behavior remain.

Backend checkout: C:/Users/Ngoc Anh/AndroidStudioProjects/nihonix-backend. Implemented verification, verified-login gate, persisted challenge limits, credential throttling, transactional refresh/reset, session revocation, full profiles, owner-scoped edits/settings/deletion, avatar content validation and cleanup. Logs omit request bodies/query strings. See [account API](../contracts/sprint-02-api.md).

Migration 1794614400000-SprintTwoAccounts adds email_verified_at, learning_settings and attempt_count. Existing accounts must verify email and sign in again. Shared database/server have not been migrated or restarted.

## Evidence

- Backend build and 27 unit tests across six suites passed.
- npm run test:sprint02 passed with actual local PostgreSQL in a unique temporary schema removed afterward. Covers migration up/down/up, register/verify/login, ownership, preferences, avatar handoff, simultaneous refresh (one success), password recovery/token-purpose/reuse rejection, throttling, logout and deletion.
- Final latest-source Android Studio Gradle run passed: :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest :app:assembleRelease -PNIHONIX_RELEASE_API_BASE_URL=https://api.example.test/api/. 61 unit tests and four emulator-5556 instrumented tests passed. UI checks include verification, small-width/large-font dark-theme editing and deletion cancellation. Release URL is a reserved packaging example, not a deployment endpoint.
- git diff --check passed in both repositories; changes remain uncommitted. Terminal Gradle initially lacked JAVA_HOME; Android Studio's configured JDK completed the final checks.
- SMTP and Cloudinary are fake providers in automated tests; no real messages or media-provider calls occurred.

## Testing approach

Retained JUnit 4, coroutine test dispatchers, fake repositories and manual DI. MockWebServer verifies real Retrofit serialization, null/204 handling, headers, refresh and account-switch races. Compose tests verify behavior; test-only AndroidX versions now support API 37 InputManager. No screenshot coverage or percentage claim is made. Backend unit tests complement actual transactional PostgreSQL/Nest HTTP integration.

The isolated harness uses PostgreSQL 13+ core gen_random_uuid with schema-only search_path; it does not install extensions or modify shared schemas.

## Acceptance and rollout

AC-001/AC-002/AC-016 account behavior has local evidence; real providers/shared-environment demos remain pending. F2 persistence and ownership passed locally; whole-product AC-017 remains open for other features. FR-008/FCM device cleanup and reminder delivery remain Sprint 08. Offline content remains Sprint 05.

Configure SMTP/Cloudinary, review/apply migration in the target environment, restart matching backend, then verify new/existing-account verification and real avatar upload/deletion/relaunch persistence. Review retention/backups and distributed gateway limits before release. Production HTTPS URL and catalog authorization policy remain open. Changes remain uncommitted.
