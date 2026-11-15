package com.nihonix.mobile.feature.topics.presentation.topic_detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nihonix.mobile.feature.topics.domain.model.JlptLevel
import com.nihonix.mobile.feature.topics.domain.model.Topic
import com.nihonix.mobile.feature.topics.presentation.components.TopicIconMapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailScreen(
    identifier: String,
    viewModel: TopicDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onLearning: (Boolean, String, String) -> Unit = { _, _, _ -> },
    onPractice: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBundle by rememberSaveable(identifier) { mutableStateOf(false) }
    LaunchedEffect(identifier, showBundle, uiState.topic?.id) {
        if (showBundle && uiState.topic != null) viewModel.loadBundle()
    }
    if (showBundle) {
        TopicBundleDialog(uiState, onClose = { showBundle = false }, onRetry = viewModel::loadBundle)
    }

    LaunchedEffect(identifier) {
        viewModel.loadTopicDetail(identifier)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = uiState.topic?.let {
                        it.title.en.ifBlank { it.title.vi.ifBlank { it.title.jp.ifBlank { it.slug } } }
                    } ?: "Topic Details"
                    Text(
                        text = titleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadTopicDetail(identifier) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading && uiState.topic == null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading topic details...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                uiState.errorMessage != null && uiState.topic == null -> {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error Loading Topic",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadTopicDetail(identifier) }) {
                            Text("Retry")
                        }
                    }
                }

                uiState.isEmpty -> {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Topic Not Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No topic details were found for \"$identifier\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadTopicDetail(identifier) }) {
                            Text("Reload")
                        }
                    }
                }

                uiState.topic != null -> {
                    TopicDetailContent(
                        topic = uiState.topic!!,
                        levelDetail = uiState.levelDetail ?: uiState.topic!!.level,
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        notice = uiState.notice,
                        onOpenBundle = { showBundle = true },
                        onLearning = { vocabulary -> onLearning(vocabulary, uiState.topic!!.id, uiState.topic!!.levelCode) },
                        onPractice = { onPractice(uiState.topic!!.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicDetailContent(
    topic: Topic,
    levelDetail: JlptLevel?,
    isLoading: Boolean,
    errorMessage: String?,
    notice: String?,
    onOpenBundle: () -> Unit,
    onLearning: (Boolean) -> Unit,
    onPractice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        notice?.let { Text(it) }
        Button(onClick = { onLearning(true) }, modifier = Modifier.fillMaxWidth()) { Text("Study vocabulary") }
        Button(onClick = { onLearning(false) }, modifier = Modifier.fillMaxWidth()) { Text("Study grammar") }
        OutlinedButton(onClick = onPractice, modifier = Modifier.fillMaxWidth()) { Text("AI Practice") }
        OutlinedButton(onClick = onOpenBundle, modifier = Modifier.fillMaxWidth()) { Text("Explore topic content") }
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = TopicIconMapper.resolveIcon(topic.iconCode, topic.slug),
                            contentDescription = topic.title.en.ifBlank { topic.slug },
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "JLPT ${topic.levelCode}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Lesson #${topic.order}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (topic.title.jp.isNotBlank()) {
                    Text(
                        text = topic.title.jp,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (topic.title.en.isNotBlank()) {
                    Text(
                        text = topic.title.en,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (topic.title.vi.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = topic.title.vi,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Level Details Card
        if (levelDetail != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "JLPT Level Information",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailInfoRow(label = "Level Code", value = levelDetail.code)
                    if (levelDetail.name.jp.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        DetailInfoRow(label = "Japanese Name", value = levelDetail.name.jp)
                    }
                    if (levelDetail.name.en.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        DetailInfoRow(label = "English Name", value = levelDetail.name.en)
                    }
                    if (levelDetail.name.vi.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        DetailInfoRow(label = "Vietnamese Name", value = levelDetail.name.vi)
                    }
                }
            }
        }

        // Topic Metadata Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Topic Metadata",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                DetailInfoRow(label = "Slug", value = topic.slug)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DetailInfoRow(label = "Topic ID", value = topic.id)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DetailInfoRow(label = "Icon Type", value = topic.iconType)
                if (topic.iconCode != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailInfoRow(label = "Icon Code", value = topic.iconCode.toString())
                }
                if (!topic.etag.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailInfoRow(label = "ETag", value = topic.etag)
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
