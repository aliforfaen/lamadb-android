package com.lamadb.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lamadb.android.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Checking)
    val state: StateFlow<AuthState> = _state

    fun checkAuth() {
        _state.value = if (repository.isAuthenticated()) {
            AuthState.Authenticated
        } else {
            AuthState.Login
        }
    }

    fun login(apiKey: String, serverUrl: String) {
        _state.value = AuthState.Checking
        viewModelScope.launch {
            val result = repository.validateAndSave(apiKey, serverUrl)
            _state.value = if (result.isSuccess) {
                AuthState.Authenticated
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun loginWithQrToken(token: String, serverUrl: String) {
        _state.value = AuthState.Checking
        viewModelScope.launch {
            val result = repository.provisionAndSave(token, serverUrl)
            _state.value = if (result.isSuccess) {
                AuthState.Authenticated
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "QR login failed")
            }
        }
    }

    fun logout() {
        repository.clear()
        _state.value = AuthState.Login
    }
}
