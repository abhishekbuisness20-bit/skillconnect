import * as admin from "firebase-admin";
import * as crypto from "crypto";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { beforeUserSignedIn } from "firebase-functions/v2/identity";

admin.initializeApp();
const db = admin.firestore();

/**
 * Explicit CORS allowlist for Web & GitHub Pages origins.
 * Native Android clients do not send Origin headers and are unaffected.
 */
const ALLOWED_CORS_ORIGINS: (string | RegExp)[] = [
  /^https:\/\/[a-zA-Z0-9-]+\.github\.io$/,
  /^https:\/\/([a-zA-Z0-9-]+\.)?skillconnect\.in$/,
  /^https:\/\/[a-zA-Z0-9-]+\.web\.app$/,
  /^https:\/\/[a-zA-Z0-9-]+\.firebaseapp\.com$/,
];

/**
 * SKILLCONNECT OFFICIAL — AUTHORIZED ADMIN EMAIL CONFIGURATION
 *
 * These three email addresses are the ONLY authorized Admin accounts
 * unless explicitly updated by the Founder.
 * Environment variables override defaults if configured in Cloud Functions secrets/env.
 */
const FOUNDER_EMAIL = (
  process.env.FOUNDER_EMAIL || "abhishek.buisness20@gmail.com"
).trim().toLowerCase();
const FOUNDER_EMAIL_CANONICAL_ALT = "abhishekbuisness20@gmail.com";

const COFOUNDER_EMAIL = (
  process.env.COFOUNDER_EMAIL || "abhayskillconnect@gmail.com"
).trim().toLowerCase();

const CEO_EMAIL = (
  process.env.CEO_EMAIL || "abhaySkillconnect39@gmail.com"
).trim().toLowerCase();

export interface AuthorizedAdminRoleSpec {
  email: string;
  adminRole: "founder" | "cofounder" | "ceo";
  canonicalAccountId: "ADM-FND-001" | "ADM-COF-001" | "ADM-CEO-001";
  displayName: "Founder" | "Abhay" | "CEO";
  defaultUsername: "Abhisheksingh" | "Abhay";
  roleLabel: "FOUNDER" | "CO-FOUNDER" | "CEO";
  accessGroup: "founder_admin_group" | "ceo_dashboard_group";
  accessGroupLabel: string;
  founderSessionPersistent: boolean;
  permissions: string[];
}

const FOUNDER_PERMISSIONS: string[] = [
  "all_access",
  "manage_admins",
  "manage_users",
  "manage_listings",
  "verify_providers",
  "manage_bookings",
  "read_delete_messages",
  "manage_platform_settings",
  "unlimited_refunds",
  "resolve_disputes",
  "resolve_reports",
  "manage_support",
  "view_audit_logs",
];

const COFOUNDER_PERMISSIONS: string[] = [
  "founder_group_access",
  "manage_users",
  "manage_listings",
  "verify_providers",
  "manage_bookings",
  "read_delete_messages",
  "manage_platform_settings",
  "unlimited_refunds",
  "resolve_disputes",
  "resolve_reports",
  "manage_support",
  "view_audit_logs",
];

const CEO_PERMISSIONS: string[] = [
  "ceo_dashboard_access",
  "view_analytics",
  "verify_providers",
  "manage_bookings",
  "read_delete_messages",
  "operational_settings",
  "refunds_up_to_10000_inr",
  "resolve_disputes",
  "resolve_reports",
  "manage_support",
  "view_audit_logs",
];

/**
 * Sanitizes untrusted user/admin text inputs on the server to prevent stored XSS,
 * HTML injection, or control-character smuggling.
 */
export function sanitizeInput(raw: unknown, maxLength = 500): string {
  const str = String(raw ?? "");
  return str
    .replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]/g, "")
    .replace(/<\s*\/?\s*script\b[^>]*>/gi, "")
    .replace(/<\s*\/?\s*(iframe|object|embed|svg|math|style|link|meta)\b[^>]*>/gi, "")
    .replace(/\bon[a-z]+\s*=/gi, "")
    .replace(/(javascript|vbscript|data)\s*:/gi, "")
    .trim()
    .slice(0, maxLength);
}

/**
 * Resolves the authenticated Google email against the three authorized admin addresses.
 * Returns null for any unauthorized Gmail/Google account.
 */
export function resolveAuthorizedAdminByEmail(
  rawEmail?: string | null
): AuthorizedAdminRoleSpec | null {
  const email = (rawEmail || "").trim().toLowerCase();
  if (!email) {
    return null;
  }

  if (email === FOUNDER_EMAIL || email === FOUNDER_EMAIL_CANONICAL_ALT) {
    return {
      email: "abhishek.buisness20@gmail.com",
      adminRole: "founder",
      canonicalAccountId: "ADM-FND-001",
      displayName: "Founder",
      defaultUsername: "Abhisheksingh",
      roleLabel: "FOUNDER",
      accessGroup: "founder_admin_group",
      accessGroupLabel: "Founder-Level Admin Access Group",
      founderSessionPersistent: true,
      permissions: FOUNDER_PERMISSIONS,
    };
  }

  if (email === COFOUNDER_EMAIL) {
    return {
      email: "abhayskillconnect@gmail.com",
      adminRole: "cofounder",
      canonicalAccountId: "ADM-COF-001",
      displayName: "Abhay",
      defaultUsername: "Abhay",
      roleLabel: "CO-FOUNDER",
      accessGroup: "founder_admin_group",
      accessGroupLabel: "Founder-Level Admin Access Group",
      founderSessionPersistent: false,
      permissions: COFOUNDER_PERMISSIONS,
    };
  }

  if (email === CEO_EMAIL) {
    return {
      email: "abhaySkillconnect39@gmail.com",
      adminRole: "ceo",
      canonicalAccountId: "ADM-CEO-001",
      displayName: "CEO",
      defaultUsername: "Abhay",
      roleLabel: "CEO",
      accessGroup: "ceo_dashboard_group",
      accessGroupLabel: "CEO Dashboard Permissions",
      founderSessionPersistent: false,
      permissions: CEO_PERMISSIONS,
    };
  }

  return null;
}

const SCRYPT_N = 16384;
const SCRYPT_R = 8;
const SCRYPT_P = 1;
const KEY_LEN_BYTES = 32;

/**
 * Derives a memory-hard scrypt password verifier (`scrypt$16384$8$1$<64-char-hex>`).
 */
export function hashAdminPassword(password: string, salt: string): string {
  const cleanPassword = password.trim();
  const derived = crypto.scryptSync(
    `${salt}:${cleanPassword}`,
    salt,
    KEY_LEN_BYTES,
    { N: SCRYPT_N, r: SCRYPT_R, p: SCRYPT_P }
  );
  return `scrypt$${SCRYPT_N}$${SCRYPT_R}$${SCRYPT_P}$${derived.toString("hex")}`;
}

/**
 * Verifies an entered administrative password in constant time (`crypto.timingSafeEqual`).
 * Supports:
 * - `scrypt$N$r$p$<hex>` (Cloud Functions memory-hard scrypt)
 * - `pbkdf2$iterations$<hex>` (PBKDF2-HMAC-SHA256 from Android/JVM or Node)
 * - Legacy 64-character hex SHA-256 (`needsRehash: true` triggers transparent upgrade to scrypt)
 */
export function verifyAdminPasswordConstantTime(
  enteredPassword: string,
  salt: string,
  storedVerifier: string
): { valid: boolean; needsRehash: boolean } {
  const cleanPassword = enteredPassword.trim();
  const cleanStored = (storedVerifier || "").trim();
  if (!cleanPassword || !salt || !cleanStored) {
    return { valid: false, needsRehash: false };
  }

  try {
    if (cleanStored.startsWith("scrypt$")) {
      const parts = cleanStored.split("$");
      if (parts.length !== 5) {
        return { valid: false, needsRehash: false };
      }
      const n = parseInt(parts[1], 10) || SCRYPT_N;
      const r = parseInt(parts[2], 10) || SCRYPT_R;
      const p = parseInt(parts[3], 10) || SCRYPT_P;
      const expectedHex = parts[4];
      const expectedBuf = Buffer.from(expectedHex, "hex");
      const computedBuf = crypto.scryptSync(
        `${salt}:${cleanPassword}`,
        salt,
        expectedBuf.length,
        { N: n, r, p }
      );
      if (expectedBuf.length !== computedBuf.length) {
        return { valid: false, needsRehash: false };
      }
      return {
        valid: crypto.timingSafeEqual(expectedBuf, computedBuf),
        needsRehash: false,
      };
    }

    if (cleanStored.startsWith("pbkdf2$")) {
      const parts = cleanStored.split("$");
      if (parts.length !== 3) {
        return { valid: false, needsRehash: false };
      }
      const iterations = Math.min(Math.max(parseInt(parts[1], 10) || 120000, 10000), 600000);
      const expectedHex = parts[2];
      const expectedBuf = Buffer.from(expectedHex, "hex");
      const computedBuf = crypto.pbkdf2Sync(
        cleanPassword,
        salt,
        iterations,
        expectedBuf.length,
        "sha256"
      );
      if (expectedBuf.length !== computedBuf.length) {
        return { valid: false, needsRehash: false };
      }
      return {
        valid: crypto.timingSafeEqual(expectedBuf, computedBuf),
        needsRehash: false,
      };
    }

    // Legacy 64-character SHA-256 hex verifier
    if (/^[a-fA-F0-9]{64}$/.test(cleanStored)) {
      const expectedBuf = Buffer.from(cleanStored.toLowerCase(), "hex");
      const computedHex = crypto
        .createHash("sha256")
        .update(`${salt}:${cleanPassword}`, "utf8")
        .digest("hex");
      const computedBuf = Buffer.from(computedHex, "hex");
      if (expectedBuf.length !== computedBuf.length) {
        return { valid: false, needsRehash: false };
      }
      const matches = crypto.timingSafeEqual(expectedBuf, computedBuf);
      return { valid: matches, needsRehash: matches };
    }
  } catch {
    return { valid: false, needsRehash: false };
  }

  return { valid: false, needsRehash: false };
}

/**
 * Per-UID sliding-window rate limiter backed by `_rate_limits/{uid}_{action}`
 * (accessible only to Admin SDK; blocked for all clients by default-deny Firestore rules).
 */
async function enforceRateLimit(
  uid: string,
  action: string,
  maxRequests: number,
  windowMs: number
): Promise<void> {
  const safeUid = uid.replace(/[^a-zA-Z0-9_-]/g, "").slice(0, 64);
  const docRef = db.collection("_rate_limits").doc(`${safeUid}_${action}`);
  const nowMs = Date.now();

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(docRef);
    const data = snap.data();
    const windowStartMs = Number(data?.windowStartMs || 0);
    const count = Number(data?.count || 0);

    if (nowMs - windowStartMs > windowMs) {
      tx.set(docRef, {
        uid: safeUid,
        action,
        windowStartMs: nowMs,
        count: 1,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      return;
    }

    if (count >= maxRequests) {
      const retrySec = Math.max(1, Math.ceil((windowStartMs + windowMs - nowMs) / 1000));
      throw new HttpsError(
        "resource-exhausted",
        `Too many requests for ${action}. Please wait ${retrySec}s before trying again.`
      );
    }

    tx.set(
      docRef,
      {
        count: count + 1,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );
  });
}

/**
 * 1. Blocking Auth Trigger (`beforeUserSignedIn`):
 * Automatically identifies if the signing-in Google account is one of the 3 authorized
 * Admin emails (Founder, Co-Founder, CEO) and attaches their verified custom claims.
 * Customer and Service Provider accounts remain completely unchanged.
 */
export const syncAuthorizedAdminOnLogin = beforeUserSignedIn(async (event) => {
  const user = event.data;
  if (!user || !user.email || !user.emailVerified) {
    return {};
  }

  const adminSpec = resolveAuthorizedAdminByEmail(user.email);
  if (!adminSpec) {
    // Standard Customer or Service Provider account — keep authentication unchanged
    return {};
  }

  // If the admin document already exists in Firestore, keep its role and permissions synced
  const adminRef = db.collection("admins").doc(user.uid);
  const adminSnap = await adminRef.get();
  if (adminSnap.exists) {
    await adminRef.set(
      {
        adminRole: adminSpec.adminRole,
        canonicalAccountId: adminSpec.canonicalAccountId,
        displayName: adminSpec.displayName,
        adminEmail: adminSpec.email.toLowerCase(),
        accessGroup: adminSpec.accessGroup,
        permissions: adminSpec.permissions,
        founderSessionPersistent: adminSpec.founderSessionPersistent,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );
  }

  return {
    sessionClaims: {
      adminRole: adminSpec.adminRole,
      canonicalAccountId: adminSpec.canonicalAccountId,
      accessGroup: adminSpec.accessGroup,
      isAuthorizedAdmin: true,
    },
  };
});

/**
 * 2. Unified Admin Gmail Login Verification & Role Assignment (`verifyAndAssignAdminRole`):
 * - Verifies the caller's authenticated Google email against FOUNDER_EMAIL, COFOUNDER_EMAIL, and CEO_EMAIL.
 * - Immediately rejects any unauthorized Gmail account with permission-denied.
 * - Verifies or initializes the secondary admin password using constant-time scrypt/PBKDF2 comparison (3 attempts max, 60s lockout).
 * - Assigns the respective role, accessGroup, and permissions in Firestore (`/admins/{uid}`)
 *   and sets Firebase Auth custom claims upon login.
 */
export const verifyAndAssignAdminRole = onCall(
  { enforceAppCheck: true, cors: ALLOWED_CORS_ORIGINS },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Google Sign-In authentication is required.");
    }

    const callerUid = request.auth.uid;
    const callerEmail = String(request.auth.token.email || "").trim().toLowerCase();
    const emailVerified = request.auth.token.email_verified === true;

    await enforceRateLimit(callerUid, "admin_login", 10, 60_000);

    if (!callerEmail || !emailVerified) {
      throw new HttpsError(
        "permission-denied",
        "A verified Google email account is required for SkillConnect Admin Login."
      );
    }

    const emailSpec = resolveAuthorizedAdminByEmail(callerEmail);
    const adminRef = db.collection("admins").doc(callerUid);
    const adminSnap = await adminRef.get();
    const existingData = adminSnap.data();

    // Check if authorized by the 3 official emails OR explicitly provisioned by the Founder in /admins/{uid}
    if (!emailSpec && !adminSnap.exists) {
      await db.collection("audit_logs").add({
        adminId: callerUid,
        adminEmail: callerEmail.slice(0, 120),
        actionType: "UNAUTHORIZED_ADMIN_LOGIN_REJECTED",
        targetCollection: "admins",
        targetId: callerUid,
        reason: `Rejected unauthorized Google email '${sanitizeInput(callerEmail, 120)}' on Admin Gmail Login Page`,
        previousState: "unauthenticated",
        newState: "rejected",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      throw new HttpsError(
        "permission-denied",
        `Unauthorized Admin Account: '${callerEmail}' is not an authorized SkillConnect Admin Gmail account. Access is strictly restricted to the configured Founder, Co-Founder, and CEO accounts.`
      );
    }

    const resolvedRole: "founder" | "cofounder" | "ceo" =
      emailSpec?.adminRole ||
      (existingData?.adminRole as "founder" | "cofounder" | "ceo") ||
      "ceo";

    const resolvedSpec: AuthorizedAdminRoleSpec = emailSpec || {
      email: callerEmail,
      adminRole: resolvedRole,
      canonicalAccountId:
        resolvedRole === "founder"
          ? "ADM-FND-001"
          : resolvedRole === "cofounder"
          ? "ADM-COF-001"
          : "ADM-CEO-001",
      displayName:
        resolvedRole === "founder"
          ? "Founder"
          : resolvedRole === "cofounder"
          ? "Abhay"
          : "CEO",
      defaultUsername: resolvedRole === "founder" ? "Abhisheksingh" : "Abhay",
      roleLabel:
        resolvedRole === "founder"
          ? "FOUNDER"
          : resolvedRole === "cofounder"
          ? "CO-FOUNDER"
          : "CEO",
      accessGroup:
        resolvedRole === "founder" || resolvedRole === "cofounder"
          ? "founder_admin_group"
          : "ceo_dashboard_group",
      accessGroupLabel:
        resolvedRole === "founder" || resolvedRole === "cofounder"
          ? "Founder-Level Admin Access Group"
          : "CEO Dashboard Permissions",
      founderSessionPersistent: resolvedRole === "founder",
      permissions:
        resolvedRole === "founder"
          ? FOUNDER_PERMISSIONS
          : resolvedRole === "cofounder"
          ? COFOUNDER_PERMISSIONS
          : CEO_PERMISSIONS,
    };

    const enteredPassword = String(request.data?.enteredPassword || "").trim();
    const sessionTimeoutMinutes = Math.min(
      Math.max(Number(request.data?.sessionTimeoutMinutes || 15), 5),
      120
    );
    const nowMs = Date.now();
    const expiresAtMs = resolvedSpec.founderSessionPersistent
      ? nowMs + 365 * 24 * 60 * 60 * 1000
      : nowMs + sessionTimeoutMinutes * 60 * 1000;
    const sessionExpiresAt = admin.firestore.Timestamp.fromMillis(expiresAtMs);
    const serverNow = admin.firestore.FieldValue.serverTimestamp();

    if (!adminSnap.exists) {
      if (enteredPassword.length < 6 || enteredPassword.length > 128) {
        throw new HttpsError(
          "invalid-argument",
          "Administrative password must be between 6 and 128 characters."
        );
      }

      const salt = `sc_salt_${crypto.randomBytes(16).toString("hex")}`;
      const passwordVerifierHash = hashAdminPassword(enteredPassword, salt);

      await adminRef.set({
        adminUid: callerUid,
        canonicalAccountId: resolvedSpec.canonicalAccountId,
        adminRole: resolvedSpec.adminRole,
        displayName: resolvedSpec.displayName,
        username: resolvedSpec.defaultUsername,
        adminEmail: callerEmail,
        accessGroup: resolvedSpec.accessGroup,
        permissions: resolvedSpec.permissions,
        passwordVerifierHash,
        passwordSalt: salt,
        mustChangePassword: false,
        failedAttempts: 0,
        lockedUntilEpochMs: 0,
        founderSessionPersistent: resolvedSpec.founderSessionPersistent,
        sessionExpiresAt,
        createdAt: serverNow,
        updatedAt: serverNow,
      });
    } else {
      const lockedUntilEpochMs = Number(existingData?.lockedUntilEpochMs || 0);
      if (lockedUntilEpochMs > nowMs) {
        const remainingSec = Math.max(1, Math.ceil((lockedUntilEpochMs - nowMs) / 1000));
        throw new HttpsError(
          "resource-exhausted",
          `Administrative login is locked due to 3 failed password attempts. Please try again in ${remainingSec}s.`
        );
      }

      if (enteredPassword.length < 6 || enteredPassword.length > 128) {
        throw new HttpsError(
          "invalid-argument",
          "Administrative password must be between 6 and 128 characters."
        );
      }

      const existingSalt = String(existingData?.passwordSalt || "");
      const existingHash = String(existingData?.passwordVerifierHash || "");
      const verification = verifyAdminPasswordConstantTime(
        enteredPassword,
        existingSalt,
        existingHash
      );

      if (!verification.valid) {
        const currentFailed = Number(existingData?.failedAttempts || 0);
        const baseAttempts = currentFailed >= 3 ? 0 : currentFailed;
        const nextFailed = baseAttempts + 1;
        const lockUntil = nextFailed >= 3 ? nowMs + 60_000 : 0;

        await adminRef.update({
          failedAttempts: nextFailed,
          lockedUntilEpochMs: lockUntil,
          updatedAt: serverNow,
        });

        await db.collection("audit_logs").add({
          adminId: callerUid,
          adminEmail: callerEmail.slice(0, 120),
          actionType: "EXECUTIVE_ADMIN_LOGIN_FAILED",
          targetCollection: "admins",
          targetId: callerUid,
          reason: `Failed secondary password attempt ${nextFailed} of 3 (${lockUntil > 0 ? "60s lockout triggered" : "active"})`,
          previousState: `failedAttempts=${baseAttempts}`,
          newState: `failedAttempts=${nextFailed}`,
          createdAt: serverNow,
        });

        if (lockUntil > 0) {
          throw new HttpsError(
            "resource-exhausted",
            "Invalid administrative password. 3 failed attempts reached — locked for 60 seconds."
          );
        } else {
          const remaining = Math.max(0, 3 - nextFailed);
          throw new HttpsError(
            "permission-denied",
            `Invalid administrative password (Attempt ${nextFailed} of 3 — ${remaining} attempts remaining before 60s lockout).`
          );
        }
      }

      const updatePayload: Record<string, unknown> = {
        adminRole: resolvedSpec.adminRole,
        canonicalAccountId: resolvedSpec.canonicalAccountId,
        displayName: resolvedSpec.displayName,
        username: resolvedSpec.defaultUsername,
        adminEmail: callerEmail,
        accessGroup: resolvedSpec.accessGroup,
        permissions: resolvedSpec.permissions,
        founderSessionPersistent: resolvedSpec.founderSessionPersistent,
        failedAttempts: 0,
        lockedUntilEpochMs: 0,
        sessionExpiresAt,
        updatedAt: serverNow,
      };

      // Transparently upgrade legacy single-round SHA-256 hash to scrypt on successful login
      if (verification.needsRehash) {
        const upgradedSalt = `sc_salt_${crypto.randomBytes(16).toString("hex")}`;
        updatePayload.passwordSalt = upgradedSalt;
        updatePayload.passwordVerifierHash = hashAdminPassword(enteredPassword, upgradedSalt);
      }

      await adminRef.update(updatePayload);
    }

    // Set Firebase Auth Custom Claims so backend token reflects verified role & access group
    await admin.auth().setCustomUserClaims(callerUid, {
      adminRole: resolvedSpec.adminRole,
      canonicalAccountId: resolvedSpec.canonicalAccountId,
      accessGroup: resolvedSpec.accessGroup,
      isAuthorizedAdmin: true,
    });

    // Write immutable Admin Audit Log
    await db.collection("audit_logs").add({
      adminId: callerUid,
      adminEmail: callerEmail.slice(0, 120),
      actionType: `EXECUTIVE_ADMIN_LOGIN_${resolvedSpec.adminRole.toUpperCase()}`,
      targetCollection: "admins",
      targetId: callerUid,
      reason: `Verified Google Admin login (${callerEmail}) -> Role: ${resolvedSpec.roleLabel} (${resolvedSpec.accessGroupLabel})`,
      previousState: "authenticated",
      newState: `${resolvedSpec.canonicalAccountId} (${resolvedSpec.adminRole})`,
      createdAt: serverNow,
    });

    return {
      adminUid: callerUid,
      adminEmail: callerEmail,
      adminRole: resolvedSpec.adminRole,
      roleLabel: resolvedSpec.roleLabel,
      canonicalAccountId: resolvedSpec.canonicalAccountId,
      displayName: resolvedSpec.displayName,
      username: resolvedSpec.defaultUsername,
      accessGroup: resolvedSpec.accessGroup,
      accessGroupLabel: resolvedSpec.accessGroupLabel,
      permissions: resolvedSpec.permissions,
      founderSessionPersistent: resolvedSpec.founderSessionPersistent,
      sessionExpiresAtEpochMs: expiresAtMs,
    };
  }
);

/**
 * 3. Founder-Only Admin Account Management (`founderManageAuthorizedAdmin`):
 * Allows ONLY the Founder (`abhishek.buisness20@gmail.com`) to update or reassign
 * an admin account in `/admins/{targetAdminUid}` and synchronizes Firebase Auth Custom Claims.
 */
export const founderManageAuthorizedAdmin = onCall(
  { enforceAppCheck: true, cors: ALLOWED_CORS_ORIGINS },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Authentication is required.");
    }

    const callerUid = request.auth.uid;
    const callerEmail = String(request.auth.token.email || "").trim().toLowerCase();
    const emailVerified = request.auth.token.email_verified === true;

    await enforceRateLimit(callerUid, "founder_manage_admin", 15, 60_000);

    const callerSpec = resolveAuthorizedAdminByEmail(callerEmail);
    const callerAdminSnap = await db.collection("admins").doc(callerUid).get();
    const isFounderCaller =
      (emailVerified && callerSpec?.adminRole === "founder") ||
      callerAdminSnap.data()?.adminRole === "founder";

    if (!isFounderCaller) {
      throw new HttpsError(
        "permission-denied",
        "Only the SkillConnect Founder is authorized to modify or reassign Admin accounts."
      );
    }

    const targetAdminUid = sanitizeInput(request.data?.targetAdminUid, 128);
    const newRole = sanitizeInput(request.data?.newRole, 32).toLowerCase();
    const newPasswordOptional = String(request.data?.newPasswordOptional || "").trim();

    if (!targetAdminUid || !/^[a-zA-Z0-9_-]{1,128}$/.test(targetAdminUid)) {
      throw new HttpsError("invalid-argument", "A valid targetAdminUid is required.");
    }
    if (!["founder", "cofounder", "ceo"].includes(newRole)) {
      throw new HttpsError("invalid-argument", "newRole must be founder, cofounder, or ceo.");
    }

    const roleConfig =
      newRole === "founder"
        ? {
            canonicalAccountId: "ADM-FND-001",
            displayName: "Founder",
            username: "Abhisheksingh",
            accessGroup: "founder_admin_group",
            founderSessionPersistent: true,
            permissions: FOUNDER_PERMISSIONS,
          }
        : newRole === "cofounder"
        ? {
            canonicalAccountId: "ADM-COF-001",
            displayName: "Abhay",
            username: "Abhay",
            accessGroup: "founder_admin_group",
            founderSessionPersistent: false,
            permissions: COFOUNDER_PERMISSIONS,
          }
        : {
            canonicalAccountId: "ADM-CEO-001",
            displayName: "CEO",
            username: "Abhay",
            accessGroup: "ceo_dashboard_group",
            founderSessionPersistent: false,
            permissions: CEO_PERMISSIONS,
          };

    const updates: Record<string, unknown> = {
      adminRole: newRole,
      canonicalAccountId: roleConfig.canonicalAccountId,
      displayName: roleConfig.displayName,
      username: roleConfig.username,
      accessGroup: roleConfig.accessGroup,
      permissions: roleConfig.permissions,
      founderSessionPersistent: roleConfig.founderSessionPersistent,
      failedAttempts: 0,
      lockedUntilEpochMs: 0,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    if (newPasswordOptional.length >= 6 && newPasswordOptional.length <= 128) {
      const newSalt = `sc_salt_${crypto.randomBytes(16).toString("hex")}`;
      updates.passwordSalt = newSalt;
      updates.passwordVerifierHash = hashAdminPassword(newPasswordOptional, newSalt);
      updates.mustChangePassword = true;
    }

    const targetRef = db.collection("admins").doc(targetAdminUid);
    await targetRef.set(updates, { merge: true });

    // Synchronize Firebase Auth Custom Claims for the target admin UID
    await admin.auth().setCustomUserClaims(targetAdminUid, {
      adminRole: newRole,
      canonicalAccountId: roleConfig.canonicalAccountId,
      accessGroup: roleConfig.accessGroup,
      isAuthorizedAdmin: true,
    });

    await db.collection("audit_logs").add({
      adminId: callerUid,
      adminEmail: (callerEmail || "founder@skillconnect.in").slice(0, 120),
      actionType: "FOUNDER_REASSIGN_OR_RESET_ADMIN",
      targetCollection: "admins",
      targetId: targetAdminUid,
      reason: `Founder updated admin ${targetAdminUid} to role=${newRole} (${roleConfig.accessGroup})`,
      previousState: "existing_admin",
      newState: `${roleConfig.canonicalAccountId} (${newRole})`,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return {
      targetAdminUid,
      adminRole: newRole,
      canonicalAccountId: roleConfig.canonicalAccountId,
      accessGroup: roleConfig.accessGroup,
      permissions: roleConfig.permissions,
    };
  }
);

/**
 * 4. Direct UPI Subscription Approval (`approveSubscriptionPayment`):
 * Verifies caller is an authorized Founder, Co-Founder, or CEO before activating Premium.
 */
export const approveSubscriptionPayment = onCall(
  { enforceAppCheck: true, cors: ALLOWED_CORS_ORIGINS },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Authentication is required.");
    }

    const callerUid = request.auth.uid;
    const callerEmail = (request.auth.token.email || "").toLowerCase();
    const emailVerified = request.auth.token.email_verified === true;

    await enforceRateLimit(callerUid, "approve_subscription", 30, 60_000);

    const paymentId = sanitizeInput(request.data?.paymentId, 128);
    const verificationNote = sanitizeInput(request.data?.verificationNote, 500);

    if (!paymentId || !/^[a-zA-Z0-9_-]{1,128}$/.test(paymentId)) {
      throw new HttpsError("invalid-argument", "A valid paymentId is required.");
    }
    if (verificationNote.length < 8 || verificationNote.length > 500) {
      throw new HttpsError(
        "invalid-argument",
        "verificationNote must be at least 8 characters confirming the merchant statement check."
      );
    }

    // Verify caller is an active Founder, Co-Founder, or CEO
    const adminSnap = await db.collection("admins").doc(callerUid).get();
    const adminData = adminSnap.data();
    const authorizedSpec = emailVerified ? resolveAuthorizedAdminByEmail(callerEmail) : null;

    let isAuthorizedReviewer = authorizedSpec !== null;
    if (!isAuthorizedReviewer && adminSnap.exists && adminData) {
      const role = String(adminData.adminRole || "");
      const expiresAt = adminData.sessionExpiresAt?.toDate?.() as Date | undefined;
      if (role === "founder" || role === "cofounder") {
        isAuthorizedReviewer = true;
      } else if (role === "ceo" && expiresAt && expiresAt.getTime() >= Date.now()) {
        isAuthorizedReviewer = true;
      }
    }

    if (!isAuthorizedReviewer) {
      throw new HttpsError(
        "permission-denied",
        "Only an active Founder, Co-Founder, or CEO can approve subscription payments."
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
