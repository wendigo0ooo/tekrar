package com.wendigo.tekrar.data.learn

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AI koç oturumu (Room v5, tablo `ai_session`). Oturumlar yedeğe girmez.
 * `mode`: chat/roleplay/correction.
 */
@Entity(tableName = "ai_session")
data class AiSessionEntity(
    @PrimaryKey val id: String,
    val mode: String,
    val unitId: String? = null,
    val scenarioId: String? = null,
    val title: String,
    val model: String,
    val createdAt: Long,
    val updatedAt: Long,
    val summaryJson: String? = null,
    val costMicros: Long = 0L,
    val promptVersion: String,
    val messageCount: Int = 0,
)
