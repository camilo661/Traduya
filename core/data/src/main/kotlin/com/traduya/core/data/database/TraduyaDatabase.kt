package com.traduya.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CallEntity::class, TranscriptLineEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TraduyaDatabase : RoomDatabase() {
    abstract fun callDao(): CallDao

    companion object {
        const val NAME = "traduya.db"
    }
}
