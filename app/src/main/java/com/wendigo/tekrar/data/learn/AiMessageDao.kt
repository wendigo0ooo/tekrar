package com.wendigo.tekrar.data.learn

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiMessageDao {

    @Query("SELECT * FROM ai_message WHERE sessionId = :sessionId ORDER BY turnIndex ASC, id ASC")
    fun observeBySession(sessionId: String): Flow<List<AiMessageEntity>>

    @Query("SELECT * FROM ai_message WHERE sessionId = :sessionId ORDER BY turnIndex ASC, id ASC")
    suspend fun getBySession(sessionId: String): List<AiMessageEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(message: AiMessageEntity): Long

    @Query("SELECT * FROM ai_message WHERE id = :id")
    suspend fun get(id: Long): AiMessageEntity?

    @Query("SELECT COALESCE(MAX(turnIndex), -1) + 1 FROM ai_message WHERE sessionId = :sessionId")
    suspend fun nextTurnIndex(sessionId: String): Int

    @Query("SELECT COUNT(*) FROM ai_message WHERE sessionId = :sessionId")
    suspend fun countBySession(sessionId: String): Int

    @Query("UPDATE ai_message SET correctionJson = :correctionJson WHERE id = :id")
    suspend fun updateCorrection(id: Long, correctionJson: String?)

    @Query("DELETE FROM ai_message WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)

    @Query("DELETE FROM ai_message")
    suspend fun deleteAll()
}
