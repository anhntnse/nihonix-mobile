# Sprint 07 — AI quota, practice and generated flashcards

Status: Implemented and locally verified; shared rollout pending. Date2026-11-15.
Scope: F12, F9 and FR-069 AI portion of F8. Prerequisites: Sprint05/06 contracts.

## Delivered frontend

- [x] Server allowance/reset, unavailable/stale/429 states.
- [x] Both exercise types, topic/type/status history filters, detail and backend results.
- [x] Room v5 answer drafts, immutable submission producer and saved generation recovery.
- [x] Generated-card preview → explicit save with stable IDs and duplicate prevention.
- [x] Legacy completed practices remain read-only without creating new completion events.

## Delivered backend

- [x] Atomic quota reservations, success consumption, replay, refund, leases and cancellation.
- [x] Existing Gemini adapter with bounded calls and strict complete-output validation.
- [x] Owner-scoped practice, transactional grading/receipts and unique completion events.
- [x] Separate active-worker cap prevents cancellation abuse; failed cancelled workers release slots.

User-approved quota: flashcards2/day; fill_blank+multiple_choice share3/day; max15; reset00:00 Asia/Ho_Chi_Minh. Charge validated success only.

## Acceptance and handoff

- [x] Local deterministic AC-010/011 generation/quota/grading and preview-save evidence.
- [x] Fresh read-only review; important findings reproduced and fixed.
- [x] Final full regression, process restart and build/lint evidence consolidated in [report](sprint-07-report.md).
- [ ] Shared migration/deployment, configured-provider and physical/multi-device acceptance.
- [ ] Sprint08 activity/streak/calendar aggregation from the durable completion event.

[API contract](../contracts/sprint-07-api.md) · [Implementation ledger](sprint-07-implementation.md).
User subsequently authorized Sprint07 commits and push for Android/backend. Deployment remains separate.
