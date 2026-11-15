package com.nihonix.mobile.feature.topics.presentation.topics_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nihonix.mobile.feature.topics.domain.model.JlptLevel
import com.nihonix.mobile.feature.topics.domain.model.Topic
import com.nihonix.mobile.feature.topics.presentation.TopicsUiState
import com.nihonix.mobile.feature.topics.presentation.components.TopicIconMapper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TopicsScreen(
    viewModel: TopicsViewModel,
    onTopicClick: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier,
    onLearning: (Boolean, String?) -> Unit = { _, _ -> },
    onProgress: () -> Unit = {},
    onFlashcards: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLevelDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.levelUpdateMessage) {
        val message = uiState.levelUpdateMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.dismissMessages()
        }
    }

    if (showLevelDialog) {
        LevelPickerDialog(
            levels = uiState.levels,
            currentLevelCode = uiState.userLevelCode,
            isLoading = uiState.isLevelsLoading,
            errorMessage = uiState.levelsError,
            onSelectLevel = { code ->
                showLevelDialog = false
                viewModel.updateMyLevel(code)
            },
            onRetry = { viewModel.refresh() },
            onDismiss = { showLevelDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Topics & Levels") },
                actions = {
                    TextButton(onClick=onProgress) { Text("Tiến độ") }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "My Profile"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onLearning(true, uiState.selectedLevelCode) }) { Text("Vocabulary") }
                TextButton(onClick = { onLearning(false, uiState.selectedLevelCode) }) { Text("Grammar") }
                TextButton(onClick=onFlashcards) { Text("Flashcards") }
            }
            uiState.notice?.let { Text(it) }
            UserLevelSummaryCard(
                uiState = uiState,
                onOpenLevelDialog = { showLevelDialog = true },
                onSetSelectedAsMyLevel = { code -> viewModel.updateMyLevel(code) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            LevelFilterRow(
                levels = uiState.levels,
                selectedLevelCode = uiState.selectedLevelCode,
                isLevelsLoading = uiState.isLevelsLoading,
                levelsError = uiState.levelsError,
                onSelectLevel = { viewModel.selectLevel(it) },
                onRetryLevels = { viewModel.refresh() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search topics by title or slug...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            TopicsContentArea(
                uiState = uiState,
                onTopicClick = onTopicClick,
                onRetryTopics = { viewModel.loadTopics() },
                onShowAllLevels = { viewModel.selectLevel(null) },
                onClearSearch = { viewModel.onSearchQueryChanged("") },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun UserLevelSummaryCard(
    uiState: TopicsUiState,
    onOpenLevelDialog: () -> Unit,
    onSetSelectedAsMyLevel: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "My JLPT Level",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = uiState.userLevelCode ?: "Not set",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (uiState.isUpdatingUserLevel) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }

                FilledTonalButton(
                    onClick = onOpenLevelDialog,
                    enabled = !uiState.isUpdatingUserLevel
                ) {
                    Text("Change Level")
                }
            }

            val selectedCode = uiState.selectedLevelCode
            val selectedDetail = uiState.selectedLevelDetail
            if (selectedCode != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (selectedDetail != null) {
                    Text(
                        "Topics: ${selectedDetail.topicCount ?: "—"} · Vocabulary: ${selectedDetail.vocabularyCount ?: "—"} · Grammar: ${selectedDetail.grammarCount ?: "—"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = buildString {
                            append("${selectedDetail.code}: ")
                            val parts = listOf(
                                selectedDetail.name.jp,
                                selectedDetail.name.en,
                                selectedDetail.name.vi
                            ).filter { it.isNotBlank() }
                            append(parts.joinToString(" • "))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!selectedCode.equals(uiState.userLevelCode, ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onSetSelectedAsMyLevel(selectedCode) },
                        enabled = !uiState.isUpdatingUserLevel,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Set $selectedCode as My Level",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            if (uiState.levelUpdateError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.levelUpdateError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun LevelFilterRow(
    levels: List<JlptLevel>,
    selectedLevelCode: String?,
    isLevelsLoading: Boolean,
    levelsError: String?,
    onSelectLevel: (String?) -> Unit,
    onRetryLevels: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = selectedLevelCode == null,
                onClick = { onSelectLevel(null) },
                label = { Text("All") }
            )
        }

        items(levels, key = { it.code }) { level ->
            FilterChip(
                selected = selectedLevelCode.equals(level.code, ignoreCase = true),
                onClick = { onSelectLevel(level.code) },
                label = { Text(level.code) }
            )
        }

        if (isLevelsLoading && levels.isEmpty()) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        } else if (levelsError != null && levels.isEmpty()) {
            item {
                AssistChip(
                    onClick = onRetryLevels,
                    label = { Text("Retry levels") }
                )
            }
        }
    }
}

@Composable
private fun TopicsContentArea(
    uiState: TopicsUiState,
    onTopicClick: (String) -> Unit,
    onRetryTopics: () -> Unit,
    onShowAllLevels: () -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.isTopicsLoading && uiState.topics.isEmpty() -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading topics...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            uiState.topicsError != null && uiState.topics.isEmpty() -> {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Unable to Load Topics",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.topicsError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetryTopics) {
                        Text("Retry")
                    }
                }
            }

            uiState.isTopicsEmpty || (uiState.topics.isEmpty() && !uiState.isTopicsLoading) -> {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Topics Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uiState.selectedLevelCode != null) {
                            "No topics are currently available for level ${uiState.selectedLevelCode}."
                        } else {
                            "No topics are currently available."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onRetryTopics) {
                            Text("Reload")
                        }
                        if (uiState.selectedLevelCode != null) {
                            OutlinedButton(onClick = onShowAllLevels) {
                                Text("Show All Levels")
                            }
                        }
                    }
                }
            }

            uiState.filteredTopics.isEmpty() -> {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Matching Topics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No topics matched \"${uiState.searchQuery}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onClearSearch) {
                        Text("Clear Search")
                    }
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (uiState.isTopicsLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (uiState.topicsError != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.topicsError,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onRetryTopics) {
                                    Text("Retry")
                                }
                            }
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.filteredTopics, key = { it.id }) { topic ->
                            TopicCard(
                                topic = topic,
                                onClick = { onTopicClick(topic.slug.ifBlank { topic.id }) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicCard(
    topic: Topic,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = TopicIconMapper.resolveIcon(topic.iconCode, topic.slug),
                        contentDescription = topic.title.en.ifBlank { topic.slug },
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = topic.levelCode,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "#${topic.order}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val primaryTitle = topic.title.en.ifBlank {
                    topic.title.vi.ifBlank { topic.title.jp.ifBlank { topic.slug } }
                }
                Text(
                    text = primaryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val secondaryParts = listOf(topic.title.jp, topic.title.vi)
                    .filter { it.isNotBlank() && it != primaryTitle }
                if (secondaryParts.isNotEmpty()) {
                    Text(
                        text = secondaryParts.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open topic",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LevelPickerDialog(
    levels: List<JlptLevel>,
    currentLevelCode: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onSelectLevel: (String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select JLPT Level") },
        text = {
            when {
                isLoading && levels.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                levels.isEmpty() -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage ?: "No JLPT levels available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        levels.forEach { level ->
                            val isSelected = level.code.equals(currentLevelCode, ignoreCase = true)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectLevel(level.code) },
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = level.code,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val desc = listOf(level.name.jp, level.name.en, level.name.vi)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" • ")
                                        if (desc.isNotBlank()) {
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
