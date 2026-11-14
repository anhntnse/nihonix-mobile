package com.nihonix.mobile.feature.auth.presentation.forgot_password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.auth.domain.repository.AuthRepository
import com.nihonix.mobile.feature.auth.presentation.ForgotPasswordStep
import com.nihonix.mobile.feature.auth.presentation.ForgotPasswordUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onCodeChanged(value: String) {
        _uiState.update { it.copy(code = value, errorMessage = null) }
    }

    fun onNewPasswordChanged(value: String) {
        _uiState.update { it.copy(newPassword = value, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun requestCode() {
        val email = _uiState.value.email.trim()
        if (!com.nihonix.mobile.core.util.ValidationUtils.isValidEmail(email)) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address") }
            return
        }

        viewModelScope.launch {
            authRepository.forgotPassword(email).collect { res ->
                when (res) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                step = ForgotPasswordStep.VERIFY_CODE,
                                infoMessage = res.data
                            )
                        }
                    }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                    is Resource.Empty -> _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun verifyCode() {
        val email = _uiState.value.email.trim()
        val code = _uiState.value.code.trim()

        if (code.isBlank() || code.length != 6) {
            _uiState.update { it.copy(errorMessage = "Please enter the 6-digit verification code") }
            return
        }

        viewModelScope.launch {
            authRepository.verifyResetCode(email, code).collect { res ->
                when (res) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                step = ForgotPasswordStep.RESET_PASSWORD,
                                tempToken = res.data.token,
                                infoMessage = res.data.message
                            )
                        }
                    }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                    is Resource.Empty -> _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun resetPassword() {
        val token = _uiState.value.tempToken
        val newPassword = _uiState.value.newPassword

        if (newPassword.length < 8) {
            _uiState.update { it.copy(errorMessage = "New password must be at least 8 characters") }
            return
        }

        viewModelScope.launch {
            authRepository.resetPassword(token, newPassword).collect { res ->
                when (res) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = true,
                                infoMessage = res.data
                            )
                        }
                    }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                    is Resource.Empty -> _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun resendCode() {
        val email = _uiState.value.email.trim()
        viewModelScope.launch {
            authRepository.resendCode(email).collect { res ->
                when (res) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> _uiState.update { it.copy(isLoading = false, infoMessage = res.data) }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                    is Resource.Empty -> _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}
