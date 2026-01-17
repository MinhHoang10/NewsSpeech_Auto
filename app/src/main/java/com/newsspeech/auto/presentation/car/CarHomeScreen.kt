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
import com.newsspeech.auto.presentation.car.screens.CategoryListScreen
import com.newsspeech.auto.presentation.car.screens.NewsByCategoryScreen
import com.newsspeech.auto.presentation.car.screens.NewsDetailScreen
import kotlinx.coroutines.launch

/**
 * Main screen cho Android Auto voi navigation
 *
 * Flow: Categories -> News by Category -> News Detail (Player)
 */
class CarHomeScreen(carContext: CarContext) : Screen(carContext) {

    private val TAG = "CarHomeScreen"

    // ViewModel
    private val viewModel = CarNewsViewModel(carContext)

    // Navigation state
    private var currentDestination: ScreenDestination = ScreenDestination.CategoryList
    private var currentCategoryNews: List<News> = emptyList()
    private var currentNewsIndex: Int = 0

    // Cached states
    private var lastUiState: CarUiState? = null
    private var lastTtsState: TtsState? = null

    init {
        Log.d(TAG, "CarHomeScreen initialized")
        observeStates()
    }

    /**
     * Observe states tu ViewModel
     */
    private fun observeStates() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state != lastUiState) {
                    lastUiState = state
                    invalidate()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.ttsState.collect { state ->
                if (state != lastTtsState) {
                    lastTtsState = state
                    invalidate()
                }
            }
        }
    }

    /**
     * Build template based on navigation state
     */
    override fun onGetTemplate(): Template {
        val uiState = lastUiState ?: CarUiState.Loading
        val ttsState = lastTtsState ?: TtsState()

        Log.d(TAG, "Building template - Destination: $currentDestination")

        // Handle loading/error/empty states
        when (uiState) {
            is CarUiState.Loading -> return LoadingView.build()
            is CarUiState.Empty -> return EmptyView.build()
            is CarUiState.Error -> return ErrorView.build(
                errorMessage = uiState.message,
                onRetry = { viewModel.refresh() }
            )
            is CarUiState.Success -> {
                // Navigate based on current destination
                return when (val dest = currentDestination) {
                    is ScreenDestination.CategoryList -> {
                        buildCategoryListScreen(uiState, ttsState)
                    }
                    is ScreenDestination.NewsByCategory -> {
                        buildNewsByCategoryScreen(dest.category, uiState, ttsState)
                    }
                    is ScreenDestination.NewsDetail -> {
                        buildNewsDetailScreen(dest.news, ttsState)
                    }
                }
            }
        }
    }

    /**
     * Build category list screen
     */
    private fun buildCategoryListScreen(
        uiState: CarUiState.Success,
        ttsState: TtsState
    ): Template {
        return CategoryListScreen.build(
            newsMap = uiState.newsMap.mapValues { it.value.map { 0 } }, // Just count
            ttsState = ttsState,
            onCategoryClick = { category ->
                navigateToCategory(category, uiState.newsMap[category] ?: emptyList())
            },
            onTestTts = { handleTestTts() }
        )
    }

    /**
     * Build news by category screen
     */
    private fun buildNewsByCategoryScreen(
        category: String,
        uiState: CarUiState.Success,
        ttsState: TtsState
    ): Template {
        return NewsByCategoryScreen.build(
            category = category,
            newsList = currentCategoryNews,
            ttsState = ttsState,
            onNewsClick = { news ->
                navigateToNewsDetail(news)
            },
            onBack = { handleBack() }
        )
    }

    /**
     * Build news detail screen
     */
    private fun buildNewsDetailScreen(
        news: News,
        ttsState: TtsState
    ): Template {
        return NewsDetailScreen.build(
            context = carContext,
            news = news,
            ttsState = ttsState,
            currentIndex = currentNewsIndex + 1,
            totalNews = currentCategoryNews.size,
            onPlayPause = { handlePlayPause(news) },
            onPrevious = { handlePrevious() },
            onNext = { handleNext() },
            onStop = { handleStop() }
        )
    }

    // ========================================
    // NAVIGATION
    // ========================================

    // ========================================
    // NAVIGATION
    // ========================================

    private fun navigateToCategory(category: String, newsList: List<News>) {
        Log.d(TAG, "Navigate to category: $category")
        currentDestination = ScreenDestination.NewsByCategory(category)
        currentCategoryNews = newsList
        invalidate()
    }

    private fun navigateToNewsDetail(news: News) {
        Log.d(TAG, "Navigate to news detail: ${news.title}")
        currentNewsIndex = currentCategoryNews.indexOf(news)
        currentDestination = ScreenDestination.NewsDetail(news)

        // Auto play when enter detail
        if (lastTtsState?.isReady == true) {
            viewModel.playNews(news)
        }

        invalidate()
    }

    // Back navigation is handled automatically by Action.BACK
    // Android Auto ScreenManager will pop the screen from stack when back is pressed

    // ========================================
    // PLAYER CONTROLS
    // ========================================

    private fun handleBack() {
        Log.d(TAG, "Back button pressed from: $currentDestination")
        when (currentDestination) {
            is ScreenDestination.NewsDetail -> {
                val category = (currentDestination as ScreenDestination.NewsDetail).news.category
                currentDestination = ScreenDestination.NewsByCategory(category)
                invalidate()
            }
            is ScreenDestination.NewsByCategory -> {
                currentDestination = ScreenDestination.CategoryList
                invalidate()
            }
            else -> {
                // Already at root
                Log.d(TAG, "Already at CategoryList, cannot go back")
            }
        }
    }

    private fun handlePlayPause(news: News) {
        Log.d(TAG, "Play/Pause")
        val ttsState = lastTtsState ?: return

        if (ttsState.isSpeaking) {
            viewModel.pauseTts()
        } else {
            viewModel.playNews(news)
        }
    }

    private fun handlePrevious() {
        Log.d(TAG, "Previous")
        if (currentNewsIndex > 0) {
            currentNewsIndex--
            val prevNews = currentCategoryNews[currentNewsIndex]
            currentDestination = ScreenDestination.NewsDetail(prevNews)
            viewModel.playNews(prevNews)
            invalidate()
        }
    }

    private fun handleNext() {
        Log.d(TAG, "Next")
        if (currentNewsIndex < currentCategoryNews.size - 1) {
            currentNewsIndex++
            val nextNews = currentCategoryNews[currentNewsIndex]
            currentDestination = ScreenDestination.NewsDetail(nextNews)
            viewModel.playNews(nextNews)
            invalidate()
        }
    }

    private fun handleStop() {
        Log.d(TAG, "Stop")
        viewModel.stopTts()
        showToast("Da dung phat")
    }

    private fun handleTestTts() {
        Log.d(TAG, "Test TTS")
        viewModel.testTts()
        showToast("Test TTS")
    }

    /**
     * Show toast helper
     */
    private fun showToast(message: String) {
        CarToast.makeText(carContext, message, CarToast.LENGTH_SHORT).show()
    }
}