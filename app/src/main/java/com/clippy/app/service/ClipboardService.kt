package com.clippy.app.service

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.clippy.app.data.preferences.PreferencesManager
import com.clippy.app.data.repository.ClipRepository
import com.clippy.app.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that monitors the clipboard for changes.
 * 
 * Note: On Android 10+, clipboard access is restricted. Apps can only read
 * clipboard content when they are in the foreground. The listener will still
 * fire, but we may not be able to read the content unless the app has focus.
 */
@AndroidEntryPoint
class ClipboardService : Service() {
    
    @Inject
    lateinit var clipRepository: ClipRepository
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    @Inject
    lateinit var notificationHelper: NotificationHelper
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var clipboardManager: ClipboardManager? = null
    private var lastClipContent: String? = null
    
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        Log.d(TAG, "Clipboard changed event received")
        onClipboardChanged()
    }
    
    companion object {
        private const val TAG = "ClipboardService"
        const val ACTION_CHECK_CLIPBOARD = "com.clippy.app.CHECK_CLIPBOARD"
        
        fun startService(context: Context) {
            Log.d(TAG, "startService called")
            val intent = Intent(context, ClipboardService::class.java)
            context.startForegroundService(intent)
        }
        
        fun stopService(context: Context) {
            Log.d(TAG, "stopService called")
            val intent = Intent(context, ClipboardService::class.java)
            context.stopService(intent)
        }
        
        /**
         * Request the service to check clipboard (useful when app comes to foreground)
         */
        fun checkClipboard(context: Context) {
            Log.d(TAG, "checkClipboard called")
            val intent = Intent(context, ClipboardService::class.java)
            intent.action = ACTION_CHECK_CLIPBOARD
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipboardListener)
        
        // Get initial clipboard content to avoid duplicating on first change
        lastClipContent = getCurrentClipboardText()
        Log.d(TAG, "Initial clipboard content: ${lastClipContent?.take(30) ?: "null"}")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand, action: ${intent?.action}")
        
        // Handle check clipboard action (when app comes to foreground)
        if (intent?.action == ACTION_CHECK_CLIPBOARD) {
            Log.d(TAG, "Checking clipboard from foreground")
            onClipboardChanged()
            return START_STICKY
        }
        
        // Start as foreground service with notification
        serviceScope.launch {
            try {
                val recentClips = clipRepository.getRecentClips(5)
                val totalCount = clipRepository.getClipCount()
                Log.d(TAG, "Building notification with ${recentClips.size} recent clips, total: $totalCount")
                val notification = notificationHelper.buildServiceNotification(recentClips, totalCount)
                startForeground(NotificationHelper.NOTIFICATION_ID, notification)
                Log.d(TAG, "Foreground service started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting foreground service", e)
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        
        clipboardManager?.removePrimaryClipChangedListener(clipboardListener)
        serviceScope.cancel()
    }
    
    /**
     * Called when clipboard content changes.
     */
    private fun onClipboardChanged() {
        val clipText = getCurrentClipboardText()
        
        Log.d(TAG, "onClipboardChanged - clipText: ${clipText?.take(30) ?: "null"}")
        
        if (clipText.isNullOrBlank()) {
            Log.d(TAG, "Clipboard is empty or not text (or access denied)")
            return
        }
        
        // Avoid duplicates
        if (clipText == lastClipContent) {
            Log.d(TAG, "Clipboard content unchanged, skipping")
            return
        }
        
        lastClipContent = clipText
        Log.d(TAG, "New clipboard content detected: ${clipText.take(50)}...")
        
        serviceScope.launch {
            try {
                // Get max history size from preferences
                val maxSize = preferencesManager.maxHistorySize.first()
                
                // Save to database
                clipRepository.addClip(clipText, maxSize)
                Log.d(TAG, "Clip saved to database successfully")
                
                // Update notification
                val showNotification = preferencesManager.showNotification.first()
                if (showNotification) {
                    val recentClips = clipRepository.getRecentClips(5)
                    val totalCount = clipRepository.getClipCount()
                    notificationHelper.updateNotification(recentClips, totalCount)
                    Log.d(TAG, "Notification updated with total count: $totalCount")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving clip", e)
            }
        }
    }
    
    /**
     * Get the current text content from clipboard.
     * Note: On Android 10+, this may return null if the app is not in foreground.
     */
    private fun getCurrentClipboardText(): String? {
        return try {
            val clip = clipboardManager?.primaryClip
            Log.d(TAG, "primaryClip: $clip, itemCount: ${clip?.itemCount ?: 0}")
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0)?.text?.toString()
                Log.d(TAG, "Clipboard text retrieved: ${text?.take(30) ?: "null"}")
                text
            } else {
                Log.d(TAG, "No clip data available")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading clipboard", e)
            null
        }
    }
    
    /**
     * Update the notification with current clips.
     */
    fun updateNotification() {
        serviceScope.launch {
            val showNotification = preferencesManager.showNotification.first()
            if (showNotification) {
                val recentClips = clipRepository.getRecentClips(5)
                val totalCount = clipRepository.getClipCount()
                notificationHelper.updateNotification(recentClips, totalCount)
            }
        }
    }
}