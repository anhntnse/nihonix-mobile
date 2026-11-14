package com.nihonix.mobile.feature.auth.presentation.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.core.util.ValidationUtils
import com.nihonix.mobile.feature.auth.domain.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmailVerificationState(
    val email: String = "", val code: String = "", val isLoading: Boolean = false,
    val error: String? = null, val message: String? = null,
    val resendAvailableAt: Long = 0, val isVerified: Boolean = false
)

class EmailVerificationViewModel(
    private val repository: AccountRepository,
    private val now: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val _uiState = MutableStateFlow(EmailVerificationState())
    val uiState = _uiState.asStateFlow()

    fun initialize(email: String) {
        if (_uiState.value.email.isBlank()) _uiState.update { it.copy(email = email) }
    }
    fun onEmailChanged(value: String) { _uiState.update { it.copy(email = value, error = null) } }
    fun onCodeChanged(value: String) { _uiState.update { it.copy(code = value.filter(Char::isDigit).take(6), error = null) } }

    fun requestCode() {
        val state = _uiState.value
        if (state.isLoading || now() < state.resendAvailableAt) return
        if (!ValidationUtils.isValidEmail(state.email.trim())) {
            _uiState.update { it.copy(error = "Enter a valid email address.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null, message = null) }
        viewModelScope.launch {
            repository.requestVerification(state.email.trim()).collect { result ->
                when (result) {
                    Resource.Loading -> Unit
                    is Resource.Success -> _uiState.update { it.copy(isLoading = false, message = result.message ?: "Check your email for a verification code.", resendAvailableAt = now() + result.data * 1000L) }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message, resendAvailableAt = if (result.statusCode == 429) now() + 60000 else it.resendAvailableAt) }
                    Resource.Empty -> _uiState.update { it.copy(isLoading = false, error = "Unable to request a code. Try again.") }
                }
            }
        }
    }

    fun verify() {
        val state = _uiState.value
        if (state.isLoading) return
        if (!ValidationUtils.isValidEmail(state.email.trim()) || !Regex("^[0-9]{6}$").matches(state.code)) {
            _uiState.update { it.copy(error = "Enter your email address and the 6-digit code.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null, message = null) }
        viewModelScope.launch {
            repository.verifyEmail(state.email.trim(), state.code).collect { result ->
                when (result) {
                    Resource.Loading -> Unit
                    is Resource.Success -> _uiState.update { it.copy(isLoading = false, isVerified = true) }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    Resource.Empty -> _uiState.update { it.copy(isLoading = false, error = "No verification response. Try again.") }
                }
            }
        }
    }
}
