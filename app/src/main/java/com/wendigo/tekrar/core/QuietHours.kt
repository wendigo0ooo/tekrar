package com.wendigo.tekrar.core

data class ReminderTime(
    val hour: Int,
    val minute: Int,
)

object QuietHours {

    const val START_HOUR = 22
    const val END_HOUR = 8

    /** 22:00–08:00 arasındaki hatırlatıcıları 08:00'e taşır. */
    fun clamp(hour: Int, minute: Int): ReminderTime {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        return if (safeHour >= START_HOUR || safeHour < END_HOUR) {
            ReminderTime(END_HOUR, 0)
        } else {
            ReminderTime(safeHour, safeMinute)
        }
    }
}
