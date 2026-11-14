# F11 — Device Registration & Push Notifications

Status: Planned delivery; source baseline reviewed 2026-11-14.
Requirements: FR-088–FR-095.
Primary sprint: [Sprint 08](../sprints/sprint-08.md).
Frontend owner: TBD. Backend owner: TBD. QA/reviewer: TBD.

## Baseline and outcome

Planned; no FCM dependency/service/device API.

Deliver the behavior specified in [SRS](../SRS.md), Section 4.11. “Frontend” means the Android client in this repository. Backend tasks are proposed work until the backend repository and existing implementation have been audited.

## Dependencies

- [F1 — Authentication & Account Management](f01-authentication.md)
- [F2 — User Profile & Settings](f02-profile-settings.md)
- [F10 — Learning Streak & Calendar](f10-streak-calendar.md)
- Agreed API/error/ownership contract in [API decisions](../architecture/api-decisions.md).
- Existing source presence does not mean release or backend acceptance has passed.

## Frontend work

- [ ] Integrate FCM and request notification permission at an appropriate user action.
- [ ] Register/rotate device tokens and reconcile account switching/logout.
- [ ] Handle foreground/background payloads and authenticated navigation safely.
- [ ] Expose reminder/push preferences linked to F2 and unregister devices during logout.

## Backend work

- [ ] Provide authenticated idempotent device registration/removal with token rotation and invalid-token cleanup.
- [ ] Implement reminder scheduler using user timezone/preferences and daily goal status.
- [ ] Deduplicate reminders and disable delivery after opt-out, logout or deletion.
- [ ] Define payload version, destination identifiers, delivery policy and operational monitoring.

## API and data contract

Proposed /devices register/update/delete contract; payload and deep-link destination allowlist must be agreed.

Data/persistence: User/device registration, FCM token, platform, last seen, notification preferences, reminder schedule and deduplication key.

Before implementation, record exact methods, fields, nullability, errors, authentication, ownership, pagination/versioning and representative fixtures in the API decision log. Preserve existing client contracts or document an explicit migration.

## Verification and acceptance

- [ ] Permission denied/revoked does not block learning; token refresh reconciles registration.
- [ ] Test logout/offline logout cleanup policy, account switch, invalid token and opted-out reminders.
- [ ] Reminder skips completed daily goals; cold/warm notification navigation respects session state.

Acceptance mapping: AC-015; completes device portion of FR-008/AC-014. These are targets, not passing test results. Apply the shared [delivery gates](../planning/delivery.md).

## Delivery handoff

- [ ] Backend contract audited and agreed; existing endpoints reused where suitable.
- [ ] Frontend and backend changes reviewed with migration/configuration notes.
- [ ] Integration verified against a shared environment; failures and remaining work recorded.
- [ ] SRS source-review status, API decisions and sprint review updated with evidence.
