package com.newsspeech.auto.presentation.car

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newsspeech.auto.data.repository.NewsRepository
import com.newsspeech.auto.domain.model.News
import com.newsspeech.auto.service.NewsPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel cho Car UI
 *
 * - Quan ly state va business logic
 * - Observe TTS state
 * - Load news data
 */
class CarNewsViewModel(context: Context) : ViewModel() {

    private val TAG = "CarNewsViewModel"

    private val newsRepository = NewsRepository(context)

    // === UI State ===
    private val _uiState = MutableStateFlow<CarUiState>(CarUiState.Loading)
    val uiState: StateFlow<CarUiState> = _uiState.asStateFlow()

    // === TTS State ===
    private val _ttsState = MutableStateFlow(TtsState())
    val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    init {
        Log.d(TAG, "CarNewsViewModel initialized")
        observeTtsState()
        loadNews()
    }

    /**
     * Observe TTS state tu NewsPlayer
     */
    private fun observeTtsState() {
        viewModelScope.launch {
            NewsPlayer.readyState.collect { ready ->
                _ttsState.value = _ttsState.value.copy(isReady = ready)
                Log.d(TAG, "TTS ready: $ready")
            }
        }

        viewModelScope.launch {
            NewsPlayer.currentlySpeaking.collect { speaking ->
                _ttsState.value = _ttsState.value.copy(isSpeaking = speaking)
                Log.d(TAG, "TTS speaking: $speaking")
            }
        }

        viewModelScope.launch {
            NewsPlayer.queueSize.collect { size ->
                _ttsState.value = _ttsState.value.copy(queueSize = size)
                Log.d(TAG, "Queue size: $size")
            }
        }
    }

    /**
     * Load news data tu repository
     */
    private fun loadNews() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading news...")
                _uiState.value = CarUiState.Loading

                // Initialize repository neu chua
                // newsRepository.initialize()

                // Load news grouped by category
                val newsMap = newsRepository.getNewsGroupedByCategory()
                val allNews = newsMap.values.flatten()

                if (allNews.isEmpty()) {
                    _uiState.value = CarUiState.Empty
                    Log.w(TAG, "No news found")
                } else {
                    _uiState.value = CarUiState.Success(
                        newsMap = newsMap,
                        allNews = allNews
                    )
                    Log.i(TAG, "Loaded ${allNews.size} news in ${newsMap.size} categories")
                }

            } catch (e: Exception) {
                _uiState.value = CarUiState.Error(e.message ?: "Unknown error")
                Log.e(TAG, "Error loading news", e)
            }
        }
    }

    /**
     * Play news voi TTS
     */
    fun playNews(news: News) {
        if (!_ttsState.value.isReady) {
            Log.w(TAG, "TTS not ready")
            return
        }

        val contentToRead = buildString {
            append("Tin tu ")
            if (news.source.isNotEmpty()) {
                append(news.source)
                append(". ")
            }

            append(news.title)
            append(". ")

            if (news.content.isNotEmpty()) {
                append(news.content)
            }
        }

        NewsPlayer.addToQueue(contentToRead)
        Log.i(TAG, "Added news to queue: ${news.title}")
    }

    /**
     * Pause TTS
     */
    fun pauseTts() {
        NewsPlayer.stop()
        Log.d(TAG, "TTS paused")
    }

    /**
     * Test TTS
     */
    fun testTts() {
        NewsPlayer.addToQueue("Day la test TTS tren Android Auto")
        Log.d(TAG, "Test TTS triggered")
    }

    /**
     * Stop TTS
     */
    fun stopTts() {
        NewsPlayer.stop()
        Log.d(TAG, "TTS stopped")
    }

    /**
     * Refresh news
     */
    fun refresh() {
        Log.d(TAG, "Refreshing news...")
        loadNews()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "CarNewsViewModel cleared")
    }
}