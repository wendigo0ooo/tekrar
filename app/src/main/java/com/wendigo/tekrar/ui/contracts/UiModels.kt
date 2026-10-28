package com.wendigo.tekrar.ui.contracts

import com.wendigo.tekrar.core.DayCount
import com.wendigo.tekrar.data.Word
import com.wendigo.tekrar.repo.AppSettings

enum class StudyMode { MIXED, REVIEW, NEW }

enum class StudyFormat { FLASHCARD, CHOICE, TYPING, LISTEN, CLOZE }

enum class LibraryFilter { ALL, DUE, NEW, FAVORITE, SUSPENDED, LEECH }

enum class LibrarySort { DUE, ALPHABETICAL, NEWEST, HARDEST }

data class DashboardUiState(
    val loading: Boolean = true,
    val dueNow: Int = 0,
    val newAvailable: Int = 0,
    val newTotal: Int = 0,
    val totalWords: Int = 0,
    val learned: Int = 0,
    val mature: Int = 0,
    val reviewedToday: Int = 0,
    val dailyGoal: Int = 20,
    val streak: Int = 0,
    val streakFreezes: Int = 0,
    val weeklyActiveDays: Int = 0,
    val weeklyGoalDays: Int = 5,
    val nextDueText: String? = null,
    val last7: List<DayCount> = emptyList(),
)

data class StudyCardUi(
    val id: Long,
    val term: String,
    val meaning: String,
    val example: String,
    val exampleTr: String,
    val notes: String,
    val tags: String,
    val pos: String,
    val favorite: Boolean,
)

data class StudyUiState(
    val loading: Boolean = true,
    val empty: Boolean = false,
    val finished: Boolean = false,
    val card: StudyCardUi? = null,
    val format: StudyFormat = StudyFormat.FLASHCARD,
    val revealed: Boolean = false,
    val answered: Boolean = false,
    val answerCorrect: Boolean? = null,
    val selectedOption: String? = null,
    val typedAnswer: String = "",
    val clozePrompt: String? = null,
    val clozeAnswer: String? = null,
    val options: List<String> = emptyList(),
    val intervalLabels: List<String> = emptyList(),
    val position: Int = 0,
    val total: Int = 0,
    val canUndo: Boolean = false,
    val reviewed: Int = 0,
    val againCount: Int = 0,
    val correctCount: Int = 0,
    val elapsedMs: Long = 0,
)

data class StatsUiState(
    val loading: Boolean = true,
    val totalWords: Int = 0,
    val learned: Int = 0,
    val young: Int = 0,
    val mature: Int = 0,
    val leech: Int = 0,
    val suspended: Int = 0,
    val retention: Double? = null,
    val reviewedToday: Int = 0,
    val dailyGoal: Int = 20,
    val streak: Int = 0,
    val streakFreezes: Int = 0,
    val frozenDays: Int = 0,
    val weeklyActiveDays: Int = 0,
    val weeklyGoalDays: Int = 5,
    val heatmap: List<DayCount> = emptyList(),
    val last30: List<DayCount> = emptyList(),
    val forecast: List<DayCount> = emptyList(),
)

data class SettingsUiState(
    val loading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val totalWords: Int = 0,
    val reviewedToday: Int = 0,
)

data class LibraryUiState(
    val loading: Boolean = true,
    val words: List<Word> = emptyList(),
    val query: String = "",
    val filter: LibraryFilter = LibraryFilter.ALL,
    val sort: LibrarySort = LibrarySort.DUE,
)

data class WordEditorUiState(
    val loading: Boolean = true,
    val word: Word? = null,
    val isEdit: Boolean = false,
    val saving: Boolean = false,
    val duplicateTerm: Boolean = false,
    val saveFailed: Boolean = false,
)
