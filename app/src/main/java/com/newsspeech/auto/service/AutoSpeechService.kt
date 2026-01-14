package com.newsspeech.auto.service

import android.content.Intent
import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.newsspeech.auto.presentation.car.CarHomeScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Service cho Android Auto
 *
 * ✅ Tạo CoroutineScope riêng vì CarAppService không phải LifecycleOwner
 * ✅ Init TTS trên IO thread ngay khi Service tạo
 * ✅ Cancel scope khi Service destroy để tránh leak
 */
class AutoSpeechService : CarAppService() {

    private val tag = "AutoSpeechService"

    // ✅ Tạo CoroutineScope riêng cho Service
    // SupervisorJob: Nếu 1 job fail, các job khác vẫn chạy
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "🚀 AutoSpeechService onCreate()")

        // ✅ Register TTS đồng bộ
        NewsPlayer.register("AutoSpeechService")

        // ✅ Pre-init TTS trên IO thread (không block main thread)
        serviceScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            NewsPlayer.init(applicationContext) { success ->
                val elapsed = System.currentTimeMillis() - startTime
                if (success) {
                    Log.i(tag, "✅ TTS pre-init OK trong Service (${elapsed}ms)")
                } else {
                    Log.e(tag, "❌ TTS pre-init FAIL trong Service (${elapsed}ms)")
                }
            }
        }
    }

    override fun onCreateSession(): Session {
        Log.d(tag, "📱 Creating new Android Auto session")

        return object : Session() {
            override fun onCreateScreen(intent: Intent): Screen {
                Log.d(tag, "🖥️ onCreateScreen() - Tạo CarHomeScreen")

                // ✅ TTS có thể chưa sẵn sàng, nhưng Screen vẫn render được
                // CarHomeScreen sẽ hiển thị "Đang tải..." nếu TTS chưa init
                return CarHomeScreen(carContext)
            }
        }
    }

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onDestroy() {
        Log.d(tag, "🛑 AutoSpeechService onDestroy()")

        // ✅ Cancel tất cả coroutines đang chạy
        serviceScope.cancel()

        // ✅ Hủy đăng ký TTS
        NewsPlayer.unregister("AutoSpeechService")

        super.onDestroy()
        Log.d(tag, "✅ AutoSpeechService destroyed")
    }
}