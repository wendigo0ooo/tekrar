package com.wendigo.tekrar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewLogDao {

    @Insert
    suspend fun insert(log: ReviewLog)

    @Query("SELECT COUNT(*) FROM review_logs WHERE reviewedAt >= :since")
    fun observeCountSince(since: Long): Flow<Int>
}
