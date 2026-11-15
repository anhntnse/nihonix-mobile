# F8 — Personal Flashcards

Status: Sprint06 manual CRUD/bulk/review/sync implemented and locally verified. FR-069 AI remains Sprint07.
Requirements: FR-059–FR-070. [Sprint06](../sprints/sprint-06.md) · [Contract](../contracts/sprint-06-api.md) · [Evidence](../sprints/sprint-06-report.md).

## Delivered behavior

Topics & Levels → Flashcards opens account-private folders, sets and cards. Users create/edit folders, bulk-edit up to200 text cards per set, select/delete cards with confirmation, and delete sets/folders. Review supports flip, previous/next and restart. Drafts persist in Room rather than large saved-state bundles; review saves stable card IDs, position and side.

Edits and immutable UUID outbox mutations commit together. WorkManager synchronizes with independent progress/flashcard producers. Server versions, receipts and deleted IDs reconcile offline edits and cascades. The user selected preserving local content on conflict: choose server copy or save a new private copy.

## Frontend

- [x] Folder/set/card CRUD, bulk actions, confirmation and review screens.
- [x] Account-scoped Room v4 migrations and bounded JSON chunks, drafts, immutable queue and producer-specific synchronization.
- [x] Preserve local conflict edits with server/copy resolution and account-pinned actions.
- [x] Generated-card preview and explicit-save boundary.
- [ ] Connect AI/quota generation in Sprint07 (FR-069).

## Backend

- [x] Reuse audited folder/set/bulk routes through owner-aware adapters.
- [x] Atomic complete-set validation and transactional cascade semantics.
- [x] Durable UUID receipts, CAS versions, dependency ordering, scoped deltas and deleted IDs.
- [x] Cross-user parent/card protection, including concurrent fresh UUID collisions.
- [x] Preserve and hide legacy unowned rows; no guessed ownership migration.
- [ ] Shared migration/deployment and real multi-device acceptance.
- [ ] AI generation/quota acceptance with F9/F12.

## Verification boundary

Local automated evidence covers private CRUD, invalid bulk rollback, replay/concurrent edits, conflict recovery, migration, account changes, editor/review restoration and process restart. See the execution report for final commands/results. AC-009 has local evidence; shared-environment acceptance remains pending. AI portions of AC-005/AC-010 remain future work. No SM-2 or inferred activity tracking is introduced.

Dependencies: [F1](f01-authentication.md), [F6](f06-offline-sync.md), [API decisions](../architecture/api-decisions.md), and the shared [delivery gates](../planning/delivery.md).
