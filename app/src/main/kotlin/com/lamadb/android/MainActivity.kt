package com.lamadb.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import com.lamadb.android.data.auth.AuthRepository
import com.lamadb.android.data.auth.SecureTokenStore
import com.lamadb.android.data.events.EventDatabase
import com.lamadb.android.data.events.QueuedEvent
import com.lamadb.android.data.push.NtfyPushWorker
import com.lamadb.android.data.wiki.WikiPageEntity
import com.lamadb.android.data.wiki.WikiSyncWorker
import com.lamadb.android.debug.DebugLaunchOptions
import com.lamadb.android.debug.DebugPreferences
import com.lamadb.android.debug.TestDataSeeder
import com.lamadb.android.debug.parseDebugLaunchOptions
import com.lamadb.android.logging.AppLogger
import com.lamadb.android.widget.EventWidgetRefreshWorker
import com.lamadb.android.onboarding.OnboardingPreferences
import com.lamadb.android.presence.PresencePreferences
import com.lamadb.android.presence.PresenceService
import com.lamadb.android.theme.LamaDBTheme
import com.lamadb.android.theme.SecurityPreferences
import com.lamadb.android.theme.ThemeMode
import com.lamadb.android.theme.ThemePreferences
import com.lamadb.android.ui.auth.AuthState
import com.lamadb.android.ui.auth.AuthViewModel
import com.lamadb.android.ui.logs.LogViewerScreen
import com.lamadb.android.ui.login.LoginScreen
import com.lamadb.android.ui.main.AppDestination
import com.lamadb.android.ui.main.AppScaffold
import com.lamadb.android.ui.onboarding.OnboardingScreen
import com.lamadb.android.ui.presence.PresenceSetupDialog
import com.lamadb.android.ui.qr.QrScannerScreen
import java.io.File

class MainActivity : FragmentActivity() {

    private var pendingShortcutAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        AppLogger.init(this)

        // Debug-only: dump app log to app-accessible external storage and exit.
        if (BuildConfig.DEBUG && intent.getBooleanExtra("DUMP_LOGS", false)) {
            val logFile = AppLogger.getLogFile()
            if (logFile != null && logFile.exists()) {
                val outFile = File(getExternalFilesDir(null), "lamadb-logs.txt")
                outFile.writeBytes(logFile.readBytes())
                AppLogger.i("LamaDB", "Logs dumped to ${outFile.absolutePath}")
            }
            finish()
            return
        }
        enableEdgeToEdge()

        pendingShortcutAction = intent.getStringExtra("shortcut")

        val launchOptions = intent.parseDebugLaunchOptions()
        applyDebugLaunchOptions(launchOptions)

        setContent {
            val context = this@MainActivity
            val themePreferences = remember { ThemePreferences(context) }
            val debugPreferences = remember { DebugPreferences(context) }
            var dynamicColor by remember { mutableStateOf(themePreferences.useDynamicColor) }
            var themeMode by remember { mutableStateOf(themePreferences.themeMode) }
            var showDebugOverlay by remember { mutableStateOf(debugPreferences.dashboardDebugOverlay) }

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
                    AuthState.Checking -> AuthCheckingScreen()

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
                        val securityPrefs = remember { SecurityPreferences(context) }
                        val biometricEnabled = securityPrefs.biometricEnabled
                        val biometricManager = remember { BiometricManager.from(context) }
                        val canAuthenticate = remember {
                            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                        }
                        val biometricAvailable = canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
                        var biometricPassed by remember { mutableStateOf(!biometricEnabled || !biometricAvailable) }

                        if (biometricPassed) {
                            val shortcutAction = pendingShortcutAction
                            var showScanner by remember { mutableStateOf(shortcutAction == "scan_qr") }

                            LaunchedEffect(shortcutAction) {
                                pendingShortcutAction = null
                            }

                            if (showScanner) {
                                QrScannerScreen(
                                    viewModel = viewModel,
                                    onBack = { showScanner = false }
                                )
                            } else {
                                AuthenticatedContent(
                                    viewModel = viewModel,
                                    themeMode = themeMode,
                                    initialDestination = if (shortcutAction == "dashboard") AppDestination.Dashboard else launchOptions.startScreen,
                                    seedData = launchOptions.seedData,
                                    onDynamicColorChanged = { dynamicColor = it },
                                    onThemeModeChanged = { themeMode = it },
                                    showDebugOverlay = showDebugOverlay,
                                    onDebugOverlayChanged = { showDebugOverlay = it }
                                )
                            }
                        } else {
                            val activity = this@MainActivity
                            val promptInfo = remember {
                                BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("Unlock LamaDB")
                                    .setNegativeButtonText("Cancel")
                                    .build()
                            }
                            val biometricPrompt = remember {
                                BiometricPrompt(
                                    activity,
                                    ContextCompat.getMainExecutor(activity),
                                    object : BiometricPrompt.AuthenticationCallback() {
                                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                            biometricPassed = true
                                        }
                                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                            when (errorCode) {
                                                BiometricPrompt.ERROR_NO_BIOMETRICS,
                                                BiometricPrompt.ERROR_HW_NOT_PRESENT,
                                                BiometricPrompt.ERROR_HW_UNAVAILABLE -> biometricPassed = true
                                                else -> viewModel.logout()
                                            }
                                        }
                                    }
                                )
                            }
                            LaunchedEffect(Unit) {
                                biometricPrompt.authenticate(promptInfo)
                            }
                            AuthCheckingScreen()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingShortcutAction = intent.getStringExtra("shortcut")
    }

    /**
     * Apply debug-only launch mutations. In release builds [options] is always
     * the default empty value, so this function is effectively a no-op.
     */
    private fun applyDebugLaunchOptions(options: DebugLaunchOptions) {
        if (!BuildConfig.DEBUG) return

        if (options.resetFirstLaunch) {
            // Clear everything and let this activity instance continue. The auth
            // check will find no credentials and the onboarding screen will show.
            runBlocking(Dispatchers.IO) { clearAppData(this@MainActivity) }
        }

        if (options.skipOnboarding) {
            OnboardingPreferences(this).onboardingCompleted = true
        }

        if (options.skipPresenceSetup) {
            PresencePreferences(this).homeSsid = DEBUG_HOME_SSID
        }

        if (options.useTestAccount) {
            saveTestAccount(this)
        }

        if (options.queueEventCount > 0) {
            runBlocking(Dispatchers.IO) {
                val dao = EventDatabase.getInstance(this@MainActivity).eventDao()
                val now = System.currentTimeMillis()
                val events = (1..options.queueEventCount).map { i ->
                    QueuedEvent(
                        source = "debug-test",
                        type = "debug_test",
                        severity = "info",
                        title = "Debug test event $i",
                        body = "Generated debug event $i.",
                        metadata = "{\"test\": $i}",
                        createdAt = now + i
                    )
                }
                events.forEach { dao.insert(it) }
            }
        }

        if (options.wikiPageCount > 0) {
            runBlocking(Dispatchers.IO) {
                val dao = EventDatabase.getInstance(this@MainActivity).wikiDao()
                val now = System.currentTimeMillis()
                val pages = (1..options.wikiPageCount).map { i ->
                    val content = "Generated debug content for page $i."
                    WikiPageEntity(
                        path = "debug/test-page-$i",
                        title = "Debug Test Page $i",
                        section = "Debug",
                        size = content.length,
                        content = content,
                        syncedAt = now + i
                    )
                }
                dao.insertAll(pages)
            }
        }

        if (options.presenceState != null) {
            val prefs = PresencePreferences(this@MainActivity)
            when (options.presenceState) {
                "home" -> prefs.homeSsid = "debug-home-ssid"
                "away" -> prefs.homeSsid = null
                "unknown" -> prefs.homeSsid = null
            }
        }

        if (options.authExpired) {
            val tokenStore = SecureTokenStore(this@MainActivity)
            tokenStore.clear()
            tokenStore.save(
                apiKey = "EXPIRED_DEBUG_KEY",
                serverUrl = "https://lamadb.debug.expired",
                userId = "debug-expired"
            )
        }
    }

    private fun saveTestAccount(context: Context) {
        val url = BuildConfig.LAMADB_TEST_URL
        val key = BuildConfig.LAMADB_TEST_API_KEY
        if (url.isBlank() || key.isBlank()) return
        SecureTokenStore(context).save(
            apiKey = key,
            serverUrl = url,
            userId = DEBUG_USER_ID
        )
    }

    companion object {
        private const val DEBUG_HOME_SSID = "debug-home"
        private const val DEBUG_USER_ID = "debug-user"
    }
}

private suspend fun clearAppData(context: Context) {
    OnboardingPreferences(context).onboardingCompleted = false
    PresencePreferences(context).clear()
    SecureTokenStore(context).clear()
    withContext(Dispatchers.IO) {
        EventDatabase.getInstance(context).clearAllTables()
    }
}

@Composable
private fun AuthenticatedContent(
    viewModel: AuthViewModel,
    themeMode: ThemeMode,
    initialDestination: AppDestination?,
    seedData: Boolean,
    onDynamicColorChanged: (Boolean) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    showDebugOverlay: Boolean = true,
    onDebugOverlayChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenStore = remember { SecureTokenStore(context) }
    val credentials = remember { tokenStore.load().getOrNull() }
    val presencePreferences = remember { PresencePreferences(context) }

    var showPresenceSetup by remember { mutableStateOf(!presencePreferences.isSetupComplete) }
    var showDashboardScanner by remember { mutableStateOf(false) }
    var showLogViewer by remember { mutableStateOf(false) }

    // Seed fixtures once when entering the authenticated UI. Only runs in debug
    // builds because seedData is forced false in release.
    if (seedData) {
        LaunchedEffect(Unit) {
            TestDataSeeder.seedAll(context)
        }
    }

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
        initialDestination = initialDestination,
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
        onResetToFirstLaunch = {
            scope.launch {
                clearAppData(context)
                (context as? android.app.Activity)?.recreate()
            }
        },
        onDynamicColorChanged = onDynamicColorChanged,
        onThemeModeChanged = onThemeModeChanged,
        showDebugOverlay = showDebugOverlay,
        onDebugOverlayChanged = onDebugOverlayChanged
    )
}


@Composable
private fun AuthCheckingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}

class AuthViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
