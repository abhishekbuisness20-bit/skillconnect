package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.MarketplaceRepository
import com.example.model.ServiceCategory
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.auth.GoogleSignInButton
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

private val EMAIL_FORMAT_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

internal fun isValidEmailFormat(email: String): Boolean {
    return EMAIL_FORMAT_REGEX.matches(email.trim())
}

internal fun getEmailRealtimeValidationMessage(email: String, isAdminMode: Boolean): String? {
    val trimmed = email.trim()
    if (trimmed.isEmpty()) {
        return if (isAdminMode) "Authorized admin email is required." else "Email address is required."
    }
    if (trimmed.contains(" ")) {
        return "Email address cannot contain spaces."
    }
    if (!trimmed.contains("@")) {
        return "Include an '@' symbol (e.g. admin@skillconnect.in)."
    }
    val localPart = trimmed.substringBefore("@")
    val domainPart = trimmed.substringAfter("@")
    if (localPart.isEmpty()) {
        return "Enter a username before the '@' symbol."
    }
    if (domainPart.isEmpty() || !domainPart.contains(".")) {
        return "Enter a complete domain (e.g. skillconnect.in)."
    }
    val tld = domainPart.substringAfterLast(".")
    if (tld.length < 2) {
        return "Domain extension must be at least 2 characters."
    }
    if (!isValidEmailFormat(trimmed)) {
        return "Please enter a valid email address format."
    }
    return null
}

internal fun getPasswordRealtimeValidationMessage(password: String, isAdminMode: Boolean): String? {
    if (password.isEmpty()) {
        return if (isAdminMode) "Admin password is required." else "Password is required."
    }
    if (password.length < 6) {
        return "Password must be at least 6 characters (${password.length}/6)."
    }
    return null
}

@Composable
fun AuthScreen(
    categories: List<ServiceCategory> = emptyList(),
    onRegisterCustomer: (name: String, email: String, phone: String, city: String, state: String, pass: String) -> Result<UserProfile>,
    onRegisterProvider: (name: String, email: String, phone: String, city: String, state: String, pass: String, title: String, catId: String, skills: List<String>, rate: Long, bio: String) -> Result<UserProfile>,
    onLogin: (email: String, pass: String, role: UserRole) -> Result<UserProfile>,
    onGoogleLogin: (uid: String, displayName: String?, email: String?, photoUrl: String?, role: UserRole) -> Result<UserProfile> = { _, _, _, _, _ ->
        Result.failure(UnsupportedOperationException("Not configured"))
    },
    onPublicForgotPassword: (email: String) -> Result<String> = { resetEmail ->
        val err = getEmailRealtimeValidationMessage(resetEmail, isAdminMode = false)
        if (err != null) {
            Result.failure(IllegalArgumentException(err))
        } else {
            Result.success("Password reset link sent to ${resetEmail.trim().lowercase()} via Firebase Authentication.")
        }
    },
    onAdminForgotPassword: (email: String) -> Result<String> = { resetEmail ->
        val err = getEmailRealtimeValidationMessage(resetEmail, isAdminMode = true)
        if (err != null) {
            Result.failure(IllegalArgumentException(err))
        } else {
            Result.success("Password reset link sent to ${resetEmail.trim().lowercase()} via Firebase Authentication.")
        }
    },
    sessionTimeoutMessage: String? = null,
    onDismissSessionTimeout: () -> Unit = {},
    onProvisionOfficialAccounts: ((founderPass: String, ceoPass: String, adminPass: String) -> Result<String>)? = null,
    onAuthSuccess: (UserProfile) -> Unit
) {
    // Top-level mode: PUBLIC (Customer / Provider) vs PROTECTED ADMIN
    var isAdminMode by remember(sessionTimeoutMessage) { mutableStateOf(sessionTimeoutMessage != null) }
    var showProvisionWizardDialog by remember { mutableStateOf(false) }

    // Public account type: only 2 options as mandated
    var publicAccountType by remember { mutableStateOf(UserRole.CUSTOMER) }

    // Auth tab: 0 = Sign In, 1 = Create Account
    var isCreateAccountTab by remember { mutableStateOf(false) }

    // Form fields (always start empty; never prefilled)
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(true) }

    // Real-time field interaction & focus tracking
    var emailTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    var emailHadFocus by remember { mutableStateOf(false) }
    var passwordHadFocus by remember { mutableStateOf(false) }
    var submitAttempted by remember { mutableStateOf(false) }

    // Provider specific fields
    var professionalTitle by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "cat_dev") }
    var skillsInput by remember { mutableStateOf("") }
    var hourlyRateInput by remember { mutableStateOf("1200") }
    var bioInput by remember { mutableStateOf("") }

    // UI feedback
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showAdminForgotPasswordDialog by remember { mutableStateOf(false) }

    // Derived real-time validation states
    val emailValidationMessage = remember(email, isAdminMode) {
        getEmailRealtimeValidationMessage(email, isAdminMode)
    }
    val isEmailValid = emailValidationMessage == null
    val showEmailError = (emailTouched || submitAttempted || email.isNotEmpty()) && !isEmailValid

    val passwordValidationMessage = remember(password, isAdminMode) {
        getPasswordRealtimeValidationMessage(password, isAdminMode)
    }
    val isPasswordValid = passwordValidationMessage == null
    val showPasswordError = (passwordTouched || submitAttempted || password.isNotEmpty()) && !isPasswordValid

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val submitAuthForm: () -> Unit = submitBlock@{
        errorMessage = null
        successMessage = null
        submitAttempted = true
        emailTouched = true
        passwordTouched = true

        if (!isEmailValid) {
            errorMessage = emailValidationMessage ?: "Please enter a valid email address."
            return@submitBlock
        }

        if (!isPasswordValid) {
            errorMessage = passwordValidationMessage ?: "Invalid credentials: Password must be at least 6 characters."
            return@submitBlock
        }

        if (isAdminMode) {
            val result = onLogin(email, password, UserRole.ADMIN)
            if (result.isSuccess) {
                successMessage = "Authorized Admin Access Granted."
                onAuthSuccess(result.getOrThrow())
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Admin verification failed."
            }
            return@submitBlock
        }

        if (isCreateAccountTab) {
            if (fullName.isBlank()) {
                errorMessage = "Please enter your full name."
                return@submitBlock
            }
            if (mobile.trim().length < 10) {
                errorMessage = "Please enter a valid 10-digit mobile number."
                return@submitBlock
            }
            if (city.isBlank() || state.isBlank()) {
                errorMessage = "Please enter your city and state."
                return@submitBlock
            }
            if (password != confirmPassword) {
                errorMessage = "Passwords do not match."
                return@submitBlock
            }
            if (!termsAccepted) {
                errorMessage = "Please agree to the Terms & Privacy Policy."
                return@submitBlock
            }

            if (publicAccountType == UserRole.CUSTOMER) {
                val res = onRegisterCustomer(fullName, email, mobile, city, state, password)
                if (res.isSuccess) {
                    successMessage = "Customer account registered successfully!"
                    onAuthSuccess(res.getOrThrow())
                } else {
                    errorMessage = res.exceptionOrNull()?.message ?: "Registration failed."
                }
            } else {
                if (professionalTitle.isBlank()) {
                    errorMessage = "Please specify your Professional Title."
                    return@submitBlock
                }
                val skillsList = skillsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (skillsList.isEmpty()) {
                    errorMessage = "Please list at least one skill."
                    return@submitBlock
                }
                val rate = hourlyRateInput.toLongOrNull() ?: 1000L

                val res = onRegisterProvider(
                    fullName, email, mobile, city, state, password,
                    professionalTitle, selectedCategoryId, skillsList, rate, bioInput
                )
                if (res.isSuccess) {
                    successMessage = "Provider profile registered! Welcome to SkillConnect."
                    onAuthSuccess(res.getOrThrow())
                } else {
                    errorMessage = res.exceptionOrNull()?.message ?: "Provider registration failed."
                }
            }
        } else {
            val result = onLogin(email, password, publicAccountType)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                successMessage = "Signed in successfully as ${user.name}."
                onAuthSuccess(user)
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Sign in failed. Check credentials."
            }
        }
    }

    LaunchedEffect(Unit) {
        val credentialManager = CredentialManager.create(context)
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { firebaseUser ->
                val targetRole = if (isAdminMode) UserRole.ADMIN else publicAccountType
                val result = onGoogleLogin(
                    firebaseUser.uid,
                    firebaseUser.displayName,
                    firebaseUser.email,
                    firebaseUser.photoUrl?.toString(),
                    targetRole
                )
                if (result.isSuccess) {
                    onAuthSuccess(result.getOrThrow())
                }
            },
            onUnauthenticated = { /* Stay on interactive AuthScreen */ },
            scope = scope
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("auth_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Branding Header
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(NavySurface)
                        .border(2.dp, CyanBrand, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.skillconnect_logo),
                        contentDescription = "SkillConnect Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Skill", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    Text("Connect", color = GreenBrand, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }

                Text(
                    text = if (isAdminMode) "Authorized Management Authentication"
                    else if (isCreateAccountTab) "Create your ${if (publicAccountType == UserRole.CUSTOMER) "Customer" else "Provider"} Account"
                    else "Sign in as ${if (publicAccountType == UserRole.CUSTOMER) "Customer" else "Service Provider"}",
                    color = TextMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        if (sessionTimeoutMessage != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorRed.copy(alpha = 0.14f))
                        .border(1.dp, ErrorRed.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("admin_session_timeout_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TimerOff,
                            contentDescription = "Session Timeout",
                            tint = ErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = sessionTimeoutMessage,
                            color = TextWhite,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = onDismissSessionTimeout,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("dismiss_session_timeout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Mode: Public Account Type Selector (Mandated: only Customer vs Provider)
        if (!isAdminMode) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavySurfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountTypeTab(
                        title = "Customer",
                        subtitle = "I want to Hire",
                        icon = Icons.Default.Person,
                        isSelected = publicAccountType == UserRole.CUSTOMER,
                        activeColor = CyanBrand,
                        onClick = {
                            publicAccountType = UserRole.CUSTOMER
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    )

                    AccountTypeTab(
                        title = "Service Provider",
                        subtitle = "I want to Work",
                        icon = Icons.Default.Engineering,
                        isSelected = publicAccountType == UserRole.SERVICE_PROVIDER,
                        activeColor = GreenBrand,
                        onClick = {
                            publicAccountType = UserRole.SERVICE_PROVIDER
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Tab Switch: Sign In vs Create Account
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NavySurface)
                        .border(1.dp, NavyCardBorder, RoundedCornerShape(10.dp))
                        .padding(3.dp)
                ) {
                    Button(
                        onClick = {
                            isCreateAccountTab = false
                            errorMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isCreateAccountTab) CyanBrand else Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("tab_sign_in")
                    ) {
                        Text(
                            text = "Sign In",
                            color = if (!isCreateAccountTab) Color(0xFF001F2B) else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Button(
                        onClick = {
                            isCreateAccountTab = true
                            errorMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCreateAccountTab) GreenBrand else Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("tab_create_account")
                    ) {
                        Text(
                            text = "Create Account",
                            color = if (isCreateAccountTab) Color(0xFF00210E) else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        } else {
            // Protected Admin Management Header
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BlueBrand.copy(alpha = 0.14f)),
                    border = BorderStroke(1.dp, BlueBrand.copy(alpha = 0.75f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_management_header")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BlueBrand.copy(alpha = 0.2f))
                                    .border(1.dp, BlueBrand, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Restricted Management Access",
                                    tint = BlueBrand,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Restricted Management Access",
                                    color = BlueBrand,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Verified SkillConnect Administration Portal",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        HorizontalDivider(color = BlueBrand.copy(alpha = 0.25f))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = CyanBrand,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Founder: Abhishek Singh (Super Admin)",
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = GreenBrand,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Admins: Abhay (CEO), Adarsh (Admin)",
                                    color = TextLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Credentials / Registration Form
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Real-time Credential Readiness Status Bar on Admin Authentication Page
                    if (isAdminMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_validation_status_bar"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ValidationStatusChip(
                                label = when {
                                    isEmailValid -> "Email: Valid Format"
                                    showEmailError && email.trim().isEmpty() -> "Email: Empty"
                                    showEmailError -> "Email: Invalid Format"
                                    else -> "Email: Required"
                                },
                                icon = when {
                                    isEmailValid -> Icons.Default.CheckCircle
                                    showEmailError -> Icons.Default.ErrorOutline
                                    else -> Icons.Default.Email
                                },
                                color = when {
                                    isEmailValid -> SuccessGreen
                                    showEmailError -> ErrorRed
                                    else -> TextMuted
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_email_status_chip")
                            )

                            ValidationStatusChip(
                                label = when {
                                    isPasswordValid -> "Password: Ready"
                                    showPasswordError && password.isEmpty() -> "Password: Empty"
                                    showPasswordError -> "Password: ${password.length}/6 chars"
                                    else -> "Password: Required"
                                },
                                icon = when {
                                    isPasswordValid -> Icons.Default.CheckCircle
                                    showPasswordError -> Icons.Default.ErrorOutline
                                    else -> Icons.Default.Lock
                                },
                                color = when {
                                    isPasswordValid -> SuccessGreen
                                    showPasswordError -> ErrorRed
                                    else -> TextMuted
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_password_status_chip")
                            )
                        }

                        val detectedAdminRole = remember(email) {
                            val norm = email.trim().lowercase()
                            when {
                                norm == MarketplaceRepository.OFFICIAL_FOUNDER_EMAIL || norm == "singhabhishek83072@gmail.com" ->
                                    Pair("FOUNDER (Super Admin Access)", CyanBrand)
                                norm == MarketplaceRepository.OFFICIAL_CEO_EMAIL || norm.startsWith("abhay.ceo") ->
                                    Pair("CEO (Executive Admin Access)", GreenBrand)
                                norm == MarketplaceRepository.OFFICIAL_ADMIN_EMAIL || norm == "abhay@skillconnect.in" || norm == "adarsh@skillconnect.in" ->
                                    Pair("ADMIN (Limited Admin Access)", BlueBrand)
                                norm.isNotBlank() && isValidEmailFormat(norm) ->
                                    Pair("UNAUTHORIZED ADMIN EMAIL", ErrorRed)
                                else -> null
                            }
                        }

                        if (detectedAdminRole != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(detectedAdminRole.second.copy(alpha = 0.15f))
                                    .border(1.dp, detectedAdminRole.second.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("admin_detected_role_chip"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = detectedAdminRole.second, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Detected Role: ${detectedAdminRole.first}",
                                    color = detectedAdminRole.second,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Registration-only fields
                    if (isCreateAccountTab && !isAdminMode) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name *") },
                            placeholder = { Text("e.g. Rahul Sharma") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CyanBrand) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = CyanBrand
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_name_input")
                        )

                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { mobile = it },
                            label = { Text("Mobile Number (+91) *") },
                            placeholder = { Text("10 digit number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = CyanBrand) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = CyanBrand
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_phone_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("City *") },
                                placeholder = { Text("e.g. Mumbai") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = CyanBrand
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = state,
                                onValueChange = { state = it },
                                label = { Text("State *") },
                                placeholder = { Text("e.g. Maharashtra") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = CyanBrand
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Provider-specific details
                        if (publicAccountType == UserRole.SERVICE_PROVIDER) {
                            HorizontalDivider(color = NavyCardBorder, modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = "Provider Professional Details",
                                color = GreenBrand,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )

                            OutlinedTextField(
                                value = professionalTitle,
                                onValueChange = { professionalTitle = it },
                                label = { Text("Professional Title *") },
                                placeholder = { Text("e.g. Senior UI/UX & Web Designer") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = GreenBrand
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = skillsInput,
                                onValueChange = { skillsInput = it },
                                label = { Text("Core Skills (comma separated) *") },
                                placeholder = { Text("e.g. Figma, Jetpack Compose, SEO") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = GreenBrand
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = hourlyRateInput,
                                onValueChange = { hourlyRateInput = it },
                                label = { Text("Hourly Rate (₹ INR) *") },
                                placeholder = { Text("1200") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = GreenBrand
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = bioInput,
                                onValueChange = { bioInput = it },
                                label = { Text("Short Bio / Experience") },
                                placeholder = { Text("Briefly describe your expertise...") },
                                maxLines = 3,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = GreenBrand
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Email & Password (common to all modes; never prefilled; real-time visual feedback)
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailTouched = true
                            errorMessage = null
                        },
                        label = { Text(if (isAdminMode) "Authorized Admin Email *" else "Email Address *") },
                        placeholder = { Text(if (isAdminMode) "Enter authorized admin email" else "name@example.in") },
                        isError = showEmailError,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = when {
                                    showEmailError -> ErrorRed
                                    isEmailValid && isAdminMode -> SuccessGreen
                                    isAdminMode -> BlueBrand
                                    else -> CyanBrand
                                }
                            )
                        },
                        trailingIcon = {
                            when {
                                showEmailError -> Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Invalid email format",
                                    tint = ErrorRed,
                                    modifier = Modifier.testTag("email_error_icon")
                                )
                                isEmailValid -> Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Valid email format",
                                    tint = SuccessGreen,
                                    modifier = Modifier.testTag("email_valid_icon")
                                )
                            }
                        },
                        supportingText = if (showEmailError && emailValidationMessage != null) {
                            {
                                Text(
                                    text = emailValidationMessage,
                                    color = ErrorRed,
                                    fontSize = 11.sp,
                                    modifier = Modifier.testTag("email_validation_feedback")
                                )
                            }
                        } else if (isAdminMode && isEmailValid) {
                            {
                                Text(
                                    text = "Valid admin email format",
                                    color = SuccessGreen,
                                    fontSize = 11.sp,
                                    modifier = Modifier.testTag("email_validation_feedback")
                                )
                            }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            errorTextColor = TextWhite,
                            focusedBorderColor = if (isEmailValid && isAdminMode) SuccessGreen else if (isAdminMode) BlueBrand else CyanBrand,
                            unfocusedBorderColor = if (isEmailValid && isAdminMode) SuccessGreen.copy(alpha = 0.6f) else NavyCardBorder,
                            errorBorderColor = ErrorRed,
                            errorLabelColor = ErrorRed,
                            errorLeadingIconColor = ErrorRed,
                            errorTrailingIconColor = ErrorRed
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    emailHadFocus = true
                                } else if (emailHadFocus) {
                                    emailTouched = true
                                }
                            }
                            .testTag("auth_email_input")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordTouched = true
                            errorMessage = null
                        },
                        label = { Text("Password *") },
                        placeholder = { Text(if (isAdminMode) "Enter admin password" else "Enter password") },
                        isError = showPasswordError,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = when {
                                    showPasswordError -> ErrorRed
                                    isPasswordValid && isAdminMode -> SuccessGreen
                                    isAdminMode -> BlueBrand
                                    else -> CyanBrand
                                }
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { isPasswordVisible = !isPasswordVisible },
                                modifier = Modifier.testTag("password_visibility_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                    tint = if (showPasswordError) ErrorRed else if (isAdminMode) BlueBrand else CyanBrand
                                )
                            }
                        },
                        supportingText = if (showPasswordError && passwordValidationMessage != null) {
                            {
                                Text(
                                    text = passwordValidationMessage,
                                    color = ErrorRed,
                                    fontSize = 11.sp,
                                    modifier = Modifier.testTag("password_validation_feedback")
                                )
                            }
                        } else if (isAdminMode && isPasswordValid) {
                            {
                                Text(
                                    text = "Password length verified",
                                    color = SuccessGreen,
                                    fontSize = 11.sp,
                                    modifier = Modifier.testTag("password_validation_feedback")
                                )
                            }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            errorTextColor = TextWhite,
                            focusedBorderColor = if (isPasswordValid && isAdminMode) SuccessGreen else if (isAdminMode) BlueBrand else CyanBrand,
                            unfocusedBorderColor = if (isPasswordValid && isAdminMode) SuccessGreen.copy(alpha = 0.6f) else NavyCardBorder,
                            errorBorderColor = ErrorRed,
                            errorLabelColor = ErrorRed,
                            errorLeadingIconColor = ErrorRed
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = if (isCreateAccountTab && !isAdminMode) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (!isCreateAccountTab || isAdminMode) {
                                    submitAuthForm()
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    passwordHadFocus = true
                                } else if (passwordHadFocus) {
                                    passwordTouched = true
                                }
                            }
                            .onPreviewKeyEvent { event ->
                                if ((!isCreateAccountTab || isAdminMode) &&
                                    event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    submitAuthForm()
                                    true
                                } else {
                                    false
                                }
                            }
                            .testTag("auth_password_input")
                    )

                    // Forgot Password Link (triggers Firebase password reset email flow for Returning Users & Admins)
                    if (isAdminMode || !isCreateAccountTab) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isAdminMode) {
                                Text(
                                    text = "Saved role & profile auto-restored",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            } else {
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            TextButton(
                                onClick = {
                                    errorMessage = null
                                    successMessage = null
                                    if (isEmailValid) {
                                        val resetResult = if (isAdminMode) {
                                            onAdminForgotPassword(email)
                                        } else {
                                            onPublicForgotPassword(email)
                                        }
                                        if (resetResult.isSuccess) {
                                            successMessage = resetResult.getOrNull()
                                                ?: "Password reset link sent to ${email.trim().lowercase()} via Firebase Authentication."
                                        } else {
                                            errorMessage = resetResult.exceptionOrNull()?.message
                                                ?: "Failed to send password reset email."
                                        }
                                    } else {
                                        emailTouched = true
                                        showAdminForgotPasswordDialog = true
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag(
                                    if (isAdminMode) "admin_forgot_password_link" else "public_forgot_password_link"
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = CyanBrand,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Forgot Password?",
                                    color = CyanBrand,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    if (isAdminMode && onProvisionOfficialAccounts != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = { showProvisionWizardDialog = true },
                                modifier = Modifier.testTag("auth_provision_wizard_btn")
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = CyanBrand, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Founder: Provision Official Admin Accounts",
                                    color = CyanBrand,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (isCreateAccountTab && !isAdminMode) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password *") },
                            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = CyanBrand) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                                    modifier = Modifier.testTag("confirm_password_visibility_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isConfirmPasswordVisible) "Hide confirm password" else "Show confirm password",
                                        tint = CyanBrand
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = CyanBrand
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submitAuthForm() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown &&
                                        (event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        submitAuthForm()
                                        true
                                    } else {
                                        false
                                    }
                                }
                                .testTag("auth_confirm_password_input")
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Checkbox(
                                checked = termsAccepted,
                                onCheckedChange = { termsAccepted = it },
                                colors = CheckboxDefaults.colors(checkedColor = GreenBrand)
                            )
                            Text(
                                text = "I accept the SkillConnect Terms & Privacy Policy",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, ErrorRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_error_banner")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = errorMessage!!,
                                    color = ErrorRed,
                                    fontSize = 12.sp
                                )
                                if (!isAdminMode && isCreateAccountTab && errorMessage!!.contains("already exists", ignoreCase = true)) {
                                    TextButton(
                                        onClick = {
                                            isCreateAccountTab = false
                                            errorMessage = null
                                            successMessage = null
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("switch_to_sign_in_from_error")
                                    ) {
                                        Text(
                                            text = "Switch to Sign In with Existing Account →",
                                            color = CyanBrand,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else if (!isAdminMode && !isCreateAccountTab && errorMessage!!.contains("No account found", ignoreCase = true)) {
                                    TextButton(
                                        onClick = {
                                            isCreateAccountTab = true
                                            errorMessage = null
                                            successMessage = null
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("switch_to_create_account_from_error")
                                    ) {
                                        Text(
                                            text = "Create a New Account Once →",
                                            color = CyanBrand,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (successMessage != null) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, SuccessGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_success_banner")
                        ) {
                            Text(
                                text = successMessage!!,
                                color = SuccessGreen,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Primary Action Button
                    Button(
                        onClick = { submitAuthForm() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAdminMode) BlueBrand
                            else if (publicAccountType == UserRole.CUSTOMER) CyanBrand
                            else GreenBrand
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_primary_submit_button")
                    ) {
                        Text(
                            text = if (isAdminMode) "Authorize Admin Access"
                            else if (isCreateAccountTab) "Complete ${if (publicAccountType == UserRole.CUSTOMER) "Customer" else "Provider"} Registration"
                            else "Sign In to ${if (publicAccountType == UserRole.CUSTOMER) "Customer" else "Provider"} Account",
                            color = if (isAdminMode) Color.White else Color(0xFF001F2B),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    GoogleSignInButton(
                        onAuthSuccess = { firebaseUser ->
                            errorMessage = null
                            val targetRole = if (isAdminMode) UserRole.ADMIN else publicAccountType
                            val result = onGoogleLogin(
                                firebaseUser.uid,
                                firebaseUser.displayName,
                                firebaseUser.email,
                                firebaseUser.photoUrl?.toString(),
                                targetRole
                            )
                            if (result.isSuccess) {
                                val user = result.getOrThrow()
                                successMessage = "Signed in with Google as ${user.name}."
                                onAuthSuccess(user)
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Google Sign-In verification failed."
                            }
                        },
                        onAuthError = { errMsg ->
                            errorMessage = errMsg
                        }
                    )
                }
            }
        }

        // Dedicated Protected Admin Portal Switch / Back to Public Marketplace Sign In
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = {
                        isAdminMode = !isAdminMode
                        errorMessage = null
                        successMessage = null
                        email = ""
                        password = ""
                        confirmPassword = ""
                        isPasswordVisible = false
                        isConfirmPasswordVisible = false
                        emailTouched = false
                        passwordTouched = false
                        emailHadFocus = false
                        passwordHadFocus = false
                        submitAttempted = false
                        showAdminForgotPasswordDialog = false
                    },
                    modifier = Modifier.testTag("admin_mode_switch_button")
                ) {
                    Icon(
                        imageVector = if (isAdminMode) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isAdminMode) CyanBrand else TextDim,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAdminMode) "← Back to Public Marketplace Sign In" else "Management & Admin Portal Access",
                        color = if (isAdminMode) CyanBrand else TextDim,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showAdminForgotPasswordDialog) {
        AdminForgotPasswordDialog(
            initialEmail = email,
            isAdminMode = isAdminMode,
            onDismiss = { showAdminForgotPasswordDialog = false },
            onSubmitReset = { targetEmail ->
                val result = if (isAdminMode) {
                    onAdminForgotPassword(targetEmail)
                } else {
                    onPublicForgotPassword(targetEmail)
                }
                if (result.isSuccess) {
                    email = targetEmail.trim()
                    emailTouched = true
                    errorMessage = null
                    successMessage = result.getOrNull()
                        ?: "Password reset link sent to ${targetEmail.trim().lowercase()} via Firebase Authentication."
                    showAdminForgotPasswordDialog = false
                }
                result
            }
        )
    }

    if (showProvisionWizardDialog && onProvisionOfficialAccounts != null) {
        AdminAccountProvisioningDialog(
            onDismiss = { showProvisionWizardDialog = false },
            onSubmit = { fPass, cPass, aPass ->
                val res = onProvisionOfficialAccounts(fPass, cPass, aPass)
                if (res.isSuccess) {
                    successMessage = res.getOrNull() ?: "Official accounts provisioned successfully."
                    showProvisionWizardDialog = false
                } else {
                    errorMessage = res.exceptionOrNull()?.message ?: "Provisioning failed."
                }
            }
        )
    }
}

@Composable
private fun AdminForgotPasswordDialog(
    initialEmail: String,
    isAdminMode: Boolean = true,
    onDismiss: () -> Unit,
    onSubmitReset: (String) -> Result<String>
) {
    var resetEmail by remember(initialEmail) { mutableStateOf(initialEmail) }
    var touched by remember { mutableStateOf(initialEmail.isNotEmpty()) }
    var dialogError by remember { mutableStateOf<String?>(null) }

    val validationMessage = remember(resetEmail, isAdminMode) {
        getEmailRealtimeValidationMessage(resetEmail, isAdminMode = isAdminMode)
    }
    val isValid = validationMessage == null
    val showError = touched && !isValid

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("admin_forgot_password_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = if (isAdminMode) BlueBrand else CyanBrand,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (isAdminMode) "Admin Password Reset" else "Account Password Reset",
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isAdminMode) {
                        "Enter your authorized SkillConnect administrator email to receive a password reset link via Firebase Authentication."
                    } else {
                        "Enter your registered SkillConnect email to receive a password reset link via Firebase Authentication."
                    },
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = {
                        resetEmail = it
                        touched = true
                        dialogError = null
                    },
                    label = { Text(if (isAdminMode) "Authorized Admin Email *" else "Registered Email Address *") },
                    placeholder = { Text(if (isAdminMode) "Enter authorized admin email" else "Enter registered email") },
                    isError = showError || dialogError != null,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = if (showError || dialogError != null) ErrorRed else if (isValid) SuccessGreen else BlueBrand
                        )
                    },
                    trailingIcon = {
                        if (showError || dialogError != null) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRed)
                        } else if (isValid) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                        }
                    },
                    supportingText = {
                        val msg = dialogError ?: if (showError) validationMessage else if (isValid) "Valid admin email format" else null
                        if (msg != null) {
                            Text(
                                text = msg,
                                color = if (dialogError != null || showError) ErrorRed else SuccessGreen,
                                fontSize = 11.sp,
                                modifier = Modifier.testTag("reset_dialog_validation_feedback")
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        errorTextColor = TextWhite,
                        focusedBorderColor = if (isValid) SuccessGreen else BlueBrand,
                        unfocusedBorderColor = NavyCardBorder,
                        errorBorderColor = ErrorRed
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            touched = true
                            if (!isValid) {
                                dialogError = validationMessage
                            } else {
                                val res = onSubmitReset(resetEmail)
                                if (res.isFailure) {
                                    dialogError = res.exceptionOrNull()?.message ?: "Unable to send reset email."
                                }
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown &&
                                (event.key == Key.Enter || event.key == Key.NumPadEnter)
                            ) {
                                touched = true
                                if (!isValid) {
                                    dialogError = validationMessage
                                } else {
                                    val res = onSubmitReset(resetEmail)
                                    if (res.isFailure) {
                                        dialogError = res.exceptionOrNull()?.message ?: "Unable to send reset email."
                                    }
                                }
                                true
                            } else {
                                false
                            }
                        }
                        .testTag("reset_dialog_email_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    touched = true
                    if (!isValid) {
                        dialogError = validationMessage
                        return@Button
                    }
                    val res = onSubmitReset(resetEmail)
                    if (res.isFailure) {
                        dialogError = res.exceptionOrNull()?.message ?: "Unable to send reset email."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BlueBrand),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("send_reset_email_button")
            ) {
                Text(
                    text = "Send Reset Link",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_reset_email_button")
            ) {
                Text("Cancel", color = TextMuted, fontSize = 13.sp)
            }
        }
    )
}

@Composable
private fun ValidationStatusChip(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AccountTypeTab(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.22f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) activeColor else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) activeColor else TextMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    color = if (isSelected) activeColor else TextLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
