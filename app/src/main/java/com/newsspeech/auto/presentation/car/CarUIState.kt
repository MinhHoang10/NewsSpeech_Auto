package com.newsspeech.auto.presentation.car

import com.newsspeech.auto.domain.model.News

/**
 * UI State cho Car Screen
 */
sealed class CarUiState {
    object Loading : CarUiState()
    object Empty : CarUiState()
    data class Success(
        val newsMap: Map<String, List<News>>,
        val allNews: List<News>
    ) : CarUiState()
    data class Error(val message: String) : CarUiState()
}

/**
 * TTS State
 */
data class TtsState(
    val isReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val queueSize: Int = 0,
    val currentNews: News? = null
)

/**
 * Screen destinations for navigation
 */
sealed class ScreenDestination {
    object CategoryList : ScreenDestination()
    data class NewsByCategory(val category: String) : ScreenDestination()
    data class NewsDetail(val news: News) : ScreenDestination()
}