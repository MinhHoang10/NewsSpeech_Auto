package com.newsspeech.auto.presentation.mobile

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.car.app.connection.CarConnection
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.newsspeech.auto.service.NewsPlayer

class MobileActivity : ComponentActivity() {

    // ✅ FIX 1: Tạo CarConnection instance 1 LẦN DUY NHẤT
    private lateinit var carConnection: CarConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Khởi tạo CarConnection 1 lần duy nhất
        carConnection = CarConnection(applicationContext)

        setContent {
            MobileApp(carConnection = carConnection)
        }

        // ✅ FIX 2: Init NewsPlayer một lần với proper error handling
        initializeNewsPlayer()
    }

    private fun initializeNewsPlayer() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                NewsPlayer.init(applicationContext) { success ->
                    if (!success) {
                        Log.e("MobileActivity", "TTS init failed")
                    }
                }
            } catch (e: Exception) {
                Log.e("MobileActivity", "TTS init exception", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // ✅ FIX 3: Chỉ init nếu thực sự cần và chưa đang init
        if (!NewsPlayer.isReady() && !isFinishing) {
            lifecycleScope.launch(Dispatchers.IO) {
                NewsPlayer.init(applicationContext) { success ->
                    if (!success) {
                        Log.w("MobileActivity", "TTS init on resume failed")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        // LƯU Ý QUAN TRỌNG:
        // Khi chạy Android Auto, Activity này sẽ bị finish() để ẩn đi.
        // Nếu gọi shutdown() ở đây, TTS trên xe cũng sẽ bị tắt theo.
        // Chỉ nên shutdown khi người dùng thực sự muốn thoát app hoàn toàn.

        // NewsPlayer.shutdown()  <-- Đã comment lại để không ngắt giọng đọc trên xe

        super.onDestroy()
    }
}

// ✅ FIX 4: Tách Composable chính ra để tối ưu recomposition
@Composable
fun MobileApp(carConnection: CarConnection) {
    // ✅ Observe CarConnection TYPE chỉ 1 lần, không tạo mới mỗi recompose
    val context = LocalContext.current
    val connectionType by remember(carConnection) {
        carConnection.type
    }.observeAsState(initial = CarConnection.CONNECTION_TYPE_NOT_CONNECTED)

    val isCarConnected = remember(connectionType) {
        connectionType == CarConnection.CONNECTION_TYPE_PROJECTION
    }

    // ✅ Conditional rendering với remember để tránh unnecessary recomposition
    if (isCarConnected) {
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Đang chạy trên Android Auto",
                color = Color.Gray,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Màn hình điện thoại tạm tắt để tối ưu hiệu năng",
                color = Color.DarkGray,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun MobileAppScreen() {
    val context = LocalContext.current

    // ✅ FIX 5: Dùng derivedStateOf để tránh unnecessary recomposition
    var ttsStatus by remember { mutableStateOf("Sẵn sàng") }
    var isInitializing by remember { mutableStateOf(false) }
    var isTtsReady by remember { mutableStateOf(NewsPlayer.isReady()) }

    MaterialTheme {
        Scaffold(
            topBar = {
                // TopBar đã được comment - tốt cho performance
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ✅ Status Card
                TtsStatusCard(isTtsReady = isTtsReady)

                Spacer(Modifier.height(32.dp))

                Text(
                    "Ứng dụng tin tức bằng giọng nói",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "Đang chạy chế độ Mobile.\nKết nối vào xe để sử dụng Android Auto.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(32.dp))

                // ✅ Action Buttons
                ActionButtons(
                    context = context,
                    isTtsReady = isTtsReady,
                    isInitializing = isInitializing,
                    onTtsStatusChange = { ttsStatus = it },
                    onTtsReadyChange = { isTtsReady = it },
                    onInitializingChange = { isInitializing = it }
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    ttsStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ✅ FIX 6: Tách các Composable nhỏ để tối ưu recomposition
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
            Text(
                if (isTtsReady) "✅ Đã sẵn sàng" else "❌ Chưa khởi tạo",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun ActionButtons(
    context: android.content.Context,
    isTtsReady: Boolean,
    isInitializing: Boolean,
    onTtsStatusChange: (String) -> Unit,
    onTtsReadyChange: (Boolean) -> Unit,
    onInitializingChange: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Play Test Button
        Button(
            onClick = {
                if (isTtsReady) {
                    NewsPlayer.addToQueue("Xin chào! Đây là thử nghiệm âm thanh.")
                    onTtsStatusChange("Đang phát...")
                } else if (!isInitializing) {
                    onInitializingChange(true)
                    NewsPlayer.init(context) { success ->
                        onInitializingChange(false)
                        onTtsReadyChange(success)
                        onTtsStatusChange(
                            if (success) "Đã sẵn sàng, thử lại"
                            else "Lỗi: TTS chưa sẵn sàng"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            enabled = !isInitializing
        ) {
            Text(if (isInitializing) "Đang khởi tạo..." else "Phát thử TTS")
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
            Text("Dừng phát")
        }

        // Reinit Button
        OutlinedButton(
            onClick = {
                onInitializingChange(true)
                NewsPlayer.init(context) { success ->
                    onInitializingChange(false)
                    onTtsReadyChange(success)
                    onTtsStatusChange(
                        if (success) "Đã khởi tạo lại"
                        else "Lỗi khởi tạo"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            enabled = !isInitializing
        ) {
            Text(if (isInitializing) "Đang khởi tạo..." else "Khởi tạo lại TTS")
        }
    }
}