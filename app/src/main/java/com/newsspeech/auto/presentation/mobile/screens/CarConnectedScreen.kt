package com.newsspeech.auto.presentation.mobile.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Screen shown when app is connected to Android Auto
 * Simple info screen telling user to use car display
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
        }
    }
}