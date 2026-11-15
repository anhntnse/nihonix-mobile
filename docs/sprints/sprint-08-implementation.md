# Sprint08 implementation design and execution ledger

Status: Implementing. User confirmed60-second streak policy and supplied existing Firebase configuration.2026-11-15.
Android base ed4aed2; backend base c7474c4. Existing checkouts and architecture retained. No automatic commit/push/deployment.

## Scope and audited baseline

F10/FR080–087 activity duration, offline ingestion and timezone-aware summaries/calendar; F11/FR088–095 permission/token/device/reminder lifecycle; complete F1 device cleanup, F2 reminder settings and F9 completion aggregation.

Backend already has JWT learning-streaks GET, POST activity and GET calendar. Its daily aggregate increments count with no immutable event ID/duration/occurred_at and uses server-local midnight; read-modify-save can race. Preserve old rows as historical observations, without inventing durations or timezone evidence. Android has no FCM dependency/service, google-services.json or notification permission. Settings already include push_enabled/reminder_enabled/time/timezone/daily_goal_minutes. Sprint07 provides unique practice_completion_events with native duration currently0. JWT contains sid, but strategy currently returns only user identity; session-aware registration will need validated sid surfaced without weakening authentication.

## Proposed approach

Retain Nest/TypeORM/PostgreSQL, Compose/MVVM/manual DI, Retrofit/Gson, environment/account-scoped Room and independent WorkManager producers. Extend audited routes with explicit event contracts and owner/account pinning. Add durable event storage instead of extending mutable daily counts; events preserve occurrence timestamps and support deterministic late/offline recomputation.

Alternatives considered: device-only totals cannot enforce server reminder goals or cross-device consistency; rewriting auth/sync stacks adds migration risk without helping the feature. FCM remote reminders match F11; local-only alarms would not satisfy backend/device requirements.

## Product decisions awaiting response

Accepted by user: record foreground engaged study/review/practice time only, pause background/idle; cumulative60 seconds qualifies a streak day, daily goal separately uses daily_goal_minutes. Event date uses the timezone captured from settings at recording; timezone changes do not rewrite historical dates. Offline events retain occurrence time. Define idle threshold and split bounded chunks in the concrete plan.

Firebase configuration supplied by user: project the-discipline-476506-q7, Android com.nihonix.mobile. CLI confirms existing registered app; client config retrieved programmatically. Backend local environment references the user-supplied service-account file outside both repositories. Do not create cloud projects, fabricate keys or print credentials. Continue independent design/audit while pending. No live notification delivery until project configuration and an authorized destination are available.

## Required behavior and review points

- Stable activity UUID and canonical payload identity; atomic queue/edit and idempotent server acknowledgement. Account-pinned actions/late-response isolation and account-deletion purge.
- Duration measured with monotonic time, bounded foreground segments and interaction idle handling; avoid duplicate timers for nested screens, config recreation and retries. Durable checkpoints limit process-death loss.
- Preserve occurrence date/timezone, reject invalid/future/excessive events, split midnight consistently including DST. Integer seconds aggregate before converting to minutes. Qualification policy awaits user.
- Practice completion uses Sprint07 unique event identity; do not double-count native timing and server completion duration. Preserve old0-duration completion without fabricated minutes.
- Monthly bounded owner-only calendar and current/longest streak derived from accepted activity; late/offline data recalculates affected history.
- FCM permission denial never blocks study. Token rotation/account ownership/session expiry/logout/deletion and invalid-token cleanup need explicit contracts. Offline logout must hide/ignore stale pushes immediately.
- Backend reminder claim/dedup per owner/local date and schedule, recheck opt-out/session/goal before delivery. Provider/network ambiguity needs documented at-most-once versus retry tradeoff; no false exactly-once FCM guarantee.
- Typed versioned allowlisted notification destinations; cold/warm launch waits for authenticated matching owner. No raw arbitrary URI routing or private payload display after logout.

## Planned verification

Pure policy RED→GREEN for timezone/midnight/DST/late events, duration/idle and dedup; isolated PostgreSQL/actual JWT fixtures with fake notification provider; Room migration/reopen/rollback/account-switch/lost-reply; Compose totals/calendar/permission/error; same-APK process death. Full Android tests/lint/debug/release and backend build/Jest plus prior-sprint regressions. One fresh read-only whole-change review followed by important-finding regression fixes. No shared-schema manual migration or deployment.

## Fresh review and fix pass

One fresh read-only review completed (sprint08_review); no Critical,5 Important findings. Single fix pass in progress, no re-review:
1. Cold startup cleared a matching persisted notification binding; binding test RED then GREEN (shell212→217), accountObserved now preserves matching owner/session.
2. Queued reminder after opt-out was still accepted; binding test RED→GREEN, local eligibility includes reminder setting and clears pending launch/display.
3. Pre-logout DELETE could rotate refresh tokens; delayed logout could erase same-owner replacement session. AuthSessionSafety2 tests RED shell214→GREEN217. Session-pinned dispatch/authenticator2 regressions RED215→GREEN217; local credential replacement/cleanup share storage monitor. Backend validated-session revocation regression pending.
4. Fractional checkpoint attribution moved a second over midnight; deterministic model assertion RED212→GREEN217. Clock drains retain occurrence ranges; tracker captures mapping before async persistence.
5. Practice completion counts moved with current timezone; PostgreSQL assertion RED213, capture timezone/date on new completion, UTC fallback for legacy without evidence. Final fixture pending.
Deferred minor: if initial preferences cannot load, study tracking starts after lifecycle/key change rather than immediately when cache later refreshes; navigate away/back to restart. Legacy count rows remain in storage but do not appear as fabricated timed history. These limits are reported explicitly.

## Ledger

- Read Sprint08/F10/F11/SRS and existing streak/entity/controller, settings, auth/session, Room/worker/ServiceLocator, learning/practice screens and completion event model.
- Android working tree contained only user-local .idea at start. Task1 policy7 tests witnessed RED then GREEN; PostgreSQL API fixture404 RED then GREEN. Device routes404 RED then GREEN, reminder policy2 assertions RED then GREEN. Backend build/Jest95 and Sprint08 activity/device/reminder fixture GREEN (shell204). Native clock/payload3 tests RED (shell205) then GREEN with full unit suite (shell207). Offline repository2 tests RED with empty queue (shell206 + IDE parser) then GREEN with Room6/outbox (shell207). Calendar UI test RED against placeholder (shell208); full native wiring and target regression shell209 pending.
- Firebase client configuration/package verified through CLI, Google-services/debug build shell191 GREEN. Admin ADC obtains access token; Service Usage metadata returned403, but FCM validate-only invalid-recipient probe reaches API (shell194), no notification sent. Private key remains outside repo. No live scheduler enabled.
- npm dependency installation reported69 aggregate audit findings; no broad automatic audit fixes performed. Runtime dependency firebase-admin14.5.1 added for FCM; audit triage remains rollout work.
