package com.wendigo.tekrar.core.grammar

import java.util.Locale

/**
 * Çevrimdışı gramer kontrol motoru (kontrat 3).
 *
 * Deterministik: aynı metin + aynı kurallar her zaman aynı sonucu üretir.
 * Android/Room/AI bağımlılığı yoktur.
 */
class GrammarChecker(
    private val lexicon: SpellingLexicon = SpellingLexicon.of(emptyList()),
) {

    fun check(
        text: String,
        topic: GrammarTopic,
        task: GrammarWriteTask? = null,
        expectQuestion: Boolean? = null,
    ): GrammarCheckResult = check(
        text = text,
        rules = topic.rules,
        expectations = task?.expect.orEmpty(),
        minWords = task?.minWords ?: 1,
        expectQuestion = expectQuestion
            ?: task?.sample?.trim()?.endsWith("?")?.takeIf { it },
    )

    fun check(
        text: String,
        rules: List<GrammarRule> = emptyList(),
        expectations: List<GrammarExpectation> = emptyList(),
        minWords: Int = 1,
        expectQuestion: Boolean? = null,
    ): GrammarCheckResult {
        val original = text
        val analysis = text.trim()
        val offset = text.length - text.trimStart().length

        if (analysis.isEmpty()) {
            return GrammarCheckResult(
                original = original,
                corrected = "",
                issues = emptyList(),
                correct = false,
                scorePercent = 0,
                hintTr = "Bir cümle yaz.",
            )
        }

        val wordTokens = WORD_TOKEN.findAll(analysis).toList()
        val turkishWords = wordTokens.filter { SpellingLexicon.containsTurkishLetter(it.value) }
        val turkishRanges = turkishWords.map { it.range }
        val hintParts = LinkedHashSet<String>()
        if (turkishWords.isNotEmpty()) hintParts += "İngilizce yazmayı dene."
        if (turkishWords.isNotEmpty() && turkishWords.size * 2 >= wordTokens.size) {
            return GrammarCheckResult(
                original = original,
                corrected = analysis,
                issues = emptyList(),
                correct = false,
                scorePercent = 0,
                hintTr = hintParts.joinToString(" "),
            )
        }

        if (wordTokens.size < minWords) {
            // Kısa cümle yine kontrol edilir; yalnızca ek ipucu ve küçük ceza alır.
            hintParts += "En az $minWords kelimeyle yaz."
        }

        val issues = ArrayList<GrammarIssue>()
        val edits = ArrayList<Edit>()
        val ruleClaims = ArrayList<IntRange>()

        var working = analysis
        val firstLetter = working.indexOfFirst { it.isLetter() }
        if (firstLetter >= 0 && working[firstLetter].isLowerCase()) {
            val lower = working[firstLetter].toString()
            val upper = working[firstLetter].uppercaseChar().toString()
            issues += GrammarIssue(
                start = offset + firstLetter,
                end = offset + firstLetter + 1,
                wrong = lower,
                correct = upper,
                noteTr = "Cümleye büyük harfle başla.",
                kind = GrammarIssueKind.CAPITALIZATION,
            )
            working = working.replaceRange(firstLetter, firstLetter + 1, upper)
        }

        // Tekrarlanan kelime: "the the" -> "the" (GRAMMAR).
        for (match in REPEATED_WORD.findAll(working)) {
            val range = match.range
            if (range.isEmpty()) continue
            val word = match.groupValues[1]
            issues += GrammarIssue(
                start = offset + range.first,
                end = offset + range.last + 1,
                wrong = analysis.substring(range.first, range.last + 1),
                correct = word,
                noteTr = "Kelime tekrarını kaldır.",
                kind = GrammarIssueKind.GRAMMAR,
            )
            edits += Edit(range, word)
            ruleClaims += range
        }

        // Konu kuralları: sırayla; çakışan sonraki eşleşmeler atlanır.
        for (rule in rules) {
            val regex = runCatching { Regex(rule.pattern, RegexOption.IGNORE_CASE) }
                .getOrNull() ?: continue
            for (match in regex.findAll(working)) {
                val range = match.range
                if (range.isEmpty()) continue
                if (ruleClaims.any { it.overlaps(range) }) continue
                val replacement = expandReplacement(match, rule.replace)
                ruleClaims += range
                if (replacement == match.value) continue
                issues += GrammarIssue(
                    start = offset + range.first,
                    end = offset + range.last + 1,
                    wrong = analysis.substring(range.first, range.last + 1),
                    correct = replacement,
                    noteTr = rule.noteTr,
                    kind = GrammarIssueKind.GRAMMAR,
                )
                edits += Edit(range, replacement)
            }
        }

        // Yazım: kural/repeat aralıklarına girmeyen ASCII kelimeler.
        for (match in ASCII_WORD.findAll(working)) {
            val range = match.range
            val token = match.value
            if (range.isEmpty()) continue
            if (turkishRanges.any { it.first <= range.first && range.last <= it.last }) continue
            if (ruleClaims.any { it.overlaps(range) }) continue
            if (edits.any { it.range.overlaps(range) }) continue
            if (token.lowercase(Locale.ROOT) in TURKISH_WORD_GUARDS) continue
            if (lexicon.isKnown(token)) continue
            val sentenceInitial = working.substring(0, range.first).none { it.isLetter() }
            val suggestion = lexicon.suggestion(token, sentenceInitial) ?: continue
            issues += GrammarIssue(
                start = offset + range.first,
                end = offset + range.last + 1,
                wrong = analysis.substring(range.first, range.last + 1),
                correct = suggestion,
                noteTr = "Doğru yazım: $suggestion",
                kind = GrammarIssueKind.SPELLING,
            )
            edits += Edit(range, suggestion)
        }

        // Noktalama.
        val lastChar = working.lastOrNull()
        val question = expectQuestion ?: startsWithQuestionWord(working)
        val endsSentence = lastChar == '.' || lastChar == '?' || lastChar == '!'
        if (endsSentence) {
            if (question && lastChar == '.') {
                val index = working.length - 1
                if (edits.none { it.range.contains(index) }) {
                    issues += GrammarIssue(
                        start = offset + index,
                        end = offset + index + 1,
                        wrong = ".",
                        correct = "?",
                        noteTr = "Soru cümlesi '?' ile biter.",
                        kind = GrammarIssueKind.PUNCTUATION,
                    )
                    edits += Edit(index..index, "?")
                }
            }
        } else {
            val properNoun = hasProperNoun(working)
            val needsPunctuation = question || wordTokens.size >= 4 || properNoun
            if (needsPunctuation) {
                val mark = if (question) "?" else "."
                issues += GrammarIssue(
                    start = offset + working.length,
                    end = offset + working.length,
                    wrong = "",
                    correct = mark,
                    noteTr = if (question) {
                        "Soru cümlesi '?' ile bitmeli."
                    } else {
                        "Cümle noktalama işaretiyle bitmeli."
                    },
                    kind = GrammarIssueKind.PUNCTUATION,
                )
                edits += Edit(working.length..working.length - 1, mark)
            }
        }

        // Çift boşluk: sessizce düzeltilir (issue yok).
        for (match in DOUBLE_SPACE.findAll(working)) {
            val range = match.range
            if (edits.any { it.range.overlaps(range) }) continue
            edits += Edit(range, " ")
        }

        // Görev beklentileri (expect): eksik yapı -20; anchor bulunursa MISSING.
        var expectedFailures = 0
        for (expectation in expectations) {
            val pattern = runCatching { Regex(expectation.pattern, RegexOption.IGNORE_CASE) }
                .getOrNull() ?: continue
            if (pattern.containsMatchIn(working)) continue
            expectedFailures++
            val anchorRegex = runCatching { Regex(expectation.anchor, RegexOption.IGNORE_CASE) }
                .getOrNull()
            val anchor = anchorRegex?.find(working)
            if (anchor == null) {
                hintParts += expectation.noteTr
            } else if (issues.none {
                    it.kind == GrammarIssueKind.MISSING &&
                        it.start == offset + anchor.range.first &&
                        it.end == offset + anchor.range.last + 1
                }
            ) {
                val anchorText = analysis.substring(
                    anchor.range.first,
                    anchor.range.last + 1,
                )
                issues += GrammarIssue(
                    start = offset + anchor.range.first,
                    end = offset + anchor.range.last + 1,
                    wrong = anchorText,
                    correct = anchorText,
                    noteTr = expectation.noteTr,
                    kind = GrammarIssueKind.MISSING,
                )
            }
        }

        val corrected = applyEdits(working, edits)
            .replace(DOUBLE_SPACE, " ")
            .trim()

        val grammarPenalty = issues.count { it.kind == GrammarIssueKind.GRAMMAR } * 20
        val spellingPenalty = issues.count { it.kind == GrammarIssueKind.SPELLING } * 15
        val punctuationPenalty = issues.count {
            it.kind == GrammarIssueKind.CAPITALIZATION ||
                it.kind == GrammarIssueKind.PUNCTUATION
        } * 5
        val expectationPenalty = expectedFailures * 20
        val shortPenalty = if (wordTokens.size < minWords) 10 else 0
        val score = (100 - grammarPenalty - spellingPenalty - punctuationPenalty -
            expectationPenalty - shortPenalty).coerceIn(0, 100)

        val hint = hintParts.takeIf { it.isNotEmpty() }?.joinToString(" ")
        val sortedIssues = issues.sortedWith(
            compareBy({ it.start }, { it.end }, { it.kind.ordinal }),
        )
        return GrammarCheckResult(
            original = original,
            corrected = corrected,
            issues = sortedIssues,
            correct = sortedIssues.isEmpty() && hint == null,
            scorePercent = score,
            hintTr = hint,
        )
    }

    private fun applyEdits(text: String, edits: List<Edit>): String {
        if (edits.isEmpty()) return text
        val applied = ArrayList<Edit>(edits.size)
        var result = text
        for (edit in edits.sortedWith(compareByDescending<Edit> { it.range.first }
            .thenByDescending { it.range.last })) {
            if (applied.any { it.conflicts(edit) }) continue
            applied += edit
            result = result.replaceRange(edit.range.first, edit.range.last + 1, edit.replacement)
        }
        return result
    }

    private fun startsWithQuestionWord(text: String): Boolean {
        val first = ASCII_WORD.find(text)?.value?.lowercase(Locale.ROOT) ?: return false
        return first in QUESTION_STARTERS
    }

    private fun hasProperNoun(text: String): Boolean = ASCII_WORD.findAll(text)
        .drop(1)
        .any { match ->
            val word = match.value
            word.length > 1 && word != "I" && word.first().isUpperCase()
        }

    private fun expandReplacement(match: MatchResult, replacement: String): String {
        val builder = StringBuilder(replacement.length)
        var index = 0
        while (index < replacement.length) {
            val char = replacement[index]
            if (char == '\\' && index + 1 < replacement.length &&
                replacement[index + 1] == '$'
            ) {
                builder.append('$')
                index += 2
                continue
            }
            if (char == '$' && index + 1 < replacement.length) {
                val next = replacement[index + 1]
                if (next.isDigit()) {
                    val group = next.digitToInt()
                    builder.append(match.groupValues.getOrElse(group) { "" })
                    index += 2
                    continue
                }
            }
            builder.append(char)
            index++
        }
        return builder.toString()
    }

    private data class Edit(
        val range: IntRange,
        val replacement: String,
    ) {
        fun conflicts(other: Edit): Boolean {
            if (range.isEmpty() && other.range.isEmpty()) return range.first == other.range.first
            if (range.isEmpty()) {
                return other.range.first < range.first && range.first <= other.range.last
            }
            if (other.range.isEmpty()) {
                return range.first < other.range.first && other.range.first <= range.last
            }
            return range.overlaps(other.range)
        }
    }

    private companion object {

        val WORD_TOKEN = Regex("[\\p{L}]+(?:['’][\\p{L}]+)*")
        val ASCII_WORD = Regex("[A-Za-z]+(?:'[A-Za-z]+)*")
        val REPEATED_WORD = Regex("\\b([A-Za-z]+)\\s+\\1\\b", RegexOption.IGNORE_CASE)
        val DOUBLE_SPACE = Regex("[ \\t]{2,}")

        val QUESTION_STARTERS = setOf(
            "am", "is", "are", "was", "were", "do", "does", "did",
            "can", "could", "will", "would", "shall", "should",
            "have", "has", "had", "what", "where", "when", "who", "whom",
            "whose", "why", "how", "which",
        )

        /** Türkçe cümlede yanlışlıkla İngilizce sanılabilecek temel kelimeler. */
        val TURKISH_WORD_GUARDS = setOf(
            "ben", "sen", "biz", "siz", "onlar", "bu", "su", "ve", "bir",
            "iki", "uc", "dort", "bes", "evet", "hayir", "merhaba",
            "nasilsin", "iyiyim", "tesekkurler", "lutfen", "ogrenci",
            "ogretmen", "ogrenciyim", "istiyorum", "seviyorum", "degil",
            "var", "yok", "cok", "az", "iyi", "kotu", "gunaydin", "aksamlar",
            "hos", "geldin", "gorusuruz",
        )
    }
}

private fun IntRange.overlaps(other: IntRange): Boolean =
    first <= other.last && other.first <= last
