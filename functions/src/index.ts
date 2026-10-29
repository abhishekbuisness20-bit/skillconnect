import * as admin from "firebase-admin";
import { onCall, HttpsError } from "firebase-functions/v2/https";

admin.initializeApp();
const db = admin.firestore();

const FOUNDER_EMAIL = "abhishekbuisness20@gmail.com";

export const approveSubscriptionPayment = onCall(
  { enforceAppCheck: true },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Authentication is required.");
    }

    const callerUid = request.auth.uid;
    const callerEmail = (request.auth.token.email || "").toLowerCase();
    const emailVerified = request.auth.token.email_verified === true;

    const paymentId = String(request.data?.paymentId || "").trim();
    const verificationNote = String(request.data?.verificationNote || "").trim();

    if (!paymentId || paymentId.length > 128) {
      throw new HttpsError("invalid-argument", "A valid paymentId is required.");
    }
    if (verificationNote.length < 8 || verificationNote.length > 500) {
      throw new HttpsError(
        "invalid-argument",
        "verificationNote must be at least 8 characters confirming the merchant statement check."
      );
    }

    // Verify caller is an active CEO or Founder
    const adminSnap = await db.collection("admins").doc(callerUid).get();
    const adminData = adminSnap.data();
    const isFounderByEmail = callerEmail === FOUNDER_EMAIL && emailVerified;

    let isAuthorizedReviewer = isFounderByEmail;
    if (!isAuthorizedReviewer && adminSnap.exists && adminData) {
      const role = String(adminData.adminRole || "");
      const expiresAt = adminData.sessionExpiresAt?.toDate?.() as Date | undefined;
      if (role === "founder") {
        isAuthorizedReviewer = true;
      } else if (role === "ceo" && expiresAt && expiresAt.getTime() >= Date.now()) {
        isAuthorizedReviewer = true;
      }
    }

    if (!isAuthorizedReviewer) {
      throw new HttpsError(
        "permission-denied",
        "Only an active CEO or Founder can approve subscription payments."
      );
    }

    const paymentRef = db.collection("payments").doc(paymentId);
    const result = await db.runTransaction(async (tx) => {
      const paymentSnap = await tx.get(paymentRef);
      if (!paymentSnap.exists) {
        throw new HttpsError("not-found", "Payment document not found.");
      }

      const payment = paymentSnap.data()!;
      if (payment.paymentType !== "subscription") {
        throw new HttpsError(
          "failed-precondition",
          "Payment document is not a subscription request."
        );
      }
      if (payment.status !== "pending") {
        throw new HttpsError(
          "failed-precondition",
          `Subscription payment is already in status '${payment.status}'.`
        );
      }

      const targetUserId = String(payment.userId || "").trim();
      if (!targetUserId) {
        throw new HttpsError("failed-precondition", "Payment record is missing userId.");
      }

      const profileRef = db.collection("user_profiles").doc(targetUserId);
      const profileSnap = await tx.get(profileRef);
      if (!profileSnap.exists) {
        throw new HttpsError("not-found", "Target user profile not found.");
      }

      const now = admin.firestore.FieldValue.serverTimestamp();
      tx.update(paymentRef, {
        status: "succeeded",
        notes: `Verified by ${callerEmail || callerUid}: ${verificationNote}`.slice(0, 500),
        updatedAt: now,
      });

      tx.update(profileRef, {
        subscriptionPlan: "premium",
        updatedAt: now,
      });

      const auditRef = db.collection("audit_logs").doc();
      tx.set(auditRef, {
        adminId: callerUid,
        adminEmail: (callerEmail || "admin@skillconnect.in").slice(0, 120),
        actionType: "APPROVE_SUBSCRIPTION_PAYMENT",
        targetCollection: "payments",
        targetId: paymentId,
        reason: verificationNote.slice(0, 500),
        previousState: "pending",
        newState: "succeeded -> subscriptionPlan=premium",
        createdAt: now,
      });

      return { targetUserId, paymentId, status: "succeeded", subscriptionPlan: "premium" };
    });

    return result;
  }
);
