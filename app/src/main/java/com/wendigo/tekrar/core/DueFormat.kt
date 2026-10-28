package com.wendigo.tekrar.core

import com.wendigo.tekrar.data.Word
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DueFormat {

    private val turkish = Locale.forLanguageTag("tr")
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", turkish)
    private val time = DateTimeFormatter.ofPattern("HH:mm", turkish)

    fun status(word: Word, now: Long = System.currentTimeMillis()): String = when {
        word.suspended -> "Askıda"
        word.isNew -> "Yeni"
        word.dueAt <= now -> "Şimdi"
        else -> relative(word.dueAt, now)
    }

    fun relative(millis: Long, now: Long = System.currentTimeMillis()): String {
        val zone = ZoneId.systemDefault()
        val moment = Instant.ofEpochMilli(millis).atZone(zone)
        val date = moment.toLocalDate()
        val today = LocalDate.now(zone)
        val label = StatsMath.dueStreakLabel(date, today)
        return if (date == today) {
            "$label ${moment.toLocalTime().format(time)}"
        } else {
            label
        }
    }

    fun date(millis: Long): String =
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dayMonth)

    fun interval(days: Int): String = when {
        days < 30 -> "$days gün"
        days < 365 -> "${(days / 30).coerceAtLeast(1)} ay"
        else -> "${(days / 365).coerceAtLeast(1)} yıl"
    }

    fun shortWeekday(date: LocalDate): String = WEEKDAYS[date.dayOfWeek.value - 1]

    val WEEKDAYS = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
}
