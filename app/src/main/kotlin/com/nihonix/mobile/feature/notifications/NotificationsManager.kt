package com.nihonix.mobile.feature.notifications
import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.JsonParser
import com.nihonix.mobile.MainActivity
import com.nihonix.mobile.core.storage.*
import com.nihonix.mobile.core.network.interceptor.SessionRequest
import com.nihonix.mobile.feature.activity.ActivityRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
class NotificationsManager(private val context:Context,environment:String,private val api:NotificationsApi,
 private val users:UserStorage,private val tokens:TokenStorage,private val activities:ActivityRepository){
 private val prefs=context.getSharedPreferences("nihonix_push_state",Context.MODE_PRIVATE)
 private val prefix=environment+":"
 private val mutex=Mutex();private var revision=0L
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
 private val _status=MutableStateFlow("Notifications are not registered.")
 val status=_status.asStateFlow()
 private val _launch=MutableStateFlow<Map<String,String>?>(null)
 val launch=_launch.asStateFlow()
 private fun key(name:String)=prefix+name
 private fun owner()=users.getUser()?.id?.takeIf{tokens.hasAccessToken()}
 private fun sid()=runCatching{JsonParser.parseString(String(java.util.Base64.getUrlDecoder().decode(tokens.getAccessToken()!!.split('.')[1]),Charsets.UTF_8)).asJsonObject.get("sid").asString}.getOrNull()
 private fun installation():String=prefs.getString(key("installation"),null)?:java.util.UUID.randomUUID().toString().also{
  check(prefs.edit().putString(key("installation"),it).commit())
 }
 fun permissionGranted()=androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()&&
  (Build.VERSION.SDK_INT<33||ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
 fun disableLocal(){
  revision++;prefs.edit().remove(key("binding")).remove(key("owner")).remove(key("session")).putBoolean(key("enabled"),false).commit()
  _launch.value=null;context.getSystemService(NotificationManager::class.java).cancelAll()
  _status.value="Notifications are disabled on this device."
 }
 fun accountObserved(){
  if(prefs.getString(key("owner"),null)!=owner()||prefs.getString(key("session"),null)!=sid())disableLocal()
 }
 fun preferencesChanged(owner:String,value:com.nihonix.mobile.feature.activity.ActivityPreferences){
  if(this.owner()!=owner)return
  prefs.edit().putBoolean(key("reminder_enabled"),value.reminder_enabled).commit()
  if(!value.push_enabled||!value.reminder_enabled)disableLocal()
 }
 fun tokenChanged(token:String){disableLocal();prefs.edit().putString(key("token"),token).commit();requestReconcile()}
 fun requestReconcile(){scope.launch{runCatching{reconcile()}.onFailure{_status.value="Notification registration pending. Connect and retry."}}}
 suspend fun reconcile():Boolean=mutex.withLock{
  val user=owner()?:run{disableLocal();return@withLock true}
  val session=sid()?:return@withLock false
  if(activities.preferences(user)==null&&!activities.refreshPreferences())return@withLock false
  val settings=activities.preferences(user)?:return@withLock false
  val enabled=settings.push_enabled&&settings.reminder_enabled&&permissionGranted()
  prefs.edit().putBoolean(key("reminder_enabled"),settings.reminder_enabled).commit()
  val startRevision=revision
  if(!enabled){
   disableLocal();FirebaseMessaging.getInstance().isAutoInitEnabled=false
   val r=api.remove(installation(),SessionRequest(user,session))
   _status.value=if(!permissionGranted())"Notification permission is off. Learning remains available." else "Push notifications are off in preferences."
   return@withLock r.isSuccessful
  }
  FirebaseMessaging.getInstance().isAutoInitEnabled=true
  val token=suspendCancellableCoroutine<String>{c->FirebaseMessaging.getInstance().token.addOnSuccessListener{if(c.isActive)c.resume(it)}
   .addOnFailureListener{if(c.isActive)c.resumeWithException(it)}}
  if(owner()!=user||sid()!=session||revision!=startRevision)return@withLock true
  val r=api.register(installation(),DeviceRegistration(token,true),SessionRequest(user,session))
  if(owner()!=user||sid()!=session||revision!=startRevision)return@withLock true
  val binding=r.body()?.takeIf{r.isSuccessful&&it.success}?.data?:return@withLock false
  check(binding.installation_id==installation())
  check(prefs.edit().putString(key("binding"),binding.binding_id).putString(key("owner"),user).putString(key("session"),session)
   .putBoolean(key("enabled"),true).commit())
  _status.value="Notifications registered for this account.";true
 }
 suspend fun beforeLogout(){
  val user=owner();val session=sid();disableLocal()
  if(user!=null)runCatching{api.remove(installation(),SessionRequest(user,session))}
 }
 fun accepted(data:Map<String,String>):NotificationPayload?{
  if(!prefs.getBoolean(key("enabled"),false)||!prefs.getBoolean(key("reminder_enabled"),false)||prefs.getString(key("owner"),null)!=owner()||
   prefs.getString(key("session"),null)!=sid())return null
  return NotificationPayload.parse(data,owner(),prefs.getString(key("binding"),null))
 }
 fun show(data:Map<String,String>){
  val payload=accepted(data)?:return
  if(!permissionGranted())return
  val channel="nihonix-learning-reminders"
  val manager=context.getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel(channel,"Learning reminders",NotificationManager.IMPORTANCE_DEFAULT))
  val intent=Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
  data.filterKeys{it in listOf("schema_version","type","destination","user_id","binding_id","notification_id")}.forEach{(k,v)->intent.putExtra("nihonix_push_"+k,v)}
  val pending=PendingIntent.getActivity(context,payload.notificationId.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val notice=NotificationCompat.Builder(context,channel).setSmallIcon(com.nihonix.mobile.R.drawable.ic_stat_reminder)
   .setContentTitle("Time for Japanese study").setContentText("Continue learning in Nihonix.").setAutoCancel(true).setContentIntent(pending).build()
  manager.notify(payload.notificationId.hashCode(),notice)
 }
 fun handleIntent(intent:Intent?){
  val data=listOf("schema_version","type","destination","user_id","binding_id","notification_id").mapNotNull{k->intent?.getStringExtra("nihonix_push_"+k)?.let{k to it}}.toMap()
  if(accepted(data)!=null)_launch.value=data
 }
 fun consumeLaunch(){_launch.value=null}
}
