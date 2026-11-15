# Sprint 06 — Private flashcards contract

Source verified 2026-11-15 in the Android and sibling NestJS repositories. Base prefix /api. [Implementation decisions](../sprints/sprint-06-implementation.md) · [Evidence](../sprints/sprint-06-report.md).

## Authentication and compatibility

All folder/flashcard routes require the existing JWT. Owner is derived from the authenticated user, never from a body user_id. Sync accepts X-Nihonix-Account; mismatch with JWT returns409 before writes. Unknown/foreign entities are not disclosed. Legacy unowned folders remain in PostgreSQL but are hidden from every user; migration never guesses an owner.

Existing routes remain: GET/POST /flashcard-folders; GET/PUT/DELETE /flashcard-folders/:idOrSlug; POST /flashcards (bulk create); PUT /flashcards/bulk; GET /flashcards/sets?folderId=UUID; GET /flashcards/sets/:setId/cards; DELETE /flashcards/sets/:setId. CRUD adapters use the same owner-aware transactional writer. Legacy routes retain their legacy response projections and generate a fresh receipt per call; offline clients must use sync for stable retry IDs.

POST /flashcards/generate is an existing backend route, now checks folder ownership before a provider call. It is not wired into the Android Sprint06 UI; provider/quota/AI acceptance and FR-069 remain Sprint07.

## Immutable mutations

POST /flashcards/sync/batch returns200 with the standard success/data envelope:

```json
{"mutations":[{"mutation_id":"11111111-1111-4111-8111-111111111111","kind":"folder","id":"22222222-2222-4222-8222-222222222222","action":"upsert","base_version":"0","name":"Japanese","description":"","order":1}]}
```

Common fields: UUID mutation_id/id; kind folder|set; action upsert|delete; base_version decimal string (0 for a new entity); optional depends_on UUID. Upsert folder fields: name, optional description/order. Upsert set fields: folder_id, name, order and the complete cards array. A card has UUID id, front/back objects with nonblank text, optional type=text and meaning, and positive order. Empty sets are supported. Moving an existing set to another folder is rejected; save a copy instead.

Limits: names255 characters, folder description1000, face text/meaning4000 each, order1–1,000,000, max200 cards/set and1–20 mutations/request. Duplicate card IDs or a card belonging to a different parent reject the complete aggregate before destructive writes. API JSON parser permits8MiB only under /api/flashcards; other routes retain100KiB. Android caps one aggregate JSON at7MiB and sends one mutation/request.

Acknowledgements contain mutation_id, kind, id, version and status accepted|conflict|rejected. They may include message, record, current, parent and deleted_ids={folders:[],sets:[],cards:[]}. A live record includes kind,id,user_id,version,deleted=false and entity fields; a set includes folder_id and complete cards. Tombstones contain deleted=true and identity/version. Set changes also return the updated parent folder version.

A JWT account-row lock serializes account writes. Each mutation stores a durable receipt and canonical payload hash. Exact retries return the identical outcome; reusing an ID with different content returns409 and rolls back that batch. Validation failures may return400 before a receipt. Business conflicts/rejections are durable per-mutation outcomes; a card-ID collision won concurrently by another owner returns409 and rolls back the entire aggregate/batch. Conditional upserts enforce ownership even when initial existence checks race across accounts.

Versions compare-and-set against folder/set heads. Child writes bump parent version, so a stale folder delete conflicts instead of deleting newer child work. depends_on resolves the earlier accepted outcome for the same entity (or its updated folder parent); an unsuccessful dependency conflicts. Deleted heads prevent stale resurrection. UUIDs and receipt payloads are immutable after queuing.

## Delta and deletion

GET /flashcards/sync/changes?after=0&limit=100 returns data.events, cursor and has_more. Cursor/version are decimal strings; default after0, limit1–100. Events are receipt outcomes in account order, including conflicts/rejections. Pages also use an8MiB cumulative snapshot budget in PostgreSQL before materializing JSON in Node; one event is always included so a legal large aggregate can advance. has_more continues the remaining history. This prevents100 large snapshots from becoming one unbounded mobile response. Persist merge and checkpoint atomically. Cursor is not a timestamp and must not be converted to a floating-point number.

Folder deletion physically cascades sets/cards and emits every deleted ID, while folder/set tombstone heads persist. Set full replacement emits removed card IDs. Receipt snapshots and tombstones let another device reconcile without resurrecting deleted content. Receipts/heads live until account deletion; compaction is future work.

## Android conflicts and recovery

Room v4 stores local edits, canonical server copies, drafts, account-scoped checkpoints and outbox payloads. Each local aggregate edit and queue insertion is atomic. Progress and flashcard producers query/retry/discard independently. Retry updates preserve queue row order and submitted UUID/payload; edits made during a request append a dependent mutation.

Conflicts keep the local content. User selects **Use server copy** (explicit discard confirmation) or **Save local copy** (new folder/set/card IDs, preserving local text). Account switches cannot apply a delayed response or submit a stale editor under the new account. Logout hides and retains private pending edits; successful account deletion purges both producers.

Review flip/previous/next/restart is local and does not infer completion or record learning time. Generated-card preview stays an editable draft until explicit save; AI generation is not enabled.

## Migration and rollout

Backend migration1794786400000 adds nullable folder.user_id, cascading user FK, heads and receipts. Existing legacy rows are preserved and quarantined. Run migration before enabling the new client on a shared environment; validate any historical parent inconsistencies and explicitly map ownership only with evidence. Rollback down removes ownership/sync metadata and can reintroduce the legacy ownership gap; it is a controlled isolated-test operation, not a safe production downgrade.

Android migration2→3 creates private tables without losing v2 cache/progress/outbox; migration3→4 adds account-keyed JSON chunks. Large outbox/canonical/draft JSON is split into bounded32,000-character rows and expanded before network use, avoiding CursorWindow limits without changing the API. References/chunks commit and purge together; retry changes metadata with UPDATE to preserve row order. Schema exports1/2/3/4 remain; no destructive fallback. Local temporary-schema integration and emulator tests do not establish shared deployment, production HTTPS, physical-device Doze or real multi-device rollout acceptance.
