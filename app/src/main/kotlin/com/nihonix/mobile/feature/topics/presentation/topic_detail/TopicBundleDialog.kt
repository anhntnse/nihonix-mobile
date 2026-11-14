package com.nihonix.mobile.feature.topics.presentation.topic_detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nihonix.mobile.feature.topics.presentation.TopicDetailUiState
import com.nihonix.mobile.feature.topics.domain.model.LocalizedText

private fun LocalizedText?.display(): String = this?.let { it.en.ifBlank { it.vi.ifBlank { it.jp } } }.orEmpty()

@Composable
fun TopicBundleDialog(state: TopicDetailUiState, onClose: () -> Unit, onRetry: () -> Unit) {
    var grammar by rememberSaveable { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize().safeDrawingPadding(), contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    TextButton(onClick = onClose) { Text("Back to topic") }
                    Text("Topic content", style = MaterialTheme.typography.headlineSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !grammar, onClick = { grammar = false }, label = { Text("Vocabulary") })
                        FilterChip(selected = grammar, onClick = { grammar = true }, label = { Text("Grammar") })
                    }
                }
                if (state.isBundleLoading) item { CircularProgressIndicator(); Text("Loading content…") }
                state.bundleError?.let { error -> item {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry) { Text("Retry content") }
                } }
                state.bundle?.let { bundle ->
                    val entries = if (grammar) bundle.grammarPatterns else bundle.vocabularyItems
                    if (entries.isEmpty()) item { Text(if (grammar) "No grammar content yet." else "No vocabulary content yet.") }
                    items(entries, key = { it.slug }) { entry ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(entry.pattern ?: entry.kanji?.takeIf { it.isNotBlank() } ?: entry.kana ?: entry.title.display().ifBlank { entry.slug },
                                    style = MaterialTheme.typography.titleMedium)
                                entry.kana?.let { Text(it) }
                                Text(if (grammar) entry.explanation.display() else entry.meaning.display())
                                entry.romaji?.let { Text(it) }
                                (if (grammar) entry.usageExamples else entry.examples).orEmpty().forEach { example ->
                                    Text(listOfNotNull(example["sentence_jp"], example["sentence_romaji"], example["sentence_en"], example["sentence_vi"]).joinToString("\n"))
                                }
                                entry.conjugationRules?.forEach { (form, rule) -> Text("$form: $rule") }
                            }
                        }
                    }
                }
            }
        }
    }
}
