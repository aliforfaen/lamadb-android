package com.lamadb.android.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProvisionResponse(
    @SerialName("api_key") val apiKey: String,
    @SerialName("user_id") val userId: String,
    @SerialName("expires_at") val expiresAt: String? = null
)
