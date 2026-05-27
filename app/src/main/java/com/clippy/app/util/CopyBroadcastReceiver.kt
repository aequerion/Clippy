package com.clippy.app.util

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.clippy.app.R

/**
 * Broadcast receiver for handling copy actions from notifications.
 */
class CopyBroadcastReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.ACTION_COPY) {
            val content = intent.getStringExtra(NotificationHelper.EXTRA_CLIP_CONTENT)
            
            if (!content.isNullOrEmpty()) {
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("Clippy", content)
                clipboardManager.setPrimaryClip(clipData)
                
                Toast.makeText(
                    context,
                    context.getString(R.string.copied_to_clipboard),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}