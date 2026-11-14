package com.nihonix.mobile.feature.topics.presentation.topics_list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.auth.domain.repository.AuthRepository
import com.nihonix.mobile.feature.topics.domain.repository.TopicsLevelsRepository
import com.nihonix.mobile.feature.topics.presentation.TopicsUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TopicsViewModel(
    private val repository: TopicsLevelsRepository,
    private val authRepository: AuthRepository,
    private val savedState: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopicsUiState(selectedLevelCode = savedState["selected_level"], searchQuery = savedState["search_query"] ?: ""))
    val uiState: StateFlow<TopicsUiState> = _uiState.asStateFlow()

    private var levelsJob: Job? = null
    private var topicsJob: Job? = null
    private var levelDetailJob: Job? = null
    private var userLevelJob: Job? = null
    private var hasInitializedSelection = savedState.get<Boolean>("selection_initialized") == true

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        loadLevels()
        if (hasInitializedSelection) {
            loadTopics(_uiState.value.selectedLevelCode)
            _uiState.value.selectedLevelCode?.let(::loadLevelDetail)
            if (authRepository.isLoggedIn()) refreshUserLevelOnly()
            return
        }
        if (authRepository.isLoggedIn()) {
            loadUserLevelAndInitialTopics()
        } else {
            hasInitializedSelection = true
            loadTopics(null)
        }
    }

    fun refresh() {
        loadLevels()
        if (authRepository.isLoggedIn()) {
            refreshUserLevelOnly()
        }
        val currentLevel = _uiState.value.selectedLevelCode
        if (currentLevel != null) {
            loadLevelDetail(currentLevel)
        }
        loadTopics(currentLevel)
    }

    private fun loadLevels() {
        levelsJob?.cancel()
        levelsJob = viewModelScope.launch {
            repository.getLevels().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLevelsLoading = true, levelsError = null) }
                    }
                    is Resource.Success -> {
                        _uiState.update { state ->
                            val matchedDetail = state.selectedLevelCode?.let { selected ->
                                resource.data.find { it.code.equals(selected, ignoreCase = true) }
                            } ?: state.selectedLevelDetail
                            state.copy(
                                isLevelsLoading = false,
                                levels = resource.data,
                                levelsError = null,
                                selectedLevelDetail = matchedDetail
                            )
                        }
                    }
                    is Resource.Empty -> {
                        _uiState.update { it.copy(isLevelsLoading = false, levels = emptyList(), levelsError = null) }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLevelsLoading = false, levelsError = resource.message) }
                    }
                }
            }
        }
    }

    private fun loadUserLevelAndInitialTopics() {
        userLevelJob?.cancel()
        userLevelJob = viewModelScope.launch {
            repository.getUserLevel().collect { resource ->
                when (resource) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        val userLevel = resource.data
                        val levelCode = userLevel.levelCode?.takeIf { it.isNotBlank() }
                        if (!hasInitializedSelection) {
                            hasInitializedSelection = true
                            _uiState.update { state ->
                                state.copy(
                                    userLevelCode = levelCode,
                                    selectedLevelCode = levelCode,
                                    selectedLevelDetail = userLevel.level
                                        ?: state.levels.find { it.code.equals(levelCode, ignoreCase = true) }
                                )
                            }
                            if (levelCode != null && userLevel.level == null) {
                                loadLevelDetail(levelCode)
                            }
                            loadTopics(levelCode)
                        } else {
                            _uiState.update { it.copy(userLevelCode = levelCode) }
                        }
                    }
                    is Resource.Error -> {
                        val fallbackCode = authRepository.currentUserFlow.value?.levelCode?.takeIf { it.isNotBlank() }
                        if (!hasInitializedSelection) {
                            hasInitializedSelection = true
                            _uiState.update { state ->
                                state.copy(
                                    userLevelCode = fallbackCode,
                                    selectedLevelCode = fallbackCode,
                                    selectedLevelDetail = state.levels.find { it.code.equals(fallbackCode, ignoreCase = true) }
                                )
                            }
                            loadTopics(fallbackCode)
                        }
                    }
                    is Resource.Empty -> {
                        if (!hasInitializedSelection) {
                            hasInitializedSelection = true
                            loadTopics(null)
                        }
                    }
                }
            }
        }
    }

    private fun refreshUserLevelOnly() {
        userLevelJob?.cancel()
        userLevelJob = viewModelScope.launch {
            repository.getUserLevel().collect { resource ->
                if (resource is Resource.Success) {
                    _uiState.update {
                        it.copy(userLevelCode = resource.data.levelCode?.takeIf { code -> code.isNotBlank() })
                    }
                }
            }
        }
    }

    fun selectLevel(levelCode: String?) {
        hasInitializedSelection = true
        savedState["selection_initialized"] = true
        savedState["selected_level"] = levelCode?.trim()?.takeIf { it.isNotBlank() }?.uppercase()
        val normalized = levelCode?.trim()?.takeIf { it.isNotBlank() }?.uppercase()
        val cachedDetail = normalized?.let { code ->
            _uiState.value.levels.find { it.code.equals(code, ignoreCase = true) }
        }
        _uiState.update {
            it.copy(
                selectedLevelCode = normalized,
                selectedLevelDetail = cachedDetail,
                levelUpdateMessage = null,
                levelUpdateError = null
            )
        }
        if (normalized != null) {
            loadLevelDetail(normalized)
        } else {
            levelDetailJob?.cancel()
        }
        loadTopics(normalized)
    }

    private fun loadLevelDetail(levelCode: String) {
        levelDetailJob?.cancel()
        levelDetailJob = viewModelScope.launch {
            repository.getLevelByCode(levelCode).collect { resource ->
                if (resource is Resource.Success &&
                    _uiState.value.selectedLevelCode.equals(levelCode, ignoreCase = true)
                ) {
                    _uiState.update { it.copy(selectedLevelDetail = resource.data) }
                }
            }
        }
    }

    fun loadTopics(levelCode: String? = _uiState.value.selectedLevelCode) {
        topicsJob?.cancel()
        topicsJob = viewModelScope.launch {
            repository.getTopics(levelCode).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update {
                            it.copy(
                                isTopicsLoading = true,
                                isTopicsEmpty = false,
                                topicsError = null
                            )
                        }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isTopicsLoading = false,
                                topics = resource.data,
                                isTopicsEmpty = resource.data.isEmpty(),
                                topicsError = null
                            )
                        }
                    }
                    is Resource.Empty -> {
                        _uiState.update {
                            it.copy(
                                isTopicsLoading = false,
                                topics = emptyList(),
                                isTopicsEmpty = true,
                                topicsError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isTopicsLoading = false,
                                topicsError = resource.message,
                                isTopicsEmpty = false
                            )
                        }
                    }
                }
            }
        }
    }

    fun updateMyLevel(levelCode: String) {
        val normalized = levelCode.trim().uppercase()
        if (normalized.isBlank()) return

        hasInitializedSelection = true
        viewModelScope.launch {
            repository.updateUserLevel(normalized).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update {
                            it.copy(
                                isUpdatingUserLevel = true,
                                levelUpdateMessage = null,
                                levelUpdateError = null
                            )
                        }
                    }
                    is Resource.Success -> {
                        val updatedCode = resource.data.levelCode ?: normalized
                        _uiState.update { state ->
                            val detail = resource.data.level
                                ?: state.levels.find { it.code.equals(updatedCode, ignoreCase = true) }
                            state.copy(
                                isUpdatingUserLevel = false,
                                userLevelCode = updatedCode,
                                selectedLevelCode = updatedCode,
                                selectedLevelDetail = detail,
                                levelUpdateMessage = resource.message ?: "JLPT level updated to $updatedCode",
                                levelUpdateError = null
                            )
                        }
                        if (resource.data.level == null) {
                            loadLevelDetail(updatedCode)
                        }
                        loadTopics(updatedCode)
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isUpdatingUserLevel = false,
                                levelUpdateError = resource.message
                            )
                        }
                    }
                    is Resource.Empty -> {
                        _uiState.update { it.copy(isUpdatingUserLevel = false) }
                    }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        savedState["search_query"] = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun dismissMessages() {
        _uiState.update {
            it.copy(
                levelUpdateMessage = null,
                levelUpdateError = null
            )
        }
    }
}
