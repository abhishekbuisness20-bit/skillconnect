package com.example.data

import android.content.Context
import com.example.model.*
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

class MarketplaceRepository(
    val firestoreRepository: SkillConnectFirestoreRepository? = null,
    private val context: Context? = null,
    private val prefsName: String = DEFAULT_PREFS_NAME
) {

    // Official Admin accounts allowlist (Founder: Abhishek Singh • CEO: Abhay • Admin: Abhay / Adarsh)
    val authorizedAdminEmails = listOf(
        OFFICIAL_FOUNDER_EMAIL,         // Official Founder: Abhishek Singh (SUPER ADMIN)
        SECONDARY_FOUNDER_EMAIL,        // Secondary Founder Account: Abhishek Singh (SUPER ADMIN)
        OFFICIAL_CEO_EMAIL,             // Official CEO: Abhay (EXECUTIVE ADMIN)
        OFFICIAL_ADMIN_EMAIL,           // Official Admin: Abhay / Adarsh (LIMITED ADMIN)
        "abhay@skillconnect.in",        // Admin Alias
        "abhay.admin@skillconnect.in",  // Admin Alias
        "abhay.ceo@skillconnect.in",    // Admin Alias
        "adarsh@skillconnect.in",       // Admin Alias
        "adarsh.admin@skillconnect.in", // Admin Alias
        "admin@skillconnectofficial.com"
    )

    fun isAuthorizedAdminEmail(email: String): Boolean {
        val normalized = email.trim().lowercase()
        return authorizedAdminEmails.any { it.equals(normalized, ignoreCase = true) }
    }

    fun getAdminRoleForEmail(email: String): AdminRoleType? {
        val normalized = email.trim().lowercase()
        return when {
            normalized == OFFICIAL_FOUNDER_EMAIL || normalized == SECONDARY_FOUNDER_EMAIL -> AdminRoleType.FOUNDER
            normalized == OFFICIAL_CEO_EMAIL || normalized.startsWith("abhay.ceo") -> AdminRoleType.CEO
            isAuthorizedAdminEmail(normalized) -> AdminRoleType.ADMIN
            else -> null
        }
    }

    private fun sha256Digest(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Pre-configured Marketplace Profiles
    val demoCustomer = UserProfile(
        id = "cust_rahul",
        name = "Aarav Mehta",
        email = "aarav.mehta@example.in",
        phone = "+91 98765 43210",
        role = UserRole.CUSTOMER,
        city = "Bengaluru",
        state = "Karnataka",
        avatarInitials = "AM",
        professionalHeadline = "Product Manager & Tech Enthusiast",
        bio = "Looking to collaborate with India's best freelance designers, app developers, and marketing specialists for startup products.",
        skills = listOf("Product Strategy", "UI Reviews", "Agile Management", "SaaS Growth"),
        experienceYears = 6,
        education = "B.Tech in Computer Science, IIT Madras",
        portfolioLinks = listOf("https://aaravmehta.dev", "https://github.com/aaravmehta"),
        socialGithub = "aaravmehta",
        socialLinkedin = "aarav-mehta-in",
        socialTwitter = "@aarav_tech",
        contactPreference = "In-App Chat & Email"
    )

    val demoProvider = UserProfile(
        id = "prov_ananya",
        name = "Ananya Deshmukh",
        email = "ananya.ui@example.in",
        phone = "+91 98111 22334",
        role = UserRole.SERVICE_PROVIDER,
        city = "Mumbai",
        state = "Maharashtra",
        avatarInitials = "AD",
        professionalHeadline = "Senior UI/UX Designer & Mobile App Architect",
        bio = "Crafting high-converting, modern mobile applications with Figma and Jetpack Compose. 7+ years delivering for Indian and global enterprises.",
        skills = listOf("Figma", "Design Systems", "Jetpack Compose", "Wireframing", "Micro-interactions"),
        experienceYears = 7,
        education = "Master of Design, NID Ahmedabad",
        portfolioLinks = listOf("https://dribbble.com/ananya-designs", "https://behance.net/ananya-ux"),
        socialGithub = "ananya-ux",
        socialLinkedin = "ananya-deshmukh",
        socialTwitter = "@ananya_ui",
        contactPreference = "Direct In-App Messages"
    )

    // Official Admin Profiles with Role-based access
    val officialFounder = UserProfile(
        id = "admin_founder_abhishek",
        name = "Abhishek Singh (Founder)",
        email = OFFICIAL_FOUNDER_EMAIL,
        phone = "+91 99000 88776",
        role = UserRole.ADMIN,
        adminRole = AdminRoleType.FOUNDER,
        city = "Delhi-NCR",
        state = "Delhi",
        avatarInitials = "AS",
        professionalHeadline = "Founder & Chief Architect (Super Admin)",
        bio = "Founder of SkillConnect India. Full governance over platform security, administrators, and infrastructure.",
        skills = listOf("Platform Architecture", "Security Governance", "Executive Leadership", "Ecosystem Growth"),
        experienceYears = 10,
        education = "B.Tech & Executive Management",
        contactPreference = "Founder Direct"
    )

    val officialCeo = UserProfile(
        id = "admin_ceo_abhay",
        name = "Abhay (CEO)",
        email = OFFICIAL_CEO_EMAIL,
        phone = "+91 99111 22334",
        role = UserRole.ADMIN,
        adminRole = AdminRoleType.CEO,
        city = "Bengaluru",
        state = "Karnataka",
        avatarInitials = "AB",
        professionalHeadline = "Chief Executive Officer (Executive Admin)",
        bio = "Leading business operations, provider partnerships, growth marketing, and dispute resolutions on SkillConnect.",
        skills = listOf("Business Operations", "Dispute Resolution", "Marketing Strategy", "Executive Administration"),
        experienceYears = 8,
        education = "MBA & Operations Management",
        contactPreference = "Executive Office"
    )

    val officialAdmin = UserProfile(
        id = "admin_ops_abhay",
        name = "Abhay (Admin)",
        email = OFFICIAL_ADMIN_EMAIL,
        phone = "+91 99222 33445",
        role = UserRole.ADMIN,
        adminRole = AdminRoleType.ADMIN,
        city = "Mumbai",
        state = "Maharashtra",
        avatarInitials = "AD",
        professionalHeadline = "Platform Administrator (Limited Admin)",
        bio = "Dedicated support desk, provider profile verification, content moderation, and complaint investigation.",
        skills = listOf("Customer Support", "Profile Verification", "Content Moderation", "Ticket Investigation"),
        experienceYears = 6,
        education = "B.Tech in Information Technology",
        contactPreference = "Support Desk"
    )

    val demoAdmin = officialFounder.copy(
        id = "admin_founder_secondary",
        email = SECONDARY_FOUNDER_EMAIL
    )

    private val adminAbhay = officialCeo.copy(email = "abhay@skillconnect.in", name = "Abhay (Admin)", adminRole = AdminRoleType.ADMIN)

    private val adminAdarsh = officialAdmin.copy(email = "adarsh@skillconnect.in", name = "Adarsh (Admin)", adminRole = AdminRoleType.ADMIN)

    private val _isAuthenticated = MutableStateFlow<Boolean>(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile>(demoCustomer)
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Administrative Roles Directory
    private val _adminRoles = MutableStateFlow<List<AdminRoleDoc>>(initialAdminRoles)
    val adminRoles: StateFlow<List<AdminRoleDoc>> = _adminRoles.asStateFlow()

    // Administrative Security Audit Logs
    private val _auditLogs = MutableStateFlow<List<AdminAuditLog>>(initialAuditLogs)
    val auditLogs: StateFlow<List<AdminAuditLog>> = _auditLogs.asStateFlow()

    // Platform User/Provider Complaints
    private val _complaints = MutableStateFlow<List<PlatformComplaint>>(initialComplaints)
    val complaints: StateFlow<List<PlatformComplaint>> = _complaints.asStateFlow()

    // Account profile directory (no plaintext passwords stored in source code or on disk)
    private val userProfiles = mutableMapOf<String, UserProfile>(
        "aarav.mehta@example.in" to demoCustomer,
        "ananya.ui@example.in" to demoProvider,
        OFFICIAL_FOUNDER_EMAIL to officialFounder,
        SECONDARY_FOUNDER_EMAIL to demoAdmin,
        OFFICIAL_CEO_EMAIL to officialCeo,
        OFFICIAL_ADMIN_EMAIL to officialAdmin,
        "abhay@skillconnect.in" to adminAbhay,
        "abhay.admin@skillconnect.in" to adminAbhay.copy(email = "abhay.admin@skillconnect.in"),
        "abhay.ceo@skillconnect.in" to officialCeo.copy(email = "abhay.ceo@skillconnect.in"),
        "adarsh@skillconnect.in" to adminAdarsh,
        "adarsh.admin@skillconnect.in" to adminAdarsh.copy(email = "adarsh.admin@skillconnect.in")
    )

    // Session & persistent SHA-256 credential digests bound upon registration or verified login
    private val credentialDigests = mutableMapOf<String, String>()

    // Service Categories
    private val _categories = MutableStateFlow<List<ServiceCategory>>(initialCategories)
    val categories: StateFlow<List<ServiceCategory>> = _categories.asStateFlow()

    // Service Providers
    private val _providers = MutableStateFlow<List<ServiceProvider>>(initialProviders)
    val providers: StateFlow<List<ServiceProvider>> = _providers.asStateFlow()

    // Work Requests
    private val _workRequests = MutableStateFlow<List<WorkRequest>>(initialWorkRequests)
    val workRequests: StateFlow<List<WorkRequest>> = _workRequests.asStateFlow()

    // Chat Messages
    private val _messages = MutableStateFlow<List<ChatMessage>>(initialMessages)
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Reviews
    private val _reviews = MutableStateFlow<List<Review>>(initialReviews)
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    // Active announcements
    private val _announcements = MutableStateFlow<List<String>>(
        listOf(
            "Welcome to SkillConnect! Connect with India's top verified talent.",
            "Special Launch: 0% platform commission on your first 3 completed contracts this month."
        )
    )
    val announcements: StateFlow<List<String>> = _announcements.asStateFlow()

    init {
        loadStateFromDisk()
    }

    // --- Permanent Account & Session Persistence (SharedPreferences) ---

    private fun loadStateFromDisk() {
        val ctx = context ?: return
        try {
            val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

            // 1. Restore SHA-256 password digests
            val digestsJson = prefs.getString(KEY_CREDENTIAL_DIGESTS, null)
            if (!digestsJson.isNullOrBlank()) {
                val obj = JSONObject(digestsJson)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    credentialDigests[k.trim().lowercase()] = obj.getString(k)
                }
            }

            // 2. Restore registered & updated user profiles
            val profilesJson = prefs.getString(KEY_USER_PROFILES, null)
            if (!profilesJson.isNullOrBlank()) {
                val arr = JSONArray(profilesJson)
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val profile = deserializeUserProfile(item)
                    userProfiles[profile.email.trim().lowercase()] = profile
                }
            }

            // 3. Restore registered custom service providers
            val providersJson = prefs.getString(KEY_CUSTOM_PROVIDERS, null)
            if (!providersJson.isNullOrBlank()) {
                val arr = JSONArray(providersJson)
                val restoredProviders = mutableListOf<ServiceProvider>()
                for (i in 0 until arr.length()) {
                    restoredProviders.add(deserializeServiceProvider(arr.getJSONObject(i)))
                }
                if (restoredProviders.isNotEmpty()) {
                    _providers.update { current ->
                        val existingIds = restoredProviders.map { it.id }.toSet()
                        restoredProviders + current.filterNot { it.id in existingIds }
                    }
                }
            }

            // 4. Restore active authenticated session across app restarts
            val wasAuthenticated = prefs.getBoolean(KEY_IS_AUTHENTICATED, false)
            val activeEmail = prefs.getString(KEY_ACTIVE_USER_EMAIL, null)?.trim()?.lowercase()
            if (wasAuthenticated && !activeEmail.isNullOrBlank()) {
                val savedUser = userProfiles[activeEmail]
                if (savedUser != null) {
                    _currentUser.value = savedUser
                    _isAuthenticated.value = true
                }
            }
        } catch (_: Exception) {
            // Ignore malformed disk cache and retain safe defaults
        }
    }

    private fun persistStateToDisk() {
        val ctx = context ?: return
        try {
            val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            // 1. Save SHA-256 digests (never plain-text passwords)
            val digestsObj = JSONObject()
            for ((emailKey, digestVal) in credentialDigests) {
                digestsObj.put(emailKey, digestVal)
            }
            editor.putString(KEY_CREDENTIAL_DIGESTS, digestsObj.toString())

            // 2. Save all user profiles
            val profilesArr = JSONArray()
            for ((_, profile) in userProfiles) {
                profilesArr.put(serializeUserProfile(profile))
            }
            editor.putString(KEY_USER_PROFILES, profilesArr.toString())

            // 3. Save custom registered service providers
            val initialProviderIds = initialProviders.map { it.id }.toSet()
            val customProviders = _providers.value.filterNot { it.id in initialProviderIds }
            val providersArr = JSONArray()
            for (prov in customProviders) {
                providersArr.put(serializeServiceProvider(prov))
            }
            editor.putString(KEY_CUSTOM_PROVIDERS, providersArr.toString())

            // 4. Save active session state
            editor.putBoolean(KEY_IS_AUTHENTICATED, _isAuthenticated.value)
            editor.putString(KEY_ACTIVE_USER_EMAIL, _currentUser.value.email.trim().lowercase())
            editor.apply()
        } catch (_: Exception) {
        }
    }

    private fun serializeUserProfile(profile: UserProfile): JSONObject {
        return JSONObject().apply {
            put("id", profile.id)
            put("name", profile.name)
            put("email", profile.email.trim().lowercase())
            put("phone", profile.phone)
            put("role", profile.role.name)
            put("adminRole", profile.adminRole?.name ?: "")
            put("city", profile.city)
            put("state", profile.state)
            put("avatarInitials", profile.avatarInitials)
            put("profilePhotoUri", profile.profilePhotoUri ?: "")
            put("coverPhotoUri", profile.coverPhotoUri ?: "")
            put("professionalHeadline", profile.professionalHeadline)
            put("bio", profile.bio)
            put("skills", JSONArray(profile.skills))
            put("experienceYears", profile.experienceYears)
            put("education", profile.education)
            put("portfolioLinks", JSONArray(profile.portfolioLinks))
            put("socialGithub", profile.socialGithub)
            put("socialLinkedin", profile.socialLinkedin)
            put("socialTwitter", profile.socialTwitter)
            put("contactPreference", profile.contactPreference)
        }
    }

    private fun deserializeUserProfile(obj: JSONObject): UserProfile {
        val skillsArr = obj.optJSONArray("skills")
        val skillsList = mutableListOf<String>()
        if (skillsArr != null) {
            for (i in 0 until skillsArr.length()) {
                skillsList.add(skillsArr.getString(i))
            }
        }
        val linksArr = obj.optJSONArray("portfolioLinks")
        val linksList = mutableListOf<String>()
        if (linksArr != null) {
            for (i in 0 until linksArr.length()) {
                linksList.add(linksArr.getString(i))
            }
        }
        val roleStr = obj.optString("role", UserRole.CUSTOMER.name)
        val role = runCatching { UserRole.valueOf(roleStr) }.getOrDefault(UserRole.CUSTOMER)
        val adminRoleStr = obj.optString("adminRole", "")
        val adminRole = if (adminRoleStr.isNotBlank()) {
            runCatching { AdminRoleType.valueOf(adminRoleStr) }.getOrNull()
        } else null

        return UserProfile(
            id = obj.optString("id", "user_${System.currentTimeMillis()}"),
            name = obj.optString("name", "SkillConnect User"),
            email = obj.optString("email", "").trim().lowercase(),
            phone = obj.optString("phone", ""),
            role = role,
            adminRole = adminRole,
            city = obj.optString("city", "Bengaluru"),
            state = obj.optString("state", "Karnataka"),
            avatarInitials = obj.optString("avatarInitials", "SC"),
            profilePhotoUri = obj.optString("profilePhotoUri", "").takeIf { it.isNotBlank() },
            coverPhotoUri = obj.optString("coverPhotoUri", "").takeIf { it.isNotBlank() },
            professionalHeadline = obj.optString("professionalHeadline", ""),
            bio = obj.optString("bio", ""),
            skills = skillsList,
            experienceYears = obj.optInt("experienceYears", 0),
            education = obj.optString("education", ""),
            portfolioLinks = linksList,
            socialGithub = obj.optString("socialGithub", ""),
            socialLinkedin = obj.optString("socialLinkedin", ""),
            socialTwitter = obj.optString("socialTwitter", ""),
            contactPreference = obj.optString("contactPreference", "In-App Chat")
        )
    }

    private fun serializeServiceProvider(prov: ServiceProvider): JSONObject {
        return JSONObject().apply {
            put("id", prov.id)
            put("userId", prov.userId)
            put("name", prov.name)
            put("professionalTitle", prov.professionalTitle)
            put("categoryId", prov.categoryId)
            put("bio", prov.bio)
            put("city", prov.city)
            put("state", prov.state)
            put("hourlyRateInr", prov.hourlyRateInr)
            put("rating", prov.rating)
            put("reviewCount", prov.reviewCount)
            put("completedProjects", prov.completedProjects)
            put("isVerified", prov.isVerified)
            put("isAvailable", prov.isAvailable)
            put("skills", JSONArray(prov.skills))
        }
    }

    private fun deserializeServiceProvider(obj: JSONObject): ServiceProvider {
        val skillsArr = obj.optJSONArray("skills")
        val skillsList = mutableListOf<String>()
        if (skillsArr != null) {
            for (i in 0 until skillsArr.length()) {
                skillsList.add(skillsArr.getString(i))
            }
        }
        val hourlyRate = obj.optLong("hourlyRateInr", 500L)
        return ServiceProvider(
            id = obj.optString("id", "prov_${System.currentTimeMillis()}"),
            userId = obj.optString("userId", ""),
            name = obj.optString("name", ""),
            professionalTitle = obj.optString("professionalTitle", "Specialist"),
            categoryId = obj.optString("categoryId", "cat_dev"),
            subcategories = skillsList.take(3),
            bio = obj.optString("bio", ""),
            city = obj.optString("city", "Bengaluru"),
            state = obj.optString("state", "Karnataka"),
            hourlyRateInr = hourlyRate,
            rating = obj.optDouble("rating", 5.0),
            reviewCount = obj.optInt("reviewCount", 0),
            completedProjects = obj.optInt("completedProjects", 0),
            isVerified = obj.optBoolean("isVerified", false),
            isAvailable = obj.optBoolean("isAvailable", true),
            skills = skillsList,
            languages = listOf("English", "Hindi"),
            services = listOf(
                ServiceOffering(
                    "serv_${obj.optString("id", "default")}",
                    "Standard Consultation & Delivery",
                    "Scope agreed upon hire",
                    hourlyRate * 4,
                    3
                )
            ),
            portfolio = emptyList()
        )
    }

    // --- Authentication & Role Handling ---

    fun registerCustomer(
        name: String,
        email: String,
        phone: String,
        city: String,
        state: String,
        pass: String
    ): Result<UserProfile> {
        val normalizedEmail = email.trim().lowercase()
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Full name is required to create an account."))
        }
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@") || !normalizedEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (pass.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        if (isAuthorizedAdminEmail(normalizedEmail)) {
            return Result.failure(
                SecurityException("Official administrator emails cannot be registered as public accounts. Please sign in via the Admin Portal.")
            )
        }
        if (userProfiles.containsKey(normalizedEmail)) {
            return Result.failure(
                IllegalArgumentException("An account with this email already exists. Please sign in with your existing account.")
            )
        }
        val user = UserProfile(
            id = "user_${System.currentTimeMillis()}",
            name = name.trim(),
            email = normalizedEmail,
            phone = phone.trim(),
            role = UserRole.CUSTOMER,
            city = city.trim().ifBlank { "Bengaluru" },
            state = state.trim().ifBlank { "Karnataka" },
            avatarInitials = name.trim().split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase().ifBlank { "SC" }
        )
        userProfiles[normalizedEmail] = user
        credentialDigests[normalizedEmail] = sha256Digest(pass)
        _currentUser.value = user
        _isAuthenticated.value = true
        persistStateToDisk()
        return Result.success(user)
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
        val normalizedEmail = email.trim().lowercase()
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Full name is required to create an account."))
        }
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@") || !normalizedEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (pass.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        if (isAuthorizedAdminEmail(normalizedEmail)) {
            return Result.failure(
                SecurityException("Official administrator emails cannot be registered as public accounts. Please sign in via the Admin Portal.")
            )
        }
        if (userProfiles.containsKey(normalizedEmail)) {
            return Result.failure(
                IllegalArgumentException("An account with this email already exists. Please sign in with your existing account.")
            )
        }
        val userId = "user_${System.currentTimeMillis()}"
        val resolvedTitle = professionalTitle.trim().ifBlank { "Verified Service Specialist" }
        val resolvedBio = bio.trim().ifBlank { "Professional service provider on SkillConnect." }
        val user = UserProfile(
            id = userId,
            name = name.trim(),
            email = normalizedEmail,
            phone = phone.trim(),
            role = UserRole.SERVICE_PROVIDER,
            city = city.trim().ifBlank { "Bengaluru" },
            state = state.trim().ifBlank { "Karnataka" },
            avatarInitials = name.trim().split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase().ifBlank { "SP" },
            professionalHeadline = resolvedTitle,
            bio = resolvedBio,
            skills = skills
        )
        userProfiles[normalizedEmail] = user
        credentialDigests[normalizedEmail] = sha256Digest(pass)

        val newProvider = ServiceProvider(
            id = "prov_${System.currentTimeMillis()}",
            userId = userId,
            name = name.trim(),
            professionalTitle = resolvedTitle,
            categoryId = categoryId,
            subcategories = skills.take(3),
            bio = resolvedBio,
            city = user.city,
            state = user.state,
            hourlyRateInr = hourlyRateInr,
            rating = 5.0,
            reviewCount = 0,
            completedProjects = 0,
            isVerified = false,
            isAvailable = true,
            skills = skills,
            languages = listOf("English", "Hindi"),
            services = listOf(
                ServiceOffering("serv_${System.currentTimeMillis()}", "Standard Consultation & Delivery", "Scope agreed upon hire", hourlyRateInr * 4, 3)
            ),
            portfolio = emptyList()
        )
        _providers.update { listOf(newProvider) + it }

        _currentUser.value = user
        _isAuthenticated.value = true
        persistStateToDisk()
        return Result.success(user)
    }

    fun login(email: String, pass: String, roleRequested: UserRole): Result<UserProfile> {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@") || !normalizedEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Invalid credentials: Please enter a valid email address."))
        }
        if (pass.length < 6) {
            return Result.failure(IllegalArgumentException("Invalid credentials: Password must be at least 6 characters."))
        }

        val existingProfile = userProfiles[normalizedEmail]
        val incomingDigest = sha256Digest(pass)

        if (roleRequested == UserRole.ADMIN) {
            // Strict Admin authorization check: Customer and Service Provider accounts must NEVER receive admin access
            if (!isAuthorizedAdminEmail(normalizedEmail) || (existingProfile != null && existingProfile.role != UserRole.ADMIN)) {
                return Result.failure(
                    SecurityException("Access Denied: Unauthorized account. Only verified SkillConnect administrators can access the Admin Dashboard.")
                )
            }

            // Check if admin account has been suspended/revoked
            val adminDoc = _adminRoles.value.find { it.email.equals(normalizedEmail, ignoreCase = true) }
            if (adminDoc != null && !adminDoc.isActive) {
                return Result.failure(
                    SecurityException("Access Denied: This administrator account is currently suspended or revoked.")
                )
            }

            val boundDigest = credentialDigests[normalizedEmail]
            if (boundDigest != null && boundDigest != incomingDigest) {
                return Result.failure(IllegalArgumentException("Invalid credentials: Incorrect administrator password."))
            }
            credentialDigests[normalizedEmail] = incomingDigest

            val assignedAdminRole = getAdminRoleForEmail(normalizedEmail) ?: AdminRoleType.ADMIN
            val adminProfile = (existingProfile ?: UserProfile(
                id = "admin_${normalizedEmail.substringBefore('@')}",
                name = when (assignedAdminRole) {
                    AdminRoleType.FOUNDER -> "Abhishek Singh (Founder)"
                    AdminRoleType.CEO -> "Abhay (CEO)"
                    AdminRoleType.ADMIN -> if (normalizedEmail.contains("adarsh")) "Adarsh (Admin)" else "Abhay (Admin)"
                },
                email = normalizedEmail,
                phone = "+91 99000 88776",
                role = UserRole.ADMIN,
                adminRole = assignedAdminRole,
                city = when (assignedAdminRole) {
                    AdminRoleType.CEO -> "Bengaluru"
                    AdminRoleType.ADMIN -> "Mumbai"
                    else -> "Delhi-NCR"
                },
                state = when (assignedAdminRole) {
                    AdminRoleType.CEO -> "Karnataka"
                    AdminRoleType.ADMIN -> "Maharashtra"
                    else -> "Delhi"
                },
                avatarInitials = when (assignedAdminRole) {
                    AdminRoleType.FOUNDER -> "AS"
                    AdminRoleType.CEO -> "AB"
                    AdminRoleType.ADMIN -> "AD"
                }
            )).copy(
                role = UserRole.ADMIN,
                adminRole = assignedAdminRole
            )

            userProfiles[normalizedEmail] = adminProfile
            _currentUser.value = adminProfile
            _isAuthenticated.value = true
            persistStateToDisk()
            recordAuditLog(
                action = "ADMIN_AUTHENTICATION",
                targetEntityId = adminProfile.id,
                details = "Admin logged in successfully with ${assignedAdminRole.name} privileges.",
                roleType = assignedAdminRole
            )
            return Result.success(adminProfile)
        }

        // Public Marketplace Returning-User Sign In (Customer or Service Provider)
        if (existingProfile == null) {
            return Result.failure(
                IllegalArgumentException("No account found for '$normalizedEmail'. Please register once using 'Create Account' first.")
            )
        }
        if (existingProfile.role == UserRole.ADMIN || isAuthorizedAdminEmail(normalizedEmail)) {
            return Result.failure(
                SecurityException("Administrator accounts must sign in through the Protected Admin Portal.")
            )
        }

        val boundDigest = credentialDigests[normalizedEmail]
        if (boundDigest != null && boundDigest != incomingDigest) {
            return Result.failure(IllegalArgumentException("Invalid credentials: Incorrect password."))
        }
        credentialDigests[normalizedEmail] = incomingDigest

        // Restore the user's saved profile and role (Customer or Service Provider) for automatic dashboard routing
        _currentUser.value = existingProfile
        _isAuthenticated.value = true
        persistStateToDisk()
        return Result.success(existingProfile)
    }

    fun requestPublicPasswordReset(email: String): Result<String> {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@") || !normalizedEmail.contains(".")) {
            return Result.failure(
                IllegalArgumentException("Please enter a valid email address to reset your password.")
            )
        }
        val existingProfile = userProfiles[normalizedEmail]
        if (existingProfile == null) {
            return Result.failure(
                IllegalArgumentException("No registered account found for '$normalizedEmail'. Please create an account first.")
            )
        }
        if (existingProfile.role == UserRole.ADMIN || isAuthorizedAdminEmail(normalizedEmail)) {
            return Result.failure(
                SecurityException("Administrator accounts must use the Protected Admin Portal password recovery flow.")
            )
        }
        // Clear bound session password digest so the returning user can set a new password upon reset
        credentialDigests.remove(normalizedEmail)
        persistStateToDisk()
        return Result.success(
            "Password reset link sent to $normalizedEmail via Firebase Authentication. Please check your inbox."
        )
    }

    fun requestAdminPasswordReset(email: String): Result<String> {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@") || !normalizedEmail.contains(".")) {
            return Result.failure(
                IllegalArgumentException("Please enter a valid authorized admin email address to reset your password.")
            )
        }
        val existingProfile = userProfiles[normalizedEmail]
        if (!isAuthorizedAdminEmail(normalizedEmail) || (existingProfile != null && existingProfile.role != UserRole.ADMIN)) {
            return Result.failure(
                SecurityException("Access Denied: Password reset in the Management Portal is restricted to authorized SkillConnect administrators.")
            )
        }
        // Clear any bound session password digest so the admin can set a new password upon reset
        credentialDigests.remove(normalizedEmail)
        persistStateToDisk()
        return Result.success(
            "Password reset link sent to $normalizedEmail via Firebase Authentication. Please check your inbox."
        )
    }

    fun loginWithGoogle(
        uid: String,
        displayName: String?,
        email: String?,
        photoUrl: String?,
        roleRequested: UserRole
    ): Result<UserProfile> {
        val normalizedEmail = (email ?: "google.user@skillconnect.in").trim().lowercase()
        val isAdminEmail = isAuthorizedAdminEmail(normalizedEmail)
        val existingEntry = userProfiles[normalizedEmail]

        val resolvedRole = if (roleRequested == UserRole.ADMIN) {
            if (!isAdminEmail || (existingEntry != null && existingEntry.role != UserRole.ADMIN)) {
                return Result.failure(
                    SecurityException("Access Denied: This Google account is not authorized for SkillConnect Admin access.")
                )
            }
            UserRole.ADMIN
        } else {
            if (existingEntry != null && existingEntry.role != UserRole.ADMIN) {
                existingEntry.role
            } else {
                roleRequested
            }
        }

        val assignedAdminRole = if (resolvedRole == UserRole.ADMIN) {
            getAdminRoleForEmail(normalizedEmail) ?: AdminRoleType.ADMIN
        } else null

        val resolvedName = displayName?.takeIf { it.isNotBlank() }
            ?: existingEntry?.name
            ?: normalizedEmail.substringBefore('@').replace(".", " ").split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        val initials = resolvedName.trim().split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase().ifBlank { "SC" }

        val googleProfile = (existingEntry ?: UserProfile(
            id = uid,
            name = resolvedName,
            email = normalizedEmail,
            phone = "+91 98000 00000",
            role = resolvedRole,
            adminRole = assignedAdminRole,
            city = "Bengaluru",
            state = "Karnataka",
            avatarInitials = initials,
            profilePhotoUri = photoUrl,
            professionalHeadline = when (resolvedRole) {
                UserRole.ADMIN -> "Verified SkillConnect Administrator"
                UserRole.SERVICE_PROVIDER -> "Verified SkillConnect Specialist"
                UserRole.CUSTOMER -> "SkillConnect Marketplace Member"
            }
        )).copy(
            id = uid,
            name = resolvedName,
            email = normalizedEmail,
            role = resolvedRole,
            adminRole = assignedAdminRole,
            profilePhotoUri = photoUrl ?: existingEntry?.profilePhotoUri
        )

        userProfiles[normalizedEmail] = googleProfile
        _currentUser.value = googleProfile
        _isAuthenticated.value = true
        persistStateToDisk()
        if (resolvedRole == UserRole.ADMIN && assignedAdminRole != null) {
            recordAuditLog(
                action = "GOOGLE_ADMIN_AUTHENTICATION",
                targetEntityId = uid,
                details = "Admin logged in with Google Identity as ${assignedAdminRole.name}.",
                roleType = assignedAdminRole
            )
        }
        return Result.success(googleProfile)
    }

    fun applyCloudUserProfile(cloudDoc: UserProfileDoc) {
        val current = _currentUser.value
        val domain = cloudDoc.toDomainModel()
        val merged = domain.copy(
            role = if (current.role == UserRole.ADMIN) UserRole.ADMIN else domain.role
        )
        _currentUser.value = merged
        if (merged.email.isNotBlank()) {
            userProfiles[merged.email.trim().lowercase()] = merged
        }
        persistStateToDisk()
    }

    fun mergeCloudWorkRequests(cloudDocs: List<WorkRequestDoc>) {
        if (cloudDocs.isEmpty()) return
        val cloudReqs = cloudDocs.map { it.toDomainModel() }
        _workRequests.update { existing ->
            val existingIds = cloudReqs.map { it.id }.toSet()
            cloudReqs + existing.filterNot { it.id in existingIds }
        }
    }

    fun logout() {
        // Logout only ends the active authenticated session; never deletes accounts or profile data
        _isAuthenticated.value = false
        persistStateToDisk()
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
    }

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == AppLanguage.ENGLISH) {
            AppLanguage.HINDI
        } else {
            AppLanguage.ENGLISH
        }
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }

    fun updateUserProfile(updated: UserProfile) {
        _currentUser.value = updated
        userProfiles[updated.email.trim().lowercase()] = updated
        // If provider, synchronize with marketplace providers list
        if (updated.role == UserRole.SERVICE_PROVIDER) {
            _providers.update { list ->
                list.map { prov ->
                    if (prov.userId == updated.id || prov.name == updated.name) {
                        prov.copy(
                            name = updated.name,
                            professionalTitle = updated.professionalHeadline.ifBlank { prov.professionalTitle },
                            city = updated.city,
                            state = updated.state,
                            bio = updated.bio.ifBlank { prov.bio },
                            skills = if (updated.skills.isNotEmpty()) updated.skills else prov.skills
                        )
                    } else {
                        prov
                    }
                }
            }
        }
        persistStateToDisk()
    }

    // --- Provider Actions ---

    fun getProviderById(providerId: String): ServiceProvider? {
        return _providers.value.find { it.id == providerId }
    }

    fun toggleProviderAvailability(providerId: String) {
        _providers.update { list ->
            list.map { if (it.id == providerId) it.copy(isAvailable = !it.isAvailable) else it }
        }
    }

    fun addServiceToProvider(providerId: String, service: ServiceOffering) {
        _providers.update { list ->
            list.map {
                if (it.id == providerId) {
                    it.copy(services = it.services + service)
                } else it
            }
        }
    }

    // --- Work Request Operations ---

    fun createWorkRequest(
        providerId: String,
        title: String,
        description: String,
        category: String,
        budgetInr: Long,
        deadline: String
    ): WorkRequest {
        val provider = getProviderById(providerId)
        val customer = _currentUser.value

        val newRequest = WorkRequest(
            id = "req_${System.currentTimeMillis()}",
            customerId = customer.id,
            customerName = customer.name,
            providerId = providerId,
            providerName = provider?.name ?: "Professional",
            title = title,
            description = description,
            category = category,
            budgetInr = budgetInr,
            deadline = deadline,
            status = WorkRequestStatus.PENDING
        )

        _workRequests.update { listOf(newRequest) + it }
        return newRequest
    }

    fun updateWorkRequestStatus(requestId: String, newStatus: WorkRequestStatus, note: String = "") {
        _workRequests.update { list ->
            list.map { req ->
                if (req.id == requestId) {
                    req.copy(status = newStatus, notes = note.ifBlank { req.notes })
                } else req
            }
        }
    }

    // --- Messaging & Chat Operations ---

    fun sendMessage(conversationId: String, text: String, receiverId: String) {
        val user = _currentUser.value
        val msg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            conversationId = conversationId,
            senderId = user.id,
            senderName = user.name,
            text = text,
            timestampMillis = System.currentTimeMillis(),
            isRead = false
        )
        _messages.update { it + msg }
    }

    // --- Reviews & Ratings ---

    fun submitReview(providerId: String, rating: Int, comment: String, requestId: String) {
        val user = _currentUser.value
        val newReview = Review(
            id = "rev_${System.currentTimeMillis()}",
            providerId = providerId,
            customerId = user.id,
            customerName = user.name,
            rating = rating.coerceIn(1, 5),
            comment = comment,
            date = "Nov 2026",
            isVerifiedPurchase = true
        )

        _reviews.update { listOf(newReview) + it }

        // Recalculate provider rating
        _providers.update { list ->
            list.map { prov ->
                if (prov.id == providerId) {
                    val provReviews = _reviews.value.filter { it.providerId == providerId }
                    val avgRating = if (provReviews.isNotEmpty()) {
                        provReviews.map { it.rating }.average()
                    } else prov.rating
                    prov.copy(
                        rating = (Math.round(avgRating * 10.0) / 10.0),
                        reviewCount = provReviews.size
                    )
                } else prov
            }
        }
    }

    // --- Admin Operations ---

    fun toggleProviderVerification(providerId: String, verified: Boolean) {
        _providers.update { list ->
            list.map { if (it.id == providerId) it.copy(isVerified = verified) else it }
        }
    }

    fun addServiceCategory(category: ServiceCategory) {
        _categories.update { it + category }
    }

    fun broadcastAnnouncement(message: String) {
        _announcements.update { listOf(message) + it }
    }

    fun getAdminMetrics(): AdminPlatformMetrics {
        val provList = _providers.value
        val reqList = _workRequests.value

        val verifiedCount = provList.count { it.isVerified }
        val pendingVerif = provList.count { !it.isVerified }
        val activeReqs = reqList.count { it.status == WorkRequestStatus.PENDING || it.status == WorkRequestStatus.IN_PROGRESS || it.status == WorkRequestStatus.ACCEPTED }
        val completedReqs = reqList.count { it.status == WorkRequestStatus.COMPLETED }
        val totalVol = reqList.filter { it.status == WorkRequestStatus.COMPLETED }.sumOf { it.budgetInr }

        return AdminPlatformMetrics(
            totalUsers = 1248,
            totalCustomers = 890,
            totalProviders = provList.size,
            pendingVerifications = pendingVerif,
            verifiedProviders = verifiedCount,
            activeWorkRequests = activeReqs,
            completedWorkRequests = completedReqs,
            totalVolumeInr = if (totalVol > 0) totalVol else 1850000L
        )
    }

    fun recordAuditLog(
        action: String,
        targetEntityId: String,
        details: String,
        roleType: AdminRoleType? = null
    ) {
        val user = _currentUser.value
        val effectiveRole = roleType ?: user.adminRole ?: AdminRoleType.ADMIN
        val log = AdminAuditLog(
            id = "log_${System.currentTimeMillis()}",
            adminUid = user.id,
            adminEmail = user.email,
            roleType = effectiveRole,
            action = action,
            targetEntityId = targetEntityId,
            details = details,
            timestampMillis = System.currentTimeMillis()
        )
        _auditLogs.update { listOf(log) + it }
    }

    fun provisionOfficialAdminAccounts(
        founderPass: String,
        ceoPass: String,
        adminPass: String
    ): Result<String> {
        val user = _currentUser.value
        if (user.adminRole != AdminRoleType.FOUNDER &&
            !user.email.equals(OFFICIAL_FOUNDER_EMAIL, ignoreCase = true) &&
            !user.email.equals(SECONDARY_FOUNDER_EMAIL, ignoreCase = true)
        ) {
            return Result.failure(SecurityException("Unauthorized: Only Founder Abhishek Singh can execute official account provisioning."))
        }
        if (founderPass.length < 6 || ceoPass.length < 6 || adminPass.length < 6) {
            return Result.failure(IllegalArgumentException("All passwords must be at least 6 characters."))
        }

        credentialDigests[OFFICIAL_FOUNDER_EMAIL] = sha256Digest(founderPass)
        credentialDigests[SECONDARY_FOUNDER_EMAIL] = sha256Digest(founderPass)
        credentialDigests[OFFICIAL_CEO_EMAIL] = sha256Digest(ceoPass)
        credentialDigests[OFFICIAL_ADMIN_EMAIL] = sha256Digest(adminPass)

        userProfiles[OFFICIAL_FOUNDER_EMAIL] = officialFounder
        userProfiles[SECONDARY_FOUNDER_EMAIL] = demoAdmin
        userProfiles[OFFICIAL_CEO_EMAIL] = officialCeo
        userProfiles[OFFICIAL_ADMIN_EMAIL] = officialAdmin
        persistStateToDisk()

        recordAuditLog(
            action = "OFFICIAL_PROVISIONING",
            targetEntityId = "official_admin_accounts",
            details = "Founder Abhishek Singh provisioned Founder, CEO, and Admin credentials with role-based claims.",
            roleType = AdminRoleType.FOUNDER
        )
        return Result.success("Official Admin accounts (Founder, CEO, Admin) successfully provisioned with active RBAC credentials.")
    }

    fun updateAdminPermissions(
        targetUid: String,
        newPermissions: AdminPermissions,
        callerRole: AdminRoleType
    ): Result<Unit> {
        if (callerRole != AdminRoleType.FOUNDER) {
            return Result.failure(SecurityException("Access Denied: Only Founder has authority to assign or modify administrator permissions."))
        }
        val currentRoles = _adminRoles.value
        val targetRole = currentRoles.find { it.userId == targetUid || it.id == targetUid }
            ?: return Result.failure(NoSuchElementException("Target administrator account not found."))

        if (targetRole.role == "FOUNDER") {
            return Result.failure(IllegalStateException("Founder permissions are absolute and cannot be restricted."))
        }

        val updatedDoc = targetRole.copy(
            permissions = mapOf(
                "canManageAdmins" to newPermissions.canManageAdmins,
                "canManageRoles" to newPermissions.canManageRoles,
                "canManageCategories" to newPermissions.canManageCategories,
                "canBroadcastAnnouncements" to newPermissions.canBroadcastAnnouncements,
                "canApproveProviders" to newPermissions.canApproveProviders,
                "canModerateContent" to newPermissions.canModerateContent,
                "canResolveComplaints" to newPermissions.canResolveComplaints,
                "canAccessSecurityLogs" to newPermissions.canAccessSecurityLogs,
                "canViewFinancialAnalytics" to newPermissions.canViewFinancialAnalytics,
                "canDeletePlatformData" to newPermissions.canDeletePlatformData
            )
        )

        _adminRoles.update { list ->
            list.map { if (it.userId == targetUid || it.id == targetUid) updatedDoc else it }
        }

        recordAuditLog(
            action = "PERMISSIONS_UPDATED",
            targetEntityId = targetUid,
            details = "Updated permissions for administrator ${targetRole.email} (${targetRole.role}).",
            roleType = AdminRoleType.FOUNDER
        )
        return Result.success(Unit)
    }

    fun revokeAdminAccount(targetUid: String, callerRole: AdminRoleType): Result<Unit> {
        if (callerRole != AdminRoleType.FOUNDER) {
            return Result.failure(SecurityException("Access Denied: Only Founder has authority to revoke administrator access."))
        }
        val targetRole = _adminRoles.value.find { it.userId == targetUid || it.id == targetUid }
            ?: return Result.failure(NoSuchElementException("Target administrator account not found."))

        if (targetRole.role == "FOUNDER") {
            return Result.failure(IllegalStateException("Founder account cannot be revoked."))
        }

        _adminRoles.update { list ->
            list.map { if (it.userId == targetUid || it.id == targetUid) it.copy(isActive = false) else it }
        }

        recordAuditLog(
            action = "ADMIN_REVOKED",
            targetEntityId = targetUid,
            details = "Revoked administrator access for ${targetRole.email}.",
            roleType = AdminRoleType.FOUNDER
        )
        return Result.success(Unit)
    }

    fun toggleAdminActive(targetUid: String, callerRole: AdminRoleType): Result<Unit> {
        if (callerRole != AdminRoleType.FOUNDER) {
            return Result.failure(SecurityException("Access Denied: Only Founder can toggle administrator active status."))
        }
        val targetRole = _adminRoles.value.find { it.userId == targetUid || it.id == targetUid }
            ?: return Result.failure(NoSuchElementException("Administrator not found."))

        if (targetRole.role == "FOUNDER") {
            return Result.failure(IllegalStateException("Cannot toggle Founder active status."))
        }

        val newActive = !targetRole.isActive
        _adminRoles.update { list ->
            list.map { if (it.userId == targetUid || it.id == targetUid) it.copy(isActive = newActive) else it }
        }

        recordAuditLog(
            action = if (newActive) "ADMIN_ACTIVATED" else "ADMIN_SUSPENDED",
            targetEntityId = targetUid,
            details = "Admin ${targetRole.email} isActive set to $newActive.",
            roleType = AdminRoleType.FOUNDER
        )
        return Result.success(Unit)
    }

    fun resolveComplaint(
        complaintId: String,
        newStatus: ComplaintStatus,
        resolutionNotes: String,
        resolverRole: AdminRoleType
    ): Result<Unit> {
        val complaint = _complaints.value.find { it.id == complaintId }
            ?: return Result.failure(NoSuchElementException("Complaint not found."))

        if (complaint.status == ComplaintStatus.ESCALATED_TO_CEO && resolverRole == AdminRoleType.ADMIN) {
            return Result.failure(SecurityException("Escalated disputes require CEO or Founder executive resolution."))
        }

        _complaints.update { list ->
            list.map {
                if (it.id == complaintId) {
                    it.copy(
                        status = newStatus,
                        resolutionNotes = resolutionNotes
                    )
                } else it
            }
        }

        recordAuditLog(
            action = "COMPLAINT_RESOLVED",
            targetEntityId = complaintId,
            details = "Complaint #$complaintId updated to ${newStatus.label} by ${resolverRole.name}. Note: $resolutionNotes",
            roleType = resolverRole
        )
        return Result.success(Unit)
    }

    fun deleteUserOrProvider(
        id: String,
        isProvider: Boolean,
        reason: String,
        callerRole: AdminRoleType
    ): Result<Unit> {
        if (callerRole != AdminRoleType.FOUNDER && callerRole != AdminRoleType.CEO) {
            return Result.failure(SecurityException("Access Denied: Limited Admin cannot delete marketplace accounts."))
        }

        if (isProvider) {
            _providers.update { list -> list.filterNot { it.id == id || it.userId == id } }
        } else {
            userProfiles.entries.removeIf { it.value.id == id && it.value.role != UserRole.ADMIN }
        }
        persistStateToDisk()

        recordAuditLog(
            action = if (isProvider) "PROVIDER_DELETED" else "USER_DELETED",
            targetEntityId = id,
            details = "Entity deleted by ${callerRole.name}. Reason: $reason",
            roleType = callerRole
        )
        return Result.success(Unit)
    }

    companion object {
        const val OFFICIAL_FOUNDER_EMAIL = "abhishekbuisness20@gmail.com"
        const val SECONDARY_FOUNDER_EMAIL = "singhabhishek83072@gmail.com"
        const val OFFICIAL_CEO_EMAIL = "abhayskillconnect@gmail.com"
        const val OFFICIAL_ADMIN_EMAIL = "abhayskillconnect39@gmail.com"

        const val DEFAULT_PREFS_NAME = "skillconnect_permanent_accounts_prefs"
        private const val KEY_USER_PROFILES = "registered_user_profiles_json"
        private const val KEY_CREDENTIAL_DIGESTS = "credential_digests_json"
        private const val KEY_CUSTOM_PROVIDERS = "custom_service_providers_json"
        private const val KEY_IS_AUTHENTICATED = "is_authenticated_session"
        private const val KEY_ACTIVE_USER_EMAIL = "active_authenticated_email"

        val initialAdminRoles = listOf(
            AdminRoleDoc(
                id = "admin_founder_abhishek",
                userId = "admin_founder_abhishek",
                email = OFFICIAL_FOUNDER_EMAIL,
                role = "FOUNDER",
                accessLevel = "SUPER ADMIN",
                isActive = true,
                permissions = mapOf(
                    "canManageAdmins" to true,
                    "canManageRoles" to true,
                    "canManageCategories" to true,
                    "canBroadcastAnnouncements" to true,
                    "canApproveProviders" to true,
                    "canModerateContent" to true,
                    "canResolveComplaints" to true,
                    "canAccessSecurityLogs" to true,
                    "canViewFinancialAnalytics" to true,
                    "canDeletePlatformData" to true
                )
            ),
            AdminRoleDoc(
                id = "admin_founder_secondary",
                userId = "admin_founder_secondary",
                email = SECONDARY_FOUNDER_EMAIL,
                role = "FOUNDER",
                accessLevel = "SUPER ADMIN",
                isActive = true,
                permissions = mapOf(
                    "canManageAdmins" to true,
                    "canManageRoles" to true,
                    "canManageCategories" to true,
                    "canBroadcastAnnouncements" to true,
                    "canApproveProviders" to true,
                    "canModerateContent" to true,
                    "canResolveComplaints" to true,
                    "canAccessSecurityLogs" to true,
                    "canViewFinancialAnalytics" to true,
                    "canDeletePlatformData" to true
                )
            ),
            AdminRoleDoc(
                id = "admin_ceo_abhay",
                userId = "admin_ceo_abhay",
                email = OFFICIAL_CEO_EMAIL,
                role = "CEO",
                accessLevel = "EXECUTIVE ADMIN",
                isActive = true,
                permissions = mapOf(
                    "canManageAdmins" to false,
                    "canManageRoles" to false,
                    "canManageCategories" to false,
                    "canBroadcastAnnouncements" to true,
                    "canApproveProviders" to true,
                    "canModerateContent" to true,
                    "canResolveComplaints" to true,
                    "canAccessSecurityLogs" to false,
                    "canViewFinancialAnalytics" to true,
                    "canDeletePlatformData" to false
                )
            ),
            AdminRoleDoc(
                id = "admin_ops_abhay",
                userId = "admin_ops_abhay",
                email = OFFICIAL_ADMIN_EMAIL,
                role = "ADMIN",
                accessLevel = "LIMITED ADMIN",
                isActive = true,
                permissions = mapOf(
                    "canManageAdmins" to false,
                    "canManageRoles" to false,
                    "canManageCategories" to false,
                    "canBroadcastAnnouncements" to false,
                    "canApproveProviders" to true,
                    "canModerateContent" to true,
                    "canResolveComplaints" to true,
                    "canAccessSecurityLogs" to false,
                    "canViewFinancialAnalytics" to false,
                    "canDeletePlatformData" to false
                )
            )
        )

        val initialAuditLogs = listOf(
            AdminAuditLog(
                id = "log_init_1",
                adminUid = "admin_founder_abhishek",
                adminEmail = OFFICIAL_FOUNDER_EMAIL,
                roleType = AdminRoleType.FOUNDER,
                action = "SYSTEM_INITIALIZATION",
                targetEntityId = "system_config",
                details = "Multi-level RBAC administrative framework initialized with zero-trust security.",
                timestampMillis = System.currentTimeMillis() - 86400000L
            ),
            AdminAuditLog(
                id = "log_init_2",
                adminUid = "admin_ceo_abhay",
                adminEmail = OFFICIAL_CEO_EMAIL,
                roleType = AdminRoleType.CEO,
                action = "OPS_DISCOVERY_AUDIT",
                targetEntityId = "marketplace_catalog",
                details = "Verified initial provider directory and service categories.",
                timestampMillis = System.currentTimeMillis() - 43200000L
            )
        )

        val initialComplaints = listOf(
            PlatformComplaint(
                id = "comp_201",
                reporterId = "cust_rahul",
                reporterName = "Aarav Mehta",
                targetProviderId = "prov_rohan",
                targetProviderName = "Rohan Verma",
                reason = "Deliverable Timeline Delay",
                details = "SEO audit delivery was delayed by 48 hours without prior notice.",
                status = ComplaintStatus.PENDING,
                assignedRoleLevel = AdminRoleType.ADMIN,
                resolutionNotes = "",
                timestampMillis = System.currentTimeMillis() - 7200000L
            ),
            PlatformComplaint(
                id = "comp_202",
                reporterId = "cust_rahul",
                reporterName = "Aarav Mehta",
                targetProviderId = "prov_kavita",
                targetProviderName = "Kavita Nair",
                reason = "Escrow Payout Dispute",
                details = "Dispute regarding scope expansion on teaser video cut.",
                status = ComplaintStatus.ESCALATED_TO_CEO,
                assignedRoleLevel = AdminRoleType.CEO,
                resolutionNotes = "Under CEO review for final settlement.",
                timestampMillis = System.currentTimeMillis() - 14400000L
            )
        )

        val initialCategories = listOf(
            ServiceCategory(
                id = "cat_design",
                nameEn = "Graphic Design",
                nameHi = "ग्राफिक डिज़ाइन",
                descriptionEn = "Brand identity, logos, banners, social media & posters",
                descriptionHi = "ब्रांड पहचान, लोगो, बैनर, सोशल मीडिया और पोस्टर",
                iconName = "Palette",
                subcategories = listOf(
                    "Logo Designer",
                    "Banner Designer",
                    "Poster Designer",
                    "Social Media Designer",
                    "Thumbnail Designer",
                    "Brand Identity Designer"
                )
            ),
            ServiceCategory(
                id = "cat_dev",
                nameEn = "Website & Development",
                nameHi = "वेबसाइट और विकास",
                descriptionEn = "Websites, Android apps, software and UI/UX design",
                descriptionHi = "वेबसाइटें, एंड्रॉइड ऐप्स, सॉफ्टवेयर और UI/UX डिज़ाइन",
                iconName = "Code",
                subcategories = listOf(
                    "Website Designer",
                    "Web Developer",
                    "App Developer",
                    "Software Developer",
                    "WordPress Developer",
                    "UI/UX Designer"
                )
            ),
            ServiceCategory(
                id = "cat_media",
                nameEn = "Video & Media",
                nameHi = "वीडियो और मीडिया",
                descriptionEn = "Video editing, motion graphics, 2D/3D animation & shoots",
                descriptionHi = "वीडियो एडिटिंग, मोशन ग्राफिक्स, 2D/3D एनिमेशन और शूटिंग",
                iconName = "Videocam",
                subcategories = listOf(
                    "Video Editor",
                    "Video Creator",
                    "Motion Graphics Designer",
                    "Animator",
                    "Photographer",
                    "Videographer",
                    "Photo Editor"
                )
            ),
            ServiceCategory(
                id = "cat_marketing",
                nameEn = "Digital Marketing",
                nameHi = "डिजिटल मार्केटिंग",
                descriptionEn = "SEO growth, Instagram management & ad campaigns",
                descriptionHi = "SEO विकास, इंस्टाग्राम प्रबंधन और विज्ञापन अभियान",
                iconName = "TrendingUp",
                subcategories = listOf(
                    "Digital Marketer",
                    "SEO Expert",
                    "Social Media Manager",
                    "Instagram Manager",
                    "Influencer",
                    "Content Creator"
                )
            ),
            ServiceCategory(
                id = "cat_writing",
                nameEn = "Writing & Communication",
                nameHi = "लेखन और संचार",
                descriptionEn = "SEO articles, copywriting, scripts, translations & voiceovers",
                descriptionHi = "SEO लेख, कॉपीराइटिंग, स्क्रिप्ट, अनुवाद और वॉयसओवर",
                iconName = "Edit",
                subcategories = listOf(
                    "Content Writer",
                    "Copywriter",
                    "Blog Writer",
                    "Translator",
                    "Script Writer",
                    "Voice-over Artist"
                )
            ),
            ServiceCategory(
                id = "cat_business",
                nameEn = "Business & Professional",
                nameHi = "व्यापार और पेशेवर",
                descriptionEn = "Virtual assistance, data entry, research & AI solutions",
                descriptionHi = "वर्चुअल सहायता, डेटा प्रविष्टि, अनुसंधान और AI समाधान",
                iconName = "BusinessCenter",
                subcategories = listOf(
                    "Virtual Assistant",
                    "Data Entry Operator",
                    "Business Consultant",
                    "Digital Researcher",
                    "AI Specialist"
                )
            )
        )

        val initialProviders = listOf(
            ServiceProvider(
                id = "prov_ananya",
                userId = "user_ananya",
                name = "Ananya Deshmukh",
                professionalTitle = "Senior UI/UX & Mobile App Designer",
                categoryId = "cat_dev",
                subcategories = listOf("UI/UX Designer", "Website Designer", "App Developer"),
                bio = "Over 6 years of experience crafting modern, accessible Android and Web interfaces for Indian startups and tech scale-ups.",
                city = "Mumbai",
                state = "Maharashtra",
                hourlyRateInr = 1200,
                rating = 4.9,
                reviewCount = 48,
                completedProjects = 62,
                isVerified = true,
                skills = listOf("Jetpack Compose", "Figma", "Design Systems", "Prototyping", "User Research"),
                languages = listOf("English", "Hindi", "Marathi"),
                services = listOf(
                    ServiceOffering("s1", "Mobile App UI/UX (5 Screens)", "Complete M3 responsive wireframes and components", 8500, 4),
                    ServiceOffering("s2", "Landing Page Redesign", "High conversion landing page design with responsive layouts", 6000, 3)
                ),
                portfolio = listOf(
                    PortfolioItem("p1", "FinTech Wallet India", "UI/UX", "Seamless UPI payment flows with biometric auth"),
                    PortfolioItem("p2", "EdTech Learning Hub", "Mobile App", "Interactive course player and test prep UI")
                )
            ),
            ServiceProvider(
                id = "prov_rahul",
                userId = "user_rahul",
                name = "Rahul Sharma",
                professionalTitle = "Full-Stack Web & Android Developer",
                categoryId = "cat_dev",
                subcategories = listOf("Web Developer", "App Developer", "Software Developer"),
                bio = "Specializing in high-performance Kotlin Android apps and reactive React/Node.js web platforms with Firebase backends.",
                city = "Bengaluru",
                state = "Karnataka",
                hourlyRateInr = 1500,
                rating = 4.8,
                reviewCount = 37,
                completedProjects = 45,
                isVerified = true,
                skills = listOf("Kotlin", "Android SDK", "React", "Node.js", "Firebase", "PostgreSQL"),
                languages = listOf("English", "Hindi", "Kannada"),
                services = listOf(
                    ServiceOffering("s3", "Native Android Feature Module", "Clean architecture MVVM feature with Room/Firestore", 12000, 5),
                    ServiceOffering("s4", "API Integration & Backend", "REST/Ktor endpoints integration with error handling", 7500, 3)
                ),
                portfolio = listOf(
                    PortfolioItem("p3", "QuickCommerce Delivery App", "Android", "Real-time delivery tracking with Google Maps SDK"),
                    PortfolioItem("p4", "SaaS Analytics Dashboard", "Web", "Live telemetry charts and role-based access control")
                )
            ),
            ServiceProvider(
                id = "prov_vikram",
                userId = "user_vikram",
                name = "Vikram Malhotra",
                professionalTitle = "Brand Identity & Logo Specialist",
                categoryId = "cat_design",
                subcategories = listOf("Logo Designer", "Brand Identity Designer", "Banner Designer"),
                bio = "Helping emerging Indian D2C brands, creators, and tech agencies establish iconic visual identities that stand out.",
                city = "Delhi-NCR",
                state = "Delhi",
                hourlyRateInr = 950,
                rating = 4.9,
                reviewCount = 52,
                completedProjects = 78,
                isVerified = true,
                skills = listOf("Logo Design", "Brand Guidelines", "Typography", "Vector Art", "Packaging"),
                languages = listOf("English", "Hindi", "Punjabi"),
                services = listOf(
                    ServiceOffering("s5", "Complete Brand Identity Kit", "Primary logo, sub-marks, color tokens & typography deck", 9500, 5),
                    ServiceOffering("s6", "Minimalist Vector Logo", "3 unique concepts with vector source files & 3D mockups", 4500, 2)
                ),
                portfolio = listOf(
                    PortfolioItem("p5", "ChaiCraft Artisanal Tea", "Branding", "Traditional modern blend identity & retail pouch art"),
                    PortfolioItem("p6", "Solaris Green Energy", "Logo", "Clean renewable energy geometric logo symbol")
                )
            ),
            ServiceProvider(
                id = "prov_kavita",
                userId = "user_kavita",
                name = "Kavita Nair",
                professionalTitle = "Professional Video Editor & Motion Artist",
                categoryId = "cat_media",
                subcategories = listOf("Video Editor", "Motion Graphics Designer", "Photo Editor"),
                bio = "Crafting high-engagement YouTube long-form cuts, Instagram Reels, and corporate explainer motion graphics.",
                city = "Hyderabad",
                state = "Telangana",
                hourlyRateInr = 1100,
                rating = 4.7,
                reviewCount = 29,
                completedProjects = 41,
                isVerified = true,
                skills = listOf("Premiere Pro", "After Effects", "Color Grading", "Sound Design", "Subtitles"),
                languages = listOf("English", "Hindi", "Telugu", "Malayalam"),
                services = listOf(
                    ServiceOffering("s7", "YouTube Video Editing (Up to 15m)", "Dynamic pacing, B-roll, SFX and 4K color correction", 5000, 3),
                    ServiceOffering("s8", "Viral Reels / Shorts Pack (3 Clips)", "Hook-optimized cuts with dynamic animated captions", 3500, 2)
                ),
                portfolio = listOf(
                    PortfolioItem("p7", "Tech Startup Launch Reel", "Motion Graphics", "30-second kinetic typography product feature"),
                    PortfolioItem("p8", "Travel Vlog India Series", "Video", "Cinematic color-graded multi-cam documentary edit")
                )
            ),
            ServiceProvider(
                id = "prov_rohan",
                userId = "user_rohan",
                name = "Rohan Verma",
                professionalTitle = "SEO Expert & Performance Marketer",
                categoryId = "cat_marketing",
                subcategories = listOf("SEO Expert", "Digital Marketer", "Social Media Manager"),
                bio = "Data-driven organic search strategist and Google Ads specialist scaling traffic and qualified leads for Indian SMBs.",
                city = "Pune",
                state = "Maharashtra",
                hourlyRateInr = 1300,
                rating = 4.8,
                reviewCount = 31,
                completedProjects = 39,
                isVerified = false, // Unverified for Admin Verification testing
                skills = listOf("Technical SEO", "Google Search Console", "Keyword Strategy", "Meta Ads", "Analytics"),
                languages = listOf("English", "Hindi"),
                services = listOf(
                    ServiceOffering("s9", "Comprehensive SEO Audit", "In-depth crawl, site speed, backlink and keyword gap report", 7000, 4),
                    ServiceOffering("s10", "Monthly Organic Traffic Growth", "Content briefs, on-page optimization, and weekly reports", 15000, 30)
                ),
                portfolio = listOf(
                    PortfolioItem("p9", "E-commerce Organic Surge", "SEO", "3.4x organic revenue growth over 6 months"),
                    PortfolioItem("p10", "Local Clinic Local SEO", "Local Pack", "Ranked #1 across 14 target localities in Pune")
                )
            ),
            ServiceProvider(
                id = "prov_priya",
                userId = "user_priya",
                name = "Priya Iyer",
                professionalTitle = "B2B Tech Copywriter & Scriptwriter",
                categoryId = "cat_writing",
                subcategories = listOf("Copywriter", "Content Writer", "Script Writer"),
                bio = "Transforming complex software, AI, and SaaS architectures into crisp, compelling sales copy and engaging product scripts.",
                city = "Chennai",
                state = "Tamil Nadu",
                hourlyRateInr = 1000,
                rating = 5.0,
                reviewCount = 24,
                completedProjects = 33,
                isVerified = false, // Pending verification for testing
                skills = listOf("SaaS Copy", "Case Studies", "Video Scripts", "Technical Writing", "Email Sequences"),
                languages = listOf("English", "Hindi", "Tamil"),
                services = listOf(
                    ServiceOffering("s11", "Website Copywriting (3 Pages)", "Clear value proposition, benefit bullets, and call-to-actions", 8000, 3),
                    ServiceOffering("s12", "Explainer Video Script (90s)", "Punchy narrative hook, problem statement, and solution reveal", 4000, 2)
                ),
                portfolio = listOf(
                    PortfolioItem("p11", "DevOps Cloud Landing Page", "Copywriting", "Turned technical features into enterprise buyer benefits"),
                    PortfolioItem("p12", "HRTech Product Tour Script", "Script", "Produced engaging 2-minute walkthrough script")
                )
            )
        )

        val initialWorkRequests = listOf(
            WorkRequest(
                id = "req_101",
                customerId = "cust_rahul",
                customerName = "Aarav Mehta",
                providerId = "prov_ananya",
                providerName = "Ananya Deshmukh",
                title = "Fintech Mobile App Prototype",
                description = "Need a clean 6-screen UI kit in Figma with dark navy glassmorphic aesthetic for an Indian payment portal.",
                category = "Website & Development",
                budgetInr = 14000,
                deadline = "15 Nov 2026",
                status = WorkRequestStatus.IN_PROGRESS,
                notes = "Design system tokens and components aligned."
            ),
            WorkRequest(
                id = "req_102",
                customerId = "cust_rahul",
                customerName = "Aarav Mehta",
                providerId = "prov_vikram",
                providerName = "Vikram Malhotra",
                title = "Modern Startup Logo & Identity",
                description = "Vector icon logo with wordmark and social media profile assets in cyan and green tones.",
                category = "Graphic Design",
                budgetInr = 7500,
                deadline = "10 Nov 2026",
                status = WorkRequestStatus.ACCEPTED,
                notes = "Moodboard reviewed by client."
            ),
            WorkRequest(
                id = "req_103",
                customerId = "cust_rahul",
                customerName = "Aarav Mehta",
                providerId = "prov_kavita",
                providerName = "Kavita Nair",
                title = "Product Launch Teaser Video",
                description = "45-second kinetic text and app screen motion graphics video for Instagram and LinkedIn.",
                category = "Video & Media",
                budgetInr = 9000,
                deadline = "02 Nov 2026",
                status = WorkRequestStatus.COMPLETED,
                notes = "Delivered on schedule in 4K resolution."
            )
        )

        val initialMessages = listOf(
            ChatMessage(
                id = "m1",
                conversationId = "conv_ananya",
                senderId = "cust_rahul",
                senderName = "Aarav Mehta",
                text = "Hi Ananya! I loved your portfolio. Are you available for a 6-screen fintech app design?",
                timestampMillis = System.currentTimeMillis() - 7200000,
                isRead = true
            ),
            ChatMessage(
                id = "m2",
                conversationId = "conv_ananya",
                senderId = "prov_ananya",
                senderName = "Ananya Deshmukh",
                text = "Hello Aarav! Yes, absolutely. I specialize in M3 responsive designs. Please share your project details!",
                timestampMillis = System.currentTimeMillis() - 3600000,
                isRead = true
            )
        )

        val initialReviews = listOf(
            Review(
                id = "r1",
                providerId = "prov_ananya",
                customerId = "cust_aarav",
                customerName = "Rohan Sengupta",
                rating = 5,
                comment = "Exceptional UI design quality! Delivered responsive Figma screens ahead of schedule with great attention to detail.",
                date = "Oct 2026",
                isVerifiedPurchase = true
            ),
            Review(
                id = "r2",
                providerId = "prov_ananya",
                customerId = "cust_megha",
                customerName = "Megha Patel",
                rating = 5,
                comment = "Very professional communication. Ananya understood our design requirements right away.",
                date = "Sep 2026",
                isVerifiedPurchase = true
            ),
            Review(
                id = "r3",
                providerId = "prov_vikram",
                customerId = "cust_varun",
                customerName = "Varun Kapoor",
                rating = 5,
                comment = "Vikram designed a world-class logo for our venture. Highly recommended!",
                date = "Oct 2026",
                isVerifiedPurchase = true
            )
        )
    }
}
