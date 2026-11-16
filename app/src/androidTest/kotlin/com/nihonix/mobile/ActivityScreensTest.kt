package com.nihonix.mobile
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.nihonix.mobile.core.offline.OfflineDatabase
import com.nihonix.mobile.feature.activity.*
import com.nihonix.mobile.ui.theme.NihonixTheme
import org.junit.*
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class ActivityScreensTest{
 @get:Rule val compose=createComposeRule()
 private val context=InstrumentationRegistry.getInstrumentation().targetContext
 private val api=ActivityTestApi()
 private lateinit var db:OfflineDatabase;private lateinit var vm:ActivityViewModel;private val store=ViewModelStore()
 @Before fun open(){
  context.deleteDatabase("sprint08-ui.db");db=Room.databaseBuilder(context,OfflineDatabase::class.java,"sprint08-ui.db").build()
  vm=ActivityViewModel(ActivityRepository(db,api,FlashUsers(),FlashTokens(),Gson(),{}))
  InstrumentationRegistry.getInstrumentation().runOnMainSync{store.put("vm",vm)}
 }
 @After fun close(){InstrumentationRegistry.getInstrumentation().runOnMainSync{store.clear()};db.close();context.deleteDatabase("sprint08-ui.db")}
 @Test fun englishFallbackStreakIsSingularOnVietnameseDeviceLocale(){
  val config=android.content.res.Configuration(context.resources.configuration).apply{setLocale(java.util.Locale.forLanguageTag("vi"))}
  val localized=context.createConfigurationContext(config)
  compose.setContent{
   androidx.compose.runtime.CompositionLocalProvider(
    androidx.compose.ui.platform.LocalContext provides localized,
    androidx.compose.ui.platform.LocalConfiguration provides config
   ){NihonixTheme(dynamicColor=false){ActivityScreen(vm)}}
  }
  compose.waitUntil(timeoutMillis=10000){compose.onAllNodesWithText("Current streak: 1 day").fetchSemanticsNodes().isNotEmpty()}
 }
 @Test fun zeroAndMultipleDaysUsePluralWithoutChangingServerCounts(){
  compose.setContent{NihonixTheme(dynamicColor=false){ActivityScreen(vm)}}
  compose.waitUntil(timeoutMillis=10000){compose.onAllNodesWithText("Current streak: 1 day").fetchSemanticsNodes().isNotEmpty()}
  api.currentStreak=0
  compose.onNodeWithText("Refresh").performClick()
  compose.waitUntil(timeoutMillis=10000){compose.onAllNodesWithText("Current streak: 0 days").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Longest streak: 2 days").assertExists()
  api.currentStreak=2
  compose.onNodeWithText("Refresh").performClick()
  compose.waitUntil(timeoutMillis=10000){compose.onAllNodesWithText("Current streak: 2 days").fetchSemanticsNodes().isNotEmpty()}
 }
 @Test fun showsServerStreakMinutesAndMonthlyCalendar(){
  compose.setContent{androidx.compose.runtime.CompositionLocalProvider(
   androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(context.resources.displayMetrics.density,2f)
  ){NihonixTheme(dynamicColor=false){ActivityScreen(vm)}}}
  compose.waitUntil(timeoutMillis=10000){compose.onAllNodesWithText("Current streak: 1 day").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Today: 1 / 15 minutes").assertExists()
  compose.onNodeWithText("Timezone: Asia/Ho_Chi_Minh").assertExists()
  compose.onNodeWithText("Next month").performScrollTo().performClick()
  compose.onNodeWithText("Previous month").performScrollTo().assertExists()
 }
}
