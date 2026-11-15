# Sprint 09 acceptance matrix (draft, 2026-11-16)

Purpose: give every SRS acceptance criterion AC-001–AC-017 a recorded outcome or a named blocker, as required by [delivery gates](../planning/delivery.md). This is an evidence index built from existing sprint reports plus the checks listed under "Sprint 09 checks run". It does **not** change SRS dispositions yet; review first, then update SRS §11.4.

Outcome legend:
- **Local pass**: automated or manual local evidence (unit/instrumented/PostgreSQL fixture/emulator run against a local isolated backend), no shared environment.
- **Partial**: some behavior has evidence, a named part does not.
- **Env-blocked**: needs shared deployment, real provider, HTTPS, or physical device; cannot be closed from a developer machine.

No AC below is marked fully accepted. Delivery gates require shared-environment acceptance for that.

## Matrix

| AC | Criterion (short) | Outcome | Evidence | What is still missing |
| --- | --- | --- | --- | --- |
| AC-001 | Register, verify email, log in | Local pass | [Sprint 02](sprint-02-report.md) `test:sprint02`; Sprint 09 emulator journey: sign up → verify code → sign in through the real app against a local backend | Real SMTP delivery (fake provider used); shared-environment demo |
| AC-002 | Unverified users blocked | Local pass | Sprint 02 verified-login gate and fixture; Sprint 09 route-guard audit and `test:sprint09` anonymous 401 on 25 personal routes | Shared-environment demo; existing accounts must verify after migration |
| AC-003 | Browse JLPT N5–N1 | Partial | [Sprint 03](sprint-03-report.md) fixture covers N5–N1 and zero counts; emulator journey showed seeded N5/N4 with topic list | Real N5–N1 dataset on a shared environment; D02 public-vs-authenticated policy undecided |
| AC-004 | Topics, vocabulary, grammar content | Local pass | Sprint 03/04 fixtures and Compose tests; emulator journey: topics list, word list (3 results), word detail | Shared data and performance measurements. Vocabulary sets and grammar not browsed on device (seed had no sets) |
| AC-005 | Previously synced content offline | Local pass | [Sprint 05](sprint-05-report.md) | Shared rollout, multi-device, Doze |
| AC-006 | ETag synchronization | Local pass | Sprint 05 (HTTP 304, manifest) | Shared rollout |
| AC-007 | Progress survives restart | Local pass | Sprint 05; same-APK force-stop restart re-run 2026-11-15 for Offline/Practice/Flashcard/Activity PASS | Physical-device process death |
| AC-008 | No duplicated progress records | Local pass | Sprint 05 durable receipts and replay fixture | Shared rollout |
| AC-009 | Personal flashcard folders/sets | Local pass | [Sprint 06](sprint-06-report.md) CRUD/offline/conflict/restart; `FlashcardScreensTest` 7/7 after test fix (see D-09-01/02) | Shared multi-device acceptance |
| AC-010 | AI generation within quota | Local pass | [Sprint 07](sprint-07-report.md) quota/reservation/replay/refund with fake provider | Configured real provider; shared acceptance |
| AC-011 | Complete exercises, view results | Local pass | Sprint 07 grading, replay, native UI | Shared acceptance; not exercised in the Sprint 09 UI journey |
| AC-012 | Activities recorded and synced | Local pass | [Sprint 08](sprint-08-report.md) tests; emulator journey: ~66 s on a word detail screen recorded as 73 server seconds (server API cross-check matched the app) | Physical device, multi-device, shared HTTPS |
| AC-013 | Streak/calendar reflect activity and timezone | Local pass | Sprint 08 midnight/DST split, late events, timezone-change stability; emulator journey: streak 1 day, day 15 shows 1m, timezone UTC, same after sign-out and sign-in | Real-timezone device check (journey ran in UTC) |
| AC-014 | Daily goal and reminder preferences | Local pass | Sprint 08 tests; emulator journey: toggled push and reminder, saved, server settings matched (goal 15, 20:00, UTC) | Shared acceptance |
| AC-015 | Push notifications handled correctly | Partial | Sprint 08 data-only payload, owner/session/binding checks, reminder dedup with fake provider; emulator obtained a real FCM token and registered it ("Notifications registered for this account") | Real recipient delivery (not authorized; fake provider sent 0 messages), Doze, scheduler enablement |
| AC-016 | Expired sessions and API errors | Local pass | Sprint 02 refresh rotation/revocation; Sprint 08 session-safe logout; Sprint 09: sessions revoked server-side, app refresh got `401 Invalid refresh token` and returned to Sign In (closes OQ-021 locally); `test:sprint09` refresh replay rejected, tokens invalid after logout | Same check against a shared backend and slow/failing networks |
| AC-017 | No cross-user data access | Local pass | Per-feature fixtures (Sprints 02, 05–08); Sprint 09 `test:sprint09` single-app cross-feature suite and route-guard audit | Admin-vs-user matrix covers users and catalog writes only; shared-environment check |

Totals: 0 fully accepted, 15 Local pass, 2 Partial (AC-003, AC-015). Every row still needs shared-environment acceptance.

## Sprint 09 checks run

### Backend route-guard audit (static read of `src/**/*.controller.ts`, 2026-11-16)

- Every route that writes data requires `JwtAuthGuard`. Catalog and AI authoring writes (`topics` POST and seed, `vocabulary-sets` POST, `vocabulary-items` and `grammar-patterns` generate) additionally require `RolesGuard` with `admin`.
- Unguarded routes are all GET: `app` health/root, `app/config`, `levels`, `catalog`, `topics` reads and bundle, vocabulary and grammar reads, `entity-sync` (check and single entity) and `ai/health`. These expose catalog, configuration or health only, matching the preserved public-read behavior (D02).
- Personal-data controllers (`flashcards*`, `progress`, `practice`, `learning-streaks`, `devices`, `users`) use class-level guards.
- Observations, no change made:
  1. There is no global `APP_GUARD`. Each controller opts in, so a new controller defaults to public. Consider a global guard with an explicit public marker, or a test that enumerates routes.
  2. `GET /ai/health` is public and returns `status` and `configured`, which tells anonymous callers whether an AI key is set. Low risk; decide whether to restrict it.
  3. `entity-sync` is public by design but is not described in the Sprint 03/04 contracts. Confirm it is intentionally part of the public catalog policy.

### Cross-feature ownership and role fixture (`npm run test:sprint09`, PASS 2026-11-16)

File: `nihonix-backend/test/sprint09.ownership.integration.ts`. One Nest app with all personal-data modules, temporary PostgreSQL schema, real JWT HTTP, fake email/FCM/AI. It asserts:
- 25 personal routes (flashcards, progress, practice, AI requests, activity, devices, users) return 401 anonymously.
- User B gets 404 on user A's flashcard folder/set/cards (read, update, delete), AI request status and cancel, practice set detail; B's progress delta and streak are empty; B cannot submit A's practice session (A's session stays not completed); `X-Nihonix-Account` of another user returns 409; B cannot remove A's device binding.
- `/users`: self read OK; other user 403 on read/update/delete; list is admin-only; self-service PUT cannot change email, status or role.
- Catalog writes (`topics`, `topics/seed`, `vocabulary-sets`, `vocabulary-items/generate`, `grammar-patterns/generate`): non-admin 403, admin passes the guard.
- Refresh token replay rejected; access and refresh tokens invalid after logout; malformed bearer 401.
- Account deletion returns 204, leaves no rows for the user in the inspected owner tables (guarded against a vacuous check: at least 4 tables inspected) and the deleted user's token is rejected.

### Emulator journey against a local isolated backend (2026-11-16)

Setup: `nihonix-backend/test/sprint09.serve.ts` served the full API on port 3099 from a temporary schema `sprint09_serve` on the local database, with fake email (code written to a temp file), fake FCM and fake AI. The debug app was built with `NIHONIX_DEBUG_API_BASE_URL=http://10.0.2.2:3099/api/` and run on `Medium_Phone_API_37.0`. Afterwards the server was stopped, the schema dropped (`--cleanup` also run), and the default debug build reinstalled. No real email, push or AI call happened; the fake FCM provider recorded 0 deliveries.

Steps through the real UI, all observed: app config check → allow local-network prompt → sign up → "Account Created" → email verification screen with the code → sign in → Topics & Levels (seeded content) → Vocabulary words list → word detail for ~66 s → Learning activity (streak 1, 1/15 min, calendar day 1m, UTC) → Profile → push and daily reminder on → Save → "Notifications registered for this account" → Sign out → sign in again (totals unchanged) → session revoked on the server → Refresh → 401 → Sign In screen.

Server cross-checks via the API matched the UI: 73 engaged seconds, streak 1, goal not completed; saved settings equal what the UI showed.

Not covered by the UI journey: practice, flashcards, progress and AI generation screens (covered by the instrumented suite and API fixtures), reminder delivery (fake provider), vocabulary sets and grammar (not seeded), non-UTC timezone.

UI observations (minor, not blocking): the streak line reads "Current streak: 1 days" (no singular form); on API 37 the debug build needs a local-network permission step before it can reach `10.0.2.2` (not applicable to a public HTTPS release endpoint).

### Release configuration review (static, 2026-11-16)

Good:
- Release sets `usesCleartextTraffic=false`; base network security config also forbids cleartext. The release API URL must be an HTTPS URL (`validateReleaseEnvironment` guard).
- No `Log.*`, `println` or HTTP logging interceptor in app code, so tokens and personal data are not logged.
- Tokens are in `EncryptedSharedPreferences` (`nihonix_secure_tokens`); the older fallback file is deleted on migration. Tokens, user storage, push state and all databases are excluded from both full backup and Android 12+ cloud backup and device transfer.
- `NihonixMessagingService` is `exported=false`; `MainActivity` is the only exported component. No `fallbackToDestructiveMigration` and no main-thread queries.
- Backend secrets (`.env.develop`, `.env.production`, `.env`, Firebase admin keys, service account files) are git-ignored; only `.env.example` is tracked. Android `.gitignore` now blocks admin SDK keys and `.secrets/`.

Findings:
- **R-1 (low)**: the `app_config_<hash>` SharedPreferences (cached public app configuration) is not in the backup exclusions. It contains no secret, but a restored copy could carry an old maintenance/version restriction to a new install. Add it to `backup_rules.xml` and `data_extraction_rules.xml`.
- **R-2 (medium, unverified)**: release builds enable R8 optimization with no project keep-rules file, and the app uses Gson reflection (124 `@SerializedName` fields across DTOs). Release builds were only assembled in earlier sprints, never run. Run a release build on a device against a real HTTPS endpoint, covering login, content and sync, before any release candidate.
- **R-3 (info)**: `app/google-services.json` is untracked and not ignored. It holds Firebase client configuration (not a server secret). Decide whether to commit it and restrict the API key in Google Cloud.

## Open defects and release blockers

| ID | Item | Severity | Status |
| --- | --- | --- | --- |
| D-09-01 | `FlashcardScreensTest.createFolderAndBulkSetValidateThenPersistJapaneseCards` failed on the API 37 emulator (also on Sprint 07 baseline `ed4aed2`) | Medium | **Fixed (test)**. Root cause: the test called `performScrollTo()` on `LazyColumn` items that are not composed until scrolled into view, so the result depended on viewport size. The test now scrolls the editor list with `performScrollToNode`. Product code unchanged |
| D-09-02 | `FlashcardScreensTest.largeTextDarkEditorKeepsSaveActionVisibleWithJapaneseInput` failed every run, same baseline | Medium | **Fixed (test)**, same cause and fix. `FlashcardScreensTest` 7/7 in three isolated runs; full suite 72 tests, 0 failures, 4 skipped (restart tests need a `restartPhase`, run manually and PASS). The cause is inferred from the fix, not from instrumenting the failing state |
| D-09-03 | `assembleRelease` not verifiable without a deployed HTTPS `NIHONIX_RELEASE_API_BASE_URL`; R-2 above also depends on it | Release-blocking for a release candidate | Env-blocked |
| D-09-04 | Shared migrations `1794614400000` (Sprint 02) through `1794959200000` (Sprint 08) not applied to a shared database; Sprint 08 down-migration is not a safe production rollback | Release-blocking | Env-blocked |
| D-09-05 | No real FCM recipient, physical-device, Doze or multi-device acceptance | Release-blocking for AC-015 | Env-blocked, needs explicit authorization |
| D-09-06 | R-1 backup exclusion for `app_config_*` | Low | Open, one-line fix per backup rule file |
| D-09-07 | Backend has no global auth guard; `GET /ai/health` discloses AI key presence | Low | Open, decision needed |

No Critical authentication, ownership or data-loss defect has been found in the checks run so far. This is absence of findings in the checks above, not proof; non-functional requirements (performance, accessibility, behavior under load) have **not** been measured.

## Remaining Sprint 09 work

Doable locally:
- Apply R-1, and decide D-09-07.
- Extend the UI journey to practice, flashcards, progress and a non-UTC timezone.
- Add a Sprint 09 step to the regression list (`test:sprint09`) and record it in the backend handoff.

Needs environment or authorization: shared deployment and HTTPS, release-build run (R-2), real SMTP/Cloudinary/FCM/AI providers, physical devices and Doze, load and monitoring checks, backup/recovery rehearsal, expected-load reminder scheduling.
