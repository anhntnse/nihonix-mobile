package com.nihonix.mobile

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import kotlinx.coroutines.runBlocking
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.*
import com.nihonix.mobile.core.offline.*
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.core.network.interceptor.SessionRequest
import com.nihonix.mobile.feature.home.*
import com.nihonix.mobile.feature.activity.*
import com.nihonix.mobile.feature.auth.domain.model.*
import com.nihonix.mobile.feature.auth.domain.repository.*
import com.nihonix.mobile.feature.auth.presentation.login.LoginViewModel
import com.nihonix.mobile.feature.auth.presentation.profile.UserProfileViewModel
import com.nihonix.mobile.feature.auth.presentation.navigation.*
import com.nihonix.mobile.feature.topics.domain.model.*
import com.nihonix.mobile.feature.topics.domain.repository.TopicsLevelsRepository
import com.nihonix.mobile.feature.topics.presentation.topics_list.TopicsViewModel
import com.nihonix.mobile.feature.learning.domain.*
import com.nihonix.mobile.feature.learning.presentation.*
import com.nihonix.mobile.feature.progress.*
import com.nihonix.mobile.feature.flashcards.data.FlashcardsRepository
import com.nihonix.mobile.feature.flashcards.presentation.FlashcardsViewModel
import com.nihonix.mobile.feature.practice.data.PracticeRepository
import com.nihonix.mobile.feature.practice.presentation.PracticeViewModel
import com.nihonix.mobile.ui.theme.NihonixTheme
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import retrofit2.Response

class HomeNavigationTest {
    @get:Rule val compose=createComposeRule()
    private val users=FlashUsers()
    private val tokens=FlashTokens()
    private val notifications=MutableStateFlow<Map<String,String>?>(null)
    private lateinit var nav:NavHostController
    private lateinit var db:OfflineDatabase
    private val store=ViewModelStore()
    private var count=0
    private lateinit var homeData:RepositoryHomeDataSource
    private var calendarCalls=0
    private val auth=object:AuthRepository{
        override val currentUserFlow=users.currentUserFlow
        override fun isLoggedIn()=tokens.hasAccessToken()
        override fun getCurrentUser():Flow<Resource<User>> = flowOf(users.getUser()?.let{Resource.Success(it)}?:Resource.Error("Signed out",401))
        override fun login(username:String,password:String):Flow<Resource<User>> = flow {
            users.saveUser(User("account-a",username,"a@example.test",levelCode="N5"));tokens.saveTokens("access","refresh")
            emit(Resource.Success(users.getUser()!!))
        }
        override fun logout():Flow<Resource<Unit>> = flow{tokens.clearTokens();users.clearUser();emit(Resource.Success(Unit))}
        override fun register(username:String,email:String,password:String,fullName:String?):Flow<Resource<User>> = flowOf(Resource.Error("Not used"))
        override fun forgotPassword(email:String):Flow<Resource<String>> = flowOf(Resource.Error("Not used"))
        override fun verifyResetCode(email:String,code:String):Flow<Resource<ResetCodeVerification>> = flowOf(Resource.Error("Not used"))
        override fun resetPassword(token:String,password:String):Flow<Resource<String>> = flowOf(Resource.Error("Not used"))
        override fun resendCode(email:String):Flow<Resource<String>> = flowOf(Resource.Error("Not used"))
    }
    private val account=object:AccountRepository{
        override fun requestVerification(email:String)=flowOf<Resource<Int>>(Resource.Error("Not used"))
        override fun verifyEmail(email:String,code:String)=flowOf<Resource<Unit>>(Resource.Error("Not used"))
        override fun updateProfile(username:String,fullName:String)=flowOf<Resource<User>>(Resource.Error("Not used"))
        override fun getSettings()=flowOf<Resource<LearningSettings>>(Resource.Success(LearningSettings()))
        override fun updateSettings(settings:LearningSettings)=flowOf<Resource<LearningSettings>>(Resource.Success(settings))
        override fun uploadAvatar(bytes:ByteArray,mimeType:String)=flowOf<Resource<User>>(Resource.Error("Not used"))
        override fun deleteAccount()=flowOf<Resource<Unit>>(Resource.Error("Not used"))
    }
    private val topics=object:TopicsLevelsRepository{
        override fun getLevels()=flowOf<Resource<List<JlptLevel>>>(Resource.Success(emptyList()))
        override fun getLevelByCode(code:String)=flowOf<Resource<JlptLevel>>(Resource.Empty)
        override fun getUserLevel()=flowOf<Resource<UserLevel>>(Resource.Success(UserLevel("account-a","N5")))
        override fun updateUserLevel(levelCode:String)=flowOf<Resource<UserLevel>>(Resource.Success(UserLevel("account-a",levelCode)))
        override fun getTopics(levelCode:String?)=flowOf<Resource<List<Topic>>>(Resource.Success(emptyList()))
        override fun getTopicDetail(identifier:String)=flowOf<Resource<Topic>>(Resource.Empty)
    }
    private val learning=object:LearningRepository{
        override suspend fun page(kind:ContentKind,filter:LearningFilter,page:Int):Resource<LearningPage> = Resource.Success(LearningPage(emptyList(),0,1,false,"v"))
        override suspend fun detail(kind:ContentKind,identifier:String):Resource<LearningEntry> = Resource.Empty
        override suspend fun topics():Resource<List<LearningTopic>> = Resource.Success(emptyList())
    }
    @Before fun setup(){
        users.saveUser(users.getUser()!!.copy(fullName="Anh",levelCode="N5"))
        db=Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,OfflineDatabase::class.java).build()
    }
    @After fun close(){InstrumentationRegistry.getInstrumentation().runOnMainSync{store.clear()};db.close()}
    private fun show(restoration:StateRestorationTester?=null){
        val gson=Gson()
        val fallback=ActivityTestApi()
        val activityApi=object:ActivityApi by fallback {
            override suspend fun calendar(year:Int,month:Int,session:SessionRequest):Response<ApiResponseDto<ActivityCalendar>> {
                calendarCalls++
                return fallback.calendar(year,month,session)
            }
        }
        val activity=ActivityRepository(db,activityApi,users,tokens,gson,{})
        val progressApi=object:ProgressApi{
            private fun data()=Response.success(ApiResponseDto(success=true,data=JsonObject().apply{
                addProperty("cursor","0");addProperty("has_more",false);add("events",JsonArray());add("acknowledgements",JsonArray())}))
            override suspend fun batch(value:ProgressBatch,session:SessionRequest)=data()
            override suspend fun changes(after:String,session:SessionRequest,limit:Int)=data()
            override suspend fun summary()=data()
        }
        val progress=ProgressRepository(db,progressApi,tokens,users,gson,{})
        homeData=RepositoryHomeDataSource(auth,activity,progress)
        val flashcards=FlashcardsRepository(db,FlashTestApi(),users,tokens,gson,{})
        val practice=PracticeRepository(db,PracticeTestApi(),users,tokens,gson,{})
        val factory=object:ViewModelProvider.Factory{
            override fun <T:ViewModel> create(modelClass:Class<T>,extras:CreationExtras):T{
                val saved=extras.createSavedStateHandle()
                val vm:ViewModel=when(modelClass){
                    HomeViewModel::class.java->HomeViewModel(users.currentUserFlow,tokens.isAuthenticated,homeData)
                    LoginViewModel::class.java->LoginViewModel(auth)
                    TopicsViewModel::class.java->TopicsViewModel(topics,auth,saved)
                    LearningViewModel::class.java->LearningViewModel(learning,saved)
                    PracticeViewModel::class.java->PracticeViewModel(practice,saved)
                    FlashcardsViewModel::class.java->FlashcardsViewModel(flashcards,saved)
                    ProgressViewModel::class.java->ProgressViewModel(progress,db.catalog())
                    ActivityViewModel::class.java->ActivityViewModel(activity)
                    UserProfileViewModel::class.java->UserProfileViewModel(auth,account)
                    else->error("Unexpected VM: "+modelClass)
                }
                store.put("vm-"+count++,vm)
                @Suppress("UNCHECKED_CAST") return vm as T
            }
        }
        val content:@Composable ()->Unit = {nav=rememberNavController();NihonixTheme(dynamicColor=false){
            NihonixNavHost(navController=nav,viewModelFactory=factory,authenticated=tokens.isAuthenticated,
                notificationLaunches=notifications,acceptNotification={it["destination"]=="activity"&&it["user_id"]==users.getUser()?.id},
                consumeNotification={notifications.value=null})
        }}
        if(restoration==null)compose.setContent(content)else restoration.setContent(content)
    }
    private fun route()=compose.runOnIdle{nav.currentDestination?.route}
    @Test fun savedSessionLandsOnHomeAndEveryShortcutReturnsToIt(){
        show()
        compose.waitUntil{route()=="home"}
        val expected=listOf(
            "Vocabulary" to LearningDestinations.LIST,"Grammar" to LearningDestinations.LIST,
            "Practice" to "practice?topicId={topicId}","Flashcards" to "flashcards",
            "Progress" to "progress","Calendar" to "activity","Topics" to TopicsDestinations.TOPICS,"Profile" to AuthDestinations.PROFILE)
        expected.forEach{(name,target)->
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("home-"+name))
            compose.onNodeWithTag("home-"+name).performClick()
            compose.waitUntil{route()==target}
            if(name in listOf("Vocabulary","Grammar"))compose.runOnIdle{
                assertEquals(if(name=="Vocabulary")"vocabulary-sets" else "grammar-sub-topics",nav.currentBackStackEntry?.arguments?.getString("kind"))
                assertEquals("N5",nav.currentBackStackEntry?.arguments?.getString("levelCode"))
            }
            compose.runOnIdle{assertTrue(nav.popBackStack())}
            compose.waitUntil{route()=="home"}
        }
    }
    @Test fun roomCountsAreAccountScopedAndSummaryRefreshDoesNotFetchCalendar()=runBlocking {
        db.progress().put(LocalProgress("account-a","vocabulary","hello","learned","a","2026-11-16T00:00:00Z","topic","N5"))
        db.progress().put(LocalProgress("account-b","grammar","foreign","learned","b","2026-11-16T00:00:00Z","topic","N3"))
        db.progress().enqueue(OutboxMessage("account-a","pending","vocabulary:hello","progress","{}"))
        show();compose.waitUntil(10000){route()=="home"}
        compose.onNodeWithTag("home-list").performScrollToNode(hasText("1 vocabulary · 0 grammar"))
        compose.onNodeWithText("1 pending · 0 need attention").assertExists()
        assertEquals(0,calendarCalls)
        assertTrue(homeData.refreshActivity("account-a"))
        assertEquals(0,calendarCalls)
        val saved=homeData.activity("account-a").first{it.summary!=null}
        assertEquals("account-a",saved.account)
        assertNotNull(saved.cachedAt)
        assertEquals(60,saved.summary?.today_seconds)
    }
    @Test fun navigationFilterSurvivesSavedStateRestoration(){
        val restoration=StateRestorationTester(compose);show(restoration)
        compose.waitUntil(10000){route()=="home"}
        compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("home-Vocabulary"))
        compose.onNodeWithTag("home-Vocabulary").performClick()
        compose.waitUntil(10000){route()==LearningDestinations.LIST}
        restoration.emulateSavedInstanceStateRestore()
        compose.waitUntil(10000){route()==LearningDestinations.LIST}
        compose.runOnIdle{
            assertEquals("N5",nav.currentBackStackEntry?.arguments?.getString("levelCode"))
            assertTrue(nav.popBackStack())
        }
        compose.waitUntil(10000){route()=="home"}
    }
    @Test fun successfulLoginLandsOnHomeAndRemovesLoginFromBackStack(){
        tokens.clearTokens();users.clearUser();show()
        compose.onNodeWithText("Username").performTextInput("learner")
        compose.onNodeWithText("Password").performTextInput("password123")
        compose.onNodeWithText("Sign In",useUnmergedTree=true).performScrollTo().performClick()
        compose.waitUntil{route()=="home"}
        compose.runOnIdle{assertNull(nav.previousBackStackEntry)}
    }
    @Test fun sessionLossClearsHomeAndProtectedBackStack(){
        show();compose.waitUntil{route()=="home"}
        compose.runOnIdle{tokens.clearTokens()}
        compose.waitUntil{route()==AuthDestinations.LOGIN}
        compose.onNodeWithText("Welcome back, Anh").assertDoesNotExist()
        compose.runOnIdle{assertNull(nav.previousBackStackEntry)}
    }
    @Test fun coldNotificationOpensActivityAboveHome(){
        notifications.value=mapOf("destination" to "activity","user_id" to "account-a");show()
        compose.waitUntil{route()=="activity"}
        compose.runOnIdle{assertEquals("home",nav.previousBackStackEntry?.destination?.route);nav.popBackStack()}
        compose.waitUntil{route()=="home"}
    }
    @Test fun unsupportedNotificationCannotOverrideHome(){
        notifications.value=mapOf("destination" to "untrusted","user_id" to "account-a");show()
        compose.waitUntil{route()=="home"}
        compose.runOnIdle{assertNull(notifications.value)}
    }
}
