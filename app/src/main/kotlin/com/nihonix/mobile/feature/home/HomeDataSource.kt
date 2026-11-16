package com.nihonix.mobile.feature.home

import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.activity.ActivityState
import kotlinx.coroutines.flow.Flow

interface HomeDataSource {
    fun profile(owner:String):Flow<Resource<User>>
    fun activity(owner:String):Flow<ActivityState>
    fun progress(owner:String):Flow<HomeProgress>
    suspend fun refreshActivity(owner:String):Boolean
    fun requestProgressSync(owner:String)
}
