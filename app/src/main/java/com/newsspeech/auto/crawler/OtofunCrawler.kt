package com.newsspeech.auto.crawler

import android.util.Log
import com.newsspeech.auto.model.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*

/**
 * Crawler cho Otofun.net
 * Lấy threads từ forum
 */
class OtofunCrawler {

    companion object {
        private const val TAG = "OtofunCrawler"
        private const val TIMEOUT = 15000

        private val CATEGORY_MAP = mapOf(
            "oto-xe-may" to "https://www.otofun.net/forums/oto-xe-may.2/",
            "kinh-doanh" to "https://www.otofun.net/forums/tttm-xe-co.292/",
            "du-lich" to "https://www.otofun.net/forums/cac-chuyen-di.24/",
            "doi-song" to "https://www.otofun.net/forums/cafe-otofun.16/"
        )
    }

    suspend fun crawl(category: String, limit: Int = 10): List<NewsArticle> = withContext(Dispatchers.IO) {
        val url = CATEGORY_MAP[category] ?: CATEGORY_MAP["doi-song"]!!

        Log.d(TAG, "🕷️ Bắt đầu crawl Otofun '$category'...")

        val newsList = mutableListOf<NewsArticle>()
        val seenLinks = mutableSetOf<String>()

        try {
            var currentPage = 1

            while (newsList.size < limit && currentPage <= 3) {
                val pageUrl = if (currentPage == 1) url else "${url}page-$currentPage"

                val doc = Jsoup.connect(pageUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT)
                    .get()

                val threads = doc.select("div.structItem-title")

                if (threads.isEmpty()) break

                for (item in threads) {
                    if (newsList.size >= limit) break

                    val aTag = item.select("a[href*=/threads/]").first() ?: continue
                    val title = aTag.text().trim()
                    val link = "https://www.otofun.net" + aTag.attr("href")

                    if (link in seenLinks) continue
                    seenLinks.add(link)

                    val articleId = extractIdFromLink(link)

                    // ✅ VÀO CHI TIẾT LẤY NỘI DUNG
                    val (content, imageUrl) = getThreadContent(link)

                    newsList.add(
                        NewsArticle(
                            id = articleId,
                            title = title,
                            content = content,
                            image = imageUrl,
                            link = link,
                            timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
                            source = "Otofun",
                            category = category
                        )
                    )

                    Log.d(TAG, "   ✅ Đã lấy: ${title.take(40)}... (${content.length} chars)")
                }

                currentPage++
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi khi crawl Otofun: ${e.message}")
        }

        Log.d(TAG, "✅ Crawl Otofun '$category': ${newsList.size} bài")
        return@withContext newsList
    }

    /**
     * Vào thread để lấy nội dung và ảnh
     */
    private fun getThreadContent(url: String): Pair<String, String?> {
        return try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(TIMEOUT)
                .get()

            val firstPost = doc.select("article.message--post").first()

            if (firstPost != null) {
                val body = firstPost.select("div.bbWrapper").first()

                if (body != null) {
                    // Lấy ảnh
                    val imgTag = body.select("img.bbImage, img").first()
                    var imageUrl: String? = null
                    if (imgTag != null) {
                        imageUrl = imgTag.attr("src")
                        if (!imageUrl.startsWith("http")) {
                            imageUrl = "https://www.otofun.net$imageUrl"
                        }
                    }

                    // Lấy toàn bộ text
                    val content = body.text()

                    return Pair(content, imageUrl)
                }
            }

            Pair("Xem chi tiết tại diễn đàn.", null)
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Lỗi lấy nội dung thread: ${e.message}")
            Pair("Lỗi tải nội dung.", null)
        }
    }

    private fun extractIdFromLink(link: String): String {
        val regex = Regex("""\\.(\d+)/?$""")
        return regex.find(link)?.groupValues?.get(1)
            ?: "otf_${link.hashCode().toString().takeLast(8)}"
    }
}