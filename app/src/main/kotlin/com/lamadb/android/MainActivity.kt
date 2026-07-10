package com.lamadb.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lamadb.android.data.auth.AuthRepository
import com.lamadb.android.data.auth.SecureTokenStore
import com.lamadb.android.data.push.NtfyPushWorker
import com.lamadb.android.data.wiki.WikiSyncWorker
import com.lamadb.android.logging.AppLogger
import com.lamadb.android.widget.EventWidgetRefreshWorker
import com.lamadb.android.onboarding.OnboardingPreferences
import com.lamadb.android.presence.PresencePreferences
import com.lamadb.android.presence.PresenceService
import com.lamadb.android.theme.LamaDBTheme
import com.lamadb.android.theme.ThemeMode
import com.lamadb.android.theme.ThemePreferences
import com.lamadb.android.ui.auth.AuthState
import com.lamadb.android.ui.auth.AuthViewModel
import com.lamadb.android.ui.logs.LogViewerScreen
import com.lamadb.android.ui.login.LoginScreen
import com.lamadb.android.ui.main.AppScaffold
import com.lamadb.android.ui.onboarding.OnboardingScreen
import com.lamadb.android.ui.presence.PresenceSetupDialog
import com.lamadb.android.ui.qr.QrScannerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.init(this)
        enableEdgeToEdge()

        setContent {
            val context = this@MainActivity
            val themePreferences = remember { ThemePreferences(context) }
            var dynamicColor by remember { mutableStateOf(themePreferences.useDynamicColor) }
            var themeMode by remember { mutableStateOf(themePreferences.themeMode) }

            LamaDBTheme(
                dynamicColor = dynamicColor,
                themeMode = themeMode
            ) {
                val viewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(
                        AuthRepository(SecureTokenStore(context))
                    )
                )
                val state by viewModel.state.collectAsState()

                // Ensure the auth check runs once on first composition.
                remember { viewModel.checkAuth(); true }

                when (state) {
                    AuthState.Checking -> {
                        // Splash / blank while checking auth.
                    }

                    AuthState.Login, is AuthState.Error -> {
                        val onboardingPreferences = remember { OnboardingPreferences(context) }
                        var showOnboarding by remember {
                            mutableStateOf(!onboardingPreferences.onboardingCompleted)
                        }

                        if (showOnboarding) {
                            OnboardingScreen(
                                onGetStarted = {
                                    onboardingPreferences.onboardingCompleted = true
                                    showOnboarding = false
                                },
                                onSkip = {
                                    onboardingPreferences.onboardingCompleted = true
                                    showOnboarding = false
                                }
                            )
                        } else {
                            var showScanner by remember { mutableStateOf(false) }
                            if (showScanner) {
                                QrScannerScreen(
                                    viewModel = viewModel,
                                    onBack = { showScanner = false }
                                )
                            } else {
                                LoginScreen(
                                    viewModel = viewModel,
                                    onScanQr = { showScanner = true }
                                )
                            }
                        }
                    }

                    AuthState.Authenticated -> {
                        AuthenticatedContent(
                            viewModel = viewModel,
                            themeMode = themeMode,
                            onDynamicColorChanged = { dynamicColor = it },
                            onThemeModeChanged = { themeMode = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedContent(
    viewModel: AuthViewModel,
    themeMode: ThemeMode,
    onDynamicColorChanged: (Boolean) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val tokenStore = remember { SecureTokenStore(context) }
    val credentials = remember { tokenStore.load().getOrNull() }
    val presencePreferences = remember { PresencePreferences(context) }

    var showPresenceSetup by remember { mutableStateOf(!presencePreferences.isSetupComplete) }
    var showDashboardScanner by remember { mutableStateOf(false) }
    var showLogViewer by remember { mutableStateOf(false) }

    if (showLogViewer) {
        LogViewerScreen(onBack = { showLogViewer = false })
        return
    }

    if (showDashboardScanner) {
        QrScannerScreen(
            viewModel = viewModel,
            onBack = { showDashboardScanner = false }
        )
        return
    }

    if (showPresenceSetup) {
        PresenceSetupDialog(
            onDismiss = {
                showPresenceSetup = false
                // User skipped setup; do not start the presence service.
            },
            onComplete = {
                showPresenceSetup = false
                PresenceService.start(context)
            }
        )
        return
    }

    // Ensure background workers are scheduled whenever the authenticated
    // UI is shown. Each worker checks its own preferences internally.
    NtfyPushWorker.syncSchedule(context)
    EventWidgetRefreshWorker.schedule(context)
    WikiSyncWorker.schedule(context)

    AppScaffold(
        serverUrl = credentials?.serverUrl ?: "",
        apiKey = credentials?.apiKey ?: "",
        themeMode = themeMode,
        onOpenQrScanner = { showDashboardScanner = true },
        onLogout = {
            PresenceService.stop(context)
            PresencePreferences(context).clear()
            viewModel.logout()
        },
        onResetPresence = {
            presencePreferences.homeSsid = null
            showPresenceSetup = true
        },
        onViewLogs = { showLogViewer = true },
        onReplayOnboarding = {
            OnboardingPreferences(context).onboardingCompleted = false
            viewModel.logout()
        },
        onDynamicColorChanged = onDynamicColorChanged,
        onThemeModeChanged = onThemeModeChanged
    )
}

class AuthViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
