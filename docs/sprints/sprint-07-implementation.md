# Sprint 07 implementation plan and execution ledger

> Execute inline using executing-plans, TDD and one fresh whole-change review. User requested execution of the existing Sprint07/F9/F12 plan and explicitly accepted the quota policy below. Preserve the established two-checkout Android Studio workflow and docs/sprints location. User subsequently authorized Sprint07 commits and push; deployment remains separate.

Goal: FR-069/071–079/096–100; quota-first generation, private practice/graded results and explicit generated-card save.
Architecture: retain Compose/MVVM/manual DI, Retrofit/Gson/Room/WorkManager and Nest/TypeORM/PostgreSQL/Gemini. Provider keys stay server-side. No new runtime dependency or streak/calendar aggregation.
Specs: sprint-07.md, ../features/f09-ai-practice.md, ../features/f12-ai-quota.md, SRS4.8/4.9/4.12 and Sprint06 flashcard contracts.

## Accepted product policy

User confirmed2026-11-15: flashcards2 requests/day; fill_blank + multiple_choice share3 requests/day; max15 items/request; reset00:00 Asia/Ho_Chi_Minh. Charge only a validated successful result. Same UUID/payload retry never charges twice. Failed/expired/cancelled reservations do not consume successful usage.

## Approach and interfaces

Reuse the existing generation/practice routes and Gemini integration; add a database request ledger rather than local counters or an external queue. Pure counters cannot serialize concurrent provider requests or recover lost responses. A new provider/framework would add configuration without helping the existing app.

- AiRequestService.execute(user,id,kind,payload,generate,persist) reserves under the account row lock, releases it for one bounded provider attempt, and atomically persists validated results plus consumption. Durable request states reserved/succeeded/failed/cancelled; payload hashes reject UUID reuse. Two-minute leases reclaim crashed attempts; late completions cannot persist after cancellation/expiry/account deletion.
- GET /ai/quota returns provider readiness and authoritative bucket used/reserved/remaining/reset/max_items; GET /ai/requests/:id recovers outcomes; POST /ai/requests/:id/cancel refunds only still-reserved work. All private routes use JWT/expected-account checks. Provider readiness follows the actual Gemini adapter.
- POST /practice/fill-blank and /practice/multiple-choice accept request_id, topic_id, exercise_count. POST /flashcards/generate accepts the existing scope fields plus request_id and returns a preview, without creating a set. Native always supplies stable UUIDs; legacy missing-ID requests get a server UUID but cannot promise lost-response retry.
- Generated output is strict: exact count/type, bounded nonblank text, valid multiple-choice IDs/options/correct answer, fill-blank text answers and bilingual explanations. Never expose answers/explanations before completion. No silent partial-output repair/save.
- GET /practice/sets adds bounded page/topic/type/status filtering; GET /practice/sets/:id is owner scoped. POST /practice/submit validates every question exactly once and grades in one transaction. Identical submissions replay; changed submissions conflict. A unique practice_completion_events row records completion once; Sprint08 consumes it for activity/streak/calendar.
- Room v5 adds account-keyed AI/practice snapshots/drafts/request state. Existing v1–v4 data remains. Submission uses its own immutable practice outbox producer; generation is explicit online work, never an automatic paid background producer. Answers resume offline; submitted drafts lock until acknowledgement or explicit resolution.
- Android shows quota/reset/unavailable distinctly, request pending/recover/cancel/error states, topic/type/status filters, practice/detail/answer/result screens and generated-card preview using Sprint06's explicit-save editor. Stable generated draft IDs prevent duplicate sets after replay.
- Account changes pin requests/actions, hide private caches and prevent late response merges; deletion purges AI/practice cache and queues in the existing final transaction.
- No real SMTP/provider calls or shared migration during automated checks; isolated PostgreSQL and deterministic provider fixtures. Keep current provider credentials/model configuration; document actual-provider acceptance separately.

## Task 1 — Quota and generation ledger

Files: backend modules/ai/quota-policy.ts/.spec.ts, ai-request.entity.ts/service.ts, controller/module/service; migration1794872800000-AiPractice.ts; data-source.ts; provider interfaces/adapters.
Interfaces produced: quotaPolicy(now), AiRequestService.execute/status/cancel/quota, structured AI errors.
- [x] RED→GREEN: Vietnam midnight/reset boundaries, separate/shared buckets, output limits.
- [x] PostgreSQL: concurrent reservations, exact replay/UUID conflict, failures/refunds, pending/expiry/cancel and cross-account access.
- [x] Single bounded provider attempt, safe errors/logging; readiness checks do not call a provider.

## Task 2 — Strict generation, practice and submission

Files: modules/practice/practice-policy.ts/.spec.ts, practice-generation.ts, practice-projections.ts, practice.service.ts/controller/DTOs; flashcards service/controller/DTOs.
Consumes Task1 execute callbacks. Produces owner library/detail/result and exactly-once completion events.
- [x] RED→GREEN: malformed/wrong-count/wrong-type output, MC answer membership, text normalization, duplicate/missing/foreign answers and time bounds.
- [x] Atomic generation/session/exercise persistence; no answer leakage before submission.
- [x] Atomic grading/receipt/completion event; concurrent/repeated submission and rollback tests.
- [x] Generated flashcards remain preview-only until the existing manual save path.

## Task 3 — Durable native AI/practice repository

Files: feature/practice/domain/* and data/*; OfflineDatabase migration4→5, SyncWorker, ServiceLocator/NetworkModule.
Interfaces: PracticeRepository.state/saveAnswers/submit/sync/generate/check/cancel/quota/purge; API DTOs match Tasks1/2.
- [x] Native model/Retrofit tests with decimal IDs, structured429, account pinning.
- [x] Room cache/drafts/immutable submission/request recovery, account isolation and migration.
- [x] Lost response/process restart preserves UUID/payload; other producers remain independent.

## Task 4 — Compose integration

Files: feature/practice/presentation/*, flashcards preview/editor integration, AppViewModelFactory, AuthNavGraph, TopicsScreen/TopicDetailScreen.
- [x] Quota-first entry, topic/type/count form, cancellable/recoverable generation and bounded practice history filters.
- [x] Both exercise types, offline draft resume, explicit submission/pending/blocked states, backend results.
- [x] AI flashcard request → preview → explicit editor save with stable generated IDs.
- [x] Compose tests for quota unknown/zero/429, validation, answer/result, account change and large-text/small-screen themes.

## Task 5 — Verification and handoff

- [x] Android final lint/debug/release packaging passed (shell172); unit/instrumented and3 same-APK restart fixtures passed; backend build/Jest84 and isolated Sprint05/06/07 regressions passed.
- [x] One fresh read-only whole-change review; reproduce important findings RED→GREEN and run green suites.
- [x] Update SRS, F8/F9/F12, sprint/API/report and backend handoff; retain rollout limits.
- [x] No automatic Sprint07 commit/push/deployment.

## Review focus

Cancellation/lease expiry racing provider success must not create content or charge twice.
A quota reset while a request is running must use its reserved day without consuming the new day.
Lost submission/generation replies must recover exact stored outcomes after process death.
Questions/answers from another set/account must never be graded or leaked.
A replayed generated preview must not silently create another flashcard set.

## Pre-flight and ledger

- Initial backend has Gemini generation and practice routes, but no quota/ledger, non-atomic saves, exposed pre-submission answers and mutable duplicate submissions.
- User approved the quota policy; existing Sprint07 execution request authorizes implementation without repeating a design-permission workflow.
- Task1→2: common request states/payload hashes and transactional persistence callbacks; Task2→3: snake_case DTOs/server grading; Task3→4: account-keyed Flow and stable UUID requests. No interface conflicts identified.
- Quota4 tests RED→GREEN; practice validation/grading7 tests RED→GREEN; bounded Gemini2 tests RED→GREEN. PostgreSQL/HTTP Sprint07 fixture initially401-vs404 RED, now passes reservations/replay/refund/cancel/expiry/preview/ownership/grading/completion/filters/account cascade with stubbed provider/mail.
- Fixture correction: provider selection initially matched the multiple_choice word in generic instructions; narrowed it to the requested type in the first sentence. The strict output validator correctly rejected the wrong fixture type.
- Native model3 tests RED observed. Repository5 instrumented tests RED observed (5 assertions fail), now GREEN with full unit run and Room v5, including lost reply/reopen, producer isolation, stale account, generation recovery and account purge. Full migration/process/HTTP/UI and final regression checks remain.
- Native practice UI3 tests first RED with empty screen, then passed after implementing quota/forms/submission/results. Duplicate Generate practice text and initial-account topic-load race reproduced and fixed. Stable AI preview replay test RED→GREEN; generated cards retain request-derived set IDs and check canonical/local history before reopening.
- Expanded PostgreSQL fixture passes real concurrent last reservations, generation/submission rollback, zero-score results, cross-midnight completion and legacy-preserving migration. Cancel-abuse active-attempt regression first returned201 instead of429; capped active attempts including cancelled unfinished work, then fixed expected-account header bypass (409-vs201 RED).
- Native transport test found Gson JsonObject rejects explicit result:null for pending/cancelled responses. Replaced wire field with JsonElement plus object projection; regression GREEN with all121 unit tests. MC option-label and stale-quota UI regressions first failed, then GREEN: render choice labels, invalidate cached quota before refresh and gate new generation on freshness. Expanded practice/flashcard UI and repository/migration targets pass together. Full ordinary suite, process restart and final review remain.
- Final ordinary native run shell162 passed after latest-generation ordering RED→GREEN and request-list expansion. Fresh read-only reviewer found2 Important defects: legacy completed practice displayed an editable form and cancelled failed workers held active slots. Both reproduced RED (UI missing score; backend201-vs429), fixed in one pass. Backend Sprint07 fixture shell165 is GREEN; final native/full backend/process/build checks remain. Minor follow-ups are recorded in the contract; no re-review planned.
- Gemini REST contract reference checked: https://ai.google.dev/api/generate-content (finishReason STOP/truncation and token configuration). Existing credentials/model configuration retained; no real provider request made.
- Ruling: retain current IDE checkouts/branches and defer commits to a separate request, matching the established sprint workflow; this preserves user-visible edits and leaves a reviewable diff.

- Final regression shell166 native unit/ordinary UI GREEN; shell167 backend build/Jest84 GREEN; shell168 isolated Sprint05/06/07 GREEN; shells169–171 same-APK install/seed/force-stop/verify3 fixtures GREEN. Shell172 final lint/debug/release is running. Requirements identifier sets retained105/19/26/17. Important findings fixed in the one planned pass; minor follow-ups recorded for rollout, no second review.

- Final shell172 lint/debug/release packaging BUILD SUCCESSFUL (2m15s), reserved example HTTPS URL only. All required local verification complete; shared/provider rollout stays open. No commit/push/deployment performed.
