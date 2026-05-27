package com.clippy.app.data.repository

import com.clippy.app.data.database.ClipDao
import com.clippy.app.data.database.ClipEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing clipboard data.
 * Acts as a single source of truth for clipboard items.
 */
@Singleton
class ClipRepository @Inject constructor(
    private val clipDao: ClipDao
) {
    /**
     * Get all clips as a Flow for reactive updates.
     */
    fun getAllClips(): Flow<List<ClipEntity>> = clipDao.getAllClips()
    
    /**
     * Get all clips as a one-time list.
     */
    suspend fun getAllClipsOnce(): List<ClipEntity> = clipDao.getAllClipsOnce()
    
    /**
     * Get pinned clips only.
     */
    fun getPinnedClips(): Flow<List<ClipEntity>> = clipDao.getPinnedClips()
    
    /**
     * Get recent clips for notification display.
     */
    suspend fun getRecentClips(limit: Int = 5): List<ClipEntity> = clipDao.getRecentClips(limit)
    
    /**
     * Get a clip by ID.
     */
    suspend fun getClipById(id: Long): ClipEntity? = clipDao.getClipById(id)
    
    /**
     * Add a new clip to the history.
     * If the same content already exists, update its timestamp instead.
     * 
     * @param content The text content to save
     * @param maxHistorySize Maximum number of unpinned items to keep
     * @return The ID of the inserted or updated clip
     */
    suspend fun addClip(content: String, maxHistorySize: Int = 100): Long {
        // Check if content already exists
        val existingClip = clipDao.getClipByContent(content)
        
        return if (existingClip != null) {
            // Update timestamp of existing clip
            clipDao.updateTimestamp(existingClip.id)
            existingClip.id
        } else {
            // Insert new clip
            val clip = ClipEntity(
                content = content,
                preview = content.take(ClipEntity.MAX_PREVIEW_LENGTH)
            )
            val id = clipDao.insertClip(clip)
            
            // Enforce history limit
            enforceHistoryLimit(maxHistorySize)
            
            id
        }
    }
    
    /**
     * Delete a clip by ID.
     */
    suspend fun deleteClip(id: Long) {
        clipDao.deleteClipById(id)
    }
    
    /**
     * Delete a clip entity.
     */
    suspend fun deleteClip(clip: ClipEntity) {
        clipDao.deleteClip(clip)
    }
    
    /**
     * Toggle pin status for a clip.
     */
    suspend fun togglePin(id: Long) {
        clipDao.togglePin(id)
    }
    
    /**
     * Pin a clip.
     */
    suspend fun pinClip(id: Long) {
        val clip = clipDao.getClipById(id)
        if (clip != null && !clip.isPinned) {
            clipDao.updateClip(clip.copy(isPinned = true))
        }
    }
    
    /**
     * Unpin a clip.
     */
    suspend fun unpinClip(id: Long) {
        val clip = clipDao.getClipById(id)
        if (clip != null && clip.isPinned) {
            clipDao.updateClip(clip.copy(isPinned = false))
        }
    }
    
    /**
     * Clear all unpinned clips.
     */
    suspend fun clearUnpinned() {
        clipDao.deleteAllUnpinned()
    }
    
    /**
     * Clear all clips including pinned.
     */
    suspend fun clearAll() {
        clipDao.deleteAll()
    }
    
    /**
     * Get the total count of clips.
     */
    suspend fun getClipCount(): Int = clipDao.getClipCount()
    
    /**
     * Enforce the maximum history size by deleting oldest unpinned clips.
     */
    suspend fun enforceHistoryLimit(maxSize: Int) {
        val unpinnedCount = clipDao.getUnpinnedClipCount()
        if (unpinnedCount > maxSize) {
            clipDao.deleteOldestUnpinned(maxSize)
        }
    }
    
    /**
     * Re-insert a deleted clip (for undo functionality).
     */
    suspend fun restoreClip(clip: ClipEntity): Long {
        return clipDao.insertClip(clip)
    }
}