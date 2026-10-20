package com.wendigo.tekrar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wendigo.tekrar.data.Word
import com.wendigo.tekrar.repo.SettingsStore
import com.wendigo.tekrar.repo.WordRepository
import com.wendigo.tekrar.ui.contracts.DashboardUiState
import com.wendigo.tekrar.ui.contracts.SettingsUiState
import com.wendigo.tekrar.ui.contracts.StudyCardUi
import com.wendigo.tekrar.ui.contracts.StudyMode
import com.wendigo.tekrar.ui.contracts.StudyUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun <T : ViewModel> viewModelFactory(create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
    }

class DashboardViewModel(repository: WordRepository) : ViewModel() {

    val state: StateFlow<DashboardUiState> = repository.observeDashboard()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(),
        )
}

class StudyViewModel(
    private val repository: WordRepository,
    private val mode: StudyMode,
) : ViewModel() {

    private val _state = MutableStateFlow(StudyUiState())
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    private val queue = ArrayDeque<Word>()
    private var position = 0
    private var total = 0
    private var busy = false

    init {
        load()
    }

    fun load() {
        if (busy) return
        busy = true
        viewModelScope.launch {
            _state.value = StudyUiState(loading = true)
            val items = repository.loadQueue(mode)
            queue.clear()
            queue.addAll(items)
            total = items.size
            position = 0
            busy = false
            if (items.isEmpty()) {
                _state.value = StudyUiState(loading = false, empty = true, total = 0)
            } else {
                showCurrent()
            }
        }
    }

    fun reveal() {
        _state.update { current ->
            if (current.card == null || current.revealed) current
            else current.copy(revealed = true)
        }
    }

    fun rate(rating: Int) {
        val current = _state.value
        val card = current.card ?: return
        if (!current.revealed || busy) return
        busy = true
        viewModelScope.launch {
            repository.review(card.id, rating)
            queue.removeFirstOrNull()
            busy = false
            showCurrent()
        }
    }

    private fun showCurrent() {
        val word = queue.firstOrNull()
        if (word == null) {
            _state.value = StudyUiState(
                loading = false,
                finished = true,
                position = total,
                total = total,
            )
            return
        }
        position += 1
        _state.value = StudyUiState(
            loading = false,
            card = StudyCardUi(
                id = word.id,
                term = word.term,
                meaning = word.meaning,
                example = word.example,
                notes = word.notes,
            ),
            revealed = false,
            position = position,
            total = total,
            intervalLabels = repository.previewIntervals(word),
        )
    }
}

class AddWordViewModel(private val repository: WordRepository) : ViewModel() {

    fun save(
        term: String,
        meaning: String,
        example: String,
        notes: String,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.addWord(term, meaning, example, notes)
            onSaved()
        }
    }
}

class SettingsViewModel(
    repository: WordRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        settings.newPerDay,
        repository.observeSettingsStats(),
    ) { limit, stats ->
        SettingsUiState(
            newPerDay = limit,
            totalWords = stats.first,
            reviewedToday = stats.second,
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(loading = true),
    )

    fun setNewPerDay(value: Int) {
        viewModelScope.launch {
            settings.setNewPerDay(value)
        }
    }
}
