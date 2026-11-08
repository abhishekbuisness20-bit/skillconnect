package com.example.ui.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.example.data.FirebaseTokenManager
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        val appCheck = FirebaseAppCheck.getInstance()
        appCheck.setTokenAutoRefreshEnabled(true)

        if (BuildConfig.DEBUG) {
            val persistenceKey = FirebaseApp.getInstance().persistenceKey
            val prefs = context.getSharedPreferences(
                "$DEBUG_STORE_PREFIX$persistenceKey",
                Context.MODE_PRIVATE
            )

            val rawIntentToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)?.trim()
            if (FirebaseTokenManager.isValidDebugSecretFormat(rawIntentToken)) {
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, rawIntentToken)
                    .commit()
                Log.d(
                    TAG,
                    "App Check debug secret updated from launch intent (${FirebaseTokenManager.maskToken(rawIntentToken)})."
                )
            } else {
                if (!rawIntentToken.isNullOrEmpty()) {
                    Log.w(
                        TAG,
                        "Ignored malformed FIREBASE_APPCHECK_DEBUG_TOKEN intent extra; validating stored secret."
                    )
                }
                val existingStoredSecret = prefs.getString(DEBUG_SECRET_KEY, null)
                if (existingStoredSecret != null && !FirebaseTokenManager.isValidDebugSecretFormat(existingStoredSecret)) {
                    prefs.edit().remove(DEBUG_SECRET_KEY).commit()
                    Log.w(
                        TAG,
                        "Cleared corrupted App Check debug secret from SharedPreferences for auto-regeneration."
                    )
                }
            }

            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed with token auto-refresh enabled.")
        } else {
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            if (resId != 0) {
                val siteKey = context.getString(resId).trim()
                if (siteKey.isNotBlank() && !siteKey.equals("null", ignoreCase = true)) {
                    appCheck.installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed with token auto-refresh enabled.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank or invalid.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        Log.e(
            TAG,
            "Failed to initialize Firebase App Check: ${FirebaseTokenManager.sanitizeLogMessage(e.message)}"
        )
    }
}
