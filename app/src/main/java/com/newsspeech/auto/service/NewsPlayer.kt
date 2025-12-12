package com.newsspeech.auto.service

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.newsspeech.auto.domain.model.News
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

object NewsPlayer : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val queue = mutableListOf<String>()
    private var isSpeaking = false
    private var isInitialized = false
    private var lastText: String? = null  // để resume
    private var initCallback: ((Boolean) -> Unit)? = null

    fun init(context: Context, callback: ((Boolean) -> Unit)? = null) {
        if (tts != null) {
            callback?.invoke(isInitialized)
            return
        }
        initCallback = callback
        // Wrap init trong coroutine để tránh chặn main nếu load chậm
        tts = TextToSpeech(context.applicationContext, this@NewsPlayer)
    }

    override fun onInit(status: Int) {
        isInitialized = (status == TextToSpeech.SUCCESS)

        if (!isInitialized) {
            Log.e("NewsPlayer", "TTS failed to initialize with status: $status")
            initCallback?.invoke(false)
            initCallback = null
            return
        }

        val result = tts?.setLanguage(Locale("vi", "VN"))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("NewsPlayer", "TTS language VN not supported")
            isInitialized = false
            initCallback?.invoke(false)
            initCallback = null
            return
        }

        tts?.setPitch(1.0f)
        tts?.setSpeechRate(1.0f)
        // tts?.language = Locale("vi", "VN") // optional

        val onUtteranceProgressListener: Int? =
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    speakNext()
                }

                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    speakNext()
                }
            })

        initCallback?.invoke(true)
        initCallback = null

        // chạy queue nếu có
        speakNext()
    }

    fun isReady(): Boolean = tts != null && isInitialized

    fun shutdown() {
        stop()
        tts?.setOnUtteranceProgressListener(null)  // Unregister để tránh leak
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    // ---------------------------
    //      QUEUE FUNCTIONS
    // ---------------------------

    fun addToQueue(text: String) {
        if (text.isBlank()) return
        queue.add(text.trim())
        if (!isSpeaking) speakNext()
    }

    /** Đọc danh sách tin theo category */
    fun speakNewsByCategory(category: String, newsList: List<News>) {
        stop()

        queue.add("Tin tức $category.")
        newsList.forEach { news ->
            val content = news.content.trim()
            if (content.isNotEmpty()) {
                queue.add("${news.title}. $content")
            } else {
                queue.add(news.title)
            }
        }
        queue.add("Đã hết tin tức $category.")

        speakNext()
    }

    private fun speakNext() {
        if (!isReady() || isSpeaking || queue.isEmpty()) return

        val text = queue.removeAt(0)
        lastText = text

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "UTT_${System.currentTimeMillis()}")

        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "NEWS_${System.currentTimeMillis()}")
        if (result == TextToSpeech.ERROR) {
            Log.e("NewsPlayer", "Speak failed")
            isSpeaking = false
            speakNext()  // Thử next
        }
    }

    // ---------------------------
    //      CONTROL FUNCTIONS
    // ---------------------------

    fun stop() {
        tts?.stop()
        queue.clear()
        isSpeaking = false
        lastText = null
    }

    fun pause() {
        tts?.stop()
        isSpeaking = false
        // queue vẫn giữ nguyên để resume
    }

    fun resume() {
        if (!isReady()) return
        if (isSpeaking) return

        // Nếu còn text chưa đọc xong → đọc lại
        lastText?.let {
            queue.add(0, it)
            lastText = null
        }

        speakNext()
    }
}