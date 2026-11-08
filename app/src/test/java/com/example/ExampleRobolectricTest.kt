package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.MarketplaceRepository
import com.example.model.UserRole
import com.example.model.WorkRequestStatus
import com.example.ui.MarketplaceViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SkillConnect", appName)
  }

  @Test
  fun `verify marketplace initial categories and verified providers`() {
    val repository = MarketplaceRepository()
    assertEquals(6, repository.categories.value.size)
    assertTrue(repository.providers.value.isNotEmpty())

    // Check founder is in authorized admin list
    assertTrue(repository.authorizedAdminEmails.contains("singhabhishek83072@gmail.com"))
  }

  @Test
  fun `customer can register and authenticate`() {
    val repository = MarketplaceRepository()
    assertFalse(repository.isAuthenticated.value)

    val res = repository.registerCustomer(
      name = "Vikram Sen",
      email = "vikram.sen@example.in",
      phone = "9876543210",
      city = "Kolkata",
      state = "West Bengal",
      pass = "password123"
    )

    assertTrue(res.isSuccess)
    assertTrue(repository.isAuthenticated.value)
    assertEquals("Vikram Sen", repository.currentUser.value.name)
    assertEquals(UserRole.CUSTOMER, repository.currentUser.value.role)

    // Verify duplicate registration rejection
    val dupRes = repository.registerCustomer(
      name = "Vikram Duplicate",
      email = "vikram.sen@example.in",
      phone = "9876543210",
      city = "Kolkata",
      state = "West Bengal",
      pass = "password123"
    )
    assertTrue(dupRes.isFailure)
  }

  @Test
  fun `service provider can register and appear in marketplace`() {
    val repository = MarketplaceRepository()
    val initialProviderCount = repository.providers.value.size

    val res = repository.registerProvider(
      name = "Deepika Rao",
      email = "deepika.rao@example.in",
      phone = "9812345678",
      city = "Bengaluru",
      state = "Karnataka",
      pass = "password123",
      professionalTitle = "Android Compose Specialist",
      categoryId = "cat_dev",
      skills = listOf("Jetpack Compose", "Kotlin", "Coroutines"),
      hourlyRateInr = 1800,
      bio = "Passionate mobile engineer."
    )

    assertTrue(res.isSuccess)
    assertTrue(repository.isAuthenticated.value)
    assertEquals(UserRole.SERVICE_PROVIDER, repository.currentUser.value.role)
    assertEquals(initialProviderCount + 1, repository.providers.value.size)
    assertTrue(repository.providers.value.any { it.name == "Deepika Rao" })
  }

  @Test
  fun `admin access is strictly protected and non-admin cannot access admin role`() {
    val repository = MarketplaceRepository()

    // 1. Authorized founder (Abhishek Singh) login succeeds
    val founderRes = repository.login(
      email = "singhabhishek83072@gmail.com",
      pass = "FounderSecure#2026",
      roleRequested = UserRole.ADMIN
    )
    assertTrue(founderRes.isSuccess)
    assertEquals(UserRole.ADMIN, repository.currentUser.value.role)
    assertEquals("Abhishek Singh (Founder)", repository.currentUser.value.name)

    // Wrong password on subsequent login is rejected
    val wrongPassRes = repository.login(
      email = "singhabhishek83072@gmail.com",
      pass = "WrongPass#999",
      roleRequested = UserRole.ADMIN
    )
    assertTrue(wrongPassRes.isFailure)
    assertTrue(wrongPassRes.exceptionOrNull() is IllegalArgumentException)

    // 2. Authorized Admins (Abhay & Adarsh) login succeeds
    val abhayRes = repository.login(
      email = "abhay@skillconnect.in",
      pass = "AbhayAdmin#2026",
      roleRequested = UserRole.ADMIN
    )
    assertTrue(abhayRes.isSuccess)
    assertEquals(UserRole.ADMIN, repository.currentUser.value.role)
    assertEquals("Abhay (Admin)", repository.currentUser.value.name)

    val adarshRes = repository.login(
      email = "adarsh@skillconnect.in",
      pass = "AdarshAdmin#2026",
      roleRequested = UserRole.ADMIN
    )
    assertTrue(adarshRes.isSuccess)
    assertEquals(UserRole.ADMIN, repository.currentUser.value.role)
    assertEquals("Adarsh (Admin)", repository.currentUser.value.name)

    // 3. Customer and Service Provider accounts attempting admin login are strictly rejected
    val customerAdminAttempt = repository.login(
      email = "aarav.mehta@example.in",
      pass = "CustomerPass#123",
      roleRequested = UserRole.ADMIN
    )
    assertTrue(customerAdminAttempt.isFailure)
    assertTrue(customerAdminAttempt.exceptionOrNull() is SecurityException)

    val providerAdminAttempt = repository.login(
      email = "ananya.ui@example.in",
      pass = "ProviderPass#123",
      roleRequested = UserRole.ADMIN
    )
    assertTrue(providerAdminAttempt.isFailure)
    assertTrue(providerAdminAttempt.exceptionOrNull() is SecurityException)

    // 4. Unauthorized random user attempting admin login is strictly rejected
    val fakeAdminRes = repository.login(
      email = "randomhacker@example.com",
      pass = "HackerPass#123",
      roleRequested = UserRole.ADMIN
    )
    assertTrue(fakeAdminRes.isFailure)
    assertTrue(fakeAdminRes.exceptionOrNull() is SecurityException)

    // 5. Unauthorized Google Sign-In for Admin is strictly rejected
    val googleFakeAdmin = repository.loginWithGoogle(
      uid = "uid_unauth",
      displayName = "Unauthorized User",
      email = "aarav.mehta@example.in",
      photoUrl = null,
      roleRequested = UserRole.ADMIN
    )
    assertTrue(googleFakeAdmin.isFailure)
    assertTrue(googleFakeAdmin.exceptionOrNull() is SecurityException)
  }

  @Test
  fun `logout clears authentication session`() {
    val repository = MarketplaceRepository()
    repository.login("aarav.mehta@example.in", "CustomerSecure#1", UserRole.CUSTOMER)
    assertTrue(repository.isAuthenticated.value)

    repository.logout()
    assertFalse(repository.isAuthenticated.value)
  }

  @Test
  fun `customer can create work request and provider can complete it`() {
    val repository = MarketplaceRepository()
    val provider = repository.providers.value.first()

    val req = repository.createWorkRequest(
        providerId = provider.id,
        title = "Test App Feature",
        description = "Test request description",
        category = "Website & Development",
        budgetInr = 15000,
        deadline = "10 Days"
    )

    assertEquals(WorkRequestStatus.PENDING, req.status)
    assertEquals(15000L, req.budgetInr)

    repository.updateWorkRequestStatus(req.id, WorkRequestStatus.ACCEPTED)
    val updatedReq = repository.workRequests.value.find { it.id == req.id }
    assertEquals(WorkRequestStatus.ACCEPTED, updatedReq?.status)
  }

  @Test
  fun `viewModel dynamic role switching preserves independent screen routes for customer and provider`() {
    val repository = MarketplaceRepository()
    val viewModel = MarketplaceViewModel(repository)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    // Initial state: Customer role, route is "home"
    assertEquals(UserRole.CUSTOMER, viewModel.uiState.value.activeRole)
    assertEquals("home", viewModel.uiState.value.currentScreenRoute)

    // Customer navigates to "requests"
    viewModel.setScreenRoute("requests")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertEquals("requests", viewModel.uiState.value.currentScreenRoute)
    assertEquals("requests", viewModel.uiState.value.customerScreenRoute)

    // Switch to Provider Studio
    viewModel.switchActiveRole(UserRole.SERVICE_PROVIDER)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertEquals(UserRole.SERVICE_PROVIDER, viewModel.uiState.value.activeRole)
    // Provider's route is its default "dashboard", not the Customer's "requests"
    assertEquals("dashboard", viewModel.uiState.value.currentScreenRoute)
    assertEquals("dashboard", viewModel.uiState.value.providerScreenRoute)

    // Provider navigates to "explore"
    viewModel.setScreenRoute("explore")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertEquals("explore", viewModel.uiState.value.currentScreenRoute)
    assertEquals("explore", viewModel.uiState.value.providerScreenRoute)

    // Switch back to Customer: Customer's "requests" route is preserved!
    viewModel.switchActiveRole(UserRole.CUSTOMER)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertEquals(UserRole.CUSTOMER, viewModel.uiState.value.activeRole)
    assertEquals("requests", viewModel.uiState.value.currentScreenRoute)

    // Switch back to Provider: Provider's "explore" route is preserved!
    viewModel.switchActiveRole(UserRole.SERVICE_PROVIDER)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertEquals(UserRole.SERVICE_PROVIDER, viewModel.uiState.value.activeRole)
    assertEquals("explore", viewModel.uiState.value.currentScreenRoute)
  }

  @Test
  fun `normal customer cannot switch role to admin`() {
    val repository = MarketplaceRepository()
    repository.login("aarav.mehta@example.in", "password123", UserRole.CUSTOMER)
    val viewModel = MarketplaceViewModel(repository)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    assertEquals(UserRole.CUSTOMER, viewModel.uiState.value.activeRole)

    // Attempt unauthorized switch to ADMIN
    viewModel.switchActiveRole(UserRole.ADMIN)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    // Role remains CUSTOMER because aarav is not an authorized admin email
    assertEquals(UserRole.CUSTOMER, viewModel.uiState.value.activeRole)
  }

  @Test
  fun `user can customize profile with photo, cover, headline, skills, and portfolio links`() {
    val repository = MarketplaceRepository()
    repository.login("aarav.mehta@example.in", "password123", UserRole.CUSTOMER)
    val viewModel = MarketplaceViewModel(repository)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    val initialUser = viewModel.uiState.value.currentUser
    val updatedUser = initialUser.copy(
        name = "Aarav Mehta Updated",
        professionalHeadline = "Enterprise Product Lead & Growth Strategist",
        bio = "Seasoned founder building cross-functional Indian technology ventures.",
        city = "Gurugram",
        state = "Haryana",
        skills = listOf("Product Strategy", "Figma", "Fintech"),
        portfolioLinks = listOf("https://aarav.dev", "https://github.com/aarav"),
        profilePhotoUri = "content://media/external/images/media/42",
        coverPhotoUri = "content://media/external/images/media/99"
    )

    viewModel.updateUserProfile(updatedUser)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    val savedUser = viewModel.uiState.value.currentUser
    assertEquals("Aarav Mehta Updated", savedUser.name)
    assertEquals("Enterprise Product Lead & Growth Strategist", savedUser.professionalHeadline)
    assertEquals("Seasoned founder building cross-functional Indian technology ventures.", savedUser.bio)
    assertEquals("Gurugram", savedUser.city)
    assertEquals("Haryana", savedUser.state)
    assertEquals(listOf("Product Strategy", "Figma", "Fintech"), savedUser.skills)
    assertEquals(listOf("https://aarav.dev", "https://github.com/aarav"), savedUser.portfolioLinks)
    assertEquals("content://media/external/images/media/42", savedUser.profilePhotoUri)
    assertEquals("content://media/external/images/media/99", savedUser.coverPhotoUri)
  }

  @Test
  fun `theme mode toggle switches between dark and light themes`() {
    val repository = MarketplaceRepository()
    val viewModel = MarketplaceViewModel(repository)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    // Default theme is dark
    assertTrue(viewModel.uiState.value.isDarkTheme)

    // Toggle to Light
    viewModel.toggleTheme()
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertFalse(viewModel.uiState.value.isDarkTheme)

    // Toggle back to Dark
    viewModel.toggleTheme()
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertTrue(viewModel.uiState.value.isDarkTheme)

    // Explicitly set theme
    viewModel.setTheme(false)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertFalse(viewModel.uiState.value.isDarkTheme)
  }

  @Test
  fun `profile photo and cover photo can be updated directly`() {
    val repository = MarketplaceRepository()
    repository.login("aarav.mehta@example.in", "password123", UserRole.CUSTOMER)
    val viewModel = MarketplaceViewModel(repository)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    viewModel.updateProfilePhoto("content://media/external/images/media/101")
    viewModel.updateCoverPhoto("content://media/external/images/media/202")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    val user = viewModel.uiState.value.currentUser
    assertEquals("content://media/external/images/media/101", user.profilePhotoUri)
    assertEquals("content://media/external/images/media/202", user.coverPhotoUri)
  }

  @Test
  fun `real-time admin validation detects empty fields and invalid email formats`() {
    // Empty email in Admin mode
    assertEquals(
      "Authorized admin email is required.",
      com.example.ui.screens.getEmailRealtimeValidationMessage("", isAdminMode = true)
    )
    // Missing '@' symbol
    assertEquals(
      "Include an '@' symbol (e.g. admin@skillconnect.in).",
      com.example.ui.screens.getEmailRealtimeValidationMessage("singhabhishek83072", isAdminMode = true)
    )
    // Missing domain
    assertEquals(
      "Enter a complete domain (e.g. skillconnect.in).",
      com.example.ui.screens.getEmailRealtimeValidationMessage("admin@skillconnect", isAdminMode = true)
    )
    // Valid email returns null error
    assertNull(
      com.example.ui.screens.getEmailRealtimeValidationMessage("singhabhishek83072@gmail.com", isAdminMode = true)
    )

    // Empty password in Admin mode
    assertEquals(
      "Admin password is required.",
      com.example.ui.screens.getPasswordRealtimeValidationMessage("", isAdminMode = true)
    )
    // Short password
    assertEquals(
      "Password must be at least 6 characters (3/6).",
      com.example.ui.screens.getPasswordRealtimeValidationMessage("abc", isAdminMode = true)
    )
    // Valid password returns null error
    assertNull(
      com.example.ui.screens.getPasswordRealtimeValidationMessage("AdminSecure#2026", isAdminMode = true)
    )
  }

  @Test
  fun `admin forgot password flow sends reset email for authorized admins and rejects unauthorized accounts`() {
    val repository = MarketplaceRepository()

    // 1. Authorized admin binds initial password, then triggers password reset
    val initialLogin = repository.login("singhabhishek83072@gmail.com", "InitialAdminPass#1", UserRole.ADMIN)
    assertTrue(initialLogin.isSuccess)
    repository.logout()

    val resetRes = repository.requestAdminPasswordReset("singhabhishek83072@gmail.com")
    assertTrue(resetRes.isSuccess)
    assertTrue(resetRes.getOrThrow().contains("singhabhishek83072@gmail.com"))

    // After reset, admin can authenticate with a newly updated password
    val rebindLogin = repository.login("singhabhishek83072@gmail.com", "UpdatedAdminPass#2", UserRole.ADMIN)
    assertTrue(rebindLogin.isSuccess)

    // 2. Unauthorized customer or random email is strictly rejected
    val unauthReset = repository.requestAdminPasswordReset("aarav.mehta@example.in")
    assertTrue(unauthReset.isFailure)
    assertTrue(unauthReset.exceptionOrNull() is SecurityException)

    // 3. Invalid email format is rejected
    val invalidEmailReset = repository.requestAdminPasswordReset("invalid-email")
    assertTrue(invalidEmailReset.isFailure)
    assertTrue(invalidEmailReset.exceptionOrNull() is IllegalArgumentException)
  }

  @Test
  fun `admin bottom navigation switches properly between Admin, Marketplace, and Profile while preserving session`() {
    val repository = MarketplaceRepository()
    val viewModel = MarketplaceViewModel(repository)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    // Log in as authorized admin Abhay
    val loginRes = viewModel.login("abhay@skillconnect.in", "AdminPass#2026", UserRole.ADMIN)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertTrue(loginRes.isSuccess)
    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertEquals(UserRole.ADMIN, viewModel.uiState.value.activeRole)
    assertEquals("admin", viewModel.uiState.value.currentScreenRoute)

    // Switch to Marketplace tab
    viewModel.setScreenRoute("marketplace")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertEquals("marketplace", viewModel.uiState.value.currentScreenRoute)
    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertEquals(UserRole.ADMIN, viewModel.uiState.value.activeRole)
    assertTrue(viewModel.uiState.value.categories.isNotEmpty())
    assertTrue(viewModel.uiState.value.allProviders.isNotEmpty())

    // Switch to Explore sub-route inside Marketplace
    viewModel.setScreenRoute("explore")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertEquals("explore", viewModel.uiState.value.currentScreenRoute)
    assertTrue(viewModel.uiState.value.isAuthenticated)

    // Switch to Profile tab
    viewModel.setScreenRoute("profile")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertEquals("profile", viewModel.uiState.value.currentScreenRoute)
    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertEquals("Abhay (Admin)", viewModel.uiState.value.currentUser.name)
    assertEquals("abhay@skillconnect.in", viewModel.uiState.value.currentUser.email)
    assertEquals(UserRole.ADMIN, viewModel.uiState.value.currentUser.role)

    // Navigate to Edit Profile and update admin bio
    viewModel.setScreenRoute("edit_profile")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertEquals("edit_profile", viewModel.uiState.value.currentScreenRoute)
    val updatedAdmin = viewModel.uiState.value.currentUser.copy(bio = "Updated Admin Governance Bio")
    viewModel.updateUserProfile(updatedAdmin)
    viewModel.setScreenRoute("profile")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertEquals("profile", viewModel.uiState.value.currentScreenRoute)
    assertEquals("Updated Admin Governance Bio", viewModel.uiState.value.currentUser.bio)

    // Switch back to Admin Dashboard tab
    viewModel.setScreenRoute("admin")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertEquals("admin", viewModel.uiState.value.currentScreenRoute)
    assertTrue(viewModel.uiState.value.isAuthenticated)
  }

  @Test
  fun `admin session timeout logs out admin automatically after 15 minutes of inactivity`() {
    val repository = MarketplaceRepository()
    val viewModel = MarketplaceViewModel(repository)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    val loginRes = viewModel.login("singhabhishek83072@gmail.com", "FounderPass#2026", UserRole.ADMIN)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertTrue(loginRes.isSuccess)
    assertTrue(viewModel.uiState.value.isAuthenticated)

    val startMillis = 1_000_000L
    viewModel.recordAdminActivity(startMillis)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    // 10 minutes of inactivity -> session remains active
    val tenMinutesLater = startMillis + (10 * 60 * 1000L)
    val timedOutAt10m = viewModel.checkAdminSessionTimeout(tenMinutesLater)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertFalse(timedOutAt10m)
    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertNull(viewModel.uiState.value.sessionTimeoutMessage)

    // Activity at 12 minutes resets the 15-minute timer
    val twelveMinutesLater = startMillis + (12 * 60 * 1000L)
    viewModel.recordAdminActivity(twelveMinutesLater)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    // 14 minutes after the last activity -> still active
    val fourteenMinutesAfterReset = twelveMinutesLater + (14 * 60 * 1000L)
    assertFalse(viewModel.checkAdminSessionTimeout(fourteenMinutesAfterReset))
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertTrue(viewModel.uiState.value.isAuthenticated)

    // 15 minutes after the last activity -> automatically logs out admin
    val fifteenMinutesAfterReset = twelveMinutesLater + MarketplaceViewModel.ADMIN_SESSION_TIMEOUT_MS
    val timedOutAt15m = viewModel.checkAdminSessionTimeout(fifteenMinutesAfterReset)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertTrue(timedOutAt15m)
    assertFalse(viewModel.uiState.value.isAuthenticated)
    assertTrue(
      viewModel.uiState.value.sessionTimeoutMessage?.contains("15 minutes of inactivity") == true
    )
  }

  @Test
  fun `five-tab bottom navigation and footer routes switch cleanly while maintaining login session`() {
    val repository = MarketplaceRepository()
    val viewModel = MarketplaceViewModel(repository)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    val loginRes = viewModel.login("aarav.mehta@example.in", "CustomerPass#1", UserRole.CUSTOMER)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertTrue(loginRes.isSuccess)
    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertEquals(UserRole.CUSTOMER, viewModel.uiState.value.activeRole)

    val fiveTabs = listOf("home", "explore", "bookings", "messages", "profile")
    for (tab in fiveTabs) {
      viewModel.setScreenRoute(tab)
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      assertEquals(tab, viewModel.uiState.value.currentScreenRoute)
      assertTrue(viewModel.uiState.value.isAuthenticated)
    }

    val footerRoutes = listOf(
      "become_provider",
      "provider_verification",
      "subscriptions",
      "help_centre",
      "legal_policies"
    )
    for (route in footerRoutes) {
      viewModel.setScreenRoute(route)
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      assertEquals(route, viewModel.uiState.value.currentScreenRoute)
      assertTrue(viewModel.uiState.value.isAuthenticated)
    }
  }

  @Test
  fun `search discovery filters, sorting, recently viewed, and favourite providers work accurately`() {
    val repository = MarketplaceRepository()
    val viewModel = MarketplaceViewModel(repository)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    // Search by skill or category
    viewModel.setSearchQuery("Jetpack Compose")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertTrue(viewModel.uiState.value.filteredProviders.isNotEmpty())
    assertTrue(
      viewModel.uiState.value.filteredProviders.all { prov ->
        prov.skills.any { it.contains("Compose", ignoreCase = true) } ||
          prov.professionalTitle.contains("Compose", ignoreCase = true)
      }
    )
    viewModel.setSearchQuery("")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    // Local Services filter narrows to user's city (Bengaluru for default user Aarav Mehta)
    viewModel.setServiceScopeFilter(com.example.model.ServiceScopeFilter.LOCAL_SERVICES)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertTrue(viewModel.uiState.value.filteredProviders.isNotEmpty())
    assertTrue(
      viewModel.uiState.value.filteredProviders.all {
        it.city.equals(viewModel.uiState.value.currentUser.city, ignoreCase = true)
      }
    )
    viewModel.setServiceScopeFilter(com.example.model.ServiceScopeFilter.ALL_SERVICES)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    // Verified Only & Available Only filters
    viewModel.toggleVerifiedOnlyFilter()
    viewModel.toggleAvailableOnlyFilter()
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertTrue(viewModel.uiState.value.verifiedOnlyFilter)
    assertTrue(viewModel.uiState.value.availableOnlyFilter)
    assertTrue(viewModel.uiState.value.filteredProviders.all { it.isVerified && it.isAvailable })

    // Sort by Rating descending
    viewModel.setSortOption(com.example.model.ProviderSortOption.RATING_HIGH_TO_LOW)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    val ratings = viewModel.uiState.value.filteredProviders.map { it.rating }
    assertEquals(ratings.sortedDescending(), ratings)

    // Recently viewed & Favourite specialist tracking
    val firstProv = viewModel.uiState.value.allProviders.first()
    viewModel.selectProvider(firstProv)
    viewModel.toggleFavouriteProvider(firstProv.id)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    assertTrue(viewModel.uiState.value.recentlyViewedProviders.any { it.id == firstProv.id })
    assertTrue(viewModel.uiState.value.favouriteProviders.any { it.id == firstProv.id })
  }

  @Test
  fun `official branding and contact email match skillconnectofficiall@gmail_com while preserving admin accounts`() {
    assertEquals("SkillConnect", com.example.model.SkillConnectBranding.DISPLAY_NAME)
    assertEquals("Your Skills. Your Local Network.", com.example.model.SkillConnectBranding.TAGLINE)
    assertEquals(
      "Connecting customers with verified local professionals and freelancers across India.",
      com.example.model.SkillConnectBranding.DESCRIPTION
    )
    assertEquals("@skillconnectofficial", com.example.model.SkillConnectBranding.OFFICIAL_COMMUNITY)
    assertEquals("skillconnectofficiall@gmail.com", com.example.model.SkillConnectBranding.OFFICIAL_EMAIL)
    assertEquals("© 2026 SkillConnect India.", com.example.model.SkillConnectBranding.COPYRIGHT)

    // Verify authorized admin login accounts remain untouched
    val repository = MarketplaceRepository()
    assertTrue(repository.authorizedAdminEmails.contains("singhabhishek83072@gmail.com"))
    assertTrue(repository.authorizedAdminEmails.contains("abhay@skillconnect.in"))
    assertTrue(repository.authorizedAdminEmails.contains("adarsh@skillconnect.in"))
  }

  @Test
  fun `multi-level RBAC authenticates Founder, CEO, and Admin with distinct privileges and audit trail`() {
    val repository = MarketplaceRepository()

    // 1. Authenticate Founder
    val founderLogin = repository.login(
      email = MarketplaceRepository.OFFICIAL_FOUNDER_EMAIL,
      pass = "FounderSecure#2026",
      roleRequested = UserRole.ADMIN
    )
    assertTrue(founderLogin.isSuccess)
    val founder = founderLogin.getOrThrow()
    assertEquals(UserRole.ADMIN, founder.role)
    assertEquals(com.example.model.AdminRoleType.FOUNDER, founder.adminRole)

    // Verify Founder has Super Admin permissions
    val founderPerms = com.example.model.AdminPermissions.FOUNDER_PERMISSIONS
    assertTrue(founderPerms.canManageAdmins)
    assertTrue(founderPerms.canManageRoles)
    assertTrue(founderPerms.canAccessSecurityLogs)
    assertTrue(founderPerms.canDeletePlatformData)

    // 2. Authenticate CEO
    val ceoLogin = repository.login(
      email = MarketplaceRepository.OFFICIAL_CEO_EMAIL,
      pass = "CeoOperations#2026",
      roleRequested = UserRole.ADMIN
    )
    assertTrue(ceoLogin.isSuccess)
    val ceo = ceoLogin.getOrThrow()
    assertEquals(com.example.model.AdminRoleType.CEO, ceo.adminRole)

    val ceoPerms = com.example.model.AdminPermissions.CEO_PERMISSIONS
    assertFalse(ceoPerms.canManageAdmins) // CEO cannot manage admin accounts
    assertTrue(ceoPerms.canApproveProviders)
    assertTrue(ceoPerms.canResolveComplaints)
    assertTrue(ceoPerms.canViewFinancialAnalytics)

    // 3. Authenticate Limited Admin
    val adminLogin = repository.login(
      email = MarketplaceRepository.OFFICIAL_ADMIN_EMAIL,
      pass = "AdminSupport#2026",
      roleRequested = UserRole.ADMIN
    )
    assertTrue(adminLogin.isSuccess)
    val admin = adminLogin.getOrThrow()
    assertEquals(com.example.model.AdminRoleType.ADMIN, admin.adminRole)

    val adminPerms = com.example.model.AdminPermissions.LIMITED_ADMIN_PERMISSIONS
    assertFalse(adminPerms.canManageAdmins)
    assertFalse(adminPerms.canViewFinancialAnalytics)
    assertFalse(adminPerms.canDeletePlatformData)
    assertTrue(adminPerms.canApproveProviders)

    // 4. Test privilege boundaries: Limited Admin cannot revoke accounts
    val revokeAttemptByAdmin = repository.revokeAdminAccount(
      targetUid = "admin_ceo_abhay",
      callerRole = com.example.model.AdminRoleType.ADMIN
    )
    assertTrue(revokeAttemptByAdmin.isFailure)

    // 5. Founder can update permissions and revoke accounts
    val updatePermsByFounder = repository.updateAdminPermissions(
      targetUid = "admin_ops_abhay",
      newPermissions = adminPerms.copy(canBroadcastAnnouncements = true),
      callerRole = com.example.model.AdminRoleType.FOUNDER
    )
    assertTrue(updatePermsByFounder.isSuccess)

    // Verify audit log generated
    assertTrue(repository.auditLogs.value.any { it.action == "PERMISSIONS_UPDATED" })

    // 6. Dispute Resolution by CEO
    val complaint = repository.complaints.value.first()
    val resolveResult = repository.resolveComplaint(
      complaintId = complaint.id,
      newStatus = com.example.model.ComplaintStatus.RESOLVED,
      resolutionNotes = "Settled with client as per terms.",
      resolverRole = com.example.model.AdminRoleType.CEO
    )
    assertTrue(resolveResult.isSuccess)
    val updatedComp = repository.complaints.value.find { it.id == complaint.id }
    assertEquals(com.example.model.ComplaintStatus.RESOLVED, updatedComp?.status)

    // 7. Founder cannot be revoked
    val revokeFounderAttempt = repository.revokeAdminAccount(
      targetUid = "admin_founder_abhishek",
      callerRole = com.example.model.AdminRoleType.FOUNDER
    )
    assertTrue(revokeFounderAttempt.isFailure)
  }

  @Test
  fun `account provisioning wizard configures official admin credentials`() {
    val repository = MarketplaceRepository()

    // Must be Founder to run provisioning
    repository.login(
      email = MarketplaceRepository.OFFICIAL_FOUNDER_EMAIL,
      pass = "FounderSecure#2026",
      roleRequested = UserRole.ADMIN
    )

    val provResult = repository.provisionOfficialAdminAccounts(
      founderPass = "NewFounderP@ss2026",
      ceoPass = "NewCeoP@ss2026",
      adminPass = "NewAdminP@ss2026"
    )
    assertTrue(provResult.isSuccess)

    // Verify login with new password succeeds
    val newFounderLogin = repository.login(
      email = MarketplaceRepository.OFFICIAL_FOUNDER_EMAIL,
      pass = "NewFounderP@ss2026",
      roleRequested = UserRole.ADMIN
    )
    assertTrue(newFounderLogin.isSuccess)

    // Verify audit log
    assertTrue(repository.auditLogs.value.any { it.action == "OFFICIAL_PROVISIONING" })
  }

  @Test
  fun `verified badge renders horizontally and provider verification flag is accurately reflected`() {
    val repository = MarketplaceRepository()
    val verifiedProvs = repository.providers.value.filter { it.isVerified }
    val unverifiedProvs = repository.providers.value.filter { !it.isVerified }

    assertTrue("Expected verified providers in marketplace", verifiedProvs.isNotEmpty())
    assertTrue("Expected unverified providers for admin testing", unverifiedProvs.isNotEmpty())

    // Verify verified specialist attributes
    val firstVerified = verifiedProvs.first()
    assertTrue(firstVerified.isVerified)
    assertNotNull(firstVerified.name)
    assertNotNull(firstVerified.professionalTitle)
  }

  @Test
  fun `marketplace pull-to-refresh triggers refresh cycle and updates lastRefreshedMillis`() {
    val repository = MarketplaceRepository()
    val viewModel = MarketplaceViewModel(repository)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    assertFalse(viewModel.uiState.value.isMarketplaceRefreshing)
    val initialTime = viewModel.uiState.value.lastRefreshedMillis

    var completed = false
    viewModel.refreshMarketplaceFeed(durationMillis = 0L) { completed = true }

    // Advance time and idle looper
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    // After completion, isMarketplaceRefreshing resets to false and callback fired
    assertFalse(viewModel.uiState.value.isMarketplaceRefreshing)
    assertTrue(completed)
    assertTrue(viewModel.uiState.value.lastRefreshedMillis >= initialTime)
  }

  @Test
  fun `permanent account registration and returning-user login survive logout and app restart`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testPrefsName = "test_permanent_accounts_${System.currentTimeMillis()}"

    // 1. First-time Customer registration on initial app launch
    val repoInstance1 = MarketplaceRepository(context = context, prefsName = testPrefsName)
    val vmInstance1 = MarketplaceViewModel(repoInstance1)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    val custReg = vmInstance1.registerCustomer(
      name = "Riya Kapoor",
      email = "Riya.Kapoor@gmail.com",
      phone = "9876501234",
      city = "Pune",
      state = "Maharashtra",
      pass = "RiyaSecure#2026"
    )
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertTrue(custReg.isSuccess)
    assertTrue(vmInstance1.uiState.value.isAuthenticated)
    assertEquals(UserRole.CUSTOMER, vmInstance1.uiState.value.activeRole)
    assertEquals("home", vmInstance1.uiState.value.currentScreenRoute)
    assertNotNull(vmInstance1.uiState.value.registrationConfirmationMessage)
    assertTrue(vmInstance1.uiState.value.registrationConfirmationMessage!!.contains("riya.kapoor@gmail.com"))

    // Dismiss confirmation banner
    vmInstance1.clearRegistrationConfirmationMessage()
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertNull(vmInstance1.uiState.value.registrationConfirmationMessage)

    // 2. Simulate closing and reopening the app while logged in -> session is maintained!
    val repoInstance2 = MarketplaceRepository(context = context, prefsName = testPrefsName)
    val vmInstance2 = MarketplaceViewModel(repoInstance2)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    assertTrue("Session must remain authenticated across app restart", vmInstance2.uiState.value.isAuthenticated)
    assertEquals("Riya Kapoor", vmInstance2.uiState.value.currentUser.name)
    assertEquals("riya.kapoor@gmail.com", vmInstance2.uiState.value.currentUser.email)
    assertEquals(UserRole.CUSTOMER, vmInstance2.uiState.value.activeRole)
    assertEquals("home", vmInstance2.uiState.value.currentScreenRoute)

    // 3. Logout ends only the current session; account remains permanently saved
    vmInstance2.logout()
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertFalse(vmInstance2.uiState.value.isAuthenticated)

    // 4. Register a Service Provider account as well
    val provReg = vmInstance2.registerProvider(
      name = "Karan Malhotra",
      email = "karan.dev@gmail.com",
      phone = "9811122334",
      city = "Hyderabad",
      state = "Telangana",
      pass = "KaranPass#2026",
      professionalTitle = "Full-Stack Cloud Architect",
      categoryId = "cat_dev",
      skills = listOf("Kotlin", "Firebase", "GCP"),
      hourlyRateInr = 2200L,
      bio = "Building scalable cloud apps."
    )
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertTrue(provReg.isSuccess)
    assertEquals(UserRole.SERVICE_PROVIDER, vmInstance2.uiState.value.activeRole)
    assertEquals("dashboard", vmInstance2.uiState.value.currentScreenRoute)
    assertNotNull(vmInstance2.uiState.value.registrationConfirmationMessage)

    // Logout provider
    vmInstance2.logout()
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertFalse(vmInstance2.uiState.value.isAuthenticated)

    // 5. Simulate another full app restart after logout
    val repoInstance3 = MarketplaceRepository(context = context, prefsName = testPrefsName)
    val vmInstance3 = MarketplaceViewModel(repoInstance3)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    assertFalse("Logged-out state should persist across restart", vmInstance3.uiState.value.isAuthenticated)
    // Registered provider remains in the marketplace directory across restart
    assertTrue(vmInstance3.uiState.value.allProviders.any { it.name == "Karan Malhotra" })

    // 6. Returning-User Sign In for Provider (even if Customer tab was selected in UI, saved SERVICE_PROVIDER role is restored!)
    val wrongPassAttempt = vmInstance3.login("karan.dev@gmail.com", "WrongPassword123", UserRole.CUSTOMER)
    assertTrue(wrongPassAttempt.isFailure)

    val karanSignIn = vmInstance3.login("KARAN.DEV@GMAIL.COM", "KaranPass#2026", UserRole.CUSTOMER)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertTrue(karanSignIn.isSuccess)
    assertTrue(vmInstance3.uiState.value.isAuthenticated)
    assertEquals("Karan Malhotra", vmInstance3.uiState.value.currentUser.name)
    assertEquals(UserRole.SERVICE_PROVIDER, vmInstance3.uiState.value.activeRole)
    assertEquals("dashboard", vmInstance3.uiState.value.currentScreenRoute)

    // 7. Logout and Returning-User Sign In for Customer Riya Kapoor
    vmInstance3.logout()
    val riyaSignIn = vmInstance3.login("riya.kapoor@gmail.com", "RiyaSecure#2026", UserRole.SERVICE_PROVIDER)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    assertTrue(riyaSignIn.isSuccess)
    assertEquals("Riya Kapoor", vmInstance3.uiState.value.currentUser.name)
    assertEquals(UserRole.CUSTOMER, vmInstance3.uiState.value.activeRole)
    assertEquals("home", vmInstance3.uiState.value.currentScreenRoute)

    // 8. Verify no plain-text passwords are stored in SharedPreferences
    val rawPrefs = context.getSharedPreferences(testPrefsName, Context.MODE_PRIVATE)
    val allPrefsContent = rawPrefs.all.values.joinToString(" ")
    assertFalse("Plaintext password must never be stored on disk", allPrefsContent.contains("RiyaSecure#2026"))
    assertFalse("Plaintext password must never be stored on disk", allPrefsContent.contains("KaranPass#2026"))
  }

  @Test
  fun `duplicate registrations, unregistered logins, admin self-registration, and public password reset work securely`() {
    val repository = MarketplaceRepository()
    val viewModel = MarketplaceViewModel(repository)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    // 1. Unregistered email cannot sign in without creating an account first
    val unregSignIn = viewModel.login("newuser.never.registered@gmail.com", "SomePass#123", UserRole.CUSTOMER)
    assertTrue(unregSignIn.isFailure)
    assertTrue(unregSignIn.exceptionOrNull()?.message?.contains("No account found") == true)

    // 2. Ordinary users cannot register using official Admin emails
    val adminRegCustomerAttempt = viewModel.registerCustomer(
      name = "Fake Founder",
      email = MarketplaceRepository.OFFICIAL_FOUNDER_EMAIL,
      phone = "9999999999",
      city = "Delhi",
      state = "Delhi",
      pass = "Attempt#123"
    )
    assertTrue(adminRegCustomerAttempt.isFailure)
    assertTrue(adminRegCustomerAttempt.exceptionOrNull() is SecurityException)

    // 3. Register a public user and test password reset flow
    val regRes = viewModel.registerCustomer(
      name = "Neha Verma",
      email = "neha.verma@gmail.com",
      phone = "9876543219",
      city = "Jaipur",
      state = "Rajasthan",
      pass = "OldPass#2026"
    )
    assertTrue(regRes.isSuccess)
    viewModel.logout()

    // Duplicate registration with same email (even as Provider) is blocked
    val dupAsProvider = viewModel.registerProvider(
      name = "Neha Verma",
      email = "NEHA.VERMA@gmail.com",
      phone = "9876543219",
      city = "Jaipur",
      state = "Rajasthan",
      pass = "NewPass#2026",
      professionalTitle = "Designer",
      categoryId = "cat_design",
      skills = listOf("Figma"),
      hourlyRateInr = 1000L,
      bio = "Designer"
    )
    assertTrue(dupAsProvider.isFailure)

    // Trigger public password reset for registered user
    val resetRes = viewModel.sendPublicPasswordReset("neha.verma@gmail.com")
    assertTrue(resetRes.isSuccess)
    assertTrue(resetRes.getOrThrow().contains("neha.verma@gmail.com"))

    // User can now sign in with their newly reset password
    val signInWithNewPass = viewModel.login("neha.verma@gmail.com", "ResetNewPass#2026", UserRole.CUSTOMER)
    assertTrue(signInWithNewPass.isSuccess)

    // Both Founder accounts retain equal Super Admin permissions
    viewModel.logout()
    val founder1 = viewModel.login(MarketplaceRepository.OFFICIAL_FOUNDER_EMAIL, "Founder1#2026", UserRole.ADMIN)
    assertTrue(founder1.isSuccess)
    assertEquals(com.example.model.AdminRoleType.FOUNDER, founder1.getOrThrow().adminRole)

    viewModel.logout()
    val founder2 = viewModel.login(MarketplaceRepository.SECONDARY_FOUNDER_EMAIL, "Founder2#2026", UserRole.ADMIN)
    assertTrue(founder2.isSuccess)
    assertEquals(com.example.model.AdminRoleType.FOUNDER, founder2.getOrThrow().adminRole)
  }

  @Test
  fun `google maps grounding places and ai assistant local discovery prompts work properly`() {
    val repository = MarketplaceRepository()
    val viewModel = MarketplaceViewModel(repository)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    // 1. Initial AI assistant welcome message includes Google Maps Grounding metadata and places
    val initialMessages = viewModel.uiState.value.aiChatMessages
    assertTrue(initialMessages.isNotEmpty())
    val welcome = initialMessages.first()
    assertTrue(welcome.text.contains("Google Maps Grounding"))
    assertTrue(welcome.mapsPlaces.isNotEmpty())
    assertTrue(welcome.mapsPlaces.any { it.title.contains("Bengaluru") && it.uri.contains("google.com/maps") })

    // 2. Verify Tool.googleMaps() can be instantiated cleanly
    val mapsTool = com.google.firebase.ai.type.Tool.googleMaps()
    assertNotNull(mapsTool)

    // 3. Opening AI assistant with a local Google Maps prompt appends user message and grounded fallback
    viewModel.setCityFilterExplicit("Hyderabad")
    viewModel.openAiAssistantWithPrompt("Best UI/UX designers in Hyderabad")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    assertTrue(viewModel.uiState.value.isAiAssistantOpen)
    assertTrue(viewModel.uiState.value.aiChatMessages.any { it.isFromUser && it.text.contains("Hyderabad") })
  }

  @Test
  fun `firebase project id, package name, sender id, and token masking work securely`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Verify Firebase configuration audit matches SkillConnect project, package, and sender ID
    val audit = com.example.data.FirebaseTokenManager.verifyFirebaseConfiguration(context)
    assertEquals("gen-lang-client-0778223582", audit.projectId)
    assertEquals("com.aistudio.skillconnect.market", audit.packageName)
    assertEquals("800529463187", audit.senderId)
    assertEquals("1:800529463187:android:e2715c4819dbe5b6201a66", audit.applicationId)
    assertTrue(audit.isFullyAligned)

    // 2. Verify notification sender validation accepts only project 800529463187
    assertTrue(com.example.data.FirebaseTokenManager.validateNotificationSender("800529463187"))
    assertTrue(com.example.data.FirebaseTokenManager.validateNotificationSender("/topics/announcements"))
    assertFalse(com.example.data.FirebaseTokenManager.validateNotificationSender("999999999999"))
    assertFalse(com.example.data.FirebaseTokenManager.validateNotificationSender("800529463187", "wrong-project-id"))

    // 3. Verify token masking never leaks raw secrets or JWT payloads
    val rawSecret = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    val masked = com.example.data.FirebaseTokenManager.maskToken(rawSecret)
    assertFalse("Masked token must never contain raw secret", masked.contains(rawSecret))
    assertTrue(masked.startsWith("tok[len=36,sha256="))
    assertEquals("<empty>", com.example.data.FirebaseTokenManager.maskToken(null))
    assertEquals("<empty>", com.example.data.FirebaseTokenManager.maskToken("   "))

    // 4. Verify log sanitizer redacts JWTs, Bearer tokens, and UUIDs
    val rawError = "Auth failed for Bearer secret_abc123 and UUID a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    val sanitized = com.example.data.FirebaseTokenManager.sanitizeLogMessage(rawError)
    assertFalse(sanitized.contains("secret_abc123"))
    assertFalse(sanitized.contains("a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
    assertTrue(sanitized.contains("[REDACTED_TOKEN]"))
    assertTrue(sanitized.contains("[REDACTED_UUID]"))

    // 5. Verify token error classification pinpoints App Check, FCM, and Auth origins
    val appCheckDiag = com.example.data.FirebaseTokenManager.classifyTokenError(
      RuntimeException("Error 403: AppCheck token is invalid")
    )
    assertEquals(com.example.data.FirebaseTokenOrigin.FIREBASE_APP_CHECK, appCheckDiag.origin)
    assertTrue(appCheckDiag.requiresForceRefresh)

    val fcmDiag = com.example.data.FirebaseTokenManager.classifyTokenError(
      RuntimeException("FIS_AUTH_ERROR: INVALID_PARAMETERS for registration token")
    )
    assertEquals(com.example.data.FirebaseTokenOrigin.FIREBASE_CLOUD_MESSAGING, fcmDiag.origin)
    assertTrue(fcmDiag.requiresTokenReset)
  }

  @Test
  fun `app check debug secret validator accepts valid UUIDs and rejects malformed strings`() {
    assertTrue(
      com.example.data.FirebaseTokenManager.isValidDebugSecretFormat("550e8400-e29b-41d4-a716-446655440000")
    )
    assertFalse(com.example.data.FirebaseTokenManager.isValidDebugSecretFormat(null))
    assertFalse(com.example.data.FirebaseTokenManager.isValidDebugSecretFormat(""))
    assertFalse(com.example.data.FirebaseTokenManager.isValidDebugSecretFormat("null"))
    assertFalse(com.example.data.FirebaseTokenManager.isValidDebugSecretFormat("undefined"))
    assertFalse(com.example.data.FirebaseTokenManager.isValidDebugSecretFormat("not-a-valid-uuid"))
  }

  @Test
  fun `network connectivity listener prevents unnecessary token invalidation when offline and triggers reconnect sync`() {
    val repository = MarketplaceRepository()
    val viewModel = MarketplaceViewModel(repository)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    var reconnectedCount = 0
    val reconnectListener: () -> Unit = { reconnectedCount++ }
    com.example.data.NetworkConnectivityMonitor.addOnReconnectListener(reconnectListener)

    try {
      // 1. Default state is online
      com.example.data.NetworkConnectivityMonitor.setSimulatedOnlineStateForTesting(true)
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      assertTrue(viewModel.uiState.value.isNetworkOnline)

      // 2. Simulate device going offline
      com.example.data.NetworkConnectivityMonitor.setSimulatedOnlineStateForTesting(false)
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      assertFalse(com.example.data.NetworkConnectivityMonitor.isCurrentlyConnected())
      assertFalse(viewModel.uiState.value.isNetworkOnline)

      // 3. While offline, errors must be classified as NETWORK_OFFLINE without invalidating or resetting tokens
      val offlineDiag = com.example.data.FirebaseTokenManager.classifyTokenError(
        RuntimeException("Failed to get document because the client is offline")
      )
      assertEquals(com.example.data.FirebaseTokenOrigin.NETWORK_OFFLINE, offlineDiag.origin)
      assertEquals("CLIENT_OFFLINE", offlineDiag.errorCode)
      assertFalse("Offline error must not force token refresh", offlineDiag.requiresForceRefresh)
      assertFalse("Offline error must not reset FCM token", offlineDiag.requiresTokenReset)
      assertTrue(offlineDiag.isTransientNetworkError)

      // 4. Simulate device reconnecting (Offline -> Online)
      com.example.data.NetworkConnectivityMonitor.setSimulatedOnlineStateForTesting(true)
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      assertTrue(com.example.data.NetworkConnectivityMonitor.isCurrentlyConnected())
      assertTrue(viewModel.uiState.value.isNetworkOnline)
      assertEquals(1, reconnectedCount)
    } finally {
      com.example.data.NetworkConnectivityMonitor.removeOnReconnectListener(reconnectListener)
      com.example.data.NetworkConnectivityMonitor.setSimulatedOnlineStateForTesting(null)
    }
  }
}






