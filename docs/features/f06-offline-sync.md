# F6 — Offline Content & Synchronization

Status: Implemented with local verification; shared-environment rollout pending.
Requirements: FR-045–FR-051. Primary sprint: [05](../sprints/sprint-05.md).

Room persists successful public level/topic/bundle and vocabulary/grammar projections per environment. Conditional requests restore saved payloads on 304; network/5xx fallback shows an offline notice. Only previously fetched URLs/pages/details are available offline.

A full authoritative six-collection manifest identifies deleted IDs and invalidates affected projections. Request generations prevent late 200/304 responses from overwriting newer cache or resurrecting removed projections. Background work validates up to 30 oldest cached projections per run.

The outbox commits local progress and a UUID operation atomically. WorkManager requires network, uses backoff/periodic recovery and scopes protected work by account/environment. Logout cancels jobs/hides data while retaining that account's queue. Permanent failures remain visible for retry/discard; confirmed account deletion purges local account state. Tokens stay outside Room and database backup/transfer is disabled.

- [x] Persistent catalog cache, ETags and 304 restoration.
- [x] Durable account-scoped outbox, checkpoints and migration 1→2.
- [x] Retry, logout/account boundaries and explicit error handling.
- [x] Local network/cache, concurrent-response and Room persistence tests.
- [ ] Shared backend migration and end-to-end deployment acceptance.
- [ ] Flashcard/activity producers: future F8/F10 work.

[Exact contract](../contracts/sprint-05-api.md) · [Verification and limits](../sprints/sprint-05-report.md) · [Architecture](../architecture/offline-sync.md).
