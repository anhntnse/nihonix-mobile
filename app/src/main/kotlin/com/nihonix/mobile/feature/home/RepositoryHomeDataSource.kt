package com.nihonix.mobile.feature.home

import com.nihonix.mobile.feature.activity.ActivityRepository
import com.nihonix.mobile.feature.auth.domain.repository.AuthRepository
import com.nihonix.mobile.feature.progress.ProgressRepository
import kotlinx.coroutines.flow.combine

class RepositoryHomeDataSource(
    private val auth:AuthRepository,
    private val activity:ActivityRepository,
    private val progress:ProgressRepository
):HomeDataSource {
    override fun profile(owner:String)=auth.getCurrentUser()
    override fun activity(owner:String)=activity.observe()
    override fun progress(owner:String)=combine(
        progress.database.progress().observe(owner),
        progress.database.progress().observeProducer(owner,"progress")
    ){records,messages->
        HomeProgress(owner,records.count{it.kind=="vocabulary"&&it.status=="learned"},
            records.count{it.kind=="grammar"&&it.status=="learned"},
            messages.count{it.state=="pending"},messages.count{it.state=="blocked"})
    }
    override suspend fun refreshActivity(owner:String):Boolean {
        if(activity.account()!=owner)return false
        val synced=activity.sync(owner)
        if(activity.account()!=owner)return false
        val refreshed=activity.refreshSummary(owner)
        return synced&&refreshed
    }
    override fun requestProgressSync(owner:String) {
        if(progress.account()==owner)progress.requestSync()
    }
}
