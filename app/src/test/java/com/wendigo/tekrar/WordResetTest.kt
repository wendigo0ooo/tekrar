package com.wendigo.tekrar

import com.wendigo.tekrar.data.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordResetTest {

    @Test
    fun resetProgressClearsSchedulingButPreservesUserStateAndContent() {
        val now = 1_234L
        val word = Word(
            id = 7,
            term = "build",
            meaning = "inşa etmek",
            example = "We build a house.",
            exampleTr = "Bir ev inşa ederiz.",
            notes = "not",
            tags = "fiil",
            pos = "verb",
            favorite = true,
            createdAt = 99L,
            state = Word.STATE_REVIEW,
            stability = 42.0,
            difficulty = 7.5,
            dueAt = 555L,
            lastReviewAt = 444L,
            introducedAt = 333L,
            reps = 9,
            lapses = 4,
            suspended = true,
        )

        val reset = word.resetProgress(now)

        assertEquals(Word.STATE_NEW, reset.state)
        assertEquals(0.0, reset.stability, 0.0)
        assertEquals(0.0, reset.difficulty, 0.0)
        assertEquals(now, reset.dueAt)
        assertNull(reset.lastReviewAt)
        assertNull(reset.introducedAt)
        assertEquals(0, reset.reps)
        assertEquals(0, reset.lapses)
        assertTrue(reset.suspended)
        assertTrue(reset.favorite)
        assertEquals("build", reset.term)
        assertEquals("inşa etmek", reset.meaning)
        assertEquals("fiil", reset.tags)
        assertEquals(99L, reset.createdAt)
    }
}
