# F10 — Learning Streak & Calendar

Status: Planned delivery; source baseline reviewed 2026-11-14.
Requirements: FR-080–FR-087.
Primary sprint: [Sprint 08](../sprints/sprint-08.md).
Frontend owner: TBD. Backend owner: TBD. QA/reviewer: TBD.

## Baseline and outcome

Planned; no duration/activity/calendar implementation.

Deliver the behavior specified in [SRS](../SRS.md), Section 4.10. “Frontend” means the Android client in this repository. Backend tasks are proposed work until the backend repository and existing implementation have been audited.

## Dependencies

- [F2 — User Profile & Settings](f02-profile-settings.md)
- [F6 — Offline Content & Synchronization](f06-offline-sync.md)
- [F7 — Learning Progress](f07-learning-progress.md)
- Agreed API/error/ownership contract in [API decisions](../architecture/api-decisions.md).
- Existing source presence does not mean release or backend acceptance has passed.

## Frontend work

- [ ] Record duration and occurrence timestamps for agreed learning events, including F9 completion.
- [ ] Queue offline activity with stable IDs and show pending/synced states.
- [ ] Build daily minutes, current/longest streak and monthly calendar views.
- [ ] Use server summaries and configured timezone rather than device-only streak calculations.

## Backend work

- [ ] Provide idempotent activity ingestion and streak/calendar summaries.
- [ ] Define qualifying activity, duration rounding, day boundary and timezone-change rules.
- [ ] Handle late/offline submissions without double counting.
- [ ] Expose daily-goal completion consumed by F11 reminder scheduling.

## API and data contract

Proposed /learning-streaks activity/streak/calendar routes; confirm date range, timezone and aggregation semantics.

Data/persistence: Activity ID, user, item/type, duration, occurred_at and timezone context; server-derived daily totals and streak.

Before implementation, record exact methods, fields, nullability, errors, authentication, ownership, pagination/versioning and representative fixtures in the API decision log. Preserve existing client contracts or document an explicit migration.

## Verification and acceptance

- [ ] Offline activities sync once; late/duplicate submissions remain correct.
- [ ] Test midnight, timezone changes, daylight-saving transitions and month boundaries.
- [ ] Practice/study events update calendar and daily goal consistently.

Acceptance mapping: AC-012, AC-013. These are targets, not passing test results. Apply the shared [delivery gates](../planning/delivery.md).

## Delivery handoff

- [ ] Backend contract audited and agreed; existing endpoints reused where suitable.
- [ ] Frontend and backend changes reviewed with migration/configuration notes.
- [ ] Integration verified against a shared environment; failures and remaining work recorded.
- [ ] SRS source-review status, API decisions and sprint review updated with evidence.
