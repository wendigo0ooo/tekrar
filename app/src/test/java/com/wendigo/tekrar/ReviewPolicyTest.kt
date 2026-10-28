package com.wendigo.tekrar

import com.wendigo.tekrar.core.ReviewPolicy
import com.wendigo.tekrar.data.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPolicyTest {

    @Test
    fun autoSuspendTriggersOnlyAtThresholdAndWhenNotSuspended() {
        assertFalse(ReviewPolicy.shouldAutoSuspend(7, false))
        assertTrue(ReviewPolicy.shouldAutoSuspend(8, false))
        assertTrue(ReviewPolicy.shouldAutoSuspend(9, false))
        assertFalse(ReviewPolicy.shouldAutoSuspend(8, true))
        assertFalse(ReviewPolicy.shouldAutoSuspend(20, true))
    }

    @Test
    fun withAutoSuspendIsIdempotent() {
        val below = Word(term = "go", meaning = "gitmek", lapses = 7)
        assertEquals(below, ReviewPolicy.withAutoSuspend(below))

        val atThreshold = Word(term = "go", meaning = "gitmek", lapses = 8)
        val suspended = ReviewPolicy.withAutoSuspend(atThreshold)
        assertTrue(suspended.suspended)
        assertEquals(suspended, ReviewPolicy.withAutoSuspend(suspended))
    }

    @Test
    fun restoreAfterUndoBringsBackSrsFieldsAndSuspendedFlag() {
        val previous = Word(
            id = 5L,
            term = "go",
            meaning = "gitmek",
            state = Word.STATE_REVIEW,
            stability = 3.0,
            difficulty = 5.0,
            dueAt = 100L,
            lastReviewAt = 90L,
            introducedAt = 80L,
            reps = 4,
            lapses = 7,
            suspended = false,
        )
        val current = Word(
            id = 5L,
            term = "go (düzenlendi)",
            meaning = "gitmek",
            state = Word.STATE_REVIEW,
            stability = 1.0,
            difficulty = 8.0,
            dueAt = 999L,
            lastReviewAt = 150L,
            introducedAt = 80L,
            reps = 5,
            lapses = 8,
            suspended = true,
        )

        val restored = ReviewPolicy.restoreAfterUndo(current, previous)

        assertEquals("go (düzenlendi)", restored.term)
        assertEquals(previous.state, restored.state)
        assertEquals(previous.stability, restored.stability, 0.0)
        assertEquals(previous.difficulty, restored.difficulty, 0.0)
        assertEquals(previous.dueAt, restored.dueAt)
        assertEquals(previous.lastReviewAt, restored.lastReviewAt)
        assertEquals(previous.introducedAt, restored.introducedAt)
        assertEquals(previous.reps, restored.reps)
        assertEquals(previous.lapses, restored.lapses)
        assertEquals(previous.suspended, restored.suspended)
    }
}
