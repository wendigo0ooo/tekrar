package com.wendigo.tekrar

import com.wendigo.tekrar.core.SessionSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSummaryTest {

    @Test
    fun accuracyPercentRoundsCorrectOverReviewed() {
        assertEquals(0, SessionSummary.accuracyPercent(0, 0))
        assertEquals(100, SessionSummary.accuracyPercent(5, 5))
        assertEquals(67, SessionSummary.accuracyPercent(2, 3))
        assertEquals(33, SessionSummary.accuracyPercent(1, 3))
    }

    @Test
    fun accuracyClampsOutOfRangeInputs() {
        assertEquals(100, SessionSummary.accuracyPercent(9, 4))
        assertEquals(0, SessionSummary.accuracyPercent(-1, 4))
    }

    @Test
    fun hardestWordsSortByFailuresThenTerm() {
        val result = SessionSummary.hardestWords(
            mapOf(
                "beta" to 2,
                "alpha" to 2,
                "gamma" to 4,
                "delta" to 0,
            )
        )

        assertEquals(listOf("gamma", "alpha", "beta"), result.map { it.term })
        assertEquals(listOf(4, 2, 2), result.map { it.failures })
    }

    @Test
    fun hardestWordsLimitAndBlankFiltering() {
        val result = SessionSummary.hardestWords(
            mapOf(
                "a" to 3,
                "b" to 2,
                "c" to 1,
                "" to 10,
                "d" to 0,
            ),
            limit = 2,
        )

        assertEquals(listOf("a", "b"), result.map { it.term })
        assertTrue(SessionSummary.hardestWords(emptyMap()).isEmpty())
    }
}
