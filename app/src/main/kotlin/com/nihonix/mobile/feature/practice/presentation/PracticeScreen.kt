package com.nihonix.mobile.feature.practice.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.nihonix.mobile.feature.practice.domain.*

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun PracticeScreen(viewModel:PracticeViewModel,onBack:()->Unit={},onOpenSet:(String)->Unit=viewModel::openSet){
 val ui by viewModel.uiState.collectAsState()
 val owner=ui.snapshot.account
 var visibleRequests by rememberSaveable(owner){mutableStateOf(5)}
 val guarded:(()->Unit)->Unit={action->viewModel.forAccount(owner,action)}
 val selected=ui.snapshot.sets.find{it.id==ui.selectedId}
 var discard by remember(owner,ui.selectedId){mutableStateOf(false)}
 BackHandler(ui.selectedId!=null){viewModel.leave(onBack)}
 if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("Discard queued answers?")},
  text={Text("Your local answers will be removed and the server practice will be reloaded.")},
  confirmButton={TextButton(onClick={discard=false;guarded{ui.selectedId?.let(viewModel::discard)}}){Text("Discard")}},
  dismissButton={TextButton(onClick={discard=false}){Text("Keep answers")}})
 Scaffold(topBar={TopAppBar(title={Text(if(ui.selectedId==null)"AI Practice" else selected?.topic_title?.get("en")?:"Practice")},
  navigationIcon={TextButton(onClick={viewModel.leave(onBack)}){Text("Back")}})}){padding->
  LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal=16.dp),
   verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)){
   ui.error?.let{item("error"){Text(it,color=MaterialTheme.colorScheme.error)}}
   ui.notice?.let{item("notice"){Text(it)}}
   if(ui.selectedId!=null){
    if(selected==null||selected.exercises.isNullOrEmpty())item{Text(if(ui.loading)"Loading practice…" else "Connect to download these questions.")}
    else if(selected.status=="completed"&&selected.result!=null){
     val result=selected.result
     item("score"){Text("Score: ${result.correct_count}/${result.total_exercises} (${result.accuracy}%)",style=MaterialTheme.typography.headlineSmall)}
     item{Text("Completion recorded once.")}
     items(result.results,key={it.exercise_id}){answer->
      Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
       val question=selected.exercises.find{it.id==answer.exercise_id}
       Text(question?.question?.text.orEmpty())
       Text(if(answer.is_correct)"Correct" else "Incorrect")
       Text("Your answer: ${answerLabel(question,answer.user_answer)}")
       Text("Correct answer: ${answerLabel(question,answer.correct_answer)}")
       Text(answer.explanation["en"].orEmpty())
       Text(answer.explanation["vi"].orEmpty())
      }}
     }
    }else if(selected.status=="completed"){
     item("legacy_score"){Text("Score: ${selected.correct_count?:0}/${selected.total_exercises} (${selected.accuracy?:0}%)",style=MaterialTheme.typography.headlineSmall)}
     item{Text("Previously completed practice. Stored results are shown below.")}
     items(selected.exercises,key={it.id}){question->
      Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
       Text(question.question.text)
       Text(when(question.is_correct){true->"Correct";false->"Incorrect";null->"Grading unavailable"})
       Text("Your answer: ${answerLabel(question,question.user_answer)}")
       Text("Correct answer: ${answerLabel(question,question.correct_answer)}")
       question.explanation?.get("en")?.let{Text(it)}
       question.explanation?.get("vi")?.let{Text(it)}
      }}
     }
    }else{
     val locked=ui.snapshot.drafts[selected.id]?.requestId!=null
     items(selected.exercises,key={it.id}){question->
      Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
       Text("${question.order}. ${question.question.text}",style=MaterialTheme.typography.titleMedium)
       question.question.hint?.let{Text(it)}
       if(question.options.isNullOrEmpty())OutlinedTextField(value=ui.answers[question.id].orEmpty(),
        onValueChange={value->guarded{viewModel.answer(question.id,value)}},enabled=!locked&&!ui.busy,
        label={Text("Your answer")},modifier=Modifier.fillMaxWidth(),maxLines=4)
       else question.options.forEach{option->
        FilterChip(selected=ui.answers[question.id]==option.id.toString(),enabled=!locked&&!ui.busy,
         onClick={guarded{viewModel.answer(question.id,option.id.toString())}},label={Text(option.text)},modifier=Modifier.fillMaxWidth())
       }
      }}
     }
     if(locked)item("pending"){
      Text("Submission pending. Answers are retained on this device.")
      ui.snapshot.messages.find{it.entityKey=="practice:"+selected.id}?.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
      TextButton(onClick={guarded{viewModel.sync()}}){Text("Retry submission")}
      TextButton(onClick={discard=true}){Text("Discard queued answers")}
     }else item("submit"){Button(onClick={guarded{viewModel.submit()}},enabled=!ui.busy,modifier=Modifier.fillMaxWidth()){Text("Submit answers")}}
    }
   }else{
    item("quota"){QuotaPanel(ui.snapshot){guarded{viewModel.refresh()}}}
    item("generator"){
     Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
      Text("Create practice",style=MaterialTheme.typography.titleLarge)
      TopicPicker("Topic",ui.topics,ui.topicId){guarded{viewModel.selectTopic(it)}}
      FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
       FilterChip(selected=ui.type=="fill_blank",onClick={guarded{viewModel.selectType("fill_blank")}},label={Text("Fill in blank")})
       FilterChip(selected=ui.type=="multiple_choice",onClick={guarded{viewModel.selectType("multiple_choice")}},label={Text("Multiple choice")})
      }
      OutlinedTextField(value=ui.count,onValueChange={value->guarded{viewModel.count(value)}},label={Text("Questions")},
       supportingText={Text("1–${ui.snapshot.quota?.max_items?:15} questions")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
      Button(onClick={guarded{viewModel.generatePractice()}},enabled=!ui.busy&&ui.topicId.isNotBlank()&&ui.snapshot.quotaFresh&&canGenerate(ui.snapshot.quota,"practice"),
       modifier=Modifier.fillMaxWidth()){Text(if(ui.busy)"Generating…" else "Generate practice")}
     }
    }
    items(ui.snapshot.requests.take(visibleRequests),key={"request:"+it.id}){request->
     GenerationCard(request,ui.busy,onCheck={guarded{viewModel.check(request.id)}},
      onRetry={guarded{viewModel.retry(request.id)}},onCancel={guarded{viewModel.cancel(request.id)}},
      onOpen={id->guarded{onOpenSet(id)}})
    }
    if(ui.snapshot.requests.size>visibleRequests)item("more_requests"){TextButton(onClick={visibleRequests+=5}){Text("Show more requests")}}
    item("filters"){
     Text("Saved practice",style=MaterialTheme.typography.titleLarge)
     TopicPicker("Filter topic",listOf(PracticeTopic("","All topics"))+ui.topics,ui.filterTopic.orEmpty()){id->guarded{viewModel.filter(topic=id.ifBlank{null})}}
     FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
      listOf(null to "All types","fill_blank" to "Fill in blank","multiple_choice" to "Multiple choice").forEach{(type,label)->
       FilterChip(selected=ui.filterType==type,onClick={guarded{viewModel.filter(type=type)}},label={Text(label)})}
     }
     FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
      listOf(null to "All statuses","in_progress" to "In progress","completed" to "Completed").forEach{(status,label)->
       FilterChip(selected=ui.filterStatus==status,onClick={guarded{viewModel.filter(status=status)}},label={Text(label)})}
     }
    }
    val sets=ui.snapshot.sets.filter{(ui.filterTopic==null||it.topic_id==ui.filterTopic)&&(ui.filterType==null||it.exercise_type==ui.filterType)&&(ui.filterStatus==null||it.status==ui.filterStatus)}
     .sortedWith(compareByDescending<PracticeSet>{it.created_at.orEmpty()}.thenBy{it.id})
    if(sets.isEmpty())item("empty"){Text("No saved practice yet.")}
    items(sets,key={"set:"+it.id}){set->
     Card(onClick={guarded{onOpenSet(set.id)}},modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){
      Text(set.topic_title["en"]?:set.topic_title.values.firstOrNull().orEmpty(),style=MaterialTheme.typography.titleMedium)
      Text("${set.exercise_type.replace('_',' ')} · ${set.total_exercises} questions · ${set.status.replace('_',' ')}")
     }}
    }
    if(ui.hasMore)item("more"){TextButton(onClick={guarded{viewModel.more()}}){Text("Load more practice")}}
   }
  }
 }
}
private fun answerLabel(question:PracticeQuestion?,value:com.google.gson.JsonElement?):String {
 if(value==null||value.isJsonNull)return "No answer"
 if(!value.isJsonPrimitive)return "Answer unavailable"
 if(value.asJsonPrimitive.isNumber)question?.options?.find{it.id==value.asInt}?.let{return it.text}
 return value.asString
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TopicPicker(label:String,topics:List<PracticeTopic>,selected:String,onSelect:(String)->Unit){
 var expanded by remember{mutableStateOf(false)}
 ExposedDropdownMenuBox(expanded=expanded,onExpandedChange={expanded=it}){
  OutlinedTextField(value=topics.find{it.id==selected}?.title?:"Select a topic",onValueChange={},readOnly=true,label={Text(label)},
   trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expanded)},modifier=Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
  ExposedDropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){topics.forEach{topic->
   DropdownMenuItem(text={Text(topic.title)},onClick={expanded=false;onSelect(topic.id)})
  }}
 }
}
