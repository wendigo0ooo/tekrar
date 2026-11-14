package com.wendigo.tekrar.learn

import com.wendigo.tekrar.core.learn.Choice
import com.wendigo.tekrar.core.learn.Cloze
import com.wendigo.tekrar.core.learn.DialogueChoice
import com.wendigo.tekrar.core.learn.ExerciseGrader
import com.wendigo.tekrar.core.learn.ListenChoice
import com.wendigo.tekrar.core.learn.Match
import com.wendigo.tekrar.core.learn.MatchPair
import com.wendigo.tekrar.core.learn.Speak
import com.wendigo.tekrar.core.learn.Translate
import com.wendigo.tekrar.core.learn.UserAnswer
import com.wendigo.tekrar.core.learn.WordOrder
import com.wendigo.tekrar.core.learn.Write
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseGraderTest {

    private val choice = Choice(
        promptTr = "'hello' ne demek?",
        options = listOf("merhaba", "hoşça kal", "teşekkürler"),
        answer = 0,
        explanationTr = "'Hello' merhaba demektir.",
    )

    @Test
    fun gradesChoiceByIndex() {
        val correct = ExerciseGrader.grade(choice, UserAnswer.Choice(0))
        assertTrue(correct.correct)
        assertEquals(100, correct.scorePercent)
        assertEquals("merhaba", correct.expectedAnswer)
        assertTrue(correct.feedbackTr.contains("Doğru"))

        val wrong = ExerciseGrader.grade(choice, UserAnswer.Choice(2))
        assertFalse(wrong.correct)
        assertEquals(0, wrong.scorePercent)
        assertEquals("merhaba", wrong.expectedAnswer)
        assertTrue(wrong.feedbackTr.contains("Doğru cevap: merhaba"))
        assertTrue(wrong.feedbackTr.contains("merhaba demektir"))
    }

    @Test
    fun gradesListenAndDialogueChoice() {
        val listen = ListenChoice("Dinle", "Hello!", listOf("Merhaba!", "Hoşça kal."), 0)
        assertTrue(ExerciseGrader.grade(listen, UserAnswer.Choice(0)).correct)
        assertFalse(ExerciseGrader.grade(listen, UserAnswer.Choice(1)).correct)

        val dialogue = DialogueChoice("Seç", null, listOf("A", "B"), 1)
        assertTrue(ExerciseGrader.grade(dialogue, UserAnswer.Choice(1)).correct)
    }

    @Test
    fun gradesMatchOnlyWhenAllPairsMatch() {
        val exercise = Match(
            promptTr = "Eşleştir",
            pairs = listOf(
                MatchPair("hello", "merhaba"),
                MatchPair("hi", "selam"),
                MatchPair("bye", "bay bay"),
            ),
        )
        val correct = ExerciseGrader.grade(
            exercise,
            UserAnswer.Pairs(mapOf("hello" to "merhaba", "hi" to "selam", "bye" to "bay bay")),
        )
        assertTrue(correct.correct)
        assertEquals(100, correct.scorePercent)

        val partial = ExerciseGrader.grade(
            exercise,
            UserAnswer.Pairs(mapOf("hello" to "merhaba", "hi" to "bay bay", "bye" to "selam")),
        )
        assertFalse(partial.correct)
        assertEquals(33, partial.scorePercent)

        val extra = ExerciseGrader.grade(
            exercise,
            UserAnswer.Pairs(
                mapOf(
                    "hello" to "merhaba",
                    "hi" to "selam",
                    "bye" to "bay bay",
                    "goodbye" to "hoşça kal",
                ),
            ),
        )
        assertFalse(extra.correct)
        assertEquals(75, extra.scorePercent)
    }

    @Test
    fun matchNormalizesValues() {
        val exercise = Match(
            promptTr = "Eşleştir",
            pairs = listOf(MatchPair("I'm", "ben"), MatchPair("well-known", "ünlü")),
        )
        val result = ExerciseGrader.grade(
            exercise,
            UserAnswer.Pairs(mapOf("I’m" to " BEN ", "well–known" to "ÜNLÜ")),
        )
        assertTrue(result.correct)
    }

    @Test
    fun gradesWordOrderStrictly() {
        val exercise = WordOrder(
            promptTr = "Sıraya koy",
            tokens = listOf("Anna", "name", "My", "is"),
            answer = listOf("My", "name", "is", "Anna"),
        )
        assertTrue(
            ExerciseGrader.grade(
                exercise,
                UserAnswer.Tokens(listOf("my", "name", "is", "anna")),
            ).correct,
        )
        val wrong = ExerciseGrader.grade(
            exercise,
            UserAnswer.Tokens(listOf("name", "My", "is", "Anna")),
        )
        assertFalse(wrong.correct)
        assertEquals("My name is Anna", wrong.expectedAnswer)
        assertTrue(wrong.feedbackTr.contains("Doğru sıra"))

        val missing = ExerciseGrader.grade(exercise, UserAnswer.Tokens(listOf("My", "name", "is")))
        assertFalse(missing.correct)
    }

    @Test
    fun gradesOptionClozeByIndex() {
        val exercise = Cloze(
            promptTr = "Doldur",
            sentence = "My ___ is Ben.",
            options = listOf("name", "bye", "hi"),
            answer = 0,
        )
        assertTrue(ExerciseGrader.grade(exercise, UserAnswer.Choice(0)).correct)
        val wrong = ExerciseGrader.grade(exercise, UserAnswer.Choice(1))
        assertFalse(wrong.correct)
        assertEquals("name", wrong.expectedAnswer)
    }

    @Test
    fun gradesTypedClozeWithLooseNormalization() {
        val exercise = Cloze(
            promptTr = "Doldur",
            sentence = "My ___ is Ben.",
            answers = listOf("name"),
        )
        assertTrue(ExerciseGrader.grade(exercise, UserAnswer.Text("  Name.  ")).correct)
        assertFalse(ExerciseGrader.grade(exercise, UserAnswer.Text("nmae")).correct)
    }

    @Test
    fun gradesTranslateVariantsWithoutTypoTolerance() {
        val exercise = Translate(
            promptTr = "Çevir",
            answers = listOf("I'm Anna.", "I am Anna."),
            hintTr = "'I am' kalıbını kullan.",
        )
        assertTrue(ExerciseGrader.grade(exercise, UserAnswer.Text("i’m anna")).correct)
        assertTrue(ExerciseGrader.grade(exercise, UserAnswer.Text("  I  AM  ANNA  ")).correct)
        assertTrue(ExerciseGrader.grade(exercise, UserAnswer.Text("I'm Anna!")).correct)

        val typo = ExerciseGrader.grade(exercise, UserAnswer.Text("Im Ana."))
        assertFalse(typo.correct)
        assertEquals("I'm Anna.", typo.expectedAnswer)
        assertTrue(typo.feedbackTr.contains("Doğru cevap"))

        val wrongType = ExerciseGrader.grade(exercise, UserAnswer.Choice(0))
        assertFalse(wrongType.correct)
    }

    @Test
    fun translateTreatsDashesAsEquivalent() {
        val exercise = Translate("Çevir", listOf("well-known place"))
        assertTrue(ExerciseGrader.grade(exercise, UserAnswer.Text("well—known place")).correct)
    }

    @Test
    fun gradesSpeakWithSimilarityScore() {
        val exercise = Speak("Söyle", "Hello, my name is Anna.")
        val perfect = ExerciseGrader.grade(
            exercise,
            UserAnswer.Speech("hello my name is anna"),
        )
        assertTrue(perfect.correct)
        assertEquals(100, perfect.scorePercent)
        assertEquals("Hello, my name is Anna.", perfect.expectedAnswer)
        assertTrue(perfect.feedbackTr.contains("Benzerlik: %100"))

        val close = ExerciseGrader.grade(
            exercise,
            UserAnswer.Speech("hello my name anna"),
        )
        assertTrue(close.correct)
        assertEquals(80, close.scorePercent)

        val low = ExerciseGrader.grade(exercise, UserAnswer.Speech("hello my name"))
        assertFalse(low.correct)
        assertEquals(60, low.scorePercent)
        assertTrue(low.feedbackTr.contains("Biraz daha dene"))
    }

    @Test
    fun speakAcceptsTypedFallback() {
        val exercise = Speak("Söyle", "My name is Anna.")
        val result = ExerciseGrader.grade(exercise, UserAnswer.Text("my name is anna"))
        assertTrue(result.correct)
    }

    @Test
    fun gradesWriteWithMinWordsAndKeywords() {
        val exercise = Write(
            promptTr = "Kendini tanıt",
            minWords = 5,
            keywords = listOf("hello", "name"),
        )
        val correct = ExerciseGrader.grade(
            exercise,
            UserAnswer.Text("Hello! My name is Anna."),
        )
        assertTrue(correct.correct)
        assertEquals(100, correct.scorePercent)

        val short = ExerciseGrader.grade(exercise, UserAnswer.Text("Hello Anna"))
        assertFalse(short.correct)
        assertEquals(0, short.scorePercent)
        assertTrue(short.feedbackTr.contains("En az 5 kelime"))
        assertTrue(short.feedbackTr.contains("name"))

        val missingKeyword = ExerciseGrader.grade(
            exercise,
            UserAnswer.Text("Hello there, I am very happy today."),
        )
        assertFalse(missingKeyword.correct)
        assertTrue(missingKeyword.feedbackTr.contains("name"))
    }

    @Test
    fun writeCountsOnlyRealTokensAndChecksKeywordCase() {
        val exercise = Write("Yaz", minWords = 3, keywords = listOf("good morning"))
        val result = ExerciseGrader.grade(
            exercise,
            UserAnswer.Text("...... GOOD   morning everyone !!"),
        )
        assertTrue(result.correct)
        assertTrue(result.feedbackTr.contains("Güzel"))
    }

    @Test
    fun writeWithoutKeywordsPassesOnLength() {
        val exercise = Write("Yaz", minWords = 2)
        assertTrue(ExerciseGrader.grade(exercise, UserAnswer.Text("Hello there")).correct)
        assertFalse(ExerciseGrader.grade(exercise, UserAnswer.Text("hello")).correct)
    }

    @Test
    fun wrongAnswerTypeDoesNotCrash() {
        val result = ExerciseGrader.grade(choice, UserAnswer.Text("merhaba"))
        assertFalse(result.correct)
        assertEquals(0, result.scorePercent)
        assertEquals("merhaba", result.expectedAnswer)

        val match = Match("Eşleştir", listOf(MatchPair("a", "b")))
        assertFalse(ExerciseGrader.grade(match, UserAnswer.Choice(0)).correct)
        assertFalse(ExerciseGrader.grade(match, UserAnswer.Pairs(emptyMap())).correct)
    }
}
