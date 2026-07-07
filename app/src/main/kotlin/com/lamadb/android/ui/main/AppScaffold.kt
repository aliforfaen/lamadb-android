package com.lamadb.android.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lamadb.android.ui.dashboard.DashboardScreen
import com.lamadb.android.ui.health.HealthDataScreen
import com.lamadb.android.ui.settings.SettingsScreen
import com.lamadb.android.ui.tasks.ScheduledTasksScreen

@Composable
fun AppScaffold(
    serverUrl: String,
    apiKey: String,
    onOpenQrScanner: () -> Unit,
    onLogout: () -> Unit,
    onResetPresence: () -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selected by rememberSaveable { mutableStateOf(AppDestination.Dashboard) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                AppDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) },
                        selected = selected == destination,
                        onClick = { selected = destination }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selected) {
                AppDestination.Dashboard -> DashboardScreen(
                    serverUrl = serverUrl,
                    apiKey = apiKey,
                    onOpenQrScanner = onOpenQrScanner
                )

                AppDestination.Tasks -> ScheduledTasksScreen()
                AppDestination.Health -> HealthDataScreen()
                AppDestination.Settings -> SettingsScreen(
                    serverUrl = serverUrl,
                    onLogout = onLogout,
                    onResetPresence = onResetPresence,
                    onDynamicColorChanged = onDynamicColorChanged
                )
            }
        }
    }
}
