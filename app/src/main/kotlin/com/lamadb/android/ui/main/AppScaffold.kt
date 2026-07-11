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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.lamadb.android.data.wiki.WikiPageEntity
import com.lamadb.android.theme.ThemeMode
import com.lamadb.android.ui.dashboard.DashboardScreen
import com.lamadb.android.ui.settings.SettingsScreen
import com.lamadb.android.ui.wiki.WikiPageScreen
import com.lamadb.android.ui.wiki.WikiScreen

@Composable
fun AppScaffold(
    serverUrl: String,
    apiKey: String,
    themeMode: ThemeMode,
    initialDestination: AppDestination? = null,
    onOpenQrScanner: () -> Unit,
    onLogout: () -> Unit,
    onResetPresence: () -> Unit,
    onViewLogs: () -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
    onResetToFirstLaunch: () -> Unit = {},
    onDynamicColorChanged: (Boolean) -> Unit = {},
    onThemeModeChanged: (ThemeMode) -> Unit = {},
    showDebugOverlay: Boolean = true,
    onDebugOverlayChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selected by rememberSaveable { mutableStateOf(initialDestination ?: AppDestination.Dashboard) }
    var selectedWikiPage by rememberSaveable { mutableStateOf<String?>(null) }

    selectedWikiPage?.let { path ->
        WikiPageScreen(
            path = path,
            onBack = { selectedWikiPage = null }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                AppDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        modifier = Modifier.testTag("nav_tab_${destination.name.lowercase()}"),
                        icon = { Icon(destination.icon, contentDescription = stringResource(destination.labelRes)) },
                        label = { Text(stringResource(destination.labelRes)) },
                        selected = selected == destination,
                        onClick = { selected = destination }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            when (selected) {
                AppDestination.Dashboard -> DashboardScreen(
                    serverUrl = serverUrl,
                    apiKey = apiKey,
                    themeMode = themeMode,
                    onOpenQrScanner = onOpenQrScanner,
                    showDebugOverlay = showDebugOverlay
                )

                AppDestination.Wiki -> WikiScreen(
                    onPageClick = { page -> selectedWikiPage = page.path }
                )

                AppDestination.Settings -> SettingsScreen(
                    serverUrl = serverUrl,
                    themeMode = themeMode,
                    onLogout = onLogout,
                    onResetPresence = onResetPresence,
                    onViewLogs = onViewLogs,
                    onReplayOnboarding = onReplayOnboarding,
                    onResetToFirstLaunch = onResetToFirstLaunch,
                    onDynamicColorChanged = onDynamicColorChanged,
                    onThemeModeChanged = onThemeModeChanged,
                    showDebugOverlay = showDebugOverlay,
                    onDebugOverlayChanged = onDebugOverlayChanged
                )
            }
        }
    }
}
