# F1 — Authentication & Account Management

Status: Implemented and locally tested (Sprint 02, device cleanup Sprint 08, re-verified Sprint 09); shared-environment/provider acceptance pending. Updated 2026-11-16.

Legend: [x] done with local evidence; [~] done locally, part still needs a shared environment or real provider; [ ] not done.

Current delivery evidence: [Sprint 02 report](../sprints/sprint-02-report.md) and [account API](../contracts/sprint-02-api.md). The baseline/checklists below preserve the original plan; implemented Android/backend account work is recorded in that report. Real SMTP/Cloudinary acceptance, migration rollout and notification device/delivery work remain open.
Requirements: FR-001–FR-011.
Primary sprint: [Sprint 02](../sprints/sprint-02.md).
Frontend owner: TBD. Backend owner: TBD. QA/reviewer: TBD.

## Baseline and outcome

Partial client implementation; email verification missing; backend unverified.

Deliver the behavior specified in [SRS](../SRS.md), Section 4.1. “Frontend” means the Android client in this repository. Backend tasks are proposed work until the backend repository and existing implementation have been audited.

## Dependencies

- Shared environment, security and contract foundation from [Sprint 01](../sprints/sprint-01.md).
- Agreed API/error/ownership contract in [API decisions](../architecture/api-decisions.md).
- Existing source presence does not mean release or backend acceptance has passed.

## Frontend work

- [x] Retain login, registration and recovery ViewModels; reconcile username login with the agreed contract. (Username login kept; [Sprint 02](../sprints/sprint-02-report.md).)
- [x] Add registration email verification, resend/cooldown and expired-code recovery screens. (Verified on emulator in Sprint 09.)
- [x] Harden token storage and session-expiration navigation; clear all user-owned local state on logout. (Sprint 01 storage; Sprint 09 emulator run: revoked session returned the app to sign-in; Sprint 08 account-scoped cleanup.)
- [x] Support backend validation errors and align registration/reset password rules. (8-character/72-byte policy for both.)

## Backend work

- [x] Audit existing register/login/recovery/refresh/logout handlers and publish request/response examples. ([Backend audit](../contracts/backend-audit.md), [account API](../contracts/sprint-02-api.md).)
- [x] Implement or verify email verification and rejection of unverified accounts; rate-limit code and credential attempts. (`test:sprint02`.)
- [x] Verify refresh-token rotation, replay rejection, expiry and logout revocation. (`test:sprint02`, `test:sprint09`.)
- [x] Define deletion/session invalidation behavior and a device-unregistration contract with F11. ([Sprint 08 contract](../contracts/sprint-08-api.md): logout revokes the session and cascades the device binding.)

## API and data contract

Existing client: POST auth/register, auth/login, auth/refresh, auth/logout, auth/forgot-password, auth/verify-reset-code, auth/reset-password, auth/resend-code. Email verification route/schema requires confirmation.

Data/persistence: Account, verified-email state, hashed credentials, verification/reset challenge, refresh session. Tokens stay in secure Android storage; never persist passwords.

Before implementation, record exact methods, fields, nullability, errors, authentication, ownership, pagination/versioning and representative fixtures in the API decision log. Preserve existing client contracts or document an explicit migration.

## Verification and acceptance

- [~] Register → verify → login against the real backend; reject unverified login. (Done against a local backend in the Sprint 09 emulator journey; shared environment pending.)
- [~] Test invalid/expired/reused codes, throttling, token expiry, concurrent refresh, replay and offline logout. (Fixtures cover all but real expiry timing on a shared server; offline logout cannot revoke the server session until connectivity or expiry, a documented limit.)
- [~] Verify credentials and tokens are absent from release logs and insecure storage. (Static review in Sprint 09: no logging, encrypted token storage, backup exclusions. A release build has not been run.)

Acceptance mapping: AC-001, AC-002, AC-016; FCM cleanup in AC-015 is finalized with F11. These are targets, not passing test results. Apply the shared [delivery gates](../planning/delivery.md).

## Delivery handoff

- [x] Backend contract audited and agreed; existing endpoints reused where suitable.
- [x] Frontend and backend changes reviewed with migration/configuration notes. (Migration `1794614400000-SprintTwoAccounts`; [Sprint 02 report](../sprints/sprint-02-report.md).)
- [ ] Integration verified against a shared environment; failures and remaining work recorded. (No shared environment yet; local results recorded in [Sprint 09 acceptance](../sprints/sprint-09-acceptance.md).)
- [x] SRS source-review status, API decisions and sprint review updated with evidence. (2026-11-16.)
