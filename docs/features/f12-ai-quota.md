# F12 — AI Usage & Quota Management

Status: Implemented and locally verified; release packaging passed.
Requirements: FR-096–FR-100. [Sprint07](../sprints/sprint-07.md) · [Contract](../contracts/sprint-07-api.md).

## Accepted policy

User confirmed2026-11-15: flashcards2/day; fill_blank and multiple_choice share3/day; maximum15 items/request; reset00:00 Asia/Ho_Chi_Minh. Only validated successful results consume usage. Same request UUID/payload replays without another charge. Server configuration may override daily defaults; native displays server metadata.

## Delivered frontend

- [x] Remaining allowance, reserved usage and server reset time.
- [x] Distinct unavailable/stale/zero states; refresh after generation.
- [x] Structured429 handling retains form and request context.
- [x] Server enforcement remains authoritative.

## Delivered backend

- [x] Authenticated quota/request-status/cancel endpoints.
- [x] User-row serialization, canonical payload hashes and durable result receipts.
- [x] Reservation leases, cancellation/refund and original-day accounting across midnight.
- [x] Maximum2 unfinished provider attempts/account, including cancelled work until its worker exits or lease expires.
- [x] Safe request/state/code logs without credentials, prompts or provider output.

Local tests exercise concurrent last-slot reservations, duplicate retries, invalid output, rollback, cancellation, expiry and cross-midnight completion. Actual-provider/shared rollout remains open; configured metadata is key presence rather than a billable health probe. Review identified cancelled failed-worker slot release; its regression now passes.

Receipt retention lasts for account lifetime. Compaction and scale acceptance require a later contract. Activity/reminder timezone policy is separate from this fixed Vietnam quota boundary.
