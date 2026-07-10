package com.lamadb.android.ui.presence

import android.Manifest
import android.net.wifi.WifiManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.lamadb.android.R
import com.lamadb.android.power.BatteryOptimizationHelper
import com.lamadb.android.power.BatteryOptimizationPreferences
import com.lamadb.android.presence.PresencePreferences
import com.lamadb.android.ui.power.BatteryOptimizationCard

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PresenceSetupDialog(
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { PresencePreferences(context) }
    val batteryPreferences = remember { BatteryOptimizationPreferences(context) }
    val batteryHelper = remember { BatteryOptimizationHelper(context) }
    var homeSsid by remember { mutableStateOf(preferences.homeSsid ?: readCurrentSsid(context)) }
    var showRationale by remember { mutableStateOf(false) }
    val shouldShowBatteryGuidance = remember {
        batteryHelper.isSamsungDevice() &&
            !batteryHelper.isIgnoringBatteryOptimizations() &&
            !batteryPreferences.guidanceDismissed
    }
    var showBatteryGuidance by remember { mutableStateOf(false) }
    var mainDialogVisible by remember { mutableStateOf(true) }

    val permissionState = rememberMultiplePermissionsState(
        permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_WIFI_STATE)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    )

    // Re-read the current SSID when the dialog resumes so it reflects the
    // network the user is currently on.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeSsid = readCurrentSsid(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (mainDialogVisible) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.presence_setup_title)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.presence_setup_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = homeSsid,
                        onValueChange = { homeSsid = it },
                        label = { Text(stringResource(R.string.presence_setup_ssid_label)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (homeSsid.isBlank()) return@Button
                        preferences.homeSsid = homeSsid
                        if (permissionState.allPermissionsGranted) {
                            if (shouldShowBatteryGuidance) {
                                mainDialogVisible = false
                                showBatteryGuidance = true
                            } else {
                                onComplete()
                            }
                        } else {
                            if (permissionState.shouldShowRationale) {
                                showRationale = true
                            } else {
                                permissionState.launchMultiplePermissionRequest()
                            }
                        }
                    },
                    enabled = homeSsid.isNotBlank()
                ) {
                    Text(stringResource(R.string.presence_setup_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.presence_setup_skip))
                }
            }
        )
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(R.string.presence_permission_title)) },
            text = { Text(stringResource(R.string.presence_permission_body)) },
            confirmButton = {
                Button(onClick = {
                    showRationale = false
                    permissionState.launchMultiplePermissionRequest()
                }) {
                    Text(stringResource(R.string.presence_permission_grant))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text(stringResource(R.string.presence_permission_deny))
                }
            }
        )
    }

    if (showBatteryGuidance) {
        AlertDialog(
            onDismissRequest = {
                showBatteryGuidance = false
                onComplete()
            },
            title = { Text(stringResource(R.string.battery_optimization_title)) },
            text = {
                BatteryOptimizationCard(
                    onDismiss = {
                        batteryPreferences.guidanceDismissed = true
                        showBatteryGuidance = false
                        onComplete()
                    },
                    modifier = Modifier.padding(top = 8.dp)
                )
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}

@Suppress("DEPRECATION")
private fun readCurrentSsid(context: android.content.Context): String {
    val wifiManager = context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as WifiManager
    val ssid = wifiManager.connectionInfo?.ssid
    return if (ssid == null || ssid == WifiManager.UNKNOWN_SSID) "" else ssid.trim('"')
}
