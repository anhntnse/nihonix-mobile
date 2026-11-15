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
 private lateinit var db:OfflineDatabase;private lateinit var vm:ActivityViewModel;private val store=ViewModelStore()
 @Before fun open(){
  context.deleteDatabase("sprint08-ui.db");db=Room.databaseBuilder(context,OfflineDatabase::class.java,"sprint08-ui.db").build()
  vm=ActivityViewModel(ActivityRepository(db,ActivityTestApi(),FlashUsers(),FlashTokens(),Gson(),{}))
  InstrumentationRegistry.getInstrumentation().runOnMainSync{store.put("vm",vm)}
 }
 @After fun close(){InstrumentationRegistry.getInstrumentation().runOnMainSync{store.clear()};db.close();context.deleteDatabase("sprint08-ui.db")}
 @Test fun showsServerStreakMinutesAndMonthlyCalendar(){
  compose.setContent{NihonixTheme(dynamicColor=false){ActivityScreen(vm)}}
  compose.waitUntil(timeoutMillis=10000){compose.onAllNodesWithText("Current streak: 1 days").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Today: 1 / 15 minutes").assertExists()
  compose.onNodeWithText("Timezone: Asia/Ho_Chi_Minh").assertExists()
  compose.onNodeWithText("Next month").performScrollTo().performClick()
  compose.onNodeWithText("Previous month").performScrollTo().assertExists()
 }
}
