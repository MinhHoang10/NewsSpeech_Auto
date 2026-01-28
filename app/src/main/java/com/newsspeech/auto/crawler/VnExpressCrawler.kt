package com.newsspeech.auto.crawler

import android.util.Log
import com.newsspeech.auto.model.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*

/**
 * Crawler cho VnExpress.net
 * Lấy tin từ RSS feed và crawl full content
 */
class VnExpressCrawler {

    companion object {
        private const val TAG = "VnExpressCrawler"
        private const val TIMEOUT = 15000

        private val RSS_MAP = mapOf(
            "thoi-su" to "thoi-su",
            "kinh-doanh" to "kinh-doanh",
            "giai-tri" to "giai-tri",
            "the-thao" to "the-thao",
            "phap-luat" to "phap-luat",
            "giao-duc" to "giao-duc",
            "suc-khoe" to "suc-khoe",
            "doi-song" to "doi-song",
            "du-lich" to "du-lich",
            "khoa-hoc" to "khoa-hoc",
            "so-hoa" to "so-hoa",
            "oto-xe-may" to "oto-xe-may"
        )
    }

    /**
     * Crawl tin tức theo category
     */
    suspend fun crawl(category: String, limit: Int = 30): List<NewsArticle> = withContext(Dispatchers.IO) {
        val slug = RSS_MAP[category] ?: "thoi-su"
        val rssUrl = "https://vnexpress.net/rss/$slug.rss"

        Log.d(TAG, "🕷️ Bắt đầu crawl VnExpress '$slug'...")

        val newsList = mutableListOf<NewsArticle>()
        val seenLinks = mutableSetOf<String>()

        try {
            val doc = Jsoup.connect(rssUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(TIMEOUT)
                .get()

            val items = doc.select("item")

            for (item in items) {
                if (newsList.size >= limit) break

                val link = item.select("link").text().trim()
                val title = item.select("title").text().trim()

                // Bỏ qua video và link trùng
                if (link in seenLinks || "video" in link.lowercase()) continue
                seenLinks.add(link)

                // Lấy ảnh từ description
                val description = item.select("description").text()
                val imageUrl = extractImageFromDescription(description)

                // Lấy thời gian
                val pubDate = item.select("pubDate").text()
                val timestamp = parseRssDate(pubDate)

                // Lấy ID từ link
                val articleId = extractIdFromLink(link)

                // ✅ VÀO CHI TIẾT LẤY FULL CONTENT
                val fullContent = getFullArticleContent(link)

                newsList.add(
                    NewsArticle(
                        id = articleId,
                        title = title,
                        content = fullContent,
                        image = imageUrl,
                        link = link,
                        timestamp = timestamp,
                        source = "VnExpress",
                        category = slug
                    )
                )

                Log.d(TAG, "   ✅ Đã lấy: ${title.take(40)}... (${fullContent.length} chars)")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi khi crawl VnExpress '$slug': ${e.message}")
        }

        Log.d(TAG, "✅ Crawl VnExpress '$slug': ${newsList.size} bài")
        return@withContext newsList
    }

    /**
     * Vào chi tiết bài báo để lấy full content
     */
    private fun getFullArticleContent(url: String): String {
        return try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(TIMEOUT)
                .get()

            // Lấy nội dung từ article.fck_detail
            val contentBlock = doc.select("article.fck_detail").first()
                ?: doc.select("div.fck_detail").first()

            if (contentBlock != null) {
                // Lấy tất cả các đoạn p.Normal
                val paragraphs = contentBlock.select("p.Normal")
                paragraphs.joinToString("\n\n") { it.text().trim() }
            } else {
                // Fallback: lấy description
                doc.select("meta[name=description]").attr("content")
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Lỗi lấy full content: ${e.message}")
            ""
        }
    }

    private fun extractImageFromDescription(html: String): String? {
        return try {
            val doc = Jsoup.parse(html)
            doc.select("img").first()?.attr("src")
        } catch (e: Exception) {
            null
        }
    }

    private fun extractIdFromLink(link: String): String {
        val regex = Regex("""(\d+)(?:\.html)?$""")
        return regex.find(link)?.groupValues?.get(1)
            ?: "vne_${link.hashCode().toString().takeLast(8)}"
    }

    private fun parseRssDate(dateStr: String): String {
        return try {
            val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            val date = format.parse(dateStr)
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(date!!)
        } catch (e: Exception) {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
        }
    }
}