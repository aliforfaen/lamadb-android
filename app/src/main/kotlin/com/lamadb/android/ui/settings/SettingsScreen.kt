package com.lamadb.android.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lamadb.android.BuildConfig
import com.lamadb.android.R
import com.lamadb.android.logging.AppLogger
import com.lamadb.android.logging.LogPreferences
import com.lamadb.android.power.BatteryOptimizationHelper
import com.lamadb.android.power.BatteryOptimizationPreferences
import com.lamadb.android.theme.ThemeMode
import com.lamadb.android.theme.ThemePreferences
import com.lamadb.android.ui.power.BatteryOptimizationCard

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    serverUrl: String,
    themeMode: ThemeMode,
    onLogout: () -> Unit,
    onResetPresence: () -> Unit,
    onViewLogs: () -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
    onDynamicColorChanged: (Boolean) -> Unit = {},
    onThemeModeChanged: (ThemeMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val batteryPreferences = remember { BatteryOptimizationPreferences(context) }
    val batteryHelper = remember { BatteryOptimizationHelper(context) }
    val logPreferences = remember { LogPreferences(context) }
    var useDynamicColor by remember {
        mutableStateOf(themePreferences.useDynamicColor && themePreferences.supportsDynamicColor)
    }
    var selectedThemeMode by remember { mutableStateOf(themeMode) }
    var loggingEnabled by remember { mutableStateOf(logPreferences.loggingEnabled) }
    var showBatteryCard by remember {
        mutableStateOf(
            !batteryHelper.isIgnoringBatteryOptimizations() &&
                !batteryPreferences.guidanceDismissed
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (showBatteryCard) {
            BatteryOptimizationCard(
                onDismiss = {
                    batteryPreferences.guidanceDismissed = true
                    showBatteryCard = false
                }
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_server_url),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = serverUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        PushSettingsCard()

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_material_you),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_material_you_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useDynamicColor,
                    enabled = themePreferences.supportsDynamicColor,
                    onCheckedChange = {
                        useDynamicColor = it
                        themePreferences.useDynamicColor = it
                        onDynamicColorChanged(it)
                    }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_dark_mode),
                    style = MaterialTheme.typography.titleMedium
                )
                ThemeModeSelector(
                    selected = selectedThemeMode,
                    onSelected = {
                        selectedThemeMode = it
                        themePreferences.themeMode = it
                        onThemeModeChanged(it)
                    }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_debug_logs),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_debug_logs_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = loggingEnabled,
                    onCheckedChange = {
                        loggingEnabled = it
                        AppLogger.updateEnabled(context, it)
                    }
                )
            }
        }

        if (loggingEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewLogs,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_view_logs))
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReplayOnboarding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_replay_onboarding))
                }

                OutlinedButton(
                    onClick = onResetPresence,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_reset_presence))
                }

                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_logout))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider()

        Text(
            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        ThemeModeOption(
            mode = ThemeMode.SYSTEM,
            labelRes = R.string.settings_dark_mode_system,
            icon = Icons.Filled.SettingsSuggest
        ),
        ThemeModeOption(
            mode = ThemeMode.LIGHT,
            labelRes = R.string.settings_dark_mode_light,
            icon = Icons.Filled.LightMode
        ),
        ThemeModeOption(
            mode = ThemeMode.DARK,
            labelRes = R.string.settings_dark_mode_dark,
            icon = Icons.Filled.DarkMode
        )
    )

    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                onClick = { onSelected(option.mode) },
                selected = selected == option.mode,
                icon = {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        modifier = Modifier.height(SegmentedButtonDefaults.IconSize)
                    )
                },
                label = { Text(stringResource(option.labelRes)) }
            )
        }
    }
}

private data class ThemeModeOption(
    val mode: ThemeMode,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
