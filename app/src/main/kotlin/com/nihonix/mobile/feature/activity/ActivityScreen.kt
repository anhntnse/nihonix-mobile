package com.nihonix.mobile.feature.activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale
import com.nihonix.mobile.R
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ActivityScreen(viewModel:ActivityViewModel,onBack:()->Unit={}){
 val ui by viewModel.uiState.collectAsState()
 val context=LocalContext.current
 val configuration=LocalConfiguration.current
 // This screen's UI copy is English; use English plural rules even on vi/ja devices.
 val streakResources=remember(context,configuration){
  context.createConfigurationContext(Configuration(configuration).apply{setLocale(Locale.ENGLISH)}).resources
 }
 val owner=ui.snapshot.account
 var discard by remember(owner){mutableStateOf<String?>(null)}
 val calendar=ui.snapshot.calendars["${ui.month.year}-${ui.month.monthValue}"]
 fun guarded(action:()->Unit){if(owner!=null&&viewModel.repository.account()==owner)action()}
 if(discard!=null)AlertDialog(onDismissRequest={discard=null},title={Text("Discard study time?")},text={Text("This unsynced activity will be removed from this device.")},
  confirmButton={TextButton(onClick={val id=discard;discard=null;guarded{if(id!=null)viewModel.discard(id)}}){Text("Discard")}},
  dismissButton={TextButton(onClick={discard=null}){Text("Keep")}})
 Scaffold(topBar={TopAppBar(title={Text("Learning activity")},navigationIcon={TextButton(onClick=onBack){Text("Back")}},
  actions={TextButton(onClick={guarded{viewModel.refresh()}}){Text("Refresh")}})}){padding->
  LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)){
   if(ui.loading)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
   ui.error?.let{item{Text(it,color=MaterialTheme.colorScheme.error)}}
   val summary=ui.snapshot.summary
   if(summary!=null)item("summary"){
    Text(streakResources.getQuantityString(R.plurals.current_streak,summary.current_streak,summary.current_streak),style=MaterialTheme.typography.headlineSmall)
    Text(streakResources.getQuantityString(R.plurals.longest_streak,summary.longest_streak,summary.longest_streak))
    Text("Today: ${summary.today_minutes} / ${summary.daily_goal_minutes} minutes",style=MaterialTheme.typography.titleLarge)
    Text(if(summary.goal_completed)"Daily goal completed" else "Keep learning toward your daily goal.")
    Text("Timezone: ${summary.timezone}")
    Text("A day with at least 60 seconds of engaged study counts toward your streak.")
   }else if(!ui.loading)item{Text("Connect to load your learning totals.")}
   if(ui.snapshot.cachedAt!=null)item{Text("Totals reflect accepted server activity. Pending time appears after synchronization.")}
   item("month"){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
     TextButton(onClick={guarded{viewModel.month(-1)}}){Text("Previous month")}
     Text(ui.month.toString(),style=MaterialTheme.typography.titleMedium)
     TextButton(onClick={guarded{viewModel.month(1)}}){Text("Next month")}
    }
   }
   item("calendar"){
    val offset=ui.month.atDay(1).dayOfWeek.value-1
    val cells=List(offset){null}+ (1..ui.month.lengthOfMonth()).map{it}
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
     Row{listOf("M","T","W","T","F","S","S").forEach{Text(it,Modifier.weight(1f))}}
     cells.chunked(7).forEach{week->
      Row(horizontalArrangement=Arrangement.spacedBy(2.dp)){
       (week+List(7-week.size){null}).forEach{day->
        if(day==null)Spacer(Modifier.weight(1f))else{
         val date=ui.month.atDay(day).toString();val info=calendar?.learning_days?.find{it.date==date}
         Surface(Modifier.weight(1f).heightIn(min=60.dp).testTag("day-"+date),
          color=if(info?.qualified==true)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant){
          Column(Modifier.padding(4.dp)){
           Text(day.toString())
           Text(if(calendar==null)"—" else "${info?.minutes?:0}m",style=MaterialTheme.typography.labelSmall)
           if((info?.practice_completions?:0)>0)Text("•",style=MaterialTheme.typography.labelSmall)
          }
         }
        }
       }
      }
     }
    }
   }
   if(calendar==null)item{Text("No saved calendar for this month. Connect to refresh.")}
   if(ui.snapshot.messages.isNotEmpty())item{Text("Pending activities: ${ui.snapshot.messages.size}",style=MaterialTheme.typography.titleMedium)}
   items(ui.snapshot.messages,key={it.mutationId}){message->
    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){
     Text(if(message.state=="blocked")"Study time needs attention" else "Study time awaiting sync")
     message.error?.let{Text(it)}
     if(message.state=="blocked")TextButton(onClick={guarded{viewModel.retry(message.mutationId)}}){Text("Retry activity")}
     TextButton(onClick={discard=message.mutationId}){Text("Discard activity")}
    }}
   }
  }
 }
}
