package com.wendigo.tekrar.data.learn

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiSessionDao {

    @Query("SELECT * FROM ai_session ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<AiSessionEntity>>

    @Query("SELECT * FROM ai_session WHERE id = :id")
    suspend fun get(id: String): AiSessionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: AiSessionEntity)

    @Query(
        "UPDATE ai_session SET summaryJson = :summaryJson, updatedAt = :updatedAt " +
            "WHERE id = :id"
    )
    suspend fun updateSummary(id: String, summaryJson: String?, updatedAt: Long)

    @Query(
        "UPDATE ai_session SET messageCount = :count, updatedAt = :updatedAt " +
            "WHERE id = :id"
    )
    suspend fun updateMessageCount(id: String, count: Int, updatedAt: Long)

    @Query("UPDATE ai_session SET model = :model, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateModel(id: String, model: String, updatedAt: Long)

    @Query("DELETE FROM ai_session WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM ai_session")
    suspend fun deleteAll()
}
