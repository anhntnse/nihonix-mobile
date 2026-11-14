# Offline persistence and synchronization design

Decision: D06 — Android architecture selected for future implementation.
Date: 2026-11-14.
Implementation target: Sprint 05 (F6/F7), extended by F8/F10.
Backend wire contract: Pending; do not infer endpoints from this design.

## Choices

Use Room as the persistent relational store for learning content and account-scoped pending mutations, and WorkManager for durable, connectivity-constrained synchronization. Retain current Compose/ViewModel/repository interfaces and manual dependency wiring. Do not add these dependencies or claim offline behavior until the corresponding feature implementation.

Room provides transactions, observable queries and schema migrations for related learning entities. WorkManager handles deferrable retries across process death. In-memory maps or ordinary preferences are not a durable outbox. A foreground-only retry loop is insufficient.

## Data boundaries

| Store | Key / fields | Rules |
| --- | --- | --- |
| Catalog cache | endpoint/environment, kind, stable server ID, locale; payload/schema version, etag, updated_at | Server owns content; never upload local catalog edits |
| Account data | account_id + stable local/entity ID; version, status, server ID | Every query/mutation must constrain account_id |
| Outbox | account_id + mutation_id; entity/type/operation, payload version, client_updated_at, attempts, state | mutation_id is a UUID reused for every retry; persist edit and outbox in one transaction |
| Sync checkpoints | endpoint/environment + account_id + collection; cursor/version | Advance only after response and local updates commit atomically |
| Tombstones | account_id + entity ID, deletion version/time, acknowledgement | Keep until acknowledged and until the agreed retention contract allows removal |

Public catalog cache may be shared between accounts only if the final access policy permits it. Authenticated/private content must include account scope. Switching backend environments must not reuse cache or cursors from another server.

Passwords never enter Room/outbox. Tokens remain in secure Android storage and are excluded from backup. Backup rules must be extended for account databases before persistence ships.

## Repository behavior

1. UI observes local state; a repository refreshes remote data without blocking cached reads.
2. Save user edits plus outbox entry in one transaction, then schedule unique work scoped to account/environment.
3. Worker confirms the active account/session and sends only that account's mutations.
4. Retry network/temporary server failures with bounded exponential backoff; reuse mutation IDs.
5. On permanent validation/conflict errors, retain the user edit, expose an actionable state and avoid infinite retries.
6. Merge successful responses and acknowledgements transactionally before advancing cursors.
7. Check account/session again before applying a response so a late request cannot update another user's active UI.

Do not schedule protected sync until authentication is usable. Expired/revoked sessions pause protected work and route to login. A future refinement must distinguish transient refresh failure from definitive token rejection; the current authenticator clears on refresh failure.

## Cache/version semantics

Send If-None-Match only with a matching saved payload. HTTP 304 preserves the payload and updates validation time; it is not an empty/error response. A 304 without local payload requires an unconditional fetch. Server deletion removes stale content and invalidates affected references according to the agreed contract.

The audited entity-sync routes currently cover only level/topic; vocabulary/grammar cache validation, manifest/cursors and successful-response behavior still require backend work. A displayed etag is not a synchronization implementation.

## Conflicts and lifecycle

Progress Last-Write-Wins is an SRS proposal until timestamp authority and deterministic tie-break rules are agreed. Never overwrite a newer local pending edit when applying an older response. Flashcard edit/delete conflicts and activity idempotency need explicit server contracts; local timestamps alone cannot establish ordering across devices.

On logout, clear tokens/profile and cancel protected sync; hide account-local data immediately. Preserve unacknowledged edits under their account scope for the same account to resume later, rather than silently discard them. Account deletion purges its local data/outbox after the agreed server deletion result. An explicit discard action and retention policy must be specified before shipping this flow. This future outbox policy supersedes generic “clear all local data” planning text; no outbox exists yet.

## Required verification

- Offline cold start after process death, restart, and database migrations.
- Local edit/outbox atomicity, duplicate/reordered acknowledgements and interrupted batches.
- ETag unchanged/changed/deleted cases and cursor recovery.
- Two-device update/delete conflicts and invalid client clocks.
- Logout/account switch while requests are in flight; no cross-account reads or uploads.
- Queue survival on logout/relogin, explicit discard and account deletion.
- Android background restrictions and connectivity loss/recovery.

No persistent content storage or worker was implemented in Sprint 01. This is the architecture boundary and acceptance plan for later delivery.
