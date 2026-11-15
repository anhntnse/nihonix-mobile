# Sprint 04 execution report

Date: 2026-11-15. Scope: F4 vocabulary and F5 grammar.

## Delivered behavior

From Topics & Levels, users can open Vocabulary or Grammar; topic details also open either flow scoped to the selected topic. Vocabulary has set and word lists/details. Grammar has sub-topic and pattern lists/details. Group details open their paginated children. Searches cover stored Japanese/readings/localized content; topic and JLPT filters narrow results. Users can switch displayed content language between English, Vietnamese and Japanese.

Shared Compose screens use lazy lists, stable item keys, explicit Load more, retry and empty states. Details render readings, meanings/explanations, examples, conjugation rules, grammar points and difficulty when available. Existing ServiceLocator/ViewModel factory and repositories remain; SavedStateHandle restores filters/search/language and navigation arguments. Session loss clears learning destinations with the existing auth behavior.

Backend adds LearningContentModule with compatible public /catalog projections, bounded pagination and response hashes. Existing vocabulary/grammar routes remain available. Authoring level mismatches now fail before invoking AI; omitted levels inherit the parent level. No shared catalog seeding, AI generation, SMTP/media calls, schema migration, commit or push was performed.

## Evidence

- Backend build passed.
- Backend unit tests: 55 passed across nine suites, including parent-level inheritance/conflict checks using fake AI.
- npm run test:sprint04 passed against actual local PostgreSQL in a unique temporary schema removed afterward. Fixtures include 25 words, 23 grammar patterns, multiple topics/levels and groups. Tests cover no-gap/no-duplicate paging, literal wildcard and multilingual search, parent filters, rich details, counts, response-version stability/changes, invalid/missing and empty content.
- npm run test:sprint03 passed again, covering existing Admin mutation gates and account/session regression behavior.
- Final Android verification passed: 94 unit tests, 15 emulator-5556 instrumented tests, debug/release assembly and lint. Gradle tasks: :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest :app:assembleRelease -PNIHONIX_RELEASE_API_BASE_URL=https://api.example.test/api/. The reserved example URL checks release packaging only. The subsequent incremental debug/release build, lint and unit-test run also passed after the final collection-navigation adjustment (94 passed, zero failures).
- Live read-only checks on localhost:3010 confirmed all four catalog collections succeed with page_size=1 and valid version metadata: 25 vocabulary items, five vocabulary sets, 133 grammar sub-topics and five grammar patterns at the time of checking. The development backend has loaded the module; shared data was not seeded or modified by these checks.
- git diff --check passed in both repositories. All 105 functional requirement IDs remain in the SRS.

New Android tests cover Retrofit query/DTO mapping, malformed payload rejection, missing-content errors, pagination append/retry, debounced search, stale-response cancellation, saved filters and detail retry. Compose tests cover list-to-detail selection, group-to-child actions, search/empty/clear, network retry, grammar rendering and small-width/large-font dark-theme Japanese examples.

Testing retains the existing JUnit/coroutine/MockWebServer/Compose approach. Jest mappings now include the existing vocabulary/grammar/AI aliases exercised by new tests. No new runtime library or architecture migration was needed.

## Limits and next steps

This is online learning content. FR-036/FR-044 persistent cache and F7 learning events remain Sprint 05; no completion/offline claim is made. Content comes from PostgreSQL, not generation during GET. Shared-environment acceptance, representative performance measurements and broader device/accessibility checks remain open. Offset pages are deterministic for a static catalog, not a multi-request immutable snapshot.

Deploy/restart the matching backend if the development watcher has not loaded the new module, install the app and demonstrate real catalog browsing/details. No new database migration is required. Existing public catalog policy and production HTTPS remain earlier rollout decisions. Exact routes/query semantics are in the [API contract](../contracts/sprint-04-api.md).
