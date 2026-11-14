package com.nihonix.mobile.feature.auth.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import coil.compose.AsyncImage
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel,
    onLogout: () -> Unit,
    onNavigateToTopics: (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    LaunchedEffect(state.errorMessage, state.message) {
        if (state.errorMessage != null || state.message != null) scroll.animateScrollTo(0)
    }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var isReadingImage by remember { mutableStateOf(false) }
    val busy = state.isSaving || isReadingImage
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            isReadingImage = true
            try {
                val (bytes, type) = withContext(Dispatchers.IO) { readAvatar(context, uri) }
                viewModel.uploadAvatar(bytes, type)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                viewModel.showError("Unable to read this image. Choose a JPEG, PNG or WebP image up to 10 MB.")
            } finally { isReadingImage = false }
        }
    }
    LaunchedEffect(state.isLoggedOut) { if (state.isLoggedOut) onLogout() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("My Profile") }, navigationIcon = {
                if (onNavigateBack != null) IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }, actions = {
                IconButton(onClick = viewModel::loadUserProfile, enabled = !busy) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh profile")
                }
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(scroll).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.isLoading || busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                if (state.user == null) Button(onClick = viewModel::loadUserProfile) { Text("Retry") }
            }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            val user = state.user
            if (user == null && !state.isLoading && state.errorMessage == null) {
                Text("No profile is available.")
                Button(onClick = viewModel::loadUserProfile) { Text("Reload profile") }
            }
            if (user != null) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(modifier = Modifier.size(96.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primaryContainer) {
                        if (!user.avatarUrl.isNullOrBlank()) {
                            AsyncImage(model = user.avatarUrl, contentDescription = "Your avatar",
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = "Your avatar", modifier = Modifier.size(48.dp))
                        }
                    }
                    TextButton(onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }, enabled = !busy) { Text("Change avatar") }
                    Text(user.fullName?.takeIf { it.isNotBlank() } ?: user.username, style = MaterialTheme.typography.headlineSmall)
                    Text("@${user.username}", style = MaterialTheme.typography.bodyMedium)
                    Text(user.email, style = MaterialTheme.typography.bodyMedium)
                    Text("JLPT ${user.levelCode ?: "not set"}", style = MaterialTheme.typography.labelLarge)
                }

                if (state.isEditing) {
                    OutlinedTextField(value = state.username, onValueChange = viewModel::onUsernameChanged,
                        label = { Text("Username") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = state.fullName, onValueChange = viewModel::onFullNameChanged,
                        label = { Text("Full name") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = viewModel::saveProfile, enabled = !busy) { Text("Save profile") }
                        TextButton(onClick = viewModel::cancelEdit, enabled = !busy) { Text("Cancel") }
                    }
                } else OutlinedButton(onClick = viewModel::beginEdit, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Edit profile")
                }

                HorizontalDivider()
                Text("Learning preferences", style = MaterialTheme.typography.titleLarge)
                if (state.isSettingsLoading) CircularProgressIndicator()
                state.settingsError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::loadSettings, enabled = !busy) { Text("Retry preferences") }
                }
                val settings = state.settings
                if (settings != null) {
                    SettingSwitch("Push notifications", settings.pushEnabled, !busy, viewModel::onPushChanged)
                    SettingSwitch("Daily reminder", settings.reminderEnabled, !busy, viewModel::onReminderChanged)
                    OutlinedTextField(value = state.dailyGoal, onValueChange = viewModel::onDailyGoalChanged,
                        label = { Text("Daily goal (minutes)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = state.reminderTime, onValueChange = viewModel::onReminderTimeChanged,
                        label = { Text("Reminder time (HH:mm)") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = state.timezone, onValueChange = viewModel::onTimezoneChanged,
                        label = { Text("Timezone") }, supportingText = { Text("For example: Asia/Ho_Chi_Minh") },
                        singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                    Button(onClick = viewModel::saveSettings, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Save preferences")
                    }
                }

                HorizontalDivider()
                if (onNavigateToTopics != null) OutlinedButton(onClick = onNavigateToTopics,
                    enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Explore Topics & Levels") }
                OutlinedButton(onClick = viewModel::logout, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
                TextButton(onClick = { confirmDelete = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete account", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { if (!busy) confirmDelete = false },
        title = { Text("Delete your account?") },
        text = { Text("Your account and personal learning data will be permanently deleted. This cannot be undone.") },
        confirmButton = {
            TextButton(onClick = { confirmDelete = false; viewModel.deleteAccount() }, enabled = !busy) {
                Text("Delete account", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }, enabled = !busy) { Text("Cancel") } }
    )
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled,
            modifier = Modifier.semantics { contentDescription = label })
    }
}

private fun readAvatar(context: Context, uri: Uri): Pair<ByteArray, String> {
    val maxBytes = 10 * 1024 * 1024
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= maxBytes)
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    } ?: error("Image unavailable")
    return bytes to (context.contentResolver.getType(uri) ?: "image/jpeg")
}
