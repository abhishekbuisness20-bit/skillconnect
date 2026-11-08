package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SkillConnectRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveAndGetUserProfile_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = SkillConnectFirestoreRepository(firestore)

    val profile = UserProfile(
      id = uid,
      name = "Alice Sharma",
      email = ALICE_EMAIL,
      phone = "+91 9876543210",
      role = UserRole.CUSTOMER,
      city = "Mumbai",
      state = "Maharashtra",
      professionalHeadline = "Tech Founder",
      skills = listOf("Product", "Design")
    )

    val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveUserProfile(profile) }
    assertTrue("Expected saveUserProfile to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

    val fetchResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getUserProfile(uid) }
    assertTrue(fetchResult.isSuccess)
    val savedDoc = fetchResult.getOrNull()
    assertNotNull(savedDoc)
    assertEquals("Alice Sharma", savedDoc?.name)
    assertEquals("Tech Founder", savedDoc?.professionalHeadline)
  }

  @Test
  fun getUserProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = SkillConnectFirestoreRepository(firestore)
    val profile = UserProfile(
      id = aliceUid,
      name = "Alice Private",
      email = ALICE_EMAIL,
      phone = "+91 9876543210",
      role = UserRole.CUSTOMER,
      city = "Mumbai",
      state = "Maharashtra"
    )
    withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.saveUserProfile(profile).getOrThrow() }

    signInTestUser(BOB_EMAIL)
    val bobRepo = SkillConnectFirestoreRepository(firestore)
    val bobReadResult = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getUserProfile(aliceUid) }
    assertTrue("Cross-user profile read should fail", bobReadResult.isFailure)
    val firestoreEx = bobReadResult.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull("Expected FirebaseFirestoreException", firestoreEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
  }

  @Test
  fun createAndObserveWorkRequests_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = SkillConnectFirestoreRepository(firestore)

    val requestId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createWorkRequest(
        customerName = "Alice Sharma",
        providerId = "prov_ananya",
        providerName = "Ananya Deshmukh",
        title = "Mobile UI Redesign",
        description = "Design 5 screens in Figma",
        category = "Website & Development",
        budgetInr = 15000L,
        deadline = "10 Days"
      ).getOrThrow()
    }
    assertTrue(requestId.isNotEmpty())

    val emittedList = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeUserWorkRequests().first { list -> list.any { it.id == requestId } }
    }
    assertTrue(emittedList.any { it.id == requestId && it.title == "Mobile UI Redesign" })
  }

  @Test
  fun getWorkRequestById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val aliceRepo = SkillConnectFirestoreRepository(firestore)
    val requestId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createWorkRequest(
        customerName = "Alice Sharma",
        providerId = "prov_ananya",
        providerName = "Ananya Deshmukh",
        title = "Private Contract",
        description = "Confidential scope",
        category = "Website & Development",
        budgetInr = 25000L,
        deadline = "14 Days"
      ).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = SkillConnectFirestoreRepository(firestore)
    val bobResult = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getWorkRequestById(requestId) }
    assertTrue(bobResult.isFailure)
    val firestoreEx = bobResult.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(firestoreEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
  }

  @Test
  fun observeUserWorkRequests_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = SkillConnectFirestoreRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeUserWorkRequests().first()
      }
      fail("Expected unauthenticated query to fail with PERMISSION_DENIED")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }
  }

  @Test
  fun syncAndGetFcmToken_authenticatedOwner_succeedsAndDeactivatesCleanly() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repository = SkillConnectFirestoreRepository(firestore)
    val sampleToken = "fcm_token_valid_registration_sample_9876543210"

    val syncRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.syncFcmToken(sampleToken, tokenId = "android_primary")
    }
    assertTrue("Expected syncFcmToken to succeed: ${syncRes.exceptionOrNull()}", syncRes.isSuccess)

    val fetchRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.getFcmTokenDoc(aliceUid, tokenId = "android_primary")
    }
    assertTrue(fetchRes.isSuccess)
    val doc = fetchRes.getOrNull()
    assertNotNull(doc)
    assertEquals(aliceUid, doc?.userId)
    assertEquals(sampleToken, doc?.fcmToken)
    assertEquals(FirebaseTokenManager.EXPECTED_PROJECT_ID, doc?.projectId)
    assertEquals(FirebaseTokenManager.EXPECTED_SENDER_ID, doc?.senderId)
    assertEquals(FirebaseTokenManager.EXPECTED_PACKAGE_NAME, doc?.packageName)
    assertTrue(doc?.isActive == true)

    val deactivateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.deactivateFcmToken(tokenId = "android_primary")
    }
    assertTrue(deactivateRes.isSuccess)
    val deactivatedDoc = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.getFcmTokenDoc(aliceUid, tokenId = "android_primary").getOrNull()
    }
    assertEquals(false, deactivatedDoc?.isActive)

    // Verify offline queueing and flush upon reconnect
    try {
      NetworkConnectivityMonitor.setSimulatedOnlineStateForTesting(false)
      val offlineToken = "fcm_token_queued_while_offline_555666777888"
      val queuedRes = repository.syncFcmToken(offlineToken, tokenId = "android_primary")
      assertTrue(queuedRes.isSuccess)
      assertTrue(repository.hasPendingOfflineFcmSync())

      NetworkConnectivityMonitor.setSimulatedOnlineStateForTesting(true)
      val flushRes = withTimeout(DEFAULT_TIMEOUT_MS) { repository.flushPendingOfflineSync() }
      assertTrue(flushRes.isSuccess)
      val flushedDoc = withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.getFcmTokenDoc(aliceUid, tokenId = "android_primary").getOrNull()
      }
      assertEquals(offlineToken, flushedDoc?.fcmToken)
      assertEquals(true, flushedDoc?.isActive)
    } finally {
      NetworkConnectivityMonitor.setSimulatedOnlineStateForTesting(null)
    }
  }

  @Test
  fun getFcmTokenDoc_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = SkillConnectFirestoreRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.syncFcmToken("fcm_token_alice_private_1234567890").getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = SkillConnectFirestoreRepository(firestore)
    val bobFetch = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.getFcmTokenDoc(aliceUid, tokenId = "android_primary")
    }
    assertTrue("Cross-user FCM token read must fail", bobFetch.isFailure)
    val firestoreEx = bobFetch.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(firestoreEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@skillconnect.in"
    const val BOB_EMAIL = "bob@skillconnect.in"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
