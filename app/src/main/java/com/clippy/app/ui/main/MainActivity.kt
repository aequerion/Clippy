package com.clippy.app.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.clippy.app.data.preferences.PreferencesManager
import com.clippy.app.service.ClipboardService
import com.clippy.app.ui.settings.SettingsActivity
import com.clippy.app.ui.theme.ClippyColors
import com.clippy.app.ui.theme.ClippyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main activity hosting the clipboard history screen.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private val viewModel: MainViewModel by viewModels()
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Notification permission granted: $isGranted")
        // Start service after permission result
        startClipboardServiceIfEnabled()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        } else {
            // For older Android versions, start service directly
            startClipboardServiceIfEnabled()
        }
        
        setContent {
            ClippyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ClippyColors.BackgroundDark
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = {
                            startActivity(Intent(this, SettingsActivity::class.java))
                        }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - refreshing clips and checking clipboard")
        // Refresh clips when returning to the app
        viewModel.refreshClips()
        
        // Check clipboard when app comes to foreground
        // This is important because on Android 10+, clipboard can only be read in foreground
        ClipboardService.checkClipboard(this)
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Notification permission already granted")
                    // Permission already granted, start service
                    startClipboardServiceIfEnabled()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.d(TAG, "Should show rationale for notification permission")
                    // Show rationale if needed, then request
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    Log.d(TAG, "Requesting notification permission")
                    // Request permission
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
    
    private fun startClipboardServiceIfEnabled() {
        lifecycleScope.launch {
            val serviceEnabled = preferencesManager.serviceEnabled.first()
            Log.d(TAG, "Service enabled: $serviceEnabled")
            if (serviceEnabled) {
                Log.d(TAG, "Starting ClipboardService")
                ClipboardService.startService(this@MainActivity)
            }
        }
    }
}