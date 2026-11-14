package com.nihonix.mobile.feature.auth.presentation

import com.nihonix.mobile.feature.auth.domain.model.User

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val validationErrors: List<String>? = null,
    val isSuccess: Boolean = false
)

data class RegisterUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val fullName: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val validationErrors: List<String>? = null,
    val isSuccess: Boolean = false
)

enum class ForgotPasswordStep {
    REQUEST_CODE,
    VERIFY_CODE,
    RESET_PASSWORD
}

data class ForgotPasswordUiState(
    val step: ForgotPasswordStep = ForgotPasswordStep.REQUEST_CODE,
    val email: String = "",
    val code: String = "",
    val tempToken: String = "",
    val newPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val errorMessage: String? = null,
    val isEmpty: Boolean = false,
    val isLoggedOut: Boolean = false
)
