package com.newsspeech.auto.presentation.mobile

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.car.app.connection.CarConnection
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import com.newsspeech.auto.service.NewsPlayer

class MobileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Quan sát trạng thái kết nối
        val connectionLiveData: LiveData<Int> = CarConnection(this).type
        connectionLiveData.observe(this) { connectionType ->
            if (connectionType == CarConnection.CONNECTION_TYPE_PROJECTION) {
                Log.d("MobileActivity", "Đang kết nối Android Auto -> Đóng UI điện thoại để nhường Focus")

                // Đưa về background (đề phòng)
                moveTaskToBack(true)

                // QUAN TRỌNG: Gọi finish() để đóng hẳn Activity,
                // giúp InputDispatcher không bị kẹt focus vào điện thoại
                finish()
            }
        }

        setContent {
            MobileAppScreen()
        }

        // Khởi tạo NewsPlayer async
        NewsPlayer.init(applicationContext) { success ->
            if (!success) {
                Log.e("MobileActivity", "TTS init failed")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Đảm bảo TTS sẵn sàng khi quay lại app
        if (!NewsPlayer.isReady()) {
            NewsPlayer.init(applicationContext) { success ->
                if (!success) {
                    Log.w("MobileActivity", "TTS init on resume failed")
                }
            }
        }
    }

    override fun onDestroy() {
        // LƯU Ý QUAN TRỌNG:
        // Khi chạy Android Auto, Activity này sẽ bị finish() để ẩn đi.
        // Nếu gọi shutdown() ở đây, TTS trên xe cũng sẽ bị tắt theo.
        // Chỉ nên shutdown khi người dùng thực sự muốn thoát app hoàn toàn (hoặc xử lý trong Service).

        // NewsPlayer.shutdown()  <-- Đã comment lại để không ngắt giọng đọc trên xe

        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileAppScreen() {
    val context = LocalContext.current
    var ttsStatus by remember { mutableStateOf("Sẵn sàng") }
    var isInitializing by remember { mutableStateOf(false) }
    var isTtsReady by remember { mutableStateOf(NewsPlayer.isReady()) }

    MaterialTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("NewsSpeech Auto") }
                )
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
                // Hiển thị trạng thái TTS
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

                // Các nút chức năng
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            if (isTtsReady) {
                                NewsPlayer.addToQueue("Xin chào! Đây là thử nghiệm âm thanh.")
                                ttsStatus = "Đang phát..."
                            } else if (!isInitializing) {
                                isInitializing = true
                                NewsPlayer.init(context) { success ->
                                    isInitializing = false
                                    isTtsReady = success
                                    ttsStatus = if (success) "Đã sẵn sàng, thử lại" else "Lỗi: TTS chưa sẵn sàng"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        enabled = !isInitializing
                    ) {
                        Text(if (isInitializing) "Đang khởi tạo..." else "Phát thử TTS")
                    }

                    Button(
                        onClick = {
                            NewsPlayer.stop()
                            ttsStatus = "Đã dừng"
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

                    OutlinedButton(
                        onClick = {
                            isInitializing = true
                            NewsPlayer.init(context) { success ->
                                isInitializing = false
                                isTtsReady = success
                                ttsStatus = if (success) "Đã khởi tạo lại" else "Lỗi khởi tạo"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        enabled = !isInitializing
                    ) {
                        Text(if (isInitializing) "Đang khởi tạo..." else "Khởi tạo lại TTS")
                    }
                }

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