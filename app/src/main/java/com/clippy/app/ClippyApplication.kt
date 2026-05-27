package com.clippy.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Clippy.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class ClippyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Application-level initialization can be done here
    }
}