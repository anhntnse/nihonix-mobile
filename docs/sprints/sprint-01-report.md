# Sprint 01 execution report

Date: 2026-11-14.
Status: Android foundation and backend audit delivered; release environment and product/backend decisions remain open.
Scope: foundation/security, existing contracts, baseline checks and future offline architecture. No backend production changes or database mutations.

## Delivered

- Debug endpoint defaults to http://10.0.2.2:3010/api/; release uses an explicit HTTPS endpoint through NIHONIX_RELEASE_API_BASE_URL.
- Release packaging fails when the endpoint is missing, HTTP, contains credentials/query/fragment, lacks a trailing slash or uses the listed local development hosts.
- Main network configuration denies cleartext and trusts system certificates. A debug resource overlay allows HTTP only for emulator loopback/localhost/127.0.0.1. Local-network permission is declared only in debug.
- Removed HTTP logging interceptor usage/dependency and raw refresh-error logging.
- Secure tokens never fall back to ordinary preferences. Old plaintext fallback preferences are removed, initialization/write failure leaves no usable session, and login reports a normal error rather than succeeding without secure persistence.
- Token writes are committed before observable session activation. Logout/refresh failure invalidates the session flow and protected navigation returns to login.
- Tokens and cached profile are excluded from cloud backup and device transfer.
- The local-network notice is optional, checks the actual OS grant, supports rationale/settings recovery and leaves navigation available.
- Audited [backend source and live read-only API](../contracts/backend-audit.md), recorded [synthetic contract fixtures](../contracts/auth-catalog-fixtures.json), and selected the [Room/WorkManager offline architecture](../architecture/offline-sync.md) for Sprint 05.

## Verification evidence

| Check | Result |
| --- | --- |
| Initial shell Gradle invocation | Could not start: JAVA_HOME/java absent in shell PATH; no machine settings changed |
| Android Studio Gradle baseline: :app:assembleDebug :app:testDebugUnitTest | Passed; 28 tests |
| Final :app:assembleDebug :app:testDebugUnitTest :app:lintDebug | Passed; 36 tests, including 8 new security/storage/endpoint cases |
| :app:assembleRelease without endpoint | Expected failure at validateReleaseEnvironment |
| :app:validateReleaseEnvironment with http://localhost:3010/api/ | Expected failure; HTTP development URL rejected |
| :app:assembleRelease with https://api.example.test/api/ | Passed; reserved example address used only to verify release packaging, not a production endpoint |
| Android Studio Gradle sync | Passed |
| Debug deployment to emulator-5556 | Passed; updated app opened Topics & Levels and displayed N5 topics with the existing session |
| Backend npm run build | Passed at commit b981b845b6e41add43e0f8f4901d5c38943635ce |
| Backend npm test -- --runInBand | Passed; 2 suites / 4 tests |
| Local root, OpenAPI, health, levels/topics | Success; 5 levels, 125 topics |
| Protected profile/user-level requests without credentials | 401 |
| Local app/config | 404 |
| git diff --check | Passed |

Lint initially found three missing includeSubdomains attributes in the new debug configuration; these were fixed and lint reran successfully. Lint warnings remain, including older dependency/custom-lint compatibility warnings; no blanket suppression or lint baseline was added.

The device check covered startup with an existing session, not permission revocation, encrypted-storage failure on real hardware, a fresh registration/login, refresh expiration, offline synchronization or full acceptance. The new storage-failure tests use JVM preference doubles; Keystore behavior still needs device-level coverage. Existing live server revision was not verified against the audited Git commit.

## Using the environment

The API supplied by the user is http://localhost:3010/api. On the Android emulator, use the default http://10.0.2.2:3010/api/. A physical device can use localhost with adb reverse, or an agreed HTTPS development endpoint.

Optional debug override:

```text
-PNIHONIX_DEBUG_API_BASE_URL=http://127.0.0.1:3010/api/
```

Release builds require NIHONIX_RELEASE_API_BASE_URL as a Gradle property or environment variable, ending in / and pointing to the real deployed HTTPS API. Do not use the example.test verification address for distribution. Debug HTTP is restricted to the three explicitly allowed local hosts; arbitrary LAN HTTP is not enabled.

Run checks through Android Studio's Gradle runner in this environment, or configure a supported JDK before invoking gradlew from a standalone shell.

## Open work and decisions

| ID | Remaining item | Responsible role / dependency |
| --- | --- | --- |
| S1-BE-01 | Provide staging/production HTTPS URL, deployment/migration owner and agreed test identities | Backend/deployment owner TBD |
| S1-CONTRACT-01 | Decide whether catalog reads should remain public or require authentication | Product/backend owner TBD; current behavior is public |
| S1-CONTRACT-02 | Define verification route/challenge/resend/expiry/rate-limit and unified password policy for Sprint 02 | Backend/product owner TBD; current register minimum 6, reset minimum 8 |
| S1-CONTRACT-03 | Confirm canonical full-profile response and ownership policy | Backend owner TBD; auth/me currently returns a reduced JWT profile |
| S1-SYNC-01 | Agree server IDs, idempotency/tombstones/cursors/conflict and activity contracts | Backend owner TBD; frontend architecture boundary is documented |
| S1-NFR-01 | Agree measurable latency/load/recovery targets and representative devices | Team planning; no performance measurements yet |
| S1-QA-01 | Add real auth/refresh/permission/Keystore integration coverage | Android/backend QA; test identities required |

Backend authorization and ownership findings are detailed in the audit. Resolve unguarded authoring/seeding, personal flashcard ownership and database TLS policy before production exposure. These were identified without invoking unsafe write endpoints.

Next implementation increment is Sprint 02 authentication/profile. Sprint 01 is not marked wholly Done while external environment and contract work remains open.

## Reference basis

The variant-specific cleartext/trust policy follows [Android network security configuration](https://developer.android.com/privacy-and-security/security-config). Backup exclusions follow [Android Auto Backup rules](https://developer.android.com/identity/data/autobackup). The permissions review used the local android-permissions-security skill; the app launcher remains exported as required for normal launching.
