# Sprint 02 — Authentication and profile/settings

Status: Android/backend implementation and local integration delivered; shared-environment rollout/provider acceptance pending.
Updated: 2026-11-14. Owners, capacity and calendar dates remain unassigned.

Scope: F1 and F2; existing F3 level behavior retained.

- [x] Verification, resend/cooldown, recovery restart and aligned password policy.
- [x] Account-safe refresh/logout and session revocation.
- [x] Profile editing, avatar picker, confirmed deletion and learning preferences.
- [x] Backend verification, transactional rotation/reset and owner-scoped contracts.
- [x] Schema migration and deterministic unit/UI/isolated PostgreSQL tests.
- [ ] Shared backend migration/restart and actual SMTP/Cloudinary acceptance.
- [ ] Production HTTPS environment, gateway limits and retention review.

Local evidence covers account AC-001/AC-002/AC-016 and F2 persistence/ownership. Whole-product AC-017 remains open. Notifications/device cleanup remain Sprint 08; offline content remains Sprint 05. No shared database changes were applied.

See [execution report](sprint-02-report.md), [account API](../contracts/sprint-02-api.md) and [delivery gates](../planning/delivery.md).
