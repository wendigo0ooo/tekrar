package com.wendigo.tekrar.repo

import android.content.Context
import com.wendigo.tekrar.core.learn.CourseIndex
import com.wendigo.tekrar.core.learn.CourseParser
import com.wendigo.tekrar.core.learn.CourseUnit
import com.wendigo.tekrar.core.learn.CourseWordSink
import com.wendigo.tekrar.core.learn.Lesson
import com.wendigo.tekrar.core.learn.LessonRef
import com.wendigo.tekrar.core.learn.RoleplayRef
import com.wendigo.tekrar.core.learn.RoleplayScenario
import com.wendigo.tekrar.core.learn.SeedResult
import com.wendigo.tekrar.core.learn.CourseSeeder
import com.wendigo.tekrar.core.learn.UnitRef
import com.wendigo.tekrar.data.AppDatabase
import com.wendigo.tekrar.data.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Asset'lerdeki kurs içeriğini yükler (`assets/course/`) ve ders kelimelerini
 * FSRS kelime tablosuna aktarır.
 *
 * Tüm okuma/yazma IO dispatcher'ında; parser saf Kotlin'dir.
 */
class CourseRepository(
    context: Context,
    private val db: AppDatabase,
) {

    private val assets = context.applicationContext.assets
    private val loadMutex = Mutex()
    private val seedMutex = Mutex()

    private var indexCache: CourseIndex? = null
    private val unitCache = HashMap<String, CourseUnit>()
    private val lessonCache = HashMap<String, Lesson>()
    private val roleplayCache = HashMap<String, RoleplayScenario>()

    suspend fun loadIndex(): CourseIndex = withContext(Dispatchers.IO) {
        indexCache ?: loadMutex.withLock {
            indexCache ?: run {
                val json = read("$ROOT/index.json")
                CourseParser.parseIndex(json, "course/index.json").also { indexCache = it }
            }
        }
    }

    suspend fun loadUnit(ref: UnitRef): CourseUnit = withContext(Dispatchers.IO) {
        unitCache[ref.id] ?: loadMutex.withLock {
            unitCache[ref.id] ?: run {
                val path = "$ROOT/${ref.file}"
                CourseParser.parseUnit(read(path), path).also { unitCache[ref.id] = it }
            }
        }
    }

    suspend fun loadLesson(unitRef: UnitRef, lessonRef: LessonRef): Lesson =
        withContext(Dispatchers.IO) {
            lessonCache[lessonRef.id] ?: loadMutex.withLock {
                lessonCache[lessonRef.id] ?: run {
                    val dir = unitRef.file.substringBeforeLast('/', missingDelimiterValue = "")
                    val path = if (dir.isEmpty()) {
                        "$ROOT/${lessonRef.file}"
                    } else {
                        "$ROOT/$dir/${lessonRef.file}"
                    }
                    CourseParser.parseLesson(read(path), path).also { lessonCache[lessonRef.id] = it }
                }
            }
        }

    suspend fun loadRoleplay(unitRef: UnitRef, roleplayRef: RoleplayRef): RoleplayScenario =
        withContext(Dispatchers.IO) {
            roleplayCache[roleplayRef.id] ?: loadMutex.withLock {
                roleplayCache[roleplayRef.id] ?: run {
                    val dir = unitRef.file.substringBeforeLast('/', missingDelimiterValue = "")
                    val path = if (dir.isEmpty()) {
                        "$ROOT/${roleplayRef.file}"
                    } else {
                        "$ROOT/$dir/${roleplayRef.file}"
                    }
                    CourseParser.parseRoleplay(read(path), path).also {
                        roleplayCache[roleplayRef.id] = it
                    }
                }
            }
        }

    /** Dersin kelimelerini FSRS kuyruğuna ekler; idempotenttir. */
    suspend fun seedLessonVocab(lesson: Lesson): SeedResult = withContext(Dispatchers.IO) {
        seedMutex.withLock {
            CourseSeeder.seed(lesson, wordSink)
        }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            indexCache = null
            unitCache.clear()
            lessonCache.clear()
            roleplayCache.clear()
        }
    }

    private fun read(path: String): String =
        assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private val wordSink = object : CourseWordSink {
        override suspend fun findByTerm(term: String): Word? =
            db.wordDao().findByTerm(term)

        override suspend fun insert(word: Word): Long =
            db.wordDao().insertIfAbsent(word)

        override suspend fun addTag(word: Word, tag: String) {
            val tags = word.tags.split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (tags.any { it.equals(tag, ignoreCase = true) }) return
            db.wordDao().update(word.copy(tags = (tags + tag).joinToString(",")))
        }
    }

    private companion object {
        const val ROOT = "course"
    }
}
