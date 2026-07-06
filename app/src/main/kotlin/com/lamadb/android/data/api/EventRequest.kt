package com.lamadb.android.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class EventRequest(
    val source: String,
    val type: String,
    val severity: String,
    val title: String,
    val body: String? = null,
    val metadata: JsonObject
)
