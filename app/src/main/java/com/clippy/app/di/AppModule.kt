package com.clippy.app.di

import android.content.Context
import androidx.room.Room
import com.clippy.app.data.database.ClipDao
import com.clippy.app.data.database.ClipDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing app-level dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    /**
     * Provides the Room database instance.
     */
    @Provides
    @Singleton
    fun provideClipDatabase(
        @ApplicationContext context: Context
    ): ClipDatabase {
        return Room.databaseBuilder(
            context,
            ClipDatabase::class.java,
            ClipDatabase.DATABASE_NAME
        )
            .addMigrations(ClipDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }
    
    /**
     * Provides the ClipDao from the database.
     */
    @Provides
    @Singleton
    fun provideClipDao(database: ClipDatabase): ClipDao {
        return database.clipDao()
    }
}