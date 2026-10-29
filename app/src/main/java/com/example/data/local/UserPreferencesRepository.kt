package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "skillconnect_prefs")

data class UserPreferences(
    val isDarkMode: Boolean = false,
    val language: String = "en", // "en" or "hi"
    val pendingRegistrationRole: String = "", // "" (not selected yet), "customer", or "provider"
    val selectedState: String = "",
    val selectedCity: String = "All India",
    val selectedLocality: String = "",
    val selectedPinCode: String = "",
    val emailNotifications: Boolean = true,
    val pushNotificationsEnabled: Boolean = false,
    val showPhonePublicly: Boolean = false,
    val locationPermissionPrompted: Boolean = false,
    val notificationPermissionPrompted: Boolean = false,
    val recentlyViewedIds: List<String> = emptyList(),
    // Executive Admin Session State (Co-Founder, CEO, Founder)
    val activeAdminRole: String = "", // "", "cofounder", "ceo", "founder"
    val activeAdminCanonicalId: String = "", // "ADM-COF-001", "ADM-CEO-001", "ADM-FND-001"
    val activeAdminDisplayName: String = "", // "Abhay", "CEO", "Founder"
    val activeAdminUsername: String = "", // "Abhay", "Adarsh", "Abhisheksingh"
    val adminSessionExpiresAtMs: Long = 0L,
    val adminLastBackgroundedAtMs: Long = 0L,
    // Jobs & Courses saved state
    val appliedJobIds: List<String> = emptyList(),
    val enrolledCourseIds: List<String> = emptyList()
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val PENDING_ROLE = stringPreferencesKey("pending_registration_role")
        val SELECTED_STATE = stringPreferencesKey("selected_state")
        val SELECTED_CITY = stringPreferencesKey("selected_city")
        val SELECTED_LOCALITY = stringPreferencesKey("selected_locality")
        val SELECTED_PIN_CODE = stringPreferencesKey("selected_pin_code")
        val EMAIL_NOTIFICATIONS = booleanPreferencesKey("email_notifications")
        val PUSH_NOTIFICATIONS = booleanPreferencesKey("push_notifications_enabled")
        val SHOW_PHONE_PUBLICLY = booleanPreferencesKey("show_phone_publicly")
        val LOCATION_PROMPTED = booleanPreferencesKey("location_permission_prompted")
        val NOTIFICATION_PROMPTED = booleanPreferencesKey("notification_permission_prompted")
        val RECENTLY_VIEWED = stringPreferencesKey("recently_viewed_ids")
        val ACTIVE_ADMIN_ROLE = stringPreferencesKey("active_admin_role")
        val ACTIVE_ADMIN_CANONICAL_ID = stringPreferencesKey("active_admin_canonical_id")
        val ACTIVE_ADMIN_DISPLAY_NAME = stringPreferencesKey("active_admin_display_name")
        val ACTIVE_ADMIN_USERNAME = stringPreferencesKey("active_admin_username")
        val ADMIN_SESSION_EXPIRES_AT_MS = longPreferencesKey("admin_session_expires_at_ms")
        val ADMIN_LAST_BACKGROUNDED_AT_MS = longPreferencesKey("admin_last_backgrounded_at_ms")
        val APPLIED_JOB_IDS = stringPreferencesKey("applied_job_ids")
        val ENROLLED_COURSE_IDS = stringPreferencesKey("enrolled_course_ids")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val rawRecent = prefs[Keys.RECENTLY_VIEWED].orEmpty()
        val rawJobs = prefs[Keys.APPLIED_JOB_IDS].orEmpty()
        val rawCourses = prefs[Keys.ENROLLED_COURSE_IDS].orEmpty()
        UserPreferences(
            isDarkMode = prefs[Keys.IS_DARK_MODE] ?: false,
            language = prefs[Keys.LANGUAGE] ?: "en",
            pendingRegistrationRole = prefs[Keys.PENDING_ROLE] ?: "",
            selectedState = prefs[Keys.SELECTED_STATE] ?: "",
            selectedCity = prefs[Keys.SELECTED_CITY] ?: "All India",
            selectedLocality = prefs[Keys.SELECTED_LOCALITY] ?: "",
            selectedPinCode = prefs[Keys.SELECTED_PIN_CODE] ?: "",
            emailNotifications = prefs[Keys.EMAIL_NOTIFICATIONS] ?: true,
            pushNotificationsEnabled = prefs[Keys.PUSH_NOTIFICATIONS] ?: false,
            showPhonePublicly = prefs[Keys.SHOW_PHONE_PUBLICLY] ?: false,
            locationPermissionPrompted = prefs[Keys.LOCATION_PROMPTED] ?: false,
            notificationPermissionPrompted = prefs[Keys.NOTIFICATION_PROMPTED] ?: false,
            recentlyViewedIds = if (rawRecent.isBlank()) emptyList() else rawRecent.split(",").filter { it.isNotBlank() },
            activeAdminRole = prefs[Keys.ACTIVE_ADMIN_ROLE] ?: "",
            activeAdminCanonicalId = prefs[Keys.ACTIVE_ADMIN_CANONICAL_ID] ?: "",
            activeAdminDisplayName = prefs[Keys.ACTIVE_ADMIN_DISPLAY_NAME] ?: "",
            activeAdminUsername = prefs[Keys.ACTIVE_ADMIN_USERNAME] ?: "",
            adminSessionExpiresAtMs = prefs[Keys.ADMIN_SESSION_EXPIRES_AT_MS] ?: 0L,
            adminLastBackgroundedAtMs = prefs[Keys.ADMIN_LAST_BACKGROUNDED_AT_MS] ?: 0L,
            appliedJobIds = if (rawJobs.isBlank()) emptyList() else rawJobs.split(",").filter { it.isNotBlank() },
            enrolledCourseIds = if (rawCourses.isBlank()) emptyList() else rawCourses.split(",").filter { it.isNotBlank() }
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_DARK_MODE] = enabled }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = if (lang == "hi") "hi" else "en" }
    }

    suspend fun setPendingRegistrationRole(role: String) {
        context.dataStore.edit { it[Keys.PENDING_ROLE] = role }
    }

    suspend fun setLocation(city: String, locality: String, state: String = "", pinCode: String = "") {
        context.dataStore.edit {
            it[Keys.SELECTED_CITY] = city
            it[Keys.SELECTED_LOCALITY] = locality
            if (state.isNotBlank() || city == "All India") {
                it[Keys.SELECTED_STATE] = if (city == "All India") "" else state
            }
            if (pinCode.isNotBlank() || city == "All India") {
                it[Keys.SELECTED_PIN_CODE] = if (city == "All India") "" else pinCode
            }
        }
    }

    suspend fun setNotificationAndPrivacy(emailNotifs: Boolean, showPhone: Boolean, pushNotifs: Boolean? = null) {
        context.dataStore.edit {
            it[Keys.EMAIL_NOTIFICATIONS] = emailNotifs
            it[Keys.SHOW_PHONE_PUBLICLY] = showPhone
            if (pushNotifs != null) {
                it[Keys.PUSH_NOTIFICATIONS] = pushNotifs
            }
        }
    }

    suspend fun markLocationPromptHandled() {
        context.dataStore.edit { it[Keys.LOCATION_PROMPTED] = true }
    }

    suspend fun markNotificationPromptHandled(enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.NOTIFICATION_PROMPTED] = true
            it[Keys.PUSH_NOTIFICATIONS] = enabled
        }
    }

    suspend fun addRecentlyViewedService(serviceId: String) {
        if (serviceId.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENTLY_VIEWED].orEmpty()
                .split(",")
                .filter { it.isNotBlank() && it != serviceId }
            val updated = (listOf(serviceId) + current).take(8)
            prefs[Keys.RECENTLY_VIEWED] = updated.joinToString(",")
        }
    }

    suspend fun setAdminSession(
        adminRole: String,
        canonicalId: String,
        displayName: String,
        username: String,
        expiresAtMs: Long
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_ADMIN_ROLE] = adminRole
            prefs[Keys.ACTIVE_ADMIN_CANONICAL_ID] = canonicalId
            prefs[Keys.ACTIVE_ADMIN_DISPLAY_NAME] = displayName
            prefs[Keys.ACTIVE_ADMIN_USERNAME] = username
            prefs[Keys.ADMIN_SESSION_EXPIRES_AT_MS] = expiresAtMs
            prefs[Keys.ADMIN_LAST_BACKGROUNDED_AT_MS] = 0L
        }
    }

    suspend fun markAdminBackgrounded(timestampMs: Long) {
        context.dataStore.edit { prefs ->
            if ((prefs[Keys.ACTIVE_ADMIN_ROLE] ?: "").isNotBlank()) {
                prefs[Keys.ADMIN_LAST_BACKGROUNDED_AT_MS] = timestampMs
            }
        }
    }

    suspend fun refreshAdminForegroundActivity(newExpiresAtMs: Long) {
        context.dataStore.edit { prefs ->
            if ((prefs[Keys.ACTIVE_ADMIN_ROLE] ?: "").isNotBlank()) {
                prefs[Keys.ADMIN_SESSION_EXPIRES_AT_MS] = newExpiresAtMs
                prefs[Keys.ADMIN_LAST_BACKGROUNDED_AT_MS] = 0L
            }
        }
    }

    suspend fun clearAdminSession() {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_ADMIN_ROLE] = ""
            prefs[Keys.ACTIVE_ADMIN_CANONICAL_ID] = ""
            prefs[Keys.ACTIVE_ADMIN_DISPLAY_NAME] = ""
            prefs[Keys.ACTIVE_ADMIN_USERNAME] = ""
            prefs[Keys.ADMIN_SESSION_EXPIRES_AT_MS] = 0L
            prefs[Keys.ADMIN_LAST_BACKGROUNDED_AT_MS] = 0L
        }
    }

    suspend fun toggleJobApplication(jobId: String): Boolean {
        var isAppliedNow = false
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.APPLIED_JOB_IDS].orEmpty().split(",").filter { it.isNotBlank() }.toMutableList()
            if (current.contains(jobId)) {
                current.remove(jobId)
                isAppliedNow = false
            } else {
                current.add(0, jobId)
                isAppliedNow = true
            }
            prefs[Keys.APPLIED_JOB_IDS] = current.joinToString(",")
        }
        return isAppliedNow
    }

    suspend fun toggleCourseEnrollment(courseId: String): Boolean {
        var isEnrolledNow = false
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.ENROLLED_COURSE_IDS].orEmpty().split(",").filter { it.isNotBlank() }.toMutableList()
            if (current.contains(courseId)) {
                current.remove(courseId)
                isEnrolledNow = false
            } else {
                current.add(0, courseId)
                isEnrolledNow = true
            }
            prefs[Keys.ENROLLED_COURSE_IDS] = current.joinToString(",")
        }
        return isEnrolledNow
    }

    suspend fun clearAllSessionDataOnLogout() {
        context.dataStore.edit { prefs ->
            prefs[Keys.PENDING_ROLE] = ""
            prefs[Keys.ACTIVE_ADMIN_ROLE] = ""
            prefs[Keys.ACTIVE_ADMIN_CANONICAL_ID] = ""
            prefs[Keys.ACTIVE_ADMIN_DISPLAY_NAME] = ""
            prefs[Keys.ACTIVE_ADMIN_USERNAME] = ""
            prefs[Keys.ADMIN_SESSION_EXPIRES_AT_MS] = 0L
            prefs[Keys.ADMIN_LAST_BACKGROUNDED_AT_MS] = 0L
        }
    }
}
