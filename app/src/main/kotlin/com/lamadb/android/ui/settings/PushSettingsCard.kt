package com.lamadb.android.ui.settings

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.lamadb.android.R
import com.lamadb.android.data.push.NtfyPushWorker
import com.lamadb.android.data.push.PushNotificationHelper
import com.lamadb.android.data.push.PushPreferences

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PushSettingsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferences = remember { PushPreferences(context) }
    var enabled by remember { mutableStateOf(preferences.pushEnabled) }
    var url by remember { mutableStateOf(preferences.ntfyUrl) }
    var topic by remember { mutableStateOf(preferences.ntfyTopic) }

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    fun onToggle(checked: Boolean) {
        if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (notificationPermission?.status?.isGranted == false) {
                notificationPermission.launchPermissionRequest()
                return
            }
        }
        enabled = checked
        preferences.pushEnabled = checked
        NtfyPushWorker.syncSchedule(context)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.push_settings_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.push_settings_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.push_enable_toggle),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(R.string.push_enable_toggle_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { onToggle(it) }
                )
            }

            if (enabled) {
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        preferences.ntfyUrl = it
                    },
                    label = { Text(stringResource(R.string.push_ntfy_url_label)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = topic,
                    onValueChange = {
                        topic = it
                        preferences.ntfyTopic = it
                    },
                    label = { Text(stringResource(R.string.push_ntfy_topic_label)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        PushNotificationHelper(context).show(
                            com.lamadb.android.data.push.NtfyMessage(
                                id = "test",
                                title = context.getString(R.string.push_test_title),
                                message = context.getString(R.string.push_test_message),
                                priority = 4
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.push_test_button))
                }
            }
        }
    }
}
