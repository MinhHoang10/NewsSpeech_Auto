package com.newsspeech.auto.presentation.car.screens

import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Template
import com.newsspeech.auto.domain.model.News
import com.newsspeech.auto.presentation.car.components.NewsItem
import com.newsspeech.auto.presentation.car.TtsState

/**
 * Màn hình danh sách tin theo category
 */
object NewsByCategoryScreen {

    /**
     * Build template hiển thị tin theo category
     *
     * @param category Tên category
     * @param newsList Danh sách tin
     * @param ttsState TTS state
     * @param onNewsClick Callback khi chọn tin
     * @param onBack Callback quay lại
     */
    fun build(
        category: String,
        newsList: List<News>,
        ttsState: TtsState,
        onNewsClick: (News) -> Unit,
        onBack: () -> Unit
    ): Template {
        val itemListBuilder = ItemList.Builder()

        // News items
        newsList.forEach { news ->
            itemListBuilder.addItem(
                NewsItem.build(
                    news = news,
                    isTtsReady = ttsState.isReady,
                    onClick = { onNewsClick(news) }
                )
            )
        }

        return ListTemplate.Builder()
            .setTitle("$category (${newsList.size})")
            .setHeaderAction(Action.BACK)
            .setSingleList(itemListBuilder.build())
            .build()
    }
}