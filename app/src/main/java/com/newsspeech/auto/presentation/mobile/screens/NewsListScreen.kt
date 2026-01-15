package com.newsspeech.auto.presentation.mobile.screens

import android.util.Log
import androidx.car.app.activity.ui.LoadingView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.newsspeech.auto.data.repository.NewsRepository
import com.newsspeech.auto.domain.model.News
import com.newsspeech.auto.presentation.mobile.components.*
import com.newsspeech.auto.service.NewsPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main news list screen for mobile
 * Displays list of news items with TTS playback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val newsRepo = remember { NewsRepository(context) }

    // News data state
    var newsList by remember { mutableStateOf<List<News>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // TTS state
    val isTtsReady by NewsPlayer.readyState.collectAsState()
    val queueSize by NewsPlayer.queueSize.collectAsState()
    val isSpeaking by NewsPlayer.currentlySpeaking.collectAsState()

    // Load news on first composition
    LaunchedEffect(Unit) {
        try {
            newsList = withContext(Dispatchers.IO) {
                newsRepo.loadNewsFromAssets()
            }
            Log.d("NewsListScreen", "✅ Loaded ${newsList.size} news items")
        } catch (e: Exception) {
            Log.e("NewsListScreen", "❌ Error loading news", e)
            errorMessage = e.message
        } finally {
            isLoading = false
        }
    }

    // Handle news click
    val onNewsClick: (News) -> Unit = { news ->
        if (isTtsReady) {
            scope.launch(Dispatchers.Default) {
                val content = buildNewsContent(news)
                NewsPlayer.addToQueue(content)
            }
        }
    }

    // Handle stop TTS
    val onStopTts: () -> Unit = {
        scope.launch(Dispatchers.Default) {
            NewsPlayer.stop()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("📰 Tin Tức Hôm Nay")
                        if (newsList.isNotEmpty()) {
                            Text(
                                "${newsList.size} tin",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            TtsControlBar(
                isTtsReady = isTtsReady,
                isSpeaking = isSpeaking,
                queueSize = queueSize,
                onStop = onStopTts
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> LoadingView()
                errorMessage != null -> ErrorView(errorMessage!!)
                newsList.isEmpty() -> EmptyView()
                else -> NewsListContent(
                    newsList = newsList,
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