package com.newsspeech.auto.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

object NewsPlayer : TextToSpeech.OnInitListener {

    private const val TAG = "NewsPlayer"
    private var tts: TextToSpeech? = null
    private var isReady = false

    // Sử dụng ConcurrentLinkedQueue để an toàn đa luồng
    private val queue = ConcurrentLinkedQueue<String>()

    // Callback để thông báo kết quả khởi tạo cho Activity/Service
    private var initCallback: ((Boolean) -> Unit)? = null

    // Biến cờ để kiểm tra xem có đang phát hay không
    private var isSpeaking = false

    fun init(context: Context, callback: ((Boolean) -> Unit)? = null) {
        if (isReady && tts != null) {
            callback?.invoke(true)
            return
        }

        initCallback = callback

        // CHÚ Ý: Sử dụng applicationContext để tránh leak Activity/Service Context
        // Không bọc trong Coroutine vì Constructor của TTS cần Looper của Main Thread (thường là vậy)
        // Việc khởi tạo này là bất đồng bộ (kết quả trả về ở onInit), nên sẽ không chặn UI 3-4s như log cũ.
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("vi", "VN"))

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Ngôn ngữ Tiếng Việt không được hỗ trợ hoặc thiếu dữ liệu!")
                isReady = false
                initCallback?.invoke(false)
            } else {
                Log.i(TAG, "TTS Init Thành công")
                isReady = true
                setupUtteranceListener()
                initCallback?.invoke(true)
            }
        } else {
            Log.e(TAG, "TTS Init Thất bại")
            isReady = false
            initCallback?.invoke(false)
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                Log.d(TAG, "Bắt đầu đọc: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "Đã đọc xong: $utteranceId")
                // Tự động đọc tin tiếp theo trong hàng đợi
                isSpeaking = false
                speakNext()
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Lỗi khi đọc: $utteranceId")
                isSpeaking = false
                speakNext() // Thử đọc tin tiếp theo dù lỗi
            }
        })
    }

    fun isReady(): Boolean = isReady

    fun addToQueue(text: String) {
        if (!isReady) {
            Log.w(TAG, "TTS chưa sẵn sàng, không thể thêm vào hàng đợi.")
            return
        }

        queue.add(text)

        // Nếu hiện tại không đọc gì, kích hoạt đọc ngay
        if (!isSpeaking && !tts!!.isSpeaking) {
            speakNext()
        }
    }

    private fun speakNext() {
        val nextText = queue.poll() // Lấy và xóa phần tử đầu tiên
        if (nextText != null) {
            val params = android.os.Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "NEWS_${System.currentTimeMillis()}")

            // Sử dụng QUEUE_FLUSH ở đây là an toàn vì chúng ta tự quản lý hàng đợi
            // và chỉ gọi speakNext khi tin trước đã xong (hoặc lần đầu).
            tts?.speak(nextText, TextToSpeech.QUEUE_FLUSH, params, "NEWS_ID")
        } else {
            isSpeaking = false
            Log.d(TAG, "Hàng đợi rỗng.")
        }
    }

    fun stop() {
        queue.clear()
        tts?.stop()
        isSpeaking = false
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}