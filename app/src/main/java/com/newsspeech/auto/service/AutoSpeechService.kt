package com.newsspeech.auto.service

import android.content.Intent
import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.newsspeech.auto.presentation.car.CarHomeScreen

/**
 * Service cho Android Auto
 *
 * ✅ Đăng ký/hủy đăng ký NewsPlayer đúng lifecycle
 * ✅ TTS chỉ shutdown khi cả Activity và Service đều thoát
 */
class AutoSpeechService : CarAppService() {

    private val TAG = "AutoSpeechService"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 AutoSpeechService onCreate()")
    }

    override fun onCreateSession(): Session {
        Log.d(TAG, "📱 Creating new Android Auto session")

        return object : Session() {
            override fun onCreateScreen(intent: Intent): Screen {
                Log.d(TAG, "🖥️ onCreateScreen() - Tạo CarHomeScreen")

                // ✅ Đăng ký sử dụng TTS
                NewsPlayer.register("AutoSpeechService")

                // Khởi tạo TTS (nếu chưa có)
                NewsPlayer.init(carContext) { success ->
                    if (success) {
                        Log.i(TAG, "✅ TTS init thành công trong Service")
                    } else {
                        Log.e(TAG, "❌ TTS init thất bại trong Service")
                    }
                }

                return CarHomeScreen(carContext)
            }
        }
    }

    override fun createHostValidator(): HostValidator {
        // ALLOW_ALL_HOSTS_VALIDATOR: Cho phép tất cả host (bao gồm Android Auto và DHU)
        // Production: Nên dùng danh sách host cụ thể
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onDestroy() {
        Log.d(TAG, "🛑 AutoSpeechService onDestroy()")

        // ✅ Hủy đăng ký TTS
        // TTS chỉ shutdown nếu activeUsers = 0 (cả Activity và Service đều thoát)
        NewsPlayer.unregister("AutoSpeechService")

        super.onDestroy()
        Log.d(TAG, "✅ AutoSpeechService destroyed")
    }
}