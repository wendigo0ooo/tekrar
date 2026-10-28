package com.wendigo.tekrar

import com.wendigo.tekrar.core.BackupFormat
import com.wendigo.tekrar.core.BackupFormatException
import com.wendigo.tekrar.core.BackupPackage
import com.wendigo.tekrar.core.BackupSettings
import com.wendigo.tekrar.data.ReviewLog
import com.wendigo.tekrar.data.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFormatTest {

    @Test
    fun roundTripPreservesWordsLogsSettingsAndIds() {
        val wordA = Word(
            id = 5L,
            term = "build",
            meaning = "inşa etmek\nikinci satır",
            example = "We \"build\" a house.",
            exampleTr = "Bir ev inşa ederiz.",
            notes = "not; ters bölü \\ testi",
            tags = "fiil, günlük",
            pos = "verb",
            favorite = true,
            createdAt = 100L,
            state = Word.STATE_REVIEW,
            stability = 12.5,
            difficulty = 6.25,
            dueAt = 200L,
            lastReviewAt = 150L,
            introducedAt = 90L,
            reps = 7,
            lapses = 2,
            suspended = true,
        )
        val wordB = Word(
            id = 9L,
            term = "go",
            meaning = "gitmek",
            createdAt = 101L,
            dueAt = 101L,
        )
        val logA = ReviewLog(
            id = 41L,
            wordId = 5L,
            rating = 3,
            reviewedAt = 160L,
            previousDueAt = 100L,
            previousStability = 10.0,
            previousDifficulty = 6.0,
            nextDueAt = 260L,
            nextStability = 13.0,
            nextDifficulty = 6.1,
            elapsedDays = 1.5,
        )
        val logB = ReviewLog(
            id = 42L,
            wordId = 9L,
            rating = 1,
            reviewedAt = 170L,
            previousDueAt = 101L,
            previousStability = 0.0,
            previousDifficulty = 0.0,
            nextDueAt = 200L,
            nextStability = 0.5,
            nextDifficulty = 4.0,
            elapsedDays = 0.0,
        )
        val settings = BackupSettings(
            newPerDay = 15,
            dailyGoal = 35,
            weeklyGoalDays = 6,
            desiredRetention = 92,
            themeMode = 2,
            autoSpeak = false,
            speechRate = 1.3f,
            reminderEnabled = true,
            reminderHour = 21,
            reminderMinute = 45,
        )
        val packageToWrite = BackupPackage(
            exportedAt = 999L,
            words = listOf(wordA, wordB),
            logs = listOf(logA, logB),
            settings = settings,
        )

        val restored = BackupFormat.parse(BackupFormat.build(packageToWrite))

        assertEquals(999L, restored.exportedAt)
        assertEquals(listOf(wordA, wordB), restored.words)
        assertEquals(listOf(logA, logB), restored.logs)
        assertEquals(settings, restored.settings)
        assertEquals(0, restored.skippedWords)
        assertEquals(0, restored.skippedLogs)
    }

    @Test
    fun rejectsMissingOrUnknownFormat() {
        assertThrows(BackupFormatException::class.java) {
            BackupFormat.parse("""{"format":3,"words":[]}""")
        }
        assertThrows(BackupFormatException::class.java) {
            BackupFormat.parse("""{"app":"tekrar","words":[]}""")
        }
        assertThrows(BackupFormatException::class.java) {
            BackupFormat.parse("""{"app":"baska","format":3,"words":[]}""")
        }
    }

    @Test
    fun rejectsNewerAndMalformedPayloads() {
        assertThrows(BackupFormatException::class.java) {
            BackupFormat.parse("""{"app":"tekrar","format":99,"words":[]}""")
        }
        assertThrows(BackupFormatException::class.java) {
            BackupFormat.parse("{bozuk json")
        }
    }

    @Test
    fun acceptsLegacyVersionWithoutIdsOrLogs() {
        val restored = BackupFormat.parse(
            """{"app":"tekrar","format":2,"words":[{"term":"go","meaning":"gitmek"}],""" +
                """"settings":{"dailyGoal":30}}"""
        )

        assertEquals(1, restored.words.size)
        assertEquals(0L, restored.words.first().id)
        assertTrue(restored.logs.isEmpty())
        assertEquals(30, restored.settings?.dailyGoal ?: -1)
        assertEquals(20, restored.settings?.newPerDay ?: -1)
        assertEquals(5, restored.settings?.weeklyGoalDays ?: -1)
    }

    @Test
    fun countsInvalidAndDuplicateRows() {
        val raw =
            """{"app":"tekrar","format":3,"exportedAt":1,"words":[""" +
                """{"id":1,"term":"Alpha","meaning":"birinci"},""" +
                """{"id":2,"term":"  alpha  ","meaning":"kopya term"},""" +
                """{"id":3,"term":"bos","meaning":"  "}],""" +
                """"reviewLogs":[""" +
                """{"id":10,"wordId":1,"rating":3,"reviewedAt":100},""" +
                """{"id":11,"wordId":2,"rating":3,"reviewedAt":100},""" +
                """{"id":12,"wordId":1,"rating":9,"reviewedAt":100}]}"""

        val restored = BackupFormat.parse(raw)

        assertEquals(1, restored.words.size)
        assertEquals(1L, restored.words.first().id)
        assertEquals(2, restored.skippedWords)
        assertEquals(1, restored.logs.size)
        assertEquals(10L, restored.logs.first().id)
        assertEquals(2, restored.skippedLogs)
    }
}
