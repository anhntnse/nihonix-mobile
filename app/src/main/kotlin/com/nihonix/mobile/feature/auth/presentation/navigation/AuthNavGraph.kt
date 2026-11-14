package com.nihonix.mobile.feature.auth.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.nihonix.mobile.feature.topics.presentation.topic_detail.TopicDetailScreen
import com.nihonix.mobile.feature.topics.presentation.topic_detail.TopicDetailViewModel
import com.nihonix.mobile.feature.topics.presentation.topics_list.TopicsScreen
import com.nihonix.mobile.feature.topics.presentation.topics_list.TopicsViewModel

object AuthDestinations {
    const val LOGIN = "auth_login"
    const val REGISTER = "auth_register"
    const val FORGOT_PASSWORD = "auth_forgot_password"
    const val PROFILE = "user_profile"
}

object TopicsDestinations {
    const val TOPICS = "topics_list"
    const val TOPIC_DETAIL = "topic_detail/{identifier}"

    fun topicDetailRoute(identifier: String): String {
        return "topic_detail/${Uri.encode(identifier)}"
    }
}

@Composable
fun NihonixNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModelFactory: AppViewModelFactory = AppViewModelFactory()
) {
    val isLoggedIn = ServiceLocator.authRepository.isLoggedIn()
    val startDestination = if (isLoggedIn) TopicsDestinations.TOPICS else AuthDestinations.LOGIN

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
                    navController.navigate(TopicsDestinations.TOPICS) {
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

        composable(TopicsDestinations.TOPICS) {
            val topicsViewModel: TopicsViewModel = viewModel(factory = viewModelFactory)
            TopicsScreen(
                viewModel = topicsViewModel,
                onTopicClick = { identifier ->
                    navController.navigate(TopicsDestinations.topicDetailRoute(identifier))
                },
                onNavigateToProfile = {
                    navController.navigate(AuthDestinations.PROFILE)
                }
            )
        }

        composable(
            route = TopicsDestinations.TOPIC_DETAIL,
            arguments = listOf(navArgument("identifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val identifier = backStackEntry.arguments?.getString("identifier").orEmpty()
            val detailViewModel: TopicDetailViewModel = viewModel(factory = viewModelFactory)
            TopicDetailScreen(
                identifier = identifier,
                viewModel = detailViewModel,
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
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToTopics = {
                    navController.navigate(TopicsDestinations.TOPICS) {
                        popUpTo(TopicsDestinations.TOPICS) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
