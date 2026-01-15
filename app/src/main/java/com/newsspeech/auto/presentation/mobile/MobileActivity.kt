package com.newsspeech.auto.presentation.mobile

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.car.app.connection.CarConnection
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.newsspeech.auto.presentation.mobile.screens.CarConnectedScreen
import com.newsspeech.auto.presentation.mobile.screens.NewsListScreen
import com.newsspeech.auto.service.NewsPlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main Activity for Mobile UI
 * Responsibilities:
 * - TTS lifecycle management
 * - CarConnection setup
 * - Screen routing (Mobile vs Car mode)
 */
@AndroidEntryPoint
class MobileActivity : ComponentActivity() {

    private val tag = "MobileActivity"
    private lateinit var carConnection: CarConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startTime = System.currentTimeMillis()
        Log.d(tag, "🚀 MobileActivity onCreate()")

        // Register TTS
        NewsPlayer.register("MobileActivity")

        // Setup CarConnection
        carConnection = CarConnection(applicationContext)

        // Set content
        setContent {
            MaterialTheme {
                MobileApp(carConnection = carConnection)
            }
        }

        val elapsedSetContent = System.currentTimeMillis() - startTime
        Log.d(tag, "⏱️ setContent() completed in ${elapsedSetContent}ms")

        // Initialize TTS in background
        initializeTts()

        val elapsedTotal = System.currentTimeMillis() - startTime
        Log.d(tag, "✅ onCreate() completed in ${elapsedTotal}ms")
    }

    private fun initializeTts() {
        lifecycleScope.launch {
            delay(100) // Wait for UI to render

            withContext(Dispatchers.IO) {
                val ttsStartTime = System.currentTimeMillis()

                NewsPlayer.init(applicationContext) { success ->
                    val elapsed = System.currentTimeMillis() - ttsStartTime
                    runOnUiThread {
                        if (success) {
                            Log.i(tag, "✅ TTS init OK (${elapsed}ms)")
                        } else {
                            Log.e(tag, "❌ TTS init FAIL (${elapsed}ms)")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(tag, "🛑 MobileActivity onDestroy()")
        NewsPlayer.unregister("MobileActivity")
        super.onDestroy()
    }
}

/**
 * Root composable - handles screen routing based on connection type
 */
@Composable
private fun MobileApp(carConnection: CarConnection) {
    var connectionType by remember {
        mutableIntStateOf(CarConnection.CONNECTION_TYPE_NOT_CONNECTED)
    }

    // Observe car connection state
    DisposableEffect(carConnection) {
        val observer = Observer<Int> { type ->
            connectionType = type
        }
        carConnection.type.observeForever(observer)

        onDispose {
            carConnection.type.removeObserver(observer)
        }
    }

    // Route to appropriate screen
    if (connectionType == CarConnection.CONNECTION_TYPE_PROJECTION) {
        CarConnectedScreen()
    } else {
        NewsListScreen()
    }
}