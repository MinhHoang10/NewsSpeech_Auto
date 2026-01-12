package com.newsspeech.auto.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Singleton quản lý Text-to-Speech cho toàn bộ app
 *
 * ✅ Thread-safe
 * ✅ Hỗ trợ nhiều component sử dụng đồng thời (Activity + Service)
 * ✅ Tự động quản lý lifecycle
 * ✅ StateFlow để observe trạng thái realtime (không polling)
 */
object NewsPlayer : TextToSpeech.OnInitListener {

    private const val TAG = "NewsPlayer"

    // === TTS Core ===
    private var tts: TextToSpeech? = null
    private var isReady = false

    // === Init Management ===
    private var isInitializing = false
    private val pendingCallbacks = mutableListOf<(Boolean) -> Unit>()

    // === Lifecycle Management ===
    private var activeUsers = 0
    private val usersLock = Any()

    // === Queue Management ===
    private val queue = ConcurrentLinkedQueue<String>()
    private val isSpeaking = AtomicBoolean(false)

    // === StateFlow for UI ===
    private val _readyState = MutableStateFlow(false)
    val readyState: StateFlow<Boolean> = _readyState.asStateFlow()

    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

    private val _currentlySpeaking = MutableStateFlow(false)
    val currentlySpeaking: StateFlow<Boolean> = _currentlySpeaking.asStateFlow()

    // ========================================
    // LIFECYCLE METHODS
    // ========================================

    /**
     * Đăng ký component sử dụng TTS
     * Gọi trong onCreate() của Activity/Service
     *
     * @param tag Tên component để debug
     */
    fun register(tag: String) {
        synchronized(usersLock) {
            activeUsers++
            Log.d(TAG, "📝 [$tag] đăng ký sử dụng TTS. Tổng users: $activeUsers")
        }
    }

    /**
     * Hủy đăng ký component
     * Gọi trong onDestroy() của Activity/Service
     * TTS chỉ shutdown khi activeUsers = 0
     *
     * @param tag Tên component để debug
     */
    fun unregister(tag: String) {
        synchronized(usersLock) {
            activeUsers--
            Log.d(TAG, "📝 [$tag] hủy đăng ký TTS. Còn lại: $activeUsers users")

            if (activeUsers <= 0) {
                Log.i(TAG, "🛑 Không còn component nào sử dụng, shutdown TTS")
                shutdown()
            }
        }
    }

    // ========================================
    // INITIALIZATION
    // ========================================

    /**
     * Khởi tạo TTS (thread-safe, có thể gọi từ nhiều nơi)
     *
     * ⚠️ QUAN TRỌNG: Hàm này PHẢI được gọi từ background thread
     * vì TextToSpeech constructor block thread 3-8 giây
     *
     * @param context Application context
     * @param callback Nhận kết quả init (true/false)
     */
    @Synchronized
    fun init(context: Context, callback: ((Boolean) -> Unit)? = null) {
        // Case 1: Đã sẵn sàng
        if (isReady && tts != null) {
            Log.d(TAG, "✅ TTS đã sẵn sàng, không cần init lại")
            callback?.invoke(true)
            return
        }

        // Case 2: Đang khởi tạo → thêm callback vào queue
        if (isInitializing) {
            Log.w(TAG, "⏳ TTS đang được khởi tạo bởi thread khác, thêm callback vào hàng đợi")
            if (callback != null) {
                pendingCallbacks.add(callback)
            }
            return
        }

        // Case 3: Chưa init → bắt đầu init
        Log.i(TAG, "🚀 Bắt đầu khởi tạo TTS...")
        isInitializing = true

        if (callback != null) {
            pendingCallbacks.add(callback)
        }

        try {
            if (tts == null) {
                // ✅ PRE-LOAD: Trigger class loading trước khi gọi constructor
                try {
                    Class.forName("android.speech.tts.TextToSpeech")
                    Class.forName("android.speech.tts.TextToSpeech\$OnInitListener")
                } catch (e: ClassNotFoundException) {
                    // Ignore
                }

                val initStartTime = System.currentTimeMillis()

                // Constructor BLOCK thread 3-8 giây!
                tts = TextToSpeech(context.applicationContext, this)

                val elapsed = System.currentTimeMillis() - initStartTime
                Log.d(TAG, "⏱️ TextToSpeech constructor took ${elapsed}ms")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception khi khởi tạo TTS", e)
            isInitializing = false
            notifyAllCallbacks(false)
        }
    }
    /**
     * Callback từ TextToSpeech khi init xong
     * ⚠️ Được gọi trên MAIN THREAD bởi TTS Engine
     */
    override fun onInit(status: Int) {
        isInitializing = false

        when (status) {
            TextToSpeech.SUCCESS -> {
                val langResult = tts?.setLanguage(Locale("vi", "VN"))

                when (langResult) {
                    TextToSpeech.LANG_MISSING_DATA -> {
                        Log.e(TAG, "❌ Thiếu dữ liệu ngôn ngữ Tiếng Việt")
                        Log.e(TAG, "💡 Hướng dẫn: Vào Settings → Language & Input → Text-to-Speech → Tải tiếng Việt")
                        isReady = false
                        _readyState.value = false
                        notifyAllCallbacks(false)
                    }
                    TextToSpeech.LANG_NOT_SUPPORTED -> {
                        Log.e(TAG, "❌ TTS Engine không hỗ trợ Tiếng Việt")
                        isReady = false
                        _readyState.value = false
                        notifyAllCallbacks(false)
                    }
                    else -> {
                        Log.i(TAG, "✅ TTS khởi tạo thành công với ngôn ngữ Tiếng Việt")
                        isReady = true
                        _readyState.value = true
                        setupUtteranceListener()
                        notifyAllCallbacks(true)
                    }
                }
            }

            TextToSpeech.ERROR -> {
                Log.e(TAG, "❌ TTS Engine bị disable hoặc không khả dụng")
                Log.e(TAG, "💡 Kiểm tra: Settings → Apps → Google Text-to-Speech → Enabled")
                isReady = false
                _readyState.value = false
                notifyAllCallbacks(false)
            }

            else -> {
                Log.e(TAG, "❌ TTS Init thất bại với status không xác định: $status")
                isReady = false
                _readyState.value = false
                notifyAllCallbacks(false)
            }
        }
    }

    /**
     * Gọi tất cả callback đang chờ kết quả init
     */
    @Synchronized
    private fun notifyAllCallbacks(success: Boolean) {
        val count = pendingCallbacks.size
        Log.d(TAG, "📢 Thông báo kết quả init ($success) cho $count callbacks")

        pendingCallbacks.forEach { callback ->
            try {
                callback.invoke(success)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi gọi callback", e)
            }
        }
        pendingCallbacks.clear()
    }

    /**
     * Setup listener để theo dõi quá trình đọc
     */
    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking.set(true)
                _currentlySpeaking.value = true
                Log.d(TAG, "🔊 Bắt đầu đọc: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "✅ Đã đọc xong: $utteranceId")
                isSpeaking.set(false)
                _currentlySpeaking.value = false

                // Tự động đọc tin tiếp theo
                speakNext()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "❌ Lỗi khi đọc: $utteranceId")
                isSpeaking.set(false)
                _currentlySpeaking.value = false

                // Vẫn thử đọc tin tiếp theo
                speakNext()
            }
        })
    }

    // ========================================
    // PLAYBACK METHODS
    // ========================================

    /**
     * Kiểm tra TTS đã sẵn sàng chưa
     */
    fun isReady(): Boolean = isReady

    /**
     * Thêm text vào hàng đợi để đọc
     * Thread-safe, có thể gọi từ nhiều thread
     *
     * @param text Nội dung cần đọc
     */
    fun addToQueue(text: String) {
        if (!isReady || text.isBlank()) {
            Log.w(TAG, "Cannot add to queue")
            return
        }

        if (text.isBlank()) {
            Log.w(TAG, "⚠️ Text rỗng, bỏ qua")
            return
        }

        queue.add(text)
        _queueSize.value = queue.size
        Log.d(TAG, "➕ Thêm vào queue: '${text.take(50)}...' (Queue size: ${queue.size})")

        // CompareAndSet: Chỉ 1 thread được quyền gọi speakNext()
        // Thread thắng sẽ set isSpeaking = true và được đọc
        // Thread thua sẽ thấy isSpeaking = true và thoát
        if (isSpeaking.compareAndSet(false, true)) {
            _currentlySpeaking.value = true
            Log.d(TAG, "🎤 Thread này được quyền đọc tin đầu tiên")
            speakNext()
        } else {
            Log.d(TAG, "⏸️ Đang đọc tin khác, tin này sẽ chờ trong queue")
        }
    }

    /**
     * Đọc tin tiếp theo trong queue
     * QUAN TRỌNG: Chỉ gọi trong UtteranceProgressListener hoặc sau compareAndSet
     */
    private fun speakNext() {
        val nextText = queue.poll()

        if (nextText != null) {
            _queueSize.value = queue.size
            Log.d(TAG, "🔢 Đọc tin: '${nextText.take(50)}...' (Còn ${queue.size} tin trong queue)")

            val params = android.os.Bundle()
            params.putString(
                TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                "NEWS_${System.currentTimeMillis()}"
            )

            try {
                // QUEUE_FLUSH: Xóa hàng đợi TTS cũ (vì ta tự quản lý queue)
                tts?.speak(nextText, TextToSpeech.QUEUE_FLUSH, params, "NEWS_ID")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception khi gọi speak()", e)
                isSpeaking.set(false)
                _currentlySpeaking.value = false
            }
        } else {
            Log.d(TAG, "🔇 Hàng đợi rỗng, dừng phát")
            isSpeaking.set(false)
            _currentlySpeaking.value = false
            _queueSize.value = 0
        }
    }

    /**
     * Dừng phát và xóa hàng đợi
     */
    fun stop() {
        val queueSize = queue.size
        queue.clear()
        _queueSize.value = 0

        try {
            tts?.stop()
            Log.i(TAG, "⏹️ Đã dừng phát và xóa $queueSize tin trong queue")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi khi stop TTS", e)
        }

        isSpeaking.set(false)
        _currentlySpeaking.value = false
    }

    /**
     * Shutdown TTS hoàn toàn
     * CHỈ gọi khi activeUsers = 0
     */
    private fun shutdown() {
        synchronized(usersLock) {
            Log.w(TAG, "🛑 Shutdown TTS...")

            try {
                stop()
                tts?.shutdown()
                tts = null
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi shutdown TTS", e)
            }

            isReady = false
            isInitializing = false
            activeUsers = 0
            pendingCallbacks.clear()

            _readyState.value = false
            _queueSize.value = 0
            _currentlySpeaking.value = false

            Log.i(TAG, "✅ TTS đã shutdown hoàn toàn")
        }
    }

    // ========================================
    // DEBUG METHODS
    // ========================================

    /**
     * Lấy thông tin trạng thái để debug
     */
//    fun getStatus(): String {
//        return """
//            |TTS Status:
//            |  - Ready: $isReady
//            |  - Initializing: $isInitializing
//            |  - Active Users: $activeUsers
//            |  - Is Speaking: ${isSpeaking.get()}
//            |  - Queue Size: ${queue.size}
//            |  - TTS Instance: ${if (tts != null) "✓" else "✗"}
//        """.trimMargin()
//    }
}