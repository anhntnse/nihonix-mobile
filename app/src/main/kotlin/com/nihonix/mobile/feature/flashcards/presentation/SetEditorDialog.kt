package com.nihonix.mobile.feature.flashcards.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SetEditorDialog(state:FlashcardsUiState,vm:FlashcardsViewModel,act:(()->Unit)->Unit) {
    val editor=state.editor ?: return
    var discard by rememberSaveable(editor.id){mutableStateOf(false)}
    var selected by rememberSaveable(editor.id){mutableStateOf(listOf<String>())}
    var remove by rememberSaveable(editor.id){mutableStateOf(false)}
    val list=rememberLazyListState()
    LaunchedEffect(state.error) {
        val index=state.error?.let{Regex("Card ([0-9]+)").find(it)?.groupValues?.get(1)?.toIntOrNull()}
        if(index!=null)list.scrollToItem(index.coerceAtMost(editor.cards.size))
    }
    Dialog(onDismissRequest={if(!state.saving)discard=true},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Surface(Modifier.fillMaxHeight().widthIn(max=840.dp).fillMaxWidth()) {
            Scaffold(topBar={TopAppBar(title={Text(if(editor.preview)"Preview cards" else "Edit set")},
                navigationIcon={TextButton(onClick={discard=true},enabled=!state.saving){Text("Cancel")}})},
                bottomBar={Surface(shadowElevation=4.dp) {Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(12.dp),
                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    state.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick={act(vm::addCard)},enabled=editor.cards.size<200&&!state.saving,modifier=Modifier.weight(1f)){Text("Add card")}
                        Button(onClick={act(vm::saveEditor)},enabled=!state.saving,modifier=Modifier.weight(1f)){Text(if(state.saving)"Saving…" else "Save set")}
                    }
                }}}) { padding ->
                LazyColumn(Modifier.fillMaxSize().padding(padding),state=list,contentPadding=PaddingValues(16.dp),
                    verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    item("set") {
                        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            if(editor.preview)Text("Review every card before saving. Nothing is added until you choose Save set.")
                            OutlinedTextField(editor.name,{text->act{vm.editorName(text)}},label={Text("Set name")},singleLine=true,modifier=Modifier.fillMaxWidth(),enabled=!state.saving)
                            Text("${editor.cards.size}/200 cards")
                            OutlinedButton(onClick={remove=true},enabled=selected.isNotEmpty()&&!state.saving){Text("Remove selected (${selected.size})")}
                        }
                    }
                    itemsIndexed(editor.cards,key={_,card->card.id}) { index,card ->
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Row {
                                Checkbox(checked=card.id in selected,onCheckedChange={checked->selected=if(checked)(selected+card.id).distinct() else selected-card.id},enabled=!state.saving,modifier=Modifier.semantics{contentDescription="Select draft card ${index+1}"})
                                Text("Card ${index+1}",modifier=Modifier.padding(top=14.dp))
                            }
                            OutlinedTextField(card.front.text,{text->act{vm.cardText(card.id,true,text)}},label={Text("Front ${index+1}")},
                                minLines=2,maxLines=4,modifier=Modifier.fillMaxWidth(),enabled=!state.saving)
                            OutlinedTextField(card.back.text,{text->act{vm.cardText(card.id,false,text)}},label={Text("Back ${index+1}")},
                                minLines=2,maxLines=4,modifier=Modifier.fillMaxWidth(),enabled=!state.saving)
                        }}
                    }
                    if(editor.cards.isEmpty())item{Text("This set is empty. Add cards now or save it for later.")}
                }
            }
        }
        if(discard) AlertDialog(onDismissRequest={discard=false},title={Text("Discard editor draft?")},
            text={Text("Your unsaved changes in this editor will be removed.")},
            confirmButton={TextButton(onClick={discard=false;act(vm::closeEditor)}){Text("Discard")}},
            dismissButton={TextButton(onClick={discard=false}){Text("Keep editing")}})
        if(remove) AlertDialog(onDismissRequest={remove=false},title={Text("Remove ${selected.size} cards?")},
            text={Text("Save the set to apply this change.")},
            confirmButton={TextButton(onClick={remove=false;act{vm.removeCards(selected.toSet())};selected=emptyList()}){Text("Remove")}},
            dismissButton={TextButton(onClick={remove=false}){Text("Cancel")}})
    }
}
