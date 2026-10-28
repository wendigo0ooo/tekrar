package com.wendigo.tekrar.core

/**
 * Builds a fill-in-the-blank prompt from an example sentence.
 *
 * Matching is whole-word and case-insensitive, but only exact surface forms
 * match: an example containing "ran" will not match the term "run", because
 * the blank must be answered with the word that is actually in the sentence.
 * Inflections and derived forms are therefore not matched.
 *
 * Apostrophes are treated as part of a word when they join two word
 * characters, so "don't" stays intact and "don" does not match inside it.
 * Hyphens are boundaries: "known" matches in "well-known", while the whole
 * hyphenated form can also be used as a term.
 */
object Cloze {

    const val BLANK = "_____"

    data class ClozePrompt(val sentence: String, val answer: String)

    fun build(example: String, term: String): ClozePrompt? {
        val text = example.trim()
        val word = term.trim()
        if (text.isEmpty() || word.isEmpty()) return null
        var index = text.indexOf(word, ignoreCase = true)
        while (index >= 0) {
            val end = index + word.length
            if (isStartBoundary(text, index) && isEndBoundary(text, end)) {
                val remainder = text.removeRange(index, end)
                if (remainder.none { Character.isLetterOrDigit(it) }) return null
                val sentence = text.substring(0, index) + BLANK + text.substring(end)
                return ClozePrompt(sentence, text.substring(index, end))
            }
            index = text.indexOf(word, index + 1, ignoreCase = true)
        }
        return null
    }

    private fun isStartBoundary(text: String, index: Int): Boolean {
        if (index == 0) return true
        val before = text[index - 1]
        if (isWordChar(before)) return false
        if (!isConnector(before)) return true
        return index < 2 || !isWordChar(text[index - 2])
    }

    private fun isEndBoundary(text: String, end: Int): Boolean {
        if (end == text.length) return true
        val after = text[end]
        if (isWordChar(after)) return false
        if (!isConnector(after)) return true
        return end + 1 >= text.length || !isWordChar(text[end + 1])
    }

    private fun isWordChar(value: Char): Boolean = Character.isLetterOrDigit(value)

    private fun isConnector(value: Char): Boolean =
        value == '\'' || value == '\u2019'
}
