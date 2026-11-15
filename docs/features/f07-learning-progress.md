# F7 — Learning Progress

Status: Implemented with local verification; shared-environment rollout pending.
Requirements: FR-052–FR-058. Primary sprint: [05](../sprints/sprint-05.md).

The user confirmed explicit **Đã học/Bỏ đánh dấu** actions on vocabulary and grammar detail screens. Opening a page does not complete an item. Local edits update visible state and commit with an account-scoped outbox operation before scheduling work.

Backend POST progress/batch provides durable mutation receipts, payload-reuse conflict detection, account locking and deterministic timestamp/UUID Last-Write-Wins. GET progress/changes pages receipt deltas; merges/checkpoints commit atomically without overwriting newer pending edits. GET progress/summary counts current catalog items; Android shows optimistic offline totals by topic/JLPT level and pending/blocked counts.

- [x] Completion policy and vocabulary/grammar item/status contract.
- [x] Local persistence plus stable UUID batches/delta reconciliation.
- [x] Topic/level summaries and retry/discard controls.
- [x] Isolated PostgreSQL concurrency/idempotency/ownership/deletion fixtures.
- [x] Room migration, interrupted transaction, restart and account-switch tests.
- [ ] Shared migration/deployment and real multi-device rollout acceptance.

Retrying a blocked edit checks the captured account and exact current mutation within a transaction. A new edit cannot be overwritten by an outdated retry. Logout preserves pending data for the same account; deletion purges it.

[Exact contract](../contracts/sprint-05-api.md) · [Verification and limits](../sprints/sprint-05-report.md).
