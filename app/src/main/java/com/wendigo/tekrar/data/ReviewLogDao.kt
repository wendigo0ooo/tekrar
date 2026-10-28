package com.wendigo.tekrar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewLogDao {

    @Insert
    suspend fun insert(log: ReviewLog): Long

    @Query("SELECT COUNT(*) FROM review_logs WHERE reviewedAt >= :since")
    fun observeCountSince(since: Long): Flow<Int>

    @Query("SELECT * FROM review_logs ORDER BY reviewedAt ASC, id ASC")
    fun observeAll(): Flow<List<ReviewLog>>

    @Query("SELECT * FROM review_logs WHERE reviewedAt >= :since ORDER BY reviewedAt ASC, id ASC")
    fun observeSince(since: Long): Flow<List<ReviewLog>>

    @Query("SELECT * FROM review_logs ORDER BY reviewedAt ASC, id ASC")
    suspend fun allLogs(): List<ReviewLog>

    @Query("DELETE FROM review_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM review_logs WHERE wordId = :wordId")
    suspend fun deleteForWord(wordId: Long)

    @Query("DELETE FROM review_logs")
    suspend fun deleteAll()
}
