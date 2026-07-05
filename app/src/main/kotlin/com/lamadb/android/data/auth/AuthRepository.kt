package com.lamadb.android.data.auth

import com.lamadb.android.data.api.LamaDBApiClient

class AuthRepository(
    private val tokenStore: SecureTokenStore,
    private val apiClientFactory: (serverUrl: String, apiKey: String?) -> LamaDBApiClient
) {

    constructor(tokenStore: SecureTokenStore) : this(
        tokenStore,
        { serverUrl, apiKey -> LamaDBApiClient(serverUrl, apiKey) }
    )

    fun isAuthenticated(): Boolean {
        return tokenStore.load().getOrNull() != null
    }

    fun getStoredServerUrl(): String? {
        return tokenStore.load().getOrNull()?.serverUrl
    }

    suspend fun validateAndSave(apiKey: String, serverUrl: String): Result<Unit> {
        val client = apiClientFactory(serverUrl, apiKey)
        return client.getMe().mapCatching { me ->
            tokenStore.save(apiKey, serverUrl, me.id).getOrThrow()
        }
    }

    suspend fun provisionAndSave(token: String, serverUrl: String): Result<Unit> {
        val anonymous = apiClientFactory(serverUrl, null)
        return anonymous.provision(com.lamadb.android.data.api.ProvisionRequest(token))
            .mapCatching { response ->
                val authenticated = apiClientFactory(serverUrl, response.apiKey)
                val me = authenticated.getMe().getOrThrow()
                tokenStore.save(response.apiKey, serverUrl, me.id).getOrThrow()
            }
    }

    fun clear(): Result<Unit> = tokenStore.clear()
}
