# F11 — Device Registration & Push Notifications

Status: Implemented and locally verified with a fake provider (Sprint 08; device registration with a real FCM token confirmed in the Sprint 09 emulator run). No real notification has been delivered; physical-device and shared-environment acceptance pending. Requirements FR-088–095. Updated 2026-11-16.
[Sprint08](../sprints/sprint-08.md) · [Contract](../contracts/sprint-08-api.md) · [Firebase setup](../troubleshooting/firebase-fcm.md).

## Delivered frontend

- [x] Firebase Messaging, explicit profile permission/device controls; denial never blocks study.
- [x] Token refresh, account/session binding, periodic reconciliation and local logout suppression.
- [x] Data-only payload and notification tap allowlist; current owner/session/binding required.
- [x] Preference changes disable queued reminder display, and cold startup preserves valid durable binding.
- [x] Private binding backup exclusion and account-deletion cleanup.

## Delivered backend

- [x] Authenticated idempotent device registration/rebinding/rotation; owner/session-scoped removal and cascade.
- [x] User timezone/time/preferences and daily-goal suppression.
- [x] Durable owner/local-date reminder claim, bounded provider attempt and unregistered-token cleanup.
- [x] Explicit scheduler opt-in; default remains off during automated/local verification.
- [x] Logout revokes validated JWT session even if refresh token rotates during device cleanup.

FCM project supplied by user: the-discipline-476506-q7; Android package com.nihonix.mobile. CLI config and debug build passed, ADC and validation-only API probe passed with no real delivery. Private key stays outside source control. No Firebase Authentication migration.

## Acceptance and limits

Local fixtures exercise binding ownership/session/rotation/logout, opt-out/goals/dedup/invalid token and payload filtering (`test:sprint08` PASS, re-run 2026-11-15). In the Sprint 09 emulator run the app obtained a real FCM token, registered it ("Notifications registered for this account") and the fake provider sent 0 messages. Actual recipient delivery and physical-device cold/warm/Doze acceptance remain rollout gates; AC-015 is not claimed as deployed acceptance. Scheduler requires FCM_REMINDERS_ENABLED=true after rollout approval.

External delivery cannot promise exactly once: claims are not retried after ambiguous provider acceptance, and crash can lose a reminder. Offline logout immediately hides/ignores old-account payloads locally; server session revocation needs connectivity or expiry. See report for deferred minor follow-ups.
