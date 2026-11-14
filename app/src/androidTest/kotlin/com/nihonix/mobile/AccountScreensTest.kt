package com.nihonix.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.auth.domain.model.*
import com.nihonix.mobile.feature.auth.domain.repository.*
import com.nihonix.mobile.feature.auth.presentation.profile.UserProfileScreen
import com.nihonix.mobile.feature.auth.presentation.profile.UserProfileViewModel
import com.nihonix.mobile.feature.auth.presentation.verification.EmailVerificationScreen
import com.nihonix.mobile.feature.auth.presentation.verification.EmailVerificationViewModel
import com.nihonix.mobile.ui.theme.NihonixTheme
import kotlinx.coroutines.flow.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI checks use only fake repositories; no accounts, mail or profile changes reach a server. */
@RunWith(AndroidJUnit4::class)
class AccountScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun verificationDisplaysInvalidInputAndCompletesWithAValidCode() {
        val account = FakeAccount()
        val viewModel = EmailVerificationViewModel(account)
        var completed = false
        compose.setContent {
            NihonixTheme(dynamicColor = false) {
                EmailVerificationScreen(viewModel, "learner@example.test", {}, { completed = true })
            }
        }
        compose.onNodeWithText("Verify email").performScrollTo().performClick()
        compose.onNodeWithText("Enter your email address and the 6-digit code.").assertExists()
        compose.onNodeWithText("6-digit code").performScrollTo().performTextInput("123456")
        compose.onNodeWithText("Verify email").performScrollTo().performClick()
        compose.waitUntil { completed }
        assertEquals(1, account.verifications)
    }

    @Test fun requestingCodeForAnUnknownEmailShowsTheErrorAndKeepsRetryEnabled() {
        val account = FakeAccount()
        account.requestResult = Resource.Error("Không tìm thấy tài khoản nào đăng ký với email này.", 404)
        val viewModel = EmailVerificationViewModel(account)
        compose.setContent {
            NihonixTheme(dynamicColor = false) {
                EmailVerificationScreen(viewModel, "unknown@example.test", {}, {})
            }
        }
        compose.onNodeWithText("Request a new code").performScrollTo().performClick()
        compose.onNodeWithText("Không tìm thấy tài khoản nào đăng ký với email này.").assertExists()
        compose.onNodeWithText("Request a new code").assertIsEnabled()
    }

    @Test fun profileEditingWorksOnASmallPhoneWithLargeTextAndDarkTheme() {
        val account = FakeAccount()
        val viewModel = UserProfileViewModel(FakeAuth(), account)
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.8f)) {
                Box(Modifier.width(375.dp)) {
                    NihonixTheme(darkTheme = true, dynamicColor = false) {
                        UserProfileScreen(viewModel, onLogout = {})
                    }
                }
            }
        }
        compose.onNodeWithText("Edit profile").performScrollTo().performClick()
        compose.onNodeWithText("Username").performScrollTo().performTextReplacement("updated_name")
        compose.onNodeWithText("Save profile").performScrollTo().performClick()
        compose.onNodeWithText("Profile updated").assertExists()
        assertEquals("updated_name", account.savedUsername)
    }

    @Test fun deletingAnAccountRequiresConfirmationAndCancelDoesNotDelete() {
        val account = FakeAccount()
        val viewModel = UserProfileViewModel(FakeAuth(), account)
        compose.setContent {
            NihonixTheme(dynamicColor = false) { UserProfileScreen(viewModel, onLogout = {}) }
        }
        compose.onNodeWithText("Delete account").performScrollTo().performClick()
        compose.onNodeWithText("Delete your account?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Delete your account?").assertDoesNotExist()
        assertEquals(0, account.deletions)
    }

    private class FakeAccount : AccountRepository {
        var savedUsername: String? = null
        var verifications = 0
        var deletions = 0
        var requestResult: Resource<Int> = Resource.Success(120)
        override fun requestVerification(email: String): Flow<Resource<Int>> = flowOf(requestResult)
        override fun verifyEmail(email: String, code: String): Flow<Resource<Unit>> { verifications++; return flowOf(Resource.Success(Unit)) }
        override fun updateProfile(username: String, fullName: String): Flow<Resource<User>> {
            savedUsername = username
            return flowOf(Resource.Success(User("u1", username, "learner@example.test", fullName)))
        }
        override fun getSettings(): Flow<Resource<LearningSettings>> = flowOf(Resource.Success(LearningSettings()))
        override fun updateSettings(settings: LearningSettings): Flow<Resource<LearningSettings>> = flowOf(Resource.Success(settings))
        override fun uploadAvatar(bytes: ByteArray, mimeType: String): Flow<Resource<User>> = flowOf(Resource.Error("not used"))
        override fun deleteAccount(): Flow<Resource<Unit>> { deletions++; return flowOf(Resource.Success(Unit)) }
    }

    private class FakeAuth : AuthRepository {
        override val currentUserFlow = MutableStateFlow<User?>(User("u1", "learner", "learner@example.test"))
        override fun isLoggedIn() = true
        override fun getCurrentUser(): Flow<Resource<User>> = flowOf(Resource.Success(currentUserFlow.value!!))
        override fun logout(): Flow<Resource<Unit>> = flowOf(Resource.Success(Unit))
        override fun login(username: String, password: String): Flow<Resource<User>> = error("not used")
        override fun register(username: String, email: String, password: String, fullName: String?): Flow<Resource<User>> = error("not used")
        override fun forgotPassword(email: String): Flow<Resource<String>> = error("not used")
        override fun verifyResetCode(email: String, code: String): Flow<Resource<ResetCodeVerification>> = error("not used")
        override fun resetPassword(token: String, password: String): Flow<Resource<String>> = error("not used")
        override fun resendCode(email: String): Flow<Resource<String>> = error("not used")
    }
}
