package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.messaging.FirebaseMessaging
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class FirebaseTokenOrigin {
    FIREBASE_AUTHENTICATION,
    FIREBASE_APP_CHECK,
    FIREBASE_CLOUD_MESSAGING,
    FIREBASE_CUSTOM_AUTH,
    NETWORK_OFFLINE,
    UNKNOWN
}

data class FirebaseConfigAudit(
    val projectId: String,
    val packageName: String,
    val senderId: String,
    val applicationId: String,
    val isProjectMatched: Boolean,
    val isPackageMatched: Boolean,
    val isSenderIdMatched: Boolean,
    val isAppIdMatched: Boolean
) {
    val isFullyAligned: Boolean
        get() = isProjectMatched && isPackageMatched && isSenderIdMatched && isAppIdMatched
}

data class TokenErrorDiagnostic(
    val origin: FirebaseTokenOrigin,
    val errorCode: String,
    val sanitizedSummary: String,
    val requiresForceRefresh: Boolean,
    val requiresTokenReset: Boolean,
    val isTransientNetworkError: Boolean = false
)

object FirebaseTokenManager {
    private const val TAG = "SkillConnectTokenMgr"

    const val EXPECTED_PROJECT_ID = "gen-lang-client-0778223582"
    const val EXPECTED_PACKAGE_NAME = "com.aistudio.skillconnect.market"
    const val EXPECTED_SENDER_ID = "800529463187"
    const val EXPECTED_APP_ID = "1:800529463187:android:e2715c4819dbe5b6201a66"

    private const val FCM_PREFS_NAME = "skillconnect_fcm_token_store"
    private const val KEY_FCM_TOKEN_DIGEST = "fcm_token_sha256"
    private const val KEY_FCM_UPDATED_AT = "fcm_token_updated_at"
    private const val KEY_LAST_VERIFIED_SENDER = "fcm_sender_id"

    private val UUID_EXACT_REGEX = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    )
    private val UUID_INLINE_REGEX = Regex(
        "\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b"
    )
    private val JWT_PATTERN = Regex("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")
    private val BEARER_PATTERN = Regex("(?i)Bearer\\s+[A-Za-z0-9._~+/-]+=*")

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var latestInMemoryFcmToken: String? = null

    @Volatile
    private var pendingFcmSyncOnReconnect: Boolean = false

    /**
     * Produces a non-reversible masked representation of any token or secret so logs never leak
     * raw credentials, JWTs, App Check secrets, or FCM registration tokens.
     */
    fun maskToken(token: String?): String {
        val cleaned = token?.trim().orEmpty()
        if (cleaned.isEmpty()) return "<empty>"
        val digest = sha256Hex(cleaned).take(8)
        return "tok[len=${cleaned.length},sha256=$digest]"
    }

    /**
     * Redacts any embedded JWT, Bearer token, or UUID secret from exception messages before logging.
     */
    fun sanitizeLogMessage(rawMessage: String?): String {
        if (rawMessage.isNullOrBlank()) return "No error details"
        return rawMessage
            .replace(JWT_PATTERN, "[REDACTED_JWT]")
            .replace(BEARER_PATTERN, "Bearer [REDACTED_TOKEN]")
            .replace(UUID_INLINE_REGEX, "[REDACTED_UUID]")
    }

    /**
     * Validates that a Firebase App Check debug secret is a properly formatted UUID and not
     * a literal "null", "undefined", or blank placeholder.
     */
    fun isValidDebugSecretFormat(secret: String?): Boolean {
        val trimmed = secret?.trim() ?: return false
        if (trimmed.isEmpty()) return false
        if (trimmed.equals("null", ignoreCase = true) || trimmed.equals("undefined", ignoreCase = true)) {
            return false
        }
        return UUID_EXACT_REGEX.matches(trimmed)
    }

    /**
     * Verifies that backend notification requests and FCM sender metadata match the configured
     * SkillConnect Firebase project and GCM sender ID.
     */
    fun validateNotificationSender(
        senderIdOrFrom: String?,
        projectId: String? = EXPECTED_PROJECT_ID
    ): Boolean {
        val normalizedSender = senderIdOrFrom?.trim().orEmpty()
        val normalizedProject = projectId?.trim().orEmpty()
        if (normalizedSender.isEmpty() || normalizedProject != EXPECTED_PROJECT_ID) {
            return false
        }
        return normalizedSender == EXPECTED_SENDER_ID ||
            normalizedSender.startsWith("/topics/")
    }

    /**
     * Audits the runtime Firebase configuration against the expected project ID, package name,
     * sender ID, and application ID.
     */
    fun verifyFirebaseConfiguration(context: Context): FirebaseConfigAudit {
        val app = try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseApp.getInstance()
        } catch (_: Exception) {
            null
        }

        val options = app?.options
        val actualProjectId = options?.projectId?.takeIf { it.isNotBlank() } ?: EXPECTED_PROJECT_ID
        val actualSenderId = options?.gcmSenderId?.takeIf { it.isNotBlank() } ?: EXPECTED_SENDER_ID
        val actualAppId = options?.applicationId?.takeIf { it.isNotBlank() } ?: EXPECTED_APP_ID
        val actualPackageName = context.packageName

        val audit = FirebaseConfigAudit(
            projectId = actualProjectId,
            packageName = actualPackageName,
            senderId = actualSenderId,
            applicationId = actualAppId,
            isProjectMatched = actualProjectId == EXPECTED_PROJECT_ID,
            isPackageMatched = actualPackageName == EXPECTED_PACKAGE_NAME,
            isSenderIdMatched = actualSenderId == EXPECTED_SENDER_ID,
            isAppIdMatched = actualAppId == EXPECTED_APP_ID
        )

        if (audit.isFullyAligned) {
            Log.d(
                TAG,
                "Firebase configuration verified: projectId=${audit.projectId}, " +
                    "package=${audit.packageName}, senderId=${audit.senderId}"
            )
        } else {
            Log.e(
                TAG,
                "Firebase configuration mismatch detected: projectMatched=${audit.isProjectMatched}, " +
                    "packageMatched=${audit.isPackageMatched}, senderMatched=${audit.isSenderIdMatched}, " +
                    "appIdMatched=${audit.isAppIdMatched}"
            )
        }
        return audit
    }

    /**
     * Classifies a Firebase exception to pinpoint whether an error originated from
     * offline network conditions, Firebase Auth, Firebase App Check, FCM, or Custom Auth.
     * Prevents offline/network failures from triggering unnecessary token invalidation!
     */
    fun classifyTokenError(
        throwable: Throwable?,
        isOnline: Boolean = NetworkConnectivityMonitor.isCurrentlyConnected()
    ): TokenErrorDiagnostic {
        if (throwable == null) {
            return TokenErrorDiagnostic(
                origin = FirebaseTokenOrigin.UNKNOWN,
                errorCode = "NONE",
                sanitizedSummary = "No error",
                requiresForceRefresh = false,
                requiresTokenReset = false,
                isTransientNetworkError = false
            )
        }

        val causes = generateSequence(throwable) { it.cause }.toList()
        val combinedMessage = causes.joinToString(" | ") { it.message.orEmpty() }
        val sanitized = sanitizeLogMessage(combinedMessage)

        val firestoreEx = causes.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        val isNetworkException = !isOnline ||
            causes.any { it is FirebaseNetworkException || it is IOException } ||
            (firestoreEx != null && firestoreEx.code == FirebaseFirestoreException.Code.UNAVAILABLE) ||
            combinedMessage.contains("SERVICE_NOT_AVAILABLE", ignoreCase = true) ||
            combinedMessage.contains("network error", ignoreCase = true) ||
            combinedMessage.contains("Unable to resolve host", ignoreCase = true) ||
            combinedMessage.contains("Failed to get document because the client is offline", ignoreCase = true)

        if (isNetworkException) {
            return TokenErrorDiagnostic(
                origin = FirebaseTokenOrigin.NETWORK_OFFLINE,
                errorCode = if (!isOnline) "CLIENT_OFFLINE" else "NETWORK_UNAVAILABLE",
                sanitizedSummary = sanitized,
                requiresForceRefresh = false,
                requiresTokenReset = false,
                isTransientNetworkError = true
            )
        }

        val authEx = causes.filterIsInstance<FirebaseAuthException>().firstOrNull()
        if (authEx != null) {
            val code = authEx.errorCode
            val isCustomTokenError = code == "ERROR_INVALID_CUSTOM_TOKEN" ||
                code == "ERROR_CUSTOM_TOKEN_MISMATCH"
            val origin = if (isCustomTokenError) {
                FirebaseTokenOrigin.FIREBASE_CUSTOM_AUTH
            } else {
                FirebaseTokenOrigin.FIREBASE_AUTHENTICATION
            }
            val needsRefresh = authEx is FirebaseAuthInvalidUserException ||
                authEx is FirebaseAuthInvalidCredentialsException ||
                code == "ERROR_USER_TOKEN_EXPIRED" ||
                code == "ERROR_INVALID_USER_TOKEN"
            return TokenErrorDiagnostic(
                origin = origin,
                errorCode = code,
                sanitizedSummary = sanitized,
                requiresForceRefresh = needsRefresh,
                requiresTokenReset = isCustomTokenError,
                isTransientNetworkError = false
            )
        }

        val isAppCheckError = combinedMessage.contains("AppCheck", ignoreCase = true) ||
            combinedMessage.contains("app attestation", ignoreCase = true) ||
            combinedMessage.contains("DebugAppCheckProvider", ignoreCase = true) ||
            combinedMessage.contains("Recaptcha", ignoreCase = true) ||
            combinedMessage.contains("App Check token is invalid", ignoreCase = true)

        if (isAppCheckError) {
            return TokenErrorDiagnostic(
                origin = FirebaseTokenOrigin.FIREBASE_APP_CHECK,
                errorCode = "APP_CHECK_TOKEN_INVALID",
                sanitizedSummary = sanitized,
                requiresForceRefresh = true,
                requiresTokenReset = true,
                isTransientNetworkError = false
            )
        }

        val isFcmError = combinedMessage.contains("FIS_AUTH_ERROR", ignoreCase = true) ||
            combinedMessage.contains("INVALID_PARAMETERS", ignoreCase = true) ||
            combinedMessage.contains("UNREGISTERED", ignoreCase = true) ||
            combinedMessage.contains("SENDER_ID_MISMATCH", ignoreCase = true) ||
            combinedMessage.contains("MISSING_INSTANCEID_SERVICE", ignoreCase = true) ||
            combinedMessage.contains("registration token", ignoreCase = true)

        if (isFcmError) {
            return TokenErrorDiagnostic(
                origin = FirebaseTokenOrigin.FIREBASE_CLOUD_MESSAGING,
                errorCode = "FCM_TOKEN_INVALID",
                sanitizedSummary = sanitized,
                requiresForceRefresh = true,
                requiresTokenReset = true,
                isTransientNetworkError = false
            )
        }

        if (firestoreEx != null && firestoreEx.code == FirebaseFirestoreException.Code.UNAUTHENTICATED) {
            return TokenErrorDiagnostic(
                origin = FirebaseTokenOrigin.FIREBASE_AUTHENTICATION,
                errorCode = firestoreEx.code.name,
                sanitizedSummary = sanitized,
                requiresForceRefresh = true,
                requiresTokenReset = false,
                isTransientNetworkError = false
            )
        }

        return TokenErrorDiagnostic(
            origin = FirebaseTokenOrigin.UNKNOWN,
            errorCode = throwable.javaClass.simpleName,
            sanitizedSummary = sanitized,
            requiresForceRefresh = combinedMessage.contains("token", ignoreCase = true) ||
                combinedMessage.contains("expired", ignoreCase = true) ||
                combinedMessage.contains("invalid", ignoreCase = true),
            requiresTokenReset = false,
            isTransientNetworkError = false
        )
    }

    /**
     * Ensures the current Firebase Authentication user has a valid ID token.
     * When the device is offline, strictly uses the cached token (`getIdToken(false)`) and skips
     * network force-refresh to avoid unnecessary token invalidation errors.
     */
    suspend fun ensureValidAuthIdToken(forceRefresh: Boolean = false): Result<String> {
        val currentUser = try {
            Firebase.auth.currentUser
        } catch (e: Exception) {
            return Result.failure(e)
        } ?: return Result.failure(IllegalStateException("No authenticated Firebase user session."))

        val isOnline = NetworkConnectivityMonitor.isCurrentlyConnected()
        val effectiveForceRefresh = forceRefresh && isOnline

        if (forceRefresh && !isOnline) {
            Log.d(TAG, "Device is offline; using cached Firebase Auth ID token instead of network force-refresh.")
        }

        return try {
            val tokenResult = currentUser.getIdToken(effectiveForceRefresh).await()
            val token = tokenResult.token
            if (!token.isNullOrBlank()) {
                Log.d(
                    TAG,
                    "Verified Firebase Auth ID token (${maskToken(token)}, forceRefresh=$effectiveForceRefresh, online=$isOnline)"
                )
                Result.success(token)
            } else if (!effectiveForceRefresh && isOnline) {
                Log.w(TAG, "Cached Firebase Auth ID token was blank; retrying with forceRefresh=true")
                val refreshed = currentUser.getIdToken(true).await().token.orEmpty()
                if (refreshed.isNotBlank()) {
                    Log.d(TAG, "Force-refreshed Firebase Auth ID token (${maskToken(refreshed)})")
                    Result.success(refreshed)
                } else {
                    Result.failure(IllegalStateException("Firebase Auth returned an empty ID token after refresh."))
                }
            } else {
                Result.failure(IllegalStateException("Firebase Auth ID token unavailable (online=$isOnline)."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val diagnostic = classifyTokenError(e, isOnline)
            Log.w(
                TAG,
                "Auth token check warning [origin=${diagnostic.origin}, code=${diagnostic.errorCode}, online=$isOnline]: ${diagnostic.sanitizedSummary}"
            )
            if (!effectiveForceRefresh && isOnline && !diagnostic.isTransientNetworkError) {
                try {
                    val retryResult = currentUser.getIdToken(true).await()
                    val retryToken = retryResult.token.orEmpty()
                    if (retryToken.isNotBlank()) {
                        Log.d(TAG, "Recovered Auth ID token via force refresh (${maskToken(retryToken)})")
                        return Result.success(retryToken)
                    }
                } catch (retryEx: CancellationException) {
                    throw retryEx
                } catch (retryEx: Exception) {
                    val retryDiag = classifyTokenError(retryEx, isOnline)
                    Log.e(
                        TAG,
                        "Force refresh of Auth ID token failed [code=${retryDiag.errorCode}]: ${retryDiag.sanitizedSummary}"
                    )
                    return Result.failure(retryEx)
                }
            }
            Result.failure(e)
        }
    }

    /**
     * Verifies and refreshes the Firebase App Check token when online, or uses the cached
     * App Check token when offline to prevent unnecessary attestation failures.
     */
    suspend fun ensureValidAppCheckToken(forceRefresh: Boolean = false): Result<String> {
        val isOnline = NetworkConnectivityMonitor.isCurrentlyConnected()
        val effectiveForceRefresh = forceRefresh && isOnline

        if (forceRefresh && !isOnline) {
            Log.d(TAG, "Device is offline; skipping App Check force-refresh to preserve cached token.")
        }

        return try {
            val tokenResult = FirebaseAppCheck.getInstance().getAppCheckToken(effectiveForceRefresh).await()
            val token = tokenResult.token
            if (token.isNotBlank()) {
                Log.d(TAG, "Verified App Check token (${maskToken(token)}, forceRefresh=$effectiveForceRefresh)")
                Result.success(token)
            } else if (!effectiveForceRefresh && isOnline) {
                val refreshed = FirebaseAppCheck.getInstance().getAppCheckToken(true).await().token
                Log.d(TAG, "Force-refreshed App Check token (${maskToken(refreshed)})")
                Result.success(refreshed)
            } else {
                Result.failure(IllegalStateException("App Check returned an empty token."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val diag = classifyTokenError(e, isOnline)
            Log.w(
                TAG,
                "App Check token request warning [origin=${diag.origin}, code=${diag.errorCode}, online=$isOnline]: ${diag.sanitizedSummary}"
            )
            if (!effectiveForceRefresh && isOnline && !diag.isTransientNetworkError) {
                return try {
                    val refreshed = FirebaseAppCheck.getInstance().getAppCheckToken(true).await().token
                    Log.d(TAG, "Recovered App Check token on retry (${maskToken(refreshed)})")
                    Result.success(refreshed)
                } catch (retryEx: CancellationException) {
                    throw retryEx
                } catch (retryEx: Exception) {
                    val retryDiag = classifyTokenError(retryEx, isOnline)
                    Log.w(TAG, "App Check fallback token warning: ${retryDiag.sanitizedSummary}")
                    Result.failure(retryEx)
                }
            }
            Result.failure(e)
        }
    }

    /**
     * Fetches the current FCM registration token, recovering via `deleteToken()` + re-registration
     * only when online and the token is genuinely invalid, and synchronizes it with Firestore.
     */
    @Suppress("DEPRECATION")
    suspend fun fetchAndSyncFcmToken(
        context: Context,
        firestoreRepository: SkillConnectFirestoreRepository? = null,
        forceReset: Boolean = false
    ): Result<String> {
        val isOnline = NetworkConnectivityMonitor.isCurrentlyConnected(context)
        if (!isOnline) {
            pendingFcmSyncOnReconnect = true
            val cached = latestInMemoryFcmToken
            Log.d(TAG, "Device is offline; deferring FCM token network sync until connectivity returns.")
            return if (!cached.isNullOrBlank()) {
                Result.success(cached)
            } else {
                Result.failure(IllegalStateException("Device is offline; FCM token sync deferred."))
            }
        }

        return try {
            if (forceReset) {
                try {
                    FirebaseMessaging.getInstance().deleteToken().await()
                    Log.d(TAG, "Cleared stale FCM registration token before regeneration.")
                } catch (delEx: Exception) {
                    Log.w(TAG, "FCM deleteToken warning: ${sanitizeLogMessage(delEx.message)}")
                }
            }

            val token = FirebaseMessaging.getInstance().token.await()
            if (token.isNullOrBlank()) {
                return Result.failure(IllegalStateException("FCM returned an empty registration token."))
            }

            cacheFcmTokenMetadata(context, token)
            pendingFcmSyncOnReconnect = false
            Log.d(TAG, "FCM registration token active (${maskToken(token)}) for sender=$EXPECTED_SENDER_ID")

            val activeUid = try {
                Firebase.auth.currentUser?.uid
            } catch (_: Exception) {
                null
            }
            if (!activeUid.isNullOrBlank() && firestoreRepository != null) {
                firestoreRepository.syncFcmToken(token)
            }
            Result.success(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val diag = classifyTokenError(e, isOnline)
            Log.w(
                TAG,
                "FCM token retrieval issue [origin=${diag.origin}, code=${diag.errorCode}, online=$isOnline]: ${diag.sanitizedSummary}"
            )
            if (!forceReset && isOnline && diag.requiresTokenReset && !diag.isTransientNetworkError) {
                return try {
                    FirebaseMessaging.getInstance().deleteToken().await()
                    val recoveredToken = FirebaseMessaging.getInstance().token.await()
                    if (!recoveredToken.isNullOrBlank()) {
                        cacheFcmTokenMetadata(context, recoveredToken)
                        pendingFcmSyncOnReconnect = false
                        Log.d(TAG, "Recovered FCM token after reset (${maskToken(recoveredToken)})")
                        val activeUid = try {
                            Firebase.auth.currentUser?.uid
                        } catch (_: Exception) {
                            null
                        }
                        if (!activeUid.isNullOrBlank() && firestoreRepository != null) {
                            firestoreRepository.syncFcmToken(recoveredToken)
                        }
                        Result.success(recoveredToken)
                    } else {
                        Result.failure(e)
                    }
                } catch (resetEx: CancellationException) {
                    throw resetEx
                } catch (resetEx: Exception) {
                    Log.w(TAG, "FCM token recovery failed: ${sanitizeLogMessage(resetEx.message)}")
                    Result.failure(resetEx)
                }
            }
            Result.failure(e)
        }
    }

    fun cacheFcmTokenMetadata(context: Context, token: String) {
        val cleaned = token.trim()
        if (cleaned.isEmpty()) return
        latestInMemoryFcmToken = cleaned
        try {
            val prefs = context.getSharedPreferences(FCM_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_FCM_TOKEN_DIGEST, sha256Hex(cleaned))
                .putLong(KEY_FCM_UPDATED_AT, System.currentTimeMillis())
                .putString(KEY_LAST_VERIFIED_SENDER, EXPECTED_SENDER_ID)
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache FCM token metadata: ${sanitizeLogMessage(e.message)}")
        }
    }

    fun getCachedFcmTokenDigest(context: Context): String? {
        return try {
            val prefs = context.getSharedPreferences(FCM_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString(KEY_FCM_TOKEN_DIGEST, null)
        } catch (_: Exception) {
            null
        }
    }

    fun getLatestInMemoryFcmToken(): String? = latestInMemoryFcmToken

    fun hasPendingFcmSyncOnReconnect(): Boolean = pendingFcmSyncOnReconnect

    /**
     * Runs startup verification of Firebase project/package/sender IDs, registers the
     * [NetworkConnectivityMonitor], and coordinates token verification when online or upon reconnect.
     */
    fun initializeAndVerify(
        context: Context,
        firestoreRepository: SkillConnectFirestoreRepository? = null
    ) {
        val appContext = context.applicationContext ?: context
        verifyFirebaseConfiguration(appContext)

        NetworkConnectivityMonitor.register(appContext) {
            backgroundScope.launch {
                Log.d(TAG, "Connectivity restored; validating tokens and flushing pending Firestore sync.")
                ensureValidAppCheckToken(forceRefresh = false)
                val currentUser = try {
                    Firebase.auth.currentUser
                } catch (_: Exception) {
                    null
                }
                if (currentUser != null) {
                    ensureValidAuthIdToken(forceRefresh = false)
                    firestoreRepository?.flushPendingOfflineSync()
                }
                if (pendingFcmSyncOnReconnect || latestInMemoryFcmToken == null) {
                    fetchAndSyncFcmToken(appContext, firestoreRepository, forceReset = false)
                } else {
                    latestInMemoryFcmToken?.let { token ->
                        if (currentUser != null) {
                            firestoreRepository?.syncFcmToken(token)
                        }
                    }
                }
            }
        }

        if (NetworkConnectivityMonitor.isCurrentlyConnected(appContext)) {
            backgroundScope.launch {
                ensureValidAppCheckToken(forceRefresh = false)
                val currentUser = try {
                    Firebase.auth.currentUser
                } catch (_: Exception) {
                    null
                }
                if (currentUser != null) {
                    ensureValidAuthIdToken(forceRefresh = false)
                }
                fetchAndSyncFcmToken(appContext, firestoreRepository, forceReset = false)
            }
        } else {
            pendingFcmSyncOnReconnect = true
            Log.d(TAG, "Startup detected offline state; deferring token refresh until network is available.")
        }
    }

    private fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
