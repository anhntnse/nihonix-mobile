# F5 — Grammar Learning

Status: Planned delivery; source baseline reviewed 2026-11-14.
Requirements: FR-037–FR-044.
Primary sprint: [Sprint 04](../sprints/sprint-04.md).
Frontend owner: TBD. Backend owner: TBD. QA/reviewer: TBD.

## Baseline and outcome

Planned; no current feature implementation.

Deliver the behavior specified in [SRS](../SRS.md), Section 4.5. “Frontend” means the Android client in this repository. Backend tasks are proposed work until the backend repository and existing implementation have been audited.

## Dependencies

- [F3 — Learning Roadmap: Levels & Topics](f03-learning-roadmap.md)
- Agreed API/error/ownership contract in [API decisions](../architecture/api-decisions.md).
- Existing source presence does not mean release or backend acceptance has passed.

## Frontend work

- [ ] Implement grammar sub-topic list/detail and grammar pattern list/detail.
- [ ] Support JLPT filtering and agreed search behavior.
- [ ] Render structures, explanations and examples from confirmed DTOs.
- [ ] Integrate F6 cached reads and F7 completion events.

## Backend work

- [ ] Audit grammar-sub-topics and grammar-patterns routes and relationships.
- [ ] Specify level/topic filters, search and pagination consistently with F4.
- [ ] Provide stable content IDs, examples and version metadata.
- [ ] Restrict authoring to Admin and validate referential integrity.

## API and data contract

Proposed groups: /grammar-sub-topics and /grammar-patterns; confirm pattern-by-subtopic route and full detail schemas.

Data/persistence: Grammar sub-topic and pattern IDs, level/topic relations, structures, explanations, examples and version fields; exact shape remains a contract decision.

Before implementation, record exact methods, fields, nullability, errors, authentication, ownership, pagination/versioning and representative fixtures in the API decision log. Preserve existing client contracts or document an explicit migration.

## Verification and acceptance

- [ ] Browse/filter/search and open sub-topic/pattern details.
- [ ] Verify Japanese text/example rendering and malformed/empty responses.
- [ ] Cached grammar remains available after restart once F6 is integrated.

Acceptance mapping: Grammar part of AC-004; offline acceptance with F6. These are targets, not passing test results. Apply the shared [delivery gates](../planning/delivery.md).

## Delivery handoff

- [ ] Backend contract audited and agreed; existing endpoints reused where suitable.
- [ ] Frontend and backend changes reviewed with migration/configuration notes.
- [ ] Integration verified against a shared environment; failures and remaining work recorded.
- [ ] SRS source-review status, API decisions and sprint review updated with evidence.
