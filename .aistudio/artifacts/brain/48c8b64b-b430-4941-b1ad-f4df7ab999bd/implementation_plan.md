# Firebase Project (`skillconnect-fc066`) & Firebase Analytics Integration Plan

## 1. Overview
Configure SkillConnect to use the `skillconnect-fc066` Firebase project with provisioned Google Sign-In OAuth credentials and integrate the **Firebase Analytics SDK** (`com.google.firebase:firebase-analytics`) to track screen views, marketplace searches, and service bookings.

---

## 2. Firebase Project & OAuth Provisioning (`skillconnect-fc066`)
- **Certificate Fingerprint Extraction**: Extract the active `SHA-1` and `SHA-256` signing fingerprints from `debug.keystore` for package `com.aistudio.skillconnect.kxmprq`.
- **Project Provisioning (`ProvisionFirebase`)**:
  - Provision Firebase resources targeting `project_id: "skillconnect-fc066"` with `requested_products: ["firestore"]` so that `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` are populated with the registered Android `certificate_hash` and OAuth 2.0 Web/Android clients required by Jetpack Credential Manager (`GetSignInWithGoogleOption`).
  - Verify that the `SHA-1` `certificate_hash` is present in `app/google-services.json` and deploy `firestore.rules` to the project.
  - Clean up the temporary root `/google-services.json` file so the Android app module uses the authoritative `app/google-services.json`.

---

## 3. Gradle & Dependency Configuration
- **Repositories & Google Services Plugin**:
  - Verify `google()` Maven repository configuration in `settings.gradle.kts` and `alias(libs.plugins.google.services)` in both root `build.gradle.kts` and `app/build.gradle.kts`.
- **Firebase Analytics SDK**:
  - Add `firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics" }` to `gradle/libs.versions.toml` (version-aligned automatically via the existing `firebase-bom`).
  - Add `implementation(libs.firebase.analytics)` to `app/build.gradle.kts`.
  - Add `ACCESS_NETWORK_STATE` and `WAKE_LOCK` permissions check in `AndroidManifest.xml` (standard install-time permissions used by Firebase Analytics) and ensure R8 rules in `app/proguard-rules.pro` preserve Firebase Analytics measurement classes.

---

## 4. Firebase Analytics Event Instrumentation
- **Analytics Helper (`app/src/main/java/com/example/util/SkillConnectAnalytics.kt`)**:
  - Create a lightweight, null-safe wrapper around `Firebase.analytics` (`FirebaseAnalytics`) that logs structured events without blocking the UI thread:
    1. **Screen Views (`FirebaseAnalytics.Event.SCREEN_VIEW`)**:
       - Automatically log active screen transitions in `MainActivity` (`Home`, `ExploreServices`, `ServiceDetail`, `BookService`, `MyBookings`, `ProviderRegistration`, `ProviderDashboard`, `Profile`, `Notifications`).
    2. **Marketplace Searches (`FirebaseAnalytics.Event.SEARCH`)**:
       - Log search queries (`FirebaseAnalytics.Param.SEARCH_TERM`) and active category filters when users search for service providers or local services from the Home or Explore marketplace search bars (debounced to avoid logging intermediate keystrokes).
    3. **Service & Provider Selection (`FirebaseAnalytics.Event.SELECT_ITEM`)**:
       - Log when a user selects a service card or verified provider profile (`item_id`, `item_name`, `item_category`).
    4. **Service Bookings (`book_service` & `booking_status_updated`)**:
       - Log booking creation in `SkillConnectViewModel` with service ID, service title, category, and price when a booking is confirmed, as well as booking status updates (accepted, completed, cancelled).
