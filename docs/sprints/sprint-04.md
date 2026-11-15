# Sprint 04 — Vocabulary and grammar

Updated: 2026-11-15.
Status: Android/backend implementation and local verification delivered; broader shared-environment acceptance and review remain pending.
Owners, estimates and calendar capacity remain unassigned.

Scope: F4 and F5. Persistent offline study and completion events follow in Sprint 05.

## Frontend

- [x] Vocabulary item/set lists and details.
- [x] Grammar sub-topic/pattern lists and details.
- [x] Topic/JLPT filters, debounced multilingual keyword search and explicit page loading.
- [x] Japanese readings, localized meanings/explanations/examples and conjugation rendering.
- [x] Learning navigation from topics and topic details; group-to-child navigation.
- [x] Saved filter/search/language state, cancellation of stale requests and loading/empty/error/retry states.
- [ ] Persistent offline reads and progress/completion events (Sprint 05).

## Backend

- [x] Audit existing routes; retain backward compatibility.
- [x] Add validated bounded /catalog collection/detail projections and stable response versions.
- [x] Test deterministic pagination, literal keyword search and parent filters with real PostgreSQL.
- [x] Keep Admin-only mutations and reject authoring level/reference conflicts before AI calls.
- [ ] Shared-environment device/demo acceptance and production rollout.

Acceptance: local evidence addresses vocabulary/grammar AC-004. AC-005/AC-006 offline acceptance remains pending F6. No shared seed/migration or provider invocation is required by this increment.

[Execution report](sprint-04-report.md) · [API contract](../contracts/sprint-04-api.md) · [Delivery gates](../planning/delivery.md)
