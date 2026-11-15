package com.nihonix.mobile.feature.practice.presentation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.nihonix.mobile.feature.practice.data.StoredGeneration
import com.nihonix.mobile.feature.practice.domain.GeneratedCards
@Composable fun GenerationCard(request:StoredGeneration,busy:Boolean,onCheck:()->Unit,onRetry:()->Unit,onCancel:()->Unit,
 onOpen:(String)->Unit={},onPreview:((GeneratedCards)->Unit)?=null){
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
  Text(if(request.kind=="flashcards")"Generated flashcards" else "Generated practice",style=MaterialTheme.typography.titleMedium)
  val status=request.snapshot?.status
  Text(when(status){"succeeded"->"Ready";"failed"->"Generation failed; no successful allowance charged.";"cancelled"->"Cancelled";else->"Request saved. Check its status before starting another."})
  request.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
  if(status=="succeeded"){
   if(request.kind=="flashcards"){
    if(onPreview!=null)TextButton(onClick={request.snapshot.result?.let{onPreview(Gson().fromJson(it,GeneratedCards::class.java))}}){Text("Review generated cards")}
    else Text("Open the selected flashcard folder to preview these cards.")
   }else TextButton(onClick={request.snapshot?.result?.get("id")?.asString?.let(onOpen)}){Text("Open generated practice")}
  }else if(status !in listOf("failed","cancelled")){
   TextButton(onClick=onCheck,enabled=!busy){Text("Check result")}
   TextButton(onClick=onRetry,enabled=!busy){Text("Retry same request")}
   TextButton(onClick=onCancel){Text("Cancel request")}
  }
 }}
}
