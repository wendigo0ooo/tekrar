package com.wendigo.tekrar.repo

import android.content.Context
import androidx.room.withTransaction
import com.wendigo.tekrar.core.BackupFormat
import com.wendigo.tekrar.core.BackupPackage
import com.wendigo.tekrar.core.BackupSettings
import com.wendigo.tekrar.core.Csv
import com.wendigo.tekrar.core.TermKey
import com.wendigo.tekrar.data.AppDatabase
import com.wendigo.tekrar.data.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Locale

data class ImportResult(
    val added: Int,
    val skipped: Int,
    val logs: Int = 0,
    val skippedLogs: Int = 0,
    val settingsApplied: Boolean = false,
    val replaced: Boolean = false,
) {
    val words: Int get() = added

    val summary: String
        get() = if (replaced) {
            val invalid = skipped + skippedLogs
            buildString {
                append("$added kelime")
                if (logs > 0) append(", $logs tekrar kaydı")
                if (settingsApplied) append(", ayarlar")
                append(" geri yüklendi")
                if (invalid > 0) append("; $invalid geçersiz/atlanan satır")
            }
        } else {
            "$added kelime eklendi" + if (skipped > 0) ", $skipped atlandı" else ""
        }
}

class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsStore,
) {

    private val words = db.wordDao()
    private val logs = db.reviewLogDao()

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val appSettings = settings.settings.first()
        val payload = BackupPackage(
            exportedAt = System.currentTimeMillis(),
            words = words.allWords().sortedWith(compareBy { TermKey.normalize(it.term) }),
            logs = logs.allLogs(),
            settings = appSettings.toBackupSettings(),
        )
        BackupFormat.build(payload)
    }

    suspend fun importJson(raw: String): ImportResult = withContext(Dispatchers.IO) {
        val payload = BackupFormat.parse(raw)
        var wordCount = 0
        var logCount = 0
        db.withTransaction {
            logs.deleteAll()
            words.deleteAll()
            for (word in payload.words) {
                words.insert(word)
                wordCount++
            }
            for (log in payload.logs) {
                logs.insert(log)
                logCount++
            }
        }
        settings.setSeedApplied(true)
        payload.settings?.let { settings.applyAll(it) }
        ImportResult(
            added = wordCount,
            skipped = payload.skippedWords,
            logs = logCount,
            skippedLogs = payload.skippedLogs,
            settingsApplied = payload.settings != null,
            replaced = true,
        )
    }

    suspend fun exportCsv(): String = withContext(Dispatchers.IO) {
        val rows = ArrayList<List<String>>()
        rows += listOf("term", "meaning", "example", "exampleTr", "notes", "tags", "pos")
        for (word in words.allWords().sortedBy { TermKey.normalize(it.term) }) {
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
            .map { TermKey.normalize(it.term) }
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
                if (!existing.add(TermKey.normalize(term))) {
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
        ImportResult(added, skipped, replaced = false)
    }

    private fun AppSettings.toBackupSettings(): BackupSettings = BackupSettings(
        newPerDay = newPerDay,
        dailyGoal = dailyGoal,
        desiredRetention = desiredRetention,
        themeMode = themeMode,
        autoSpeak = autoSpeak,
        speechRate = speechRate,
        reminderEnabled = reminderEnabled,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
    )
}
