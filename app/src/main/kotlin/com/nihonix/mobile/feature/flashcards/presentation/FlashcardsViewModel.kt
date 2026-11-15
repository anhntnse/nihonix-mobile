package com.nihonix.mobile.feature.flashcards.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.flashcards.data.*
import com.nihonix.mobile.feature.flashcards.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class SetEditor(val id:String,val folderId:String,val name:String,val cards:List<CardDraft>,val order:Int=1,val preview:Boolean=false)
data class FlashcardsUiState(val snapshot:FlashcardsState=FlashcardsState(),val folderId:String?=null,val setId:String?=null,
    val loading:Boolean=true,val saving:Boolean=false,val editor:SetEditor?=null,val editorLoading:Boolean=false,
    val error:String?=null,val notice:String?=null)
class FlashcardsViewModel(private val repository:FlashcardsRepository,private val saved:SavedStateHandle=SavedStateHandle()):ViewModel() {
    private val state=MutableStateFlow(FlashcardsUiState(folderId=saved["folderId"],setId=saved["setId"]))
    val uiState=state.asStateFlow()
    private data class PendingDraft(val owner:String,val editor:SetEditor,val generation:Long)
    private val writes=Channel<PendingDraft>(Channel.CONFLATED)
    private val draftLock=Mutex()
    @Volatile private var generation=0L
    private var owner:String?=saved["owner"]
    init {
        viewModelScope.launch(Dispatchers.IO) {
            for(draft in writes) draftLock.withLock {
                if(draft.generation==generation&&state.value.editor?.id==draft.editor.id&&repository.account()==draft.owner)
                    repository.saveDraft(draft.owner,draft.editor.id,repository.gson.toJson(draft.editor))
            }
        }
        viewModelScope.launch {
            repository.observe().collect { snapshot ->
                if(owner!=null&&owner!=snapshot.account) {
                    generation++;saved["editorId"]=null;saved["folderId"]=null;saved["setId"]=null
                    state.value=FlashcardsUiState()
                }
                owner=snapshot.account
                saved["owner"]=snapshot.account
                state.update { it.copy(snapshot=snapshot,loading=false) }
                val draftId=saved.get<String>("editorId")
                if(draftId!=null&&snapshot.account!=null&&state.value.editor==null&&!state.value.editorLoading) restoreEditor(snapshot.account,draftId)
            }
        }
        repository.requestSync()
    }
    private fun restoreEditor(account:String,id:String) {
        state.update{it.copy(editorLoading=true)}
        viewModelScope.launch {
            val payload=repository.draft(account,id)
            if(repository.account()!=account||saved.get<String>("editorId")!=id)return@launch
            val editor=withContext(Dispatchers.Default) {payload?.let{runCatching{repository.gson.fromJson(it,SetEditor::class.java)}.getOrNull()}}
            if(editor==null)saved["editorId"]=null
            state.update{it.copy(editor=editor,editorLoading=false)}
        }
    }
    fun forAccount(expected:String?,action:()->Unit) {
        if(expected!=null&&expected==repository.account()&&expected==state.value.snapshot.account)action()
    }
    fun openFolder(id:String) {saved["folderId"]=id;saved["setId"]=null;state.update{it.copy(folderId=id,setId=null,error=null,notice=null)}}
    fun openSet(id:String) {saved["setId"]=id;state.update{it.copy(setId=id,error=null,notice=null)}}
    fun back():Boolean {
        if(state.value.editor!=null){closeEditor();return true}
        if(state.value.setId!=null){saved["setId"]=null;state.update{it.copy(setId=null,error=null)};return true}
        if(state.value.folderId!=null){saved["folderId"]=null;state.update{it.copy(folderId=null,error=null)};return true}
        return false
    }
    private fun action(call:suspend (String)->Resource<Unit>) {
        val account=state.value.snapshot.account ?: return
        if(state.value.saving)return
        state.update{it.copy(saving=true,error=null,notice=null)}
        viewModelScope.launch {
            try {
                val result=call(account)
                if(repository.account()==account) state.update{it.copy(saving=false,error=(result as? Resource.Error)?.message,
                    notice=if(result is Resource.Success)result.message else null)}
            } catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception){state.update{it.copy(saving=false,error="Could not save flashcards. Your draft is retained.")}}
        }
    }
    fun saveFolder(id:String?,name:String,description:String) = action { account ->
        repository.saveFolder(FolderDraft(id?:UUID.randomUUID().toString(),name,description),account)
    }
    fun deleteFolder(id:String)=action{account->repository.deleteFolder(id,account)}
    fun deleteSet(id:String)=action{account->repository.deleteSet(id,account)}
    fun useServer(kind:String,id:String)=action{account->repository.useServer(kind,id,account)}
    fun saveCopy(kind:String,id:String)=action{account->repository.saveCopy(kind,id,account)}
    fun sync(){repository.requestSync();state.update{it.copy(notice="Sync queued. Updates will appear when connected.",error=null)}}
    fun editSet(id:String?=state.value.setId) {
        val account=state.value.snapshot.account ?: return
        val folder=state.value.folderId ?: return
        val existing=state.value.snapshot.sets.find{it.id==id}
        state.update{it.copy(editorLoading=true,error=null)}
        viewModelScope.launch {
            val editor=withContext(Dispatchers.Default) {
                SetEditor(existing?.id?:UUID.randomUUID().toString(),existing?.folderId?:folder,existing?.name.orEmpty(),
                    if(existing==null) listOf(newCard()) else repository.cards(state.value.snapshot,existing.id),existing?.position?:1)
            }
            if(repository.account()!=account)return@launch
            generation++;saved["editorId"]=editor.id
            state.update{it.copy(editor=editor,editorLoading=false)}
            persistEditor(editor)
        }
    }
    fun previewGenerated(cards:List<CardDraft>) {
        val folder=state.value.folderId ?: return
        val drafts=cards.map{it.copy(id=UUID.randomUUID().toString())}
        val editor=SetEditor(UUID.randomUUID().toString(),folder,"Generated preview",drafts,preview=true)
        generation++;saved["editorId"]=editor.id
        state.update{it.copy(editor=editor,error=null)}
        persistEditor(editor)
    }
    private fun newCard()=CardDraft(UUID.randomUUID().toString(),CardFace(""),CardFace(""))
    private fun persistEditor(editor:SetEditor) {
        val account=state.value.snapshot.account ?: return
        writes.trySend(PendingDraft(account,editor,generation))
    }
    fun editorName(name:String) {state.value.editor?.let{updateEditor(it.copy(name=name.take(255)))}}
    fun addCard() {state.value.editor?.takeIf{it.cards.size<200}?.let{updateEditor(it.copy(cards=it.cards+newCard().copy(order=it.cards.size+1)))}}
    fun removeCards(ids:Set<String>) {state.value.editor?.let{updateEditor(it.copy(cards=it.cards.filterNot{card->card.id in ids}.mapIndexed{index,card->card.copy(order=index+1)}))}}
    fun cardText(id:String,front:Boolean,text:String) {
        state.value.editor?.let{editor->
            updateEditor(editor.copy(cards=editor.cards.map{card->if(card.id!=id)card else if(front)card.copy(front=card.front.copy(text=text.take(4000))) else card.copy(back=card.back.copy(text=text.take(4000)))}))
        }
    }
    private fun updateEditor(editor:SetEditor) {state.update{it.copy(editor=editor,error=null)};persistEditor(editor)}
    fun closeEditor() {
        val account=state.value.snapshot.account ?: return
        val id=state.value.editor?.id ?: return
        generation++;saved["editorId"]=null;state.update{it.copy(editor=null,error=null)}
        viewModelScope.launch(Dispatchers.IO){draftLock.withLock{repository.clearDraft(account,id)}}
    }
    fun saveEditor() {
        val editor=state.value.editor ?: return
        val account=state.value.snapshot.account ?: return
        val draft=SetDraft(editor.id,editor.folderId,editor.name,editor.cards,editor.order)
        validateSet(draft)?.let{message->state.update{it.copy(error=message)};return}
        if(state.value.saving)return
        state.update{it.copy(saving=true,error=null)}
        viewModelScope.launch {
            val result=repository.saveSet(draft,account)
            if(repository.account()!=account)return@launch
            if(result is Resource.Success) {
                generation++;saved["editorId"]=null
                state.update{it.copy(editor=null,saving=false,notice=result.message)}
                withContext(Dispatchers.IO){draftLock.withLock{repository.clearDraft(account,editor.id)}}
            } else state.update{it.copy(saving=false,error=(result as? Resource.Error)?.message)}
        }
    }
    fun deleteCards(ids:Set<String>) {
        val snapshot=state.value.snapshot;val set=snapshot.sets.find{it.id==state.value.setId} ?: return
        val cards=repository.cards(snapshot,set.id).filterNot{it.id in ids}.mapIndexed{index,card->card.copy(order=index+1)}
        action{account->repository.saveSet(SetDraft(set.id,set.folderId,set.name,cards,set.position),account)}
    }
}
