package com.lamadb.android.data.push

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Minimal HTTP client for polling an ntfy server.
 *
 * This intentionally does not reuse [com.lamadb.android.data.api.LamaDBApiClient]
 * because ntfy is usually hosted on a different URL and never needs Bearer auth.
 */
class NtfyApiClient(
    private val baseUrl: String,
    private val topic: String,
    engine: HttpClientEngine = Android.create()
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(engine) {
        install(ContentNegotiation) { json(json) }
    }

    /**
     * Poll the topic for messages published since [since] (ntfy duration string,
     * e.g. "15m", "1h", "1d"). Returns an empty list on network or parse errors.
     */
    suspend fun poll(since: String = "15m"): List<NtfyMessage> {
        val url = buildPollUrl(since)
        return try {
            val response: HttpResponse = client.get(url)
            if (response.status.value != 200) return emptyList()
            parseNdjson(response.bodyAsText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Check whether the configured ntfy server/topic is reachable.
     */
    suspend fun health(): Result<NtfyHealth> = runCatching {
        val url = buildPollUrl("1h")
        val response: HttpResponse = client.get(url)
        val text = response.bodyAsText().trim()
        val count = text.lines().count { it.isNotBlank() }
        NtfyHealth(
            reachable = response.status.value == 200,
            topic = topic,
            messageCount = count
        )
    }

    private fun buildPollUrl(since: String): String {
        val server = baseUrl.trimEnd('/')
        return "$server/$topic/json?poll=1&since=$since"
    }

    private fun parseNdjson(text: String): List<NtfyMessage> {
        if (text.isBlank()) return emptyList()
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                try {
                    json.decodeFromString(NtfyMessage.serializer(), line)
                } catch (_: Exception) {
                    null
                }
            }
            .toList()
    }
}
