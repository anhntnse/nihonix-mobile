# F12 — AI Usage & Quota Management

Status: Planned delivery; source baseline reviewed 2026-11-14.
Requirements: FR-096–FR-100.
Primary sprint: [Sprint 07](../sprints/sprint-07.md).
Frontend owner: TBD. Backend owner: TBD. QA/reviewer: TBD.

## Baseline and outcome

Planned; all numerical limits remain unverified proposals.

Deliver the behavior specified in [SRS](../SRS.md), Section 4.12. “Frontend” means the Android client in this repository. Backend tasks are proposed work until the backend repository and existing implementation have been audited.

## Dependencies

- [F1 — Authentication & Account Management](f01-authentication.md)
- Agreed API/error/ownership contract in [API decisions](../architecture/api-decisions.md).
- Existing source presence does not mean release or backend acceptance has passed.

## Frontend work

- [ ] Display remaining flashcard/practice allowance and server-provided reset time.
- [ ] Refresh quota after generation and handle 429 without losing draft/request context.
- [ ] Distinguish unavailable/stale quota from zero allowance; avoid claiming frontend enforcement.
- [ ] Use agreed limit metadata rather than duplicating authoritative constants.

## Backend work

- [ ] Provide quota read contract and atomically reserve/consume allowance per generation request.
- [ ] Decide shared/separate exercise quotas, reset timezone and maximum output counts.
- [ ] Define retry idempotency, timeout/cancellation charging and reservation/refund policy.
- [ ] Return structured 429/error/reset metadata and instrument usage.

## API and data contract

Proposed /ai health/quota operations. Confirm exact routes and response shape; original 2 requests/15 flashcards and 3 requests/15 fill-in questions are proposals.

Data/persistence: User, feature quota bucket, reset boundary, limit/used/reserved counts and generation request ledger.

Before implementation, record exact methods, fields, nullability, errors, authentication, ownership, pagination/versioning and representative fixtures in the API decision log. Preserve existing client contracts or document an explicit migration.

## Verification and acceptance

- [ ] Concurrent requests cannot exceed quota; retries charge once.
- [ ] UI/server agree on reset times and 429 responses.
- [ ] Generation failures and cancellations follow the documented charging policy.

Acceptance mapping: Quota portion of AC-010. These are targets, not passing test results. Apply the shared [delivery gates](../planning/delivery.md).

## Delivery handoff

- [ ] Backend contract audited and agreed; existing endpoints reused where suitable.
- [ ] Frontend and backend changes reviewed with migration/configuration notes.
- [ ] Integration verified against a shared environment; failures and remaining work recorded.
- [ ] SRS source-review status, API decisions and sprint review updated with evidence.
