package com.newsspeech.auto.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.newsspeech.auto.domain.model.News
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class NewsRepository(private val context: Context) {

    private val gson = Gson()

    suspend fun getNewsGroupedByCategory(): Map<String, List<News>> = withContext(Dispatchers.IO) {
        val newsList = loadNewsFromAssets()
        newsList.groupBy { it.category }
    }

    suspend fun loadNewsFromAssets(): List<News> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("all_news.json")
                .bufferedReader()
                .use { it.readText() }

            val type = object : TypeToken<List<News>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}