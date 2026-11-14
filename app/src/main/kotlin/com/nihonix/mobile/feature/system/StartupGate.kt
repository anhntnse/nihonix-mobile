package com.nihonix.mobile.feature.system

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun StartupGate(viewModel: StartupViewModel, content: @Composable () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val health by viewModel.healthStatus.collectAsState()
    val context = LocalContext.current
    var updateError by remember { mutableStateOf(false) }
    when (state.status) {
        StartupStatus.READY -> Column(Modifier.fillMaxSize()) {
            state.warning?.let {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(it)
                        TextButton(onClick = viewModel::refresh) { Text("Check again") }
                    }
                }
            }
            Box(Modifier.weight(1f)) { content() }
        }
        else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (state.status) {
                StartupStatus.LOADING -> { CircularProgressIndicator(); Text("Checking Nihonix availability…") }
                StartupStatus.MAINTENANCE -> {
                    Text("Under maintenance", style = MaterialTheme.typography.headlineSmall)
                    Text(state.config?.maintenanceMessage.orEmpty())
                }
                StartupStatus.UPDATE_REQUIRED -> {
                    Text("Update required", style = MaterialTheme.typography.headlineSmall)
                    Text("Update Nihonix to continue.")
                    Button(onClick = {
                        updateError = runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.config?.updateUrl)))
                        }.isFailure
                    }) { Text("Update app") }
                    if (updateError) Text("No app could open the update link.", color = MaterialTheme.colorScheme.error)
                }
                StartupStatus.UNAVAILABLE -> {
                    Text("Connection unavailable", style = MaterialTheme.typography.headlineSmall)
                    Text(state.warning.orEmpty())
                    TextButton(onClick = viewModel::checkHealth) { Text("Check service") }
                    health?.let { Text(it) }
                    TextButton(onClick = viewModel::continueWithoutCheck) { Text("Continue without checking") }
                }
                else -> Unit
            }
            if (state.status == StartupStatus.MAINTENANCE || state.status == StartupStatus.UPDATE_REQUIRED) {
                state.warning?.let { Text(it) }
            }
            if (state.status != StartupStatus.LOADING)
                Button(onClick = viewModel::refresh) { Text("Retry") }
        }
    }
}
