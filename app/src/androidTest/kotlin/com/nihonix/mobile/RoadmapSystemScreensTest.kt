package com.nihonix.mobile

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.feature.system.*
import com.nihonix.mobile.feature.topics.domain.model.*
import com.nihonix.mobile.feature.topics.presentation.TopicDetailUiState
import com.nihonix.mobile.feature.topics.presentation.topic_detail.TopicBundleDialog
import com.nihonix.mobile.ui.theme.NihonixTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class RoadmapSystemScreensTest {
    @get:Rule val compose = createComposeRule()
    private fun viewModel(json: String): StartupViewModel {
        val api = object : SystemApi {
            override suspend fun config(): Response<ApiResponseDto<AppConfig>> =
                Response.success(Gson().fromJson(json, object : com.google.gson.reflect.TypeToken<ApiResponseDto<AppConfig>>() {}.type))
            override suspend fun health(): Response<okhttp3.ResponseBody> = Response.success(null)
        }
        val cache = object : ConfigCache {
            override fun read(): Pair<AppConfig, Long>? = null
            override fun save(config: AppConfig, time: Long) {}
        }
        return StartupViewModel(StartupRepository(api, cache), 1)
    }
    @Test fun maintenanceBlocksContentAndOffersRetry() {
        val vm = viewModel("""{"data":{"schema_version":1,"maintenance":true,"maintenance_message":"Scheduled work","minimum_version_code":1,"update_url":"https://example.test","cache_ttl_seconds":60}}""")
        compose.setContent { NihonixTheme(dynamicColor = false) { StartupGate(vm) { androidx.compose.material3.Text("Study") } } }
        compose.waitUntil { vm.uiState.value.status == StartupStatus.MAINTENANCE }
        compose.onNodeWithText("Under maintenance").assertIsDisplayed()
        compose.onNodeWithText("Study").assertDoesNotExist()
        compose.onNodeWithText("Continue without checking").assertDoesNotExist()
        compose.onNodeWithText("Retry").assertIsDisplayed()
    }
    @Test fun obsoleteVersionBlocksStudyAndOffersUpdate() {
        val vm = viewModel("""{"data":{"schema_version":1,"maintenance":false,"minimum_version_code":2,"update_url":"https://example.test","cache_ttl_seconds":60}}""")
        compose.setContent { NihonixTheme(dynamicColor = false) { StartupGate(vm) { androidx.compose.material3.Text("Study") } } }
        compose.waitUntil { vm.uiState.value.status == StartupStatus.UPDATE_REQUIRED }
        compose.onNodeWithText("Update required").assertIsDisplayed()
        compose.onNodeWithText("Update app").assertIsDisplayed()
        compose.onNodeWithText("Study").assertDoesNotExist()
        compose.onNodeWithText("Continue without checking").assertDoesNotExist()
    }
    @Test fun unavailableConfigAllowsExplicitContinuation() {
        val vm = viewModel("""{"data":null}""")
        compose.setContent { NihonixTheme(dynamicColor = false) { StartupGate(vm) { androidx.compose.material3.Text("Study") } } }
        compose.waitUntil { vm.uiState.value.status == StartupStatus.UNAVAILABLE }
        compose.onNodeWithText("Continue without checking").performClick()
        compose.onNodeWithText("Study").assertIsDisplayed()
    }
    @Test fun bundleTabsShowContentAndEmptyGrammar() {
        val state = TopicDetailUiState(bundle = TopicBundle(schemaVersion = 1, version = "test",
            vocabularyItems = listOf(CatalogEntry(slug = "hello", kana = "こんにちは", meaning = LocalizedText(en = "Hello")))))
        compose.setContent { NihonixTheme(darkTheme = true, dynamicColor = false) { TopicBundleDialog(state, {}, {}) } }
        compose.onNodeWithText("Hello").assertIsDisplayed()
        compose.onNodeWithText("Grammar").performClick()
        compose.onNodeWithText("No grammar content yet.").assertIsDisplayed()
        compose.onNodeWithText("Back to topic").assertIsDisplayed()
    }
}
