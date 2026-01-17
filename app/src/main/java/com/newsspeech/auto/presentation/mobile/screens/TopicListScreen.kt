package com.newsspeech.auto.presentation.mobile.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.newsspeech.auto.domain.model.News

/**
 * Màn hình hiển thị danh sách các chủ đề
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicListScreen(
    newsList: List<News>,
    onTopicClick: (String) -> Unit,
    onBack: () -> Unit
) {
    // Nhóm tin theo chủ đề và đếm số lượng
    val topicGroups = newsList
        .filter { it.category.isNotEmpty() }
        .groupBy { it.category }
        .map { (category, news) ->
            TopicItem(
                name = category,
                count = news.size,
                emoji = getCategoryEmoji(category)
            )
        }
        .sortedByDescending { it.count }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🗂️ Chọn chủ đề")
                        Text(
                            "${topicGroups.size} chủ đề",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (topicGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Không có chủ đề nào",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(topicGroups) { topicItem ->
                    TopicCard(
                        topic = topicItem,
                        onClick = { onTopicClick(topicItem.name) }
                    )
                }
            }
        }
    }
}

private data class TopicItem(
    val name: String,
    val count: Int,
    val emoji: String
)

@Composable
private fun TopicCard(
    topic: TopicItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = topic.emoji,
                    style = MaterialTheme.typography.displaySmall
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = topic.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${topic.count} tin tức",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${topic.count}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Lấy emoji tương ứng với category
 */
private fun getCategoryEmoji(category: String): String {
    return when (category.lowercase()) {
        "thế giới", "quốc tế" -> "🌍"
        "kinh doanh", "kinh tế" -> "💼"
        "công nghệ", "khoa học" -> "💻"
        "giải trí", "văn hóa" -> "🎭"
        "thể thao" -> "⚽"
        "sức khỏe", "y tế" -> "🏥"
        "giáo dục" -> "📚"
        "pháp luật" -> "⚖️"
        "đời sống" -> "🏡"
        "du lịch" -> "✈️"
        "ô tô", "xe" -> "🚗"
        else -> "📰"
    }
}