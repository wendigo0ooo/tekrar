package com.wendigo.tekrar.core

import java.time.DayOfWeek
import java.time.LocalDate

data class WeeklyProgress(
    val days: Int,
    val goal: Int,
) {
    val fraction: Float
        get() = if (goal <= 0) 0f else (days.toFloat() / goal).coerceIn(0f, 1f)

    val achieved: Boolean
        get() = days >= goal
}

/**
 * Pazartesi–Pazar haftasında en az bir tekrar yapılan günleri sayar.
 * Tüm hesaplar yerel [LocalDate] üzerindedir; DST geçişleri gün sınırını kaydırmaz.
 */
object WeeklyGoal {

    const val MIN_GOAL = 3
    const val MAX_GOAL = 7

    fun weekStart(today: LocalDate): LocalDate =
        today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())

    fun weekEnd(today: LocalDate): LocalDate = weekStart(today).plusDays(6)

    fun activeDays(days: Map<LocalDate, Int>, today: LocalDate): Int {
        val start = weekStart(today)
        val end = minOf(today, weekEnd(today))
        return days.count { (date, count) ->
            count > 0 && !date.isBefore(start) && !date.isAfter(end)
        }
    }

    fun progress(days: Map<LocalDate, Int>, today: LocalDate, goalDays: Int): WeeklyProgress {
        val goal = goalDays.coerceIn(MIN_GOAL, MAX_GOAL)
        return WeeklyProgress(activeDays(days, today), goal)
    }
}
