package com.wendigo.tekrar

import com.wendigo.tekrar.core.WeeklySummary
import com.wendigo.tekrar.data.ReviewLog
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklySummaryTest {

    private val istanbul = ZoneId.of("Europe/Istanbul")

    private fun instant(text: String): Long =
        OffsetDateTime.parse(text).toInstant().toEpochMilli()

    @Test
    fun nextSundayIsSameSundayBeforeEighteen() {
        val now = instant("2026-10-25T17:59:00+03:00")

        val next = WeeklySummary.nextSundayAt(now, istanbul)

        assertEquals(instant("2026-10-25T18:00:00+03:00"), next)
    }

    @Test
    fun nextSundaySkipsToFollowingWeekAtOrAfterEighteen() {
        val atEighteen = instant("2026-10-25T18:00:00+03:00")
        val later = instant("2026-10-25T18:01:00+03:00")

        assertEquals(
            instant("2026-11-01T18:00:00+03:00"),
            WeeklySummary.nextSundayAt(atEighteen, istanbul),
        )
        assertEquals(
            instant("2026-11-01T18:00:00+03:00"),
            WeeklySummary.nextSundayAt(later, istanbul),
        )
    }

    @Test
    fun nextSundayFromMidweek() {
        val now = instant("2026-10-28T09:30:00+03:00")

        assertEquals(
            instant("2026-11-01T18:00:00+03:00"),
            WeeklySummary.nextSundayAt(now, istanbul),
        )
    }

    @Test
    fun nextSundayRespectsTimezoneAtSameInstant() {
        val now = instant("2026-10-25T16:00:00Z")
        val tokyo = ZoneId.of("Asia/Tokyo")

        val istanbulNext = WeeklySummary.nextSundayAt(now, istanbul)
        val tokyoNext = WeeklySummary.nextSundayAt(now, tokyo)

        assertEquals(instant("2026-11-01T15:00:00Z"), istanbulNext)
        assertEquals(instant("2026-11-01T09:00:00Z"), tokyoNext)
    }

    @Test
    fun statsCountsLastSevenDaysAndCurrentStreak() {
        val today = LocalDate.of(2026, 10, 28)
        val logs = listOf(
            log(today),
            log(today),
            log(today.minusDays(1)),
            log(today.minusDays(2)),
            log(today.minusDays(7)),
        )

        val stats = WeeklySummary.stats(logs, today, istanbul)

        assertEquals(3, stats.activeDays)
        assertEquals(4, stats.cards)
        assertEquals(3, stats.streak)
        assertEquals(
            "Bu hafta 3 gün çalıştın · 4 kart · 3 günlük seri",
            WeeklySummary.summaryText(stats),
        )
    }

    @Test
    fun emptyWeekProducesZeroCards() {
        val today = LocalDate.of(2026, 10, 28)

        val stats = WeeklySummary.stats(emptyList(), today, istanbul)

        assertEquals(0, stats.activeDays)
        assertEquals(0, stats.cards)
        assertEquals(0, stats.streak)
    }

    private fun log(day: LocalDate, zone: ZoneId = istanbul): ReviewLog {
        val stamp = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        return ReviewLog(
            wordId = 1L,
            rating = 3,
            reviewedAt = stamp,
            previousDueAt = stamp,
            previousStability = 1.0,
            previousDifficulty = 5.0,
            nextDueAt = stamp,
            nextStability = 2.0,
            nextDifficulty = 5.0,
            elapsedDays = 1.0,
        )
    }
}
