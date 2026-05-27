package com.clippy.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clippy.app.BuildConfig
import com.clippy.app.R
import com.clippy.app.ui.theme.ClippyColors
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

/**
 * Settings screen composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SettingsUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ClippyColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ClippyColors.BackgroundDark,
                    titleContentColor = ClippyColors.TextPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ClippyColors.BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Service Settings Section
            SettingsSection(title = "Service") {
                SwitchSettingItem(
                    icon = Icons.Default.PlayArrow,
                    title = stringResource(R.string.service_enabled),
                    subtitle = stringResource(R.string.service_enabled_summary),
                    checked = uiState.serviceEnabled,
                    onCheckedChange = { viewModel.setServiceEnabled(it) }
                )
                
                SwitchSettingItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.show_notification),
                    subtitle = stringResource(R.string.show_notification_summary),
                    checked = uiState.showNotification,
                    onCheckedChange = { viewModel.setShowNotification(it) },
                    enabled = uiState.serviceEnabled
                )
            }
            
            // History Settings Section
            SettingsSection(title = "History") {
                SliderSettingItem(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.max_history_size),
                    subtitle = stringResource(R.string.max_history_size_summary),
                    value = uiState.maxHistorySize,
                    onValueChange = { viewModel.setMaxHistorySize(it) },
                    valueRange = 10f..500f,
                    steps = 9 // 10, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500
                )
            }
            
            // About Section
            SettingsSection(title = stringResource(R.string.about)) {
                InfoSettingItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.version),
                    value = BuildConfig.VERSION_NAME
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Settings section with title.
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = ClippyColors.AccentGreen,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ClippyColors.CardBackground
            ),
            border = BorderStroke(1.dp, ClippyColors.Border),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(4.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * Switch setting item.
 */
@Composable
private fun SwitchSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) ClippyColors.TextSecondary else ClippyColors.TextTertiary,
            modifier = Modifier.padding(end = 16.dp)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) ClippyColors.TextPrimary else ClippyColors.TextTertiary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ClippyColors.TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ClippyColors.TextPrimary,
                checkedTrackColor = ClippyColors.AccentGreen,
                uncheckedThumbColor = ClippyColors.TextSecondary,
                uncheckedTrackColor = ClippyColors.SurfaceElevated,
                uncheckedBorderColor = ClippyColors.Border
            )
        )
    }
}

/**
 * Slider setting item.
 */
@Composable
private fun SliderSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ClippyColors.TextSecondary,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ClippyColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ClippyColors.TextSecondary
                )
            }
            Text(
                text = "${sliderValue.roundToInt()}",
                style = MaterialTheme.typography.titleMedium,
                color = ClippyColors.AccentGreen
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChange(sliderValue.roundToInt()) },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.padding(top = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = ClippyColors.AccentGreen,
                activeTrackColor = ClippyColors.AccentGreen,
                inactiveTrackColor = ClippyColors.SurfaceElevated
            )
        )
    }
}

/**
 * Info setting item (read-only).
 */
@Composable
private fun InfoSettingItem(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ClippyColors.TextSecondary,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = ClippyColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = ClippyColors.TextSecondary
        )
    }
}