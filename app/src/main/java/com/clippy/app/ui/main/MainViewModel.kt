package com.clippy.app.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clippy.app.data.database.ClipEntity
import com.clippy.app.data.preferences.PreferencesManager
import com.clippy.app.data.repository.ClipRepository
import com.clippy.app.service.ClipboardService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the main screen.
 */
data class MainUiState(
    val clips: List<ClipEntity> = emptyList(),
    val isLoading: Boolean = true,
    val serviceEnabled: Boolean = true,
    val showNotification: Boolean = true,
    val maxHistorySize: Int = 100
)

/**
 * One-time events for the UI.
 */
sealed class MainUiEvent {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : MainUiEvent()
    data class ClipCopied(val clip: ClipEntity) : MainUiEvent()
    data class ClipDeleted(val clip: ClipEntity) : MainUiEvent()
    object NavigateToSettings : MainUiEvent()
}

/**
 * ViewModel for the main clipboard history screen.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val clipRepository: ClipRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(true)
    
    /**
     * Combined UI state from multiple sources.
     */
    val uiState: StateFlow<MainUiState> = combine(
        clipRepository.getAllClips(),
        preferencesManager.serviceEnabled,
        preferencesManager.showNotification,
        preferencesManager.maxHistorySize,
        _isLoading
    ) { clips, serviceEnabled, showNotification, maxHistorySize, isLoading ->
        MainUiState(
            clips = clips,
            isLoading = isLoading && clips.isEmpty(),
            serviceEnabled = serviceEnabled,
            showNotification = showNotification,
            maxHistorySize = maxHistorySize
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )
    
    private val _events = MutableSharedFlow<MainUiEvent>()
    val events = _events.asSharedFlow()
    
    // For undo functionality
    private var lastDeletedClip: ClipEntity? = null
    
    init {
        viewModelScope.launch {
            // Mark loading as complete after initial data load
            clipRepository.getAllClips().collect {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Copy a clip's content to the system clipboard.
     */
    fun copyToClipboard(clip: ClipEntity) {
        viewModelScope.launch {
            try {
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("Clippy", clip.content)
                clipboardManager.setPrimaryClip(clipData)
                
                _events.emit(MainUiEvent.ClipCopied(clip))
                _events.emit(MainUiEvent.ShowSnackbar("Copied to clipboard"))
            } catch (e: Exception) {
                _events.emit(MainUiEvent.ShowSnackbar("Failed to copy"))
            }
        }
    }
    
    /**
     * Toggle pin status for a clip.
     */
    fun togglePin(clip: ClipEntity) {
        viewModelScope.launch {
            clipRepository.togglePin(clip.id)
            val message = if (clip.isPinned) "Unpinned" else "Pinned"
            _events.emit(MainUiEvent.ShowSnackbar(message))
        }
    }
    
    /**
     * Delete a clip with undo support.
     */
    fun deleteClip(clip: ClipEntity) {
        viewModelScope.launch {
            lastDeletedClip = clip
            clipRepository.deleteClip(clip)
            _events.emit(MainUiEvent.ClipDeleted(clip))
            _events.emit(MainUiEvent.ShowSnackbar("Deleted", "Undo"))
        }
    }
    
    /**
     * Restore the last deleted clip.
     */
    fun undoDelete() {
        viewModelScope.launch {
            lastDeletedClip?.let { clip ->
                clipRepository.restoreClip(clip)
                lastDeletedClip = null
                _events.emit(MainUiEvent.ShowSnackbar("Restored"))
            }
        }
    }
    
    /**
     * Clear all unpinned clips.
     */
    fun clearAllUnpinned() {
        viewModelScope.launch {
            clipRepository.clearUnpinned()
            _events.emit(MainUiEvent.ShowSnackbar("Cleared all unpinned items"))
        }
    }
    
    /**
     * Toggle the clipboard monitoring service.
     */
    fun toggleService(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setServiceEnabled(enabled)
            if (enabled) {
                ClipboardService.startService(context)
                _events.emit(MainUiEvent.ShowSnackbar("Clipboard monitoring enabled"))
            } else {
                ClipboardService.stopService(context)
                _events.emit(MainUiEvent.ShowSnackbar("Clipboard monitoring disabled"))
            }
        }
    }
    
    /**
     * Start the clipboard service if enabled.
     */
    fun startServiceIfEnabled() {
        viewModelScope.launch {
            preferencesManager.serviceEnabled.collect { enabled ->
                if (enabled) {
                    ClipboardService.startService(context)
                }
            }
        }
    }
    
    /**
     * Navigate to settings.
     */
    fun navigateToSettings() {
        viewModelScope.launch {
            _events.emit(MainUiEvent.NavigateToSettings)
        }
    }
}