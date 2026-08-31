package com.premium.timer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TimerStateEntity::class, SessionHistoryEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timerStateDao(): TimerStateDao
    abstract fun sessionHistoryDao(): SessionHistoryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /**
         * v1 -> v2: adds appearance/precision columns to timer_state (with safe defaults so
         * existing rows don't break) and adds the new session_history table. This is an ADDITIVE
         * migration — no existing data is dropped, per the "never lose data on update" requirement.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE timer_state ADD COLUMN visualStyle TEXT NOT NULL DEFAULT 'DIGITAL'")
                db.execSQL("ALTER TABLE timer_state ADD COLUMN backgroundType TEXT NOT NULL DEFAULT 'COLOR'")
                db.execSQL("ALTER TABLE timer_state ADD COLUMN backgroundImageUri TEXT")
                db.execSQL("ALTER TABLE timer_state ADD COLUMN precision TEXT NOT NULL DEFAULT 'HMS'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS session_history (
                        id TEXT NOT NULL PRIMARY KEY,
                        timerName TEXT NOT NULL,
                        mode TEXT NOT NULL,
                        startWallClock INTEGER NOT NULL,
                        endWallClock INTEGER NOT NULL,
                        plannedMillis INTEGER NOT NULL,
                        actualActiveMillis INTEGER NOT NULL,
                        completed INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /** v2 -> v3: typography + lightweight project/tag fields. Additive, no data loss. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE timer_state ADD COLUMN fontChoice TEXT NOT NULL DEFAULT 'MODERN'")
                db.execSQL("ALTER TABLE timer_state ADD COLUMN project TEXT")
                db.execSQL("ALTER TABLE timer_state ADD COLUMN tags TEXT")
                db.execSQL("ALTER TABLE session_history ADD COLUMN project TEXT")
                db.execSQL("ALTER TABLE session_history ADD COLUMN tags TEXT")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "premium_timer.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
