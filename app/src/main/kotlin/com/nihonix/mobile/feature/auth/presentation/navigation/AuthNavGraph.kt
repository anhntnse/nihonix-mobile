package com.nihonix.mobile.feature.auth.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nihonix.mobile.core.di.AppViewModelFactory
import com.nihonix.mobile.core.di.ServiceLocator
import com.nihonix.mobile.feature.auth.presentation.forgot_password.ForgotPasswordScreen
import com.nihonix.mobile.feature.auth.presentation.forgot_password.ForgotPasswordViewModel
import com.nihonix.mobile.feature.auth.presentation.login.LoginScreen
import com.nihonix.mobile.feature.auth.presentation.login.LoginViewModel
import com.nihonix.mobile.feature.auth.presentation.profile.UserProfileScreen
import com.nihonix.mobile.feature.auth.presentation.profile.UserProfileViewModel
import com.nihonix.mobile.feature.auth.presentation.register.RegisterScreen
import com.nihonix.mobile.feature.auth.presentation.register.RegisterViewModel

object AuthDestinations {
    const val LOGIN = "auth_login"
    const val REGISTER = "auth_register"
    const val FORGOT_PASSWORD = "auth_forgot_password"
    const val PROFILE = "user_profile"
}

@Composable
fun NihonixNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModelFactory: AppViewModelFactory = AppViewModelFactory()
) {
    val isLoggedIn = ServiceLocator.authRepository.isLoggedIn()
    val startDestination = if (isLoggedIn) AuthDestinations.PROFILE else AuthDestinations.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(AuthDestinations.LOGIN) {
            val loginViewModel: LoginViewModel = viewModel(factory = viewModelFactory)
            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToRegister = {
                    navController.navigate(AuthDestinations.REGISTER)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(AuthDestinations.FORGOT_PASSWORD)
                },
                onLoginSuccess = {
                    navController.navigate(AuthDestinations.PROFILE) {
                        popUpTo(AuthDestinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(AuthDestinations.REGISTER) {
            val registerViewModel: RegisterViewModel = viewModel(factory = viewModelFactory)
            RegisterScreen(
                viewModel = registerViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.popBackStack(AuthDestinations.LOGIN, inclusive = false)
                }
            )
        }

        composable(AuthDestinations.FORGOT_PASSWORD) {
            val forgotPasswordViewModel: ForgotPasswordViewModel = viewModel(factory = viewModelFactory)
            ForgotPasswordScreen(
                viewModel = forgotPasswordViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(AuthDestinations.PROFILE) {
            val profileViewModel: UserProfileViewModel = viewModel(factory = viewModelFactory)
            UserProfileScreen(
                viewModel = profileViewModel,
                onLogout = {
                    navController.navigate(AuthDestinations.LOGIN) {
                        popUpTo(AuthDestinations.PROFILE) { inclusive = true }
                    }
                }
            )
        }
    }
}
