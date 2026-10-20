package com.wendigo.tekrar.repo

import android.content.Context
import androidx.room.withTransaction
import com.wendigo.tekrar.core.Srs
import com.wendigo.tekrar.data.AppDatabase
import com.wendigo.tekrar.data.ReviewLog
import com.wendigo.tekrar.data.Word
import com.wendigo.tekrar.ui.contracts.DashboardUiState
import com.wendigo.tekrar.ui.contracts.StudyMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class WordRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsStore,
) {

    private val words = db.wordDao()
    private val logs = db.reviewLogDao()

    fun observeDashboard(): Flow<DashboardUiState> {
        val now = System.currentTimeMillis()
        val dayStart = startOfDay(now)
        val counts = combine(
            words.observeReviewDueCount(now),
            words.observeNewTotal(),
            words.observeTotal(),
            words.observeNextReviewDue(now),
            logs.observeCountSince(dayStart),
        ) { due, newTotal, total, nextDue, reviewedToday ->
            Counts(due, newTotal, total, nextDue, reviewedToday)
        }
        return combine(
            counts,
            words.observeIntroducedSince(dayStart),
            settings.newPerDay,
        ) { c, introducedToday, newLimit ->
            val remaining = (newLimit - introducedToday).coerceAtLeast(0)
            DashboardUiState(
                dueNow = c.due,
                newAvailable = minOf(c.newTotal, remaining),
                totalWords = c.total,
                reviewedToday = c.reviewedToday,
                nextDueText = c.nextDue?.let { formatNextDue(it) },
                loading = false,
            )
        }
    }

    fun observeSettingsStats(): Flow<Pair<Int, Int>> {
        val dayStart = startOfDay(System.currentTimeMillis())
        return combine(
            words.observeTotal(),
            logs.observeCountSince(dayStart),
        ) { total, reviewedToday -> total to reviewedToday }
    }

    suspend fun loadQueue(mode: StudyMode): List<Word> {
        val now = System.currentTimeMillis()
        val dayStart = startOfDay(now)
        val due = words.dueReviews(now)
        val introducedToday = words.introducedCountSince(dayStart)
        val newLimit = settings.newPerDay.first()
        val newAllowed = (newLimit - introducedToday).coerceAtLeast(0)
        val fresh = if (newAllowed > 0) words.newWords(newAllowed) else emptyList()
        return when (mode) {
            StudyMode.REVIEW -> due
            StudyMode.NEW -> fresh
            StudyMode.MIXED -> due + fresh
        }
    }

    fun previewIntervals(word: Word, now: Long = System.currentTimeMillis()): List<String> {
        val isNew = word.state == Word.STATE_NEW
        val elapsedDays = elapsedDays(word, now)
        return (Srs.AGAIN..Srs.EASY).map { rating ->
            if (rating == Srs.AGAIN && elapsedDays < 1.0) {
                "10 dk"
            } else {
                val result = Srs.review(
                    stability = word.stability,
                    difficulty = word.difficulty,
                    elapsedDays = elapsedDays,
                    isNew = isNew,
                    rating = rating,
                )
                formatInterval(result.intervalDays)
            }
        }
    }

    suspend fun review(wordId: Long, rating: Int): Word? {
        val word = words.getById(wordId) ?: return null
        val now = System.currentTimeMillis()
        val isNew = word.state == Word.STATE_NEW
        val elapsed = elapsedDays(word, now)
        val result = Srs.review(
            stability = word.stability,
            difficulty = word.difficulty,
            elapsedDays = elapsed,
            isNew = isNew,
            rating = rating,
        )
        val dueAt = if (rating == Srs.AGAIN && elapsed < 1.0) {
            now + RELEARN_DELAY_MS
        } else {
            now + result.intervalDays * DAY_MS
        }
        val updated = word.copy(
            state = Word.STATE_REVIEW,
            stability = result.stability,
            difficulty = result.difficulty,
            dueAt = dueAt,
            lastReviewAt = now,
            introducedAt = word.introducedAt ?: now,
            reps = word.reps + 1,
            lapses = word.lapses + if (!isNew && rating == Srs.AGAIN) 1 else 0,
        )
        db.withTransaction {
            words.update(updated)
            logs.insert(
                ReviewLog(
                    wordId = word.id,
                    rating = rating,
                    reviewedAt = now,
                    previousDueAt = word.dueAt,
                    previousStability = word.stability,
                    previousDifficulty = word.difficulty,
                    nextDueAt = dueAt,
                    nextStability = result.stability,
                    nextDifficulty = result.difficulty,
                    elapsedDays = elapsed,
                )
            )
        }
        return updated
    }

    suspend fun addWord(term: String, meaning: String, example: String, notes: String): Long {
        val now = System.currentTimeMillis()
        return words.insert(
            Word(
                term = term,
                meaning = meaning,
                example = example,
                notes = notes,
                createdAt = now,
                dueAt = now,
                state = Word.STATE_NEW,
            )
        )
    }

    suspend fun seedIfEmpty() {
        if (words.countAll() > 0) return
        val raw = context.assets.open(SEED_ASSET).bufferedReader().use { it.readText() }
        val array = JSONArray(raw)
        val now = System.currentTimeMillis()
        val seen = HashSet<String>()
        val batch = ArrayList<Word>(array.length())
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val term = item.optString("term").trim()
            val meaning = item.optString("meaning").trim()
            if (term.isEmpty() || meaning.isEmpty()) continue
            if (!seen.add(term.lowercase(Locale.ROOT))) continue
            batch += Word(
                term = term,
                meaning = meaning,
                example = item.optString("example").trim(),
                createdAt = now,
                dueAt = now,
            )
        }
        db.withTransaction {
            batch.forEach { words.insert(it) }
        }
    }

    private fun elapsedDays(word: Word, now: Long): Double {
        val last = word.lastReviewAt ?: return 0.0
        return (now - last).coerceAtLeast(0L).toDouble() / DAY_MS
    }

    private fun formatInterval(days: Int): String = when {
        days < 30 -> "$days gün"
        days < 365 -> "${(days / 30).coerceAtLeast(1)} ay"
        else -> "${(days / 365).coerceAtLeast(1)} yıl"
    }

    private fun formatNextDue(millis: Long): String {
        val zone = ZoneId.systemDefault()
        val moment = Instant.ofEpochMilli(millis).atZone(zone)
        val date = moment.toLocalDate()
        val today = LocalDate.now(zone)
        val time = moment.toLocalTime().truncatedTo(ChronoUnit.MINUTES)
        return when (date) {
            today -> "bugün $time"
            today.plusDays(1) -> "yarın $time"
            else -> date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("tr")))
        }
    }

    private fun startOfDay(millis: Long): Long {
        val zone = ZoneId.systemDefault()
        return Instant.ofEpochMilli(millis)
            .atZone(zone)
            .toLocalDate()
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }

    private data class Counts(
        val due: Int,
        val newTotal: Int,
        val total: Int,
        val nextDue: Long?,
        val reviewedToday: Int,
    )

    companion object {
        private const val DAY_MS = 86_400_000L
        private const val RELEARN_DELAY_MS = 10 * 60 * 1000L
        private const val SEED_ASSET = "seed_words.json"
    }
}
