package com.lamadb.android.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lamadb.android.BuildConfig
import com.lamadb.android.R
import com.lamadb.android.data.push.NtfyPushWorker
import com.lamadb.android.data.wiki.WikiSyncWorker
import com.lamadb.android.debug.TestDataSeeder
import com.lamadb.android.logging.AppLogger
import com.lamadb.android.logging.LogPreferences
import com.lamadb.android.onboarding.OnboardingPreferences
import com.lamadb.android.power.BatteryOptimizationHelper
import com.lamadb.android.power.BatteryOptimizationPreferences
import com.lamadb.android.theme.ThemeMode
import com.lamadb.android.theme.SecurityPreferences
import com.lamadb.android.theme.ThemePreferences
import com.lamadb.android.ui.power.BatteryOptimizationCard
import com.lamadb.android.widget.EventWidgetRefreshWorker
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import androidx.biometric.BiometricManager

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    serverUrl: String,
    themeMode: ThemeMode,
    onLogout: () -> Unit,
    onResetPresence: () -> Unit,
    onViewLogs: () -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
    onResetToFirstLaunch: () -> Unit = {},
    onDynamicColorChanged: (Boolean) -> Unit = {},
    onThemeModeChanged: (ThemeMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themePreferences = remember { ThemePreferences(context) }
    val batteryPreferences = remember { BatteryOptimizationPreferences(context) }
    val batteryHelper = remember { BatteryOptimizationHelper(context) }
    val logPreferences = remember { LogPreferences(context) }
    var useDynamicColor by remember {
        mutableStateOf(themePreferences.useDynamicColor && themePreferences.supportsDynamicColor)
    }
    var selectedThemeMode by remember { mutableStateOf(themeMode) }
    var loggingEnabled by remember { mutableStateOf(logPreferences.loggingEnabled) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showBatteryCard by remember {
        mutableStateOf(
            !batteryHelper.isIgnoringBatteryOptimizations() &&
                !batteryPreferences.guidanceDismissed
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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
                    modifier = Modifier.testTag("settings_material_you_toggle"),
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

        val securityPrefs = remember { SecurityPreferences(context) }
        val biometricManager = remember { BiometricManager.from(context) }
        val canAuthenticate = remember {
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        }
        val biometricAvailable = canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
        var biometricEnabled by remember { mutableStateOf(securityPrefs.biometricEnabled) }

        LaunchedEffect(biometricAvailable) {
            if (!biometricAvailable && biometricEnabled) {
                biometricEnabled = false
                securityPrefs.biometricEnabled = false
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
                        text = "Biometric lock",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (biometricAvailable) {
                            "Require fingerprint or face to open the app"
                        } else {
                            "Not available on this device"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    modifier = Modifier.testTag("settings_biometric_toggle"),
                    checked = biometricEnabled,
                    enabled = biometricAvailable,
                    onCheckedChange = {
                        biometricEnabled = it
                        securityPrefs.biometricEnabled = it
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
                    modifier = Modifier.testTag("settings_dark_mode_selector"),
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
                        modifier = Modifier.fillMaxWidth().testTag("settings_view_logs_button")
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
                    modifier = Modifier.fillMaxWidth().testTag("settings_replay_onboarding_button")
                ) {
                    Text(stringResource(R.string.settings_replay_onboarding))
                }

                OutlinedButton(
                    onClick = onResetPresence,
                    modifier = Modifier.fillMaxWidth().testTag("settings_reset_presence_button")
                ) {
                    Text(stringResource(R.string.settings_reset_presence))
                }

                Button(
                    onClick = { showLogoutConfirm = true },
                    modifier = Modifier.fillMaxWidth().testTag("settings_logout_button")
                ) {
                    Text(stringResource(R.string.settings_logout))
                }
            }
        }


        if (showLogoutConfirm) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirm = false },
                title = { Text(stringResource(R.string.settings_logout_confirm_title)) },
                text = { Text(stringResource(R.string.settings_logout_confirm_body)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutConfirm = false
                            onLogout()
                        }
                    ) {
                        Text(stringResource(R.string.settings_logout_confirm_positive))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirm = false }) {
                        Text(stringResource(R.string.settings_logout_confirm_negative))
                    }
                }
            )
        }
        if (BuildConfig.DEBUG) {
            DebugSettingsCard(
                onSkipOnboarding = {
                    OnboardingPreferences(context).onboardingCompleted = true
                },
                onSeedData = {
                    scope.launch { TestDataSeeder.seedAll(context) }
                },
                onResetToFirstLaunch = onResetToFirstLaunch,
                onCrashApp = { throw RuntimeException("Debug crash triggered from settings") },
                onTriggerWorkers = { triggerDebugWorkers(context) }
            )
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

@Composable
private fun DebugSettingsCard(
    onSkipOnboarding: () -> Unit,
    onSeedData: () -> Unit,
    onResetToFirstLaunch: () -> Unit,
    onCrashApp: () -> Unit,
    onTriggerWorkers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.debug_tools_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )

            OutlinedButton(
                onClick = onSkipOnboarding,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.debug_skip_onboarding))
            }

            OutlinedButton(
                onClick = onSeedData,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.debug_seed_data))
            }

            OutlinedButton(
                onClick = onResetToFirstLaunch,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.debug_reset_first_launch))
            }

            OutlinedButton(
                onClick = onTriggerWorkers,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.debug_trigger_workers))
            }

            Button(
                onClick = onCrashApp,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.debug_crash_app))
            }
        }
    }
}

private fun triggerDebugWorkers(context: android.content.Context) {
    val workManager = WorkManager.getInstance(context)
    workManager.enqueue(OneTimeWorkRequestBuilder<WikiSyncWorker>().build())
    workManager.enqueue(OneTimeWorkRequestBuilder<EventWidgetRefreshWorker>().build())
    workManager.enqueue(OneTimeWorkRequestBuilder<NtfyPushWorker>().build())
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


@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    com.lamadb.android.theme.LamaDBTheme {
        SettingsScreen(
            serverUrl = "https://lamadb.tailnet",
            themeMode = com.lamadb.android.theme.ThemeMode.SYSTEM,
            onLogout = {},
            onResetPresence = {}
        )
    }
}
