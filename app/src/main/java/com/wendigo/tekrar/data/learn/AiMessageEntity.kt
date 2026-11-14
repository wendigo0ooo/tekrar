package com.wendigo.tekrar.data.learn

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AI koç mesajı (Room v5, tablo `ai_message`).
 * `role`: user/assistant/system; `status`: pending/streaming/done/error.
 */
@Entity(
    tableName = "ai_message",
    indices = [Index(value = ["sessionId"])],
)
data class AiMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val role: String,
    val content: String,
    val correctionJson: String? = null,
    val turnIndex: Int,
    val status: String,
    val tokens: Int? = null,
    val promptVersion: String? = null,
    val createdAt: Long,
)
