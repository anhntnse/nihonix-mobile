package com.nihonix.mobile.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nihonix.mobile.feature.auth.presentation.forgot_password.ForgotPasswordViewModel
import com.nihonix.mobile.feature.auth.presentation.login.LoginViewModel
import com.nihonix.mobile.feature.auth.presentation.profile.UserProfileViewModel
import com.nihonix.mobile.feature.auth.presentation.register.RegisterViewModel

@Suppress("UNCHECKED_CAST")
class AppViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val authRepo = ServiceLocator.authRepository
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(authRepo) as T
            }
            modelClass.isAssignableFrom(RegisterViewModel::class.java) -> {
                RegisterViewModel(authRepo) as T
            }
            modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java) -> {
                ForgotPasswordViewModel(authRepo) as T
            }
            modelClass.isAssignableFrom(UserProfileViewModel::class.java) -> {
                UserProfileViewModel(authRepo) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
