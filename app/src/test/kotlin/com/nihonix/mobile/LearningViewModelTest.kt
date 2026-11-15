package com.nihonix.mobile

import androidx.lifecycle.SavedStateHandle
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.learning.domain.*
import com.nihonix.mobile.feature.learning.presentation.*
import com.nihonix.mobile.feature.topics.domain.model.LocalizedText
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class LearningViewModelTest {
    private val dispatcher=StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun close() { Dispatchers.resetMain() }
    private fun saved()=SavedStateHandle(mapOf("kind" to ContentKind.WORDS.wire))
    private class Repo : LearningRepository {
        val calls=mutableListOf<Pair<LearningFilter,Int>>()
        var pageResult: (LearningFilter,Int) -> Resource<LearningPage> = { _,page ->
            Resource.Success(LearningPage(listOf(LearningEntry("word-$page","word-$page",kana="こんにちは")),2,page,page<2,"version"))
        }
        var detailResult: Resource<LearningEntry> = Resource.Error("Content not found",404)
        var waitForQuery: CompletableDeferred<Resource<LearningPage>>?=null
        override suspend fun page(kind:ContentKind,filter:LearningFilter,page:Int): Resource<LearningPage> {
            calls+=filter to page
            if(filter.query=="old") return withContext(NonCancellable) { waitForQuery!!.await() }
            return pageResult(filter,page)
        }
        override suspend fun detail(kind:ContentKind,identifier:String)=detailResult
        override suspend fun topics(): Resource<List<LearningTopic>> = Resource.Success(listOf(LearningTopic("topic",LocalizedText(en="Greetings"),"N5")))
    }
    @Test fun pagesAppendAndDuplicateLoadMoreIsIgnored()=runTest(dispatcher) {
        val repo=Repo();val vm=LearningViewModel(repo,saved());advanceUntilIdle()
        vm.nextPage();vm.nextPage();advanceUntilIdle()
        assertEquals(listOf(1,2),repo.calls.map{it.second})
        assertEquals(listOf("word-1","word-2"),vm.uiState.value.items.map{it.key})
        assertFalse(vm.uiState.value.hasMore)
    }
    @Test fun appendFailureKeepsRowsAndRetriesTheSamePage()=runTest(dispatcher) {
        val repo=Repo();val vm=LearningViewModel(repo,saved());advanceUntilIdle()
        repo.pageResult={_,_->Resource.Error("Network down",503)}
        vm.nextPage();advanceUntilIdle()
        assertEquals(1,vm.uiState.value.items.size);assertEquals(1,vm.uiState.value.page)
        repo.pageResult={_,page->Resource.Success(LearningPage(listOf(LearningEntry("next","next",kana="はい")),2,page,false,"v"))}
        vm.nextPage();advanceUntilIdle()
        assertEquals(listOf(1,2,2),repo.calls.map{it.second})
        assertNull(vm.uiState.value.error)
    }
    @Test fun searchIsDebouncedAndResetsPagination()=runTest(dispatcher) {
        val repo=Repo();val vm=LearningViewModel(repo,saved());advanceUntilIdle()
        vm.search("ko");vm.search("kon")
        advanceTimeBy(299);runCurrent();assertEquals(1,repo.calls.size)
        advanceUntilIdle()
        assertEquals("kon",repo.calls.last().first.query);assertEquals(1,repo.calls.last().second)
    }
    @Test fun lateResultsFromOldQueryCannotReplaceNewFilter()=runTest(dispatcher) {
        val repo=Repo();repo.waitForQuery=CompletableDeferred()
        val vm=LearningViewModel(repo,saved());advanceUntilIdle()
        vm.search("old");advanceTimeBy(301);runCurrent()
        vm.level("N3");vm.search("new");advanceTimeBy(301);runCurrent()
        repo.waitForQuery!!.complete(Resource.Success(LearningPage(listOf(LearningEntry("stale","stale",kana="古い")),1,1,false,"old")))
        advanceUntilIdle()
        assertEquals("N3",vm.uiState.value.filter.levelCode)
        assertEquals("new",vm.uiState.value.filter.query)
        assertFalse(vm.uiState.value.items.any{it.key=="stale"})
    }
    @Test fun filtersAndLanguageAreRestoredFromSavedState()=runTest(dispatcher) {
        val repo=Repo();val saved=saved();val vm=LearningViewModel(repo,saved);advanceUntilIdle()
        vm.level("N4");vm.topic("topic");vm.search("hello");vm.language("vi");advanceUntilIdle()
        val recreated=LearningViewModel(repo,saved);advanceUntilIdle()
        assertEquals("N4",recreated.uiState.value.filter.levelCode)
        assertEquals("topic",recreated.uiState.value.filter.topicId)
        assertEquals("hello",recreated.uiState.value.filter.query)
        assertEquals("vi",recreated.uiState.value.language)
    }
    @Test fun changingLevelClearsIncompatibleTopic()=runTest(dispatcher) {
        val repo=Repo();val vm=LearningViewModel(repo,saved());advanceUntilIdle()
        vm.topic("topic");vm.level("N3");advanceUntilIdle()
        assertNull(vm.uiState.value.filter.topicId);assertNull(repo.calls.last().first.topicId)
    }
    @Test fun emptyPageIsNotTreatedAsANetworkFailure()=runTest(dispatcher) {
        val repo=Repo();repo.pageResult={_,_->Resource.Success(LearningPage(emptyList(),0,1,false,"v"))}
        val vm=LearningViewModel(repo,saved());advanceUntilIdle()
        assertTrue(vm.uiState.value.items.isEmpty());assertNull(vm.uiState.value.error);assertFalse(vm.uiState.value.loading)
    }
    @Test fun missingDetailCanBeRetried()=runTest(dispatcher) {
        val repo=Repo();val vm=LearningDetailViewModel(repo,SavedStateHandle(mapOf("kind" to "vocabulary-items","identifier" to "hello")))
        advanceUntilIdle();assertEquals("Content not found",vm.uiState.value.error)
        repo.detailResult=Resource.Success(LearningEntry("hello","hello",kana="こんにちは"))
        vm.refresh();advanceUntilIdle();assertNotNull(vm.uiState.value.entry);assertNull(vm.uiState.value.error)
    }
}
