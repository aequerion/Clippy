package com.clippy.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Dark color scheme using GitGraph-inspired colors.
 */
private val DarkColorScheme = darkColorScheme(
    primary = ClippyColors.AccentGreen,
    onPrimary = ClippyColors.TextPrimary,
    primaryContainer = ClippyColors.AccentGreenDark,
    onPrimaryContainer = ClippyColors.TextPrimary,
    
    secondary = ClippyColors.LinkBlue,
    onSecondary = ClippyColors.TextPrimary,
    secondaryContainer = ClippyColors.SurfaceElevated,
    onSecondaryContainer = ClippyColors.TextPrimary,
    
    tertiary = ClippyColors.PinGold,
    onTertiary = ClippyColors.BackgroundDark,
    
    background = ClippyColors.BackgroundDark,
    onBackground = ClippyColors.TextPrimary,
    
    surface = ClippyColors.CardBackground,
    onSurface = ClippyColors.TextPrimary,
    surfaceVariant = ClippyColors.SurfaceElevated,
    onSurfaceVariant = ClippyColors.TextSecondary,
    
    error = ClippyColors.ErrorRed,
    onError = ClippyColors.TextPrimary,
    errorContainer = ClippyColors.ErrorBackground,
    onErrorContainer = ClippyColors.ErrorRed,
    
    outline = ClippyColors.Border,
    outlineVariant = ClippyColors.BorderLight,
    
    inverseSurface = ClippyColors.TextPrimary,
    inverseOnSurface = ClippyColors.BackgroundDark,
    inversePrimary = ClippyColors.AccentGreenDark,
    
    scrim = ClippyColors.BackgroundDark.copy(alpha = 0.5f)
)

/**
 * Clippy app theme.
 * Currently only supports dark mode to match GitGraph aesthetic.
 */
@Composable
fun ClippyTheme(
    darkTheme: Boolean = true, // Always dark for now
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ClippyColors.BackgroundDark.toArgb()
            window.navigationBarColor = ClippyColors.BackgroundDark.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}