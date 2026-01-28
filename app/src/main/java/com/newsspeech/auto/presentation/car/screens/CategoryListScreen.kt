package com.newsspeech.auto.presentation.car.screens

import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Template
import com.newsspeech.auto.presentation.car.components.CategoryItem
import com.newsspeech.auto.presentation.car.components.TtsControlBar
import com.newsspeech.auto.presentation.car.TtsState

/**
 * Man hinh danh sach categories
 */
object CategoryListScreen {

    /**
     * Build template hien thi cac categories
     *
     * @param newsMap Map<Category, NewsCount> -  Đổi từ List<Int> thành Int
     * @param ttsState TTS state
     * @param onCategoryClick Callback khi chon category
     * @param onTestTts Test TTS
     */
    fun build(
        newsMap: Map<String, Int>,  //  Đổi từ List<Int> thành Int
        ttsState: TtsState,
        onCategoryClick: (String) -> Unit,
        onTestTts: () -> Unit
    ): Template {
        val itemListBuilder = ItemList.Builder()

        // TTS Status
        itemListBuilder.addItem(TtsControlBar.buildStatusRow(ttsState))

        // Test TTS (neu ready)
        if (ttsState.isReady) {
            itemListBuilder.addItem(TtsControlBar.buildTestRow(onTestTts))
        }

        // Categories - ✅ Giờ newsCount đã là Int, không cần .size
        newsMap.entries
            .sortedByDescending { it.value } //  Sort trực tiếp theo Int
            .forEach { (category, newsCount) ->
                itemListBuilder.addItem(
                    CategoryItem.build(
                        category = category,
                        newsCount = newsCount,  //  Trực tiếp dùng Int
                        onClick = { onCategoryClick(category) }
                    )
                )
            }

        return ListTemplate.Builder()
            .setTitle("📰 Chu De Tin Tuc")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(itemListBuilder.build())
            .build()
    }
}