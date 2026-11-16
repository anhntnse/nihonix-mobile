# Offline persistence and synchronization

D06/D07/D08: implemented locally in Sprint 05, 2026-11-15. The user confirmed manual learned/unlearned actions. [Wire contract](../contracts/sprint-05-api.md) and [verification](../sprints/sprint-05-report.md) are authoritative.

**D02 override — 2026-11-16:** User requires login for every catalog read per SRS/BR-001. Levels/topics/bundles, vocabulary/grammar, /catalog and entity-sync (including POST check) require Bearer JWT. Auth/config/liveness remain public; authoring requires Admin. Android removes catalog No-Authentication, gates cache on current local login and pins background refresh to account/session. Cached shared content remains available offline within that login; 401/403 never falls back. Conditional GET runs after JWT and returns private/no-cache, Vary Authorization. This supersedes earlier public-read statements below. [Decision/evidence](../planning/remaining-work/evidence/catalog-authentication.md).

## Android

Room v5 (Sprint07) retains the v2 public catalog JSON projections by exact URL/query, ETag and validation time. The database filename scopes the backend URL with SHA-256. Private progress uses composite account/item keys; outbox and checkpoints are account-scoped. Tokens/passwords never enter this database. Backup/device-transfer exclusions cover databases.

Version 1→2 adds an outbox account/state index without losing queued edits; both schema exports are retained. There is no destructive migration fallback.

OkHttp conditionally validates public reads. 304 preserves payload; no saved payload retries without the condition. Network/5xx can return saved content with an offline notice, while 404/410 invalidate affected projections. A complete authoritative inventory prunes deleted references. Atomic request sequence/generation checks prevent delayed responses from replacing newer payloads or restoring invalidations.

Previously visited pages/details form the offline boundary. The manifest contains inventory rather than every body; WorkManager validates at most 30 saved projections per run. Full-catalog download, unlimited-scale manifests and future snapshot pagination are outside this increment.

## User edits and conflicts

Local edit plus UUID outbox entry is one Room transaction. Unsent edits for an item coalesce; sent IDs remain immutable for retries. Server receipts return identical outcomes on replay and reject changed content under a reused ID. A user-row lock orders each account's commit/cursor sequence.

LWW compares client timestamp then lowercase UUID. Clock over five minutes ahead is a durable per-item rejection. Client deltas/checkpoints commit together; newer pending edits suppress older merges. Explicit retry generates a new ID only if the captured account and expected blocked mutation remain current.

## Lifecycle and work

WorkManager jobs require connected network, use exponential backoff and are uniquely scoped to environment/account. Immediate work and a 15-minute periodic recovery are scheduled on startup/sign-in; edits schedule additional work. Background execution timing remains controlled by Android.

Logout clears authentication/profile, cancels protected jobs and hides progress. Account-specific pending data stays for relogin. Existing session validation and an expected-account header reject requests racing an account switch. Permanent errors are retained for explicit retry/discard. Discard removes queued optimistic rows and restarts server replay; acknowledged local rows remain. Successful server account deletion purges that account's records, queue and checkpoint.

Sprint06 implements the flashcard producer with CAS versions, immutable dependent mutations and local-preserving server/copy conflict resolution. Progress retains timestamp/UUID LWW and coalescing; these policies are producer-specific. Room2→3 adds private folder/set/card/canonical/draft tables;3→4 adds bounded JSON chunks for large aggregates. Retry metadata uses UPDATE so rowid/dependency order stays stable. Activity remains a future producer. See [Sprint06 contract](../contracts/sprint-06-api.md). Receipt retention currently lasts for account lifetime; any future compaction requires an explicit full-resync contract.

## Sprint07 practice producer

Room4→5 adds account-keyed practice snapshots, answer drafts and stable generation requests. Answers and immutable submission UUID/payload commit atomically; practice is an independent WorkManager producer. Lost replies replay the same submission. New generation is explicit online work and never automatically retried as a paid background action. Check/retry/cancel preserves its request context. Completed results do not automatically mark catalog items learned.

Native AI previews preserve stable generated set/card IDs and require explicit save. Account changes reject stale actions/late merges; deletion purges practice cache and producer alongside existing data. Legacy completed practice is read-only, using stored exercise answers/explanations when no new receipt exists. [Contract](../contracts/sprint-07-api.md).

## Verification and rollout

Tests cover 304/updated/deleted cache, old-response races, durable outbox, migration, interrupted local transaction, account switch and retry ownership, PostgreSQL concurrent conflict/idempotent replay/delta/summary behavior. See the report for exact commands and process-death evidence.

Shared migration, deployment HTTPS and real multi-device/background-device acceptance remain rollout gates. No shared-schema migration or deployment is authorized by these documents.
