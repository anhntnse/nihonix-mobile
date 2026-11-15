# Nihonix documentation

Updated: 2026-11-15.

## Start here

- [Email verification fix](troubleshooting/email-verification.md): missing-account errors, SMTP tracing and regression evidence (2026-11-15).

- [Sprint 06 execution report](sprints/sprint-06-report.md): private flashcards, bulk editing/review, conflict resolution and durable sync.
- [Sprint 05 execution report](sprints/sprint-05-report.md): offline catalog, durable progress, process recovery and rollout limits.
- [Sprint 04 execution report](sprints/sprint-04-report.md): vocabulary/grammar delivery, contracts and tests.
- [Sprint 03 execution report](sprints/sprint-03-report.md): roadmap/configuration delivery and rollout evidence.
- [Sprint 02 execution report](sprints/sprint-02-report.md): account/profile delivery, test evidence and rollout work.
- [Sprint 01 execution report](sprints/sprint-01-report.md): delivered foundation, checks and remaining work.
- [Software requirements specification](SRS.md): product requirements and source-reviewed implementation status.
- [Original Word-to-Markdown conversion](nihonix-original.md): preserved source reference.
- [Feature plans](features/README.md): all 13 features, with Android frontend and backend tasks.
- [Sprint roadmap](sprints/README.md): delivery order, implemented increments and individual sprint backlogs.
- [Delivery process](planning/delivery.md): status rules, handoff and acceptance gates.
- [API and architecture decisions](architecture/api-decisions.md): unresolved contracts and technical decisions.

## Layout

```text
docs/
  README.md
  SRS.md
  nihonix-original.md
  architecture/
    api-decisions.md
  features/
    README.md
    f01-authentication.md ... f13-system-configuration.md
  planning/
    delivery.md
  sprints/
    README.md
    sprint-01.md ... sprint-09.md
```

## Working conventions

The Android app is the frontend. The supplied backend has now been [audited at a pinned commit](contracts/backend-audit.md), built and smoke-checked locally. Backend plans combine existing capabilities with missing work; endpoint presence does not establish acceptance. Each feature file includes its own frontend/backend backlog, requirements, dependencies, API/data questions and acceptance checks.

The SRS defines product scope. Feature plans explain delivery. Sprint files select and sequence that work. Keep existing SRS paths stable so source links and references continue to work.

Use feature IDs F1–F13 and original FR/AC identifiers in tasks and PRs. Assign task IDs such as F1-FE-01, F1-BE-01 and F1-QA-01 during sprint planning; the current checklists are unestimated backlog. Update status and attach evidence after implementation. Planned checkboxes do not imply work has started.

Sprint 01 started on 2026-11-14; future sprint dates, team ownership and capacity remain TBD. The two-week assumption and nine-sprint sequence are provisional; Sprint 01 audits determine whether scope or sequence must change.
