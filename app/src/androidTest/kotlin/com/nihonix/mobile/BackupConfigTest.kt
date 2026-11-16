package com.nihonix.mobile

import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.nihonix.mobile.core.network.ApiConstants
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.feature.system.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/** Run seed -> bmgr local backup -> clear target app -> restore -> verify, same APK. */
class BackupConfigTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val names = listOf(
        "app_config_" + ApiConstants.DEFAULT_BASE_URL.hashCode(),
        "app_config_123456", "app_config_-654321",
        "nihonix_secure_tokens", "nihonix_tokens_fallback",
        "nihonix_user_storage", "nihonix_push_state"
    )

    @Test fun configurationAndPrivateStateDoNotTransfer(): Unit = runBlocking {
        when (InstrumentationRegistry.getArguments().getString("backupPhase")) {
            "seed" -> {
                names.forEach { name ->
                    assertTrue(context.getSharedPreferences(name, 0).edit()
                        .putString("backup_probe", "private-installation-state").commit())
                }
                val oldConfig = AppConfig(1, true, "Old restriction", 99, "https://example.test/update", 60)
                PreferencesConfigCache(context.getSharedPreferences(names.first(), 0), Gson())
                    .save(oldConfig, 1_000L)
                context.openOrCreateDatabase("backup-config-probe.db", 0, null).use {
                    it.execSQL("CREATE TABLE probe (value TEXT)")
                    it.execSQL("INSERT INTO probe VALUES ('private')")
                }
                // This unexcluded file is a positive control proving restore actually happened.
                context.filesDir.resolve("backup-config-control.txt").writeText("restore-control")
                assertEquals(StartupStatus.MAINTENANCE, oldConfig.restriction(1))
            }
            "verify" -> {
                assertEquals("restore-control", context.filesDir.resolve("backup-config-control.txt").readText())
                names.forEach { name ->
                    assertFalse("Restored private preference: $name",
                        context.getSharedPreferences(name, 0).contains("backup_probe"))
                }
                assertFalse(context.getDatabasePath("backup-config-probe.db").exists())
                val cache = PreferencesConfigCache(context.getSharedPreferences(names.first(), 0), Gson())
                assertNull("Old maintenance/version config transferred", cache.read())
                var available = false
                val api = object : SystemApi {
                    override suspend fun config(): Response<ApiResponseDto<AppConfig>> =
                        if (!available) Response.error(503, "".toResponseBody())
                        else Response.success(ApiResponseDto(success = true,
                            data = AppConfig(1, false, "", 1, "https://example.test/update", 60)))
                    override suspend fun health(): Response<okhttp3.ResponseBody> = error("Not used")
                }
                val repository = StartupRepository(api, cache) { 2_000L }
                assertEquals(StartupStatus.UNAVAILABLE, repository.load(1).status)
                available = true
                assertEquals(StartupStatus.READY, repository.load(1).status)
                assertEquals(false, cache.read()!!.first.maintenance)
                names.forEach { context.getSharedPreferences(it, 0).edit().clear().commit() }
                context.filesDir.resolve("backup-config-control.txt").delete()
            }
            else -> org.junit.Assume.assumeTrue("Requires explicit backupPhase seed/verify", false)
        }
    }
}
