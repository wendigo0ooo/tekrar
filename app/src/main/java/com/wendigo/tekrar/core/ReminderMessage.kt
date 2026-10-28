package com.wendigo.tekrar.core

sealed interface ReminderMessage {
    data class Due(val count: Int) : ReminderMessage
    data class NewWords(val count: Int) : ReminderMessage
    data object None : ReminderMessage

    companion object {
        fun of(due: Int, newAvailable: Int): ReminderMessage = when {
            due > 0 -> Due(due)
            newAvailable > 0 -> NewWords(newAvailable)
            else -> None
        }
    }
}
