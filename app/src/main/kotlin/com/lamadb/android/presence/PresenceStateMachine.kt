package com.lamadb.android.presence

import java.util.Locale

sealed class PresenceState {
    data object Home : PresenceState()
    data object Away : PresenceState()
    data object Unknown : PresenceState()
}

data class PresenceEvent(
    val state: PresenceState,
    val previousState: PresenceState,
    val ssid: String?,
    val confidence: Double
)

/**
 * Determines stable presence state from WiFi SSID changes with a debounce.
 *
 * SSID matching is case-insensitive and strips surrounding quotes that Android
 * often adds (e.g. "\"Valhalla-5G\"").
 *
 * A null SSID is treated as away because we cannot confirm the home network.
 */
class PresenceStateMachine(
    private val homeSsid: String,
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MILLIS,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    private var stableState: PresenceState = PresenceState.Unknown
    private var pendingState: PresenceState? = null
    private var pendingSince: Long = 0

    /**
     * Evaluates the current network SSID. Returns a [PresenceEvent] only when
     * the desired state has remained unchanged for [debounceMillis]. Until then
     * the state machine records the pending transition and returns null.
     */
    fun evaluate(ssid: String?): PresenceEvent? {
        val desired = desiredState(ssid)
        val now = clock()

        if (desired == stableState) {
            // Any pending transition to the stable state is cancelled.
            pendingState = null
            return null
        }

        if (desired != pendingState) {
            // New transition request; start the debounce window.
            pendingState = desired
            pendingSince = now
            return null
        }

        if (now - pendingSince >= debounceMillis) {
            val event = PresenceEvent(
                state = desired,
                previousState = stableState,
                ssid = ssid?.normalizeSsid(),
                confidence = if (ssid == null) CONFIDENCE_NO_SSID else CONFIDENCE_SSID
            )
            stableState = desired
            pendingState = null
            return event
        }

        return null
    }

    fun currentState(): PresenceState = stableState

    /**
     * Returns true if a transition has been requested but the debounce window
     * has not yet elapsed.
     */
    fun hasPendingTransition(): Boolean = pendingState != null && pendingState != stableState

    private fun desiredState(ssid: String?): PresenceState {
        if (ssid == null) return PresenceState.Away
        return if (ssid.normalizeSsid() == homeSsid.normalizeSsid()) {
            PresenceState.Home
        } else {
            PresenceState.Away
        }
    }

    private fun String.normalizeSsid(): String =
        trim('"').lowercase(Locale.getDefault())

    companion object {
        const val DEFAULT_DEBOUNCE_MILLIS = 30_000L
        const val CONFIDENCE_SSID = 0.95
        const val CONFIDENCE_NO_SSID = 0.70
    }
}
