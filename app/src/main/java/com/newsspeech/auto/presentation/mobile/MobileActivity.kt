package com.newsspeech.auto.presentation.mobile

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.car.app.connection.CarConnection
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.newsspeech.auto.service.NewsPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity hiển thị trên điện thoại
 *
 * ✅ FIX: TTS init KHÔNG block UI render
 * ✅ FIX: setContent() gọi NGAY LẬP TỨC
 * ✅ FIX: TTS init chạy SAU KHI UI đã render xong
 */
@AndroidEntryPoint
class MobileActivity : ComponentActivity() {

    private val tag = "MobileActivity"
    private lateinit var carConnection: CarConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startTime = System.currentTimeMillis()
        Log.d(tag, "🚀 MobileActivity onCreate()")

        // ✅ 1. Register TTS đồng bộ (KHÔNG init)
        NewsPlayer.register("MobileActivity")

        // ✅ 2. Setup CarConnection
        carConnection = CarConnection(applicationContext)

        // ✅ 3. Set content NGAY LẬP TỨC (< 50ms)
        setContent {
            MaterialTheme {
                MobileApp(carConnection = carConnection)
            }
        }

        val elapsedSetContent = System.currentTimeMillis() - startTime
        Log.d(tag, "⏱️ setContent() completed in ${elapsedSetContent}ms")

        // ✅ 4. QUAN TRỌNG: Init TTS SAU KHI UI render
        // Delay 100ms để đảm bảo frame đầu tiên đã vẽ xong
        lifecycleScope.launch {
            delay(100) // Chờ UI render xong

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
        Log.d(tag, "✅ onCreate() completed in ${elapsedTotal}ms (without TTS)")
    }

    override fun onResume() {
        super.onResume()
        Log.d(tag, "▶️ onResume()")
    }

    override fun onDestroy() {
        Log.d(tag, "🛑 MobileActivity onDestroy()")
        NewsPlayer.unregister("MobileActivity")
        super.onDestroy()
    }
}

// ========================================
// COMPOSABLES - OPTIMIZED
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
        MobileAppScreen()
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
 * ✅ OPTIMIZED: Sử dụng remember để giảm recomposition
 */
@Composable
fun MobileAppScreen() {
    val scope = rememberCoroutineScope()

    // ✅ Collect StateFlow (real-time updates)
    val isTtsReady by NewsPlayer.readyState.collectAsState()
    val queueSize by NewsPlayer.queueSize.collectAsState()
    val isSpeaking by NewsPlayer.currentlySpeaking.collectAsState()

    // ✅ Remember computed state để tránh recompute mỗi frame
    val statusMsg by remember {
        derivedStateOf {
            when {
                !isTtsReady -> "Đang khởi tạo TTS..."
                isSpeaking -> "Đang phát... (còn $queueSize tin)"
                queueSize > 0 -> "Có $queueSize tin đang chờ"
                else -> "Sẵn sàng"
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Status Card
            StatusCard(isTtsReady = isTtsReady, isSpeaking = isSpeaking)

            Spacer(Modifier.height(32.dp))

            // Title
            Text("🎙️ NewsSpeech Auto", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Ứng dụng tin tức bằng giọng nói",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Đang chạy chế độ Mobile.\nKết nối vào xe để sử dụng Android Auto.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // Buttons
            ActionButtons(
                isTtsReady = isTtsReady,
                scope = scope
            )

            Spacer(Modifier.height(24.dp))

            // Status text
            Text(
                statusMsg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusCard(isTtsReady: Boolean, isSpeaking: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = when {
            isSpeaking -> MaterialTheme.colorScheme.tertiaryContainer
            isTtsReady -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.errorContainer
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Trạng thái TTS", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    isSpeaking -> "🔊 Đang phát"
                    isTtsReady -> "✅ Đã sẵn sàng"
                    else -> "⌛ Đang khởi tạo"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun ActionButtons(
    isTtsReady: Boolean,
    scope: CoroutineScope
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Play button
        Button(
            onClick = {
                if (isTtsReady) {
                    scope.launch(Dispatchers.Default) {
                        NewsPlayer.addToQueue("Xin chào! Đây là thử nghiệm âm thanh từ NewsSpeech Auto.")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            enabled = isTtsReady
        ) {
            Text("🔊 Phát thử TTS")
        }

        // Stop button
        Button(
            onClick = {
                scope.launch(Dispatchers.Default) {
                    NewsPlayer.stop()
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            enabled = isTtsReady
        ) {
            Text("⏹️ Dừng phát")
        }
    }
}