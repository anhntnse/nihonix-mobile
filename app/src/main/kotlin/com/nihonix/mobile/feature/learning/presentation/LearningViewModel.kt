package com.nihonix.mobile.feature.learning.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.learning.domain.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LearningListState(
    val kind: ContentKind = ContentKind.SETS, val filter: LearningFilter = LearningFilter(),
    val items: List<LearningEntry> = emptyList(), val topics: List<LearningTopic> = emptyList(),
    val loading: Boolean = false, val error: String? = null, val topicsError: String? = null,
    val total: Int = 0, val page: Int = 0, val hasMore: Boolean = false, val language: String = "en", val notice: String? = null
)
class LearningViewModel(private val repository: LearningRepository, private val saved: SavedStateHandle = SavedStateHandle()) : ViewModel() {
    private val state = MutableStateFlow(LearningListState(
        kind = ContentKind.parse(saved["kind"]), filter = LearningFilter(saved["query"] ?: "",
            saved.get<String>("topicId")?.takeIf { it.isNotBlank() },saved.get<String>("levelCode")?.takeIf { it.isNotBlank() },
            saved.get<String>("parentId")?.takeIf { it.isNotBlank() }), language = saved["language"] ?: "en"))
    val uiState = state.asStateFlow()
    private var job: Job? = null
    private var topicJob: Job? = null
    private var revision = 0
    init { refresh(); loadTopics() }
    fun loadTopics() {
        topicJob?.cancel()
        topicJob = viewModelScope.launch {
            when(val result = repository.topics()) {
                is Resource.Success -> state.update { it.copy(topics=result.data,topicsError=null) }
                is Resource.Error -> state.update { it.copy(topicsError=result.message) }
                else -> state.update { it.copy(topicsError="Topic filters are unavailable.") }
            }
        }
    }
    fun language(value: String) {
        if(value !in listOf("en","vi","jp")) return
        saved["language"] = value
        state.update { it.copy(language=value) }
    }
    fun search(value: String) {
        val query = value.take(120)
        saved["query"] = query
        state.update { it.copy(filter=it.filter.copy(query=query)) }
        load(true,300)
    }
    fun level(value: String?) {
        saved["levelCode"] = value
        saved["topicId"] = null
        state.update { it.copy(filter=it.filter.copy(levelCode=value,topicId=null)) }
        refresh()
    }
    fun topic(value: String?) {
        saved["topicId"] = value
        state.update { it.copy(filter=it.filter.copy(topicId=value)) }
        refresh()
    }
    fun refresh() = load(true)
    fun nextPage() { if(!state.value.loading && state.value.hasMore) load(false) }
    private fun load(reset: Boolean, debounce: Long = 0) {
        job?.cancel()
        val generation = ++revision
        val before = state.value
        val next = if(reset) 1 else before.page+1
        state.update { it.copy(loading=true,error=null,
            items=if(reset) emptyList() else it.items, page=if(reset) 0 else it.page,
            total=if(reset) 0 else it.total,hasMore=if(reset) false else it.hasMore) }
        job = viewModelScope.launch {
            if(debounce > 0) delay(debounce)
            val result = repository.page(before.kind,before.filter,next)
            if(generation != revision) return@launch
            when(result) {
                is Resource.Success -> state.update {
                    it.copy(loading=false,error=null,notice=result.message?.takeIf { message -> message.startsWith("Offline") },
                        items=(if(reset) result.data.items else it.items+result.data.items).distinctBy { entry -> entry.key },
                        total=result.data.total,page=result.data.page,hasMore=result.data.hasMore)
                }
                is Resource.Error -> state.update { it.copy(loading=false,error=result.message) }
                else -> state.update { it.copy(loading=false,error="No catalog data was received. Please retry.") }
            }
        }
    }
}
data class LearningDetailState(val entry: LearningEntry? = null,val loading: Boolean = true,val error: String? = null,
    val notice: String? = null, val progress: com.nihonix.mobile.feature.progress.ItemProgress = com.nihonix.mobile.feature.progress.ItemProgress(), val saving: Boolean = false)
class LearningDetailViewModel(private val repository: LearningRepository,private val saved: SavedStateHandle = SavedStateHandle(),
    private val tracker: com.nihonix.mobile.feature.progress.ProgressTracker? = null) : ViewModel() {
    val kind = ContentKind.parse(saved["kind"])
    private val progressKind get() = if (kind == ContentKind.WORDS) "vocabulary" else "grammar"
    private var progressJob: Job? = null
    fun toggleLearned() {
        val current = state.value
        val entry = current.entry ?: return
        val progress = tracker ?: return
        if (current.saving || kind.group) return
        state.update { it.copy(saving=true,error=null) }
        viewModelScope.launch {
            try {
                val result = progress.mark(progressKind, entry, !current.progress.learned)
                state.update { it.copy(saving=false,error=(result as? Resource.Error)?.message) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { state.update { it.copy(saving=false,error="Could not save progress. Please retry.") } }
        }
    }
    private val state = MutableStateFlow(LearningDetailState())
    val uiState = state.asStateFlow()
    private var job: Job? = null
    init { refresh() }
    fun refresh() {
        job?.cancel()
        state.update { it.copy(loading=true,error=null) }
        job = viewModelScope.launch {
            when(val result = repository.detail(kind,saved["identifier"] ?: "")) {
                is Resource.Success -> {
                    state.value=LearningDetailState(entry=result.data,loading=false,notice=result.message?.takeIf { it.startsWith("Offline") })
                    progressJob?.cancel()
                    if (!kind.group) progressJob = viewModelScope.launch {
                        tracker?.observe(progressKind, result.data.slug)?.collect { progress -> state.update { it.copy(progress=progress) } }
                    }
                }
                is Resource.Error -> state.update { it.copy(loading=false,error=result.message) }
                else -> state.update { it.copy(loading=false,error="Content was not found.") }
            }
        }
    }
}
