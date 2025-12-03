package com.newsspeech.auto.service

import android.content.Intent
import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.newsspeech.auto.presentation.car.CarHomeScreen

class AutoSpeechService : CarAppService() {

    override fun onCreateSession(): Session {
        return object : Session() {
            override fun onCreateScreen(intent: Intent): Screen {
                // Khởi tạo NewsPlayer ở đây để đảm bảo context sẵn sàng
                NewsPlayer.init(carContext) { success ->
                if (!success) Log.e("AutoSpeechService", "TTS init failed")
                }
                return CarHomeScreen(carContext)
            }
        }
    }

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onDestroy() {
        NewsPlayer.shutdown()
        super.onDestroy()
    }
}