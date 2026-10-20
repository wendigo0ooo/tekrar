package com.wendigo.tekrar.ui.contracts

enum class StudyMode { MIXED, REVIEW, NEW }

data class DashboardUiState(
    val dueNow: Int = 0,
    val newAvailable: Int = 0,
    val totalWords: Int = 0,
    val reviewedToday: Int = 0,
    val nextDueText: String? = null,
    val loading: Boolean = true,
)

data class StudyCardUi(
    val id: Long,
    val term: String,
    val meaning: String,
    val example: String,
    val notes: String,
)

data class StudyUiState(
    val loading: Boolean = true,
    val finished: Boolean = false,
    val empty: Boolean = false,
    val card: StudyCardUi? = null,
    val revealed: Boolean = false,
    val position: Int = 0,
    val total: Int = 0,
    val intervalLabels: List<String> = emptyList(),
)

data class SettingsUiState(
    val newPerDay: Int = 20,
    val totalWords: Int = 0,
    val reviewedToday: Int = 0,
    val loading: Boolean = false,
)
