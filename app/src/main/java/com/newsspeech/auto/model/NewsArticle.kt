package com.newsspeech.auto.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity cho Room Database
 * Lưu trữ tin tức đã crawl
 */
@Entity(tableName = "news_articles")
data class NewsArticle(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val image: String?,
    val link: String,
    val timestamp: String,
    val source: String,  // "VnExpress" hoặc "Otofun"
    val category: String
)

/**
 * Extension function để convert sang News (domain model)
 */
fun NewsArticle.toNews(): com.newsspeech.auto.domain.model.News {
    return com.newsspeech.auto.domain.model.News(
        id = id,
        title = title,
        content = content,
        link = link,
        timestamp = timestamp,
        source = source,
        category = category,
        image = image
    )
}