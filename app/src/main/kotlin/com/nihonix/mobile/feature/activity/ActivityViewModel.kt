package com.nihonix.mobile.feature.activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.YearMonth
data class ActivityUiState(val snapshot:ActivityState=ActivityState(),val month:YearMonth=YearMonth.now(),val loading:Boolean=true,val error:String?=null)
class ActivityViewModel(val repository:ActivityRepository):ViewModel(){
 private val state=MutableStateFlow(ActivityUiState());val uiState=state.asStateFlow()
 private var owner=repository.account();private var job:Job?=null;private var manualMonth=false
 init{
  viewModelScope.launch{repository.observe().collect{snapshot->
   if(owner!=snapshot.account){job?.cancel();owner=snapshot.account;manualMonth=false;state.value=ActivityUiState(loading=false)}
   state.update{it.copy(snapshot=snapshot)}
  }}
  refresh()
 }
 fun month(offset:Long){val next=state.value.month.plusMonths(offset);if(next.year !in 2020..2100)return
  manualMonth=true;state.update{it.copy(month=next)};refresh()
 }
 fun refresh(){
  job?.cancel();val account=repository.account()?:return
  state.update{it.copy(loading=true,error=null)}
  job=viewModelScope.launch{
   try{
    repository.sync(account)
    val m=state.value.month
    val ok=repository.refresh(m.year,m.monthValue)
    if(repository.account()==account){
     state.update{it.copy(error=if(ok)null else "Could not refresh. Saved activity is shown below.")}
     if(!manualMonth)state.value.snapshot.summary?.today?.take(7)?.let{date->
      val serverMonth=YearMonth.parse(date)
      if(serverMonth!=state.value.month){state.update{it.copy(month=serverMonth)};repository.refresh(serverMonth.year,serverMonth.monthValue)}
     }
    }
   }catch(cancelled:CancellationException){throw cancelled}
   catch(_:Exception){if(repository.account()==account)state.update{it.copy(error="Offline. Saved activity and pending study time are retained.")}}
   finally{if(repository.account()==account)state.update{it.copy(loading=false)}}
  }
 }
 fun retry(id:String){val account=repository.account()?:return;viewModelScope.launch{repository.retry(id,account);refresh()}}
 fun discard(id:String){val account=repository.account()?:return;viewModelScope.launch{repository.discard(id,account);refresh()}}
}
