package com.newsspeech.auto.presentation.mobile.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.newsspeech.auto.data.repository.NewsRepository
import com.newsspeech.auto.domain.model.News
import com.newsspeech.auto.presentation.mobile.NewsFilter
import com.newsspeech.auto.presentation.mobile.components.*
import com.newsspeech.auto.service.NewsPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main news list screen for mobile
 * ✅ REALTIME: Tin tức xuất hiện dần khi crawl
 * Supports filtering by source or category
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListScreen(
    filter: NewsFilter? = null,
    onBack: (() -> Unit)? = null,
    onNewsListLoaded: ((List<News>) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val newsRepo = remember { NewsRepository(context) }

    // 🔥 REALTIME: Dùng Flow để nhận update liên tục từ database
    var allNewsList by remember { mutableStateOf<List<News>>(emptyList()) }
    var isInitialLoad by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // TTS state
    val isTtsReady by NewsPlayer.readyState.collectAsState()
    val queueSize by NewsPlayer.queueSize.collectAsState()
    val isSpeaking by NewsPlayer.currentlySpeaking.collectAsState()

    // REALTIME: Lắng nghe database updates
    LaunchedEffect(Unit) {
        try {
            newsRepo.getNewsFlow().collect { newsList ->
                Log.d("NewsListScreen", "📡 Received ${newsList.size} news from database")

                if (newsList.isNotEmpty()) {
                    allNewsList = newsList
                    isInitialLoad = false
                    errorMessage = null
                    onNewsListLoaded?.invoke(newsList)
                } else if (!isInitialLoad) {
                    // Database bị clear, load từ JSON
                    Log.w("NewsListScreen", "⚠️ Database empty, loading from JSON...")
                    val jsonNews = newsRepo.loadNewsFromAssets()
                    if (jsonNews.isNotEmpty()) {
                        allNewsList = jsonNews
                        onNewsListLoaded?.invoke(jsonNews)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NewsListScreen", "❌ Error observing news flow", e)
            errorMessage = "Lỗi: ${e.localizedMessage ?: e.message}"
            isInitialLoad = false
        }
    }

    // Initial load from JSON if database empty
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val count = newsRepo.getNewsCount()
                if (count == 0) {
                    Log.d("NewsListScreen", "📂 Database empty, loading from JSON...")
                    val jsonNews = newsRepo.loadNewsFromAssets()
                    if (jsonNews.isNotEmpty()) {
                        allNewsList = jsonNews
                        onNewsListLoaded?.invoke(jsonNews)
                    }
                }
                isInitialLoad = false
            } catch (e: Exception) {
                Log.e("NewsListScreen", "❌ Error initial load", e)
                isInitialLoad = false
            }
        }
    }

    // Lọc danh sách tin tức dựa trên filter
    val filteredNewsList = remember(allNewsList, filter) {
        when (filter) {
            is NewsFilter.BySource -> {
                allNewsList.filter { it.source == filter.source }
            }
            is NewsFilter.ByTopic -> {
                allNewsList.filter { it.category == filter.topic }
            }
            null -> allNewsList
        }
    }

    // Manual refresh function
    val onRefresh: () -> Unit = {
        scope.launch {
            try {
                Log.d("NewsListScreen", "🔄 Manual refresh...")
                val freshNews = newsRepo.loadNews()
                if (freshNews.isNotEmpty()) {
                    allNewsList = freshNews
                    onNewsListLoaded?.invoke(freshNews)
                }
            } catch (e: Exception) {
                Log.e("NewsListScreen", "❌ Error refreshing", e)
                errorMessage = "Lỗi refresh: ${e.localizedMessage}"
            }
        }
    }

    // Handle news click
    val onNewsClick: (News) -> Unit = { news ->
        if (isTtsReady) {
            scope.launch(Dispatchers.Default) {
                val title = news.title
                val content = buildNewsContent(news)
                Log.d("NewsListScreen", "🔊 Adding to TTS queue: $title")
                NewsPlayer.addToQueue(title, content)

            }
        } else {
            Log.w("NewsListScreen", "⚠️ TTS not ready yet")
        }
    }

    // Handle stop TTS
    val onStopTts: () -> Unit = {
        scope.launch(Dispatchers.Default) {
            NewsPlayer.stop()
        }
    }

    val skipNextTts: () -> Unit = {
        scope.launch(Dispatchers.Default) {
            NewsPlayer.skipNext()
        }
    }

    // Tạo title dựa trên filter
    val screenTitle = when (filter) {
        is NewsFilter.BySource -> "📰 ${filter.source}"
        is NewsFilter.ByTopic -> "🗂️ ${filter.topic}"
        null -> "📰 Tin Tức Hôm Nay"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(screenTitle)

                        if (filteredNewsList.isNotEmpty()) {

                            Text(
//                                text = " Chao mung den voi tin tuc tu VnExpress",
//                                style = MaterialTheme.typography.bodyMedium,
                                "${filteredNewsList.size} tin",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Refresh button
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        },
        bottomBar = {
            TtsControlBar(
                isTtsReady = isTtsReady,
                isSpeaking = isSpeaking,
                queueSize = queueSize,
                onStop = onStopTts,
                skipNext = skipNextTts
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isInitialLoad -> LoadingView()
                errorMessage != null -> ErrorView(errorMessage!!)
                filteredNewsList.isEmpty() -> EmptyView()
                else -> NewsListContent(
                    newsList = filteredNewsList,
                    isTtsReady = isTtsReady,
                    onNewsClick = onNewsClick
                )
            }
        }
    }
}

/**
 * News list content - scrollable list of news items
 */
@Composable
private fun NewsListContent(
    newsList: List<News>,
    isTtsReady: Boolean,
    onNewsClick: (News) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = newsList,
            key = { it.id }
        ) { news ->
            NewsItem(
                news = news,
                isTtsReady = isTtsReady,
                onClick = { onNewsClick(news) }
            )
        }
    }
}

/**
 * Build TTS content from news
 */
private fun buildNewsContent(news: News): String {
    return buildString {
        append("Tin từ ")
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
}