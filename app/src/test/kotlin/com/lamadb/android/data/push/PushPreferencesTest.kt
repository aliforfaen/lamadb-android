package com.lamadb.android.data.push

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PushPreferencesTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences("lamadb_push", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.apply() } returns Unit

        every { prefs.getBoolean("push_enabled", false) } returns false
        every { prefs.getString("ntfy_url", PushPreferences.DEFAULT_NTFY_URL) } returns PushPreferences.DEFAULT_NTFY_URL
        every { prefs.getString("ntfy_topic", PushPreferences.DEFAULT_NTFY_TOPIC) } returns PushPreferences.DEFAULT_NTFY_TOPIC
        every { prefs.getLong("last_message_time", 0L) } returns 0L
        every { prefs.getBoolean("channels_created", false) } returns false
    }

    @Test
    fun defaults_mirrorBackendConfiguration() {
        val pushPreferences = PushPreferences(context)

        assertFalse(pushPreferences.pushEnabled)
        assertEquals(PushPreferences.DEFAULT_NTFY_URL, pushPreferences.ntfyUrl)
        assertEquals(PushPreferences.DEFAULT_NTFY_TOPIC, pushPreferences.ntfyTopic)
        assertEquals(0L, pushPreferences.lastMessageTime)
        assertFalse(pushPreferences.channelsCreated)
    }

    @Test
    fun setters_persistValues() {
        val pushPreferences = PushPreferences(context)

        pushPreferences.pushEnabled = true
        pushPreferences.ntfyUrl = "https://ntfy.local"
        pushPreferences.ntfyTopic = "lamadb-alerts"
        pushPreferences.lastMessageTime = 12345L
        pushPreferences.channelsCreated = true

        verify { editor.putBoolean("push_enabled", true) }
        verify { editor.putString("ntfy_url", "https://ntfy.local") }
        verify { editor.putString("ntfy_topic", "lamadb-alerts") }
        verify { editor.putLong("last_message_time", 12345L) }
        verify { editor.putBoolean("channels_created", true) }
        verify { editor.apply() }
    }
}
