package com.wendigo.tekrar.ui.ai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendigo.tekrar.R
import com.wendigo.tekrar.ai.AiChatEvent
import com.wendigo.tekrar.ai.AiRepository
import com.wendigo.tekrar.ai.AiSessionProfile
import com.wendigo.tekrar.ai.AiSessionSummary
import com.wendigo.tekrar.ai.AiSettingsStore
import com.wendigo.tekrar.ai.CorrectionResult
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** AI koç ekranının sohbet türü. Veritabanındaki `mode` alanıyla eşleşir. */
enum class AiChatMode(val wire: String, val titleRes: Int) {
    CHAT("chat", R.string.ai_mode_chat),
    ROLEPLAY("roleplay", R.string.ai_mode_roleplay),
    CORRECTION("correction", R.string.ai_mode_correction),
    ;

    companion object {
        fun fromWire(value: String?): AiChatMode {
            val normalized = value?.trim()?.lowercase(Locale.ROOT)
            return entries.firstOrNull { it.wire == normalized } ?: CHAT
        }
    }
}

enum class AiChatRole { USER, ASSISTANT }

/** Bir kullanıcı mesajının düzeltme kartı (AI yanıtından bağımsız gelir). */
data class AiCorrectionUi(
    val original: String,
    val corrected: String,
    val noteTr: String,
    val severity: String? = null,
)

data class AiChatMessageUi(
    val id: Long = 0L,
    val role: AiChatRole,
    val text: String,
    val streaming: Boolean = false,
    val pending: Boolean = false,
    val failed: Boolean = false,
    val correction: AiCorrectionUi? = null,
)

enum class AiChatErrorKind { KEY_MISSING, LIMIT, SUMMARY, GENERIC }

data class AiChatErrorUi(
    val kind: AiChatErrorKind,
    val message: String,
    val retryable: Boolean = false,
)

data class AiSessionSummaryUi(
    val strengths: List<String>,
    val fixes: List<String>,
    val nextFocus: String?,
    val cefrNote: String,
)

/** Oturum sonu özeti. Boş taslak gösterilmez; AI/kural tabanlı özet yoksa hata bandı çıkar. */
data class AiChatUiState(
    val mode: AiChatMode = AiChatMode.CHAT,
    val title: String = "",
    val cefrLevel: String = "A1",
    val settingTr: String? = null,
    val goalTr: String? = null,
    val targetPhrases: List<String> = emptyList(),
    val sessionId: String? = null,
    val messages: List<AiChatMessageUi> = emptyList(),
    val draft: String = "",
    val isStarting: Boolean = false,
    val isStreaming: Boolean = false,
    val isSummarizing: Boolean = false,
    val summary: AiSessionSummaryUi? = null,
    val ended: Boolean = false,
    val error: AiChatErrorUi? = null,
    val hasKey: Boolean = true,
    val autoSpeak: Boolean = false,
    val limitReached: Boolean = false,
) {
    val canSend: Boolean get() = AiChatRules.canSend(this, draft)

    val canEnd: Boolean
        get() = sessionId != null &&
            !isStarting &&
            !isStreaming &&
            !isSummarizing &&
            !ended &&
            summary == null &&
            messages.any { it.role == AiChatRole.USER && it.text.isNotBlank() }
}

/**
 * Sohbet isteği. NavHost, senaryo/rol yapma içeriğinden bu isteği üretir;
 * ViewModel oturumu buna göre açar.
 */
data class AiChatRequest(
    val mode: AiChatMode = AiChatMode.CHAT,
    val title: String = "",
    val unitId: String? = null,
    val scenarioId: String? = null,
    val openingEn: String? = null,
    val profile: AiSessionProfile = AiSessionProfile(),
)

/**
 * Sohbet ekranının durum geçişleri. Android'e bağlı değildir; JVM testlerinde
 * doğrudan kullanılır.
 */
internal object AiChatRules {

    const val MAX_DRAFT_LENGTH = 1200
    const val MAX_SUMMARY_ITEMS = 3

    fun cleanDraft(text: String): String = text.take(MAX_DRAFT_LENGTH)

    fun canSend(state: AiChatUiState, text: String): Boolean =
        text.isNotBlank() &&
            state.sessionId != null &&
            state.hasKey &&
            !state.limitReached &&
            !state.isStarting &&
            !state.isStreaming &&
            !state.isSummarizing &&
            !state.ended &&
            state.summary == null

    fun summaryUi(
        summary: AiSessionSummary?,
        fallbackCefr: String,
    ): AiSessionSummaryUi? {
        if (summary == null) return null
        val strengths = cleanItems(summary.strengths)
        val fixes = cleanItems(summary.fixes)
        val focus = summary.nextFocus?.trim()?.takeIf { it.isNotEmpty() }
        if (strengths.isEmpty() && fixes.isEmpty() && focus == null) return null
        val cefr = summary.cefrNote?.trim()?.takeIf { it.isNotEmpty() }
            ?: fallbackCefr.trim().takeIf { it.isNotEmpty() }
            ?: "A1"
        return AiSessionSummaryUi(
            strengths = strengths,
            fixes = fixes,
            nextFocus = focus,
            cefrNote = cefr.uppercase(Locale.ROOT),
        )
    }

    fun correctionUi(
        result: CorrectionResult?,
        original: String,
    ): AiCorrectionUi? {
        if (result == null || !result.hasError) return null
        val corrected = result.corrected?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val note = result.noteTr?.trim().orEmpty()
        val source = original.trim()
        if (corrected == source && note.isEmpty()) return null
        val severity = result.severity
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it == "low" || it == "medium" || it == "high" }
        return AiCorrectionUi(
            original = source,
            corrected = corrected,
            noteTr = note,
            severity = severity,
        )
    }

    private fun cleanItems(values: List<String>?): List<String> = values.orEmpty()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .take(MAX_SUMMARY_ITEMS)
}

/**
 * AI koç sohbetini yönetir: oturum açma, streaming yanıt, düzeltme kartları ve
 * oturum sonu özeti. Tüm metinler Türkçe; asistan/İngilizce içerik olduğu gibi kalır.
 *
 * @param dailyUsedProvider Bugünün kullanıcı mesajı sayısını döner (nav/DI verir).
 *   Null ise günlük sayaç kapalıdır; yalnız ayarlardaki limit kaydedilir.
 */
class AiChatViewModel(
    private val repository: AiRepository,
    private val settings: AiSettingsStore,
    private val appContext: Context,
    private val dailyUsedProvider: (suspend () -> Int)? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(AiChatUiState(isStarting = true))
    val state: StateFlow<AiChatUiState> = _state.asStateFlow()

    private var request: AiChatRequest = AiChatRequest()
    private var sessionJob: Job? = null
    private var started = false
    private var nextTempId = -1L
    private var dailyUsed: Int? = null
    private var streamToken = 0L

    init {
        viewModelScope.launch {
            settings.state.collect { current ->
                _state.update { st ->
                    val used = dailyUsed
                    st.copy(
                        autoSpeak = current.autoSpeak,
                        limitReached = if (used != null) {
                            used >= current.dailyMessageLimit
                        } else {
                            st.limitReached
                        },
                    )
                }
            }
        }
    }

    /** Oturumu başlatır. Zaten açık bir oturum varsa yeniden kurmaz. */
    fun start(request: AiChatRequest) {
        begin(request, force = false)
    }

    /** Mevcut oturumu kapatıp aynı istekle yeni oturum açar (özet kartındaki "Yeni sohbet"). */
    fun newSession(request: AiChatRequest = this.request) {
        begin(request, force = true)
    }

    fun onDraftChange(text: String) {
        _state.update { it.copy(draft = AiChatRules.cleanDraft(text)) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /** Ayarlardan dönüşte anahtar eklenmişse oturumu açar. NavHost resume'da çağırır. */
    fun refreshKeyState() {
        if (_state.value.hasKey) return
        viewModelScope.launch {
            val hasKey = withContext(Dispatchers.IO) {
                runCatching { repository.hasKey() }.getOrDefault(false)
            }
            if (!hasKey) return@launch
            _state.update { it.copy(hasKey = true) }
            val current = _state.value
            if (current.sessionId == null && !current.isStarting) {
                begin(request, force = true)
            }
        }
    }

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val snapshot = _state.value
        if (snapshot.isStarting || snapshot.isSummarizing || snapshot.ended || snapshot.summary != null) {
            return
        }
        if (!snapshot.hasKey) {
            _state.update { it.copy(error = keyMissingError()) }
            return
        }
        if (snapshot.limitReached) {
            _state.update { it.copy(error = limitError()) }
            return
        }
        if (snapshot.sessionId == null) return
        if (snapshot.isStreaming) {
            stopStreaming(markFailed = true)
        } else {
            cancelPendingWork()
        }
        dispatch(clean, appendUser = true)
    }

    /**
     * Son kullanıcı mesajını yeniden gönderir. UI'da yeni kullanıcı balonu
     * eklenmez; son hatalı/boş asistan balonu kaldırılır.
     */
    fun retryLast() {
        val snapshot = _state.value
        if (snapshot.isStarting || snapshot.isSummarizing || snapshot.ended || snapshot.summary != null) {
            return
        }
        if (snapshot.sessionId == null) {
            begin(request, force = true)
            return
        }
        if (snapshot.isStreaming) return
        if (!snapshot.hasKey) {
            _state.update { it.copy(error = keyMissingError()) }
            return
        }
        if (snapshot.limitReached) {
            _state.update { it.copy(error = limitError()) }
            return
        }
        val userIndex = snapshot.messages.indexOfLast {
            it.role == AiChatRole.USER && it.text.isNotBlank()
        }
        if (userIndex < 0) return
        val failedTail = snapshot.messages.drop(userIndex + 1).any { it.failed || it.streaming }
        if (snapshot.error == null && !failedTail) return
        val lastText = snapshot.messages[userIndex].text
        _state.update {
            it.copy(
                messages = it.messages.take(userIndex + 1),
                error = null,
            )
        }
        dispatch(lastText, appendUser = false)
    }

    /**
     * Oturumu bitirir ve özet ister. Özet alınamazsa yerel/boş özet gösterilmez;
     * kullanıcıya hata bandı ve tekrar deneme sunulur.
     */
    fun endSession() {
        val snapshot = _state.value
        if (snapshot.isSummarizing || snapshot.summary != null) return
        val sessionId = snapshot.sessionId ?: return
        if (snapshot.messages.none { it.role == AiChatRole.USER }) return
        if (snapshot.isStreaming) {
            stopStreaming(markFailed = true)
        } else {
            cancelPendingWork()
        }
        _state.update { it.copy(isSummarizing = true, error = null) }
        sessionJob = viewModelScope.launch {
            try {
                val raw = repository.summarize(sessionId, request.profile)
                val summary = AiChatRules.summaryUi(raw, request.profile.level)
                _state.update { st ->
                    if (summary == null) {
                        st.copy(
                            isSummarizing = false,
                            ended = true,
                            error = summaryError(),
                        )
                    } else {
                        st.copy(
                            isSummarizing = false,
                            ended = true,
                            summary = summary,
                            error = null,
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        isSummarizing = false,
                        ended = true,
                        error = summaryError(),
                    )
                }
            }
        }
    }

    fun close() {
        streamToken += 1
        sessionJob?.cancel()
        sessionJob = null
        _state.update { it.copy(isStreaming = false, isSummarizing = false) }
    }

    private fun begin(request: AiChatRequest, force: Boolean) {
        if (!force && started) {
            this.request = request
            return
        }
        started = true
        streamToken += 1
        sessionJob?.cancel()
        sessionJob = null
        this.request = request
        dailyUsed = null
        val title = request.title.trim().ifBlank {
            appContext.getString(request.mode.titleRes)
        }
        _state.value = AiChatUiState(
            mode = request.mode,
            title = title,
            cefrLevel = request.profile.level.trim().uppercase(Locale.ROOT)
                .ifBlank { "A1" },
            settingTr = request.profile.settingTr?.trim()?.takeIf { it.isNotEmpty() },
            goalTr = request.profile.goalTr?.trim()?.takeIf { it.isNotEmpty() },
            targetPhrases = request.profile.targetPhrases
                .map { it.trim() }
                .filter { it.isNotEmpty() },
            isStarting = true,
            autoSpeak = settings.state.value.autoSpeak,
            hasKey = true,
        )
        sessionJob = viewModelScope.launch {
            val hasKey = withContext(Dispatchers.IO) {
                runCatching { repository.hasKey() }.getOrDefault(false)
            }
            if (!hasKey) {
                _state.update { it.copy(isStarting = false, hasKey = false) }
                return@launch
            }
            val used = readDailyUsed()
            dailyUsed = used
            if (used != null && used >= settings.state.value.dailyMessageLimit) {
                _state.update {
                    it.copy(limitReached = true, error = limitError())
                }
            }
            val sessionTitle = _state.value.title
            val created = try {
                Result.success(
                    repository.createSession(
                        mode = request.mode.wire,
                        title = sessionTitle,
                        unitId = request.unitId,
                        scenarioId = request.scenarioId,
                        openingEn = request.openingEn,
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
            created.fold(
                onSuccess = { result ->
                    _state.update { st ->
                        st.copy(
                            sessionId = result.session.id,
                            isStarting = false,
                            hasKey = true,
                            messages = result.openingMessage?.let { opening ->
                                listOf(
                                    AiChatMessageUi(
                                        id = opening.id,
                                        role = AiChatRole.ASSISTANT,
                                        text = opening.content,
                                    ),
                                )
                            }.orEmpty(),
                        )
                    }
                },
                onFailure = {
                    _state.update { st ->
                        st.copy(
                            isStarting = false,
                            hasKey = true,
                            error = AiChatErrorUi(
                                kind = AiChatErrorKind.GENERIC,
                                message = appContext.getString(R.string.ai_chat_error_start),
                                retryable = true,
                            ),
                        )
                    }
                },
            )
        }
    }

    private fun dispatch(text: String, appendUser: Boolean) {
        val sessionId = _state.value.sessionId ?: return
        val userTempId = if (appendUser) nextTempId-- else 0L
        val assistantTempId = nextTempId--
        _state.update { st ->
            val messages = if (appendUser) {
                st.messages + AiChatMessageUi(
                    id = userTempId,
                    role = AiChatRole.USER,
                    text = text,
                    pending = true,
                )
            } else {
                val index = st.messages.indexOfLast { it.role == AiChatRole.USER }
                if (index < 0) {
                    st.messages
                } else {
                    st.messages.toMutableList().also { list ->
                        list[index] = list[index].copy(
                            pending = true,
                            failed = false,
                            correction = null,
                        )
                    }
                }
            }
            st.copy(
                messages = messages + AiChatMessageUi(
                    id = assistantTempId,
                    role = AiChatRole.ASSISTANT,
                    text = "",
                    pending = true,
                    streaming = true,
                ),
                draft = "",
                isStreaming = true,
                error = null,
            )
        }
        var userSaved = false
        val token = ++streamToken
        sessionJob = viewModelScope.launch {
            try {
                repository.send(sessionId, text, request.profile).collect { event ->
                    when (event) {
                        is AiChatEvent.UserSaved -> {
                            userSaved = true
                            if (appendUser) {
                                _state.update { st ->
                                    st.copy(
                                        messages = st.messages.map { message ->
                                            if (message.id == userTempId) {
                                                message.copy(id = event.messageId, pending = false)
                                            } else {
                                                message
                                            }
                                        },
                                    )
                                }
                            } else {
                                _state.update { st ->
                                    val index = st.messages.indexOfLast {
                                        it.role == AiChatRole.USER
                                    }
                                    if (index < 0) {
                                        st
                                    } else {
                                        st.copy(
                                            messages = st.messages.toMutableList().also { list ->
                                                list[index] = list[index].copy(
                                                    id = event.messageId,
                                                    pending = false,
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                            val usedNow = (dailyUsed ?: 0) + 1
                            dailyUsed = usedNow
                            val limit = settings.state.value.dailyMessageLimit
                            if (usedNow >= limit) {
                                _state.update {
                                    it.copy(limitReached = true, error = limitError())
                                }
                            }
                        }

                        is AiChatEvent.Delta -> _state.update { st ->
                            st.copy(
                                messages = st.messages.map { message ->
                                    if (message.id == assistantTempId) {
                                        message.copy(text = message.text + event.text)
                                    } else {
                                        message
                                    }
                                },
                            )
                        }

                        is AiChatEvent.AssistantSaved -> _state.update { st ->
                            st.copy(
                                // Ana yanıt tamamlandı; düzeltme çağrısı arka planda
                                // sürerken kullanıcı yeni tur başlatabilir.
                                isStreaming = false,
                                messages = st.messages.map { message ->
                                    if (message.id == assistantTempId) {
                                        message.copy(
                                            id = event.messageId,
                                            text = event.text,
                                            streaming = false,
                                            pending = false,
                                            failed = false,
                                        )
                                    } else {
                                        message
                                    }
                                },
                            )
                        }

                        is AiChatEvent.CorrectionReady -> {
                            val correction = AiChatRules.correctionUi(event.correction, text)
                            if (correction != null) {
                                _state.update { st ->
                                    st.copy(
                                        messages = st.messages.map { message ->
                                            if (message.id == event.messageId) {
                                                message.copy(correction = correction)
                                            } else {
                                                message
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        is AiChatEvent.Completed -> _state.update { st ->
                            st.copy(
                                isStreaming = false,
                                messages = st.messages.map { message ->
                                    if (message.streaming) {
                                        message.copy(
                                            streaming = false,
                                            pending = false,
                                        )
                                    } else {
                                        message
                                    }
                                },
                            )
                        }

                        is AiChatEvent.Error -> _state.update { st ->
                            st.copy(
                                isStreaming = false,
                                error = AiChatErrorUi(
                                    kind = AiChatErrorKind.GENERIC,
                                    message = event.messageTr,
                                    retryable = event.retryable,
                                ),
                                messages = st.messages.mapNotNull { message ->
                                    when {
                                        message.id == assistantTempId -> {
                                            if (message.text.isBlank()) {
                                                null
                                            } else {
                                                message.copy(
                                                    streaming = false,
                                                    pending = false,
                                                    failed = true,
                                                )
                                            }
                                        }

                                        message.role == AiChatRole.USER &&
                                            message.pending &&
                                            !userSaved -> message.copy(
                                            pending = false,
                                            failed = true,
                                        )

                                        else -> message
                                    }
                                },
                            )
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { st ->
                    st.copy(
                        isStreaming = false,
                        error = AiChatErrorUi(
                            kind = AiChatErrorKind.GENERIC,
                            message = appContext.getString(R.string.ai_chat_error_generic),
                            retryable = true,
                        ),
                        messages = st.messages
                            .mapNotNull { message ->
                                when {
                                    message.id == assistantTempId -> {
                                        if (message.text.isBlank()) {
                                            null
                                        } else {
                                            message.copy(
                                                streaming = false,
                                                pending = false,
                                                failed = true,
                                            )
                                        }
                                    }

                                    else -> message
                                }
                            },
                    )
                }
            } finally {
                if (token == streamToken) {
                    _state.update { st ->
                        st.copy(
                            isStreaming = false,
                            messages = st.messages
                                .map { message ->
                                    if (message.streaming) {
                                        message.copy(streaming = false, pending = false)
                                    } else {
                                        message
                                    }
                                }
                                .filterNot { it.id == assistantTempId && it.text.isBlank() },
                        )
                    }
                }
            }
        }
    }

    private fun cancelPendingWork() {
        streamToken += 1
        sessionJob?.cancel()
        sessionJob = null
    }

    private fun stopStreaming(markFailed: Boolean) {
        cancelPendingWork()
        _state.update { st ->
            st.copy(
                isStreaming = false,
                messages = st.messages
                    .map { message ->
                        if (message.streaming) {
                            message.copy(
                                streaming = false,
                                pending = false,
                                failed = markFailed,
                            )
                        } else {
                            message
                        }
                    }
                    .filterNot {
                        it.role == AiChatRole.ASSISTANT && it.failed && it.text.isBlank()
                    },
            )
        }
    }

    private suspend fun readDailyUsed(): Int? {
        val provider = dailyUsedProvider ?: return null
        return try {
            provider()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            null
        }
    }

    private fun keyMissingError(): AiChatErrorUi = AiChatErrorUi(
        kind = AiChatErrorKind.KEY_MISSING,
        message = appContext.getString(R.string.ai_chat_error_key_missing),
        retryable = false,
    )

    private fun limitError(): AiChatErrorUi = AiChatErrorUi(
        kind = AiChatErrorKind.LIMIT,
        message = appContext.getString(
            R.string.ai_chat_error_limit,
            settings.state.value.dailyMessageLimit,
        ),
        retryable = false,
    )

    private fun summaryError(): AiChatErrorUi = AiChatErrorUi(
        kind = AiChatErrorKind.SUMMARY,
        message = appContext.getString(R.string.ai_chat_error_summary),
        retryable = true,
    )
}
