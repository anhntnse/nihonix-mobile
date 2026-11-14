# Email verification request diagnosis and fix

Updated: 2026-11-15.

## Confirmed cause

Android AccountApiService calls POST /api/auth/request-email-verification with {email}. AuthController delegates to EmailVerificationService.request. The supplied test email had no matching account in the local database; the original implementation skipped OTP creation and EmailService but still returned HTTP 200 and a success-like generic message. The user confirmed that no account had been created.

Independent SMTP verification and a test submission had already succeeded (provider 250). This did not prove that the verification endpoint reached EmailService. SMTP configuration was not changed by this fix.

## Corrected behavior

Per the explicit product request, a well-formed email that has no account now returns 404 with “Không tìm thấy tài khoản nào đăng ký với email này.” No OTP is generated/stored and EmailService is not called. This deliberately supersedes the previous generic unknown-account response. Malformed email remains DTO validation 400.

For an existing unverified account, a six-digit cryptographically random OTP is bcrypt-hashed and committed in a transaction before sending. It expires after 15 minutes. A valid pending challenge suppresses resending for two minutes; expired/consumed challenges do not block a new issuance. Already-verified accounts retain their existing no-send behavior.

A real PostgreSQL regression exposed a second defect: legacy created_at timestamp values were read seven hours behind the application clock, bypassing resend cooldown. Cooldown now derives issuance time from the explicitly app-written expires_at minus the fixed 15-minute TTL, avoiding a schema/timezone change.

Mail failures no longer become HTTP 200 success for explicit verification requests: the failed challenge is removed by its own ID and the endpoint returns 503. Registration keeps its existing account-created/retry-guidance behavior. Provider submissions without an accepted recipient are treated as failures.

Email lookup is case-insensitive exact equality; underscores in email addresses no longer act as ILIKE wildcards. Verify-email uses the same lookup. Session/login/reset architecture and SMTP host/port/credentials remain unchanged.

Android displays the server error without starting a new resend countdown. A new request clears previous success text so success/error messages do not contradict each other.

## Trace logs

Follow X-Request-Id from the response through:

1. verification.request_received
2. verification.skipped with account_not_found, already_verified, account_removed or resend_cooldown; or
3. verification.challenge_saved
4. smtp.verification_send_started
5. smtp.verification_accepted and verification.delivery_accepted; or smtp.verification_failed / verification.delivery_failed
6. verification.challenge_cleanup_failed if failed-delivery cleanup also fails

Database stages use verification.account_lookup_failed and verification.challenge_creation_failed. SMTP transport initialization records host/port/secure and credential-presence booleans only. Failure logs include exception type, code and numeric SMTP response code. Logs do not contain OTPs, OTP hashes, passwords, raw provider messages or recipient addresses; the request includes a short normalized-email fingerprint for correlation.

SMTP acceptance proves provider submission, not inbox placement. A cooldown skip intentionally sends no additional email.

## Verification and rollout

- Backend build and 51 unit tests across eight suites passed.
- npx ts-node -r tsconfig-paths/register test/verification-mail.integration.ts passed using a unique temporary local PostgreSQL schema and fake mail delivery. Covers HTTP 404, hashed OTP persistence before sending, consumption, cooldown, verified skip, SMTP failure 503, cleanup and immediate retry. Schema/account are removed afterward.
- Android debug build/lint, 78 unit tests and nine emulator-5556 instrumented tests passed. The UI regression requests a code for an unknown email, asserts the exact error text and verifies retry remains enabled. git diff --check passed in both repositories.
- No new real email was sent for this fix. The integration harness has a separate explicitly invoked --live-smtp mode; it is not run by normal tests.
- Live smoke check against http://localhost:3010/api/auth/request-email-verification confirmed HTTP 404 and the exact requested Vietnamese message for a database-confirmed missing account. The running backend has loaded the fix; no manual process restart or shared database migration was performed by the agent. Install/run the updated app. Use a registered unverified account for an actual verification flow.
