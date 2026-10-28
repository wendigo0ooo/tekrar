package com.wendigo.tekrar.repo

import android.content.Context
import androidx.room.withTransaction
import com.wendigo.tekrar.core.DueFormat
import com.wendigo.tekrar.core.Srs
import com.wendigo.tekrar.core.StatsMath
import com.wendigo.tekrar.data.AppDatabase
import com.wendigo.tekrar.data.ReviewLog
import com.wendigo.tekrar.data.Word
import com.wendigo.tekrar.ui.contracts.DashboardUiState
import com.wendigo.tekrar.ui.contracts.StatsUiState
import com.wendigo.tekrar.ui.contracts.StudyMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

data class ReviewOutcome(
    val word: Word,
    val logId: Long,
)

class WordRepository(
    context: Context,
    private val db: AppDatabase,
    private val settings: SettingsStore,
) {

    private val appContext = context.applicationContext
    private val words = db.wordDao()
    private val logs = db.reviewLogDao()

    fun observeDashboard(): Flow<DashboardUiState> = combine(
        words.observeAllWords(),
        logs.observeAll(),
        settings.settings,
    ) { allWords, allLogs, appSettings ->
        buildDashboard(allWords, allLogs, appSettings)
    }

    fun observeStats(): Flow<StatsUiState> = combine(
        words.observeAllWords(),
        logs.observeAll(),
        settings.settings,
    ) { allWords, allLogs, appSettings ->
        buildStats(allWords, allLogs, appSettings)
    }

    fun observeLibrary(): Flow<List<Word>> = words.observeAllWords()

    fun observeWord(id: Long): Flow<Word?> = words.observeById(id)

    fun observeTotals(): Flow<Pair<Int, Int>> = combine(
        words.observeAllWords(),
        logs.observeAll(),
    ) { allWords, allLogs ->
        allWords.size to todayCount(allLogs)
    }

    suspend fun loadQueue(mode: StudyMode): List<Word> {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val dayStart = StatsMath.startOfDay(today, zone)
        val appSettings = settings.settings.first()
        val due = words.dueReviews(now)
        val introducedToday = words.introducedCountSince(dayStart)
        val newAllowed = (appSettings.newPerDay - introducedToday).coerceAtLeast(0)
        val fresh = if (newAllowed > 0) words.newWords(newAllowed) else emptyList()
        return when (mode) {
            StudyMode.REVIEW -> due
            StudyMode.NEW -> fresh
            StudyMode.MIXED -> due + fresh
        }
    }

    suspend fun previewIntervals(word: Word, now: Long = System.currentTimeMillis()): List<String> {
        val isNew = word.state == Word.STATE_NEW
        val elapsedDays = elapsedDays(word, now)
        val retention = settings.settings.first().retentionFraction
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
                    desiredRetention = retention,
                )
                DueFormat.interval(result.intervalDays)
            }
        }
    }

    suspend fun review(wordId: Long, rating: Int): ReviewOutcome? {
        val word = words.getById(wordId) ?: return null
        val now = System.currentTimeMillis()
        val isNew = word.state == Word.STATE_NEW
        val elapsed = elapsedDays(word, now)
        val retention = settings.settings.first().retentionFraction
        val result = Srs.review(
            stability = word.stability,
            difficulty = word.difficulty,
            elapsedDays = elapsed,
            isNew = isNew,
            rating = rating,
            desiredRetention = retention,
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
        val logId = db.withTransaction {
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
        return ReviewOutcome(updated, logId)
    }

    suspend fun undoReview(logId: Long, previous: Word) {
        db.withTransaction {
            words.update(previous)
            logs.deleteById(logId)
        }
    }

    suspend fun addWord(
        term: String,
        meaning: String,
        example: String,
        exampleTr: String,
        notes: String,
        tags: String,
        pos: String,
    ): Long {
        val now = System.currentTimeMillis()
        return words.insert(
            Word(
                term = term,
                meaning = meaning,
                example = example,
                exampleTr = exampleTr,
                notes = notes,
                tags = tags,
                pos = pos,
                createdAt = now,
                dueAt = now,
                state = Word.STATE_NEW,
            )
        )
    }

    suspend fun updateWord(
        id: Long,
        term: String,
        meaning: String,
        example: String,
        exampleTr: String,
        notes: String,
        tags: String,
        pos: String,
    ): Word? {
        val word = words.getById(id) ?: return null
        val updated = word.copy(
            term = term,
            meaning = meaning,
            example = example,
            exampleTr = exampleTr,
            notes = notes,
            tags = tags,
            pos = pos,
        )
        words.update(updated)
        return updated
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) {
        words.updateFavorite(id, favorite)
    }

    suspend fun setSuspended(id: Long, suspended: Boolean): Word? {
        val word = words.getById(id) ?: return null
        val updated = word.copy(suspended = suspended)
        words.update(updated)
        return updated
    }

    suspend fun resetProgress(id: Long) {
        words.resetProgress(id, System.currentTimeMillis())
    }

    suspend fun deleteWord(id: Long) {
        db.withTransaction {
            logs.deleteForWord(id)
            words.deleteById(id)
        }
    }

    suspend fun resetAllProgress() {
        db.withTransaction {
            words.resetAllProgress(System.currentTimeMillis())
            logs.deleteAll()
        }
    }

    suspend fun allWords(): List<Word> = words.allWords()

    suspend fun seedIfEmpty() {
        if (words.countAll() > 0) return
        val raw = appContext.assets.open(SEED_ASSET).bufferedReader().use { it.readText() }
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
                exampleTr = item.optString("exampleTr").trim(),
                tags = item.optString("tags").trim(),
                pos = item.optString("pos").trim(),
                createdAt = now,
                dueAt = now,
            )
        }
        db.withTransaction {
            batch.forEach { words.insert(it) }
        }
    }

    suspend fun backfillExampleTr() {
        val raw = appContext.assets.open(SEED_ASSET).bufferedReader().use { it.readText() }
        val array = JSONArray(raw)
        val translations = HashMap<String, Pair<String, String>>()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val term = item.optString("term").trim().lowercase(Locale.ROOT)
            val example = item.optString("example").trim()
            val exampleTr = item.optString("exampleTr").trim()
            if (term.isNotEmpty() && exampleTr.isNotEmpty()) {
                translations[term] = example to exampleTr
            }
        }
        val pending = words.allWords().filter { word ->
            word.exampleTr.isBlank() &&
                translations[word.term.trim().lowercase(Locale.ROOT)]
                    ?.first == word.example.trim()
        }
        if (pending.isEmpty()) return
        db.withTransaction {
            pending.forEach { word ->
                val tr = translations.getValue(word.term.trim().lowercase(Locale.ROOT)).second
                words.updateExampleTr(word.id, tr)
            }
        }
    }

    private fun buildDashboard(
        allWords: List<Word>,
        allLogs: List<ReviewLog>,
        appSettings: AppSettings,
    ): DashboardUiState {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val byDay = StatsMath.reviewsByDay(allLogs, zone)
        val dueNow = allWords.count { !it.suspended && !it.isNew && it.dueAt <= now }
        val newTotal = allWords.count { !it.suspended && it.isNew }
        val introducedToday = allWords.count {
            it.introducedAt?.let { stamp -> StatsMath.dayKey(stamp, zone) == today } == true
        }
        val newAvailable = minOf(newTotal, (appSettings.newPerDay - introducedToday).coerceAtLeast(0))
        val nextDue = allWords
            .asSequence()
            .filter { !it.suspended && !it.isNew && it.dueAt > now }
            .map { it.dueAt }
            .minOrNull()
        return DashboardUiState(
            loading = false,
            dueNow = dueNow,
            newAvailable = newAvailable,
            newTotal = newTotal,
            totalWords = allWords.size,
            learned = allWords.count { !it.suspended && !it.isNew },
            mature = allWords.count { !it.suspended && !it.isNew && it.stability >= 21.0 },
            reviewedToday = byDay[today] ?: 0,
            dailyGoal = appSettings.dailyGoal,
            streak = StatsMath.streak(byDay, today),
            nextDueText = nextDue?.let { DueFormat.relative(it, now) },
            last7 = StatsMath.lastNDays(byDay, today, 7),
        )
    }

    private fun buildStats(
        allWords: List<Word>,
        allLogs: List<ReviewLog>,
        appSettings: AppSettings,
    ): StatsUiState {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val byDay = StatsMath.reviewsByDay(allLogs, zone)
        val active = allWords.filter { !it.suspended }
        return StatsUiState(
            loading = false,
            totalWords = allWords.size,
            learned = active.count { !it.isNew },
            young = active.count { !it.isNew && it.stability < 21.0 },
            mature = active.count { !it.isNew && it.stability >= 21.0 },
            leech = active.count { it.isLeech },
            suspended = allWords.count { it.suspended },
            retention = StatsMath.retention(allLogs),
            reviewedToday = byDay[today] ?: 0,
            dailyGoal = appSettings.dailyGoal,
            streak = StatsMath.streak(byDay, today),
            heatmap = StatsMath.lastNDays(byDay, today, 91),
            last30 = StatsMath.lastNDays(byDay, today, 30),
            forecast = StatsMath.forecast(allWords, today, 7, now, zone),
        )
    }

    private fun todayCount(allLogs: List<ReviewLog>): Int {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        return allLogs.count { StatsMath.dayKey(it.reviewedAt, zone) == today }
    }

    private fun elapsedDays(word: Word, now: Long): Double {
        val last = word.lastReviewAt ?: return 0.0
        return (now - last).coerceAtLeast(0L).toDouble() / DAY_MS
    }

    companion object {
        private const val DAY_MS = 86_400_000L
        private const val RELEARN_DELAY_MS = 10 * 60 * 1000L
        private const val SEED_ASSET = "seed_words.json"
    }
}

