package com.newsspeech.auto.data.repository

import android.content.Context
import android.util.Log
import com.newsspeech.auto.database.NewsDatabase
import com.newsspeech.auto.domain.model.News
import com.newsspeech.auto.model.toNews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository để load tin tức
 * ✅ Đọc từ Room Database (tin đã crawl)
 * ✅ Fallback sang assets/all_news.json nếu database trống
 */
class NewsRepository(private val context: Context) {

    private val tag = "NewsRepository"
    private val database = NewsDatabase.getDatabase(context)

    /**
     * Load danh sách tin tức từ Database
     * Nếu database trống, fallback sang JSON
     */
    suspend fun loadNews(): List<News> = withContext(Dispatchers.IO) {
        try {
            // 🔥 DEBUG: Kiểm tra database path
            val dbPath = context.getDatabasePath("news_database").absolutePath
            Log.d(tag, "🔍 Database path: $dbPath")

            val count = database.newsDao().getCount()
            Log.d(tag, "🔍 Database count: $count")

            if (count > 0) {
                Log.i(tag, "📂 Loading $count news from Database...")

                val articles = database.newsDao().getAllNews().first()
                Log.d(tag, "🔍 Retrieved ${articles.size} articles from DAO")

                // 🔥 LOG CHI TIẾT 3 BÀI ĐẦU
                articles.take(3).forEachIndexed { index, article ->
                    Log.d(tag, """
                        📰 Article $index:
                           ID: ${article.id}
                           Title: ${article.title.take(50)}...
                           Source: ${article.source}
                           Category: ${article.category}
                           Timestamp: ${article.timestamp}
                           Has Image: ${article.image != null}
                    """.trimIndent())
                }

                val newsList = articles.map { it.toNews() }
                Log.d(tag, "🔍 Mapped to ${newsList.size} News objects")

                // 🔥 VERIFY MAPPING
                newsList.take(3).forEachIndexed { index, news ->
                    Log.d(tag, """
                        ✅ News $index:
                           ID: ${news.id}
                           Title: ${news.title.take(50)}...
                           Source: ${news.source}
                           Category: ${news.category}
                    """.trimIndent())
                }

                Log.i(tag, "✅ Loaded ${newsList.size} news from Database")
                return@withContext newsList
            } else {
                Log.w(tag, "⚠️ Database empty (count=0), loading from assets/all_news.json...")
                return@withContext loadNewsFromAssets()
            }

        } catch (e: Exception) {
            Log.e(tag, "❌ Error loading from database", e)
            e.printStackTrace()
            Log.e(tag, "Fallback to JSON...")
            return@withContext loadNewsFromAssets()
        }
    }

    /**
     * Load tin tức theo Flow (realtime updates)
     */
    fun getNewsFlow(): Flow<List<News>> {
        Log.d(tag, "📡 Setting up Flow listener...")
        return database.newsDao().getAllNews().map { articles ->
            Log.d(tag, "📡 Flow emitted: ${articles.size} articles")
            articles.map { it.toNews() }
        }
    }

    /**
     * Load theo category
     */
    fun getNewsByCategory(category: String): Flow<List<News>> {
        return database.newsDao().getNewsByCategory(category).map { articles ->
            articles.map { it.toNews() }
        }
    }

    /**
     * Fallback: Load từ assets/all_news.json
     */
    suspend fun loadNewsFromAssets(): List<News> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "📂 Loading news from assets/all_news.json...")

            val jsonString = context.assets.open("all_news.json").bufferedReader().use {
                it.readText()
            }

            val newsType = object : com.google.gson.reflect.TypeToken<List<News>>() {}.type
            val newsList: List<News> = com.google.gson.Gson().fromJson(jsonString, newsType)

            Log.i(tag, "✅ Loaded ${newsList.size} news items from JSON")

            // 🔥 LOG CHI TIẾT 3 BÀI ĐẦU TỪ JSON
            newsList.take(3).forEachIndexed { index, news ->
                Log.d(tag, """
                    📄 JSON News $index:
                       Title: ${news.title.take(50)}...
                       Source: ${news.source}
                       Category: ${news.category}
                """.trimIndent())
            }

            return@withContext newsList

        } catch (e: Exception) {
            Log.e(tag, "❌ Error reading all_news.json from assets", e)
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    /**
     * Load tin tức và nhóm theo category
     */
    suspend fun getNewsGroupedByCategory(): Map<String, List<News>> = withContext(Dispatchers.IO) {
        val newsList = loadNews()
        return@withContext newsList.groupBy { it.category }
    }

    /**
     * Đếm số lượng tin trong database
     */
    suspend fun getNewsCount(): Int = withContext(Dispatchers.IO) {
        val count = database.newsDao().getCount()
        Log.d(tag, "🔍 getNewsCount() = $count")
        return@withContext count
    }

    /**
     * Xóa toàn bộ database (để debug)
     */
    suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        Log.d(tag, "🗑️ Clearing database...")
        database.newsDao().deleteAll()
        val newCount = database.newsDao().getCount()
        Log.d(tag, "✅ Database cleared. New count: $newCount")
    }

    /**
     * Debug: Kiểm tra database có data không
     */
    suspend fun debugDatabaseInfo() = withContext(Dispatchers.IO) {
        try {
            val count = database.newsDao().getCount()
            Log.d(tag, "=" .repeat(50))
            Log.d(tag, "🔍 DATABASE DEBUG INFO")
            Log.d(tag, "=" .repeat(50))
            Log.d(tag, "Total articles: $count")

            if (count > 0) {
                val allArticles = database.newsDao().getAllNews().first()

                // Group by source
                val bySource = allArticles.groupBy { it.source }
                bySource.forEach { (source, articles) ->
                    Log.d(tag, "Source '$source': ${articles.size} articles")
                }

                // Group by category
                val byCategory = allArticles.groupBy { it.category }
                byCategory.forEach { (category, articles) ->
                    Log.d(tag, "Category '$category': ${articles.size} articles")
                }

                // Show first 3
                Log.d(tag, "-" .repeat(50))
                Log.d(tag, "First 3 articles:")
                allArticles.take(3).forEach { article ->
                    Log.d(tag, "  - [${article.source}] ${article.title.take(60)}...")
                }
            } else {
                Log.d(tag, "⚠️ Database is EMPTY!")
            }
            Log.d(tag, "=" .repeat(50))
        } catch (e: Exception) {
            Log.e(tag, "❌ Error in debugDatabaseInfo", e)
        }
    }
}