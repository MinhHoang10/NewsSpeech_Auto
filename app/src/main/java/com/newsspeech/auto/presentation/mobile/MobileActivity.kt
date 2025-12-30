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

/**
 * Activity hiển thị trên điện thoại
 *
 * ✅ Đăng ký/hủy đăng ký NewsPlayer đúng lifecycle
 * ✅ Xử lý CarConnection không memory leak
 * ✅ Hiển thị UI khác nhau khi connected/disconnected Android Auto
 */
class MobileActivity : ComponentActivity() {

    private val tag = "MobileActivity"
    private lateinit var carConnection: CarConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "🚀 MobileActivity onCreate()")

        // ✅ Đăng ký sử dụng TTS
        NewsPlayer.register("MobileActivity")

        // Khởi tạo CarConnection (để detect Android Auto)
        carConnection = CarConnection(applicationContext)

        setContent {
            MobileApp(carConnection = carConnection)
        }

        // Khởi tạo TTS
        initializeNewsPlayer()
    }

    private fun initializeNewsPlayer() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                NewsPlayer.init(applicationContext) { success ->
                    if (success) {
                        Log.i(tag, "✅ TTS init thành công trong Activity")
                    } else {
                        Log.e(tag, "❌ TTS init thất bại trong Activity")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Exception khi init TTS", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(tag, "▶️ onResume()")

        // Nếu TTS chưa sẵn sàng và Activity chưa bị destroy
        if (!NewsPlayer.isReady() && !isFinishing) {
            Log.w(tag, "⚠️ TTS chưa sẵn sàng, thử init lại")
            lifecycleScope.launch(Dispatchers.IO) {
                NewsPlayer.init(applicationContext) { success ->
                    if (!success) {
                        Log.w(tag, "❌ TTS init on resume thất bại")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(tag, "🛑 MobileActivity onDestroy()")

        // ✅ Hủy đăng ký TTS
        // TTS chỉ shutdown nếu activeUsers = 0
        NewsPlayer.unregister("MobileActivity")

        super.onDestroy()
        Log.d(tag, "✅ MobileActivity destroyed")
    }
}

// ========================================
// COMPOSABLES
// ========================================

/**
 * Root composable - Hiển thị UI khác nhau tùy trạng thái Android Auto
 */
@Composable
fun MobileApp(carConnection: CarConnection) {
//    val context = LocalContext.current

    // ✅ State để lưu connection type
    var connectionType by remember {
        mutableIntStateOf(CarConnection.CONNECTION_TYPE_NOT_CONNECTED)
    }

    // ✅ DisposableEffect: Tự động cleanup Observer khi Composable dispose
    DisposableEffect(carConnection) {
        val observer = Observer<Int> { type ->
            connectionType = type
            Log.d("MobileApp", "🔌 Connection type changed: $type")
        }

        // Đăng ký observer
        carConnection.type.observeForever(observer)
        Log.d("MobileApp", "✅ CarConnection observer registered")

        // Cleanup khi Composable bị dispose (ví dụ: xoay màn hình)
        onDispose {
            carConnection.type.removeObserver(observer)
            Log.d("MobileApp", "🗑️ CarConnection observer removed")
        }
    }

    val isCarConnected = remember(connectionType) {
        connectionType == CarConnection.CONNECTION_TYPE_PROJECTION
    }

    // Hiển thị UI tương ứng
    if (isCarConnected) {
        CarConnectedScreen()
    } else {
        MobileAppScreen()
    }
}

/**
 * Màn hình khi đang connected Android Auto
 */
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
            Text(
                text = "🚗",
                style = MaterialTheme.typography.displayLarge
            )

            Text(
                text = "Đang chạy trên Android Auto",
                color = Color.Gray,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Màn hình điện thoại tạm tắt để tối ưu hiệu năng",
                color = Color.DarkGray,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Màn hình chính khi chạy trên điện thoại
 */
@Composable
fun MobileAppScreen() {
    val context = LocalContext.current

    // States
    var ttsStatus by remember { mutableStateOf("Sẵn sàng") }
    var isInitializing by remember { mutableStateOf(false) }
    var isTtsReady by remember { mutableStateOf(NewsPlayer.isReady()) }

    MaterialTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // TTS Status Card
                TtsStatusCard(isTtsReady = isTtsReady)

                Spacer(Modifier.height(32.dp))

                // App Title
                Text(
                    "🎙️ NewsSpeech Auto",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Ứng dụng tin tức bằng giọng nói",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "Đang chạy chế độ Mobile.\nKết nối vào xe để sử dụng Android Auto.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(32.dp))

                // Action Buttons
                ActionButtons(
                    context = context,
                    isTtsReady = isTtsReady,
                    isInitializing = isInitializing,
                    onTtsStatusChange = { ttsStatus = it },
                    onTtsReadyChange = { isTtsReady = it },
                    onInitializingChange = { isInitializing = it }
                )

                Spacer(Modifier.height(24.dp))

                // Status text
                Text(
                    ttsStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Card hiển thị trạng thái TTS
 */
@Composable
private fun TtsStatusCard(isTtsReady: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTtsReady) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Trạng thái TTS",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (isTtsReady) "✅ Đã sẵn sàng" else "⌛ Chưa khởi tạo",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Các nút điều khiển
 */
@Composable
private fun ActionButtons(
    context: android.content.Context,
    isTtsReady: Boolean,
    isInitializing: Boolean,
    onTtsStatusChange: (String) -> Unit,
    onTtsReadyChange: (Boolean) -> Unit,
    onInitializingChange: (Boolean) -> Unit
) {
    // State để hiển thị dialog lỗi
    var showTtsErrorDialog by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Play Test Button
        Button(
            onClick = {
                if (isTtsReady) {
                    NewsPlayer.addToQueue("Xin chào! Đây là thử nghiệm âm thanh từ NewsSpeech Auto.")
                    onTtsStatusChange("Đang phát...")
                } else if (!isInitializing) {
                    onInitializingChange(true)
                    NewsPlayer.init(context) { success ->
                        onInitializingChange(false)
                        onTtsReadyChange(success)

                        if (success) {
                            onTtsStatusChange("Đã sẵn sàng, thử lại")
                        } else {
                            onTtsStatusChange("Lỗi: TTS chưa sẵn sàng")
                            showTtsErrorDialog = true
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            enabled = !isInitializing
        ) {
            Text(if (isInitializing) "Đang khởi tạo..." else "🔊 Phát thử TTS")
        }

        // Stop Button
        Button(
            onClick = {
                NewsPlayer.stop()
                onTtsStatusChange("Đã dừng")
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

        // Reinit Button
        OutlinedButton(
            onClick = {
                onInitializingChange(true)
                NewsPlayer.init(context) { success ->
                    onInitializingChange(false)
                    onTtsReadyChange(success)

                    if (success) {
                        onTtsStatusChange("Đã khởi tạo lại")
                    } else {
                        onTtsStatusChange("Lỗi khởi tạo")
                        showTtsErrorDialog = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            enabled = !isInitializing
        ) {
            Text(if (isInitializing) "Đang khởi tạo..." else "🔄 Khởi tạo lại TTS")
        }
    }

    // ✅ Dialog hướng dẫn khắc phục lỗi TTS
    if (showTtsErrorDialog) {
        AlertDialog(
            onDismissRequest = { showTtsErrorDialog = false },
            icon = { Text("⚠️", style = MaterialTheme.typography.displaySmall) },
            title = { Text("Lỗi Text-to-Speech") },
            text = {
                Text(
                    "TTS không khả dụng. Vui lòng kiểm tra:\n\n" +
                            "1️⃣ Vào Settings → Apps → Google Text-to-Speech\n" +
                            "2️⃣ Đảm bảo app đang BẬT (Enabled)\n" +
                            "3️⃣ Tải ngôn ngữ Tiếng Việt nếu chưa có\n" +
                            "4️⃣ Khởi động lại app sau khi cài đặt"
                )
            },
            confirmButton = {
                TextButton(onClick = { showTtsErrorDialog = false }) {
                    Text("Đã hiểu")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        try {
                            // Mở Settings TTS
                            val intent = Intent()
                            intent.action = "com.android.settings.TTS_SETTINGS"
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("MobileActivity", "Không thể mở TTS Settings", e)
                        }
                        showTtsErrorDialog = false
                    }
                ) {
                    Text("Mở Settings")
                }
            }
        )
    }
}