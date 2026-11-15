package com.wendigo.tekrar.repo

import com.wendigo.tekrar.core.learn.LessonStatus
import com.wendigo.tekrar.data.learn.LessonProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gramer ilerlemesi. Yeni Room tablosu/migration yoktur: mevcut
 * `lesson_progress` tablosu `grammar:<topicId>` kimlikleriyle kullanılır.
 * Bu kimlikler `CoursePath`/`unitCompleted` akışını etkilemez.
 */
class GrammarProgressRepository(
    private val progress: CourseProgressRepository,
) {

    /** Yalnız gramer konularının durumları: topicId -> durum. */
    suspend fun statusMap(): Map<String, LessonStatus> = withContext(Dispatchers.IO) {
        progress.statusMap().mapNotNull { (lessonId, status) ->
            val topicId = topicIdOf(lessonId) ?: return@mapNotNull null
            topicId to status
        }.toMap()
    }

    suspend fun status(topicId: String): LessonStatus =
        statusMap()[topicId] ?: LessonStatus.AVAILABLE

    suspend fun progress(topicId: String): LessonProgressEntity? =
        progress.progress(lessonIdOf(topicId))

    suspend fun markOpened(
        topicId: String,
        now: Long = System.currentTimeMillis(),
    ) {
        progress.markOpened(lessonIdOf(topicId), UNIT_ID, now)
    }

    /** 3 görev bitince çağrılır; konu `completed` olarak işaretlenir. */
    suspend fun complete(
        topicId: String,
        scorePercent: Int,
        now: Long = System.currentTimeMillis(),
    ): LessonProgressEntity = progress.completeLesson(
        lessonId = lessonIdOf(topicId),
        unitId = UNIT_ID,
        scorePercent = scorePercent,
        now = now,
    )

    suspend fun bestScore(topicId: String): Int =
        progress.progress(lessonIdOf(topicId))?.bestScore ?: 0

    suspend fun bestScores(): Map<String, Int> = withContext(Dispatchers.IO) {
        val result = LinkedHashMap<String, Int>()
        for (topicId in statusMap().keys) {
            result[topicId] = progress.progress(lessonIdOf(topicId))?.bestScore ?: 0
        }
        result
    }

    suspend fun completedTopicIds(): Set<String> = statusMap()
        .filterValues { it == LessonStatus.COMPLETED || it == LessonStatus.MASTERED }
        .keys

    companion object {
        const val PREFIX = "grammar:"
        const val UNIT_ID = "grammar"

        fun lessonIdOf(topicId: String): String = "$PREFIX$topicId"

        fun topicIdOf(lessonId: String): String? =
            lessonId.removePrefix(PREFIX).takeIf { it != lessonId && it.isNotEmpty() }
    }
}
