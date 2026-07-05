package com.lamadb.android.data.api

import kotlinx.serialization.Serializable

@Serializable
data class MeResponse(
    val id: String,
    val name: String? = null,
    val email: String? = null
)
