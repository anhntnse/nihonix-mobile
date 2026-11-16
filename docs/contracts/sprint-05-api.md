# Sprint 05 — Offline catalog and progress contract

Implemented 2026-11-15 in the Android and sibling NestJS backend checkouts. Public reads and existing authentication contracts remain compatible. Local verification is recorded in the [report](../sprints/sprint-05-report.md); shared rollout is separate.

**D02 override — 2026-11-16:** User requires login for every catalog read per SRS/BR-001. Levels/topics/bundles, vocabulary/grammar, /catalog and entity-sync (including POST check) require Bearer JWT. Auth/config/liveness remain public; authoring requires Admin. Android removes catalog No-Authentication, gates cache on current local login and pins background refresh to account/session. Cached shared content remains available offline within that login; 401/403 never falls back. Conditional GET runs after JWT and returns private/no-cache, Vary Authorization. This supersedes earlier public-read statements below. [Decision/evidence](../planning/remaining-work/evidence/catalog-authentication.md).

## Catalog

- `GET /api/catalog/manifest`: public full authoritative inventory, schema_version=1, collections and SHA-256 version. Six collections: levels, topics, vocabulary-items, vocabulary-sets, grammar-sub-topics, grammar-patterns.
- Entries contain id, slug, level_code, topic_id, topic_slug, title and version; context fields may be null. Grammar item context can be resolved via topic_slug. A set's level is resolved through its topic.
- This is one complete snapshot, without pagination. Absence denotes deletion; never prune from a failed or incomplete snapshot. Changed inventory/version changes the ETag.
- Public GET /api/catalog/*, /api/levels/* and /api/topics/* set quoted SHA-256 ETag over the data projection and Cache-Control: private, no-cache. If-None-Match supports multiple/weak tags and returns bodyless 304 on a match.
- Existing successful entity-sync responses now actually send the standard JSON envelope; previous 304 behavior remains.
- Android persists public successful JSON payloads up to 8 MiB per URL/query with ETag and validation time. Authenticated reads are never cached. A 304 restores the exact payload; missing payload triggers an unconditional request.
- Network/5xx may show saved content with an offline notice. 404/410 removes affected projections. A complete manifest invalidates projections containing deleted IDs; request ordering prevents old responses from restoring invalidated content.
- Offline scope is previously fetched URLs/pages/details, not an automatic download of all item bodies. Background refresh validates at most 30 oldest saved projections per run. The full manifest remains suitable only for the current small catalog; paginate snapshots with a completeness token before scaling.

## Progress

All routes require existing access JWT/session validation. Account identity comes from JWT. Optional X-Nihonix-Account must equal the JWT user or return 409; Android pins it to prevent a session switch from sending a queued edit as a different user.

`POST /api/progress/batch` (200):

```json
{"mutations":[{"mutation_id":"91f74bd0-65e1-4f9c-b984-30fd5a84dbe3","item_type":"vocabulary","item_id":"hello","status":"learned","client_updated_at":"2026-11-15T00:00:00.000Z"}]}
```

- 1–50 mutations; UUID ID, vocabulary|grammar, existing item slug, learned|not_learned, ISO timestamp; extra body fields rejected.
- Standard envelope data.acknowledgements: each mutation_id, status accepted|stale|rejected; accepted/stale include canonical record, rejected includes message.
- Canonical record: user_id, item_type, item_id, status, mutation_id, client_updated_at, topic_id, level_code, version (decimal string).
- Atomically serialize each account's batches with a user-row lock. Receipt unique(account, mutation_id) persists the exact outcome; retrying the same normalized payload returns it. Changed payload under an existing ID returns 409 and rolls back the whole batch.
- LWW compares client_updated_at, then lowercase UUID lexicographically. Equal timestamp and ID is a retry. Server receipt version is the synchronization ordering, not a second conflict clock.
- Device clock over five minutes ahead and unknown/deleted items produce durable per-item rejections; valid mutations in the same batch can succeed. Retry a corrected edit under a new ID.
- Backdated edits are allowed and lose to newer existing progress. Client increments a per-item timestamp to order rapid taps.

`GET /api/progress/changes?after=0&limit=100`:

- after: 1–18 decimal digits, default 0; limit 1–100.
- data: events, cursor, has_more. Events carry receipt version and stored acknowledgement. Scope is JWT account; ascending receipt versions, gaps allowed.
- Replay includes stale/rejected receipts; only records affect local state. Client commits merges and cursor together. Newer pending edits take precedence until acknowledged.
- Receipts are retained for account lifetime; no expiry or compaction currently. A future retention change requires a full-resync/reset contract.

`GET /api/progress/summary`:

- data.topics: topic_id, level_code, title, learned, total; data.levels: level_code, learned, total.
- Count existing vocabulary/grammar items only; exclude deleted references. learned counts status learned; total counts all supported items.
- Android derives an optimistic offline summary from its current authoritative inventory and account-local progress; it converges after delta sync. Pending counts explicitly distinguish it from server acceptance.

## Storage and lifecycle

Room v2 adds an outbox account/state index through non-destructive migration 1→2. Export both schemas. Environment-specific database; account-composite progress/outbox keys and checkpoint. No passwords/tokens in Room; database excluded from cloud backup and device transfer.

Explicit Đã học/Bỏ đánh dấu was confirmed by the user; opening content never marks it. Local edit and stable UUID outbox entry commit in one transaction. Unsent edits on one item coalesce. WorkManager requires network, has exponential backoff and a bounded attempt count; periodic work provides later recovery. HTTP408/429/5xx and connection failures retry; permanent failures remain visible for retry/discard.

Logout hides data/cancels protected jobs but retains account-scoped pending edits for relogin. Acknowledgements remove only their exact mutation ID. Retry verifies the captured account and expected blocked mutation inside its transaction. Discard removes optimistic rows tied to queued edits, clears queue/cursor and replays canonical history later; acknowledged rows stay available. Successful server account deletion invalidates its active session before purging that account's local records/queue/checkpoint, so late sync cannot recreate deleted local data.

Backend migration: `1794700000000-OfflineProgress.ts` creates learning_progress and progress_receipts with user cascade deletion. Apply through the existing migration deployment process; no shared-environment migration is part of this implementation task.

Flashcards/activity have no producer yet. Outbox producer/payload columns are their extension point; do not claim F8/F10 synchronization is delivered.
