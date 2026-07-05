package com.lamadb.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lamadb.android.data.auth.AuthRepository
import com.lamadb.android.data.auth.SecureTokenStore
import com.lamadb.android.theme.LamaDBTheme
import com.lamadb.android.ui.auth.AuthState
import com.lamadb.android.ui.auth.AuthViewModel
import com.lamadb.android.ui.login.LoginScreen
import com.lamadb.android.ui.qr.QrScannerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LamaDBTheme {
                val viewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(
                        AuthRepository(SecureTokenStore(this@MainActivity))
                    )
                )
                val state by viewModel.state.collectAsState()

                when (state) {
                    AuthState.Checking -> {
                        // Splash / blank while checking auth.
                    }
                    AuthState.Login, is AuthState.Error -> {
                        var showScanner by remember { mutableStateOf(false) }
                        if (showScanner) {
                            QrScannerScreen(
                                viewModel = viewModel,
                                onBack = { showScanner = false }
                            )
                        } else {
                            LoginScreen(
                                viewModel = viewModel,
                                onScanQr = { showScanner = true }
                            )
                        }
                    }
                    AuthState.Authenticated -> {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            PlaceholderScreen(
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

class AuthViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun PlaceholderScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.placeholder_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.placeholder_body),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PlaceholderScreenPreview() {
    LamaDBTheme {
        PlaceholderScreen()
    }
}
