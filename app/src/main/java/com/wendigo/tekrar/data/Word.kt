package com.wendigo.tekrar.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val term: String,
    val meaning: String,
    val example: String = "",
    @ColumnInfo(defaultValue = "''")
    val exampleTr: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val state: Int = STATE_NEW,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val dueAt: Long = System.currentTimeMillis(),
    val lastReviewAt: Long? = null,
    val introducedAt: Long? = null,
    val reps: Int = 0,
    val lapses: Int = 0,
    val suspended: Boolean = false,
) {
    companion object {
        const val STATE_NEW = 0
        const val STATE_REVIEW = 1
    }
}
