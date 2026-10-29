# Implementation Plan — Permanent Merchant UPI Payment Details (`6393481228@ybl`)

## 1. Objective
Configure the permanent official Merchant UPI ID **`6393481228@ybl`** for all Customer and Service Provider subscription purchases in SkillConnect, displaying it prominently both on the main **Subscriptions** screen and inside the **Direct UPI Subscription Checkout** dialog with **Copy UPI ID** and **Open UPI App** actions.

---

## 2. Key Changes

### A. Update Official Merchant UPI Constant (`ProfileAdminSupportScreens.kt`)
- Replace the placeholder `const val MERCHANT_UPI_ID = "YOUR_UPI_ID@bank"` with the permanent merchant UPI ID:
  - `const val MERCHANT_UPI_ID = "6393481228@ybl"`
  - `const val MERCHANT_UPI_NAME = "SkillConnect India"`
- Remove the placeholder warning check (`if (MERCHANT_UPI_ID == "YOUR_UPI_ID@bank")`) so clicking **Open UPI App** always launches the standard `upi://pay?pa=6393481228@ybl...` deep link on any installed UPI app (PhonePe, GPay, Paytm, BHIM, etc.).

### B. Permanent Merchant Payment Detail Card on Main Subscriptions Screen (`ProfileAdminSupportScreens.kt`)
- Add a dedicated **Official Merchant Payment Details** card directly on the `SubscriptionsScreen` (visible to all Customers and Service Providers viewing plans):
  - Displays **Merchant Name**: `SkillConnect India`
  - Displays **Official Permanent UPI ID**: `6393481228@ybl`
  - Includes a **Copy UPI ID** button (copies `6393481228@ybl` to the clipboard with instant confirmation feedback) and a **Pay / Open UPI App** button pre-filled with the selected role's Premium plan price (`₹customerPremiumPrice` or `₹providerPremiumPrice`).
  - Explains the 2-step flow: pay `6393481228@ybl` via any UPI app, then click Upgrade below to submit the 12-digit UTR for verification.

### C. Direct UPI Subscription Checkout Dialog (`ProfileAdminSupportScreens.kt`)
- Ensure the **Step 1: Pay Merchant UPI ID** box inside the checkout dialog clearly highlights:
  - **Merchant**: `SkillConnect India`
  - **Permanent UPI ID**: `6393481228@ybl`
  - **Amount Payable**: `₹<price>`
  - **Copy UPI ID** button and **Open UPI App** button (`upi://pay?pa=6393481228@ybl&pn=SkillConnect%20India&am=<price>&cu=INR...`), followed by Step 2 to submit the UTR reference.
- Also record the target merchant UPI ID (`6393481228@ybl`) in the subscription payment notes in `SkillConnectRepository.kt` so both the user's Payment History and the Admin/Founder verification ledger show the exact merchant VPA used.

---

## 3. Verification Plan
1. Compile the Android project using `compile_applet` and confirm zero errors.
2. Verify that `6393481228@ybl` is visible on the main Subscription screen and in the Direct UPI Checkout dialog for both Customer and Service Provider plans, with working **Copy UPI ID** and **Open UPI App** buttons.
