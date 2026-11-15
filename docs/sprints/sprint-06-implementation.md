# Sprint 06 — Personal flashcards implementation plan

> Execute task-by-task using superpowers:executing-plans, with regression tests and one final fresh review. User requested implementation of the existing Sprint06/F8 plan; continue the established existing-checkout/manual-DI workflow. No automatic commit/push/deployment is included.

Goal: FR059–068/070 and AC009: personal folders, sets, bulk cards, offline review and durable synchronization. FR069 AI remains Sprint07.
Spec: sprint-06.md, ../features/f08-flashcards.md, ../SRS.md §4.8; Sprint05 Room/WorkManager architecture.

## Decisions and interfaces

- Keep Kotlin/Compose/Material3/MVVM, Retrofit/Gson/Room/WorkManager and Nest/TypeORM/PostgreSQL. No new runtime dependency, no SM2 or learning-activity inference.
- The user explicitly selected preserving local edits and choosing conflict resolution. Use optimistic per-folder/set versions, not timestamp overwrites. Keep blocked local payloads; offer server copy or save a new private copy.
- Preserve legacy unowned rows without exposing/adopting them. Add nullable folder.user_id with user cascade; every API checks owner through the entire parent chain. Migration does not guess ownership or delete legacy data.
- Reuse existing folder and flashcards routes through owner-aware adapters. New POST /flashcards/sync/batch and GET /flashcards/sync/changes provide durable UUID receipts, versions and per-event deleted_ids.
- Mutations: mutation_id UUID, kind folder|set, id UUID, action upsert|delete, base_version decimal string, optional depends_on UUID; upsert fields name, description/order for folder, folder_id and complete cards[] for set. Card IDs are stable UUIDs; front/back contain nonblank text (max4000), type text and optional meaning. Names max255, descriptions max1000, max200 cards/set, max20 mutations/batch, delta pages max100.
- Bulk set replacement is atomic, including validation of duplicate/foreign IDs before any write. Folder deletion cascades all descendants and emits their deleted IDs. Tombstone heads prevent stale resurrection. Child changes also bump the parent's version to prevent stale folder deletion from losing newer child work.
- A dependency references the previous local entity mutation (or the last folder/child mutation for folder changes), so edits made while an earlier request is in flight stay immutable and can apply sequentially without rebasing a submitted payload.
- Per-account user-row locking orders receipts/cursors; receipt payload mismatch under reused ID fails409. Conflicts/rejections are durable acknowledgements; unrelated valid operations can be acknowledged separately. A single set's bulk mutation always succeeds or fails as a unit.
- Room v4 adds account-keyed folders/sets/cards, canonical server copies and flashcard checkpoint. Keep edit+outbox atomic; do not coalesce submitted mutations. Scope progress and flashcard producer queries/retry/discard separately. Account deletion purges both; logout retains hidden pending operations.
- Browser: folders → folder sets → set cards, create/edit dialogs/editor with bulk selection/deletion, explicit confirmation before cascades. Review: flip, previous/next, restart; save position/side and draft editing through recreation. No swipe-only controls. Reuse theme, scrolling forms, IME/insets and48dp targets.
- Generated-card preview/save boundary accepts validated drafts and requires explicit save; no AI provider request is wired in this sprint.

## Task 1 — Owner-safe transactional server

Files: modules/flashcards/sync/*, folder entity/services/controller, flashcards services/controller/DTOs, database migration1794786400000-PersonalFlashcards.ts, app/data-source modules.
Interfaces: FlashcardSyncService.batch(userId,mutations), changes(userId,after,limit); owner-aware folder/set reads and legacy mutations.
- [x] RED: pure mutation policy tests for foreign IDs, duplicates, invalid faces and dependency version resolution.
- [x] GREEN: policy, migration, receipts/heads, atomic writes/cascades and owner adapters.
- [x] Real PostgreSQL/HTTP fixture: users A/B, unowned legacy rows, malicious parents/cards, invalid bulk rollback, replay, concurrent CAS, delete/edit, cursor/deleted IDs and migration preservation.

## Task 2 — Durable Android producer

Files: core/offline/OfflineDatabase.kt and SyncWorker.kt; feature/flashcards/data/* and domain/*; progress repository/DI/account purge wiring.
Interfaces: FlashcardsRepository.observe():Flow<FlashcardsState>, saveFolder/saveSet/deleteFolder/deleteSet, sync(owner), useServer(kind,id), saveCopy(kind,id), purge(owner).
- [x] Model validation RED→GREEN; Retrofit wire contract tests verify dependency/full cards, receipt/deletion parsing, decimal cursor and owner pinning.
- [x] Room migration2→3→4 preserves progress/cache/outbox and adds bounded JSON storage; producer queries cannot consume another producer.
- [x] Atomic local edits, immutable dependency queue, acknowledgement merge and canonical conflict copies.
- [x] Repository tests: lost acknowledgement/retry, edit during request, conflict/copy/server resolution, deletion, account switch/logout and failed transaction.

## Task 3 — Browser/editor/review

Files: feature/flashcards/presentation/*, AppViewModelFactory, AuthNavGraph, TopicsScreen.
- [x] Build folder/set/card flows with error/empty/pending/conflict states and draft restoration.
- [x] Bulk edit/delete with confirmation and atomic save; generated-draft preview explicit-save boundary.
- [x] Review flip/previous/next/restart with stable IDs/position, surviving rotation and handling deleted cards.
- [x] Compose verification: CRUD, bulk validation/selection, review and restoration, small-width/large text/dark mode.

## Task 4 — Integrated delivery

- [x] Full Android unit/instrumented/lint/debug/release checks; backend build/Jest and isolated Sprint04/05/06 PostgreSQL.
- [x] Process-restart fixture for pending private edits/deletions; no shared-schema migration, SMTP or AI calls.
- [x] Fresh read-only review, reproduce/fix important findings, then update SRS/feature/sprint/API/report evidence.
- [x] Leave code reviewable and uncommitted until separately requested.

## Review focus

Ownership at every parent/child edge; stale cascade deletion; edit while acknowledgement is in flight; independent producer queues; account change during conflict resolution. Keep canonical server data separate from editable local copies.

## Execution ledger

- Initial audit: legacy routes require JWT but folder has no owner column; bulk update can target arbitrary card IDs and mutates before validating ownership. Existing backend data must be preserved and quarantined until explicit owner mapping.
- User confirmed preserve-local/manual-conflict resolution.
- UI guidance: focused Compose state/LazyColumn guidance used with the existing theme.
- Policy tests witnessed RED→GREEN (11); native model tests RED→GREEN (5). Real isolated PostgreSQL/HTTP fixture passes, including large Japanese bulk/413.
- Fresh read-only review found two important races: cross-owner fresh UUID upserts and INSERT OR REPLACE retry reordering. Both reproduced with failing regressions before fixes. Conditional owner/parent upserts and stable UPDATE metadata address them.
- Large200-card draft test reproduced SQLiteBlobTooBigException. Room3→4 adds bounded32,000-character payload chunks for outbox/canonical/draft JSON; large aggregate reopen/sync/server-restore and draft restoration now pass. Schema generation must precede test-assets packaging; explicit Gradle task dependency added.
- Full ordinary instrumentation passes with phase-specific fixtures excluded from that Gradle invocation; direct runner reports OK (47 tests), with45 ordinary tests and2 phase-only assumptions. Both Sprint05/06 seed→force-stop→verify fixtures pass on unchanged installed APKs with different process PIDs.
- Large200-card server delta regression first failed when two snapshots exceeded8MiB in one page. PostgreSQL now applies a byte budget before returning JSON, retaining cursor/has_more continuation; regression passes.
- Final checks pass:114 Android unit tests;45 ordinary instrumented tests; both same-APK process restart fixtures; lint/debug/release packaging; backend build/71 Jest tests; isolated PostgreSQL Sprint04/05/06. git diff --check passes in both repositories and all original requirement IDs remain. User subsequently authorized grouped commits and push; shared deployment and AI FR-069 remain open. See [report](sprint-06-report.md).
