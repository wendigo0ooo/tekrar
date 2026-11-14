package com.wendigo.tekrar.ai

import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

interface AiClient {
    fun streamChat(
        config: AiConfig,
        messages: List<AiChatMessage>,
        sessionId: String,
    ): Flow<AiStreamEvent>

    suspend fun testConnection(config: AiConfig): Result<Unit>
}

class OpenAiCompatibleClient(
    private val httpClient: OkHttpClient = defaultClient(),
) : AiClient {

    override fun streamChat(
        config: AiConfig,
        messages: List<AiChatMessage>,
        sessionId: String,
    ): Flow<AiStreamEvent> = channelFlow {
        val callRef = AtomicReference<Call?>(null)
        val worker = launch {
            try {
                runStream(config, messages, sessionId, callRef) { event -> send(event) }
            } finally {
                close()
            }
        }
        awaitClose { callRef.get()?.cancel() }
        worker.join()
    }.flowOn(Dispatchers.IO)

    override suspend fun testConnection(config: AiConfig): Result<Unit> =
        withContext(Dispatchers.IO) {
            val probe = config.copy(temperature = 0.0, maxTokens = PROBE_MAX_TOKENS)
            val request = try {
                buildRequest(
                    probe,
                    listOf(AiChatMessage(AiRole.USER, "ping")),
                    stream = false,
                    sessionId = "",
                )
            } catch (error: IllegalArgumentException) {
                return@withContext Result.failure(
                    AiClientException("Sağlayıcı adresi geçersiz.", false)
                )
            }
            try {
                val call = httpClient.newCall(request)
                awaitResponse(call).use { response ->
                    if (response.isSuccessful) {
                        Result.success(Unit)
                    } else {
                        val raw = runCatching { response.body?.string() }.getOrNull()
                        val info = AiErrors.fromHttpCode(
                            response.code,
                            AiErrors.extractServerMessage(raw),
                        )
                        Result.failure(
                            AiClientException(info.messageTr, info.retryable, response.code)
                        )
                    }
                }
            } catch (error: IOException) {
                val info = AiErrors.fromThrowable(error)
                Result.failure(AiClientException(info.messageTr, info.retryable))
            }
        }

    private suspend fun runStream(
        config: AiConfig,
        messages: List<AiChatMessage>,
        sessionId: String,
        callRef: AtomicReference<Call?>,
        emit: suspend (AiStreamEvent) -> Unit,
    ) {
        var attempt = 0
        while (true) {
            attempt += 1
            val request = try {
                buildRequest(config, messages, stream = true, sessionId = sessionId)
            } catch (error: IllegalArgumentException) {
                emit(AiStreamEvent.Error("Sağlayıcı adresi geçersiz.", false))
                return
            }
            val call = httpClient.newCall(request)
            callRef.set(call)
            currentCoroutineContext().ensureActive()
            var retry = false
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        val raw = runCatching { response.body?.string() }.getOrNull()
                        val info = AiErrors.fromHttpCode(
                            response.code,
                            AiErrors.extractServerMessage(raw),
                        )
                        val canRetry = (response.code == 429 || response.code in 500..599) &&
                            attempt < MAX_ATTEMPTS
                        if (canRetry) {
                            retry = true
                        } else {
                            emit(AiStreamEvent.Error(info.messageTr, info.retryable, response.code))
                        }
                    } else {
                        streamResponse(response, emit)
                        return
                    }
                }
            } catch (error: IOException) {
                currentCoroutineContext().ensureActive()
                val info = AiErrors.fromThrowable(error)
                emit(AiStreamEvent.Error(info.messageTr, info.retryable))
                return
            } finally {
                callRef.compareAndSet(call, null)
            }
            if (!retry) return
            delay(RETRY_DELAY_MS)
        }
    }

    private suspend fun streamResponse(
        response: Response,
        emit: suspend (AiStreamEvent) -> Unit,
    ) {
        val body = response.body ?: throw IOException("Boş yanıt gövdesi")
        val parser = SseParser()
        var done = false
        body.charStream().use { reader ->
            val buffer = CharArray(BUFFER_SIZE)
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                val events = parser.feed(String(buffer, 0, count))
                for (event in events) {
                    emit(event)
                    if (event is AiStreamEvent.Done) done = true
                }
                if (done) break
            }
        }
        if (!done) emit(AiStreamEvent.Done())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun awaitResponse(call: Call): Response =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response) { response.close() }
                }
            })
        }

    private fun buildRequest(
        config: AiConfig,
        messages: List<AiChatMessage>,
        stream: Boolean,
        sessionId: String,
    ): Request {
        val payload = AiJson.buildChatRequestJson(config, messages, stream)
        return Request.Builder()
            .url(config.chatCompletionsUrl())
            .header("Authorization", "Bearer ${config.apiKey.trim()}")
            .header("Content-Type", JSON_CONTENT_TYPE)
            .header("Accept", if (stream) "text/event-stream" else "application/json")
            .header("x-opencode-session", normalizeSessionId(sessionId))
            .header("User-Agent", USER_AGENT)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    private fun normalizeSessionId(sessionId: String): String {
        val trimmed = sessionId.trim()
        return when {
            trimmed.isEmpty() -> "ses_" + UUID.randomUUID()
            trimmed.startsWith("ses_") -> trimmed
            else -> "ses_$trimmed"
        }
    }

    companion object {
        const val USER_AGENT = "Tekrar/3.0 (Android)"
        const val RETRY_DELAY_MS = 1_500L
        const val MAX_ATTEMPTS = 2
        const val PROBE_MAX_TOKENS = 8

        private const val BUFFER_SIZE = 2_048
        private const val JSON_CONTENT_TYPE = "application/json"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
