package com.lamadb.android.data.api

import kotlinx.serialization.Serializable

/**
 * Wiki page summary returned by `GET /api/wiki/pages`.
 */
@Serializable
data class WikiPageResponse(
    val path: String,
    val title: String,
    val section: String = "",
    val size: Int = 0
)
