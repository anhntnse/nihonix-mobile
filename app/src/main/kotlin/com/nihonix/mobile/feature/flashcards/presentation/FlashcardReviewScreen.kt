package com.nihonix.mobile.feature.flashcards.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardReviewScreen(viewModel:FlashcardReviewViewModel,onBack:()->Unit) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(topBar={TopAppBar(title={Text(state.title.ifBlank{"Review cards"})},navigationIcon={TextButton(onClick=onBack){Text("Back")}})}) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item {
                if(!state.loaded)CircularProgressIndicator()
                else if(state.card==null) {
                    Text(if(state.total==0)"No cards available." else "Review complete.",style=MaterialTheme.typography.headlineSmall)
                    TextButton(onClick=viewModel::restart,enabled=state.total>0){Text("Restart review")}
                } else {
                    val card=state.card!!
                    val face=if(state.back)card.back else card.front
                    Text("Card ${state.position+1} of ${state.total}")
                    Text(if(state.back)"Back" else "Front",style=MaterialTheme.typography.labelLarge)
                    Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text(face.text,style=MaterialTheme.typography.headlineMedium)
                        face.meaning?.takeIf{it.isNotBlank()}?.let{Text(it)}
                    }}
                    Button(onClick=viewModel::flip,modifier=Modifier.fillMaxWidth()){Text(if(state.back)"Show front" else "Show answer")}
                }
            }
            if(state.total>0)item {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick=viewModel::previous,enabled=state.position>0,modifier=Modifier.weight(1f)){Text("Previous")}
                    Button(onClick=viewModel::next,enabled=state.position<state.total,modifier=Modifier.weight(1f)){Text("Next")}
                }
            }
        }
    }
}
