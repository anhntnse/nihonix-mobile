# Sprint 05 — Offline catalog and learning progress

Status: Implemented and locally verified; shared-environment rollout pending.
Execution date: 2026-11-15. Original duration assumption: two weeks; no team capacity/estimates invented.
Scope: F6/F7 plus F3/F4/F5 persistent caching. Shared-environment rollout remains separate.

## Frontend

- [x] S5-FE01: Environment-specific Room catalog cache, ETag/304 restoration, offline notice and deletion invalidation.
  - [x] S5-FE02: Atomic progress/outbox edits, stable UUID retries, incremental checkpoints and account isolation.
  - [x] S5-FE03: Explicit Đã học/Bỏ đánh dấu actions; topic/JLPT summaries and pending/error/retry/discard controls.
  - [x] S5-FE04: Connectivity-constrained WorkManager work, backoff/periodic recovery and logout/deletion lifecycle.
  - [x] S5-QA01: Restart, migration, rollback, account switch and old-response concurrency tests.

## Backend

- [x] S5-BE01: Complete catalog inventory and resource ETag/304; repair successful legacy entity-sync response.
  - [x] S5-BE02: Progress migration plus authenticated batch/changes/summary.
  - [x] S5-BE03: Durable receipts, payload-reuse conflict detection, account serialization, timestamp/UUID LWW and partial rejections.
  - [x] S5-QA02: Isolated PostgreSQL fixtures for migration, JWT ownership, concurrent edits, duplicates, deltas, deletion and summaries.

## Acceptance and review

- [x] AC-005/006: Saved-content cache/304/change/deletion transport fixtures and durable cache recovery.
  - [x] AC-007: Database reopen, migration and actual force-stop with same-APK process restart retain progress/outbox.
  - [x] AC-008: Stable retries and database constraints prevent duplicate progress; per-account receipts replay exact outcomes.
  - [x] Fresh code review: fixed retry/account race and delayed cache-response race, each reproduced RED→GREEN.
  - [x] Outbox producer/payload extension point for later F8/F10; those producers remain unimplemented.
  - [ ] Shared backend migration/deployment, actual HTTPS endpoint and real multi-device/Doze/network-transition rollout acceptance.

## Evidence and next steps

[Execution report](sprint-05-report.md) · [API contract](../contracts/sprint-05-api.md) · [Implementation decisions/ledger](sprint-05-implementation.md).

Offline is limited to previously fetched pages/details. The complete inventory contains metadata, not every item body. Release acceptance is not implied by local automated checks.

After implementation review, the user authorized grouped Sprint05 commits and push to the current Android/backend branches. Deployment remains separate. Next feature increment is [Sprint 06](sprint-06.md), personal flashcards.
