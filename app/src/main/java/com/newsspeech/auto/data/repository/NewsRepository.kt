package com.newsspeech.auto.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.newsspeech.auto.domain.model.News
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Repository để load tin tức từ file JSON trong assets
 */
class NewsRepository(private val context: Context) {

    private val gson = Gson()
    private val tag = "NewsRepository"

    /**
     * Load danh sách tin tức từ file all_news.json trong assets
     * @return List<News> - danh sách tin tức
     */
    suspend fun loadNewsFromAssets(): List<News> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "📂 Loading news from assets/all_news.json...")

            // Đọc file JSON từ assets
            val jsonString = context.assets.open("all_news.json").bufferedReader().use {
                it.readText()
            }

            // Parse JSON thành List<News>
            val newsType = object : TypeToken<List<News>>() {}.type
            val newsList: List<News> = gson.fromJson(jsonString, newsType)

            Log.i(tag, "✅ Loaded ${newsList.size} news items from JSON")

            newsList

        } catch (e: IOException) {
            Log.e(tag, "❌ Error reading all_news.json from assets", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(tag, "❌ Error parsing JSON", e)
            emptyList()
        }
    }

    /**
     * Load tin tức và nhóm theo category
     * @return Map<String, List<News>> - Map với key là category
     */
    suspend fun getNewsGroupedByCategory(): Map<String, List<News>> = withContext(Dispatchers.IO) {
        val newsList = loadNewsFromAssets()
        newsList.groupBy { it.category }
    }
}