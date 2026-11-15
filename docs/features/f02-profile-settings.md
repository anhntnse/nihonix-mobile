# F2 — User Profile & Settings

Status: Implemented and locally tested (Sprint 02, reminder settings Sprint 08, re-verified Sprint 09); shared-environment/provider acceptance pending. Updated 2026-11-16.

Legend: [x] done with local evidence; [~] done locally, part still needs a shared environment or real provider; [ ] not done.

Current delivery evidence: [Sprint 02 report](../sprints/sprint-02-report.md) and [account API](../contracts/sprint-02-api.md). The baseline/checklists below preserve the original plan; implemented Android/backend account work is recorded in that report. Real SMTP/Cloudinary acceptance, migration rollout and notification device/delivery work remain open.
Requirements: FR-012–FR-020.
Primary sprint: [Sprint 02](../sprints/sprint-02.md).
Frontend owner: TBD. Backend owner: TBD. QA/reviewer: TBD.

## Baseline and outcome

Profile read and JLPT level update exist; editing/settings/deletion planned.

Deliver the behavior specified in [SRS](../SRS.md), Section 4.2. “Frontend” means the Android client in this repository. Backend tasks are proposed work until the backend repository and existing implementation have been audited.

## Dependencies

- [F1 — Authentication & Account Management](f01-authentication.md)
- Agreed API/error/ownership contract in [API decisions](../architecture/api-decisions.md).
- Existing source presence does not mean release or backend acceptance has passed.

## Frontend work

- [x] Retain profile loading and connect edit-profile and avatar picker/upload states. ([Sprint 02](../sprints/sprint-02-report.md).)
- [x] Add daily goal, notification/reminder, time and timezone preferences. (Sprint 09 emulator run: saved settings matched the server.)
- [x] Provide account-deletion confirmation and clear session/cache on success.
- [x] Keep level updates consistent with F3 and show server validation/retry states.

## Backend work

- [x] Audit POST auth/me versus proposed users/me contract; choose canonical routes. ([Account API](../contracts/sprint-02-api.md).)
- [x] Provide owner-scoped profile update, avatar validation/storage and account deletion.
- [x] Persist validated learning settings and expose timezone-aware defaults.
- [~] Specify account deletion retention, media cleanup and session/device invalidation. (Session/device invalidation and data cascade are specified and tested, including the Sprint 09 deletion fixture; the retention and backup policy review is still open.)

## API and data contract

Current profile retrieval is POST auth/me. Confirm profile mutation, avatar, deletion and settings schemas; GET/PATCH users/me/level already have client methods.

Data/persistence: User profile, avatar reference, levelCode, push_enabled, reminder_enabled, reminder_time, timezone, daily_goal_minutes. User-owned data must be isolated by account.

Before implementation, record exact methods, fields, nullability, errors, authentication, ownership, pagination/versioning and representative fixtures in the API decision log. Preserve existing client contracts or document an explicit migration.

## Verification and acceptance

- [~] Profile read/edit/avatar persists across relaunch. (Profile and settings persistence checked locally, including sign-out/sign-in in Sprint 09; real avatar upload to the media provider not verified.)
- [x] Reject cross-user access and invalid avatar/timezone/goal values. (`test:sprint02`, `test:sprint09`: other users get 403, self-service PUT cannot change email, status or role.)
- [~] Deletion invalidates sessions and follows the documented retention policy. (Sessions invalidated and owned data removed in tests; retention policy review pending.)

Acceptance mapping: AC-014, AC-017; reminder delivery is completed with F11. These are targets, not passing test results. Apply the shared [delivery gates](../planning/delivery.md).

## Delivery handoff

- [x] Backend contract audited and agreed; existing endpoints reused where suitable.
- [x] Frontend and backend changes reviewed with migration/configuration notes. (Migration `1794614400000-SprintTwoAccounts`.)
- [ ] Integration verified against a shared environment; failures and remaining work recorded. (No shared environment yet; see [Sprint 09 acceptance](../sprints/sprint-09-acceptance.md).)
- [x] SRS source-review status, API decisions and sprint review updated with evidence. (2026-11-16.)
