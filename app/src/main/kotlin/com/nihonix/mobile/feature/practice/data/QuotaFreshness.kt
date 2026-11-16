package com.nihonix.mobile.feature.practice.data

import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

private const val QUOTA_TTL_MS = 60_000L

/** Server reset is a second deadline; a future/invalid cache must never grant local readiness. */
fun isQuotaFresh(savedAt: Long, now: Long, resetsAt: String? = null): Boolean {
    if (savedAt <= 0 || now < savedAt || now - savedAt >= QUOTA_TTL_MS) return false
    return resetsAt == null || runCatching { now < Instant.parse(resetsAt).toEpochMilli() }.getOrDefault(false)
}

/** Cold, cancellable observer: no work survives its collector or a replaced account/cache snapshot. */
fun observeQuotaFreshness(
    savedAt: Long,
    resetsAt: String?,
    now: () -> Long = System::currentTimeMillis
): Flow<Boolean> = flow {
    while (true) {
        val current = now()
        val fresh = isQuotaFresh(savedAt, current, resetsAt)
        emit(fresh)
        if (!fresh) return@flow
        val ttlRemaining = QUOTA_TTL_MS - (current - savedAt)
        val resetRemaining = resetsAt?.let { Instant.parse(it).toEpochMilli() - current } ?: ttlRemaining
        // Recheck wall time after resume/clock changes without polling Room or the network.
        delay(minOf(1_000L, ttlRemaining, resetRemaining))
    }
}.distinctUntilChanged()
