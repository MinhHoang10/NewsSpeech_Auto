package com.newsspeech.auto.service

import android.speech.tts.Voice
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
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
 * ✅ Audio Focus cho Android Auto
 * ✅ StateFlow để observe trạng thái realtime
 */
object NewsPlayer : TextToSpeech.OnInitListener {

    private const val TAG = "NewsPlayer"

    // === TTS Core ===
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var appContext: Context? = null

    // === Audio Focus ===
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

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

    // === Voices StateFlow ===
    private val _availableVoices = MutableStateFlow<List<Voice>>(emptyList())
    val availableVoices: StateFlow<List<Voice>> = _availableVoices.asStateFlow()

    // ========================================
    // LIFECYCLE METHODS
    // ========================================

    fun register(tag: String) {
        synchronized(usersLock) {
            activeUsers++
            Log.d(TAG, "🔒 [$tag] đăng ký sử dụng TTS. Tổng users: $activeUsers")
        }
    }

    fun unregister(tag: String) {
        synchronized(usersLock) {
            activeUsers--
            Log.d(TAG, "🔓 [$tag] hủy đăng ký TTS. Còn lại: $activeUsers users")

            if (activeUsers <= 0) {
                Log.i(TAG, "🛑 Không còn component nào sử dụng, shutdown TTS")
                shutdown()
            }
        }
    }

    // ========================================
    // INITIALIZATION
    // ========================================

    @Synchronized
    fun init(context: Context, callback: ((Boolean) -> Unit)? = null) {
        // Lưu context
        if (appContext == null) {
            appContext = context.applicationContext
            audioManager = appContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            Log.d(TAG, "📊 AudioManager initialized: ${audioManager != null}")
        }

        // Case 1: Đã sẵn sàng
        if (isReady && tts != null) {
            Log.d(TAG, "✅ TTS đã sẵn sàng, không cần init lại")
            callback?.invoke(true)
            return
        }

        // Case 2: Đang khởi tạo
        if (isInitializing) {
            Log.w(TAG, "⏳ TTS đang được khởi tạo bởi thread khác, thêm callback vào hàng đợi")
            if (callback != null) {
                pendingCallbacks.add(callback)
            }
            return
        }

        // Case 3: Chưa init
        Log.i(TAG, "🚀 Bắt đầu khởi tạo TTS...")
        isInitializing = true

        if (callback != null) {
            pendingCallbacks.add(callback)
        }

        try {
            if (tts == null) {
                val initStartTime = System.currentTimeMillis()
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

    override fun onInit(status: Int) {
        isInitializing = false

        when (status) {
            TextToSpeech.SUCCESS -> {
                val langResult = tts?.setLanguage(Locale("vi", "VN"))

                when (langResult) {
                    TextToSpeech.LANG_MISSING_DATA -> {
                        Log.e(TAG, "❌ Thiếu dữ liệu ngôn ngữ Tiếng Việt")
                        isReady = false
                        _readyState.value = false
                        _availableVoices.value = emptyList()
                        notifyAllCallbacks(false)
                    }
                    TextToSpeech.LANG_NOT_SUPPORTED -> {
                        Log.e(TAG, "❌ TTS Engine không hỗ trợ Tiếng Việt")
                        isReady = false
                        _readyState.value = false
                        _availableVoices.value = emptyList()
                        notifyAllCallbacks(false)
                    }
                    else -> {
                        Log.i(TAG, "✅ TTS khởi tạo thành công với ngôn ngữ Tiếng Việt")

                        // Configure TTS for Android Auto
                        tts?.apply {
                            setSpeechRate(1.0f)
                            setPitch(1.0f)
                        }

                        // ✅ Load và lưu danh sách giọng vào StateFlow
                        loadAndCacheVoices()

                        // Log chi tiết
                        logAvailableVoices()

                        isReady = true
                        _readyState.value = true
                        setupUtteranceListener()
                        notifyAllCallbacks(true)
                    }
                }
            }

            TextToSpeech.ERROR -> {
                Log.e(TAG, "❌ TTS Engine bị disable hoặc không khả dụng")
                isReady = false
                _readyState.value = false
                _availableVoices.value = emptyList()
                notifyAllCallbacks(false)
            }

            else -> {
                Log.e(TAG, "❌ TTS Init thất bại với status không xác định: $status")
                isReady = false
                _readyState.value = false
                _availableVoices.value = emptyList()
                notifyAllCallbacks(false)
            }
        }
    }

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
                speakNext()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "❌ Lỗi khi đọc: $utteranceId")
                isSpeaking.set(false)
                _currentlySpeaking.value = false
                speakNext()
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                super.onStop(utteranceId, interrupted)
                Log.d(TAG, "⏹️ TTS stopped: $utteranceId, interrupted: $interrupted")
                abandonAudioFocus()
            }
        })
    }

    // ========================================
    // VOICES MANAGEMENT
    // ========================================

    /**
     * ✅ Load và cache danh sách giọng vào StateFlow
     */
    private fun loadAndCacheVoices() {
        val allVoices = tts?.voices

        if (allVoices.isNullOrEmpty()) {
            Log.w(TAG, "❌ Không tìm thấy giọng đọc nào")
            _availableVoices.value = emptyList()
            return
        }

        // Lọc và sort giọng tiếng Việt
        val vietnameseVoices = allVoices
            .filter { it.locale.language == "vi" }
            .sortedWith(compareByDescending<Voice> { voice ->
                var score = 0

                // Điểm chất lượng
                when (voice.quality) {
                    Voice.QUALITY_VERY_HIGH -> score += 100
                    Voice.QUALITY_HIGH -> score += 80
                    Voice.QUALITY_NORMAL -> score += 50
                    else -> score += 0
                }

                // Ưu tiên offline
                if (!voice.isNetworkConnectionRequired) score += 50

                score
            }.thenBy { it.name })

        _availableVoices.value = vietnameseVoices

        Log.d(TAG, "✅ Đã cache ${vietnameseVoices.size} giọng tiếng Việt vào StateFlow")
    }

    /**
     * Lấy danh sách tất cả giọng đọc tiếng Việt
     */
    fun getAvailableVietnameseVoices(): List<Voice> {
        // ✅ Trả về từ StateFlow (đã được cache)
        return _availableVoices.value
    }

    /**
     * Lấy giọng đọc hiện tại
     */
    fun getCurrentVoice(): Voice? {
        return tts?.voice
    }

    /**
     * Đặt giọng đọc theo tên
     */
    fun setVoice(voiceName: String): Boolean {
        if (tts == null) {
            Log.w(TAG, "⚠️ TTS chưa được khởi tạo")
            return false
        }

        val voice = _availableVoices.value.find { it.name == voiceName }

        return if (voice != null) {
            val result = tts?.setVoice(voice)
            val success = result == TextToSpeech.SUCCESS

            if (success) {
                Log.i(TAG, "✅ Đã đổi sang giọng: $voiceName")
            } else {
                Log.e(TAG, "❌ Không thể đổi sang giọng: $voiceName")
            }

            success
        } else {
            Log.w(TAG, "⚠️ Không tìm thấy giọng: $voiceName")
            false
        }
    }

    /**
     * Đặt tốc độ đọc (0.1 - 3.0)
     */
    fun setSpeechRate(rate: Float): Boolean {
        if (tts == null) {
            Log.w(TAG, "⚠️ TTS chưa được khởi tạo")
            return false
        }

        val validRate = rate.coerceIn(0.1f, 3.0f)
        val result = tts?.setSpeechRate(validRate)
        val success = result == TextToSpeech.SUCCESS

        if (success) {
            Log.i(TAG, "✅ Đã đặt tốc độ đọc: ${validRate}x")
        } else {
            Log.e(TAG, "❌ Không thể đặt tốc độ đọc")
        }

        return success
    }

    /**
     * Đặt cao độ giọng nói (0.1 - 2.0)
     */
    fun setPitch(pitch: Float): Boolean {
        if (tts == null) {
            Log.w(TAG, "⚠️ TTS chưa được khởi tạo")
            return false
        }

        val validPitch = pitch.coerceIn(0.1f, 2.0f)
        val result = tts?.setPitch(validPitch)
        val success = result == TextToSpeech.SUCCESS

        if (success) {
            Log.i(TAG, "✅ Đã đặt cao độ: $validPitch")
        } else {
            Log.e(TAG, "❌ Không thể đặt cao độ")
        }

        return success
    }

    /**
     * Kiểm tra xem có nhiều giọng đọc tiếng Việt không
     */
    fun hasMultipleVietnameseVoices(): Boolean {
        return _availableVoices.value.size > 1
    }

    /**
     * Log chi tiết tất cả giọng đọc
     */
    fun logAvailableVoices() {
        if (tts == null) {
            Log.w(TAG, "⚠️ TTS chưa được khởi tạo")
            return
        }

        Log.d(TAG, "=== KIỂM TRA GIỌNG ĐỌC TTS ===")

        val allVoices = tts?.voices

        if (allVoices.isNullOrEmpty()) {
            Log.w(TAG, "❌ Không tìm thấy giọng đọc nào")
            return
        }

        Log.d(TAG, "📊 Tổng số giọng: ${allVoices.size}")

        val vietnameseVoices = _availableVoices.value

        if (vietnameseVoices.isEmpty()) {
            Log.w(TAG, "❌ Không có giọng đọc tiếng Việt")

            Log.d(TAG, "📋 Một số giọng khác:")
            allVoices.take(5).forEach { voice ->
                Log.d(TAG, "  - ${voice.name} (${voice.locale})")
            }
        } else {
            Log.d(TAG, "✅ Tìm thấy ${vietnameseVoices.size} giọng tiếng Việt:")

            vietnameseVoices.forEachIndexed { index, voice ->
                Log.d(TAG, """
                [$index] ${voice.name}
                  Locale: ${voice.locale}
                  Chất lượng: ${getQualityString(voice.quality)}
                  Độ trễ: ${getLatencyString(voice.latency)}
                  Yêu cầu mạng: ${voice.isNetworkConnectionRequired}
                  Features: ${voice.features}
            """.trimIndent())
            }
        }

        val currentVoice = tts?.voice
        if (currentVoice != null) {
            Log.d(TAG, "\n🎤 GIỌNG ĐANG SỬ DỤNG:")
            Log.d(TAG, "  Tên: ${currentVoice.name}")
            Log.d(TAG, "  Locale: ${currentVoice.locale}")
            Log.d(TAG, "  Chất lượng: ${getQualityString(currentVoice.quality)}")
        }

        Log.d(TAG, "=====================================")
    }

    private fun getQualityString(quality: Int): String {
        return when (quality) {
            Voice.QUALITY_VERY_HIGH -> "Rất cao"
            Voice.QUALITY_HIGH -> "Cao"
            Voice.QUALITY_NORMAL -> "Bình thường"
            Voice.QUALITY_LOW -> "Thấp"
            Voice.QUALITY_VERY_LOW -> "Rất thấp"
            else -> "Không xác định ($quality)"
        }
    }

    private fun getLatencyString(latency: Int): String {
        return when (latency) {
            Voice.LATENCY_VERY_LOW -> "Rất thấp"
            Voice.LATENCY_LOW -> "Thấp"
            Voice.LATENCY_NORMAL -> "Bình thường"
            Voice.LATENCY_HIGH -> "Cao"
            Voice.LATENCY_VERY_HIGH -> "Rất cao"
            else -> "Không xác định ($latency)"
        }
    }

    // ========================================
    // AUDIO FOCUS MANAGEMENT
    // ========================================

    private fun requestAudioFocus(): Boolean {
        if (audioManager == null) {
            Log.e(TAG, "❌ AudioManager is null, cannot request audio focus")
            return false
        }

        if (hasAudioFocus) {
            Log.d(TAG, "🔊 Already has audio focus")
            return true
        }

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener { focusChange ->
                    handleAudioFocusChange(focusChange)
                }
                .build()

            audioManager?.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                { focusChange -> handleAudioFocusChange(focusChange) },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        } ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED

        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)

        if (hasAudioFocus) {
            Log.i(TAG, "✅ Audio focus GRANTED")
        } else {
            Log.e(TAG, "❌ Audio focus DENIED (result: $result)")
        }

        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager?.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus { }
        }

        hasAudioFocus = false
        Log.d(TAG, "🔇 Audio focus abandoned")
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "🔊 Audio focus GAIN")
                hasAudioFocus = true
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.w(TAG, "🔇 Audio focus LOSS - stopping TTS")
                hasAudioFocus = false
                stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.w(TAG, "⏸️ Audio focus LOSS_TRANSIENT - pausing")
                hasAudioFocus = false
                tts?.stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "🔉 Audio focus DUCK - continue at lower volume")
            }
        }
    }

    // ========================================
    // PLAYBACK METHODS
    // ========================================

    fun isReady(): Boolean = isReady

    fun addToQueue(text: String) {
        if (!isReady) {
            Log.w(TAG, "⚠️ TTS not ready, cannot add to queue")
            return
        }

        if (text.isBlank()) {
            Log.w(TAG, "⚠️ Text rỗng, bỏ qua")
            return
        }

        queue.add(text)
        _queueSize.value = queue.size
        Log.d(TAG, "➕ Thêm vào queue: '${text.take(50)}...' (Queue size: ${queue.size})")

        if (isSpeaking.compareAndSet(false, true)) {
            _currentlySpeaking.value = true
            Log.d(TAG, "🎤 Thread này được quyền đọc tin đầu tiên")
            speakNext()
        } else {
            Log.d(TAG, "⏸️ Đang đọc tin khác, tin này sẽ chờ trong queue")
        }
    }

    fun addToQueue(title: String, content: String) {
        if (!isReady) {
            Log.w(TAG, "⚠️ TTS not ready, cannot add to queue")
            return
        }

        if (content.isBlank()) {
            Log.w(TAG, "⚠️ Content rỗng, bỏ qua")
            return
        }

        val fullText = buildString {
            append(title)
            append(". ")
            append(content)
        }

        queue.add(fullText)
        _queueSize.value = queue.size
        Log.d(TAG, "➕ Thêm vào queue: '$title' (Queue size: ${queue.size})")

        if (isSpeaking.compareAndSet(false, true)) {
            _currentlySpeaking.value = true
            Log.d(TAG, "🎤 Thread này được quyền đọc tin đầu tiên")
            speakNext()
        } else {
            Log.d(TAG, "⏸️ Đang đọc tin khác, tin này sẽ chờ trong queue")
        }
    }

    private fun speakNext() {
        val nextText = queue.poll()

        if (nextText != null) {
            _queueSize.value = queue.size
            Log.d(TAG, "📢 Đọc tin: '${nextText.take(50)}...' (Còn ${queue.size} tin trong queue)")

            if (!requestAudioFocus()) {
                Log.e(TAG, "❌ Failed to get audio focus, cannot speak")
                isSpeaking.set(false)
                _currentlySpeaking.value = false
                return
            }

            val params = android.os.Bundle()
            params.putString(
                TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                "NEWS_${System.currentTimeMillis()}"
            )
            params.putInt(
                TextToSpeech.Engine.KEY_PARAM_STREAM,
                AudioManager.STREAM_MUSIC
            )

            try {
                tts?.speak(nextText, TextToSpeech.QUEUE_FLUSH, params, "NEWS_ID")
                Log.d(TAG, "🔊 TTS speak() called with audio focus")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception khi gọi speak()", e)
                isSpeaking.set(false)
                _currentlySpeaking.value = false
                abandonAudioFocus()
            }
        } else {
            Log.d(TAG, "🔇 Hàng đợi rỗng, dừng phát")
            isSpeaking.set(false)
            _currentlySpeaking.value = false
            _queueSize.value = 0
            abandonAudioFocus()
        }
    }

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
        abandonAudioFocus()
    }

    private fun shutdown() {
        synchronized(usersLock) {
            Log.w(TAG, "🛑 Shutdown TTS...")

            try {
                stop()
                tts?.shutdown()
                tts = null
                audioManager = null
                audioFocusRequest = null
                appContext = null
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi shutdown TTS", e)
            }

            isReady = false
            isInitializing = false
            activeUsers = 0
            hasAudioFocus = false
            pendingCallbacks.clear()

            _readyState.value = false
            _queueSize.value = 0
            _currentlySpeaking.value = false
            _availableVoices.value = emptyList()

            Log.i(TAG, "✅ TTS đã shutdown hoàn toàn")
        }
    }
}