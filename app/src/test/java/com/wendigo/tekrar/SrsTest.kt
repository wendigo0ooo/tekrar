package com.wendigo.tekrar

import com.wendigo.tekrar.core.Srs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SrsTest {

    private val eps = 1e-9

    @Test
    fun newCardGoodUsesSecondParameter() {
        val r = Srs.review(0.0, 0.0, 0.0, isNew = true, rating = Srs.GOOD)
        assertEquals(3.173, r.stability, 1e-9)
        assertEquals(3, r.intervalDays)
    }

    @Test
    fun newCardAgainUsesFirstParameter() {
        val r = Srs.review(0.0, 0.0, 0.0, isNew = true, rating = Srs.AGAIN)
        assertEquals(0.40255, r.stability, 1e-9)
        assertEquals(1, r.intervalDays)
    }

    @Test
    fun newCardEasyUsesFourthParameter() {
        val r = Srs.review(0.0, 0.0, 0.0, isNew = true, rating = Srs.EASY)
        assertEquals(15.69105, r.stability, 1e-9)
        assertEquals(16, r.intervalDays)
    }

    @Test
    fun intervalEqualsStabilityAtNinetyPercentRetention() {
        for (s in listOf(0.5, 1.0, 3.173, 10.0, 42.0, 365.0)) {
            val expected = s.toInt().coerceAtLeast(1)
            val actual = Srs.intervalDays(s, 0.9)
            assertTrue(
                "stability=$s expected~$expected actual=$actual",
                kotlin.math.abs(actual - expected) <= 1,
            )
        }
    }

    @Test
    fun retrievabilityAtStabilityEqualsTarget() {
        for (s in listOf(0.4, 1.0, 5.0, 100.0)) {
            assertEquals(0.9, Srs.retrievability(s, s), 1e-6)
        }
    }

    @Test
    fun recallIncreasesStabilityAndForgetDecreasesIt() {
        val base = Srs.review(0.0, 0.0, 0.0, isNew = true, rating = Srs.GOOD)
        val recalled = Srs.review(base.stability, base.difficulty, 3.0, false, Srs.GOOD)
        val forgotten = Srs.review(base.stability, base.difficulty, 3.0, false, Srs.AGAIN)
        assertTrue(recalled.stability > base.stability)
        assertTrue(forgotten.stability < base.stability)
    }

    @Test
    fun higherRatingYieldsLongerInterval() {
        val base = Srs.review(0.0, 0.0, 0.0, isNew = true, rating = Srs.GOOD)
        val hard = Srs.review(base.stability, base.difficulty, 4.0, false, Srs.HARD)
        val good = Srs.review(base.stability, base.difficulty, 4.0, false, Srs.GOOD)
        val easy = Srs.review(base.stability, base.difficulty, 4.0, false, Srs.EASY)
        assertTrue(hard.stability < good.stability)
        assertTrue(good.stability < easy.stability)
    }

    @Test
    fun difficultyStaysInBounds() {
        for (rating in 1..4) {
            val r = Srs.review(0.0, 0.0, 0.0, isNew = true, rating = rating)
            assertTrue(r.difficulty in 1.0..10.0)
        }
        var d = 5.0
        var s = 10.0
        for (rating in listOf(1, 1, 1, 4, 4, 2, 3, 1)) {
            val r = Srs.review(s, d, 14.0, false, rating)
            d = r.difficulty
            s = r.stability
            assertTrue(d in 1.0..10.0)
            assertTrue(s >= Srs.MIN_STABILITY)
        }
    }

    @Test
    fun sameDayReviewUsesShortTermCurve() {
        val base = Srs.review(0.0, 0.0, 0.0, isNew = true, rating = Srs.GOOD)
        val sameDayAgain = Srs.review(base.stability, base.difficulty, 0.2, false, Srs.AGAIN)
        assertTrue(sameDayAgain.stability < base.stability)
        assertTrue(sameDayAgain.intervalDays in 1..3)
    }

    @Test
    fun intervalIsClampedToMaximum() {
        assertEquals(Srs.MAX_INTERVAL_DAYS, Srs.intervalDays(1_000_000.0, 0.9))
    }

    @Test
    fun retrievabilityIsOneForNewElapsedTime() {
        assertEquals(1.0, Srs.retrievability(0.0, 12.0), eps)
    }
}
