package com.wendigo.tekrar.core.learn

import kotlin.math.roundToInt

/**
 * Ders alıştırmaları için deterministik puanlama (kontrat 2.4).
 *
 * AI kullanılmaz; aynı cevap her zaman aynı [GradeResult] üretir. Her ders
 * AI kapalıyken de tamamlanabilir.
 */
object ExerciseGrader {

    fun grade(exercise: Exercise, answer: UserAnswer): GradeResult = when (exercise) {
        is Choice -> gradeIndexAnswer(
            options = exercise.options,
            correctIndex = exercise.answer,
            explanation = exercise.explanationTr,
            answer = answer,
        )

        is ListenChoice -> gradeIndexAnswer(
            options = exercise.options,
            correctIndex = exercise.answer,
            explanation = exercise.explanationTr,
            answer = answer,
        )

        is DialogueChoice -> gradeIndexAnswer(
            options = exercise.options,
            correctIndex = exercise.answer,
            explanation = exercise.explanationTr,
            answer = answer,
        )

        is Match -> gradeMatch(exercise, answer)
        is WordOrder -> gradeWordOrder(exercise, answer)
        is Cloze -> gradeCloze(exercise, answer)
        is Translate -> gradeTranslate(exercise, answer)
        is Speak -> gradeSpeak(exercise, answer)
        is Write -> gradeWrite(exercise, answer)
    }

    private fun gradeIndexAnswer(
        options: List<String>,
        correctIndex: Int,
        explanation: String?,
        answer: UserAnswer,
    ): GradeResult {
        val expected = options.getOrElse(correctIndex) { "" }
        val picked = (answer as? UserAnswer.Choice)?.index
        val correct = picked == correctIndex
        val feedback = if (correct) {
            if (explanation.isNullOrBlank()) "Doğru!" else "Doğru! $explanation"
        } else {
            buildString {
                append("Doğru cevap: ").append(expected)
                if (!explanation.isNullOrBlank()) append(' ').append(explanation)
            }
        }
        return GradeResult(
            correct = correct,
            scorePercent = if (correct) 100 else 0,
            expectedAnswer = expected,
            feedbackTr = feedback,
        )
    }

    private fun gradeMatch(exercise: Match, answer: UserAnswer): GradeResult {
        val expected = exercise.pairs.associate {
            AnswerNormalizer.normalize(it.left) to AnswerNormalizer.normalize(it.right)
        }
        val actual = (answer as? UserAnswer.Pairs)?.values.orEmpty().entries.associate {
            AnswerNormalizer.normalize(it.key) to AnswerNormalizer.normalize(it.value)
        }
        val matched = expected.count { (left, right) -> actual[left] == right }
        val total = maxOf(expected.size, actual.size)
        val correct = actual == expected
        val score = if (total == 0) {
            if (correct) 100 else 0
        } else {
            (100.0 * matched / total).roundToInt().coerceIn(0, 100)
        }
        val expectedText = exercise.pairs.joinToString(" · ") { "${it.left} = ${it.right}" }
        val feedback = if (correct) {
            if (exercise.explanationTr.isNullOrBlank()) {
                "Tüm eşleşmeler doğru!"
            } else {
                "Tüm eşleşmeler doğru! ${exercise.explanationTr}"
            }
        } else {
            buildString {
                append("Doğru eşleşme: ").append(matched).append('/').append(expected.size).append(". ")
                append("Doğru cevap: ").append(expectedText)
                if (!exercise.explanationTr.isNullOrBlank()) append(' ').append(exercise.explanationTr)
            }
        }
        return GradeResult(correct, score, expectedText, feedback)
    }

    private fun gradeWordOrder(exercise: WordOrder, answer: UserAnswer): GradeResult {
        val expected = exercise.answer
        val picked = (answer as? UserAnswer.Tokens)?.values.orEmpty()
        val correct = picked.size == expected.size &&
            picked.map(AnswerNormalizer::normalize) == expected.map(AnswerNormalizer::normalize)
        val expectedText = expected.joinToString(" ")
        val feedback = if (correct) {
            if (exercise.explanationTr.isNullOrBlank()) "Doğru sıra!" else "Doğru sıra! ${exercise.explanationTr}"
        } else {
            buildString {
                append("Doğru sıra: ").append(expectedText)
                if (!exercise.explanationTr.isNullOrBlank()) append(' ').append(exercise.explanationTr)
            }
        }
        return GradeResult(correct, if (correct) 100 else 0, expectedText, feedback)
    }

    private fun gradeCloze(exercise: Cloze, answer: UserAnswer): GradeResult {
        val options = exercise.options
        val answerIndex = exercise.answer
        if (options != null && answerIndex != null) {
            return gradeIndexAnswer(options, answerIndex, exercise.explanationTr, answer)
        }
        val accepted = exercise.answers.orEmpty()
        val expected = accepted.firstOrNull().orEmpty()
        val typed = (answer as? UserAnswer.Text)?.value
        val correct = typed != null &&
            accepted.any { AnswerNormalizer.normalizeLoose(it) == AnswerNormalizer.normalizeLoose(typed) }
        val feedback = if (correct) {
            if (exercise.explanationTr.isNullOrBlank()) "Doğru!" else "Doğru! ${exercise.explanationTr}"
        } else {
            buildString {
                append("Doğru cevap: ").append(expected)
                if (!exercise.explanationTr.isNullOrBlank()) append(' ').append(exercise.explanationTr)
            }
        }
        return GradeResult(correct, if (correct) 100 else 0, expected, feedback)
    }

    private fun gradeTranslate(exercise: Translate, answer: UserAnswer): GradeResult {
        val typed = (answer as? UserAnswer.Text)?.value
        val expected = exercise.answers.firstOrNull().orEmpty()
        val correct = typed != null && exercise.answers.any {
            AnswerNormalizer.normalizeLoose(it) == AnswerNormalizer.normalizeLoose(typed)
        }
        val feedback = if (correct) {
            if (exercise.explanationTr.isNullOrBlank()) "Doğru çeviri!" else "Doğru çeviri! ${exercise.explanationTr}"
        } else {
            buildString {
                append("Doğru cevap: ").append(expected)
                if (!exercise.hintTr.isNullOrBlank()) append(' ').append(exercise.hintTr)
            }
        }
        return GradeResult(correct, if (correct) 100 else 0, expected, feedback)
    }

    private fun gradeSpeak(exercise: Speak, answer: UserAnswer): GradeResult {
        val transcript = when (answer) {
            is UserAnswer.Speech -> answer.transcript
            is UserAnswer.Text -> answer.value
            else -> null
        }
        if (transcript == null) {
            return GradeResult(
                correct = false,
                scorePercent = 0,
                expectedAnswer = exercise.target,
                feedbackTr = "Sesli cevap bekleniyordu. Hedef: ${exercise.target}",
            )
        }
        val score = SpeechMatcher.score(exercise.target, transcript)
        val correct = score >= SpeechMatcher.PASS_SCORE
        val detail = if (correct) {
            "Benzerlik: %$score. Yeterli."
        } else {
            "Benzerlik: %$score. Biraz daha dene."
        }
        val feedback = if (!exercise.explanationTr.isNullOrBlank()) {
            "$detail ${exercise.explanationTr}"
        } else {
            detail
        }
        return GradeResult(correct, score, exercise.target, feedback)
    }

    private fun gradeWrite(exercise: Write, answer: UserAnswer): GradeResult {
        val text = (answer as? UserAnswer.Text)?.value.orEmpty()
        val wordCount = AnswerNormalizer.wordCount(text)
        val answerWords = AnswerNormalizer.words(text)
        val missingKeywords = exercise.keywords.filterNot {
            AnswerNormalizer.containsPhrase(answerWords, it)
        }
        val enoughWords = wordCount >= exercise.minWords
        val correct = enoughWords && missingKeywords.isEmpty()
        val expected = exercise.sample
            ?: buildString {
                append("En az ").append(exercise.minWords).append(" kelime")
                if (exercise.keywords.isNotEmpty()) {
                    append("; anahtar kelimeler: ").append(exercise.keywords.joinToString(", "))
                }
            }
        val feedback = if (correct) {
            if (exercise.explanationTr.isNullOrBlank()) {
                "Güzel! $wordCount kelime yazdın."
            } else {
                "Güzel! $wordCount kelime yazdın. ${exercise.explanationTr}"
            }
        } else {
            buildString {
                if (!enoughWords) {
                    append("En az ").append(exercise.minWords)
                        .append(" kelime gerekiyor (yazdığın: ").append(wordCount).append(").")
                }
                if (missingKeywords.isNotEmpty()) {
                    if (isNotEmpty()) append(' ')
                    append("Eksik anahtar kelimeler: ")
                        .append(missingKeywords.joinToString(", ")).append('.')
                }
                if (!exercise.rubricTr.isNullOrBlank()) {
                    if (isNotEmpty()) append(' ')
                    append(exercise.rubricTr)
                }
            }
        }
        return GradeResult(correct, if (correct) 100 else 0, expected, feedback)
    }
}
