package com.lamadb.android.data.auth

import com.lamadb.android.data.api.LamaDBApiClient
import com.lamadb.android.data.api.MeResponse
import com.lamadb.android.data.api.ProvisionRequest
import com.lamadb.android.data.api.ProvisionResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private lateinit var store: SecureTokenStore
    private lateinit var apiClient: LamaDBApiClient
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        store = mockk(relaxed = true)
        apiClient = mockk(relaxed = true)
        repository = AuthRepository(store) { _, _ -> apiClient }
    }

    @Test
    fun validateAndSave_savesCredentials_whenApiMeSucceeds() = runTest {
        coEvery { apiClient.getMe() } returns Result.success(MeResponse("user-1", "Ali", null))
        every { store.save(any(), any(), any()) } returns Result.success(Unit)

        val result = repository.validateAndSave("key-123", "https://lamadb.test")

        assertTrue(result.isSuccess)
        verify { store.save("key-123", "https://lamadb.test", "user-1") }
    }

    @Test
    fun validateAndSave_doesNotSave_whenApiMeFails() = runTest {
        coEvery { apiClient.getMe() } returns Result.failure(RuntimeException("401"))

        val result = repository.validateAndSave("bad-key", "https://lamadb.test")

        assertTrue(result.isFailure)
        verify(exactly = 0) { store.save(any(), any(), any()) }
    }

    @Test
    fun provisionAndSave_exchangesTokenAndSaves() = runTest {
        coEvery { apiClient.provision(any()) } returns Result.success(
            ProvisionResponse("new-key", "user-1", null)
        )
        coEvery { apiClient.getMe() } returns Result.success(MeResponse("user-1", null, null))
        every { store.save(any(), any(), any()) } returns Result.success(Unit)

        val result = repository.provisionAndSave("qr-token", "https://lamadb.test")

        assertTrue(result.isSuccess)
        coVerify { apiClient.provision(ProvisionRequest("qr-token")) }
        verify { store.save("new-key", "https://lamadb.test", "user-1") }
    }

    @Test
    fun isAuthenticated_returnsTrue_whenCredentialsExist() {
        every { store.load() } returns Result.success(
            SecureTokenStore.StoredCredentials("key", "https://lamadb.test", "user-1")
        )

        assertTrue(repository.isAuthenticated())
    }

    @Test
    fun isAuthenticated_returnsFalse_whenCredentialsMissing() {
        every { store.load() } returns Result.success(null)

        assertFalse(repository.isAuthenticated())
    }

    @Test
    fun clear_delegatesToStore() {
        every { store.clear() } returns Result.success(Unit)

        val result = repository.clear()

        assertTrue(result.isSuccess)
        verify { store.clear() }
    }
}
