# Sprint 04 learning-content API

Implemented 2026-11-15. Existing vocabulary/grammar routes retain their original array/detail contracts. Android uses the new bounded catalog projection below; no AI call occurs during GET requests.

## Routes

Public reads use the existing /api prefix and success/message/data/errors/statusCode envelope.

| Collection kind | List | Detail identifier |
| --- | --- | --- |
| vocabulary-items | GET catalog/vocabulary-items | GET catalog/vocabulary-items/{slug} |
| vocabulary-sets | GET catalog/vocabulary-sets | GET catalog/vocabulary-sets/{UUID} |
| grammar-sub-topics | GET catalog/grammar-sub-topics | GET catalog/grammar-sub-topics/{slug} |
| grammar-patterns | GET catalog/grammar-patterns | GET catalog/grammar-patterns/{slug} |

Data is read from existing PostgreSQL tables. It may have been seeded or generated previously; no per-row provenance field is added. Existing Admin-only authoring routes remain responsible for writes.

## List parameters

| Parameter | Behavior |
| --- | --- |
| page | Integer 1–10000, default 1 |
| page_size | Integer 1–100, default 20; Android requests 20 |
| q | Trimmed keyword, maximum 120 characters; case-insensitive literal substring |
| topic_id | Topic UUID, all collections |
| level_code | N5/N4/N3/N2/N1, all collections |
| set_id | Vocabulary set UUID; vocabulary-items only |
| sub_topic_slug | Grammar parent slug; grammar-patterns only |

Search matches slug and localized title for groups/sets; descriptions for sets; kanji/kana/romaji and vi/en/jp meanings for words; Japanese/romaji structure and vi/en/jp explanations for patterns. It does not perform fuzzy search, kana conversion or accent folding. Percent and underscore are escaped as literal characters. SQL values are parameterized.

Data shape:

```json
{"schema_version":1,"items":[],"total":0,"page":1,"page_size":20,"has_more":false,"version":"<64 lowercase SHA-256 hex characters>"}
```

Ordering is order ASC, slug ASC (slug is unique). Page/count reads use a REPEATABLE READ transaction. Paging has no gaps/duplicates when the underlying catalog is unchanged. Offset pages do not share a database snapshot across requests: refresh the list after catalog updates. Android cancels superseded requests, rejects late responses, resets pagination after filters/search and retains rows/page on append failure for retry.

All entries include topic context. Vocabulary sets expose level_code derived from their topic. Sets and grammar sub-topics include items_count, counting their words/patterns. Group details remain bounded metadata; children use the filtered paginated item/pattern collection.

## Details, versions and errors

Details add schema_version=1 and version, preserve Japanese readings, localized meanings/explanations, examples, conjugation rules, grammar points and difficulty where supplied. Nullable/absent optional content stays optional. Version hashes describe each response projection; they are not global sync cursors, tombstones or HTTP 304 support.

Invalid paging/level/UUID/query fields return 400. Unknown kind, missing content or nonexistent requested parent return 404. Valid filters matching no rows return an empty 200 page, including pages beyond the end. Unsupported parent-filter/collection combinations return 400.

## Authoring integrity and scope

Vocabulary generation now derives level_code from its topic; grammar generation derives it from its sub-topic. An explicitly conflicting level returns 400 before AI is called. Level DTOs restrict N5–N1. Existing authoring guards remain Admin-only and are regression-tested. No AI provider, mail or Cloudinary call is made by the catalog tests.

FR-028–FR-035 and FR-037–FR-043 have local implementation/test evidence. FR-036/FR-044 offline persistence and learning completion events remain Sprint 05. Existing public reads are retained pending the broader D02 product policy; Android learning destinations remain inside authenticated navigation.

See [Sprint 04 report](../sprints/sprint-04-report.md).
