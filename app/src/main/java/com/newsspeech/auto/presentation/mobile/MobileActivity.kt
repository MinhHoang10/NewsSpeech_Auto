package com.newsspeech.auto.presentation.mobile

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.car.app.connection.CarConnection
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.newsspeech.auto.data.repository.NewsRepository
import com.newsspeech.auto.domain.model.News
import com.newsspeech.auto.service.NewsPlayer
//import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity hiển thị trên điện thoại
 *
 * ✅ TTS ENABLED - Production ready
 */

@AndroidEntryPoint
class MobileActivity : ComponentActivity() {

    private val tag = "MobileActivity"
    private lateinit var carConnection: CarConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startTime = System.currentTimeMillis()
        Log.d(tag, "🚀 MobileActivity onCreate()")

        // ✅ TTS Registration
        NewsPlayer.register("MobileActivity")

        // ✅ Setup CarConnection
        carConnection = CarConnection(applicationContext)

        // ✅ Set content
        setContent {
            MaterialTheme {
                MobileApp(carConnection = carConnection)
            }
        }

        val elapsedSetContent = System.currentTimeMillis() - startTime
        Log.d(tag, "⏱️ setContent() completed in ${elapsedSetContent}ms")

        // ✅ TTS Initialization
        lifecycleScope.launch {
            delay(100)

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

        val elapsedTotal = System.currentTimeMillis() - startTime
        Log.d(tag, "✅ onCreate() completed in ${elapsedTotal}ms")
    }

    override fun onDestroy() {
        Log.d(tag, "🛑 MobileActivity onDestroy()")
        NewsPlayer.unregister("MobileActivity")
        super.onDestroy()
    }
}

// ========================================
// COMPOSABLES
// ========================================

@Composable
fun MobileApp(carConnection: CarConnection) {
    var connectionType by remember { mutableIntStateOf(CarConnection.CONNECTION_TYPE_NOT_CONNECTED) }

    DisposableEffect(carConnection) {
        val observer = Observer<Int> { type ->
            connectionType = type
        }
        carConnection.type.observeForever(observer)

        onDispose {
            carConnection.type.removeObserver(observer)
        }
    }

    if (connectionType == CarConnection.CONNECTION_TYPE_PROJECTION) {
        CarConnectedScreen()
    } else {
        NewsListScreen()
    }
}

@Composable
fun CarConnectedScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🚗", style = MaterialTheme.typography.displayLarge)
            Text(
                "Đang chạy trên Android Auto",
                color = Color.Gray,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Main news list screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListScreen() {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val newsRepo = remember { NewsRepository(context) }

    // State
    var newsList by remember { mutableStateOf<List<News>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // TTS State
    val isTtsReady by NewsPlayer.readyState.collectAsState()
    val queueSize by NewsPlayer.queueSize.collectAsState()
    val isSpeaking by NewsPlayer.currentlySpeaking.collectAsState()

    // Load news
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
                onStop = {
                    scope.launch(Dispatchers.Default) {
                        NewsPlayer.stop()
                    }
                }
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
                else -> NewsListView(
                    newsList = newsList,
                    isTtsReady = isTtsReady,
                    onNewsClick = { news ->
                        if (isTtsReady) {
                            scope.launch(Dispatchers.Default) {
                                val content = buildString {
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
                                NewsPlayer.addToQueue(content)
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Loading view
 */
@Composable
fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Đang tải tin tức...")
        }
    }
}

/**
 * Error view
 */
@Composable
fun ErrorView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text("❌", style = MaterialTheme.typography.displayMedium)
            Text(
                "Không thể tải tin tức",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Empty view
 */
@Composable
fun EmptyView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text("📭", style = MaterialTheme.typography.displayMedium)
            Text(
                "Chưa có tin tức",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Vui lòng thêm file all_news.json vào assets",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * News list view
 */
@Composable
fun NewsListView(
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
 * News item card
 */
@Composable
fun NewsItem(
    news: News,
    isTtsReady: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isTtsReady, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isTtsReady) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category tag
            if (news.category.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = news.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Title
            Text(
                text = news.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Content preview
            if (news.content.isNotEmpty()) {
                Text(
                    text = news.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Metadata
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (news.source.isNotEmpty()) {
                    Text(
                        text = news.source,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (news.timestamp.isNotEmpty()) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimestamp(news.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // TTS hint
            if (isTtsReady) {
                Text(
                    text = "🔊 Chạm để nghe",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "⏳ TTS đang khởi tạo...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * TTS Control bar at bottom
 */
@Composable
fun TtsControlBar(
    isTtsReady: Boolean,
    isSpeaking: Boolean,
    queueSize: Int,
    onStop: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = when {
                        !isTtsReady -> "⏳ Đang khởi tạo TTS..."
                        isSpeaking -> "🔊 Đang phát"
                        queueSize > 0 -> "⏸️ Có $queueSize tin đang chờ"
                        else -> "✅ Sẵn sàng"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                if (isSpeaking || queueSize > 0) {
                    LinearProgressIndicator(
                        modifier = Modifier.width(200.dp)
                    )
                }
            }

            // Stop button
            if (isSpeaking || queueSize > 0) {
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("⏹️ Dừng")
                }
            }
        }
    }
}

/**
 * Format timestamp
 */
fun formatTimestamp(timestamp: String): String {
    return try {
        when {
            timestamp.contains("T") -> {
                val parts = timestamp.split("T")
                val date = parts[0].split("-")
                val time = parts.getOrNull(1)?.split(":")

                if (date.size >= 3 && time != null && time.size >= 2) {
                    "${date[2]}/${date[1]} ${time[0]}:${time[1]}"
                } else {
                    timestamp
                }
            }
            else -> timestamp
        }
    } catch (e: Exception) {
        timestamp
    }
}