package com.nihonix.mobile

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.*
import com.nihonix.mobile.feature.home.*
import com.nihonix.mobile.feature.activity.*
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.core.offline.OutboxMessage
import com.nihonix.mobile.ui.theme.NihonixTheme
import org.junit.*
import org.junit.Assert.*
import java.time.Instant

class HomeScreenTest {
    @get:Rule val compose=createComposeRule()
    private fun state()=HomeUiState("a",HomeSection(User("a","learner","a@example.test",fullName="Anh",levelCode="N5")),
        HomeSection(ActivityState("a",ActivitySummary(1,3,125,2,15,false,true,"2026-11-16","Asia/Ho_Chi_Minh"),
            messages=listOf(OutboxMessage("a","m","activity:m","activity","{}")),cachedAt=42)),
        HomeSection(HomeProgress("a",3,2,1,1)),Instant.parse("2026-11-16T03:00:00Z"))
    @Test fun showsAcceptedTotalsAndPendingCountsSeparately(){
        compose.setContent{NihonixTheme(dynamicColor=false){HomeContent(state(),{}, {})}}
        compose.onNodeWithText("Welcome back, Anh").assertExists()
        compose.onNodeWithText("JLPT N5").assertExists()
        compose.onNodeWithText("2 / 15 minutes").assertExists()
        compose.onNodeWithText("Current streak: 1 day").assertExists()
        compose.onNodeWithText("1 unsynced study event").assertExists()
        compose.onNodeWithText("Accepted server time. Unsynced activity is not included.").assertExists()
        compose.onNodeWithTag("home-list").performScrollToNode(hasText("3 vocabulary · 2 grammar"))
    }
    @Test fun everyShortcutDispatchesItsTypedDestination(){
        val visited=mutableListOf<HomeDestination>()
        compose.setContent{NihonixTheme(dynamicColor=false){HomeContent(state(),{}, {visited+=it})}}
        HomeDestination.entries.forEach{destination->
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("home-"+destination.name))
            compose.onNodeWithTag("home-"+destination.name).performClick()
        }
        assertEquals(HomeDestination.entries.toList(),visited)
    }
    @Test fun errorDoesNotHideShortcutsOrSavedProfileAndRefreshRemainsAvailable(){
        var refreshes=0
        val ui=state().copy(activity=HomeSection(error="Offline. Saved activity retained."))
        compose.setContent{NihonixTheme(dynamicColor=false){HomeContent(ui,{refreshes++},{})}}
        compose.onNodeWithText("Welcome back, Anh").assertExists()
        compose.onNodeWithText("Offline. Saved activity retained.").assertExists()
        compose.onNodeWithText("Refresh").performClick()
        assertEquals(1,refreshes)
        compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("home-Practice"))
        compose.onNodeWithTag("home-Practice").assertIsEnabled()
    }
    @Test fun largeTextDarkNarrowLayoutKeepsAllActionsReachable(){
        var visited:HomeDestination?=null
        compose.setContent{val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,2f)){
                NihonixTheme(darkTheme=true,dynamicColor=false){
                    Box(Modifier.width(320.dp)){HomeContent(state(),{},{visited=it})}
                }
            }
        }
        HomeDestination.entries.forEach{destination->
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("home-"+destination.name))
            compose.onNodeWithTag("home-"+destination.name).assertIsDisplayed().performClick()
            assertEquals(destination,visited)
        }
    }
    @Test fun lazyScrollRestoresAfterStateRecreation(){
        val restoration=StateRestorationTester(compose)
        restoration.setContent{NihonixTheme(dynamicColor=false){HomeContent(state(),{}, {})}}
        compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("home-Topics"))
        compose.onNodeWithTag("home-Topics").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("home-Topics").assertIsDisplayed()
    }
    @Test fun staleDayIsNotPresentedAsTodaysProgress(){
        val initial=state()
        val saved=initial.activity.data!!
        val ui=initial.copy(activity=HomeSection(saved.copy(summary=saved.summary!!.copy(today="2026-11-15"))))
        compose.setContent{NihonixTheme(dynamicColor=false){HomeContent(ui,{}, {})}}
        compose.onNodeWithText("2 / 15 minutes").assertDoesNotExist()
        compose.onNodeWithText("No accepted totals saved for today.").assertExists()
        compose.onNodeWithText("Daily goal: 15 minutes").assertExists()
    }
}
