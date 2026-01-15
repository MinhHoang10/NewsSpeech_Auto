package com.newsspeech.auto.presentation.mobile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.newsspeech.auto.domain.model.News
import com.newsspeech.auto.util.formatTimestamp

/**
 * News item card component
 * Displays a single news item with click to play TTS
 */
@Composable
fun NewsItem(
    news: News,
    isTtsReady: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = isTtsReady, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isTtsReady) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category tag
            if (news.category.isNotEmpty()) {
                CategoryTag(category = news.category)
            }

            // Title
            NewsTitle(title = news.title)

            // Content preview
            if (news.content.isNotEmpty()) {
                NewsContent(content = news.content)
            }

            // Metadata (source + timestamp)
            NewsMetadata(
                source = news.source,
                timestamp = news.timestamp
            )

            // TTS status hint
            TtsStatusHint(isTtsReady = isTtsReady)
        }
    }
}

@Composable
private fun CategoryTag(category: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = category,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun NewsTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun NewsContent(content: String) {
    Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun NewsMetadata(
    source: String,
    timestamp: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (source.isNotEmpty()) {
            Text(
                text = source,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (timestamp.isNotEmpty()) {
            if (source.isNotEmpty()) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatTimestamp(timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TtsStatusHint(isTtsReady: Boolean) {
    Text(
        text = if (isTtsReady) {
            "🔊 Chạm để nghe"
        } else {
            "⏳ TTS đang khởi tạo..."
        },
        style = MaterialTheme.typography.labelSmall,
        color = if (isTtsReady) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}