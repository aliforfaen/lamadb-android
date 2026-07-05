package com.lamadb.android.ui.auth

import com.lamadb.android.data.auth.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: AuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = AuthViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun checkAuth_emitsAuthenticated_whenCredentialsExist() {
        every { repository.isAuthenticated() } returns true

        viewModel.checkAuth()

        assertTrue(viewModel.state.value is AuthState.Authenticated)
    }

    @Test
    fun checkAuth_emitsLogin_whenCredentialsMissing() {
        every { repository.isAuthenticated() } returns false

        viewModel.checkAuth()

        assertTrue(viewModel.state.value is AuthState.Login)
    }

    @Test
    fun login_emitsAuthenticated_onSuccess() = runTest {
        coEvery { repository.validateAndSave(any(), any()) } returns Result.success(Unit)

        viewModel.login("key", "https://lamadb.test")

        assertTrue(viewModel.state.value is AuthState.Authenticated)
    }

    @Test
    fun login_emitsError_onFailure() = runTest {
        coEvery { repository.validateAndSave(any(), any()) } returns Result.failure(RuntimeException("bad"))

        viewModel.login("key", "https://lamadb.test")

        assertTrue(viewModel.state.value is AuthState.Error)
    }

    @Test
    fun logout_callsClearAndEmitsLogin() {
        every { repository.clear() } returns Result.success(Unit)

        viewModel.logout()

        verify { repository.clear() }
        assertTrue(viewModel.state.value is AuthState.Login)
    }
}
