package com.lamadb.android.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Event row returned by `GET /api/events`.
 */
@Serializable
data class EventResponse(
    val id: Int,
    val ts: String,
    val source: String,
    val type: String,
    val severity: String,
    val title: String,
    val body: String? = null,
    val processed: Boolean = false,
    val ticker: Boolean = false,
    val tags: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    val count: Int = 1
)
