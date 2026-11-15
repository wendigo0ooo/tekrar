package com.wendigo.tekrar.grammar

import com.wendigo.tekrar.core.grammar.GrammarIssueKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrammarCheckerTest {

    private val checker = GrammarFixtures.checker()

    @Test
    fun toBeRuleFixesIis() {
        val result = checker.check(
            "I is a student.",
            rules = listOf(GrammarFixtures.rule("(?<=\\bI\\s)(is|are)\\b", "am")),
        )

        assertEquals("I am a student.", result.corrected)
        assertFalse(result.correct)
        assertEquals(80, result.scorePercent)
        assertEquals(1, result.issues.size)
        val issue = result.issues.single()
        assertEquals("is", issue.wrong)
        assertEquals("am", issue.correct)
        assertEquals(GrammarIssueKind.GRAMMAR, issue.kind)
    }

    @Test
    fun toBeRuleFixesHeAre() {
        val result = checker.check(
            "He are tired.",
            rules = listOf(GrammarFixtures.rule("(?<=\\bHe\\s)(are|am)\\b", "is")),
        )

        assertEquals("He is tired.", result.corrected)
        assertEquals("are", result.issues.single().wrong)
        assertEquals("is", result.issues.single().correct)
        assertEquals(80, result.scorePercent)
    }

    @Test
    fun capitalizationIsFixed() {
        val result = checker.check("i am happy")

        assertEquals("I am happy", result.corrected)
        assertEquals(95, result.scorePercent)
        val issue = result.issues.single()
        assertEquals(GrammarIssueKind.CAPITALIZATION, issue.kind)
        assertEquals(0, issue.start)
        assertEquals(1, issue.end)
        assertEquals("i", issue.wrong)
        assertEquals("I", issue.correct)
    }

    @Test
    fun correctShortSentenceHasFullScore() {
        val result = checker.check("I am happy")

        assertTrue(result.correct)
        assertEquals(100, result.scorePercent)
        assertTrue(result.issues.isEmpty())
        assertNull(result.hintTr)
        assertEquals("I am happy", result.corrected)
    }

    @Test
    fun spellingMistakeIsFixed() {
        val result = checker.check("I amm happy.")

        assertEquals("I am happy.", result.corrected)
        assertEquals(85, result.scorePercent)
        val issue = result.issues.single()
        assertEquals(GrammarIssueKind.SPELLING, issue.kind)
        assertEquals("amm", issue.wrong)
        assertEquals("am", issue.correct)
    }

    @Test
    fun presentSimpleThirdPersonRule() {
        val result = checker.check(
            "She go to school.",
            rules = listOf(GrammarFixtures.rule("(?<=\\bShe\\s)go\\b", "goes")),
        )

        assertEquals("She goes to school.", result.corrected)
        assertEquals("go", result.issues.single().wrong)
        assertEquals("goes", result.issues.single().correct)
    }

    @Test
    fun doesQuestionDropsThirdPersonS() {
        val result = checker.check(
            "Does he likes tea?",
            rules = listOf(GrammarFixtures.rule("(?<=\\bDoes he\\s)likes\\b", "like")),
        )

        assertEquals("Does he like tea?", result.corrected)
        assertEquals("likes", result.issues.single().wrong)
        assertEquals("like", result.issues.single().correct)
    }

    @Test
    fun thereIsPluralRule() {
        val result = checker.check(
            "There is two cats.",
            rules = listOf(GrammarFixtures.rule("(?<=\\bThere\\s)is\\b", "are")),
        )

        assertEquals("There are two cats.", result.corrected)
        assertEquals("is", result.issues.single().wrong)
        assertEquals("are", result.issues.single().correct)
    }

    @Test
    fun modalCanDropsInfinitiveTo() {
        val result = checker.check(
            "I can to swim.",
            rules = listOf(GrammarFixtures.rule("(?<=\\bcan\\s)to\\b", "")),
        )

        assertEquals("I can swim.", result.corrected)
        assertEquals("to", result.issues.single().wrong)
        assertEquals("", result.issues.single().correct)
    }

    @Test
    fun articleBecomesAnBeforeVowel() {
        val result = checker.check(
            "A engineer",
            rules = listOf(GrammarFixtures.rule("\\ba\\b(?=\\s+[aeiou])", "an")),
        )

        assertEquals("an engineer", result.corrected)
        assertEquals("a", result.issues.single().wrong.lowercase())
        assertEquals("an", result.issues.single().correct)
    }

    @Test
    fun articleBecomesABeforeConsonant() {
        val result = checker.check(
            "An teacher",
            rules = listOf(GrammarFixtures.rule("\\ban\\b(?=\\s+[^aeiou\\s])", "a")),
        )

        assertEquals("a teacher", result.corrected)
        assertEquals("an", result.issues.single().wrong.lowercase())
        assertEquals("a", result.issues.single().correct)
    }

    @Test
    fun missingVerbIsInsertedWithGroupReference() {
        val result = checker.check(
            "I from Turkey.",
            rules = listOf(GrammarFixtures.rule("\\bI\\s+(from)\\b", "I am $1")),
        )

        assertEquals("I am from Turkey.", result.corrected)
        assertEquals("I from", result.issues.single().wrong)
        assertEquals("I am from", result.issues.single().correct)
    }

    @Test
    fun questionWordOrderIsFixed() {
        val result = checker.check(
            "How you are?",
            rules = listOf(GrammarFixtures.rule("(?<=\\bHow\\s)you are\\b", "are you")),
        )

        assertEquals("How are you?", result.corrected)
        assertEquals("you are", result.issues.single().wrong)
        assertEquals("are you", result.issues.single().correct)
    }

    @Test
    fun ageExpressionIsFixedWithGroupReference() {
        val result = checker.check(
            "I have 20 years.",
            rules = listOf(GrammarFixtures.rule("\\bI have (\\d+) years\\b", "I am $1 years old")),
        )

        assertEquals("I am 20 years old.", result.corrected)
        assertEquals("I have 20 years", result.issues.single().wrong)
        assertEquals("I am 20 years old", result.issues.single().correct)
    }

    @Test
    fun prepositionIsFixed() {
        val result = checker.check(
            "In night",
            rules = listOf(GrammarFixtures.rule("\\bin night\\b", "at night")),
        )

        assertEquals("at night", result.corrected)
        assertEquals("in night", result.issues.single().wrong.lowercase())
        assertEquals("at night", result.issues.single().correct)
    }

    @Test
    fun possessiveIsFixed() {
        val result = checker.check(
            "I name is Ali.",
            rules = listOf(GrammarFixtures.rule("\\bI name\\b", "My name")),
        )

        assertEquals("My name is Ali.", result.corrected)
        assertEquals("I name", result.issues.single().wrong)
        assertEquals("My name", result.issues.single().correct)
    }

    @Test
    fun doQuestionIsFixed() {
        val result = checker.check(
            "You like tea?",
            rules = listOf(GrammarFixtures.rule("^You like\\b", "Do you like")),
        )

        assertEquals("Do you like tea?", result.corrected)
        assertEquals("You like", result.issues.single().wrong)
        assertEquals("Do you like", result.issues.single().correct)
    }

    @Test
    fun articleInAmStudentIsFixed() {
        val result = checker.check(
            "I am student.",
            rules = listOf(GrammarFixtures.rule("\\bam student\\b", "am a student")),
        )

        assertEquals("I am a student.", result.corrected)
        assertEquals("am student", result.issues.single().wrong)
        assertEquals("am a student", result.issues.single().correct)
    }

    @Test
    fun punctuationIsAddedForProperNoun() {
        val result = checker.check("I am Deniz")

        assertEquals("I am Deniz.", result.corrected)
        assertEquals(95, result.scorePercent)
        val issue = result.issues.single()
        assertEquals(GrammarIssueKind.PUNCTUATION, issue.kind)
        assertEquals("", issue.wrong)
        assertEquals(".", issue.correct)
    }

    @Test
    fun turkishSentenceGetsHintAndZeroScore() {
        val result = checker.check("Ben öğrenciyim.")

        assertEquals(0, result.scorePercent)
        assertFalse(result.correct)
        assertNotNull(result.hintTr)
        assertTrue(result.issues.isEmpty())
        assertEquals("Ben öğrenciyim.", result.corrected)
    }

    @Test
    fun repeatedWordIsRemoved() {
        val result = checker.check("The the cat is happy.")

        assertEquals("The cat is happy.", result.corrected)
        val issue = result.issues.single()
        assertEquals(GrammarIssueKind.GRAMMAR, issue.kind)
        assertEquals("The the", issue.wrong)
        assertEquals("The", issue.correct)
        assertEquals(80, result.scorePercent)
    }

    @Test
    fun doubleSpaceIsFixedSilently() {
        val result = checker.check("I am  happy.")

        assertEquals("I am happy.", result.corrected)
        assertTrue(result.issues.isEmpty())
        assertTrue(result.correct)
        assertEquals(100, result.scorePercent)
    }

    @Test
    fun shortSentenceStillGetsCheckedWithHintAndSmallPenalty() {
        val result = checker.check(
            "I am.",
            expectations = emptyList(),
            minWords = 3,
        )

        assertEquals(90, result.scorePercent)
        assertFalse(result.correct)
        assertNotNull(result.hintTr)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun missingExpectationWithAnchorBecomesMissingIssue() {
        val result = checker.check(
            "I student.",
            expectations = listOf(GrammarFixtures.expect("\\bI am\\b", "\\bI\\b")),
        )

        assertEquals("I student.", result.corrected)
        assertEquals(80, result.scorePercent)
        val issue = result.issues.single()
        assertEquals(GrammarIssueKind.MISSING, issue.kind)
        assertEquals("I", issue.wrong)
        assertEquals("I", issue.correct)
        assertEquals("Beklenen yapı eksik.", issue.noteTr)
    }

    @Test
    fun missingExpectationWithoutAnchorOnlyHints() {
        val result = checker.check(
            "I student.",
            expectations = listOf(GrammarFixtures.expect("\\bI am\\b", "\\bzzz\\b")),
        )

        assertFalse(result.correct)
        assertEquals(80, result.scorePercent)
        assertTrue(result.issues.isEmpty())
        assertEquals("Beklenen yapı eksik.", result.hintTr)
    }

    @Test
    fun questionMarkIsAddedWhenExpected() {
        val result = checker.check(
            "You like tea",
            expectQuestion = true,
        )

        assertEquals("You like tea?", result.corrected)
        assertEquals(95, result.scorePercent)
        assertEquals(GrammarIssueKind.PUNCTUATION, result.issues.single().kind)
        assertEquals("?", result.issues.single().correct)
    }

    @Test
    fun questionMarkIsAddedWhenTaskSampleIsQuestion() {
        val topic = GrammarFixtures.topic(
            tasks = listOf(
                GrammarFixtures.task(sample = "Do you like tea?"),
            ) + GrammarFixtures.tasks().drop(1),
        )

        val result = checker.check("You like tea", topic, topic.write.tasks.first())

        assertEquals("You like tea?", result.corrected)
        assertEquals(GrammarIssueKind.PUNCTUATION, result.issues.single().kind)
    }

    @Test
    fun overlappingRulesOnlyApplyFirst() {
        val rules = listOf(
            GrammarFixtures.rule("\\bI\\s+is\\b", "I am", "Birinci."),
            GrammarFixtures.rule("\\bI\\s+is\\b", "I are", "İkinci."),
        )

        val result = checker.check("I is a student.", rules = rules)

        assertEquals("I am a student.", result.corrected)
        assertEquals(1, result.issues.size)
        assertEquals("I am", result.issues.single().correct)
        assertEquals("Birinci.", result.issues.single().noteTr)
    }

    @Test
    fun unknownWordIsNotFlagged() {
        val result = checker.check("I am blorf happy.")

        assertTrue(result.issues.isEmpty())
        assertTrue(result.correct)
        assertEquals(100, result.scorePercent)
    }

    @Test
    fun commonTypoMapIsUsed() {
        val result = checker.check("I studnt here.")

        assertEquals("I student here.", result.corrected)
        val issue = result.issues.single()
        assertEquals(GrammarIssueKind.SPELLING, issue.kind)
        assertEquals("studnt", issue.wrong)
        assertEquals("student", issue.correct)
    }

    @Test
    fun turkishCharacterWordOnlyHints() {
        val result = checker.check("I am from Türkiye.")

        assertTrue(result.issues.isEmpty())
        assertFalse(result.correct)
        assertNotNull(result.hintTr)
        assertEquals(100, result.scorePercent)
    }

    @Test
    fun fourWordStatementGetsPunctuation() {
        val result = checker.check("I am from home")

        assertEquals("I am from home.", result.corrected)
        assertEquals(GrammarIssueKind.PUNCTUATION, result.issues.single().kind)
    }

    @Test
    fun issueRangesPointAtOriginalSentence() {
        val result = checker.check(
            "  I is a student.",
            rules = listOf(GrammarFixtures.rule("(?<=\\bI\\s)(is|are)\\b", "am")),
        )

        val issue = result.issues.single()
        assertEquals("is", result.original.substring(issue.start, issue.end))
        assertEquals("I am a student.", result.corrected)
    }
}
