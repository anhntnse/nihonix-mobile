package com.nihonix.mobile.core.di

import androidx.lifecycle.ViewModel
import com.nihonix.mobile.feature.auth.presentation.verification.EmailVerificationViewModel
import androidx.lifecycle.ViewModelProvider
import com.nihonix.mobile.feature.auth.presentation.forgot_password.ForgotPasswordViewModel
import com.nihonix.mobile.feature.auth.presentation.login.LoginViewModel
import com.nihonix.mobile.feature.auth.presentation.profile.UserProfileViewModel
import com.nihonix.mobile.feature.auth.presentation.register.RegisterViewModel
import com.nihonix.mobile.feature.topics.presentation.topic_detail.TopicDetailViewModel
import com.nihonix.mobile.feature.topics.presentation.topics_list.TopicsViewModel

@Suppress("UNCHECKED_CAST")
class AppViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val authRepo = ServiceLocator.authRepository
        val topicsLevelsRepo = ServiceLocator.topicsLevelsRepository
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
                UserProfileViewModel(authRepo, ServiceLocator.accountRepository) as T
            }
            modelClass.isAssignableFrom(EmailVerificationViewModel::class.java) -> {
                EmailVerificationViewModel(ServiceLocator.accountRepository) as T
            }
            modelClass.isAssignableFrom(TopicsViewModel::class.java) -> {
                TopicsViewModel(topicsLevelsRepo, authRepo) as T
            }
            modelClass.isAssignableFrom(TopicDetailViewModel::class.java) -> {
                TopicDetailViewModel(topicsLevelsRepo) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
