package com.newsspeech.auto.data.remote

import com.newsspeech.auto.domain.model.News
import retrofit2.http.GET

interface NewsApiService {
    @GET("/news")
    suspend fun getAllNews(): List<News>
}