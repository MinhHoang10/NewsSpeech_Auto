package com.newsspeech.auto.presentation.car.components

import androidx.car.app.model.Action
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Template

/**
 * Component hiển thị loading state
 */
object LoadingView {

    fun build(title: String = "Đang tải tin tức..."): Template {
        return ListTemplate.Builder()
            .setTitle(title)
            .setLoading(true)
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}