package com.wendigo.tekrar.core

import com.wendigo.tekrar.data.Word
import com.wendigo.tekrar.ui.contracts.StudyMode
import java.util.Locale

object CustomSession {

    const val ALL_DECKS_ARG = "@all"
    const val LIBRARY_DECK_ARG = "@library"
    const val ALL_TAGS_ARG = "@all-tags"

    data class Query(
        val mode: StudyMode = StudyMode.MIXED,
        val deck: String? = null,
        val tag: String? = null,
        val maxCards: Int? = null,
        val cram: Boolean = false,
    )

    private val dueOrder = compareBy<Word>({ it.dueAt }, { it.id })

    fun parseTags(raw: String): List<String> =
        raw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun normalizeTag(raw: String): String = raw.trim().lowercase(Locale.ROOT)

    fun hasTag(word: Word, tag: String?): Boolean {
        if (tag.isNullOrBlank()) return true
        val needle = normalizeTag(tag)
        return parseTags(word.tags).any { normalizeTag(it) == needle }
    }

    fun tags(words: List<Word>): List<String> =
        words.asSequence()
            .flatMap { parseTags(it.tags).asSequence() }
            .distinctBy { normalizeTag(it) }
            .sortedBy { normalizeTag(it) }
            .toList()

    fun matchesScope(word: Word, deck: String?, tag: String?): Boolean =
        !word.suspended &&
            (deck == null || word.deck == deck) &&
            hasTag(word, tag)

    fun selectQueue(
        words: List<Word>,
        now: Long,
        query: Query,
        shuffle: (List<Word>) -> List<Word> = { it.shuffled() },
    ): List<Word> {
        val scoped = words.asSequence()
            .filter { matchesScope(it, query.deck, query.tag) }
            .toList()
        val selected = when {
            query.cram -> shuffle(scoped)
            query.mode == StudyMode.LEECH -> LeechPolicy.leeches(scoped)
            query.mode == StudyMode.NEW -> scoped
                .filter { it.isNew }
                .sortedBy { it.id }
            query.mode == StudyMode.REVIEW -> scoped
                .filter { !it.isNew && it.dueAt <= now }
                .sortedWith(dueOrder)
            else -> {
                val due = scoped
                    .filter { !it.isNew && it.dueAt <= now }
                    .sortedWith(dueOrder)
                val fresh = scoped
                    .filter { it.isNew }
                    .sortedBy { it.id }
                due + fresh
            }
        }
        val limit = query.maxCards?.takeIf { it > 0 } ?: return selected
        return selected.take(limit)
    }

    fun deckArg(deck: String?): String = when (deck) {
        null -> ALL_DECKS_ARG
        "" -> LIBRARY_DECK_ARG
        else -> deck
    }

    fun deckFromArg(arg: String?): String? = when (arg) {
        null, "", ALL_DECKS_ARG -> null
        LIBRARY_DECK_ARG -> ""
        else -> arg
    }

    fun tagArg(tag: String?): String =
        tag?.trim()?.takeIf { it.isNotEmpty() } ?: ALL_TAGS_ARG

    fun tagFromArg(arg: String?): String? =
        arg?.takeIf { it.isNotBlank() && it != ALL_TAGS_ARG }
}
