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
     * Get all active clips (not deleted) ordered by pinned status (pinned first) then by timestamp (newest first).
     */
    @Query("SELECT * FROM clips WHERE isDeleted = 0 ORDER BY isPinned DESC, timestamp DESC")
    fun getAllClips(): Flow<List<ClipEntity>>
    
    /**
     * Get all active clips as a one-time list (not Flow).
     */
    @Query("SELECT * FROM clips WHERE isDeleted = 0 ORDER BY isPinned DESC, timestamp DESC")
    suspend fun getAllClipsOnce(): List<ClipEntity>
    
    /**
     * Get only pinned clips (not deleted).
     */
    @Query("SELECT * FROM clips WHERE isPinned = 1 AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getPinnedClips(): Flow<List<ClipEntity>>
    
    /**
     * Get only unpinned clips ordered by timestamp (not deleted).
     */
    @Query("SELECT * FROM clips WHERE isPinned = 0 AND isDeleted = 0 ORDER BY timestamp DESC")
    suspend fun getUnpinnedClips(): List<ClipEntity>
    
    /**
     * Get the most recent clips (for notification display, not deleted).
     */
    @Query("SELECT * FROM clips WHERE isDeleted = 0 ORDER BY isPinned DESC, timestamp DESC LIMIT :limit")
    suspend fun getRecentClips(limit: Int): List<ClipEntity>
    
    /**
     * Get a clip by its ID.
     */
    @Query("SELECT * FROM clips WHERE id = :id")
    suspend fun getClipById(id: Long): ClipEntity?
    
    /**
     * Check if content already exists in the database (not deleted).
     */
    @Query("SELECT * FROM clips WHERE content = :content AND isDeleted = 0 LIMIT 1")
    suspend fun getClipByContent(content: String): ClipEntity?
    
    /**
     * Get the count of all active clips.
     */
    @Query("SELECT COUNT(*) FROM clips WHERE isDeleted = 0")
    suspend fun getClipCount(): Int
    
    /**
     * Get the count of unpinned active clips.
     */
    @Query("SELECT COUNT(*) FROM clips WHERE isPinned = 0 AND isDeleted = 0")
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
     * Delete a clip permanently.
     */
    @Delete
    suspend fun deleteClip(clip: ClipEntity)
    
    /**
     * Delete a clip permanently by ID.
     */
    @Query("DELETE FROM clips WHERE id = :id")
    suspend fun deleteClipById(id: Long)
    
    /**
     * Soft delete (move to bin) - delete all unpinned clips.
     */
    @Query("UPDATE clips SET isDeleted = 1, deletedAt = :deletedAt WHERE isPinned = 0 AND isDeleted = 0")
    suspend fun softDeleteAllUnpinned(deletedAt: Long = System.currentTimeMillis())
    
    /**
     * Soft delete (move to bin) a single clip by ID.
     */
    @Query("UPDATE clips SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteById(id: Long, deletedAt: Long = System.currentTimeMillis())
    
    /**
     * Delete all clips permanently (including pinned).
     */
    @Query("DELETE FROM clips")
    suspend fun deleteAll()
    
    /**
     * Delete oldest unpinned clips to maintain history limit.
     * Keeps the most recent 'keepCount' unpinned items.
     */
    @Query("""
        DELETE FROM clips 
        WHERE isPinned = 0 AND isDeleted = 0
        AND id NOT IN (
            SELECT id FROM clips 
            WHERE isPinned = 0 AND isDeleted = 0
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
    
    // ==================== BIN RELATED QUERIES ====================
    
    /**
     * Get all deleted clips (bin items) ordered by deletion time (newest first).
     */
    @Query("SELECT * FROM clips WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getBinClips(): Flow<List<ClipEntity>>
    
    /**
     * Get all deleted clips as a one-time list.
     */
    @Query("SELECT * FROM clips WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    suspend fun getBinClipsOnce(): List<ClipEntity>
    
    /**
     * Get the count of clips in bin.
     */
    @Query("SELECT COUNT(*) FROM clips WHERE isDeleted = 1")
    suspend fun getBinCount(): Int
    
    /**
     * Restore a clip from bin.
     */
    @Query("UPDATE clips SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreFromBin(id: Long)
    
    /**
     * Restore all clips from bin.
     */
    @Query("UPDATE clips SET isDeleted = 0, deletedAt = NULL WHERE isDeleted = 1")
    suspend fun restoreAllFromBin()
    
    /**
     * Permanently delete all clips in bin.
     */
    @Query("DELETE FROM clips WHERE isDeleted = 1")
    suspend fun emptyBin()
    
    /**
     * Permanently delete clips that have been in bin for more than the specified time.
     * Used for 30-day auto-clear.
     */
    @Query("DELETE FROM clips WHERE isDeleted = 1 AND deletedAt < :cutoffTime")
    suspend fun deleteOldBinItems(cutoffTime: Long)
}