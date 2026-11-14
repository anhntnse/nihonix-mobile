package com.nihonix.mobile.feature.auth.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.auth.domain.model.LearningSettings
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.auth.domain.repository.AccountRepository
import com.nihonix.mobile.feature.auth.domain.repository.AuthRepository
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountProfileState(
    val isLoading: Boolean = false, val user: User? = null,
    val errorMessage: String? = null, val isEmpty: Boolean = false,
    val isLoggedOut: Boolean = false, val isSaving: Boolean = false,
    val isEditing: Boolean = false, val username: String = "", val fullName: String = "",
    val message: String? = null, val settings: LearningSettings? = null,
    val isSettingsLoading: Boolean = false, val settingsError: String? = null,
    val dailyGoal: String = "", val reminderTime: String = "", val timezone: String = ""
)

class UserProfileViewModel(
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountProfileState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUserFlow.collect { user -> _uiState.update { it.copy(user = user) } }
        }
        loadUserProfile()
        loadSettings()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { result ->
                when (result) {
                    Resource.Loading -> _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    is Resource.Success -> _uiState.update { it.copy(isLoading = false, user = result.data, isEmpty = false) }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                    Resource.Empty -> _uiState.update { it.copy(isLoading = false, isEmpty = it.user == null) }
                }
            }
        }
    }

    fun beginEdit() { _uiState.update { it.copy(isEditing = true, username = it.user?.username.orEmpty(), fullName = it.user?.fullName.orEmpty(), errorMessage = null, message = null) } }
    fun cancelEdit() { if (!_uiState.value.isSaving) _uiState.update { it.copy(isEditing = false) } }
    fun onUsernameChanged(value: String) { _uiState.update { it.copy(username = value) } }
    fun onFullNameChanged(value: String) { _uiState.update { it.copy(fullName = value) } }
    fun showError(message: String) { _uiState.update { it.copy(errorMessage = message) } }

    fun saveProfile() {
        val state = _uiState.value
        if (state.username.trim().length !in 3..50 || state.fullName.trim().length > 100) {
            showError("Use a username of 3–50 characters and a name of up to 100 characters.")
            return
        }
        mutate(accountRepository.updateProfile(state.username, state.fullName)) { user ->
            _uiState.update { it.copy(user = user, isEditing = false, message = "Profile updated") }
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            accountRepository.getSettings().collect { result ->
                when (result) {
                    Resource.Loading -> _uiState.update { it.copy(isSettingsLoading = true, settingsError = null) }
                    is Resource.Success -> setSettings(result.data)
                    is Resource.Error -> _uiState.update { it.copy(isSettingsLoading = false, settingsError = result.message) }
                    Resource.Empty -> _uiState.update { it.copy(isSettingsLoading = false, settingsError = "Learning settings are unavailable.") }
                }
            }
        }
    }

    private fun setSettings(settings: LearningSettings) {
        _uiState.update { it.copy(settings = settings, isSettingsLoading = false, settingsError = null,
            dailyGoal = settings.dailyGoalMinutes.toString(), reminderTime = settings.reminderTime, timezone = settings.timezone) }
    }
    fun onDailyGoalChanged(value: String) { _uiState.update { it.copy(dailyGoal = value) } }
    fun onReminderTimeChanged(value: String) { _uiState.update { it.copy(reminderTime = value) } }
    fun onTimezoneChanged(value: String) { _uiState.update { it.copy(timezone = value) } }
    fun onPushChanged(value: Boolean) { _uiState.update { it.copy(settings = it.settings?.copy(pushEnabled = value)) } }
    fun onReminderChanged(value: Boolean) { _uiState.update { it.copy(settings = it.settings?.copy(reminderEnabled = value)) } }

    fun saveSettings() {
        val state = _uiState.value
        val settings = state.settings ?: return
        val minutes = state.dailyGoal.toIntOrNull()
        val zone = state.timezone.trim()
        if (minutes == null || minutes !in 1..1440 ||
            !Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(state.reminderTime) ||
            zone !in ZoneId.getAvailableZoneIds() && zone != "UTC"
        ) {
            showError("Enter a goal of 1–1440 minutes, a time as HH:mm, and a valid timezone such as Asia/Ho_Chi_Minh.")
            return
        }
        mutate(accountRepository.updateSettings(settings.copy(dailyGoalMinutes = minutes, reminderTime = state.reminderTime, timezone = zone))) {
            setSettings(it)
            _uiState.update { stateValue -> stateValue.copy(message = "Learning settings saved") }
        }
    }

    fun uploadAvatar(bytes: ByteArray, mimeType: String) {
        mutate(accountRepository.uploadAvatar(bytes, mimeType)) { user ->
            _uiState.update { it.copy(user = user, message = "Avatar updated") }
        }
    }

    fun deleteAccount() {
        mutate(accountRepository.deleteAccount()) { _uiState.update { it.copy(user = null, isLoggedOut = true) } }
    }

    fun logout() {
        mutate(authRepository.logout()) { _uiState.update { it.copy(user = null, isLoggedOut = true) } }
    }

    private fun <T> mutate(operation: Flow<Resource<T>>, success: (T) -> Unit) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null, message = null) }
        viewModelScope.launch {
            operation.collect { result ->
                when (result) {
                    Resource.Loading -> Unit
                    is Resource.Success -> { _uiState.update { it.copy(isSaving = false) }; success(result.data) }
                    is Resource.Error -> _uiState.update { it.copy(isSaving = false, errorMessage = result.message) }
                    Resource.Empty -> _uiState.update { it.copy(isSaving = false, errorMessage = "No response was received. Try again.") }
                }
            }
        }
    }
}
