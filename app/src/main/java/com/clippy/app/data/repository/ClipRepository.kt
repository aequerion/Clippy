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
     * Soft delete a clip (move to bin) by ID.
     */
    suspend fun deleteClip(id: Long) {
        clipDao.softDeleteById(id)
    }
    
    /**
     * Soft delete a clip entity (move to bin).
     */
    suspend fun deleteClip(clip: ClipEntity) {
        clipDao.softDeleteById(clip.id)
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
     * Soft delete all unpinned clips (move to bin).
     */
    suspend fun clearUnpinned() {
        clipDao.softDeleteAllUnpinned()
    }
    
    /**
     * Clear all clips including pinned (permanent delete).
     */
    suspend fun clearAll() {
        clipDao.deleteAll()
    }
    
    /**
     * Get the total count of active clips.
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
    
    // ==================== BIN RELATED METHODS ====================
    
    /**
     * Get all clips in the bin as a Flow.
     */
    fun getBinClips(): Flow<List<ClipEntity>> = clipDao.getBinClips()
    
    /**
     * Get all clips in the bin as a one-time list.
     */
    suspend fun getBinClipsOnce(): List<ClipEntity> = clipDao.getBinClipsOnce()
    
    /**
     * Get the count of clips in bin.
     */
    suspend fun getBinCount(): Int = clipDao.getBinCount()
    
    /**
     * Restore a clip from bin.
     */
    suspend fun restoreFromBin(id: Long) {
        clipDao.restoreFromBin(id)
    }
    
    /**
     * Restore all clips from bin.
     */
    suspend fun restoreAllFromBin() {
        clipDao.restoreAllFromBin()
    }
    
    /**
     * Permanently delete all clips in bin.
     */
    suspend fun emptyBin() {
        clipDao.emptyBin()
    }
    
    /**
     * Permanently delete a single clip from bin.
     */
    suspend fun permanentlyDelete(id: Long) {
        clipDao.deleteClipById(id)
    }
    
    /**
     * Clean up old bin items (30-day auto-clear).
     * Deletes items that have been in bin for more than 30 days.
     */
    suspend fun cleanupOldBinItems() {
        val cutoffTime = System.currentTimeMillis() - ClipEntity.BIN_RETENTION_MS
        clipDao.deleteOldBinItems(cutoffTime)
    }
}