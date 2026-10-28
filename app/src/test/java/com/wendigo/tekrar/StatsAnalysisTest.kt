package com.wendigo.tekrar

import com.wendigo.tekrar.core.StatsAnalysis
import com.wendigo.tekrar.data.ReviewLog
import com.wendigo.tekrar.data.Word
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsAnalysisTest {

    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 10, 24)
    private val now = millis(today) + 12 * 3_600_000L

    private fun millis(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun log(
        wordId: Long = 1,
        rating: Int = 3,
        date: LocalDate = today,
        elapsed: Double = 2.0,
        previousStability: Double = 5.0,
    ) = ReviewLog(
        wordId = wordId,
        rating = rating,
        reviewedAt = millis(date),
        previousDueAt = millis(date.minusDays(2)),
        previousStability = previousStability,
        previousDifficulty = 5.0,
        nextDueAt = millis(date.plusDays(2)),
        nextStability = 6.0,
        nextDifficulty = 5.0,
        elapsedDays = elapsed,
    )

    private fun word(
        id: Long,
        tags: String = "",
        dueAt: Long = millis(today) + 3_600_000L,
        state: Int = Word.STATE_REVIEW,
        suspended: Boolean = false,
        lapses: Int = 0,
    ) = Word(
        id = id,
        term = "w$id",
        meaning = "m$id",
        tags = tags,
        dueAt = dueAt,
        state = state,
        suspended = suspended,
        lapses = lapses,
    )

    @Test
    fun parseTagsNormalizesTrimsAndDeduplicates() {
        assertEquals(
            listOf("yds", "phrasal"),
            StatsAnalysis.parseTags("  YDS, Phrasal , ,yds "),
        )
        assertTrue(StatsAnalysis.parseTags("   ").isEmpty())
        assertTrue(StatsAnalysis.parseTags(", ,").isEmpty())
    }

    @Test
    fun trueRetentionUsesGoodThresholdAndSkipsNewFirstReviews() {
        val repeats = List(20) { index ->
            log(rating = if (index < 18) 3 else if (index == 18) 2 else 1)
        }
        val newFirst = List(5) { log(rating = 3, previousStability = 0.0) }
        val stats = StatsAnalysis.retentionStats(repeats + newFirst)
        assertEquals(20, stats.sample)
        assertEquals(0.9, stats.trueRetention!!, 0.0001)

        val includingNew = StatsAnalysis.retentionStats(
            repeats + newFirst,
            excludeNewFirstReviews = false,
        )
        assertEquals(25, includingNew.sample)
        assertEquals(23.0 / 25.0, includingNew.trueRetention!!, 0.0001)
    }

    @Test
    fun trueRetentionNeedsMinimumSample() {
        val few = List(19) { log() }
        val stats = StatsAnalysis.retentionStats(few)
        assertEquals(19, stats.sample)
        assertNull(stats.trueRetention)
    }

    @Test
    fun matureRetentionUsesStabilityAtReviewTime() {
        val mature = List(20) { log(previousStability = 21.0) }
        val young = List(20) { log(rating = 1, previousStability = 20.99) }
        val stats = StatsAnalysis.retentionStats(mature + young)
        assertEquals(1.0, stats.matureRetention!!, 0.0001)
        assertEquals(0.5, stats.trueRetention!!, 0.0001)
    }

    @Test
    fun matureRetentionIsNullBelowMinimumSample() {
        val onlyMature = List(19) { log(previousStability = 30.0) }
        assertNull(StatsAnalysis.retentionStats(onlyMature).matureRetention)
    }

    @Test
    fun retentionBucketsUseElapsedDayBoundaries() {
        val logs = listOf(
            log(elapsed = 0.5, rating = 3),
            log(elapsed = 1.0, rating = 1),
            log(elapsed = 6.99, rating = 3),
            log(elapsed = 7.0, rating = 3),
            log(elapsed = 29.99, rating = 1),
            log(elapsed = 30.0, rating = 3),
            log(elapsed = 89.99, rating = 3),
            log(elapsed = 90.0, rating = 1),
        )
        val buckets = StatsAnalysis.retentionStats(logs).buckets
        assertEquals(5, buckets.size)
        assertEquals(listOf(1, 2, 2, 2, 1), buckets.map { it.reviews })
        assertEquals(listOf(1, 1, 1, 2, 0), buckets.map { it.correct })
        assertEquals(1.0, buckets[0].accuracy!!, 0.0001)
        assertEquals(0.5, buckets[1].accuracy!!, 0.0001)
        assertEquals(0.5, buckets[2].accuracy!!, 0.0001)
        assertEquals(1.0, buckets[3].accuracy!!, 0.0001)
        assertEquals(0.0, buckets[4].accuracy!!, 0.0001)
    }

    @Test
    fun emptyRetentionBucketsHaveNoAccuracy() {
        val buckets = StatsAnalysis.retentionStats(emptyList()).buckets
        assertEquals(5, buckets.size)
        assertTrue(buckets.all { it.reviews == 0 && it.accuracy == null })
    }

    @Test
    fun tagPerformanceJoinsLogsAndIgnoresDeletedWords() {
        val words = listOf(
            word(id = 1, tags = "YDS, phrasal", dueAt = millis(today.minusDays(1))),
            word(
                id = 2,
                tags = "yds",
                dueAt = millis(today.plusDays(1)),
                lapses = Word.LEECH_LAPSES,
            ),
            word(id = 3, tags = ""),
            word(id = 4, tags = "grammar", suspended = true),
        )
        val logs = listOf(
            log(wordId = 1, rating = 3),
            log(wordId = 1, rating = 1),
            log(wordId = 2, rating = 3),
            log(wordId = 3, rating = 1),
            log(wordId = 99, rating = 3),
        )
        val snapshot = StatsAnalysis.analyze(words, logs, today, now, zone)
        val byTag = snapshot.tagPerformance.associateBy { it.tag }

        val yds = byTag.getValue("yds")
        assertEquals(2, yds.wordCount)
        assertEquals(3, yds.reviews)
        assertEquals(2, yds.correct)
        assertEquals(1, yds.dueNow)
        assertEquals(1, yds.leeches)
        assertEquals(2.0 / 3.0, yds.accuracy!!, 0.0001)

        val phrasal = byTag.getValue("phrasal")
        assertEquals(1, phrasal.wordCount)
        assertEquals(2, phrasal.reviews)
        assertEquals(0.5, phrasal.accuracy!!, 0.0001)

        val grammar = byTag.getValue("grammar")
        assertEquals(1, grammar.wordCount)
        assertEquals(0, grammar.reviews)
        assertNull(grammar.accuracy)
        assertEquals(0, grammar.dueNow)
        assertTrue(byTag.keys.none { it == "99" })
    }

    @Test
    fun tagSelectionAppendsWorstAccuracyTag() {
        val words = (1L..5L).map { id -> word(id = id, tags = "good$id") } +
            word(id = 10, tags = "bad")
        val logs = buildList {
            for (id in 1L..5L) {
                repeat(6) { add(log(wordId = id, rating = 3)) }
            }
            repeat(5) { add(log(wordId = 10, rating = 1)) }
        }
        val tags = StatsAnalysis.analyze(
            words,
            logs,
            today,
            now,
            zone,
            tagLimit = 3,
        ).tagPerformance
        assertEquals(4, tags.size)
        assertNotNull(tags.firstOrNull { it.tag == "bad" })
        assertEquals(0.0, tags.first { it.tag == "bad" }.accuracy!!, 0.0001)
    }

    @Test
    fun analyzeComputesWorkloadForecastAndOverdue() {
        val words = listOf(
            word(id = 1, dueAt = millis(today.minusDays(1))),
            word(id = 2, dueAt = millis(today) + 3_600_000L),
            word(id = 3, dueAt = millis(today.plusDays(2))),
            word(id = 4, dueAt = millis(today.plusDays(6))),
            word(id = 5, dueAt = millis(today.plusDays(7))),
            word(id = 6, dueAt = millis(today.plusDays(29))),
            word(id = 7, dueAt = millis(today.plusDays(30))),
            word(id = 8, state = Word.STATE_NEW, dueAt = millis(today)),
            word(id = 9, dueAt = millis(today), suspended = true),
        )
        val snapshot = StatsAnalysis.analyze(words, emptyList(), today, now, zone)
        assertEquals(7, snapshot.forecast.size)
        assertEquals(listOf(2, 0, 1, 0, 0, 0, 1), snapshot.forecast.map { it.count })
        assertEquals(4, snapshot.workload7)
        assertEquals(6, snapshot.workload30)
        assertEquals(2, snapshot.overdueNow)
    }
}
