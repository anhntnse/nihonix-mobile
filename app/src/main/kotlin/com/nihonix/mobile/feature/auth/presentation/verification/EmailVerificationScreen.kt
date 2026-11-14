package com.nihonix.mobile.feature.auth.presentation.verification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    viewModel: EmailVerificationViewModel, initialEmail: String,
    onBack: () -> Unit, onVerified: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var remaining by remember { mutableIntStateOf(0) }
    LaunchedEffect(initialEmail) { viewModel.initialize(initialEmail) }
    LaunchedEffect(state.isVerified) { if (state.isVerified) onVerified() }
    LaunchedEffect(state.resendAvailableAt) {
        do {
            remaining = ((state.resendAvailableAt - System.currentTimeMillis() + 999) / 1000).toInt().coerceAtLeast(0)
            if (remaining > 0) delay(1000)
        } while (remaining > 0)
    }
    Scaffold(modifier = modifier.fillMaxSize(), topBar = {
        TopAppBar(title = { Text("Verify your email") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Enter the code sent to your email. Codes expire after 15 minutes.")
            OutlinedTextField(value = state.email, onValueChange = viewModel::onEmailChanged,
                label = { Text("Email address") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.code, onValueChange = viewModel::onCodeChanged,
                label = { Text("6-digit code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth())
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.message?.let { Text(it) }
            if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Button(onClick = viewModel::verify, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) { Text("Verify email") }
            OutlinedButton(onClick = viewModel::requestCode, enabled = !state.isLoading && remaining == 0,
                modifier = Modifier.fillMaxWidth()) { Text(if (remaining > 0) "Request another code in ${remaining}s" else "Request a new code") }
            Text("Already verified? Return to sign in.")
            TextButton(onClick = onVerified, enabled = !state.isLoading) { Text("Go to sign in") }
        }
    }
}
