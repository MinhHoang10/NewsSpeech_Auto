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
import kotlinx.coroutines.launch

class CarHomeScreen(carContext: CarContext) : Screen(carContext) {

    // Repo đã được viết chuẩn với Dispatchers.IO, nên gọi ở đây an toàn
    private val newsRepo = NewsRepository(carContext)

    // Cache danh sách tin để không phải load lại mỗi khi invalidate
    private var newsList: List<News> = emptyList()

    // Trạng thái loading
    private var isLoading = true

    init {
        loadData()
    }

    private fun loadData() {
        // Sử dụng lifecycleScope của Screen để tự động hủy nếu thoát màn hình
        lifecycleScope.launch {
            try {
                // Gọi hàm suspend trong Repo (nó sẽ tự nhảy sang IO thread)
                // Nên KHÔNG GÂY LAG UI
                newsList = newsRepo.loadNewsFromAssets()
            } catch (e: Exception) {
                Log.e("CarHomeScreen", "Lỗi load tin: ${e.message}")
            } finally {
                // Tắt loading và yêu cầu vẽ lại giao diện
                isLoading = false
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        // 1. Trạng thái Đang tải
        if (isLoading) {
            return ListTemplate.Builder()
                .setTitle("Đang tải tin tức...")
                .setLoading(true)
                .setHeaderAction(Action.APP_ICON)
                .build()
        }

        // 2. Trạng thái Danh sách trống (hoặc lỗi)
        if (newsList.isEmpty()) {
            val emptyRow = Row.Builder()
                .setTitle("Không có tin tức")
                .addText("Không tìm thấy dữ liệu trong assets/all_news.json")
                .build()

            return ListTemplate.Builder()
                .setTitle("Tin Tức")
                .setHeaderAction(Action.APP_ICON)
                .setSingleList(ItemList.Builder().addItem(emptyRow).build())
                .build()
        }

        // 3. Trạng thái Có dữ liệu -> Hiển thị danh sách
        return buildNewsListTemplate(newsList)
    }

    private fun buildNewsListTemplate(list: List<News>): ListTemplate {
        val itemListBuilder = ItemList.Builder()

        list.forEach { news ->
            // Tạo nội dung hàng
            val row = Row.Builder()
                .setTitle(news.title) // Tiêu đề tin

            // Kiểm tra null safety cho các trường khác (nếu model có nullable)
            val desc = if (!news.content.isNullOrEmpty()) news.content else "Chạm để nghe chi tiết"
            row.addText(desc)

            // Xử lý sự kiện click
            row.setOnClickListener {
                // Logic đọc tin
                val contentToRead = "Tin: ${news.title}. ${news.content ?: ""}"
                NewsPlayer.addToQueue(contentToRead)

                // (Tùy chọn) Hiện thông báo nhỏ trên xe
                CarToast.makeText(carContext, "Đang phát...", CarToast.LENGTH_SHORT).show()
            }

            itemListBuilder.addItem(row.build())
        }

        return ListTemplate.Builder()
            .setTitle("Tin Tức Hôm Nay (${list.size})")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(itemListBuilder.build())
            .build()
    }
}