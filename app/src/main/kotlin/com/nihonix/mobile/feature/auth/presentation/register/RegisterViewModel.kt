package com.nihonix.mobile.feature.auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.auth.domain.repository.AuthRepository
import com.nihonix.mobile.feature.auth.presentation.RegisterUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onUsernameChanged(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null, validationErrors = null) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null, validationErrors = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null, validationErrors = null) }
    }

    fun onFullNameChanged(value: String) {
        _uiState.update { it.copy(fullName = value, errorMessage = null, validationErrors = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, validationErrors = null) }
    }

    fun register() {
        val currentState = _uiState.value
        val username = currentState.username.trim()
        val email = currentState.email.trim()
        val password = currentState.password
        val fullName = currentState.fullName.trim()

        if (username.length < 3) {
            _uiState.update { it.copy(errorMessage = "Username must be at least 3 characters") }
            return
        }
        if (!com.nihonix.mobile.core.util.ValidationUtils.isValidEmail(email)) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address") }
            return
        }
        if (password.length < 8 || password.toByteArray(Charsets.UTF_8).size > 72) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 8 characters and at most 72 UTF-8 bytes") }
            return
        }

        viewModelScope.launch {
            authRepository.register(
                username = username,
                email = email,
                password = password,
                fullName = fullName.ifBlank { null }
            ).collect { resource ->
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
