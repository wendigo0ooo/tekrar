package com.wendigo.tekrar

import com.wendigo.tekrar.core.AnswerCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerCheckTest {

    @Test
    fun normalizeIgnoresCaseAndPunctuation() {
        assertEquals("book", AnswerCheck.normalize("  Book! "))
        assertEquals("kitap", AnswerCheck.normalize("Kitap."))
    }

    @Test
    fun normalizeRemovesParentheses() {
        assertEquals("run", AnswerCheck.normalize("run (verb)"))
    }

    @Test
    fun variantsSplitAlternatives() {
        val variants = AnswerCheck.variants("to go / leave, depart")
        assertEquals(listOf("go", "leave", "depart"), variants)
    }

    @Test
    fun matchesExactAndCaseInsensitive() {
        assertTrue(AnswerCheck.matches("Go", "to go"))
        assertTrue(AnswerCheck.matches("depart", "to go / leave, depart"))
        assertFalse(AnswerCheck.matches("arrive", "to go / leave, depart"))
    }

    @Test
    fun matchesSingleTypoForLongWords() {
        assertTrue(AnswerCheck.matches("beautifull", "beautiful"))
        assertFalse(AnswerCheck.matches("beautif", "beautiful"))
    }

    @Test
    fun shortWordsRequireExactMatch() {
        assertFalse(AnswerCheck.matches("cta", "cat"))
        assertTrue(AnswerCheck.matches("cat", "cat"))
    }

    @Test
    fun emptyInputNeverMatches() {
        assertFalse(AnswerCheck.matches("   ", "cat"))
        assertFalse(AnswerCheck.matches("", ""))
    }

    @Test
    fun levenshteinDistance() {
        assertEquals(0, AnswerCheck.levenshtein("book", "book"))
        assertEquals(1, AnswerCheck.levenshtein("book", "books"))
        assertEquals(3, AnswerCheck.levenshtein("kitten", "sitting"))
    }
}
