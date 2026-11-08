const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read users or work_requests", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("work_requests").get());
});

test("Authenticated user: can create and read their own user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice Sharma",
      email: "alice@example.in",
      role: "CUSTOMER",
      city: "Mumbai",
      state: "Maharashtra",
      skills: ["Figma", "Product"],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: cannot read another user's PII profile (PII Blanket Test)", async () => {
  const now = new Date(Date.now() - 1000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      name: "Bob Verma",
      email: "bob@example.in",
      role: "CUSTOMER",
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
});

test("Authenticated user: cannot self-escalate role to ADMIN", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice Hacker",
      email: "alice@example.in",
      role: "ADMIN",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user: cannot inject shadow field on work_request creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(
    aliceDb.collection("work_requests").doc("req_1").set({
      userId: ALICE_UID,
      customerName: "Alice",
      providerId: "prov_ananya",
      providerName: "Ananya",
      title: "App Design",
      description: "Design 5 screens",
      category: "Website & Development",
      budgetInr: 12000,
      deadline: "10 Days",
      status: "PENDING",
      ghostField: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user: can create and query their own work_requests with matching userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    aliceDb.collection("work_requests").doc("req_valid").set({
      userId: ALICE_UID,
      customerName: "Alice",
      providerId: "prov_ananya",
      providerName: "Ananya",
      title: "App Design",
      description: "Design 5 screens",
      category: "Website & Development",
      budgetInr: 12000,
      deadline: "10 Days",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("work_requests").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: fails query on work_requests without userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("work_requests").get());
});

test("Founder: can create and update admin_roles document", async () => {
  const founderDb = testEnv.authenticatedContext("founder_uid", {
    email: "abhishekbuisness20@gmail.com",
    role: "FOUNDER"
  }).firestore();
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    founderDb.collection("admin_roles").doc("ceo_uid").set({
      uid: "ceo_uid",
      email: "abhayskillconnect@gmail.com",
      role: "CEO",
      displayName: "Abhay (CEO)",
      isActive: true,
      updatedAt: now
    })
  );
});

test("Non-Founder Admin or Customer: cannot modify admin_roles document", async () => {
  const ceoDb = testEnv.authenticatedContext("ceo_uid", {
    email: "abhayskillconnect@gmail.com",
    role: "CEO"
  }).firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(
    ceoDb.collection("admin_roles").doc("admin_uid").set({
      uid: "admin_uid",
      email: "abhayskillconnect39@gmail.com",
      role: "ADMIN",
      displayName: "Adarsh (Admin)",
      isActive: true,
      updatedAt: now
    })
  );
});

test("Admin: can create audit log but cannot modify or delete it", async () => {
  const adminDb = testEnv.authenticatedContext("admin_uid", {
    email: "abhayskillconnect39@gmail.com",
    role: "ADMIN"
  }).firestore();
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    adminDb.collection("admin_audit_logs").doc("log_1").set({
      adminUid: "admin_uid",
      adminEmail: "abhayskillconnect39@gmail.com",
      roleType: "ADMIN",
      action: "VERIFY_PROVIDER",
      createdAt: now
    })
  );
  await assertFails(
    adminDb.collection("admin_audit_logs").doc("log_1").update({
      action: "MODIFIED_ACTION"
    })
  );
});

test("Authenticated user: can sync their own valid FCM token with verified projectId and senderId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("fcm_tokens")
      .doc("android_primary")
      .set({
        userId: ALICE_UID,
        fcmToken: "fcm_valid_registration_token_sample_1234567890",
        projectId: "gen-lang-client-0778223582",
        senderId: "800529463187",
        packageName: "com.aistudio.skillconnect.market",
        platform: "ANDROID",
        isActive: true,
        updatedAt: now,
      })
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("fcm_tokens")
      .doc("android_primary")
      .get()
  );
});

test("Authenticated user: cannot sync FCM token with mismatched senderId or for another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  // Wrong senderId must fail
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("fcm_tokens")
      .doc("android_primary")
      .set({
        userId: ALICE_UID,
        fcmToken: "fcm_valid_registration_token_sample_1234567890",
        projectId: "gen-lang-client-0778223582",
        senderId: "999999999999",
        packageName: "com.aistudio.skillconnect.market",
        platform: "ANDROID",
        isActive: true,
        updatedAt: now,
      })
  );

  // Writing to Bob's fcm_tokens subcollection must fail
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("fcm_tokens")
      .doc("android_primary")
      .set({
        userId: BOB_UID,
        fcmToken: "fcm_valid_registration_token_sample_1234567890",
        projectId: "gen-lang-client-0778223582",
        senderId: "800529463187",
        packageName: "com.aistudio.skillconnect.market",
        platform: "ANDROID",
        isActive: true,
        updatedAt: now,
      })
  );
});


