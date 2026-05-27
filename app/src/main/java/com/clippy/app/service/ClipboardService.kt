package com.clippy.app.service

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
        onClipboardChanged()
    }
    
    companion object {
        private const val TAG = "ClipboardService"
        
        fun startService(context: Context) {
            val intent = Intent(context, ClipboardService::class.java)
            context.startForegroundService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, ClipboardService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipboardListener)
        
        // Get initial clipboard content to avoid duplicating on first change
        lastClipContent = getCurrentClipboardText()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        
        // Start as foreground service with notification
        serviceScope.launch {
            val recentClips = clipRepository.getRecentClips(5)
            val notification = notificationHelper.buildServiceNotification(recentClips)
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
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
        
        if (clipText.isNullOrBlank()) {
            Log.d(TAG, "Clipboard is empty or not text")
            return
        }
        
        // Avoid duplicates
        if (clipText == lastClipContent) {
            Log.d(TAG, "Clipboard content unchanged")
            return
        }
        
        lastClipContent = clipText
        Log.d(TAG, "New clipboard content: ${clipText.take(50)}...")
        
        serviceScope.launch {
            try {
                // Get max history size from preferences
                val maxSize = preferencesManager.maxHistorySize.first()
                
                // Save to database
                clipRepository.addClip(clipText, maxSize)
                
                // Update notification
                val showNotification = preferencesManager.showNotification.first()
                if (showNotification) {
                    val recentClips = clipRepository.getRecentClips(5)
                    notificationHelper.updateNotification(recentClips)
                }
                
                Log.d(TAG, "Clip saved successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving clip", e)
            }
        }
    }
    
    /**
     * Get the current text content from clipboard.
     */
    private fun getCurrentClipboardText(): String? {
        return try {
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0)?.text?.toString()
            } else {
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
                notificationHelper.updateNotification(recentClips)
            }
        }
    }
}