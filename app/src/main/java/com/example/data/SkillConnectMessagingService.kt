package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SkillConnectMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Suppress("DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (token.isBlank()) {
            Log.w(TAG, "Received blank FCM token in onNewToken; ignoring.")
            return
        }

        Log.d(
            TAG,
            "FCM onNewToken received (${FirebaseTokenManager.maskToken(token)}) " +
                "for project=${FirebaseTokenManager.EXPECTED_PROJECT_ID}"
        )
        FirebaseTokenManager.cacheFcmTokenMetadata(applicationContext, token)

        val currentUser = try {
            Firebase.auth.currentUser
        } catch (_: Exception) {
            null
        }

        if (currentUser != null) {
            serviceScope.launch {
                try {
                    val repo = SkillConnectFirestoreRepository(applicationContext)
                    repo.syncFcmToken(token)
                } catch (e: Exception) {
                    Log.w(
                        TAG,
                        "Deferred FCM token sync until next authenticated session: " +
                            FirebaseTokenManager.sanitizeLogMessage(e.message)
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val sender = message.from ?: message.senderId
        if (!FirebaseTokenManager.validateNotificationSender(sender)) {
            Log.w(
                TAG,
                "Ignored FCM message from unrecognized sender/project (expected sender=${FirebaseTokenManager.EXPECTED_SENDER_ID})."
            )
            return
        }

        Log.d(
            TAG,
            "Verified FCM message received from valid project sender=${FirebaseTokenManager.EXPECTED_SENDER_ID}, " +
                "messageId=${message.messageId ?: "none"}, dataKeys=${message.data.keys}"
        )
    }

    companion object {
        private const val TAG = "SkillConnectFCM"
    }
}
