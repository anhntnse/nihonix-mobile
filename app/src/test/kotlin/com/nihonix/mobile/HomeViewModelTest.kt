package com.nihonix.mobile

import androidx.lifecycle.ViewModelStore
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.home.*
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.activity.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher=StandardTestDispatcher()
    private val stores=mutableListOf<ViewModelStore>()
    private val user=MutableStateFlow<User?>(User("a","learner","a@example.test",fullName="Anh",levelCode="N5"))
    private val signedIn=MutableStateFlow(true)
    private val now=Instant.parse("2026-11-16T03:00:00Z")
    private fun summary(today:String="2026-11-16")=ActivitySummary(2,4,125,2,15,false,true,today,"Asia/Ho_Chi_Minh")
    private inner class Source:HomeDataSource {
        val activities=MutableStateFlow(ActivityState("a",summary(),cachedAt=42))
        val learned=MutableStateFlow(HomeProgress("a",3,2,1,1))
        var profileResult:Resource<User> = Resource.Success(user.value!!)
        var activityOk=true
        var hold:CompletableDeferred<Boolean>?=null
        var profileError=false
        var progressError=false
        override fun profile(owner:String):Flow<Resource<User>> = flow {
            emit(Resource.Loading)
            if(profileError)throw java.io.IOException("offline")
            emit(profileResult)
        }
        override fun activity(owner:String)=activities
        override fun progress(owner:String):Flow<HomeProgress> = if(progressError) flow {throw java.io.IOException("db")} else learned
        override suspend fun refreshActivity(owner:String):Boolean {
            if(owner=="a"&&hold!=null)return withContext(NonCancellable){hold!!.await()}
            return activityOk
        }
        override fun requestProgressSync(owner:String) {}
    }
    private fun vm(source:Source):HomeViewModel=HomeViewModel(user,signedIn,source,{now}).also{
        ViewModelStore().apply{put("home",it);stores+=this}
    }
    @Before fun setup(){Dispatchers.setMain(dispatcher)}
    @After fun close(){stores.forEach{it.clear()};Dispatchers.resetMain()}
    @Test fun combinesActualServerActivityAndLocalCountsWithoutAddingPendingMinutes()=runTest(dispatcher){
        val view=vm(Source());runCurrent()
        val state=view.uiState.value
        assertEquals("a",state.account);assertEquals("Anh",state.profile.data?.fullName)
        assertEquals(125,state.todaySummary()?.today_seconds)
        assertEquals(15,state.todaySummary()?.daily_goal_minutes)
        assertEquals(3,state.progress.data?.vocabulary)
        assertEquals(1,state.progress.data?.pending)
        assertEquals(42L,state.activity.data?.cachedAt)
        assertFalse(state.activity.loading)
    }
    @Test fun offlineRefreshKeepsSavedActivityAndReportsOnlyItsSectionError()=runTest(dispatcher){
        val source=Source().apply{activityOk=false};val view=vm(source);runCurrent()
        assertNotNull(view.uiState.value.activity.error)
        assertEquals(2,view.uiState.value.todaySummary()?.today_minutes)
        assertNull(view.uiState.value.profile.error)
        assertEquals(2,view.uiState.value.progress.data?.grammar)
    }
    @Test fun thrownProfileAndProgressErrorsDoNotBlockActivityOrSavedGreeting()=runTest(dispatcher){
        val source=Source().apply{profileError=true;progressError=true};val view=vm(source);runCurrent()
        assertEquals("Anh",view.uiState.value.profile.data?.fullName)
        assertNotNull(view.uiState.value.profile.error)
        assertNotNull(view.uiState.value.progress.error)
        assertEquals(125,view.uiState.value.todaySummary()?.today_seconds)
    }
    @Test fun lateOldAccountRefreshAndSnapshotsCannotReachNewAccount()=runTest(dispatcher){
        val source=Source().apply{hold=CompletableDeferred()};val view=vm(source);runCurrent()
        user.value=User("b","second","b@example.test",levelCode="N3")
        source.profileResult=Resource.Success(user.value!!)
        source.activities.value=ActivityState("b",summary().copy(today_seconds=60,today_minutes=1))
        source.learned.value=HomeProgress("b",9)
        runCurrent()
        source.hold!!.complete(false);runCurrent()
        assertEquals("b",view.uiState.value.account)
        assertEquals("second",view.uiState.value.profile.data?.username)
        assertEquals(9,view.uiState.value.progress.data?.vocabulary)
        assertNull(view.uiState.value.activity.error)
        source.activities.value=ActivityState("a",summary().copy(today_seconds=999))
        source.learned.value=HomeProgress("a",999)
        runCurrent()
        assertEquals("b",view.uiState.value.activity.data?.account)
        assertEquals(9,view.uiState.value.progress.data?.vocabulary)
    }
    @Test fun tokenLossClearsEvenIfSavedUserRemainsAndLateWorkFinishes()=runTest(dispatcher){
        val source=Source().apply{hold=CompletableDeferred()};val view=vm(source);runCurrent()
        signedIn.value=false;runCurrent()
        source.hold!!.complete(true);runCurrent()
        assertNull(view.uiState.value.account)
        assertNull(view.uiState.value.profile.data)
        assertNull(view.uiState.value.activity.data)
        assertNull(view.uiState.value.progress.data)
    }
    @Test fun savedUserWithoutAuthenticatedSessionDoesNotStartReads()=runTest(dispatcher){
        signedIn.value=false;val view=vm(Source());runCurrent()
        assertNull(view.uiState.value.account);assertNull(view.uiState.value.profile.data)
    }
    @Test fun nextDayDoesNotPresentYesterdayAsTodaysProgressAndRespectsServerTimezone()=runTest(dispatcher){
        val source=Source().apply{activities.value=ActivityState("a",summary("2026-11-15"),cachedAt=1)}
        val view=vm(source);runCurrent()
        assertNull(view.uiState.value.todaySummary())
        source.activities.value=ActivityState("a",summary().copy(timezone="America/Los_Angeles",today="2026-11-15"))
        runCurrent()
        assertNotNull(view.uiState.value.todaySummary()) // UTC03:00 is still the previous day in LA
    }
    @Test fun refreshRestartsAProgressObserverThatPreviouslyFailed()=runTest(dispatcher){
        val source=Source().apply{progressError=true};val view=vm(source);runCurrent()
        assertNotNull(view.uiState.value.progress.error)
        source.progressError=false;view.refresh();runCurrent()
        assertNull(view.uiState.value.progress.error)
        assertEquals(3,view.uiState.value.progress.data?.vocabulary)
    }
    @Test fun changedPreferenceTimezoneCannotReusePreviousTimezoneTotals()=runTest(dispatcher){
        val source=Source().apply {
            activities.value=ActivityState("a",summary("2026-11-15").copy(timezone="America/Los_Angeles"),
                preferences=ActivityPreferences(timezone="Asia/Ho_Chi_Minh"))
        }
        val view=vm(source);runCurrent()
        assertNull("Previous timezone's totals must be treated as saved history",view.uiState.value.todaySummary())
        assertEquals(125,view.uiState.value.activity.data?.summary?.today_seconds)
    }
    @Test fun goalOnlyPreferenceChangeKeepsValidAcceptedSeconds()=runTest(dispatcher){
        val source=Source().apply{
            activities.value=ActivityState("a",summary(),preferences=ActivityPreferences(timezone="Asia/Ho_Chi_Minh",daily_goal_minutes=30))
        }
        val view=vm(source);runCurrent()
        assertEquals(125,view.uiState.value.todaySummary()?.today_seconds)
        assertEquals(30,view.uiState.value.activity.data?.preferences?.daily_goal_minutes)
    }
    @Test fun clockTickClearsYesterdayWithoutWaitingForNetwork()=runTest(dispatcher){
        var instant=now
        val view=HomeViewModel(user,signedIn,Source(),{instant})
        ViewModelStore().apply{put("home",view);stores+=this}
        runCurrent();assertNotNull(view.uiState.value.todaySummary())
        instant=Instant.parse("2026-11-16T17:00:00Z");view.updateClock()
        assertNull(view.uiState.value.todaySummary())
    }
    @Test fun retryRecoversOneFailedSectionWhileKeepingOtherSections()=runTest(dispatcher){
        val source=Source().apply{activityOk=false};val view=vm(source);runCurrent()
        source.activityOk=true;view.refresh();runCurrent()
        assertNull(view.uiState.value.activity.error)
        assertFalse(view.uiState.value.activity.loading)
        assertEquals(3,view.uiState.value.progress.data?.vocabulary)
    }
}
