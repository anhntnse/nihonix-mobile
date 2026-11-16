package com.nihonix.mobile.feature.auth.presentation.navigation

import com.nihonix.mobile.feature.learning.presentation.*
import com.nihonix.mobile.feature.learning.domain.*
import android.net.Uri
import com.nihonix.mobile.feature.home.*
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
    viewModelFactory: androidx.lifecycle.ViewModelProvider.Factory = AppViewModelFactory(),
    authenticated: kotlinx.coroutines.flow.StateFlow<Boolean> = ServiceLocator.tokenStorage.isAuthenticated,
    notificationLaunches: kotlinx.coroutines.flow.StateFlow<Map<String,String>?> = ServiceLocator.notifications.launch,
    acceptNotification: (Map<String,String>)->Boolean = { ServiceLocator.notifications.accepted(it)!=null },
    consumeNotification: ()->Unit = { ServiceLocator.notifications.consumeLaunch() }
) {
    val hasSession by authenticated.collectAsState()
    val notificationLaunch by notificationLaunches.collectAsState()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    LaunchedEffect(notificationLaunch,hasSession,currentRoute) {
        val data=notificationLaunch
        if(data!=null&&currentRoute!=null) {
            if(hasSession&&acceptNotification(data))navController.navigate("activity"){launchSingleTop=true}
            consumeNotification()
        }
    }
    val startDestination = remember {
        if (hasSession) "home" else AuthDestinations.LOGIN
    }

    // A refresh failure or logout removes protected destinations from the back stack.
    LaunchedEffect(hasSession, currentRoute) {
        val protectedRoute = currentRoute == "home" || currentRoute == TopicsDestinations.TOPICS ||
            currentRoute == TopicsDestinations.TOPIC_DETAIL ||
            currentRoute == AuthDestinations.PROFILE ||
            currentRoute == LearningDestinations.LIST || currentRoute == LearningDestinations.DETAIL || currentRoute == "progress" || currentRoute == "activity" || currentRoute?.startsWith("flashcards") == true || currentRoute?.startsWith("practice") == true
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
                    if(authenticated.value)navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
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

        composable("home") {
            val home:HomeViewModel = viewModel(factory=viewModelFactory)
            HomeScreen(home) { destination->
                val route=when(destination) {
                    HomeDestination.Vocabulary->LearningDestinations.list(ContentKind.SETS,LearningFilter(levelCode=home.uiState.value.profile.data?.levelCode))
                    HomeDestination.Grammar->LearningDestinations.list(ContentKind.GRAMMAR_TOPICS,LearningFilter(levelCode=home.uiState.value.profile.data?.levelCode))
                    HomeDestination.Practice->"practice?topicId="
                    HomeDestination.Flashcards->"flashcards"
                    HomeDestination.Progress->"progress"
                    HomeDestination.Calendar->"activity"
                    HomeDestination.Topics->TopicsDestinations.TOPICS
                    HomeDestination.Profile->AuthDestinations.PROFILE
                }
                navController.navigate(route){launchSingleTop=true}
            }
        }

        composable(TopicsDestinations.TOPICS) {
            val topicsViewModel: TopicsViewModel = viewModel(factory = viewModelFactory)
            TopicsScreen(
                viewModel = topicsViewModel,
                onProgress = { navController.navigate("progress") },
                onActivity = { navController.navigate("activity") },
                onFlashcards = { navController.navigate("flashcards") },
                onPractice = { navController.navigate("practice?topicId=") },
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
                onPractice = { topic -> navController.navigate("practice?topicId="+Uri.encode(topic)) },
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
            val studyState by learning.uiState.collectAsState()
            com.nihonix.mobile.feature.activity.StudySession(if(learning.kind==ContentKind.WORDS)"vocabulary" else "grammar",
                studyState.entry?.key.orEmpty(),enabled=!learning.kind.group&&studyState.entry!=null)
            LearningDetailScreen(learning,onBack={navController.popBackStack()},onOpenItems={entry ->
                val child = if(learning.kind==ContentKind.SETS) ContentKind.WORDS else ContentKind.PATTERNS
                navController.navigate(LearningDestinations.list(child,LearningFilter(
                    topicId=entry.topicId,levelCode=entry.levelCode,parentId=entry.key)))
            })
        }

        composable("practice?topicId={topicId}",arguments=listOf(navArgument("topicId"){type=NavType.StringType;defaultValue=""})) {
            val practice:com.nihonix.mobile.feature.practice.presentation.PracticeViewModel=viewModel(factory=viewModelFactory)
            com.nihonix.mobile.feature.practice.presentation.PracticeScreen(practice,onBack={navController.popBackStack()},
                onOpenSet={id->navController.navigate("practice/set/"+Uri.encode(id))})
        }
        composable("practice/set/{sessionId}",arguments=listOf(navArgument("sessionId"){type=NavType.StringType})) {
            val practice:com.nihonix.mobile.feature.practice.presentation.PracticeViewModel=viewModel(factory=viewModelFactory)
            val study by practice.uiState.collectAsState()
            val set=study.snapshot.sets.find{it.id==study.selectedId}
            com.nihonix.mobile.feature.activity.StudySession("practice",study.selectedId.orEmpty(),enabled=set?.status=="in_progress"&&set.exercises.isNotEmpty())
            com.nihonix.mobile.feature.practice.presentation.PracticeScreen(practice,onBack={navController.popBackStack()})
        }

        listOf("flashcards","flashcards/folder/{folderId}","flashcards/folder/{folderId}/set/{setId}").forEach { route ->
            composable(route,arguments=buildList {
                if(route.contains("{folderId}")) add(navArgument("folderId"){type=NavType.StringType})
                if(route.contains("{setId}")) add(navArgument("setId"){type=NavType.StringType})
            }) {
                val flashcards:com.nihonix.mobile.feature.flashcards.presentation.FlashcardsViewModel=viewModel(factory=viewModelFactory)
                val ai:com.nihonix.mobile.feature.practice.presentation.PracticeViewModel=viewModel(factory=viewModelFactory)
                com.nihonix.mobile.feature.flashcards.presentation.FlashcardsScreen(flashcards,aiViewModel=ai,
                    onBack={navController.popBackStack()},
                    onReview={id->navController.navigate("flashcards/review/"+android.net.Uri.encode(id))},
                    onFolder={id->navController.navigate("flashcards/folder/"+android.net.Uri.encode(id))},
                    onSet={id->navController.navigate("flashcards/folder/"+android.net.Uri.encode(flashcards.uiState.value.folderId.orEmpty())+"/set/"+android.net.Uri.encode(id))})
            }
        }
        composable("flashcards/review/{setId}",arguments=listOf(navArgument("setId"){type=NavType.StringType})) {
            val review:com.nihonix.mobile.feature.flashcards.presentation.FlashcardReviewViewModel=viewModel(factory=viewModelFactory)
            val reviewState by review.uiState.collectAsState()
            com.nihonix.mobile.feature.activity.StudySession("flashcards",it.arguments?.getString("setId").orEmpty(),enabled=reviewState.card!=null)
            com.nihonix.mobile.feature.flashcards.presentation.FlashcardReviewScreen(review){navController.popBackStack()}
        }

        composable("activity") {
            val activity:com.nihonix.mobile.feature.activity.ActivityViewModel=viewModel(factory=viewModelFactory)
            com.nihonix.mobile.feature.activity.ActivityScreen(activity){navController.popBackStack()}
        }
        composable("progress") {
            val progress: com.nihonix.mobile.feature.progress.ProgressViewModel = viewModel(factory=viewModelFactory)
            com.nihonix.mobile.feature.progress.ProgressScreen(progress) { navController.popBackStack() }
        }

        composable(AuthDestinations.PROFILE) {
            val profileViewModel: UserProfileViewModel = viewModel(factory = viewModelFactory)
            UserProfileScreen(
                viewModel = profileViewModel,
                notificationControls = { com.nihonix.mobile.feature.notifications.NotificationPermissionCard(ServiceLocator.notifications) },
                onLogout = {
                    navController.navigate(AuthDestinations.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToTopics = {
                    navController.navigate(TopicsDestinations.TOPICS) {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
