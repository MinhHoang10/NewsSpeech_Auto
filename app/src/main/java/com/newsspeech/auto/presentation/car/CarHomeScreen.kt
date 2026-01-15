package com.newsspeech.auto.presentation.car

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Template
import androidx.lifecycle.lifecycleScope
import com.newsspeech.auto.domain.model.News
import com.newsspeech.auto.presentation.car.components.EmptyView
import com.newsspeech.auto.presentation.car.components.ErrorView
import com.newsspeech.auto.presentation.car.components.LoadingView
import com.newsspeech.auto.presentation.car.screens.NewsListScreen
import kotlinx.coroutines.launch

/**
 * Màn hình chính trên Android Auto
 *
 * ✅ Clean Architecture với MVVM
 * ✅ Component-based UI
 * ✅ Reactive state management
 */
class CarHomeScreen(carContext: CarContext) : Screen(carContext) {

    private val TAG = "CarHomeScreen"

    // ViewModel quản lý state và business logic
    private val viewModel = CarNewsViewModel(carContext)

    // State cache để tránh re-render không cần thiết
    private var lastUiState: CarUiState? = null
    private var lastTtsState: TtsState? = null

    init {
        Log.d(TAG, "🖥️ CarHomeScreen initialized")
        observeStates()
    }

    /**
     * Observe các state từ ViewModel
     */
    private fun observeStates() {
        // Observe UI State (news data)
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state != lastUiState) {
                    Log.d(TAG, "🔄 UI State changed: $state")
                    lastUiState = state
                    invalidate()
                }
            }
        }

        // Observe TTS State
        lifecycleScope.launch {
            viewModel.ttsState.collect { state ->
                if (state != lastTtsState) {
                    Log.d(TAG, "🎤 TTS State changed: $state")
                    lastTtsState = state
                    invalidate()
                }
            }
        }
    }

    /**
     * Build template dựa trên state
     */
    override fun onGetTemplate(): Template {
        val uiState = lastUiState ?: CarUiState.Loading
        val ttsState = lastTtsState ?: TtsState()

        Log.d(TAG, "🎨 Building template - UI: $uiState, TTS: $ttsState")

        return when (uiState) {
            is CarUiState.Loading -> {
                LoadingView.build()
            }

            is CarUiState.Empty -> {
                EmptyView.build()
            }

            is CarUiState.Error -> {
                ErrorView.build(
                    errorMessage = uiState.message,
                    onRetry = { handleRetry() }
                )
            }

            is CarUiState.Success -> {
                NewsListScreen.build(
                    newsList = uiState.allNews,
                    ttsState = ttsState,
                    onNewsClick = { news -> handleNewsClick(news) },
                    onTestTts = { handleTestTts() },
                    onStopTts = { handleStopTts() }
                )
            }
        }
    }

    /**
     * Xử lý khi user click vào tin
     */
    private fun handleNewsClick(news: News) {
        Log.d(TAG, "👆 User clicked: ${news.title}")

        val ttsState = lastTtsState ?: TtsState()

        if (!ttsState.isReady) {
            showToast("Đang khởi tạo TTS, vui lòng thử lại")
            return
        }

        // Play news qua ViewModel
        viewModel.playNews(news)

        // Show toast
        showToast("🔊 Đang phát...")
    }

    /**
     * Test TTS
     */
    private fun handleTestTts() {
        Log.d(TAG, "🔊 Test TTS triggered")
        viewModel.testTts()
        showToast("Test TTS")
    }

    /**
     * Stop TTS
     */
    private fun handleStopTts() {
        Log.d(TAG, "⏹️ Stop TTS triggered")
        viewModel.stopTts()
        showToast("Đã dừng phát")
    }

    /**
     * Retry loading
     */
    private fun handleRetry() {
        Log.d(TAG, "🔄 Retry triggered")
        viewModel.refresh()
        showToast("Đang tải lại...")
    }

    /**
     * Helper để show toast
     */
    private fun showToast(message: String) {
        CarToast.makeText(carContext, message, CarToast.LENGTH_SHORT).show()
    }
}