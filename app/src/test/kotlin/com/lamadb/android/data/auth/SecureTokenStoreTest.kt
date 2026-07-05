package com.lamadb.android.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SecureTokenStoreTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } returns Unit

        mockkStatic(MasterKey::class)
        mockkStatic(EncryptedSharedPreferences::class)
        every {
            EncryptedSharedPreferences.create(
                any<Context>(),
                any(),
                any<MasterKey>(),
                any(),
                any()
            )
        } returns prefs
    }

    @Test
    fun save_storesAllValues() {
        val store = SecureTokenStore(context)
        val result = store.save("key-123", "https://lamadb.test", "user-456")

        assertTrue(result.isSuccess)
        verify { editor.putString("api_key", "key-123") }
        verify { editor.putString("server_url", "https://lamadb.test") }
        verify { editor.putString("user_id", "user-456") }
        verify { editor.apply() }
    }

    @Test
    fun load_returnsCredentials_whenAllValuesPresent() {
        every { prefs.getString("api_key", null) } returns "key-123"
        every { prefs.getString("server_url", null) } returns "https://lamadb.test"
        every { prefs.getString("user_id", null) } returns "user-456"

        val store = SecureTokenStore(context)
        val result = store.load()

        assertTrue(result.isSuccess)
        val credentials = result.getOrNull()
        assertEquals("key-123", credentials?.apiKey)
        assertEquals("https://lamadb.test", credentials?.serverUrl)
        assertEquals("user-456", credentials?.userId)
    }

    @Test
    fun load_returnsNull_whenApiKeyMissing() {
        every { prefs.getString("api_key", null) } returns null
        every { prefs.getString("server_url", null) } returns "https://lamadb.test"
        every { prefs.getString("user_id", null) } returns "user-456"

        val result = SecureTokenStore(context).load()

        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrNull())
    }

    @Test
    fun clear_removesAllValues() {
        val result = SecureTokenStore(context).clear()

        assertTrue(result.isSuccess)
        verify { editor.remove("api_key") }
        verify { editor.remove("server_url") }
        verify { editor.remove("user_id") }
        verify { editor.apply() }
    }
}
