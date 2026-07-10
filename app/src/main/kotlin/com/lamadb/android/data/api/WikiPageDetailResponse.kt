package com.lamadb.android.data.api

import kotlinx.serialization.Serializable

/**
 * Full wiki page returned by `GET /api/wiki/page/{path}`.
 */
@Serializable
data class WikiPageDetailResponse(
    val id: String? = null,
    val title: String,
    val path: String,
    val section: String = "",
    val content: String = "",
    val size: Int = 0,
    val tags: List<String> = emptyList()
)
