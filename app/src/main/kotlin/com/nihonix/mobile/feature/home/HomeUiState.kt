package com.nihonix.mobile.feature.home

import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.activity.ActivityState
import com.nihonix.mobile.feature.activity.ActivitySummary
import java.time.Instant
import java.time.ZoneId

data class HomeSection<T>(val data:T?=null,val loading:Boolean=false,val error:String?=null)
data class HomeProgress(val account:String,val vocabulary:Int=0,val grammar:Int=0,val pending:Int=0,val blocked:Int=0)
data class HomeUiState(
    val account:String?=null,
    val profile:HomeSection<User> = HomeSection(),
    val activity:HomeSection<ActivityState> = HomeSection(),
    val progress:HomeSection<HomeProgress> = HomeSection(),
    val now:Instant=Instant.now()
) {
    fun todaySummary():ActivitySummary? = activity.data?.summary?.takeIf { summary->
        val timezone=activity.data?.preferences?.timezone
        (timezone==null||timezone==summary.timezone)&&
            runCatching { now.atZone(ZoneId.of(summary.timezone)).toLocalDate().toString()==summary.today }.getOrDefault(false)
    }
}
enum class HomeDestination { Vocabulary, Grammar, Practice, Flashcards, Progress, Calendar, Topics, Profile }
