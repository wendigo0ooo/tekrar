package com.wendigo.tekrar.learn

import com.wendigo.tekrar.core.learn.Choice
import com.wendigo.tekrar.core.learn.Example
import com.wendigo.tekrar.core.learn.Exercise
import com.wendigo.tekrar.core.learn.Lesson
import com.wendigo.tekrar.core.learn.Match
import com.wendigo.tekrar.core.learn.MatchPair
import com.wendigo.tekrar.core.learn.TeachCard
import com.wendigo.tekrar.core.learn.UserAnswer
import com.wendigo.tekrar.core.learn.VocabItem
import com.wendigo.tekrar.core.learn.WordOrder
import com.wendigo.tekrar.ui.learn.LessonPhase
import com.wendigo.tekrar.ui.learn.LessonSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonSessionTest {

    private fun lesson(
        teach: List<TeachCard> = emptyList(),
        exercises: List<Exercise> = listOf(Choice("Soru", listOf("a", "b"), 0)),
    ) = Lesson(
        schema = 1,
        id = "a1-u01-l01",
        unit = "a1-u01",
        cefr = "A1",
        title = "Merhaba",
        objectiveTr = "Selamlaş.",
        estMin = 4,
        vocab = listOf(
            VocabItem(
                term = "hello",
                meaningTr = "merhaba",
                pos = "ünlem",
                example = "Hello!",
                exampleTr = "Merhaba!",
            ),
        ),
        teach = teach,
        exercises = exercises,
    )

    private fun teachCard(title: String) = TeachCard(
        titleTr = title,
        bodyTr = "Kısa açıklama.",
        examples = listOf(Example(en = "Hello!", tr = "Merhaba!")),
    )

    @Test
    fun noTeachStartsOnFirstExercise() {
        val session = LessonSession(lesson())
        assertEquals(LessonPhase.EXERCISE, session.phase)
        assertEquals(0, session.stepIndex)
        assertEquals(1, session.currentExerciseNumber)
        assertEquals(1, session.exerciseCount)
        assertNull(session.currentTeach)
        assertNotNull(session.currentExercise)
        assertEquals(0f, session.progress, 0.0001f)
    }

    @Test
    fun teachCardsRunBeforeExercises() {
        val session = LessonSession(
            lesson(
                teach = listOf(teachCard("Birinci"), teachCard("İkinci")),
                exercises = listOf(Choice("Soru", listOf("a", "b"), 0)),
            ),
        )
        assertEquals(LessonPhase.TEACH, session.phase)
        assertEquals("Birinci", session.currentTeach?.titleTr)
        assertEquals(0, session.currentExerciseNumber)
        assertEquals(0f, session.progress, 0.0001f)

        assertEquals(LessonPhase.TEACH, session.next())
        assertEquals("İkinci", session.currentTeach?.titleTr)

        assertEquals(LessonPhase.EXERCISE, session.next())
        assertNull(session.currentTeach)
        assertNotNull(session.currentExercise)
        assertEquals(0f, session.progress, 0.0001f)
    }

    @Test
    fun submitIsIgnoredDuringTeach() {
        val session = LessonSession(
            lesson(teach = listOf(teachCard("Birinci")), exercises = listOf(Choice("Soru", listOf("a", "b"), 0))),
        )
        assertNull(session.submit(UserAnswer.Choice(0)))
        assertEquals(LessonPhase.TEACH, session.phase)
        assertEquals(0, session.answers.size)
        assertEquals(0, session.accuracyPercent)
    }

    @Test
    fun correctChoiceIsRecorded() {
        val session = LessonSession(lesson())
        val result = session.submit(UserAnswer.Choice(0))

        assertNotNull(result)
        assertTrue(result!!.correct)
        assertEquals(100, result.scorePercent)
        assertEquals(LessonPhase.FEEDBACK, session.phase)
        assertEquals(1, session.answers.size)
        assertEquals(1, session.correctCount)
        assertEquals(100, session.accuracyPercent)
        assertEquals(100, session.score)
        assertEquals(1f, session.progress, 0.0001f)
        assertEquals("a", session.lastGrade?.expectedAnswer)
    }

    @Test
    fun wrongChoiceRecordsZero() {
        val session = LessonSession(lesson())
        val result = session.submit(UserAnswer.Choice(1))

        assertNotNull(result)
        assertFalse(result!!.correct)
        assertEquals(0, result.scorePercent)
        assertEquals(0, session.accuracyPercent)
        assertEquals("a", session.lastGrade?.expectedAnswer)
        assertEquals(1, session.answers.size)
    }

    @Test
    fun secondSubmitDuringFeedbackIsIgnored() {
        val session = LessonSession(lesson())
        session.submit(UserAnswer.Choice(0))
        assertNull(session.submit(UserAnswer.Choice(1)))
        assertEquals(1, session.answers.size)
        assertEquals(100, session.accuracyPercent)
    }

    @Test
    fun nextAfterFeedbackFinishesLastExercise() {
        val session = LessonSession(lesson())
        session.submit(UserAnswer.Choice(0))

        assertEquals(LessonPhase.FINISHED, session.next())
        assertTrue(session.isFinished)
        assertNull(session.currentExercise)
        assertNull(session.lastGrade)
        assertEquals(1f, session.progress, 0.0001f)
        assertEquals(1, session.currentExerciseNumber)
        assertEquals(100, session.accuracyPercent)
    }

    @Test
    fun progressGrowsWithEachAnsweredExercise() {
        val session = LessonSession(
            lesson(
                exercises = listOf(
                    Choice("Bir", listOf("a", "b"), 0),
                    Choice("İki", listOf("c", "d"), 0),
                ),
            ),
        )
        assertEquals(1, session.currentExerciseNumber)
        session.submit(UserAnswer.Choice(0))
        assertEquals(0.5f, session.progress, 0.0001f)

        session.next()
        assertEquals(LessonPhase.EXERCISE, session.phase)
        assertEquals(2, session.currentExerciseNumber)
        assertEquals(0.5f, session.progress, 0.0001f)

        session.submit(UserAnswer.Choice(1))
        assertEquals(1f, session.progress, 0.0001f)
        assertEquals(50, session.accuracyPercent)
        assertEquals(LessonPhase.FINISHED, session.next())
    }

    @Test
    fun matchAndWordOrderAreGraded() {
        val session = LessonSession(
            lesson(
                exercises = listOf(
                    Match(
                        promptTr = "Eşleştir",
                        pairs = listOf(
                            MatchPair("hello", "merhaba"),
                            MatchPair("bye", "hoşça kal"),
                        ),
                    ),
                    WordOrder(
                        promptTr = "Sırala",
                        tokens = listOf("morning", "Good"),
                        answer = listOf("Good", "morning"),
                    ),
                ),
            ),
        )

        val match = session.submit(
            UserAnswer.Pairs(mapOf("hello" to "merhaba", "bye" to "hoşça kal")),
        )
        assertNotNull(match)
        assertTrue(match!!.correct)
        session.next()

        val order = session.submit(UserAnswer.Tokens(listOf("Good", "morning")))
        assertNotNull(order)
        assertTrue(order!!.correct)
        assertEquals(2, session.answers.size)
        assertEquals(100, session.accuracyPercent)
    }

    @Test
    fun partialMatchIsIncorrectAndRecordsScore() {
        val session = LessonSession(
            lesson(
                exercises = listOf(
                    Match(
                        promptTr = "Eşleştir",
                        pairs = listOf(
                            MatchPair("hello", "merhaba"),
                            MatchPair("bye", "hoşça kal"),
                        ),
                    ),
                ),
            ),
        )
        val result = session.submit(UserAnswer.Pairs(mapOf("hello" to "merhaba")))
        assertNotNull(result)
        assertFalse(result!!.correct)
        assertEquals(50, result.scorePercent)
        assertEquals(0, session.accuracyPercent)
        assertEquals(50, session.score)
    }

    @Test
    fun accuracyRoundsAcrossThreeExercises() {
        val session = LessonSession(
            lesson(
                exercises = listOf(
                    Choice("Bir", listOf("a", "b"), 0),
                    Choice("İki", listOf("c", "d"), 0),
                    Choice("Üç", listOf("e", "f"), 0),
                ),
            ),
        )
        session.submit(UserAnswer.Choice(0))
        session.next()
        session.submit(UserAnswer.Choice(1))
        session.next()
        session.submit(UserAnswer.Choice(0))

        assertEquals(3, session.answeredCount)
        assertEquals(2, session.correctCount)
        assertEquals(67, session.accuracyPercent)
    }

    @Test
    fun emptyLessonFinishesImmediately() {
        val session = LessonSession(lesson(teach = emptyList(), exercises = emptyList()))
        assertEquals(LessonPhase.FINISHED, session.phase)
        assertTrue(session.isFinished)
        assertEquals(0, session.totalSteps)
        assertEquals(1f, session.progress, 0.0001f)
        assertEquals(0, session.accuracyPercent)
    }
}
