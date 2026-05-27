package com.clippy.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for clipboard items.
 */
@Dao
interface ClipDao {
    
    /**
     * Get all clips ordered by pinned status (pinned first) then by timestamp (newest first).
     */
    @Query("SELECT * FROM clips ORDER BY isPinned DESC, timestamp DESC")
    fun getAllClips(): Flow<List<ClipEntity>>
    
    /**
     * Get all clips as a one-time list (not Flow).
     */
    @Query("SELECT * FROM clips ORDER BY isPinned DESC, timestamp DESC")
    suspend fun getAllClipsOnce(): List<ClipEntity>
    
    /**
     * Get only pinned clips.
     */
    @Query("SELECT * FROM clips WHERE isPinned = 1 ORDER BY timestamp DESC")
    fun getPinnedClips(): Flow<List<ClipEntity>>
    
    /**
     * Get only unpinned clips ordered by timestamp.
     */
    @Query("SELECT * FROM clips WHERE isPinned = 0 ORDER BY timestamp DESC")
    suspend fun getUnpinnedClips(): List<ClipEntity>
    
    /**
     * Get the most recent clips (for notification display).
     */
    @Query("SELECT * FROM clips ORDER BY isPinned DESC, timestamp DESC LIMIT :limit")
    suspend fun getRecentClips(limit: Int): List<ClipEntity>
    
    /**
     * Get a clip by its ID.
     */
    @Query("SELECT * FROM clips WHERE id = :id")
    suspend fun getClipById(id: Long): ClipEntity?
    
    /**
     * Check if content already exists in the database.
     */
    @Query("SELECT * FROM clips WHERE content = :content LIMIT 1")
    suspend fun getClipByContent(content: String): ClipEntity?
    
    /**
     * Get the count of all clips.
     */
    @Query("SELECT COUNT(*) FROM clips")
    suspend fun getClipCount(): Int
    
    /**
     * Get the count of unpinned clips.
     */
    @Query("SELECT COUNT(*) FROM clips WHERE isPinned = 0")
    suspend fun getUnpinnedClipCount(): Int
    
    /**
     * Insert a new clip.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: ClipEntity): Long
    
    /**
     * Update an existing clip.
     */
    @Update
    suspend fun updateClip(clip: ClipEntity)
    
    /**
     * Delete a clip.
     */
    @Delete
    suspend fun deleteClip(clip: ClipEntity)
    
    /**
     * Delete a clip by ID.
     */
    @Query("DELETE FROM clips WHERE id = :id")
    suspend fun deleteClipById(id: Long)
    
    /**
     * Delete all unpinned clips.
     */
    @Query("DELETE FROM clips WHERE isPinned = 0")
    suspend fun deleteAllUnpinned()
    
    /**
     * Delete all clips (including pinned).
     */
    @Query("DELETE FROM clips")
    suspend fun deleteAll()
    
    /**
     * Delete oldest unpinned clips to maintain history limit.
     * Keeps the most recent 'keepCount' unpinned items.
     */
    @Query("""
        DELETE FROM clips 
        WHERE isPinned = 0 
        AND id NOT IN (
            SELECT id FROM clips 
            WHERE isPinned = 0 
            ORDER BY timestamp DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun deleteOldestUnpinned(keepCount: Int)
    
    /**
     * Toggle pin status for a clip.
     */
    @Query("UPDATE clips SET isPinned = NOT isPinned WHERE id = :id")
    suspend fun togglePin(id: Long)
    
    /**
     * Update timestamp for a clip (when re-copied).
     */
    @Query("UPDATE clips SET timestamp = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(id: Long, timestamp: Long = System.currentTimeMillis())
}