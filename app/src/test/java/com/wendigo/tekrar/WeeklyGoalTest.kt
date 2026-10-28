package com.wendigo.tekrar

import com.wendigo.tekrar.core.StatsMath
import com.wendigo.tekrar.core.WeeklyGoal
import com.wendigo.tekrar.data.ReviewLog
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyGoalTest {

    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 10, 28)

    private fun log(date: LocalDate, zone: ZoneId) = ReviewLog(
        wordId = 1,
        rating = 3,
        reviewedAt = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
        previousDueAt = 0L,
        previousStability = 1.0,
        previousDifficulty = 5.0,
        nextDueAt = 0L,
        nextStability = 2.0,
        nextDifficulty = 5.0,
        elapsedDays = 1.0,
    )

    @Test
    fun weekRunsFromMondayToSunday() {
        assertEquals(LocalDate.of(2026, 10, 26), WeeklyGoal.weekStart(today))
        assertEquals(LocalDate.of(2026, 11, 1), WeeklyGoal.weekEnd(today))
        assertEquals(
            LocalDate.of(2026, 10, 26),
            WeeklyGoal.weekStart(LocalDate.of(2026, 11, 1)),
        )
        assertEquals(
            LocalDate.of(2026, 11, 2),
            WeeklyGoal.weekStart(LocalDate.of(2026, 11, 2)),
        )
    }

    @Test
    fun countsDistinctReviewDaysNotCards() {
        val days = mapOf(
            today to 12,
            today.minusDays(2) to 1,
            today.minusDays(3) to 4,
            today.minusDays(7) to 9,
        )
        assertEquals(2, WeeklyGoal.activeDays(days, today))
        assertEquals(2, WeeklyGoal.progress(days, today, goalDays = 5).days)
    }

    @Test
    fun ignoresFutureDaysInSameWeek() {
        val days = mapOf(today to 1, today.plusDays(2) to 3)
        assertEquals(1, WeeklyGoal.activeDays(days, today))
    }

    @Test
    fun emptyLogsMeanZeroProgress() {
        val drawn = StatsMath.reviewsByDay(emptyList(), zone)
        val progress = WeeklyGoal.progress(drawn, today, goalDays = 5)
        assertEquals(0, progress.days)
        assertEquals(5, progress.goal)
        assertEquals(0f, progress.fraction, 0.0001f)
    }

    @Test
    fun goalIsClampedToThreeSeven() {
        assertEquals(3, WeeklyGoal.progress(emptyMap(), today, goalDays = 0).goal)
        assertEquals(7, WeeklyGoal.progress(emptyMap(), today, goalDays = 42).goal)
    }

    @Test
    fun dstTransitionStillCountsLocalDates() {
        val newYork = ZoneId.of("America/New_York")
        val sunday = LocalDate.of(2026, 3, 8)
        val logs = listOf(
            log(LocalDate.of(2026, 3, 7), newYork),
            log(LocalDate.of(2026, 3, 8), newYork),
            log(LocalDate.of(2026, 3, 9), newYork),
        )
        val drawn = StatsMath.reviewsByDay(logs, newYork)
        assertEquals(2, WeeklyGoal.activeDays(drawn, sunday))
    }
}
