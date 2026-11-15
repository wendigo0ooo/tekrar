package com.wendigo.tekrar.grammar

import com.wendigo.tekrar.core.grammar.GrammarSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrammarSessionTest {

    private val checker = GrammarFixtures.checker()

    @Test
    fun startsAtFirstTask() {
        val session = GrammarSession(GrammarFixtures.topic(), checker)

        assertEquals(3, session.taskCount)
        assertEquals(0, session.currentIndex)
        assertEquals("w1", session.currentTask?.id)
        assertFalse(session.isFinished)
        assertTrue(session.results.isEmpty())
    }

    @Test
    fun cannotAdvanceWithoutChecking() {
        val session = GrammarSession(GrammarFixtures.topic(), checker)

        assertFalse(session.next())
        assertEquals(0, session.currentIndex)
        assertTrue(session.results.isEmpty())
        assertEquals("w1", session.currentTask?.id)
    }

    @Test
    fun checkDoesNotRecordUntilNext() {
        val session = GrammarSession(GrammarFixtures.topic(), checker)

        val result = session.check("I am here.")
        assertNotNull(result)
        assertEquals(0, session.checkedCount)
        assertTrue(session.results.isEmpty())

        assertFalse(session.next())
        assertEquals(1, session.checkedCount)
        assertEquals(1, session.results.size)
        assertEquals("w2", session.currentTask?.id)
        assertNull(session.lastResult)
    }

    @Test
    fun retryReplacesOnlyLastCheck() {
        val session = GrammarSession(GrammarFixtures.topic(), checker)

        session.check("I amm here.")
        assertEquals(85, session.lastResult?.scorePercent)
        session.retry()
        assertNull(session.lastResult)
        assertEquals(0, session.checkedCount)
        session.check("I am here.")
        assertEquals(100, session.lastResult?.scorePercent)

        session.next()
        assertEquals(1, session.checkedCount)
        assertEquals(100, session.results.single().scorePercent)
    }

    @Test
    fun threeTasksFinishAndAverageScores() {
        val topic = GrammarFixtures.topic(
            tasks = listOf(
                GrammarFixtures.task(
                    id = "w1",
                    expect = listOf(GrammarFixtures.expect("\\bI am\\b", "\\bI\\b")),
                ),
                GrammarFixtures.task(
                    id = "w2",
                    expect = listOf(GrammarFixtures.expect("\\bI am\\b", "\\bI\\b")),
                ),
                GrammarFixtures.task(
                    id = "w3",
                    expect = listOf(GrammarFixtures.expect("\\bI am\\b", "\\bI\\b")),
                ),
            ),
        )
        val session = GrammarSession(topic, checker)

        session.check("I am here.")
        assertFalse(session.next())

        session.check("I student.")
        assertFalse(session.next())

        session.check("I am here.")
        assertTrue(session.next())

        assertTrue(session.isFinished)
        assertNull(session.currentTask)
        assertEquals(3, session.checkedCount)
        assertEquals(2, session.correctCount)
        assertEquals(93, session.averageScore)
        val summary = session.summary()
        assertEquals(3, summary.taskCount)
        assertEquals(3, summary.checkedCount)
        assertEquals(2, summary.correctCount)
        assertEquals(93, summary.averageScore)
        assertEquals(1, summary.issues.count { it.kind.name == "MISSING" })
    }

    @Test
    fun nextAfterFinishDoesNotDuplicate() {
        val session = GrammarSession(GrammarFixtures.topic(), checker)
        repeat(3) {
            session.check("I am here.")
            session.next()
        }
        assertTrue(session.isFinished)

        assertTrue(session.next())
        assertTrue(session.next())
        assertEquals(3, session.results.size)
    }

    @Test
    fun restartClearsProgress() {
        val session = GrammarSession(GrammarFixtures.topic(), checker)
        session.check("I am here.")
        session.next()
        session.check("I am here.")
        session.next()

        session.restart()

        assertFalse(session.isFinished)
        assertEquals(0, session.currentIndex)
        assertEquals("w1", session.currentTask?.id)
        assertTrue(session.results.isEmpty())
        assertNull(session.lastResult)
        assertEquals(0, session.averageScore)
    }
}
