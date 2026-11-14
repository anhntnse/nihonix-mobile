# F9 — AI Practice & Generation

Status: Planned delivery; source baseline reviewed 2026-11-14.
Requirements: FR-071–FR-079.
Primary sprint: [Sprint 07](../sprints/sprint-07.md).
Frontend owner: TBD. Backend owner: TBD. QA/reviewer: TBD.

## Baseline and outcome

Planned; AI services and exercise screens absent.

Deliver the behavior specified in [SRS](../SRS.md), Section 4.9. “Frontend” means the Android client in this repository. Backend tasks are proposed work until the backend repository and existing implementation have been audited.

## Dependencies

- [F3 — Learning Roadmap: Levels & Topics](f03-learning-roadmap.md)
- [F7 — Learning Progress](f07-learning-progress.md)
- [F8 — Personal Flashcards](f08-flashcards.md)
- [F12 — AI Usage & Quota Management](f12-ai-quota.md)
- Agreed API/error/ownership contract in [API decisions](../architecture/api-decisions.md).
- Existing source presence does not mean release or backend acceptance has passed.

## Frontend work

- [ ] Build fill-in-the-blank and multiple-choice generation, practice list/detail, answer submission and results.
- [ ] Show cancellable/loading/error/quota states; preserve in-progress answers as agreed.
- [ ] Display backend grading and create completion/activity events only after the agreed success condition.
- [ ] Integrate AI flashcard preview/save from F8 without silently persisting generated drafts.

## Backend work

- [ ] Define provider integration behind authenticated backend operations; keep provider credentials server-side.
- [ ] Validate generated question/card structure and persist owner-scoped practice sets.
- [ ] Specify answer grading, repeat submission/idempotency and result retention.
- [ ] Reserve/enforce quotas atomically with F12 and document failure/refund behavior.

## API and data contract

Proposed /practice generation/list/detail/submit contracts plus AI flashcard generation contract; exact schemas, sync scope and draft-save policy require agreement.

Data/persistence: Practice set/type/topic/status, question/options, answer/result, generation request ID and quota reservation. Define what can resume offline; generation requires network.

Before implementation, record exact methods, fields, nullability, errors, authentication, ownership, pagination/versioning and representative fixtures in the API decision log. Preserve existing client contracts or document an explicit migration.

## Verification and acceptance

- [ ] Generate both exercise types; submit and show backend results.
- [ ] Handle malformed provider output, timeout, cancellation and repeated submission.
- [ ] Quota cannot be bypassed by concurrent requests; successful completion emits progress once.

Acceptance mapping: AC-010, AC-011; activity portion of AC-012 completes with F10. These are targets, not passing test results. Apply the shared [delivery gates](../planning/delivery.md).

## Delivery handoff

- [ ] Backend contract audited and agreed; existing endpoints reused where suitable.
- [ ] Frontend and backend changes reviewed with migration/configuration notes.
- [ ] Integration verified against a shared environment; failures and remaining work recorded.
- [ ] SRS source-review status, API decisions and sprint review updated with evidence.
