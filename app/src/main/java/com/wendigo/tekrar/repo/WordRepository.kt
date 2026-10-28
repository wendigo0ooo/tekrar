package com.wendigo.tekrar.repo

import android.content.Context
import androidx.room.withTransaction
import com.wendigo.tekrar.core.DeckCatalog
import com.wendigo.tekrar.core.DeckDefinition
import com.wendigo.tekrar.core.DeckFormatException
import com.wendigo.tekrar.core.DueFormat
import com.wendigo.tekrar.core.LeechPolicy
import com.wendigo.tekrar.core.Srs
import com.wendigo.tekrar.core.StatsMath
import com.wendigo.tekrar.core.StreakFreeze
import com.wendigo.tekrar.core.TermKey
import com.wendigo.tekrar.core.WeeklyGoal
import com.wendigo.tekrar.data.AppDatabase
import com.wendigo.tekrar.data.ReviewLog
import com.wendigo.tekrar.data.Word
import com.wendigo.tekrar.ui.contracts.DashboardUiState
import com.wendigo.tekrar.ui.contracts.StatsUiState
import com.wendigo.tekrar.ui.contracts.StudyMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

data class ReviewOutcome(
    val word: Word,
    val logId: Long,
)

data class BundledDeck(
    val assetName: String,
    val definition: DeckDefinition,
)

data class DeckInstallResult(
    val deckName: String,
    val added: Int,
    val skipped: Int,
)

sealed interface WordWriteResult {
    data class Saved(val id: Long) : WordWriteResult
    data object DuplicateTerm : WordWriteResult
    data object NotFound : WordWriteResult
}

class WordRepository(
    context: Context,
    private val db: AppDatabase,
    private val settings: SettingsStore,
) {

    private val appContext = context.applicationContext
    private val words = db.wordDao()
    private val logs = db.reviewLogDao()
    private val seedMutex = Mutex()
    private val streakMutex = Mutex()

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
            StudyMode.LEECH -> LeechPolicy.leeches(words.allWords())
        }
    }

    /**
     * Bir desteye ait karışık oturum kuyruğu: vadesi gelen tekrarlar + günlük
     * kotaya göre yeni kelimeler. Yalnızca [deck] etiketli kelimeler seçilir.
     */
    suspend fun loadDeckQueue(deck: String): List<Word> {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val appSettings = settings.settings.first()
        val scoped = words.allWords().filter { !it.suspended && it.deck == deck }
        val due = scoped
            .filter { !it.isNew && it.dueAt <= now }
            .sortedWith(compareBy({ it.dueAt }, { it.id }))
        val introducedToday = scoped.count {
            it.introducedAt?.let { stamp -> StatsMath.dayKey(stamp, zone) == today } == true
        }
        val newAllowed = (appSettings.newPerDay - introducedToday).coerceAtLeast(0)
        val fresh = if (newAllowed > 0) {
            scoped.filter { it.isNew }.sortedBy { it.id }.take(newAllowed)
        } else {
            emptyList()
        }
        return due + fresh
    }

    suspend fun bundledDecks(): List<BundledDeck> = withContext(Dispatchers.IO) {
        BUNDLED_DECK_ASSETS.mapNotNull { assetName ->
            runCatching {
                BundledDeck(assetName, readDeckAsset(assetName))
            }.getOrNull()
        }
    }

    suspend fun installDeck(assetName: String): DeckInstallResult {
        if (assetName !in BUNDLED_DECK_ASSETS) {
            throw DeckFormatException("Bilinmeyen deste: $assetName")
        }
        val definition = readDeckAsset(assetName)
        val now = System.currentTimeMillis()
        return db.withTransaction {
            val existing = words.allWords()
                .map { TermKey.normalize(it.term) }
                .toHashSet()
            var added = 0
            var skipped = 0
            for (item in definition.words) {
                if (!existing.add(TermKey.normalize(item.term))) {
                    skipped++
                    continue
                }
                words.insert(
                    Word(
                        term = item.term,
                        meaning = item.meaning,
                        example = item.example,
                        exampleTr = item.exampleTr,
                        pos = item.pos,
                        deck = definition.name,
                        cefr = item.cefr,
                        createdAt = now,
                        dueAt = now,
                    )
                )
                added++
            }
            DeckInstallResult(definition.name, added, skipped)
        }
    }

    /**
     * Seviye testi için adaylar. Başlangıç destesi kuruluysa yalnız ondan, değilse
     * tüm kelimelerden CEFR bantlarına yayılmış biçimde en fazla [limit] kelime seçer.
     */
    suspend fun placementWords(limit: Int = 15): List<Word> {
        val active = words.allWords().filter { !it.suspended }
        val preferred = active.filter { it.deck == SEED_DECK }
        val source = if (preferred.isNotEmpty()) preferred else active
        if (source.isEmpty() || limit <= 0) return emptyList()
        val bands = DeckCatalog.CEFR_LEVELS + ""
        val grouped = source.groupBy { DeckCatalog.parseCefr(it.cefr) }
            .mapValues { (_, items) -> items.shuffled() }
        val picked = ArrayList<Word>(limit)
        val cursors = HashMap<String, Int>()
        var progressed = true
        while (picked.size < limit && progressed) {
            progressed = false
            for (band in bands) {
                if (picked.size >= limit) break
                val items = grouped[band] ?: continue
                val cursor = cursors[band] ?: 0
                if (cursor < items.size) {
                    picked += items[cursor]
                    cursors[band] = cursor + 1
                    progressed = true
                }
            }
        }
        return picked
    }

    /**
     * Bilinen yeni kelimeleri pürüzlü bir başlangıç planıyla işaretler. Mevcut
     * ilerlemesi olan kelimelere dokunmaz; böylece geçmiş kaybolmaz.
     */
    suspend fun applyPlacement(knownIds: Collection<Long>, newPerDay: Int) {
        val now = System.currentTimeMillis()
        val dueAt = now + PLACEMENT_INTERVAL_MS
        db.withTransaction {
            for (id in knownIds.toSet()) {
                val word = words.getById(id) ?: continue
                if (!word.isNew || word.suspended) continue
                words.update(
                    word.copy(
                        state = Word.STATE_REVIEW,
                        stability = PLACEMENT_STABILITY,
                        dueAt = dueAt,
                        introducedAt = now,
                        reps = 1,
                    )
                )
            }
        }
        settings.setNewPerDay(newPerDay)
    }

    private suspend fun readDeckAsset(assetName: String): DeckDefinition =
        withContext(Dispatchers.IO) {
            val raw = appContext.assets
                .open("decks/$assetName")
                .bufferedReader()
                .use { it.readText() }
            DeckCatalog.parse(raw).deck
        }

    suspend fun dueTomorrowCount(): Int {
        val zone = ZoneId.systemDefault()
        val tomorrow = LocalDate.now(zone).plusDays(1)
        val start = StatsMath.startOfDay(tomorrow, zone)
        val end = StatsMath.startOfDay(tomorrow.plusDays(1), zone)
        return words.dueCountBetween(start, end)
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
    ): WordWriteResult {
        val now = System.currentTimeMillis()
        val key = TermKey.normalize(term)
        return db.withTransaction {
            if (key.isNotEmpty() && words.allWords().any { TermKey.normalize(it.term) == key }) {
                return@withTransaction WordWriteResult.DuplicateTerm
            }
            val id = words.insert(
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
            WordWriteResult.Saved(id)
        }
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
    ): WordWriteResult {
        val key = TermKey.normalize(term)
        return db.withTransaction {
            val word = words.getById(id) ?: return@withTransaction WordWriteResult.NotFound
            val previousKey = TermKey.normalize(word.term)
            if (key != previousKey && key.isNotEmpty() &&
                words.allWords().any { it.id != id && TermKey.normalize(it.term) == key }
            ) {
                return@withTransaction WordWriteResult.DuplicateTerm
            }
            words.update(
                word.copy(
                    term = term,
                    meaning = meaning,
                    example = example,
                    exampleTr = exampleTr,
                    notes = notes,
                    tags = tags,
                    pos = pos,
                )
            )
            WordWriteResult.Saved(id)
        }
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
        val now = System.currentTimeMillis()
        db.withTransaction {
            val word = words.getById(id) ?: return@withTransaction
            words.update(word.resetProgress(now))
            logs.deleteForWord(id)
        }
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
        settings.clearFreezeState()
    }

    /**
     * Günlük seri dondurma kontrolü. Kaçırılan günleri (varsa) dondurur, ardından
     * 7'nin katına ulaşan seri için dondurma kazandırır. Idempotenttir; eşzamanlı
     * çağrılar mutex ile sıraya girer.
     */
    suspend fun syncStreakFreezes(
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zone),
    ) = streakMutex.withLock {
        val snapshot = settings.settings.first()
        val byDay = StatsMath.reviewsByDay(logs.allLogs(), zone)
        val reviewedDays = byDay.filterValues { it > 0 }.keys
        var frozenDays = snapshot.frozenDays
        var stock = snapshot.streakFreezes
        var milestone = snapshot.lastFreezeMilestone

        val decision = StreakFreeze.decide(reviewedDays, frozenDays, today, stock)
        if (decision.consumed > 0) {
            frozenDays = frozenDays + decision.days
            stock = decision.stockAfter
        }

        val streak = StatsMath.streak(byDay, today, frozenDays)
        val grant = StreakFreeze.grant(streak, milestone, stock)
        stock = (stock + grant.granted).coerceAtMost(StreakFreeze.MAX_FREEZES)
        milestone = grant.milestone

        if (frozenDays != snapshot.frozenDays ||
            stock != snapshot.streakFreezes ||
            milestone != snapshot.lastFreezeMilestone
        ) {
            settings.setFreezeState(stock, frozenDays, milestone)
        }
    }

    suspend fun allWords(): List<Word> = words.allWords()

    suspend fun seedIfEmpty() = seedMutex.withLock {
        if (settings.settings.first().seedApplied) return@withLock
        if (words.countAll() > 0) {
            settings.setSeedApplied(true)
            return@withLock
        }
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
            if (!seen.add(TermKey.normalize(term))) continue
            batch += Word(
                term = term,
                meaning = meaning,
                example = item.optString("example").trim(),
                exampleTr = item.optString("exampleTr").trim(),
                tags = item.optString("tags").trim(),
                pos = item.optString("pos").trim(),
                deck = item.optString("deck").trim().ifEmpty { SEED_DECK },
                cefr = DeckCatalog.parseCefr(item.optString("cefr")),
                createdAt = now,
                dueAt = now,
            )
        }
        if (batch.isNotEmpty()) {
            db.withTransaction {
                batch.forEach { words.insert(it) }
            }
        }
        settings.setSeedApplied(true)
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
        val weekly = WeeklyGoal.progress(byDay, today, appSettings.weeklyGoalDays)
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
            streak = StatsMath.streak(byDay, today, appSettings.frozenDays),
            streakFreezes = appSettings.streakFreezes,
            weeklyActiveDays = weekly.days,
            weeklyGoalDays = weekly.goal,
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
        val weekly = WeeklyGoal.progress(byDay, today, appSettings.weeklyGoalDays)
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
            streak = StatsMath.streak(byDay, today, appSettings.frozenDays),
            streakFreezes = appSettings.streakFreezes,
            frozenDays = appSettings.frozenDays.size,
            weeklyActiveDays = weekly.days,
            weeklyGoalDays = weekly.goal,
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
        private const val SEED_DECK = "Başlangıç"
        private const val PLACEMENT_INTERVAL_MS = 60 * DAY_MS
        private const val PLACEMENT_STABILITY = 30.0
        private val BUNDLED_DECK_ASSETS = listOf("yds.json", "phrasal.json")
    }
}

