package com.newsspeech.auto.domain.model

data class News(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val link: String = "",
    val timestamp: String = "",
    val source: String = "",
    val category: String = "",
    val image: String? = null
)