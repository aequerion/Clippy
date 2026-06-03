package com.clippy.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for storing clipboard history.
 */
@Database(
    entities = [ClipEntity::class],
    version = 2,
    exportSchema = true
)
abstract class ClipDatabase : RoomDatabase() {
    
    abstract fun clipDao(): ClipDao
    
    companion object {
        const val DATABASE_NAME = "clippy_database"
        
        /**
         * Migration from version 1 to 2: Add bin (soft delete) support.
         * Adds isDeleted and deletedAt columns.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add isDeleted column with default value 0 (false)
                database.execSQL("ALTER TABLE clips ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                // Add deletedAt column (nullable)
                database.execSQL("ALTER TABLE clips ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            }
        }
    }
}