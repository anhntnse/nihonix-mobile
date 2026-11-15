package com.nihonix.mobile.feature.flashcards.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.nihonix.mobile.feature.flashcards.data.FlashcardsRepository
import com.nihonix.mobile.feature.flashcards.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FlashcardReviewUiState(val title:String="",val card:CardDraft?=null,val position:Int=0,val total:Int=0,val back:Boolean=false,val loaded:Boolean=false)
class FlashcardReviewViewModel(private val repository:FlashcardsRepository,private val saved:SavedStateHandle=SavedStateHandle()):ViewModel() {
    private val state=MutableStateFlow(FlashcardReviewUiState())
    val uiState=state.asStateFlow()
    private var cards:List<CardDraft> = emptyList()
    private var ids:List<String> = saved.get<ArrayList<String>>("reviewIds")?.toList().orEmpty()
    private var position:Int=saved["position"]?:0
    private var back:Boolean=saved["back"]?:false
    private var account:String?=saved["account"]
    init {
        viewModelScope.launch {
            repository.observe().collect { snapshot ->
                if(account!=null&&account!=snapshot.account){ids=emptyList();position=0;back=false}
                account=snapshot.account
                saved["account"]=snapshot.account
                val setId=saved.get<String>("setId").orEmpty()
                val set=snapshot.sets.find{it.id==setId&&!it.deleted}
                cards=if(set==null)emptyList() else repository.cards(snapshot,setId)
                val currentId=ids.getOrNull(position)
                if(ids.isEmpty()&&cards.isNotEmpty())ids=cards.map{it.id}
                ids=ids.filter{id->cards.any{it.id==id}}
                position=if(currentId!=null&&currentId in ids)ids.indexOf(currentId) else position.coerceIn(0,ids.size)
                if(ids.getOrNull(position)!=currentId)back=false
                render(set?.name.orEmpty())
            }
        }
    }
    private fun render(title:String=state.value.title) {
        saved["reviewIds"]=ArrayList(ids);saved["position"]=position;saved["back"]=back
        state.value=FlashcardReviewUiState(title,ids.getOrNull(position)?.let{id->cards.find{it.id==id}},position,ids.size,back,true)
    }
    fun flip(){if(state.value.card!=null){back=!back;render()}}
    fun next(){position=(position+1).coerceAtMost(ids.size);back=false;render()}
    fun previous(){position=(position-1).coerceAtLeast(0);back=false;render()}
    fun restart(){ids=cards.map{it.id};position=0;back=false;render()}
}
