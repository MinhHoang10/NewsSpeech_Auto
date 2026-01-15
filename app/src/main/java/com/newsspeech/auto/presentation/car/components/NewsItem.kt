package com.newsspeech.auto.presentation.car.components

import androidx.car.app.model.Row
import com.newsspeech.auto.domain.model.News

/**
 * Component cho mỗi news item
 */
object NewsItem {

    /**
     * Build row cho 1 tin tức
     *
     * @param news News data
     * @param isTtsReady TTS có sẵn sàng không
     * @param onClick Callback khi click
     */
    fun build(
        news: News,
        isTtsReady: Boolean,
        onClick: (News) -> Unit
    ): Row {
        val rowBuilder = Row.Builder()
            .setTitle(news.title)

        // Description
        val description = when {
            news.content.isNotEmpty() -> {
                if (news.content.length > 100) {
                    news.content.take(100) + "..."
                } else {
                    news.content
                }
            }
            else -> "Chạm để nghe chi tiết"
        }
        rowBuilder.addText(description)

        // Metadata (source + timestamp)
        val metadata = buildMetadata(news)
        if (metadata.isNotEmpty()) {
            rowBuilder.addText(metadata)
        }

        // Click handler
        if (isTtsReady) {
            rowBuilder.setOnClickListener {
                onClick(news)
            }
        } else {
            rowBuilder.addText("⏳ TTS đang khởi tạo...")
            rowBuilder.setBrowsable(false)
        }

        return rowBuilder.build()
    }

    /**
     * Build metadata string (source • timestamp)
     */
    private fun buildMetadata(news: News): String {
        return buildString {
            if (news.source.isNotEmpty()) {
                append(news.source)
            }
            if (news.timestamp.isNotEmpty()) {
                if (isNotEmpty()) append(" • ")
                append(formatTimestamp(news.timestamp))
            }
        }
    }

    /**
     * Format timestamp từ ISO format sang readable format
     * VD: "2025-11-18T09:08:34" -> "18/11 09:08"
     */
    private fun formatTimestamp(timestamp: String): String {
        return try {
            when {
                timestamp.contains("T") -> {
                    val parts = timestamp.split("T")
                    val date = parts[0].split("-")
                    val time = parts.getOrNull(1)?.split(":")

                    if (date.size >= 3 && time != null && time.size >= 2) {
                        "${date[2]}/${date[1]} ${time[0]}:${time[1]}"
                    } else {
                        timestamp
                    }
                }
                else -> timestamp
            }
        } catch (e: Exception) {
            timestamp
        }
    }
}