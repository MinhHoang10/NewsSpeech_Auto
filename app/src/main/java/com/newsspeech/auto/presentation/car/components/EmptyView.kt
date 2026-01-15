package com.newsspeech.auto.presentation.car.components

import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template

/**
 * Component hiển thị empty state
 */
object EmptyView {

    fun build(
        title: String = "Tin Tức",
        message: String = "Không có tin tức",
        subtitle: String? = null
    ): Template {
        val rowBuilder = Row.Builder()
            .setTitle("⚠️ $message")

        if (subtitle != null) {
            rowBuilder.addText(subtitle)
        } else {
            rowBuilder.addText("Không tìm thấy dữ liệu từ MongoDB")
            rowBuilder.addText("Vui lòng kiểm tra kết nối và khởi động lại app")
        }

        return ListTemplate.Builder()
            .setTitle(title)
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(
                ItemList.Builder()
                    .addItem(rowBuilder.build())
                    .build()
            )
            .build()
    }
}