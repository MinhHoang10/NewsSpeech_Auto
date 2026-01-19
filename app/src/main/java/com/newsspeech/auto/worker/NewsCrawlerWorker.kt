package com.newsspeech.auto.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.newsspeech.auto.crawler.OtofunCrawler
import com.newsspeech.auto.crawler.VnExpressCrawler
import com.newsspeech.auto.database.NewsDatabase
import kotlinx.coroutines.flow.first

/**
 * WorkManager Worker để crawl tin tức
 * Chạy background, tự động retry nếu fail
 */
class NewsCrawlerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "NewsCrawlerWorker"

        // Categories giống Python script
        private val VN_CATEGORIES = listOf(
            "thoi-su", "kinh-doanh", "giai-tri", "the-thao",
            "phap-luat", "giao-duc", "suc-khoe", "doi-song",
            "du-lich", "khoa-hoc", "so-hoa", "oto-xe-may"
        )

        private val OF_CATEGORIES = listOf(
            "oto-xe-may", "kinh-doanh", "du-lich", "doi-song"
        )

        private const val LIMIT_VN = 30
        private const val LIMIT_OF = 10
    }

    private val database = NewsDatabase.getDatabase(applicationContext)
    private val vnCrawler = VnExpressCrawler()
    private val ofCrawler = OtofunCrawler()

    override suspend fun doWork(): Result {
        return try {
            val startTime = System.currentTimeMillis()
            Log.i(TAG, "=" .repeat(60))
            Log.i(TAG, "🕷️ [Worker] BẮT ĐẦU CRAWL...")
            Log.i(TAG, "=" .repeat(60))

            // 🔥 DEBUG: Kiểm tra database trước khi crawl
            val countBefore = database.newsDao().getCount()
            Log.d(TAG, "🔍 Count BEFORE crawl: $countBefore")

            val allNews = mutableListOf<com.newsspeech.auto.model.NewsArticle>()

            // 1. Crawl VnExpress
            Log.d(TAG, "")
            Log.d(TAG, "--- 1. CRAWLING VNEXPRESS ---")
            for (category in VN_CATEGORIES) {
                try {
                    Log.d(TAG, "   ⏳ Crawling $category...")
                    val news = vnCrawler.crawl(category, LIMIT_VN)
                    allNews.addAll(news)
                    Log.d(TAG, "   ✅ $category: ${news.size} bài")

                    // 🔥 SAVE INCREMENTALLY (Realtime)
                    if (news.isNotEmpty()) {
                        database.newsDao().insertAll(news)
                        val currentCount = database.newsDao().getCount()
                        Log.d(TAG, "   📊 Database now has: $currentCount articles")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "   ❌ Lỗi $category: ${e.message}", e)
                }
            }

            // 2. Crawl Otofun
            Log.d(TAG, "")
            Log.d(TAG, "--- 2. CRAWLING OTOFUN ---")
            for (category in OF_CATEGORIES) {
                try {
                    Log.d(TAG, "   ⏳ Crawling $category...")
                    val news = ofCrawler.crawl(category, LIMIT_OF)
                    allNews.addAll(news)
                    Log.d(TAG, "   ✅ $category: ${news.size} bài")

                    // 🔥 SAVE INCREMENTALLY (Realtime)
                    if (news.isNotEmpty()) {
                        database.newsDao().insertAll(news)
                        val currentCount = database.newsDao().getCount()
                        Log.d(TAG, "   📊 Database now has: $currentCount articles")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "   ❌ Lỗi $category: ${e.message}", e)
                }
            }

            // 3. Verify Database
            Log.d(TAG, "")
            Log.d(TAG, "--- 3. VERIFY DATABASE ---")
            val countAfter = database.newsDao().getCount()
            Log.d(TAG, "🔍 Count AFTER crawl: $countAfter")

            if (countAfter > 0) {
                val savedArticles = database.newsDao().getAllNews().first()
                Log.d(TAG, "✅ Retrieved ${savedArticles.size} articles from database")

                // Show first 3
                savedArticles.take(3).forEachIndexed { index, article ->
                    Log.d(TAG, """
                        📰 Saved Article $index:
                           ID: ${article.id}
                           Title: ${article.title.take(60)}...
                           Source: ${article.source}
                           Category: ${article.category}
                    """.trimIndent())
                }

                // Group by source
                val bySource = savedArticles.groupBy { it.source }
                Log.d(TAG, "")
                Log.d(TAG, "📊 Summary by Source:")
                bySource.forEach { (source, articles) ->
                    Log.d(TAG, "   $source: ${articles.size} articles")
                }

                // Group by category
                val byCategory = savedArticles.groupBy { it.category }
                Log.d(TAG, "")
                Log.d(TAG, "📊 Summary by Category:")
                byCategory.forEach { (category, articles) ->
                    Log.d(TAG, "   $category: ${articles.size} articles")
                }
            } else {
                Log.e(TAG, "❌ Database is EMPTY after crawl!")
            }

            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
            Log.i(TAG, "")
            Log.i(TAG, "=" .repeat(60))
            Log.i(TAG, "✅ [Worker] HOÀN THÀNH: ${allNews.size} bài trong ${elapsed}s")
            Log.i(TAG, "=" .repeat(60))

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Worker] Lỗi: ${e.message}", e)
            e.printStackTrace()
            Result.retry()
        }
    }
}