package com.lamadb.android.data.push

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A message received from an ntfy topic.
 *
 * ntfy returns newline-delimited JSON (NDJSON); each line is one of these objects.
 */
@Serializable
data class NtfyMessage(
    val id: String,
    val time: Long = 0,
    val topic: String = "",
    val title: String? = null,
    val message: String = "",
    val priority: Int = DEFAULT_PRIORITY,
    val tags: List<String> = emptyList()
) {
    companion object {
        const val MIN_PRIORITY = 1
        const val MAX_PRIORITY = 5
        const val DEFAULT_PRIORITY = 3
    }
}

/**
 * ntfy server health/status response.
 */
@Serializable
data class NtfyHealth(
    val reachable: Boolean,
    val topic: String,
    @SerialName("message_count")
    val messageCount: Int
)
