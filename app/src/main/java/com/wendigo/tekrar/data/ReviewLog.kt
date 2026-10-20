package com.wendigo.tekrar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "review_logs")
data class ReviewLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: Long,
    val rating: Int,
    val reviewedAt: Long,
    val previousDueAt: Long,
    val previousStability: Double,
    val previousDifficulty: Double,
    val nextDueAt: Long,
    val nextStability: Double,
    val nextDifficulty: Double,
    val elapsedDays: Double,
)
