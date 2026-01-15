package com.newsspeech.auto.presentation.car.components

import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template

/**
 * Component hiển thị error state
 */
object ErrorView {

    fun build(
        title: String = "Lỗi",
        errorMessage: String,
        onRetry: (() -> Unit)? = null
    ): Template {
        val itemListBuilder = ItemList.Builder()

        // Error message row
        val errorRow = Row.Builder()
            .setTitle("❌ $errorMessage")
            .addText("Không thể tải tin tức từ MongoDB")
            .addText("Vui lòng kiểm tra kết nối internet và MongoDB")
            .build()

        itemListBuilder.addItem(errorRow)

        // Retry button row (if provided)
        if (onRetry != null) {
            val retryRow = Row.Builder()
                .setTitle("🔄 Thử lại")
                .addText("Chạm để tải lại tin tức")
                .setOnClickListener(onRetry)
                .build()

            itemListBuilder.addItem(retryRow)
        }

        return ListTemplate.Builder()
            .setTitle(title)
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(itemListBuilder.build())
            .build()
    }
}