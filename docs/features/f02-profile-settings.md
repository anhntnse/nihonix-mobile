# F2 — User Profile & Settings

Status: Implemented and locally tested in Sprint 02; shared-environment/provider acceptance pending. Updated 2026-11-14.

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

- [ ] Retain profile loading and connect edit-profile and avatar picker/upload states.
- [ ] Add daily goal, notification/reminder, time and timezone preferences.
- [ ] Provide account-deletion confirmation and clear session/cache on success.
- [ ] Keep level updates consistent with F3 and show server validation/retry states.

## Backend work

- [ ] Audit POST auth/me versus proposed users/me contract; choose canonical routes.
- [ ] Provide owner-scoped profile update, avatar validation/storage and account deletion.
- [ ] Persist validated learning settings and expose timezone-aware defaults.
- [ ] Specify account deletion retention, media cleanup and session/device invalidation.

## API and data contract

Current profile retrieval is POST auth/me. Confirm profile mutation, avatar, deletion and settings schemas; GET/PATCH users/me/level already have client methods.

Data/persistence: User profile, avatar reference, levelCode, push_enabled, reminder_enabled, reminder_time, timezone, daily_goal_minutes. User-owned data must be isolated by account.

Before implementation, record exact methods, fields, nullability, errors, authentication, ownership, pagination/versioning and representative fixtures in the API decision log. Preserve existing client contracts or document an explicit migration.

## Verification and acceptance

- [ ] Profile read/edit/avatar persists across relaunch.
- [ ] Reject cross-user access and invalid avatar/timezone/goal values.
- [ ] Deletion invalidates sessions and follows the documented retention policy.

Acceptance mapping: AC-014, AC-017; reminder delivery is completed with F11. These are targets, not passing test results. Apply the shared [delivery gates](../planning/delivery.md).

## Delivery handoff

- [ ] Backend contract audited and agreed; existing endpoints reused where suitable.
- [ ] Frontend and backend changes reviewed with migration/configuration notes.
- [ ] Integration verified against a shared environment; failures and remaining work recorded.
- [ ] SRS source-review status, API decisions and sprint review updated with evidence.
