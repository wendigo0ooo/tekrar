package com.wendigo.tekrar.ai

import com.wendigo.tekrar.data.AppDatabase
import com.wendigo.tekrar.data.learn.AiMessageEntity
import com.wendigo.tekrar.data.learn.AiSessionEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

data class AiSessionProfile(
    val level: String = "A1",
    val scenarioTitle: String? = null,
    val settingTr: String? = null,
    val roleEn: String? = null,
    val goalTr: String? = null,
    val targetPhrases: List<String> = emptyList(),
    val targetWords: List<String> = emptyList(),
)

data class CreatedAiSession(
    val session: AiSessionEntity,
    val openingMessage: AiMessageEntity?,
)

sealed interface AiChatEvent {
    data class UserSaved(val messageId: Long, val turnIndex: Int) : AiChatEvent

    data class Delta(val text: String) : AiChatEvent

    data class AssistantSaved(
        val messageId: Long,
        val text: String,
        val usageJson: String?,
    ) : AiChatEvent

    data class CorrectionReady(
        val messageId: Long,
        val correction: CorrectionResult,
    ) : AiChatEvent

    data class Completed(val assistantMessageId: Long) : AiChatEvent

    data class Error(val messageTr: String, val retryable: Boolean) : AiChatEvent
}

/**
 * AI koç oturumlarını yönetir: mesaj geçmişi, streaming yanıt, düzeltme kartı
 * ve oturum özeti. Tüm ağ hataları kullanıcıya Türkçe mesaj olarak döner;
 * ders/FSRS akışı bu katmana bağımlı değildir.
 */
class AiRepository(
    db: AppDatabase,
    private val client: AiClient,
    private val keyStore: SecureKeyStore,
    private val settings: AiSettingsStore,
) {

    private val sessions = db.aiSessionDao()
    private val messages = db.aiMessageDao()

    fun hasKey(): Boolean = runCatching {
        !keyStore.load().isNullOrBlank()
    }.getOrDefault(false)

    fun saveApiKey(key: String) {
        keyStore.save(key.trim())
    }

    fun clearApiKey() {
        keyStore.clear()
    }

    fun currentConfig(): Result<AiConfig> {
        val key = runCatching { keyStore.load() }.getOrElse { error ->
            return Result.failure(
                AiClientException(
                    "AI anahtarı okunamadı; ayarlardan yeniden gir.",
                    false,
                ),
            )
        }
        if (key.isNullOrBlank()) {
            return Result.failure(
                AiClientException("AI anahtarı ayarlanmadı.", false),
            )
        }
        return runCatching { settings.config(key) }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                Result.failure(
                    AiClientException(
                        error.message ?: "AI ayarları geçersiz.",
                        false,
                    ),
                )
            },
        )
    }

    fun observeSessions(): Flow<List<AiSessionEntity>> = sessions.observeAll()

    fun observeMessages(sessionId: String): Flow<List<AiMessageEntity>> =
        messages.observeBySession(sessionId)

    suspend fun getSession(sessionId: String): AiSessionEntity? = sessions.get(sessionId)

    suspend fun getMessages(sessionId: String): List<AiMessageEntity> =
        withContext(Dispatchers.IO) { messages.getBySession(sessionId) }

    suspend fun createSession(
        mode: String,
        title: String,
        unitId: String? = null,
        scenarioId: String? = null,
        openingEn: String? = null,
    ): CreatedAiSession = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val id = "ses_" + UUID.randomUUID().toString().replace("-", "")
        val model = settings.state.value.model
        val session = AiSessionEntity(
            id = id,
            mode = mode,
            unitId = unitId,
            scenarioId = scenarioId,
            title = title.trim().ifBlank { "AI Koç" },
            model = model,
            createdAt = now,
            updatedAt = now,
            promptVersion = PromptBuilder.PROMPT_VERSION,
            messageCount = 0,
        )
        sessions.insert(session)

        val opening = openingEn?.trim()?.takeIf { it.isNotEmpty() }?.let { text ->
            val entity = AiMessageEntity(
                sessionId = id,
                role = AiRole.ASSISTANT.wire,
                content = text,
                turnIndex = 0,
                status = "done",
                promptVersion = PromptBuilder.PROMPT_VERSION,
                createdAt = now,
            )
            val messageId = messages.insert(entity)
            sessions.updateMessageCount(id, 1, now)
            entity.copy(id = messageId)
        }

        CreatedAiSession(
            session = session.copy(messageCount = if (opening != null) 1 else 0),
            openingMessage = opening,
        )
    }

    /**
     * Kullanıcı mesajını kaydeder, yanıtı stream eder, tamamlanınca asistan
     * mesajını ve (varsa) düzeltme kartını kaydeder.
     */
    fun send(
        sessionId: String,
        userText: String,
        profile: AiSessionProfile,
    ): Flow<AiChatEvent> = flow {
        val configResult = currentConfig()
        val config = configResult.getOrNull()
        if (config == null) {
            val error = configResult.exceptionOrNull()
            emit(
                AiChatEvent.Error(
                    error?.message ?: "AI anahtarı ayarlanmadı.",
                    retryable = false,
                ),
            )
            return@flow
        }
        val text = userText.trim()
        if (text.isEmpty()) return@flow

        val session = sessions.get(sessionId)
        if (session == null) {
            emit(AiChatEvent.Error("Sohbet bulunamadı.", retryable = false))
            return@flow
        }

        val now = System.currentTimeMillis()
        val turnIndex = messages.nextTurnIndex(sessionId)
        val userEntity = AiMessageEntity(
            sessionId = sessionId,
            role = AiRole.USER.wire,
            content = text,
            turnIndex = turnIndex,
            status = "done",
            promptVersion = PromptBuilder.PROMPT_VERSION,
            createdAt = now,
        )
        val userMessageId = messages.insert(userEntity)
        emit(AiChatEvent.UserSaved(userMessageId, turnIndex))
        updateSessionCount(sessionId, now)

        val history = messages.getBySession(sessionId).mapNotNull { item ->
            val role = AiRole.fromWire(item.role) ?: return@mapNotNull null
            AiChatMessage(role, item.content)
        }
        val system = PromptBuilder.tutorSystem(
            level = profile.level,
            scenarioTitle = profile.scenarioTitle,
            settingTr = profile.settingTr,
            roleEn = profile.roleEn,
            goalTr = profile.goalTr,
            targetPhrases = profile.targetPhrases,
            targetWords = profile.targetWords,
        )
        val requestMessages = PromptBuilder.tutorMessages(history, system)

        val answer = StringBuilder()
        var usageJson: String? = null
        var streamError: AiStreamEvent.Error? = null
        try {
            client.streamChat(config, requestMessages, sessionId).collect { event ->
                when (event) {
                    is AiStreamEvent.Delta -> {
                        answer.append(event.text)
                        emit(AiChatEvent.Delta(event.text))
                    }

                    is AiStreamEvent.Done -> usageJson = event.usageJson
                    is AiStreamEvent.Error -> streamError = event
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            streamError = AiStreamEvent.Error(
                messageTr = "Yanıt alınamadı; bağlantını kontrol et.",
                retryable = true,
            )
        }

        val partial = answer.toString().trim()
        if (streamError != null) {
            if (partial.isNotEmpty()) {
                messages.insert(
                    AiMessageEntity(
                        sessionId = sessionId,
                        role = AiRole.ASSISTANT.wire,
                        content = partial,
                        turnIndex = messages.nextTurnIndex(sessionId),
                        status = "error",
                        promptVersion = PromptBuilder.PROMPT_VERSION,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
                updateSessionCount(sessionId)
            }
            emit(
                AiChatEvent.Error(
                    streamError?.messageTr ?: "Yanıt alınamadı.",
                    streamError?.retryable ?: true,
                ),
            )
            return@flow
        }

        if (partial.isEmpty()) {
            emit(AiChatEvent.Error("Model boş yanıt döndürdü.", retryable = true))
            return@flow
        }

        val assistantEntity = AiMessageEntity(
            sessionId = sessionId,
            role = AiRole.ASSISTANT.wire,
            content = partial,
            turnIndex = messages.nextTurnIndex(sessionId),
            status = "done",
            promptVersion = PromptBuilder.PROMPT_VERSION,
            createdAt = System.currentTimeMillis(),
        )
        val assistantId = messages.insert(assistantEntity)
        updateSessionCount(sessionId)
        emit(AiChatEvent.AssistantSaved(assistantId, partial, usageJson))

        val cachedCorrection = messages.get(userMessageId)?.correctionJson?.let {
            PromptBuilder.parseCorrectionJson(it)
        }
        val correction = cachedCorrection ?: try {
            requestCorrection(config, text, sessionId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            null
        }
        if (correction != null) {
            messages.updateCorrection(userMessageId, encodeCorrection(correction))
            emit(AiChatEvent.CorrectionReady(userMessageId, correction))
        }
        emit(AiChatEvent.Completed(assistantId))
    }.flowOn(Dispatchers.IO)

    suspend fun summarize(
        sessionId: String,
        profile: AiSessionProfile,
    ): AiSessionSummary? = withContext(Dispatchers.IO) {
        val history = messages.getBySession(sessionId)
        if (history.isEmpty()) return@withContext null
        val transcript = history.mapNotNull { item ->
            val role = AiRole.fromWire(item.role) ?: return@mapNotNull null
            AiChatMessage(role, item.content)
        }
        val draft = buildSummaryDraft(history)
        val fallback = fallbackSummary(history, profile)
        val config = currentConfig().getOrNull()
        val aiSummary = config?.let {
            try {
                requestSummary(it, transcript, draft, sessionId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                null
            }
        }
        val summary = aiSummary ?: fallback
        val now = System.currentTimeMillis()
        sessions.updateSummary(sessionId, encodeSummary(summary), now)
        summary
    }

    suspend fun testConnection(): Result<Unit> {
        val config = currentConfig().getOrElse { error ->
            return Result.failure(
                AiClientException(
                    error.message ?: "AI ayarları geçersiz.",
                    false,
                ),
            )
        }
        return client.testConnection(config)
    }

    /**
     * Gramer yazma görevi için ikincil AI kontrolü. Oturum DB'ye yazılmaz.
     * Başarısızlıkta [Result.failure]; yerel kontrol akışı etkilenmez.
     */
    suspend fun checkGrammar(
        text: String,
        topicTitleTr: String,
        level: String = "A1",
        sessionId: String = "grammar-check",
    ): Result<GrammarAiResult> = withContext(Dispatchers.IO) {
        val sentence = text.trim()
        if (sentence.isEmpty()) {
            return@withContext Result.failure(
                AiClientException("Kontrol edilecek cümle boş.", false),
            )
        }
        val configResult = currentConfig()
        val config = configResult.getOrNull()
            ?: return@withContext Result.failure(
                configResult.exceptionOrNull()
                    ?: AiClientException("AI anahtarı ayarlanmadı.", false),
            )
        val answer = StringBuilder()
        var streamError: AiStreamEvent.Error? = null
        try {
            client.streamChat(
                config = config.copy(
                    temperature = GrammarAi.TEMPERATURE,
                    maxTokens = GrammarAi.MAX_TOKENS,
                ),
                messages = listOf(
                    AiChatMessage(
                        AiRole.USER,
                        PromptBuilder.grammarCheckPrompt(sentence, topicTitleTr, level),
                    ),
                ),
                sessionId = sessionId,
            ).collect { event ->
                when (event) {
                    is AiStreamEvent.Delta -> answer.append(event.text)
                    is AiStreamEvent.Error -> streamError = event
                    is AiStreamEvent.Done -> Unit
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return@withContext Result.failure(
                AiClientException(
                    "AI kontrolü yapılamadı; yerel kontrol geçerli.",
                    true,
                ),
            )
        }
        val failure = streamError
        if (failure != null) {
            return@withContext Result.failure(
                AiClientException(
                    "AI kontrolü yapılamadı; yerel kontrol geçerli.",
                    failure.retryable,
                ),
            )
        }
        val parsed = GrammarAi.parseJson(answer.toString())
            ?: return@withContext Result.failure(
                AiClientException(
                    "AI kontrolü yapılamadı; yerel kontrol geçerli.",
                    true,
                ),
            )
        Result.success(parsed)
    }

    suspend fun deleteAllHistory() = withContext(Dispatchers.IO) {
        messages.deleteAll()
        sessions.deleteAll()
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        messages.deleteBySession(sessionId)
        sessions.delete(sessionId)
    }

    private suspend fun requestCorrection(
        config: AiConfig,
        userMessage: String,
        sessionId: String,
    ): CorrectionResult? {
        val text = StringBuilder()
        var failed = false
        client.streamChat(
            config = config.copy(temperature = 0.2, maxTokens = 250),
            messages = listOf(AiChatMessage(AiRole.USER, PromptBuilder.correctionPrompt(userMessage))),
            sessionId = sessionId,
        ).collect { event ->
            when (event) {
                is AiStreamEvent.Delta -> text.append(event.text)
                is AiStreamEvent.Error -> failed = true
                is AiStreamEvent.Done -> Unit
            }
        }
        if (failed) return null
        return PromptBuilder.parseCorrectionJson(text.toString())
    }

    private suspend fun requestSummary(
        config: AiConfig,
        transcript: List<AiChatMessage>,
        draft: String,
        sessionId: String,
    ): AiSessionSummary? {
        val text = StringBuilder()
        var failed = false
        client.streamChat(
            config = config.copy(temperature = 0.2, maxTokens = 500),
            messages = listOf(
                AiChatMessage(
                    AiRole.USER,
                    PromptBuilder.summaryPrompt(transcript, draft),
                ),
            ),
            sessionId = sessionId,
        ).collect { event ->
            when (event) {
                is AiStreamEvent.Delta -> text.append(event.text)
                is AiStreamEvent.Error -> failed = true
                is AiStreamEvent.Done -> Unit
            }
        }
        if (failed) return null
        return PromptBuilder.parseSummaryJson(text.toString())
    }

    private suspend fun updateSessionCount(
        sessionId: String,
        now: Long = System.currentTimeMillis(),
    ) {
        sessions.updateMessageCount(
            id = sessionId,
            count = messages.countBySession(sessionId),
            updatedAt = now,
        )
    }

    private fun buildSummaryDraft(history: List<AiMessageEntity>): String {
        val userTurns = history.count { it.role == AiRole.USER.wire }
        val corrections = history.mapNotNull { item ->
            item.correctionJson?.let { PromptBuilder.parseCorrectionJson(it) }
        }.filter { it.hasError }
        val notes = corrections.mapNotNull { it.noteTr }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(3)
        return buildString {
            append("Kullanıcı turu: ").append(userTurns)
            append("; düzeltme sayısı: ").append(corrections.size)
            if (notes.isNotEmpty()) {
                append("; notlar: ").append(notes.joinToString(" | "))
            }
        }
    }

    private fun fallbackSummary(
        history: List<AiMessageEntity>,
        profile: AiSessionProfile,
    ): AiSessionSummary {
        val userTurns = history.count { it.role == AiRole.USER.wire }
        val notes = history.mapNotNull { item ->
            item.correctionJson?.let { PromptBuilder.parseCorrectionJson(it) }
        }.filter { it.hasError }
            .mapNotNull { it.noteTr }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(3)
        return AiSessionSummary(
            strengths = listOf(
                "$userTurns turluk pratik tamamlandı.",
                "Hedef ifadeleri kullanmaya devam ettin.",
            ),
            fixes = notes.ifEmpty {
                listOf("Kısa cümlelerle pratik yapmaya devam et.")
            },
            nextFocus = "Bir sonraki oturumda hedef ifadeleri kendi cümlelerinde kullan.",
            cefrNote = profile.level.uppercase(Locale.ROOT),
        )
    }

    private fun encodeCorrection(correction: CorrectionResult): String = AiJson.encode(
        linkedMapOf(
            "has_error" to correction.hasError,
            "corrected" to correction.corrected,
            "note_tr" to correction.noteTr,
            "severity" to correction.severity,
        ),
    )

    private fun encodeSummary(summary: AiSessionSummary): String = AiJson.encode(
        linkedMapOf(
            "strengths" to summary.strengths,
            "fixes" to summary.fixes,
            "next_focus" to summary.nextFocus,
            "cefr_note" to summary.cefrNote,
        ),
    )
}
