package com.nihonix.mobile

import com.nihonix.mobile.feature.practice.data.isQuotaFresh
import com.nihonix.mobile.feature.practice.data.observeQuotaFreshness
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuotaFreshnessTest {
    @Test fun ttlBoundaryAndFutureClockAreNotTrusted() {
        assertTrue(isQuotaFresh(100_000, 159_999))
        assertFalse(isQuotaFresh(100_000, 160_000))
        assertFalse(isQuotaFresh(100_000, 99_999))
        assertFalse(isQuotaFresh(0, 0))
    }

    @Test fun serverResetInvalidatesEvenARecentCache() {
        assertTrue(isQuotaFresh(100_000, 119_999, "1970-01-01T00:02:00Z"))
        assertFalse(isQuotaFresh(100_000, 120_000, "1970-01-01T00:02:00Z"))
        assertFalse(isQuotaFresh(100_000, 100_001, "invalid"))
    }

    @Test fun idleObserverExpiresAtDeadlineWithoutAnyDatabaseEmission() = runTest {
        val values = mutableListOf<Boolean>()
        val job = launch { observeQuotaFreshness(100_000, null) { 100_000 + testScheduler.currentTime }.collect { values.add(it) } }
        runCurrent()
        assertEquals(listOf(true), values)
        advanceTimeBy(59_999)
        runCurrent()
        assertEquals(listOf(true), values)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(true, false), values)
        job.join()
    }

    @Test fun clockJumpOnResumeInvalidatesAtNextCheck() = runTest {
        var wallTime = 100_000L
        val values = mutableListOf<Boolean>()
        val job = launch { observeQuotaFreshness(100_000, null) { wallTime }.collect { values.add(it) } }
        runCurrent()
        wallTime = 200_000L
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(listOf(true, false), values)
        job.join()
    }

    @Test fun cancellationStopsClockWorkAndNewCollectorRechecksTime() = runTest {
        var reads = 0
        var wallTime = 100_000L
        val flow = observeQuotaFreshness(100_000, null) { reads++; wallTime }
        val first = launch { flow.collect() }
        runCurrent()
        first.cancel()
        first.join()
        val before = reads
        advanceTimeBy(120_000)
        runCurrent()
        assertEquals(before, reads)
        wallTime = 300_000
        val values = mutableListOf<Boolean>()
        flow.collect { values.add(it) }
        assertEquals(listOf(false), values)
    }
}
