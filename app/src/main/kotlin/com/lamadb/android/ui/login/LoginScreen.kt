package com.lamadb.android.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lamadb.android.BuildConfig
import com.lamadb.android.R
import com.lamadb.android.ui.auth.AuthViewModel
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.lamadb.android.data.auth.AuthRepository
import com.lamadb.android.data.auth.SecureTokenStore
import com.lamadb.android.theme.LamaDBTheme

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onScanQr: () -> Unit,
    modifier: Modifier = Modifier
) {
    var serverUrl by rememberSaveable { mutableStateOf("https://lamadb.tailnet") }
    var apiKey by rememberSaveable { mutableStateOf("") }
    val authState by viewModel.state.collectAsState()
    val isLoading = authState is com.lamadb.android.ui.auth.AuthState.Checking
    val errorMessage = (authState as? com.lamadb.android.ui.auth.AuthState.Error)?.message

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("LamaDB URL") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth().testTag("login_server_url")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API key") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth().testTag("login_api_key")
        )

        Spacer(modifier = Modifier.height(24.dp))

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.login(apiKey, serverUrl) },
            enabled = serverUrl.isNotBlank() && apiKey.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth().testTag("login_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Log in")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onScanQr,
            modifier = Modifier.fillMaxWidth().testTag("login_scan_qr_button")
        ) {
            Text("Scan QR code")
        }

        if (BuildConfig.LAMADB_TEST_URL.isNotBlank() && BuildConfig.LAMADB_TEST_API_KEY.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    serverUrl = BuildConfig.LAMADB_TEST_URL
                    apiKey = BuildConfig.LAMADB_TEST_API_KEY
                    viewModel.login(apiKey, serverUrl)
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Debug log in")
            }
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    val context = LocalContext.current
    val viewModel = remember {
        AuthViewModel(AuthRepository(SecureTokenStore(context)))
    }
    LamaDBTheme {
        LoginScreen(
            viewModel = viewModel,
            onScanQr = {}
        )
    }
}
