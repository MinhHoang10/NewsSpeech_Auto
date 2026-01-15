package com.newsspeech.auto.presentation.car.components

import androidx.car.app.model.Row
import com.newsspeech.auto.presentation.car.TtsState

/**
 * Component hiển thị TTS control bar
 */
object TtsControlBar {

    /**
     * Tạo row hiển thị trạng thái TTS
     */
    fun buildStatusRow(ttsState: TtsState): Row {
        val status = when {
            !ttsState.isReady -> "⏳ Đang khởi tạo TTS..."
            ttsState.isSpeaking -> "🔊 Đang phát (${ttsState.queueSize} tin trong queue)"
            ttsState.queueSize > 0 -> "⏸️ Có ${ttsState.queueSize} tin đang chờ"
            else -> "✅ TTS sẵn sàng - Chạm vào tin để nghe"
        }

        return Row.Builder()
            .setTitle(status)
            .setBrowsable(false)
            .build()
    }

    /**
     * Tạo row test TTS
     */
    fun buildTestRow(onTest: () -> Unit): Row {
        return Row.Builder()
            .setTitle("🔊 TEST TTS")
            .addText("Click để test giọng nói")
            .setOnClickListener(onTest)
            .build()
    }

    /**
     * Tạo row stop TTS
     */
    fun buildStopRow(onStop: () -> Unit, enabled: Boolean): Row {
        val builder = Row.Builder()
            .setTitle("⏹️ DỪNG PHÁT")
            .addText("Click để dừng phát tin")

        if (enabled) {
            builder.setOnClickListener(onStop)
        } else {
            builder.setBrowsable(false)
            builder.addText("(Không có tin đang phát)")
        }

        return builder.build()
    }
}