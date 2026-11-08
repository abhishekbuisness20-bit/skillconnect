package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors real-time Android network connectivity using [ConnectivityManager.NetworkCallback]
 * so that Firebase Authentication, App Check, FCM, and Firestore operations gracefully handle
 * offline states without triggering unnecessary token invalidation or force-refresh failures.
 */
object NetworkConnectivityMonitor {
    private const val TAG = "SkillConnectNetMonitor"

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    @Volatile
    private var simulatedOnlineOverride: Boolean? = null

    @Volatile
    private var registeredConnectivityManager: ConnectivityManager? = null

    @Volatile
    private var activeCallback: ConnectivityManager.NetworkCallback? = null

    private val reconnectListeners = CopyOnWriteArrayList<() -> Unit>()

    /**
     * Returns whether the application currently has network connectivity.
     * Respects [setSimulatedOnlineStateForTesting] when set in unit tests.
     */
    fun isCurrentlyConnected(context: Context? = null): Boolean {
        simulatedOnlineOverride?.let { return it }
        if (context != null) {
            val evaluated = evaluateContextConnectivity(context)
            _isOnline.value = evaluated
            return evaluated
        }
        return _isOnline.value
    }

    /**
     * Allows unit and Robolectric tests to deterministically simulate offline/online transitions.
     * Passing `null` clears the test override and restores system connectivity tracking.
     */
    fun setSimulatedOnlineStateForTesting(online: Boolean?) {
        val previous = isCurrentlyConnected()
        simulatedOnlineOverride = online
        val current = online ?: true
        _isOnline.value = current
        if (!previous && current) {
            Log.d(TAG, "Simulated connectivity restored (Offline -> Online); notifying listeners.")
            notifyReconnectListeners()
        }
    }

    fun addOnReconnectListener(listener: () -> Unit) {
        if (!reconnectListeners.contains(listener)) {
            reconnectListeners.add(listener)
        }
    }

    fun removeOnReconnectListener(listener: () -> Unit) {
        reconnectListeners.remove(listener)
    }

    /**
     * Registers a system [ConnectivityManager.NetworkCallback] to track live connectivity changes
     * and invoke [onReconnected] when the device transitions from offline back to online.
     */
    fun register(context: Context, onReconnected: (() -> Unit)? = null) {
        onReconnected?.let { addOnReconnectListener(it) }

        val appContext = context.applicationContext ?: context
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return

        val initialOnline = simulatedOnlineOverride ?: evaluateContextConnectivity(appContext)
        _isOnline.value = initialOnline

        if (activeCallback != null && registeredConnectivityManager === cm) {
            return
        }

        unregister()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateOnlineState(true, "Network available")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                updateOnlineState(hasInternet, "Capabilities changed (hasInternet=$hasInternet)")
            }

            override fun onLost(network: Network) {
                val stillConnected = cm.activeNetwork != null && evaluateConnectivityManager(cm)
                updateOnlineState(stillConnected, "Network lost (fallbackConnected=$stillConnected)")
            }

            override fun onUnavailable() {
                updateOnlineState(false, "Network unavailable")
            }
        }

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, callback)
            registeredConnectivityManager = cm
            activeCallback = callback
            Log.d(TAG, "NetworkConnectivityMonitor registered (initialOnline=$initialOnline).")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register NetworkCallback: ${e.message}")
        }
    }

    fun unregister() {
        val cm = registeredConnectivityManager
        val cb = activeCallback
        if (cm != null && cb != null) {
            try {
                cm.unregisterNetworkCallback(cb)
            } catch (_: Exception) {
            }
        }
        registeredConnectivityManager = null
        activeCallback = null
    }

    private fun updateOnlineState(newOnlineState: Boolean, reason: String) {
        if (simulatedOnlineOverride != null) return
        val wasOnline = _isOnline.value
        if (wasOnline != newOnlineState) {
            _isOnline.value = newOnlineState
            Log.d(TAG, "Network connectivity state changed: $wasOnline -> $newOnlineState ($reason)")
            if (!wasOnline && newOnlineState) {
                notifyReconnectListeners()
            }
        }
    }

    private fun notifyReconnectListeners() {
        for (listener in reconnectListeners) {
            try {
                listener.invoke()
            } catch (e: Exception) {
                Log.w(TAG, "Reconnect listener error: ${e.message}")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun evaluateContextConnectivity(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        return evaluateConnectivityManager(cm)
    }

    @Suppress("DEPRECATION")
    private fun evaluateConnectivityManager(cm: ConnectivityManager): Boolean {
        return try {
            val activeNet = cm.activeNetwork
            if (activeNet != null) {
                val caps = cm.getNetworkCapabilities(activeNet)
                if (caps != null) {
                    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
            }
            val legacyInfo = cm.activeNetworkInfo
            if (legacyInfo != null) {
                return legacyInfo.isConnectedOrConnecting
            }
            true
        } catch (_: Exception) {
            true
        }
    }
}
