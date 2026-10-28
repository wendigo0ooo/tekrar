package com.wendigo.tekrar.repo

import android.content.Context
import androidx.room.withTransaction
import com.wendigo.tekrar.core.Csv
import com.wendigo.tekrar.data.AppDatabase
import com.wendigo.tekrar.data.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class ImportResult(val added: Int, val skipped: Int) {
    val summary: String
        get() = "$added kelime eklendi" + if (skipped > 0) ", $skipped atlandı" else ""
}

class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsStore,
) {

    private val words = db.wordDao()

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val appSettings = settings.settings.first()
        val array = JSONArray()
        for (word in words.allWords().sortedBy { it.term.lowercase(Locale.ROOT) }) {
            array.put(
                JSONObject().apply {
                    put("term", word.term)
                    put("meaning", word.meaning)
                    put("example", word.example)
                    put("exampleTr", word.exampleTr)
                    put("notes", word.notes)
                    put("tags", word.tags)
                    put("pos", word.pos)
                    put("favorite", word.favorite)
                    put("createdAt", word.createdAt)
                    put("state", word.state)
                    put("stability", word.stability)
                    put("difficulty", word.difficulty)
                    put("dueAt", word.dueAt)
                    put("lastReviewAt", word.lastReviewAt ?: JSONObject.NULL)
                    put("introducedAt", word.introducedAt ?: JSONObject.NULL)
                    put("reps", word.reps)
                    put("lapses", word.lapses)
                    put("suspended", word.suspended)
                }
            )
        }
        val root = JSONObject().apply {
            put("app", "tekrar")
            put("format", FORMAT_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("words", array)
            put(
                "settings",
                JSONObject().apply {
                    put("newPerDay", appSettings.newPerDay)
                    put("dailyGoal", appSettings.dailyGoal)
                    put("desiredRetention", appSettings.desiredRetention)
                    put("themeMode", appSettings.themeMode)
                    put("autoSpeak", appSettings.autoSpeak)
                    put("speechRate", appSettings.speechRate)
                }
            )
        }
        root.toString(2)
    }

    suspend fun importJson(raw: String): ImportResult = withContext(Dispatchers.IO) {
        val root = JSONObject(raw)
        val array = root.optJSONArray("words")
            ?: throw IllegalArgumentException("Yedek dosyasında kelime listesi yok")
        var added = 0
        var skipped = 0
        val existing = words.allWords()
            .map { it.term.trim().lowercase(Locale.ROOT) }
            .toHashSet()
        val now = System.currentTimeMillis()
        db.withTransaction {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val term = item.optString("term").trim()
                val meaning = item.optString("meaning").trim()
                if (term.isEmpty() || meaning.isEmpty()) {
                    skipped++
                    continue
                }
                if (!existing.add(term.lowercase(Locale.ROOT))) {
                    skipped++
                    continue
                }
                words.insert(
                    Word(
                        term = term,
                        meaning = meaning,
                        example = item.optString("example"),
                        exampleTr = item.optString("exampleTr"),
                        notes = item.optString("notes"),
                        tags = item.optString("tags"),
                        pos = item.optString("pos"),
                        favorite = item.optBoolean("favorite", false),
                        createdAt = item.optLong("createdAt", now),
                        state = if (item.optInt("state", 0) == Word.STATE_REVIEW) {
                            Word.STATE_REVIEW
                        } else {
                            Word.STATE_NEW
                        },
                        stability = item.optDouble("stability", 0.0).coerceAtLeast(0.0),
                        difficulty = item.optDouble("difficulty", 0.0).coerceIn(0.0, 10.0),
                        dueAt = item.optLong("dueAt", now).takeIf { it > 0 } ?: now,
                        lastReviewAt = item.optLongOrNull("lastReviewAt"),
                        introducedAt = item.optLongOrNull("introducedAt"),
                        reps = item.optInt("reps", 0).coerceAtLeast(0),
                        lapses = item.optInt("lapses", 0).coerceAtLeast(0),
                        suspended = item.optBoolean("suspended", false),
                    )
                )
                added++
            }
        }
        ImportResult(added, skipped)
    }

    suspend fun exportCsv(): String = withContext(Dispatchers.IO) {
        val rows = ArrayList<List<String>>()
        rows += listOf("term", "meaning", "example", "exampleTr", "notes", "tags", "pos")
        for (word in words.allWords().sortedBy { it.term.lowercase(Locale.ROOT) }) {
            rows += listOf(
                word.term,
                word.meaning,
                word.example,
                word.exampleTr,
                word.notes,
                word.tags,
                word.pos,
            )
        }
        Csv.encode(rows) + "\r\n"
    }

    suspend fun importCsv(raw: String): ImportResult = withContext(Dispatchers.IO) {
        val rows = Csv.parse(raw)
        if (rows.isEmpty()) return@withContext ImportResult(0, 0)
        val first = rows.first().firstOrNull()?.trim()?.lowercase(Locale.ROOT)
        val start = if (first == "term" || first == "kelime") 1 else 0
        var added = 0
        var skipped = 0
        val existing = words.allWords()
            .map { it.term.trim().lowercase(Locale.ROOT) }
            .toHashSet()
        val now = System.currentTimeMillis()
        db.withTransaction {
            for (index in start until rows.size) {
                val row = rows[index]
                val term = row.getOrNull(0)?.trim().orEmpty()
                val meaning = row.getOrNull(1)?.trim().orEmpty()
                if (term.isEmpty() || meaning.isEmpty()) {
                    skipped++
                    continue
                }
                if (!existing.add(term.lowercase(Locale.ROOT))) {
                    skipped++
                    continue
                }
                words.insert(
                    Word(
                        term = term,
                        meaning = meaning,
                        example = row.getOrNull(2)?.trim().orEmpty(),
                        exampleTr = row.getOrNull(3)?.trim().orEmpty(),
                        notes = row.getOrNull(4)?.trim().orEmpty(),
                        tags = row.getOrNull(5)?.trim().orEmpty(),
                        pos = row.getOrNull(6)?.trim().orEmpty(),
                        createdAt = now,
                        dueAt = now,
                    )
                )
                added++
            }
        }
        ImportResult(added, skipped)
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (isNull(key)) null else optLong(key, 0L).takeIf { it > 0 }

    companion object {
        const val FORMAT_VERSION = 2
    }
}
