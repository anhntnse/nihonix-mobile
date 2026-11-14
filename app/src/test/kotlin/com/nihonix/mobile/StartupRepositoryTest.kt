package com.nihonix.mobile

import com.google.gson.Gson
import com.nihonix.mobile.feature.system.*
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class StartupRepositoryTest {
    private val server = MockWebServer()
    private class Cache : ConfigCache {
        var value: Pair<AppConfig, Long>? = null
        override fun read() = value
        override fun save(config: AppConfig, time: Long) { value = config to time }
    }
    private val cache = Cache()
    private lateinit var repository: StartupRepository
    @Before fun setup() {
        server.start()
        val api = Retrofit.Builder().baseUrl(server.url("/api/"))
            .client(OkHttpClient()).addConverterFactory(GsonConverterFactory.create(Gson())).build()
            .create(SystemApi::class.java)
        repository = StartupRepository(api, cache) { 1_000_000L }
    }
    @After fun cleanup() { server.shutdown() }
    private fun config(maintenance: Boolean = false, minimum: Long = 1) =
        AppConfig(1, maintenance, "Maintenance", minimum, "https://example.test/update", 60)
    private fun reply(config: AppConfig) {
        server.enqueue(MockResponse().setBody("{\"data\":" + Gson().toJson(config) + "}"))
    }
    @Test fun freshConfigIsStoredAndUsesVersionCode() = runBlocking {
        reply(config(minimum = 12))
        assertEquals(StartupStatus.UPDATE_REQUIRED, repository.load(11).status)
        assertNotNull(cache.value)
        assertEquals("/api/app/config", server.takeRequest().path)
    }
    @Test fun supportedVersionIsReady() = runBlocking {
        reply(config(minimum = 12))
        assertEquals(StartupStatus.READY, repository.load(12).status)
    }
    @Test fun maintenanceTakesPrecedence() = runBlocking {
        reply(config(true, 99))
        assertEquals(StartupStatus.MAINTENANCE, repository.load(1).status)
    }
    @Test fun outageUsesFreshCache() = runBlocking {
        cache.value = config() to 999_000L
        server.enqueue(MockResponse().setResponseCode(503))
        val result = repository.load(1)
        assertEquals(StartupStatus.READY, result.status)
        assertNotNull(result.warning)
    }
    @Test fun expiredPermissiveCacheRequiresExplicitChoice() = runBlocking {
        cache.value = config() to 1L
        server.enqueue(MockResponse().setResponseCode(503))
        assertEquals(StartupStatus.UNAVAILABLE, repository.load(1).status)
    }
    @Test fun expiredKnownRestrictionCannotBeBypassedByOutage() = runBlocking {
        cache.value = config(true) to 1L
        server.enqueue(MockResponse().setResponseCode(503))
        assertEquals(StartupStatus.MAINTENANCE, repository.load(1).status)
    }
    @Test fun futureCacheTimestampIsNotTrustedAsFresh() = runBlocking {
        cache.value = config() to 2_000_000L
        server.enqueue(MockResponse().setResponseCode(503))
        assertEquals(StartupStatus.UNAVAILABLE, repository.load(1).status)
    }
    @Test fun malformedConfigDoesNotReplaceSavedRestriction() = runBlocking {
        cache.value = config(true) to 1L
        server.enqueue(MockResponse().setBody("{\"data\":{\"schema_version\":1}}"))
        assertEquals(StartupStatus.MAINTENANCE, repository.load(1).status)
        assertEquals(true, cache.value?.first?.maintenance)
    }
    @Test fun unsafeUpdateUrlIsRejected() = runBlocking {
        reply(config().copy(updateUrl = "http://example.test/update"))
        assertEquals(StartupStatus.UNAVAILABLE, repository.load(1).status)
        assertNull(cache.value)
    }
    @Test fun unknownSchemaIsRejected() = runBlocking {
        reply(config().copy(schemaVersion = 2))
        assertEquals(StartupStatus.UNAVAILABLE, repository.load(1).status)
    }
    @Test fun healthFailureDoesNotDiscardValidConfig() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        assertFalse(repository.health())
        reply(config())
        assertEquals(StartupStatus.READY, repository.load(1).status)
    }
}
