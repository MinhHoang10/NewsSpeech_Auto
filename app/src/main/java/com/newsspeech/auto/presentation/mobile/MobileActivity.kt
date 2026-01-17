package com.newsspeech.auto.presentation.mobile

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.car.app.connection.CarConnection
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.newsspeech.auto.domain.model.News
import com.newsspeech.auto.presentation.mobile.screens.*
import com.newsspeech.auto.service.NewsPlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main Activity for Mobile UI
 * Responsibilities:
 * - TTS lifecycle management
 * - CarConnection setup
 * - Screen routing (Mobile vs Car mode)
 * - Navigation management
 */
@AndroidEntryPoint
class MobileActivity : ComponentActivity() {

    private val tag = "MobileActivity"
    private lateinit var carConnection: CarConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startTime = System.currentTimeMillis()
        Log.d(tag, "🚀 MobileActivity onCreate()")

        // Register TTS
        NewsPlayer.register("MobileActivity")

        // Setup CarConnection
        carConnection = CarConnection(applicationContext)

        // Set content
        setContent {
            MaterialTheme {
                MobileApp(carConnection = carConnection)
            }
        }

        val elapsedSetContent = System.currentTimeMillis() - startTime
        Log.d(tag, "⏱️ setContent() completed in ${elapsedSetContent}ms")

        // Initialize TTS in background
        initializeTts()

        val elapsedTotal = System.currentTimeMillis() - startTime
        Log.d(tag, "✅ onCreate() completed in ${elapsedTotal}ms")
    }

    private fun initializeTts() {
        lifecycleScope.launch {
            delay(100) // Wait for UI to render

            withContext(Dispatchers.IO) {
                val ttsStartTime = System.currentTimeMillis()

                NewsPlayer.init(applicationContext) { success ->
                    val elapsed = System.currentTimeMillis() - ttsStartTime
                    runOnUiThread {
                        if (success) {
                            Log.i(tag, "✅ TTS init OK (${elapsed}ms)")
                        } else {
                            Log.e(tag, "❌ TTS init FAIL (${elapsed}ms)")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(tag, "🛑 MobileActivity onDestroy()")
        NewsPlayer.unregister("MobileActivity")
        super.onDestroy()
    }
}

/**
 * Root composable - handles screen routing based on connection type
 */
@Composable
private fun MobileApp(carConnection: CarConnection) {
    var connectionType by remember {
        mutableIntStateOf(CarConnection.CONNECTION_TYPE_NOT_CONNECTED)
    }

    // Observe car connection state
    DisposableEffect(carConnection) {
        val observer = Observer<Int> { type ->
            connectionType = type
        }
        carConnection.type.observeForever(observer)

        onDispose {
            carConnection.type.removeObserver(observer)
        }
    }

    // Route to appropriate screen
    if (connectionType == CarConnection.CONNECTION_TYPE_PROJECTION) {
        CarConnectedScreen()
    } else {
        NavigationContainer()
    }
}

/**
 * Navigation container - quản lý điều hướng giữa các màn hình
 */
@Composable
private fun NavigationContainer() {
    // State để quản lý navigation
    var currentScreen by remember { mutableStateOf<Screen>(Screen.MainCategory) }
    var newsList by remember { mutableStateOf<List<News>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<NewsFilter?>(null) }

    // Load dữ liệu ban đầu cho MainCategoryScreen
    LaunchedEffect(Unit) {
        if (newsList.isEmpty()) {
            currentScreen = Screen.TempLoadData
        }
    }

    when (val screen = currentScreen) {
        is Screen.MainCategory -> {
            // ĐÂY LÀ MÀN HÌNH CHÍNH - Hiển thị cả Nguồn và Chủ đề
            MainCategoryScreen(
                newsList = newsList,
                isLoading = isLoading,
                onRefresh = {
                    isLoading = true
                    currentScreen = Screen.TempLoadData
                },
                onSourceClick = { source ->
                    selectedFilter = NewsFilter.BySource(source)
                    currentScreen = Screen.NewsList
                },
                onTopicClick = { topic ->
                    selectedFilter = NewsFilter.ByTopic(topic)
                    currentScreen = Screen.NewsList
                },
                onSettingsClick = {
                    currentScreen = Screen.Settings
                }
            )
        }

        is Screen.NewsList -> {
            // Màn hình danh sách tin đã lọc
            NewsListScreen(
                filter = selectedFilter,
                onBack = {
                    currentScreen = Screen.MainCategory
                },
                onNewsListLoaded = { loadedNews ->
                    // Lưu danh sách tin để dùng cho màn hình chính
                    if (newsList.isEmpty() || newsList != loadedNews) {
                        newsList = loadedNews
                        isLoading = false
                    }
                }
            )
        }

        is Screen.TempLoadData -> {
            // Màn hình tạm để load data ban đầu
            NewsListScreen(
                filter = null,
                onBack = null,
                onNewsListLoaded = { loadedNews ->
                    newsList = loadedNews
                    isLoading = false
                    currentScreen = Screen.MainCategory
                }
            )
        }

        is Screen.Settings -> {
            // Màn hình cài đặt
            SettingsScreen(
                onBack = {
                    currentScreen = Screen.MainCategory
                }
            )
        }
    }
}

/**
 * Sealed class để định nghĩa các màn hình
 */
private sealed class Screen {
    object MainCategory : Screen()
    object NewsList : Screen()
    object TempLoadData : Screen()
    object Settings : Screen()
}

/**
 * Filter để lọc tin tức
 */
sealed class NewsFilter {
    data class BySource(val source: String) : NewsFilter()
    data class ByTopic(val topic: String) : NewsFilter()
}