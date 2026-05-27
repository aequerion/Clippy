package com.clippy.app.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.clippy.app.ui.theme.ClippyColors
import com.clippy.app.ui.theme.ClippyTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Settings activity hosting the settings screen.
 */
@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    
    private val viewModel: SettingsViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ClippyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ClippyColors.BackgroundDark
                ) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }
}