package com.clippy.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.clippy.app.data.preferences.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Broadcast receiver to restart the clipboard service after device boot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.d(TAG, "Boot completed, checking if service should start")
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val serviceEnabled = preferencesManager.serviceEnabled.first()
                    if (serviceEnabled) {
                        Log.d(TAG, "Starting clipboard service after boot")
                        ClipboardService.startService(context)
                    } else {
                        Log.d(TAG, "Service is disabled, not starting")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking service preference", e)
                    // Default to starting the service
                    ClipboardService.startService(context)
                }
            }
        }
    }
}