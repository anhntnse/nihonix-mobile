package com.nihonix.mobile.feature.learning.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nihonix.mobile.feature.learning.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningListScreen(viewModel: LearningViewModel,onBack: () -> Unit,
    onEntry: (LearningEntry) -> Unit,onCollection: (ContentKind,LearningFilter) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(topBar = { TopAppBar(title={ Text(state.kind.label) },
        navigationIcon={ IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back") } },
        actions={ IconButton(onClick=viewModel::refresh) { Icon(Icons.Default.Refresh,"Refresh content") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding(),contentPadding=PaddingValues(16.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item(key="filters") {
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(state.filter.parentId == null) {
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            val choices = if(state.kind.vocabulary) listOf(ContentKind.SETS,ContentKind.WORDS) else listOf(ContentKind.GRAMMAR_TOPICS,ContentKind.PATTERNS)
                            choices.forEach { kind ->
                                FilterChip(selected=kind==state.kind,onClick={ if(kind!=state.kind) onCollection(kind,state.filter.copy(query="")) },
                                    label={ Text(kind.label) })
                            }
                        }
                        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            items(listOf("All levels","N5","N4","N3","N2","N1")) { level ->
                                val code = level.takeUnless { it=="All levels" }
                                FilterChip(selected=state.filter.levelCode==code,onClick={viewModel.level(code)},label={Text(level)})
                            }
                        }
                        TopicFilter(state,viewModel::topic)
                        state.topicsError?.let { Text(it,color=MaterialTheme.colorScheme.error)
                            TextButton(onClick=viewModel::loadTopics) { Text("Retry topic filters") } }
                    } else Text("Showing content in the selected "+if(state.kind.vocabulary) "vocabulary set." else "grammar topic.")
                    OutlinedTextField(value=state.filter.query,onValueChange=viewModel::search,
                        label={Text("Search Japanese, reading or meaning")},singleLine=true,modifier=Modifier.fillMaxWidth(),
                        trailingIcon={ if(state.filter.query.isNotEmpty()) TextButton(onClick={viewModel.search("")}) { Text("Clear") } })
                    ContentLanguage(state.language,viewModel::language)
                    if(state.page>0) Text("${state.total} results",style=MaterialTheme.typography.labelLarge)
                }
            }
            state.notice?.let { notice -> item { Text(notice) } }
            if(state.loading && state.items.isEmpty()) item { CircularProgressIndicator(); Text("Loading content…") }
            if(!state.loading && state.error==null && state.items.isEmpty()) item {
                Text("No content found. Try another search or filter.",style=MaterialTheme.typography.bodyLarge)
            }
            items(state.items,key={it.key}) { entry ->
                Card(onClick={onEntry(entry)},modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text(entry.label(state.language),style=MaterialTheme.typography.titleLarge)
                        if(entry.kana.isNotBlank() && entry.kana!=entry.label(state.language)) Text(entry.kana)
                        if(entry.romaji.isNotBlank()) Text(entry.romaji)
                        Text(if(state.kind==ContentKind.PATTERNS) entry.explanation.getByLanguage(state.language)
                            else if(state.kind==ContentKind.WORDS) entry.meaning.getByLanguage(state.language)
                            else entry.description, maxLines=3)
                        Text(listOf(entry.levelCode,entry.topicTitle.getByLanguage(state.language),
                            entry.itemCount?.let { "$it items" }.orEmpty()).filter { it.isNotBlank() }.joinToString(" · "),
                            style=MaterialTheme.typography.labelMedium)
                    }
                }
            }
            state.error?.let { error -> item(key="error") {
                Text(error,color=MaterialTheme.colorScheme.error)
                Button(onClick={if(state.page==0) viewModel.refresh() else viewModel.nextPage()}) { Text("Retry") }
            } }
            if(state.items.isNotEmpty() && state.hasMore) item(key="more") {
                Button(onClick=viewModel::nextPage,enabled=!state.loading,modifier=Modifier.fillMaxWidth()) {
                    Text(if(state.loading) "Loading more…" else "Load more")
                }
            }
        }
    }
}
@Composable
private fun TopicFilter(state: LearningListState,onTopic: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val current = state.topics.firstOrNull { it.id==state.filter.topicId }
    Box {
        OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) {
            Text(current?.title?.getByLanguage(state.language) ?: if(state.filter.topicId==null) "All topics" else "Selected topic")
        }
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            DropdownMenuItem(text={Text("All topics")},onClick={expanded=false;onTopic(null)})
            state.topics.filter { state.filter.levelCode==null || it.levelCode==state.filter.levelCode }.forEach { topic ->
                DropdownMenuItem(text={Text(topic.title.getByLanguage(state.language))},onClick={expanded=false;onTopic(topic.id)})
            }
        }
    }
}
@Composable
private fun ContentLanguage(language: String,onLanguage: (String) -> Unit) {
    LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        items(listOf("en" to "English","vi" to "Tiếng Việt","jp" to "日本語")) { (code,label) ->
            FilterChip(selected=language==code,onClick={onLanguage(code)},label={Text(label)})
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningDetailScreen(viewModel: LearningDetailViewModel,onBack: () -> Unit,onOpenItems: (LearningEntry) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var language by rememberSaveable { mutableStateOf("en") }
    Scaffold(topBar={TopAppBar(title={Text(viewModel.kind.label)},navigationIcon={
        IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back") }
    },actions={IconButton(onClick=viewModel::refresh) { Icon(Icons.Default.Refresh,"Refresh content") }})}) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp),
            verticalArrangement=Arrangement.spacedBy(16.dp)) {
            state.notice?.let { notice -> item { Text(notice) } }
            if(state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error -> item { Text(error,color=MaterialTheme.colorScheme.error)
                Button(onClick=viewModel::refresh) { Text("Retry") } } }
            state.entry?.let { entry ->
                item {
                    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        ContentLanguage(language) { language=it }
                        Text(entry.label(language),style=MaterialTheme.typography.headlineMedium)
                        Text(listOf(entry.levelCode,entry.topicTitle.getByLanguage(language)).filter{it.isNotBlank()}.joinToString(" · "))
                        if(entry.kana.isNotBlank()) Text(entry.kana,style=MaterialTheme.typography.titleLarge)
                        if(entry.romaji.isNotBlank()) Text(entry.romaji)
                        if(entry.patternRomaji.isNotBlank()) Text(entry.patternRomaji)
                        if(entry.wordType.isNotBlank()) Text(entry.wordType,style=MaterialTheme.typography.labelLarge)
                        if(entry.description.isNotBlank()) Text(entry.description)
                        if(viewModel.kind.group) {
                            Text("${entry.itemCount ?: 0} items")
                            Button(onClick={onOpenItems(entry)},modifier=Modifier.fillMaxWidth()) {
                                Text(if(viewModel.kind==ContentKind.SETS) "Study words" else "Study grammar patterns")
                            }
                        } else {
                            Text(if(viewModel.kind==ContentKind.WORDS) "Meaning" else "Explanation",
                                style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                            Text(if(viewModel.kind==ContentKind.WORDS) entry.meaning.getByLanguage(language) else entry.explanation.getByLanguage(language))
                            if(entry.difficulty.isNotBlank()) Text("Difficulty: ${entry.difficulty}")
                            Button(onClick=viewModel::toggleLearned, enabled=!state.saving, modifier=Modifier.fillMaxWidth()) {
                                Text(if(state.progress.learned) "Bỏ đánh dấu" else "Đã học")
                            }
                            if(state.progress.pending) Text("Đã lưu trên thiết bị · Chờ đồng bộ")
                            state.progress.error?.let { Text(it, color=MaterialTheme.colorScheme.error) }
                        }
                    }
                }
                if(entry.rules.isNotEmpty()) item {
                    Text("Conjugation",style=MaterialTheme.typography.titleLarge)
                    entry.rules.forEach { (form,rule) -> Text("$form: $rule") }
                }
                if(entry.grammarPoints.isNotEmpty()) item {
                    Text("Grammar points",style=MaterialTheme.typography.titleLarge)
                    entry.grammarPoints.forEach { Text("• $it") }
                }
                if(entry.examples.isNotEmpty()) item { Text("Examples",style=MaterialTheme.typography.titleLarge) }
                items(entry.examples) { example ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(example.jp,style=MaterialTheme.typography.titleMedium)
                            if(example.romaji.isNotBlank()) Text(example.romaji)
                            Text(example.translation(language))
                        }
                    }
                }
                if(!viewModel.kind.group && entry.examples.isEmpty()) item { Text("No examples available.") }
            }
        }
    }
}
