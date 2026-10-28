package com.wendigo.tekrar.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wendigo.tekrar.R
import com.wendigo.tekrar.core.AnswerCheck
import com.wendigo.tekrar.core.Cloze
import com.wendigo.tekrar.core.LeechPolicy
import com.wendigo.tekrar.core.SessionSummary
import com.wendigo.tekrar.core.Srs
import com.wendigo.tekrar.core.StudySession
import com.wendigo.tekrar.data.Word
import com.wendigo.tekrar.notify.Notifications
import com.wendigo.tekrar.notify.ReminderScheduler
import com.wendigo.tekrar.repo.BackupManager
import com.wendigo.tekrar.repo.FileIo
import com.wendigo.tekrar.repo.SettingsStore
import com.wendigo.tekrar.repo.WordRepository
import com.wendigo.tekrar.repo.WordWriteResult
import com.wendigo.tekrar.ui.contracts.DashboardUiState
import com.wendigo.tekrar.ui.contracts.LeechesUiState
import com.wendigo.tekrar.ui.contracts.LibraryFilter
import com.wendigo.tekrar.ui.contracts.LibrarySort
import com.wendigo.tekrar.ui.contracts.LibraryUiState
import com.wendigo.tekrar.ui.contracts.SettingsUiState
import com.wendigo.tekrar.ui.contracts.StatsUiState
import com.wendigo.tekrar.ui.contracts.StudyCardUi
import com.wendigo.tekrar.ui.contracts.StudyFormat
import com.wendigo.tekrar.ui.contracts.StudyMode
import com.wendigo.tekrar.ui.contracts.StudyUiState
import com.wendigo.tekrar.ui.contracts.WordEditorUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

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

class StatsViewModel(repository: WordRepository) : ViewModel() {

    val state: StateFlow<StatsUiState> = repository.observeStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState(),
        )
}

class LibraryViewModel(repository: WordRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(LibraryFilter.ALL)
    private val sort = MutableStateFlow(LibrarySort.DUE)

    val state: StateFlow<LibraryUiState> = combine(
        repository.observeLibrary(),
        query,
        filter,
        sort,
    ) { words, currentQuery, currentFilter, currentSort ->
        LibraryUiState(
            loading = false,
            words = applyFilters(words, currentQuery, currentFilter, currentSort),
            query = currentQuery,
            filter = currentFilter,
            sort = currentSort,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(),
    )

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: LibraryFilter) {
        filter.value = value
    }

    fun setSort(value: LibrarySort) {
        sort.value = value
    }

    private fun applyFilters(
        words: List<Word>,
        query: String,
        filter: LibraryFilter,
        sort: LibrarySort,
    ): List<Word> {
        val now = System.currentTimeMillis()
        val needle = query.trim().lowercase(Locale.ROOT)
        val filtered = words.asSequence()
            .filter { word ->
                when (filter) {
                    LibraryFilter.ALL -> true
                    LibraryFilter.DUE -> !word.suspended && !word.isNew && word.dueAt <= now
                    LibraryFilter.NEW -> !word.suspended && word.isNew
                    LibraryFilter.FAVORITE -> word.favorite
                    LibraryFilter.SUSPENDED -> word.suspended
                    LibraryFilter.LEECH -> LeechPolicy.isLeech(word)
                }
            }
            .filter { word ->
                needle.isEmpty() || haystack(word).contains(needle)
            }
        val sorted = when (sort) {
            LibrarySort.DUE -> filtered.sortedWith(
                compareBy<Word> { it.suspended }
                    .thenBy { it.dueAt }
                    .thenBy { it.term.lowercase(Locale.ROOT) }
            )
            LibrarySort.ALPHABETICAL -> filtered.sortedBy { it.term.lowercase(Locale.ROOT) }
            LibrarySort.NEWEST -> filtered.sortedByDescending { it.createdAt }
            LibrarySort.HARDEST -> filtered.sortedWith(
                compareByDescending<Word> { it.lapses }
                    .thenByDescending { it.difficulty }
            )
        }
        return sorted.toList()
    }

    private fun haystack(word: Word): String = buildString {
        append(word.term.lowercase(Locale.ROOT))
        append(' ')
        append(word.meaning.lowercase(Locale.ROOT))
        append(' ')
        append(word.tags.lowercase(Locale.ROOT))
        append(' ')
        append(word.notes.lowercase(Locale.ROOT))
        append(' ')
        append(word.example.lowercase(Locale.ROOT))
    }
}

class LeechesViewModel(private val repository: WordRepository) : ViewModel() {

    val state: StateFlow<LeechesUiState> = repository.observeLibrary()
        .map { words ->
            LeechesUiState(loading = false, words = LeechPolicy.leeches(words))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LeechesUiState(),
        )

    fun suspendWord(word: Word) {
        viewModelScope.launch {
            repository.setSuspended(word.id, true)
        }
    }
}

class WordDetailViewModel(
    private val repository: WordRepository,
    id: Long,
) : ViewModel() {

    val word: StateFlow<Word?> = repository.observeWord(id)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun toggleFavorite(current: Word) {
        viewModelScope.launch {
            repository.setFavorite(current.id, !current.favorite)
        }
    }

    fun toggleSuspended(current: Word) {
        viewModelScope.launch {
            repository.setSuspended(current.id, !current.suspended)
        }
    }

    fun resetProgress(current: Word) {
        viewModelScope.launch {
            repository.resetProgress(current.id)
        }
    }

    fun delete(current: Word, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteWord(current.id)
            onDeleted()
        }
    }
}

class WordEditorViewModel(
    private val repository: WordRepository,
    private val id: Long?,
) : ViewModel() {

    private val _state = MutableStateFlow(
        WordEditorUiState(loading = id != null, isEdit = id != null)
    )
    val state: StateFlow<WordEditorUiState> = _state.asStateFlow()

    init {
        if (id != null) {
            viewModelScope.launch {
                val word = repository.observeWord(id).first()
                _state.value = WordEditorUiState(loading = false, word = word, isEdit = true)
            }
        }
    }

    fun save(
        term: String,
        meaning: String,
        example: String,
        exampleTr: String,
        notes: String,
        tags: String,
        pos: String,
        onSaved: () -> Unit,
    ) {
        if (_state.value.saving) return
        _state.update {
            it.copy(saving = true, duplicateTerm = false, saveFailed = false)
        }
        viewModelScope.launch {
            val result = runCatching {
                if (id == null) {
                    repository.addWord(term, meaning, example, exampleTr, notes, tags, pos)
                } else {
                    repository.updateWord(id, term, meaning, example, exampleTr, notes, tags, pos)
                }
            }
            _state.update { it.copy(saving = false) }
            result.fold(
                onSuccess = { outcome ->
                    when (outcome) {
                        is WordWriteResult.Saved -> onSaved()
                        WordWriteResult.DuplicateTerm ->
                            _state.update { it.copy(duplicateTerm = true) }
                        WordWriteResult.NotFound -> onSaved()
                    }
                },
                onFailure = {
                    _state.update { it.copy(saveFailed = true) }
                },
            )
        }
    }

    fun clearDuplicateError() {
        _state.update { if (it.duplicateTerm) it.copy(duplicateTerm = false) else it }
    }

    fun delete(onDeleted: () -> Unit) {
        val current = id ?: return
        viewModelScope.launch {
            repository.deleteWord(current)
            onDeleted()
        }
    }
}

class StudyViewModel(
    private val repository: WordRepository,
    private val mode: StudyMode,
    val format: StudyFormat,
) : ViewModel() {

    private val _state = MutableStateFlow(StudyUiState(format = format))
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    private val session = StudySession()
    private val attempts = HashMap<Long, Int>()
    private var pool: List<Word> = emptyList()
    private var initialTotal = 0
    private var requeues = 0
    private var done = 0
    private var againCount = 0
    private var correctCount = 0
    private var strengthenedCount = 0
    private val againWords = LinkedHashMap<Long, Pair<Word, Int>>()
    private var startedAt = 0L
    private var lastUndo: Undo? = null
    private var pendingRating: Int? = null
    private var pendingWord: Word? = null

    private data class Undo(
        val word: Word,
        val logId: Long,
        val rating: Int,
        val requeued: Boolean,
    )

    init {
        load()
    }

    fun load() {
        if (!session.occupy()) return
        viewModelScope.launch {
            try {
                _state.value = StudyUiState(loading = true, format = format)
                pool = runCatching { repository.allWords() }.getOrDefault(emptyList())
                val items = runCatching { repository.loadQueue(mode) }.getOrDefault(emptyList())
                session.load(items)
                initialTotal = items.size
                requeues = 0
                done = 0
                againCount = 0
                correctCount = 0
                strengthenedCount = 0
                againWords.clear()
                attempts.clear()
                lastUndo = null
                pendingRating = null
                pendingWord = null
                startedAt = System.currentTimeMillis()
                if (items.isEmpty()) {
                    _state.value = StudyUiState(
                        loading = false,
                        empty = true,
                        format = format,
                    )
                } else {
                    showCurrent()
                }
            } finally {
                session.release()
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
        if (format != StudyFormat.FLASHCARD) return
        val current = _state.value
        if (!current.revealed || current.card == null) return
        val word = session.begin(current.card.id) ?: return
        record(word, rating)
    }

    fun choose(option: String) {
        if (session.isBusy) return
        if (format != StudyFormat.CHOICE && format != StudyFormat.LISTEN) return
        val current = _state.value
        if (current.answered || current.card == null) return
        val word = session.current ?: return
        if (word.id != current.card.id) return
        val correct = option == current.card.meaning
        pendingWord = word
        pendingRating = if (correct) Srs.GOOD else Srs.AGAIN
        _state.update {
            it.copy(
                answered = true,
                answerCorrect = correct,
                selectedOption = option,
                revealed = true,
            )
        }
    }

    fun submitTyping(value: String) {
        if (session.isBusy) return
        if (format != StudyFormat.TYPING && format != StudyFormat.CLOZE) return
        val current = _state.value
        if (current.answered || current.card == null) return
        val word = session.current ?: return
        if (word.id != current.card.id) return
        val expected = if (format == StudyFormat.CLOZE) {
            Cloze.build(current.card.example, current.card.term)?.answer ?: current.card.term
        } else {
            current.card.term
        }
        val correct = AnswerCheck.matches(value, expected)
        pendingWord = word
        pendingRating = if (correct) Srs.GOOD else Srs.AGAIN
        _state.update {
            it.copy(
                answered = true,
                answerCorrect = correct,
                typedAnswer = value,
                revealed = true,
            )
        }
    }

    fun continueAfterAnswer() {
        val word = pendingWord ?: return
        val rating = pendingRating ?: Srs.GOOD
        val started = session.begin(word.id) ?: return
        pendingWord = null
        pendingRating = null
        record(started, rating)
    }

    fun undo() {
        val undo = lastUndo ?: return
        if (!session.occupy()) return
        pendingWord = null
        pendingRating = null
        viewModelScope.launch {
            try {
                runCatching { repository.undoReview(undo.logId, undo.word) }
                session.undo(undo.word)
                done = (done - 1).coerceAtLeast(0)
                if (undo.rating == Srs.AGAIN) {
                    againCount = (againCount - 1).coerceAtLeast(0)
                    val previous = againWords[undo.word.id]
                    if (previous != null) {
                        if (previous.second <= 1) {
                            againWords.remove(undo.word.id)
                        } else {
                            againWords[undo.word.id] = previous.first to (previous.second - 1)
                        }
                    }
                    if (undo.requeued && requeues > 0) requeues--
                } else {
                    correctCount = (correctCount - 1).coerceAtLeast(0)
                    if (undo.rating == Srs.GOOD || undo.rating == Srs.EASY) {
                        strengthenedCount = (strengthenedCount - 1).coerceAtLeast(0)
                    }
                }
                if (undo.requeued) {
                    attempts[undo.word.id] = ((attempts[undo.word.id] ?: 1) - 1).coerceAtLeast(0)
                }
                lastUndo = null
                showCurrent(reveal = format == StudyFormat.FLASHCARD)
            } finally {
                session.release()
            }
        }
    }

    private fun record(word: Word, rating: Int) {
        viewModelScope.launch {
            try {
                val outcome = runCatching { repository.review(word.id, rating) }.getOrNull()
                if (outcome == null) {
                    pendingWord = word
                    pendingRating = rating
                    return@launch
                }
                done++
                if (rating == Srs.AGAIN) {
                    againCount++
                    val previous = againWords[word.id]
                    againWords[word.id] = word to ((previous?.second ?: 0) + 1)
                } else {
                    correctCount++
                    if (rating == Srs.GOOD || rating == Srs.EASY) strengthenedCount++
                }
                val requeue = rating == Srs.AGAIN &&
                    (attempts[word.id] ?: 0) < MAX_REQUEUE_ATTEMPTS
                val removed = session.finish(word.id, if (requeue) outcome.word else null)
                if (requeue && removed) {
                    attempts[word.id] = (attempts[word.id] ?: 0) + 1
                    requeues++
                }
                lastUndo = Undo(word, outcome.logId, rating, requeue && removed)
                showCurrent()
            } finally {
                session.release()
            }
        }
    }

    private suspend fun showCurrent(reveal: Boolean = false) {
        val word = session.advance()
        if (word == null) {
            finish()
            return
        }
        val options = if (format == StudyFormat.CHOICE || format == StudyFormat.LISTEN) {
            buildOptions(word)
        } else {
            emptyList()
        }
        val cloze = if (format == StudyFormat.CLOZE) {
            Cloze.build(word.example, word.term)
        } else {
            null
        }
        _state.value = StudyUiState(
            loading = false,
            card = word.toCard(),
            format = format,
            revealed = reveal,
            clozePrompt = cloze?.sentence,
            clozeAnswer = cloze?.answer,
            options = options,
            intervalLabels = if (format == StudyFormat.FLASHCARD) {
                repository.previewIntervals(word)
            } else {
                emptyList()
            },
            position = done + 1,
            total = initialTotal + requeues,
            canUndo = lastUndo != null,
            reviewed = done,
            againCount = againCount,
            correctCount = correctCount,
            strengthenedCount = strengthenedCount,
            strugglingCount = againCount,
            accuracyPercent = SessionSummary.accuracyPercent(correctCount, done),
        )
    }

    private suspend fun finish() {
        val hardWords = SessionSummary.hardestWords(
            againWords.values.associate { (word, failures) -> word.term to failures }
        )
        val tomorrowDue = runCatching { repository.dueTomorrowCount() }.getOrNull()
        _state.value = StudyUiState(
            loading = false,
            finished = true,
            format = format,
            position = done,
            total = done,
            canUndo = lastUndo != null,
            reviewed = done,
            againCount = againCount,
            correctCount = correctCount,
            elapsedMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L),
            strengthenedCount = strengthenedCount,
            strugglingCount = againCount,
            accuracyPercent = SessionSummary.accuracyPercent(correctCount, done),
            tomorrowDue = tomorrowDue,
            hardWords = hardWords,
        )
    }

    private fun buildOptions(word: Word): List<String> {
        val correct = word.meaning.trim()
        val distractors = pool.asSequence()
            .filter { it.id != word.id }
            .map { it.meaning.trim() }
            .filter { it.isNotEmpty() && it != correct }
            .distinct()
            .shuffled()
            .take(3)
            .toList()
        return (distractors + correct).shuffled()
    }

    private fun Word.toCard(): StudyCardUi = StudyCardUi(
        id = id,
        term = term,
        meaning = meaning,
        example = example,
        exampleTr = exampleTr,
        notes = notes,
        tags = tags,
        pos = pos,
        favorite = favorite,
        lapses = lapses,
        isLeech = isLeech,
    )

    companion object {
        private const val MAX_REQUEUE_ATTEMPTS = 2
    }
}

class SettingsViewModel(
    private val repository: WordRepository,
    private val settings: SettingsStore,
    private val backupManager: BackupManager,
    private val appContext: Context,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        settings.settings,
        repository.observeTotals(),
    ) { appSettings, totals ->
        SettingsUiState(
            loading = false,
            settings = appSettings,
            totalWords = totals.first,
            reviewedToday = totals.second,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun setNewPerDay(value: Int) {
        viewModelScope.launch { settings.setNewPerDay(value) }
    }

    fun setDailyGoal(value: Int) {
        viewModelScope.launch { settings.setDailyGoal(value) }
    }

    fun setRetention(value: Int) {
        viewModelScope.launch { settings.setDesiredRetention(value) }
    }

    fun setThemeMode(value: Int) {
        viewModelScope.launch { settings.setThemeMode(value) }
    }

    fun setAutoSpeak(value: Boolean) {
        viewModelScope.launch { settings.setAutoSpeak(value) }
    }

    fun setSpeechRate(value: Float) {
        viewModelScope.launch { settings.setSpeechRate(value) }
    }

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            if (enabled && !Notifications.canPost(appContext)) {
                _message.value =
                    appContext.getString(R.string.settings_reminder_permission_denied)
                return@launch
            }
            settings.setReminder(enabled, hour, minute)
            if (enabled) {
                ReminderScheduler.schedule(appContext, hour, minute)
                _message.value = appContext.getString(R.string.settings_reminder_on)
            } else {
                ReminderScheduler.cancel(appContext)
                _message.value = appContext.getString(R.string.settings_reminder_off)
            }
        }
    }

    fun testNotification() {
        val sent = Notifications.showReminder(
            appContext,
            appContext.getString(R.string.notif_test_title),
            appContext.getString(R.string.notif_test_text),
        )
        _message.value = appContext.getString(
            if (sent) R.string.settings_reminder_test_sent else R.string.settings_notification_failed,
        )
    }

    fun exportJson(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val text = backupManager.exportJson()
                withContext(Dispatchers.IO) { FileIo.writeText(appContext, uri, text) }
            }
            _message.value = result.fold(
                { appContext.getString(R.string.settings_backup_saved) },
                { appContext.getString(R.string.settings_backup_error, it.message ?: "") },
            )
        }
    }

    fun importJson(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val text = withContext(Dispatchers.IO) { FileIo.readText(appContext, uri) }
                backupManager.importJson(text)
            }
            result.onSuccess {
                val restored = settings.settings.first()
                if (restored.reminderEnabled) {
                    if (Notifications.canPost(appContext)) {
                        ReminderScheduler.schedule(
                            appContext,
                            restored.reminderHour,
                            restored.reminderMinute,
                        )
                    } else {
                        ReminderScheduler.cancel(appContext)
                    }
                } else {
                    ReminderScheduler.cancel(appContext)
                }
            }
            _message.value = result.fold(
                { appContext.getString(R.string.settings_restore_done, it.summary) },
                { appContext.getString(R.string.settings_backup_error, it.message ?: "") },
            )
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val text = backupManager.exportCsv()
                withContext(Dispatchers.IO) { FileIo.writeText(appContext, uri, text) }
            }
            _message.value = result.fold(
                { appContext.getString(R.string.settings_csv_saved) },
                { appContext.getString(R.string.settings_backup_error, it.message ?: "") },
            )
        }
    }

    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val text = withContext(Dispatchers.IO) { FileIo.readText(appContext, uri) }
                backupManager.importCsv(text)
            }
            _message.value = result.fold(
                { appContext.getString(R.string.settings_restore_done, it.summary) },
                { appContext.getString(R.string.settings_backup_error, it.message ?: "") },
            )
        }
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            repository.resetAllProgress()
            _message.value = appContext.getString(R.string.settings_reset_done)
        }
    }
}
