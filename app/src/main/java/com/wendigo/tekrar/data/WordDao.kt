package com.wendigo.tekrar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Query("SELECT * FROM words WHERE suspended = 0 ORDER BY dueAt ASC, id ASC")
    fun observeAll(): Flow<List<Word>>

    @Query("SELECT COUNT(*) FROM words WHERE suspended = 0 AND state = 1 AND dueAt <= :now")
    fun observeReviewDueCount(now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE suspended = 0 AND state = 0")
    fun observeNewTotal(): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE suspended = 0")
    fun observeTotal(): Flow<Int>

    @Query("SELECT MIN(dueAt) FROM words WHERE suspended = 0 AND state = 1 AND dueAt > :now")
    fun observeNextReviewDue(now: Long): Flow<Long?>

    @Query("SELECT COUNT(*) FROM words WHERE suspended = 0 AND introducedAt IS NOT NULL AND introducedAt >= :since")
    fun observeIntroducedSince(since: Long): Flow<Int>

    @Query("SELECT * FROM words WHERE suspended = 0 AND state = 1 AND dueAt <= :now ORDER BY dueAt ASC, id ASC")
    suspend fun dueReviews(now: Long): List<Word>

    @Query("SELECT * FROM words WHERE suspended = 0 AND state = 0 ORDER BY id ASC LIMIT :limit")
    suspend fun newWords(limit: Int): List<Word>

    @Query("SELECT COUNT(*) FROM words WHERE suspended = 0 AND introducedAt IS NOT NULL AND introducedAt >= :since")
    suspend fun introducedCountSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM words")
    suspend fun countAll(): Int

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getById(id: Long): Word?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(word: Word): Long

    @Update
    suspend fun update(word: Word)

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun deleteById(id: Long)
}
