package com.lamadb.android.debug

import android.content.Context
import com.lamadb.android.BuildConfig
import com.lamadb.android.data.events.EventDatabase
import com.lamadb.android.data.events.QueuedEvent
import com.lamadb.android.data.push.NtfyMessage
import com.lamadb.android.data.push.PushNotificationHelper
import com.lamadb.android.data.wiki.WikiPageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Debug-only fixture data for smoke tests.
 *
 * All public methods are no-ops in release builds so the seeder can be called
 * from Compose or worker code without affecting production users.
 */
object TestDataSeeder {

    suspend fun seedAll(context: Context) {
        if (!BuildConfig.DEBUG) return
        withContext(Dispatchers.IO) {
            seedEvents(context)
            seedWikiPages(context)
            showTestNotification(context)
        }
    }

    suspend fun seedEvents(context: Context) {
        if (!BuildConfig.DEBUG) return
        withContext(Dispatchers.IO) {
            val dao = EventDatabase.getInstance(context).eventDao()
            if (dao.count() > 0) return@withContext

            val now = System.currentTimeMillis()
            val events = listOf(
                QueuedEvent(
                    source = "smoke-test",
                    type = "presence",
                    severity = "info",
                    title = "Home network detected",
                    body = "Device joined the home WiFi.",
                    metadata = metadata("ssid" to "Home-5G"),
                    createdAt = now - 300_000
                ),
                QueuedEvent(
                    source = "smoke-test",
                    type = "health",
                    severity = "warning",
                    title = "Health sync delayed",
                    body = "Samsung Health did not return data in time.",
                    metadata = metadata("retry" to 1),
                    createdAt = now - 600_000
                ),
                QueuedEvent(
                    source = "smoke-test",
                    type = "system",
                    severity = "critical",
                    title = "Disk space low",
                    body = "Backend reported less than 10% free disk space.",
                    metadata = metadata("percent_free" to 8),
                    createdAt = now - 900_000
                ),
                QueuedEvent(
                    source = "smoke-test",
                    type = "wiki",
                    severity = "info",
                    title = "Wiki cache refreshed",
                    body = "4 pages cached for offline reading.",
                    metadata = metadata("count" to 4),
                    createdAt = now - 1_200_000
                )
            )
            events.forEach { dao.insert(it) }
        }
    }

    suspend fun seedWikiPages(context: Context) {
        if (!BuildConfig.DEBUG) return
        withContext(Dispatchers.IO) {
            val dao = EventDatabase.getInstance(context).wikiDao()
            if (dao.count() > 0) return@withContext

            val now = System.currentTimeMillis()
            val pages = listOf(
                WikiPageEntity(
                    path = "smoke/test-page",
                    title = "Test Page",
                    section = "Testing",
                    size = 120,
                    content = "# Test Page\n\nThis is a seeded wiki page for smoke tests.",
                    syncedAt = now
                ),
                WikiPageEntity(
                    path = "smoke/android-harness",
                    title = "Android Test Harness",
                    section = "Testing",
                    size = 240,
                    content = "# Android Test Harness\n\nUse `adb shell am start` with debug extras to jump to any screen.",
                    syncedAt = now
                ),
                WikiPageEntity(
                    path = "home/welcome",
                    title = "Welcome",
                    section = "Home",
                    size = 180,
                    content = "# Welcome\n\nWelcome to the LamaDB wiki cache.",
                    syncedAt = now
                )
            )
            dao.insertAll(pages)
        }
    }

    fun showTestNotification(context: Context) {
        if (!BuildConfig.DEBUG) return
        val helper = PushNotificationHelper(context)
        helper.show(
            NtfyMessage(
                id = "smoke-test-${System.currentTimeMillis()}",
                time = System.currentTimeMillis() / 1000,
                topic = "lamadb-test",
                title = "Smoke test notification",
                message = "If you see this, the ntfy notification path is working.",
                priority = 4
            )
        )
    }

    private fun metadata(vararg pairs: Pair<String, Any>): String {
        val json = pairs.associate { it.first to JsonPrimitive(it.second.toString()) }
        return JsonObject(json).toString()
    }
}
