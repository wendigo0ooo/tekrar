package com.wendigo.tekrar

import com.wendigo.tekrar.core.StreakFreeze
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakFreezeTest {

    private val today = LocalDate.of(2026, 10, 28)

    @Test
    fun grantsAtEachSevenDayMilestone() {
        val first = StreakFreeze.grant(streak = 7, lastMilestone = 0, stock = 0)
        assertEquals(1, first.granted)
        assertEquals(7, first.milestone)

        val second = StreakFreeze.grant(streak = 14, lastMilestone = 7, stock = 1)
        assertEquals(1, second.granted)
        assertEquals(14, second.milestone)
    }

    @Test
    fun grantIsIdempotentForSameMilestone() {
        val again = StreakFreeze.grant(streak = 9, lastMilestone = 7, stock = 1)
        assertEquals(0, again.granted)
        assertEquals(7, again.milestone)
    }

    @Test
    fun grantCapsStockAtTwo() {
        val capped = StreakFreeze.grant(streak = 14, lastMilestone = 7, stock = 2)
        assertEquals(0, capped.granted)
        assertEquals(14, capped.milestone)
    }

    @Test
    fun noGrantBelowSevenDays() {
        val result = StreakFreeze.grant(streak = 6, lastMilestone = 0, stock = 0)
        assertEquals(0, result.granted)
        assertEquals(0, result.milestone)
    }

    @Test
    fun consumesSingleMissedDayBridgingActiveDays() {
        val reviewed = setOf(today.minusDays(2))
        val decision = StreakFreeze.decide(reviewed, emptySet(), today, stock = 2)
        assertEquals(listOf(today.minusDays(1)), decision.days)
        assertEquals(1, decision.stockAfter)
    }

    @Test
    fun coversWholeMultiDayGapWhenStockAllows() {
        val reviewed = setOf(today.minusDays(3))
        val decision = StreakFreeze.decide(reviewed, emptySet(), today, stock = 2)
        assertEquals(listOf(today.minusDays(2), today.minusDays(1)), decision.days)
        assertEquals(0, decision.stockAfter)
    }

    @Test
    fun multiDayGapConsumesNothingWhenStockIsTooSmall() {
        val reviewed = setOf(today.minusDays(4))
        val decision = StreakFreeze.decide(reviewed, emptySet(), today, stock = 2)
        assertTrue(decision.days.isEmpty())
        assertEquals(2, decision.stockAfter)
    }

    @Test
    fun alreadyFrozenDaysAreNotConsumedTwice() {
        val reviewed = setOf(today.minusDays(3))
        val frozen = setOf(today.minusDays(2))
        val decision = StreakFreeze.decide(reviewed, frozen, today, stock = 1)
        assertEquals(listOf(today.minusDays(1)), decision.days)
        assertEquals(0, decision.stockAfter)
    }

    @Test
    fun noConsumeWhenYesterdayWasReviewed() {
        val reviewed = setOf(today.minusDays(1), today.minusDays(2))
        val decision = StreakFreeze.decide(reviewed, emptySet(), today, stock = 2)
        assertTrue(decision.days.isEmpty())
        assertEquals(2, decision.stockAfter)
    }

    @Test
    fun neverFreezesToday() {
        val reviewed = setOf(today.minusDays(1))
        val decision = StreakFreeze.decide(reviewed, emptySet(), today, stock = 2)
        assertTrue(decision.days.isEmpty())
    }

    @Test
    fun noConsumeWithoutStock() {
        val reviewed = setOf(today.minusDays(2))
        val decision = StreakFreeze.decide(reviewed, emptySet(), today, stock = 0)
        assertTrue(decision.days.isEmpty())
        assertEquals(0, decision.stockAfter)
    }

    @Test
    fun noConsumeWithoutAnyActiveAnchor() {
        val decision = StreakFreeze.decide(emptySet(), emptySet(), today, stock = 2)
        assertTrue(decision.days.isEmpty())
        assertEquals(2, decision.stockAfter)
    }
}
