package com.nihonix.mobile

import android.os.Bundle
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
                    NihonixNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}