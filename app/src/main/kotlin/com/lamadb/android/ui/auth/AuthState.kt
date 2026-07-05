package com.lamadb.android.ui.auth

sealed class AuthState {
    data object Checking : AuthState()
    data object Login : AuthState()
    data object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}
