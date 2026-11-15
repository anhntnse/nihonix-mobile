package com.nihonix.mobile.feature.notifications
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
@Composable fun NotificationPermissionCard(manager:NotificationsManager){
 val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current.lifecycle
 val status by manager.status.collectAsState()
 var granted by remember{mutableStateOf(manager.permissionGranted())}
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){
  granted=manager.permissionGranted();if(!granted)manager.disableLocal();manager.requestReconcile()
 }
 DisposableEffect(lifecycle){
  val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME){
   granted=manager.permissionGranted();if(!granted)manager.disableLocal();manager.requestReconcile()
  }}
  lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer)}
 }
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Text("Notifications on this device",style=MaterialTheme.typography.titleMedium)
  Text(status)
  Text("Save Push notifications in preferences and allow device notifications to receive reminders. Study works without permission.")
  Button(onClick={
   if(Build.VERSION.SDK_INT>=33&&!granted)permission.launch(Manifest.permission.POST_NOTIFICATIONS)
   else manager.requestReconcile()
  }){Text(if(granted)"Refresh device registration" else "Allow notifications")}
  TextButton(onClick={context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))}){Text("Open notification settings")}
 }}
}
