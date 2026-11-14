package com.nihonix.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nihonix.mobile.core.network.ApiConstants
import com.nihonix.mobile.core.network.ApiEndpoint
import com.nihonix.mobile.feature.auth.presentation.navigation.NihonixNavHost
import com.nihonix.mobile.ui.theme.NihonixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NihonixTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        if (BuildConfig.DEBUG &&
                            ApiEndpoint.isLocalDevelopment(ApiConstants.DEFAULT_BASE_URL)
                        ) {
                            LocalNetworkAccessNotice(this@MainActivity)
                        }
                        NihonixNavHost(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Optional access to the local development server; navigation is always available. */
@Composable
private fun LocalNetworkAccessNotice(activity: ComponentActivity) {
    if (Build.VERSION.SDK_INT < 37) return

    var permissionRevision by remember { mutableIntStateOf(0) }
    var requestedBefore by rememberSaveable { mutableStateOf(false) }
    var dismissed by rememberSaveable { mutableStateOf(false) }
    var showRationale by remember { mutableStateOf(false) }
    val permission = Manifest.permission.ACCESS_LOCAL_NETWORK
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionRevision++ }

    // Recheck the real OS permission after returning from Settings; don't cache a grant.
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionRevision++
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    key(permissionRevision) {
        if (!dismissed && ContextCompat.checkSelfPermission(activity, permission) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Allow local network access to connect to Nihonix.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            when {
                                ContextCompat.checkSelfPermission(activity, permission) ==
                                    PackageManager.PERMISSION_GRANTED -> permissionRevision++
                                activity.shouldShowRequestPermissionRationale(permission) ->
                                    showRationale = true
                                requestedBefore -> activity.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", activity.packageName, null)
                                    )
                                )
                                else -> {
                                    requestedBefore = true
                                    launcher.launch(permission)
                                }
                            }
                        }) {
                            Text(
                                if (requestedBefore &&
                                    !activity.shouldShowRequestPermissionRationale(permission)
                                ) "Open settings" else "Allow access"
                            )
                        }
                        TextButton(onClick = { dismissed = true }) { Text("Later") }
                    }
                }
            }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Local network access") },
            text = { Text("Nihonix needs this access to reach the local server. You can continue using the app without granting it.") },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    requestedBefore = true
                    launcher.launch(permission)
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) { Text("Cancel") }
            }
        )
    }
}
