package com.clippy.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.clippy.app.R
import com.clippy.app.data.database.ClipEntity
import com.clippy.app.ui.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for managing notifications.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_ID = "clippy_clipboard_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_COPY = "com.clippy.app.ACTION_COPY"
        const val EXTRA_CLIP_ID = "clip_id"
        const val EXTRA_CLIP_CONTENT = "clip_content"
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Create the notification channel for Android 8.0+.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Build the foreground service notification.
     * @param recentClips List of recent clips to show in expanded notification (max 5)
     * @param totalCount Total number of clips in history (for display)
     */
    fun buildServiceNotification(recentClips: List<ClipEntity> = emptyList(), totalCount: Int = recentClips.size): android.app.Notification {
        // Intent to open the app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        
        if (recentClips.isEmpty()) {
            builder.setContentText(context.getString(R.string.notification_text))
        } else {
            // Show recent clips in expanded notification
            val inboxStyle = NotificationCompat.InboxStyle()
                .setBigContentTitle(context.getString(R.string.clipboard_history))
            
            recentClips.take(5).forEach { clip ->
                val preview = clip.preview.take(50).let {
                    if (clip.preview.length > 50) "$it..." else it
                }
                inboxStyle.addLine(preview)
            }
            
            builder.setContentText("$totalCount items in history")
            builder.setStyle(inboxStyle)
            
            // Add quick copy action for the most recent clip
            recentClips.firstOrNull()?.let { clip ->
                val copyIntent = Intent(context, CopyBroadcastReceiver::class.java).apply {
                    action = ACTION_COPY
                    putExtra(EXTRA_CLIP_ID, clip.id)
                    putExtra(EXTRA_CLIP_CONTENT, clip.content)
                }
                val copyPendingIntent = PendingIntent.getBroadcast(
                    context,
                    clip.id.toInt(),
                    copyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    R.drawable.ic_copy,
                    context.getString(R.string.notification_action_copy),
                    copyPendingIntent
                )
            }
        }
        
        return builder.build()
    }
    
    /**
     * Update the notification with new clips.
     * @param recentClips List of recent clips to show in expanded notification (max 5)
     * @param totalCount Total number of clips in history (for display)
     */
    fun updateNotification(recentClips: List<ClipEntity>, totalCount: Int = recentClips.size) {
        val notification = buildServiceNotification(recentClips, totalCount)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Cancel the notification.
     */
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}