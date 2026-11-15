package com.wendigo.tekrar.ui.grammar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendigo.tekrar.core.grammar.GrammarTopicRef
import com.wendigo.tekrar.core.learn.LessonStatus
import com.wendigo.tekrar.repo.GrammarProgressRepository
import com.wendigo.tekrar.repo.GrammarRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ana ekrandaki üç bölüm (kontrat 6): konu sırasına göre 1-5, 6-10, 11-16. */
enum class GrammarSection {
    BASICS,
    MEETING,
    ROUTINE,
}

data class GrammarTopicCardUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val estMin: Int,
    val colorHex: String?,
    val status: LessonStatus,
)

data class GrammarSectionUi(
    val section: GrammarSection,
    val topics: List<GrammarTopicCardUi>,
)

data class GrammarHomeUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val sections: List<GrammarSectionUi> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val totalEstMin: Int = 0,
    val nextTopicId: String? = null,
    val nextTopicTitle: String = "",
    val allCompleted: Boolean = false,
)

/**
 * Gramer ana ekranı: index sırası + `grammar:<topicId>` ilerlemesi.
 *
 * İlerleme Room'da tutulduğu için ekran geri geldiğinde [refresh] çağrılmalıdır
 * (Öğren ekranındaki `LifecycleResumeEffect` deseniyle aynı).
 */
class GrammarHomeViewModel(
    private val grammarRepository: GrammarRepository,
    private val grammarProgress: GrammarProgressRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GrammarHomeUiState())
    val state: StateFlow<GrammarHomeUiState> = _state.asStateFlow()

    private var refs: List<GrammarTopicRef> = emptyList()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() {
        load()
    }

    /** Konu tamamlandıktan sonra ekrana dönüldüğünde ilerlemeyi tazeler. */
    fun refresh() {
        val current = refs
        if (current.isEmpty() || _state.value.loading) return
        viewModelScope.launch {
            val statuses = try {
                withContext(Dispatchers.IO) { grammarProgress.statusMap() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                return@launch
            }
            _state.value = buildState(current, statuses)
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = false) }
            try {
                val index = withContext(Dispatchers.IO) { grammarRepository.loadIndex() }
                val statuses: Map<String, LessonStatus> = try {
                    withContext(Dispatchers.IO) { grammarProgress.statusMap() }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Throwable) {
                    emptyMap()
                }
                refs = index.topics
                _state.value = buildState(index.topics, statuses)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                refs = emptyList()
                _state.value = GrammarHomeUiState(loading = false, error = true)
            }
        }
    }

    private fun buildState(
        topics: List<GrammarTopicRef>,
        statuses: Map<String, LessonStatus>,
    ): GrammarHomeUiState {
        val cards = topics.map { ref ->
            GrammarTopicCardUi(
                id = ref.id,
                title = ref.titleTr,
                subtitle = ref.subtitleTr,
                estMin = ref.estMin,
                colorHex = ref.color,
                status = statuses[ref.id] ?: LessonStatus.AVAILABLE,
            )
        }
        val completed = cards.count { isDone(it.status) }
        val next = cards.firstOrNull { !isDone(it.status) } ?: cards.firstOrNull()
        val sections = GrammarSection.entries.mapNotNull { section ->
            val sectionTopics = cards.filterIndexed { index, _ ->
                sectionOf(index) == section
            }
            if (sectionTopics.isEmpty()) {
                null
            } else {
                GrammarSectionUi(section = section, topics = sectionTopics)
            }
        }
        return GrammarHomeUiState(
            loading = false,
            error = false,
            sections = sections,
            completedCount = completed,
            totalCount = cards.size,
            totalEstMin = topics.sumOf { it.estMin },
            nextTopicId = next?.id,
            nextTopicTitle = next?.title.orEmpty(),
            allCompleted = cards.isNotEmpty() && completed == cards.size,
        )
    }

    private fun isDone(status: LessonStatus): Boolean =
        status == LessonStatus.COMPLETED || status == LessonStatus.MASTERED

    private fun sectionOf(index: Int): GrammarSection = when {
        index < 5 -> GrammarSection.BASICS
        index < 10 -> GrammarSection.MEETING
        else -> GrammarSection.ROUTINE
    }
}
