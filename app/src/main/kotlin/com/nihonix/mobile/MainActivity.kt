package com.nihonix.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.nihonix.mobile.feature.auth.presentation.navigation.NihonixNavHost
import com.nihonix.mobile.ui.theme.NihonixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NihonixTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var localNetworkAllowed by remember {
                        mutableStateOf(Build.VERSION.SDK_INT < 37 ||
                            checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED)
                    }
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted -> localNetworkAllowed = granted }
                    LaunchedEffect(Unit) {
                        if (!localNetworkAllowed) {
                            permissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                        }
                    }
                    if (localNetworkAllowed) {
                        NihonixNavHost(modifier = Modifier.padding(innerPadding))
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Allow local network access to connect to Nihonix.")
                            Button(onClick = {
                                permissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                            }) { Text("Allow access") }
                        }
                    }
                }
            }
        }
    }
}