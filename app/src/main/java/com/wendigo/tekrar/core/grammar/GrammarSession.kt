package com.wendigo.tekrar.core.grammar

import kotlin.math.roundToInt

/**
 * Bir konunun "Sıra sende" akışı: 3 yazma görevi tek tek.
 *
 * Kural: kontrol edilmeden sonraki göreve geçilmez; kullanıcı istediği kadar
 * "tekrar dene" diyebilir. Sonuçlar yalnızca [next] çağrısında kaydedilir.
 */
class GrammarSession(
    val topic: GrammarTopic,
    private val checker: GrammarChecker,
    startIndex: Int = 0,
) {

    private val tasks: List<GrammarWriteTask> = topic.write.tasks
    private val recorded = ArrayList<GrammarCheckResult>()

    var currentIndex: Int = startIndex.coerceIn(0, tasks.size)
        private set

    var lastResult: GrammarCheckResult? = null
        private set

    val taskCount: Int get() = tasks.size

    val currentTask: GrammarWriteTask?
        get() = if (isFinished) null else tasks.getOrNull(currentIndex)

    val isFinished: Boolean get() = currentIndex >= tasks.size

    val checkedCount: Int get() = recorded.size

    val results: List<GrammarCheckResult> get() = recorded.toList()

    val correctCount: Int get() = recorded.count { it.correct }

    /** Tamamlanan görevlerin ortalama skoru (yuvarlanmış). */
    val averageScore: Int
        get() = if (recorded.isEmpty()) {
            0
        } else {
            (recorded.sumOf { it.scorePercent }.toDouble() / recorded.size).roundToInt()
        }

    /** Geçerli görevi kontrol eder; sonucu kaydetmez (tekrar denenebilir). */
    fun check(text: String): GrammarCheckResult? {
        val task = currentTask ?: return null
        return checker.check(text, topic, task).also { lastResult = it }
    }

    /** Aynı görevde yeni deneme için sonucu temizler. */
    fun retry() {
        lastResult = null
    }

    /** Kontrol edilmiş sonucu kaydedip sonraki göreve geçer. */
    fun next(): Boolean {
        if (isFinished) return true
        val result = lastResult ?: return false
        recorded += result
        lastResult = null
        currentIndex++
        return isFinished
    }

    fun restart() {
        recorded.clear()
        lastResult = null
        currentIndex = 0
    }

    fun summary(): GrammarSessionSummary = GrammarSessionSummary(
        taskCount = tasks.size,
        checkedCount = recorded.size,
        correctCount = correctCount,
        averageScore = averageScore,
        issues = recorded.flatMap { it.issues },
    )
}
