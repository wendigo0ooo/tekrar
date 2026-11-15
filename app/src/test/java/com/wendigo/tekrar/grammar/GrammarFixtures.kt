package com.wendigo.tekrar.grammar

import com.wendigo.tekrar.core.grammar.GrammarChecker
import com.wendigo.tekrar.core.grammar.GrammarExpectation
import com.wendigo.tekrar.core.grammar.GrammarRule
import com.wendigo.tekrar.core.grammar.GrammarTipStep
import com.wendigo.tekrar.core.grammar.GrammarTopic
import com.wendigo.tekrar.core.grammar.GrammarWrite
import com.wendigo.tekrar.core.grammar.GrammarWriteTask
import com.wendigo.tekrar.core.grammar.SpellingLexicon

internal object GrammarFixtures {

    fun lexicon(): SpellingLexicon = SpellingLexicon.of(
        listOf(
            "engineer", "cat", "cats", "tea", "student", "happy", "school",
            "tired", "swim", "go", "like", "from", "turkey", "night", "name",
            "ali", "deniz", "here",
        ),
    )

    fun checker(): GrammarChecker = GrammarChecker(lexicon())

    fun tasks(expect: List<GrammarExpectation> = emptyList()): List<GrammarWriteTask> =
        (1..3).map { index ->
            GrammarWriteTask(
                id = "w$index",
                promptTr = "Test görevi $index",
                hintTr = null,
                sample = "I am here.",
                minWords = 2,
                expect = expect,
            )
        }

    fun task(
        id: String = "w1",
        minWords: Int = 2,
        sample: String = "I am here.",
        expect: List<GrammarExpectation> = emptyList(),
    ): GrammarWriteTask = GrammarWriteTask(
        id = id,
        promptTr = "Test görevi",
        hintTr = null,
        sample = sample,
        minWords = minWords,
        expect = expect,
    )

    fun topic(
        rules: List<GrammarRule> = emptyList(),
        tasks: List<GrammarWriteTask> = tasks(),
    ): GrammarTopic = GrammarTopic(
        schema = 1,
        id = "g-test",
        cefr = "A1",
        titleTr = "Test",
        subtitleTr = "Test konusu",
        objectiveTr = "Test amacı",
        estMin = 3,
        sourceNotes = emptyList(),
        steps = listOf(
            GrammarTipStep("Bir", "Gövde bir."),
            GrammarTipStep("İki", "Gövde iki."),
            GrammarTipStep("Üç", "Gövde üç."),
        ),
        rules = rules,
        write = GrammarWrite(
            introTr = "Test girişi",
            tasks = tasks,
        ),
    )

    fun rule(pattern: String, replace: String, note: String = "Kural."): GrammarRule =
        GrammarRule(pattern = pattern, replace = replace, noteTr = note)

    fun expect(
        pattern: String,
        anchor: String,
        note: String = "Beklenen yapı eksik.",
    ): GrammarExpectation = GrammarExpectation(pattern, anchor, note)
}
