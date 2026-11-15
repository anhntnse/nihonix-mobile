# API and architecture decisions

Status: Partially resolved through Sprint 06. D09 accepted: authenticated owner chains, atomic complete-set writes, durable UUID receipts, CAS/dependency versions and deletion heads. User confirmed preserve-local/manual conflict resolution. Room v4 chunks large JSON rows and isolates producers; [contract](../contracts/sprint-06-api.md), [evidence](../sprints/sprint-06-report.md). D10 has only preview/explicit-save preparation; AI/quota remains Sprint07. Updated: 2026-11-15.

This register tracks decisions required to execute the [feature plans](../features/README.md). Routes in the SRS outside the existing auth/topics services are proposals until verified. Do not invent a backend language/framework or assume endpoints are production-ready.

## Current frontend baseline

Kotlin, Compose/Material 3, Navigation Compose, MVVM feature packages, repository interfaces, Retrofit/OkHttp/Gson and coroutines/Flow. Dependencies use ServiceLocator and AppViewModelFactory. Sprint 01 tokens use encrypted preferences without an ordinary-preferences fallback; user JSON uses private SharedPreferences. Sprint 05 adds Room public-content caching, protected account-scoped outbox and WorkManager background synchronization. FCM and AI integration remain future work.

Existing API methods are documented in [SRS Appendix A](../SRS.md#appendix-a-implemented-client-api-contracts). The default URL is emulator HTTP, profile retrieval is POST auth/me and login submits username/password.

## Decisions to close

D01 and D03 now have source-audit evidence; D06/D07/D08 now have Sprint 05 implementation and local contract evidence. D15's Android security/startup changes are implemented; deployment HTTPS is still missing. Other product/backend decisions remain open. Target sprint is a planning dependency, not an agreed deadline.

| ID | Decision and required evidence | Features | Target sprint |
| --- | --- | --- | --- |
| D01 | Locate backend repository/stack, endpoint inventory, staging environment, database migrations and deployment owner | All | 01 |
| D02 | Public catalog reads versus authenticated-only SRS policy; provide authorization matrix and negative tests | F1/F3–F5 | 01 |
| D03 | Username/email login, password policy, verify-email route, challenge expiry/resend/rate-limit and token rotation contracts | F1 | 01 |
| D04 | Canonical profile/settings/avatar/deletion routes and retention/session/media cleanup | F2 | 02 |
| D05 | Exact vocabulary/grammar/bundle DTO fields, nullability, stable IDs, pagination/search/order and fixtures | F3–F5 | 03, before Sprint 04 |
| D06 | Persistent-store/background worker choice, account boundaries, migrations, outbox IDs and retry conventions | F6–F8/F10 | 01 design; 05 verification |
| D07 | Manifest/entity routes, ETag scope/304 handling, tombstones/cursors and conflict timestamps/tie-break rules | F6–F8 | 04, before Sprint 05 |
| D08 | Progress item types, completion conditions, activity duration and aggregation rules | F7/F9/F10 | 04, before Sprint 05 |
| D09 | Flashcard parent ownership, bulk transaction/error semantics, deletion and multi-device conflicts | F8 | 05, before Sprint 06 |
| D10 | AI provider responsibility, generated schema, confirmation/save policy, grading and repeat submission | F8/F9 | 06, before Sprint 07 |
| D11 | Authoritative quotas/output caps, shared buckets, reset timezone, reservations, retry charging and refunds | F9/F12 | 06, before Sprint 07 |
| D12 | Qualifying activity, late submissions, timezone changes, daylight-saving and streak day boundaries | F10 | 07, before Sprint 08 |
| D13 | FCM token lifecycle, offline logout cleanup, payload destinations, scheduling and daily-goal suppression | F1/F2/F11 | 07, before Sprint 08 |
| D14 | App config schema, maintenance/minimum version, cache expiry and network/offline launch fallback | F13 | 02, before Sprint 03 |
| D15 | HTTPS release environment, logging redaction, secure-storage failure and local-network permission startup behavior | F1/F13 | 01 |
| D16 | Measurable NFR targets: startup/list latency, expected list/load sizes, sync recovery, accessibility and release support | All | 01 draft; 09 verification |

Original AI numerical limits remain proposals until D11 is accepted. Backend ownership, authorization, idempotency and quota enforcement cannot be delegated to the client.

## Recording an accepted decision

Add a dated entry using the following fields:

- Decision ID and status: Accepted / Superseded.
- Owner, date and affected FR/NFR IDs.
- Chosen behavior and rationale.
- Exact endpoint/schema or architecture choice with examples.
- Authentication, validation, errors and backward-compatibility/migration implications.
- Alternatives considered and operational/rollback implications when relevant.
- Links to implementation, contract fixtures and verification evidence.

Then update affected feature/sprint files and the SRS. Do not silently convert an unresolved proposal into an implementation fact.

## Sprint 05 outcomes — 2026-11-15

D06/D07 implemented locally: environment-specific Room v2, non-destructive migration, public projection/ETag cache, complete inventory deletion semantics, protected account-scoped outbox, WorkManager retries and atomic receipt/delta checkpoints. D08: the user explicitly confirmed manual Đã học/Bỏ đánh dấu; item types vocabulary/grammar, statuses learned/not_learned, timestamp/UUID LWW and current-catalog topic/level summaries. [Contract](../contracts/sprint-05-api.md) · [Evidence](../sprints/sprint-05-report.md). D09 flashcard conflicts and F10 activity remain separate. Shared rollout/production HTTPS and D02 policy remain open.

## Sprint 04 outcomes — 2026-11-15

D05 online vocabulary/grammar contracts are now implemented: compatible /catalog page/detail projections, deterministic ordering, literal multilingual substring search, topic/level/parent filters, rich Japanese content and per-response versions. Existing catalog arrays remain unchanged. Admin mutation regression and parent-level authoring checks pass locally. [Contract](../contracts/sprint-04-api.md) · [Evidence](../sprints/sprint-04-report.md). D07 persistent sync/304/tombstones and F6/F7 cache/progress remain Sprint 05; D02 public-read policy is unchanged.

## Sprint 03 outcomes — 2026-11-14

D05 has count/bundle schemas and stable versions implemented for F3; F4/F5 learning behavior remains Sprint 04. D14 implements public app/config, numeric versionCode comparison, HTTPS update destinations, validated cache TTLs, explicit unavailable-config continuation and retained cached restrictions. Health is diagnostic; responses carry X-Request-Id. See [canonical contract](../contracts/sprint-03-api.md) and [test evidence](../sprints/sprint-03-report.md). D02 preserves existing public catalog reads pending product policy; mutations now require Admin. Shared deployment and D15 production HTTPS remain open.

## Sprint 02 outcomes — 2026-11-14

D03/D04 account contracts are implemented and locally tested: username login; 8-character/72-byte password policy; new-account verification; purpose-scoped rotating tokens and immediate session revocation; full POST auth/me; owner-scoped profile/settings/avatar/deletion. See [canonical contract](../contracts/sprint-02-api.md) and [evidence](../sprints/sprint-02-report.md). Shared migration/provider acceptance remains pending. D02 catalog policy and D15 production HTTPS remain open. Earlier entries below preserve historical findings.

## Sprint 01 outcomes — 2026-11-14

- **D01 — Partially resolved:** [Backend audit](../contracts/backend-audit.md) records NestJS/TypeORM/PostgreSQL source at b981b845b6e41add43e0f8f4901d5c38943635ce, build/tests and local smoke checks. HTTPS staging, deployment/migration ownership and test identities remain open.
- **D02 — Observed, policy open:** Catalog reads are public in both source and local server. No guest entry is exposed by Android navigation. Keep current behavior until the product authorization policy is explicitly resolved.
- **D03 — Current wire contract established:** username/password login; registration password minimum 6, reset minimum 8; full_name optional. Flat token pair under data. Resend is for password recovery. Verification/new-account challenge and password-policy alignment remain Sprint 02 work. [Examples](../contracts/auth-catalog-fixtures.json).
- **D04 — Contract gap found:** POST auth/me returns a reduced JWT profile; richer profile fields are not returned there. Agree/implement canonical profile retrieval before editing/profile integration.
- **D06 — Android design selected:** Room plus WorkManager, account-scoped data/outbox, UUID retry IDs, transactional acknowledgements and environment-scoped cursors. No persistence dependencies or worker are installed yet. [Design and lifecycle policy](offline-sync.md). Backend wire contracts remain open.
- **D07 — Existing sync is partial:** Only level/topic kinds are enabled. Verify the successful @Res response path and extend contracts before offline promises.
- **D15 — Android implemented:** debug/release separation, HTTPS release gate, no request logging, secure storage failure handling, backup exclusions, optional debug-only local permission and session-loss navigation. A real HTTPS release URL is still required; the example.test build address was only a packaging check.
- **D16 — Measurement work open:** agree targets and representative devices in planning; passing build/unit tests is not a performance measurement.

Evidence and exact verification commands: [Sprint 01 report](../sprints/sprint-01-report.md). Product/backend ownership and the remaining contracts are not assigned to invented team members.
