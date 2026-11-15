package com.nihonix.mobile.feature.auth.presentation.navigation

import com.nihonix.mobile.feature.learning.presentation.*
import com.nihonix.mobile.feature.learning.domain.*
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nihonix.mobile.feature.auth.presentation.verification.EmailVerificationScreen
import com.nihonix.mobile.feature.auth.presentation.verification.EmailVerificationViewModel
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
    const val VERIFY_EMAIL = "auth_verify_email?email={email}"
    fun verificationRoute(email: String = "") = "auth_verify_email?email=${Uri.encode(email)}"
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
    val hasSession by ServiceLocator.tokenStorage.isAuthenticated.collectAsState()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val startDestination = remember {
        if (hasSession) TopicsDestinations.TOPICS else AuthDestinations.LOGIN
    }

    // A refresh failure or logout removes protected destinations from the back stack.
    LaunchedEffect(hasSession, currentRoute) {
        val protectedRoute = currentRoute == TopicsDestinations.TOPICS ||
            currentRoute == TopicsDestinations.TOPIC_DETAIL ||
            currentRoute == AuthDestinations.PROFILE ||
            currentRoute == LearningDestinations.LIST || currentRoute == LearningDestinations.DETAIL || currentRoute == "progress" || currentRoute?.startsWith("flashcards") == true
        if (!hasSession && protectedRoute) {
            navController.navigate(AuthDestinations.LOGIN) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

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
                onNavigateToVerifyEmail = { navController.navigate(AuthDestinations.verificationRoute()) },
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
                    navController.navigate(AuthDestinations.verificationRoute(registerViewModel.uiState.value.email.trim())) {
                        popUpTo(AuthDestinations.REGISTER) { inclusive = true }
                    }
                }
            )
        }

        composable(
            AuthDestinations.VERIFY_EMAIL,
            arguments = listOf(navArgument("email") { type = NavType.StringType; defaultValue = "" })
        ) { entry ->
            val verification: EmailVerificationViewModel = viewModel(factory = viewModelFactory)
            EmailVerificationScreen(
                viewModel = verification,
                initialEmail = entry.arguments?.getString("email").orEmpty(),
                onBack = { navController.popBackStack() },
                onVerified = {
                    navController.navigate(AuthDestinations.LOGIN) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
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
                onProgress = { navController.navigate("progress") },
                onFlashcards = { navController.navigate("flashcards") },
                onTopicClick = { identifier ->
                    navController.navigate(TopicsDestinations.topicDetailRoute(identifier))
                },
                onNavigateToProfile = {
                    navController.navigate(AuthDestinations.PROFILE)
                },
                onLearning = { vocabulary, level ->
                    navController.navigate(LearningDestinations.list(if(vocabulary) ContentKind.SETS else ContentKind.GRAMMAR_TOPICS,
                        LearningFilter(levelCode=level)))
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
                onLearning = { vocabulary, topic, level ->
                    navController.navigate(LearningDestinations.list(if(vocabulary) ContentKind.SETS else ContentKind.GRAMMAR_TOPICS,
                        LearningFilter(topicId=topic,levelCode=level)))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            LearningDestinations.LIST,
            arguments = listOf(navArgument("kind") { type = NavType.StringType }) +
                listOf("topicId","levelCode","parentId").map { key -> navArgument(key) { type=NavType.StringType; defaultValue="" } }
        ) {
            val learning: LearningViewModel = viewModel(factory=viewModelFactory)
            LearningListScreen(learning,onBack={navController.popBackStack()},
                onEntry={entry -> navController.navigate(LearningDestinations.detail(learning.uiState.value.kind,entry.key))},
                onCollection={kind,filter -> navController.navigate(LearningDestinations.list(kind,filter)) })
        }
        composable(LearningDestinations.DETAIL,
            arguments=listOf(navArgument("kind") { type=NavType.StringType },navArgument("identifier") { type=NavType.StringType })) {
            val learning: LearningDetailViewModel = viewModel(factory=viewModelFactory)
            LearningDetailScreen(learning,onBack={navController.popBackStack()},onOpenItems={entry ->
                val child = if(learning.kind==ContentKind.SETS) ContentKind.WORDS else ContentKind.PATTERNS
                navController.navigate(LearningDestinations.list(child,LearningFilter(
                    topicId=entry.topicId,levelCode=entry.levelCode,parentId=entry.key)))
            })
        }

        listOf("flashcards","flashcards/folder/{folderId}","flashcards/folder/{folderId}/set/{setId}").forEach { route ->
            composable(route,arguments=buildList {
                if(route.contains("{folderId}")) add(navArgument("folderId"){type=NavType.StringType})
                if(route.contains("{setId}")) add(navArgument("setId"){type=NavType.StringType})
            }) {
                val flashcards:com.nihonix.mobile.feature.flashcards.presentation.FlashcardsViewModel=viewModel(factory=viewModelFactory)
                com.nihonix.mobile.feature.flashcards.presentation.FlashcardsScreen(flashcards,
                    onBack={navController.popBackStack()},
                    onReview={id->navController.navigate("flashcards/review/"+android.net.Uri.encode(id))},
                    onFolder={id->navController.navigate("flashcards/folder/"+android.net.Uri.encode(id))},
                    onSet={id->navController.navigate("flashcards/folder/"+android.net.Uri.encode(flashcards.uiState.value.folderId.orEmpty())+"/set/"+android.net.Uri.encode(id))})
            }
        }
        composable("flashcards/review/{setId}",arguments=listOf(navArgument("setId"){type=NavType.StringType})) {
            val review:com.nihonix.mobile.feature.flashcards.presentation.FlashcardReviewViewModel=viewModel(factory=viewModelFactory)
            com.nihonix.mobile.feature.flashcards.presentation.FlashcardReviewScreen(review){navController.popBackStack()}
        }

        composable("progress") {
            val progress: com.nihonix.mobile.feature.progress.ProgressViewModel = viewModel(factory=viewModelFactory)
            com.nihonix.mobile.feature.progress.ProgressScreen(progress) { navController.popBackStack() }
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
