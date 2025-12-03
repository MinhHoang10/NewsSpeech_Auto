package com.newsspeech.auto.presentation.car

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.lifecycleScope
import com.newsspeech.auto.data.repository.NewsRepository
import com.newsspeech.auto.domain.model.News
import com.newsspeech.auto.service.NewsPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CarHomeScreen(carContext: CarContext) : Screen(carContext) {

    private val newsRepo = NewsRepository(carContext)
    private var newsList: List<News> = emptyList()  // Cache

    init {
        // Load async ngay khi init
        lifecycleScope.launch {
            newsList = loadNewsAsync()
            invalidate()  // Refresh template sau khi load done
        }
    }

    private suspend fun loadNewsAsync(): List<News> {
        return try {
            withContext(Dispatchers.IO) { newsRepo.loadNewsFromAssets() }
        } catch (e: Exception) {
            Log.e("CarHomeScreen", "Load news error: ${e.message}")
            emptyList()
        }
    }

    override fun onGetTemplate(): Template {
        return if (newsList.isEmpty()) {
            // Show loading hoặc empty/error
            ListTemplate.Builder()
                .setTitle("Đang tải tin tức...")
                .setLoading(true)  // Hoặc buildEmptyList() như cũ
                .build()
        } else {
            buildTemplate(newsList)
        }
    }

    

    private fun buildTemplate(newsList: List<com.newsspeech.auto.domain.model.News>): Template {
        return ListTemplate.Builder().apply {
            setTitle("Tin tức hôm nay")
            setHeaderAction(Action.APP_ICON)

            if (newsList.isEmpty()) {
                setSingleList(buildEmptyList())
            } else {
                setTitle("Tin tức hôm nay (${newsList.size})")
                setSingleList(buildNewsList(newsList))
            }
        }.build()
    }

    private fun buildEmptyList(): ItemList {
        return ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle("Không có tin tức")
                    .addText("Vui lòng kiểm tra dữ liệu")
                    .setBrowsable(false)
                    .build()
            )
            .build()
    }

    private fun buildNewsList(newsList: List<com.newsspeech.auto.domain.model.News>): ItemList {
        return ItemList.Builder().apply {
            newsList.forEach { news ->
                addItem(
                    Row.Builder()
                        .setTitle(news.title)
                        .addText("Nguồn: ${news.source}")
                        .setOnClickListener {
                            // Đảm bảo chạy trên main thread
                            screenManager?.let { manager ->
                                val fullText = "Tin ${news.title}. ${news.content}"
                                NewsPlayer.addToQueue(fullText)
                            }
                        }
                        .build()
                )
            }
        }.build()
    }

    private fun buildErrorTemplate(errorMessage: String): Template {
        return ListTemplate.Builder()
            .setSingleList(
                ItemList.Builder()
                    .addItem(
                        Row.Builder()
                            .setTitle("Lỗi tải tin tức")
                            .addText(errorMessage)
                            .build()
                    )
                    .build()
            )
            .build()
    }
}