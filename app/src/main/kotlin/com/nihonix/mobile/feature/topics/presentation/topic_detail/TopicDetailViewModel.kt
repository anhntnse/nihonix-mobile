package com.nihonix.mobile.feature.topics.presentation.topic_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.topics.domain.repository.TopicsLevelsRepository
import com.nihonix.mobile.feature.topics.presentation.TopicDetailUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TopicDetailViewModel(
    private val repository: TopicsLevelsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopicDetailUiState())
    val uiState: StateFlow<TopicDetailUiState> = _uiState.asStateFlow()

    private var bundleJob: Job? = null
    private var identifier: String? = null
    fun loadBundle() {
        val key = identifier ?: return
        bundleJob?.cancel()
        bundleJob = viewModelScope.launch {
            repository.getTopicBundle(key).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isBundleLoading = true, bundleError = null) }
                    is Resource.Success -> _uiState.update { it.copy(isBundleLoading = false, bundle = result.data, bundleError = null) }
                    is Resource.Error -> _uiState.update { it.copy(isBundleLoading = false, bundleError = result.message) }
                    is Resource.Empty -> _uiState.update { it.copy(isBundleLoading = false, bundleError = "No bundle was returned.") }
                }
            }
        }
    }
    private var loadJob: Job? = null
    private var levelJob: Job? = null

    fun loadTopicDetail(identifier: String) {
        if (this.identifier != identifier) {
            bundleJob?.cancel()
            _uiState.value = TopicDetailUiState()
        }
        this.identifier = identifier
        loadJob?.cancel()
        levelJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getTopicDetail(identifier).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update {
                            it.copy(
                                isLoading = true,
                                errorMessage = null,
                                isEmpty = false
                            )
                        }
                    }
                    is Resource.Success -> {
                        val topic = resource.data
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                topic = topic,
                                levelDetail = topic.level,
                                isEmpty = false,
                                errorMessage = null
                            )
                        }
                        if (topic.level == null && topic.levelCode.isNotBlank()) {
                            loadFallbackLevelDetail(topic.levelCode)
                        }
                    }
                    is Resource.Empty -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                topic = null,
                                isEmpty = true,
                                errorMessage = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = resource.message,
                                isEmpty = false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadFallbackLevelDetail(levelCode: String) {
        levelJob?.cancel()
        levelJob = viewModelScope.launch {
            repository.getLevelByCode(levelCode).collect { resource ->
                if (resource is Resource.Success) {
                    _uiState.update { it.copy(levelDetail = resource.data) }
                }
            }
        }
    }
}
