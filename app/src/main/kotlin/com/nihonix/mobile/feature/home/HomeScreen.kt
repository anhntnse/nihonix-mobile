package com.nihonix.mobile.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(viewModel:HomeViewModel,onNavigate:(HomeDestination)->Unit) {
    val state by viewModel.uiState.collectAsState()
    val user by viewModel.users.collectAsState()
    val authenticated by viewModel.authenticated.collectAsState()
    val lifecycle=LocalLifecycleOwner.current
    DisposableEffect(lifecycle,viewModel) {
        val observer=LifecycleEventObserver { _,event->
            if(event==Lifecycle.Event.ON_RESUME)viewModel.onVisible()
        }
        lifecycle.lifecycle.addObserver(observer)
        if(lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))viewModel.onVisible()
        onDispose{lifecycle.lifecycle.removeObserver(observer)}
    }
    LaunchedEffect(viewModel) {
        while(isActive){delay(60_000);viewModel.updateClock()}
    }
    // Gate at render/click time as well as at every async state update.
    val visible=state.takeIf{authenticated&&user?.id==it.account}?:HomeUiState()
    HomeContent(visible,viewModel::refresh){destination->
        if(viewModel.authenticated.value&&viewModel.users.value?.id==state.account)onNavigate(destination)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(state:HomeUiState,onRefresh:()->Unit,onNavigate:(HomeDestination)->Unit) {
    val scroll=rememberLazyListState()
    Scaffold(topBar={TopAppBar(title={Text("Home")},actions={
        TextButton(onClick=onRefresh,enabled=state.account!=null){Text("Refresh")}
    })}) { insets->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets),contentAlignment=Alignment.TopCenter) {
            val columns=if(maxWidth>=360.dp&&LocalDensity.current.fontScale<1.5f)2 else 1
            val shortcuts=listOf(
                Triple(HomeDestination.Vocabulary,"Vocabulary","Learn words and expressions"),
                Triple(HomeDestination.Grammar,"Grammar","Explore patterns and examples"),
                Triple(HomeDestination.Practice,"AI Practice","Choose a topic and exercise"),
                Triple(HomeDestination.Flashcards,"Flashcards","Review your personal sets"),
                Triple(HomeDestination.Progress,"Progress","View learned items and sync"),
                Triple(HomeDestination.Calendar,"Learning calendar","See your activity history"),
                Triple(HomeDestination.Topics,"Browse topics","Explore the JLPT roadmap"),
                Triple(HomeDestination.Profile,"Profile","Manage your learning goals")
            )
            LazyColumn(Modifier.widthIn(max=720.dp).fillMaxSize().testTag("home-list"),state=scroll,
                contentPadding=PaddingValues(16.dp,16.dp,16.dp,32.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                item("welcome") {
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        val user=state.profile.data
                        Text("Welcome back"+(user?.let{", "+it.fullName?.takeIf(String::isNotBlank).orEmpty().ifBlank{it.username}}?:""),
                            style=MaterialTheme.typography.headlineMedium,modifier=Modifier.semantics{heading()})
                        Text("Make time for your Japanese today.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                        FilledTonalButton(onClick={onNavigate(HomeDestination.Topics)},enabled=state.account!=null) {
                            Text(user?.levelCode?.let{"JLPT $it"}?:"Choose your JLPT level")
                        }
                        if(state.profile.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
                        state.profile.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
                    }
                }
                item("activity") {
                    Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text("Your day",style=MaterialTheme.typography.titleLarge,modifier=Modifier.semantics{heading()})
                            if(state.activity.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
                            val saved=state.activity.data
                            val summary=state.todaySummary()
                            val goal=saved?.preferences?.daily_goal_minutes?:saved?.summary?.daily_goal_minutes
                            if(summary!=null&&goal!=null&&goal>0) {
                                Text("${summary.today_minutes} / $goal minutes",style=MaterialTheme.typography.headlineSmall)
                                LinearProgressIndicator(progress={ (summary.today_seconds/(goal*60f)).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth())
                                Text(if(summary.today_seconds>=goal*60)"Daily goal completed" else "A little study brings you closer.")
                                Text("Current streak: ${summary.current_streak} "+if(summary.current_streak==1)"day" else "days")
                                Text("Accepted server time. Unsynced activity is not included.",style=MaterialTheme.typography.bodySmall)
                                Text("${summary.today} · ${summary.timezone}",style=MaterialTheme.typography.bodySmall)
                            } else {
                                Text("No accepted totals saved for today.")
                                Text(goal?.let{"Daily goal: $it minutes"}?:"Connect to load your daily goal.")
                                saved?.summary?.let{Text("Saved streak: ${it.current_streak} "+(if(it.current_streak==1)"day" else "days")+" (as of ${it.today})")}
                            }
                            val pending=saved?.messages?.size?:0
                            if(pending>0)Text("$pending unsynced study "+if(pending==1)"event" else "events")
                            saved?.cachedAt?.let{timestamp->
                                val time=runCatching{DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timestamp))}.getOrDefault("unknown")
                                Text("Last saved: $time",style=MaterialTheme.typography.bodySmall)
                            }
                            state.activity.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
                        }
                    }
                }
                item("progress") {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text("Learning on this device",style=MaterialTheme.typography.titleLarge,modifier=Modifier.semantics{heading()})
                            if(state.progress.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
                            state.progress.data?.let{p->
                                Text("${p.vocabulary} vocabulary · ${p.grammar} grammar",style=MaterialTheme.typography.titleMedium)
                                Text("Marked as learned, including local changes.")
                                if(p.pending+p.blocked>0)Text("${p.pending} pending · ${p.blocked} need attention")
                                else Text("No unsynced learning changes.")
                            }?:Text("Your learned items will appear here.")
                            state.progress.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
                        }
                    }
                }
                item("shortcuts-title"){Text("Keep learning",style=MaterialTheme.typography.titleLarge,modifier=Modifier.semantics{heading()})}
                shortcuts.chunked(columns).forEach{row->
                    item("shortcuts-"+row.first().first.name) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            row.forEach{(destination,title,description)->
                                OutlinedCard(onClick={onNavigate(destination)},enabled=state.account!=null,
                                    modifier=Modifier.weight(1f).heightIn(min=104.dp).testTag("home-"+destination.name)) {
                                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                        Icon(when(destination){
                                            HomeDestination.Vocabulary->Icons.Outlined.MenuBook
                                            HomeDestination.Grammar->Icons.Outlined.Translate
                                            HomeDestination.Practice->Icons.Outlined.Quiz
                                            HomeDestination.Flashcards->Icons.Outlined.Style
                                            HomeDestination.Progress->Icons.Outlined.Insights
                                            HomeDestination.Calendar->Icons.Outlined.CalendarMonth
                                            HomeDestination.Topics->Icons.Outlined.Explore
                                            HomeDestination.Profile->Icons.Outlined.Person
                                        },contentDescription=null,tint=MaterialTheme.colorScheme.primary)
                                        Text(title,style=MaterialTheme.typography.titleMedium)
                                        Text(description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            if(row.size<columns)Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
