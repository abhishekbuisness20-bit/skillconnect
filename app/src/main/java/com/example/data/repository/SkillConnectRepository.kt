package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdminAccount
import com.example.data.model.AdminAuditLog
import com.example.data.model.AppNotification
import com.example.data.model.BookingRequest
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.FavouriteItem
import com.example.data.model.PaymentDispute
import com.example.data.model.PaymentRecord
import com.example.data.model.PlatformSetting
import com.example.data.model.SafetyReport
import com.example.data.model.ServiceListing
import com.example.data.model.ServiceReview
import com.example.data.model.SupportTicket
import com.example.data.model.UserProfile
import com.example.data.model.VerificationSubmission
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Date
import java.util.UUID

class SkillConnectRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun generatePlatformId(uid: String, role: String): String {
        val prefix = if (role == "provider") "SC-PRO" else "SC-CUS"
        val cleanUid = uid.uppercase().filter { it.isLetterOrDigit() }
        val suffix = (cleanUid + UUID.randomUUID().toString().uppercase().filter { it.isLetterOrDigit() }).take(8)
        return "$prefix-$suffix"
    }

    // --- 1. User & Provider Profiles ---

    fun observeMyProfile(userId: String): Flow<UserProfile?> =
        db.collection("user_profiles").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.GET, "user_profiles/$userId")
                }
                throw e
            }

    fun observePublicProviders(): Flow<List<UserProfile>> =
        db.collection("user_profiles")
            .whereEqualTo("isPublicProvider", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "user_profiles")
                }
                throw e
            }

    fun observeAllProfilesForAdmin(): Flow<List<UserProfile>> =
        db.collection("user_profiles")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "user_profiles")
                }
                throw e
            }

    suspend fun getProfile(userId: String): Result<UserProfile?> = runCatching {
        val docRef = db.collection("user_profiles").document(userId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createInitialProfile(
        displayName: String,
        role: String,
        city: String,
        locality: String,
        preferredLanguage: String,
        headline: String = "",
        bio: String = "",
        skills: List<String> = emptyList(),
        startingPrice: Long = 0L,
        onboardingCompleted: Boolean? = null
    ): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val normalizedRole = if (role == "provider") "provider" else "customer"
        val generatedPlatformId = generatePlatformId(uid, normalizedRole)
        val isCompleted = onboardingCompleted ?: (normalizedRole == "customer")
        val initialVerificationStatus = if (normalizedRole == "provider" && !isCompleted) "draft" else "not_submitted"
        val docRef = db.collection("user_profiles").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "platformId" to generatedPlatformId,
            "displayName" to displayName.trim().ifBlank { "SkillConnect Member" }.take(80),
            "headline" to headline.trim().take(120),
            "bio" to bio.trim().take(600),
            "role" to normalizedRole,
            "city" to city.trim().ifBlank { "Bengaluru" }.take(60),
            "locality" to locality.trim().take(80),
            "preferredLanguage" to if (preferredLanguage == "hi") "hi" else "en",
            "skills" to skills.take(10).map { it.take(50) },
            "startingPrice" to startingPrice.coerceIn(0L, 10000000L),
            "availability" to "Mon–Sat, 9:00 AM – 7:00 PM",
            "isVerified" to false,
            "verificationStatus" to initialVerificationStatus,
            "subscriptionPlan" to "free",
            "isPublicProvider" to (normalizedRole == "provider"),
            "accountStatus" to "active",
            "publicPhone" to "",
            "completedJobsCount" to 0L,
            "ratingSum" to 0L,
            "reviewCount" to 0L,
            "onboardingStep" to 1L,
            "onboardingCompleted" to isCompleted,
            "experienceLevel" to "Intermediate",
            "experienceYears" to 2L,
            "languagesSpoken" to listOf("English", "Hindi"),
            "qualifications" to "",
            "businessType" to "Individual Freelancer",
            "businessName" to "",
            "primaryCategory" to "Graphic Design",
            "subcategories" to "",
            "pricingModel" to "fixed",
            "remoteAvailable" to true,
            "inPersonAvailable" to true,
            "serviceState" to "Karnataka",
            "pinCode" to "",
            "serviceAreaRadius" to "Within 15 km",
            "avatarUri" to "",
            "coverUri" to "",
            "portfolioSummaryJson" to "",
            "publicListingConsent" to true,
            "verificationRejectionReason" to "",
            "emailVerified" to true,
            "phoneVerified" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            UserProfile(
                id = uid,
                userId = uid,
                platformId = generatedPlatformId,
                displayName = displayName.trim().ifBlank { "SkillConnect Member" }.take(80),
                headline = headline.trim().take(120),
                bio = bio.trim().take(600),
                role = normalizedRole,
                city = city.trim().ifBlank { "Bengaluru" }.take(60),
                locality = locality.trim().take(80),
                preferredLanguage = if (preferredLanguage == "hi") "hi" else "en",
                skills = skills.take(10).map { it.take(50) },
                startingPrice = startingPrice.coerceIn(0L, 10000000L),
                verificationStatus = initialVerificationStatus,
                isPublicProvider = (normalizedRole == "provider"),
                onboardingStep = 1L,
                onboardingCompleted = isCompleted
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateProfile(profile: UserProfile): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("user_profiles").document(uid)
        val ensuredPlatformId = profile.platformId.ifBlank { generatePlatformId(uid, profile.role) }
        val updates = mapOf(
            "platformId" to ensuredPlatformId.take(40),
            "displayName" to profile.displayName.trim().ifBlank { "SkillConnect User" }.take(80),
            "headline" to profile.headline.trim().take(120),
            "bio" to profile.bio.trim().take(600),
            "city" to profile.city.trim().ifBlank { "Bengaluru" }.take(60),
            "locality" to profile.locality.trim().take(80),
            "preferredLanguage" to if (profile.preferredLanguage == "hi") "hi" else "en",
            "skills" to profile.skills.take(10).map { it.take(50) },
            "startingPrice" to profile.startingPrice.coerceIn(0L, 10000000L),
            "availability" to profile.availability.trim().take(100),
            "verificationStatus" to profile.verificationStatus,
            "isPublicProvider" to (profile.role == "provider" && profile.publicListingConsent && profile.verificationStatus != "draft"),
            "publicPhone" to profile.publicPhone.trim().take(20),
            "onboardingStep" to profile.onboardingStep.coerceIn(1L, 8L),
            "onboardingCompleted" to profile.onboardingCompleted,
            "experienceLevel" to profile.experienceLevel.trim().take(40),
            "experienceYears" to profile.experienceYears.coerceIn(0L, 80L),
            "languagesSpoken" to profile.languagesSpoken.take(10).map { it.take(30) },
            "qualifications" to profile.qualifications.trim().take(300),
            "businessType" to profile.businessType.trim().take(60),
            "businessName" to profile.businessName.trim().take(120),
            "primaryCategory" to profile.primaryCategory.trim().take(60),
            "subcategories" to profile.subcategories.trim().take(200),
            "pricingModel" to profile.pricingModel.trim().take(40),
            "remoteAvailable" to profile.remoteAvailable,
            "inPersonAvailable" to profile.inPersonAvailable,
            "serviceState" to profile.serviceState.trim().take(60),
            "pinCode" to profile.pinCode.trim().take(12),
            "serviceAreaRadius" to profile.serviceAreaRadius.trim().take(80),
            "avatarUri" to profile.avatarUri.trim().take(500),
            "coverUri" to profile.coverUri.trim().take(500),
            "portfolioSummaryJson" to profile.portfolioSummaryJson.trim().take(4000),
            "publicListingConsent" to profile.publicListingConsent,
            "emailVerified" to profile.emailVerified,
            "phoneVerified" to profile.phoneVerified,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun adminUpdateUserVerificationAndStatus(
        targetUserId: String,
        isVerified: Boolean,
        verificationStatus: String,
        accountStatus: String,
        rejectionReason: String = "",
        newRole: String? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("user_profiles").document(targetUserId)
        val updates = mutableMapOf<String, Any>(
            "isVerified" to isVerified,
            "verificationStatus" to verificationStatus,
            "accountStatus" to accountStatus,
            "verificationRejectionReason" to rejectionReason.trim().take(500),
            "isPublicProvider" to (accountStatus == "active" && verificationStatus !in listOf("draft", "suspended")),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (newRole != null && newRole in listOf("customer", "provider")) {
            updates["role"] = newRole
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- 2. Service Listings ---

    fun observePublishedServices(): Flow<List<ServiceListing>> =
        db.collection("services")
            .whereEqualTo("isPublished", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ServiceListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "services")
                }
                throw e
            }

    fun observeProviderServices(providerId: String): Flow<List<ServiceListing>> =
        db.collection("services")
            .whereEqualTo("providerId", providerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ServiceListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "services")
                }
                throw e
            }

    suspend fun createService(
        providerName: String,
        providerVerified: Boolean,
        title: String,
        category: String,
        subcategory: String,
        shortSummary: String,
        description: String,
        skills: List<String>,
        startingPrice: Long,
        pricingModel: String,
        deliverables: String,
        turnaround: String,
        city: String,
        locality: String,
        availability: String,
        terms: String,
        isPublished: Boolean
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val serviceId = "srv_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("services").document(serviceId)
        val payload = mapOf(
            "providerId" to uid,
            "providerName" to providerName.trim().ifBlank { "Verified Provider" }.take(80),
            "providerVerified" to providerVerified,
            "title" to title.trim().take(120),
            "category" to category.trim().ifBlank { "Other Local Services" }.take(60),
            "subcategory" to subcategory.trim().take(60),
            "shortSummary" to shortSummary.trim().take(200),
            "description" to description.trim().take(2000),
            "skills" to skills.take(10).map { it.take(50) },
            "startingPrice" to startingPrice.coerceIn(0L, 10000000L),
            "pricingModel" to if (pricingModel in listOf("fixed", "hourly", "quote")) pricingModel else "fixed",
            "deliverables" to deliverables.trim().take(1000),
            "turnaround" to turnaround.trim().take(100),
            "city" to city.trim().ifBlank { "Bengaluru" }.take(60),
            "locality" to locality.trim().take(80),
            "availability" to availability.trim().take(100),
            "terms" to terms.trim().take(500),
            "isPublished" to isPublished,
            "isDraft" to !isPublished,
            "isFeatured" to false,
            "moderationStatus" to "approved",
            "moderationReason" to "",
            "ratingSum" to 0L,
            "reviewCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            serviceId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateService(service: ServiceListing): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("services").document(service.id)
        val updates = mapOf(
            "providerName" to service.providerName.trim().take(80),
            "providerVerified" to service.providerVerified,
            "title" to service.title.trim().take(120),
            "category" to service.category.trim().take(60),
            "subcategory" to service.subcategory.trim().take(60),
            "shortSummary" to service.shortSummary.trim().take(200),
            "description" to service.description.trim().take(2000),
            "skills" to service.skills.take(10).map { it.take(50) },
            "startingPrice" to service.startingPrice.coerceIn(0L, 10000000L),
            "pricingModel" to service.pricingModel,
            "deliverables" to service.deliverables.trim().take(1000),
            "turnaround" to service.turnaround.trim().take(100),
            "city" to service.city.trim().take(60),
            "locality" to service.locality.trim().take(80),
            "availability" to service.availability.trim().take(100),
            "terms" to service.terms.trim().take(500),
            "isPublished" to service.isPublished,
            "isDraft" to service.isDraft,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteService(serviceId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("services").document(serviceId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun adminModerateService(
        serviceId: String,
        isPublished: Boolean,
        isFeatured: Boolean,
        moderationStatus: String,
        moderationReason: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("services").document(serviceId)
        try {
            docRef.update(
                mapOf(
                    "isPublished" to isPublished,
                    "isFeatured" to isFeatured,
                    "moderationStatus" to moderationStatus,
                    "moderationReason" to moderationReason.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- 3. Booking & Enquiry System (12 States + Payment Tracking) ---

    fun observeCustomerBookings(customerId: String): Flow<List<BookingRequest>> =
        db.collection("booking_requests")
            .whereEqualTo("customerId", customerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BookingRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "booking_requests")
                }
                throw e
            }

    fun observeProviderBookings(providerId: String): Flow<List<BookingRequest>> =
        db.collection("booking_requests")
            .whereEqualTo("providerId", providerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BookingRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "booking_requests")
                }
                throw e
            }

    fun observeAllBookingsForAdmin(): Flow<List<BookingRequest>> =
        db.collection("booking_requests")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BookingRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "booking_requests")
                }
                throw e
            }

    suspend fun createBookingRequest(
        customerName: String,
        providerId: String,
        providerName: String,
        serviceId: String,
        serviceTitle: String,
        category: String,
        requirements: String,
        preferredDate: String,
        attachmentNote: String,
        quotedPrice: Long,
        requestType: String,
        preferredTimeSlot: String = "10:00 AM – 1:00 PM",
        paymentMethod: String = "upi",
        isDraft: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val rawUuid = UUID.randomUUID().toString().replace("-", "")
        val bookingId = "bk_${rawUuid.take(16)}"
        val bookingRef = "SC-BK-${rawUuid.uppercase().take(8)}"
        val platformFee = (quotedPrice * 5L) / 100L
        val totalInr = quotedPrice + platformFee
        val docRef = db.collection("booking_requests").document(bookingId)
        val payload = mapOf(
            "customerId" to uid,
            "customerName" to customerName.trim().ifBlank { "Customer" }.take(80),
            "providerId" to providerId,
            "providerName" to providerName.trim().ifBlank { "Provider" }.take(80),
            "serviceId" to serviceId,
            "serviceTitle" to serviceTitle.trim().take(120),
            "category" to category.trim().ifBlank { "General" }.take(60),
            "requirements" to requirements.trim().take(1500),
            "preferredDate" to preferredDate.trim().ifBlank { "Flexible" }.take(60),
            "preferredTimeSlot" to preferredTimeSlot.trim().take(60),
            "attachmentNote" to attachmentNote.trim().take(300),
            "quotedPrice" to quotedPrice.coerceIn(0L, 10000000L),
            "requestType" to if (requestType == "quote") "quote" else "booking",
            "status" to if (isDraft) "draft" else "pending",
            "statusNote" to "",
            "hasReviewed" to false,
            "bookingRef" to bookingRef,
            "paymentMethod" to paymentMethod.take(40),
            "paymentStatus" to "unpaid",
            "paymentReferenceId" to "",
            "platformFeeInr" to platformFee.coerceAtLeast(0L),
            "totalAmountInr" to totalInr.coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            bookingId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateBookingStatusByCustomer(
        bookingId: String,
        newStatus: String,
        statusNote: String = "",
        paymentMethod: String? = null,
        paymentStatus: String? = null,
        paymentReferenceId: String? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("booking_requests").document(bookingId)
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus,
            "statusNote" to statusNote.trim().take(500),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (paymentMethod != null) updates["paymentMethod"] = paymentMethod.take(40)
        if (paymentStatus != null) updates["paymentStatus"] = paymentStatus.take(40)
        if (paymentReferenceId != null) updates["paymentReferenceId"] = paymentReferenceId.take(128)
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateBookingStatusByProvider(
        bookingId: String,
        newStatus: String,
        statusNote: String = "",
        quotedPrice: Long? = null,
        paymentStatus: String? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("booking_requests").document(bookingId)
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus,
            "statusNote" to statusNote.trim().take(500),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (quotedPrice != null) {
            val coerced = quotedPrice.coerceIn(0L, 10000000L)
            updates["quotedPrice"] = coerced
            updates["totalAmountInr"] = coerced + (coerced * 5L) / 100L
        }
        if (paymentStatus != null) {
            updates["paymentStatus"] = paymentStatus.take(40)
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun adminUpdateBookingAndRefund(
        bookingId: String,
        newStatus: String,
        statusNote: String,
        paymentStatus: String? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("booking_requests").document(bookingId)
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus,
            "statusNote" to statusNote.trim().take(500),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (paymentStatus != null) {
            updates["paymentStatus"] = paymentStatus.take(40)
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- 4. Payments, Refunds, Payouts & Subscriptions Ledger ---

    fun observeUserPayments(userId: String): Flow<List<PaymentRecord>> =
        db.collection("payments")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PaymentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "payments")
                }
                throw e
            }

    fun observeProviderPayments(providerId: String): Flow<List<PaymentRecord>> =
        db.collection("payments")
            .whereEqualTo("providerId", providerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PaymentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "payments")
                }
                throw e
            }

    fun observeAllPaymentsForAdmin(): Flow<List<PaymentRecord>> =
        db.collection("payments")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PaymentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "payments")
                }
                throw e
            }

    suspend fun createPaymentRecord(
        providerId: String,
        bookingId: String,
        paymentType: String,
        method: String,
        amountInr: Long,
        status: String,
        transactionRef: String,
        notes: String,
        idempotencyKey: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val safeType = if (paymentType in listOf("booking_payment", "subscription", "payout", "refund")) paymentType else "booking_payment"
        val cleanBookingId = bookingId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(48)
        val paymentId = when {
            idempotencyKey.isNotBlank() -> idempotencyKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(64)
            cleanBookingId.isNotBlank() && safeType == "booking_payment" -> "pay_${cleanBookingId}_booking"
            else -> "pay_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
        val docRef = db.collection("payments").document(paymentId)
        val validStatuses = listOf("pending", "processing", "succeeded", "failed", "refund_pending", "refunded", "disputed", "paid_out")
        val payload = mapOf(
            "userId" to uid,
            "providerId" to providerId.take(128),
            "bookingId" to bookingId.take(128),
            "paymentType" to safeType,
            "method" to if (method in listOf("upi", "card", "netbanking", "bank_transfer", "cash_offline")) method else "upi",
            "amountInr" to amountInr.coerceIn(0L, 10000000L),
            "status" to if (status in validStatuses) status else "succeeded",
            "transactionRef" to transactionRef.trim().take(120),
            "notes" to notes.trim().take(500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "status" to if (status in validStatuses) status else "succeeded",
                        "notes" to notes.trim().take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(payload).await()
            }
            paymentId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updatePaymentStatus(
        paymentId: String,
        newStatus: String,
        notes: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("payments").document(paymentId)
        try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "notes" to notes.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    /**
     * Direct UPI Subscription Request:
     * Writes a PENDING subscription payment request with the user's UTR/reference.
     * Never activates 'premium' from the Android client.
     */
    suspend fun submitDirectUpiSubscriptionRequest(
        role: String,
        amountInr: Long,
        utrReference: String,
        payerUpiNote: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanUtr = utrReference.trim()
        require(cleanUtr.length >= 6) {
            "Please enter a valid UPI UTR / transaction reference number (at least 6 characters)."
        }
        val normalizedRole = if (role == "provider") "provider" else "customer"
        val txRef = if (cleanUtr.startsWith("UTR-", ignoreCase = true)) {
            cleanUtr.take(120)
        } else {
            "UTR-$cleanUtr".take(120)
        }
        val extraNote = if (payerUpiNote.isNotBlank()) " • Payer Info: ${payerUpiNote.trim()}" else ""
        val notes = "Direct UPI Subscription (${normalizedRole.uppercase()} Premium Plan) • Paid to Merchant UPI: 6393481228@ybl • Ref: $txRef$extraNote".take(500)
        createPaymentRecord(
            providerId = if (normalizedRole == "provider") uid else "",
            bookingId = "",
            paymentType = "subscription",
            method = "upi",
            amountInr = amountInr.coerceIn(1L, 1000000L),
            status = "pending",
            transactionRef = txRef,
            notes = notes
        ).getOrThrow()
    }

    /**
     * Active CEO / Founder Subscription Payment Verification & Approval:
     * Activates subscriptionPlan = 'premium' on user_profiles/{userId} ONLY after an active CEO/Founder
     * confirms the UTR credit in the merchant bank/UPI statement.
     */
    suspend fun adminApproveOrRejectSubscriptionPayment(
        payment: PaymentRecord,
        approve: Boolean,
        verificationNote: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val cleanNote = verificationNote.trim()
        require(cleanNote.length >= 8) {
            "Verification note must be at least 8 characters confirming the merchant bank/UPI statement check."
        }
        require(payment.paymentType == "subscription") {
            "Selected payment record is not a subscription request."
        }
        val nextPayStatus = if (approve) "succeeded" else "failed"
        val paymentRef = db.collection("payments").document(payment.id)
        val profileRef = db.collection("user_profiles").document(payment.userId)
        try {
            paymentRef.update(
                mapOf(
                    "status" to nextPayStatus,
                    "notes" to "${payment.notes} | Reviewer: $cleanNote".take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (payment.userId.isNotBlank()) {
                profileRef.update(
                    mapOf(
                        "subscriptionPlan" to if (approve) "premium" else "free",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, paymentRef.path)
            throw e
        }
    }

    // --- 5. Messaging & Conversations ---

    fun observeCustomerConversations(customerId: String): Flow<List<Conversation>> =
        db.collection("conversations")
            .whereEqualTo("customerId", customerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "conversations")
                }
                throw e
            }

    fun observeProviderConversations(providerId: String): Flow<List<Conversation>> =
        db.collection("conversations")
            .whereEqualTo("providerId", providerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "conversations")
                }
                throw e
            }

    fun observeMessages(conversationId: String, currentUserId: String, isCustomer: Boolean): Flow<List<ChatMessage>> {
        val roleField = if (isCustomer) "customerId" else "providerId"
        return db.collection("messages")
            .whereEqualTo("conversationId", conversationId)
            .whereEqualTo(roleField, currentUserId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "messages")
                }
                throw e
            }
    }

    suspend fun getOrCreateConversation(
        customerId: String,
        customerName: String,
        providerId: String,
        providerName: String,
        bookingId: String = "",
        serviceTitle: String = ""
    ): Result<String> = runCatching {
        requireUserId()
        val convId = "conv_${customerId.take(20)}_${providerId.take(20)}"
        val docRef = db.collection("conversations").document(convId)
        try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "customerId" to customerId,
                    "customerName" to customerName.trim().ifBlank { "Customer" }.take(80),
                    "providerId" to providerId,
                    "providerName" to providerName.trim().ifBlank { "Provider" }.take(80),
                    "bookingId" to bookingId.take(128),
                    "serviceTitle" to serviceTitle.trim().take(120),
                    "lastMessage" to "Conversation started",
                    "unreadCustomerCount" to 0L,
                    "unreadProviderCount" to 0L,
                    "isBlocked" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            convId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleConversationBlock(conversationId: String, blocked: Boolean): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("conversations").document(conversationId)
        try {
            docRef.update(
                mapOf(
                    "isBlocked" to blocked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun sendMessage(
        conversationId: String,
        customerId: String,
        providerId: String,
        senderName: String,
        text: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val msgRef = db.collection("messages").document(msgId)
        val trimmedText = text.trim().take(1000)
        val payload = mapOf(
            "conversationId" to conversationId,
            "customerId" to customerId,
            "providerId" to providerId,
            "senderId" to uid,
            "senderName" to senderName.trim().ifBlank { "Member" }.take(80),
            "text" to trimmedText,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            msgRef.set(payload).await()
            db.collection("conversations").document(conversationId).update(
                mapOf(
                    "lastMessage" to trimmedText.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            msgId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            throw e
        }
    }

    // --- 6. Notifications ---

    fun observeNotifications(userId: String): Flow<List<AppNotification>> =
        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "notifications")
                }
                throw e
            }

    suspend fun sendNotification(
        recipientUserId: String,
        title: String,
        body: String,
        type: String,
        relatedId: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val notifId = "ntf_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("notifications").document(notifId)
        val payload = mapOf(
            "userId" to recipientUserId,
            "senderId" to uid,
            "title" to title.trim().take(120),
            "body" to body.trim().take(500),
            "type" to type,
            "relatedId" to relatedId.take(128),
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("notifications").document(notificationId)
        try {
            docRef.update(
                mapOf(
                    "isRead" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun markAllNotificationsRead(notificationIds: List<String>): Result<Unit> = runCatching {
        requireUserId()
        notificationIds.forEach { id ->
            markNotificationRead(id)
        }
    }

    // --- 7. Reviews & Ratings ---

    fun observePublishedReviews(): Flow<List<ServiceReview>> =
        db.collection("reviews")
            .whereEqualTo("moderationStatus", "published")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ServiceReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "reviews")
                }
                throw e
            }

    suspend fun submitReview(
        booking: BookingRequest,
        rating: Int,
        comment: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val reviewRef = db.collection("reviews").document(booking.id)
        val ratingLong = rating.coerceIn(1, 5).toLong()
        val payload = mapOf(
            "bookingId" to booking.id,
            "serviceId" to booking.serviceId,
            "providerId" to booking.providerId,
            "customerId" to uid,
            "customerName" to booking.customerName.trim().ifBlank { "Customer" }.take(80),
            "rating" to ratingLong,
            "comment" to comment.trim().take(1000),
            "isReported" to false,
            "moderationStatus" to "published",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            reviewRef.set(payload).await()
            db.collection("booking_requests").document(booking.id).update(
                mapOf(
                    "hasReviewed" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (booking.serviceId.isNotBlank()) {
                runCatching {
                    db.collection("services").document(booking.serviceId).update(
                        mapOf(
                            "ratingSum" to FieldValue.increment(ratingLong),
                            "reviewCount" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            runCatching {
                db.collection("user_profiles").document(booking.providerId).update(
                    mapOf(
                        "ratingSum" to FieldValue.increment(ratingLong),
                        "reviewCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reviewRef.path)
            throw e
        }
    }

    // --- 8. Favourites ---

    fun observeFavourites(userId: String): Flow<List<FavouriteItem>> =
        db.collection("users").document(userId).collection("favourites")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FavouriteItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "users/$userId/favourites")
                }
                throw e
            }

    suspend fun toggleFavourite(
        targetId: String,
        targetType: String,
        title: String,
        subtitle: String,
        location: String,
        price: Long,
        currentlySaved: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val favId = "fav_${targetType}_${targetId.take(40)}"
        val docRef = db.collection("users").document(uid).collection("favourites").document(favId)
        try {
            if (currentlySaved) {
                docRef.delete().await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "targetId" to targetId.take(128),
                    "targetType" to if (targetType == "provider") "provider" else "service",
                    "title" to title.trim().ifBlank { "Saved Item" }.take(120),
                    "subtitle" to subtitle.trim().take(120),
                    "location" to location.trim().take(100),
                    "price" to price.coerceIn(0L, 10000000L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- 9. Provider Verification & Admin Queue ---

    fun observeMyVerification(userId: String): Flow<VerificationSubmission?> =
        db.collection("verification_submissions").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(VerificationSubmission::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.GET, "verification_submissions/$userId")
                }
                throw e
            }

    fun observeAllVerificationsForAdmin(): Flow<List<VerificationSubmission>> =
        db.collection("verification_submissions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(VerificationSubmission::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "verification_submissions")
                }
                throw e
            }

    suspend fun submitProviderVerification(
        legalName: String,
        businessName: String,
        category: String,
        city: String,
        experienceYears: Long,
        portfolioSummary: String,
        documentReferenceNote: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("verification_submissions").document(uid)
        val safeLegal = legalName.trim().ifBlank { "Provider Applicant" }.take(100)
        val safeSummary = portfolioSummary.trim().ifBlank { "Professional portfolio submitted via SkillConnect Onboarding." }.take(1000)
        val payload = mapOf(
            "userId" to uid,
            "legalName" to safeLegal,
            "businessName" to businessName.trim().take(120),
            "category" to category.trim().ifBlank { "General" }.take(60),
            "city" to city.trim().ifBlank { "Bengaluru" }.take(60),
            "experienceYears" to experienceYears.coerceIn(0L, 80L),
            "portfolioSummary" to safeSummary,
            "documentReferenceNote" to documentReferenceNote.trim().take(300),
            "status" to "pending_review",
            "adminNote" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "legalName" to safeLegal,
                        "businessName" to businessName.trim().take(120),
                        "category" to category.trim().ifBlank { "General" }.take(60),
                        "city" to city.trim().ifBlank { "Bengaluru" }.take(60),
                        "experienceYears" to experienceYears.coerceIn(0L, 80L),
                        "portfolioSummary" to safeSummary,
                        "documentReferenceNote" to documentReferenceNote.trim().take(300),
                        "status" to "pending_review",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(payload).await()
            }
            db.collection("user_profiles").document(uid).update(
                mapOf(
                    "verificationStatus" to "pending_review",
                    "onboardingCompleted" to true,
                    "onboardingStep" to 8L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun adminReviewProviderVerification(
        targetUserId: String,
        newStatus: String, // under_review, approved, rejected, additional_info_required, suspended
        adminNote: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val subRef = db.collection("verification_submissions").document(targetUserId)
        try {
            val snap = subRef.get().await()
            if (snap.exists()) {
                subRef.update(
                    mapOf(
                        "status" to newStatus,
                        "adminNote" to adminNote.trim().take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val isApproved = newStatus == "approved"
            val acctStatus = if (newStatus == "suspended") "suspended" else "active"
            adminUpdateUserVerificationAndStatus(
                targetUserId = targetUserId,
                isVerified = isApproved,
                verificationStatus = newStatus,
                accountStatus = acctStatus,
                rejectionReason = if (isApproved) "" else adminNote
            ).getOrThrow()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, subRef.path)
            throw e
        }
    }

    // --- 10. Safety Reports & Support Tickets ---

    fun observeMyReports(userId: String): Flow<List<SafetyReport>> =
        db.collection("reports")
            .whereEqualTo("reporterId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SafetyReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "reports")
                }
                throw e
            }

    fun observeAllReportsForAdmin(): Flow<List<SafetyReport>> =
        db.collection("reports")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SafetyReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "reports")
                }
                throw e
            }

    suspend fun submitReport(
        targetId: String,
        targetType: String,
        reason: String,
        details: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("reports").document(reportId)
        val payload = mapOf(
            "reporterId" to uid,
            "targetId" to targetId.take(128),
            "targetType" to if (targetType in listOf("service", "provider", "review", "user")) targetType else "service",
            "reason" to reason.trim().take(120),
            "details" to details.trim().take(1000),
            "status" to "open",
            "adminResolution" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun adminUpdateReportStatus(
        reportId: String,
        newStatus: String,
        resolution: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("reports").document(reportId)
        try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "adminResolution" to resolution.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeMySupportTickets(userId: String): Flow<List<SupportTicket>> =
        db.collection("support_tickets")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "support_tickets")
                }
                throw e
            }

    fun observeAllTicketsForAdmin(): Flow<List<SupportTicket>> =
        db.collection("support_tickets")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "support_tickets")
                }
                throw e
            }

    suspend fun submitSupportTicket(
        name: String,
        contactEmail: String,
        category: String,
        relatedBookingId: String,
        description: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val ticketId = "tkt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("support_tickets").document(ticketId)
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().ifBlank { "Member" }.take(80),
            "contactEmail" to contactEmail.trim().ifBlank { "support@skillconnect.in" }.take(120),
            "category" to category.trim().ifBlank { "General" }.take(60),
            "relatedBookingId" to relatedBookingId.trim().take(128),
            "description" to description.trim().take(1500),
            "status" to "open",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun adminUpdateSupportTicketStatus(
        ticketId: String,
        newStatus: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("support_tickets").document(ticketId)
        try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- 11. Global Platform Settings & Admin Audit Logs ---

    fun observeGlobalSettings(): Flow<PlatformSetting?> =
        db.collection("platform_settings")
            .whereEqualTo("settingKey", "global")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PlatformSetting::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .firstOrNull()
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "platform_settings")
                }
                throw e
            }

    suspend fun saveGlobalSettings(
        announcementText: String,
        announcementActive: Boolean,
        freeListingLimit: Long,
        premiumPriceInr: Long,
        billingPeriod: String,
        paymentsDemoMode: Boolean,
        maintenanceMode: Boolean? = null,
        sessionTimeoutMinutes: Long? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("platform_settings").document("global")
        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "announcementText" to announcementText.trim().take(300),
                    "announcementActive" to announcementActive,
                    "freeListingLimit" to freeListingLimit.coerceIn(1L, 100L),
                    "premiumPriceInr" to premiumPriceInr.coerceIn(0L, 1000000L),
                    "billingPeriod" to billingPeriod.trim().ifBlank { "Monthly" }.take(40),
                    "paymentsDemoMode" to paymentsDemoMode,
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (maintenanceMode != null) {
                    updates["maintenanceMode"] = maintenanceMode
                }
                if (sessionTimeoutMinutes != null) {
                    updates["sessionTimeoutMinutes"] = sessionTimeoutMinutes.coerceIn(5L, 120L)
                }
                docRef.update(updates).await()
            } else {
                val createMap = mutableMapOf<String, Any>(
                    "settingKey" to "global",
                    "announcementText" to announcementText.trim().take(300),
                    "announcementActive" to announcementActive,
                    "freeListingLimit" to freeListingLimit.coerceIn(1L, 100L),
                    "premiumPriceInr" to premiumPriceInr.coerceIn(0L, 1000000L),
                    "billingPeriod" to billingPeriod.trim().ifBlank { "Monthly" }.take(40),
                    "paymentsDemoMode" to paymentsDemoMode,
                    "maintenanceMode" to (maintenanceMode ?: false),
                    "sessionTimeoutMinutes" to (sessionTimeoutMinutes ?: 15L).coerceIn(5L, 120L),
                    "updatedBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createMap).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeAdminAuditLogs(): Flow<List<AdminAuditLog>> =
        db.collection("audit_logs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AdminAuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "audit_logs")
                }
                throw e
            }

    suspend fun recordAdminAuditLog(
        adminEmail: String,
        actionType: String,
        targetCollection: String,
        targetId: String,
        reason: String,
        previousState: String,
        newState: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val logId = "aud_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("audit_logs").document(logId)
        val payload = mapOf(
            "adminId" to uid,
            "adminEmail" to adminEmail.trim().ifBlank { "admin@skillconnect.in" }.take(120),
            "actionType" to actionType.trim().take(80),
            "targetCollection" to targetCollection.trim().take(60),
            "targetId" to targetId.trim().take(128),
            "reason" to reason.trim().take(500),
            "previousState" to previousState.trim().take(120),
            "newState" to newState.trim().take(120),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- 12. Tiered Executive Admin Accounts (Co-Founder, CEO, Founder) & 15-Minute Session Security ---

    fun hashAdminPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:${password.trim()}".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun observeMyAdminAccount(adminUid: String): Flow<AdminAccount?> =
        db.collection("admins").document(adminUid)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(AdminAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.GET, "admins/$adminUid")
                }
                throw e
            }

    fun observeAllAdminsForFounder(): Flow<List<AdminAccount>> =
        db.collection("admins")
            .snapshots()
            .map { snap ->
                snap.toObjects(AdminAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "admins")
                }
                throw e
            }

    suspend fun authenticateOrProvisionExecutiveAdmin(
        selectedRole: String,
        enteredUsername: String,
        enteredPassword: String,
        sessionTimeoutMinutes: Long = 15L
    ): Result<AdminAccount> = runCatching {
        val uid = requireUserId()
        val normalizedRole = selectedRole.trim().lowercase()
        val cleanUsername = enteredUsername.trim()
        val cleanPassword = enteredPassword.trim()
        require(cleanPassword.length >= 6) {
            "Administrative password must be at least 6 characters."
        }

        val (canonicalId, displayName, canonicalUsername, isFounderPersistent) = when (normalizedRole) {
            "cofounder" -> {
                require(cleanUsername.equals("Abhay", ignoreCase = true)) {
                    "Co-Founder Login strictly requires username 'Abhay'."
                }
                Quadruple("ADM-COF-001", "Abhay", "Abhay", false)
            }
            "ceo" -> {
                require(cleanUsername.equals("Abhay", ignoreCase = true) || cleanUsername.equals("Adarsh", ignoreCase = true)) {
                    "CEO Login requires accepted username alias 'Abhay' or 'Adarsh'."
                }
                val resolvedAlias = if (cleanUsername.equals("Adarsh", ignoreCase = true)) "Adarsh" else "Abhay"
                Quadruple("ADM-CEO-001", "CEO", resolvedAlias, false)
            }
            "founder" -> {
                require(cleanUsername.equals("Abhisheksingh", ignoreCase = true)) {
                    "Founder Login strictly requires username 'Abhisheksingh'."
                }
                Quadruple("ADM-FND-001", "Founder", "Abhisheksingh", true)
            }
            else -> throw IllegalArgumentException("Invalid administrative role selected.")
        }

        val nowMs = System.currentTimeMillis()
        val expiresAtMs = if (isFounderPersistent) {
            nowMs + 365L * 24L * 60L * 60L * 1000L
        } else {
            nowMs + sessionTimeoutMinutes.coerceIn(5L, 120L) * 60L * 1000L
        }
        val sessionExpiresTimestamp = Timestamp(Date(expiresAtMs))

        val docRef = db.collection("admins").document(uid)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val salt = "sc_salt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                val verifierHash = hashAdminPassword(cleanPassword, salt)
                val payload = mapOf(
                    "adminUid" to uid,
                    "canonicalAccountId" to canonicalId,
                    "adminRole" to normalizedRole,
                    "displayName" to displayName,
                    "username" to canonicalUsername,
                    "passwordVerifierHash" to verifierHash,
                    "passwordSalt" to salt,
                    "mustChangePassword" to false,
                    "failedAttempts" to 0L,
                    "lockedUntilEpochMs" to 0L,
                    "founderSessionPersistent" to isFounderPersistent,
                    "sessionExpiresAt" to sessionExpiresTimestamp,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                AdminAccount(
                    id = uid,
                    adminUid = uid,
                    canonicalAccountId = canonicalId,
                    adminRole = normalizedRole,
                    displayName = displayName,
                    username = canonicalUsername,
                    passwordVerifierHash = verifierHash,
                    passwordSalt = salt,
                    mustChangePassword = false,
                    failedAttempts = 0L,
                    lockedUntilEpochMs = 0L,
                    founderSessionPersistent = isFounderPersistent,
                    sessionExpiresAt = sessionExpiresTimestamp
                )
            } else {
                val existing = snap.toObject(AdminAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Unable to parse existing administrative account.")

                if (existing.lockedUntilEpochMs > nowMs) {
                    val remainingSec = ((existing.lockedUntilEpochMs - nowMs) / 1000L).coerceAtLeast(1L)
                    throw IllegalStateException("Account temporarily locked due to failed login attempts. Try again in ${remainingSec}s.")
                }

                if (existing.adminRole != normalizedRole) {
                    throw IllegalStateException(
                        "This Google account is already bound to the ${existing.displayName} (${existing.canonicalAccountId}) role. Role self-escalation is prohibited; only the Founder can reassign administrative roles."
                    )
                }

                val computedHash = hashAdminPassword(cleanPassword, existing.passwordSalt)
                if (!computedHash.equals(existing.passwordVerifierHash, ignoreCase = true)) {
                    val nextFailed = existing.failedAttempts + 1L
                    val lockUntil = if (nextFailed >= 5L) nowMs + 60_000L else 0L
                    docRef.update(
                        mapOf(
                            "failedAttempts" to nextFailed,
                            "lockedUntilEpochMs" to lockUntil,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    if (lockUntil > 0L) {
                        throw IllegalArgumentException("Invalid administrative password. 5 failed attempts reached — locked for 60 seconds.")
                    } else {
                        val left = (5L - nextFailed).coerceAtLeast(0L)
                        throw IllegalArgumentException("Invalid administrative password ($left attempts remaining before 60s lockout).")
                    }
                }

                docRef.update(
                    mapOf(
                        "username" to canonicalUsername,
                        "failedAttempts" to 0L,
                        "lockedUntilEpochMs" to 0L,
                        "sessionExpiresAt" to sessionExpiresTimestamp,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                existing.copy(
                    username = canonicalUsername,
                    failedAttempts = 0L,
                    lockedUntilEpochMs = 0L,
                    sessionExpiresAt = sessionExpiresTimestamp
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun refreshAdminSessionDeadline(
        adminRole: String,
        sessionTimeoutMinutes: Long = 15L
    ): Result<Long> = runCatching {
        val uid = requireUserId()
        val nowMs = System.currentTimeMillis()
        val expiresAtMs = if (adminRole == "founder") {
            nowMs + 365L * 24L * 60L * 60L * 1000L
        } else {
            nowMs + sessionTimeoutMinutes.coerceIn(5L, 120L) * 60L * 1000L
        }
        val docRef = db.collection("admins").document(uid)
        try {
            docRef.update(
                mapOf(
                    "sessionExpiresAt" to Timestamp(Date(expiresAtMs)),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            expiresAtMs
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun changeAdminPassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cleanNew = newPassword.trim()
        require(cleanNew.length >= 6) { "New password must be at least 6 characters." }
        val docRef = db.collection("admins").document(uid)
        try {
            val snap = docRef.get().await()
            val existing = snap.toObject(AdminAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalStateException("Admin account not found.")
            val currentHash = hashAdminPassword(currentPassword.trim(), existing.passwordSalt)
            require(currentHash.equals(existing.passwordVerifierHash, ignoreCase = true)) {
                "Current administrative password is incorrect."
            }
            val newSalt = "sc_salt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val newHash = hashAdminPassword(cleanNew, newSalt)
            docRef.update(
                mapOf(
                    "passwordSalt" to newSalt,
                    "passwordVerifierHash" to newHash,
                    "mustChangePassword" to false,
                    "failedAttempts" to 0L,
                    "lockedUntilEpochMs" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun founderReassignOrResetAdmin(
        targetAdminUid: String,
        newRole: String,
        newPasswordOptional: String = ""
    ): Result<Unit> = runCatching {
        requireUserId()
        val normalizedRole = newRole.trim().lowercase()
        val (canonicalId, displayName, defaultUsername, isFounderPersistent) = when (normalizedRole) {
            "cofounder" -> Quadruple("ADM-COF-001", "Abhay", "Abhay", false)
            "ceo" -> Quadruple("ADM-CEO-001", "CEO", "Abhay", false)
            "founder" -> Quadruple("ADM-FND-001", "Founder", "Abhisheksingh", true)
            else -> throw IllegalArgumentException("Unsupported admin role: $newRole")
        }
        val docRef = db.collection("admins").document(targetAdminUid)
        val updates = mutableMapOf<String, Any>(
            "adminRole" to normalizedRole,
            "canonicalAccountId" to canonicalId,
            "displayName" to displayName,
            "username" to defaultUsername,
            "founderSessionPersistent" to isFounderPersistent,
            "failedAttempts" to 0L,
            "lockedUntilEpochMs" to 0L,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (newPasswordOptional.trim().length >= 6) {
            val newSalt = "sc_salt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            updates["passwordSalt"] = newSalt
            updates["passwordVerifierHash"] = hashAdminPassword(newPasswordOptional.trim(), newSalt)
            updates["mustChangePassword"] = true
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- 13. Payment & Booking Disputes (/disputes/{disputeId}) ---

    fun observeCustomerDisputes(customerId: String): Flow<List<PaymentDispute>> =
        db.collection("disputes")
            .whereEqualTo("customerId", customerId)
            .snapshots()
            .map { snap ->
                snap.toObjects(PaymentDispute::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "disputes")
                }
                throw e
            }

    fun observeProviderDisputes(providerId: String): Flow<List<PaymentDispute>> =
        db.collection("disputes")
            .whereEqualTo("providerId", providerId)
            .snapshots()
            .map { snap ->
                snap.toObjects(PaymentDispute::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "disputes")
                }
                throw e
            }

    fun observeAllDisputesForAdmin(): Flow<List<PaymentDispute>> =
        db.collection("disputes")
            .snapshots()
            .map { snap ->
                snap.toObjects(PaymentDispute::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception && e !is CancellationException) {
                    handleFirestoreError(e, OperationType.LIST, "disputes")
                }
                throw e
            }

    suspend fun createPaymentDispute(
        booking: BookingRequest,
        paymentId: String,
        reason: String,
        statement: String,
        evidenceNote: String,
        isFiledByCustomer: Boolean
    ): Result<String> = runCatching {
        requireUserId()
        val cleanBookingId = booking.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(48)
        val disputeId = "dsp_$cleanBookingId"
        val docRef = db.collection("disputes").document(disputeId)
        val amount = booking.effectiveTotalInr.coerceIn(0L, 10000000L)
        val payload = mapOf(
            "bookingId" to booking.id,
            "paymentId" to paymentId.take(128),
            "customerId" to booking.customerId,
            "customerName" to booking.customerName.take(80),
            "providerId" to booking.providerId,
            "providerName" to booking.providerName.take(80),
            "disputedAmountInr" to amount,
            "reason" to reason.trim().ifBlank { "Service or payment dispute" }.take(200),
            "customerStatement" to if (isFiledByCustomer) statement.trim().take(1500) else "",
            "providerStatement" to if (!isFiledByCustomer) statement.trim().take(1500) else "",
            "evidenceNote" to evidenceNote.trim().take(500),
            "status" to "open",
            "assignedRole" to "cofounder",
            "adminDecisionNote" to "",
            "decidedByAdminId" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            disputeId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun addDisputeStatement(
        disputeId: String,
        isCustomer: Boolean,
        statement: String,
        evidenceNote: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("disputes").document(disputeId)
        val fieldName = if (isCustomer) "customerStatement" else "providerStatement"
        try {
            docRef.update(
                mapOf(
                    fieldName to statement.trim().take(1500),
                    "evidenceNote" to evidenceNote.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun adminResolveOrEscalateDispute(
        dispute: PaymentDispute,
        newStatus: String,
        assignedRole: String,
        decisionNote: String
    ): Result<Unit> = runCatching {
        val adminUid = requireUserId()
        val docRef = db.collection("disputes").document(dispute.id)
        try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "assignedRole" to assignedRole,
                    "adminDecisionNote" to decisionNote.trim().take(600),
                    "decidedByAdminId" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

