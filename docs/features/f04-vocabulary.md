# F4 — Vocabulary Learning

Status: Planned delivery; source baseline reviewed 2026-11-14.
Requirements: FR-028–FR-036.
Primary sprint: [Sprint 04](../sprints/sprint-04.md).
Frontend owner: TBD. Backend owner: TBD. QA/reviewer: TBD.

## Baseline and outcome

Planned; no current feature implementation.

Deliver the behavior specified in [SRS](../SRS.md), Section 4.4. “Frontend” means the Android client in this repository. Backend tasks are proposed work until the backend repository and existing implementation have been audited.

## Dependencies

- [F3 — Learning Roadmap: Levels & Topics](f03-learning-roadmap.md)
- Agreed API/error/ownership contract in [API decisions](../architecture/api-decisions.md).
- Existing source presence does not mean release or backend acceptance has passed.

## Frontend work

- [ ] Implement vocabulary list, detail and set list/detail using existing Compose/MVVM patterns.
- [ ] Add topic filter, keyword search, pagination and loading/empty/retry states.
- [ ] Map agreed Japanese reading, meaning and example fields without inventing required DTO fields.
- [ ] Connect cached reads in F6 and learning completion events in F7.

## Backend work

- [ ] Audit vocabulary-items and vocabulary-sets endpoints; define detail and topic-filter semantics.
- [ ] Provide stable pagination and search behavior with deterministic ordering.
- [ ] Validate catalog references and Admin-only mutations; supply representative seed content.
- [ ] Expose version/ETag metadata compatible with F6.

## API and data contract

Proposed groups: /vocabulary-items and /vocabulary-sets. Confirm methods, query names, page shape and content fields before coding.

Data/persistence: Vocabulary item/set identifiers, topic/level relations, agreed reading/meaning/example fields and version metadata.

Before implementation, record exact methods, fields, nullability, errors, authentication, ownership, pagination/versioning and representative fixtures in the API decision log. Preserve existing client contracts or document an explicit migration.

## Verification and acceptance

- [ ] List/search/filter and page without gaps or duplicates; open set/item details.
- [ ] Handle missing content and network failures.
- [ ] Cached vocabulary survives process restart once F6 is integrated.

Acceptance mapping: Vocabulary part of AC-004; AC-005/AC-006 after F6. These are targets, not passing test results. Apply the shared [delivery gates](../planning/delivery.md).

## Delivery handoff

- [ ] Backend contract audited and agreed; existing endpoints reused where suitable.
- [ ] Frontend and backend changes reviewed with migration/configuration notes.
- [ ] Integration verified against a shared environment; failures and remaining work recorded.
- [ ] SRS source-review status, API decisions and sprint review updated with evidence.
