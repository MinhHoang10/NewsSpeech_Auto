package com.newsspeech.auto.presentation.car.components

import androidx.car.app.model.Row
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat

/**
 * Component cho category item
 */
object CategoryItem {

    /**
     * Build row cho 1 category
     *
     * @param category Tên category
     * @param newsCount Số lượng tin trong category
     * @param onClick Callback khi click
     */
    fun build(
        category: String,
        newsCount: Int,
        onClick: () -> Unit
    ): Row {
        return Row.Builder()
            .setTitle(getCategoryIcon(category) + " " + category)
            .addText("$newsCount tin tức")
            .setBrowsable(true)
            .setOnClickListener(onClick)
            .build()
    }

    /**
     * Lấy icon theo category
     */
    private fun getCategoryIcon(category: String): String {
        return when (category.lowercase()) {
            "thế giới" -> "🌍"
            "kinh tế" -> "💰"
            "thể thao" -> "⚽"
            "công nghệ" -> "💻"
            "giải trí" -> "🎬"
            "sức khỏe" -> "🏥"
            "pháp luật" -> "⚖️"
            "giáo dục" -> "📚"
            "xe" -> "🚗"
            "du lịch" -> "✈️"
            else -> "📰"
        }
    }
}