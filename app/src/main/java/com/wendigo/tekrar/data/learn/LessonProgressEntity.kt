package com.wendigo.tekrar.data.learn

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Ders ilerlemesi (Room v5, tablo `lesson_progress`).
 * Durum metinleri `core.learn.LessonStatus` adlarıyla küçük harflidir:
 * locked/available/in_progress/completed/mastered.
 */
@Entity(
    tableName = "lesson_progress",
    indices = [
        Index(value = ["unitId"]),
        Index(value = ["status"]),
    ],
)
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val unitId: String,
    val status: String,
    val bestScore: Int = 0,
    val lastScore: Int = 0,
    val attempts: Int = 0,
    val completedAt: Long? = null,
    val completedEpochDay: Long? = null,
    val contentVersion: String? = null,
    val lastOpenedAt: Long? = null,
    val updatedAt: Long = 0L,
)
