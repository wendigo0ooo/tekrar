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
    @ColumnInfo(defaultValue = "''")
    val tags: String = "",
    @ColumnInfo(defaultValue = "''")
    val pos: String = "",
    @ColumnInfo(defaultValue = "0")
    val favorite: Boolean = false,
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
    val isNew: Boolean get() = state == STATE_NEW
    val isLeech: Boolean get() = lapses >= LEECH_LAPSES

    fun resetProgress(now: Long): Word = copy(
        state = STATE_NEW,
        stability = 0.0,
        difficulty = 0.0,
        dueAt = now,
        lastReviewAt = null,
        introducedAt = null,
        reps = 0,
        lapses = 0,
    )

    companion object {
        const val STATE_NEW = 0
        const val STATE_REVIEW = 1
        const val LEECH_LAPSES = 8
    }
}
