# Delivery process and acceptance gates

## Source and ownership

Requirements live in [SRS](../SRS.md); frontend/backend tasks live in [feature plans](../features/README.md); sprint commitments and reviews live in [sprints](../sprints/README.md). The supplied NestJS/TypeORM/PostgreSQL backend has been [audited at a pinned commit](../contracts/backend-audit.md) and checked against its local API. Production HTTPS, deployment ownership and provider readiness remain unverified. Reuse the audit and implement only missing or incorrect behavior.

Frontend follows the existing Kotlin/Compose/Material 3 MVVM structure, repository contracts, Retrofit/OkHttp/Gson, coroutines/Flow and manual ServiceLocator/ViewModel factory wiring. Do not schedule a Hilt/XML UI rewrite as a prerequisite. Room/WorkManager and account-scoped outbox boundaries are selected in the [offline design](../architecture/offline-sync.md); implementation remains Sprint 05 work.

## Work-item states

| State | Meaning |
| --- | --- |
| Planned | Scoped but not started |
| Ready | Owner, estimate, dependencies, contract and acceptance agreed |
| In progress | Implementation underway |
| In review | Changes ready for review and verification |
| Blocked | Named blocker, owner and next action recorded |
| Done | Integrated behavior passes acceptance with linked evidence |

“Implemented in client” in the SRS is source-review evidence, not a sprint delivery state. All new plans start Planned. Existing code can move tasks forward after verification, without rebuilding working behavior.

## Definition of Ready

- Requirement IDs and frontend/backend/QA owners are assigned.
- Exact API methods, DTOs, errors, authentication/ownership and fixtures are agreed.
- Required migrations, identifiers, pagination/versioning and rollback implications are known.
- Loading, empty, error, offline and session-expiration behavior is defined where relevant.
- Dependencies and test environment/data are available.
- Scope fits the team's estimated capacity; unresolved choices have an owner and decision date.

## Definition of Done

- Frontend and backend behavior meets referenced requirements and agreed contracts.
- Reviews and relevant automated checks pass; include command/environment/result evidence.
- Real integration and meaningful error/permission/ownership cases are verified.
- Persistence, process-death recovery, retries and conflict behavior are checked for affected features.
- Sensitive data is absent from logs; release configuration and account isolation are correct.
- Migrations/configuration and operational or rollback instructions are documented where relevant.
- Feature checklist, sprint review, API decisions and SRS implementation status reflect evidence.
- Unfinished dependencies are recorded as carryover, not hidden behind a Done status.

## Handoff checklist

1. Backend publishes schema/examples and seed/test identities; Android validates DTO mapping.
2. Frontend/backend implement against the agreed contract and share validation/error cases.
3. Integration review checks success, failure, auth expiry, malformed response and retry behavior.
4. QA records AC IDs, environment/build, steps, expected/actual result and evidence.
5. Sprint review records completed acceptance, defects and remaining dependencies.

## Scope and release

Keep the original SRS exclusions unless a deliberate scope change is documented. No production deployment, notification delivery to real users or external ticket creation is authorized by these planning files. Release readiness requires all AC-001–AC-017 and applicable NFRs to have recorded outcomes, with unresolved critical authentication, ownership or data-loss issues blocking release.
