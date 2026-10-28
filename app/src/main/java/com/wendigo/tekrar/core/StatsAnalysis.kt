package com.wendigo.tekrar.core

import com.wendigo.tekrar.data.ReviewLog
import com.wendigo.tekrar.data.Word
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Bir aralığa (geçen gün) göre tutundurma. [reviews] o aralıkta yapılan
 * tekrarların sayısı, [correct] bunlardan doğru (rating >= GOOD) olanlar.
 */
data class RetentionBucket(
    val label: String,
    val reviews: Int,
    val correct: Int,
) {
    val accuracy: Double? get() = if (reviews == 0) null else correct.toDouble() / reviews
}

data class RetentionStats(
    val trueRetention: Double?,
    val matureRetention: Double?,
    val buckets: List<RetentionBucket>,
    val sample: Int,
)

/**
 * Etiket bazlı performans. [reviews]/[correct] yalnızca var olan kelimelerin
 * günlüklerinden gelir; silinmiş kelimelerin günlükleri yok sayılır.
 */
data class TagPerformance(
    val tag: String,
    val wordCount: Int,
    val reviews: Int,
    val correct: Int,
    val dueNow: Int,
    val leeches: Int,
) {
    val accuracy: Double? get() = if (reviews == 0) null else correct.toDouble() / reviews
}

data class StatsSnapshot(
    val byDay: Map<LocalDate, Int>,
    val retention: RetentionStats,
    val tagPerformance: List<TagPerformance>,
    val forecast: List<DayCount>,
    val workload7: Int,
    val workload30: Int,
    val overdueNow: Int,
)

/**
 * İstatistik ekranının saf hesaplamaları. Girdi listeleri değiştirilmez.
 *
 * Tanımlar:
 * - Doğru tekrar: rating >= [Srs.GOOD] (HARD ve AGAIN yanlış sayılır).
 * - Yeni kartın ilk tekrarı: inceleme anındaki `previousStability <= 0.0`.
 * - Gerçek tutundurma: ilk tekrarlar hariç doğru / toplam; en az
 *   [MIN_RETENTION_SAMPLE] örnek yoksa null.
 * - Oturmuş tutundurma: inceleme anında `previousStability >= 21.0` olan
 *   tekrarlarda doğru / toplam.
 * - Tutundurma eğrisi: ilk tekrarlar hariç, geçen süreye (`elapsedDays`) göre
 *   <1, 1–7, 7–30, 30–90 ve 90+ gün kovaları; boş kova accuracy = null.
 */
object StatsAnalysis {

    const val MIN_RETENTION_SAMPLE = 20
    const val MATURE_STABILITY = 21.0
    const val FORECAST_DAYS = 30
    const val FORECAST_SHORT_DAYS = 7
    const val TAG_LIMIT = 8
    const val WORST_TAG_MIN_REVIEWS = 5

    private val BUCKET_LABELS = listOf(
        "<1 gün",
        "1–7 gün",
        "7–30 gün",
        "30–90 gün",
        "90+ gün",
    )

    fun parseTags(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split(',')
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun retentionStats(
        logs: List<ReviewLog>,
        minSample: Int = MIN_RETENTION_SAMPLE,
        excludeNewFirstReviews: Boolean = true,
    ): RetentionStats {
        var sample = 0
        var correct = 0
        var matureSample = 0
        var matureCorrect = 0
        val bucketReviews = IntArray(BUCKET_LABELS.size)
        val bucketCorrect = IntArray(BUCKET_LABELS.size)
        for (log in logs) {
            val passed = log.rating >= Srs.GOOD
            if (log.previousStability >= MATURE_STABILITY) {
                matureSample++
                if (passed) matureCorrect++
            }
            val newFirstReview = log.previousStability <= 0.0
            if (excludeNewFirstReviews && newFirstReview) continue
            sample++
            if (passed) correct++
            val index = bucketIndex(log.elapsedDays)
            bucketReviews[index]++
            if (passed) bucketCorrect[index]++
        }
        val buckets = BUCKET_LABELS.mapIndexed { index, label ->
            RetentionBucket(label, bucketReviews[index], bucketCorrect[index])
        }
        return RetentionStats(
            trueRetention = if (sample >= minSample) correct.toDouble() / sample else null,
            matureRetention = if (matureSample >= minSample) {
                matureCorrect.toDouble() / matureSample
            } else {
                null
            },
            buckets = buckets,
            sample = sample,
        )
    }

    /**
     * Tek geçişte günlük aktivite, tutundurma ve etiket performansı.
     * [logs] yalnızca bir kez dolaşılır; kelimelerin etiketleri de bir kez
     * ayrıştırılıp `wordId` üzerinden yeniden kullanılır.
     */
    fun analyze(
        words: List<Word>,
        logs: List<ReviewLog>,
        today: LocalDate,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
        tagLimit: Int = TAG_LIMIT,
    ): StatsSnapshot {
        val byDay = HashMap<LocalDate, Int>()
        val tagsByWordId = HashMap<Long, List<String>>(words.size)
        val tagAccs = HashMap<String, TagAccumulator>()
        var overdueNow = 0

        for (word in words) {
            val tags = parseTags(word.tags)
            tagsByWordId[word.id] = tags
            val due = !word.suspended && !word.isNew && word.dueAt <= now
            if (due) overdueNow++
            val leech = !word.suspended && word.isLeech
            for (tag in tags) {
                val acc = tagAccs.getOrPut(tag) { TagAccumulator() }
                acc.wordCount++
                if (due) acc.dueNow++
                if (leech) acc.leeches++
            }
        }

        var sample = 0
        var correct = 0
        var matureSample = 0
        var matureCorrect = 0
        val bucketReviews = IntArray(BUCKET_LABELS.size)
        val bucketCorrect = IntArray(BUCKET_LABELS.size)

        for (log in logs) {
            val key = StatsMath.dayKey(log.reviewedAt, zone)
            byDay[key] = (byDay[key] ?: 0) + 1

            val passed = log.rating >= Srs.GOOD
            if (log.previousStability >= MATURE_STABILITY) {
                matureSample++
                if (passed) matureCorrect++
            }
            val newFirstReview = log.previousStability <= 0.0
            if (!newFirstReview) {
                sample++
                if (passed) correct++
                val index = bucketIndex(log.elapsedDays)
                bucketReviews[index]++
                if (passed) bucketCorrect[index]++
            }

            val tags = tagsByWordId[log.wordId] ?: continue
            for (tag in tags) {
                val acc = tagAccs[tag] ?: continue
                acc.reviews++
                if (passed) acc.correct++
            }
        }

        val retention = RetentionStats(
            trueRetention = if (sample >= MIN_RETENTION_SAMPLE) {
                correct.toDouble() / sample
            } else {
                null
            },
            matureRetention = if (matureSample >= MIN_RETENTION_SAMPLE) {
                matureCorrect.toDouble() / matureSample
            } else {
                null
            },
            buckets = BUCKET_LABELS.mapIndexed { index, label ->
                RetentionBucket(label, bucketReviews[index], bucketCorrect[index])
            },
            sample = sample,
        )

        val forecast30 = StatsMath.forecast(words, today, FORECAST_DAYS, zone)
        val forecast7 = forecast30.take(FORECAST_SHORT_DAYS)
        return StatsSnapshot(
            byDay = byDay,
            retention = retention,
            tagPerformance = selectTagPerformance(tagAccs, tagLimit),
            forecast = forecast7,
            workload7 = forecast7.sumOf { it.count },
            workload30 = forecast30.sumOf { it.count },
            overdueNow = overdueNow,
        )
    }

    private fun bucketIndex(elapsedDays: Double): Int = when {
        elapsedDays < 1.0 -> 0
        elapsedDays < 7.0 -> 1
        elapsedDays < 30.0 -> 2
        elapsedDays < 90.0 -> 3
        else -> 4
    }

    private fun selectTagPerformance(
        accs: Map<String, TagAccumulator>,
        limit: Int,
    ): List<TagPerformance> {
        val all = accs.map { (tag, acc) ->
            TagPerformance(
                tag = tag,
                wordCount = acc.wordCount,
                reviews = acc.reviews,
                correct = acc.correct,
                dueNow = acc.dueNow,
                leeches = acc.leeches,
            )
        }
        val selected = all
            .sortedWith(compareByDescending<TagPerformance> { it.reviews }.thenBy { it.tag })
            .take(limit)
            .toMutableList()
        val worst = all
            .filter { it.reviews >= WORST_TAG_MIN_REVIEWS && it.accuracy != null }
            .minWithOrNull(
                compareBy<TagPerformance> { it.accuracy!! }
                    .thenByDescending { it.reviews }
                    .thenBy { it.tag }
            )
        if (worst != null && selected.none { it.tag == worst.tag }) {
            selected += worst
        }
        return selected
    }

    private class TagAccumulator {
        var wordCount = 0
        var reviews = 0
        var correct = 0
        var dueNow = 0
        var leeches = 0
    }
}
