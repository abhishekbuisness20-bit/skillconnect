package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.AdminAccount
import com.example.data.model.AdminAuditLog
import com.example.data.model.AppNotification
import com.example.data.model.BookingRequest
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.FavouriteItem
import com.example.data.model.JobOpportunityItem
import com.example.data.model.PaymentDispute
import com.example.data.model.PaymentRecord
import com.example.data.model.PlatformSetting
import com.example.data.model.SafetyReport
import com.example.data.model.ServiceListing
import com.example.data.model.ServiceReview
import com.example.data.model.SkillCourseItem
import com.example.data.model.SupportTicket
import com.example.data.model.UserProfile
import com.example.data.model.VerificationSubmission
import com.example.data.repository.SkillConnectRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AppDestination {
    HOME,
    EXPLORE,
    PROVIDER_DIRECTORY,
    SERVICE_DETAIL,
    PROVIDER_PROFILE,
    PROVIDER_ONBOARDING_WIZARD,
    CUSTOMER_DASHBOARD,
    PROVIDER_DASHBOARD,
    CREATE_EDIT_SERVICE,
    BOOKINGS,
    MESSAGES,
    FAVOURITES,
    PROFILE_SETTINGS,
    ADMIN_DASHBOARD,
    PROVIDER_VERIFICATION,
    SUBSCRIPTIONS,
    HELP_AND_LEGAL
}

data class ExploreFilterState(
    val keyword: String = "",
    val selectedCategory: String = "All",
    val selectedCity: String = "All India",
    val selectedLocality: String = "",
    val maxPriceInr: Long = 100000L,
    val minRating: Double = 0.0,
    val verifiedOnly: Boolean = false,
    val availabilityQuery: String = "",
    val pricingModelFilter: String = "All", // "All", "fixed", "hourly", "quote"
    val sortBy: String = "relevance", // "relevance", "newest", "price_low", "price_high"
    val isGridView: Boolean = false,
    val visibleCount: Int = 10
)

class SkillConnectViewModel(
    private val repository: SkillConnectRepository,
    private val preferencesRepository: UserPreferencesRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserDisplayName: String
) : ViewModel() {

    private val isLegacyFounderEmail: Boolean =
        currentUserEmail.equals("abhishekbuisness20@gmail.com", ignoreCase = true)

    private var initialRoleRoutingDone = false
    private var statusDismissJob: Job? = null
    private var errorDismissJob: Job? = null

    // Navigation & Backstack
    private val _navigationStack = MutableStateFlow(listOf(AppDestination.HOME))
    private val _currentDestination = MutableStateFlow(AppDestination.HOME)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    // Selected entities for detail screens
    private val _selectedServiceId = MutableStateFlow<String?>(null)
    val selectedServiceId: StateFlow<String?> = _selectedServiceId.asStateFlow()

    private val _selectedProviderId = MutableStateFlow<String?>(null)
    val selectedProviderId: StateFlow<String?> = _selectedProviderId.asStateFlow()

    private val _editingService = MutableStateFlow<ServiceListing?>(null)
    val editingService: StateFlow<ServiceListing?> = _editingService.asStateFlow()

    private val _activeConversation = MutableStateFlow<Conversation?>(null)
    val activeConversation: StateFlow<Conversation?> = _activeConversation.asStateFlow()

    // Compare services selection
    private val _comparedServiceIds = MutableStateFlow<Set<String>>(emptySet())
    val comparedServiceIds: StateFlow<Set<String>> = _comparedServiceIds.asStateFlow()

    // Status feedback banner (standardized 2-second auto-dismissing toast)
    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    // Explore Filter State
    private val _filterState = MutableStateFlow(ExploreFilterState())
    val filterState: StateFlow<ExploreFilterState> = _filterState.asStateFlow()

    // Preferences stream
    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserPreferences())

    // Executive Admin Account stream (/admins/{currentUserId})
    val myAdminAccountState: StateFlow<AdminAccount?> = repository.observeMyAdminAccount(currentUserId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    // Active Executive Role ("cofounder", "ceo", "founder", or "")
    val activeExecutiveRole: StateFlow<String> = combine(
        myAdminAccountState,
        userPreferences
    ) { adminDoc, prefs ->
        when {
            adminDoc != null && adminDoc.adminRole in listOf("cofounder", "ceo", "founder") -> adminDoc.adminRole
            prefs.activeAdminRole in listOf("cofounder", "ceo", "founder") -> prefs.activeAdminRole
            isLegacyFounderEmail -> "founder"
            else -> ""
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        if (isLegacyFounderEmail) "founder" else ""
    )

    val isAdminUser: Boolean
        get() = isLegacyFounderEmail ||
            activeExecutiveRole.value.isNotBlank() ||
            myAdminAccountState.value?.adminRole?.isNotBlank() == true ||
            userPreferences.value.activeAdminRole.isNotBlank()

    // 1. My Profile
    val myProfileState: StateFlow<UiState<UserProfile?>> = repository.observeMyProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing profile", e)
            emit(UiState.Error(e.message ?: "Failed to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    // 2. Public Providers
    val publicProvidersState: StateFlow<UiState<List<UserProfile>>> = repository.observePublicProviders()
        .map<List<UserProfile>, UiState<List<UserProfile>>> { list ->
            // Hide drafts and suspended listings from public directory per Section 7 rules
            UiState.Success(
                list.filter {
                    it.accountStatus == "active" &&
                        it.verificationStatus !in listOf("draft", "suspended") &&
                        it.publicListingConsent
                }
            )
        }
        .catch { e ->
            Log.w(TAG, "Error observing providers", e)
            emit(UiState.Error(e.message ?: "Failed to load providers"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    // 3. Published Services & My Provider Services
    val publishedServicesState: StateFlow<UiState<List<ServiceListing>>> = repository.observePublishedServices()
        .map<List<ServiceListing>, UiState<List<ServiceListing>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing published services", e)
            emit(UiState.Error(e.message ?: "Failed to load services"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myProviderServicesState: StateFlow<UiState<List<ServiceListing>>> = repository.observeProviderServices(currentUserId)
        .map<List<ServiceListing>, UiState<List<ServiceListing>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing my services", e)
            emit(UiState.Error(e.message ?: "Failed to load provider listings"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    // Filtered & Sorted Services for Explore Screen
    val filteredServices: StateFlow<List<ServiceListing>> = combine(
        publishedServicesState,
        _filterState
    ) { state, filter ->
        val raw = (state as? UiState.Success)?.data.orEmpty()
        val matched = raw.filter { s ->
            val kw = filter.keyword.trim().lowercase()
            val matchesKeyword = kw.isEmpty() ||
                s.title.lowercase().contains(kw) ||
                s.shortSummary.lowercase().contains(kw) ||
                s.description.lowercase().contains(kw) ||
                s.providerName.lowercase().contains(kw) ||
                s.category.lowercase().contains(kw) ||
                s.skills.any { it.lowercase().contains(kw) }

            val matchesCategory = filter.selectedCategory == "All" ||
                s.category.equals(filter.selectedCategory, ignoreCase = true)

            val matchesCity = filter.selectedCity == "All India" ||
                s.city.equals("All India", ignoreCase = true) ||
                s.city.equals(filter.selectedCity, ignoreCase = true) ||
                s.city.contains(filter.selectedCity, ignoreCase = true) ||
                filter.selectedCity.contains(s.city, ignoreCase = true)

            val matchesLocality = filter.selectedLocality.isBlank() ||
                filter.selectedLocality == "All Localities" ||
                s.locality.contains(filter.selectedLocality.trim(), ignoreCase = true)

            val matchesPrice = s.pricingModel == "quote" || s.startingPrice <= filter.maxPriceInr

            val matchesRating = filter.minRating <= 0.0 || s.averageRating >= filter.minRating

            val matchesVerified = !filter.verifiedOnly || s.providerVerified

            val matchesPricingModel = filter.pricingModelFilter == "All" ||
                s.pricingModel.equals(filter.pricingModelFilter, ignoreCase = true)

            val matchesAvailability = filter.availabilityQuery.isBlank() ||
                s.availability.contains(filter.availabilityQuery.trim(), ignoreCase = true)

            matchesKeyword && matchesCategory && matchesCity && matchesLocality &&
                matchesPrice && matchesRating && matchesVerified && matchesPricingModel && matchesAvailability
        }

        val sorted = when (filter.sortBy) {
            "newest" -> matched.sortedByDescending { it.createdAt?.seconds ?: 0L }
            "price_low" -> matched.sortedBy { it.startingPrice }
            "price_high" -> matched.sortedByDescending { it.startingPrice }
            else -> matched.sortedWith(
                compareByDescending<ServiceListing> { it.isFeatured }
                    .thenByDescending { it.providerVerified }
                    .thenByDescending { it.averageRating }
            )
        }
        sorted.take(filter.visibleCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // 4. Customer & Provider Bookings
    val customerBookingsState: StateFlow<UiState<List<BookingRequest>>> = repository.observeCustomerBookings(currentUserId)
        .map<List<BookingRequest>, UiState<List<BookingRequest>>> { list ->
            UiState.Success(list.sortedByDescending { it.createdAt?.seconds ?: 0L })
        }
        .catch { e ->
            Log.w(TAG, "Error observing customer bookings", e)
            emit(UiState.Error(e.message ?: "Failed to load customer bookings"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val providerBookingsState: StateFlow<UiState<List<BookingRequest>>> = repository.observeProviderBookings(currentUserId)
        .map<List<BookingRequest>, UiState<List<BookingRequest>>> { list ->
            UiState.Success(list.sortedByDescending { it.createdAt?.seconds ?: 0L })
        }
        .catch { e ->
            Log.w(TAG, "Error observing provider bookings", e)
            emit(UiState.Error(e.message ?: "Failed to load provider bookings"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    // 5. Payments, Payouts & Disputes Ledger
    val myPaymentsState: StateFlow<List<PaymentRecord>> = combine(
        repository.observeUserPayments(currentUserId).catch { emit(emptyList()) },
        repository.observeProviderPayments(currentUserId).catch { emit(emptyList()) }
    ) { userPays, provPays ->
        (userPays + provPays).distinctBy { it.id }.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myDisputesState: StateFlow<List<PaymentDispute>> = combine(
        repository.observeCustomerDisputes(currentUserId).catch { emit(emptyList()) },
        repository.observeProviderDisputes(currentUserId).catch { emit(emptyList()) }
    ) { custDisputes, provDisputes ->
        (custDisputes + provDisputes).distinctBy { it.id }.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // 6. Conversations & Messages
    val conversationsState: StateFlow<UiState<List<Conversation>>> = combine(
        repository.observeCustomerConversations(currentUserId),
        repository.observeProviderConversations(currentUserId)
    ) { custConvs, provConvs ->
        val merged = (custConvs + provConvs)
            .distinctBy { it.id }
            .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
        UiState.Success(merged) as UiState<List<Conversation>>
    }.catch { e ->
        Log.w(TAG, "Error observing conversations", e)
        emit(UiState.Error(e.message ?: "Failed to load conversations"))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessagesState: StateFlow<List<ChatMessage>> = _activeConversation
        .flatMapLatest { conv ->
            if (conv == null) {
                flowOf(emptyList())
            } else {
                val isCustomer = conv.customerId == currentUserId
                repository.observeMessages(conv.id, currentUserId, isCustomer)
                    .map { list -> list.sortedBy { it.createdAt?.seconds ?: 0L } }
                    .catch { emit(emptyList()) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // 7. Notifications
    val notificationsState: StateFlow<List<AppNotification>> = repository.observeNotifications(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // 8. Reviews
    val publishedReviewsState: StateFlow<List<ServiceReview>> = repository.observePublishedReviews()
        .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // 9. Favourites
    val favouritesState: StateFlow<List<FavouriteItem>> = repository.observeFavourites(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // 10. Provider Verification
    val myVerificationState: StateFlow<VerificationSubmission?> = repository.observeMyVerification(currentUserId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    // 11. Reports & Support Tickets
    val myReportsState: StateFlow<List<SafetyReport>> = repository.observeMyReports(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val mySupportTicketsState: StateFlow<List<SupportTicket>> = repository.observeMySupportTickets(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // 12. Global Platform Settings
    val globalSettingsState: StateFlow<PlatformSetting?> = repository.observeGlobalSettings()
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    // 13. Role-Gated Admin Streams (reactively scoped to Co-Founder, CEO, and Founder permissions)
    @OptIn(ExperimentalCoroutinesApi::class)
    val adminAllProfilesState: StateFlow<List<UserProfile>> = activeExecutiveRole
        .flatMapLatest { role ->
            if (role in listOf("cofounder", "ceo", "founder")) {
                repository.observeAllProfilesForAdmin().catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminAllVerificationsState: StateFlow<List<VerificationSubmission>> = activeExecutiveRole
        .flatMapLatest { role ->
            if (role in listOf("cofounder", "ceo", "founder")) {
                repository.observeAllVerificationsForAdmin().catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminAllBookingsState: StateFlow<List<BookingRequest>> = activeExecutiveRole
        .flatMapLatest { role ->
            // Co-Founder does not read global booking ledger; only CEO & Founder
            if (role in listOf("ceo", "founder")) {
                repository.observeAllBookingsForAdmin().catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminAllPaymentsState: StateFlow<List<PaymentRecord>> = activeExecutiveRole
        .flatMapLatest { role ->
            // Co-Founder has no access to payment ledger; only CEO & Founder
            if (role in listOf("ceo", "founder")) {
                repository.observeAllPaymentsForAdmin().catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminAllDisputesState: StateFlow<List<PaymentDispute>> = activeExecutiveRole
        .flatMapLatest { role ->
            if (role in listOf("cofounder", "ceo", "founder")) {
                repository.observeAllDisputesForAdmin()
                    .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }
                    .catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminAllReportsState: StateFlow<List<SafetyReport>> = activeExecutiveRole
        .flatMapLatest { role ->
            if (role in listOf("cofounder", "ceo", "founder")) {
                repository.observeAllReportsForAdmin().catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminAllTicketsState: StateFlow<List<SupportTicket>> = activeExecutiveRole
        .flatMapLatest { role ->
            if (role in listOf("cofounder", "ceo", "founder")) {
                repository.observeAllTicketsForAdmin().catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminAuditLogsState: StateFlow<List<AdminAuditLog>> = activeExecutiveRole
        .flatMapLatest { role ->
            if (role in listOf("cofounder", "ceo", "founder")) {
                repository.observeAdminAuditLogs()
                    .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }
                    .catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allAdminsState: StateFlow<List<AdminAccount>> = activeExecutiveRole
        .flatMapLatest { role ->
            if (role == "founder") {
                repository.observeAllAdminsForFounder().catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Role-Aware Route Guard Navigation
    fun navigateTo(destination: AppDestination) {
        val myProfile = (myProfileState.value as? UiState.Success)?.data
        val activeRole = myProfile?.role ?: "customer"

        if (destination == AppDestination.ADMIN_DASHBOARD && !isAdminUser) {
            showError("Access Restricted: Executive Administrator privileges required.")
            return
        }

        if (!isAdminUser && activeRole == "customer" && destination in setOf(
                AppDestination.PROVIDER_DASHBOARD,
                AppDestination.PROVIDER_ONBOARDING_WIZARD,
                AppDestination.CREATE_EDIT_SERVICE,
                AppDestination.PROVIDER_VERIFICATION
            )
        ) {
            showError("Customer accounts cannot access Provider tools. Submit a Role Change request in Profile Settings.")
            return
        }

        if (!isAdminUser && activeRole == "provider" && destination == AppDestination.CUSTOMER_DASHBOARD) {
            // Providers always use Provider Dashboard, never Customer Dashboard
            val target = AppDestination.PROVIDER_DASHBOARD
            if (_currentDestination.value != target) {
                _navigationStack.update { (it + target).takeLast(12) }
                _currentDestination.value = target
            }
            return
        }

        if (_currentDestination.value != destination) {
            _navigationStack.update { (it + destination).takeLast(12) }
            _currentDestination.value = destination
        }
    }

    fun navigateBack(): Boolean {
        val myProfile = (myProfileState.value as? UiState.Success)?.data
        val defaultDest = if (myProfile?.role == "provider") AppDestination.PROVIDER_DASHBOARD else AppDestination.HOME
        val stack = _navigationStack.value
        return if (stack.size > 1) {
            val newStack = stack.dropLast(1)
            _navigationStack.value = newStack
            _currentDestination.value = newStack.last()
            true
        } else if (_currentDestination.value != defaultDest) {
            _navigationStack.value = listOf(defaultDest)
            _currentDestination.value = defaultDest
            true
        } else {
            false
        }
    }

    fun openServiceDetail(serviceId: String) {
        _selectedServiceId.value = serviceId
        viewModelScope.launch {
            preferencesRepository.addRecentlyViewedService(serviceId)
        }
        navigateTo(AppDestination.SERVICE_DETAIL)
    }

    fun openProviderProfile(providerId: String) {
        _selectedProviderId.value = providerId
        navigateTo(AppDestination.PROVIDER_PROFILE)
    }

    fun openCreateOrEditService(serviceToEdit: ServiceListing? = null) {
        _editingService.value = serviceToEdit
        navigateTo(AppDestination.CREATE_EDIT_SERVICE)
    }

    fun openConversationWithProvider(
        customerId: String,
        customerName: String,
        providerId: String,
        providerName: String,
        bookingId: String = "",
        serviceTitle: String = ""
    ) {
        if (customerId == providerId) {
            showError("You cannot start a conversation with your own profile.")
            return
        }
        viewModelScope.launch {
            repository.getOrCreateConversation(
                customerId = customerId,
                customerName = customerName,
                providerId = providerId,
                providerName = providerName,
                bookingId = bookingId,
                serviceTitle = serviceTitle
            ).onSuccess { convId ->
                val conv = Conversation(
                    id = convId,
                    customerId = customerId,
                    customerName = customerName,
                    providerId = providerId,
                    providerName = providerName,
                    bookingId = bookingId,
                    serviceTitle = serviceTitle
                )
                _activeConversation.value = conv
                navigateTo(AppDestination.MESSAGES)
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Could not open conversation")
            }
        }
    }

    fun selectConversation(conversation: Conversation?) {
        _activeConversation.value = conversation
    }

    fun toggleBlockConversation(conversation: Conversation) {
        viewModelScope.launch {
            val nextBlocked = !conversation.isBlocked
            repository.toggleConversationBlock(conversation.id, nextBlocked)
                .onSuccess {
                    _activeConversation.value = conversation.copy(isBlocked = nextBlocked)
                    showStatus(if (nextBlocked) "Conversation blocked." else "Conversation unblocked.")
                }
                .onFailure { e -> showError(e.localizedMessage ?: "Failed to update conversation") }
        }
    }

    fun clearBanners() {
        statusDismissJob?.cancel()
        errorDismissJob?.cancel()
        _statusBanner.value = null
        _errorBanner.value = null
    }

    // Standardized 2-Second Auto-Dismissing Toast Notifications (Section J)
    fun showStatus(msg: String) {
        if (msg.isBlank()) return
        errorDismissJob?.cancel()
        _errorBanner.value = null
        if (_statusBanner.value == msg && statusDismissJob?.isActive == true) return
        _statusBanner.value = msg
        statusDismissJob?.cancel()
        statusDismissJob = viewModelScope.launch {
            delay(TOAST_DURATION_MS)
            if (_statusBanner.value == msg) {
                _statusBanner.value = null
            }
        }
    }

    fun showError(msg: String) {
        if (msg.isBlank()) return
        statusDismissJob?.cancel()
        _statusBanner.value = null
        if (_errorBanner.value == msg && errorDismissJob?.isActive == true) return
        _errorBanner.value = msg
        errorDismissJob?.cancel()
        errorDismissJob = viewModelScope.launch {
            delay(TOAST_DURATION_MS)
            if (_errorBanner.value == msg) {
                _errorBanner.value = null
            }
        }
    }

    // Filter updates
    fun updateFilter(transform: (ExploreFilterState) -> ExploreFilterState) {
        _filterState.update(transform)
    }

    fun clearAllFilters() {
        _filterState.value = ExploreFilterState(isGridView = _filterState.value.isGridView)
    }

    fun loadMoreServices() {
        _filterState.update { it.copy(visibleCount = it.visibleCount + 10) }
    }

    fun toggleCompareService(serviceId: String) {
        _comparedServiceIds.update { current ->
            if (serviceId in current) current - serviceId
            else if (current.size >= 3) current
            else current + serviceId
        }
    }

    // --- Trusted Role Initialization & Routing ---
    fun ensureProfileInitialized(
        requestedRole: String,
        city: String,
        locality: String,
        preferredLanguage: String
    ) {
        viewModelScope.launch {
            val existing = repository.getProfile(currentUserId).getOrNull()
            val targetCity = city.takeIf { it != "All India" && it.isNotBlank() } ?: "Bengaluru"

            if (existing == null) {
                val chosenRole = if (requestedRole == "provider") "provider" else "customer"
                repository.createInitialProfile(
                    displayName = currentUserDisplayName.ifBlank { "SkillConnect Member" },
                    role = chosenRole,
                    city = targetCity,
                    locality = locality,
                    preferredLanguage = preferredLanguage,
                    headline = if (chosenRole == "provider") "Local Professional in $targetCity" else "SkillConnect Customer",
                    bio = "Connected on SkillConnect India ($targetCity).",
                    onboardingCompleted = (chosenRole == "customer")
                ).onSuccess { created ->
                    repository.sendNotification(
                        recipientUserId = currentUserId,
                        title = "Welcome to SkillConnect (${created.effectivePlatformId})",
                        body = if (chosenRole == "provider") {
                            "Complete your 8-step Provider Onboarding Wizard to submit your profile for verification."
                        } else {
                            "Your Customer account (${created.effectivePlatformId}) is ready. Explore verified local services!"
                        },
                        type = "registration_complete",
                        relatedId = currentUserId
                    )
                    preferencesRepository.setPendingRegistrationRole("")
                    initialRoleRoutingDone = true
                    if (chosenRole == "provider") {
                        _navigationStack.value = listOf(AppDestination.PROVIDER_ONBOARDING_WIZARD)
                        _currentDestination.value = AppDestination.PROVIDER_ONBOARDING_WIZARD
                    } else {
                        _navigationStack.value = listOf(AppDestination.CUSTOMER_DASHBOARD)
                        _currentDestination.value = AppDestination.CUSTOMER_DASHBOARD
                    }
                }
            } else {
                // Ensure legacy profile gets a persistent platformId if missing
                if (existing.platformId.isBlank()) {
                    repository.updateProfile(existing.copy(platformId = existing.effectivePlatformId))
                }
                // Detect role mismatch if user clicked a different registration card for an already-registered account
                if (requestedRole.isNotBlank() && requestedRole != existing.role) {
                    showError(
                        "Account ${existing.effectivePlatformId} is already registered as a ${existing.role.uppercase()}. " +
                            "Roles cannot be silently changed—submit a Role Change request in Profile Settings if needed."
                    )
                }
                preferencesRepository.setPendingRegistrationRole("")

                if (!initialRoleRoutingDone) {
                    initialRoleRoutingDone = true
                    if (existing.role == "provider") {
                        val dest = if (!existing.onboardingCompleted && existing.verificationStatus == "draft") {
                            AppDestination.PROVIDER_ONBOARDING_WIZARD
                        } else {
                            AppDestination.PROVIDER_DASHBOARD
                        }
                        _navigationStack.value = listOf(dest)
                        _currentDestination.value = dest
                    } else {
                        _navigationStack.value = listOf(AppDestination.CUSTOMER_DASHBOARD)
                        _currentDestination.value = AppDestination.CUSTOMER_DASHBOARD
                    }
                }
            }
            preferencesRepository.setLocation(targetCity, locality)
            preferencesRepository.setLanguage(preferredLanguage)
        }
    }

    // --- 8-Step Provider Onboarding Wizard Actions ---

    fun saveProviderWizardDraft(
        updatedProfile: UserProfile,
        step: Int,
        resumeLater: Boolean = false
    ) {
        viewModelScope.launch {
            val toSave = updatedProfile.copy(
                role = "provider",
                onboardingStep = step.coerceIn(1, 8).toLong()
            )
            repository.updateProfile(toSave)
                .onSuccess {
                    if (resumeLater) {
                        repository.sendNotification(
                            recipientUserId = currentUserId,
                            title = "Onboarding Draft Saved (Step $step/8)",
                            body = "Your Provider Onboarding progress has been saved. Resume anytime from your Provider Dashboard.",
                            type = "profile_saved",
                            relatedId = currentUserId
                        )
                        showStatus("Draft saved at Step $step of 8. You can resume onboarding anytime.")
                        _navigationStack.value = listOf(AppDestination.PROVIDER_DASHBOARD)
                        _currentDestination.value = AppDestination.PROVIDER_DASHBOARD
                    } else {
                        showStatus("Step $step saved.")
                    }
                }
                .onFailure { e ->
                    showError(e.localizedMessage ?: "Failed to save onboarding draft")
                }
        }
    }

    fun submitProviderOnboardingWizard(
        updatedProfile: UserProfile,
        legalName: String,
        documentReferenceNote: String,
        firstServiceTitle: String,
        firstServiceDesc: String,
        firstServiceDeliverables: String,
        firstServiceTurnaround: String
    ) {
        viewModelScope.launch {
            val finalizedProfile = updatedProfile.copy(
                role = "provider",
                onboardingStep = 8L,
                onboardingCompleted = true,
                verificationStatus = "pending_review",
                isPublicProvider = updatedProfile.publicListingConsent
            )
            repository.updateProfile(finalizedProfile)
                .onSuccess {
                    // Create initial service listing if provided during Step 3
                    val existingServices = (myProviderServicesState.value as? UiState.Success)?.data.orEmpty()
                    if (firstServiceTitle.trim().length >= 3 && existingServices.isEmpty()) {
                        repository.createService(
                            providerName = finalizedProfile.displayName.ifBlank { currentUserDisplayName },
                            providerVerified = false,
                            title = firstServiceTitle.trim(),
                            category = finalizedProfile.primaryCategory.ifBlank { "Graphic Design" },
                            subcategory = finalizedProfile.subcategories.take(60),
                            shortSummary = finalizedProfile.headline.ifBlank { "Professional ${finalizedProfile.primaryCategory} service in ${finalizedProfile.city}" },
                            description = firstServiceDesc.trim().ifBlank { finalizedProfile.bio.ifBlank { "Trusted local service provider on SkillConnect." } },
                            skills = finalizedProfile.skills,
                            startingPrice = finalizedProfile.startingPrice,
                            pricingModel = finalizedProfile.pricingModel,
                            deliverables = firstServiceDeliverables.ifBlank { "Standard professional deliverables as agreed" },
                            turnaround = firstServiceTurnaround.ifBlank { "2-3 Days" },
                            city = finalizedProfile.city,
                            locality = finalizedProfile.locality,
                            availability = finalizedProfile.availability,
                            terms = "Standard SkillConnect booking policy",
                            isPublished = true
                        )
                    }

                    repository.submitProviderVerification(
                        legalName = legalName.ifBlank { finalizedProfile.displayName },
                        businessName = finalizedProfile.businessName,
                        category = finalizedProfile.primaryCategory,
                        city = finalizedProfile.city,
                        experienceYears = finalizedProfile.experienceYears,
                        portfolioSummary = finalizedProfile.bio.ifBlank { "Completed 8-step Provider Onboarding." },
                        documentReferenceNote = documentReferenceNote
                    ).onSuccess {
                        repository.sendNotification(
                            recipientUserId = currentUserId,
                            title = "Verification Submitted (${finalizedProfile.effectivePlatformId})",
                            body = "Your provider profile is now Pending Review by SkillConnect Administrators.",
                            type = "verification_update",
                            relatedId = currentUserId
                        )
                        showStatus("Profile submitted for Admin Verification! Status: Pending Review (${finalizedProfile.effectivePlatformId}).")
                        _navigationStack.value = listOf(AppDestination.PROVIDER_DASHBOARD)
                        _currentDestination.value = AppDestination.PROVIDER_DASHBOARD
                    }.onFailure { e ->
                        showError(e.localizedMessage ?: "Failed to submit verification request")
                    }
                }
                .onFailure { e ->
                    showError(e.localizedMessage ?: "Failed to save provider profile")
                }
        }
    }

    fun saveUserProfile(updated: UserProfile) {
        viewModelScope.launch {
            val existing = (myProfileState.value as? UiState.Success)?.data
            val res = if (existing == null) {
                repository.createInitialProfile(
                    displayName = updated.displayName,
                    role = updated.role,
                    city = updated.city,
                    locality = updated.locality,
                    preferredLanguage = updated.preferredLanguage,
                    headline = updated.headline,
                    bio = updated.bio,
                    skills = updated.skills,
                    startingPrice = updated.startingPrice
                ).map { }
            } else {
                // Keep existing role immutable on user self-updates
                repository.updateProfile(updated.copy(role = existing.role))
            }
            res.onSuccess {
                preferencesRepository.setLocation(updated.city, updated.locality)
                preferencesRepository.setLanguage(updated.preferredLanguage)
                repository.sendNotification(
                    recipientUserId = currentUserId,
                    title = "Profile Saved",
                    body = "Your profile details (${updated.effectivePlatformId}) were updated.",
                    type = "profile_saved",
                    relatedId = currentUserId
                )
                showStatus("Profile and preferences saved.")
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Failed to update profile")
            }
        }
    }

    fun requestRoleChangeTicket(currentRole: String, targetRole: String, reason: String) {
        viewModelScope.launch {
            val myProfile = (myProfileState.value as? UiState.Success)?.data
            val platformId = myProfile?.effectivePlatformId ?: currentUserId
            repository.submitSupportTicket(
                name = myProfile?.displayName ?: currentUserDisplayName,
                contactEmail = currentUserEmail,
                category = "Role Change Request ($currentRole -> $targetRole)",
                relatedBookingId = platformId,
                description = "Account ID: $platformId. Requesting role change from $currentRole to $targetRole. Reason: $reason"
            ).onSuccess {
                showStatus("Role change request submitted to Admin Support. Reference ID: $platformId.")
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Could not submit role change request")
            }
        }
    }

    fun saveServiceListing(
        existingId: String?,
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
    ) {
        viewModelScope.launch {
            val myProfile = (myProfileState.value as? UiState.Success)?.data
            val providerName = myProfile?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
            val providerVerified = myProfile?.isStrictVerifiedBadgeActive == true

            val result = if (existingId.isNullOrBlank()) {
                repository.createService(
                    providerName = providerName,
                    providerVerified = providerVerified,
                    title = title,
                    category = category,
                    subcategory = subcategory,
                    shortSummary = shortSummary,
                    description = description,
                    skills = skills,
                    startingPrice = startingPrice,
                    pricingModel = pricingModel,
                    deliverables = deliverables,
                    turnaround = turnaround,
                    city = city,
                    locality = locality,
                    availability = availability,
                    terms = terms,
                    isPublished = isPublished
                ).map { }
            } else {
                val current = _editingService.value ?: ServiceListing(id = existingId, providerId = currentUserId)
                repository.updateService(
                    current.copy(
                        providerName = providerName,
                        providerVerified = providerVerified,
                        title = title,
                        category = category,
                        subcategory = subcategory,
                        shortSummary = shortSummary,
                        description = description,
                        skills = skills,
                        startingPrice = startingPrice,
                        pricingModel = pricingModel,
                        deliverables = deliverables,
                        turnaround = turnaround,
                        city = city,
                        locality = locality,
                        availability = availability,
                        terms = terms,
                        isPublished = isPublished,
                        isDraft = !isPublished
                    )
                )
            }

            result.onSuccess {
                showStatus(if (isPublished) "Service listing published to SkillConnect!" else "Draft saved.")
                navigateTo(AppDestination.PROVIDER_DASHBOARD)
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Failed to save service listing")
            }
        }
    }

    fun deleteServiceListing(serviceId: String) {
        viewModelScope.launch {
            repository.deleteService(serviceId)
                .onSuccess { showStatus("Service listing deleted.") }
                .onFailure { e -> showError(e.localizedMessage ?: "Could not delete listing") }
        }
    }

    fun submitBookingOrQuoteRequest(
        service: ServiceListing,
        requirements: String,
        preferredDate: String,
        attachmentNote: String,
        requestType: String,
        preferredTimeSlot: String = "10:00 AM – 1:00 PM",
        paymentMethod: String = "upi"
    ) {
        if (service.providerId == currentUserId) {
            showError("You cannot submit a booking request for your own service listing.")
            return
        }
        viewModelScope.launch {
            val myProfile = (myProfileState.value as? UiState.Success)?.data
            val custName = myProfile?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
            repository.createBookingRequest(
                customerName = custName,
                providerId = service.providerId,
                providerName = service.providerName,
                serviceId = service.id,
                serviceTitle = service.title,
                category = service.category,
                requirements = requirements,
                preferredDate = preferredDate,
                attachmentNote = attachmentNote,
                quotedPrice = service.startingPrice,
                requestType = requestType,
                preferredTimeSlot = preferredTimeSlot,
                paymentMethod = paymentMethod
            ).onSuccess { bookingId ->
                repository.sendNotification(
                    recipientUserId = service.providerId,
                    title = "New ${if (requestType == "quote") "Quote Enquiry" else "Booking Request"}",
                    body = "$custName sent an enquiry for '${service.title}' ($preferredDate, $preferredTimeSlot).",
                    type = "new_enquiry",
                    relatedId = bookingId
                )
                showStatus("Request submitted! Track status in Bookings.")
                navigateTo(AppDestination.BOOKINGS)
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Could not submit booking request")
            }
        }
    }

    fun updateCustomerBookingStatus(booking: BookingRequest, newStatus: String, note: String) {
        viewModelScope.launch {
            repository.updateBookingStatusByCustomer(booking.id, newStatus, note)
                .onSuccess {
                    repository.sendNotification(
                        recipientUserId = booking.providerId,
                        title = "Booking ${booking.effectiveBookingRef} Updated",
                        body = "Customer updated '${booking.serviceTitle}' to ${newStatus.replace("_", " ")}.",
                        type = "status_changed",
                        relatedId = booking.id
                    )
                    showStatus("Booking ${booking.effectiveBookingRef} updated to ${newStatus.replace("_", " ")}.")
                }
                .onFailure { e -> showError(e.localizedMessage ?: "Failed to update booking") }
        }
    }

    fun updateProviderBookingStatus(
        booking: BookingRequest,
        newStatus: String,
        note: String,
        updatedQuote: Long? = null
    ) {
        viewModelScope.launch {
            repository.updateBookingStatusByProvider(booking.id, newStatus, note, updatedQuote)
                .onSuccess {
                    val notifType = when (newStatus) {
                        "accepted", "awaiting_payment", "confirmed" -> "booking_accepted"
                        "declined" -> "booking_declined"
                        "completed" -> "review_reminder"
                        else -> "status_changed"
                    }
                    repository.sendNotification(
                        recipientUserId = booking.customerId,
                        title = "Booking ${booking.effectiveBookingRef}: ${newStatus.replace("_", " ").uppercase()}",
                        body = "${booking.providerName} updated '${booking.serviceTitle}' to ${newStatus.replace("_", " ")}.",
                        type = notifType,
                        relatedId = booking.id
                    )
                    showStatus("Booking ${booking.effectiveBookingRef} marked as ${newStatus.replace("_", " ")}.")
                }
                .onFailure { e -> showError(e.localizedMessage ?: "Failed to update booking status") }
        }
    }

    // --- Hybrid Payment, Refund, Payout & Subscription Workflows ---

    fun processBookingPayment(
        booking: BookingRequest,
        method: String, // "upi", "card", "netbanking", "cash_offline"
        transactionRef: String
    ) {
        viewModelScope.launch {
            val isOffline = method == "cash_offline"
            val payStatus = if (isOffline) "pending" else "succeeded"
            val bookingPayStatus = if (isOffline) "pending_offline" else "paid_online"
            val safeRef = transactionRef.ifBlank {
                if (isOffline) "OFFLINE-SETTLEMENT" else "${method.uppercase()}-${System.currentTimeMillis().toString().takeLast(8)}"
            }
            repository.createPaymentRecord(
                providerId = booking.providerId,
                bookingId = booking.id,
                paymentType = "booking_payment",
                method = method,
                amountInr = booking.effectiveTotalInr,
                status = payStatus,
                transactionRef = safeRef,
                notes = if (isOffline) "Direct offline settlement selected for ${booking.effectiveBookingRef}" else "Online payment via ${method.uppercase()} for ${booking.effectiveBookingRef}"
            ).onSuccess { paymentId ->
                repository.updateBookingStatusByCustomer(
                    bookingId = booking.id,
                    newStatus = "confirmed",
                    statusNote = if (isOffline) "Confirmed with direct offline settlement ($safeRef)" else "Confirmed & paid online ($safeRef)",
                    paymentMethod = method,
                    paymentStatus = bookingPayStatus,
                    paymentReferenceId = paymentId
                ).onSuccess {
                    repository.sendNotification(
                        recipientUserId = booking.providerId,
                        title = "Payment & Booking Confirmed (${booking.effectiveBookingRef})",
                        body = "Customer confirmed ₹${booking.effectiveTotalInr} via ${method.replace("_", " ").uppercase()} (Ref: $safeRef).",
                        type = "payment_update",
                        relatedId = booking.id
                    )
                    showStatus("Booking ${booking.effectiveBookingRef} confirmed! Payment Ref: $safeRef.")
                }.onFailure { e ->
                    showError(e.localizedMessage ?: "Payment recorded, but booking status update failed")
                }
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Payment transaction failed")
            }
        }
    }

    fun requestBookingRefund(booking: BookingRequest, reason: String) {
        viewModelScope.launch {
            val refCode = "RFD-${System.currentTimeMillis().toString().takeLast(8)}"
            repository.createPaymentRecord(
                providerId = booking.providerId,
                bookingId = booking.id,
                paymentType = "refund",
                method = if (booking.paymentMethod in listOf("upi", "card", "netbanking", "bank_transfer", "cash_offline")) booking.paymentMethod else "upi",
                amountInr = booking.effectiveTotalInr,
                status = "refund_pending",
                transactionRef = refCode,
                notes = reason
            ).onSuccess {
                repository.updateBookingStatusByCustomer(
                    bookingId = booking.id,
                    newStatus = "refund_pending",
                    statusNote = "Refund requested: $reason",
                    paymentStatus = "refund_requested"
                ).onSuccess {
                    repository.sendNotification(
                        recipientUserId = booking.providerId,
                        title = "Refund Requested (${booking.effectiveBookingRef})",
                        body = "Customer requested a refund for '${booking.serviceTitle}'. Reason: $reason",
                        type = "refund_update",
                        relatedId = booking.id
                    )
                    showStatus("Refund request ($refCode) submitted and queued for review.")
                }
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Could not submit refund request")
            }
        }
    }

    fun requestProviderPayout(amountInr: Long, destinationNote: String) {
        if (amountInr <= 0L) {
            showError("Enter a valid payout amount greater than ₹0.")
            return
        }
        viewModelScope.launch {
            val refCode = "PYT-${System.currentTimeMillis().toString().takeLast(8)}"
            repository.createPaymentRecord(
                providerId = currentUserId,
                bookingId = "",
                paymentType = "payout",
                method = "bank_transfer",
                amountInr = amountInr,
                status = "pending",
                transactionRef = refCode,
                notes = destinationNote.ifBlank { "Standard NEFT/IMPS/UPI Provider Payout" }
            ).onSuccess {
                showStatus("Payout request $refCode for ₹$amountInr submitted for settlement.")
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Could not request payout")
            }
        }
    }

    fun upgradeSubscriptionPlan(
        plan: String,
        method: String,
        priceInr: Long,
        utrReference: String = "",
        payerUpiNote: String = ""
    ) {
        viewModelScope.launch {
            val myProfile = (myProfileState.value as? UiState.Success)?.data
            val role = myProfile?.role ?: "customer"
            if (plan == "premium" || plan == "pro") {
                val safeUtr = utrReference.trim()
                if (safeUtr.length < 6) {
                    showError("Please enter your UPI UTR / transaction reference (at least 6 characters).")
                    return@launch
                }
                repository.submitDirectUpiSubscriptionRequest(
                    role = role,
                    amountInr = priceInr,
                    utrReference = safeUtr,
                    payerUpiNote = payerUpiNote
                ).onSuccess { paymentId ->
                    repository.sendNotification(
                        recipientUserId = currentUserId,
                        title = "Subscription Payment Pending Verification",
                        body = "Your Direct UPI UTR ($safeUtr) for ₹$priceInr is pending CEO/Founder merchant statement verification.",
                        type = "payment_update",
                        relatedId = paymentId
                    )
                    showStatus("Pending UPI subscription request (UTR: $safeUtr) submitted! Activates after CEO/Founder review.")
                }.onFailure { e ->
                    showError(e.localizedMessage ?: "Subscription request submission failed")
                }
            } else {
                showStatus("Your account remains on the Free Plan until an approved subscription upgrade.")
            }
        }
    }

    fun sendChatMessage(text: String) {
        val conv = _activeConversation.value ?: return
        if (conv.isBlocked) {
            showError("This conversation is currently blocked.")
            return
        }
        if (text.isBlank()) return
        viewModelScope.launch {
            val myProfile = (myProfileState.value as? UiState.Success)?.data
            val senderName = myProfile?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
            repository.sendMessage(
                conversationId = conv.id,
                customerId = conv.customerId,
                providerId = conv.providerId,
                senderName = senderName,
                text = text
            ).onSuccess {
                val recipientId = if (conv.customerId == currentUserId) conv.providerId else conv.customerId
                repository.sendNotification(
                    recipientUserId = recipientId,
                    title = "New Message from $senderName",
                    body = text.take(120),
                    type = "new_message",
                    relatedId = conv.id
                )
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Message failed to send")
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    fun markAllNotificationsRead() {
        val unreadIds = notificationsState.value.filter { !it.isRead }.map { it.id }
        if (unreadIds.isEmpty()) return
        viewModelScope.launch {
            repository.markAllNotificationsRead(unreadIds)
                .onSuccess { showStatus("All notifications marked as read.") }
        }
    }

    fun submitCompletedJobReview(booking: BookingRequest, rating: Int, comment: String) {
        viewModelScope.launch {
            repository.submitReview(booking, rating, comment)
                .onSuccess {
                    showStatus("Thank you! Your verified review has been published.")
                }
                .onFailure { e ->
                    showError(e.localizedMessage ?: "Could not submit review (ensure job is completed and not already reviewed).")
                }
        }
    }

    fun toggleFavouriteItem(
        targetId: String,
        targetType: String,
        title: String,
        subtitle: String,
        location: String,
        price: Long
    ) {
        val isSaved = favouritesState.value.any { it.targetId == targetId && it.targetType == targetType }
        viewModelScope.launch {
            repository.toggleFavourite(
                targetId = targetId,
                targetType = targetType,
                title = title,
                subtitle = subtitle,
                location = location,
                price = price,
                currentlySaved = isSaved
            ).onSuccess {
                showStatus(if (isSaved) "Removed from Favourites." else "Saved to Favourites.")
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Could not update favourites")
            }
        }
    }

    fun submitProviderVerification(
        legalName: String,
        businessName: String,
        category: String,
        city: String,
        experienceYears: Long,
        portfolioSummary: String,
        documentReferenceNote: String
    ) {
        viewModelScope.launch {
            repository.submitProviderVerification(
                legalName = legalName,
                businessName = businessName,
                category = category,
                city = city,
                experienceYears = experienceYears,
                portfolioSummary = portfolioSummary,
                documentReferenceNote = documentReferenceNote
            ).onSuccess {
                repository.sendNotification(
                    recipientUserId = currentUserId,
                    title = "Verification Submitted",
                    body = "Your verification details are now Pending Review.",
                    type = "verification_update",
                    relatedId = currentUserId
                )
                showStatus("Verification submission received and marked Pending Review.")
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Verification submission failed")
            }
        }
    }

    fun submitSafetyReport(targetId: String, targetType: String, reason: String, details: String) {
        viewModelScope.launch {
            repository.submitReport(targetId, targetType, reason, details)
                .onSuccess { showStatus("Report submitted to SkillConnect Trust & Safety.") }
                .onFailure { e -> showError(e.localizedMessage ?: "Failed to submit report") }
        }
    }

    fun submitSupportTicket(
        name: String,
        email: String,
        category: String,
        relatedBookingId: String,
        description: String
    ) {
        viewModelScope.launch {
            repository.submitSupportTicket(name, email, category, relatedBookingId, description)
                .onSuccess { showStatus("Support ticket created. Track its status in Help & Support.") }
                .onFailure { e -> showError(e.localizedMessage ?: "Failed to create support ticket") }
        }
    }

    // --- Admin Actions with Immutable Audit Logging ---

    fun adminSavePlatformSettings(
        announcementText: String,
        announcementActive: Boolean,
        freeListingLimit: Long,
        premiumPriceInr: Long,
        billingPeriod: String,
        paymentsDemoMode: Boolean,
        maintenanceMode: Boolean = false,
        sessionTimeoutMinutes: Long = 15L
    ) {
        viewModelScope.launch {
            repository.saveGlobalSettings(
                announcementText = announcementText,
                announcementActive = announcementActive,
                freeListingLimit = freeListingLimit,
                premiumPriceInr = premiumPriceInr,
                billingPeriod = billingPeriod,
                paymentsDemoMode = paymentsDemoMode,
                maintenanceMode = maintenanceMode,
                sessionTimeoutMinutes = sessionTimeoutMinutes
            ).onSuccess {
                repository.recordAdminAuditLog(
                    adminEmail = currentUserEmail,
                    actionType = "UPDATE_PLATFORM_SETTINGS",
                    targetCollection = "platform_settings",
                    targetId = "global",
                    reason = "Updated platform settings (maintenance=$maintenanceMode, timeout=${sessionTimeoutMinutes}m)",
                    previousState = "active",
                    newState = "premiumInr=$premiumPriceInr, freeLimit=$freeListingLimit, maintenance=$maintenanceMode"
                )
                showStatus("Platform settings & announcement bar updated.")
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Admin permission required to update platform settings")
            }
        }
    }

    fun adminReviewProvider(
        targetUserId: String,
        newStatus: String, // under_review, approved, rejected, additional_info_required, suspended
        reason: String
    ) {
        viewModelScope.launch {
            val prevStatus = adminAllProfilesState.value.firstOrNull { it.userId == targetUserId }?.verificationStatus ?: "pending_review"
            repository.adminReviewProviderVerification(
                targetUserId = targetUserId,
                newStatus = newStatus,
                adminNote = reason
            ).onSuccess {
                repository.recordAdminAuditLog(
                    adminEmail = currentUserEmail,
                    actionType = "PROVIDER_VERIFICATION_${newStatus.uppercase()}",
                    targetCollection = "user_profiles",
                    targetId = targetUserId,
                    reason = reason.ifBlank { "Admin verification review" },
                    previousState = prevStatus,
                    newState = newStatus
                )
                val title = when (newStatus) {
                    "approved" -> "Verified by SkillConnect!"
                    "rejected" -> "Verification Rejected"
                    "additional_info_required" -> "Additional Verification Info Requested"
                    "suspended" -> "Provider Listing Suspended"
                    else -> "Verification Under Review"
                }
                val body = when (newStatus) {
                    "approved" -> "Congratulations! Your Verified by SkillConnect badge is now live."
                    else -> "Admin note: ${reason.ifBlank { "Please check your Verification Status page." }}"
                }
                repository.sendNotification(
                    recipientUserId = targetUserId,
                    title = title,
                    body = body,
                    type = "verification_update",
                    relatedId = targetUserId
                )
                showStatus("Provider verification updated to $newStatus and logged in Audit Trail.")
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Admin verification action failed")
            }
        }
    }

    fun adminApproveProvider(targetUserId: String, approve: Boolean) {
        adminReviewProvider(
            targetUserId = targetUserId,
            newStatus = if (approve) "approved" else "rejected",
            reason = if (approve) "Approved by SkillConnect Admin" else "Verification criteria not met"
        )
    }

    fun adminChangeUserRole(targetProfile: UserProfile, newRole: String, reason: String) {
        viewModelScope.launch {
            repository.adminUpdateUserVerificationAndStatus(
                targetUserId = targetProfile.userId,
                isVerified = targetProfile.isVerified,
                verificationStatus = targetProfile.verificationStatus,
                accountStatus = targetProfile.accountStatus,
                rejectionReason = targetProfile.verificationRejectionReason,
                newRole = newRole
            ).onSuccess {
                repository.recordAdminAuditLog(
                    adminEmail = currentUserEmail,
                    actionType = "CHANGE_USER_ROLE",
                    targetCollection = "user_profiles",
                    targetId = targetProfile.userId,
                    reason = reason.ifBlank { "Authorized support role change" },
                    previousState = targetProfile.role,
                    newState = newRole
                )
                repository.sendNotification(
                    recipientUserId = targetProfile.userId,
                    title = "Account Role Updated to ${newRole.uppercase()}",
                    body = "An administrator processed your role change to $newRole.",
                    type = "support_update",
                    relatedId = targetProfile.userId
                )
                showStatus("Updated ${targetProfile.displayName}'s role to $newRole.")
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Failed to change user role")
            }
        }
    }

    fun adminModerateListing(serviceId: String, isPublished: Boolean, isFeatured: Boolean, status: String, reason: String) {
        viewModelScope.launch {
            repository.adminModerateService(serviceId, isPublished, isFeatured, status, reason)
                .onSuccess {
                    repository.recordAdminAuditLog(
                        adminEmail = currentUserEmail,
                        actionType = "MODERATE_SERVICE_LISTING",
                        targetCollection = "services",
                        targetId = serviceId,
                        reason = reason.ifBlank { "Listing moderation update" },
                        previousState = "published",
                        newState = "status=$status, published=$isPublished, featured=$isFeatured"
                    )
                    showStatus("Listing moderation updated and recorded in Audit Logs.")
                }
                .onFailure { e -> showError(e.localizedMessage ?: "Admin moderation failed") }
        }
    }

    fun adminProcessBookingRefund(booking: BookingRequest, approveRefund: Boolean, adminNote: String) {
        viewModelScope.launch {
            val activeRole = activeExecutiveRole.value
            if (activeRole == "cofounder") {
                showError("Co-Founder accounts cannot directly execute refunds. Flag or escalate the dispute to CEO.")
                return@launch
            }
            if (approveRefund && activeRole == "ceo" && booking.effectiveTotalInr > 10000L) {
                showError("CEO refund limit is ₹10,000 INR. Refunds above ₹10,000 require Founder approval.")
                return@launch
            }
            val nextStatus = if (approveRefund) "refunded" else "confirmed"
            val nextPayStatus = if (approveRefund) "refunded" else "paid_online"
            repository.adminUpdateBookingAndRefund(
                bookingId = booking.id,
                newStatus = nextStatus,
                statusNote = adminNote.ifBlank { if (approveRefund) "Refund processed by Admin" else "Refund declined by Admin" },
                paymentStatus = nextPayStatus
            ).onSuccess {
                repository.recordAdminAuditLog(
                    adminEmail = currentUserEmail,
                    actionType = if (approveRefund) "APPROVE_REFUND" else "DECLINE_REFUND",
                    targetCollection = "booking_requests",
                    targetId = booking.id,
                    reason = adminNote,
                    previousState = booking.status,
                    newState = nextStatus
                )
                repository.sendNotification(
                    recipientUserId = booking.customerId,
                    title = if (approveRefund) "Refund Processed (${booking.effectiveBookingRef})" else "Refund Update (${booking.effectiveBookingRef})",
                    body = adminNote.ifBlank { if (approveRefund) "Your refund has been approved and processed." else "Your refund request was reviewed." },
                    type = "refund_update",
                    relatedId = booking.id
                )
                showStatus("Booking ${booking.effectiveBookingRef} updated to $nextStatus.")
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Failed to process refund")
            }
        }
    }

    fun adminUpdatePaymentOrPayout(payment: PaymentRecord, newStatus: String, note: String) {
        viewModelScope.launch {
            val activeRole = activeExecutiveRole.value
            if (activeRole == "cofounder") {
                showError("Co-Founder accounts do not have payment ledger or refund approval privileges.")
                return@launch
            }
            if (newStatus == "refunded" && activeRole == "ceo" && payment.amountInr > 10000L) {
                showError("CEO refund limit is ₹10,000 INR. Escalate refunds above ₹10,000 to Founder.")
                return@launch
            }
            repository.updatePaymentStatus(payment.id, newStatus, note)
                .onSuccess {
                    repository.recordAdminAuditLog(
                        adminEmail = currentUserEmail,
                        actionType = "UPDATE_PAYMENT_${newStatus.uppercase()}",
                        targetCollection = "payments",
                        targetId = payment.id,
                        reason = note,
                        previousState = payment.status,
                        newState = newStatus
                    )
                    val targetUid = payment.providerId.ifBlank { payment.userId }
                    if (targetUid.isNotBlank()) {
                        repository.sendNotification(
                            recipientUserId = targetUid,
                            title = "Payment / Payout Update (${payment.transactionRef})",
                            body = "Status updated to ${newStatus.uppercase()}. $note",
                            type = "payment_update",
                            relatedId = payment.id
                        )
                    }
                    showStatus("Payment ${payment.transactionRef} updated to $newStatus.")
                }
                .onFailure { e -> showError(e.localizedMessage ?: "Could not update payment") }
        }
    }

    fun adminReviewSubscriptionPayment(
        payment: PaymentRecord,
        approve: Boolean,
        verificationNote: String
    ) {
        viewModelScope.launch {
            val activeRole = activeExecutiveRole.value
            if (activeRole !in listOf("ceo", "founder") && !isLegacyFounderEmail) {
                showError("Only an active CEO or Founder can approve or reject Direct UPI subscription payments.")
                return@launch
            }
            val cleanNote = verificationNote.trim()
            if (cleanNote.length < 8) {
                showError("Enter a verification note (min 8 chars) confirming the merchant bank/UPI statement check.")
                return@launch
            }
            repository.adminApproveOrRejectSubscriptionPayment(
                payment = payment,
                approve = approve,
                verificationNote = cleanNote
            ).onSuccess {
                val actionLabel = if (approve) "APPROVE_SUBSCRIPTION_PAYMENT" else "REJECT_SUBSCRIPTION_PAYMENT"
                repository.recordAdminAuditLog(
                    adminEmail = currentUserEmail,
                    actionType = actionLabel,
                    targetCollection = "payments",
                    targetId = payment.id,
                    reason = cleanNote,
                    previousState = payment.status,
                    newState = if (approve) "succeeded -> subscriptionPlan=premium" else "failed -> subscriptionPlan=free"
                )
                if (payment.userId.isNotBlank()) {
                    repository.sendNotification(
                        recipientUserId = payment.userId,
                        title = if (approve) "SkillConnect Premium Activated!" else "Subscription Payment Not Verified",
                        body = if (approve) {
                            "Your Direct UPI payment (${payment.transactionRef}) was verified. Your Premium plan is now active!"
                        } else {
                            "Your Direct UPI payment (${payment.transactionRef}) could not be verified: $cleanNote"
                        },
                        type = "payment_update",
                        relatedId = payment.id
                    )
                }
                showStatus(
                    if (approve) {
                        "Verified ${payment.transactionRef} and activated Premium for user ${payment.userId.take(10)}."
                    } else {
                        "Rejected subscription request ${payment.transactionRef}."
                    }
                )
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Failed to review subscription payment")
            }
        }
    }

    fun adminResolveReport(report: SafetyReport, newStatus: String, resolution: String) {
        viewModelScope.launch {
            repository.adminUpdateReportStatus(report.id, newStatus, resolution)
                .onSuccess {
                    repository.recordAdminAuditLog(
                        adminEmail = currentUserEmail,
                        actionType = "RESOLVE_SAFETY_REPORT",
                        targetCollection = "reports",
                        targetId = report.id,
                        reason = resolution,
                        previousState = report.status,
                        newState = newStatus
                    )
                    showStatus("Report ${report.id.take(10)} marked $newStatus.")
                }
                .onFailure { e -> showError(e.localizedMessage ?: "Could not update report") }
        }
    }

    fun adminResolveTicket(ticket: SupportTicket, newStatus: String) {
        viewModelScope.launch {
            repository.adminUpdateSupportTicketStatus(ticket.id, newStatus)
                .onSuccess {
                    repository.recordAdminAuditLog(
                        adminEmail = currentUserEmail,
                        actionType = "UPDATE_SUPPORT_TICKET",
                        targetCollection = "support_tickets",
                        targetId = ticket.id,
                        reason = "Ticket status changed to $newStatus",
                        previousState = ticket.status,
                        newState = newStatus
                    )
                    repository.sendNotification(
                        recipientUserId = ticket.userId,
                        title = "Support Ticket Updated (${ticket.category})",
                        body = "Your support ticket status is now ${newStatus.replace("_", " ").uppercase()}.",
                        type = "support_update",
                        relatedId = ticket.id
                    )
                    showStatus("Support ticket marked $newStatus.")
                }
                .onFailure { e -> showError(e.localizedMessage ?: "Could not update ticket") }
        }
    }

    // --- Executive Admin Authentication, Session Security & Dispute Resolution (Sections B, C, F, G) ---

    fun loginOrProvisionExecutiveAdmin(
        requestedRole: String,
        usernameInput: String,
        passwordInput: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.authenticateOrProvisionExecutiveAdmin(
                selectedRole = requestedRole,
                enteredUsername = usernameInput,
                enteredPassword = passwordInput
            ).onSuccess { adminAccount ->
                val expiresMs = if (adminAccount.adminRole == "founder") {
                    System.currentTimeMillis() + 365L * 24L * 3600L * 1000L
                } else {
                    System.currentTimeMillis() + ADMIN_SESSION_TIMEOUT_MS
                }
                preferencesRepository.setAdminSession(
                    adminRole = adminAccount.adminRole,
                    canonicalId = adminAccount.canonicalAccountId,
                    displayName = adminAccount.displayName,
                    username = usernameInput.trim(),
                    expiresAtMs = expiresMs
                )
                repository.recordAdminAuditLog(
                    adminEmail = currentUserEmail,
                    actionType = "EXECUTIVE_ADMIN_LOGIN_${adminAccount.adminRole.uppercase()}",
                    targetCollection = "admins",
                    targetId = adminAccount.adminUid,
                    reason = "Signed in via ${adminAccount.displayName} Login Card as ${usernameInput.trim()}",
                    previousState = "authenticated",
                    newState = "${adminAccount.canonicalAccountId} (${adminAccount.adminRole})"
                )
                val roleBannerName = when (adminAccount.adminRole) {
                    "cofounder" -> "CO-FOUNDER"
                    "ceo" -> "CEO"
                    else -> "FOUNDER"
                }
                showStatus("Signed in as $roleBannerName (${adminAccount.canonicalAccountId}).")
                _navigationStack.value = listOf(AppDestination.ADMIN_DASHBOARD)
                _currentDestination.value = AppDestination.ADMIN_DASHBOARD
                onSuccess()
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Executive admin authentication failed")
            }
        }
    }

    fun logoutExecutiveAdminSession() {
        viewModelScope.launch {
            val prevRole = activeExecutiveRole.value
            preferencesRepository.clearAdminSession()
            showStatus(if (prevRole.isNotBlank()) "Signed out of ${prevRole.uppercase()} admin session." else "Admin session closed.")
            val myProfile = (myProfileState.value as? UiState.Success)?.data
            val fallback = if (myProfile?.role == "provider") AppDestination.PROVIDER_DASHBOARD else AppDestination.HOME
            _navigationStack.value = listOf(fallback)
            _currentDestination.value = fallback
        }
    }

    fun changeExecutiveAdminPassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            repository.changeAdminPassword(currentPassword, newPassword)
                .onSuccess {
                    repository.recordAdminAuditLog(
                        adminEmail = currentUserEmail,
                        actionType = "ADMIN_PASSWORD_CHANGED",
                        targetCollection = "admins",
                        targetId = currentUserId,
                        reason = "Executive admin updated password",
                        previousState = "active",
                        newState = "active_rehashed"
                    )
                    showStatus("Admin password updated securely.")
                }
                .onFailure { e ->
                    showError(e.localizedMessage ?: "Failed to update admin password")
                }
        }
    }

    fun founderReassignOrResetAdmin(
        targetAdminUid: String,
        newRole: String,
        newTempPassword: String
    ) {
        if (activeExecutiveRole.value != "founder") {
            showError("Only the Founder (ADM-FND-001) can modify executive admin accounts.")
            return
        }
        viewModelScope.launch {
            repository.founderReassignOrResetAdmin(
                targetAdminUid = targetAdminUid,
                newRole = newRole,
                newPasswordOptional = newTempPassword.trim()
            ).onSuccess {
                repository.recordAdminAuditLog(
                    adminEmail = currentUserEmail,
                    actionType = "FOUNDER_UPDATE_ADMIN_ACCOUNT",
                    targetCollection = "admins",
                    targetId = targetAdminUid,
                    reason = "Founder updated executive role to $newRole",
                    previousState = "existing",
                    newState = newRole
                )
                showStatus("Admin account $targetAdminUid updated to ${newRole.uppercase()}.")
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Failed to update executive admin account")
            }
        }
    }

    /**
     * Section G: Admin Session Security (15-Minute Timeout + Founder Exception).
     * Co-Founder and CEO sessions expire after 15 minutes in background/inactivity.
     * Founder (ADM-FND-001) is exempt and remains persistently signed in.
     */
    fun onAppBackgrounded() {
        viewModelScope.launch {
            val role = activeExecutiveRole.value
            if (role in listOf("cofounder", "ceo")) {
                preferencesRepository.markAdminBackgrounded(System.currentTimeMillis())
            }
        }
    }

    fun onAppResumed() {
        viewModelScope.launch {
            val prefs = userPreferences.value
            val role = activeExecutiveRole.value
            if (role.isBlank()) return@launch
            // Founder Exception: Founder is NEVER auto-logged out after 15 minutes in background
            if (role == "founder" || myAdminAccountState.value?.founderSessionPersistent == true) {
                preferencesRepository.markAdminBackgrounded(0L)
                return@launch
            }
            val now = System.currentTimeMillis()
            val bgAt = prefs.adminLastBackgroundedAtMs
            val expiredByBackground = bgAt > 0L && (now - bgAt) >= ADMIN_SESSION_TIMEOUT_MS
            val expiredByDeadline = prefs.adminSessionExpiresAtMs in 1L until now
            if (expiredByBackground || expiredByDeadline) {
                preferencesRepository.clearAdminSession()
                showError("Executive session expired after 15 minutes of inactivity. Please re-authenticate.")
                if (_currentDestination.value == AppDestination.ADMIN_DASHBOARD) {
                    _navigationStack.value = listOf(AppDestination.HOME)
                    _currentDestination.value = AppDestination.HOME
                }
            } else {
                val newExp = now + ADMIN_SESSION_TIMEOUT_MS
                preferencesRepository.refreshAdminForegroundActivity(newExp)
                repository.refreshAdminSessionDeadline(role)
            }
        }
    }

    // --- Payment Dispute Workflows (Section F) ---

    fun fileBookingOrPaymentDispute(
        booking: BookingRequest,
        paymentId: String,
        reason: String,
        statement: String,
        evidenceNote: String
    ) {
        viewModelScope.launch {
            val isCustomer = booking.customerId == currentUserId
            val safePayId = paymentId.ifBlank {
                booking.paymentReferenceId.ifBlank { "pay_${booking.id.take(16)}_booking" }
            }
            repository.createPaymentDispute(
                booking = booking,
                paymentId = safePayId,
                reason = reason,
                statement = statement,
                evidenceNote = evidenceNote,
                isFiledByCustomer = isCustomer
            ).onSuccess { disputeId ->
                if (isCustomer) {
                    repository.updateBookingStatusByCustomer(
                        bookingId = booking.id,
                        newStatus = "disputed",
                        statusNote = "Payment dispute filed ($disputeId): $reason",
                        paymentStatus = "disputed"
                    )
                }
                val otherPartyId = if (isCustomer) booking.providerId else booking.customerId
                repository.sendNotification(
                    recipientUserId = otherPartyId,
                    title = "Payment Dispute Opened (${booking.effectiveBookingRef})",
                    body = "A dispute ($disputeId) was filed for '${booking.serviceTitle}': $reason",
                    type = "refund_update",
                    relatedId = disputeId
                )
                showStatus("Dispute $disputeId filed and routed to Co-Founder/Admin review.")
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Could not file payment dispute")
            }
        }
    }

    fun addDisputeStatement(
        disputeId: String,
        isCustomer: Boolean,
        statement: String,
        evidenceNote: String
    ) {
        viewModelScope.launch {
            repository.addDisputeStatement(
                disputeId = disputeId,
                isCustomer = isCustomer,
                statement = statement,
                evidenceNote = evidenceNote
            ).onSuccess {
                showStatus("Dispute statement & evidence saved.")
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Failed to update dispute statement")
            }
        }
    }

    fun adminHandleDispute(
        dispute: PaymentDispute,
        newStatus: String,
        assignedRole: String,
        decisionNote: String
    ) {
        viewModelScope.launch {
            repository.adminResolveOrEscalateDispute(
                dispute = dispute,
                newStatus = newStatus,
                assignedRole = assignedRole,
                decisionNote = decisionNote
            ).onSuccess {
                // If resolved_refunded, also update linked booking and payment record
                if (newStatus == "resolved_refunded" && dispute.bookingId.isNotBlank()) {
                    repository.adminUpdateBookingAndRefund(
                        bookingId = dispute.bookingId,
                        newStatus = "refunded",
                        statusNote = "Dispute ${dispute.id} resolved with refund: $decisionNote",
                        paymentStatus = "refunded"
                    )
                    if (dispute.paymentId.isNotBlank()) {
                        repository.updatePaymentStatus(
                            paymentId = dispute.paymentId,
                            newStatus = "refunded",
                            notes = "Refund approved via Dispute ${dispute.id}"
                        )
                    }
                }
                repository.recordAdminAuditLog(
                    adminEmail = currentUserEmail,
                    actionType = "DISPUTE_${newStatus.uppercase()}",
                    targetCollection = "disputes",
                    targetId = dispute.id,
                    reason = decisionNote,
                    previousState = dispute.status,
                    newState = "$newStatus (assigned=$assignedRole, amount=₹${dispute.disputedAmountInr})"
                )
                repository.sendNotification(
                    recipientUserId = dispute.customerId,
                    title = "Dispute Update (${dispute.id})",
                    body = "Status: ${newStatus.replace("_", " ").uppercase()}. Decision: $decisionNote",
                    type = "refund_update",
                    relatedId = dispute.id
                )
                showStatus("Dispute ${dispute.id} updated to ${newStatus.replace("_", " ")}.")
            }.onFailure { e ->
                showError(e.localizedMessage ?: "Dispute action rejected by role policy")
            }
        }
    }

    // --- Jobs & Earning Opportunities + Skills & Courses Hub Actions ---

    fun toggleJobApplication(job: JobOpportunityItem) {
        viewModelScope.launch {
            val isAppliedNow = preferencesRepository.toggleJobApplication(job.id)
            showStatus(
                if (!isAppliedNow) {
                    "Withdrew application for '${job.title}'."
                } else {
                    "Applied for '${job.title}' (${job.companyOrClient})!"
                }
            )
        }
    }

    fun toggleCourseEnrollment(course: SkillCourseItem) {
        viewModelScope.launch {
            val isEnrolledNow = preferencesRepository.toggleCourseEnrollment(course.id)
            showStatus(
                if (!isEnrolledNow) {
                    "Removed '${course.title}' from active courses."
                } else {
                    "Enrolled in '${course.title}'!"
                }
            )
        }
    }

    // --- Full Logout Workflow (Section I) ---

    fun performFullLogout(onReadyToSignOut: () -> Unit) {
        viewModelScope.launch {
            clearBanners()
            _activeConversation.value = null
            _selectedServiceId.value = null
            _selectedProviderId.value = null
            _editingService.value = null
            _navigationStack.value = listOf(AppDestination.HOME)
            _currentDestination.value = AppDestination.HOME
            preferencesRepository.clearAllSessionDataOnLogout()
            onReadyToSignOut()
        }
    }

    // --- Preferences & Permissions ---

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDarkMode(enabled) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { preferencesRepository.setLanguage(lang) }
    }

    fun setLocation(
        city: String,
        locality: String,
        state: String = "",
        pinCode: String = ""
    ) {
        viewModelScope.launch {
            val resolvedState = state.ifBlank {
                if (city.equals("All India", ignoreCase = true)) "" else com.example.data.model.IndiaLocationDirectory.stateForCity(city)
            }
            val resolvedPin = pinCode.ifBlank {
                if (city.equals("All India", ignoreCase = true)) "" else com.example.data.model.IndiaLocationDirectory.defaultPinForCity(city)
            }
            preferencesRepository.setLocation(
                city = city,
                locality = locality,
                state = resolvedState,
                pinCode = resolvedPin
            )
            preferencesRepository.markLocationPromptHandled()
            _filterState.update { it.copy(selectedCity = city, selectedLocality = locality) }
            val summary = buildString {
                append(city)
                if (locality.isNotBlank() && locality != "All Localities") append(", $locality")
                if (resolvedPin.isNotBlank()) append(" ($resolvedPin)")
            }
            showStatus("Location set to $summary")
        }
    }

    fun markLocationPromptDismissed() {
        viewModelScope.launch { preferencesRepository.markLocationPromptHandled() }
    }

    fun markNotificationPromptHandled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.markNotificationPromptHandled(enabled)
            showStatus(if (enabled) "Notifications enabled for booking and verification alerts." else "Notification preference saved.")
        }
    }

    fun setNotificationAndPrivacy(emailNotifs: Boolean, showPhone: Boolean, pushNotifs: Boolean? = null) {
        viewModelScope.launch { preferencesRepository.setNotificationAndPrivacy(emailNotifs, showPhone, pushNotifs) }
    }

    private companion object {
        const val TAG = "SkillConnectVM"
        const val TOAST_DURATION_MS = 2000L
        const val ADMIN_SESSION_TIMEOUT_MS = 15L * 60L * 1000L
    }
}
