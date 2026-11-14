# F13 — Application Configuration & System

Status: Sprint 03 implementation locally verified; shared-environment rollout pending.
Requirements: FR-101–FR-105. Primary sprint: [03](../sprints/sprint-03.md). Owners/reviewer: TBD.

## Frontend plan and delivery

- [x] Sprint 01 development/release separation, HTTPS release gate and credential-safe logging.
- [x] Retrieve validated startup configuration; detect maintenance and minimum numeric versionCode.
- [x] Open safe update destination and offer retry/error states.
- [x] Cache configuration by environment with fetch timestamp and bounded TTL.
- [x] Fresh cached fallback; retain known restrictions; explicit continuation when no usable config exists.
- [x] Diagnostic health check without independently blocking study.
- [ ] Verify actual production HTTPS/update destination and shared-environment startup scenarios.

## Backend plan and delivery

- [x] Retain health liveness contract and add public app/config.
- [x] Publish schema/version/maintenance/update/TTL fields from validated environment configuration.
- [x] Preserve error envelopes and add server-generated request IDs.
- [x] Automated configuration validation and real PostgreSQL/HTTP contract tests.
- [ ] Configure/deploy target environment and verify production availability/rollout behavior.

## Contract and acceptance

[Canonical API and startup policy](../contracts/sprint-03-api.md) defines precedence, cache expiry, failure behavior, update validation and defaults. Configuration cache is not the Sprint 05 learning-content cache. Health is liveness, not database/provider readiness.

Supported/obsolete versions, maintenance, malformed/missing config, stale/future caches and health failures have local test evidence in the [Sprint 03 report](../sprints/sprint-03-report.md). System acceptance alongside AC-016 still requires shared-environment demonstration. Whole-product errors and performance need later feature/release verification.

Dependencies: F1 session navigation; F6 persistent learning content. [API decisions](../architecture/api-decisions.md), [delivery gates](../planning/delivery.md).
