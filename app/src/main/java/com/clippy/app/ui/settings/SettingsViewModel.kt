package com.clippy.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clippy.app.data.preferences.PreferencesManager
import com.clippy.app.data.repository.ClipRepository
import com.clippy.app.service.ClipboardService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for settings screen.
 */
data class SettingsUiState(
    val serviceEnabled: Boolean = true,
    val showNotification: Boolean = true,
    val maxHistorySize: Int = 100,
    val clipCount: Int = 0
)

/**
 * One-time events for settings UI.
 */
sealed class SettingsUiEvent {
    data class ShowSnackbar(val message: String) : SettingsUiEvent()
}

/**
 * ViewModel for the settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val clipRepository: ClipRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    /**
     * Combined UI state from preferences.
     */
    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesManager.serviceEnabled,
        preferencesManager.showNotification,
        preferencesManager.maxHistorySize
    ) { serviceEnabled, showNotification, maxHistorySize ->
        SettingsUiState(
            serviceEnabled = serviceEnabled,
            showNotification = showNotification,
            maxHistorySize = maxHistorySize
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )
    
    private val _events = MutableSharedFlow<SettingsUiEvent>()
    val events = _events.asSharedFlow()
    
    /**
     * Toggle clipboard monitoring service.
     */
    fun setServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setServiceEnabled(enabled)
            if (enabled) {
                ClipboardService.startService(context)
                _events.emit(SettingsUiEvent.ShowSnackbar("Clipboard monitoring enabled"))
            } else {
                ClipboardService.stopService(context)
                _events.emit(SettingsUiEvent.ShowSnackbar("Clipboard monitoring disabled"))
            }
        }
    }
    
    /**
     * Toggle notification visibility.
     */
    fun setShowNotification(show: Boolean) {
        viewModelScope.launch {
            preferencesManager.setShowNotification(show)
            _events.emit(SettingsUiEvent.ShowSnackbar(
                if (show) "Notification enabled" else "Notification disabled"
            ))
        }
    }
    
    /**
     * Update maximum history size.
     */
    fun setMaxHistorySize(size: Int) {
        viewModelScope.launch {
            preferencesManager.setMaxHistorySize(size)
            // Enforce the new limit
            clipRepository.enforceHistoryLimit(size)
            _events.emit(SettingsUiEvent.ShowSnackbar("History size set to $size"))
        }
    }
}