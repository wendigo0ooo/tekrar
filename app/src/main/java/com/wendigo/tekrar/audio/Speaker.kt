package com.wendigo.tekrar.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class Speaker(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null
    private var rate: Float = 1.0f

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                ready = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
                tts?.setSpeechRate(rate)
                val queued = pending
                pending = null
                if (queued != null) speak(queued)
            }
        }
    }

    fun setRate(value: Float) {
        rate = value.coerceIn(0.5f, 1.5f)
        tts?.setSpeechRate(rate)
    }

    fun speak(text: String, value: Float = rate) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        if (!ready) {
            pending = clean
            return
        }
        tts?.setSpeechRate(value.coerceIn(0.5f, 1.5f))
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "tekrar-word")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        pending = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
