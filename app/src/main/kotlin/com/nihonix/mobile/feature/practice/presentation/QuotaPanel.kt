package com.nihonix.mobile.feature.practice.presentation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nihonix.mobile.feature.practice.data.PracticeState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
@Composable fun QuotaPanel(snapshot:PracticeState,onRefresh:()->Unit){
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
  Text("AI allowance",style=MaterialTheme.typography.titleMedium)
  val quota=snapshot.quota
  if(quota==null)Text("AI allowance unavailable")
  else{
   Text("Practice allowance: ${quota.buckets["practice"]?.remaining?:0}")
   Text("Flashcard allowance: ${quota.buckets["flashcards"]?.remaining?:0}")
   if(!quota.provider.configured)Text("AI is not configured on the server.")
   val reset=runCatching{Instant.parse(quota.resets_at).atZone(ZoneId.of(quota.timezone)).format(DateTimeFormatter.ofPattern("HH:mm"))}.getOrDefault(quota.resets_at)
   Text("Reset: $reset ${quota.timezone}")
   if(!snapshot.quotaFresh)Text("Saved allowance may be out of date.")
  }
  TextButton(onClick=onRefresh){Text("Refresh allowance")}
 }}
}
