package com.traduya.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CallEntity::class, TranscriptLineEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class TraduyaDatabase : RoomDatabase() {
    abstract fun callDao(): CallDao

    companion object {
        const val NAME = "traduya.db"

        /** v2: grabación de la llamada. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE calls ADD COLUMN recordingPath TEXT")
            }
        }
    }
}
