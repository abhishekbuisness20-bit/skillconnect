# SkillConnect — Firebase Token Invalid Error Fix Plan

## 1. Root Cause Analysis & Investigation Summary

An end-to-end audit of the SkillConnect Firebase configuration, initialization pipeline, and token lifecycle identified four root causes behind the **Firebase token invalid** error:

1. **Firebase App Check Debug Secret Validation & Stale Cache (`AppCheckHelper.kt`)**
   - When the app launches without an explicit `FIREBASE_APPCHECK_DEBUG_TOKEN` intent extra, or when a previously cached debug secret in `SharedPreferences` (`com.google.firebase.appcheck.debug.store.<persistenceKey>`) is malformed, `"null"`, or unregistered, `DebugAppCheckProviderFactory` attempts to exchange an invalid secret with the Firebase App Check backend, triggering `403 / AppCheck token is invalid` errors during App Check verification and downstream Firestore/AI requests.
   - `AppCheckHelper` also lacked `setTokenAutoRefreshEnabled(true)`, input format validation (UUID verification), and automatic stale-secret recovery when token exchange fails.

2. **Firebase Auth ID Token Expiration & Firestore Request Recovery (`SkillConnectFirestoreRepository.kt`)**
   - When a returning user restores a session or remains active across token expiration boundaries, Firestore operations and authenticated calls did not proactively validate or force-refresh expired/invalid Auth ID tokens (`getIdToken(true)`) before retrying failed requests.

3. **Missing Firebase Cloud Messaging (FCM) Service & Firestore Token Sync**
   - While `google-services.json` defines `project_number` / `gcm_defaultSenderId` (`800529463187`) for project `gen-lang-client-0778223582` and package `com.aistudio.skillconnect.market`, the app did not yet include `firebase-messaging`, a `FirebaseMessagingService` (`onNewToken` / `onMessageReceived`), or automatic FCM token synchronization and invalid-token regeneration (`deleteToken()` + re-fetch) with Firestore.

4. **Duplicate Root Configuration File (`/google-services (4).json`)**
   - An uploaded copy `/google-services (4).json` exists in the project root alongside `/app/google-services.json`. Both match project `gen-lang-client-0778223582` (`package_name: com.aistudio.skillconnect.market`, `project_number: 800529463187`). The root duplicate will be cleaned up while keeping `/app/google-services.json` and `firebase_applet_config.xml` authoritative.

---

## 2. Verified Firebase Project Identity

| Parameter | Verified Value | Source |
| :--- | :--- | :--- |
| **Firebase Project ID** | `gen-lang-client-0778223582` | `app/google-services.json` & `firebase_applet_config.xml` |
| **Android Package Name** | `com.aistudio.skillconnect.market` | `app/build.gradle.kts` & `app/google-services.json` |
| **GCM / FCM Sender ID** | `800529463187` | `app/google-services.json` (`project_number`) |
| **Firebase App ID** | `1:800529463187:android:e2715c4819dbe5b6201a66` | `app/google-services.json` (`mobilesdk_app_id`) |

---

## 3. Architecture & Token Lifecycle Strategy

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        MainActivity (Startup)                           │
│  1. Verify FirebaseApp options (projectId, applicationId, gcmSenderId)  │
│  2. AppCheckHelper.initAppCheck() -> Validate UUID, enable auto-refresh │
│  3. FirebaseTokenManager.initialize() -> Auth + AppCheck + FCM sync     │
└───────────────┬─────────────────────────┬───────────────────────────────┘
                │                         │
                ▼                         ▼
┌───────────────────────────────┐ ┌───────────────────────────────────────┐
│        AppCheckHelper         │ │         FirebaseTokenManager          │
│ • Sanitizes debug token input │ │ • Validates Project & Sender ID       │
│ • Clears corrupted prefs      │ │ • Refreshes Auth ID token (getIdToken)│
│ • PlayIntegrity (Release) /   │ │ • Fetches & refreshes FCM token       │
│   DebugProvider (Debug)       │ │ • Recovers from INVALID_PARAMETERS /  │
│ • Auto-fallback & masked logs │ │   UNREGISTERED via deleteToken()      │
└───────────────┬───────────────┘ └───────────────────┬───────────────────┘
                │                                     │
                ▼                                     ▼
┌───────────────────────────────┐ ┌───────────────────────────────────────┐
│SkillConnectFirestoreRepository│ │      SkillConnectMessagingService     │
│ • Pre-flight Auth token check │ │ • Extends FirebaseMessagingService    │
│ • Retries once with force-    │ │ • onNewToken() -> syncs to Firestore  │
│   refreshed token on auth/    │ │   /users/{uid} & /users/{uid}/        │
│   token errors                │ │   fcmTokens/{deviceId}                │
│ • Syncs FCM metadata securely │ │ • Validates incoming senderId         │
└───────────────────────────────┘ └───────────────────────────────────────┘
```

### Core Components to Implement / Update

- **App Check Hardening (`AppCheckHelper.kt`)**:
  - Validate any incoming `FIREBASE_APPCHECK_DEBUG_TOKEN` intent extra and existing cached debug secret against strict UUID formatting (rejecting `"null"`, `"undefined"`, or blank values).
  - Enable `FirebaseAppCheck.getInstance().setTokenAutoRefreshEnabled(true)`.
  - Attach a token pre-flight verifier (`verifyAndRefreshAppCheckToken`) that detects invalid/expired App Check tokens, clears corrupted debug secrets if necessary, and logs status using masked token identifiers (never logging raw tokens or secrets).
- **Centralized Token Manager (`FirebaseTokenManager.kt`)**:
  - Verify at runtime that `FirebaseApp.getInstance().options` matches `projectId = "gen-lang-client-0778223582"`, `gcmSenderId = "800529463187"`, and `applicationId = "1:800529463187:android:e2715c4819dbe5b6201a66"`.
  - Provide `ensureValidAuthToken(forceRefresh: Boolean)` using `FirebaseAuth.getInstance().currentUser?.getIdToken(forceRefresh)` with automatic force-refresh fallback when a token is expired or invalid.
  - Provide `syncFcmTokenWithFirestore()` and `refreshFcmTokenOnInvalidError()` (calling `FirebaseMessaging.getInstance().deleteToken()` followed by `FirebaseMessaging.getInstance().token` to recover from `FIS_AUTH_ERROR` / `INVALID_PARAMETERS` / `UNREGISTERED`).
  - Provide `maskToken(token: String?)` utility so all diagnostic logs print only a safe prefix/suffix length digest (e.g., `len=162, sha256=8f3a...c12b`) without exposing raw tokens.
- **FCM Service Integration (`SkillConnectMessagingService.kt` & `AndroidManifest.xml`)**:
  - Add `firebase-messaging` to `gradle/libs.versions.toml` and `app/build.gradle.kts`.
  - Implement `SkillConnectMessagingService` extending `FirebaseMessagingService` to handle `onNewToken(token: String)` and `onMessageReceived(message: RemoteMessage)`.
  - Verify incoming message `from` / `senderId` matches project sender ID `800529463187` and store the latest FCM registration token + project/sender metadata in Firestore under `/users/{userId}` and `/users/{userId}/fcmTokens/{tokenDocId}`.
- **Firestore Repository & Security Rules (`SkillConnectFirestoreRepository.kt` & `firestore.rules`)**:
  - Add automatic token-refresh retry handling around Firestore operations if a `UNAVAILABLE` / `UNAUTHENTICATED` token error occurs.
  - Update `firestore.rules` to allow authenticated users to read/write their own `/users/{userId}/fcmTokens/{tokenId}` documents with strict schema validation while preserving all existing zero-trust RBAC rules.
- **Testing & Verification**:
  - Validate updated `firestore.rules` against the local Firestore emulator (`firestore.test.js` & `SkillConnectRepositoryRuleTest`).
  - Add Robolectric unit tests in `ExampleRobolectricTest.kt` verifying project/sender ID alignment, token masking (ensuring raw tokens never appear in logs), App Check debug secret sanitization, and token refresh fallback behavior.
