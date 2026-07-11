package com.lamadb.android.data.push

import android.content.Context
import android.content.SharedPreferences

/**
 * User-facing settings for the ntfy push integration.
 *
 * Defaults mirror the LamaDB backend defaults so that users on the same
 * self-hosted stack can enable push with a single toggle.
 */
class PushPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var pushEnabled: Boolean
        get() = prefs.getBoolean(KEY_PUSH_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PUSH_ENABLED, value).apply()

    var ntfyUrl: String
        get() = prefs.getString(KEY_NTFY_URL, DEFAULT_NTFY_URL) ?: DEFAULT_NTFY_URL
        set(value) = prefs.edit().putString(KEY_NTFY_URL, value.trim()).apply()

    var ntfyTopic: String
        get() = prefs.getString(KEY_NTFY_TOPIC, DEFAULT_NTFY_TOPIC) ?: DEFAULT_NTFY_TOPIC
        set(value) = prefs.edit().putString(KEY_NTFY_TOPIC, value.trim()).apply()

    /**
     * Highest ntfy message timestamp already shown. Used to avoid duplicate
     * notifications across worker runs.
     */
    var lastMessageTime: Long
        get() = prefs.getLong(KEY_LAST_MESSAGE_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_MESSAGE_TIME, value).apply()

    /**
     * True when the user has completed the one-time notification channel setup.
     */
    var channelsCreated: Boolean
        get() = prefs.getBoolean(KEY_CHANNELS_CREATED, false)
        set(value) = prefs.edit().putBoolean(KEY_CHANNELS_CREATED, value).apply()

    companion object {
        private const val PREFS_NAME = "lamadb_push"
        private const val KEY_PUSH_ENABLED = "push_enabled"
        private const val KEY_NTFY_URL = "ntfy_url"
        private const val KEY_NTFY_TOPIC = "ntfy_topic"
        private const val KEY_LAST_MESSAGE_TIME = "last_message_time"
        private const val KEY_CHANNELS_CREATED = "channels_created"

        const val DEFAULT_NTFY_URL = "https://ntfy.notflix.no"
        const val DEFAULT_NTFY_TOPIC = "hermes-worker"
    }
}
