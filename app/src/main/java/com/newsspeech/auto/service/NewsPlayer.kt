package com.newsspeech.auto.service

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
    private var appContext: Context? = null  // ✅ Lưu context để request audio focus

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

    // ========================================
    // LIFECYCLE METHODS
    // ========================================

    fun register(tag: String) {
        synchronized(usersLock) {
            activeUsers++
            Log.d(TAG, "📝 [$tag] đăng ký sử dụng TTS. Tổng users: $activeUsers")
        }
    }

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

    @Synchronized
    fun init(context: Context, callback: ((Boolean) -> Unit)? = null) {
        // Lưu context
        if (appContext == null) {
            appContext = context.applicationContext
            // ✅ Khởi tạo AudioManager
            audioManager = appContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            Log.d(TAG, "🔊 AudioManager initialized: ${audioManager != null}")
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
                // Pre-load classes
                try {
                    Class.forName("android.speech.tts.TextToSpeech")
                    Class.forName("android.speech.tts.TextToSpeech\$OnInitListener")
                } catch (e: ClassNotFoundException) {
                    // Ignore
                }

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

                        // ✅ Configure TTS for Android Auto
                        tts?.apply {
                            setSpeechRate(1.0f)  // Normal speed
                            setPitch(1.0f)       // Normal pitch
                        }

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

                // Đọc tin tiếp theo
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

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                super.onStop(utteranceId, interrupted)
                Log.d(TAG, "⏹️ TTS stopped: $utteranceId, interrupted: $interrupted")

                // ✅ Abandon audio focus when stopped
                abandonAudioFocus()
            }
        })
    }

    // ========================================
    // AUDIO FOCUS MANAGEMENT
    // ========================================

    /**
     * ✅ Request audio focus before speaking
     */
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
            // Android 8.0+
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)  // ✅ USAGE_MEDIA for Android Auto
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
            // Android < 8.0
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                { focusChange -> handleAudioFocusChange(focusChange) },
                AudioManager.STREAM_MUSIC,  // ✅ Use MUSIC stream
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

    /**
     * ✅ Abandon audio focus when done
     */
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

    /**
     * ✅ Handle audio focus changes
     */
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
                // Continue playing at lower volume
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

    private fun speakNext() {
        val nextText = queue.poll()

        if (nextText != null) {
            _queueSize.value = queue.size
            Log.d(TAG, "📢 Đọc tin: '${nextText.take(50)}...' (Còn ${queue.size} tin trong queue)")

            // ✅ Request audio focus BEFORE speaking
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

            // ✅ Set audio stream for Android Auto
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
            abandonAudioFocus()  // ✅ Release audio focus when done
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
        abandonAudioFocus()  // ✅ Release audio focus
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

            Log.i(TAG, "✅ TTS đã shutdown hoàn toàn")
        }
    }
}