package com.wendigo.tekrar.core

import com.wendigo.tekrar.data.ReviewLog
import com.wendigo.tekrar.data.Word
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class DayCount(val date: LocalDate, val count: Int)

object StatsMath {

    fun today(zone: ZoneId = ZoneId.systemDefault()): LocalDate = LocalDate.now(zone)

    fun dayKey(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun startOfDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun reviewsByDay(
        logs: List<ReviewLog>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Map<LocalDate, Int> {
        val map = HashMap<LocalDate, Int>()
        for (log in logs) {
            val key = dayKey(log.reviewedAt, zone)
            map[key] = (map[key] ?: 0) + 1
        }
        return map
    }

    fun streak(days: Map<LocalDate, Int>, today: LocalDate): Int {
        val todayCount = days[today] ?: 0
        var cursor = if (todayCount > 0) today else today.minusDays(1)
        var streak = 0
        while ((days[cursor] ?: 0) > 0) {
            streak++
            cursor = cursor.minusDays(1)
            if (streak > 3650) break
        }
        return streak
    }

    fun lastNDays(
        days: Map<LocalDate, Int>,
        today: LocalDate,
        count: Int,
    ): List<DayCount> =
        (count - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            DayCount(date, days[date] ?: 0)
        }

    fun forecast(
        words: List<Word>,
        today: LocalDate,
        horizon: Int,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<DayCount> {
        val buckets = (0 until horizon).associate { offset ->
            today.plusDays(offset.toLong()) to 0
        }.toMutableMap()
        for (word in words) {
            if (word.suspended || word.isNew) continue
            val date = dayKey(word.dueAt, zone)
            if (date in buckets.keys) {
                buckets[date] = (buckets[date] ?: 0) + 1
            }
        }
        return (0 until horizon).map { offset ->
            val date = today.plusDays(offset.toLong())
            DayCount(date, buckets[date] ?: 0)
        }
    }

    fun retention(logs: List<ReviewLog>): Double? {
        val eligible = logs.filter { it.elapsedDays >= 1.0 }
        if (eligible.size < 20) return null
        val passed = eligible.count { it.rating >= 2 }
        return passed.toDouble() / eligible.size
    }

    fun reviewedBetween(logs: List<ReviewLog>, from: LocalDate, to: LocalDate, zone: ZoneId): Int =
        logs.count {
            val key = dayKey(it.reviewedAt, zone)
            !key.isBefore(from) && !key.isAfter(to)
        }

    fun dueStreakLabel(date: LocalDate, today: LocalDate): String = when {
        date.isBefore(today) -> "gecikti"
        date == today -> "bugün"
        date == today.plusDays(1) -> "yarın"
        date == today.plusDays(2) -> "2 gün sonra"
        else -> "${ChronoUnit.DAYS.between(today, date)} gün sonra"
    }
}
