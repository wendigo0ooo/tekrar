package com.wendigo.tekrar.data.learn

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonProgressDao {

    @Query("SELECT * FROM lesson_progress ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId")
    fun observe(lessonId: String): Flow<LessonProgressEntity?>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId")
    suspend fun get(lessonId: String): LessonProgressEntity?

    @Query("SELECT * FROM lesson_progress")
    suspend fun getAll(): List<LessonProgressEntity>

    @Query("SELECT completedEpochDay FROM lesson_progress WHERE completedEpochDay IS NOT NULL")
    suspend fun completedEpochDays(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: LessonProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LessonProgressEntity>)

    @Query("DELETE FROM lesson_progress")
    suspend fun deleteAll()
}
