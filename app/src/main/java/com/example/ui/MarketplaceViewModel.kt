package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminRoleDoc
import com.example.data.MarketplaceRepository
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Tool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "MarketplaceViewModel"

data class MarketplaceUiState(
    val isAuthenticated: Boolean = false,
    val currentUser: UserProfile,
    val activeRole: UserRole = UserRole.CUSTOMER,
    val isDarkTheme: Boolean = true,
    val customerScreenRoute: String = "home",
    val providerScreenRoute: String = "dashboard",
    val adminScreenRoute: String = "admin",
    val currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    val categories: List<ServiceCategory> = emptyList(),
    val isCategoriesLoading: Boolean = false,
    val allProviders: List<ServiceProvider> = emptyList(),
    val filteredProviders: List<ServiceProvider> = emptyList(),
    val workRequests: List<WorkRequest> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val announcements: List<String> = emptyList(),
    val adminMetrics: AdminPlatformMetrics,
    val adminRolesList: List<AdminRoleDoc> = emptyList(),
    val auditLogsList: List<AdminAuditLog> = emptyList(),
    val complaintsList: List<PlatformComplaint> = emptyList(),
    val activeAdminRole: AdminRoleType? = null,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedCity: String? = null,
    val serviceScopeFilter: ServiceScopeFilter = ServiceScopeFilter.ALL_SERVICES,
    val availableOnlyFilter: Boolean = false,
    val verifiedOnlyFilter: Boolean = false,
    val sortOption: ProviderSortOption = ProviderSortOption.RELEVANCE,
    val recentlyViewedProviders: List<ServiceProvider> = emptyList(),
    val favouriteProviderIds: Set<String> = emptySet(),
    val favouriteProviders: List<ServiceProvider> = emptyList(),
    val searchSuggestions: List<String> = listOf(
        "Figma UI/UX",
        "Android Compose",
        "Brand Logo",
        "4K Video Editing",
        "SEO Growth",
        "GST & Tax Filing",
        "Next.js Website",
        "Content Writing"
    ),
    val notifications: List<InAppNotification> = emptyList(),
    val isAiAssistantOpen: Boolean = false,
    val aiChatMessages: List<AiChatMessage> = emptyList(),
    val aiUiState: AiAssistantUiState = AiAssistantUiState.Idle,
    val selectedProvider: ServiceProvider? = null,
    val activeConversationPartnerId: String? = null,
    val activeConversationPartnerName: String? = null,
    val currentScreenRoute: String = "home",
    val isCreateRequestDialogOpen: Boolean = false,
    val isReviewDialogOpen: Boolean = false,
    val targetWorkRequestIdForReview: String? = null,
    val lastAdminActivityMillis: Long = System.currentTimeMillis(),
    val sessionTimeoutMessage: String? = null,
    val registrationConfirmationMessage: String? = null,
    val isMarketplaceRefreshing: Boolean = false,
    val lastRefreshedMillis: Long = System.currentTimeMillis(),
    val isNetworkOnline: Boolean = true
)

class MarketplaceViewModel(
    private val repository: MarketplaceRepository = MarketplaceRepository()
) : ViewModel() {

    companion object {
        const val ADMIN_SESSION_TIMEOUT_MS: Long = 15 * 60 * 1000L // 15 minutes in milliseconds
    }

    // Lazy initialization of Gemini model via Firebase AI Logic with Google Maps Grounding
    private val geminiModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleMaps())
        )
    }

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _selectedCity = MutableStateFlow<String?>(null)
    private val _serviceScopeFilter = MutableStateFlow(ServiceScopeFilter.ALL_SERVICES)
    private val _availableOnlyFilter = MutableStateFlow(false)
    private val _verifiedOnlyFilter = MutableStateFlow(false)
    private val _sortOption = MutableStateFlow(ProviderSortOption.RELEVANCE)
    private val _recentlyViewedIds = MutableStateFlow<List<String>>(emptyList())
    private val _favouriteIds = MutableStateFlow<Set<String>>(setOf("prov_1", "prov_2"))
    private val _notifications = MutableStateFlow<List<InAppNotification>>(
        listOf(
            InAppNotification(
                id = "notif_welcome",
                title = "Welcome to SkillConnect India",
                message = "Browse verified local professionals and freelancers across India with 0% platform commission.",
                category = "Platform"
            ),
            InAppNotification(
                id = "notif_verified",
                title = "Verified Talent Network",
                message = "All top-rated providers carry the SkillConnect Verified badge for trusted hiring.",
                category = "Verification"
            )
        )
    )
    private val _isAiAssistantOpen = MutableStateFlow(false)
    private val _aiChatMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                id = "ai_welcome",
                isFromUser = false,
                text = "Namaste! I'm your SkillConnect Gemini AI Assistant powered by Google Maps Grounding (gemini-3.8-flash). Ask me about services, local business hubs on Google Maps, pricing in ₹ INR, or finding the right verified Indian professional for your project!",
                mapsPlaces = listOf(
                    MapsGroundedPlace(
                        title = "Bengaluru Tech & Design Hub • Google Maps",
                        uri = "https://www.google.com/maps/search/?api=1&query=Bengaluru+Karnataka+India"
                    ),
                    MapsGroundedPlace(
                        title = "Mumbai Creative Studios • Google Maps",
                        uri = "https://www.google.com/maps/search/?api=1&query=Mumbai+Maharashtra+India"
                    )
                )
            )
        )
    )
    private val _aiUiState = MutableStateFlow<AiAssistantUiState>(AiAssistantUiState.Idle)

    private val _selectedProvider = MutableStateFlow<ServiceProvider?>(null)
    private val _activeChatPartner = MutableStateFlow<Pair<String, String>?>(null)
    private val _activeRole = MutableStateFlow<UserRole>(
        if (repository.isAuthenticated.value) repository.currentUser.value.role else UserRole.CUSTOMER
    )
    private val _customerScreenRoute = MutableStateFlow("home")
    private val _providerScreenRoute = MutableStateFlow("dashboard")
    private val _adminScreenRoute = MutableStateFlow("admin")
    private val _isCreateRequestOpen = MutableStateFlow(false)
    private val _isReviewOpen = MutableStateFlow(false)
    private val _reviewTargetReqId = MutableStateFlow<String?>(null)
    private val _isCategoriesLoading = MutableStateFlow(false)
    private val _isMarketplaceRefreshing = MutableStateFlow(false)
    private val _lastRefreshedMillis = MutableStateFlow(System.currentTimeMillis())
    private val _lastAdminActivityMillis = MutableStateFlow(System.currentTimeMillis())
    private val _sessionTimeoutMessage = MutableStateFlow<String?>(null)
    private val _registrationConfirmationMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MarketplaceUiState> = combine(
        repository.isAuthenticated,
        repository.currentUser,
        repository.currentLanguage,
        repository.categories,
        repository.providers,
        repository.workRequests,
        repository.messages,
        repository.reviews,
        repository.announcements,
        _searchQuery,
        _selectedCategory,
        _selectedCity,
        _serviceScopeFilter,
        _availableOnlyFilter,
        _verifiedOnlyFilter,
        _sortOption,
        _recentlyViewedIds,
        _favouriteIds,
        _notifications,
        _isAiAssistantOpen,
        _aiChatMessages,
        _aiUiState,
        _selectedProvider,
        _activeChatPartner,
        _activeRole,
        _customerScreenRoute,
        _providerScreenRoute,
        _adminScreenRoute,
        _isCreateRequestOpen,
        _isReviewOpen,
        _reviewTargetReqId,
        repository.isDarkTheme,
        _isCategoriesLoading,
        _lastAdminActivityMillis,
        _sessionTimeoutMessage,
        repository.adminRoles,
        repository.auditLogs,
        repository.complaints,
        _isMarketplaceRefreshing,
        _lastRefreshedMillis,
        _registrationConfirmationMessage,
        com.example.data.NetworkConnectivityMonitor.isOnline
    ) { args: Array<Any?> ->
        val isAuth = args[0] as Boolean
        val user = args[1] as UserProfile
        val lang = args[2] as AppLanguage
        val cats = args[3] as List<ServiceCategory>
        val provs = args[4] as List<ServiceProvider>
        val reqs = args[5] as List<WorkRequest>
        val msgs = args[6] as List<ChatMessage>
        val revs = args[7] as List<Review>
        val anns = args[8] as List<String>
        val query = args[9] as String
        val selCat = args[10] as String?
        val selCity = args[11] as String?
        val scopeFilter = args[12] as ServiceScopeFilter
        val availOnly = args[13] as Boolean
        val verifOnly = args[14] as Boolean
        val sortOpt = args[15] as ProviderSortOption
        val recentIds = args[16] as List<String>
        val favIds = args[17] as Set<String>
        val notifs = args[18] as List<InAppNotification>
        val aiOpen = args[19] as Boolean
        val aiMsgs = args[20] as List<AiChatMessage>
        val aiState = args[21] as AiAssistantUiState
        val selProv = args[22] as ServiceProvider?
        val chatPartner = args[23] as Pair<String, String>?
        val role = args[24] as UserRole
        val custRoute = args[25] as String
        val provRoute = args[26] as String
        val adminRoute = args[27] as String
        val isReqOpen = args[28] as Boolean
        val isRevOpen = args[29] as Boolean
        val targetReq = args[30] as String?
        val isDark = args[31] as Boolean
        val isCatsLoading = args[32] as Boolean
        val lastAdminAct = args[33] as Long
        val timeoutMsg = args[34] as String?
        val adminRoles = args[35] as List<AdminRoleDoc>
        val auditLogs = args[36] as List<AdminAuditLog>
        val complaints = args[37] as List<PlatformComplaint>
        val isRefreshing = args[38] as Boolean
        val lastRefreshed = args[39] as Long
        val regConfirmMsg = args[40] as String?
        val isNetOnline = args[41] as Boolean

        val currentRoute = when (role) {
            UserRole.SERVICE_PROVIDER -> provRoute
            UserRole.ADMIN -> adminRoute
            UserRole.CUSTOMER -> custRoute
        }

        val categoryNameById = cats.associateBy({ it.id }, { "${it.nameEn} ${it.nameHi}" })

        val filteredUnsorted = provs.filter { provider ->
            val catNames = categoryNameById[provider.categoryId].orEmpty()
            val matchesQuery = query.isBlank() ||
                    provider.name.contains(query, ignoreCase = true) ||
                    provider.professionalTitle.contains(query, ignoreCase = true) ||
                    catNames.contains(query, ignoreCase = true) ||
                    provider.city.contains(query, ignoreCase = true) ||
                    provider.skills.any { it.contains(query, ignoreCase = true) } ||
                    provider.subcategories.any { it.contains(query, ignoreCase = true) }

            val matchesCat = selCat == null || provider.categoryId == selCat ||
                    provider.subcategories.any { it.equals(selCat, ignoreCase = true) }

            val effectiveLocalCity = selCity ?: user.city.takeIf { it.isNotBlank() }
            val matchesCity = when {
                selCity != null -> provider.city.equals(selCity, ignoreCase = true)
                scopeFilter == ServiceScopeFilter.LOCAL_SERVICES && effectiveLocalCity != null ->
                    provider.city.equals(effectiveLocalCity, ignoreCase = true)
                else -> true
            }

            val matchesAvailability = !availOnly || provider.isAvailable
            val matchesVerified = !verifOnly || provider.isVerified

            matchesQuery && matchesCat && matchesCity && matchesAvailability && matchesVerified
        }

        val filtered = when (sortOpt) {
            ProviderSortOption.RELEVANCE -> filteredUnsorted.sortedWith(
                compareByDescending<ServiceProvider> { it.isVerified }
                    .thenByDescending { it.completedProjects }
                    .thenByDescending { it.rating }
            )
            ProviderSortOption.RATING_HIGH_TO_LOW -> filteredUnsorted.sortedWith(
                compareByDescending<ServiceProvider> { it.rating }
                    .thenByDescending { it.reviewCount }
            )
            ProviderSortOption.PRICE_LOW_TO_HIGH -> filteredUnsorted.sortedBy { it.hourlyRateInr }
        }

        val recentProviders = recentIds.mapNotNull { id -> provs.find { it.id == id } }
        val favProviders = provs.filter { it.id in favIds }

        MarketplaceUiState(
            isAuthenticated = isAuth,
            currentUser = user,
            activeRole = role,
            isDarkTheme = isDark,
            customerScreenRoute = custRoute,
            providerScreenRoute = provRoute,
            adminScreenRoute = adminRoute,
            currentLanguage = lang,
            categories = cats,
            isCategoriesLoading = isCatsLoading,
            allProviders = provs,
            filteredProviders = filtered,
            workRequests = reqs,
            messages = msgs,
            reviews = revs,
            announcements = anns,
            adminMetrics = repository.getAdminMetrics(),
            adminRolesList = adminRoles,
            auditLogsList = auditLogs,
            complaintsList = complaints,
            activeAdminRole = user.adminRole,
            searchQuery = query,
            selectedCategory = selCat,
            selectedCity = selCity,
            serviceScopeFilter = scopeFilter,
            availableOnlyFilter = availOnly,
            verifiedOnlyFilter = verifOnly,
            sortOption = sortOpt,
            recentlyViewedProviders = recentProviders,
            favouriteProviderIds = favIds,
            favouriteProviders = favProviders,
            notifications = notifs,
            isAiAssistantOpen = aiOpen,
            aiChatMessages = aiMsgs,
            aiUiState = aiState,
            selectedProvider = selProv,
            activeConversationPartnerId = chatPartner?.first,
            activeConversationPartnerName = chatPartner?.second,
            currentScreenRoute = currentRoute,
            isCreateRequestDialogOpen = isReqOpen,
            isReviewDialogOpen = isRevOpen,
            targetWorkRequestIdForReview = targetReq,
            lastAdminActivityMillis = lastAdminAct,
            sessionTimeoutMessage = timeoutMsg,
            registrationConfirmationMessage = regConfirmMsg,
            isMarketplaceRefreshing = isRefreshing,
            lastRefreshedMillis = lastRefreshed,
            isNetworkOnline = isNetOnline
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MarketplaceUiState(
            isAuthenticated = repository.isAuthenticated.value,
            currentUser = repository.currentUser.value,
            activeRole = if (repository.isAuthenticated.value) repository.currentUser.value.role else UserRole.CUSTOMER,
            isDarkTheme = true,
            customerScreenRoute = "home",
            providerScreenRoute = "dashboard",
            adminScreenRoute = "admin",
            currentScreenRoute = if (repository.isAuthenticated.value) {
                when (repository.currentUser.value.role) {
                    UserRole.SERVICE_PROVIDER -> "dashboard"
                    UserRole.ADMIN -> "admin"
                    UserRole.CUSTOMER -> "home"
                }
            } else "home",
            adminMetrics = repository.getAdminMetrics(),
            adminRolesList = MarketplaceRepository.initialAdminRoles,
            auditLogsList = MarketplaceRepository.initialAuditLogs,
            complaintsList = MarketplaceRepository.initialComplaints,
            isMarketplaceRefreshing = false,
            lastRefreshedMillis = System.currentTimeMillis(),
            isNetworkOnline = com.example.data.NetworkConnectivityMonitor.isCurrentlyConnected()
        )
    )

    fun setCategoriesLoading(loading: Boolean) {
        _isCategoriesLoading.value = loading
    }

    fun refreshCategoriesFromServer(durationMillis: Long = 650L) {
        viewModelScope.launch {
            _isCategoriesLoading.value = true
            delay(durationMillis)
            _isCategoriesLoading.value = false
        }
    }

    fun refreshMarketplaceFeed(durationMillis: Long = 750L, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isMarketplaceRefreshing.value = true
            _isCategoriesLoading.value = true
            if (durationMillis > 0) {
                delay(durationMillis)
            }
            _lastRefreshedMillis.value = System.currentTimeMillis()
            _isCategoriesLoading.value = false
            _isMarketplaceRefreshing.value = false
            onComplete?.invoke()
        }
    }

    // --- Authentication Actions ---

    fun registerCustomer(
        name: String,
        email: String,
        phone: String,
        city: String,
        state: String,
        pass: String
    ): Result<UserProfile> {
        val result = repository.registerCustomer(name, email, phone, city, state, pass)
        if (result.isSuccess) {
            val user = result.getOrThrow()
            _activeRole.value = UserRole.CUSTOMER
            _customerScreenRoute.value = "home"
            _sessionTimeoutMessage.value = null
            _registrationConfirmationMessage.value =
                "Account created for ${user.email}! Your Customer profile is permanently saved for future logins."
            addNotification(
                title = "Account Created Successfully",
                message = "Welcome ${user.name}! Your SkillConnect Customer account (${user.email}) is permanently registered.",
                category = "Account"
            )
            startCloudSyncForUser(user)
        }
        return result
    }

    fun registerProvider(
        name: String,
        email: String,
        phone: String,
        city: String,
        state: String,
        pass: String,
        professionalTitle: String,
        categoryId: String,
        skills: List<String>,
        hourlyRateInr: Long,
        bio: String
    ): Result<UserProfile> {
        val result = repository.registerProvider(
            name = name,
            email = email,
            phone = phone,
            city = city,
            state = state,
            pass = pass,
            professionalTitle = professionalTitle,
            categoryId = categoryId,
            skills = skills,
            hourlyRateInr = hourlyRateInr,
            bio = bio
        )
        if (result.isSuccess) {
            val user = result.getOrThrow()
            _activeRole.value = UserRole.SERVICE_PROVIDER
            _providerScreenRoute.value = "dashboard"
            _sessionTimeoutMessage.value = null
            _registrationConfirmationMessage.value =
                "Account created for ${user.email}! Your Service Provider profile is permanently saved for future logins."
            addNotification(
                title = "Provider Account Created",
                message = "Welcome ${user.name}! Your SkillConnect Service Provider account (${user.email}) is permanently registered.",
                category = "Account"
            )
            startCloudSyncForUser(user)
        }
        return result
    }

    fun clearRegistrationConfirmationMessage() {
        _registrationConfirmationMessage.value = null
    }

    fun login(email: String, pass: String, role: UserRole): Result<UserProfile> {
        val result = repository.login(email, pass, role)
        if (result.isSuccess) {
            val userProfile = result.getOrThrow()
            val userRole = userProfile.role
            _activeRole.value = userRole
            _sessionTimeoutMessage.value = null
            _registrationConfirmationMessage.value = null
            when (userRole) {
                UserRole.CUSTOMER -> _customerScreenRoute.value = "home"
                UserRole.SERVICE_PROVIDER -> _providerScreenRoute.value = "dashboard"
                UserRole.ADMIN -> {
                    _adminScreenRoute.value = "admin"
                    _lastAdminActivityMillis.value = System.currentTimeMillis()
                }
            }
            startCloudSyncForUser(userProfile)
        }
        return result
    }

    fun sendPublicPasswordReset(email: String): Result<String> {
        val result = repository.requestPublicPasswordReset(email)
        if (result.isSuccess) {
            viewModelScope.launch {
                com.example.ui.auth.sendFirebasePasswordResetEmail(email)
            }
        }
        return result
    }

    fun sendAdminPasswordReset(email: String): Result<String> {
        val result = repository.requestAdminPasswordReset(email)
        if (result.isSuccess) {
            viewModelScope.launch {
                com.example.ui.auth.sendFirebasePasswordResetEmail(email)
            }
        }
        return result
    }

    private var cloudSyncJob: Job? = null

    fun loginWithGoogle(
        uid: String,
        displayName: String?,
        email: String?,
        photoUrl: String?,
        role: UserRole
    ): Result<UserProfile> {
        val result = repository.loginWithGoogle(uid, displayName, email, photoUrl, role)
        if (result.isSuccess) {
            val userProfile = result.getOrThrow()
            _activeRole.value = userProfile.role
            _sessionTimeoutMessage.value = null
            _registrationConfirmationMessage.value = null
            when (userProfile.role) {
                UserRole.CUSTOMER -> _customerScreenRoute.value = "home"
                UserRole.SERVICE_PROVIDER -> _providerScreenRoute.value = "dashboard"
                UserRole.ADMIN -> {
                    _adminScreenRoute.value = "admin"
                    _lastAdminActivityMillis.value = System.currentTimeMillis()
                }
            }
            startCloudSyncForUser(userProfile)
        }
        return result
    }

    private fun startCloudSyncForUser(initialProfile: UserProfile) {
        val cloudRepo = repository.firestoreRepository ?: return
        val firebaseUser = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        } ?: return

        val authenticatedUid = firebaseUser.uid
        cloudSyncJob?.cancel()
        cloudSyncJob = viewModelScope.launch {
            com.example.data.FirebaseTokenManager.ensureValidAuthIdToken(forceRefresh = false)
            com.example.data.FirebaseTokenManager.ensureValidAppCheckToken(forceRefresh = false)

            com.example.data.FirebaseTokenManager.getLatestInMemoryFcmToken()?.let { fcmToken ->
                cloudRepo.syncFcmToken(fcmToken)
            }

            val existingRes = cloudRepo.getUserProfile(authenticatedUid)
            val existingDoc = existingRes.getOrNull()
            if (existingDoc != null) {
                repository.applyCloudUserProfile(existingDoc)
                val restoredRole = repository.currentUser.value.role
                _activeRole.value = restoredRole
            } else {
                cloudRepo.saveUserProfile(initialProfile.copy(id = authenticatedUid))
            }

            launch {
                cloudRepo.observeUserProfile(authenticatedUid)
                    .catch { /* Handled via handleFirestoreError in repository */ }
                    .collect { doc ->
                        if (doc != null) {
                            repository.applyCloudUserProfile(doc)
                        }
                    }
            }

            launch {
                cloudRepo.observeUserWorkRequests()
                    .catch { /* Handled via handleFirestoreError in repository */ }
                    .collect { docs ->
                        repository.mergeCloudWorkRequests(docs)
                    }
            }
        }
    }

    fun logout() {
        cloudSyncJob?.cancel()
        cloudSyncJob = null
        repository.firestoreRepository?.let { cloudRepo ->
            val hasFirebaseUser = try {
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
            } catch (_: Exception) {
                false
            }
            if (hasFirebaseUser) {
                viewModelScope.launch {
                    cloudRepo.deactivateFcmToken()
                }
            }
        }
        repository.logout()
        _activeRole.value = UserRole.CUSTOMER
        _customerScreenRoute.value = "home"
        _providerScreenRoute.value = "dashboard"
        _adminScreenRoute.value = "admin"
        _selectedProvider.value = null
        _activeChatPartner.value = null
        _isAiAssistantOpen.value = false
        _registrationConfirmationMessage.value = null
    }

    // --- Admin Session Timeout (15 Minutes Inactivity Protection) ---

    fun recordAdminActivity(timestampMillis: Long = System.currentTimeMillis()) {
        if (repository.isAuthenticated.value &&
            (_activeRole.value == UserRole.ADMIN || repository.currentUser.value.role == UserRole.ADMIN)
        ) {
            _lastAdminActivityMillis.value = timestampMillis
        }
    }

    fun checkAdminSessionTimeout(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val isAdminSession = repository.isAuthenticated.value &&
            (_activeRole.value == UserRole.ADMIN || repository.currentUser.value.role == UserRole.ADMIN)
        if (!isAdminSession) return false

        val elapsed = nowMillis - _lastAdminActivityMillis.value
        if (elapsed >= ADMIN_SESSION_TIMEOUT_MS) {
            _sessionTimeoutMessage.value =
                "Session expired due to 15 minutes of inactivity. Please sign in again to protect sensitive marketplace data."
            logout()
            return true
        }
        return false
    }

    fun clearSessionTimeoutMessage() {
        _sessionTimeoutMessage.value = null
    }

    // --- Navigation & Role Actions ---

    fun switchActiveRole(newRole: UserRole) {
        if (newRole == UserRole.ADMIN) {
            val email = repository.currentUser.value.email
            if (!repository.isAuthorizedAdminEmail(email)) {
                return // Reject unauthorized privilege escalation
            }
            _adminScreenRoute.value = "admin"
            _lastAdminActivityMillis.value = System.currentTimeMillis()
        }
        recordAdminActivity()
        _activeRole.value = newRole
        _selectedProvider.value = null
        _activeChatPartner.value = null
    }

    fun setScreenRoute(route: String) {
        recordAdminActivity()
        when (_activeRole.value) {
            UserRole.CUSTOMER -> _customerScreenRoute.value = route
            UserRole.SERVICE_PROVIDER -> _providerScreenRoute.value = route
            UserRole.ADMIN -> _adminScreenRoute.value = route
        }
        if (route in listOf(
                "home", "explore", "marketplace", "bookings", "requests",
                "messages", "profile", "dashboard", "admin",
                "become_provider", "provider_verification", "subscriptions",
                "help_centre", "legal_policies", "notifications"
            )
        ) {
            _selectedProvider.value = null
            _activeChatPartner.value = null
        }
    }

    fun toggleLanguage() {
        recordAdminActivity()
        repository.toggleLanguage()
    }

    fun setLanguage(language: AppLanguage) {
        recordAdminActivity()
        repository.setLanguage(language)
    }

    fun toggleTheme() {
        recordAdminActivity()
        repository.toggleTheme()
    }

    fun setTheme(isDark: Boolean) {
        recordAdminActivity()
        repository.setTheme(isDark)
    }

    fun updateUserProfile(updated: UserProfile) {
        recordAdminActivity()
        repository.updateUserProfile(updated)
        repository.firestoreRepository?.let { cloudRepo ->
            viewModelScope.launch {
                cloudRepo.saveUserProfile(updated)
            }
        }
    }

    fun updateProfilePhoto(uri: String) {
        val current = repository.currentUser.value
        val updated = current.copy(profilePhotoUri = uri)
        updateUserProfile(updated)
    }

    fun updateCoverPhoto(uri: String) {
        val current = repository.currentUser.value
        val updated = current.copy(coverPhotoUri = uri)
        updateUserProfile(updated)
    }

    // --- Search, Discovery & Filtering ---

    fun setSearchQuery(query: String) {
        recordAdminActivity()
        _searchQuery.value = query
    }

    fun selectCategoryFilter(categoryId: String?) {
        recordAdminActivity()
        _selectedCategory.value = if (_selectedCategory.value == categoryId) null else categoryId
    }

    fun setCategoryFilterExplicit(categoryId: String?) {
        recordAdminActivity()
        _selectedCategory.value = categoryId
    }

    fun selectCityFilter(city: String?) {
        recordAdminActivity()
        _selectedCity.value = if (_selectedCity.value == city) null else city
    }

    fun setCityFilterExplicit(city: String?) {
        recordAdminActivity()
        _selectedCity.value = city
    }

    fun setServiceScopeFilter(scope: ServiceScopeFilter) {
        recordAdminActivity()
        _serviceScopeFilter.value = scope
    }

    fun toggleAvailableOnlyFilter() {
        recordAdminActivity()
        _availableOnlyFilter.value = !_availableOnlyFilter.value
    }

    fun toggleVerifiedOnlyFilter() {
        recordAdminActivity()
        _verifiedOnlyFilter.value = !_verifiedOnlyFilter.value
    }

    fun setSortOption(option: ProviderSortOption) {
        recordAdminActivity()
        _sortOption.value = option
    }

    fun clearAllDiscoveryFilters() {
        recordAdminActivity()
        _searchQuery.value = ""
        _selectedCategory.value = null
        _selectedCity.value = null
        _serviceScopeFilter.value = ServiceScopeFilter.ALL_SERVICES
        _availableOnlyFilter.value = false
        _verifiedOnlyFilter.value = false
        _sortOption.value = ProviderSortOption.RELEVANCE
    }

    fun selectProvider(provider: ServiceProvider) {
        recordAdminActivity()
        _selectedProvider.value = provider
        _recentlyViewedIds.update { current ->
            (listOf(provider.id) + current.filterNot { it == provider.id }).take(8)
        }
    }

    fun clearSelectedProvider() {
        recordAdminActivity()
        _selectedProvider.value = null
    }

    fun toggleFavouriteProvider(providerId: String) {
        recordAdminActivity()
        _favouriteIds.update { current ->
            if (providerId in current) current - providerId else current + providerId
        }
    }

    // --- Notifications & Support ---

    fun markAllNotificationsRead() {
        recordAdminActivity()
        _notifications.update { list -> list.map { it.copy(isRead = true) } }
    }

    fun addNotification(title: String, message: String, category: String = "Platform") {
        _notifications.update { current ->
            listOf(
                InAppNotification(
                    id = "notif_${System.currentTimeMillis()}",
                    title = title,
                    message = message,
                    category = category
                )
            ) + current
        }
    }

    fun submitProviderReport(providerId: String, reason: String, details: String) {
        recordAdminActivity()
        val provName = repository.getProviderById(providerId)?.name ?: "Provider"
        addNotification(
            title = "Trust & Safety Report Submitted",
            message = "Your report regarding $provName ($reason) has been logged for review by SkillConnect Management (${SkillConnectBranding.OFFICIAL_EMAIL}).",
            category = "Support"
        )
    }

    // --- Gemini AI Assistant (Firebase AI Logic) ---

    fun openAiAssistant() {
        recordAdminActivity()
        _isAiAssistantOpen.value = true
    }

    fun openAiAssistantWithPrompt(prompt: String) {
        recordAdminActivity()
        _isAiAssistantOpen.value = true
        if (prompt.isNotBlank()) {
            askGeminiAi(prompt)
        }
    }

    fun closeAiAssistant() {
        recordAdminActivity()
        _isAiAssistantOpen.value = false
    }

    fun askGeminiAi(userQuestion: String) {
        val trimmed = userQuestion.trim()
        if (trimmed.isEmpty()) return
        recordAdminActivity()

        val userMsg = AiChatMessage(
            id = "user_${System.currentTimeMillis()}",
            isFromUser = true,
            text = trimmed
        )
        _aiChatMessages.update { it + userMsg }
        _aiUiState.value = AiAssistantUiState.Loading

        val activeCity = _selectedCity.value ?: repository.currentUser.value.city.ifBlank { "Bengaluru" }
        val catalogSummary = repository.providers.value.joinToString("\n") { prov ->
            "- ${prov.name} (${prov.professionalTitle}) in ${prov.city}, ${prov.state}: ₹${prov.hourlyRateInr}/hr, Rating ${prov.rating} (${prov.reviewCount} reviews), Verified=${prov.isVerified}, Available=${prov.isAvailable}, Skills=${prov.skills.joinToString(", ")}"
        }
        val fullPrompt = """
            You are the official SkillConnect India AI Assistant with Google Maps Grounding enabled.
            Tagline: "${SkillConnectBranding.TAGLINE}"
            Official Support Email: ${SkillConnectBranding.OFFICIAL_EMAIL}
            Active User Location / City Context: $activeCity, India
            Live Verified Providers on SkillConnect:
            $catalogSummary

            Use Google Maps grounding where relevant to provide up-to-date, accurate local service area insights, commercial hubs, and location context in India alongside matching SkillConnect providers, skills, and INR (₹) rates from the live catalog above.

            User Question: $trimmed
        """.trimIndent()

        viewModelScope.launch {
            val accumulatedText = StringBuilder()
            val groundedPlaces = mutableListOf<MapsGroundedPlace>()
            val defaultMapsPlace = buildDefaultMapsGroundedPlace(trimmed, activeCity)
            try {
                withTimeout(60_000L) {
                    geminiModel.generateContentStream(fullPrompt).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        accumulatedText.append(chunkText)
                        chunk.candidates.forEach { candidate ->
                            candidate.groundingMetadata?.groundingChunks?.forEach { gChunk ->
                                val mapsChunk = gChunk.maps
                                val chunkTitle = mapsChunk?.title.orEmpty()
                                val chunkUri = mapsChunk?.uri.orEmpty()
                                val chunkPlaceId = mapsChunk?.placeId.orEmpty()
                                if (chunkTitle.isNotBlank() && chunkUri.isNotBlank()) {
                                    if (groundedPlaces.none { it.uri == chunkUri || it.title == chunkTitle }) {
                                        groundedPlaces.add(
                                            MapsGroundedPlace(
                                                title = chunkTitle,
                                                uri = chunkUri,
                                                placeId = chunkPlaceId
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        _aiUiState.value = AiAssistantUiState.Streaming(accumulatedText.toString())
                    }
                }
                val finalAnswer = accumulatedText.toString().trim()
                val finalPlaces = if (groundedPlaces.isNotEmpty()) groundedPlaces.toList() else listOf(defaultMapsPlace)
                if (finalAnswer.isNotEmpty()) {
                    _aiChatMessages.update {
                        it + AiChatMessage(
                            id = "ai_${System.currentTimeMillis()}",
                            isFromUser = false,
                            text = finalAnswer,
                            mapsPlaces = finalPlaces
                        )
                    }
                    _aiUiState.value = AiAssistantUiState.Success(finalAnswer)
                } else {
                    val fallback = buildSmartCatalogRecommendation(trimmed)
                    _aiChatMessages.update {
                        it + AiChatMessage(
                            id = "ai_fallback_${System.currentTimeMillis()}",
                            isFromUser = false,
                            text = fallback,
                            isFallback = true,
                            mapsPlaces = finalPlaces
                        )
                    }
                    _aiUiState.value = AiAssistantUiState.Error(
                        message = "Model returned an empty response.",
                        fallbackAdvice = fallback
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini AI streaming timed out", e)
                val fallback = buildSmartCatalogRecommendation(trimmed)
                _aiChatMessages.update {
                    it + AiChatMessage(
                        id = "ai_timeout_${System.currentTimeMillis()}",
                        isFromUser = false,
                        text = fallback,
                        isFallback = true,
                        mapsPlaces = listOf(defaultMapsPlace)
                    )
                }
                _aiUiState.value = AiAssistantUiState.Error(
                    message = "AI request timed out after 60 seconds. Showing live SkillConnect catalog matches below.",
                    fallbackAdvice = fallback
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI generation failed", e)
                val fallback = buildSmartCatalogRecommendation(trimmed)
                _aiChatMessages.update {
                    it + AiChatMessage(
                        id = "ai_err_${System.currentTimeMillis()}",
                        isFromUser = false,
                        text = fallback,
                        isFallback = true,
                        mapsPlaces = listOf(defaultMapsPlace)
                    )
                }
                _aiUiState.value = AiAssistantUiState.Error(
                    message = e.localizedMessage ?: "Live Gemini AI service is temporarily unreachable. Showing verified SkillConnect directory matches.",
                    fallbackAdvice = fallback
                )
            }
        }
    }

    private fun buildDefaultMapsGroundedPlace(query: String, fallbackCity: String): MapsGroundedPlace {
        val knownCities = listOf("Bengaluru", "Mumbai", "Delhi-NCR", "Delhi", "Hyderabad", "Pune", "Chennai", "Kolkata")
        val matchedCity = knownCities.firstOrNull { query.contains(it, ignoreCase = true) } ?: fallbackCity
        val encoded = matchedCity.replace(" ", "+") + "+India"
        return MapsGroundedPlace(
            title = "$matchedCity Service Hub • Google Maps",
            uri = "https://www.google.com/maps/search/?api=1&query=$encoded"
        )
    }

    private fun buildSmartCatalogRecommendation(query: String): String {
        val providers = repository.providers.value
        val keywords = query.lowercase().split(" ", ",", ".", "?", "!").filter { it.length >= 3 }
        val matches = providers.filter { prov ->
            keywords.any { kw ->
                prov.name.lowercase().contains(kw) ||
                    prov.professionalTitle.lowercase().contains(kw) ||
                    prov.city.lowercase().contains(kw) ||
                    prov.skills.any { it.lowercase().contains(kw) } ||
                    prov.subcategories.any { it.lowercase().contains(kw) }
            }
        }.ifEmpty {
            providers.sortedByDescending { it.rating }.take(3)
        }

        val recommendations = matches.take(3).joinToString("\n") { prov ->
            "• ${prov.name} — ${prov.professionalTitle} (${prov.city}) • ₹${prov.hourlyRateInr}/hr • ★ ${prov.rating}"
        }
        return "Here are top verified SkillConnect specialists matching your inquiry:\n$recommendations\n\nTap any provider card in Explore to view their portfolio, message them directly, or book with 0% platform commission."
    }

    // --- Chat & Work Request Actions ---

    fun openChatWith(partnerId: String, partnerName: String) {
        recordAdminActivity()
        _activeChatPartner.value = Pair(partnerId, partnerName)
    }

    fun closeChat() {
        recordAdminActivity()
        _activeChatPartner.value = null
    }

    fun sendMessage(text: String) {
        recordAdminActivity()
        val partner = _activeChatPartner.value ?: return
        val convId = "conv_${partner.first}"
        repository.sendMessage(convId, text, partner.first)
    }

    fun openCreateWorkRequestDialog(provider: ServiceProvider) {
        recordAdminActivity()
        _selectedProvider.value = provider
        _isCreateRequestOpen.value = true
    }

    fun closeCreateWorkRequestDialog() {
        recordAdminActivity()
        _isCreateRequestOpen.value = false
    }

    fun submitWorkRequest(
        title: String,
        description: String,
        category: String,
        budgetInr: Long,
        deadline: String
    ) {
        recordAdminActivity()
        val provider = _selectedProvider.value ?: return
        val customer = repository.currentUser.value
        repository.createWorkRequest(
            providerId = provider.id,
            title = title,
            description = description,
            category = category,
            budgetInr = budgetInr,
            deadline = deadline
        )
        repository.firestoreRepository?.let { cloudRepo ->
            viewModelScope.launch {
                cloudRepo.createWorkRequest(
                    customerName = customer.name,
                    providerId = provider.id,
                    providerName = provider.name,
                    title = title,
                    description = description,
                    category = category,
                    budgetInr = budgetInr,
                    deadline = deadline
                )
            }
        }
        addNotification(
            title = "Booking Request Sent",
            message = "Your booking '$title' with ${provider.name} (₹$budgetInr) has been submitted.",
            category = "Bookings"
        )
        _isCreateRequestOpen.value = false
        setScreenRoute("requests")
        _selectedProvider.value = null
    }

    fun updateRequestStatus(requestId: String, status: WorkRequestStatus, note: String = "") {
        recordAdminActivity()
        repository.updateWorkRequestStatus(requestId, status, note)
        addNotification(
            title = "Booking Status Updated",
            message = "Booking #$requestId is now marked as ${status.labelEn}.",
            category = "Bookings"
        )
    }

    fun cancelBooking(requestId: String) {
        recordAdminActivity()
        repository.updateWorkRequestStatus(requestId, WorkRequestStatus.CANCELLED, "Cancelled by user")
        addNotification(
            title = "Booking Cancelled",
            message = "Booking #$requestId has been cancelled.",
            category = "Bookings"
        )
    }

    fun openReviewDialog(workRequestId: String, providerId: String) {
        recordAdminActivity()
        _reviewTargetReqId.value = workRequestId
        val prov = repository.getProviderById(providerId)
        _selectedProvider.value = prov
        _isReviewOpen.value = true
    }

    fun closeReviewDialog() {
        recordAdminActivity()
        _isReviewOpen.value = false
        _reviewTargetReqId.value = null
    }

    fun submitReview(rating: Int, comment: String) {
        recordAdminActivity()
        val prov = _selectedProvider.value ?: return
        val reqId = _reviewTargetReqId.value ?: return
        repository.submitReview(prov.id, rating, comment, reqId)
        addNotification(
            title = "Review Published",
            message = "Your $rating★ review for ${prov.name} is now live on SkillConnect.",
            category = "Reviews"
        )
        _isReviewOpen.value = false
        _reviewTargetReqId.value = null
    }

    // --- Admin Actions ---

    fun toggleProviderVerification(providerId: String, verified: Boolean) {
        recordAdminActivity()
        repository.toggleProviderVerification(providerId, verified)
    }

    fun addNewCategory(category: ServiceCategory) {
        recordAdminActivity()
        repository.addServiceCategory(category)
    }

    fun broadcastAnnouncement(message: String) {
        recordAdminActivity()
        repository.broadcastAnnouncement(message)
        addNotification(
            title = "Platform Announcement",
            message = message,
            category = "Announcement"
        )
    }

    fun toggleProviderAvailability(providerId: String) {
        recordAdminActivity()
        repository.toggleProviderAvailability(providerId)
    }

    // --- Multi-Level RBAC Admin Actions ---

    fun provisionOfficialAdminAccounts(
        founderPass: String,
        ceoPass: String,
        adminPass: String
    ): Result<String> {
        recordAdminActivity()
        val res = repository.provisionOfficialAdminAccounts(founderPass, ceoPass, adminPass)
        if (res.isSuccess) {
            addNotification(
                title = "Official Admin Provisioning",
                message = "Founder, CEO, and Admin credentials have been initialized with verified RBAC claims.",
                category = "Security"
            )
        }
        return res
    }

    fun updateAdminPermissions(targetUid: String, newPermissions: AdminPermissions): Result<Unit> {
        recordAdminActivity()
        val callerRole = repository.currentUser.value.adminRole ?: AdminRoleType.ADMIN
        val res = repository.updateAdminPermissions(targetUid, newPermissions, callerRole)
        if (res.isSuccess) {
            addNotification(
                title = "Permissions Updated",
                message = "Updated permissions for administrator $targetUid.",
                category = "Security"
            )
        }
        return res
    }

    fun revokeAdmin(targetUid: String): Result<Unit> {
        recordAdminActivity()
        val callerRole = repository.currentUser.value.adminRole ?: AdminRoleType.ADMIN
        val res = repository.revokeAdminAccount(targetUid, callerRole)
        if (res.isSuccess) {
            addNotification(
                title = "Administrator Access Revoked",
                message = "Revoked administrator access for $targetUid.",
                category = "Security"
            )
        }
        return res
    }

    fun toggleAdminActive(targetUid: String): Result<Unit> {
        recordAdminActivity()
        val callerRole = repository.currentUser.value.adminRole ?: AdminRoleType.ADMIN
        return repository.toggleAdminActive(targetUid, callerRole)
    }

    fun resolveComplaint(complaintId: String, newStatus: ComplaintStatus, resolutionNotes: String): Result<Unit> {
        recordAdminActivity()
        val callerRole = repository.currentUser.value.adminRole ?: AdminRoleType.ADMIN
        val res = repository.resolveComplaint(complaintId, newStatus, resolutionNotes, callerRole)
        if (res.isSuccess) {
            addNotification(
                title = "Complaint #${complaintId.takeLast(4)} Updated",
                message = "Status updated to ${newStatus.label}. Note: $resolutionNotes",
                category = "Compliance"
            )
        }
        return res
    }

    fun deleteUserOrProvider(id: String, isProvider: Boolean, reason: String): Result<Unit> {
        recordAdminActivity()
        val callerRole = repository.currentUser.value.adminRole ?: AdminRoleType.ADMIN
        val res = repository.deleteUserOrProvider(id, isProvider, reason, callerRole)
        if (res.isSuccess) {
            addNotification(
                title = if (isProvider) "Provider Account Deleted" else "User Account Deleted",
                message = "Action executed by ${callerRole.name}. Reason: $reason",
                category = "Moderation"
            )
        }
        return res
    }

    fun recordAdminAuditLog(action: String, targetEntityId: String, details: String) {
        recordAdminActivity()
        repository.recordAuditLog(action, targetEntityId, details)
    }
}
