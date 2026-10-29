package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.SkillConnectRepository
import com.example.ui.AppDestination
import com.example.ui.SkillConnectViewModel
import com.example.ui.UiState
import com.example.data.model.IndiaLocationDirectory
import com.example.ui.components.AllIndiaLocationPickerDialog
import com.example.ui.components.SkillConnectAuthScreen
import com.example.ui.components.SkillConnectBrandLogo
import com.example.ui.components.attemptAutoSignIn
import com.example.ui.components.performSignOut
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.BookingsAndEnquiriesScreen
import com.example.ui.screens.CreateEditServiceScreen
import com.example.ui.screens.CustomerDashboardScreen
import com.example.ui.screens.ExploreServicesScreen
import com.example.ui.screens.FavouritesAndCompareScreen
import com.example.ui.screens.HelpCentreAndLegalScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MessagesAndNotificationsScreen
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.screens.ProviderDashboardScreen
import com.example.ui.screens.ProviderDirectoryScreen
import com.example.ui.screens.ProviderOnboardingWizardScreen
import com.example.ui.screens.ProviderPublicProfileScreen
import com.example.ui.screens.ProviderVerificationScreen
import com.example.ui.screens.ServiceDetailScreen
import com.example.ui.screens.SubscriptionsScreen
import com.example.ui.theme.SkillBlue
import com.example.ui.theme.SkillConnectTheme
import com.example.ui.theme.SkillCyan
import com.example.ui.theme.SkillErrorRed
import com.example.ui.theme.SkillNavy
import com.example.ui.theme.SkillPurple
import com.example.ui.theme.SkillSuccessGreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkillConnectRootApp()
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun SkillConnectRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsRepo = remember { UserPreferencesRepository(context.applicationContext) }
    val prefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(initialValue = UserPreferences())

    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    // Attempt silent auto sign-in once on startup if unauthenticated
    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            val credentialManager = CredentialManager.create(context)
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val isHindi = prefs.language == "hi"
    var pendingExecutiveLogin by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    SkillConnectTheme(
        darkTheme = prefs.isDarkMode,
        isHindi = isHindi
    ) {
        val user = currentUser
        if (user == null) {
            SkillConnectAuthScreen(
                isDarkMode = prefs.isDarkMode,
                isHindi = isHindi,
                onToggleDarkMode = {
                    scope.launch { prefsRepo.setDarkMode(!prefs.isDarkMode) }
                },
                onToggleLanguage = {
                    scope.launch { prefsRepo.setLanguage(if (isHindi) "en" else "hi") }
                },
                onRoleSelectedBeforeAuth = { selectedRole ->
                    scope.launch {
                        prefsRepo.setPendingRegistrationRole(selectedRole)
                    }
                },
                onAuthSuccess = { role, city, locality, lang ->
                    scope.launch {
                        if (role.isNotBlank()) {
                            prefsRepo.setPendingRegistrationRole(role)
                        }
                        prefsRepo.setLocation(city, locality)
                        prefsRepo.setLanguage(lang)
                    }
                },
                onAdminAuthSuccess = { adminRole, adminUsername, adminPassword ->
                    pendingExecutiveLogin = Triple(adminRole, adminUsername, adminPassword)
                }
            )
        } else {
            val viewModel: SkillConnectViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        SkillConnectViewModel(
                            repository = SkillConnectRepository(db),
                            preferencesRepository = UserPreferencesRepository(app),
                            currentUserId = user.uid,
                            currentUserEmail = user.email.orEmpty(),
                            currentUserDisplayName = user.displayName ?: "SkillConnect Member"
                        )
                    }
                }
            )

            LaunchedEffect(user.uid, prefs.pendingRegistrationRole) {
                val chosenRole = prefs.pendingRegistrationRole
                viewModel.ensureProfileInitialized(
                    requestedRole = chosenRole,
                    city = prefs.selectedCity.takeIf { it != "All India" } ?: "Bengaluru",
                    locality = prefs.selectedLocality,
                    preferredLanguage = prefs.language
                )
            }

            LaunchedEffect(user.uid, pendingExecutiveLogin) {
                val pending = pendingExecutiveLogin
                if (pending != null) {
                    pendingExecutiveLogin = null
                    viewModel.loginOrProvisionExecutiveAdmin(
                        requestedRole = pending.first,
                        usernameInput = pending.second,
                        passwordInput = pending.third
                    )
                }
            }

            SkillConnectMainShell(
                viewModel = viewModel,
                isHindi = isHindi,
                isDarkMode = prefs.isDarkMode
            )
        }
    }
}

@Composable
fun SkillConnectMainShell(
    viewModel: SkillConnectViewModel,
    isHindi: Boolean,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val currentDest by viewModel.currentDestination.collectAsStateWithLifecycle()
    val myProfileState by viewModel.myProfileState.collectAsStateWithLifecycle()
    val publicProvidersState by viewModel.publicProvidersState.collectAsStateWithLifecycle()
    val publishedServicesState by viewModel.publishedServicesState.collectAsStateWithLifecycle()
    val myProviderServicesState by viewModel.myProviderServicesState.collectAsStateWithLifecycle()
    val filteredServices by viewModel.filteredServices.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val customerBookingsState by viewModel.customerBookingsState.collectAsStateWithLifecycle()
    val providerBookingsState by viewModel.providerBookingsState.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val activeConversation by viewModel.activeConversation.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val notifications by viewModel.notificationsState.collectAsStateWithLifecycle()
    val publishedReviews by viewModel.publishedReviewsState.collectAsStateWithLifecycle()
    val favourites by viewModel.favouritesState.collectAsStateWithLifecycle()
    val myVerification by viewModel.myVerificationState.collectAsStateWithLifecycle()
    val myReports by viewModel.myReportsState.collectAsStateWithLifecycle()
    val mySupportTickets by viewModel.mySupportTicketsState.collectAsStateWithLifecycle()
    val myPayments by viewModel.myPaymentsState.collectAsStateWithLifecycle()
    val adminAllProfiles by viewModel.adminAllProfilesState.collectAsStateWithLifecycle()
    val adminAllVerifications by viewModel.adminAllVerificationsState.collectAsStateWithLifecycle()
    val adminAllBookings by viewModel.adminAllBookingsState.collectAsStateWithLifecycle()
    val adminAllPayments by viewModel.adminAllPaymentsState.collectAsStateWithLifecycle()
    val adminAllReports by viewModel.adminAllReportsState.collectAsStateWithLifecycle()
    val adminAllTickets by viewModel.adminAllTicketsState.collectAsStateWithLifecycle()
    val auditLogs by viewModel.adminAuditLogsState.collectAsStateWithLifecycle()
    val activeExecutiveRole by viewModel.activeExecutiveRole.collectAsStateWithLifecycle()
    val myAdminAccount by viewModel.myAdminAccountState.collectAsStateWithLifecycle()
    val allAdmins by viewModel.allAdminsState.collectAsStateWithLifecycle()
    val myDisputes by viewModel.myDisputesState.collectAsStateWithLifecycle()
    val adminAllDisputes by viewModel.adminAllDisputesState.collectAsStateWithLifecycle()
    val globalSettings by viewModel.globalSettingsState.collectAsStateWithLifecycle()
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val selectedServiceId by viewModel.selectedServiceId.collectAsStateWithLifecycle()
    val selectedProviderId by viewModel.selectedProviderId.collectAsStateWithLifecycle()
    val editingService by viewModel.editingService.collectAsStateWithLifecycle()
    val comparedServiceIds by viewModel.comparedServiceIds.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val errorBanner by viewModel.errorBanner.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onAppBackgrounded()
                Lifecycle.Event.ON_RESUME -> viewModel.onAppResumed()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val myProfile = (myProfileState as? UiState.Success)?.data
    val publishedServices = (publishedServicesState as? UiState.Success)?.data.orEmpty()
    val myProviderServices = (myProviderServicesState as? UiState.Success)?.data.orEmpty()
    val publicProviders = (publicProvidersState as? UiState.Success)?.data.orEmpty()
    val customerBookings = (customerBookingsState as? UiState.Success)?.data.orEmpty()
    val providerBookings = (providerBookingsState as? UiState.Success)?.data.orEmpty()
    val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
    val unreadNotifCount = notifications.count { !it.isRead }
    val isProviderAccount = myProfile?.role == "provider"
    val hasAdminAccess = viewModel.isAdminUser || activeExecutiveRole.isNotBlank()

    // Contextual Location & Notification Permission Dialog States
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    var showAllIndiaLocationPicker by remember { mutableStateOf(false) }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                IndiaLocationDirectory.detectCurrentIndiaLocation(context)
                    .onSuccess { resolved ->
                        viewModel.setLocation(
                            city = resolved.city,
                            locality = resolved.locality,
                            state = resolved.state,
                            pinCode = resolved.pinCode
                        )
                    }
                    .onFailure {
                        showAllIndiaLocationPicker = true
                    }
            }
        } else {
            viewModel.markLocationPromptDismissed()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.markNotificationPromptHandled(granted)
    }

    LaunchedEffect(myProfile?.userId, prefs.locationPermissionPrompted, prefs.notificationPermissionPrompted) {
        if (myProfile != null) {
            val hasLocationPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasLocationPerm && !prefs.locationPermissionPrompted) {
                showLocationPermissionDialog = true
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotifPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasNotifPerm && !prefs.notificationPermissionPrompted) {
                    showNotificationPermissionDialog = true
                }
            }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showDrawerLogoutConfirmDialog by remember { mutableStateOf(false) }

    // Mandatory BackHandler for drawer and state-switched sub-screens
    BackHandler(enabled = drawerState.isOpen || currentDest != AppDestination.HOME || activeConversation != null) {
        when {
            drawerState.isOpen -> {
                scope.launch { drawerState.close() }
            }
            activeConversation != null -> {
                viewModel.selectConversation(null)
            }
            else -> {
                viewModel.navigateBack()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SkillConnectNavigationDrawerSheet(
                isHindi = isHindi,
                isProviderAccount = isProviderAccount,
                isAdminUser = hasAdminAccess,
                activeExecutiveRole = activeExecutiveRole,
                userDisplayName = myProfile?.displayName?.ifBlank { viewModel.currentUserDisplayName }
                    ?: viewModel.currentUserDisplayName,
                userEmail = viewModel.currentUserEmail,
                platformId = myProfile?.effectivePlatformId.orEmpty(),
                selectedCity = prefs.selectedCity,
                selectedLocality = prefs.selectedLocality,
                currentDestination = currentDest,
                unreadNotifCount = unreadNotifCount,
                onSelectDestination = { dest ->
                    scope.launch { drawerState.close() }
                    viewModel.selectConversation(null)
                    viewModel.navigateTo(dest)
                },
                onOpenLocationPicker = {
                    scope.launch { drawerState.close() }
                    showAllIndiaLocationPicker = true
                },
                onRequestLogout = {
                    scope.launch { drawerState.close() }
                    showDrawerLogoutConfirmDialog = true
                }
            )
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 720.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    GlobalTopNavigationHeader(
                        isWideScreen = isWideScreen,
                        isHindi = isHindi,
                        isDarkMode = isDarkMode,
                        isProviderAccount = isProviderAccount,
                        currentDestination = currentDest,
                        unreadNotifCount = unreadNotifCount,
                        onOpenDrawerClick = {
                            scope.launch {
                                if (drawerState.isOpen) drawerState.close() else drawerState.open()
                            }
                        },
                        onRequestLocationClick = {
                            showAllIndiaLocationPicker = true
                        },
                        onNavigate = { dest ->
                            viewModel.selectConversation(null)
                            viewModel.navigateTo(dest)
                        },
                        onToggleLanguage = {
                            viewModel.setLanguage(if (isHindi) "en" else "hi")
                        },
                        onToggleDarkMode = {
                            viewModel.setDarkMode(!isDarkMode)
                        }
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        AuthenticatedMobileBottomBar(
                            isHindi = isHindi,
                            isProviderAccount = isProviderAccount,
                            currentDestination = currentDest,
                            unreadNotifCount = unreadNotifCount,
                            onNavigate = { dest ->
                                viewModel.selectConversation(null)
                                viewModel.navigateTo(dest)
                            }
                        )
                    }
                }
            ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        NavigationRailItem(
                            selected = currentDest == AppDestination.HOME,
                            onClick = { viewModel.navigateTo(AppDestination.HOME) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") }
                        )
                        if (isProviderAccount) {
                            NavigationRailItem(
                                selected = currentDest == AppDestination.PROVIDER_DASHBOARD,
                                onClick = { viewModel.navigateTo(AppDestination.PROVIDER_DASHBOARD) },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Workspace") },
                                label = { Text("Workspace") }
                            )
                            NavigationRailItem(
                                selected = currentDest == AppDestination.PROVIDER_ONBOARDING_WIZARD,
                                onClick = { viewModel.navigateTo(AppDestination.PROVIDER_ONBOARDING_WIZARD) },
                                icon = { Icon(Icons.Default.VerifiedUser, contentDescription = "Onboarding") },
                                label = { Text("Wizard") }
                            )
                        } else {
                            NavigationRailItem(
                                selected = currentDest == AppDestination.EXPLORE,
                                onClick = { viewModel.navigateTo(AppDestination.EXPLORE) },
                                icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                                label = { Text("Explore") }
                            )
                            NavigationRailItem(
                                selected = currentDest == AppDestination.PROVIDER_DIRECTORY,
                                onClick = { viewModel.navigateTo(AppDestination.PROVIDER_DIRECTORY) },
                                icon = { Icon(Icons.Default.Groups, contentDescription = "Providers") },
                                label = { Text("Directory") }
                            )
                        }
                        NavigationRailItem(
                            selected = currentDest == AppDestination.BOOKINGS,
                            onClick = { viewModel.navigateTo(AppDestination.BOOKINGS) },
                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Bookings") },
                            label = { Text("Bookings") }
                        )
                        NavigationRailItem(
                            selected = currentDest == AppDestination.MESSAGES,
                            onClick = { viewModel.navigateTo(AppDestination.MESSAGES) },
                            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Messages") },
                            label = { Text("Messages") }
                        )
                        if (!isProviderAccount) {
                            NavigationRailItem(
                                selected = currentDest == AppDestination.FAVOURITES,
                                onClick = { viewModel.navigateTo(AppDestination.FAVOURITES) },
                                icon = { Icon(Icons.Default.Bookmark, contentDescription = "Saved") },
                                label = { Text("Saved") }
                            )
                        }
                        NavigationRailItem(
                            selected = currentDest == AppDestination.PROFILE_SETTINGS,
                            onClick = { viewModel.navigateTo(AppDestination.PROFILE_SETTINGS) },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile") }
                        )
                        if (hasAdminAccess) {
                            NavigationRailItem(
                                selected = currentDest == AppDestination.ADMIN_DASHBOARD,
                                onClick = { viewModel.navigateTo(AppDestination.ADMIN_DASHBOARD) },
                                icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
                                label = { Text("Admin") }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // Feedback Banner
                    if (statusBanner != null || errorBanner != null) {
                        val isErr = errorBanner != null
                        Surface(
                            color = if (isErr) SkillErrorRed else SkillSuccessGreen,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = errorBanner ?: statusBanner.orEmpty(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearBanners() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White)
                                }
                            }
                        }
                    }

                    // Active Screen Content
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (currentDest) {
                            AppDestination.HOME -> HomeScreen(
                                isHindi = isHindi,
                                globalSettings = globalSettings,
                                publishedServicesState = publishedServicesState,
                                publicProvidersState = publicProvidersState,
                                favourites = favourites,
                                selectedCity = prefs.selectedCity,
                                appliedJobIds = prefs.appliedJobIds,
                                enrolledCourseIds = prefs.enrolledCourseIds,
                                onToggleJobApplication = { job ->
                                    viewModel.toggleJobApplication(job)
                                },
                                onToggleCourseEnrollment = { course ->
                                    viewModel.toggleCourseEnrollment(course)
                                },
                                onSearchTriggered = { kw, city ->
                                    viewModel.updateFilter {
                                        it.copy(keyword = kw, selectedCity = city)
                                    }
                                    viewModel.navigateTo(AppDestination.EXPLORE)
                                },
                                onCategorySelected = { catId ->
                                    viewModel.updateFilter {
                                        it.copy(selectedCategory = catId)
                                    }
                                    viewModel.navigateTo(AppDestination.EXPLORE)
                                },
                                onOpenServiceDetail = { viewModel.openServiceDetail(it) },
                                onOpenProviderProfile = { viewModel.openProviderProfile(it) },
                                onNavigate = { viewModel.navigateTo(it) },
                                onToggleFavourite = { id, type, title, sub, loc, price ->
                                    viewModel.toggleFavouriteItem(id, type, title, sub, loc, price)
                                }
                            )

                            AppDestination.EXPLORE -> ExploreServicesScreen(
                                isHindi = isHindi,
                                filterState = filterState,
                                publishedServicesState = publishedServicesState,
                                filteredServices = filteredServices,
                                favourites = favourites,
                                appliedJobIds = prefs.appliedJobIds,
                                enrolledCourseIds = prefs.enrolledCourseIds,
                                onToggleJobApplication = { job ->
                                    viewModel.toggleJobApplication(job)
                                },
                                onToggleCourseEnrollment = { course ->
                                    viewModel.toggleCourseEnrollment(course)
                                },
                                onUpdateFilter = { viewModel.updateFilter(it) },
                                onClearFilters = { viewModel.clearAllFilters() },
                                onLoadMore = { viewModel.loadMoreServices() },
                                onOpenServiceDetail = { viewModel.openServiceDetail(it) },
                                onOpenProviderProfile = { viewModel.openProviderProfile(it) },
                                onToggleFavourite = { id, type, title, sub, loc, price ->
                                    viewModel.toggleFavouriteItem(id, type, title, sub, loc, price)
                                },
                                onOpenCreateService = {
                                    if (isProviderAccount) {
                                        viewModel.openCreateOrEditService(null)
                                    } else {
                                        viewModel.navigateTo(AppDestination.PROVIDER_DIRECTORY)
                                    }
                                }
                            )

                            AppDestination.PROVIDER_DIRECTORY -> ProviderDirectoryScreen(
                                isHindi = isHindi,
                                publicProvidersState = publicProvidersState,
                                favourites = favourites,
                                onOpenProviderProfile = { viewModel.openProviderProfile(it) },
                                onMessageProvider = { provId, provName ->
                                    viewModel.openConversationWithProvider(
                                        customerId = viewModel.currentUserId,
                                        customerName = myProfile?.displayName ?: viewModel.currentUserDisplayName,
                                        providerId = provId,
                                        providerName = provName
                                    )
                                },
                                onToggleFavourite = { id, type, title, sub, loc, price ->
                                    viewModel.toggleFavouriteItem(id, type, title, sub, loc, price)
                                }
                            )

                            AppDestination.SERVICE_DETAIL -> {
                                val srv = (publishedServices + myProviderServices).firstOrNull { it.id == selectedServiceId }
                                val prov = publicProviders.firstOrNull { it.userId == srv?.providerId }
                                ServiceDetailScreen(
                                    service = srv,
                                    allServices = publishedServices,
                                    providerProfile = prov,
                                    reviews = publishedReviews,
                                    favourites = favourites,
                                    onBack = { viewModel.navigateBack() },
                                    onOpenProviderProfile = { viewModel.openProviderProfile(it) },
                                    onOpenServiceDetail = { viewModel.openServiceDetail(it) },
                                    onSubmitRequest = { service, reqs, date, timeSlot, note, type, paymentMethod ->
                                        viewModel.submitBookingOrQuoteRequest(
                                            service = service,
                                            requirements = reqs,
                                            preferredDate = date,
                                            attachmentNote = note,
                                            requestType = type,
                                            preferredTimeSlot = timeSlot,
                                            paymentMethod = paymentMethod
                                        )
                                    },
                                    onToggleFavourite = { id, type, title, sub, loc, price ->
                                        viewModel.toggleFavouriteItem(id, type, title, sub, loc, price)
                                    },
                                    onMessageProvider = { provId, provName, srvTitle ->
                                        viewModel.openConversationWithProvider(
                                            customerId = viewModel.currentUserId,
                                            customerName = myProfile?.displayName ?: viewModel.currentUserDisplayName,
                                            providerId = provId,
                                            providerName = provName,
                                            serviceTitle = srvTitle
                                        )
                                    },
                                    onReportListing = { id, type, reason, details ->
                                        viewModel.submitSafetyReport(id, type, reason, details)
                                    }
                                )
                            }

                            AppDestination.PROVIDER_PROFILE -> {
                                val prov = publicProviders.firstOrNull { it.userId == selectedProviderId }
                                    ?: if (selectedProviderId == viewModel.currentUserId) myProfile else null
                                val provServices = publishedServices.filter { it.providerId == selectedProviderId }
                                ProviderPublicProfileScreen(
                                    provider = prov,
                                    providerServices = provServices,
                                    reviews = publishedReviews,
                                    favourites = favourites,
                                    onBack = { viewModel.navigateBack() },
                                    onOpenServiceDetail = { viewModel.openServiceDetail(it) },
                                    onMessageProvider = { provId, provName ->
                                        viewModel.openConversationWithProvider(
                                            customerId = viewModel.currentUserId,
                                            customerName = myProfile?.displayName ?: viewModel.currentUserDisplayName,
                                            providerId = provId,
                                            providerName = provName
                                        )
                                    },
                                    onToggleFavourite = { id, type, title, sub, loc, price ->
                                        viewModel.toggleFavouriteItem(id, type, title, sub, loc, price)
                                    },
                                    onReportProfile = { id, type, reason, details ->
                                        viewModel.submitSafetyReport(id, type, reason, details)
                                    }
                                )
                            }

                            AppDestination.CUSTOMER_DASHBOARD -> {
                                val recentServices = prefs.recentlyViewedIds.mapNotNull { id ->
                                    publishedServices.firstOrNull { it.id == id }
                                }
                                CustomerDashboardScreen(
                                    profile = myProfile,
                                    fallbackName = viewModel.currentUserDisplayName,
                                    customerBookings = customerBookings,
                                    savedItems = favourites,
                                    recentlyViewedServices = recentServices,
                                    recommendedServices = publishedServices.take(4),
                                    notifications = notifications,
                                    onOpenServiceDetail = { viewModel.openServiceDetail(it) },
                                    onOpenProviderProfile = { viewModel.openProviderProfile(it) },
                                    onCancelBooking = { bk, reason ->
                                        viewModel.updateCustomerBookingStatus(bk, "cancelled", reason)
                                    },
                                    onContactProvider = { bk ->
                                        viewModel.openConversationWithProvider(
                                            customerId = bk.customerId,
                                            customerName = bk.customerName,
                                            providerId = bk.providerId,
                                            providerName = bk.providerName,
                                            bookingId = bk.id,
                                            serviceTitle = bk.serviceTitle
                                        )
                                    },
                                    onSubmitReview = { bk, rating, comment ->
                                        viewModel.submitCompletedJobReview(bk, rating, comment)
                                    },
                                    onPayBooking = { bk, method ->
                                        viewModel.processBookingPayment(bk, method, "")
                                    },
                                    onRequestRefund = { bk, reason ->
                                        viewModel.requestBookingRefund(bk, reason)
                                    },
                                    onNavigate = { viewModel.navigateTo(it) }
                                )
                            }

                            AppDestination.PROVIDER_DASHBOARD -> ProviderDashboardScreen(
                                profile = myProfile,
                                myServices = myProviderServices,
                                providerBookings = providerBookings,
                                reviews = publishedReviews,
                                providerPayments = myPayments,
                                notifications = notifications,
                                onAddNewService = { viewModel.openCreateOrEditService(null) },
                                onEditService = { viewModel.openCreateOrEditService(it) },
                                onDeleteService = { viewModel.deleteServiceListing(it) },
                                onProviderBookingAction = { bk, newStatus, note ->
                                    viewModel.updateProviderBookingStatus(bk, newStatus, note)
                                },
                                onContactCustomer = { bk ->
                                    viewModel.openConversationWithProvider(
                                        customerId = bk.customerId,
                                        customerName = bk.customerName,
                                        providerId = bk.providerId,
                                        providerName = bk.providerName,
                                        bookingId = bk.id,
                                        serviceTitle = bk.serviceTitle
                                    )
                                },
                                onOpenVerification = { viewModel.navigateTo(AppDestination.PROVIDER_VERIFICATION) },
                                onOpenSubscriptions = { viewModel.navigateTo(AppDestination.SUBSCRIPTIONS) },
                                onOpenOnboardingWizard = { viewModel.navigateTo(AppDestination.PROVIDER_ONBOARDING_WIZARD) },
                                onOpenPublicProfile = {
                                    viewModel.openProviderProfile(viewModel.currentUserId)
                                },
                                onRequestPayout = { amount, upiOrBank ->
                                    viewModel.requestProviderPayout(amount, upiOrBank)
                                },
                                onNavigate = { viewModel.navigateTo(it) }
                            )

                            AppDestination.PROVIDER_ONBOARDING_WIZARD -> ProviderOnboardingWizardScreen(
                                profile = myProfile,
                                userEmail = viewModel.currentUserEmail,
                                fallbackName = viewModel.currentUserDisplayName,
                                existingVerification = myVerification,
                                onSaveStepDraft = { updatedProfile, step ->
                                    viewModel.saveProviderWizardDraft(updatedProfile, step, resumeLater = false)
                                },
                                onCompleteWizard = { finalProfile, idDocType, idDocNumber, portfolioSummary, firstServiceTitle, firstServiceDesc ->
                                    viewModel.submitProviderOnboardingWizard(
                                        updatedProfile = finalProfile,
                                        legalName = finalProfile.displayName,
                                        documentReferenceNote = "$idDocType: $idDocNumber",
                                        firstServiceTitle = firstServiceTitle,
                                        firstServiceDesc = firstServiceDesc,
                                        firstServiceDeliverables = portfolioSummary,
                                        firstServiceTurnaround = "2–3 Days"
                                    )
                                },
                                onExitWizard = {
                                    viewModel.navigateTo(AppDestination.PROVIDER_DASHBOARD)
                                }
                            )

                            AppDestination.CREATE_EDIT_SERVICE -> CreateEditServiceScreen(
                                existingService = editingService,
                                defaultCity = myProfile?.city ?: "Bengaluru",
                                defaultLocality = myProfile?.locality.orEmpty(),
                                onCancel = { viewModel.navigateBack() },
                                onSaveService = { id, title, cat, sub, sum, desc, skills, price, model, deliv, turn, city, loc, avail, terms, pub ->
                                    viewModel.saveServiceListing(
                                        id, title, cat, sub, sum, desc, skills, price, model, deliv, turn, city, loc, avail, terms, pub
                                    )
                                }
                            )

                            AppDestination.BOOKINGS -> BookingsAndEnquiriesScreen(
                                activeRole = myProfile?.role ?: "customer",
                                customerBookings = customerBookings,
                                providerBookings = providerBookings,
                                onUpdateCustomerBooking = { bk, status, note ->
                                    viewModel.updateCustomerBookingStatus(bk, status, note)
                                },
                                onUpdateProviderBooking = { bk, status, note ->
                                    viewModel.updateProviderBookingStatus(bk, status, note)
                                },
                                onSubmitReview = { bk, rating, comment ->
                                    viewModel.submitCompletedJobReview(bk, rating, comment)
                                },
                                onPayBooking = { bk, method ->
                                    viewModel.processBookingPayment(bk, method, "")
                                },
                                onRequestRefund = { bk, reason ->
                                    viewModel.requestBookingRefund(bk, reason)
                                },
                                onMessageParty = { bk, _ ->
                                    viewModel.openConversationWithProvider(
                                        customerId = bk.customerId,
                                        customerName = bk.customerName,
                                        providerId = bk.providerId,
                                        providerName = bk.providerName,
                                        bookingId = bk.id,
                                        serviceTitle = bk.serviceTitle
                                    )
                                },
                                onExploreServices = { viewModel.navigateTo(AppDestination.EXPLORE) }
                            )

                            AppDestination.MESSAGES -> MessagesAndNotificationsScreen(
                                currentUserId = viewModel.currentUserId,
                                conversations = conversations,
                                activeConversation = activeConversation,
                                messages = activeMessages,
                                notifications = notifications,
                                onSelectConversation = { viewModel.selectConversation(it) },
                                onSendMessage = { viewModel.sendChatMessage(it) },
                                onMarkNotificationRead = { viewModel.markNotificationRead(it) },
                                onMarkAllNotificationsRead = { viewModel.markAllNotificationsRead() },
                                onToggleBlockConversation = { convId, _ ->
                                    val targetConv = conversations.firstOrNull { it.id == convId } ?: activeConversation
                                    if (targetConv != null) {
                                        viewModel.toggleBlockConversation(targetConv)
                                    }
                                },
                                onReportConversation = { id, reason, details ->
                                    viewModel.submitSafetyReport(id, "user", reason, details)
                                }
                            )

                            AppDestination.FAVOURITES -> FavouritesAndCompareScreen(
                                favourites = favourites,
                                allServices = publishedServices,
                                comparedServiceIds = comparedServiceIds,
                                onToggleCompare = { viewModel.toggleCompareService(it) },
                                onOpenServiceDetail = { viewModel.openServiceDetail(it) },
                                onOpenProviderProfile = { viewModel.openProviderProfile(it) },
                                onRemoveFavourite = { fav ->
                                    viewModel.toggleFavouriteItem(
                                        fav.targetId,
                                        fav.targetType,
                                        fav.title,
                                        fav.subtitle,
                                        fav.location,
                                        fav.price
                                    )
                                },
                                onExploreServices = { viewModel.navigateTo(AppDestination.EXPLORE) }
                            )

                            AppDestination.PROFILE_SETTINGS -> ProfileAndSettingsScreen(
                                profile = myProfile,
                                userEmail = viewModel.currentUserEmail,
                                fallbackName = viewModel.currentUserDisplayName,
                                preferences = prefs,
                                isAdminUser = hasAdminAccess,
                                activeExecutiveRole = activeExecutiveRole,
                                myAdminAccount = myAdminAccount,
                                myVerification = myVerification,
                                myBookings = if (isProviderAccount) providerBookings else customerBookings,
                                myConversations = conversations,
                                myPayments = myPayments,
                                myDisputes = myDisputes,
                                mySupportTickets = mySupportTickets,
                                onSaveProfile = { viewModel.saveUserProfile(it) },
                                onToggleDarkMode = { viewModel.setDarkMode(it) },
                                onChangeLanguage = { viewModel.setLanguage(it) },
                                onUpdateNotificationPrivacy = { emailNotif, showPhone, pushNotif ->
                                    viewModel.setNotificationAndPrivacy(emailNotif, showPhone, pushNotif)
                                },
                                onToggleJobApplication = { job ->
                                    viewModel.toggleJobApplication(job)
                                },
                                onToggleCourseEnrollment = { course ->
                                    viewModel.toggleCourseEnrollment(course)
                                },
                                onFileDispute = { booking, paymentId, reason, statement, evidence ->
                                    viewModel.fileBookingOrPaymentDispute(
                                        booking = booking,
                                        paymentId = paymentId,
                                        reason = reason,
                                        statement = statement,
                                        evidenceNote = evidence
                                    )
                                },
                                onLoginExecutiveAdmin = { role, username, password ->
                                    viewModel.loginOrProvisionExecutiveAdmin(role, username, password)
                                },
                                onLogoutExecutiveAdmin = {
                                    viewModel.logoutExecutiveAdminSession()
                                },
                                onRequestRoleChangeTicket = { targetRole, reason ->
                                    viewModel.requestRoleChangeTicket(
                                        currentRole = myProfile?.role ?: "customer",
                                        targetRole = targetRole,
                                        reason = reason
                                    )
                                },
                                onRequestDeletionTicket = {
                                    viewModel.submitSupportTicket(
                                        name = myProfile?.displayName ?: viewModel.currentUserDisplayName,
                                        email = viewModel.currentUserEmail,
                                        category = "Account Deletion Request",
                                        relatedBookingId = "",
                                        description = "Please process account deletion for UID ${viewModel.currentUserId}."
                                    )
                                    viewModel.performFullLogout {
                                        performSignOut(credentialManager, {}, scope)
                                    }
                                },
                                onRequestLocationPermission = {
                                    showLocationPermissionDialog = true
                                },
                                onRequestNotificationPermission = {
                                    showNotificationPermissionDialog = true
                                },
                                onNavigate = { viewModel.navigateTo(it) },
                                onSignOut = {
                                    viewModel.performFullLogout {
                                        performSignOut(credentialManager, {}, scope)
                                    }
                                }
                            )

                            AppDestination.PROVIDER_VERIFICATION -> ProviderVerificationScreen(
                                profile = myProfile,
                                submission = myVerification,
                                onSubmitVerification = { legal, biz, cat, city, exp, port, doc ->
                                    viewModel.submitProviderVerification(legal, biz, cat, city, exp, port, doc)
                                }
                            )

                            AppDestination.SUBSCRIPTIONS -> SubscriptionsScreen(
                                profile = myProfile,
                                globalSettings = globalSettings,
                                myPayments = myPayments,
                                onSubscribePlan = { planName, amountInr, method, utrReference, payerUpiNote ->
                                    val planKey = if (planName == "pro" || planName == "premium") "premium" else "free"
                                    viewModel.upgradeSubscriptionPlan(planKey, method, amountInr, utrReference, payerUpiNote)
                                }
                            )

                            AppDestination.ADMIN_DASHBOARD -> AdminDashboardScreen(
                                isAdminUser = hasAdminAccess,
                                activeExecutiveRole = activeExecutiveRole,
                                myAdminAccount = myAdminAccount,
                                allAdmins = allAdmins,
                                allDisputes = if (adminAllDisputes.isNotEmpty()) adminAllDisputes else myDisputes,
                                allUsers = adminAllProfiles,
                                allServices = publishedServices,
                                publicProviders = publicProviders,
                                allVerifications = adminAllVerifications,
                                allBookings = adminAllBookings,
                                allPayments = adminAllPayments,
                                reviews = publishedReviews,
                                reports = if (adminAllReports.isNotEmpty()) adminAllReports else myReports,
                                supportTickets = if (adminAllTickets.isNotEmpty()) adminAllTickets else mySupportTickets,
                                auditLogs = auditLogs,
                                globalSettings = globalSettings,
                                onSaveGlobalSettings = { text, active, freeLim, price, period, demo ->
                                    viewModel.adminSavePlatformSettings(text, active, freeLim, price, period, demo)
                                },
                                onApproveProvider = { uid, approve ->
                                    viewModel.adminApproveProvider(uid, approve)
                                },
                                onVerificationDecision = { uid, decision, reason ->
                                    val targetStatus = when (decision) {
                                        "approve" -> "approved"
                                        "reject" -> "rejected"
                                        "request_info" -> "additional_info_required"
                                        "suspend" -> "suspended"
                                        "revoke" -> "not_submitted"
                                        else -> "under_review"
                                    }
                                    viewModel.adminReviewProvider(uid, targetStatus, reason)
                                },
                                onModerateService = { srvId, pub, feat, status, reason ->
                                    viewModel.adminModerateListing(srvId, pub, feat, status, reason)
                                },
                                onSetUserAccountStatus = { uid, status, reason ->
                                    viewModel.adminReviewProvider(
                                        targetUserId = uid,
                                        newStatus = if (status == "suspended") "suspended" else "approved",
                                        reason = reason
                                    )
                                },
                                onProcessPaymentAction = { payment, newStatus, note ->
                                    viewModel.adminUpdatePaymentOrPayout(payment, newStatus, note)
                                },
                                onReviewSubscriptionPayment = { payment, approve, verificationNote ->
                                    viewModel.adminReviewSubscriptionPayment(payment, approve, verificationNote)
                                },
                                onResolveSafetyReport = { reportId, status, note ->
                                    val rep = (adminAllReports + myReports).firstOrNull { it.id == reportId }
                                    if (rep != null) {
                                        viewModel.adminResolveReport(rep, status, note)
                                    }
                                },
                                onRespondSupportTicket = { ticket, status, note, approveRoleChangeTo ->
                                    viewModel.adminResolveTicket(ticket, status)
                                    if (!approveRoleChangeTo.isNullOrBlank()) {
                                        val targetProf = (adminAllProfiles + publicProviders).firstOrNull { it.userId == ticket.userId }
                                        if (targetProf != null) {
                                            viewModel.adminChangeUserRole(targetProf, approveRoleChangeTo, note)
                                        }
                                    }
                                },
                                onHandleDispute = { dispute, newStatus, assignedRole, decisionNote ->
                                    viewModel.adminHandleDispute(dispute, newStatus, assignedRole, decisionNote)
                                },
                                onLoginExecutiveAdmin = { role, username, password ->
                                    viewModel.loginOrProvisionExecutiveAdmin(role, username, password)
                                },
                                onLogoutExecutiveAdmin = {
                                    viewModel.logoutExecutiveAdminSession()
                                },
                                onChangeAdminPassword = { currentPassword, newPassword ->
                                    viewModel.changeExecutiveAdminPassword(currentPassword, newPassword)
                                },
                                onFounderManageAdmin = { targetAdminUid, newRole, tempPassword ->
                                    viewModel.founderReassignOrResetAdmin(targetAdminUid, newRole, tempPassword)
                                }
                            )

                            AppDestination.HELP_AND_LEGAL -> HelpCentreAndLegalScreen(
                                isHindi = isHindi,
                                defaultName = myProfile?.displayName ?: viewModel.currentUserDisplayName,
                                defaultEmail = viewModel.currentUserEmail,
                                supportTickets = mySupportTickets,
                                onSubmitSupportTicket = { name, email, cat, bkId, desc ->
                                    viewModel.submitSupportTicket(name, email, cat, bkId, desc)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    }

    if (showDrawerLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDrawerLogoutConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Confirm Logout",
                    tint = SkillErrorRed,
                    modifier = Modifier.size(30.dp)
                )
            },
            title = {
                Text(
                    text = if (isHindi) "लॉगआउट की पुष्टि करें" else "Confirm Logout",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isHindi) {
                        "क्या आप अपने SkillConnect खाते से लॉगआउट कर पहले लॉगिन पेज पर जाना चाहते हैं?"
                    } else {
                        "Are you sure you want to log out of your SkillConnect account and return to the login screen?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDrawerLogoutConfirmDialog = false
                        viewModel.performFullLogout {
                            performSignOut(credentialManager, {}, scope)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillErrorRed),
                    modifier = Modifier.testTag("drawer_confirm_logout_button")
                ) {
                    Text(if (isHindi) "हां, लॉगआउट करें" else "Yes, Logout", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDrawerLogoutConfirmDialog = false },
                    modifier = Modifier.testTag("drawer_cancel_logout_button")
                ) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // Contextual Location Pre-Permission Dialog
    if (showLocationPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showLocationPermissionDialog = false
                viewModel.markLocationPromptDismissed()
            },
            icon = {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Location Access",
                    tint = SkillBlue,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Discover Local Professionals Near You",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Allow location access to discover professionals who serve your area. You can also enter your city manually."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationPermissionDialog = false
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                    modifier = Modifier.testTag("allow_location_permission_button")
                ) {
                    Text("Allow Location")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            showLocationPermissionDialog = false
                            viewModel.markLocationPromptDismissed()
                            showAllIndiaLocationPicker = true
                        },
                        modifier = Modifier.testTag("enter_location_manually_button")
                    ) {
                        Text("Enter Location Manually")
                    }
                    TextButton(
                        onClick = {
                            showLocationPermissionDialog = false
                            viewModel.markLocationPromptDismissed()
                        },
                        modifier = Modifier.testTag("location_not_now_button")
                    ) {
                        Text("Not Now")
                    }
                }
            }
        )
    }

    if (showAllIndiaLocationPicker) {
        AllIndiaLocationPickerDialog(
            initialCity = prefs.selectedCity,
            initialLocality = prefs.selectedLocality,
            initialState = prefs.selectedState,
            initialPinCode = prefs.selectedPinCode,
            allowAllIndia = true,
            isHindi = isHindi,
            onDismiss = { showAllIndiaLocationPicker = false },
            onLocationSelected = { stateName, city, locality, pinCode ->
                showAllIndiaLocationPicker = false
                viewModel.setLocation(
                    city = city,
                    locality = locality,
                    state = stateName,
                    pinCode = pinCode
                )
            }
        )
    }

    // Contextual Notification Pre-Permission Dialog
    if (showNotificationPermissionDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AlertDialog(
            onDismissRequest = {
                showNotificationPermissionDialog = false
                viewModel.markNotificationPromptHandled(false)
            },
            icon = {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = "Notification Access",
                    tint = SkillPurple,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Stay Updated on Bookings & Alerts",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Enable notifications to receive booking updates, new messages and verification status alerts."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationPermissionDialog = false
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                    modifier = Modifier.testTag("enable_notifications_permission_button")
                ) {
                    Text("Enable Notifications")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotificationPermissionDialog = false
                        viewModel.markNotificationPromptHandled(false)
                    },
                    modifier = Modifier.testTag("notifications_maybe_later_button")
                ) {
                    Text("Maybe Later")
                }
            }
        )
    }
}

@Composable
private fun GlobalTopNavigationHeader(
    isWideScreen: Boolean,
    isHindi: Boolean,
    isDarkMode: Boolean,
    isProviderAccount: Boolean,
    currentDestination: AppDestination,
    unreadNotifCount: Int,
    onOpenDrawerClick: () -> Unit,
    onRequestLocationClick: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleDarkMode: () -> Unit
) {
    Surface(
        color = SkillNavy,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onOpenDrawerClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("header_hamburger_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = if (isHindi) "नेविगेशन मेनू खोलें" else "Open Navigation Menu",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                SkillConnectBrandLogo(
                    compact = !isWideScreen,
                    onNavyHeader = true,
                    onClick = { onNavigate(AppDestination.HOME) }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onRequestLocationClick,
                    modifier = Modifier.testTag("header_location_button")
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Location Settings", tint = SkillCyan)
                }

                IconButton(
                    onClick = { onNavigate(AppDestination.EXPLORE) },
                    modifier = Modifier.testTag("header_search_shortcut")
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search Services", tint = Color.White)
                }

                IconButton(
                    onClick = { onNavigate(AppDestination.MESSAGES) },
                    modifier = Modifier.testTag("header_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifCount > 0) {
                                Badge { Text("$unreadNotifCount") }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                    }
                }

                IconButton(
                    onClick = onToggleLanguage,
                    modifier = Modifier.testTag("header_language_selector")
                ) {
                    Icon(Icons.Default.Language, contentDescription = "Switch Language", tint = Color.White)
                }

                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier.testTag("header_theme_switch")
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Switch Theme",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { onNavigate(AppDestination.PROFILE_SETTINGS) },
                    modifier = Modifier.testTag("header_profile_menu")
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isProviderAccount) SkillPurple else SkillBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile & Dashboards",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillConnectNavigationDrawerSheet(
    isHindi: Boolean,
    isProviderAccount: Boolean,
    isAdminUser: Boolean,
    activeExecutiveRole: String,
    userDisplayName: String,
    userEmail: String,
    platformId: String,
    selectedCity: String,
    selectedLocality: String,
    currentDestination: AppDestination,
    unreadNotifCount: Int,
    onSelectDestination: (AppDestination) -> Unit,
    onOpenLocationPicker: () -> Unit,
    onRequestLogout: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .width(310.dp)
            .testTag("skillconnect_navigation_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Drawer Header with Brand Identity & Active User Info
            Surface(
                color = SkillNavy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SkillConnectBrandLogo(
                        compact = false,
                        onNavyHeader = true,
                        onClick = { onSelectDestination(AppDestination.HOME) }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isProviderAccount) SkillPurple else SkillBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userDisplayName.ifBlank { "SkillConnect Member" },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            if (userEmail.isNotBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (isProviderAccount) SkillPurple.copy(alpha = 0.28f) else SkillCyan.copy(alpha = 0.24f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (isProviderAccount) {
                                    if (isHindi) "सर्विस प्रोवाइडर" else "SERVICE PROVIDER"
                                } else {
                                    if (isHindi) "ग्राहक (Customer)" else "CUSTOMER"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        if (platformId.isNotBlank()) {
                            Text(
                                text = platformId,
                                style = MaterialTheme.typography.labelSmall,
                                color = SkillCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenLocationPicker() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SkillCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (selectedLocality.isNotBlank() && selectedLocality != "All Localities") {
                                    "$selectedCity, $selectedLocality"
                                } else {
                                    selectedCity
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (isHindi) "बदलें" else "Change",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkillCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SECTION 1: Marketplace & Discovery
            DrawerSectionTitle(if (isHindi) "मार्केटप्लेस और खोज" else "MARKETPLACE & DISCOVERY")

            DrawerMenuItem(
                label = if (isHindi) "होम (Home)" else "Home",
                icon = Icons.Default.Home,
                selected = currentDestination == AppDestination.HOME,
                testTag = "drawer_item_home",
                onClick = { onSelectDestination(AppDestination.HOME) }
            )
            DrawerMenuItem(
                label = if (isHindi) "सेवाएं एक्सप्लोर करें" else "Explore Services",
                icon = Icons.Default.Explore,
                selected = currentDestination == AppDestination.EXPLORE,
                testTag = "drawer_item_explore_services",
                onClick = { onSelectDestination(AppDestination.EXPLORE) }
            )
            DrawerMenuItem(
                label = if (isHindi) "सेवाएं खोजें (Search)" else "Search Services",
                icon = Icons.Default.Search,
                selected = false,
                testTag = "drawer_item_search_services",
                onClick = { onSelectDestination(AppDestination.EXPLORE) }
            )
            DrawerMenuItem(
                label = if (isHindi) "प्रोवाइडर डायरेक्टरी" else "Provider Directory",
                icon = Icons.Default.Groups,
                selected = currentDestination == AppDestination.PROVIDER_DIRECTORY,
                testTag = "drawer_item_provider_directory",
                onClick = { onSelectDestination(AppDestination.PROVIDER_DIRECTORY) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // SECTION 2: Dashboards & Activity (Role-Specific)
            DrawerSectionTitle(if (isHindi) "डैशबोर्ड और गतिविधि" else "DASHBOARDS & ACTIVITY")

            if (!isProviderAccount || isAdminUser) {
                DrawerMenuItem(
                    label = if (isHindi) "ग्राहक डैशबोर्ड" else "Customer Dashboard",
                    icon = Icons.Default.Dashboard,
                    selected = currentDestination == AppDestination.CUSTOMER_DASHBOARD,
                    testTag = "drawer_item_customer_dashboard",
                    onClick = { onSelectDestination(AppDestination.CUSTOMER_DASHBOARD) }
                )
            }

            if (isProviderAccount || isAdminUser) {
                DrawerMenuItem(
                    label = if (isHindi) "सर्विस प्रोवाइडर डैशबोर्ड" else "Service Provider Dashboard",
                    icon = Icons.Default.Dashboard,
                    selected = currentDestination == AppDestination.PROVIDER_DASHBOARD,
                    testTag = "drawer_item_provider_dashboard",
                    onClick = { onSelectDestination(AppDestination.PROVIDER_DASHBOARD) }
                )
                DrawerMenuItem(
                    label = if (isHindi) "नई सेवा जोड़ें" else "Create / Add Service",
                    icon = Icons.Default.AddCircleOutline,
                    selected = currentDestination == AppDestination.CREATE_EDIT_SERVICE,
                    testTag = "drawer_item_create_service",
                    onClick = { onSelectDestination(AppDestination.CREATE_EDIT_SERVICE) }
                )
                DrawerMenuItem(
                    label = if (isHindi) "प्रोवाइडर ऑनबोर्डिंग और सत्यापन" else "Provider Onboarding & Verification",
                    icon = Icons.Default.VerifiedUser,
                    selected = currentDestination == AppDestination.PROVIDER_ONBOARDING_WIZARD ||
                        currentDestination == AppDestination.PROVIDER_VERIFICATION,
                    testTag = "drawer_item_provider_verification",
                    onClick = { onSelectDestination(AppDestination.PROVIDER_ONBOARDING_WIZARD) }
                )
            }

            DrawerMenuItem(
                label = if (isHindi) "बुकिंग्स और पूछताछ" else "Bookings & Enquiries",
                icon = Icons.Default.CalendarMonth,
                selected = currentDestination == AppDestination.BOOKINGS,
                testTag = "drawer_item_bookings",
                onClick = { onSelectDestination(AppDestination.BOOKINGS) }
            )

            DrawerMenuItem(
                label = if (isHindi) "नोटिफिकेशन्स और चैट" else "Notifications",
                icon = Icons.Default.Notifications,
                selected = currentDestination == AppDestination.MESSAGES,
                badgeCount = unreadNotifCount,
                testTag = "drawer_item_notifications",
                onClick = { onSelectDestination(AppDestination.MESSAGES) }
            )

            if (!isProviderAccount || isAdminUser) {
                DrawerMenuItem(
                    label = if (isHindi) "पसंदीदा सेवाएं" else "Saved Favourites",
                    icon = Icons.Default.Bookmark,
                    selected = currentDestination == AppDestination.FAVOURITES,
                    testTag = "drawer_item_favourites",
                    onClick = { onSelectDestination(AppDestination.FAVOURITES) }
                )
            }

            if (isAdminUser) {
                DrawerMenuItem(
                    label = "Executive Admin Portal (${activeExecutiveRole.ifBlank { "Admin" }.uppercase()})",
                    icon = Icons.Default.AdminPanelSettings,
                    selected = currentDestination == AppDestination.ADMIN_DASHBOARD,
                    testTag = "drawer_item_admin_portal",
                    onClick = { onSelectDestination(AppDestination.ADMIN_DASHBOARD) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // SECTION 3: Account, Billing & Support
            DrawerSectionTitle(if (isHindi) "खाता, बिलिंग और सहायता" else "ACCOUNT, BILLING & SUPPORT")

            DrawerMenuItem(
                label = if (isHindi) "मेरी प्रोफ़ाइल (My Profile)" else "My Profile",
                icon = Icons.Default.Person,
                selected = currentDestination == AppDestination.PROFILE_SETTINGS,
                testTag = "drawer_item_my_profile",
                onClick = { onSelectDestination(AppDestination.PROFILE_SETTINGS) }
            )
            DrawerMenuItem(
                label = if (isHindi) "सब्सक्रिप्शन / प्लान अपग्रेड" else "Subscription / Upgrade Plan",
                icon = Icons.Default.Star,
                selected = currentDestination == AppDestination.SUBSCRIPTIONS,
                testTag = "drawer_item_subscriptions",
                onClick = { onSelectDestination(AppDestination.SUBSCRIPTIONS) }
            )
            DrawerMenuItem(
                label = if (isHindi) "पेमेंट हिस्ट्री (Payment History)" else "Payment History",
                icon = Icons.Default.ReceiptLong,
                selected = false,
                testTag = "drawer_item_payment_history",
                onClick = { onSelectDestination(AppDestination.SUBSCRIPTIONS) }
            )
            DrawerMenuItem(
                label = if (isHindi) "सेटिंग्स (Settings)" else "Settings",
                icon = Icons.Default.Settings,
                selected = false,
                testTag = "drawer_item_settings",
                onClick = { onSelectDestination(AppDestination.PROFILE_SETTINGS) }
            )
            DrawerMenuItem(
                label = if (isHindi) "सहायता और समर्थन (Help & Support)" else "Help & Support",
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                selected = currentDestination == AppDestination.HELP_AND_LEGAL,
                testTag = "drawer_item_help_support",
                onClick = { onSelectDestination(AppDestination.HELP_AND_LEGAL) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // SECTION 4: Session / Logout
            NavigationDrawerItem(
                label = {
                    Text(
                        text = if (isHindi) "लॉगआउट / साइन आउट" else "Logout / Sign Out",
                        fontWeight = FontWeight.Bold,
                        color = SkillErrorRed
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = SkillErrorRed
                    )
                },
                selected = false,
                onClick = onRequestLogout,
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_logout")
            )
        }
    }
}

@Composable
private fun DrawerSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
    )
}

@Composable
private fun DrawerMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    badgeCount: Int = 0,
    testTag: String,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label
            )
        },
        badge = if (badgeCount > 0) {
            {
                Badge(containerColor = SkillBlue, contentColor = Color.White) {
                    Text("$badgeCount")
                }
            }
        } else null,
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = SkillBlue.copy(alpha = 0.14f),
            selectedIconColor = SkillBlue,
            selectedTextColor = SkillBlue
        ),
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .testTag(testTag)
    )
}

@Composable
private fun AuthenticatedMobileBottomBar(
    isHindi: Boolean,
    isProviderAccount: Boolean,
    currentDestination: AppDestination,
    unreadNotifCount: Int,
    onNavigate: (AppDestination) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        if (isProviderAccount) {
            NavigationBarItem(
                selected = currentDestination == AppDestination.PROVIDER_DASHBOARD,
                onClick = { onNavigate(AppDestination.PROVIDER_DASHBOARD) },
                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                label = { Text(if (isHindi) "डैशबोर्ड" else "Workspace") },
                modifier = Modifier.testTag("bottom_nav_provider_dashboard")
            )
            NavigationBarItem(
                selected = currentDestination == AppDestination.BOOKINGS,
                onClick = { onNavigate(AppDestination.BOOKINGS) },
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Requests") },
                label = { Text(if (isHindi) "अनुरोध" else "Bookings") },
                modifier = Modifier.testTag("bottom_nav_bookings")
            )
            NavigationBarItem(
                selected = currentDestination == AppDestination.MESSAGES,
                onClick = { onNavigate(AppDestination.MESSAGES) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (unreadNotifCount > 0) {
                                Badge { Text("$unreadNotifCount") }
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Messages")
                    }
                },
                label = { Text(if (isHindi) "चैट" else "Messages") },
                modifier = Modifier.testTag("bottom_nav_messages")
            )
            NavigationBarItem(
                selected = currentDestination == AppDestination.PROVIDER_ONBOARDING_WIZARD ||
                    currentDestination == AppDestination.PROVIDER_VERIFICATION,
                onClick = { onNavigate(AppDestination.PROVIDER_ONBOARDING_WIZARD) },
                icon = { Icon(Icons.Default.VerifiedUser, contentDescription = "Verification") },
                label = { Text(if (isHindi) "सत्यापन" else "Verify") },
                modifier = Modifier.testTag("bottom_nav_verify")
            )
            NavigationBarItem(
                selected = currentDestination == AppDestination.PROFILE_SETTINGS,
                onClick = { onNavigate(AppDestination.PROFILE_SETTINGS) },
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text(if (isHindi) "प्रोफ़ाइल" else "Profile") },
                modifier = Modifier.testTag("bottom_nav_profile")
            )
        } else {
            NavigationBarItem(
                selected = currentDestination == AppDestination.HOME,
                onClick = { onNavigate(AppDestination.HOME) },
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text(if (isHindi) "होम" else "Home") },
                modifier = Modifier.testTag("bottom_nav_home")
            )
            NavigationBarItem(
                selected = currentDestination == AppDestination.EXPLORE ||
                    currentDestination == AppDestination.SERVICE_DETAIL ||
                    currentDestination == AppDestination.PROVIDER_DIRECTORY,
                onClick = { onNavigate(AppDestination.EXPLORE) },
                icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                label = { Text(if (isHindi) "खोजें" else "Explore") },
                modifier = Modifier.testTag("bottom_nav_explore")
            )
            NavigationBarItem(
                selected = currentDestination == AppDestination.BOOKINGS,
                onClick = { onNavigate(AppDestination.BOOKINGS) },
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Bookings") },
                label = { Text(if (isHindi) "बुकिंग्स" else "Bookings") },
                modifier = Modifier.testTag("bottom_nav_bookings")
            )
            NavigationBarItem(
                selected = currentDestination == AppDestination.MESSAGES,
                onClick = { onNavigate(AppDestination.MESSAGES) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (unreadNotifCount > 0) {
                                Badge { Text("$unreadNotifCount") }
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Messages")
                    }
                },
                label = { Text(if (isHindi) "चैट" else "Messages") },
                modifier = Modifier.testTag("bottom_nav_messages")
            )
            NavigationBarItem(
                selected = currentDestination == AppDestination.PROFILE_SETTINGS ||
                    currentDestination == AppDestination.CUSTOMER_DASHBOARD,
                onClick = { onNavigate(AppDestination.CUSTOMER_DASHBOARD) },
                icon = { Icon(Icons.Default.Person, contentDescription = "Dashboard") },
                label = { Text(if (isHindi) "डैशबोर्ड" else "Account") },
                modifier = Modifier.testTag("bottom_nav_profile")
            )
        }
    }
}
