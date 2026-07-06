package com.lamadb.android.data.events

import android.content.Context
import com.lamadb.android.data.api.EventRequest
import com.lamadb.android.data.api.LamaDBApiClient
import com.lamadb.android.data.auth.SecureTokenStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class EventRepository(
    context: Context,
    private val dao: EventDao = EventDatabase.getInstance(context).eventDao(),
    private val tokenStore: SecureTokenStore = SecureTokenStore(context),
    private val apiClientFactory: (serverUrl: String, apiKey: String) -> LamaDBApiClient = { url, key ->
        LamaDBApiClient(url, key)
    }
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun enqueue(event: EventRequest) {
        val entity = QueuedEvent(
            source = event.source,
            type = event.type,
            severity = event.severity,
            title = event.title,
            body = event.body,
            metadata = json.encodeToString(JsonObject.serializer(), event.metadata),
            createdAt = System.currentTimeMillis()
        )
        dao.insert(entity)
    }

    /**
     * Drains up to [batchSize] queued events to the LamaDB backend.
     * Returns the number of events successfully sent.
     */
    suspend fun drain(batchSize: Int = 50): Int {
        val credentials = tokenStore.load().getOrNull() ?: return 0
        val client = apiClientFactory(credentials.serverUrl, credentials.apiKey)
        val events = dao.oldest(batchSize)
        if (events.isEmpty()) return 0

        var sent = 0
        for (event in events) {
            val request = event.toRequest() ?: continue
            val result = client.postEvent(request)
            if (result.isSuccess) {
                dao.delete(event)
                sent++
            } else {
                val retries = event.retryCount + 1
                if (retries > MAX_RETRIES) {
                    dao.delete(event)
                } else {
                    dao.update(event.copy(retryCount = retries))
                }
            }
        }
        return sent
    }

    suspend fun prune(maxAgeMillis: Long = MAX_AGE_MILLIS) {
        val cutoff = System.currentTimeMillis() - maxAgeMillis
        dao.deleteOlderThan(cutoff)
    }

    private fun QueuedEvent.toRequest(): EventRequest? = try {
        EventRequest(
            source = source,
            type = type,
            severity = severity,
            title = title,
            body = body,
            metadata = json.decodeFromString(metadata)
        )
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val MAX_RETRIES = 5
        private const val MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000

        fun createDefault(context: Context): EventRepository {
            return EventRepository(context)
        }
    }
}
