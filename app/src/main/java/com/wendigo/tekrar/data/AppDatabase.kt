package com.wendigo.tekrar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wendigo.tekrar.data.learn.AiMessageEntity
import com.wendigo.tekrar.data.learn.AiMessageDao
import com.wendigo.tekrar.data.learn.AiSessionEntity
import com.wendigo.tekrar.data.learn.AiSessionDao
import com.wendigo.tekrar.data.learn.LessonProgressEntity
import com.wendigo.tekrar.data.learn.LessonProgressDao

@Database(
    entities = [
        Word::class,
        ReviewLog::class,
        LessonProgressEntity::class,
        AiSessionEntity::class,
        AiMessageEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun lessonProgressDao(): LessonProgressDao
    abstract fun aiSessionDao(): AiSessionDao
    abstract fun aiMessageDao(): AiMessageDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE words ADD COLUMN exampleTr TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE words ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE words ADD COLUMN pos TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE words ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE words ADD COLUMN deck TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE words ADD COLUMN cefr TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE words ADD COLUMN sourceKey TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_words_sourceKey " +
                        "ON words(sourceKey)",
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS lesson_progress (" +
                        "lessonId TEXT NOT NULL PRIMARY KEY, " +
                        "unitId TEXT NOT NULL, " +
                        "status TEXT NOT NULL, " +
                        "bestScore INTEGER NOT NULL, " +
                        "lastScore INTEGER NOT NULL, " +
                        "attempts INTEGER NOT NULL, " +
                        "completedAt INTEGER, " +
                        "completedEpochDay INTEGER, " +
                        "contentVersion TEXT, " +
                        "lastOpenedAt INTEGER, " +
                        "updatedAt INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_lesson_progress_unitId " +
                        "ON lesson_progress(unitId)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_lesson_progress_status " +
                        "ON lesson_progress(status)",
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS ai_session (" +
                        "id TEXT NOT NULL PRIMARY KEY, " +
                        "mode TEXT NOT NULL, " +
                        "unitId TEXT, " +
                        "scenarioId TEXT, " +
                        "title TEXT NOT NULL, " +
                        "model TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL, " +
                        "updatedAt INTEGER NOT NULL, " +
                        "summaryJson TEXT, " +
                        "costMicros INTEGER NOT NULL, " +
                        "promptVersion TEXT NOT NULL, " +
                        "messageCount INTEGER NOT NULL)",
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS ai_message (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "sessionId TEXT NOT NULL, " +
                        "role TEXT NOT NULL, " +
                        "content TEXT NOT NULL, " +
                        "correctionJson TEXT, " +
                        "turnIndex INTEGER NOT NULL, " +
                        "status TEXT NOT NULL, " +
                        "tokens INTEGER, " +
                        "promptVersion TEXT, " +
                        "createdAt INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_ai_message_sessionId " +
                        "ON ai_message(sessionId)",
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tekrar.db",
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                    )
                    .build()
                    .also { instance = it }
            }
    }
}
