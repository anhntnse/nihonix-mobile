package com.nihonix.mobile.feature.activity
data class ActivitySegment(val id:String,val activity_type:String,val occurred_at:String,val duration_seconds:Int,val timezone:String,val item_id:String?=null)
data class ActivityBatch(val events:List<ActivitySegment>)
data class ActivityAck(val accepted:List<String>)
data class ActivityPreferences(val push_enabled:Boolean=false,val reminder_enabled:Boolean=false,val reminder_time:String="20:00",val timezone:String="UTC",val daily_goal_minutes:Int=15)
data class ActivitySummary(val current_streak:Int,val longest_streak:Int,val today_seconds:Int,val today_minutes:Int,val daily_goal_minutes:Int,
 val goal_completed:Boolean,val is_today_completed:Boolean,val today:String,val timezone:String)
data class ActivityDay(val date:String,val seconds:Int,val minutes:Int,val qualified:Boolean,val goal_completed:Boolean,val practice_completions:Int)
data class ActivityCalendar(val year:Int,val month:Int,val timezone:String,val learning_days:List<ActivityDay>,val total_days:Int)
