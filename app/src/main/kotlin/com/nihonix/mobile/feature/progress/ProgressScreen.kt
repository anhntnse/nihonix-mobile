package com.nihonix.mobile.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonParser
import com.nihonix.mobile.core.offline.CatalogDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProgressSummary(val label:String,val learned:Int,val total:Int)
data class ProgressState(val rows:List<ProgressSummary> = emptyList(),val pending:Int=0,val blocked:Int=0,val error:String?=null,val levels:List<ProgressSummary> = emptyList())
class ProgressViewModel(private val repository:ProgressRepository,private val catalog:CatalogDao):ViewModel() {
    private val state=MutableStateFlow(ProgressState())
    val uiState=state.asStateFlow()
    init {
        viewModelScope.launch(Dispatchers.IO) {
            combine(repository.records(),repository.outbox(),catalog.observeAll()) { rows,messages,cached ->
                val manifest=cached.firstOrNull{it.url.endsWith("/catalog/manifest")}
                val collections=manifest?.let { runCatching { JsonParser.parseString(it.payload).asJsonObject.getAsJsonObject("data").getAsJsonArray("collections") }.getOrNull() }
                val topics=collections?.firstOrNull{it.asJsonObject["kind"].asString=="topics"}?.asJsonObject?.getAsJsonArray("entries")
                val topicIds=topics?.associate { it.asJsonObject["slug"].asString to it.asJsonObject["id"].asString }.orEmpty()
                val items=collections?.filter{it.asJsonObject["kind"].asString in listOf("vocabulary-items","grammar-patterns")}?.flatMap { collection ->
                    val kind=if(collection.asJsonObject["kind"].asString=="vocabulary-items") "vocabulary" else "grammar"
                    collection.asJsonObject.getAsJsonArray("entries").map { entry ->
                        val item=entry.asJsonObject
                        Triple(kind,item["slug"].asString,
                            item["topic_id"]?.takeUnless{it.isJsonNull}?.asString ?: topicIds[item["topic_slug"]?.takeUnless{it.isJsonNull}?.asString].orEmpty())
                    }
                }.orEmpty()
                val learned=rows.filter{it.status=="learned"}.map{it.kind to it.itemId}.toSet()
                val summaries=topics?.map { value ->
                    val topic=value.asJsonObject; val id=topic["id"].asString
                    val subset=items.filter{it.third==id}
                    val title=topic["title"]?.takeUnless{it.isJsonNull}?.let { if(it.isJsonObject) it.asJsonObject["en"]?.asString else it.asString } ?: topic["slug"].asString
                    ProgressSummary(topic["level_code"].asString+" · "+title,subset.count{(it.first to it.second) in learned},subset.size)
                }.orEmpty()
                ProgressState(summaries,messages.count{it.state=="pending"},messages.count{it.state=="blocked"},messages.firstOrNull{it.error!=null}?.error,
                    summaries.groupBy{it.label.substringBefore(" · ")}.map { (level,topics) ->
                        ProgressSummary(level,topics.sumOf{it.learned},topics.sumOf{it.total})
                    })
            }.collect { state.value=it }
        }
    }
    fun sync()=repository.requestSync()
    fun retry() { viewModelScope.launch(Dispatchers.IO) { repository.retryBlocked() } }
    fun discard() { viewModelScope.launch(Dispatchers.IO) { repository.discardPending() } }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(viewModel:ProgressViewModel,onBack:()->Unit) {
    val state by viewModel.uiState.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Bỏ thay đổi chưa đồng bộ?")},
        text={Text("Tiến độ đã lưu trên máy sẽ được tải lại từ máy chủ khi có mạng.")},
        confirmButton={TextButton(onClick={confirm=false;viewModel.discard()}){Text("Bỏ thay đổi")}},
        dismissButton={TextButton(onClick={confirm=false}){Text("Hủy")}})
    Scaffold(topBar={TopAppBar(title={Text("Tiến độ học")},navigationIcon={TextButton(onClick=onBack){Text("Quay lại")}})}) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                Text("Chờ đồng bộ: ${state.pending} · Cần xử lý: ${state.blocked}")
                Text("Thống kê dựa trên danh mục đã tải về thiết bị.")
                Button(onClick=viewModel::sync){Text("Đồng bộ")}
                if(state.blocked>0) TextButton(onClick=viewModel::retry){Text("Thử lại thay đổi bị từ chối")}
                if(state.pending+state.blocked>0) TextButton(onClick={confirm=true}){Text("Bỏ thay đổi chưa đồng bộ")}
            }
            if(state.rows.isEmpty()) item { Text("Chưa có danh mục. Kết nối mạng và đồng bộ để tải danh mục.") }
            state.error?.let { message -> item { Text(message,color=MaterialTheme.colorScheme.error) } }
            (state.levels+state.rows).forEach { row -> item {
                Text(row.label,style=MaterialTheme.typography.titleMedium)
                Text("${row.learned}/${row.total} đã học")
                LinearProgressIndicator(progress={if(row.total==0) 0f else row.learned.toFloat()/row.total},modifier=Modifier.fillMaxWidth())
            } }
        }
    }
}
