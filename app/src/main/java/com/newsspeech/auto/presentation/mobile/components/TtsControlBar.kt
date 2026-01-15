package com.newsspeech.auto.presentation.mobile.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * TTS control bar shown at bottom of screen
 * Displays TTS status and stop button
 */
@Composable
fun TtsControlBar(
    isTtsReady: Boolean,
    isSpeaking: Boolean,
    queueSize: Int,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
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
            // Status section
            TtsStatus(
                isTtsReady = isTtsReady,
                isSpeaking = isSpeaking,
                queueSize = queueSize
            )

            // Stop button
            if (isSpeaking || queueSize > 0) {
                StopButton(onClick = onStop)
            }
        }
    }
}

@Composable
private fun TtsStatus(
    isTtsReady: Boolean,
    isSpeaking: Boolean,
    queueSize: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Status text
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

        // Progress indicator when speaking
        if (isSpeaking || queueSize > 0) {
            LinearProgressIndicator(
                modifier = Modifier.width(200.dp)
            )
        }
    }
}

@Composable
private fun StopButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    ) {
        Text("⏹️ Dừng")
    }
}