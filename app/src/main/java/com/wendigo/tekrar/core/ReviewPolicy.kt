package com.wendigo.tekrar.core

import com.wendigo.tekrar.data.Word

/**
 * Tekrar sonrası askıya alma ve geri alma kararları. Android bağımlılığı yoktur.
 */
object ReviewPolicy {

    fun shouldAutoSuspend(
        lapses: Int,
        suspended: Boolean,
        threshold: Int = Word.LEECH_LAPSES,
    ): Boolean = !suspended && lapses >= threshold

    fun withAutoSuspend(word: Word, threshold: Int = Word.LEECH_LAPSES): Word =
        if (shouldAutoSuspend(word.lapses, word.suspended, threshold)) {
            word.copy(suspended = true)
        } else {
            word
        }

    /**
     * Geri almada yalnızca SRS alanlarını ve askı durumunu önceki hale döndürür;
     * kelimenin içeriğinde bu arada yapılan düzenlemeler korunur.
     */
    fun restoreAfterUndo(current: Word, previous: Word): Word = current.copy(
        state = previous.state,
        stability = previous.stability,
        difficulty = previous.difficulty,
        dueAt = previous.dueAt,
        lastReviewAt = previous.lastReviewAt,
        introducedAt = previous.introducedAt,
        reps = previous.reps,
        lapses = previous.lapses,
        suspended = previous.suspended,
    )
}
