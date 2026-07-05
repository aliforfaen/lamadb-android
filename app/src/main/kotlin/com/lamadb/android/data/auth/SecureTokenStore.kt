package com.lamadb.android.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureTokenStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(apiKey: String, serverUrl: String, userId: String): Result<Unit> = runCatching {
        prefs.edit()
            .putString(KEY_API_KEY, apiKey)
            .putString(KEY_SERVER_URL, serverUrl)
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun load(): Result<StoredCredentials?> = runCatching {
        val apiKey = prefs.getString(KEY_API_KEY, null)
        val serverUrl = prefs.getString(KEY_SERVER_URL, null)
        val userId = prefs.getString(KEY_USER_ID, null)

        if (apiKey != null && serverUrl != null && userId != null) {
            StoredCredentials(apiKey, serverUrl, userId)
        } else {
            null
        }
    }

    fun clear(): Result<Unit> = runCatching {
        prefs.edit()
            .remove(KEY_API_KEY)
            .remove(KEY_SERVER_URL)
            .remove(KEY_USER_ID)
            .apply()
    }

    data class StoredCredentials(
        val apiKey: String,
        val serverUrl: String,
        val userId: String
    )

    companion object {
        private const val PREFS_FILE = "lamadb_auth"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USER_ID = "user_id"
    }
}
