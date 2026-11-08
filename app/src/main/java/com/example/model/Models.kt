package com.example.model

enum class UserRole {
    CUSTOMER,
    SERVICE_PROVIDER,
    ADMIN
}

enum class AppLanguage {
    ENGLISH,
    HINDI
}

enum class WorkRequestStatus(val labelEn: String, val labelHi: String) {
    PENDING("Pending", "लंबित"),
    ACCEPTED("Accepted", "स्वीकृत"),
    REJECTED("Rejected", "अस्वीकृत"),
    IN_PROGRESS("In Progress", "प्रगति पर"),
    COMPLETED("Completed", "पूर्ण"),
    CANCELLED("Cancelled", "रद्द")
}

enum class AdminRoleType(val roleCode: String, val displayName: String, val levelTitle: String) {
    FOUNDER("FOUNDER", "Founder", "Super Admin"),
    CEO("CEO", "Chief Executive Officer", "Executive Admin"),
    ADMIN("ADMIN", "Administrator", "Limited Admin");

    companion object {
        fun fromCode(code: String?): AdminRoleType? {
            if (code == null) return null
            val upper = code.trim().uppercase()
            return entries.find { it.roleCode == upper || it.name == upper }
        }
    }
}

data class AdminPermissions(
    val canManageAdmins: Boolean = false,
    val canManageRoles: Boolean = false,
    val canManageCategories: Boolean = false,
    val canBroadcastAnnouncements: Boolean = false,
    val canApproveProviders: Boolean = false,
    val canModerateContent: Boolean = false,
    val canResolveComplaints: Boolean = false,
    val canAccessSecurityLogs: Boolean = false,
    val canViewFinancialAnalytics: Boolean = false,
    val canDeletePlatformData: Boolean = false
) {
    companion object {
        val FOUNDER_PERMISSIONS = AdminPermissions(
            canManageAdmins = true,
            canManageRoles = true,
            canManageCategories = true,
            canBroadcastAnnouncements = true,
            canApproveProviders = true,
            canModerateContent = true,
            canResolveComplaints = true,
            canAccessSecurityLogs = true,
            canViewFinancialAnalytics = true,
            canDeletePlatformData = true
        )

        val CEO_PERMISSIONS = AdminPermissions(
            canManageAdmins = false,
            canManageRoles = false,
            canManageCategories = false,
            canBroadcastAnnouncements = true,
            canApproveProviders = true,
            canModerateContent = true,
            canResolveComplaints = true,
            canAccessSecurityLogs = false,
            canViewFinancialAnalytics = true,
            canDeletePlatformData = false
        )

        val LIMITED_ADMIN_PERMISSIONS = AdminPermissions(
            canManageAdmins = false,
            canManageRoles = false,
            canManageCategories = false,
            canBroadcastAnnouncements = false,
            canApproveProviders = true,
            canModerateContent = true,
            canResolveComplaints = true,
            canAccessSecurityLogs = false,
            canViewFinancialAnalytics = false,
            canDeletePlatformData = false
        )
    }
}

data class AdminAuditLog(
    val id: String,
    val adminUid: String,
    val adminEmail: String,
    val roleType: AdminRoleType,
    val action: String,
    val targetEntityId: String = "",
    val details: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)

enum class ComplaintStatus(val label: String) {
    PENDING("Pending Review"),
    INVESTIGATING("Investigating"),
    ESCALATED_TO_CEO("Escalated to CEO"),
    RESOLVED("Resolved"),
    DISMISSED("Dismissed")
}

data class PlatformComplaint(
    val id: String,
    val reporterId: String,
    val reporterName: String,
    val targetProviderId: String? = null,
    val targetProviderName: String? = null,
    val reason: String,
    val details: String,
    val status: ComplaintStatus = ComplaintStatus.PENDING,
    val assignedRoleLevel: AdminRoleType = AdminRoleType.ADMIN,
    val resolutionNotes: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val adminRole: AdminRoleType? = null,
    val city: String,
    val state: String,
    val avatarInitials: String = "",
    val language: AppLanguage = AppLanguage.ENGLISH,
    val profilePhotoUri: String? = null,
    val coverPhotoUri: String? = null,
    val professionalHeadline: String = "",
    val bio: String = "",
    val skills: List<String> = emptyList(),
    val experienceYears: Int = 0,
    val education: String = "",
    val portfolioLinks: List<String> = emptyList(),
    val portfolioImages: List<String> = emptyList(),
    val socialGithub: String = "",
    val socialLinkedin: String = "",
    val socialTwitter: String = "",
    val contactPreference: String = "Email & In-App Chat"
)

data class PortfolioItem(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val imageResName: String = ""
)

data class ServiceOffering(
    val id: String,
    val name: String,
    val description: String,
    val startingPriceInr: Long,
    val deliveryDays: Int
)

data class ServiceProvider(
    val id: String,
    val userId: String,
    val name: String,
    val professionalTitle: String,
    val categoryId: String,
    val subcategories: List<String>,
    val bio: String,
    val city: String,
    val state: String,
    val hourlyRateInr: Long,
    val rating: Double,
    val reviewCount: Int,
    val completedProjects: Int,
    val isVerified: Boolean,
    val isAvailable: Boolean = true,
    val skills: List<String>,
    val languages: List<String>,
    val services: List<ServiceOffering> = emptyList(),
    val portfolio: List<PortfolioItem> = emptyList()
)

data class ServiceCategory(
    val id: String,
    val nameEn: String,
    val nameHi: String,
    val descriptionEn: String,
    val descriptionHi: String,
    val iconName: String,
    val subcategories: List<String>
)

data class WorkRequest(
    val id: String,
    val customerId: String,
    val customerName: String,
    val providerId: String,
    val providerName: String,
    val title: String,
    val description: String,
    val category: String,
    val budgetInr: Long,
    val deadline: String,
    val status: WorkRequestStatus = WorkRequestStatus.PENDING,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val notes: String = ""
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class Conversation(
    val id: String,
    val customerId: String,
    val customerName: String,
    val providerId: String,
    val providerName: String,
    val lastMessage: String,
    val lastTimestampMillis: Long,
    val unreadCount: Int = 0
)

data class Review(
    val id: String,
    val providerId: String,
    val customerId: String,
    val customerName: String,
    val rating: Int,
    val comment: String,
    val date: String,
    val isVerifiedPurchase: Boolean = true
)

data class AdminPlatformMetrics(
    val totalUsers: Int,
    val totalCustomers: Int,
    val totalProviders: Int,
    val pendingVerifications: Int,
    val verifiedProviders: Int,
    val activeWorkRequests: Int,
    val completedWorkRequests: Int,
    val totalVolumeInr: Long
)

enum class ProviderSortOption(val label: String) {
    RELEVANCE("Relevance"),
    RATING_HIGH_TO_LOW("Top Rated"),
    PRICE_LOW_TO_HIGH("Lowest Rate")
}

enum class ServiceScopeFilter(val label: String) {
    ALL_SERVICES("All Services"),
    LOCAL_SERVICES("Local Services")
}

data class InAppNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val category: String = "Platform"
)

data class MapsGroundedPlace(
    val title: String,
    val uri: String,
    val placeId: String = ""
)

data class AiChatMessage(
    val id: String,
    val isFromUser: Boolean,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isFallback: Boolean = false,
    val mapsPlaces: List<MapsGroundedPlace> = emptyList()
)

sealed interface AiAssistantUiState {
    data object Idle : AiAssistantUiState
    data object Loading : AiAssistantUiState
    data class Streaming(val partialText: String) : AiAssistantUiState
    data class Success(val text: String) : AiAssistantUiState
    data class Error(val message: String, val fallbackAdvice: String? = null) : AiAssistantUiState
}

object SkillConnectBranding {
    const val BRAND_NAME = "SKILLCONNECT"
    const val DISPLAY_NAME = "SkillConnect"
    const val TAGLINE = "Your Skills. Your Local Network."
    const val DESCRIPTION = "Connecting customers with verified local professionals and freelancers across India."
    const val OFFICIAL_COMMUNITY = "@skillconnectofficial"
    const val OFFICIAL_EMAIL = "skillconnectofficiall@gmail.com"
    const val COPYRIGHT = "© 2026 SkillConnect India."
}

