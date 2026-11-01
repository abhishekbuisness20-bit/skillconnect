package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

/**
 * Checks whether the device currently has an active, validated internet connection.
 */
fun isNetworkConnectedAndValidated(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    return hasInternet && isValidated
}

@Stable
class NetworkConnectivityState(
    isInitiallyOnline: Boolean,
    private val onRecheckRequest: () -> Boolean
) {
    var isOnline by mutableStateOf(isInitiallyOnline)
        internal set

    var showBackOnlineBanner by mutableStateOf(false)
        internal set

    internal var hasBeenOffline by mutableStateOf(!isInitiallyOnline)

    fun updateOnlineStatus(online: Boolean) {
        if (!online) {
            hasBeenOffline = true
            showBackOnlineBanner = false
            isOnline = false
        } else {
            val wasOffline = !isOnline || hasBeenOffline
            isOnline = true
            if (wasOffline) {
                hasBeenOffline = false
                showBackOnlineBanner = true
            }
        }
    }

    fun dismissBackOnlineBanner() {
        showBackOnlineBanner = false
    }

    /**
     * Manually re-evaluates the current active network capabilities and returns true if online.
     */
    fun recheckNow(): Boolean {
        val currentOnline = onRecheckRequest()
        updateOnlineStatus(currentOnline)
        return currentOnline
    }
}

/**
 * Remembers and observes real-time internet connectivity changes via [ConnectivityManager.NetworkCallback],
 * tracking both offline states and brief back-online recovery notifications.
 */
@Composable
fun rememberNetworkConnectivityState(
    backOnlineDisplayDurationMs: Long = 2800L
): NetworkConnectivityState {
    val context = LocalContext.current
    val state = remember(context) {
        NetworkConnectivityState(
            isInitiallyOnline = isNetworkConnectedAndValidated(context),
            onRecheckRequest = { isNetworkConnectedAndValidated(context) }
        )
    }

    DisposableEffect(context) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            onDispose { }
        } else {
            // Ensure initial state is synchronized upon attaching observer
            state.updateOnlineStatus(isNetworkConnectedAndValidated(context))

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    val isOnlineNow = capabilities != null &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    if (isOnlineNow) {
                        state.updateOnlineStatus(true)
                    } else {
                        state.updateOnlineStatus(isNetworkConnectedAndValidated(context))
                    }
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    val isValidated =
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    if (isValidated) {
                        state.updateOnlineStatus(true)
                    } else {
                        state.updateOnlineStatus(isNetworkConnectedAndValidated(context))
                    }
                }

                override fun onLost(network: Network) {
                    state.updateOnlineStatus(isNetworkConnectedAndValidated(context))
                }

                override fun onUnavailable() {
                    state.updateOnlineStatus(false)
                }
            }

            try {
                connectivityManager.registerDefaultNetworkCallback(callback)
            } catch (_: Exception) {
                try {
                    val request = NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()
                    connectivityManager.registerNetworkCallback(request, callback)
                } catch (_: Exception) {
                    // Fallback relies on manual recheck if callback registration fails
                }
            }

            onDispose {
                try {
                    connectivityManager.unregisterNetworkCallback(callback)
                } catch (_: Exception) {
                }
            }
        }
    }

    LaunchedEffect(state.showBackOnlineBanner) {
        if (state.showBackOnlineBanner) {
            delay(backOnlineDisplayDurationMs)
            state.dismissBackOnlineBanner()
        }
    }

    return state
}
