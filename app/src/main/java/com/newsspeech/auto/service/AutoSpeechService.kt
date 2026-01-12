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
 * ✅ Init TTS trên background thread THẬT SỰ để không block UI
 */
class AutoSpeechService : CarAppService() {

    private val tag = "AutoSpeechService"

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "🚀 AutoSpeechService onCreate()")

        // ✅ Pre-init TTS ngay khi Service được tạo (background thread THẬT)
        // QUAN TRỌNG: Dùng Thread thay vì coroutine vì TextToSpeech
        // constructor BLOCK thread 3-8 giây
        Thread {
            NewsPlayer.register("AutoSpeechService")
            NewsPlayer.init(applicationContext) { success ->
                if (success) {
                    Log.i(tag, "✅ TTS pre-init thành công trong Service")
                } else {
                    Log.e(tag, "❌ TTS pre-init thất bại trong Service")
                }
            }
        }.start()
    }

    override fun onCreateSession(): Session {
        Log.d(tag, "📱 Creating new Android Auto session")

        return object : Session() {
            override fun onCreateScreen(intent: Intent): Screen {
                Log.d(tag, "🖥️ onCreateScreen() - Tạo CarHomeScreen")

                // ✅ Không cần register/init ở đây nữa vì đã làm trong onCreate()
                // Screen có thể render ngay, TTS sẽ sẵn sàng sau

                return CarHomeScreen(carContext)
            }
        }
    }

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onDestroy() {
        Log.d(tag, "🛑 AutoSpeechService onDestroy()")

        // ✅ Hủy đăng ký TTS
        NewsPlayer.unregister("AutoSpeechService")

        super.onDestroy()
        Log.d(tag, "✅ AutoSpeechService destroyed")
    }
}