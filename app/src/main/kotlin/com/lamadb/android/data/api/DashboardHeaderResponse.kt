package com.lamadb.android.data.api

import kotlinx.serialization.Serializable

/**
 * Response from `GET /api/dashboard/header`.
 *
 * The Android client only consumes the ticker list; the status_bar payload is
 * ignored because the app computes equivalent state natively.
 */
@Serializable
data class DashboardHeaderResponse(
    val ticker: List<TickerItemResponse> = emptyList()
)

/**
 * A single marquee item for the dashboard ticker.
 */
@Serializable
data class TickerItemResponse(
    val id: Int,
    val ts: String? = null,
    val source: String = "",
    val sourceIcon: String? = null,
    val severity: String = "info",
    val title: String,
    val body: String? = null,
    val tags: List<String> = emptyList(),
    val icon: String = "✓"
)
