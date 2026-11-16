# Authorization matrix — current backend

Date: 2026-11-16. Backend baseline: `91d151ba58f9f84d2a2b6d65c7bae4ecea1f4699` plus uncommitted section-02 changes.
Source: registered module/controller metadata traversed by `src/security/route-access.spec.ts`; HTTP ownership fixture `test/sprint09.ownership.integration.ts`.
No providers or database are started by the unit inventory itself.

## Policy and evidence

- 82 routes / 20 controllers: 13 public, 63 authenticated/owner or catalog, 6 Admin-only.
- Public means no JWT gate; DTO/challenge validation, credential throttling, not-found and maintenance behavior still apply. It does not mean every request returns 200.
- Authenticated/owner means valid session plus the feature's owner predicates. Admin does not gain access to another user's private practice/flashcards merely from role; the Users controller explicitly permits self/Admin operations.
- Admin-only: guest 401, ordinary user 403; valid Admin reaches DTO/service validation.
- Integration now exercises anonymous rejection on all 69 guarded routes (including 22 catalog patterns), rejected guest and accepted authenticated catalog reads, cross-user IDs/header pinning, Admin writes, token replay/revocation and deletion.
- Unit metadata covers every registered route; it is not proof of every possible service ownership input. HTTP tests and per-feature fixtures remain necessary.
- Paths are relative to /api, except root GET / is excluded from the prefix in production main.ts.
- D02 **Accepted** by user 2026-11-16: all catalog reads require login per BR-001, including read-only POST entity-sync/check. Catalog content is shared among authenticated users; it is not another user's private data.
- ai/health is public liveness `{status:"ok"}`; provider configured/readiness remains available only through authenticated ai/quota.

## Route matrix

| Method | Relative path | Guest | User/owner | Other user | Admin |
| --- | --- | --- | --- | --- | --- |
| DELETE | `/devices/:id` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| DELETE | `/flashcard-folders/:idOrSlug` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| DELETE | `/flashcards/sets/:setId` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| DELETE | `/users/:id` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| GET | `/` | Public, validation applies | Public, validation applies | Public, validation applies | Public, validation applies |
| GET | `/ai/health` | Public, validation applies | Public, validation applies | Public, validation applies | Public, validation applies |
| GET | `/ai/quota` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| GET | `/ai/requests/:id` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| GET | `/app/config` | Public, validation applies | Public, validation applies | Public, validation applies | Public, validation applies |
| GET | `/catalog/:kind` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/catalog/:kind/:identifier` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/catalog/manifest` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/entity-sync/:entityKind/:entityKey` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/flashcard-folders` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| GET | `/flashcard-folders/:idOrSlug` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| GET | `/flashcards/sets` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| GET | `/flashcards/sets/:setId/cards` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| GET | `/flashcards/sync/changes` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| GET | `/grammar-patterns` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/grammar-patterns/:slug` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/grammar-patterns/sub-topic/:grammarSubTopicSlug` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/grammar-sub-topics` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/grammar-sub-topics/:slug` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/grammar-sub-topics/level/:levelCode` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/health` | Public, validation applies | Public, validation applies | Public, validation applies | Public, validation applies |
| GET | `/learning-streaks` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| GET | `/learning-streaks/calendar` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| GET | `/levels` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/levels/:code` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/practice/sets` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| GET | `/practice/sets/:id` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| GET | `/progress/changes` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| GET | `/progress/summary` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| GET | `/topics` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/topics/:identifier` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/topics/:identifier/bundle` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/users` | 401 | 403 | 403 | Allowed; validation applies |
| GET | `/users/:id` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| GET | `/users/me/level` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| GET | `/users/me/settings` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| GET | `/vocabulary-items` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/vocabulary-items/:slug` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/vocabulary-items/sets/all` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/vocabulary-sets` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/vocabulary-sets/:id` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| GET | `/vocabulary-sets/by-topic/:topicId` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| PATCH | `/users/me/level` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| PATCH | `/users/me/profile` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| PATCH | `/users/me/settings` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| POST | `/ai/requests/:id/cancel` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| POST | `/auth/forgot-password` | Public, validation applies | Public, validation applies | Public, validation applies | Public, validation applies |
| POST | `/auth/login` | Public, validation applies | Public, validation applies | Public, validation applies | Public, validation applies |
| POST | `/auth/logout` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| POST | `/auth/me` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| POST | `/auth/refresh` | Public, validation applies | Public, validation applies | Public, validation applies | Public, validation applies |
| POST | `/auth/register` | Public, validation applies | Public, validation applies | Public, validation applies | Public, validation applies |
| POST | `/auth/request-email-verification` | Public, validation applies | Public, validation applies | Public, validation applies | Public, validation applies |
| POST | `/auth/resend-code` | Public, validation applies | Public, validation applies | Public, validation applies | Public, validation applies |
| POST | `/auth/reset-password` | Public, validation applies | Public, validation applies | Public, validation applies | Public, validation applies |
| POST | `/auth/verify-email` | Public, validation applies | Public, validation applies | Public, validation applies | Public, validation applies |
| POST | `/auth/verify-reset-code` | Public, validation applies | Public, validation applies | Public, validation applies | Public, validation applies |
| POST | `/entity-sync/check` | 401 | Authenticated catalog | Authenticated catalog | Authenticated catalog |
| POST | `/flashcard-folders` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| POST | `/flashcards` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| POST | `/flashcards/generate` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| POST | `/flashcards/sync/batch` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| POST | `/grammar-patterns/generate` | 401 | 403 | 403 | Allowed; validation applies |
| POST | `/learning-streaks/activities/batch` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| POST | `/learning-streaks/activity` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| POST | `/practice/fill-blank` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| POST | `/practice/multiple-choice` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| POST | `/practice/submit` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| POST | `/progress/batch` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| POST | `/topics` | 401 | 403 | 403 | Allowed; validation applies |
| POST | `/topics/seed` | 401 | 403 | 403 | Allowed; validation applies |
| POST | `/users/me/avatar` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| POST | `/vocabulary-items/generate` | 401 | 403 | 403 | Allowed; validation applies |
| POST | `/vocabulary-sets` | 401 | 403 | 403 | Allowed; validation applies |
| PUT | `/devices/:id` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| PUT | `/flashcard-folders/:idOrSlug` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| PUT | `/flashcards/bulk` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |
| PUT | `/users/:id` | 401 | Owner scope | Own scope only; foreign ID denied/hidden | Owner scope; Users self/Admin exception |

## Regression contract

The test traverses AppModule imports, including dynamic modules, and checks exact public exceptions. A new unguarded controller or loss of an Admin guard fails the test. Dedicated negative probes verify those failures. There is no global JWT guard: enforcement remains at controllers; the test catches omissions when the suite runs. Public Swagger setup in main.ts is outside controller metadata and remains an operational exposure decision.
