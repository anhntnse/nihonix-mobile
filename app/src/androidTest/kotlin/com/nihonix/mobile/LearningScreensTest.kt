package com.nihonix.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.learning.domain.*
import com.nihonix.mobile.feature.learning.presentation.*
import com.nihonix.mobile.feature.topics.domain.model.LocalizedText
import com.nihonix.mobile.ui.theme.NihonixTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LearningScreensTest {
    @get:Rule val compose=createComposeRule()
    private class Repo:LearningRepository {
        var fail=false
        var calls=0
        val word=LearningEntry("hello","hello",kana="こんにちは",meaning=LocalizedText(en="Hello",vi="Xin chào"),
            examples=listOf(LearningExample("こんにちは","konnichiwa","Xin chào","Hello")))
        override suspend fun topics(): Resource<List<LearningTopic>> = Resource.Success(emptyList())
        override suspend fun page(kind:ContentKind,filter:LearningFilter,page:Int): Resource<LearningPage> {
            calls++
            return if(fail) Resource.Error("Connection unavailable",503)
            else Resource.Success(LearningPage(if(filter.query=="missing") emptyList() else listOf(word),
                if(filter.query=="missing") 0 else 1,1,false,"v"))
        }
        override suspend fun detail(kind:ContentKind,identifier:String): Resource<LearningEntry> = Resource.Success(
            when(kind) {
                ContentKind.SETS -> LearningEntry("set1","greetings",title=LocalizedText(en="Greetings"),itemCount=2)
                ContentKind.PATTERNS -> LearningEntry("desu","desu",pattern="です",explanation=LocalizedText(en="Polite ending"),
                    rules=mapOf("present" to "です"),examples=listOf(LearningExample("学生です","gakusei desu","Tôi là học sinh","I am a student")))
                else -> word
            })
    }
    @Test fun setDetailOpensItsWordsWithoutInventingCompletion() {
        val vm=LearningDetailViewModel(Repo(),SavedStateHandle(mapOf("kind" to "vocabulary-sets","identifier" to "set1")))
        var group:String?=null
        compose.setContent { NihonixTheme(dynamicColor=false) { LearningDetailScreen(vm,{}, {group=it.key}) } }
        compose.onNodeWithText("Study words").performScrollTo().performClick()
        assertEquals("set1",group)
    }
    @Test fun grammarDetailShowsExplanationAndConjugation() {
        val vm=LearningDetailViewModel(Repo(),SavedStateHandle(mapOf("kind" to "grammar-patterns","identifier" to "desu")))
        compose.setContent { NihonixTheme(dynamicColor=false) { LearningDetailScreen(vm,{},{}) } }
        compose.onNodeWithText("Polite ending").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("present: です").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("gakusei desu").performScrollTo().assertIsDisplayed()
    }
    @Test fun listOpensTheSelectedWord() {
        val repo=Repo();val vm=LearningViewModel(repo,SavedStateHandle(mapOf("kind" to "vocabulary-items")))
        var selected:String?=null
        compose.setContent { NihonixTheme(dynamicColor=false) { LearningListScreen(vm,{}, {selected=it.key},{_,_->}) } }
        compose.onNodeWithText("こんにちは").performScrollTo().performClick()
        assertEquals("hello",selected)
    }
    @Test fun searchDisplaysAnEmptyStateAndCanBeCleared() {
        val vm=LearningViewModel(Repo(),SavedStateHandle(mapOf("kind" to "vocabulary-items")))
        compose.setContent { NihonixTheme(dynamicColor=false) { LearningListScreen(vm,{},{},{_,_->}) } }
        compose.onNodeWithText("Search Japanese, reading or meaning").performTextInput("missing")
        compose.waitUntil { vm.uiState.value.filter.query=="missing" && !vm.uiState.value.loading }
        compose.onNodeWithText("No content found. Try another search or filter.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Clear").performScrollTo().performClick()
        compose.waitUntil { !vm.uiState.value.loading && vm.uiState.value.items.isNotEmpty() }
        compose.onNodeWithText("こんにちは").performScrollTo().assertIsDisplayed()
    }
    @Test fun networkErrorOffersRetryWithoutLeavingTheScreen() {
        val repo=Repo();repo.fail=true
        val vm=LearningViewModel(repo,SavedStateHandle(mapOf("kind" to "vocabulary-items")))
        compose.setContent { NihonixTheme(dynamicColor=false) { LearningListScreen(vm,{},{},{_,_->}) } }
        compose.onNodeWithText("Connection unavailable").performScrollTo().assertIsDisplayed()
        repo.fail=false
        compose.onNodeWithText("Retry").performScrollTo().performClick()
        compose.onNodeWithText("こんにちは").performScrollTo().assertIsDisplayed()
    }
    @Test fun detailShowsJapaneseExamplesWithLargeTextAndDarkTheme() {
        val vm=LearningDetailViewModel(Repo(),SavedStateHandle(mapOf("kind" to "vocabulary-items","identifier" to "hello")))
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,1.8f)) {
                Box(Modifier.width(375.dp)) {
                    NihonixTheme(darkTheme=true,dynamicColor=false) { LearningDetailScreen(vm,{},{}) }
                }
            }
        }
        compose.onNodeWithText("Meaning").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Examples").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("konnichiwa").performScrollTo().assertIsDisplayed()
    }
}
