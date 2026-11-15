package com.nihonix.mobile.feature.practice.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.practice.data.*
import com.nihonix.mobile.feature.practice.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PracticeTopic(val id:String,val title:String)
data class PracticeUiState(val snapshot:PracticeState=PracticeState(),val topics:List<PracticeTopic> = emptyList(),
 val topicId:String="",val type:String="fill_blank",val count:String="10",val selectedId:String?=null,
 val filterTopic:String?=null,val filterType:String?=null,val filterStatus:String?=null,val hasMore:Boolean=false,val page:Int=1,
 val answers:Map<String,String> = emptyMap(),val busy:Boolean=false,val loading:Boolean=true,val error:String?=null,val notice:String?=null)
class PracticeViewModel(val repository:PracticeRepository,private val saved:SavedStateHandle=SavedStateHandle(),
 private val loadTopics:suspend()->List<PracticeTopic> = {emptyList()},
 private val syncFlashcards:suspend(String)->Boolean={true}):ViewModel(){
 private val state=MutableStateFlow(PracticeUiState(topicId=saved.get<String>("topicId").orEmpty(),selectedId=saved["sessionId"]))
 val uiState=state.asStateFlow()
 private var owner:String?=saved["practiceOwner"]
 private var draftLoaded:String?=null
 private data class Pending(val owner:String,val id:String,val answers:Map<String,String>,val version:Long)
 private val writes=Channel<Pending>(Channel.CONFLATED)
 private val draftLock=Mutex()
 private var version=0L
 private var generationJob:Job?=null
 init{
  viewModelScope.launch(Dispatchers.IO){
   for(write in writes)draftLock.withLock{
    if(write.version==version&&repository.account()==write.owner)repository.saveAnswers(write.id,write.answers,write.owner)
   }
  }
  viewModelScope.launch{
   repository.observe().collect{snapshot->
    if(owner!=null&&owner!=snapshot.account){
     generationJob?.cancel();version++;draftLoaded=null;saved["sessionId"]=null
     state.value=PracticeUiState(loading=false)
    }
    owner=snapshot.account;saved["practiceOwner"]=snapshot.account
    val id=state.value.selectedId
    val firstDraft=id!=null&&draftLoaded!=id&&snapshot.sets.any{it.id==id&&!it.exercises.isNullOrEmpty()}
    val answers=if(firstDraft){draftLoaded=id;snapshot.drafts[id]?.answers.orEmpty()}else state.value.answers
    state.update{it.copy(snapshot=snapshot,answers=answers,loading=false)}
   }
  }
  refresh()
  state.value.selectedId?.let{openSet(it)}
 }
 fun forAccount(expected:String?,action:()->Unit){if(expected!=null&&expected==repository.account()&&expected==state.value.snapshot.account)action()}
 private suspend fun safely(call:suspend()->Unit){
  try{call()}catch(cancelled:CancellationException){throw cancelled}
  catch(_:Exception){state.update{it.copy(error="Connection unavailable. Saved drafts and pending requests are retained.")}}
 }
 fun refresh(){
  viewModelScope.launch{safely{repository.refreshQuota().let{if(it is Resource.Error)state.update{s->s.copy(error=it.message)}}}}
  val requestedOwner=repository.account()
  viewModelScope.launch{safely{
   val topics=loadTopics()
   if(requestedOwner!=repository.account())return@safely
   state.update{it.copy(topics=topics,topicId=it.topicId.ifBlank{topics.firstOrNull()?.id.orEmpty()})}
   reloadLibrary()
  }}
 }
 private suspend fun reloadLibrary(page:Int=1){
  val before=state.value
  when(val result=repository.loadLibrary(before.filterTopic,before.filterType,before.filterStatus,page)){
   is Resource.Success->state.update{it.copy(page=page,hasMore=result.data.has_more)}
   is Resource.Error->state.update{it.copy(error=result.message)}
   else->Unit
  }
 }
 fun selectTopic(id:String){saved["topicId"]=id;state.update{it.copy(topicId=id)}}
 fun selectType(type:String){state.update{it.copy(type=type)}}
 fun count(value:String){state.update{it.copy(count=value.filter(Char::isDigit).take(2))}}
 fun filter(topic:String?=state.value.filterTopic,type:String?=state.value.filterType,status:String?=state.value.filterStatus){
  state.update{it.copy(filterTopic=topic,filterType=type,filterStatus=status)}
  viewModelScope.launch{safely{reloadLibrary()}}
 }
 fun more(){viewModelScope.launch{safely{reloadLibrary(state.value.page+1)}}}
 fun openSet(id:String){
  saved["sessionId"]=id;draftLoaded=null
  state.update{it.copy(selectedId=id,answers=emptyMap(),error=null,notice=null)}
  viewModelScope.launch{safely{
   when(val result=repository.loadSet(id)){is Resource.Error->state.update{it.copy(error=result.message)};else->Unit}
  }}
 }
 fun answer(id:String,value:String){
  val before=state.value;val account=before.snapshot.account?:return;val setId=before.selectedId?:return
  if(before.snapshot.drafts[setId]?.requestId!=null)return
  val answers=before.answers+(id to value.take(200));state.update{it.copy(answers=answers,error=null)}
  version++;writes.trySend(Pending(account,setId,answers,version))
 }
 fun submit(){
  val before=state.value;val account=before.snapshot.account?:return;val id=before.selectedId?:return
  val set=before.snapshot.sets.find{it.id==id}?:return
  validateAnswers(set,answerValues(set,before.answers))?.let{error->state.update{it.copy(error=error)};return}
  if(before.busy)return
  state.update{it.copy(busy=true,error=null)}
  viewModelScope.launch{
   try{
    draftLock.withLock{
     version++
     val savedDraft=repository.saveAnswers(id,before.answers,account)
     if(savedDraft is Resource.Error){state.update{it.copy(error=savedDraft.message)};return@withLock}
     val submitted=repository.submit(id,account)
     if(submitted is Resource.Error)state.update{it.copy(error=submitted.message)}
    }
    if(repository.account()==account)repository.sync(account)
   }catch(cancelled:CancellationException){throw cancelled}
   catch(_:Exception){if(repository.account()==account)state.update{it.copy(notice="Answers are queued. They will submit when connected.")}}
   finally{if(repository.account()==account)state.update{it.copy(busy=false)}}
  }
 }
 fun sync(){repository.requestSync();viewModelScope.launch{safely{repository.account()?.let{repository.sync(it)}}}}
 fun discard(id:String){val account=state.value.snapshot.account?:return;viewModelScope.launch{safely{
  version++;draftLock.withLock{repository.discardSubmission(id,account)};draftLoaded=null;state.update{it.copy(answers=emptyMap(),error=null)}
  repository.loadSet(id)
 }}}
 fun leave(onBack:()->Unit){
  val before=state.value;val account=before.snapshot.account;val id=before.selectedId
  viewModelScope.launch{
   if(account!=null&&id!=null&&before.snapshot.sets.find{it.id==id}?.status!="completed"&&before.snapshot.drafts[id]?.requestId==null){
    version++;draftLock.withLock{repository.saveAnswers(id,before.answers,account)}
   }
   saved["sessionId"]=null;draftLoaded=null;state.update{it.copy(selectedId=null,answers=emptyMap())};onBack()
  }
 }
 private fun generation(action:suspend()->Resource<*>){
  val account=repository.account()?:return
  if(state.value.busy)return
  state.update{it.copy(busy=true,error=null,notice=null)}
  generationJob=viewModelScope.launch{
   try{
    when(val result=action()){
     is Resource.Error->if(repository.account()==account)state.update{it.copy(error=result.message)}
     else->Unit
    }
   }catch(cancelled:CancellationException){throw cancelled}
   catch(_:Exception){if(repository.account()==account)state.update{it.copy(error="Connection interrupted. Check the saved request before starting another generation.")}}
   finally{
    if(repository.account()==account){
     state.update{it.copy(busy=false)}
     viewModelScope.launch{safely{repository.refreshQuota()}}
    }
   }
  }
 }
 fun generatePractice(){
  val current=state.value
  if(!current.snapshot.quotaFresh||!canGenerate(current.snapshot.quota,"practice")){state.update{it.copy(error="Refresh AI allowance before generating.")};return}
  generation{repository.startPractice(current.type,current.topicId,current.count.toIntOrNull()?:0)}
 }
 fun generateCards(folder:String,name:String,count:Int,topic:String){
  val account=repository.account()?:return
  if(!state.value.snapshot.quotaFresh||!canGenerate(state.value.snapshot.quota,"flashcards")){state.update{it.copy(error="Refresh AI allowance before generating.")};return}
  generation{
   if(!syncFlashcards(account))Resource.Error("Connect and sync this folder first.")
   else repository.startCards(folder,name,count,topic)
  }
 }
 fun check(id:String){generation{repository.checkGeneration(id)}}
 fun retry(id:String){generation{repository.retryGeneration(id)}}
 fun cancel(id:String){
  generationJob?.cancel()
  val account=repository.account()?:return
  viewModelScope.launch{safely{
   val result=repository.cancelGeneration(id)
   if(repository.account()==account){
    state.update{it.copy(busy=false,error=(result as? Resource.Error)?.message,notice=if(result is Resource.Success)"Request status updated." else null)}
    repository.refreshQuota()
   }
  }}
 }
}
