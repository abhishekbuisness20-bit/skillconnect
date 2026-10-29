package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BookingRequest
import com.example.data.model.PaymentDispute
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SkillConnectRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createProfileAndService_authenticatedProvider_generatesUniquePlatformIdAndEmitsInFlow() = runBlocking {
        val providerEmail = "provider_${UUID.randomUUID().toString().take(8)}@skillconnect.in"
        val providerUid = signInTestUser(providerEmail)
        val repository = SkillConnectRepository(firestore)

        val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createInitialProfile(
                displayName = "Vikram Nair",
                role = "provider",
                city = "Bengaluru",
                locality = "Koramangala",
                preferredLanguage = "en",
                headline = "Full-Stack Web Developer",
                bio = "Building high-performance web apps for Indian SMBs.",
                skills = listOf("React", "Kotlin"),
                startingPrice = 4999L
            )
        }
        assertTrue("Profile creation should succeed: ${profileRes.exceptionOrNull()}", profileRes.isSuccess)
        val createdProfile = profileRes.getOrThrow()
        assertTrue(
            "Provider platformId should start with SC-PRO-",
            createdProfile.platformId.startsWith("SC-PRO-")
        )

        val serviceRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createService(
                providerName = "Vikram Nair",
                providerVerified = false,
                title = "Business Website Development",
                category = "Website Development",
                subcategory = "Full-Stack",
                shortSummary = "Responsive modern website for local businesses.",
                description = "Complete custom website with SEO optimization, mobile responsive layout, and contact forms.",
                skills = listOf("Web", "SEO"),
                startingPrice = 4999L,
                pricingModel = "fixed",
                deliverables = "5-page responsive site + deployment",
                turnaround = "5 Days",
                city = "Bengaluru",
                locality = "Koramangala",
                availability = "Mon-Sat",
                terms = "50% milestone structure",
                isPublished = true
            )
        }
        assertTrue("Service creation should succeed: ${serviceRes.exceptionOrNull()}", serviceRes.isSuccess)
        val serviceId = serviceRes.getOrThrow()

        val services = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePublishedServices().first { list -> list.any { it.id == serviceId } }
        }
        assertTrue(services.any { it.id == serviceId && it.providerId == providerUid })
    }

    @Test
    fun bookingPaymentChatAndDisputeWorkflow_customerAndProviderIsolation_succeeds() = runBlocking {
        val providerEmail = "prov_${UUID.randomUUID().toString().take(8)}@skillconnect.in"
        val customerEmail = "cust_${UUID.randomUUID().toString().take(8)}@skillconnect.in"

        val providerUid = signInTestUser(providerEmail)
        val customerUid = signInTestUser(customerEmail)
        val customerRepo = SkillConnectRepository(firestore)

        // Safe getOrCreateConversation when conversation does not exist yet
        val convRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            customerRepo.getOrCreateConversation(
                customerId = customerUid,
                customerName = "Neha Gupta",
                providerId = providerUid,
                providerName = "Vikram Nair",
                serviceTitle = "Home Electrical Inspection"
            )
        }
        assertTrue("getOrCreateConversation should succeed without PERMISSION_DENIED: ${convRes.exceptionOrNull()}", convRes.isSuccess)

        val bookingRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            customerRepo.createBookingRequest(
                customerName = "Neha Gupta",
                providerId = providerUid,
                providerName = "Vikram Nair",
                serviceId = "srv_sample_1",
                serviceTitle = "Home Electrical Inspection",
                category = "Electrician",
                requirements = "Full MCB panel check and 3 ceiling fan installations in Indiranagar.",
                preferredDate = "2026-11-10",
                attachmentNote = "",
                quotedPrice = 899L,
                requestType = "booking",
                preferredTimeSlot = "10:00 AM - 1:00 PM",
                paymentMethod = "upi"
            )
        }
        assertTrue("Booking creation failed: ${bookingRes.exceptionOrNull()}", bookingRes.isSuccess)
        val bookingId = bookingRes.getOrThrow()

        // Sign in as provider and move to awaiting_payment
        signInTestUser(providerEmail)
        val providerRepo = SkillConnectRepository(firestore)
        val acceptRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            providerRepo.updateBookingStatusByProvider(bookingId, "awaiting_payment", "Accepted, please complete UPI payment")
        }
        assertTrue("Provider accept should succeed: ${acceptRes.exceptionOrNull()}", acceptRes.isSuccess)

        // Sign in as customer, create idempotent payment record, and confirm booking
        signInTestUser(customerEmail)
        val payRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            customerRepo.createPaymentRecord(
                providerId = providerUid,
                bookingId = bookingId,
                paymentType = "booking_payment",
                method = "upi",
                amountInr = 944L,
                status = "succeeded",
                transactionRef = "UPI-TEST-12345",
                notes = "Paid via UPI"
            )
        }
        assertTrue("Payment creation should succeed: ${payRes.exceptionOrNull()}", payRes.isSuccess)
        val paymentId = payRes.getOrThrow()

        val confirmRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            customerRepo.updateBookingStatusByCustomer(
                bookingId = bookingId,
                newStatus = "confirmed",
                statusNote = "Payment completed ($paymentId)",
                paymentMethod = "upi",
                paymentStatus = "paid_online",
                paymentReferenceId = paymentId
            )
        }
        assertTrue("Customer confirm should succeed: ${confirmRes.exceptionOrNull()}", confirmRes.isSuccess)

        // Provider marks completed
        signInTestUser(providerEmail)
        val completeRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            providerRepo.updateBookingStatusByProvider(bookingId, "completed", "All 3 fans installed and tested")
        }
        assertTrue("Provider complete should succeed: ${completeRes.exceptionOrNull()}", completeRes.isSuccess)

        // Stranger cannot read this booking
        val strangerEmail = "stranger_${UUID.randomUUID().toString().take(8)}@skillconnect.in"
        val strangerUid = signInTestUser(strangerEmail)
        val strangerRepo = SkillConnectRepository(firestore)

        var thrown: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                strangerRepo.observeCustomerBookings(customerUid).first()
            }
        } catch (e: Throwable) {
            thrown = e
        }
        assertNotNull("Expected cross-user booking query to fail", thrown)
        val fsEx = thrown?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException, got $thrown", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        assertTrue(strangerUid.isNotEmpty())
    }

    @Test
    fun executiveAdminRbac_coFounderCeoAliasAndFounderRefundThresholds_enforcedCorrectly() = runBlocking {
        val customerEmail = "cust_dsp_${UUID.randomUUID().toString().take(6)}@skillconnect.in"
        val providerEmail = "prov_dsp_${UUID.randomUUID().toString().take(6)}@skillconnect.in"
        val coFounderEmail = "cofounder_${UUID.randomUUID().toString().take(6)}@skillconnect.in"
        val ceoEmail = "ceo_${UUID.randomUUID().toString().take(6)}@skillconnect.in"
        val founderEmail = "founder_${UUID.randomUUID().toString().take(6)}@skillconnect.in"

        val providerUid = signInTestUser(providerEmail)
        val customerUid = signInTestUser(customerEmail)
        val customerRepo = SkillConnectRepository(firestore)

        // Create a ₹15,000 disputed booking (> ₹10,000 CEO threshold)
        val highBooking = BookingRequest(
            id = "bk_high_${UUID.randomUUID().toString().replace("-", "").take(10)}",
            customerId = customerUid,
            customerName = "Rohan Sharma",
            providerId = providerUid,
            providerName = "Vikram Nair",
            serviceId = "srv_web_1",
            serviceTitle = "E-Commerce Website Build",
            category = "Website Development",
            quotedPrice = 15000L,
            totalAmountInr = 15000L
        )
        val dspRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            customerRepo.createPaymentDispute(
                booking = highBooking,
                paymentId = "pay_high_1",
                reason = "Milestone deliverable incomplete",
                statement = "Checkout integration was not delivered by deadline.",
                evidenceNote = "Attached milestone checklist",
                isFiledByCustomer = true
            )
        }
        assertTrue("Dispute creation should succeed: ${dspRes.exceptionOrNull()}", dspRes.isSuccess)
        val disputeId = dspRes.getOrThrow()
        val disputeModel = PaymentDispute(
            id = disputeId,
            bookingId = highBooking.id,
            paymentId = "pay_high_1",
            customerId = customerUid,
            customerName = "Rohan Sharma",
            providerId = providerUid,
            providerName = "Vikram Nair",
            disputedAmountInr = 15000L,
            reason = "Milestone deliverable incomplete"
        )

        // 1. Provision Co-Founder (Abhay -> ADM-COF-001)
        signInTestUser(coFounderEmail)
        val coFounderRepo = SkillConnectRepository(firestore)
        val cofAuth = withTimeout(DEFAULT_TIMEOUT_MS) {
            coFounderRepo.authenticateOrProvisionExecutiveAdmin("cofounder", "Abhay", "CoFounderPass#1")
        }
        assertTrue("Co-Founder provisioning should succeed: ${cofAuth.exceptionOrNull()}", cofAuth.isSuccess)
        assertEquals("ADM-COF-001", cofAuth.getOrThrow().canonicalAccountId)

        // Co-Founder can escalate dispute to CEO, but CANNOT mark resolved_refunded
        val cofRefundAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            coFounderRepo.adminResolveOrEscalateDispute(
                dispute = disputeModel,
                newStatus = "resolved_refunded",
                assignedRole = "cofounder",
                decisionNote = "Unauthorized refund attempt"
            )
        }
        assertFalse("Co-Founder must NOT be allowed to directly approve a refund", cofRefundAttempt.isSuccess)

        val cofEscalate = withTimeout(DEFAULT_TIMEOUT_MS) {
            coFounderRepo.adminResolveOrEscalateDispute(
                dispute = disputeModel,
                newStatus = "escalated_to_ceo",
                assignedRole = "ceo",
                decisionNote = "Verified evidence, escalating to CEO"
            )
        }
        assertTrue("Co-Founder escalation to CEO should succeed: ${cofEscalate.exceptionOrNull()}", cofEscalate.isSuccess)

        // 2. Provision CEO with alias 'Adarsh' and verify login with alias 'Abhay' resolves to same ADM-CEO-001
        signInTestUser(ceoEmail)
        val ceoRepo = SkillConnectRepository(firestore)
        val ceoFirst = withTimeout(DEFAULT_TIMEOUT_MS) {
            ceoRepo.authenticateOrProvisionExecutiveAdmin("ceo", "Adarsh", "CeoPass#2026")
        }
        assertTrue("CEO provisioning with Adarsh should succeed: ${ceoFirst.exceptionOrNull()}", ceoFirst.isSuccess)
        assertEquals("ADM-CEO-001", ceoFirst.getOrThrow().canonicalAccountId)

        val ceoSecond = withTimeout(DEFAULT_TIMEOUT_MS) {
            ceoRepo.authenticateOrProvisionExecutiveAdmin("ceo", "Abhay", "CeoPass#2026")
        }
        assertTrue("CEO login with Abhay alias should resolve to same ADM-CEO-001 account", ceoSecond.isSuccess)
        assertEquals("ADM-CEO-001", ceoSecond.getOrThrow().canonicalAccountId)

        // CEO cannot approve ₹15,000 refund (> ₹10,000 limit), must escalate to Founder
        val ceoOverLimitAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            ceoRepo.adminResolveOrEscalateDispute(
                dispute = disputeModel,
                newStatus = "resolved_refunded",
                assignedRole = "ceo",
                decisionNote = "Attempting refund over 10k limit"
            )
        }
        assertFalse("CEO must NOT be allowed to approve refund > 10,000 INR", ceoOverLimitAttempt.isSuccess)

        val ceoEscalateFounder = withTimeout(DEFAULT_TIMEOUT_MS) {
            ceoRepo.adminResolveOrEscalateDispute(
                dispute = disputeModel,
                newStatus = "escalated_to_founder",
                assignedRole = "founder",
                decisionNote = "Amount exceeds 10,000 INR CEO cap; escalating to Founder"
            )
        }
        assertTrue("CEO escalation to Founder should succeed: ${ceoEscalateFounder.exceptionOrNull()}", ceoEscalateFounder.isSuccess)

        // 3. Provision Founder (Abhisheksingh -> ADM-FND-001) and approve ₹15,000 refund
        signInTestUser(founderEmail)
        val founderRepo = SkillConnectRepository(firestore)
        val founderAuth = withTimeout(DEFAULT_TIMEOUT_MS) {
            founderRepo.authenticateOrProvisionExecutiveAdmin("founder", "Abhisheksingh", "FounderPass#99")
        }
        assertTrue("Founder provisioning should succeed: ${founderAuth.exceptionOrNull()}", founderAuth.isSuccess)
        assertEquals("ADM-FND-001", founderAuth.getOrThrow().canonicalAccountId)
        assertTrue("Founder session must be persistent", founderAuth.getOrThrow().founderSessionPersistent)

        val founderApprove = withTimeout(DEFAULT_TIMEOUT_MS) {
            founderRepo.adminResolveOrEscalateDispute(
                dispute = disputeModel,
                newStatus = "resolved_refunded",
                assignedRole = "founder",
                decisionNote = "Founder approved full ₹15,000 refund"
            )
        }
        assertTrue("Founder unlimited refund approval should succeed: ${founderApprove.exceptionOrNull()}", founderApprove.isSuccess)
    }

    @Test
    fun observePublishedServices_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = SkillConnectRepository(firestore)

        var thrown: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observePublishedServices().first()
            }
        } catch (e: Throwable) {
            thrown = e
        }
        assertNotNull("Expected unauthenticated query to fail", thrown)
        val fsEx = thrown?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $thrown", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun directUpiSubscriptionRequestAndCeoApproval_enforcesPendingRequestAndActivatesPremium() = runBlocking {
        val customerEmail = "cust_upi_${UUID.randomUUID().toString().take(6)}@skillconnect.in"
        val ceoEmail = "ceo_upi_${UUID.randomUUID().toString().take(6)}@skillconnect.in"

        val customerUid = signInTestUser(customerEmail)
        val customerRepo = SkillConnectRepository(firestore)

        val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            customerRepo.createInitialProfile(
                displayName = "Ananya Verma",
                role = "customer",
                city = "Delhi",
                locality = "Connaught Place",
                preferredLanguage = "en"
            )
        }
        assertTrue("Profile creation should succeed: ${profileRes.exceptionOrNull()}", profileRes.isSuccess)

        val subReqRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            customerRepo.submitDirectUpiSubscriptionRequest(
                role = "customer",
                amountInr = 249L,
                utrReference = "430299887766",
                payerUpiNote = "Paid via GPay"
            )
        }
        assertTrue("Pending Direct UPI subscription request should succeed: ${subReqRes.exceptionOrNull()}", subReqRes.isSuccess)
        val paymentId = subReqRes.getOrThrow()

        val pendingPayments = withTimeout(FLOW_TIMEOUT_MS) {
            customerRepo.observeUserPayments(customerUid).first { list -> list.any { it.id == paymentId } }
        }
        val paymentRecord = pendingPayments.first { it.id == paymentId }
        assertEquals("pending", paymentRecord.status)
        assertEquals("UTR-430299887766", paymentRecord.transactionRef)

        // Profile must still be on 'free' before CEO/Founder verification
        val profileBeforeApproval = withTimeout(DEFAULT_TIMEOUT_MS) {
            customerRepo.getProfile(customerUid).getOrThrow()
        }
        assertEquals("free", profileBeforeApproval?.subscriptionPlan)

        // Sign in as CEO and approve the Direct UPI subscription payment after bank statement verification
        signInTestUser(ceoEmail)
        val ceoRepo = SkillConnectRepository(firestore)
        val ceoAuth = withTimeout(DEFAULT_TIMEOUT_MS) {
            ceoRepo.authenticateOrProvisionExecutiveAdmin("ceo", "Abhay", "CeoPass#2026")
        }
        assertTrue("CEO auth should succeed: ${ceoAuth.exceptionOrNull()}", ceoAuth.isSuccess)

        val approveRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            ceoRepo.adminApproveOrRejectSubscriptionPayment(
                payment = paymentRecord,
                approve = true,
                verificationNote = "Confirmed ₹249 credit for UTR-430299887766 in merchant bank statement"
            )
        }
        assertTrue("CEO subscription approval should succeed: ${approveRes.exceptionOrNull()}", approveRes.isSuccess)

        // Verify profile is now upgraded to 'premium'
        signInTestUser(customerEmail)
        val profileAfterApproval = withTimeout(DEFAULT_TIMEOUT_MS) {
            customerRepo.getProfile(customerUid).getOrThrow()
        }
        assertEquals("premium", profileAfterApproval?.subscriptionPlan)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}

