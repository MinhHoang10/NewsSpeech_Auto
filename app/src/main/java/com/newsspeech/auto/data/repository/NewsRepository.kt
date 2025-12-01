package com.newsspeech.auto.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.newsspeech.auto.domain.model.News

class NewsRepository(private val context: Context) {

    private val gson = Gson()

    fun getNewsGroupedByCategory(): Map<String, List<News>> {
        val newsList = loadNewsFromAssets()
        return newsList.groupBy { it.category }
    }

    fun loadNewsFromAssets(): List<News> {
        return try {
            val jsonString = context.assets.open("all_news.json")
                .bufferedReader()
                .use { it.readText() }

            val type = object : TypeToken<List<News>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}