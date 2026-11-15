package com.nihonix.mobile.feature.flashcards.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.nihonix.mobile.feature.flashcards.data.*
import com.nihonix.mobile.feature.flashcards.domain.*

@Composable
fun FlashcardsScreen(viewModel:FlashcardsViewModel,onBack:()->Unit,onReview:(String)->Unit,
    onFolder:(String)->Unit=viewModel::openFolder,onSet:(String)->Unit=viewModel::openSet) {
    val state by viewModel.uiState.collectAsState()
    key(state.snapshot.account) { FlashcardsContent(viewModel,state,onBack,onReview,onFolder,onSet) }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashcardsContent(vm:FlashcardsViewModel,state:FlashcardsUiState,onBack:()->Unit,onReview:(String)->Unit,
    onFolder:(String)->Unit,onSet:(String)->Unit) {
    val data=state.snapshot
    val folder=data.folders.find{it.id==state.folderId&&!it.deleted}
    val set=data.sets.find{it.id==state.setId&&!it.deleted}
    val title=if(state.setId!=null)set?.name?:"Set unavailable" else if(state.folderId!=null)folder?.name?:"Folder unavailable" else "Flashcards"
    var folderDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var folderSubmitting by rememberSaveable { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }
    var selected by rememberSaveable(state.setId) { mutableStateOf(listOf<String>()) }
    fun act(action:()->Unit)=vm.forAccount(data.account,action)
    LaunchedEffect(state.saving,state.notice,state.error) {
        if(folderSubmitting&&!state.saving&&state.error==null&&state.notice!=null){folderSubmitting=false;folderDialog=null}
    }
    if(folderDialog!=null) {
        val existing=data.folders.find{it.id==folderDialog}
        FolderEditor(existing,state.saving,state.error,onDismiss={if(!state.saving)folderDialog=null},onSave={name,description->
            folderSubmitting=true;act{vm.saveFolder(existing?.id,name,description)}
        })
    }
    confirm?.let { target ->
        val destructive=target.startsWith("folder:")||target.startsWith("set:")||target=="cards"
        AlertDialog(onDismissRequest={confirm=null},title={Text(if(destructive)"Delete flashcards?" else "Use server copy?")},
            text={Text(when {
                target.startsWith("folder:")->"This deletes the folder and all its sets and cards. The deletion will sync when connected."
                target.startsWith("set:")->"This deletes the set and all its cards."
                target=="cards"->"Delete ${selected.size} selected cards from this set?"
                else->"Discard the local edits for this item and restore the latest downloaded server copy?"
            })},
            confirmButton={TextButton(onClick={
                confirm=null
                act { when {
                    target.startsWith("folder:")->vm.deleteFolder(target.substringAfter(':'))
                    target.startsWith("set:")->vm.deleteSet(target.substringAfter(':'))
                    target=="cards"->{vm.deleteCards(selected.toSet());selected=emptyList()}
                    else->{val parts=target.split(':');vm.useServer(parts[1],parts[2])}
                } }
            }){Text(if(destructive)"Delete" else "Use server")}},
            dismissButton={TextButton(onClick={confirm=null}){Text("Cancel")}})
    }
    if(state.editor!=null) SetEditorDialog(state,vm) { action->act(action) }
    Scaffold(topBar={TopAppBar(title={Text(title,maxLines=1)},navigationIcon={
        IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back")}
    },actions={TextButton(onClick={act(vm::sync)}){Text("Sync")}})}) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item("status") {
                Text("Pending: ${data.messages.count{it.state=="pending"}} · Needs attention: ${data.messages.count{it.state=="blocked"}}")
                Text("Changes are saved on this device before syncing.",style=MaterialTheme.typography.bodySmall)
                state.notice?.let{Text(it)}
                state.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
                if(state.loading||state.saving||state.editorLoading)LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            items(data.messages.filter{it.state=="blocked"}.distinctBy{it.entityKey},key={"issue:"+it.entityKey}) { message ->
                val parts=message.entityKey.split(':')
                if(parts.size==3) {
                    val kind=parts[1];val id=parts[2]
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("Sync needs attention",style=MaterialTheme.typography.titleMedium)
                        Text(if(kind=="folder")data.folders.find{it.id==id}?.name?:id else data.sets.find{it.id==id}?.name?:id)
                        Text(message.error?:"This item changed on another device.")
                        TextButton(onClick={confirm="server:$kind:$id"},enabled=!state.saving){Text("Use server copy")}
                        OutlinedButton(onClick={act{vm.saveCopy(kind,id)}},enabled=!state.saving){Text("Save local copy")}
                    } }
                }
            }
            when {
                state.folderId==null -> {
                    item { Button(onClick={folderSubmitting=false;folderDialog=""},enabled=!state.saving){Text("New folder")} }
                    val folders=data.folders.filterNot{it.deleted}
                    if(!state.loading&&folders.isEmpty())item {Text("No folders yet. Create one to start your personal flashcards.")}
                    items(folders,key={it.id}) { row ->
                        Card(onClick={onFolder(row.id)},modifier=Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                Text(row.name,style=MaterialTheme.typography.titleLarge)
                                if(row.description.isNotBlank())Text(row.description)
                                Text("${data.sets.count{it.folderId==row.id&&!it.deleted}} sets")
                            }
                        }
                    }
                }
                state.setId==null -> {
                    if(folder==null)item {Text("This folder is unavailable. Pending changes can be recovered above.")}
                    else {
                        item {
                            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                if(folder.description.isNotBlank())Text(folder.description)
                                Button(onClick={act{vm.editSet(null)}},enabled=!state.saving&&!state.editorLoading){Text("New set")}
                                Row {TextButton(onClick={folderSubmitting=false;folderDialog=folder.id}){Text("Edit folder")}
                                    TextButton(onClick={confirm="folder:${folder.id}"}){Text("Delete folder")}}
                            }
                        }
                        val sets=data.sets.filter{it.folderId==folder.id&&!it.deleted}
                        if(sets.isEmpty())item {Text("No sets in this folder.")}
                        items(sets,key={it.id}) { row ->
                            Card(onClick={onSet(row.id)},modifier=Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    Text(row.name,style=MaterialTheme.typography.titleLarge)
                                    Text("${data.cards.count{it.setId==row.id}} cards")
                                }
                            }
                        }
                    }
                }
                else -> {
                    if(set==null)item {Text("This set is unavailable. Pending changes can be recovered above.")}
                    else {
                        val cards=data.cards.filter{it.setId==set.id}
                        item {
                            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                Button(onClick={onReview(set.id)},enabled=cards.isNotEmpty()){Text("Review cards")}
                                Row {TextButton(onClick={act{vm.editSet(set.id)}},enabled=!state.editorLoading){Text("Edit set")}
                                    TextButton(onClick={confirm="set:${set.id}"}){Text("Delete set")}}
                                OutlinedButton(onClick={confirm="cards"},enabled=selected.isNotEmpty()&&!state.saving){Text("Delete selected (${selected.size})")}
                            }
                        }
                        if(cards.isEmpty())item {Text("No cards yet. Edit this set to add cards.")}
                        items(cards,key={it.id}) { card ->
                            val gson=remember{Gson()}
                            val front=remember(card.front){gson.fromJson(card.front,CardFace::class.java)}
                            val back=remember(card.back){gson.fromJson(card.back,CardFace::class.java)}
                            Card(Modifier.fillMaxWidth()) {Row(Modifier.padding(12.dp)) {
                                Checkbox(checked=card.id in selected,onCheckedChange={checked->selected=if(checked)(selected+card.id).distinct() else selected-card.id},
                                    modifier=Modifier.semantics{contentDescription="Select card "+front.text.take(60)})
                                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                    Text(front.text,style=MaterialTheme.typography.titleMedium)
                                    Text(back.text)
                                    back.meaning?.takeIf{it.isNotBlank()}?.let{Text(it)}
                                }
                            }}
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun FolderEditor(folder:LocalFolder?,saving:Boolean,error:String?,onDismiss:()->Unit,onSave:(String,String)->Unit) {
    var name by rememberSaveable(folder?.id){mutableStateOf(folder?.name.orEmpty())}
    var description by rememberSaveable(folder?.id){mutableStateOf(folder?.description.orEmpty())}
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(folder==null)"New folder" else "Edit folder")},
        text={Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name,{name=it.take(255)},label={Text("Folder name")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(description,{description=it.take(1000)},label={Text("Description")},minLines=2,maxLines=4,modifier=Modifier.fillMaxWidth())
            error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        }},
        confirmButton={TextButton(onClick={onSave(name,description)},enabled=!saving&&name.isNotBlank()){Text("Save folder")}},
        dismissButton={TextButton(onClick=onDismiss,enabled=!saving){Text("Cancel")}})
}
