package com.newsspeech.auto.presentation.mobile

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.newsspeech.auto.service.NewsPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity hiển thị trên điện thoại
 *
 * ✅ OPTIMIZED: Giảm thiểu main thread blocking
 * ✅ Simple UI: Giảm Compose rendering time
 * ✅ Async everything: TTS, state updates, operations
 */
@AndroidEntryPoint
class MobileActivity : ComponentActivity() {

    private val tag = "MobileActivity"
    private lateinit var carConnection: CarConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startTime = System.currentTimeMillis()
        Log.d(tag, "🚀 MobileActivity onCreate()")

        // ✅ 1. Register TTS SYNC (nhanh, không block)
        NewsPlayer.register("MobileActivity")

        // ✅ 2. Init TTS ASYNC (chạy background)
        lifecycleScope.launch(Dispatchers.IO) {
            NewsPlayer.init(applicationContext) { success ->
                val elapsed = System.currentTimeMillis() - startTime
                if (success) {
                    Log.i(tag, "✅ TTS init OK (${elapsed}ms)")
                } else {
                    Log.e(tag, "❌ TTS init FAIL (${elapsed}ms)")
                }
            }
        }

        // ✅ 3. Setup CarConnection
        carConnection = CarConnection(applicationContext)

        // ✅ 4. Set content IMMEDIATELY (không đợi gì cả)
        setContent {
            // ✅ MaterialTheme bọc ngoài để cache theme
            MaterialTheme {
                MobileApp(carConnection = carConnection)
            }
        }

        val elapsedTotal = System.currentTimeMillis() - startTime
        Log.d(tag, "⏱️ onCreate() completed in ${elapsedTotal}ms")
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
// COMPOSABLES - ULTRA SIMPLIFIED
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

    // ✅ Simple conditional rendering
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
 * ✅ ULTRA SIMPLE UI - Minimum components
 */
@Composable
fun MobileAppScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ✅ Only 2 states
    var isTtsReady by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("Đang khởi tạo TTS...") }

    // ✅ Async TTS status check
    LaunchedEffect(Unit) {
        launch(Dispatchers.Default) {
            kotlinx.coroutines.delay(1000) // Wait for init
            while (true) {
                val ready = NewsPlayer.isReady()
                if (ready != isTtsReady) {
                    isTtsReady = ready
                    if (ready) statusMsg = "Sẵn sàng"
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    // ✅ Simple Surface (không dùng Scaffold - quá phức tạp)
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
            StatusCard(isTtsReady = isTtsReady)

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
                onStatusChange = { statusMsg = it },
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

/**
 * Status card - Minimal styling
 */
@Composable
private fun StatusCard(isTtsReady: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isTtsReady) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
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
                if (isTtsReady) "✅ Đã sẵn sàng" else "⌛ Đang khởi tạo",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Action buttons - Ultra simple
 */
@Composable
private fun ActionButtons(
    isTtsReady: Boolean,
    onStatusChange: (String) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
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
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            onStatusChange("Đang phát...")
                        }
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
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        onStatusChange("Đã dừng")
                    }
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