package com.nihonix.mobile.feature.practice.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nihonix.mobile.feature.practice.domain.*

@Composable fun FlashcardGenerationPanel(vm:PracticeViewModel,folderId:String,onPreview:(GeneratedCards)->Unit){
 val ui by vm.uiState.collectAsState()
 val owner=ui.snapshot.account
 var name by rememberSaveable(owner,folderId){mutableStateOf("")}
 var topic by rememberSaveable(owner,folderId){mutableStateOf("")}
 var count by rememberSaveable(owner,folderId){mutableStateOf("5")}
 var visibleRequests by rememberSaveable(owner,folderId){mutableStateOf(5)}
 fun act(action:()->Unit)=vm.forAccount(owner,action)
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
  QuotaPanel(ui.snapshot){act{vm.refresh()}}
  Text("AI flashcards",style=MaterialTheme.typography.titleMedium)
  Text("Generate a preview, then review and save the cards yourself.")
  OutlinedTextField(name,{name=it.take(255)},label={Text("AI set name")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(topic,{topic=it.take(500)},label={Text("Learning topic")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(count,{count=it.filter(Char::isDigit).take(2)},label={Text("Cards")},
   supportingText={Text("1–${ui.snapshot.quota?.max_items?:15} cards")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
  Button(onClick={act{vm.generateCards(folderId,name,count.toIntOrNull()?:0,topic)}},
   enabled=!ui.busy&&ui.snapshot.quotaFresh&&canGenerate(ui.snapshot.quota,"flashcards")&&name.isNotBlank(),modifier=Modifier.fillMaxWidth()){
   Text(if(ui.busy)"Generating…" else "Generate card preview")
  }
  ui.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
  val requests=ui.snapshot.requests.filter{it.kind=="flashcards"&&runCatching{vm.repository.gson.fromJson(it.payload,com.nihonix.mobile.feature.practice.data.GenerateCards::class.java).folder_id==folderId}.getOrDefault(false)}
  requests.take(visibleRequests).forEach{request->
    GenerationCard(request,ui.busy,{act{vm.check(request.id)}},{act{vm.retry(request.id)}},{act{vm.cancel(request.id)}},
     onPreview={draft->act{onPreview(draft)}})
   }
  if(requests.size>visibleRequests)TextButton(onClick={visibleRequests+=5}){Text("Show more requests")}
 }
}
