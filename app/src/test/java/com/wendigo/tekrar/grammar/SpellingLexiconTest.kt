package com.wendigo.tekrar.grammar

import com.wendigo.tekrar.core.grammar.SpellingLexicon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpellingLexiconTest {

    private val lexicon = SpellingLexicon.of(
        listOf("study", "run", "stop", "cat", "student", "happy"),
    )

    @Test
    fun functionWordsAreKnown() {
        assertTrue(lexicon.isKnown("the"))
        assertTrue(lexicon.isKnown("am"))
        assertTrue(lexicon.isKnown("I"))
        assertTrue(lexicon.isKnown("you"))
        assertTrue(lexicon.isKnown("from"))
    }

    @Test
    fun contractionsAreKnown() {
        assertTrue(lexicon.isKnown("I'm"))
        assertTrue(lexicon.isKnown("don't"))
        assertTrue(lexicon.isKnown("they're"))
        assertFalse(lexicon.isKnown("i'am"))
    }

    @Test
    fun inflectionBodiesAreAccepted() {
        assertTrue(lexicon.isKnown("studies"))
        assertTrue(lexicon.isKnown("studied"))
        assertTrue(lexicon.isKnown("running"))
        assertTrue(lexicon.isKnown("stopped"))
        assertTrue(lexicon.isKnown("cats"))
        assertTrue(lexicon.isKnown("studying"))
    }

    @Test
    fun commonTyposAreSuggested() {
        assertEquals("don't", lexicon.suggestion("dont"))
        assertEquals("because", lexicon.suggestion("becuase"))
        assertEquals("student", lexicon.suggestion("studnt"))
        assertEquals("I'm", lexicon.suggestion("im"))
    }

    @Test
    fun closeSpellingIsSuggested() {
        assertEquals("student", lexicon.suggestion("studnt"))
        assertEquals("happy", lexicon.suggestion("happpy"))
    }

    @Test
    fun lengthLimitIsRespected() {
        assertEquals("cat", lexicon.suggestion("catt"))
        assertNull(lexicon.suggestion("cccccccccccc"))
    }

    @Test
    fun unknownWordIsNotFlagged() {
        assertNull(lexicon.suggestion("blorf"))
        assertNull(lexicon.suggestion("xylophone"))
    }

    @Test
    fun properNounMidSentenceIsNotFlagged() {
        assertNull(lexicon.suggestion("Deniz", sentenceInitial = false))
        assertNull(lexicon.suggestion("deniz", sentenceInitial = true))
    }

    @Test
    fun sentenceInitialCapitalizationIsPreserved() {
        assertEquals("Happy", lexicon.suggestion("Happpy", sentenceInitial = true))
        assertNull(lexicon.suggestion("Happpy", sentenceInitial = false))
    }

    @Test
    fun unknownApostropheFormIsNotFlagged() {
        assertNull(lexicon.suggestion("blorf's", sentenceInitial = false))
        assertTrue(lexicon.isKnown("student's"))
    }
}
