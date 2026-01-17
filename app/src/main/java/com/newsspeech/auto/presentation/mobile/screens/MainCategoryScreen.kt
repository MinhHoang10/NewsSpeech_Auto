package com.newsspeech.auto.presentation.mobile.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.newsspeech.auto.domain.model.News

/**
 * Màn hình chính hiển thị cả Nguồn và Chủ đề - Modern Design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainCategoryScreen(
    newsList: List<News>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onSourceClick: (String) -> Unit,
    onTopicClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(newsList) {
        if (newsList.isNotEmpty()) {
            showContent = true
        }
    }

    // Nhóm theo nguồn
    val sourceGroups = remember(newsList) {
        newsList
            .groupBy { it.source }
            .map { (source, news) ->
                CategoryItem(
                    name = source.ifEmpty { "Không rõ nguồn" },
                    count = news.size,
                    emoji = "📰",
                    color = CategoryColor.Blue
                )
            }
            .sortedByDescending { it.count }
    }

    // Nhóm theo chủ đề
    val topicGroups = remember(newsList) {
        newsList
            .filter { it.category.isNotEmpty() }
            .groupBy { it.category }
            .map { (category, news) ->
                CategoryItem(
                    name = category,
                    count = news.size,
                    emoji = getCategoryEmoji(category),
                    color = getCategoryColor(category)
                )
            }
            .sortedByDescending { it.count }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Tin Tức",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (newsList.isNotEmpty()) {
                            Text(
                                "${newsList.size} bài viết • Hôm nay",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Cài đặt"
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới",
                            tint = if (isLoading) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn() + expandVertically()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Stats Card
                if (newsList.isNotEmpty()) {
                    item {
                        StatsCard(
                            totalNews = newsList.size,
                            sourcesCount = sourceGroups.size,
                            topicsCount = topicGroups.size
                        )
                    }
                }

                // Section: Nguồn tin
                item {
                    ModernSectionHeader(
                        title = "Nguồn tin",
                        count = sourceGroups.size,
                        icon = "📰"
                    )
                }

                items(sourceGroups) { item ->
                    ModernCategoryCard(
                        item = item,
                        onClick = { onSourceClick(item.name) }
                    )
                }

                // Spacer
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Section: Chủ đề
                item {
                    ModernSectionHeader(
                        title = "Chủ đề",
                        count = topicGroups.size,
                        icon = "🗂️"
                    )
                }

                items(topicGroups) { item ->
                    ModernCategoryCard(
                        item = item,
                        onClick = { onTopicClick(item.name) }
                    )
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

private data class CategoryItem(
    val name: String,
    val count: Int,
    val emoji: String,
    val color: CategoryColor
)

private enum class CategoryColor {
    Blue, Purple, Orange, Green, Red, Pink, Teal, Amber
}

@Composable
private fun StatsCard(
    totalNews: Int,
    sourcesCount: Int,
    topicsCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = "$totalNews",
                label = "Tin tức",
                icon = "📰"
            )

            VerticalDivider(
                modifier = Modifier.height(48.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )

            StatItem(
                value = "$sourcesCount",
                label = "Nguồn",
                icon = "📡"
            )

            VerticalDivider(
                modifier = Modifier.height(48.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )

            StatItem(
                value = "$topicsCount",
                label = "Chủ đề",
                icon = "🏷️"
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ModernSectionHeader(
    title: String,
    count: Int,
    icon: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$count mục",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ModernCategoryCard(
    item: CategoryItem,
    onClick: () -> Unit
) {
    val containerColor = when (item.color) {
        CategoryColor.Blue -> MaterialTheme.colorScheme.primaryContainer
        CategoryColor.Purple -> MaterialTheme.colorScheme.secondaryContainer
        CategoryColor.Orange -> MaterialTheme.colorScheme.tertiaryContainer
        CategoryColor.Green -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        CategoryColor.Red -> MaterialTheme.colorScheme.errorContainer
        CategoryColor.Pink -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        CategoryColor.Teal -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        CategoryColor.Amber -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    }

    val contentColor = when (item.color) {
        CategoryColor.Blue -> MaterialTheme.colorScheme.onPrimaryContainer
        CategoryColor.Purple -> MaterialTheme.colorScheme.onSecondaryContainer
        CategoryColor.Orange -> MaterialTheme.colorScheme.onTertiaryContainer
        CategoryColor.Green -> MaterialTheme.colorScheme.onPrimaryContainer
        CategoryColor.Red -> MaterialTheme.colorScheme.onErrorContainer
        CategoryColor.Pink -> MaterialTheme.colorScheme.onSecondaryContainer
        CategoryColor.Teal -> MaterialTheme.colorScheme.onTertiaryContainer
        CategoryColor.Amber -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icon container
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    contentColor.copy(alpha = 0.1f),
                                    contentColor.copy(alpha = 0.05f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.emoji,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                    Text(
                        text = "${item.count} bài viết",
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Count badge
                Surface(
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${item.count}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.5f)
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
        else -> "📌"
    }
}

/**
 * Lấy màu tương ứng với category
 */
private fun getCategoryColor(category: String): CategoryColor {
    return when (category.lowercase()) {
        "thế giới", "quốc tế" -> CategoryColor.Blue
        "kinh doanh", "kinh tế" -> CategoryColor.Green
        "công nghệ", "khoa học" -> CategoryColor.Purple
        "giải trí", "văn hóa" -> CategoryColor.Pink
        "thể thao" -> CategoryColor.Orange
        "sức khỏe", "y tế" -> CategoryColor.Red
        "giáo dục" -> CategoryColor.Teal
        "pháp luật" -> CategoryColor.Amber
        "đời sống" -> CategoryColor.Green
        "du lịch" -> CategoryColor.Blue
        "ô tô", "xe" -> CategoryColor.Orange
        else -> CategoryColor.Blue
    }
}