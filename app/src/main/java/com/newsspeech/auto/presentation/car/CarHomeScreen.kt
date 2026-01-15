package com.newsspeech.auto.presentation.car

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.lifecycleScope
import com.newsspeech.auto.data.repository.NewsRepository
import com.newsspeech.auto.domain.model.News
import com.newsspeech.auto.service.NewsPlayer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Màn hình chính trên Android Auto
 *
 * ✅ Load dữ liệu bất đồng bộ không block UI
 * ✅ Cache dữ liệu để không load lại khi invalidate()
 * ✅ Observe TTS state như MobileActivity
 */
class CarHomeScreen(carContext: CarContext) : Screen(carContext) {

    private val tag = "CarHomeScreen"

    // Repository đã dùng Dispatchers.IO
    private val newsRepo = NewsRepository(carContext)

    // Cache danh sách tin
    private var newsList: List<News> = emptyList()

    // Trạng thái loading
    private var isLoading = true

    // ✅ TTS State (observe từ StateFlow)
    private var isTtsReady = false
    private var isSpeaking = false
    private var queueSize = 0

    init {
        Log.d(tag, "🖥️ CarHomeScreen initialized")
        loadData()
        observeTtsState()  // ✅ Quan sát TTS state
    }

    /**
     * ✅ Observe TTS state giống MobileActivity
     */
    private fun observeTtsState() {
        // Observe readyState
        lifecycleScope.launch {
            NewsPlayer.readyState.collectLatest { ready ->
                val changed = isTtsReady != ready
                isTtsReady = ready
                Log.d(tag, "🎤 TTS ready: $ready")
                if (changed && !isLoading) {
                    invalidate()  // Re-render khi state thay đổi
                }
            }
        }

        // Observe speaking state
        lifecycleScope.launch {
            NewsPlayer.currentlySpeaking.collectLatest { speaking ->
                val changed = isSpeaking != speaking
                isSpeaking = speaking
                Log.d(tag, "🔊 TTS speaking: $speaking")
                if (changed && !isLoading) {
                    invalidate()
                }
            }
        }

        // Observe queue size
        lifecycleScope.launch {
            NewsPlayer.queueSize.collectLatest { size ->
                val changed = queueSize != size
                queueSize = size
                Log.d(tag, "📋 Queue size: $size")
                if (changed && !isLoading) {
                    invalidate()
                }
            }
        }
    }

    /**
     * Load dữ liệu từ assets
     * Chạy trong coroutine để không block UI
     */
    private fun loadData() {
        lifecycleScope.launch {
            try {
                Log.d(tag, "📥 Bắt đầu load tin tức...")

                // Gọi suspend function (tự động chạy trên IO thread)
                newsList = newsRepo.loadNewsFromAssets()

                Log.i(tag, "✅ Load thành công ${newsList.size} tin")
            } catch (e: Exception) {
                Log.e(tag, "❌ Lỗi khi load tin: ${e.message}", e)
                newsList = emptyList()
            } finally {
                isLoading = false
                invalidate() // Yêu cầu re-render
            }
        }
    }

    /**
     * Build template để hiển thị trên xe
     * Được gọi mỗi khi invalidate()
     */
    override fun onGetTemplate(): Template {
        Log.d(tag, "🎨 onGetTemplate() - loading:$isLoading, news:${newsList.size}, ttsReady:$isTtsReady, speaking:$isSpeaking, queue:$queueSize")

        // Case 1: Đang loading
        if (isLoading) {
            return ListTemplate.Builder()
                .setTitle("Đang tải tin tức...")
                .setLoading(true)
                .setHeaderAction(Action.APP_ICON)
                .build()
        }

        // Case 2: Danh sách rỗng (lỗi hoặc không có dữ liệu)
        if (newsList.isEmpty()) {
            return buildEmptyTemplate()
        }

        // Case 3: Có dữ liệu → Hiển thị danh sách
        return buildNewsListTemplate(newsList)
    }

    /**
     * Template khi không có dữ liệu
     */
    private fun buildEmptyTemplate(): ListTemplate {
        val emptyRow = Row.Builder()
            .setTitle("⚠️ Không có tin tức")
            .addText("Không tìm thấy dữ liệu trong assets/all_news.json")
            .addText("Vui lòng kiểm tra file và khởi động lại app")
            .build()

        return ListTemplate.Builder()
            .setTitle("Tin Tức")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(
                ItemList.Builder()
                    .addItem(emptyRow)
                    .build()
            )
            .build()
    }

    /**
     * Template hiển thị danh sách tin
     */
    private fun buildNewsListTemplate(list: List<News>): ListTemplate {
        val itemListBuilder = ItemList.Builder()

        // ✅ Thêm TTS status row ở đầu
        val ttsStatusRow = Row.Builder()
            .setTitle(
                when {
                    !isTtsReady -> "⏳ Đang khởi tạo TTS..."
                    isSpeaking -> "🔊 Đang phát ($queueSize tin trong queue)"
                    queueSize > 0 -> "⏸️ Có $queueSize tin đang chờ"
                    else -> "✅ TTS sẵn sàng - Chạm vào tin để nghe"
                }
            )
            .setBrowsable(false)  // Không cho click vào row này
            .build()

        itemListBuilder.addItem(ttsStatusRow)

        // Thêm vào buildNewsListTemplate() sau ttsStatusRow
        val testRow = Row.Builder()
            .setTitle(" TEST TTS")
            .addText("Click để test giọng nói")
            .setOnClickListener {
                NewsPlayer.addToQueue("Đây là test TTS trên Android Auto")
                CarToast.makeText(carContext, "Test TTS", CarToast.LENGTH_SHORT).show()
            }
            .build()

        itemListBuilder.addItem(testRow)

        // Thêm các tin tức
        list.forEachIndexed { index, news ->
            val row = Row.Builder()
                .setTitle(news.title)

            // Hiển thị description
            val description = when {
                news.content.isNotEmpty() -> {
                    if (news.content.length > 100) {
                        news.content.take(100) + "..."
                    } else {
                        news.content
                    }
                }
                else -> "Chạm để nghe chi tiết"
            }
            row.addText(description)

            // Hiển thị metadata (nguồn và thời gian)
            if (news.source.isNotEmpty() || news.timestamp.isNotEmpty()) {
                val metadata = buildString {
                    if (news.source.isNotEmpty()) {
                        append(news.source)
                    }
                    if (news.timestamp.isNotEmpty()) {
                        if (isNotEmpty()) append(" • ")
                        append(formatTimestamp(news.timestamp))
                    }
                }
                row.addText(metadata)
            }

            // ✅ Disable row nếu TTS chưa ready
            if (isTtsReady) {
                row.setOnClickListener {
                    handleNewsClick(news)
                }
            } else {
                row.addText("⏳ TTS đang khởi tạo...")
                row.setBrowsable(false)
            }

            itemListBuilder.addItem(row.build())
        }

        return ListTemplate.Builder()
            .setTitle("Tin Tức Hôm Nay (${list.size})")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(itemListBuilder.build())
            .build()
    }

    /**
     * Xử lý khi user click vào 1 tin
     */
    private fun handleNewsClick(news: News) {
        Log.d(tag, "👆 User clicked: ${news.title}")

        // ✅ Kiểm tra TTS state từ observed value
        if (!isTtsReady) {
            Log.w(tag, "⚠️ TTS chưa sẵn sàng")
            CarToast.makeText(
                carContext,
                "Đang khởi tạo TTS, vui lòng thử lại",
                CarToast.LENGTH_SHORT
            ).show()
            return
        }

        // Tạo nội dung đọc giống MobileActivity
        val contentToRead = buildString {
            append("Tin từ ")
            if (news.source.isNotEmpty()) {
                append(news.source)
                append(". ")
            }

            append(news.title)
            append(". ")

            if (news.content.isNotEmpty()) {
                append(news.content)
            }
        }

        // ✅ Thêm vào queue trong coroutine như MobileActivity
        lifecycleScope.launch {
            NewsPlayer.addToQueue(contentToRead)

            // Hiển thị thông báo
            CarToast.makeText(
                carContext,
                "🔊 Đang phát...",
                CarToast.LENGTH_SHORT
            ).show()

            Log.i(tag, "✅ Đã thêm tin vào queue")
        }
    }

    /**
     * Format timestamp giống MobileActivity
     */
    private fun formatTimestamp(timestamp: String): String {
        return try {
            when {
                timestamp.contains("T") -> {
                    val parts = timestamp.split("T")
                    val date = parts[0].split("-")
                    val time = parts.getOrNull(1)?.split(":")

                    if (date.size >= 3 && time != null && time.size >= 2) {
                        "${date[2]}/${date[1]} ${time[0]}:${time[1]}"
                    } else {
                        timestamp
                    }
                }
                else -> timestamp
            }
        } catch (e: Exception) {
            timestamp
        }
    }
}