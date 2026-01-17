package com.newsspeech.auto.presentation.mobile.screens

import android.speech.tts.Voice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.newsspeech.auto.service.NewsPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Màn hình Settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // TTS Settings State
    var availableVoices by remember { mutableStateOf<List<Voice>>(emptyList()) }
    var currentVoice by remember { mutableStateOf<Voice?>(null) }
    var speechRate by remember { mutableStateOf(1.0f) }
    var isTestingSpeech by remember { mutableStateOf(false) }
    var selectedVoiceForTest by remember { mutableStateOf<String?>(null) }

    // Load initial settings
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            availableVoices = NewsPlayer.getAvailableVietnameseVoices()
            currentVoice = NewsPlayer.getCurrentVoice()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Cài đặt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === PHẦN 1: TỐC ĐỘ ĐỌC ===
            item {
                SettingSection(title = "🎚️ Tốc độ đọc")
            }

            item {
                SpeechRateSlider(
                    currentRate = speechRate,
                    onRateChanged = { newRate ->
                        speechRate = newRate
                        scope.launch(Dispatchers.Default) {
                            NewsPlayer.setSpeechRate(newRate)
                        }
                    }
                )
            }

            // Spacer
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // === PHẦN 2: GIỌNG ĐỌC ===
            item {
                SettingSection(
                    title = "🎤 Giọng đọc",
                    subtitle = if (availableVoices.size > 1) {
                        "${availableVoices.size} giọng có sẵn"
                    } else {
                        "Chỉ có 1 giọng"
                    }
                )
            }

            if (availableVoices.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            "Không tìm thấy giọng đọc tiếng Việt",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else {
                itemsIndexed(availableVoices) { index, voice ->
                    VoiceSelectionCard(
                        voiceIndex = index + 1,
                        voice = voice,
                        isSelected = currentVoice?.name == voice.name,
                        isTesting = selectedVoiceForTest == voice.name && isTestingSpeech,
                        onSelect = {
                            scope.launch(Dispatchers.Default) {
                                val success = NewsPlayer.setVoice(voice.name)
                                if (success) {
                                    currentVoice = voice
                                }
                            }
                        },
                        onTest = {
                            if (isTestingSpeech && selectedVoiceForTest == voice.name) {
                                // Đang test giọng này -> Stop
                                scope.launch(Dispatchers.Default) {
                                    NewsPlayer.stop()
                                }
                                isTestingSpeech = false
                                selectedVoiceForTest = null
                            } else {
                                // Test giọng mới
                                scope.launch(Dispatchers.Default) {
                                    // Lưu giọng hiện tại
                                    val previousVoice = NewsPlayer.getCurrentVoice()

                                    // Đổi sang giọng test
                                    NewsPlayer.setVoice(voice.name)

                                    // Phát thử
                                    selectedVoiceForTest = voice.name
                                    isTestingSpeech = true

                                    NewsPlayer.addToQueue(
                                        "Đây là giọng đọc số ${index + 1}. " +
                                                "Xin chào, đây là ví dụ về giọng đọc tiếng Việt. " +
                                                "Bạn có thích giọng này không?"
                                    )

                                    // Đợi đọc xong rồi restore giọng cũ
                                    kotlinx.coroutines.delay(6000)

                                    if (previousVoice != null && currentVoice?.name != voice.name) {
                                        NewsPlayer.setVoice(previousVoice.name)
                                    }

                                    isTestingSpeech = false
                                    selectedVoiceForTest = null
                                }
                            }
                        }
                    )
                }
            }

            // Spacer
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // === PHẦN 3: VỀ ỨNG DỤNG ===
            item {
                SettingSection(title = "ℹ️ Thông tin")
            }

            item {
                AboutCard()
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    subtitle: String? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Divider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun SpeechRateSlider(
    currentRate: Float,
    onRateChanged: (Float) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tốc độ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${String.format("%.1f", currentRate)}x",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Slider(
                value = currentRate,
                onValueChange = onRateChanged,
                valueRange = 0.5f..2.0f,
                steps = 14, // 0.5, 0.6, 0.7, ... 2.0
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "0.5x (Chậm)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "2.0x (Nhanh)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VoiceSelectionCard(
    voiceIndex: Int,
    voice: Voice,
    isSelected: Boolean,
    isTesting: Boolean,
    onSelect: () -> Unit,
    onTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎙️",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Column {
                        Text(
                            "Giọng nói $voiceIndex",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            getVoiceQuality(voice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Đang chọn",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Test button
                OutlinedButton(
                    onClick = onTest,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isTesting) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (isTesting) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isTesting) "Dừng" else "Nghe thử")
                }

                // Select button
                if (!isSelected) {
                    Button(
                        onClick = onSelect,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Chọn")
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "📰 News Speech Auto",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Phiên bản: 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Ứng dụng đọc tin tức tự động cho Android Auto",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getVoiceQuality(voice: Voice): String {
    val quality = when (voice.quality) {
        Voice.QUALITY_VERY_HIGH -> "Chất lượng cao"
        Voice.QUALITY_HIGH -> "Chất lượng tốt"
        Voice.QUALITY_NORMAL -> "Chất lượng bình thường"
        else -> "Chất lượng cơ bản"
    }

    val network = if (voice.isNetworkConnectionRequired) " • Yêu cầu mạng" else " • Offline"

    return quality + network
}