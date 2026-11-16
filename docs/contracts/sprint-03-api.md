# Sprint 03 roadmap and application configuration

Implemented 2026-11-14 in Android and the existing sibling backend checkout. Local tests do not imply deployment. All routes below use /api and the existing success/message/data/errors/statusCode envelope.

**D02 override — 2026-11-16:** User requires login for every catalog read per SRS/BR-001. Levels/topics/bundles, vocabulary/grammar, /catalog and entity-sync (including POST check) require Bearer JWT. Auth/config/liveness remain public; authoring requires Admin. Android removes catalog No-Authentication, gates cache on current local login and pins background refresh to account/session. Cached shared content remains available offline within that login; 401/403 never falls back. Conditional GET runs after JWT and returns private/no-cache, Vary Authorization. This supersedes earlier public-read statements below. [Decision/evidence](../planning/remaining-work/evidence/catalog-authentication.md).

## Catalog access and counts

Existing public catalog reads are retained for compatibility. Android still uses its existing authenticated navigation; no guest entry screen was added. D02's stricter authenticated-catalog product policy remains unresolved. Catalog mutation routes now require an active verified JWT session and role admin: POST topics, topics/seed, vocabulary-sets, vocabulary-items/generate and grammar-patterns/generate. Guest requests return 401; ordinary users return 403. Admin content seeding is never triggered automatically by this sprint.

GET levels and levels/{code} return the existing code/name/updated_at plus nonnegative integer topic_count, vocabulary_count and grammar_count. Counts refer to topic rows, vocabulary item rows and grammar pattern rows by level_code, not groups. Empty levels have zero counts. Level order is N5 through N1. Missing levels return 404. Android displays a dash when an older server omits counts rather than inventing zero.

GET topics retains levelCode filtering, validates the N5–N1 enum and uses order then id as deterministic ordering. Existing stored ETags are retained; missing-record fallback hashing now uses a fixed timestamp instead of the current clock.

## Complete topic bundle

GET topics/{identifier}/bundle accepts topic UUID or slug. Missing topics return 404. Data contains:

- schema_version: 1.
- version: SHA-256 of the complete ordered content, excluding the version field.
- topic: topic metadata and level.
- vocabulary_sets and vocabulary_items: all sets/items belonging to this topic.
- grammar_sub_topics and grammar_patterns: all groups/patterns belonging to this topic.

Arrays are ordered by order then slug and are empty when content is absent. Groups retain IDs/slugs, localized text and ordering; items retain examples and grammar content retains explanations/conjugation rules. Reads use a REPEATABLE READ transaction for a consistent snapshot. The content version is stable for unchanged content and changes when bundle data changes; it is not yet a persistent-sync cursor, HTTP 304 contract or tombstone mechanism.

Android retrieves the bundle on opening topic content, supports Vocabulary/Grammar tabs and error/retry/empty states, and preserves navigation/filter/search state. Full vocabulary/grammar learning interactions remain Sprint 04, persistent cache/sync FR-027 remains Sprint 05.

## Startup configuration

GET app/config is public:

```json
{"schema_version":1,"maintenance":false,"maintenance_message":"Nihonix is temporarily under maintenance. Please try again later.","minimum_version_code":1,"update_url":"https://play.google.com/store/apps/details?id=com.nihonix.mobile","cache_ttl_seconds":3600}
```

Configuration comes from backend environment variables APP_MAINTENANCE (true/false), APP_MAINTENANCE_MESSAGE (maximum returned length 500), APP_MIN_VERSION_CODE (positive safe integer), APP_UPDATE_URL (HTTPS without URL credentials) and APP_CONFIG_TTL_SECONDS (integer 60–86400). Invalid configuration returns 503 with a generic message. Deploy a valid update destination before raising the minimum version; the default Play URL is a destination template, not evidence that a store listing exists.

Android compares numeric versionCode, never display-version strings. Maintenance takes precedence over an obsolete version. Both states block ordinary navigation and offer retry; update opens the validated HTTPS destination and reports when no handler exists.

Only validated configuration is cached in environment-specific private preferences with the successful fetch timestamp. On refresh failure, fresh cached permissive config may continue with a warning. Expired permissive config or absent/malformed config shows retry plus an explicit Continue without checking action. A saved maintenance/update restriction remains enforced even after expiry until a valid replacement arrives. Future timestamps are not accepted as fresh. A cache-write failure does not invalidate a successful online check.

GET health retains the existing liveness response/envelope; it does not certify database/provider readiness. Android exposes a diagnostic check on the unavailable screen. Health failure never independently blocks study or overrides valid configuration. Content offline availability is still limited by the not-yet-delivered persistent cache.

Successful and failed responses include a server-generated X-Request-Id for diagnostics. Error logs include that ID and omit request bodies/query strings. Existing response envelopes remain compatible.

See [Sprint 03 report](../sprints/sprint-03-report.md) for verification and rollout limits.
