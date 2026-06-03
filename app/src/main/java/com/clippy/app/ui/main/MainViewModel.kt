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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Enum for current view mode.
 */
enum class ViewMode {
    CLIPS,
    BIN
}

/**
 * UI State for the main screen.
 */
data class MainUiState(
    val clips: List<ClipEntity> = emptyList(),
    val binClips: List<ClipEntity> = emptyList(),
    val isLoading: Boolean = true,
    val serviceEnabled: Boolean = true,
    val showNotification: Boolean = true,
    val maxHistorySize: Int = 100,
    val viewMode: ViewMode = ViewMode.CLIPS,
    val showClearAllDialog: Boolean = false
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
    private val _viewMode = MutableStateFlow(ViewMode.CLIPS)
    private val _showClearAllDialog = MutableStateFlow(false)
    
    // For undo functionality
    private var lastDeletedClipId: Long? = null
    private var lastDeletedClipIds: List<Long> = emptyList()
    
    /**
     * Combined UI state from multiple sources.
     */
    val uiState: StateFlow<MainUiState> = combine(
        clipRepository.getAllClips(),
        clipRepository.getBinClips(),
        preferencesManager.serviceEnabled,
        preferencesManager.showNotification,
        preferencesManager.maxHistorySize,
        _isLoading,
        _viewMode,
        _showClearAllDialog
    ) { values ->
        val clips = values[0] as List<ClipEntity>
        val binClips = values[1] as List<ClipEntity>
        val serviceEnabled = values[2] as Boolean
        val showNotification = values[3] as Boolean
        val maxHistorySize = values[4] as Int
        val isLoading = values[5] as Boolean
        val viewMode = values[6] as ViewMode
        val showClearAllDialog = values[7] as Boolean
        
        MainUiState(
            clips = clips,
            binClips = binClips,
            isLoading = isLoading && clips.isEmpty(),
            serviceEnabled = serviceEnabled,
            showNotification = showNotification,
            maxHistorySize = maxHistorySize,
            viewMode = viewMode,
            showClearAllDialog = showClearAllDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )
    
    private val _events = MutableSharedFlow<MainUiEvent>()
    val events = _events.asSharedFlow()
    
    init {
        viewModelScope.launch {
            // Mark loading as complete after initial data load
            clipRepository.getAllClips().collect {
                _isLoading.value = false
            }
        }
        
        // Clean up old bin items on startup
        viewModelScope.launch {
            clipRepository.cleanupOldBinItems()
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
     * Delete a clip (move to bin) with undo support.
     */
    fun deleteClip(clip: ClipEntity) {
        viewModelScope.launch {
            lastDeletedClipId = clip.id
            lastDeletedClipIds = emptyList() // Clear bulk delete tracking
            clipRepository.deleteClip(clip)
            _events.emit(MainUiEvent.ClipDeleted(clip))
            _events.emit(MainUiEvent.ShowSnackbar("Moved to bin", "Undo"))
        }
    }
    
    /**
     * Undo the last delete operation.
     */
    fun undoDelete() {
        viewModelScope.launch {
            // Handle single item undo
            lastDeletedClipId?.let { id ->
                clipRepository.restoreFromBin(id)
                lastDeletedClipId = null
                _events.emit(MainUiEvent.ShowSnackbar("Restored"))
            }
            
            // Handle bulk undo (clear all)
            if (lastDeletedClipIds.isNotEmpty()) {
                lastDeletedClipIds.forEach { id ->
                    clipRepository.restoreFromBin(id)
                }
                val count = lastDeletedClipIds.size
                lastDeletedClipIds = emptyList()
                _events.emit(MainUiEvent.ShowSnackbar("Restored $count items"))
            }
        }
    }
    
    /**
     * Show confirmation dialog for clearing all unpinned items.
     */
    fun showClearAllDialog() {
        _showClearAllDialog.value = true
    }
    
    /**
     * Hide confirmation dialog.
     */
    fun hideClearAllDialog() {
        _showClearAllDialog.value = false
    }
    
    /**
     * Clear all unpinned clips (with confirmation) with undo support.
     */
    fun confirmClearAllUnpinned() {
        viewModelScope.launch {
            // Get the IDs of unpinned clips before deleting for undo
            val unpinnedClips = uiState.value.clips.filter { !it.isPinned }
            lastDeletedClipIds = unpinnedClips.map { it.id }
            lastDeletedClipId = null // Clear single delete tracking
            
            clipRepository.clearUnpinned()
            _showClearAllDialog.value = false
            _events.emit(MainUiEvent.ShowSnackbar("Moved ${unpinnedClips.size} items to bin", "Undo"))
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
    
    /**
     * Refresh clips - triggers a re-emission of the flow.
     * This is useful when returning to the app to ensure latest data is shown.
     */
    fun refreshClips() {
        viewModelScope.launch {
            _isLoading.value = true
            // The flow will automatically emit new values
            // Just trigger a brief loading state
            kotlinx.coroutines.delay(100)
            _isLoading.value = false
            
            // Also clean up old bin items
            clipRepository.cleanupOldBinItems()
        }
    }
    
    /**
     * Add a clip from clipboard content (manual capture).
     */
    fun addClipFromClipboard(content: String) {
        viewModelScope.launch {
            try {
                val maxSize = preferencesManager.maxHistorySize.first()
                clipRepository.addClip(content, maxSize)
                _events.emit(MainUiEvent.ShowSnackbar("Clipboard captured"))
            } catch (e: Exception) {
                _events.emit(MainUiEvent.ShowSnackbar("Failed to capture clipboard"))
            }
        }
    }
    
    // ==================== BIN RELATED METHODS ====================
    
    /**
     * Switch to bin view.
     */
    fun showBin() {
        _viewMode.value = ViewMode.BIN
    }
    
    /**
     * Switch to clips view.
     */
    fun showClips() {
        _viewMode.value = ViewMode.CLIPS
    }
    
    /**
     * Restore a clip from bin.
     */
    fun restoreFromBin(clip: ClipEntity) {
        viewModelScope.launch {
            clipRepository.restoreFromBin(clip.id)
            _events.emit(MainUiEvent.ShowSnackbar("Restored"))
        }
    }
    
    /**
     * Permanently delete a clip from bin.
     */
    fun permanentlyDelete(clip: ClipEntity) {
        viewModelScope.launch {
            clipRepository.permanentlyDelete(clip.id)
            _events.emit(MainUiEvent.ShowSnackbar("Permanently deleted"))
        }
    }
    
    /**
     * Empty the entire bin.
     */
    fun emptyBin() {
        viewModelScope.launch {
            clipRepository.emptyBin()
            _events.emit(MainUiEvent.ShowSnackbar("Bin emptied"))
        }
    }
    
    /**
     * Restore all clips from bin.
     */
    fun restoreAllFromBin() {
        viewModelScope.launch {
            clipRepository.restoreAllFromBin()
            _events.emit(MainUiEvent.ShowSnackbar("All items restored"))
        }
    }
}