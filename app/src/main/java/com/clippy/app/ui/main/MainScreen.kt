package com.clippy.app.ui.main

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clippy.app.data.database.ClipEntity
import com.clippy.app.ui.theme.ClippyColors
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main screen displaying clipboard history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is MainUiEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = if (event.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed && event.actionLabel == "Undo") {
                        viewModel.undoDelete()
                    }
                }
                is MainUiEvent.NavigateToSettings -> onNavigateToSettings()
                else -> {}
            }
        }
    }
    
    // Clear All Confirmation Dialog
    if (uiState.showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideClearAllDialog() },
            title = {
                Text(
                    "Clear All Unpinned Items?",
                    color = ClippyColors.TextPrimary
                )
            },
            text = {
                Text(
                    "This will move all unpinned items to the bin. You can restore them within 30 days.",
                    color = ClippyColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmClearAllUnpinned() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ClippyColors.ErrorRed
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.hideClearAllDialog() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ClippyColors.TextSecondary
                    )
                ) {
                    Text("Cancel")
                }
            },
            containerColor = ClippyColors.CardBackground
        )
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.viewMode == ViewMode.CLIPS) "Clippy" else "Bin",
                        fontWeight = FontWeight.Bold,
                        color = ClippyColors.TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ClippyColors.BackgroundDark
                ),
                navigationIcon = {
                    if (uiState.viewMode == ViewMode.BIN) {
                        IconButton(onClick = { viewModel.showClips() }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = ClippyColors.TextPrimary
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.viewMode == ViewMode.CLIPS) {
                        // Capture clipboard button
                        IconButton(onClick = {
                            captureClipboard(context, viewModel)
                        }) {
                            Icon(
                                Icons.Default.ContentPaste,
                                contentDescription = "Capture Clipboard",
                                tint = ClippyColors.AccentGreen
                            )
                        }
                        // Bin button
                        IconButton(onClick = { viewModel.showBin() }) {
                            BadgedBox(
                                badge = {
                                    if (uiState.binClips.isNotEmpty()) {
                                        Badge(
                                            containerColor = ClippyColors.ErrorRed
                                        ) {
                                            Text(
                                                uiState.binClips.size.toString(),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Bin",
                                    tint = ClippyColors.TextSecondary
                                )
                            }
                        }
                        // Clear all button
                        IconButton(onClick = { viewModel.showClearAllDialog() }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear All",
                                tint = ClippyColors.TextSecondary
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = ClippyColors.TextSecondary
                            )
                        }
                    } else {
                        // Bin view actions
                        if (uiState.binClips.isNotEmpty()) {
                            IconButton(onClick = { viewModel.restoreAllFromBin() }) {
                                Icon(
                                    Icons.Default.RestoreFromTrash,
                                    contentDescription = "Restore All",
                                    tint = ClippyColors.AccentGreen
                                )
                            }
                            IconButton(onClick = { viewModel.emptyBin() }) {
                                Icon(
                                    Icons.Default.DeleteForever,
                                    contentDescription = "Empty Bin",
                                    tint = ClippyColors.ErrorRed
                                )
                            }
                        }
                    }
                }
            )
        },
        containerColor = ClippyColors.BackgroundDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = ClippyColors.AccentGreen
                    )
                }
                uiState.viewMode == ViewMode.CLIPS -> {
                    if (uiState.clips.isEmpty()) {
                        EmptyState(
                            onCaptureClipboard = { captureClipboard(context, viewModel) }
                        )
                    } else {
                        ClipList(
                            clips = uiState.clips,
                            onCopyClip = { viewModel.copyToClipboard(it) },
                            onTogglePin = { viewModel.togglePin(it) },
                            onDeleteClip = { viewModel.deleteClip(it) }
                        )
                    }
                }
                uiState.viewMode == ViewMode.BIN -> {
                    if (uiState.binClips.isEmpty()) {
                        EmptyBinState()
                    } else {
                        BinList(
                            clips = uiState.binClips,
                            onRestore = { viewModel.restoreFromBin(it) },
                            onPermanentDelete = { viewModel.permanentlyDelete(it) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Capture current clipboard content manually
 */
private fun captureClipboard(context: Context, viewModel: MainViewModel) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = clipboardManager.primaryClip
    if (clip != null && clip.itemCount > 0) {
        val text = clip.getItemAt(0)?.text?.toString()
        if (!text.isNullOrBlank()) {
            viewModel.addClipFromClipboard(text)
        }
    }
}

/**
 * Empty state when no clips are available.
 */
@Composable
private fun EmptyState(
    onCaptureClipboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ContentPaste,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = ClippyColors.TextSecondary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No clips yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = ClippyColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Copy some text and tap the paste button\nto capture it, or copy while this app is open",
            fontSize = 14.sp,
            color = ClippyColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCaptureClipboard,
            colors = ButtonDefaults.buttonColors(
                containerColor = ClippyColors.AccentGreen
            )
        ) {
            Icon(
                Icons.Default.ContentPaste,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Capture Clipboard")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Note: On Android 10+, clipboard can only be\nread when this app is in the foreground",
            fontSize = 12.sp,
            color = ClippyColors.TextSecondary.copy(alpha = 0.7f),
            lineHeight = 16.sp
        )
    }
}

/**
 * Empty state for bin.
 */
@Composable
private fun EmptyBinState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.DeleteOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = ClippyColors.TextSecondary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Bin is empty",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = ClippyColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Deleted items will appear here.\nItems are automatically removed after 30 days.",
            fontSize = 14.sp,
            color = ClippyColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 20.sp
        )
    }
}

/**
 * List of clipboard items.
 */
@Composable
private fun ClipList(
    clips: List<ClipEntity>,
    onCopyClip: (ClipEntity) -> Unit,
    onTogglePin: (ClipEntity) -> Unit,
    onDeleteClip: (ClipEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = clips,
            key = { it.id }
        ) { clip ->
            ClipItem(
                clip = clip,
                onCopy = { onCopyClip(clip) },
                onTogglePin = { onTogglePin(clip) },
                onDelete = { onDeleteClip(clip) }
            )
        }
    }
}

/**
 * List of bin items.
 */
@Composable
private fun BinList(
    clips: List<ClipEntity>,
    onRestore: (ClipEntity) -> Unit,
    onPermanentDelete: (ClipEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = clips,
            key = { it.id }
        ) { clip ->
            BinItem(
                clip = clip,
                onRestore = { onRestore(clip) },
                onPermanentDelete = { onPermanentDelete(clip) }
            )
        }
    }
}

/**
 * Individual clip item card.
 */
@Composable
private fun ClipItem(
    clip: ClipEntity,
    onCopy: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = ClippyColors.CardBackground
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (clip.isPinned) ClippyColors.AccentGreen.copy(alpha = 0.5f)
            else ClippyColors.Border
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header with timestamp and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (clip.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            modifier = Modifier.size(14.dp),
                            tint = ClippyColors.AccentGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        formatTimestamp(clip.timestamp),
                        fontSize = 12.sp,
                        color = ClippyColors.TextSecondary
                    )
                }
                
                Row {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (clip.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (clip.isPinned) "Unpin" else "Pin",
                            modifier = Modifier.size(18.dp),
                            tint = if (clip.isPinned) ClippyColors.AccentGreen else ClippyColors.TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp),
                            tint = ClippyColors.ErrorRed
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Content
            Text(
                clip.content,
                fontSize = 14.sp,
                color = ClippyColors.TextPrimary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
            
            // Character count
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${clip.content.length} characters",
                fontSize = 11.sp,
                color = ClippyColors.TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Individual bin item card.
 */
@Composable
private fun BinItem(
    clip: ClipEntity,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = ClippyColors.CardBackground.copy(alpha = 0.7f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            ClippyColors.Border.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header with deletion time and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = ClippyColors.ErrorRed.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Deleted ${formatTimestamp(clip.deletedAt ?: clip.timestamp)}",
                        fontSize = 12.sp,
                        color = ClippyColors.TextSecondary
                    )
                }
                
                Row {
                    IconButton(
                        onClick = onRestore,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.RestoreFromTrash,
                            contentDescription = "Restore",
                            modifier = Modifier.size(18.dp),
                            tint = ClippyColors.AccentGreen
                        )
                    }
                    IconButton(
                        onClick = onPermanentDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = "Delete Permanently",
                            modifier = Modifier.size(18.dp),
                            tint = ClippyColors.ErrorRed
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Content
            Text(
                clip.content,
                fontSize = 14.sp,
                color = ClippyColors.TextPrimary.copy(alpha = 0.7f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
            
            // Days remaining
            clip.deletedAt?.let { deletedAt ->
                val daysRemaining = calculateDaysRemaining(deletedAt)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (daysRemaining > 0) "$daysRemaining days until permanent deletion"
                    else "Will be deleted soon",
                    fontSize = 11.sp,
                    color = if (daysRemaining <= 7) ClippyColors.ErrorRed.copy(alpha = 0.7f)
                    else ClippyColors.TextSecondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Calculate days remaining before permanent deletion.
 */
private fun calculateDaysRemaining(deletedAt: Long): Int {
    val now = System.currentTimeMillis()
    val expirationTime = deletedAt + ClipEntity.BIN_RETENTION_MS
    val remainingMs = expirationTime - now
    return (remainingMs / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
}

/**
 * Format timestamp to human-readable string.
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}