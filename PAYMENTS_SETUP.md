# SkillConnect Direct UPI subscriptions — setup and security

## What this implementation does
- The Android subscription screen supports both customer and service-provider profiles.
- Users can launch an installed UPI app, pay a configured UPI ID, and submit their UTR/reference.
- The app writes a **pending** subscription payment request. It never activates Premium from the Android client.
- The `approveSubscriptionPayment` Firebase callable (and CEO/Founder Admin Review tool) is restricted to an active CEO/Founder and activates the plan only after that reviewer confirms the credit in the merchant bank/UPI statement.
- Firestore rules prevent a normal user from setting `subscriptionPlan` to `premium` on profile creation or profile updates, and subscription payment requests must start as `pending`.

## Required configuration before testing/release
1. Replace `YOUR_UPI_ID@bank` in `app/src/main/java/com/example/ui/screens/ProfileAdminSupportScreens.kt` with the official merchant UPI ID. Do not ship the placeholder.
2. Ensure your production Firestore profile collection is `user_profiles/{uid}` and the admin documents are `admins/{uid}` (matching the current project rules/repository).
3. Deploy the updated `firestore.rules`.
4. In `functions/`, run `npm install`, then `npm run build`; deploy with Firebase CLI after selecting the correct Firebase project. Cloud Functions deployment generally requires a billing-enabled Firebase project.
5. Enable Firebase App Check for the Android app and configure its production provider before deploying the callable. `enforceAppCheck: true` is intentional.
6. Test with a low-value payment and an authorised reviewer. Verify that a pending request leaves the profile on Free and that approval changes the payment to `succeeded` and the profile plan to `premium`.

## Important limitation of direct UPI
A UTR typed by a user, a screenshot, or opening a UPI app is **not** proof that the merchant received money. A direct UPI ID/QR flow has no trusted payment-provider API in this project, so it cannot perform automatic bank-side verification. A reviewer must compare the UTR, amount, date and credit in the merchant statement before calling `approveSubscriptionPayment`. Never activate subscriptions solely because a user submitted a UTR. For fully automatic verification, integrate a payment gateway/PSP with signed server-side webhooks.

## Calling the approval function
Call the Firebase callable `approveSubscriptionPayment` (or use the CEO/Founder Subscription Review panel in the Admin Dashboard) from an authenticated admin-only review tool with:
- `paymentId`: Firestore payment document ID
- `verificationNote`: short note confirming the merchant statement check (minimum 8 characters)

Only active CEO/Founder accounts can call it. Keep the reviewer tool private; do not expose approval controls to normal customers/providers. The callable logs an audit record. Review admin document field conventions against your production admin data before launch.
