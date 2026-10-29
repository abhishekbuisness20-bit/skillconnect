# SkillConnect Security Specification (Phase 0 TDD)

## 1. Data Invariants

1. **Authentication Gate**: Unauthenticated users (`request.auth == null`) cannot read or write any document across any collection.
2. **User Profile Ownership & Privilege Escalation Prevention**: A user can only create `/user_profiles/{userId}` where `userId == request.auth.uid`, with `isVerified == false` and `accountStatus == 'active'`. Only an admin can modify `isVerified` or `accountStatus`.
3. **Service Listing Integrity**: Only the authenticated provider (`incoming().providerId == request.auth.uid`) can create or edit their own service listing in `/services/{serviceId}`. Unlisted drafts (`isPublished == false`) are only readable by their owning provider or an admin.
4. **Booking Request Party Isolation & Terminal Locking**: Only the customer (`customerId == request.auth.uid`) can create a booking request, and `customerId != providerId`. Only the customer, provider, or admin can read/list the booking. Once a booking reaches `'cancelled'` or `'declined'`, no further updates by non-admins are allowed.
5. **Verified Completed-Job Review Invariant**: A review in `/reviews/{bookingId}` can only be created by the customer who owns `/booking_requests/{bookingId}` after that booking's `status == 'completed'`. Keying by `{bookingId}` prevents duplicate reviews for the same booking.
6. **PII & Verification Isolation**: `/verification_submissions/{userId}`, `/users/{userId}/favourites/{favId}`, `/reports/{reportId}`, and `/support_tickets/{ticketId}` are strictly isolated to the owning user (`request.auth.uid`) and verified admins.
7. **Query Enforcement (`allow list`)**: No collection permits blanket `allow list: if isSignedIn();`. Every `allow list` rule evaluates `resource.data` (e.g., `resource.data.isPublished == true`, `resource.data.customerId == request.auth.uid`, `resource.data.userId == request.auth.uid`).

## 2. The "Dirty Dozen" Payloads

1. **Unauthenticated Scraping**: Unauthenticated client queries `/services` or `/user_profiles`. -> REJECTED.
2. **Self-Verification Escalation**: User creates or updates `/user_profiles/{uid}` with `isVerified: true`. -> REJECTED.
3. **Shadow Field Injection**: User updates `/user_profiles/{uid}` with an undeclared field `isAdmin: true`. -> REJECTED via `hasOnly()`.
4. **Cross-Provider Service Hijack**: User A creates or updates `/services/{serviceId}` with `providerId: userB`. -> REJECTED.
5. **Unfiltered Service List**: Client runs `db.collection("services").get()` without `where("isPublished", "==", true)` or `where("providerId", "==", uid)`. -> REJECTED.
6. **Self-Booking Abuse**: Provider creates `/booking_requests/{id}` where `customerId == providerId`. -> REJECTED.
7. **Terminal Booking Mutation**: Provider or customer updates a `'cancelled'` or `'declined'` booking back to `'accepted'`. -> REJECTED.
8. **Fake Review Without Completed Booking**: User submits `/reviews/{bookingId}` when `/booking_requests/{bookingId}` does not exist or is `'pending'`. -> REJECTED via `get()` check on booking status.
9. **Cross-User Chat Eavesdropping**: User C queries `/messages` where `customerId == userA` and `providerId == userB`. -> REJECTED.
10. **PII Verification Leak**: User B attempts `get` on `/verification_submissions/{userA}`. -> REJECTED.
11. **Admin Email Spoofing**: Attacker with `email == 'abhishekbuisness20@gmail.com'` but `email_verified == false` attempts admin moderation. -> REJECTED.
12. **Value Poisoning / Denial of Wallet**: Attacker writes a 10,000-character string to `title` or 50 elements into `skills`. -> REJECTED by `isValid*()` size bounds.
