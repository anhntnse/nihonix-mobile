package com.nihonix.mobile.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.auth.domain.repository.AuthRepository
import com.nihonix.mobile.feature.auth.presentation.LoginUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChanged(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null, validationErrors = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null, validationErrors = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, validationErrors = null) }
    }

    fun login() {
        val currentState = _uiState.value
        val username = currentState.username.trim()
        val password = currentState.password

        if (username.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Username cannot be empty") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Password cannot be empty") }
            return
        }

        viewModelScope.launch {
            authRepository.login(username, password).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, errorMessage = null, validationErrors = null) }
                    }
                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false, isSuccess = true, errorMessage = null) }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = resource.message,
                                validationErrors = resource.validationErrors
                            )
                        }
                    }
                    is Resource.Empty -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }
}
