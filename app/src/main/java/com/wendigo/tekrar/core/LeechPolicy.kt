package com.wendigo.tekrar.core

import com.wendigo.tekrar.data.Word
import java.util.Locale

object LeechPolicy {

    fun isLeech(word: Word, threshold: Int = Word.LEECH_LAPSES): Boolean =
        !word.suspended && word.lapses >= threshold

    fun leeches(words: List<Word>, threshold: Int = Word.LEECH_LAPSES): List<Word> =
        words.asSequence()
            .filter { isLeech(it, threshold) }
            .sortedWith(
                compareByDescending<Word> { it.lapses }
                    .thenBy { it.dueAt }
                    .thenBy { it.term.lowercase(Locale.ROOT) }
            )
            .toList()
}
