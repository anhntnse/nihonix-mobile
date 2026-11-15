# F9 — AI Practice & Generation

Status: Implemented and locally verified; release packaging passed.
Requirements: FR-071–FR-079. [Sprint07](../sprints/sprint-07.md) · [Contract](../contracts/sprint-07-api.md).

## Delivered frontend

- [x] Fill-in-the-blank and multiple-choice generation with topic/count selection.
- [x] Private history, topic/type/status filters, bounded server pages, detail and backend results.
- [x] Persist answers offline; immutable submission UUID/payload and locked drafts survive restart.
- [x] Explicit check/retry/cancel of saved generation requests; quota unknown/stale/429 states.
- [x] Generated cards require preview and explicit editor save; stable IDs prevent duplicate saves.

## Delivered backend

- [x] Existing Gemini integration with one bounded attempt, strict complete-output validation and safe error metadata.
- [x] Owner-scoped transactional generation; answers/explanations hidden before completion.
- [x] Exact-answer grading, submission receipts and one completion event per session in the same transaction.
- [x] Atomic quota reservation/consumption with F12; failed/cancelled/expired results do not consume successful allowance.

Practice completion does not mark vocabulary/grammar learned. Native submits zero duration rather than inventing study time. FR-078 has durable completion records; activity/streak/calendar aggregation remains Sprint08. Cached questions/drafts resume offline; generation requires network.

## Acceptance and handoff

AC-010/011 local deterministic coverage includes malformed output, concurrency, cancellation, quota exhaustion, grading/replay and rollback. Fresh review found legacy completed-session display and cancelled failed-worker lease issues; both were reproduced RED and fixed GREEN. Shared migration, actual configured provider acceptance and physical-device/cross-device rollout remain open. See the implementation ledger for current evidence. Existing architecture, provider credentials/model and SMTP configuration are retained.

Dependencies: [F3](f03-learning-roadmap.md), [F6](f06-offline-sync.md), [F8](f08-flashcards.md), [F12](f12-ai-quota.md).
