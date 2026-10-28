package com.wendigo.tekrar

import com.wendigo.tekrar.core.StatsMath
import com.wendigo.tekrar.data.ReviewLog
import com.wendigo.tekrar.data.Word
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsMathTest {

    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 10, 24)

    private fun millis(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun log(rating: Int, date: LocalDate, elapsed: Double = 2.0) = ReviewLog(
        wordId = 1,
        rating = rating,
        reviewedAt = millis(date),
        previousDueAt = millis(date.minusDays(2)),
        previousStability = 2.0,
        previousDifficulty = 5.0,
        nextDueAt = millis(date.plusDays(2)),
        nextStability = 3.0,
        nextDifficulty = 5.0,
        elapsedDays = elapsed,
    )

    @Test
    fun streakCountsConsecutiveDaysEndingToday() {
        val days = mapOf(
            today to 3,
            today.minusDays(1) to 2,
            today.minusDays(2) to 1,
            today.minusDays(4) to 5,
        )
        assertEquals(3, StatsMath.streak(days, today))
    }

    @Test
    fun streakFallsBackToYesterday() {
        val days = mapOf(today.minusDays(1) to 1, today.minusDays(2) to 1)
        assertEquals(2, StatsMath.streak(days, today))
    }

    @Test
    fun streakIsZeroWithoutActivity() {
        assertEquals(0, StatsMath.streak(emptyMap(), today))
    }

    @Test
    fun lastNDaysFillsMissingDays() {
        val days = mapOf(today to 4, today.minusDays(2) to 1)
        val result = StatsMath.lastNDays(days, today, 3)
        assertEquals(listOf(1, 0, 4), result.map { it.count })
        assertEquals(today, result.last().date)
    }

    @Test
    fun forecastSkipsNewSuspendedAndOutOfHorizonWords() {
        val words = listOf(
            Word(term = "a", meaning = "a", state = Word.STATE_REVIEW, dueAt = millis(today)),
            Word(term = "b", meaning = "b", state = Word.STATE_REVIEW, dueAt = millis(today.plusDays(1))),
            Word(term = "c", meaning = "c", state = Word.STATE_NEW, dueAt = millis(today)),
            Word(term = "d", meaning = "d", state = Word.STATE_REVIEW, dueAt = millis(today), suspended = true),
            Word(term = "e", meaning = "e", state = Word.STATE_REVIEW, dueAt = millis(today.plusDays(5))),
        )
        val result = StatsMath.forecast(words, today, horizon = 3, zone = zone)
        assertEquals(3, result.size)
        assertEquals(1, result[0].count)
        assertEquals(1, result[1].count)
        assertEquals(0, result[2].count)
    }

    @Test
    fun retentionNeedsTwentyEligibleReviews() {
        val few = List(19) { log(3, today.minusDays(it.toLong())) }
        assertNull(StatsMath.retention(few))
    }

    @Test
    fun retentionCountsRatingsAboveAgain() {
        val logs = List(20) { index -> log(if (index < 18) 3 else 1, today) }
        assertEquals(0.9, StatsMath.retention(logs)!!, 0.0001)
    }

    @Test
    fun retentionIgnoresSameDayReviews() {
        val logs = List(20) { log(3, today, elapsed = 0.0) }
        assertNull(StatsMath.retention(logs))
    }

    @Test
    fun dueStreakLabelUsesRelativeDays() {
        assertEquals("gecikti", StatsMath.dueStreakLabel(today.minusDays(1), today))
        assertEquals("bugün", StatsMath.dueStreakLabel(today, today))
        assertEquals("yarın", StatsMath.dueStreakLabel(today.plusDays(1), today))
        assertEquals("2 gün sonra", StatsMath.dueStreakLabel(today.plusDays(2), today))
        assertEquals("9 gün sonra", StatsMath.dueStreakLabel(today.plusDays(9), today))
    }
}
