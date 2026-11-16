package com.nihonix.mobile.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.auth.domain.model.User
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class HomeViewModel(
    val users:StateFlow<User?>,
    val authenticated:StateFlow<Boolean>,
    private val source:HomeDataSource,
    private val clock:()->java.time.Instant = java.time.Instant::now
):ViewModel() {
    private val state=MutableStateFlow(HomeUiState(now=clock()))
    val uiState=state.asStateFlow()
    private var generation=0
    private var refreshVersion=0
    private var owner:String?=null
    private val observers=mutableListOf<Job>()
    private val refreshes=mutableListOf<Job>()

    init {
        viewModelScope.launch {
            combine(users,authenticated){user,valid->user?.takeIf{valid}}.collect { user->
                if(owner!=user?.id) {
                    generation++
                    observers.forEach{it.cancel()};observers.clear()
                    refreshes.forEach{it.cancel()};refreshes.clear()
                    owner=user?.id
                    state.value=HomeUiState(account=owner,profile=HomeSection(user),now=clock())
                    if(owner!=null)refresh()
                } else if(user!=null) {
                    state.update{it.copy(profile=it.profile.copy(data=user))}
                }
            }
        }
    }
    private fun current(id:String,revision:Int)=owner==id&&generation==revision&&
        authenticated.value&&users.value?.id==id

    fun updateClock(){state.update{it.copy(now=clock())}}
    fun onVisible(){updateClock();if(refreshes.none{it.isActive})refresh()}

    fun refresh() {
        val id=owner?:return
        val revision=generation
        if(!current(id,revision))return
        val attempt=++refreshVersion
        fun active()=current(id,revision)&&attempt==refreshVersion
        refreshes.forEach{it.cancel()};refreshes.clear()
        observers.forEach{it.cancel()};observers.clear()
        state.update{it.copy(now=clock(),profile=it.profile.copy(loading=true,error=null),
            activity=it.activity.copy(loading=true,error=null),progress=it.progress.copy(loading=true,error=null))}
        observers+=viewModelScope.launch {
            try {
                source.activity(id).collect { snapshot->
                    if(active()&&snapshot.account==id)state.update{it.copy(activity=it.activity.copy(data=snapshot))}
                }
            } catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception){if(active())state.update{it.copy(activity=it.activity.copy(loading=false,error="Saved activity could not be loaded. Try Refresh."))}}
        }
        observers+=viewModelScope.launch {
            try {
                source.progress(id).collect { snapshot->
                    if(active()&&snapshot.account==id)state.update{it.copy(progress=HomeSection(snapshot))}
                }
            } catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception){if(active())state.update{it.copy(progress=it.progress.copy(loading=false,error="Learning progress could not be loaded. Try Refresh."))}}
        }
        refreshes+=viewModelScope.launch {
            try {
                source.profile(id).collect { result->
                    if(active())when(result){
                        is Resource.Success->if(result.data.id==id)state.update{it.copy(profile=HomeSection(result.data))}
                        is Resource.Error->state.update{it.copy(profile=it.profile.copy(loading=false,error="Profile could not refresh. Saved information is shown."))}
                        is Resource.Empty->state.update{it.copy(profile=it.profile.copy(loading=false))}
                        is Resource.Loading->Unit
                    }
                }
            } catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception){if(active())state.update{it.copy(profile=it.profile.copy(loading=false,error="Offline. Saved profile is shown."))}}
            finally{if(active())state.update{it.copy(profile=it.profile.copy(loading=false))}}
        }
        refreshes+=viewModelScope.launch {
            try {
                val ok=source.refreshActivity(id)
                if(active())state.update{it.copy(activity=it.activity.copy(loading=false,error=if(ok)null else "Could not refresh activity. Saved totals are shown when available."))}
            } catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception){if(active())state.update{it.copy(activity=it.activity.copy(loading=false,error="Offline. Saved totals and unsynced activity are retained."))}}
        }
        try {source.requestProgressSync(id)}
        catch(_:Exception){if(active())state.update{it.copy(progress=it.progress.copy(error="Progress sync could not start. Local changes are retained."))}}
    }
}
