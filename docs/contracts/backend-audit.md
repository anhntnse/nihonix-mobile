# Sprint 01 backend audit

Reviewed: 2026-11-14.
Repository: [anhntnse/nihonix-backend](https://github.com/anhntnse/nihonix-backend).
Audited commit: `b981b845b6e41add43e0f8f4901d5c38943635ce`.
Source checkout used: temporary audit clone, outside the Android repository. No backend source changes, migrations, seeding or deployment were performed.

## Environment and evidence

- Backend stack: NestJS 11, TypeScript, TypeORM 0.3 and PostgreSQL; JWT/bcrypt authentication, SMTP recovery, Cloudinary avatars and a backend AI provider integration.
- [Bootstrap](https://github.com/anhntnse/nihonix-backend/blob/b981b845b6e41add43e0f8f4901d5c38943635ce/src/main.ts) defaults to port 3010 and prefix /api; Swagger is /api-docs and JSON is /api-docs-json. The older README's port 3000 is stale relative to bootstrap and the supplied environment.
- [Modules](https://github.com/anhntnse/nihonix-backend/blob/b981b845b6e41add43e0f8f4901d5c38943635ce/src/app.module.ts) and [configuration](https://github.com/anhntnse/nihonix-backend/blob/b981b845b6e41add43e0f8f4901d5c38943635ce/src/config/configuration.ts) load .env.develop outside production and .env.production in production. Environment files and secrets were not inspected or copied.
- Tooling observed: Node v24.21.0; npm 11.19.0.
- Locked dependencies installed in the temporary clone with `npm ci --ignore-scripts --no-audit --no-fund`.
- `npm run build`: passed. `npm test -- --runInBand`: 2 suites, 4 tests passed.
- These unit tests cover app-controller responses and registration DTO validation; they do not establish auth rotation, ownership, quota or database integration correctness.

The live server at http://localhost:3010 was tested separately using read-only requests. Its deployed commit was not exposed, so the smoke results must not be treated as proof that it runs the audited Git revision.

## Live smoke results

| Request without credentials | Result |
| --- | --- |
| GET / | 200 |
| GET /api-docs-json | Success; 53 path entries |
| GET /api/health | 200 |
| GET /api/levels | 200, success envelope, 5 items |
| GET /api/topics | 200, success envelope, 125 items |
| GET /api/users/me/level | 401 |
| POST /api/auth/me | 401 |
| GET /api/app/config | 404 |

No test accounts were created and no real email, AI generation, notification, seed or mutation endpoint was invoked. Authenticated user journeys need agreed test identities and additional integration checks.

## Observed endpoint inventory

This inventory follows controller source rather than the old API_INVENTORY.md count, which is internally inconsistent. Existence and guards are source observations, not end-to-end readiness.

| Group | Observed routes, relative to /api | Access in inspected source |
| --- | --- | --- |
| Auth | POST auth/register, login, refresh, forgot-password, verify-reset-code, reset-password, resend-code | Public |
| Auth profile/session | POST auth/me, auth/logout | JWT |
| Users | GET users; GET/PUT/DELETE users/:id; GET/PATCH users/me/level; POST users/me/avatar | JWT; list is Admin; update/delete check self/Admin; detail does not check owner |
| Levels | GET levels; GET levels/:code | Public |
| Topics | GET topics?levelCode=; GET topics/:identifier; POST topics; POST topics/seed | Public, including writes |
| Vocabulary items | GET vocabulary-items; GET vocabulary-items/sets/all; GET vocabulary-items/:slug; POST vocabulary-items/generate | No controller guards found |
| Vocabulary sets | GET vocabulary-sets; GET vocabulary-sets/by-topic/:topicId; GET vocabulary-sets/:id; POST vocabulary-sets | No controller guards found |
| Grammar sub-topics | GET grammar-sub-topics; GET grammar-sub-topics/:slug; GET grammar-sub-topics/level/:levelCode | No controller guards found |
| Grammar patterns | GET grammar-patterns; GET grammar-patterns/:slug; GET grammar-patterns/sub-topic/:grammarSubTopicSlug; POST grammar-patterns/generate | No controller guards found |
| Entity sync | POST entity-sync/check; GET entity-sync/:entityKind/:entityKey | Public; kinds currently level/topic |
| Flashcard folders | POST/GET flashcard-folders; GET/PUT/DELETE flashcard-folders/:idOrSlug | JWT, no requesting user passed to service |
| Flashcards | POST flashcards; POST flashcards/generate; PUT flashcards/bulk; GET flashcards/sets; GET flashcards/sets/:setId/cards; DELETE flashcards/sets/:setId | JWT, no requesting user passed to service |
| Practice | POST practice/fill-blank, practice/multiple-choice, practice/submit; GET practice/sets, practice/sets/:id | JWT; requesting user passed to service |
| Streak/calendar | GET learning-streaks, learning-streaks/calendar; POST learning-streaks/activity | JWT; requesting user passed to service |
| AI | GET ai/health | Public |
| System | GET health; root GET / outside prefix | Public |

Evidence: [auth](https://github.com/anhntnse/nihonix-backend/blob/b981b845b6e41add43e0f8f4901d5c38943635ce/src/modules/auth/auth.controller.ts), [users](https://github.com/anhntnse/nihonix-backend/blob/b981b845b6e41add43e0f8f4901d5c38943635ce/src/modules/users/users.controller.ts), [topics](https://github.com/anhntnse/nihonix-backend/blob/b981b845b6e41add43e0f8f4901d5c38943635ce/src/modules/topics/topics.controller.ts), [sync](https://github.com/anhntnse/nihonix-backend/blob/b981b845b6e41add43e0f8f4901d5c38943635ce/src/modules/entity-sync/entity-sync.controller.ts), [flashcards](https://github.com/anhntnse/nihonix-backend/blob/b981b845b6e41add43e0f8f4901d5c38943635ce/src/modules/flashcards/flashcards.controller.ts), [practice](https://github.com/anhntnse/nihonix-backend/blob/b981b845b6e41add43e0f8f4901d5c38943635ce/src/modules/practice/practice.controller.ts), [streaks](https://github.com/anhntnse/nihonix-backend/blob/b981b845b6e41add43e0f8f4901d5c38943635ce/src/modules/learning-streaks/learning-streaks.controller.ts). Global bootstrap/module review found no global authentication guard that would protect the otherwise unguarded routes.

## Confirmed contracts and mismatches

- Login submits username/password. [AuthService](https://github.com/anhntnse/nihonix-backend/blob/b981b845b6e41add43e0f8f4901d5c38943635ce/src/modules/auth/auth.service.ts) returns user, accessToken and refreshToken directly under data; the inventory's nested tokens example is stale.
- Registration requires username length >=3, valid email and password length >=6; full_name is optional. Password reset requires >=8 characters. Preserve the current wire shape and resolve the policy difference before changing validation.
- POST auth/me returns req.user from JwtStrategy/validateUser: id, username, email, full_name and role. It does not call the fuller getCurrentUser method, so avatar, language and level fields are missing. Mobile profile refresh can overwrite richer cached fields with defaults/nulls. Backend should return a canonical full profile in Sprint 02.
- POST auth/resend-code is a password-reset operation. It must not be reused as registration-email verification.
- GET/PATCH users/me/level use level_code; PATCH accepts only N5–N1. Topic filter query is levelCode. Localized fields are vi/en/jp.
- ResponseInterceptor wraps success/message/data/errors/statusCode and moves a returned message property into the outer envelope. Code-verification messages must be read from the envelope.
- Public catalog access is implemented on both sides, while the SRS guest policy is authenticated-only. Record a product decision; do not assume a controller annotation resolves the intended policy.

Source-derived, non-secret examples: [auth/catalog fixtures](auth-catalog-fixtures.json). These are illustrative fixtures, not recorded successful login sessions.

## Backend gaps and follow-up

| Priority | Finding | Required follow-up / feature |
| --- | --- | --- |
| Release blocker | Only a local HTTP environment is supplied | Provide deployed HTTPS URL, TLS and staging/deployment owner; F13 |
| High | Registration creates active accounts and login has no email-verification check; verify-email route absent | Implement verification/challenge contract and rejection of unverified accounts; F1 / Sprint 02 |
| High | Topics create/seed and catalog generation/write routes lack guards | Protect Admin authoring/seeding/generation; test negative access before exposing deployment; F3–F5 |
| High | GET users/:id requires JWT but has no self/Admin check | Enforce agreed profile privacy/ownership; F2 |
| High | FlashcardFolder has no user_id; folder/card controllers do not pass requesting user | Ownership migration and per-user authorization before personal flashcards; F8 |
| High | Refresh rotation uses read/compare/update without a demonstrated atomic replay/concurrency check | Verify replay, concurrent refresh and same-second issuance; F1 |
| High | Entity sync supports only level/topic; success path uses @Res without passthrough yet returns an entity | Verify/fix successful response completion and 304 behavior before F6; extend content kinds/contracts |
| Planned | No progress/device/app-config controller in inspected module inventory; live app/config is 404 | Implement confirmed contracts in F7/F11/F13 |
| Planned | AI health exists, but quota endpoint/enforcement is not established by this audit | Audit provider call paths and define atomic quotas before F9/F12 |
| Planned | Activity API accepts type/count rather than explicit duration/occurred_at/timezone | Extend idempotent offline activity contract for F10 |
| Operational | DatabaseModule disables certificate verification for its Neon-specific SSL branch | Establish verified database TLS and deployment configuration before release |

Evidence for ownership gaps: [folder entity](https://github.com/anhntnse/nihonix-backend/blob/b981b845b6e41add43e0f8f4901d5c38943635ce/src/modules/flashcard-folders/entity/flashcard-folder.entity.ts), [folder controller](https://github.com/anhntnse/nihonix-backend/blob/b981b845b6e41add43e0f8f4901d5c38943635ce/src/modules/flashcard-folders/flashcard-folders.controller.ts). TLS configuration: [database module](https://github.com/anhntnse/nihonix-backend/blob/b981b845b6e41add43e0f8f4901d5c38943635ce/src/database/database.module.ts). These are static findings; no unauthorized data access or write exploit was attempted.

## Sprint handoff

The repository and current wire contracts are now known. Staging credentials/HTTPS, product access policy, backend ownership and missing verification/sync contracts remain open. See [API decisions](../architecture/api-decisions.md), [offline design](../architecture/offline-sync.md) and [Sprint 01](../sprints/sprint-01.md). The backend was audited and tested, not modified or deployed.
