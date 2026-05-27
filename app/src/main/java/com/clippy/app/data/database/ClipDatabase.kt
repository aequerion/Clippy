package com.clippy.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for storing clipboard history.
 */
@Database(
    entities = [ClipEntity::class],
    version = 1,
    exportSchema = true
)
abstract class ClipDatabase : RoomDatabase() {
    
    abstract fun clipDao(): ClipDao
    
    companion object {
        const val DATABASE_NAME = "clippy_database"
    }
}