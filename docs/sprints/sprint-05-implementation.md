# Sprint 05 implementation decisions

Execution requested 2026-11-15. Builds on the existing approved Sprint 01 Room/WorkManager design and Sprint 05 backlog; retain current checkouts, Compose/MVVM/manual DI and Nest/TypeORM/PostgreSQL.

1. Cache only public catalog GETs in environment-specific Room storage. Conditional ETags restore the cached payload on 304; failed reads may use saved content with an offline notice. Authoritative manifest inventories identify deleted entries and invalidate affected cached projections. Previously downloaded content remains the offline boundary, not every catalog item automatically.
2. Persist a local progress edit and UUID outbox message atomically. Explicit learned/unlearned buttons are the default completion policy; opening content alone does not complete it. The user explicitly confirmed “Dùng nút Đã học/Bỏ đánh dấu”.
3. Scope all user data, queue/checkpoints and WorkManager jobs by account/environment. Logout hides data and stops protected jobs; unacknowledged edits survive for the same account to resume. Successful account deletion purges account storage. Cache can be shared only because these catalog reads are public.
4. Server batch receipts make mutation retries idempotent; reusing an ID with another payload fails. Conflicts compare ISO client timestamp, then mutation UUID; future clocks beyond five minutes are rejected. Per-account locking orders commits/cursors. Delta checkpoints and acknowledgements apply transactionally; pending newer local edits are not overwritten.
5. Summaries group learned vocabulary/grammar by topic and JLPT level. Deleted catalog references are not counted. WorkManager retries network/5xx with backoff, pauses on missing auth and retains permanent failures for visible retry/discard.
6. Verify server behavior in a temporary PostgreSQL schema; verify Room restart/migration/304/outbox/account races on the emulator and use hermetic unit/network tests. No shared schema migration, production deployment, automatic commit or push is part of this request.

Tasks: conditional HTTP/manifest; progress migration/batch/delta/summary; Room schema/cache; outbox/worker/lifecycle; UI state and progress actions/summaries; tests and SRS/report updates. Room/WorkManager follow the selected architecture rather than introducing a second persistence stack.

## Execution ledger

- Backend policy: failing deterministic conflict tests witnessed, then implementation GREEN (5 policy tests). Batch/delta/summary and HTTP fixtures pass in isolated PostgreSQL.
- Room/cache/worker wiring built successfully; v1 schema exported before adding non-destructive v1→v2 outbox index migration.
- Migration test exposed serialization ABI mismatch (app core1.7.3 versus Room test JSON1.8.1). Aligned app/test serialization1.8.1; migration data-preservation test now GREEN.
- Fresh review found account-switch retry and delayed cache response races. Both cache repro tests failed first, then passed with synchronized sequence/generation guards. Retry ownership repro failed first, then passed with captured owner and expected mutation checks inside the transaction.
- Final lifecycle check found deletion could purge before invalidating the active session, allowing a late sync to recreate rows. A failing callback-order regression demonstrated it; session invalidation now precedes the final purge transaction.
- Same-APK instrumentation seed → force-stop → verify passed with a different PID; cached payload, local progress and stable queued mutation remained. Separate Gradle connected-test invocations reinstall/clear the target app, so they cannot prove process-only recovery.
- Ruling: retain existing checkouts/manual DI as authorized by the user's current IDE/backend workflow; no automatic commit/push. Cost: changes remain uncommitted and require the normal later review/integration step.
- Ruling: authoritative complete inventory plus invalidated cached projections, with a 30-projection background refresh bound, meets the current small-catalog scope. Cost: previously unfetched content is unavailable offline; large catalogs will need snapshot pagination/full-download work.
- Ruling: receipt retention lasts for account lifetime, with no premature expiry. Cost: storage grows until a future explicit compaction/full-resync protocol.
- Shared rollout/real HTTPS/OEM background-device acceptance remains outside local implementation completion. Final commands/counts and remaining limits are recorded in the report.
- After implementation review, the user authorized feature-related commits and push to the current Android/backend branches; this supersedes the original no-automatic-commit scope.
