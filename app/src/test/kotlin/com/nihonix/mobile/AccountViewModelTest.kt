package com.nihonix.mobile

import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.auth.domain.model.*
import com.nihonix.mobile.feature.auth.domain.repository.*
import com.nihonix.mobile.feature.auth.presentation.profile.UserProfileViewModel
import com.nihonix.mobile.feature.auth.presentation.verification.EmailVerificationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun close() { Dispatchers.resetMain() }

    @Test fun verificationValidatesInputsBeforeCallingTheServer() = runTest(dispatcher) {
        val account = FakeAccount()
        val vm = EmailVerificationViewModel(account)
        vm.onEmailChanged("invalid")
        vm.onCodeChanged("12")
        vm.verify()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        assertEquals(0, account.verifyCalls)
    }

    @Test fun missingAccountDisplaysTheServerErrorWithoutStartingCooldown() = runTest(dispatcher) {
        val account = FakeAccount()
        account.requestResult = Resource.Error("Không tìm thấy tài khoản nào đăng ký với email này.", 404)
        val vm = EmailVerificationViewModel(account) { 1000L }
        vm.initialize("unknown@example.test")
        vm.requestCode()
        advanceUntilIdle()
        assertEquals("Không tìm thấy tài khoản nào đăng ký với email này.", vm.uiState.value.error)
        assertEquals(0L, vm.uiState.value.resendAvailableAt)
        assertNull(vm.uiState.value.message)
        assertFalse(vm.uiState.value.isLoading)
        vm.requestCode()
        advanceUntilIdle()
        assertEquals(2, account.requests)
    }

    @Test fun smtpFailureClearsAnOldSuccessMessageAndAllowsRetry() = runTest(dispatcher) {
        val account = FakeAccount()
        var now = 1000L
        val vm = EmailVerificationViewModel(account) { now }
        vm.initialize("learner@example.test")
        vm.requestCode()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.message)
        now += 120001L
        account.requestResult = Resource.Error("Unable to send the verification email. Please try again.", 503)
        vm.requestCode()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.message)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.resendAvailableAt <= now)
    }

    @Test fun resendUsesServerCooldownAndAllowsRetryAfterItExpires() = runTest(dispatcher) {
        val account = FakeAccount()
        var now = 1000L
        val vm = EmailVerificationViewModel(account) { now }
        vm.initialize("learner@example.test")
        vm.requestCode()
        advanceUntilIdle()
        vm.requestCode()
        advanceUntilIdle()
        assertEquals(1, account.requests)
        now += 120001
        vm.requestCode()
        advanceUntilIdle()
        assertEquals(2, account.requests)
    }

    @Test fun expiredVerificationKeepsTheEmailAndDisplaysTheServerError() = runTest(dispatcher) {
        val account = FakeAccount()
        account.verificationResult = Resource.Error("Code expired", 400)
        val vm = EmailVerificationViewModel(account)
        vm.initialize("learner@example.test")
        vm.onCodeChanged("123456")
        vm.verify()
        advanceUntilIdle()
        assertEquals("Code expired", vm.uiState.value.error)
        assertEquals("learner@example.test", vm.uiState.value.email)
        assertFalse(vm.uiState.value.isVerified)
    }

    @Test fun successfulVerificationCompletesWithoutCreatingALoginSession() = runTest(dispatcher) {
        val account = FakeAccount()
        val vm = EmailVerificationViewModel(account)
        vm.initialize("learner@example.test")
        vm.onCodeChanged("123456")
        vm.verify()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isVerified)
        assertEquals(1, account.verifyCalls)
    }

    @Test fun profileSaveValidatesAndUpdatesTheDisplayedUser() = runTest(dispatcher) {
        val account = FakeAccount()
        val vm = UserProfileViewModel(FakeAuth(), account)
        advanceUntilIdle()
        vm.beginEdit()
        vm.onUsernameChanged("x")
        vm.saveProfile()
        advanceUntilIdle()
        assertEquals(0, account.profileSaves)
        vm.onUsernameChanged("new_name")
        vm.onFullNameChanged("New Name")
        vm.saveProfile()
        advanceUntilIdle()
        assertEquals("new_name", vm.uiState.value.user?.username)
        assertFalse(vm.uiState.value.isEditing)
    }

    @Test fun invalidPreferencesDoNotSendAnUpdate() = runTest(dispatcher) {
        val account = FakeAccount()
        val vm = UserProfileViewModel(FakeAuth(), account)
        advanceUntilIdle()
        vm.onDailyGoalChanged("0")
        vm.onReminderTimeChanged("25:90")
        vm.onTimezoneChanged("not-a-zone")
        vm.saveSettings()
        advanceUntilIdle()
        assertEquals(0, account.settingsSaves)
        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test fun validPreferencesArePersistedThroughTheRepository() = runTest(dispatcher) {
        val account = FakeAccount()
        val vm = UserProfileViewModel(FakeAuth(), account)
        advanceUntilIdle()
        vm.onDailyGoalChanged("30")
        vm.onReminderTimeChanged("21:30")
        vm.onTimezoneChanged("Asia/Ho_Chi_Minh")
        vm.saveSettings()
        advanceUntilIdle()
        assertEquals(30, account.savedSettings?.dailyGoalMinutes)
        assertEquals("Asia/Ho_Chi_Minh", vm.uiState.value.settings?.timezone)
    }

    @Test fun failedDeletionKeepsTheUserAndSuccessfulDeletionLeavesTheProfile() = runTest(dispatcher) {
        val account = FakeAccount()
        account.deletionResult = Resource.Error("Offline")
        val vm = UserProfileViewModel(FakeAuth(), account)
        advanceUntilIdle()
        vm.deleteAccount()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoggedOut)
        assertNotNull(vm.uiState.value.user)
        account.deletionResult = Resource.Success(Unit)
        vm.deleteAccount()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isLoggedOut)
        assertNull(vm.uiState.value.user)
    }

    private class FakeAccount : AccountRepository {
        var requests = 0
        var verifyCalls = 0
        var profileSaves = 0
        var settingsSaves = 0
        var savedSettings: LearningSettings? = null
        var requestResult: Resource<Int> = Resource.Success(120)
        var verificationResult: Resource<Unit> = Resource.Success(Unit)
        var deletionResult: Resource<Unit> = Resource.Success(Unit)
        override fun requestVerification(email: String): Flow<Resource<Int>> { requests++; return flowOf(requestResult) }
        override fun verifyEmail(email: String, code: String): Flow<Resource<Unit>> { verifyCalls++; return flowOf(verificationResult) }
        override fun updateProfile(username: String, fullName: String): Flow<Resource<User>> {
            profileSaves++
            return flowOf(Resource.Success(User("u1", username, "learner@example.test", fullName)))
        }
        override fun getSettings(): Flow<Resource<LearningSettings>> = flowOf(Resource.Success(LearningSettings()))
        override fun updateSettings(settings: LearningSettings): Flow<Resource<LearningSettings>> {
            settingsSaves++; savedSettings = settings; return flowOf(Resource.Success(settings))
        }
        override fun uploadAvatar(bytes: ByteArray, mimeType: String): Flow<Resource<User>> = flowOf(Resource.Error("not used"))
        override fun deleteAccount(): Flow<Resource<Unit>> = flowOf(deletionResult)
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
