package com.newsspeech.auto.presentation.car.screens

import android.content.Context
import androidx.car.app.model.*
import com.newsspeech.auto.domain.model.News
import com.newsspeech.auto.presentation.car.components.PlayerControls
import com.newsspeech.auto.presentation.car.TtsState

/**
 * Man hinh chi tiet tin tuc - Spotify style
 *
 * Layout:
 * - Title (prominent, large)
 * - Source (metadata)
 * - Progress indicator (Tin X / Y • Status)
 * - Previous/Next actions
 * - Play/Pause + Stop in ActionStrip
 */
object NewsDetailScreen {

    fun build(
        context: Context,
        news: News,
        ttsState: TtsState,
        currentIndex: Int,
        totalNews: Int,
        onPlayPause: () -> Unit,
        onPrevious: () -> Unit,
        onNext: () -> Unit,
        onStop: () -> Unit
    ): Template {
        val paneBuilder = Pane.Builder()

        // 1. Title row (large, prominent like song title)
        paneBuilder.addRow(
            Row.Builder()
                .setTitle(news.title)
                .build()
        )

        // 2. Source row (like artist name)
        paneBuilder.addRow(
            Row.Builder()
                .setTitle(news.source)
                .build()
        )

        // 3. Progress row (like "0:34 / 3:20")
        paneBuilder.addRow(
            Row.Builder()
                .setTitle(buildProgressText(currentIndex, totalNews, ttsState))
                .build()
        )

        // 4. Previous button (if available)
        if (currentIndex > 1) {
            paneBuilder.addAction(
                Action.Builder()
                    .setTitle("⏮ Tin truoc")
                    .setOnClickListener(onPrevious)
                    .build()
            )
        }

        // 5. Next button (if available)
        if (currentIndex < totalNews) {
            paneBuilder.addAction(
                Action.Builder()
                    .setTitle("⏭ Tin tiep")
                    .setOnClickListener(onNext)
                    .build()
            )
        }

        // Build ActionStrip with Play/Pause + Stop (max 2 actions)
        val actionStrip = PlayerControls.buildActionStrip(
            context = context,
            isPlaying = ttsState.isSpeaking,
            hasPrevious = currentIndex > 1,
            hasNext = currentIndex < totalNews,
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext,
            onStop = onStop
        )

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle(news.category)
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStrip)
            .build()
    }

    /**
     * Build progress text: "Tin 1 / 10 • Dang phat"
     */
    private fun buildProgressText(
        currentIndex: Int,
        totalNews: Int,
        ttsState: TtsState
    ): String {
        val status = when {
            !ttsState.isReady -> "Cho TTS"
            ttsState.isSpeaking -> "Dang phat"
            else -> "Tam dung"
        }
        return "Tin $currentIndex / $totalNews • $status"
    }
}