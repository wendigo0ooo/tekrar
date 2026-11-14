package com.wendigo.tekrar.ui.learn

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * `SpeechRecognizer` için küçük, tek kullanımlık sarmalayıcı.
 *
 * - Cihazda STT yoksa [isAvailable] false döner; çağıran taraf yazma alanına düşer.
 * - İzin kontrolü çağıran taraftadır; izin yoksa [start] kullanılmamalıdır.
 * - Tüm çağrılar ana iş parçacığında yapılmalıdır (Android kuralı); Compose
 *   tıklamaları bu koşulu sağlar.
 */
enum class SpeechInputError {
    PERMISSION,
    NO_MATCH,
    NETWORK,
    BUSY,
    UNAVAILABLE,
    UNKNOWN,
}

class SpeechInput(
    private val context: Context,
    private val onListeningChanged: (Boolean) -> Unit = {},
    private val onResult: (String) -> Unit,
    private val onFailure: (SpeechInputError) -> Unit,
) {

    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var destroyed = false

    fun isAvailable(): Boolean =
        runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)

    fun start() {
        if (destroyed) return
        if (!isAvailable()) {
            onFailure(SpeechInputError.UNAVAILABLE)
            return
        }
        if (listening) return
        val engine = try {
            recognizer ?: SpeechRecognizer
                .createSpeechRecognizer(context.applicationContext)
                .also { created ->
                    created.setRecognitionListener(listener)
                    recognizer = created
                }
        } catch (failure: Throwable) {
            onFailure(SpeechInputError.UNKNOWN)
            return
        }
        engine.setRecognitionListener(listener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        listening = true
        onListeningChanged(true)
        try {
            engine.startListening(intent)
        } catch (failure: Throwable) {
            listening = false
            onListeningChanged(false)
            onFailure(SpeechInputError.UNKNOWN)
        }
    }

    fun stop() {
        if (!listening) return
        listening = false
        onListeningChanged(false)
        runCatching { recognizer?.stopListening() }
    }

    fun destroy() {
        destroyed = true
        listening = false
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            if (!listening) return
            listening = false
            onListeningChanged(false)
            onFailure(mapError(error))
        }

        override fun onResults(results: Bundle?) {
            if (!listening) return
            listening = false
            onListeningChanged(false)
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (text.isBlank()) {
                onFailure(SpeechInputError.NO_MATCH)
            } else {
                onResult(text)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) = Unit

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun mapError(error: Int): SpeechInputError = when (error) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechInputError.PERMISSION
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechInputError.NO_MATCH

        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechInputError.NETWORK

        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechInputError.BUSY
        else -> SpeechInputError.UNKNOWN
    }
}
