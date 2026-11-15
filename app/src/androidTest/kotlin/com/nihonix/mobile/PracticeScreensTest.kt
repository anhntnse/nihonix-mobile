package com.nihonix.mobile
import androidx.compose.ui.test.*
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.nihonix.mobile.core.offline.OfflineDatabase
import com.nihonix.mobile.feature.practice.data.PracticeRepository
import com.nihonix.mobile.feature.practice.presentation.*
import com.nihonix.mobile.ui.theme.NihonixTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class PracticeScreensTest{
 @get:Rule val compose=createComposeRule()
 private val context=InstrumentationRegistry.getInstrumentation().targetContext
 private lateinit var db:OfflineDatabase
 private lateinit var repository:PracticeRepository
 private lateinit var vm:PracticeViewModel
 private val api=PracticeTestApi()
 private val store=ViewModelStore()
 @Before fun setup(){
  context.deleteDatabase("sprint07-ui.db");db=Room.databaseBuilder(context,OfflineDatabase::class.java,"sprint07-ui.db").build()
  repository=PracticeRepository(db,api,FlashUsers(),FlashTokens(),Gson(),{})
  vm=PracticeViewModel(repository,loadTopics={listOf(PracticeTopic("topic","Animals"))})
  InstrumentationRegistry.getInstrumentation().runOnMainSync{store.put("vm",vm)}
 }
 @After fun close(){InstrumentationRegistry.getInstrumentation().runOnMainSync{store.clear()};db.close();context.deleteDatabase("sprint07-ui.db")}
 private fun content(){compose.setContent{NihonixTheme(dynamicColor=false){PracticeScreen(vm)}}}
 @Test fun quotaPanelDisplaysAuthoritativeAllowanceAndReset(){
  content()
  compose.waitUntil(timeoutMillis=10000){compose.onAllNodesWithText("Practice allowance: 3").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Flashcard allowance: 2").assertExists()
  compose.onNodeWithText("Reset: 00:00 Asia/Ho_Chi_Minh").assertExists()
 }
 @Test fun requiresAnswersThenShowsBackendResults() {
  runBlocking{repository.loadSet(api.set.id)}
  content();compose.runOnIdle{vm.openSet(api.set.id)}
  compose.onNodeWithText("Submit answers").performScrollTo().performClick()
  compose.onNodeWithText("Answer every question before submitting.").assertExists()
  compose.onNodeWithText("Your answer").performTextInput("猫")
  compose.onNodeWithText("Submit answers").performScrollTo().performClick()
  compose.waitUntil(timeoutMillis=10000){compose.onAllNodesWithText("Score: 1/1 (100%)").fetchSemanticsNodes().isNotEmpty()}
  assertEquals(1,api.receipts.size)
 }
 @Test fun staleQuotaIsNotPresentedAsFreshOrZero(){
  content()
  compose.waitUntil(timeoutMillis=10000){compose.onAllNodesWithText("Practice allowance: 3").fetchSemanticsNodes().isNotEmpty()}
  api.quotaUnavailable=true
  compose.onNodeWithText("Refresh allowance").performClick()
  compose.waitUntil(timeoutMillis=10000){compose.onAllNodesWithText("Saved allowance may be out of date.").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Practice allowance: 3").assertExists()
  compose.onNodeWithText("Generate practice").performScrollTo().assertIsNotEnabled()
 }
 @Test fun quotaExceededRetainsTheGenerationForm(){
  content()
  compose.waitUntil(timeoutMillis=10000){compose.onAllNodesWithText("Practice allowance: 3").fetchSemanticsNodes().isNotEmpty()}
  api.quotaExceeded=true
  compose.onNodeWithText("Questions").performScrollTo().performTextClearance()
  compose.onNodeWithText("Questions").performTextInput("1")
  compose.onNodeWithText("Generate practice").performScrollTo().performClick()
  compose.waitUntil(timeoutMillis=10000){compose.onAllNodesWithText("AI allowance used.",substring=true).fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Questions").assertTextContains("1",substring=true)
  assertEquals(0,api.generationCalls)
 }
 @Test fun multipleChoiceResultShowsTheOptionText() {
  api.set=api.set.copy(exercise_type="multiple_choice",exercises=listOf(com.nihonix.mobile.feature.practice.domain.PracticeQuestion(
   "question","multiple_choice",com.nihonix.mobile.feature.practice.domain.QuestionText("Select cat"),
   listOf(com.nihonix.mobile.feature.practice.domain.PracticeOption(1,"猫"),com.nihonix.mobile.feature.practice.domain.PracticeOption(2,"犬")),1)))
  runBlocking{repository.loadSet(api.set.id)}
  content();compose.runOnIdle{vm.openSet(api.set.id)}
  compose.onNodeWithText("猫").performClick()
  compose.onNodeWithText("Submit answers").performScrollTo().performClick()
  compose.waitUntil(timeoutMillis=10000){compose.onAllNodesWithText("Score: 1/1 (100%)").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Your answer: 猫").assertExists()
 }
 @Test fun legacyCompletedPracticeIsReadOnlyAndShowsStoredResults(){
  val gson=Gson()
  api.set=api.set.copy(status="completed",result=null,correct_count=0,accuracy=0,total_time=5,
   exercises=api.set.exercises.map{it.copy(correct_answer=gson.toJsonTree("猫"),user_answer=gson.toJsonTree("犬"),
    is_correct=false,explanation=mapOf("en" to "Cat","vi" to "Mèo"))})
  runBlocking{repository.loadSet(api.set.id)}
  content();compose.runOnIdle{vm.openSet(api.set.id)}
  compose.onNodeWithText("Score: 0/1 (0%)").assertExists()
  compose.onNodeWithText("Your answer: 犬").assertExists()
  compose.onNodeWithText("Correct answer: 猫").assertExists()
  compose.onNodeWithText("Submit answers").assertDoesNotExist()
  compose.onNodeWithText("Completion recorded once.").assertDoesNotExist()
 }
 @Test fun darkLargeTextPracticeRemainsScrollable(){
  runBlocking{repository.loadSet(api.set.id)}
  compose.setContent{androidx.compose.runtime.CompositionLocalProvider(
   androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(androidx.compose.ui.platform.LocalDensity.current.density,1.8f)){
   androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.width(360.dp)){
    NihonixTheme(darkTheme=true,dynamicColor=false){PracticeScreen(vm)}
   }
  }}
  compose.runOnIdle{vm.openSet(api.set.id)}
  compose.onNodeWithText("Your answer").performScrollTo().performClick().performTextInput("猫")
  compose.onNodeWithText("Submit answers").performScrollTo().assertIsDisplayed()
 }
 @Test fun generatedCardsRequireReviewAndSaveAndReplayDoesNotDuplicate(){
  val flash=com.nihonix.mobile.feature.flashcards.data.FlashcardsRepository(db,FlashTestApi(),FlashUsers(),FlashTokens(),Gson(),{})
  val flashVm=com.nihonix.mobile.feature.flashcards.presentation.FlashcardsViewModel(flash)
  InstrumentationRegistry.getInstrumentation().runOnMainSync{store.put("flash",flashVm)}
  val folder=java.util.UUID.randomUUID().toString()
  runBlocking{flash.saveFolder(com.nihonix.mobile.feature.flashcards.domain.FolderDraft(folder,"AI folder"))}
  compose.setContent{NihonixTheme(dynamicColor=false){com.nihonix.mobile.feature.flashcards.presentation.FlashcardsScreen(
   flashVm,{},{},aiViewModel=vm)}}
  compose.onNodeWithText("AI folder").performClick()
  compose.onNodeWithText("Generate with AI").performClick()
  compose.onNodeWithText("AI set name").performScrollTo().performTextInput("Generated animals")
  compose.onNodeWithText("Cards").performScrollTo().performTextClearance()
  compose.onNodeWithText("Cards").performTextInput("1")
  compose.onNodeWithText("Generate card preview").performScrollTo().performClick()
  compose.waitUntil(timeoutMillis=10000){compose.onAllNodesWithText("Review generated cards").fetchSemanticsNodes().isNotEmpty()}
  runBlocking{assertTrue(db.flashcards().sets("account-a").isEmpty())}
  compose.onNodeWithText("Review generated cards").performScrollTo().performClick()
  compose.onNodeWithText("Save set").performClick()
  compose.waitUntil(timeoutMillis=10000){flashVm.uiState.value.snapshot.sets.size==1&&flashVm.uiState.value.editor==null}
  compose.onNodeWithText("Review generated cards").performScrollTo().performClick()
  compose.waitUntil(timeoutMillis=10000){flashVm.uiState.value.notice?.contains("already saved")==true}
  assertEquals(1,api.generationCalls)
  assertEquals(1,flashVm.uiState.value.snapshot.sets.size)
 }
 @Test fun generationRequiresExplicitRequestAndDoesNotSaveFlashcards(){
  content()
  compose.waitUntil(timeoutMillis=10000){compose.onAllNodesWithText("Practice allowance: 3").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Questions").performScrollTo().performTextClearance()
  compose.onNodeWithText("Questions").performTextInput("1")
  compose.onNodeWithText("Generate practice").performScrollTo().performClick()
  compose.waitUntil(timeoutMillis=10000){compose.onAllNodesWithText("Open generated practice").fetchSemanticsNodes().isNotEmpty()}
  assertEquals(1,api.generationCalls)
  runBlocking{assertTrue(db.flashcards().sets("account-a").isEmpty())}
 }
}
