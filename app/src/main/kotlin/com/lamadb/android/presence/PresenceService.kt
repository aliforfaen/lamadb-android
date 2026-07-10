package com.lamadb.android.presence

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lamadb.android.MainActivity
import com.lamadb.android.R
import com.lamadb.android.data.api.EventRequest
import com.lamadb.android.data.events.EventDrainWorker
import com.lamadb.android.data.events.EventRepository
import com.lamadb.android.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class PresenceService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var preferences: PresencePreferences
    private lateinit var stateMachine: PresenceStateMachine
    private lateinit var eventRepository: EventRepository
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiManager: WifiManager
    private val handler = Handler(Looper.getMainLooper())

    private var lastKnownSsid: String? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = reevaluate()
        override fun onLost(network: Network) = reevaluate()
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) = reevaluate()
    }

    private val debounceRunnable = Runnable { reevaluate() }

    override fun onCreate() {
        super.onCreate()
        preferences = PresencePreferences(this)
        eventRepository = EventRepository.createDefault(this)
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        stateMachine = PresenceStateMachine(
            homeSsid = preferences.homeSsid ?: "",
            debounceMillis = PresenceStateMachine.DEFAULT_DEBOUNCE_MILLIS
        )
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasLocationPermission()) {
            AppLogger.w(TAG, "Location permission missing; cannot start presence foreground service")
            stopSelf()
            return START_NOT_STICKY
        }
        AppLogger.i(TAG, "Presence service started")
        startForeground(NOTIFICATION_ID, buildNotification(stateMachine.currentState()))
        scheduleDrainWorker()
        registerNetworkCallback()
        reevaluate()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(debounceRunnable)
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: IllegalArgumentException) {
            // Already unregistered.
        }
        serviceScope.cancel()
        serviceJob.cancel()
    }

    private fun registerNetworkCallback() {
        val request = android.net.NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    /**
     * Re-reads the current WiFi SSID and evaluates the state machine. If a
     * transition is pending, schedule another evaluation after the debounce.
     */
    private fun reevaluate() {
        handler.removeCallbacks(debounceRunnable)
        val ssid = readCurrentSsid()
        lastKnownSsid = ssid
        val event = stateMachine.evaluate(ssid)
        if (event != null) {
            serviceScope.launch { sendEvent(event) }
            updateNotification(event.state)
        } else {
            updateNotification(stateMachine.currentState())
            // If a transition is pending, re-evaluate after the debounce window so
            // we emit an event even if no further network callback fires.
            if (stateMachine.hasPendingTransition()) {
                handler.postDelayed(debounceRunnable, PresenceStateMachine.DEFAULT_DEBOUNCE_MILLIS)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun readCurrentSsid(): String? {
        if (!hasLocationPermission()) {
            AppLogger.w(TAG, "Location permission missing; cannot read SSID")
            return null
        }
        return try {
            val ssid = wifiManager.connectionInfo?.ssid
            if (ssid == null || ssid == WifiManager.UNKNOWN_SSID) null else ssid
        } catch (e: SecurityException) {
            AppLogger.e(TAG, "Failed to read SSID", e)
            null
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun sendEvent(event: com.lamadb.android.presence.PresenceEvent) {
        val title = when (event.state) {
            PresenceState.Home -> "Ali arrived home"
            PresenceState.Away -> "Ali left home"
            PresenceState.Unknown -> "Presence updated"
        }
        val body = event.ssid?.let { "Connected to WiFi '$it'" }
            ?: if (event.state == PresenceState.Away) "Not connected to home WiFi" else null

        val request = EventRequest(
            source = EVENT_SOURCE,
            type = EVENT_TYPE,
            severity = "info",
            title = title,
            body = body,
            metadata = JsonObject(
                mapOf(
                    "device_id" to JsonPrimitive(preferences.deviceId),
                    "sensor" to JsonPrimitive("wifi"),
                    "state" to JsonPrimitive(stateName(event.state)),
                    "ssid" to JsonPrimitive(event.ssid),
                    "previous_state" to JsonPrimitive(stateName(event.previousState)),
                    "confidence" to JsonPrimitive(event.confidence)
                )
            )
        )
        eventRepository.enqueue(request)
        EventDrainWorker.schedule(this) // try to flush soon
        AppLogger.i(TAG, "Queued presence event: ${event.state} (ssid=${event.ssid})")
    }

    private fun updateNotification(state: PresenceState) {
        val notification = buildNotification(state)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(state: PresenceState): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = when (state) {
            PresenceState.Home -> getString(R.string.presence_notification_home)
            PresenceState.Away -> getString(R.string.presence_notification_away)
            PresenceState.Unknown -> getString(R.string.presence_notification_unknown)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.presence_notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.presence_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.presence_channel_description)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun scheduleDrainWorker() {
        EventDrainWorker.schedule(this)
    }

    private fun stateName(state: PresenceState): String = when (state) {
        PresenceState.Home -> "home"
        PresenceState.Away -> "away"
        PresenceState.Unknown -> "unknown"
    }

    companion object {
        private const val TAG = "LamaDB"
        private const val CHANNEL_ID = "lamadb_presence"
        private const val NOTIFICATION_ID = 1
        private const val EVENT_SOURCE = "android_life_os"
        private const val EVENT_TYPE = "presence"

        fun start(context: Context) {
            val intent = Intent(context, PresenceService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PresenceService::class.java))
        }
    }
}
