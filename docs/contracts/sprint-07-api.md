# Sprint 07 — AI quota, generation and practice

Verified source contract2026-11-15. Base /api. User accepted flashcards2/day, shared fill_blank+multiple_choice3/day, max15 items/request, reset00:00 Asia/Ho_Chi_Minh (UTC+7). Backend is authoritative; no client counter enforcement. [Execution plan](../sprints/sprint-07-implementation.md).

## Authentication and quota

GET /ai/quota, GET /ai/requests/:id, POST /ai/requests/:id/cancel and all practice/flashcard generation require JWT. X-Nihonix-Account, when supplied, must equal the JWT user; mismatch409 before writes/provider calls. Foreign request/set IDs return404.

GET /ai/quota returns the normal success/data envelope. data has day, resets_at (ISO UTC), timezone, max_items=15, provider={status,configured}, buckets.flashcards and buckets.practice. A bucket contains limit, used, reserved, remaining=max(0,limit-used-reserved). Used counts validated successful results; reserved counts active current-day work. Provider configured means key presence in the actual Gemini adapter, not a billable health probe or proof the configured model/key is accepted upstream. Public GET /ai/health returns the same adapter's metadata.

Backend AI_FLASHCARDS_DAILY_LIMIT and AI_PRACTICE_DAILY_LIMIT override defaults2/3 (integer0–100). Invalid configuration fails closed. Maximum output remains15. The quota timezone is the accepted fixed Vietnam policy, independent of a user's activity/reminder timezone.

Quota exhaustion429 has errors.code=AI_QUOTA_EXCEEDED and bucket, resets_at, timezone, limit/used/reserved/remaining. A separate429 AI_GENERATION_BUSY bounds unfinished provider attempts to2/account, including cancelled attempts whose worker has not finished. Busy is not a zero daily allowance.

## Durable generation

POST /practice/fill-blank or /practice/multiple-choice:

```json
{"request_id":"11111111-1111-4111-8111-111111111111","topic_id":"22222222-2222-4222-8222-222222222222","exercise_count":10}
```

Count1–15, default10. Topic must exist and contain vocabulary or grammar material. The prompt uses bounded existing catalog material. New fill_blank exercises use a short text answer and a ____ placeholder; multiple_choice uses4 distinct options with integer IDs1–4. Both require bounded questions and bilingual vi/en explanations. Wrong count/type, duplicate options, missing/oversized/blank fields or a non-member correct answer reject the entire result; no partial save/repair.

POST /flashcards/generate keeps existing folder_id, set_name, count(default5), level_code(defaultN5), difficulty, topic and custom_prompt fields and adds request_id. It now returns a preview rather than automatically creating a set. Native had no generation caller before Sprint07. This intentional legacy behavior change enforces explicit confirmation. Folder ownership is checked before provider access and again before committing the result. Counts1–15; names255, scope topic500/custom prompt2000; generated faces satisfy the existing text-card contract. Result includes draft_id=request_id, folder_id, set_name and stable UUID cards. Save uses the existing Sprint06 bulk/outbox contract only after explicit editor confirmation.

POST generation returns201 with data={request_id,kind,status,result,error}. kind is flashcards|fill_blank|multiple_choice; status reserved|succeeded|failed|cancelled. result is null for pending/failed/cancelled, otherwise a preview or full practice projection. error is null or {code,status}. Native's wire model handles explicit JSON null. A duplicate request still running returns reserved; check GET /ai/requests/:id to recover without a new provider call.

Native supplies stable UUIDs. Legacy missing-ID requests receive a new server UUID and cannot promise retry identity. Canonical payload hashes reject changed kind/content under the same account/request UUID with409. Exact successful retries replay the stored result and never generate/charge again. Failed UUIDs remain terminal; a new explicit attempt needs a new UUID. Recovery GET does not invoke a provider.

A user-row lock reserves allowance, then releases before one bounded provider attempt (45-second deadline; no nested retries). A second transaction atomically persists validated content and marks success. No generation/session/exercise partial commit is accepted. Two-minute leases fail/refund crashed reservations; late results cannot persist after expiry, cancellation or account deletion. A request finishing after midnight consumes its originally reserved day, not the new day.

POST /ai/requests/:id/cancel returns200 with current state. It cancels/refunds only reserved work; a race won by success returns the successful result. Provider processing may finish later, but a cancelled result is not saved/charged. The independent active-attempt slot remains until that worker finishes or its lease expires, preventing cancellation from starting unlimited concurrent calls.

Errors:400 input,404 missing/foreign scope,409 changed request/submission/account,429 quota/busy,502 AI_INVALID_OUTPUT/generation persistence failure,503 AI_UNAVAILABLE/provider unavailable/interrupted,504 AI_TIMEOUT. Logs record request/user IDs, kind/state and safe codes, not prompts, answers, provider response text or credentials. No real provider call is part of the automated fixtures.

## Practice history and grading

GET /practice/sets?topic_id=UUID&exercise_type=fill_blank|multiple_choice&status=in_progress|completed&page=1&page_size=20 returns {items,total,page,page_size,has_more}. Page1–10000, size1–50, deterministic created_at/id order. The old unpaged array becomes this bounded page projection; Android had no prior practice caller.

GET /practice/sets/:id returns owner-only metadata plus ordered exercises. Before completion it omits correct_answer, explanation, user_answer and grading fields. Results/answers are returned only for the owner's completed set. Legacy completed rows may have result:null because they predate submission receipts; Android renders their stored session score and exercise answers/explanations read-only, without inventing a completion event.

POST /practice/submit:

```json
{"request_id":"33333333-3333-4333-8333-333333333333","session_id":"44444444-4444-4444-8444-444444444444","answers":[{"exercise_id":"55555555-5555-4555-8555-555555555555","user_answer":"猫","time_spent":0}]}
```

Every exercise must appear exactly once. Foreign/duplicate/missing IDs reject before writes. MC answers are integer option IDs; text answers are nonblank and≤200 characters. Fill text compares NFKC-normalized, trimmed, lowercase strings, without fuzzy/semantic grading. Legacy numeric-option fill_blank rows remain gradable as choices. Optional time_spent is integer0–3600 per question; native currently submits0 and does not infer activity duration.

Grading, answers, completed session, canonical submission receipt and unique practice_completion_events row commit together. Identical UUID/payload retries replay; changed UUID content or a different answer set after completion conflicts409. A new UUID with identical completed answers returns the original canonical result/completion, without another event.

Result includes request_id,session_id,total_exercises,correct_count,accuracy,total_time,completion_id,completed_at and per-question user/correct answer, explanation and correctness. Practice completion is recorded once regardless of score. No vocabulary/grammar item is automatically marked learned. Sprint08 consumes the durable event for activity/streak/calendar aggregation; that integration is not claimed here.

## Android persistence and compatibility

Room4→5 adds practice_cache(account,key,kind,payload,savedAt), preserving prior progress/flashcards/outbox. Snapshots/drafts/generation input are private per account/environment; backup exclusions continue. Generation input/UUID is committed before sending. Retry/check/cancel retains that context; background work never automatically starts another paid generation.

Answers save as drafts; submission UUID/payload and locked draft commit together in the separate practice outbox producer. WorkManager and immediate sync share its mutex; transient failures retry unchanged payload, permanent errors remain blocked until explicit discard/reload. Other producers are not consumed. Account changes pin actions and ignore late responses; final account-deletion purge covers all producers/caches.

Cached questions and answers resume offline; new generation requires online quota/provider access. Failed allowance refresh marks saved quota stale and disables fresh generation without presenting stale counts as zero. Completed answers/results come from the backend. Generated preview replays use stable IDs and check prior local/canonical set history before opening another editor.

## Migration and rollout

Migration1794872800000 adds ai_generation_requests, practice_submissions and practice_completion_events with owner cascades/unique constraints. It preserves legacy practice sessions/exercises. Down removes new ledger/receipt/event metadata and is not a safe production rollback for consumed usage. Controlled tests use temporary PostgreSQL schemas only.

Minor review follow-ups: quota freshness is recalculated on cache emissions, so an idle screen can retain a freshness flag beyond60 seconds (backend still enforces limits); an alternate UUID replay of identical completed answers returns the canonical result without reserving that alternate UUID; POST generation readiness checks precede receipt lookup, so use GET recovery during provider configuration outages. These do not create extra charges or completion events.

Receipt/request retention and native cache growth currently last for account lifetime; compaction/large-scale cache performance remain rollout work. Shared migration/deployment, real configured Gemini model/key acceptance, physical-device background/network transitions and cross-device acceptance remain separate. Existing SMTP and provider credentials/model configuration are unchanged.
