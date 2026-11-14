# Sprint 03 execution report

Date: 2026-11-14. Status: Android/backend implementation delivered; shared-environment acceptance remains pending.

## Changes

Android displays topic/vocabulary/grammar counts for the selected JLPT level and opens complete topic content from the detail screen. Vocabulary/Grammar tabs show localized content and examples with loading, retry and empty states. Filter/search use SavedStateHandle through the existing ViewModel factory; topic navigation and bundle visibility use saved Compose/navigation state.

Startup now retrieves validated app configuration, blocks on maintenance or an obsolete numeric versionCode, offers update/retry actions, and provides explicit continuation when configuration cannot be checked. Environment-scoped cached configuration preserves known restrictions during outages. Health is diagnostic, not a separate launch gate. Existing manual DI, Compose theme and username login remain.

Backend changes are in C:/Users/Ngoc Anh/AndroidStudioProjects/nihonix-backend. Counts and complete bundles use consistent PostgreSQL snapshots. Bundle hashes and fallback topic ETags are stable. Catalog mutation routes now require admin; existing public reads remain compatible. Public configuration is environment-controlled and validates version/TTL/update URLs. Responses carry server-generated diagnostic request IDs.

Exact contract and defaults: [Sprint 03 API](../contracts/sprint-03-api.md).

## Verification

Backend npm run build and npm test -- --runInBand passed: 40 tests in seven suites. npm run test:sprint03 passed with actual PostgreSQL/Nest HTTP in a unique temporary schema, removed afterward. It verifies N5–N1/zero counts, UUID/slug bundle equivalence, version changes after item edits, stable fallback ETags, missing/invalid catalog inputs, public configuration, guest/non-admin rejection, successful Admin topic creation, request IDs, and Sprint 02 account regressions. SMTP/Cloudinary remain fake providers; no AI generation is invoked.

Android final test run passed 75 unit tests and eight emulator-5556 instrumented tests, debug/release assembly and lint. Gradle tasks: :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest :app:assembleRelease -PNIHONIX_RELEASE_API_BASE_URL=https://api.example.test/api/. The reserved example URL checks release packaging only. The subsequent debug/release build and lint also passed after the final adjustment retaining the navigation controller across config retry. git diff --check passed in both repositories.

Tests use existing JUnit/coroutine/MockWebServer and Compose infrastructure. Focused config tests cover version boundaries, maintenance priority, fresh/expired/future cached timestamps, malformed config and unsafe update URLs. UI tests cover blocking maintenance/update screens, explicit unavailable-config continuation and bundle tabs/empty grammar. No performance benchmark or complete accessibility certification is claimed.

## Rollout and remaining work

No new Sprint 03 schema migration is required. Sprint 02's migration and matching auth backend still need rollout if not yet applied. Restart/deploy the reviewed backend build and configure APP_* variables before a shared app demo. Verify real content counts, provider integration, update destination and maintenance on/off in the target environment. Production HTTPS remains unprovided. Public catalog policy D02 remains a product decision; existing public behavior was preserved.

FR-027 persistent topic caching remains Sprint 05. Full F4/F5 learning screens remain Sprint 04. Shared environment, whole-product acceptance and production readiness are not marked complete. No server restart, shared data seeding, commits or push were performed.
