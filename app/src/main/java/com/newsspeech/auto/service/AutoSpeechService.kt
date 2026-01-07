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
 * ✅ Đăng ký/hủy đăng ký NewsPlayer đúng lifecycle
 * ✅ TTS chỉ shutdown khi cả Activity và Service đều thoát
 * ✅ Init TTS trên background thread để không block UI
 */
class AutoSpeechService : CarAppService() {

    private val TAG = "AutoSpeechService"

    // ✅ Coroutine scope cho Service
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 AutoSpeechService onCreate()")

        // ✅ Pre-init TTS ngay khi Service được tạo (background thread)
        serviceScope.launch(Dispatchers.IO) {
            NewsPlayer.register("AutoSpeechService")
            NewsPlayer.init(applicationContext) { success ->
                if (success) {
                    Log.i(TAG, "✅ TTS pre-init thành công trong Service")
                } else {
                    Log.e(TAG, "❌ TTS pre-init thất bại trong Service")
                }
            }
        }
    }

    override fun onCreateSession(): Session {
        Log.d(TAG, "📱 Creating new Android Auto session")

        return object : Session() {
            override fun onCreateScreen(intent: Intent): Screen {
                Log.d(TAG, "🖥️ onCreateScreen() - Tạo CarHomeScreen")

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
        Log.d(TAG, "🛑 AutoSpeechService onDestroy()")

        // ✅ Hủy coroutine scope
        serviceScope.cancel()

        // ✅ Hủy đăng ký TTS
        NewsPlayer.unregister("AutoSpeechService")

        super.onDestroy()
        Log.d(TAG, "✅ AutoSpeechService destroyed")
    }
}