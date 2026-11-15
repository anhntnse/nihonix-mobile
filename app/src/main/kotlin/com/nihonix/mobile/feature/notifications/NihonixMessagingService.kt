package com.nihonix.mobile.feature.notifications
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nihonix.mobile.core.di.ServiceLocator
class NihonixMessagingService:FirebaseMessagingService(){
 override fun onNewToken(token:String){ServiceLocator.notifications.tokenChanged(token)}
 override fun onMessageReceived(message:RemoteMessage){ServiceLocator.notifications.show(message.data)}
}
