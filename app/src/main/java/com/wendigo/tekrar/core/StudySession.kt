package com.wendigo.tekrar.core

import com.wendigo.tekrar.data.Word

class StudySession {

    private val queue = ArrayDeque<Word>()
    private var currentId: Long? = null
    private var busy = false

    val current: Word?
        get() = queue.firstOrNull()?.takeIf { it.id == currentId }

    val isBusy: Boolean get() = busy

    fun load(words: List<Word>) {
        queue.clear()
        queue.addAll(words)
        currentId = queue.firstOrNull()?.id
    }

    fun occupy(): Boolean {
        if (busy) return false
        busy = true
        return true
    }

    fun begin(targetId: Long? = null): Word? {
        val word = current ?: return null
        if (targetId != null && targetId != word.id) return null
        if (!occupy()) return null
        return word
    }

    fun finish(recordedId: Long, requeue: Word? = null): Boolean {
        if (queue.firstOrNull()?.id != recordedId) return false
        queue.removeFirst()
        if (requeue != null) queue.addLast(requeue)
        return true
    }

    fun advance(): Word? {
        currentId = queue.firstOrNull()?.id
        return current
    }

    fun release() {
        busy = false
    }

    fun undo(word: Word) {
        queue.removeAll { it.id == word.id }
        queue.addFirst(word)
    }
}
