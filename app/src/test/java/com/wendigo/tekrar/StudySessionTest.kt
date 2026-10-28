package com.wendigo.tekrar

import com.wendigo.tekrar.core.StudySession
import com.wendigo.tekrar.data.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudySessionTest {

    private fun word(id: Long) = Word(id = id, term = "term$id", meaning = "meaning$id")

    @Test
    fun secondRatingIsIgnoredWhileFirstIsStillBeingRecorded() {
        val first = word(1)
        val second = word(2)
        val session = StudySession()
        session.load(listOf(first, second))
        val recorded = mutableListOf<Long>()

        session.begin(first.id)?.let { recorded += it.id }
        session.begin(first.id)?.let { recorded += it.id }

        assertEquals(listOf(first.id), recorded)
        assertEquals(first.id, session.current?.id)
        assertTrue(session.isBusy)
    }

    @Test
    fun staleRatingDoesNotTouchTheNextCard() {
        val first = word(1)
        val second = word(2)
        val session = StudySession()
        session.load(listOf(first, second))

        val action = session.begin(first.id)
        assertEquals(first.id, action?.id)
        assertTrue(session.finish(first.id))
        assertEquals(second.id, session.advance()?.id)
        session.release()

        assertNull(session.begin(first.id))
        assertEquals(second.id, session.current?.id)
    }

    @Test
    fun finishOnlyRemovesMatchingQueueHead() {
        val first = word(1)
        val second = word(2)
        val session = StudySession()
        session.load(listOf(first, second))

        assertFalse(session.finish(second.id))
        assertEquals(first.id, session.current?.id)

        assertTrue(session.finish(first.id))
        assertEquals(second.id, session.advance()?.id)
    }

    @Test
    fun againAppendsRequeuedWordAfterCurrent() {
        val first = word(1)
        val second = word(2)
        val session = StudySession()
        session.load(listOf(first, second))

        session.begin(first.id)
        assertTrue(session.finish(first.id, requeue = first))

        assertEquals(second.id, session.advance()?.id)
        assertTrue(session.finish(second.id))
        assertEquals(first.id, session.advance()?.id)
    }
}
