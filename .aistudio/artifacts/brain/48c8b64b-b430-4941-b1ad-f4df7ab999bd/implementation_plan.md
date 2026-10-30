# Implementation Plan — Admin Security, Message Governance, Customer Logout, Blue Tick Verification & Full Admin Inspection

## 1. Objective
Update SkillConnect to enforce a strict **3-attempt limit** on Administration login, restrict **message reading and deletion exclusively to Admins**, show **only self Booking notices and Login alerts** to Customers and Service Providers, add a prominent **Logout option on Customer Profile & Dashboard**, apply a **Blue Tick + "Verified by SkillConnect"** badge when an Admin approves a Service Provider profile, and allow **Admins to view the full details of any user**.

---

## 2. Key Changes

### A. Strict 3-Attempt Administration Login Limit (`AuthComponents.kt`, `SkillConnectRepository.kt`, `ProfileAdminSupportScreens.kt`)
- Change the maximum Administration login attempts from `5` to **`3` attempts** across:
  - Initial `SkillConnectAuthScreen` Executive Admin login (`AuthComponents.kt`)
  - Backend Firestore verifier `authenticateOrBootstrapExecutiveAdmin` (`SkillConnectRepository.kt`)
  - In-app `ProfileAndSettingsScreen` and `AdminDashboardScreen` Executive Admin login cards (`ProfileAdminSupportScreens.kt`)
- Display live attempt counter feedback (`"Attempt X of 3 — Y attempts remaining"`) on failed attempts, and lock administrative login for **60 seconds** upon reaching 3 failed attempts. On success within 3 attempts, reset `failedAttempts` to `0`.

### B. Admin-Only Message Read & Delete + Self-Only Booking & Login Notices for Customers/Providers (`SkillConnectRepository.kt`, `SkillConnectViewModel.kt`, `BookingsMessagesFavouritesScreens.kt`, `ProfileAdminSupportScreens.kt`, `firestore.rules`)
- **Customers & Service Providers (`MessagesAndNotificationsScreen` & `ProfileAndSettingsScreen`)**:
  - Show only the signed-in user's own (`userId == currentUserId`) **Booking Notices** and **Self Login / Account Security Alerts** (including a self-login session status card showing their own account, role, Platform ID, and active login status).
  - Restrict direct message thread reading and deletion to Admins, displaying a clear notice to Customers and Providers that message transcripts are managed by Administration while all their personal Booking and Login notifications are shown in real time.
- **Admins (`AdminDashboardScreen` & `MessagesAndNotificationsScreen`)**:
  - Add a dedicated **Messages & Conversations (Admin Read & Delete)** section in `AdminDashboardScreen` and enable full Admin message inspection and **Delete Message / Delete Conversation** actions in `SkillConnectRepository.kt` (`observeAllConversationsForAdmin`, `observeAllMessagesForAdmin`, `adminDeleteMessage`, `adminDeleteConversation`).

### C. Prominent Logout Option on Customer Profile & Customer Dashboard (`ProfileAdminSupportScreens.kt`, `DashboardsAndCreateServiceScreens.kt`, `MainActivity.kt`)
- Ensure the **Customer Profile** (`ProfileAndSettingsScreen`) and **Customer Dashboard** (`CustomerDashboardScreen`) both display a prominent **Logout** button right below the Customer profile card (with a confirmation dialog that clears session state and returns directly to the initial login screen).

### D. Admin Profile Approve Button -> Blue Tick & "Verified by SkillConnect" Badge (`SkillConnectRepository.kt`, `SkillConnectViewModel.kt`, `ProfileAdminSupportScreens.kt`, `ServiceAndProviderDetailScreens.kt`, `HomeAndExploreScreens.kt`, `DashboardsAndCreateServiceScreens.kt`)
- When an Admin clicks **Approve Profile** on a Service Provider (available in `Verification Queue`, `Providers`, and `Users` tabs of `AdminDashboardScreen`):
  - Update `user_profiles/{providerId}` with `isVerified = true`, `verificationStatus = "approved"`, `accountStatus = "active"`, and `isPublicProvider = true`.
  - Also update all `services` documents belonging to that provider with `providerVerified = true` so every listing card immediately reflects the verified status.
  - Render a prominent **Blue Tick icon (`Icons.Default.Verified` / `Icons.Default.VerifiedUser` in `SkillBlue`)** next to the Service Provider's name and a **"Verified by SkillConnect"** badge across:
    - Provider Profile card (`ProfileAndSettingsScreen`)
    - Provider Dashboard (`ProviderDashboardScreen`)
    - Public Provider Profile & Service Detail screens (`ServiceAndProviderDetailScreens.kt`)
    - Home & Provider Directory cards (`HomeAndExploreScreens.kt`)

### E. Full User Details Inspection for Admins (`ProfileAdminSupportScreens.kt`)
- Add a **"View Full Details"** button on every user, customer, and provider card in `AdminDashboardScreen` (`Users`, `Customers`, `Providers`, and `Verification Queue` tabs).
- Tapping **View Full Details** opens a comprehensive **Admin Full User Inspection Dialog** displaying all profile fields, Platform ID (`SC-CUS-...` / `SC-PRO-...`), Role, Account & Verification Status, Blue Tick status, Contact Phone, State/City/Locality/PIN, Skills, Bio, Starting Price, Subscription Plan, Completed Jobs, Rating, Bookings history, Payments history, and direct **Approve Blue Tick ("Verified by SkillConnect")** / **Suspend / Reactivate** actions.

---

## 3. Verification Plan
1. Validate Firestore rules if updated and run `compile_applet` to ensure the project compiles with zero errors.
2. Verify all 5 user requirements across Auth, Admin Portal, Customer/Provider Profiles, Notifications, and Provider Directory.
