package com.nihonix.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.nihonix.mobile.core.offline.OfflineDatabase
import com.nihonix.mobile.feature.flashcards.data.*
import com.nihonix.mobile.feature.flashcards.domain.*
import com.nihonix.mobile.feature.flashcards.presentation.*
import com.nihonix.mobile.ui.theme.NihonixTheme
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FlashcardScreensTest {
    @get:Rule val compose=createComposeRule()
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db:OfflineDatabase
    private lateinit var repository:FlashcardsRepository
    private lateinit var vm:FlashcardsViewModel
    private val store=ViewModelStore()
    private val folder=FolderDraft(UUID.randomUUID().toString(),"Personal")
    private val cards=listOf(CardDraft(UUID.randomUUID().toString(),CardFace("猫"),CardFace("Cat"),1),
        CardDraft(UUID.randomUUID().toString(),CardFace("犬"),CardFace("Dog"),2))
    private val set=SetDraft(UUID.randomUUID().toString(),folder.id,"Animals",cards)
    @Before fun setup() {
        context.deleteDatabase("sprint06-ui.db")
        db=Room.databaseBuilder(context,OfflineDatabase::class.java,"sprint06-ui.db").build()
        repository=FlashcardsRepository(db,FlashTestApi(),FlashUsers(),FlashTokens(),Gson(),{})
        vm=FlashcardsViewModel(repository)
        InstrumentationRegistry.getInstrumentation().runOnMainSync{store.put("browser",vm)}
    }
    @After fun close(){InstrumentationRegistry.getInstrumentation().runOnMainSync{store.clear()};db.close();context.deleteDatabase("sprint06-ui.db")}
    private fun seed()=runBlocking {repository.saveFolder(folder);repository.saveSet(set)}
    private fun content() {
        compose.setContent { NihonixTheme(dynamicColor=false) { FlashcardsScreen(vm,{vm.back()},{}) } }
        compose.waitUntil(timeoutMillis=10_000){!vm.uiState.value.loading}
    }
    private fun waitForSet() {compose.waitUntil(timeoutMillis=10_000){vm.uiState.value.snapshot.sets.any{it.id==set.id}}}
    @Test fun createFolderAndBulkSetValidateThenPersistJapaneseCards() {
        content()
        compose.onNodeWithText("New folder").performClick()
        compose.onNodeWithText("Folder name").performTextInput("旅行")
        compose.onNodeWithText("Save folder").performClick()
        compose.onNodeWithText("旅行").assertIsDisplayed().performClick()
        compose.onNodeWithText("New set").performClick()
        compose.onNodeWithText("Set name").performTextInput("Greetings")
        compose.onNodeWithText("Save set").performClick()
        compose.waitUntil(timeoutMillis=10_000){vm.uiState.value.error?.contains("front")==true}
        assertNotNull(vm.uiState.value.editor)
        compose.onNodeWithText("Front 1").performScrollTo().performTextInput("こんにちは")
        compose.onNodeWithText("Back 1").performScrollTo().performTextInput("Hello")
        compose.onNodeWithText("Add card").performClick()
        compose.onNodeWithText("Front 2").performScrollTo().performTextInput("ありがとう")
        compose.onNodeWithText("Back 2").performScrollTo().performTextInput("Thank you")
        compose.onNodeWithText("Save set").performClick()
        compose.waitUntil(timeoutMillis=10_000){vm.uiState.value.editor==null&&vm.uiState.value.snapshot.sets.size==1}
        compose.onNodeWithText("Greetings").assertIsDisplayed()
        assertEquals(2,vm.uiState.value.snapshot.cards.size)
        assertEquals(2,vm.uiState.value.snapshot.messages.size)
    }
    @Test fun deleteSelectedCardsRequiresConfirmationAndKeepsOtherCards() {
        seed();content();waitForSet()
        compose.onNodeWithText("Personal").performClick()
        compose.onNodeWithText("Animals").performClick()
        compose.onNodeWithContentDescription("Select card 猫").performClick()
        compose.onNodeWithText("Delete selected (1)").performScrollTo().performClick()
        assertEquals(2,vm.uiState.value.snapshot.cards.size)
        compose.onNodeWithText("Delete",useUnmergedTree=true).performClick()
        compose.waitUntil(timeoutMillis=10_000){vm.uiState.value.snapshot.cards.size==1}
        compose.onNodeWithText("犬").assertIsDisplayed()
        compose.onNodeWithText("猫").assertDoesNotExist()
    }
    @Test fun previewNeverWritesUntilExplicitSave() {
        runBlocking{repository.saveFolder(folder)}
        content()
        compose.onNodeWithText("Personal").performClick()
        compose.runOnIdle{vm.previewGenerated(cards)}
        compose.onNodeWithText("Preview cards").assertIsDisplayed()
        assertEquals(1,vm.uiState.value.snapshot.messages.size)
        assertTrue(vm.uiState.value.snapshot.sets.isEmpty())
        compose.onNodeWithText("Save set").performClick()
        compose.waitUntil(timeoutMillis=10_000){vm.uiState.value.editor==null&&vm.uiState.value.snapshot.sets.size==1}
        assertEquals(2,vm.uiState.value.snapshot.cards.size)
    }
    @Test fun replayedAiPreviewDoesNotCreateAnotherSet() {
        runBlocking{repository.saveFolder(folder)}
        content()
        compose.onNodeWithText("Personal").performClick()
        val draft=com.nihonix.mobile.feature.practice.domain.GeneratedCards(UUID.randomUUID().toString(),folder.id,"AI animals",cards)
        compose.runOnIdle{vm.previewGeneratedDraft(draft)}
        compose.onNodeWithText("Preview cards").assertIsDisplayed()
        assertTrue(vm.uiState.value.snapshot.sets.isEmpty())
        compose.onNodeWithText("Save set").performClick()
        compose.waitUntil(timeoutMillis=10_000){vm.uiState.value.editor==null&&vm.uiState.value.snapshot.sets.size==1}
        compose.runOnIdle{vm.previewGeneratedDraft(draft)}
        compose.waitUntil(timeoutMillis=10_000){vm.uiState.value.notice?.contains("already saved")==true}
        assertNull(vm.uiState.value.editor)
        assertEquals(draft.draft_id,vm.uiState.value.snapshot.sets.single().id)
        assertEquals(2,vm.uiState.value.snapshot.messages.size)
    }
    @Test fun reviewFlipPositionAndSideSurviveViewModelRecreation() {
        seed()
        val saved=SavedStateHandle(mapOf("setId" to set.id))
        var active by mutableStateOf(FlashcardReviewViewModel(repository,saved))
        InstrumentationRegistry.getInstrumentation().runOnMainSync{store.put("review",active)}
        compose.setContent { NihonixTheme(dynamicColor=false){FlashcardReviewScreen(active,{})} }
        compose.onNodeWithText("Show answer").performClick()
        compose.onNodeWithText("Cat").assertIsDisplayed()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Show answer").performClick()
        compose.onNodeWithText("Dog").assertIsDisplayed()
        compose.runOnIdle {
            val restored=SavedStateHandle(saved.keys().associateWith{saved.get<Any?>(it)})
            active=FlashcardReviewViewModel(repository,restored);store.put("review",active)
        }
        compose.onNodeWithText("Dog").assertIsDisplayed()
        compose.onNodeWithText("Card 2 of 2").assertIsDisplayed()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Review complete.").assertIsDisplayed()
        compose.onNodeWithText("Restart review").performClick()
        compose.onNodeWithText("猫").assertIsDisplayed()
    }
    @Test fun largeTextDarkEditorKeepsSaveActionVisibleWithJapaneseInput() {
        seed()
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,1.8f)) {
            Box(Modifier.width(375.dp)){NihonixTheme(darkTheme=true,dynamicColor=false){FlashcardsScreen(vm,{vm.back()},{})}}
        }}
        waitForSet()
        compose.runOnIdle{vm.openFolder(folder.id);vm.editSet(set.id)}
        compose.onNodeWithText("Front 1").performScrollTo().performClick()
        compose.onNodeWithText("Save set").assertIsDisplayed()
        compose.onNodeWithText("Back 2").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Save set").performClick()
        compose.waitUntil(timeoutMillis=10_000){vm.uiState.value.editor==null}
    }
    @Test fun largeDraftIsRestoredFromRoomInsteadOfSavedStateBundle() {
        val largeCards=(1..200).map { CardDraft(UUID.randomUUID().toString(),CardFace("猫".repeat(2000)),CardFace("犬".repeat(2000)),it) }
        runBlocking {repository.saveFolder(folder);assertTrue(repository.saveSet(set.copy(cards=largeCards)) is com.nihonix.mobile.core.result.Resource.Success)}
        waitForSet()
        val saved=SavedStateHandle(mapOf("folderId" to folder.id))
        val original=FlashcardsViewModel(repository,saved)
        InstrumentationRegistry.getInstrumentation().runOnMainSync{store.put("draft",original)}
        compose.waitUntil(timeoutMillis=10_000){!original.uiState.value.loading}
        compose.runOnIdle{original.editSet(set.id)}
        compose.waitUntil(timeoutMillis=10_000){original.uiState.value.editor!=null}
        compose.runOnIdle{original.editorName("Unsaved Japanese draft")}
        val id=original.uiState.value.editor!!.id
        runBlocking {
            withTimeout(5000){while(repository.draft("account-a",id)?.contains("Unsaved Japanese draft")!=true)delay(20)}
        }
        lateinit var restored:FlashcardsViewModel
        compose.runOnIdle {
            restored=FlashcardsViewModel(repository,SavedStateHandle(saved.keys().associateWith{saved.get<Any?>(it)}))
            store.put("draft",restored)
        }
        compose.waitUntil(timeoutMillis=10_000){restored.uiState.value.editor?.name=="Unsaved Japanese draft"}
        assertFalse(saved.keys().contains("editorPayload"))
        assertEquals(200,restored.uiState.value.editor!!.cards.size)
        runBlocking { assertTrue(repository.draft("account-a",id)!!.toByteArray(Charsets.UTF_8).size>1024*1024) }
        assertTrue(saved.keys().all{saved.get<Any?>(it) !is SetDraft})
    }
}
