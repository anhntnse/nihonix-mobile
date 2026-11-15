package com.nihonix.mobile.feature.activity
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.nihonix.mobile.core.di.ServiceLocator
import kotlinx.coroutines.*
@Composable fun StudySession(kind:String,item:String,enabled:Boolean=true){
 val user by ServiceLocator.userStorage.currentUserFlow.collectAsState()
 val signedIn by ServiceLocator.tokenStorage.isAuthenticated.collectAsState()
 val lifecycle=LocalLifecycleOwner.current.lifecycle
 LaunchedEffect(user?.id,signedIn,kind,item,enabled,lifecycle){
  val owner=user?.id?:return@LaunchedEffect
  if(!signedIn||!enabled)return@LaunchedEffect
  lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){
   val repository=ServiceLocator.activityRepository
   val settings=withContext(Dispatchers.IO){
    repository.preferences(owner)?:runCatching{repository.refreshPreferences();repository.preferences(owner)}.getOrNull()
   }?:return@repeatOnLifecycle
   if(repository.account()!=owner)return@repeatOnLifecycle
   val lease=ServiceLocator.studyTracker.start(owner,kind,item,settings.timezone)
   try{while(isActive){delay(15000);ServiceLocator.studyTracker.checkpoint(lease)}}
   finally{ServiceLocator.studyTracker.stop(lease)}
  }
 }
}
