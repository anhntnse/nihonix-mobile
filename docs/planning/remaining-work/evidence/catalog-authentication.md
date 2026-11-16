# D02 — Authenticated catalog execution ledger

User decision: 2026-11-16, require login for catalog reads per SRS/BR-001. Scope includes levels/topics/bundles, legacy vocabulary/grammar endpoints, /catalog pages/details/manifest and entity-sync GET/read-only POST check. Auth/config/liveness endpoints remain public; authoring remains Admin-only.

Status: **Verified locally** — D02 Accepted and implemented; backend/unit/lint/debug/emulator regression and review passed. No commit/push/deploy/release/shared database mutation or real providers.

Rulings:
- Preserve shared catalog projections (identical content for all signed-in learners) and Room schema. Require a current local login at the access boundary; never serve cache after logout or return an old in-flight login response. Locally cached login can study offline; remote revocation is only knowable when online, and 401/403 must never fall back to cache.
- Catalog refresh now belongs to account work; cancel legacy anonymous work and pin refresh requests to captured account/session. Credentials are not persisted in cache. Refresh only in-environment catalog URLs from the store, never foreign or private endpoints.
- Keep controller JWT guards, with exact route policy regression. No new global auth guard; no change to quota or private-owner rules.

RED evidence:
- Backend route policy test found all 22 previously public catalog route patterns missing JWT.
- Android real NetworkModule/MockWebServer tests: absent catalog Bearer headers, authenticated payload not cached, cached body available logged out, delayed prior-session response delivered after login replacement (5 failures, 1 existing revocation case pass).
- Initial Android harness failed endpoint validation because Windows reverse DNS reported loopback as kubernetes.docker.internal; pin fixture URL host to literal 127.0.0.1, do not weaken production endpoint policy.
- After guards, expanded Sprint09 reached Vary assertion and failed because authenticated catalog did not advertise Authorization variation; headers now private/no-cache + Vary Authorization, including legacy routes.
- Added foreign cached URL regression: background refresh followed an out-of-environment URL before the filter; other 8 new catalog-auth tests already passed. Filter now restricts refresh to the configured catalog origin/path.

## Verification — latest local source

- Initial Android cache/wire regression: 14/14 pass. Final full unit run: **146 passed, 0 failed**, including 9 new real NetworkModule/MockWebServer authorization cases, 8 existing cache ordering/304 regressions and prior private-account suites. New cases cover same-session refresh + HTTP304, no login background work, foreign cached URL rejection and session replacement during a delayed response.
- `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`: successful. Debug only.
- Backend `npm run build` and `npm test -- --runInBand`: successful, **18 suites / 101 tests**.
- `npm run test:sprint03`, `test:sprint04`, `test:sprint05`, `test:sprint09`: PASS using random isolated local PostgreSQL schemas and fake providers. Sprint04 now registers/verifies/logs in a real fixture user. Sprint09 tests all 22 catalog patterns as guest401 and authenticated200; conditional guest401, authenticated304, malformed/revoked/deleted-account tokens401 and private/no-cache + Vary Authorization.
- Legacy `test/api.e2e-spec.ts` catalog calls now attach its accessToken; suite not run because its shared-environment/auth harness is not isolated. No shared database/provider operation authorized by this change.
- Fresh independent read-only D02 review: no actionable correctness/security findings. Reviewer did not mutate files or run concurrent tests.
- SRS OQ-015, API decision, Sprint03/04/05 contracts, offline architecture, Sprint09 acceptance, section02 plan and 82-route authorization matrix reconciled. Matrix now **13 public / 63 authenticated catalog-or-owner / 6 Admin-only**. Public auth/bootstrap/liveness remain explicit exceptions.
- `:app:connectedDebugAndroidTest` on emulator-5554/API37: **74 passed, 0 failed, 5 intentionally ignored** (4 explicit restart-phase fixtures + BackupConfigTest). Restart fixtures rerun with seed4/4 → force-stop → verify4/4, same installed APK, different PID. Initial manual attempt found Gradle had removed instrumentation package; installed the just-built debug/test APKs before successful phases. Backup behavior unchanged; its cloud/transfer phases remain prior group01 evidence, not rerun in D02.
- Final diff checks clean in both repositories; package.json/lock unchanged. Existing .idea files preserved. No commit/push/deploy/release/shared database/provider operations.
