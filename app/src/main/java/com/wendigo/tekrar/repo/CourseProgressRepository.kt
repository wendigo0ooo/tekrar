package com.wendigo.tekrar.repo

import com.wendigo.tekrar.core.StatsMath
import com.wendigo.tekrar.core.learn.CourseUnit
import com.wendigo.tekrar.core.learn.LessonStatus
import com.wendigo.tekrar.data.AppDatabase
import com.wendigo.tekrar.data.learn.LessonProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Ders ilerlemesi + ünite kilidi. Gün anahtarı `StatsMath.dayKey` üzerinden
 * tek yerden üretilir; seri hesabı `completedEpochDay` ile birleştirilebilir.
 */
class CourseProgressRepository(
    db: AppDatabase,
) {

    private val dao = db.lessonProgressDao()

    fun observeAll(): Flow<List<LessonProgressEntity>> = dao.observeAll()

    suspend fun statusMap(): Map<String, LessonStatus> = withContext(Dispatchers.IO) {
        dao.getAll().associate { it.lessonId to statusOf(it.status) }
    }

    suspend fun progress(lessonId: String): LessonProgressEntity? =
        withContext(Dispatchers.IO) { dao.get(lessonId) }

    suspend fun markOpened(lessonId: String, unitId: String, now: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            val existing = dao.get(lessonId)
            val entity = (existing ?: LessonProgressEntity(
                lessonId = lessonId,
                unitId = unitId,
                status = LessonStatus.AVAILABLE.name.lowercase(),
                updatedAt = now,
            )).copy(
                lastOpenedAt = now,
                updatedAt = now,
            )
            dao.upsert(entity)
        }

    suspend fun completeLesson(
        lessonId: String,
        unitId: String,
        scorePercent: Int,
        contentVersion: String? = null,
        now: Long = System.currentTimeMillis(),
    ): LessonProgressEntity = withContext(Dispatchers.IO) {
        val existing = dao.get(lessonId)
        val safeScore = scorePercent.coerceIn(0, 100)
        val wasMastered = existing?.status == LessonStatus.MASTERED.name.lowercase()
        val entity = LessonProgressEntity(
            lessonId = lessonId,
            unitId = unitId,
            status = if (wasMastered) {
                LessonStatus.MASTERED.name.lowercase()
            } else {
                LessonStatus.COMPLETED.name.lowercase()
            },
            bestScore = maxOf(existing?.bestScore ?: 0, safeScore),
            lastScore = safeScore,
            attempts = (existing?.attempts ?: 0) + 1,
            completedAt = now,
            completedEpochDay = StatsMath.dayKey(now).toEpochDay(),
            contentVersion = contentVersion ?: existing?.contentVersion,
            lastOpenedAt = now,
            updatedAt = now,
        )
        dao.upsert(entity)
        entity
    }

    suspend fun failLesson(
        lessonId: String,
        unitId: String,
        scorePercent: Int,
        now: Long = System.currentTimeMillis(),
    ): LessonProgressEntity = withContext(Dispatchers.IO) {
        val existing = dao.get(lessonId)
        val entity = (existing ?: LessonProgressEntity(
            lessonId = lessonId,
            unitId = unitId,
            status = LessonStatus.IN_PROGRESS.name.lowercase(),
            updatedAt = now,
        )        ).copy(
            status = LessonStatus.IN_PROGRESS.name.lowercase(),
            lastScore = scorePercent.coerceIn(0, 100),
            attempts = (existing?.attempts ?: 0) + 1,
            lastOpenedAt = now,
            updatedAt = now,
        )
        dao.upsert(entity)
        entity
    }

    suspend fun unitCompleted(unit: CourseUnit): Boolean = withContext(Dispatchers.IO) {
        val map = statusMap()
        unit.lessons.all { lesson ->
            when (map[lesson.id]) {
                LessonStatus.COMPLETED, LessonStatus.MASTERED -> true
                else -> false
            }
        }
    }

    suspend fun completedUnitIds(units: List<CourseUnit>): Set<String> =
        withContext(Dispatchers.IO) {
            units.filter { unitCompleted(it) }.map { it.id }.toSet()
        }

    /** Seri hesabı için ders tamamlama günleri (epoch day). */
    suspend fun activityEpochDays(): Set<Long> = withContext(Dispatchers.IO) {
        dao.completedEpochDays().toSet()
    }

    /** Debug/test yardımcısı: bugünün epoch day değeri. */
    fun todayEpochDay(now: Long = System.currentTimeMillis()): Long =
        StatsMath.dayKey(now, java.time.ZoneId.systemDefault()).toEpochDay()

    suspend fun todayCompletedCount(now: Long = System.currentTimeMillis()): Int =
        withContext(Dispatchers.IO) {
            val today = StatsMath.dayKey(now)
            dao.getAll().count {
                it.completedEpochDay == today.toEpochDay() &&
                    (it.status == LessonStatus.COMPLETED.name.lowercase() ||
                        it.status == LessonStatus.MASTERED.name.lowercase())
            }
        }

    private fun statusOf(raw: String): LessonStatus =
        LessonStatus.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
            ?: LessonStatus.AVAILABLE
}
