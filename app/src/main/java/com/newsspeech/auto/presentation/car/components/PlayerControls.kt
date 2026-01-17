package com.newsspeech.auto.presentation.car.components

import android.content.Context
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat

/**
 * Component cho player controls
 */
object PlayerControls {

    /**
     * Build action strip voi play/pause va stop
     *
     * @param context Context (CarContext) can thiet de tao icon
     *
     * IMPORTANT: PaneTemplate chi cho phep TOI DA 2 ACTIONS trong ActionStrip
     * Nen chi giu lai Play/Pause va Stop
     * Previous/Next se duoc xu ly bang cach swipe hoac button trong UI
     */
    fun buildActionStrip(
        context: Context,
        isPlaying: Boolean,
        hasPrevious: Boolean,
        hasNext: Boolean,
        onPlayPause: () -> Unit,
        onPrevious: () -> Unit,
        onNext: () -> Unit,
        onStop: () -> Unit
    ): ActionStrip {
        return ActionStrip.Builder()
            // Play/Pause (action 1)
            .addAction(
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(
                                context,
                                if (isPlaying) {
                                    android.R.drawable.ic_media_pause
                                } else {
                                    android.R.drawable.ic_media_play
                                }
                            )
                        ).build()
                    )
                    .setOnClickListener(onPlayPause)
                    .build()
            )
            // Stop (action 2)
            .addAction(
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(
                                context,
                                android.R.drawable.ic_delete
                            )
                        ).build()
                    )
                    .setOnClickListener(onStop)
                    .build()
            )
            .build()
    }

    /**
     * Build simple progress row
     */
    fun buildProgressRow(
        currentTitle: String,
        position: Int,
        total: Int
    ): String {
        return "$currentTitle ($position/$total)"
    }
}