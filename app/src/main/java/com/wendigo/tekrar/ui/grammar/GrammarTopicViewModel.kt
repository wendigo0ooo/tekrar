package com.wendigo.tekrar.ui.grammar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendigo.tekrar.ai.AiRepository
import com.wendigo.tekrar.ai.GrammarAiResult
import com.wendigo.tekrar.audio.Speaker
import com.wendigo.tekrar.core.grammar.GrammarCheckResult
import com.wendigo.tekrar.core.grammar.GrammarSession
import com.wendigo.tekrar.core.grammar.GrammarSessionSummary
import com.wendigo.tekrar.core.grammar.GrammarStep
import com.wendigo.tekrar.core.grammar.GrammarTopic
import com.wendigo.tekrar.core.grammar.GrammarWriteTask
import com.wendigo.tekrar.repo.GrammarProgressRepository
import com.wendigo.tekrar.repo.GrammarRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GrammarPhase {
    EXPLAIN,
    WRITE,
    SUMMARY,
}

data class GrammarTopicUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val topicId: String = "",
    val title: String = "",
    val subtitle: String = "",
    val phase: GrammarPhase = GrammarPhase.EXPLAIN,
    val stepIndex: Int = 0,
    val stepCount: Int = 0,
    val step: GrammarStep? = null,
    val taskIndex: Int = 0,
    val taskCount: Int = 0,
    val task: GrammarWriteTask? = null,
    val writeIntro: String = "",
    val input: String = "",
    val result: GrammarCheckResult? = null,
    val aiAvailable: Boolean = false,
    val aiLoading: Boolean = false,
    val aiResult: GrammarAiResult? = null,
    val aiFailed: Boolean = false,
    val summary: GrammarSessionSummary? = null,
    val saveFailed: Boolean = false,
)

/**
 * Konu ekranı: anlatım kartları -> 3 yazma görevi -> özet.
 *
 * [GrammarSession] ViewModel'da tutulur; ekran döndürmede oturum kaybolmaz.
 * "Tekrar dene" [GrammarSession.retry], "Sonraki" [GrammarSession.next] çağırır;
 * böylece kontrol sonuçları yalnız görev geçilirken kaydedilir.
 */
class GrammarTopicViewModel(
    private val grammarRepository: GrammarRepository,
    private val grammarProgress: GrammarProgressRepository,
    private val aiRepository: AiRepository,
    private val speaker: Speaker,
) : ViewModel() {

    private val _state = MutableStateFlow(GrammarTopicUiState())
    val state: StateFlow<GrammarTopicUiState> = _state.asStateFlow()

    private var topic: GrammarTopic? = null
    private var session: GrammarSession? = null
    private var loadedTopicId: String = ""
    private var lastTopicId: String = ""
    private var loadJob: Job? = null
    private var aiJob: Job? = null
    private var completeRequested = false

    fun start(topicId: String) {
        if (topicId.isBlank()) {
            _state.value = GrammarTopicUiState(loading = false, error = true)
            return
        }
        // Döndürme/tema değişiminde oturumu sıfırlama.
        if (topicId == loadedTopicId && session != null) return
        lastTopicId = topicId
        loadJob?.cancel()
        aiJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = GrammarTopicUiState(loading = true)
            topic = null
            session = null
            loadedTopicId = ""
            completeRequested = false
            try {
                val loaded = withContext(Dispatchers.IO) {
                    grammarRepository.loadTopic(topicId)
                }
                if (loaded == null) {
                    _state.value = GrammarTopicUiState(loading = false, error = true)
                    return@launch
                }
                val checker = withContext(Dispatchers.IO) { grammarRepository.checker() }
                val activeSession = GrammarSession(loaded, checker)
                topic = loaded
                session = activeSession
                loadedTopicId = topicId
                try {
                    grammarProgress.markOpened(topicId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    // İlerleme yazılamasa da konu açılmaya devam eder.
                }
                _state.value = initialState(loaded, activeSession)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                _state.value = GrammarTopicUiState(loading = false, error = true)
            }
        }
    }

    fun retryLoad() {
        val id = lastTopicId
        if (id.isBlank()) return
        loadedTopicId = ""
        session = null
        start(id)
    }

    fun continueExplain() {
        val current = topic ?: return
        val state = _state.value
        if (state.phase != GrammarPhase.EXPLAIN) return
        val next = state.stepIndex + 1
        if (next < current.steps.size) {
            _state.value = state.copy(
                stepIndex = next,
                step = current.steps[next],
            )
        } else {
            enterWrite()
        }
    }

    fun onInputChange(text: String) {
        val state = _state.value
        if (state.phase != GrammarPhase.WRITE || state.result != null) return
        _state.value = state.copy(input = text)
    }

    fun check() {
        val state = _state.value
        val active = session ?: return
        if (state.phase != GrammarPhase.WRITE || state.result != null) return
        if (state.input.isBlank()) return
        val result = active.check(state.input) ?: return
        aiJob?.cancel()
        _state.value = state.copy(
            result = result,
            aiLoading = false,
            aiResult = null,
            aiFailed = false,
        )
    }

    fun retry() {
        val state = _state.value
        if (state.phase != GrammarPhase.WRITE || state.result == null) return
        aiJob?.cancel()
        session?.retry()
        _state.value = state.copy(
            result = null,
            aiLoading = false,
            aiResult = null,
            aiFailed = false,
        )
    }

    fun next() {
        val state = _state.value
        val active = session ?: return
        if (state.phase != GrammarPhase.WRITE || state.result == null) return
        aiJob?.cancel()
        val finished = active.next()
        if (finished) {
            enterSummary()
            return
        }
        _state.value = state.copy(
            taskIndex = active.currentIndex,
            taskCount = active.taskCount,
            task = active.currentTask,
            input = "",
            result = null,
            aiLoading = false,
            aiResult = null,
            aiFailed = false,
        )
    }

    fun aiCheck() {
        val state = _state.value
        val current = topic ?: return
        val local = state.result ?: return
        if (state.phase != GrammarPhase.WRITE || !state.aiAvailable) return
        if (state.aiLoading) return
        aiJob?.cancel()
        _state.value = state.copy(aiLoading = true, aiFailed = false)
        val text = local.original
        aiJob = viewModelScope.launch {
            val outcome: Result<GrammarAiResult> = try {
                aiRepository.checkGrammar(text, current.titleTr, current.cefr)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                Result.failure(failure)
            }
            val latest = _state.value
            if (latest.phase != GrammarPhase.WRITE || latest.result?.original != text) {
                return@launch
            }
            outcome.fold(
                onSuccess = { ai ->
                    _state.update {
                        it.copy(aiLoading = false, aiResult = ai, aiFailed = false)
                    }
                },
                onFailure = {
                    _state.update {
                        it.copy(aiLoading = false, aiResult = null, aiFailed = true)
                    }
                },
            )
        }
    }

    fun retryComplete() {
        val summary = _state.value.summary ?: return
        ensureCompleted(summary.averageScore)
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        speaker.speak(text)
    }

    override fun onCleared() {
        aiJob?.cancel()
        loadJob?.cancel()
        speaker.shutdown()
        super.onCleared()
    }

    private fun initialState(
        loaded: GrammarTopic,
        activeSession: GrammarSession,
    ): GrammarTopicUiState {
        val steps = loaded.steps
        val base = GrammarTopicUiState(
            loading = false,
            error = false,
            topicId = loaded.id,
            title = loaded.titleTr,
            subtitle = loaded.subtitleTr,
            phase = if (steps.isEmpty()) GrammarPhase.WRITE else GrammarPhase.EXPLAIN,
            stepIndex = 0,
            stepCount = steps.size,
            step = steps.firstOrNull(),
            taskIndex = activeSession.currentIndex,
            taskCount = activeSession.taskCount,
            task = activeSession.currentTask,
            writeIntro = loaded.write.introTr,
            aiAvailable = runCatching { aiRepository.hasKey() }.getOrDefault(false),
        )
        return if (activeSession.isFinished) {
            base.copy(
                phase = GrammarPhase.SUMMARY,
                step = null,
                task = null,
                summary = activeSession.summary(),
            )
        } else {
            base
        }
    }

    private fun enterWrite() {
        val active = session ?: return
        val task = active.currentTask
        if (task == null) {
            enterSummary()
            return
        }
        aiJob?.cancel()
        _state.value = _state.value.copy(
            phase = GrammarPhase.WRITE,
            step = null,
            taskIndex = active.currentIndex,
            taskCount = active.taskCount,
            task = task,
            input = "",
            result = null,
            aiLoading = false,
            aiResult = null,
            aiFailed = false,
        )
    }

    private fun enterSummary() {
        val active = session ?: return
        val summary = active.summary()
        aiJob?.cancel()
        _state.value = _state.value.copy(
            phase = GrammarPhase.SUMMARY,
            step = null,
            task = null,
            result = null,
            input = "",
            summary = summary,
            aiLoading = false,
            aiResult = null,
            aiFailed = false,
        )
        ensureCompleted(summary.averageScore)
    }

    private fun ensureCompleted(score: Int) {
        if (completeRequested) return
        val id = loadedTopicId
        if (id.isBlank()) return
        completeRequested = true
        viewModelScope.launch {
            try {
                // Kullanıcı özetten hemen çıksa bile tamamlama yazısı bitmeli.
                withContext(NonCancellable) {
                    grammarProgress.complete(id, score)
                }
                _state.update { it.copy(saveFailed = false) }
            } catch (failure: Throwable) {
                completeRequested = false
                _state.update { it.copy(saveFailed = true) }
            }
        }
    }
}
