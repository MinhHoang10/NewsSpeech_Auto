package com.newsspeech.auto

import android.app.Application
import android.util.Log
import androidx.work.*
import com.newsspeech.auto.worker.NewsCrawlerWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

/**
 * Application class - Entry point của toàn bộ app
 *
 * ✅ Khởi tạo Hilt dependency injection
 * ✅ Chạy crawler ngay khi app start
 * ✅ Setup crawler định kỳ mỗi 60 phút
 */
@HiltAndroidApp
class NewsApp : Application() {

    companion object {
        private const val TAG = "NewsApp"
        private const val WORK_NAME = "news_crawler_periodic"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 NewsApp onCreate() - App starting...")

        // ✅ Setup crawl định kỳ
        setupPeriodicCrawl()

        // ✅ Crawl ngay lập tức khi app khởi động
        runImmediateCrawl()

        Log.d(TAG, "✅ NewsApp initialized successfully")
    }

    /**
     * Thiết lập crawl định kỳ mỗi 60 phút
     * Chỉ chạy khi có mạng và pin > 15%
     */
    private fun setupPeriodicCrawl() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)  // Yêu cầu có mạng
            .setRequiresBatteryNotLow(true)  // Chỉ chạy khi pin > 15%
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<NewsCrawlerWorker>(
            60, TimeUnit.MINUTES  // ✅ Mỗi 60 phút
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                15, TimeUnit.MINUTES  // Nếu lỗi, thử lại sau 15 phút
            )
            .addTag("news_crawler")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,  // Giữ nguyên nếu đã có
            periodicWork
        )

        Log.i(TAG, "✅ Đã thiết lập crawl định kỳ mỗi 60 phút")
    }

    /**
     * Crawl ngay lập tức khi app khởi động
     * Chạy background, không block onCreate()
     */
    fun runImmediateCrawl() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateWork = OneTimeWorkRequestBuilder<NewsCrawlerWorker>()
            .setConstraints(constraints)
            .addTag("news_crawler_immediate")
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "news_crawler_immediate",
            ExistingWorkPolicy.REPLACE,  // Thay thế nếu đang chạy
            immediateWork
        )

        Log.i(TAG, "🚀 Đã khởi chạy crawl ngay lập tức")
    }

    /**
     * Hủy tất cả crawl jobs (dùng khi cần)
     */
    fun cancelAllCrawls() {
        WorkManager.getInstance(this).cancelUniqueWork(WORK_NAME)
        WorkManager.getInstance(this).cancelAllWorkByTag("news_crawler")
        Log.i(TAG, "⏹️ Đã hủy tất cả crawl jobs")
    }
}