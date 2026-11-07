package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.model.AdminRoleType
import com.example.model.SkillConnectBranding
import com.example.model.UserRole
import com.example.ui.MarketplaceUiState
import com.example.ui.MarketplaceViewModel
import com.example.ui.auth.signOutFromGoogleAndFirebase
import com.example.ui.components.SkillConnectTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MarketplaceViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var showSplash by remember { mutableStateOf(true) }
    var showOnboarding by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }

    // 15-Minute Admin Inactivity Session Timeout Monitor
    val isAdminSession = uiState.isAuthenticated &&
        (uiState.activeRole == UserRole.ADMIN || uiState.currentUser.role == UserRole.ADMIN)

    LaunchedEffect(isAdminSession, uiState.lastAdminActivityMillis) {
        if (isAdminSession) {
            while (true) {
                val now = System.currentTimeMillis()
                val elapsed = now - uiState.lastAdminActivityMillis
                val remaining = (MarketplaceViewModel.ADMIN_SESSION_TIMEOUT_MS - elapsed).coerceAtLeast(0L)
                val checkInterval = minOf(remaining.coerceAtLeast(1_000L), 5_000L)
                delay(checkInterval)
                if (viewModel.checkAdminSessionTimeout()) {
                    signOutFromGoogleAndFirebase(
                        context = context,
                        onSignOutComplete = {},
                        scope = coroutineScope
                    )
                    break
                }
            }
        }
    }

    if (showSplash) {
        SplashScreen(
            onSplashFinished = {
                showSplash = false
                showOnboarding = !uiState.isAuthenticated
            }
        )
        return
    }

    if (showOnboarding && !uiState.isAuthenticated) {
        OnboardingScreen(
            currentLanguage = uiState.currentLanguage,
            onLanguageChange = { viewModel.setLanguage(it) },
            onGetStarted = { showOnboarding = false }
        )
        return
    }

    // Authentication Guard: Protected features require active login
    if (!uiState.isAuthenticated) {
        AuthScreen(
            categories = uiState.categories,
            onRegisterCustomer = { name, email, phone, city, state, pass ->
                viewModel.registerCustomer(name, email, phone, city, state, pass)
            },
            onRegisterProvider = { name, email, phone, city, state, pass, title, catId, skills, rate, bio ->
                viewModel.registerProvider(name, email, phone, city, state, pass, title, catId, skills, rate, bio)
            },
            onLogin = { email, pass, role ->
                viewModel.login(email, pass, role)
            },
            onGoogleLogin = { uid, displayName, email, photoUrl, role ->
                viewModel.loginWithGoogle(uid, displayName, email, photoUrl, role)
            },
            onPublicForgotPassword = { email ->
                viewModel.sendPublicPasswordReset(email)
            },
            onAdminForgotPassword = { email ->
                viewModel.sendAdminPasswordReset(email)
            },
            sessionTimeoutMessage = uiState.sessionTimeoutMessage,
            onDismissSessionTimeout = { viewModel.clearSessionTimeoutMessage() },
            onProvisionOfficialAccounts = { fPass, cPass, aPass ->
                viewModel.provisionOfficialAdminAccounts(fPass, cPass, aPass)
            },
            onAuthSuccess = { /* Session restored and routed in ViewModel */ }
        )
        return
    }

    // Modal Overlays (Highest priority in navigation hierarchy)
    if (uiState.selectedProvider != null && !uiState.isCreateRequestDialogOpen) {
        val prov = uiState.selectedProvider!!
        ProviderDetailScreen(
            provider = prov,
            reviews = uiState.reviews,
            isFavourite = uiState.favouriteProviderIds.contains(prov.id),
            onToggleFavourite = { viewModel.toggleFavouriteProvider(prov.id) },
            onReportProvider = { reason -> viewModel.submitProviderReport(prov.id, reason, reason) },
            onBack = { viewModel.clearSelectedProvider() },
            onRequestWork = { viewModel.openCreateWorkRequestDialog(prov) },
            onSendMessage = {
                viewModel.openChatWith(prov.id, prov.name)
            }
        )
        return
    }

    if (uiState.activeConversationPartnerId != null) {
        ChatScreen(
            currentUserId = uiState.currentUser.id,
            partnerId = uiState.activeConversationPartnerId!!,
            partnerName = uiState.activeConversationPartnerName ?: "Contact",
            messages = uiState.messages,
            onSendMessage = { text -> viewModel.sendMessage(text) },
            onBack = { viewModel.closeChat() }
        )
        return
    }

    val footerRoutes = setOf(
        "become_provider",
        "provider_verification",
        "subscriptions",
        "help_centre",
        "legal_policies"
    )

    val unreadNotificationCount = uiState.notifications.count { !it.isRead }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SkillConnectDrawerSheet(
                uiState = uiState,
                onNavigateRoute = { route ->
                    coroutineScope.launch { drawerState.close() }
                    if (route == "explore_services") {
                        viewModel.setScreenRoute("explore")
                    } else {
                        viewModel.setScreenRoute(route)
                    }
                },
                onOpenAiAssistant = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.openAiAssistant()
                },
                onOpenLocationDialog = {
                    coroutineScope.launch { drawerState.close() }
                    showLocationDialog = true
                },
                onLogout = {
                    coroutineScope.launch { drawerState.close() }
                    signOutFromGoogleAndFirebase(
                        context = context,
                        onSignOutComplete = { viewModel.logout() },
                        scope = coroutineScope
                    )
                }
            )
        }
    ) {
        // Main Role-Aware Scaffold (preserves Bottom Navigation across tabs & profile editing)
        Scaffold(
            modifier = Modifier.pointerInput(isAdminSession) {
                if (isAdminSession) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.pressed }) {
                                viewModel.recordAdminActivity()
                            }
                        }
                    }
                }
            },
            topBar = {
                SkillConnectTopBar(
                    title = when (uiState.activeRole) {
                        UserRole.CUSTOMER -> when (uiState.currentScreenRoute) {
                            "explore" -> "Explore Services"
                            "bookings", "requests" -> "My Bookings"
                            "messages" -> "Messages & Alerts"
                            "profile", "edit_profile", "preview_profile" -> "My Profile"
                            in footerRoutes -> "Platform Hub"
                            else -> "Customer Marketplace"
                        }
                        UserRole.SERVICE_PROVIDER -> when (uiState.currentScreenRoute) {
                            "explore" -> "Explore Marketplace"
                            "bookings", "requests" -> "Client Bookings"
                            "messages" -> "Client Messages"
                            "profile", "edit_profile", "preview_profile" -> "Provider Profile"
                            else -> "Provider Studio"
                        }
                        UserRole.ADMIN -> when (uiState.currentScreenRoute) {
                            "marketplace", "explore", "home", "requests", "bookings" -> "Public Marketplace"
                            "profile", "edit_profile", "preview_profile" -> "Admin Profile"
                            else -> "Management Hub"
                        }
                    },
                    currentRole = uiState.activeRole,
                    currentLanguage = uiState.currentLanguage,
                    selectedCity = uiState.selectedCity,
                    unreadNotificationCount = unreadNotificationCount,
                    onRoleSelect = { /* Role switching handled in Profile */ },
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onHamburgerClick = {
                        coroutineScope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                    onLocationClick = { showLocationDialog = true },
                    onAiAssistantClick = { viewModel.openAiAssistant() },
                    onSearchClick = { viewModel.setScreenRoute("explore") },
                    onNotificationClick = { viewModel.setScreenRoute("messages") },
                    showRoleSwitcher = false
                )
            },
            floatingActionButton = {
                // Floating Gemini Chat Button
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAiAssistant() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Ask Gemini AI",
                            tint = Color(0xFF001F2B)
                        )
                    },
                    text = {
                        Text(
                            text = "Ask Gemini",
                            color = Color(0xFF001F2B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    containerColor = CyanBrand,
                    contentColor = Color(0xFF001F2B),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("floating_gemini_chat_button")
                )
            },
            bottomBar = {
                RoleAwareBottomNavigation(
                    currentRole = uiState.activeRole,
                    currentAdminRole = uiState.currentUser.adminRole,
                    currentRoute = uiState.currentScreenRoute,
                    unreadCount = unreadNotificationCount,
                    onRouteSelect = { route -> viewModel.setScreenRoute(route) }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val handleFooterNavigation: (String) -> Unit = { route ->
                    when (route) {
                        "explore" -> viewModel.setScreenRoute("explore")
                        else -> viewModel.setScreenRoute(route)
                    }
                }

                if (uiState.currentScreenRoute == "edit_profile") {
                    EditProfileScreen(
                        user = uiState.currentUser,
                        categories = uiState.categories,
                        onBack = { viewModel.setScreenRoute("profile") },
                        onSave = { updated ->
                            viewModel.updateUserProfile(updated)
                            viewModel.setScreenRoute("profile")
                        },
                        onPreview = { viewModel.setScreenRoute("preview_profile") }
                    )
                } else if (uiState.currentScreenRoute == "preview_profile") {
                    PublicProfilePreviewScreen(
                        user = uiState.currentUser,
                        onBack = { viewModel.setScreenRoute("profile") },
                        onEdit = { viewModel.setScreenRoute("edit_profile") }
                    )
                } else if (uiState.currentScreenRoute in footerRoutes) {
                    FooterDestinationScreen(
                        routeKey = uiState.currentScreenRoute,
                        uiState = uiState,
                        onBack = {
                            when (uiState.activeRole) {
                                UserRole.CUSTOMER -> viewModel.setScreenRoute("home")
                                UserRole.SERVICE_PROVIDER -> viewModel.setScreenRoute("dashboard")
                                UserRole.ADMIN -> viewModel.setScreenRoute("admin")
                            }
                        },
                        onSwitchToProviderRole = {
                            viewModel.switchActiveRole(UserRole.SERVICE_PROVIDER)
                            viewModel.setScreenRoute("dashboard")
                        },
                        onNavigateRoute = handleFooterNavigation,
                        onSubmitSupportTicket = { subject, message ->
                            viewModel.addNotification(
                                title = "Support Ticket: $subject",
                                message = "Submitted to ${SkillConnectBranding.OFFICIAL_EMAIL}: $message",
                                category = "Support"
                            )
                        }
                    )
                } else {
                    when (uiState.activeRole) {
                        UserRole.CUSTOMER -> {
                            when (uiState.currentScreenRoute) {
                                "home" -> CustomerHomeScreen(
                                    uiState = uiState,
                                    onSearchChange = { viewModel.setSearchQuery(it) },
                                    onCategoryClick = { catId ->
                                        viewModel.selectCategoryFilter(catId)
                                        viewModel.setScreenRoute("explore")
                                    },
                                    onProviderClick = { prov -> viewModel.selectProvider(prov) },
                                    onExploreAllClick = { viewModel.setScreenRoute("explore") },
                                    onRefreshMarketplace = { viewModel.refreshMarketplaceFeed() },
                                    onServiceScopeChange = { scope -> viewModel.setServiceScopeFilter(scope) },
                                    onToggleAvailableOnly = { viewModel.toggleAvailableOnlyFilter() },
                                    onToggleVerifiedOnly = { viewModel.toggleVerifiedOnlyFilter() },
                                    onSortOptionChange = { sort -> viewModel.setSortOption(sort) },
                                    onToggleFavouriteProvider = { provId -> viewModel.toggleFavouriteProvider(provId) },
                                    onOpenAiAssistant = { viewModel.openAiAssistant() },
                                    onNavigateRoute = handleFooterNavigation
                                )
                                "explore" -> MarketplaceExploreScreen(
                                    uiState = uiState,
                                    onSearchChange = { viewModel.setSearchQuery(it) },
                                    onCategoryFilter = { viewModel.selectCategoryFilter(it) },
                                    onCityFilter = { viewModel.selectCityFilter(it) },
                                    onProviderClick = { prov -> viewModel.selectProvider(prov) },
                                    onRefreshMarketplace = { viewModel.refreshMarketplaceFeed() },
                                    onServiceScopeChange = { scope -> viewModel.setServiceScopeFilter(scope) },
                                    onToggleAvailableOnly = { viewModel.toggleAvailableOnlyFilter() },
                                    onToggleVerifiedOnly = { viewModel.toggleVerifiedOnlyFilter() },
                                    onSortOptionChange = { sort -> viewModel.setSortOption(sort) },
                                    onToggleFavouriteProvider = { provId -> viewModel.toggleFavouriteProvider(provId) },
                                    onClearAllFilters = { viewModel.clearAllDiscoveryFilters() },
                                    onNavigateRoute = handleFooterNavigation
                                )
                                "bookings", "requests" -> CustomerRequestsScreen(
                                    requests = uiState.workRequests,
                                    currentCustomerId = uiState.currentUser.id,
                                    onOpenChat = { provId, provName -> viewModel.openChatWith(provId, provName) },
                                    onLeaveReview = { reqId, provId -> viewModel.openReviewDialog(reqId, provId) },
                                    onCancelBooking = { reqId -> viewModel.cancelBooking(reqId) },
                                    onNavigateRoute = handleFooterNavigation
                                )
                                "messages" -> MessagesAndNotificationsScreen(
                                    uiState = uiState,
                                    onOpenChat = { partnerId, partnerName ->
                                        viewModel.openChatWith(partnerId, partnerName)
                                    },
                                    onMarkAllNotificationsRead = { viewModel.markAllNotificationsRead() },
                                    onExploreProviders = { viewModel.setScreenRoute("explore") },
                                    onNavigateRoute = handleFooterNavigation
                                )
                                "profile" -> ProfileScreen(
                                    user = uiState.currentUser,
                                    activeRole = uiState.activeRole,
                                    isDarkTheme = uiState.isDarkTheme,
                                    currentLanguage = uiState.currentLanguage,
                                    favouriteProviders = uiState.favouriteProviders,
                                    onProviderClick = { prov -> viewModel.selectProvider(prov) },
                                    onNavigateFooterRoute = handleFooterNavigation,
                                    onToggleLanguage = { viewModel.toggleLanguage() },
                                    onToggleTheme = { viewModel.toggleTheme() },
                                    onSwitchRole = { newRole -> viewModel.switchActiveRole(newRole) },
                                    onNavigateToEditProfile = { viewModel.setScreenRoute("edit_profile") },
                                    onNavigateToPreviewProfile = { viewModel.setScreenRoute("preview_profile") },
                                    onLogout = {
                                        signOutFromGoogleAndFirebase(
                                            context = context,
                                            onSignOutComplete = { viewModel.logout() },
                                            scope = coroutineScope
                                        )
                                    }
                                )
                                else -> CustomerHomeScreen(
                                    uiState = uiState,
                                    onSearchChange = { viewModel.setSearchQuery(it) },
                                    onCategoryClick = { catId ->
                                        viewModel.selectCategoryFilter(catId)
                                        viewModel.setScreenRoute("explore")
                                    },
                                    onProviderClick = { prov -> viewModel.selectProvider(prov) },
                                    onExploreAllClick = { viewModel.setScreenRoute("explore") },
                                    onRefreshMarketplace = { viewModel.refreshMarketplaceFeed() },
                                    onServiceScopeChange = { scope -> viewModel.setServiceScopeFilter(scope) },
                                    onToggleAvailableOnly = { viewModel.toggleAvailableOnlyFilter() },
                                    onToggleVerifiedOnly = { viewModel.toggleVerifiedOnlyFilter() },
                                    onSortOptionChange = { sort -> viewModel.setSortOption(sort) },
                                    onToggleFavouriteProvider = { provId -> viewModel.toggleFavouriteProvider(provId) },
                                    onOpenAiAssistant = { viewModel.openAiAssistant() },
                                    onNavigateRoute = handleFooterNavigation
                                )
                            }
                        }

                        UserRole.SERVICE_PROVIDER -> {
                            when (uiState.currentScreenRoute) {
                                "dashboard", "home" -> ProviderDashboardScreen(
                                    uiState = uiState,
                                    onStatusChange = { reqId, status -> viewModel.updateRequestStatus(reqId, status) },
                                    onToggleAvailability = {
                                        val prov = uiState.allProviders.find { it.userId == uiState.currentUser.id }
                                            ?: uiState.allProviders.firstOrNull()
                                        if (prov != null) viewModel.toggleProviderAvailability(prov.id)
                                    },
                                    onOpenChat = { custId, custName -> viewModel.openChatWith(custId, custName) },
                                    onAddService = {}
                                )
                                "explore" -> MarketplaceExploreScreen(
                                    uiState = uiState,
                                    onSearchChange = { viewModel.setSearchQuery(it) },
                                    onCategoryFilter = { viewModel.selectCategoryFilter(it) },
                                    onCityFilter = { viewModel.selectCityFilter(it) },
                                    onProviderClick = { prov -> viewModel.selectProvider(prov) },
                                    onRefreshMarketplace = { viewModel.refreshMarketplaceFeed() },
                                    onServiceScopeChange = { scope -> viewModel.setServiceScopeFilter(scope) },
                                    onToggleAvailableOnly = { viewModel.toggleAvailableOnlyFilter() },
                                    onToggleVerifiedOnly = { viewModel.toggleVerifiedOnlyFilter() },
                                    onSortOptionChange = { sort -> viewModel.setSortOption(sort) },
                                    onToggleFavouriteProvider = { provId -> viewModel.toggleFavouriteProvider(provId) },
                                    onClearAllFilters = { viewModel.clearAllDiscoveryFilters() },
                                    onNavigateRoute = handleFooterNavigation
                                )
                                "bookings", "requests" -> ProviderDashboardScreen(
                                    uiState = uiState,
                                    onStatusChange = { reqId, status -> viewModel.updateRequestStatus(reqId, status) },
                                    onToggleAvailability = {},
                                    onOpenChat = { custId, custName -> viewModel.openChatWith(custId, custName) },
                                    onAddService = {}
                                )
                                "messages" -> MessagesAndNotificationsScreen(
                                    uiState = uiState,
                                    onOpenChat = { partnerId, partnerName ->
                                        viewModel.openChatWith(partnerId, partnerName)
                                    },
                                    onMarkAllNotificationsRead = { viewModel.markAllNotificationsRead() },
                                    onExploreProviders = { viewModel.setScreenRoute("explore") },
                                    onNavigateRoute = handleFooterNavigation
                                )
                                "profile" -> ProfileScreen(
                                    user = uiState.currentUser,
                                    activeRole = uiState.activeRole,
                                    isDarkTheme = uiState.isDarkTheme,
                                    currentLanguage = uiState.currentLanguage,
                                    favouriteProviders = uiState.favouriteProviders,
                                    onProviderClick = { prov -> viewModel.selectProvider(prov) },
                                    onNavigateFooterRoute = handleFooterNavigation,
                                    onToggleLanguage = { viewModel.toggleLanguage() },
                                    onToggleTheme = { viewModel.toggleTheme() },
                                    onSwitchRole = { newRole -> viewModel.switchActiveRole(newRole) },
                                    onNavigateToEditProfile = { viewModel.setScreenRoute("edit_profile") },
                                    onNavigateToPreviewProfile = { viewModel.setScreenRoute("preview_profile") },
                                    onLogout = {
                                        signOutFromGoogleAndFirebase(
                                            context = context,
                                            onSignOutComplete = { viewModel.logout() },
                                            scope = coroutineScope
                                        )
                                    }
                                )
                                else -> ProviderDashboardScreen(
                                    uiState = uiState,
                                    onStatusChange = { reqId, status -> viewModel.updateRequestStatus(reqId, status) },
                                    onToggleAvailability = {},
                                    onOpenChat = { custId, custName -> viewModel.openChatWith(custId, custName) },
                                    onAddService = {}
                                )
                            }
                        }

                        UserRole.ADMIN -> {
                            val adminRole = uiState.currentUser.adminRole ?: AdminRoleType.ADMIN
                            when (uiState.currentScreenRoute) {
                                "admin", "dashboard", "founder", "ceo_dashboard", "audit_logs", "complaints" -> {
                                    when (adminRole) {
                                        AdminRoleType.FOUNDER -> FounderDashboardScreen(
                                            uiState = uiState,
                                            onVerifyProvider = { provId, verified -> viewModel.toggleProviderVerification(provId, verified) },
                                            onAddCategory = { cat -> viewModel.addNewCategory(cat) },
                                            onBroadcastAnnouncement = { msg -> viewModel.broadcastAnnouncement(msg) },
                                            onUpdatePermissions = { targetUid, perms -> viewModel.updateAdminPermissions(targetUid, perms) },
                                            onRevokeAdmin = { targetUid -> viewModel.revokeAdmin(targetUid) },
                                            onToggleAdminActive = { targetUid -> viewModel.toggleAdminActive(targetUid) },
                                            onDeleteEntity = { id, isProv, reason -> viewModel.deleteUserOrProvider(id, isProv, reason) },
                                            onProvisionAccounts = { f, c, a -> viewModel.provisionOfficialAdminAccounts(f, c, a) }
                                        )
                                        AdminRoleType.CEO -> CeoDashboardScreen(
                                            uiState = uiState,
                                            onVerifyProvider = { provId, verified -> viewModel.toggleProviderVerification(provId, verified) },
                                            onResolveComplaint = { compId, status, notes -> viewModel.resolveComplaint(compId, status, notes) },
                                            onBroadcastPromotion = { msg -> viewModel.broadcastAnnouncement(msg) }
                                        )
                                        AdminRoleType.ADMIN -> AdminDashboardScreen(
                                            uiState = uiState,
                                            onVerifyProvider = { provId, verified -> viewModel.toggleProviderVerification(provId, verified) },
                                            onAddCategory = { cat -> viewModel.addNewCategory(cat) },
                                            onBroadcastAnnouncement = { msg -> viewModel.broadcastAnnouncement(msg) },
                                            onResolveComplaint = { compId, status, notes -> viewModel.resolveComplaint(compId, status, notes) }
                                        )
                                    }
                                }
                                "marketplace", "explore", "home", "requests", "bookings" -> AdminPublicMarketplaceScreen(
                                    uiState = uiState,
                                    currentSubRoute = uiState.currentScreenRoute,
                                    onSubRouteChange = { route -> viewModel.setScreenRoute(route) },
                                    onSearchChange = { viewModel.setSearchQuery(it) },
                                    onCategoryClick = { catId ->
                                        viewModel.selectCategoryFilter(catId)
                                        viewModel.setScreenRoute("explore")
                                    },
                                    onCategoryFilter = { viewModel.selectCategoryFilter(it) },
                                    onCityFilter = { viewModel.selectCityFilter(it) },
                                    onProviderClick = { prov -> viewModel.selectProvider(prov) },
                                    onExploreAllClick = { viewModel.setScreenRoute("explore") },
                                    onOpenChat = { provId, provName -> viewModel.openChatWith(provId, provName) },
                                    onLeaveReview = { reqId, provId -> viewModel.openReviewDialog(reqId, provId) },
                                    onRefreshMarketplace = { viewModel.refreshMarketplaceFeed() },
                                    onOpenAiAssistant = { viewModel.openAiAssistant() },
                                    onNavigateFooterRoute = handleFooterNavigation
                                )
                                "messages" -> MessagesAndNotificationsScreen(
                                    uiState = uiState,
                                    onOpenChat = { partnerId, partnerName ->
                                        viewModel.openChatWith(partnerId, partnerName)
                                    },
                                    onMarkAllNotificationsRead = { viewModel.markAllNotificationsRead() },
                                    onExploreProviders = { viewModel.setScreenRoute("explore") },
                                    onNavigateRoute = handleFooterNavigation
                                )
                                "profile" -> ProfileScreen(
                                    user = uiState.currentUser,
                                    activeRole = uiState.activeRole,
                                    isDarkTheme = uiState.isDarkTheme,
                                    currentLanguage = uiState.currentLanguage,
                                    favouriteProviders = uiState.favouriteProviders,
                                    onProviderClick = { prov -> viewModel.selectProvider(prov) },
                                    onNavigateFooterRoute = handleFooterNavigation,
                                    onToggleLanguage = { viewModel.toggleLanguage() },
                                    onToggleTheme = { viewModel.toggleTheme() },
                                    onSwitchRole = { newRole -> viewModel.switchActiveRole(newRole) },
                                    onNavigateToEditProfile = { viewModel.setScreenRoute("edit_profile") },
                                    onNavigateToPreviewProfile = { viewModel.setScreenRoute("preview_profile") },
                                    onLogout = {
                                        signOutFromGoogleAndFirebase(
                                            context = context,
                                            onSignOutComplete = { viewModel.logout() },
                                            scope = coroutineScope
                                        )
                                    }
                                )
                                else -> {
                                    when (adminRole) {
                                        AdminRoleType.FOUNDER -> FounderDashboardScreen(
                                            uiState = uiState,
                                            onVerifyProvider = { provId, verified -> viewModel.toggleProviderVerification(provId, verified) },
                                            onAddCategory = { cat -> viewModel.addNewCategory(cat) },
                                            onBroadcastAnnouncement = { msg -> viewModel.broadcastAnnouncement(msg) },
                                            onUpdatePermissions = { targetUid, perms -> viewModel.updateAdminPermissions(targetUid, perms) },
                                            onRevokeAdmin = { targetUid -> viewModel.revokeAdmin(targetUid) },
                                            onToggleAdminActive = { targetUid -> viewModel.toggleAdminActive(targetUid) },
                                            onDeleteEntity = { id, isProv, reason -> viewModel.deleteUserOrProvider(id, isProv, reason) },
                                            onProvisionAccounts = { f, c, a -> viewModel.provisionOfficialAdminAccounts(f, c, a) }
                                        )
                                        AdminRoleType.CEO -> CeoDashboardScreen(
                                            uiState = uiState,
                                            onVerifyProvider = { provId, verified -> viewModel.toggleProviderVerification(provId, verified) },
                                            onResolveComplaint = { compId, status, notes -> viewModel.resolveComplaint(compId, status, notes) },
                                            onBroadcastPromotion = { msg -> viewModel.broadcastAnnouncement(msg) }
                                        )
                                        AdminRoleType.ADMIN -> AdminDashboardScreen(
                                            uiState = uiState,
                                            onVerifyProvider = { provId, verified -> viewModel.toggleProviderVerification(provId, verified) },
                                            onAddCategory = { cat -> viewModel.addNewCategory(cat) },
                                            onBroadcastAnnouncement = { msg -> viewModel.broadcastAnnouncement(msg) },
                                            onResolveComplaint = { compId, status, notes -> viewModel.resolveComplaint(compId, status, notes) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Account Created Confirmation Banner (shown after one-time permanent registration)
                if (uiState.registrationConfirmationMessage != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface.copy(alpha = 0.96f)),
                        border = BorderStroke(1.5.dp, SuccessGreen),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .fillMaxWidth()
                            .zIndex(10f)
                            .testTag("account_created_confirmation_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Account Created",
                                tint = SuccessGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Permanent Account Created",
                                    color = SuccessGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = uiState.registrationConfirmationMessage!!,
                                    color = TextWhite,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearRegistrationConfirmationMessage() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("dismiss_registration_confirmation_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss confirmation",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Dialogs
                if (uiState.isCreateRequestDialogOpen && uiState.selectedProvider != null) {
                    CreateWorkRequestDialog(
                        provider = uiState.selectedProvider!!,
                        onDismiss = { viewModel.closeCreateWorkRequestDialog() },
                        onSubmit = { title, desc, cat, budget, deadline ->
                            viewModel.submitWorkRequest(title, desc, cat, budget, deadline)
                        }
                    )
                }

                if (uiState.isReviewDialogOpen && uiState.selectedProvider != null) {
                    SubmitReviewDialog(
                        providerName = uiState.selectedProvider!!.name,
                        onDismiss = { viewModel.closeReviewDialog() },
                        onSubmit = { rating, comment ->
                            viewModel.submitReview(rating, comment)
                        }
                    )
                }

                if (uiState.isAiAssistantOpen) {
                    GeminiAiAssistantDialog(
                        messages = uiState.aiChatMessages,
                        aiUiState = uiState.aiUiState,
                        onSendPrompt = { prompt -> viewModel.askGeminiAi(prompt) },
                        onDismiss = { viewModel.closeAiAssistant() }
                    )
                }

                if (showLocationDialog) {
                    LocationSelectorDialog(
                        selectedCity = uiState.selectedCity,
                        onSelectCity = { city -> viewModel.setCityFilterExplicit(city) },
                        onDismiss = { showLocationDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillConnectDrawerSheet(
    uiState: MarketplaceUiState,
    onNavigateRoute: (String) -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenLocationDialog: () -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = NavySurface,
        drawerContentColor = TextWhite,
        modifier = Modifier
            .widthIn(max = 310.dp)
            .testTag("skillconnect_navigation_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Brand Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF000130))
                        .border(
                            1.5.dp,
                            Brush.linearGradient(listOf(CyanBrand, GreenBrand)),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.skillconnect_logo),
                        contentDescription = "SkillConnect Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = SkillConnectBranding.DISPLAY_NAME,
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = SkillConnectBranding.TAGLINE,
                        color = CyanBrand,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Active User Summary
            Surface(
                color = NavyBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NavyCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CyanBrand.copy(alpha = 0.15f))
                            .border(1.dp, CyanBrand, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.currentUser.name.take(2).uppercase(),
                            color = CyanBrand,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.currentUser.name,
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${uiState.selectedCity ?: uiState.currentUser.city} • ${uiState.activeRole.name}",
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            HorizontalDivider(color = NavyCardBorder)

            Text(
                text = "MARKETPLACE NAVIGATION",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            DrawerNavRow(
                icon = Icons.Default.Home,
                label = "Home",
                selected = uiState.currentScreenRoute == "home",
                onClick = { onNavigateRoute("home") },
                testTag = "drawer_nav_home"
            )
            DrawerNavRow(
                icon = Icons.Default.Explore,
                label = "Explore Services",
                selected = uiState.currentScreenRoute == "explore",
                onClick = { onNavigateRoute("explore") },
                testTag = "drawer_nav_explore"
            )
            DrawerNavRow(
                icon = Icons.AutoMirrored.Filled.Assignment,
                label = "Bookings",
                selected = uiState.currentScreenRoute in listOf("bookings", "requests"),
                onClick = { onNavigateRoute("bookings") },
                testTag = "drawer_nav_bookings"
            )
            DrawerNavRow(
                icon = Icons.AutoMirrored.Filled.Chat,
                label = "Messages & Alerts",
                selected = uiState.currentScreenRoute == "messages",
                onClick = { onNavigateRoute("messages") },
                testTag = "drawer_nav_messages"
            )
            DrawerNavRow(
                icon = Icons.Default.Person,
                label = "My Profile",
                selected = uiState.currentScreenRoute == "profile",
                onClick = { onNavigateRoute("profile") },
                testTag = "drawer_nav_profile"
            )

            // AI & Location Quick Actions
            DrawerNavRow(
                icon = Icons.Default.AutoAwesome,
                label = "Ask Gemini AI Assistant",
                selected = false,
                tint = CyanBrand,
                onClick = onOpenAiAssistant,
                testTag = "drawer_nav_ai_assistant"
            )
            DrawerNavRow(
                icon = Icons.Default.LocationOn,
                label = "Location: ${uiState.selectedCity ?: "All India"}",
                selected = false,
                tint = GreenBrand,
                onClick = onOpenLocationDialog,
                testTag = "drawer_nav_location"
            )

            HorizontalDivider(color = NavyCardBorder)

            Text(
                text = "PLATFORM & RESOURCES",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            DrawerNavRow(
                icon = Icons.Default.WorkOutline,
                label = "Become a Provider",
                selected = uiState.currentScreenRoute == "become_provider",
                onClick = { onNavigateRoute("become_provider") },
                testTag = "drawer_nav_become_provider"
            )
            DrawerNavRow(
                icon = Icons.Default.Verified,
                label = "Provider Verification",
                selected = uiState.currentScreenRoute == "provider_verification",
                onClick = { onNavigateRoute("provider_verification") },
                testTag = "drawer_nav_provider_verification"
            )
            DrawerNavRow(
                icon = Icons.Default.WorkspacePremium,
                label = "Subscriptions",
                selected = uiState.currentScreenRoute == "subscriptions",
                onClick = { onNavigateRoute("subscriptions") },
                testTag = "drawer_nav_subscriptions"
            )
            DrawerNavRow(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                label = "Help Centre",
                selected = uiState.currentScreenRoute == "help_centre",
                onClick = { onNavigateRoute("help_centre") },
                testTag = "drawer_nav_help_centre"
            )
            DrawerNavRow(
                icon = Icons.Default.Gavel,
                label = "Legal Policies",
                selected = uiState.currentScreenRoute == "legal_policies",
                onClick = { onNavigateRoute("legal_policies") },
                testTag = "drawer_nav_legal_policies"
            )

            HorizontalDivider(color = NavyCardBorder)

            // Contact & Official Community Info
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Official Community: ${SkillConnectBranding.OFFICIAL_COMMUNITY}",
                    color = CyanBrand,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Official Email: ${SkillConnectBranding.OFFICIAL_EMAIL}",
                    color = GreenBrand,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = SkillConnectBranding.COPYRIGHT,
                    color = TextDim,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = onLogout,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drawer_logout_button")
            ) {
                Text("Sign Out", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DrawerNavRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    tint: Color = if (selected) CyanBrand else TextLight,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) CyanBrand.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = if (selected) CyanBrand else TextWhite,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun AdminPublicMarketplaceScreen(
    uiState: MarketplaceUiState,
    currentSubRoute: String,
    onSubRouteChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onCategoryFilter: (String?) -> Unit,
    onCityFilter: (String?) -> Unit,
    onProviderClick: (com.example.model.ServiceProvider) -> Unit,
    onExploreAllClick: () -> Unit,
    onOpenChat: (String, String) -> Unit,
    onLeaveReview: (String, String) -> Unit,
    onRefreshMarketplace: () -> Unit = {},
    onOpenAiAssistant: () -> Unit = {},
    onNavigateFooterRoute: (String) -> Unit = {}
) {
    val activeSection = when (currentSubRoute) {
        "explore" -> "explore"
        "requests", "bookings" -> "requests"
        else -> "marketplace"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_public_marketplace_screen")
    ) {
        Surface(
            color = NavySurface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdminMarketplaceSectionPill(
                    label = "Marketplace Home",
                    icon = Icons.Default.Storefront,
                    selected = activeSection == "marketplace",
                    onClick = { onSubRouteChange("marketplace") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_market_subtab_home")
                )
                AdminMarketplaceSectionPill(
                    label = "Directory (${uiState.filteredProviders.size})",
                    icon = Icons.Default.Explore,
                    selected = activeSection == "explore",
                    onClick = { onSubRouteChange("explore") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_market_subtab_explore")
                )
                AdminMarketplaceSectionPill(
                    label = "Bookings (${uiState.workRequests.size})",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    selected = activeSection == "requests",
                    onClick = { onSubRouteChange("requests") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_market_subtab_requests")
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (activeSection) {
                "explore" -> MarketplaceExploreScreen(
                    uiState = uiState,
                    onSearchChange = onSearchChange,
                    onCategoryFilter = onCategoryFilter,
                    onCityFilter = onCityFilter,
                    onProviderClick = onProviderClick,
                    onRefreshMarketplace = onRefreshMarketplace,
                    onNavigateRoute = onNavigateFooterRoute
                )
                "requests" -> CustomerRequestsScreen(
                    requests = uiState.workRequests,
                    currentCustomerId = uiState.currentUser.id,
                    onOpenChat = onOpenChat,
                    onLeaveReview = onLeaveReview,
                    onNavigateRoute = onNavigateFooterRoute
                )
                else -> CustomerHomeScreen(
                    uiState = uiState,
                    onSearchChange = onSearchChange,
                    onCategoryClick = onCategoryClick,
                    onProviderClick = onProviderClick,
                    onExploreAllClick = onExploreAllClick,
                    onRefreshMarketplace = onRefreshMarketplace,
                    onOpenAiAssistant = onOpenAiAssistant,
                    onNavigateRoute = onNavigateFooterRoute
                )
            }
        }
    }
}

@Composable
private fun AdminMarketplaceSectionPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) CyanBrand.copy(alpha = 0.18f) else NavySurfaceVariant
    val borderColor = if (selected) CyanBrand else NavyCardBorder
    val contentColor = if (selected) CyanBrand else TextLight

    Row(
        modifier = modifier
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun RoleAwareBottomNavigation(
    currentRole: UserRole,
    currentAdminRole: AdminRoleType? = null,
    currentRoute: String,
    unreadCount: Int = 0,
    onRouteSelect: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(10f)
            .navigationBarsPadding()
            .testTag("bottom_navigation_bar")
    ) {
        when (currentRole) {
            UserRole.CUSTOMER -> {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF001F2B),
                    selectedTextColor = CyanBrand,
                    indicatorColor = CyanBrand,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )

                // 1. Home – Main marketplace homepage
                NavigationBarItem(
                    selected = currentRoute == "home",
                    onClick = { onRouteSelect("home") },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.testTag("customer_nav_home"),
                    colors = navItemColors
                )

                // 2. Explore – Browse skills, categories and providers
                NavigationBarItem(
                    selected = currentRoute == "explore",
                    onClick = { onRouteSelect("explore") },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                    label = { Text("Explore", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.testTag("customer_nav_explore"),
                    colors = navItemColors
                )

                // 3. Bookings – Upcoming, completed and cancelled bookings
                NavigationBarItem(
                    selected = currentRoute in listOf("bookings", "requests"),
                    onClick = { onRouteSelect("bookings") },
                    icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Bookings") },
                    label = { Text("Bookings", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.testTag("customer_nav_bookings"),
                    colors = navItemColors
                )

                // 4. Messages – Customer-provider communication and notifications
                NavigationBarItem(
                    selected = currentRoute == "messages",
                    onClick = { onRouteSelect("messages") },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(containerColor = ErrorRed, contentColor = Color.White) {
                                        Text(unreadCount.coerceAtMost(9).toString(), fontSize = 8.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Messages")
                        }
                    },
                    label = { Text("Messages", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.testTag("customer_nav_messages"),
                    colors = navItemColors
                )

                // 5. Profile – User information, profile editing and settings
                NavigationBarItem(
                    selected = currentRoute in listOf("profile", "edit_profile", "preview_profile"),
                    onClick = { onRouteSelect("profile") },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.testTag("customer_nav_profile"),
                    colors = navItemColors
                )
            }

            UserRole.SERVICE_PROVIDER -> {
                val providerColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF00210E),
                    selectedTextColor = GreenBrand,
                    indicatorColor = GreenBrand,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )

                // 1. Studio / Home
                NavigationBarItem(
                    selected = currentRoute in listOf("dashboard", "home"),
                    onClick = { onRouteSelect("dashboard") },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.testTag("provider_nav_studio"),
                    colors = providerColors
                )

                // 2. Explore
                NavigationBarItem(
                    selected = currentRoute == "explore",
                    onClick = { onRouteSelect("explore") },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                    label = { Text("Explore", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.testTag("provider_nav_market"),
                    colors = providerColors
                )

                // 3. Bookings
                NavigationBarItem(
                    selected = currentRoute in listOf("bookings", "requests"),
                    onClick = { onRouteSelect("bookings") },
                    icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Bookings") },
                    label = { Text("Bookings", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.testTag("provider_nav_bookings"),
                    colors = providerColors
                )

                // 4. Messages
                NavigationBarItem(
                    selected = currentRoute == "messages",
                    onClick = { onRouteSelect("messages") },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Messages") },
                    label = { Text("Messages", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.testTag("provider_nav_messages"),
                    colors = providerColors
                )

                // 5. Profile
                NavigationBarItem(
                    selected = currentRoute in listOf("profile", "edit_profile", "preview_profile"),
                    onClick = { onRouteSelect("profile") },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.testTag("provider_nav_profile"),
                    colors = providerColors
                )
            }

            UserRole.ADMIN -> {
                when (currentAdminRole) {
                    AdminRoleType.FOUNDER -> {
                        val isFounderDashboardSelected = currentRoute in listOf("admin", "dashboard", "founder")
                        val isMarketplaceSelected = currentRoute in listOf("marketplace", "explore", "home", "requests", "bookings")
                        val isAuditLogsSelected = currentRoute == "audit_logs"
                        val isProfileSelected = currentRoute in listOf("profile", "edit_profile", "preview_profile")

                        val founderTabColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF001F2B),
                            selectedTextColor = CyanBrand,
                            indicatorColor = CyanBrand,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )

                        NavigationBarItem(
                            selected = isFounderDashboardSelected,
                            onClick = { onRouteSelect("admin") },
                            icon = { Icon(Icons.Default.Shield, contentDescription = "Founder") },
                            label = { Text("Founder", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("founder_nav_dashboard"),
                            colors = founderTabColors
                        )
                        NavigationBarItem(
                            selected = isMarketplaceSelected,
                            onClick = { onRouteSelect("marketplace") },
                            icon = { Icon(Icons.Default.Explore, contentDescription = "Marketplace") },
                            label = { Text("Marketplace", fontSize = 11.sp) },
                            modifier = Modifier.testTag("admin_nav_marketplace"),
                            colors = founderTabColors
                        )
                        NavigationBarItem(
                            selected = isAuditLogsSelected,
                            onClick = { onRouteSelect("audit_logs") },
                            icon = { Icon(Icons.Default.History, contentDescription = "Audit Logs") },
                            label = { Text("Audit Logs", fontSize = 11.sp) },
                            modifier = Modifier.testTag("founder_nav_audit"),
                            colors = founderTabColors
                        )
                        NavigationBarItem(
                            selected = isProfileSelected,
                            onClick = { onRouteSelect("profile") },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile", fontSize = 11.sp) },
                            modifier = Modifier.testTag("admin_nav_profile"),
                            colors = founderTabColors
                        )
                    }

                    AdminRoleType.CEO -> {
                        val isCeoDashboardSelected = currentRoute in listOf("admin", "dashboard", "ceo_dashboard")
                        val isMarketplaceSelected = currentRoute in listOf("marketplace", "explore", "home", "requests", "bookings")
                        val isDisputesSelected = currentRoute == "complaints"
                        val isProfileSelected = currentRoute in listOf("profile", "edit_profile", "preview_profile")

                        val ceoTabColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00210E),
                            selectedTextColor = GreenBrand,
                            indicatorColor = GreenBrand,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )

                        NavigationBarItem(
                            selected = isCeoDashboardSelected,
                            onClick = { onRouteSelect("admin") },
                            icon = { Icon(Icons.Default.BusinessCenter, contentDescription = "CEO Desk") },
                            label = { Text("CEO Desk", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("ceo_nav_dashboard"),
                            colors = ceoTabColors
                        )
                        NavigationBarItem(
                            selected = isMarketplaceSelected,
                            onClick = { onRouteSelect("marketplace") },
                            icon = { Icon(Icons.Default.Explore, contentDescription = "Marketplace") },
                            label = { Text("Marketplace", fontSize = 11.sp) },
                            modifier = Modifier.testTag("admin_nav_marketplace"),
                            colors = ceoTabColors
                        )
                        NavigationBarItem(
                            selected = isDisputesSelected,
                            onClick = { onRouteSelect("complaints") },
                            icon = { Icon(Icons.Default.ReportProblem, contentDescription = "Disputes") },
                            label = { Text("Disputes", fontSize = 11.sp) },
                            modifier = Modifier.testTag("ceo_nav_disputes"),
                            colors = ceoTabColors
                        )
                        NavigationBarItem(
                            selected = isProfileSelected,
                            onClick = { onRouteSelect("profile") },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile", fontSize = 11.sp) },
                            modifier = Modifier.testTag("admin_nav_profile"),
                            colors = ceoTabColors
                        )
                    }

                    else -> {
                        val isAdminTabSelected = currentRoute in listOf("admin", "dashboard")
                        val isMarketplaceTabSelected = currentRoute in listOf("marketplace", "explore", "home", "requests", "bookings")
                        val isProfileTabSelected = currentRoute in listOf("profile", "edit_profile", "preview_profile")

                        val adminTabColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = BlueBrand,
                            indicatorColor = BlueBrand,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )

                        NavigationBarItem(
                            selected = isAdminTabSelected,
                            onClick = { onRouteSelect("admin") },
                            icon = { Icon(Icons.Default.SupportAgent, contentDescription = "Support Desk") },
                            label = { Text("Support", fontSize = 11.sp) },
                            modifier = Modifier.testTag("admin_nav_admin"),
                            colors = adminTabColors
                        )
                        NavigationBarItem(
                            selected = isMarketplaceTabSelected,
                            onClick = { onRouteSelect("marketplace") },
                            icon = { Icon(Icons.Default.Explore, contentDescription = "Marketplace") },
                            label = { Text("Marketplace", fontSize = 11.sp) },
                            modifier = Modifier.testTag("admin_nav_marketplace"),
                            colors = adminTabColors
                        )
                        NavigationBarItem(
                            selected = isProfileTabSelected,
                            onClick = { onRouteSelect("profile") },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile", fontSize = 11.sp) },
                            modifier = Modifier.testTag("admin_nav_profile"),
                            colors = adminTabColors
                        )
                    }
                }
            }
        }
    }
}
