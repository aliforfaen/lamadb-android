package com.lamadb.android.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observes the device's network connectivity and emits a [ConnectionState].
 *
 * The initial value is determined synchronously from the active network; subsequent
 * values are delivered via [ConnectivityManager.NetworkCallback].
 */
class ConnectivityObserver(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun observe(): Flow<ConnectionState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(ConnectionState.Available)
            }

            override fun onLost(network: Network) {
                trySend(ConnectionState.Unavailable)
            }

            override fun onUnavailable() {
                trySend(ConnectionState.Unavailable)
            }
        }

        val request = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // Emit the current state before registering for updates.
        trySend(currentState())
        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    fun currentState(): ConnectionState {
        val network = connectivityManager.activeNetwork ?: return ConnectionState.Unavailable
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
            ConnectionState.Available
        } else {
            ConnectionState.Unavailable
        }
    }
}

sealed class ConnectionState {
    data object Available : ConnectionState()
    data object Unavailable : ConnectionState()
}
