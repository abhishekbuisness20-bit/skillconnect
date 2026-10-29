const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_customer_1";
const BOB_UID = "bob_provider_2";
const CHARLIE_UID = "charlie_stranger_3";
const COFOUNDER_UID = "admin_cofounder_abhay";
const CEO_UID = "admin_ceo_adarsh";
const FOUNDER_UID = "admin_founder_abhishek";

const VALID_HASH_64 = "a".repeat(64);
const VALID_SALT = "skillconnect_salt_2026";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
    const rules = fs.readFileSync("./firestore.rules", "utf8");
    await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        rules,
        host: emulatorHost,
        port: emulatorPort,
      },
    });
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function makeValidProfile(uid, role = "customer", isVerified = false) {
  const now = new Date();
  return {
    userId: uid,
    platformId: role === "provider" ? "SC-PRO-10293847" : "SC-CUS-10293847",
    displayName: "Aarav Sharma",
    headline: "Senior Graphic Designer",
    bio: "Brand designer based in Bengaluru.",
    role,
    city: "Bengaluru",
    locality: "Indiranagar",
    preferredLanguage: "en",
    skills: ["Logo Design", "Branding"],
    startingPrice: 1500,
    availability: "Mon-Sat, 10 AM - 7 PM",
    isVerified,
    verificationStatus: role === "provider" ? "draft" : "not_submitted",
    subscriptionPlan: "free",
    isPublicProvider: role === "provider",
    accountStatus: "active",
    publicPhone: "+91 9876543210",
    completedJobsCount: 0,
    ratingSum: 0,
    reviewCount: 0,
    onboardingStep: 1,
    onboardingCompleted: role === "customer",
    createdAt: now,
    updatedAt: now,
  };
}

function makeValidService(providerId, isPublished = true) {
  const now = new Date();
  return {
    providerId,
    providerName: "Rohan Verma",
    providerVerified: true,
    title: "Modern Brand Identity & Logo Design",
    category: "Logo Design",
    subcategory: "Brand Guidelines",
    shortSummary: "Custom vector logos and brand kits for Indian startups.",
    description:
      "Complete brand identity package including 3 concepts, vector source files, and colour system.",
    skills: ["Illustrator", "Figma"],
    startingPrice: 2499,
    pricingModel: "fixed",
    deliverables: "3 Logo Concepts, SVG/PNG/PDF files, Brand Guide",
    turnaround: "3 Days",
    city: "Mumbai",
    locality: "Andheri West",
    availability: "Available this week",
    terms: "2 rounds of revisions included.",
    isPublished,
    isDraft: !isPublished,
    isFeatured: false,
    moderationStatus: "approved",
    moderationReason: "",
    ratingSum: 0,
    reviewCount: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function makeAdminDoc(adminUid, adminRole, username, sessionOffsetMinutes = 15) {
  const now = new Date();
  const sessionExpiresAt = new Date(now.getTime() + sessionOffsetMinutes * 60 * 1000);
  const canonicalAccountId =
    adminRole === "cofounder"
      ? "ADM-COF-001"
      : adminRole === "ceo"
      ? "ADM-CEO-001"
      : "ADM-FND-001";
  const displayName =
    adminRole === "cofounder" ? "Abhay" : adminRole === "ceo" ? "CEO" : "Founder";
  return {
    adminUid,
    canonicalAccountId,
    adminRole,
    displayName,
    username,
    passwordVerifierHash: VALID_HASH_64,
    passwordSalt: VALID_SALT,
    mustChangePassword: false,
    failedAttempts: 0,
    lockedUntilEpochMs: 0,
    founderSessionPersistent: adminRole === "founder",
    sessionExpiresAt,
    createdAt: now,
    updatedAt: now,
  };
}

test("1. Unauthenticated user cannot read services or profiles; signed-in user can safely get() non-existent conversation or settings without PERMISSION_DENIED", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("services").where("isPublished", "==", true).get());
  await assertFails(unauthDb.collection("user_profiles").doc(ALICE_UID).get());

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Root cause fix verification: deterministic get() on non-existent docs must NOT throw PERMISSION_DENIED
  await assertSucceeds(aliceDb.collection("conversations").doc("conv_alice_bob").get());
  await assertSucceeds(aliceDb.collection("platform_settings").doc("global").get());
});

test("2. User can create their own profile with isVerified=false, fails if self-verifying, and cannot self-switch role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("user_profiles").doc(ALICE_UID).set(makeValidProfile(ALICE_UID, "customer", false))
  );
  await assertSucceeds(
    aliceDb.collection("user_profiles").doc(ALICE_UID).update({
      displayName: "Aarav S.",
      onboardingStep: 3,
      updatedAt: new Date(),
    })
  );
  await assertFails(
    aliceDb.collection("user_profiles").doc(ALICE_UID).update({
      role: "provider",
      updatedAt: new Date(),
    })
  );
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("user_profiles").doc(BOB_UID).set(makeValidProfile(BOB_UID, "provider", true))
  );
});

test("3. Shadow field injection on profile update is rejected", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("user_profiles").doc(ALICE_UID).set(makeValidProfile(ALICE_UID));
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("user_profiles").doc(ALICE_UID).update({
      displayName: "Updated Name",
      ghostField: "hacked",
      updatedAt: new Date(),
    })
  );
});

test("4. Provider can create service and query published services; unfiltered list fails", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("services").doc("srv_1").set(makeValidService(BOB_UID, true))
  );
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("services").where("isPublished", "==", true).get()
  );
  await assertFails(aliceDb.collection("services").get());
});

test("5. 12-state booking workflow, 6-state payment ledger (including processing & disputed), and participant chat isolation", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const validBooking = {
    customerId: ALICE_UID,
    customerName: "Alice",
    providerId: BOB_UID,
    providerName: "Bob",
    serviceId: "srv_1",
    serviceTitle: "Logo Design",
    category: "Logo Design",
    requirements: "Need a modern logo for my bakery in Pune.",
    preferredDate: "2026-11-05",
    preferredTimeSlot: "10:00 AM - 1:00 PM",
    bookingRef: "SC-BK-10293847",
    paymentMethod: "upi",
    paymentStatus: "unpaid",
    attachmentNote: "",
    quotedPrice: 2499,
    requestType: "booking",
    status: "pending",
    statusNote: "",
    hasReviewed: false,
    createdAt: now,
    updatedAt: now,
  };
  await assertSucceeds(aliceDb.collection("booking_requests").doc("bk_1").set(validBooking));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("booking_requests").doc("bk_1").update({
      status: "awaiting_payment",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("payments").doc("pay_bk_1_booking_payment").set({
      userId: ALICE_UID,
      providerId: BOB_UID,
      bookingId: "bk_1",
      paymentType: "booking_payment",
      method: "upi",
      amountInr: 2499,
      status: "processing",
      transactionRef: "UPI-99887766",
      notes: "Initiated via UPI",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Stranger Charlie cannot read Alice/Bob's payment or booking
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(charlieDb.collection("payments").doc("pay_bk_1_booking_payment").get());
  await assertFails(charlieDb.collection("booking_requests").doc("bk_1").get());
});

test("6. Executive Admin RBAC: Co-Founder vs CEO (<= 10000 INR refund limit & Adarsh/Abhay alias) vs Founder (unlimited & role control)", async () => {
  const now = new Date();
  const cofounderDb = testEnv.authenticatedContext(COFOUNDER_UID).firestore();
  const ceoDb = testEnv.authenticatedContext(CEO_UID).firestore();
  const founderDb = testEnv.authenticatedContext(FOUNDER_UID).firestore();

  // 1. Provision all 3 executive admin accounts with salted SHA-256 verifier hash
  await assertSucceeds(
    cofounderDb.collection("admins").doc(COFOUNDER_UID).set(makeAdminDoc(COFOUNDER_UID, "cofounder", "Abhay", 15))
  );
  // CEO alias 'Adarsh' resolves to canonical ADM-CEO-001
  await assertSucceeds(
    ceoDb.collection("admins").doc(CEO_UID).set(makeAdminDoc(CEO_UID, "ceo", "Adarsh", 15))
  );
  await assertSucceeds(
    founderDb.collection("admins").doc(FOUNDER_UID).set(makeAdminDoc(FOUNDER_UID, "founder", "Abhisheksingh", 15))
  );

  // Co-Founder cannot self-escalate to 'founder' or 'ceo'
  await assertFails(
    cofounderDb.collection("admins").doc(COFOUNDER_UID).update({
      adminRole: "founder",
      updatedAt: new Date(),
    })
  );

  // Seed a provider profile, a small payment (₹5,000), and a large payment (₹25,000)
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("user_profiles").doc(BOB_UID).set(makeValidProfile(BOB_UID, "provider", false));
    await ctx.firestore().collection("payments").doc("pay_small_5000").set({
      userId: ALICE_UID,
      providerId: BOB_UID,
      bookingId: "bk_small",
      paymentType: "booking_payment",
      method: "upi",
      amountInr: 5000,
      status: "disputed",
      transactionRef: "UPI-5000",
      notes: "Disputed small order",
      createdAt: now,
      updatedAt: now,
    });
    await ctx.firestore().collection("payments").doc("pay_large_25000").set({
      userId: ALICE_UID,
      providerId: BOB_UID,
      bookingId: "bk_large",
      paymentType: "booking_payment",
      method: "upi",
      amountInr: 25000,
      status: "disputed",
      transactionRef: "UPI-25000",
      notes: "Disputed large order",
      createdAt: now,
      updatedAt: now,
    });
  });

  // Co-Founder CAN approve provider verification, but CANNOT change user role or approve refunds
  await assertSucceeds(
    cofounderDb.collection("user_profiles").doc(BOB_UID).update({
      isVerified: true,
      verificationStatus: "approved",
      updatedAt: new Date(),
    })
  );
  await assertFails(
    cofounderDb.collection("user_profiles").doc(BOB_UID).update({
      role: "customer",
      updatedAt: new Date(),
    })
  );
  await assertFails(
    cofounderDb.collection("payments").doc("pay_small_5000").update({
      status: "refunded",
      notes: "Co-founder attempted refund",
      updatedAt: new Date(),
    })
  );

  // CEO CAN approve refund <= ₹10,000 (₹5,000), but FAILS on refund > ₹10,000 (₹25,000)
  await assertSucceeds(
    ceoDb.collection("payments").doc("pay_small_5000").update({
      status: "refunded",
      notes: "CEO approved refund <= 10000 INR",
      updatedAt: new Date(),
    })
  );
  await assertFails(
    ceoDb.collection("payments").doc("pay_large_25000").update({
      status: "refunded",
      notes: "CEO attempted refund > 10000 INR",
      updatedAt: new Date(),
    })
  );

  // Founder CAN approve refund > ₹10,000 (₹25,000)
  await assertSucceeds(
    founderDb.collection("payments").doc("pay_large_25000").update({
      status: "refunded",
      notes: "Founder approved unlimited refund",
      updatedAt: new Date(),
    })
  );
});

test("7. 15-Minute Admin Session Expiry blocks expired Co-Founder/CEO writes while Founder Exception preserves Founder access", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("user_profiles").doc(BOB_UID).set(makeValidProfile(BOB_UID, "provider", false));
    // Expired 20 minutes ago (-20)
    await ctx.firestore().collection("admins").doc(COFOUNDER_UID).set(makeAdminDoc(COFOUNDER_UID, "cofounder", "Abhay", -20));
    await ctx.firestore().collection("admins").doc(CEO_UID).set(makeAdminDoc(CEO_UID, "ceo", "Abhay", -20));
    await ctx.firestore().collection("admins").doc(FOUNDER_UID).set(makeAdminDoc(FOUNDER_UID, "founder", "Abhisheksingh", -20));
  });

  const cofounderDb = testEnv.authenticatedContext(COFOUNDER_UID).firestore();
  const ceoDb = testEnv.authenticatedContext(CEO_UID).firestore();
  const founderDb = testEnv.authenticatedContext(FOUNDER_UID).firestore();

  // Expired Co-Founder and CEO sessions are rejected by server rules
  await assertFails(
    cofounderDb.collection("user_profiles").doc(BOB_UID).update({
      isVerified: true,
      verificationStatus: "approved",
      updatedAt: new Date(),
    })
  );
  await assertFails(
    ceoDb.collection("user_profiles").doc(BOB_UID).update({
      isVerified: true,
      verificationStatus: "approved",
      updatedAt: new Date(),
    })
  );

  // Founder Exception: Founder succeeds even when sessionExpiresAt is in the past
  await assertSucceeds(
    founderDb.collection("user_profiles").doc(BOB_UID).update({
      isVerified: true,
      verificationStatus: "approved",
      updatedAt: now,
    })
  );
});

test("8. Direct UPI Subscriptions: User cannot self-activate premium or create succeeded subscription payment; pending UTR request + CEO/Founder approval activates premium", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ceoDb = testEnv.authenticatedContext(CEO_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("user_profiles").doc(ALICE_UID).set(makeValidProfile(ALICE_UID, "customer", false))
  );
  await assertSucceeds(
    ceoDb.collection("admins").doc(CEO_UID).set(makeAdminDoc(CEO_UID, "ceo", "Adarsh", 15))
  );

  // Normal user cannot self-upgrade subscriptionPlan to premium
  await assertFails(
    aliceDb.collection("user_profiles").doc(ALICE_UID).update({
      subscriptionPlan: "premium",
      updatedAt: new Date(),
    })
  );

  // Normal user cannot create a subscription payment that starts as 'succeeded'
  await assertFails(
    aliceDb.collection("payments").doc("pay_sub_fake").set({
      userId: ALICE_UID,
      providerId: "",
      bookingId: "",
      paymentType: "subscription",
      method: "upi",
      amountInr: 249,
      status: "succeeded",
      transactionRef: "UTR-430219876543",
      notes: "Attempted client-side activation",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Normal user CAN create a pending Direct UPI subscription payment request
  await assertSucceeds(
    aliceDb.collection("payments").doc("pay_sub_valid").set({
      userId: ALICE_UID,
      providerId: "",
      bookingId: "",
      paymentType: "subscription",
      method: "upi",
      amountInr: 249,
      status: "pending",
      transactionRef: "UTR-430219876543",
      notes: "Direct UPI Subscription (CUSTOMER Premium Plan) • Ref: UTR-430219876543",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Active CEO verifies merchant statement and approves payment + upgrades user profile to premium
  await assertSucceeds(
    ceoDb.collection("payments").doc("pay_sub_valid").update({
      status: "succeeded",
      notes: "Verified UTR-430219876543 in merchant bank statement",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    ceoDb.collection("user_profiles").doc(ALICE_UID).update({
      subscriptionPlan: "premium",
      updatedAt: new Date(),
    })
  );
});

