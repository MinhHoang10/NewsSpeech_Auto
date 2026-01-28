package com.newsspeech.auto.presentation.car

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.model.Template
import androidx.lifecycle.lifecycleScope
import com.newsspeech.auto.domain.model.News
import com.newsspeech.auto.presentation.car.components.EmptyView
import com.newsspeech.auto.presentation.car.components.ErrorView
import com.newsspeech.auto.presentation.car.components.LoadingView
import com.newsspeech.auto.presentation.car.screens.CategoryListScreen
import kotlinx.coroutines.launch

/**
 * Main screen - Category List
 */
class CarHomeScreen(carContext: CarContext) : Screen(carContext) {

    private val TAG = "CarHomeScreen"

    // ViewModel
    private val viewModel = CarNewsViewModel(carContext)

    // Cached states
    private var lastUiState: CarUiState? = null
    private var lastTtsState: TtsState? = null

    init {
        Log.d(TAG, "CarHomeScreen initialized")
        observeStates()
    }

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

    override fun onGetTemplate(): Template {
        val uiState = lastUiState ?: CarUiState.Loading
        val ttsState = lastTtsState ?: TtsState()

        Log.d(TAG, "Building CategoryList template")

        return when (uiState) {
            is CarUiState.Loading -> LoadingView.build()
            is CarUiState.Empty -> EmptyView.build()
            is CarUiState.Error -> ErrorView.build(
                errorMessage = uiState.message,
                onRetry = { viewModel.refresh() }
            )
            is CarUiState.Success -> {
                CategoryListScreen.build(
                    newsMap = uiState.newsMap.mapValues { it.value.size },
                    ttsState = ttsState,
                    onCategoryClick = { category ->
                        navigateToCategory(category, uiState.newsMap[category] ?: emptyList())
                    },
                    onTestTts = { handleTestTts() }
                )
            }
        }
    }

    private fun navigateToCategory(category: String, newsList: List<News>) {
        Log.d(TAG, "Navigate to category: $category")
        val categoryScreen = NewsByCategoryScreen(carContext, category, newsList, viewModel)
        screenManager.push(categoryScreen)
    }

    private fun handleTestTts() {
        Log.d(TAG, "Test TTS")
        viewModel.testTts()
        showToast("Test TTS")
    }

    private fun showToast(message: String) {
        CarToast.makeText(carContext, message, CarToast.LENGTH_SHORT).show()
    }
}

/**
 * Screen hiển thị danh sách tin theo category
 */
class NewsByCategoryScreen(
    carContext: CarContext,
    private val category: String,
    private val newsList: List<News>,
    private val viewModel: CarNewsViewModel
) : Screen(carContext) {

    private val TAG = "NewsByCategoryScreen"

    private var lastTtsState: TtsState? = null

    init {
        Log.d(TAG, "NewsByCategoryScreen initialized for: $category")
        observeTtsState()
    }

    private fun observeTtsState() {
        lifecycleScope.launch {
            viewModel.ttsState.collect { state ->
                if (state != lastTtsState) {
                    lastTtsState = state
                    invalidate()
                }
            }
        }
    }

    override fun onGetTemplate(): Template {
        val ttsState = lastTtsState ?: TtsState()

        return com.newsspeech.auto.presentation.car.screens.NewsByCategoryScreen.build(
            category = category,
            newsList = newsList,
            ttsState = ttsState,
            onNewsClick = { news ->
                navigateToNewsDetail(news)
            }
        )
    }

    private fun navigateToNewsDetail(news: News) {
        Log.d(TAG, "Navigate to news detail: ${news.title}")
        val newsIndex = newsList.indexOf(news)
        val detailScreen = NewsDetailScreen(carContext, news, newsList, newsIndex, viewModel)
        screenManager.push(detailScreen)
    }
}

/**
 * Screen hiển thị chi tiết tin
 */
class NewsDetailScreen(
    carContext: CarContext,
    private val news: News,
    private val allNews: List<News>,
    private var currentIndex: Int,
    private val viewModel: CarNewsViewModel
) : Screen(carContext) {

    private val TAG = "NewsDetailScreen"

    private var lastTtsState: TtsState? = null

    init {
        Log.d(TAG, "NewsDetailScreen initialized for: ${news.title}")
        observeTtsState()
        observeLifecycle()

        // Auto play when enter detail
        if (viewModel.ttsState.value.isReady) {
            viewModel.playNews(news)
        }
    }

    private fun observeTtsState() {
        lifecycleScope.launch {
            viewModel.ttsState.collect { state ->
                if (state != lastTtsState) {
                    lastTtsState = state
                    invalidate()
                }
            }
        }
    }

    private fun observeLifecycle() {
        lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                super.onDestroy(owner)
                viewModel.stopTts()
                Log.d(TAG, "NewsDetailScreen destroyed, TTS stopped")
            }
        })
    }

    override fun onGetTemplate(): Template {
        val ttsState = lastTtsState ?: TtsState()

        return com.newsspeech.auto.presentation.car.screens.NewsDetailScreen.build(
            context = carContext,
            news = news,
            ttsState = ttsState,
            currentIndex = currentIndex + 1,
            totalNews = allNews.size,
            onPlayPause = { handlePlayPause(news) },
            onPrevious = { handlePrevious() },
            onNext = { handleNext() },
            onStop = { handleStop() }
        )
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
        if (currentIndex > 0) {
            currentIndex--
            val prevNews = allNews[currentIndex]

            // Update this screen instead of navigating
            viewModel.playNews(prevNews)
            invalidate()
        }
    }

    private fun handleNext() {
        Log.d(TAG, "Next")
        if (currentIndex < allNews.size - 1) {
            currentIndex++
            val nextNews = allNews[currentIndex]

            // Update this screen instead of navigating
            viewModel.playNews(nextNews)
            invalidate()
        }
    }

    private fun handleStop() {
        Log.d(TAG, "Stop")
        viewModel.stopTts()
        showToast("Da dung phat")
    }

    private fun showToast(message: String) {
        CarToast.makeText(carContext, message, CarToast.LENGTH_SHORT).show()
    }
}