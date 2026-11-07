# Project: SkillConnect – Multi-Level Role-Based Administration System

A complete, production-ready, multi-level administration architecture for SkillConnect that establishes strict role-based dashboards, backend-enforced permissions, secure in-app account provisioning, and audit trails for Founder (Super Admin), CEO (Executive Admin), and Admin (Limited Admin) roles while preserving all existing marketplace capabilities.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed in Phase 1 and govern the implementation:

- **Role Synchronization (Confirmed)**: **Hybrid Firebase Custom Claims + Firestore `admin_roles` collection**.
  - Custom claims (`role: "FOUNDER" | "CEO" | "ADMIN"`) in the Firebase Auth token provide instant, zero-latency validation.
  - The Firestore `/admin_roles/{uid}` collection acts as the durable backend source of truth with explicit permission flags, UID-based security rules, and audit logging.
- **Official Account Provisioning (Confirmed)**: **In-app Secure Account Provisioning Wizard for Founder**.
  - A secure, dedicated management wizard in the Admin Authentication screen allows the authorized Founder to initialize, bind, and verify the official accounts (`abhishekbuisness20@gmail.com`, `abhaySkillconnect@gmail.com`, `abhaySkillconnect39@gmail.com`) using Firebase Authentication and encrypted credentials without exposing passwords in source code.
- **Dashboard Presentation (Confirmed)**: **Role-tailored dashboards with distinct bottom tabs and action bars**.
  - **Founder Dashboard**: Super Admin control (account management, permission grants/revocations, category management, security audit logs, platform broadcasts, full deletion/complaint overrides).
  - **CEO Dashboard**: Executive Admin control (business metrics, provider/customer approvals, marketing broadcasts, complaint escalations, assigned administrator oversight).
  - **Admin Dashboard**: Limited Admin control (verification queue, customer support tickets, post moderation, flagged report resolution).
  - **Public Marketplace**: Customer and Service Provider experiences remain completely unaffected.

---

## 1. Overview & Core Concept

SkillConnect currently has a single general Admin view. This enhancement upgrades the administration backend to an enterprise-grade, multi-tier hierarchy:

```
┌────────────────────────────────────────────────────────────────────────┐
│                      SKILLCONNECT ACCESS HIERARCHY                     │
├────────────────────────────────────────────────────────────────────────┤
│  Level 1: FOUNDER (Super Admin) - abhishekbuisness20@gmail.com        │
│  - Full Governance, CEO/Admin Provisioning, Audit Logs, Platform Config│
├────────────────────────────────────────────────────────────────────────┤
│  Level 2: CEO (Executive Admin) - abhaySkillconnect@gmail.com          │
│  - Operations, Analytics, Profile Approvals, Business Escalations     │
├────────────────────────────────────────────────────────────────────────┤
│  Level 3: ADMIN (Limited Admin) - abhaySkillconnect39@gmail.com        │
│  - Verification Queue, Customer Support, Moderation, Assigned Tickets │
├────────────────────────────────────────────────────────────────────────┤
│  Level 4: MARKETPLACE USERS (Preserved)                                │
│  - Service Providers (Studio Workspace) & Customers (Marketplace)     │
└────────────────────────────────────────────────────────────────────────┘
```

- **Zero Privilege Escalation**: Frontend email checks are strictly accompanied by Firebase UID validation, custom token claims (`role`), and Firestore security rules checking `/admin_roles/{uid}`.
- **Audit Logging**: Every sensitive action (verifying a provider, changing category, banning an account, updating role permissions) records an immutable entry into `/admin_audit_logs/{logId}`.
- **Destructive Action Confirmation**: Dialog gates prevent accidental bans, deletions, or role revocations.
- **Inactivity Session Timeout**: The existing 15-minute inactive auto-logout is extended across all three administrative dashboard levels.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Admin Sign-In Flow**:
   - The user opens the **Admin Authentication** tab.
   - Entering an authorized admin email displays a role badge indicator:
     - `abhishekbuisness20@gmail.com` -> **FOUNDER • Super Admin** (Navy/Gold badge)
     - `abhaySkillconnect@gmail.com` -> **CEO • Executive Admin** (Navy/Cyan badge)
     - `abhaySkillconnect39@gmail.com` -> **ADMIN • Operations & Support** (Navy/Blue badge)
   - Upon successful Firebase Authentication, the app evaluates custom claims/token role and routes directly to the matching tailored dashboard:
     - Founder -> `founder_dashboard`
     - CEO -> `ceo_dashboard`
     - Admin -> `admin_dashboard`

2. **Founder Dashboard**:
   - **Header**: Founder credentials (`abhishekbuisness20@gmail.com`), Super Admin badge, Security shield icon, and 15-minute inactivity tracker.
   - **Admin Management Hub**: View, invite, activate, or revoke CEO and Admin accounts. Set granular permissions (`canManageCategories`, `canBroadcast`, `canApproveProviders`, `canResolveComplaints`).
   - **Security Audit Logs**: Live feed of administrative actions with timestamp, admin email, action type, and affected resource ID.
   - **Full Platform Controls**: Category editor, broadcast announcements, platform fee/commission controls, and system health telemetry.

3. **CEO Dashboard**:
   - **Executive Metrics**: Gross volume, active work orders, customer acquisition rates, and provider conversion charts.
   - **Business Operations**: High-value booking approvals, customer complaint escalations, marketing broadcasts, and team supervision.
   - **Approval Queue**: Fast-track review of verified provider applications and featured showcase spots.

4. **Admin Dashboard**:
   - **Verification Queue**: Document review for service providers seeking the SkillConnect Verified Badge.
   - **Support Desk & Complaints**: Ticket queue for dispute mediation and support inquiries sent to `skillconnectofficiall@gmail.com`.
   - **Content Moderation**: Review flagged service offerings or reported reviews.

### Navigation Hierarchy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       ROLE-AWARE NAVIGATION BAR                             │
├──────────────────────┬──────────────────────────────────────────────────────┤
│ Role                 │ Bottom Navigation Tabs                               │
├──────────────────────┼──────────────────────────────────────────────────────┤
│ FOUNDER              │ [Founder Hub] [Marketplace] [Audit Logs] [Profile]   │
│ CEO                  │ [Operations]  [Marketplace] [Analytics]  [Profile]   │
│ ADMIN                │ [Desk Queue]  [Marketplace] [Reports]    [Profile]   │
│ CUSTOMER             │ [Home] [Explore] [Bookings] [Messages] [Profile]     │
│ PROVIDER             │ [Studio] [Explore] [Bookings] [Messages] [Profile]   │
└──────────────────────┴──────────────────────────────────────────────────────┘
```

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Hybrid Claims & Firestore Role Collection**:
  - *Chosen Approach*: Firebase Auth claims provide fast, tamper-proof client-side and rule-side checks (`request.auth.token.role`). The Firestore `/admin_roles/{uid}` collection stores dynamic permission toggles and provides observable updates without forcing token refresh cycles.
  - *Why*: Eliminates security vulnerabilities where an attacker modifies frontend client memory.
- **Decision 2: In-App Provisioning Wizard for Founder**:
  - *Chosen Approach*: A secure setup wizard accessible only by the verified Founder account that initializes Firebase Auth credentials and writes the initial administrative records to Firestore `/admin_roles`.
  - *Why*: Satisfies the strict requirement to never hardcode passwords in source code while providing a functional first-turn setup.
- **Decision 3: Non-Destructive Backward Compatibility**:
  - *Chosen Approach*: Maintain existing mock/demo admin accounts for Robolectric test suites and local development while activating real Firebase Auth credentials for production execution.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                              CLIENT (COMPOSE)                          │
│                                                                        │
│   AuthScreen ──> Firebase.auth.signInWithEmailAndPassword()           │
│         │                                                              │
│         ▼                                                              │
│   Token Result ──> Custom Claims (role: FOUNDER / CEO / ADMIN)        │
│         │                                                              │
│         ├─ FOUNDER ──> FounderDashboardScreen                          │
│         ├─ CEO     ──> CeoDashboardScreen                              │
│         └─ ADMIN   ──> AdminDashboardScreen                            │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        FIREBASE FIRESTORE DATA LAYER                   │
│                                                                        │
│   /admin_roles/{uid}        <── Role metadata, status & permissions    │
│   /admin_audit_logs/{logId} <── Immutable log of admin operations      │
│   /complaints/{id}          <── Multi-tier dispute resolution tickets  │
│   /users/{uid}              <── User profiles (read/write isolation)   │
│   /work_requests/{id}       <── Bookings & work orders                 │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          FIRESTORE SECURITY RULES                      │
│                                                                        │
│   isFounder()  := request.auth.token.role == 'FOUNDER' ||              │
│                   request.auth.token.email == 'abhishekbuisness20@...' │
│   isCeo()      := request.auth.token.role == 'CEO' || isFounder()      │
│   isAdmin()    := request.auth.token.role in ['ADMIN','CEO','FOUNDER'] │
│                                                                        │
│   match /admin_roles/{uid}        { allow read: if isAdmin();          │
│                                     allow write: if isFounder(); }     │
│   match /admin_audit_logs/{logId} { allow read: if isCeo();            │
│                                     allow create: if isAdmin(); }      │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Model Additions

1. **`AdminRole` Model**:
   - `uid: String`
   - `email: String`
   - `role: AdminRoleType` (`FOUNDER`, `CEO`, `ADMIN`)
   - `displayName: String`
   - `isActive: Boolean`
   - `permissions: AdminPermissions`
   - `assignedBy: String`
   - `updatedAt: Timestamp`

2. **`AdminAuditLog` Model**:
   - `id: String`
   - `adminUid: String`
   - `adminEmail: String`
   - `action: String` (e.g., `VERIFY_PROVIDER`, `UPDATE_ROLE`, `ADD_CATEGORY`, `RESOLVE_COMPLAINT`)
   - `targetEntityId: String`
   - `details: String`
   - `timestamp: Timestamp`

3. **`PlatformComplaint` Model**:
   - `id: String`
   - `reporterId: String`
   - `targetProviderId: String?`
   - `workRequestId: String?`
   - `reason: String`
   - `status: ComplaintStatus` (`OPEN`, `INVESTIGATING`, `RESOLVED`, `ESCALATED_TO_CEO`, `DISMISSED`)
   - `assignedAdminUid: String?`
   - `resolutionNotes: String?`

---

## 5. Implementation Steps & File Modifications

1. **Model Layer (`app/src/main/java/com/example/model/Models.kt`)**:
   - Add `AdminRoleType` (`FOUNDER`, `CEO`, `ADMIN`).
   - Add `AdminPermissions` data class.
   - Add `AdminAuditLog` and `PlatformComplaint` domain models.
   - Extend `UserRole` or integrate `AdminRoleType` into `UserProfile`.

2. **Security Rules & Blueprint (`firestore.rules`, `firebase-blueprint.json`, `firestore.test.js`)**:
   - Add `/admin_roles/{uid}`, `/admin_audit_logs/{logId}`, and `/complaints/{id}` to `firebase-blueprint.json`.
   - Update `firestore.rules` with `isFounder()`, `isCeo()`, and `isAdmin()` helper functions checking claims and `/admin_roles/{uid}`.
   - Enforce Founder-only write access on `/admin_roles`, CEO+Founder read on `/admin_audit_logs`, and role-gated complaint resolution.
   - Validate rules locally with `firestore.test.js` and deploy via `deploy_firebase`.

3. **Repository Layer (`MarketplaceRepository.kt` & `SkillConnectFirestoreRepository.kt`)**:
   - Update authorized admin email list with:
     - `abhishekbuisness20@gmail.com` (Founder)
     - `abhaySkillconnect@gmail.com` (CEO)
     - `abhaySkillconnect39@gmail.com` (Admin)
   - Add methods: `fetchAdminRole()`, `recordAuditLog()`, `observeAuditLogs()`, `updateAdminPermissions()`, `observeComplaints()`, `updateComplaintStatus()`.
   - Add `provisionOfficialAdminAccounts()` for the secure in-app setup wizard.

4. **ViewModel (`MarketplaceViewModel.kt`)**:
   - Add `adminRole: AdminRoleType?` to `MarketplaceUiState`.
   - Implement role detection on login (Firebase Auth custom claims / Firestore document check).
   - Add actions for CEO and Admin management, audit log streaming, and complaint handling.

5. **UI Screens**:
   - **`AuthScreen.kt`**: Update Admin mode to show the new official emails, account provisioning wizard trigger, and role badge indicators.
   - **`AdminScreens.kt`**:
     - Create `FounderDashboardScreen` (Super Admin controls, Team governance, Audit logs, Category & Config controls).
     - Create `CeoDashboardScreen` (Executive analytics, Provider approvals, Complaint escalations, Supervision).
     - Update `AdminDashboardScreen` (Support desk, Provider verification, Moderation).
     - Add `AdminAuditLogViewerDialog` and `ConfirmationActionDialog`.
   - **`MainScreen.kt`**:
     - Route logged-in Founder to `founder_dashboard`, CEO to `ceo_dashboard`, and Admin to `admin_dashboard`.
     - Update bottom navigation and drawer options tailored to each role level.

6. **Robolectric & Unit Tests (`ExampleRobolectricTest.kt`)**:
   - Add test cases verifying:
     - Founder, CEO, and Admin role authentication and routing.
     - Privilege separation (CEO cannot modify Founder permissions; Admin cannot access CEO analytics or Founder logs).
     - Unauthorized customer attempts are rejected.
     - Audit logs are recorded on administrative operations.
     - 15-minute session timeout operates across all three dashboards.
