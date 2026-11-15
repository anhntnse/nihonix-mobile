package com.nihonix.mobile.feature.notifications
data class NotificationPayload(val destination:String,val notificationId:String){
 companion object{
  fun parse(data:Map<String,String>,owner:String?,binding:String?):NotificationPayload?{
   if(owner.isNullOrBlank()||binding.isNullOrBlank()||data["schema_version"]!="1"||data["type"]!="learning_reminder"||
    data["user_id"]!=owner||data["binding_id"]!=binding||data["destination"]!="activity")return null
   val id=data["notification_id"]?.takeIf{it.isNotBlank()&&it.length<=100}?:return null
   return NotificationPayload("activity",id)
  }
 }
}
