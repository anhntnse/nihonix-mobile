# Sprint 02 account API

Implemented 2026-11-14 in Android and the sibling backend. Base path /api. Supersedes Sprint 01 auth/profile observations; deployment remains pending.

## Authentication

2026-11-15 correction: verification requests for unknown emails now return 404 with “Không tìm thấy tài khoản nào đăng ký với email này.” as explicitly requested. Delivery failures return 503 rather than success; detailed [diagnosis, logging and regression evidence](../troubleshooting/email-verification.md). SMTP configuration remains unchanged.

Login remains username/password. Registration/reset require at least eight characters and at most 72 UTF-8 bytes. Registration creates an unverified account; login rejects it with 403 until verified. Migration does not silently verify existing accounts.

| Method | Route | Contract |
| --- | --- | --- |
| POST | auth/register | Existing body; verification_required and delivery guidance |
| POST | auth/request-email-verification | {email}; 404 when no account exists; 503 on delivery failure; otherwise retry_after_seconds=120 |
| POST | auth/verify-email | {email,code}; six ASCII digits; consumes challenge |
| POST | auth/login | {username,password}; flat token pair under data |
| POST | auth/refresh | {refresh_token}; atomic one-use rotation |
| POST | auth/logout | Existing refresh-token body; revokes session |
| POST | auth/me | Bearer access token; full current profile |
| POST | auth/forgot-password | Existing email request |
| POST | auth/verify-reset-code | Existing email/code request; temporary reset token |
| POST | auth/reset-password | Existing temporary token/new password; single use, revokes sessions |

Verification expires after 15 minutes; resend interval 120 seconds; at most five failed guesses. Recovery also persists guess counts. Reset tokens expire after ten minutes and cannot authenticate API requests. Access/refresh purposes differ and require an active session. Full refresh JWT is digested before bcrypt to avoid truncation. Concurrent refresh permits only one rotation. Logout/reset/deletion invalidate access immediately; old tokens require a new login after rollout.

Auth POSTs have a per-process IP limit of ten/minute (refresh/logout excluded), returning 429 and Retry-After. Multi-instance deployment needs coordinated gateway limits. Android retains sessions on temporary refresh outages, clears revoked sessions, and guards against late responses from another account.

## Profile

Bearer authentication; owner identity comes from JWT.

| Method | Route | Contract |
| --- | --- | --- |
| PATCH | users/me/profile | Optional username (trimmed 3–50), full_name (up to 100); returns profile |
| GET/PATCH | users/me/settings | push_enabled, reminder_enabled, reminder_time, timezone, daily_goal_minutes |
| POST | users/me/avatar | Multipart avatar; JPEG/PNG/WebP content validation; 10 MB maximum |
| DELETE | users/{id} | Owner or existing privileged role; 204 |
| GET/PATCH | users/me/level | Existing contract retained |

Settings default to false/false, 20:00, UTC and 15 minutes. Time is HH:mm; timezone must be recognized; goal integer 1–1440. Null/unknown fields and self-service email/status escalation are rejected. Other-user profile reads are forbidden except existing privileged role.

Avatar cleanup precedes account deletion; provider failure preserves the account for retry. Existing database cascades remove owned records; client token/profile storage clears on success only. Retention/backups require deployment review. Preferences are persisted configuration: notification delivery and device cleanup remain Sprint 08, offline learning caches Sprint 05.

See [test and rollout evidence](../sprints/sprint-02-report.md). PostgreSQL tests use an isolated temporary schema and fake SMTP/Cloudinary providers; real-provider/shared-environment acceptance is pending.
