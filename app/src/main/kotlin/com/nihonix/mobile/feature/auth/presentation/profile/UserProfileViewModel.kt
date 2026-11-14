package com.nihonix.mobile.feature.auth.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.auth.domain.repository.AuthRepository
import com.nihonix.mobile.feature.auth.presentation.ProfileUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // Observe local cached user flow
        viewModelScope.launch {
            authRepository.currentUserFlow.collect { cachedUser ->
                if (cachedUser != null) {
                    _uiState.update { it.copy(user = cachedUser, isEmpty = false) }
                }
            }
        }
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = resource.data,
                                errorMessage = null,
                                isEmpty = false
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = resource.message
                            )
                        }
                    }
                    is Resource.Empty -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isEmpty = true
                            )
                        }
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout().collect { res ->
                if (res is Resource.Success) {
                    _uiState.update { it.copy(isLoggedOut = true, user = null) }
                }
            }
        }
    }
}
