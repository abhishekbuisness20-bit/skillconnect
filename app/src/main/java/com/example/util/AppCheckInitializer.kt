package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

private const val TAG = "AppCheckInit"

// App Check contract keys defined by the Firebase SDK and runtime harness.
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"

/**
 * Initializes Firebase App Check to protect backend resources (Firestore, Cloud Storage,
 * and Cloud Functions) from abuse by ensuring requests originate from legitimate instances
 * of the SkillConnect application.
 *
 * - Release builds (`!BuildConfig.DEBUG`): Installs [PlayIntegrityAppCheckProviderFactory]
 *   to attest device and application integrity via Google Play Integrity API.
 * - Debug builds (`BuildConfig.DEBUG`): Reads `FIREBASE_APPCHECK_DEBUG_TOKEN` from the launch
 *   [Intent] (persisted synchronously with `.commit()`) and installs [DebugAppCheckProviderFactory]
 *   for emulator and development environments.
 */
fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        // 1. Ensure default FirebaseApp is initialized before configuring App Check
        val appContext = context.applicationContext
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext)
        }

        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.setTokenAutoRefreshEnabled(true)

        if (BuildConfig.DEBUG) {
            // 2. Debug build: Persist debug token synchronously before installing Debug provider
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = appContext.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                // CRITICAL: Must use synchronous commit() so the debug secret is persisted
                // before DebugAppCheckProviderFactory reads SharedPreferences.
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra; using generated debug token.")
            }

            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            // 3. Release build: Enforce hardware-backed attestation via Google Play Integrity
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "PlayIntegrityAppCheckProviderFactory installed.")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}
