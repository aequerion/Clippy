package com.clippy.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a clipboard item stored in the database.
 */
@Entity(tableName = "clips")
data class ClipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * The text content of the clipboard item.
     */
    val content: String,
    
    /**
     * Timestamp when the item was copied (milliseconds since epoch).
     */
    val timestamp: Long = System.currentTimeMillis(),
    
    /**
     * Whether this item is pinned by the user.
     * Pinned items are not auto-deleted when history limit is reached.
     */
    val isPinned: Boolean = false,
    
    /**
     * Type of content (for future use with images).
     * Currently only "text" is supported.
     */
    val contentType: String = CONTENT_TYPE_TEXT,
    
    /**
     * Preview text for display (truncated version of content).
     */
    val preview: String = content.take(MAX_PREVIEW_LENGTH),
    
    /**
     * Whether this item is in the bin (soft deleted).
     */
    val isDeleted: Boolean = false,
    
    /**
     * Timestamp when the item was moved to bin (null if not deleted).
     */
    val deletedAt: Long? = null
) {
    companion object {
        const val CONTENT_TYPE_TEXT = "text"
        const val CONTENT_TYPE_IMAGE = "image"
        const val MAX_PREVIEW_LENGTH = 200
        
        // 30 days in milliseconds
        const val BIN_RETENTION_DAYS = 30
        const val BIN_RETENTION_MS = BIN_RETENTION_DAYS * 24 * 60 * 60 * 1000L
    }
}