package com.newsspeech.auto.service

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.newsspeech.auto.domain.model.News
import java.util.Locale

object NewsPlayer : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val queue = mutableListOf<String>()
    private var isSpeaking = false
    private var isInitialized = false
    private var lastText: String? = null  // để resume

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        isInitialized = (status == TextToSpeech.SUCCESS)

        if (!isInitialized) {
            println("TTS failed to initialize with status: $status")
            return
        }

        val result = tts?.setLanguage(Locale("vi", "VN"))  // Hoặc Locale.VIETNAM
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Handle error: Log hoặc notify user install TTS data từ Settings
            println("TTS language VN not supported")
            isInitialized = false
            return
        }

        tts?.setPitch(1.0f)
        tts?.setSpeechRate(1.0f)
        // tts?.language = Locale("vi", "VN") // optional

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

        // chạy queue nếu có
        speakNext()
    }

    fun isReady(): Boolean = tts != null && isInitialized

    fun shutdown() {
        stop()
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

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "NEWS_${System.currentTimeMillis()}")
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
