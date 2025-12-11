package com.newsspeech.auto.presentation.mobile

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.newsspeech.auto.service.NewsPlayer

class MobileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_CAR) {
            // Ẩn mobile UI để nhường focus cho car screen
            finish()  // Hoặc moveTaskToBack(true) để minimize
            return
        }

        // Khởi tạo NewsPlayer với application context để tránh memory leak
        // NewsPlayer.init(applicationContext)

        setContent {
            MobileAppScreen()
        }

        // Init async
        NewsPlayer.init(applicationContext) { success ->
            if (!success) {
                // Toast hoặc update state: e.g., ttsStatus = "Lỗi init TTS"
                Log.e("MobileActivity", "TTS init failed")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Đảm bảo TTS sẵn sàng khi quay lại app
        if (!NewsPlayer.isReady()) {
            NewsPlayer.init(applicationContext)
        }
    }

    override fun onDestroy() {
        // Chỉ shutdown TTS khi app thực sự bị destroy
        NewsPlayer.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileAppScreen() {
    val context = LocalContext.current
    var ttsStatus by remember { mutableStateOf("Sẵn sàng") }
    var isInitializing by remember { mutableStateOf(false) }

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
                        containerColor = if (NewsPlayer.isReady()) {
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
                            if (NewsPlayer.isReady()) "✅ Đã sẵn sàng" else "❌ Chưa khởi tạo",
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
                    "Ứng dụng chính hoạt động trên Android Auto. Bạn có thể thử nghiệm tính năng Text-to-Speech tại đây.",
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
                            if (NewsPlayer.isReady()) {
                                NewsPlayer.addToQueue("Xin chào! ...")
                                ttsStatus = "Đang phát..."
                            } else if (!isInitializing) {
                                isInitializing = true
                                NewsPlayer.init(context) { success ->
                                    isInitializing = false
                                    ttsStatus = if (success) "Đã sẵn sàng, thử lại" else "Lỗi: TTS chưa sẵn sàng"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("Phát thử TTS")
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
                        )
                    ) {
                        Text("Dừng phát")
                    }

                    OutlinedButton(
                        onClick = {
                            NewsPlayer.init(context)
                            ttsStatus = if (NewsPlayer.isReady()) "Đã khởi tạo lại" else "Lỗi khởi tạo"
                        },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("Khởi tạo lại TTS")
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Hiển thị thông báo trạng thái
                Text(
                    ttsStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}