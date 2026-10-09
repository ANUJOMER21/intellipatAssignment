package com.example.intellipatassignment.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import android.util.Log
import kotlinx.coroutines.flow.onEach

interface ConnectivityObserver {
    val isOnline: Flow<Boolean>
    fun isCurrentlyOnline(): Boolean
}

class AndroidConnectivityObserver(context: Context) : ConnectivityObserver {
    private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun isCurrentlyOnline(): Boolean {
        val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(isCurrentlyOnline()) }
            override fun onLost(network: Network) { trySend(isCurrentlyOnline()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(isCurrentlyOnline())
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        manager.registerNetworkCallback(request, callback)
        trySend(isCurrentlyOnline())
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged().onEach { Log.d(TAG, "Network online = $it") }

    private companion object {
        const val TAG = "Connectivity"
    }
}
