package com.nihonix.mobile

import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.auth.domain.model.ResetCodeVerification
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.auth.domain.repository.AuthRepository
import com.nihonix.mobile.feature.auth.presentation.ForgotPasswordStep
import com.nihonix.mobile.feature.auth.presentation.forgot_password.ForgotPasswordViewModel
import com.nihonix.mobile.feature.auth.presentation.login.LoginViewModel
import com.nihonix.mobile.feature.auth.presentation.register.RegisterViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loginViewModel_emptyCredentials_showsError() {
        val viewModel = LoginViewModel(fakeRepository)
        viewModel.login()

        assertEquals("Username cannot be empty", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun loginViewModel_validCredentials_successfulLogin() = runTest(testDispatcher) {
        val viewModel = LoginViewModel(fakeRepository)
        viewModel.onUsernameChanged("valid_user")
        viewModel.onPasswordChanged("password123")

        viewModel.login()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun loginViewModel_invalidCredentials_showsBackendError() = runTest(testDispatcher) {
        fakeRepository.shouldFail = true
        fakeRepository.failureMessage = "Invalid credentials"

        val viewModel = LoginViewModel(fakeRepository)
        viewModel.onUsernameChanged("bad_user")
        viewModel.onPasswordChanged("wrong_pass")

        viewModel.login()
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Invalid credentials", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun registerViewModel_shortUsername_showsValidation() {
        val viewModel = RegisterViewModel(fakeRepository)
        viewModel.onUsernameChanged("ab") // min is 3
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("123456")

        viewModel.register()
        assertEquals("Username must be at least 3 characters", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun registerViewModel_shortPassword_showsValidation() {
        val viewModel = RegisterViewModel(fakeRepository)
        viewModel.onUsernameChanged("valid_user")
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("12345") // min is 6

        viewModel.register()
        assertEquals("Password must be at least 6 characters", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun registerViewModel_validInput_successfulRegistration() = runTest(testDispatcher) {
        val viewModel = RegisterViewModel(fakeRepository)
        viewModel.onUsernameChanged("valid_user")
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password123")

        viewModel.register()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun forgotPasswordViewModel_fullFlow() = runTest(testDispatcher) {
        val viewModel = ForgotPasswordViewModel(fakeRepository)

        // Step 1: Request Code
        viewModel.onEmailChanged("test@example.com")
        viewModel.requestCode()
        testScheduler.advanceUntilIdle()

        assertEquals(ForgotPasswordStep.VERIFY_CODE, viewModel.uiState.value.step)

        // Step 2: Verify Code
        viewModel.onCodeChanged("123456")
        viewModel.verifyCode()
        testScheduler.advanceUntilIdle()

        assertEquals(ForgotPasswordStep.RESET_PASSWORD, viewModel.uiState.value.step)
        assertEquals("temp_token_123", viewModel.uiState.value.tempToken)

        // Step 3: Reset Password
        viewModel.onNewPasswordChanged("newPassword123")
        viewModel.resetPassword()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
    }

    // --- Fake Repository for Unit Testing ---
    class FakeAuthRepository : AuthRepository {
        var shouldFail: Boolean = false
        var failureMessage: String = "Operation failed"
        private val _currentUser = MutableStateFlow<User?>(null)
        override val currentUserFlow: StateFlow<User?> = _currentUser

        override fun isLoggedIn(): Boolean = _currentUser.value != null

        override fun login(username: String, password: String): Flow<Resource<User>> = flow {
            emit(Resource.Loading)
            if (shouldFail) {
                emit(Resource.Error(failureMessage, 401))
            } else {
                val user = User(
                    id = "test-id-1",
                    username = username,
                    email = "$username@example.com",
                    role = "user"
                )
                _currentUser.value = user
                emit(Resource.Success(user))
            }
        }

        override fun register(
            username: String,
            email: String,
            password: String,
            fullName: String?
        ): Flow<Resource<User>> = flow {
            emit(Resource.Loading)
            if (shouldFail) {
                emit(Resource.Error(failureMessage, 400))
            } else {
                val user = User(
                    id = "test-id-reg",
                    username = username,
                    email = email,
                    fullName = fullName,
                    role = "user"
                )
                emit(Resource.Success(user))
            }
        }

        override fun logout(): Flow<Resource<Unit>> = flow {
            _currentUser.value = null
            emit(Resource.Success(Unit))
        }

        override fun getCurrentUser(): Flow<Resource<User>> = flow {
            val user = _currentUser.value
            if (user != null) {
                emit(Resource.Success(user))
            } else {
                emit(Resource.Error("No user logged in", 401))
            }
        }

        override fun forgotPassword(email: String): Flow<Resource<String>> = flow {
            emit(Resource.Loading)
            emit(Resource.Success("Code sent to $email"))
        }

        override fun verifyResetCode(email: String, code: String): Flow<Resource<ResetCodeVerification>> = flow {
            emit(Resource.Loading)
            emit(Resource.Success(ResetCodeVerification(valid = true, token = "temp_token_123", message = "Verified")))
        }

        override fun resetPassword(token: String, password: String): Flow<Resource<String>> = flow {
            emit(Resource.Loading)
            emit(Resource.Success("Password reset successfully"))
        }

        override fun resendCode(email: String): Flow<Resource<String>> = flow {
            emit(Resource.Loading)
            emit(Resource.Success("Code resent"))
        }
    }
}
