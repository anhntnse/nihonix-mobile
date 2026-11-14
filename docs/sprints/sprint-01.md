# Sprint 01 — Foundation, contracts and release configuration

Status: In progress — Android foundation and backend audit delivered; environment/contracts remain open.
Started: 2026-11-14. End date/capacity: TBD.
Duration assumption: Two weeks; no calendar commitment.
Android implementation: completed in this workspace. Backend/deployment owner and QA ownership: TBD.

## Goal and scope

Foundation work for F1, F6 and F13. See the [execution report](sprint-01-report.md) for changes, exact commands, results and limits.

## Frontend backlog

- [x] Audit existing auth/topics behavior and tests; record reproducible baseline results (28 original tests).
- [x] Separate development/release endpoint settings; remove request logging and insecure token fallback.
- [x] Select Room/WorkManager, persistent-store/outbox boundaries and account isolation for future implementation; [decision](../architecture/offline-sync.md).
- [x] Define and implement non-blocking development local-network permission behavior and session-loss navigation.
- [x] Add focused security/storage/endpoint checks (36 total Android unit tests).
- [x] Build both variants, run lint, sync IDE and smoke-test the updated debug app on emulator-5556.

## Backend backlog

- [x] Locate repository and audit source at b981b845b6e41add43e0f8f4901d5c38943635ce.
- [x] Inventory actual routes/guards and compare current auth/catalog/profile DTOs.
- [x] Publish source-derived fixtures and record public catalog behavior.
- [x] Build backend and run its existing unit tests (4 tests); perform read-only local API smoke checks.
- [ ] Resolve public-catalog versus authenticated-only product policy.
- [ ] Confirm staging HTTPS, test identities, deployment/migration process and ownership.
- [ ] Agree backend sync/idempotency/conflict conventions and provider responsibilities.

## Integration and acceptance

- [x] Backend access is established and unresolved decisions/blockers are recorded.
- [x] Android security gaps are addressed; production endpoint absence explicitly blocks release packaging.
- [x] Android/backend baseline results and offline architecture direction are documented.
- [ ] Contract/product decisions have assigned owners and accepted outcomes.
- [ ] Real authenticated/error/permission/Keystore integration coverage is recorded.

Apply the [shared delivery gates](../planning/delivery.md). Current source/test results do not imply all SRS acceptance criteria passed.

## Execution and review

| Item | Evidence |
| --- | --- |
| Android changes and verification | [Execution report](sprint-01-report.md) |
| Backend inventory, contracts and gaps | [Backend audit](../contracts/backend-audit.md) |
| Contract examples | [Synthetic fixtures](../contracts/auth-catalog-fixtures.json) |
| Offline architecture | [Persistence/sync design](../architecture/offline-sync.md) |
| Decisions and owners still needed | [API decisions](../architecture/api-decisions.md) |
| Production readiness | Blocked on deployed HTTPS and backend authorization/ownership follow-up |
| Next increment | Sprint 02 auth verification/profile; preserve the current username-based wire contract until changed explicitly |

No backend code was modified or deployed. No live user/account mutation, email, AI or notification generation was performed.
