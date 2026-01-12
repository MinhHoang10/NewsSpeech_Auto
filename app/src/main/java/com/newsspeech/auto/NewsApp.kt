package com.newsspeech.auto

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class - Entry point của toàn bộ app
 *
 * ✅ Khởi tạo Hilt dependency injection
 * ✅ Pre-warm Compose để giảm cold start lag
 * ✅ Chạy TRƯỚC mọi Activity/Service
 * ✅ Tối ưu để không block main thread
 */
@HiltAndroidApp
class NewsApp : Application() {

    companion object {
        private const val TAG = "NewsApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 NewsApp onCreate() - App starting...")

        // ✅ Pre-warm Compose runtime trên background thread
        // KHÔNG log ở đây vì thread chưa chạy xong
//        Thread {
//            preWarmCompose()
//        }.start()

        Log.d(TAG, "✅ NewsApp initialized successfully")
    }

    /**
     * Pre-load Compose classes vào memory
     * Không bắt buộc nhưng giúp UI mượt hơn
     * ✅ Chạy trên background thread để không block onCreate()
     */
//    private fun preWarmCompose() {
//        try {
//            // Trigger class loading của các component Compose chính
//            Class.forName("androidx.compose.runtime.Composer")
//            Class.forName("androidx.compose.ui.platform.AndroidComposeView")
//            Class.forName("androidx.compose.material3.ButtonKt")
//            Class.forName("androidx.compose.foundation.layout.ColumnKt")
//            Class.forName("androidx.compose.foundation.layout.RowKt")
//
//            Log.d(TAG, "✅ Compose pre-warmed")
//        } catch (e: ClassNotFoundException) {
//            Log.w(TAG, "⚠️ Could not pre-warm Compose (not critical): ${e.message}")
//        } catch (e: Exception) {
//            Log.w(TAG, "⚠️ Error pre-warming Compose: ${e.message}")
//        }
//    }
//
//    override fun onTerminate() {
//        Log.d(TAG, "🛑 NewsApp onTerminate() - App shutting down")
//        super.onTerminate()
//    }
//
//    override fun onLowMemory() {
//        super.onLowMemory()
//        Log.w(TAG, "⚠️ onLowMemory() - System is running low on memory")
//    }
//
//    override fun onTrimMemory(level: Int) {
//        super.onTrimMemory(level)
//        Log.w(TAG, "⚠️ onTrimMemory(level=$level) - System requesting memory cleanup")
//    }
}