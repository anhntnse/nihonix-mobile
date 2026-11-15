# Sprint08 — Activity, calendar, device and reminders

Implementation contract2026-11-15, base /api. Final verification in progress.
User confirmed a streak day at60 engaged seconds; daily goal separately uses settings.daily_goal_minutes.

## Activity

JWT and optional X-Nihonix-Account pinning apply to all routes; mismatch409. Owner comes from validated JWT.
POST /learning-streaks/activities/batch (200): {events:[{id:UUID,activity_type:vocabulary|grammar|flashcards|practice,occurred_at:ISO8601,duration_seconds:integer1..300,timezone:IANA,item_id?:string≤255}]}. Batch1..50. Response {accepted:[UUID]} in normal success/data envelope. POST /learning-streaks/activity accepts one event with the same fields; legacy count-only writes are intentionally rejected rather than inventing study duration. No Android caller existed for the old contract.

Capture timezone and occurrence at recording. Backend splits integer seconds into date slices using IANA timezone, including midnight/DST. Timestamp may be at most365 days old or5 minutes ahead (including duration). Duplicate UUID/canonical content replays; changed content409. Validation, all event inserts and acknowledgement are one user-locked transaction. Old receipts replay without revalidating timestamp age. No UUID coalescing/rewrite after send.

GET /learning-streaks returns current_streak,longest_streak,total_learning_days,last_learned_date,streak_start_date,is_today_completed,streak_status,today_seconds,today_minutes,daily_goal_minutes,goal_completed,today,timezone,qualification_seconds. Today uses current configured timezone. Historical event date slices do not move after timezone edits. Seconds aggregate before flooring minutes;60 seconds qualifies, goal compares seconds to goal*60. Current streak remains active if last qualifying day is today or yesterday; an earlier gap resets current to0. Longest is recomputed from accepted days, including late offline data.

GET /learning-streaks/calendar?year=YYYY&month=M (year2020..2100,month1..12; defaults current configured local month) returns {year,month,timezone,learning_days:[{date,seconds,minutes,qualified,goal_completed,practice_completions}],total_days}. Zero-activity dates may be omitted; native renders the complete month. New practice completion events capture timezone/activity_date in their payload; legacy completion events without that evidence use a stable UTC date. Timezone edits never regroup existing completions. Sprint07 completion events supply counts only; foreground practice time uses the same activity producer, so stored completion duration is not counted again. Legacy daily count/streak rows remain preserved but are not converted to invented timed events.

## Devices

PUT /devices/:installationUUID (200): {token:string20..4096,enabled:boolean}. JWT must still reference a valid session; response {installation_id,binding_id,enabled}. Owner/session ID are server-derived, never body fields. Registration is serialized, installation/token uniquely rebound; unchanged binding replays, owner/session/token/enabled changes rotate binding UUID. DELETE /devices/:id returns200 {removed:true}, owner-filtered and idempotent. A previous owner cannot remove the installation's newer binding. Session/user deletion cascades device removal; expired sessions are excluded from sends.

## Reminder delivery

FCM_REMINDERS_ENABLED=true explicitly enables a30-second backend scheduler; default off. Existing settings push_enabled AND reminder_enabled, reminder_time and IANA timezone control eligibility. The server skips inactive/unverified accounts, expired/revoked sessions and completed daily goals. A10-minute due window tolerates scheduling delay; missed DST times/windows are skipped, repeated hours dedup by owner/local date.

Durable reminder_attempts claim one owner/local-date attempt before network access. Recheck settings, goal, device binding and session before sending. No retries after ambiguous FCM acceptance: a process crash can lose an attempt, and a provider timeout may still deliver later. This avoids promising exactly-once external delivery. Unregistered-token errors remove only the still-matching binding; generic invalid-argument errors do not purge tokens.

Payload is data-only: schema_version=1,type=learning_reminder,destination=activity,user_id,binding_id,notification_id. No arbitrary URL, private answers or personal title/body. Native checks current account/session/binding and permission before showing a generic reminder. Notification tap uses immutable PendingIntent and checks the same payload again before protected Activity navigation. Unknown versions/types/destinations and stale bindings are ignored.

## Android durability/lifecycle

Room5→6 adds activity_cache(account,key,payload,savedAt) for private summary/calendar/preferences. Existing catalog/progress/flashcard/practice rows remain. Activity has an independent immutable outbox producer and mutex, stable retry UUID/payload, account pinning/late-response protection, blocked-error retry/discard and deletion purge. Backups exclude databases and notification binding preferences.

One foreground study lease covers loaded vocabulary/grammar detail, flashcard review and incomplete practice. Monotonic clock stops on background/navigation/account change and stops credit after60 seconds without interaction. Screen entry and user input establish engagement.15-second checkpoints persist bounded immutable segments; process death can lose the last unchecked interval and subsecond remainder, never already queued time. Foreground study does not mark catalog items learned. Pending seconds are shown separately from authoritative totals.

Permission is requested only from the profile's device-notification controls; denial does not block study. Saving preferences refreshes registration; token refresh, sign-in and periodic sync reconcile binding. Local logout clears binding/visible notifications immediately and attempts unregister before existing server logout. Offline logout cannot revoke a server session without connectivity; data-only stale pushes are ignored locally, and server binding expires with session or is rebound at next login. Reinstall/device-restore does not restore old private bindings.

## Configuration and rollout

Existing Firebase project the-discipline-476506-q7 and Android package com.nihonix.mobile supplied by user. Client configuration retrieved via CLI; backend GOOGLE_APPLICATION_CREDENTIALS references a file outside source control. Private key never committed. No auth provider, SMTP or AI configuration change. Firebase Admin credential/token and validate-only API probe succeeded without delivering a real message.

Migration1794959200000 adds learning_activity_events,notification_devices,reminder_attempts. Down removes immutable activity/idempotency/dedup metadata and is not a safe production rollback. No manual shared migration/deployment or live scheduler activation performed. Existing local synchronize=true watcher can synchronize new entities. Lifetime event/receipt/cache growth needs later retention/resync/scale policy.

Real recipient delivery, physical-device Doze/network transitions, cross-device overlap/acceptance, configured HTTPS and shared deployment remain rollout gates. Automated fixtures use isolated PostgreSQL schemas and fake delivery provider. See sprint execution report for final evidence.
