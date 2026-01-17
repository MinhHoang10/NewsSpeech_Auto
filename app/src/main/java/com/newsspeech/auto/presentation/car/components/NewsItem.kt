package com.newsspeech.auto.presentation.car.components

import androidx.car.app.model.Row
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.newsspeech.auto.domain.model.News

/**
 * Component cho moi news item
 */
object NewsItem {

    /**
     * Build row cho 1 tin tuc
     *
     * @param news News data
     * @param isTtsReady TTS co san sang khong
     * @param onClick Callback khi click
     */
    fun build(
        news: News,
        isTtsReady: Boolean,
        onClick: (News) -> Unit
    ): Row {
        val rowBuilder = Row.Builder()
            .setTitle(news.title)

        // Add image if available
        if (!news.image.isNullOrEmpty()) {
            try {
                rowBuilder.setImage(
                    CarIcon.Builder(
                        IconCompat.createWithContentUri(news.image)
                    ).build(),
                    Row.IMAGE_TYPE_LARGE
                )
            } catch (e: Exception) {
                // Skip image if failed
            }
        }

        // Description
        val description = when {
            news.content.isNotEmpty() -> {
                if (news.content.length > 100) {
                    news.content.take(100) + "..."
                } else {
                    news.content
                }
            }
            else -> "Cham de nghe chi tiet"
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
            rowBuilder.addText("⏳ TTS dang khoi tao...")
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
     * Format timestamp tu ISO format sang readable format
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