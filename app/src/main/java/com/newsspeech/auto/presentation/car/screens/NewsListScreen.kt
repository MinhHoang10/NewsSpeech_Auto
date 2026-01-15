package com.newsspeech.auto.presentation.car.screens

import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Template
import com.newsspeech.auto.domain.model.News
import com.newsspeech.auto.presentation.car.TtsState
import com.newsspeech.auto.presentation.car.components.NewsItem
import com.newsspeech.auto.presentation.car.components.TtsControlBar

/**
 * Screen hiển thị danh sách tin tức
 */
object NewsListScreen {

    /**
     * Build template cho danh sách tin tức
     *
     * @param newsList Danh sách tin tức
     * @param ttsState Trạng thái TTS
     * @param onNewsClick Callback khi click vào tin
     * @param onTestTts Callback test TTS
     * @param onStopTts Callback stop TTS
     */
    fun build(
        newsList: List<News>,
        ttsState: TtsState,
        onNewsClick: (News) -> Unit,
        onTestTts: () -> Unit,
        onStopTts: () -> Unit
    ): Template {
        val itemListBuilder = ItemList.Builder()

        // 1. TTS Status Row
        itemListBuilder.addItem(TtsControlBar.buildStatusRow(ttsState))

        // 2. Test TTS Row (nếu TTS ready)
        if (ttsState.isReady) {
            itemListBuilder.addItem(TtsControlBar.buildTestRow(onTestTts))
        }

        // 3. Stop Button (nếu đang phát hoặc có queue)
        if (ttsState.isSpeaking || ttsState.queueSize > 0) {
            itemListBuilder.addItem(
                TtsControlBar.buildStopRow(
                    onStop = onStopTts,
                    enabled = true
                )
            )
        }

        // 4. News Items
        newsList.forEach { news ->
            itemListBuilder.addItem(
                NewsItem.build(
                    news = news,
                    isTtsReady = ttsState.isReady,
                    onClick = onNewsClick
                )
            )
        }

        return ListTemplate.Builder()
            .setTitle("Tin Tức Hôm Nay (${newsList.size})")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(itemListBuilder.build())
            .build()
    }
}