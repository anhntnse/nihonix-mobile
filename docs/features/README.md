# Feature delivery plans

Each plan contains frontend and backend work, contract/data decisions, dependencies, verification and a delivery handoff. Read the [SRS](../SRS.md) for the full requirement text and [sprint roadmap](../sprints/README.md) for scheduling.

| Feature | Requirements | Current client baseline | Primary sprint |
| --- | --- | --- | --- |
| [F1 — Authentication & Account Management](f01-authentication.md) | FR-001–FR-011 | Account flows implemented and locally tested; provider rollout and FCM cleanup pending. | [02](../sprints/sprint-02.md) |
| [F2 — User Profile & Settings](f02-profile-settings.md) | FR-012–FR-020 | Profile/settings/avatar/deletion implemented and locally tested; real-provider rollout pending. | [02](../sprints/sprint-02.md) |
| [F3 — Learning Roadmap: Levels & Topics](f03-learning-roadmap.md) | FR-021–FR-027 | Browsing/counts/bundles and saved filters implemented; persistent public cache delivered locally in Sprint 05. | [03](../sprints/sprint-03.md) |
| [F4 — Vocabulary Learning](f04-vocabulary.md) | FR-028–FR-036 | Online lists/details/search/filters/pages locally verified in Sprint 04; offline/progress delivered locally in Sprint 05. | [04](../sprints/sprint-04.md) |
| [F5 — Grammar Learning](f05-grammar.md) | FR-037–FR-044 | Online lists/details/search/filters/pages locally verified in Sprint 04; offline/progress delivered locally in Sprint 05. | [04](../sprints/sprint-04.md) |
| [F6 — Offline Content & Synchronization](f06-offline-sync.md) | FR-045–FR-051 | Room/ETag cache, authoritative deletion inventory and WorkManager outbox locally tested. | [05](../sprints/sprint-05.md) |
| [F7 — Learning Progress](f07-learning-progress.md) | FR-052–FR-058 | Manual learned/unlearned, durable batches/deltas and topic/level summaries locally tested. | [05](../sprints/sprint-05.md) |
| [F8 — Personal Flashcards](f08-flashcards.md) | FR-059–FR-070 | Private CRUD/bulk/review, offline queue and manual conflict recovery implemented in Sprint06; AI preview/save implemented in Sprint07; locally verified. | [06](../sprints/sprint-06.md) |
| [F9 — AI Practice & Generation](f09-ai-practice.md) | FR-071–FR-079 | Implemented: private generation, library/filter/detail, durable submissions and server results; locally verified. | [07](../sprints/sprint-07.md) |
| [F10 — Learning Streak & Calendar](f10-streak-calendar.md) | FR-080–FR-087 | Planned; no duration/activity/calendar implementation. | [08](../sprints/sprint-08.md) |
| [F11 — Device Registration & Push Notifications](f11-notifications.md) | FR-088–FR-095 | Planned; no FCM dependency/service/device API. | [08](../sprints/sprint-08.md) |
| [F12 — AI Usage & Quota Management](f12-ai-quota.md) | FR-096–FR-100 | User-approved separate/shared buckets, atomic quota ledger and native reset/429 states implemented; locally verified. | [07](../sprints/sprint-07.md) |
| [F13 — Application Configuration & System](f13-system-configuration.md) | FR-101–FR-105 | Startup config, health, maintenance/version gates and fallback implemented and locally tested. | [03](../sprints/sprint-03.md) |

## Cross-sprint completion

- Sprint 01 supplies shared auth/security/environment and offline architecture foundations.
- F3/F4/F5 persistent caching completes with F6 in Sprint 05.
- F8 manual flashcards and sync land in Sprint 06; AI generation completes with F9/F12 in Sprint 07.
- F1 device cleanup, F2 reminder delivery and F9 activity integration complete with F10/F11 in Sprint 08.
- F12 quota enforcement must be ready before F9 generation is enabled.
- Sprint 09 verifies all requirements together; a primary sprint does not mean every cross-feature dependency is already complete.

Backend source and local API have now been [audited](../contracts/backend-audit.md); the table describes Android implementation, not backend readiness. See the [Sprint 03 report](../sprints/sprint-03-report.md) and earlier reports for verification and rollout limits. Preserve the SRS exclusions: no social login, payments, SM-2, rankings, admin web UI, translated app UI or dedicated listening/writing/Kanji modules in this release.
