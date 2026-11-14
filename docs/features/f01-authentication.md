# F1 — Authentication & Account Management

Status: Implemented and locally tested in Sprint 02; shared-environment/provider acceptance pending. Updated 2026-11-14.

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

- [ ] Retain login, registration and recovery ViewModels; reconcile username login with the agreed contract.
- [ ] Add registration email verification, resend/cooldown and expired-code recovery screens.
- [ ] Harden token storage and session-expiration navigation; clear all user-owned local state on logout.
- [ ] Support backend validation errors and align registration/reset password rules.

## Backend work

- [ ] Audit existing register/login/recovery/refresh/logout handlers and publish request/response examples.
- [ ] Implement or verify email verification and rejection of unverified accounts; rate-limit code and credential attempts.
- [ ] Verify refresh-token rotation, replay rejection, expiry and logout revocation.
- [ ] Define deletion/session invalidation behavior and a device-unregistration contract with F11.

## API and data contract

Existing client: POST auth/register, auth/login, auth/refresh, auth/logout, auth/forgot-password, auth/verify-reset-code, auth/reset-password, auth/resend-code. Email verification route/schema requires confirmation.

Data/persistence: Account, verified-email state, hashed credentials, verification/reset challenge, refresh session. Tokens stay in secure Android storage; never persist passwords.

Before implementation, record exact methods, fields, nullability, errors, authentication, ownership, pagination/versioning and representative fixtures in the API decision log. Preserve existing client contracts or document an explicit migration.

## Verification and acceptance

- [ ] Register → verify → login against the real backend; reject unverified login.
- [ ] Test invalid/expired/reused codes, throttling, token expiry, concurrent refresh, replay and offline logout.
- [ ] Verify credentials and tokens are absent from release logs and insecure storage.

Acceptance mapping: AC-001, AC-002, AC-016; FCM cleanup in AC-015 is finalized with F11. These are targets, not passing test results. Apply the shared [delivery gates](../planning/delivery.md).

## Delivery handoff

- [ ] Backend contract audited and agreed; existing endpoints reused where suitable.
- [ ] Frontend and backend changes reviewed with migration/configuration notes.
- [ ] Integration verified against a shared environment; failures and remaining work recorded.
- [ ] SRS source-review status, API decisions and sprint review updated with evidence.
