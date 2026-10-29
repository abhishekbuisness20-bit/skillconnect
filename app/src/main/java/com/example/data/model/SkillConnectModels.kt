package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class PortfolioProjectItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val skillsUsed: String = "",
    val imageUri: String = "",
    val projectLink: String = ""
)

data class ProfileCompletionCheck(
    val label: String,
    val isDone: Boolean
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val platformId: String = "",
    val displayName: String = "",
    val headline: String = "",
    val bio: String = "",
    val role: String = "customer", // "customer" or "provider"
    val city: String = "Bengaluru",
    val locality: String = "",
    val preferredLanguage: String = "en", // "en" or "hi"
    val skills: List<String> = emptyList(),
    val startingPrice: Long = 0L,
    val availability: String = "Mon–Sat, 9:00 AM – 7:00 PM",
    val isVerified: Boolean = false,
    val verificationStatus: String = "not_submitted", // draft, not_submitted, pending_review, under_review, approved, rejected, additional_info_required, suspended
    val subscriptionPlan: String = "free", // "free" or "premium"
    val isPublicProvider: Boolean = false,
    val accountStatus: String = "active", // "active" or "suspended"
    val publicPhone: String = "",
    val completedJobsCount: Long = 0L,
    val ratingSum: Long = 0L,
    val reviewCount: Long = 0L,
    val onboardingStep: Long = 1L,
    val onboardingCompleted: Boolean = false,
    val experienceLevel: String = "Intermediate",
    val experienceYears: Long = 2L,
    val languagesSpoken: List<String> = listOf("English", "Hindi"),
    val qualifications: String = "",
    val businessType: String = "Individual Freelancer",
    val businessName: String = "",
    val primaryCategory: String = "Graphic Design",
    val subcategories: String = "",
    val pricingModel: String = "fixed",
    val remoteAvailable: Boolean = true,
    val inPersonAvailable: Boolean = true,
    val serviceState: String = "Karnataka",
    val pinCode: String = "",
    val serviceAreaRadius: String = "Within 15 km",
    val avatarUri: String = "",
    val coverUri: String = "",
    val portfolioSummaryJson: String = "",
    val publicListingConsent: Boolean = true,
    val verificationRejectionReason: String = "",
    val emailVerified: Boolean = true,
    val phoneVerified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val averageRating: Double
        get() = if (reviewCount > 0L) ratingSum.toDouble() / reviewCount.toDouble() else 0.0

    val effectivePlatformId: String
        get() {
            if (platformId.isNotBlank()) return platformId
            val prefix = if (role == "provider") "SC-PRO" else "SC-CUS"
            val seed = userId.ifBlank { id }.uppercase().filter { it.isLetterOrDigit() }.padEnd(8, '0').take(8)
            return "$prefix-$seed"
        }

    val isStrictVerifiedBadgeActive: Boolean
        get() = isVerified && verificationStatus == "approved" && accountStatus == "active"

    val verificationBadgeLabel: String
        get() = when {
            accountStatus == "suspended" || verificationStatus == "suspended" -> "Listing Suspended"
            isStrictVerifiedBadgeActive -> "Verified by SkillConnect"
            verificationStatus == "under_review" -> "Under Review"
            verificationStatus == "pending_review" -> "Verification Pending"
            verificationStatus == "rejected" -> "Verification Rejected"
            verificationStatus == "additional_info_required" -> "Additional Info Requested"
            verificationStatus == "draft" -> "Draft Profile"
            else -> "Unverified Profile"
        }

    fun parsePortfolioProjects(): List<PortfolioProjectItem> {
        if (portfolioSummaryJson.isBlank()) return emptyList()
        return portfolioSummaryJson.split("||").mapNotNull { entry ->
            val parts = entry.split("::")
            if (parts.size >= 2) {
                PortfolioProjectItem(
                    id = parts.getOrNull(0).orEmpty(),
                    title = parts.getOrNull(1).orEmpty(),
                    description = parts.getOrNull(2).orEmpty(),
                    skillsUsed = parts.getOrNull(3).orEmpty(),
                    imageUri = parts.getOrNull(4).orEmpty(),
                    projectLink = parts.getOrNull(5).orEmpty()
                )
            } else null
        }
    }

    fun buildCompletionChecklist(hasPublishedService: Boolean): List<ProfileCompletionCheck> {
        return listOf(
            ProfileCompletionCheck("Account verified", emailVerified || phoneVerified),
            ProfileCompletionCheck("Profile photo or portfolio added", avatarUri.isNotBlank() || portfolioSummaryJson.isNotBlank()),
            ProfileCompletionCheck("Professional information completed", headline.isNotBlank() && bio.length >= 15 && skills.isNotEmpty()),
            ProfileCompletionCheck("Category selected", primaryCategory.isNotBlank()),
            ProfileCompletionCheck("Service published", hasPublishedService || startingPrice > 0L),
            ProfileCompletionCheck("Location & service area set", city.isNotBlank() && (locality.isNotBlank() || serviceAreaRadius.isNotBlank())),
            ProfileCompletionCheck(
                "Verification submitted",
                verificationStatus in listOf("pending_review", "under_review", "approved")
            )
        )
    }

    fun completionPercentage(hasPublishedService: Boolean): Int {
        val checks = buildCompletionChecklist(hasPublishedService)
        val done = checks.count { it.isDone }
        return ((done * 100f) / checks.size).toInt()
    }
}

fun encodePortfolioProjects(items: List<PortfolioProjectItem>): String {
    return items.take(8).joinToString("||") { item ->
        listOf(
            item.id.replace("::", " ").replace("||", " "),
            item.title.replace("::", " ").replace("||", " ").take(80),
            item.description.replace("::", " ").replace("||", " ").take(250),
            item.skillsUsed.replace("::", " ").replace("||", " ").take(100),
            item.imageUri.replace("::", " ").replace("||", " ").take(200),
            item.projectLink.replace("::", " ").replace("||", " ").take(150)
        ).joinToString("::")
    }.take(4000)
}

@IgnoreExtraProperties
data class ServiceListing(
    @DocumentId val id: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val providerVerified: Boolean = false,
    val title: String = "",
    val category: String = "Graphic Design",
    val subcategory: String = "",
    val shortSummary: String = "",
    val description: String = "",
    val skills: List<String> = emptyList(),
    val startingPrice: Long = 0L,
    val pricingModel: String = "fixed", // "fixed", "hourly", "quote"
    val deliverables: String = "",
    val turnaround: String = "",
    val city: String = "Bengaluru",
    val locality: String = "",
    val availability: String = "Available Mon–Sat",
    val terms: String = "",
    val isPublished: Boolean = true,
    val isDraft: Boolean = false,
    val isFeatured: Boolean = false,
    val moderationStatus: String = "approved", // "approved", "pending", "rejected"
    val moderationReason: String = "",
    val ratingSum: Long = 0L,
    val reviewCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val averageRating: Double
        get() = if (reviewCount > 0L) ratingSum.toDouble() / reviewCount.toDouble() else 0.0
}

@IgnoreExtraProperties
data class BookingRequest(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val serviceId: String = "",
    val serviceTitle: String = "",
    val category: String = "",
    val requirements: String = "",
    val preferredDate: String = "",
    val attachmentNote: String = "",
    val quotedPrice: Long = 0L,
    val requestType: String = "booking", // "booking" or "quote"
    val status: String = "pending", // draft, pending, accepted, declined, awaiting_payment, confirmed, in_progress, completed, cancelled, disputed, refund_pending, refunded
    val statusNote: String = "",
    val hasReviewed: Boolean = false,
    val bookingRef: String = "",
    val preferredTimeSlot: String = "10:00 AM – 1:00 PM",
    val paymentMethod: String = "upi", // upi, card, netbanking, cash_after_service
    val paymentStatus: String = "unpaid", // unpaid, paid_online, pending_offline, refund_requested, refunded
    val paymentReferenceId: String = "",
    val platformFeeInr: Long = 0L,
    val totalAmountInr: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val effectiveBookingRef: String
        get() = bookingRef.ifBlank {
            "SC-BK-${id.removePrefix("bk_").uppercase().padEnd(8, '0').take(8)}"
        }

    val effectiveTotalInr: Long
        get() = if (totalAmountInr > 0L) totalAmountInr else quotedPrice
}

@IgnoreExtraProperties
data class PaymentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val providerId: String = "",
    val bookingId: String = "",
    val paymentType: String = "booking_payment", // booking_payment, subscription, payout, refund
    val method: String = "upi", // upi, card, netbanking, bank_transfer, cash_offline
    val amountInr: Long = 0L,
    val status: String = "succeeded", // pending, succeeded, failed, refund_pending, refunded, paid_out
    val transactionRef: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminAuditLog(
    @DocumentId val id: String = "",
    val adminId: String = "",
    val adminEmail: String = "",
    val actionType: String = "",
    val targetCollection: String = "",
    val targetId: String = "",
    val reason: String = "",
    val previousState: String = "",
    val newState: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val bookingId: String = "",
    val serviceTitle: String = "",
    val lastMessage: String = "",
    val unreadCustomerCount: Long = 0L,
    val unreadProviderCount: Long = 0L,
    val isBlocked: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val customerId: String = "",
    val providerId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val senderId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "new_enquiry",
    val relatedId: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ServiceReview(
    @DocumentId val id: String = "",
    val bookingId: String = "",
    val serviceId: String = "",
    val providerId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val isReported: Boolean = false,
    val moderationStatus: String = "published", // "published", "flagged", "removed"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FavouriteItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetId: String = "",
    val targetType: String = "service", // "service" or "provider"
    val title: String = "",
    val subtitle: String = "",
    val location: String = "",
    val price: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VerificationSubmission(
    @DocumentId val id: String = "",
    val userId: String = "",
    val legalName: String = "",
    val businessName: String = "",
    val category: String = "Graphic Design",
    val city: String = "Bengaluru",
    val experienceYears: Long = 1L,
    val portfolioSummary: String = "",
    val documentReferenceNote: String = "",
    val status: String = "pending_review", // pending_review, under_review, approved, rejected, additional_info_required, suspended
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SafetyReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetId: String = "",
    val targetType: String = "service", // "service", "provider", "review", "user"
    val reason: String = "",
    val details: String = "",
    val status: String = "open", // "open", "investigating", "resolved", "dismissed"
    val adminResolution: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val contactEmail: String = "",
    val category: String = "Booking Issue",
    val relatedBookingId: String = "",
    val description: String = "",
    val status: String = "open", // "open", "in_progress", "resolved"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PlatformSetting(
    @DocumentId val id: String = "",
    val settingKey: String = "global",
    val announcementText: String = "Welcome to SkillConnect India — Verified Local Professionals & Freelancers",
    val announcementActive: Boolean = true,
    val freeListingLimit: Long = 3L,
    val premiumPriceInr: Long = 499L,
    val billingPeriod: String = "Monthly",
    val paymentsDemoMode: Boolean = true,
    val maintenanceMode: Boolean = false,
    val sessionTimeoutMinutes: Long = 15L,
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminAccount(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val canonicalAccountId: String = "ADM-COF-001", // "ADM-COF-001", "ADM-CEO-001", "ADM-FND-001"
    val adminRole: String = "cofounder", // "cofounder", "ceo", "founder"
    val displayName: String = "Abhay", // "Abhay", "CEO", "Founder"
    val username: String = "Abhay", // "Abhay", "Adarsh", "Abhisheksingh"
    val passwordVerifierHash: String = "",
    val passwordSalt: String = "",
    val mustChangePassword: Boolean = false,
    val failedAttempts: Long = 0L,
    val lockedUntilEpochMs: Long = 0L,
    val founderSessionPersistent: Boolean = false,
    val sessionExpiresAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PaymentDispute(
    @DocumentId val id: String = "",
    val bookingId: String = "",
    val paymentId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val disputedAmountInr: Long = 0L,
    val reason: String = "",
    val customerStatement: String = "",
    val providerStatement: String = "",
    val evidenceNote: String = "",
    val status: String = "open", // open, under_review, escalated_to_ceo, escalated_to_founder, resolved_refunded, resolved_rejected
    val assignedRole: String = "cofounder", // cofounder, ceo, founder
    val adminDecisionNote: String = "",
    val decidedByAdminId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class JobOpportunityItem(
    val id: String,
    val title: String,
    val companyOrClient: String,
    val category: String,
    val city: String,
    val payoutRangeInr: String,
    val commitmentType: String, // "Immediate Gig", "Part-Time Contract", "Project Milestone"
    val skillsRequired: List<String>,
    val summary: String,
    val verifiedClient: Boolean = true
)

data class SkillCourseItem(
    val id: String,
    val title: String,
    val instructorName: String,
    val category: String,
    val durationHours: Int,
    val level: String, // "Beginner", "Intermediate", "Pro Certification"
    val priceInr: Long,
    val badgeEarned: String,
    val summary: String
)

