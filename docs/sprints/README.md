# Sprint roadmap

Status: Sprints 01–03 implemented and locally tested; shared-environment acceptance and remaining decisions open. Sprints 04–09 proposed. Updated: 2026-11-14.

Latest evidence: [Sprint 03 execution report](sprint-03-report.md). Sprint 02 provider/migration rollout and Sprint 03 shared-environment acceptance remain pending.

Assume two-week sprints with Android frontend and backend work proceeding together. Team size, velocity, availability, estimates and calendar dates are unknown, so this is a dependency-based sequence rather than a committed 18-week schedule. The [backend audit](../contracts/backend-audit.md) establishes the current source/local API baseline; its findings must guide estimation and remaining contract decisions.

| Sprint | Goal | Scope |
| --- | --- | --- |
| [01](sprint-01.md) | Foundation, contracts and release configuration | Cross-cutting foundations for F1, F6 and F13 |
| [02](sprint-02.md) | Authentication and profile/settings | F1 and F2; JLPT level behavior coordinated with F3 |
| [03](sprint-03.md) | Roadmap and application configuration | F3 and F13; content caching delivered in Sprint 05 |
| [04](sprint-04.md) | Vocabulary and grammar | F4 and F5; offline acceptance follows in Sprint 05 |
| [05](sprint-05.md) | Offline catalog and learning progress | F6 and F7; complete F3/F4/F5 caching |
| [06](sprint-06.md) | Personal flashcards and synchronization | F8 manual CRUD/bulk/review/sync; AI generation follows Sprint 07 |
| [07](sprint-07.md) | AI quota, practice and generated flashcards | F12 first, then F9 and AI portion of F8 |
| [08](sprint-08.md) | Streak, calendar and notifications | F10 and F11; complete F2 reminders, F1 device cleanup and F9 activity integration |
| [09](sprint-09.md) | Integration, acceptance and release readiness | F1–F13, all AC-001–AC-017 and non-functional requirements |

## Dependency sequence

Foundation/contracts → auth/settings → roadmap/config → vocabulary/grammar → offline/progress → flashcards → quota/AI → activity/notifications → release acceptance.

Offline schema and API conventions are decided in Sprint 01 even though persistent learning behavior is delivered in Sprint 05. This reduces rework in content repositories. Features may be prepared earlier using agreed fixtures, but integration is complete only with backend acceptance.

Within Sprint 07, deliver/test quota enforcement before enabling generation. Within Sprint 08, establish activity aggregation and daily-goal status before reminder scheduling.

## Planning each sprint

1. Verify prerequisite contracts, environment and representative data.
2. Select concrete frontend/backend/QA tasks from [feature plans](../features/README.md).
3. Assign owners, task IDs, estimates and realistic capacity; split oversized scope.
4. Define a shared demo and acceptance cases before implementation.
5. Record integration test evidence, blockers and carryover in the sprint file.
6. Update feature status and SRS source-review notes after review.

Do not mark a whole feature complete while its cross-sprint acceptance remains open. Use the [delivery gates](../planning/delivery.md) for all increments.
