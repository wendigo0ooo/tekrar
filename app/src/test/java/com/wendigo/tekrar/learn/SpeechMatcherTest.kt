package com.wendigo.tekrar.learn

import com.wendigo.tekrar.core.learn.SpeechMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechMatcherTest {

    @Test
    fun exactMatchScoresHundred() {
        assertEquals(100, SpeechMatcher.score("Hello, my name is Anna.", "hello my name is anna"))
    }

    @Test
    fun punctuationAndCaseAreIgnored() {
        assertEquals(
            100,
            SpeechMatcher.score("I’m fine, thank you!", "i'm fine thank you"),
        )
    }

    @Test
    fun missingWordDropsScore() {
        assertEquals(
            80,
            SpeechMatcher.score("Hello, my name is Anna.", "hello my name anna"),
        )
        assertTrue(SpeechMatcher.score("Hello, my name is Anna.", "hello my name anna") >= SpeechMatcher.PASS_SCORE)
    }

    @Test
    fun lowSimilarityFailsThreshold() {
        val score = SpeechMatcher.score("Hello, my name is Anna.", "hello my name")
        assertEquals(60, score)
        assertTrue(score < SpeechMatcher.PASS_SCORE)
    }

    @Test
    fun extraWordsReduceScore() {
        val score = SpeechMatcher.score("My name is Anna", "well my name is Anna actually")
        assertEquals(67, score)
    }

    @Test
    fun emptyTranscriptScoresZero() {
        assertEquals(0, SpeechMatcher.score("Hello", ""))
        assertEquals(0, SpeechMatcher.score("Hello", "   "))
    }

    @Test
    fun emptyTargetAndTranscriptScoresHundred() {
        assertEquals(100, SpeechMatcher.score("", ""))
        assertEquals(0, SpeechMatcher.score("", "hello"))
    }

    @Test
    fun scoreIsBounded() {
        val score = SpeechMatcher.score("a b c", "x y z")
        assertTrue(score in 0..100)
        assertEquals(0, score)
    }
}
