package com.wendigo.tekrar

import com.wendigo.tekrar.core.LeechPolicy
import com.wendigo.tekrar.data.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeechPolicyTest {

    private fun word(
        id: Long,
        lapses: Int,
        dueAt: Long = 0L,
        suspended: Boolean = false,
        term: String = "term$id",
    ) = Word(
        id = id,
        term = term,
        meaning = "meaning$id",
        dueAt = dueAt,
        lapses = lapses,
        suspended = suspended,
    )

    @Test
    fun thresholdMatchesWordConstant() {
        val below = word(1, Word.LEECH_LAPSES - 1)
        val at = word(2, Word.LEECH_LAPSES)
        val above = word(3, Word.LEECH_LAPSES + 1)

        assertFalse(LeechPolicy.isLeech(below))
        assertTrue(LeechPolicy.isLeech(at))
        assertTrue(LeechPolicy.isLeech(above))
    }

    @Test
    fun suspendedWordsAreExcluded() {
        val active = word(1, Word.LEECH_LAPSES)
        val suspended = word(2, Word.LEECH_LAPSES, suspended = true)

        assertEquals(listOf(active), LeechPolicy.leeches(listOf(active, suspended)))
    }

    @Test
    fun ordersByLapsesDescendingThenDueAtAscending() {
        val low = word(1, Word.LEECH_LAPSES, dueAt = 10L)
        val highLate = word(2, Word.LEECH_LAPSES + 3, dueAt = 30L)
        val highSoon = word(3, Word.LEECH_LAPSES + 3, dueAt = 20L)
        val ignored = word(4, Word.LEECH_LAPSES - 1, dueAt = 1L)

        val result = LeechPolicy.leeches(listOf(low, highLate, highSoon, ignored))

        assertEquals(listOf(highSoon.id, highLate.id, low.id), result.map { it.id })
    }

    @Test
    fun customThresholdIsRespected() {
        val word = word(1, lapses = 2)
        assertTrue(LeechPolicy.isLeech(word, threshold = 2))
        assertFalse(LeechPolicy.isLeech(word, threshold = 3))
    }
}
