package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.MarketplaceCatalog
import com.example.ui.theme.SkillBlue
import com.example.ui.theme.SkillBrandGradient
import com.example.ui.theme.SkillCyan
import com.example.ui.theme.SkillNavy
import com.example.ui.theme.SkillPurple
import com.example.ui.theme.SkillSuccessGreen
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Official SkillConnect brand logo using the uploaded interconnected blue-and-green twin-figure
 * emblem and "SkillConnect" wordmark.
 */
@Composable
fun SkillConnectBrandLogo(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onNavyHeader: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.skillconnect_logo_emblem),
            contentDescription = "SkillConnect Logo",
            modifier = Modifier
                .size(if (compact) 36.dp else 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Skill",
                    style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (onNavyHeader) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Connect",
                    style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (onNavyHeader) Color(0xFF6EEB38) else SkillSuccessGreen
                )
            }
            if (!compact) {
                Text(
                    text = "Your Skills. Your Local Network.",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (onNavyHeader) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Full horizontal SkillConnect official logo banner displaying the uploaded graphic.
 */
@Composable
fun SkillConnectOfficialLogoBanner(
    modifier: Modifier = Modifier,
    height: Dp = 96.dp
) {
    Surface(
        color = Color(0xFF04102E),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.skillconnect_logo_full),
                contentDescription = "SkillConnect Official Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun ConnectedNodesPinSymbol(size: Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val topLeft = Offset(w * 0.25f, h * 0.30f)
        val topRight = Offset(w * 0.75f, h * 0.30f)
        val bottomPin = Offset(w * 0.50f, h * 0.80f)
        val centerHub = Offset(w * 0.50f, h * 0.45f)

        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = topLeft,
            end = centerHub,
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = topRight,
            end = centerHub,
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = topLeft,
            end = bottomPin,
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = topRight,
            end = bottomPin,
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )

        drawCircle(color = SkillCyan, radius = w * 0.13f, center = topLeft)
        drawCircle(color = Color.White, radius = w * 0.13f, center = topRight)
        drawCircle(color = Color.White, radius = w * 0.14f, center = centerHub)
        drawCircle(color = SkillCyan, radius = w * 0.12f, center = bottomPin)
    }
}

// 1. Silent Auto-Sign-In on App Startup (Single GetGoogleIdOption per request)
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

// 2. Interactive Google Sign-In Flow (Single GetSignInWithGoogleOption per request)
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type returned from Google Sign-In.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
        }
    }
}

// 3. Sign-Out Flow
fun performSignOut(
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillConnectAuthScreen(
    isDarkMode: Boolean,
    isHindi: Boolean,
    onToggleDarkMode: () -> Unit,
    onToggleLanguage: () -> Unit,
    onRoleSelectedBeforeAuth: (String) -> Unit = {},
    onAdminAuthSuccess: (adminRole: String, username: String, password: String) -> Unit = { _, _, _ -> },
    onAuthSuccess: (role: String, city: String, locality: String, preferredLanguage: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var selectedRole by remember { mutableStateOf("customer") }
    var selectedCity by remember { mutableStateOf("Bengaluru") }
    var selectedLocality by remember { mutableStateOf("Indiranagar") }
    var acceptedTerms by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cityExpanded by remember { mutableStateOf(false) }

    // Executive Admin Login Card State (Section B)
    // Only shown after a specific card ("cofounder", "ceo", "founder") is selected
    var selectedAdminRoleCard by remember { mutableStateOf<String?>(null) }
    var adminUsernameInput by remember { mutableStateOf("") }
    var adminPasswordInput by remember { mutableStateOf("") }
    var failedAdminAttempts by remember { mutableStateOf(0) }
    var adminLockoutUntilMs by remember { mutableStateOf(0L) }

    val availableCities = remember { MarketplaceCatalog.indianCities.keys.filter { it != "All India" } }

    fun triggerGoogleSignIn(targetRole: String) {
        if (!acceptedTerms) {
            errorMessage = "Please accept the Terms of Service and Privacy Policy to continue."
            return
        }
        selectedRole = targetRole
        onRoleSelectedBeforeAuth(targetRole)
        errorMessage = null
        isLoading = true
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isLoading = false
                onAuthSuccess(
                    targetRole,
                    selectedCity,
                    selectedLocality,
                    if (isHindi) "hi" else "en"
                )
            },
            onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = {
                isLoading = false
            }
        )
    }

    fun triggerExecutiveAdminSignIn(adminRole: String) {
        val now = System.currentTimeMillis()
        if (adminLockoutUntilMs > now) {
            val remainingSec = ((adminLockoutUntilMs - now) / 1000L).coerceAtLeast(1L)
            errorMessage = "Too many failed login attempts. Try again in ${remainingSec}s."
            return
        }
        if (!acceptedTerms) {
            errorMessage = "Please accept the Terms of Service and Privacy Policy to continue."
            return
        }
        val cleanUser = adminUsernameInput.trim()
        val cleanPass = adminPasswordInput.trim()

        val validUsername = when (adminRole) {
            "cofounder" -> cleanUser.equals("Abhay", ignoreCase = true)
            "ceo" -> cleanUser.equals("Abhay", ignoreCase = true) || cleanUser.equals("Adarsh", ignoreCase = true)
            "founder" -> cleanUser.equals("Abhisheksingh", ignoreCase = true)
            else -> false
        }

        if (!validUsername) {
            failedAdminAttempts += 1
            if (failedAdminAttempts >= 5) {
                adminLockoutUntilMs = System.currentTimeMillis() + 60_000L
            }
            errorMessage = when (adminRole) {
                "cofounder" -> "Invalid username for Co-Founder Login."
                "ceo" -> "Invalid username for CEO Login."
                "founder" -> "Invalid username for Founder Login."
                else -> "Invalid executive username."
            }
            return
        }

        if (cleanPass.length < 6) {
            failedAdminAttempts += 1
            if (failedAdminAttempts >= 5) {
                adminLockoutUntilMs = System.currentTimeMillis() + 60_000L
            }
            errorMessage = "Enter a valid administrative password (minimum 6 characters)."
            return
        }

        errorMessage = null
        isLoading = true
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isLoading = false
                failedAdminAttempts = 0
                onAdminAuthSuccess(adminRole, cleanUser, cleanPass)
                onAuthSuccess(
                    "customer",
                    selectedCity,
                    selectedLocality,
                    if (isHindi) "hi" else "en"
                )
            },
            onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = {
                isLoading = false
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navy Brand Bar
            Surface(
                color = SkillNavy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkillConnectBrandLogo(onNavyHeader = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleLanguage,
                            modifier = Modifier.testTag("auth_language_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Switch Language",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = onToggleDarkMode,
                            modifier = Modifier.testTag("auth_theme_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Dark Mode",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Hero visual card with Official Uploaded Logo Banner
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SkillNavy),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column {
                        SkillConnectOfficialLogoBanner(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            height = 110.dp
                        )
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_connect_1793072729236),
                            contentDescription = "SkillConnect India Local Services Marketplace",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(155.dp),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.padding(18.dp)) {
                            Surface(
                                color = SkillCyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (isHindi) "भारत का लोकल सर्विसेज़ मार्केटप्लेस" else "INDIA'S LOCAL SERVICES MARKETPLACE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkillCyan,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isHindi) "सही प्रोफेशनल खोजें। अपना काम पूरा करें।" else "Discover professionals, connect with customers, grow your business.",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isHindi) {
                                    "वेरिफाइड सर्विस प्रोवाइडर्स, फ्रीलांसर्स और लोकल प्रोफेशनल्स से सीधे जुड़ें।"
                                } else {
                                    "Choose your account role below. Customers, Service Providers, and Executive Administrators have separate onboarding, role locks, and dedicated dashboards."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Location & Language Configuration Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isHindi) "1. अपना शहर और भाषा चुनें" else "1. Select Your Indian City & Language",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        AllIndiaLocationSelectorField(
                            city = selectedCity,
                            locality = selectedLocality,
                            label = if (isHindi) "शहर, राज्य और पिन कोड (All-India City & PIN)" else "Primary City, State & PIN in India",
                            allowAllIndia = false,
                            isHindi = isHindi,
                            testTag = "auth_city_selector",
                            onLocationChanged = { _, newCity, newLocality, _ ->
                                selectedCity = newCity
                                if (newLocality.isNotBlank()) {
                                    selectedLocality = newLocality
                                }
                            }
                        )

                        OutlinedTextField(
                            value = selectedLocality,
                            onValueChange = { selectedLocality = it },
                            label = { Text(if (isHindi) "लोकैलिटी / एरिया (Locality)" else "Locality / Area (Optional)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_locality_input")
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isHindi) "भाषा:" else "Language:",
                                style = MaterialTheme.typography.labelLarge
                            )
                            FilterChip(
                                selected = !isHindi,
                                onClick = { if (isHindi) onToggleLanguage() },
                                label = { Text("English") }
                            )
                            FilterChip(
                                selected = isHindi,
                                onClick = { if (!isHindi) onToggleLanguage() },
                                label = { Text("हिन्दी (Hindi)") }
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { acceptedTerms = !acceptedTerms }
                        ) {
                            Checkbox(
                                checked = acceptedTerms,
                                onCheckedChange = { acceptedTerms = it },
                                modifier = Modifier.testTag("auth_terms_checkbox")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) {
                                    "मैं SkillConnect की सेवा की शर्तों और गोपनीयता नीति से सहमत हूँ।"
                                } else {
                                    "I agree to SkillConnect's Terms of Service, Community Standards, and Privacy Policy."
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Dedicated Role-Selection Section: CARD A (CUSTOMER) & CARD B (SERVICE PROVIDER)
                Text(
                    text = if (isHindi) "2. अपनी भूमिका चुनें (Choose Your Role)" else "2. Choose Your Account Role",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // CARD A — CUSTOMER
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (selectedRole == "customer" && selectedAdminRoleCard == null) 2.dp else 1.dp,
                            color = if (selectedRole == "customer" && selectedAdminRoleCard == null) SkillBlue else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            selectedRole = "customer"
                            selectedAdminRoleCard = null
                            onRoleSelectedBeforeAuth("customer")
                        }
                        .testTag("role_customer_chip")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(SkillBlue.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = SkillBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "CUSTOMER ACCOUNT • SC-CUS ID",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SkillBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isHindi) "सेवाएं खोजें (Find Services)" else "Find Services",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (selectedRole == "customer" && selectedAdminRoleCard == null) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = SkillBlue
                                )
                            }
                        }

                        Text(
                            text = if (isHindi) {
                                "प्रोफेशनल्स खोजें, सेवाओं की तुलना करें और सही व्यक्ति को काम पर रखें।"
                            } else {
                                "Discover professionals, compare services and hire the right person."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { triggerGoogleSignIn("customer") },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("continue_as_customer_button")
                        ) {
                            if (isLoading && selectedRole == "customer" && selectedAdminRoleCard == null) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Signing in as Customer...")
                            } else {
                                Text(
                                    text = if (isHindi) "ग्राहक के रूप में आगे बढ़ें" else "Continue as Customer",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // CARD B — SERVICE PROVIDER
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (selectedRole == "provider" && selectedAdminRoleCard == null) 2.dp else 1.dp,
                            color = if (selectedRole == "provider" && selectedAdminRoleCard == null) SkillPurple else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            selectedRole = "provider"
                            selectedAdminRoleCard = null
                            onRoleSelectedBeforeAuth("provider")
                        }
                        .testTag("role_provider_chip")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(SkillPurple.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Work,
                                        contentDescription = null,
                                        tint = SkillPurple,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "SERVICE PROVIDER • SC-PRO ID + 8-STEP WIZARD",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SkillPurple,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isHindi) "अपनी सेवाएं दें (Offer Your Services)" else "Offer Your Services",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (selectedRole == "provider" && selectedAdminRoleCard == null) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = SkillPurple
                                )
                            }
                        }

                        Text(
                            text = if (isHindi) {
                                "अपनी प्रोफेशनल प्रोफ़ाइल बनाएं, अपने कौशल दिखाएं और ग्राहकों से काम के अनुरोध प्राप्त करें।"
                            } else {
                                "Create your professional profile, showcase your skills and receive customer enquiries."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { triggerGoogleSignIn("provider") },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = SkillPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("register_as_provider_button")
                        ) {
                            if (isLoading && selectedRole == "provider" && selectedAdminRoleCard == null) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Starting Provider Onboarding...")
                            } else {
                                Text(
                                    text = if (isHindi) "सर्विस प्रोवाइडर के रूप में रजिस्टर करें" else "Register as Service Provider",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // =========================================================================
                // SECTION B: THREE SEPARATE ADMINISTRATIVE LOGIN CARDS
                // 1. CO-FOUNDER
                // 2. CEO
                // 3. FOUNDER
                // Username and password inputs appear ONLY after a specific card is selected.
                // =========================================================================
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isHindi) "3. एडमिनिस्ट्रेटिव लॉगिन (Executive Admin Portal)" else "3. Administrative Login Options",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select an administrative role below to enter your username and password.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                data class AdminCardOption(
                    val roleKey: String,
                    val roleLabel: String,
                    val responsibilities: String,
                    val badgeColor: Color
                )

                val adminLoginCards = remember {
                    listOf(
                        AdminCardOption(
                            roleKey = "cofounder",
                            roleLabel = "CO-FOUNDER",
                            responsibilities = "Profile verification, verification requests, provider/customer document review, routine support, and assigned operational tasks.",
                            badgeColor = SkillCyan
                        ),
                        AdminCardOption(
                            roleKey = "ceo",
                            roleLabel = "CEO",
                            responsibilities = "Business analytics, job activity, bookings, provider/customer statistics, escalated verifications, complaints, payment disputes (refunds up to ₹10,000), and operational management.",
                            badgeColor = SkillBlue
                        ),
                        AdminCardOption(
                            roleKey = "founder",
                            roleLabel = "FOUNDER",
                            responsibilities = "Full platform oversight, admin/role management, high-risk approvals, commission/financial reports, platform settings, audit logs, and emergency controls.",
                            badgeColor = SkillPurple
                        )
                    )
                }

                adminLoginCards.forEach { card ->
                    val isCardSelected = selectedAdminRoleCard == card.roleKey
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isCardSelected) 2.dp else 1.dp,
                                color = if (isCardSelected) card.badgeColor else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                if (!isCardSelected) {
                                    selectedAdminRoleCard = card.roleKey
                                    adminUsernameInput = ""
                                    adminPasswordInput = ""
                                    errorMessage = null
                                }
                            }
                            .testTag("admin_login_card_${card.roleKey}")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(card.badgeColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = card.badgeColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = card.roleLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (isCardSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected Admin Card",
                                        tint = card.badgeColor
                                    )
                                }
                            }

                            Text(
                                text = card.responsibilities,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Show Username and Password fields ONLY after this specific login card is selected
                            if (isCardSelected) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Surface(
                                    color = card.badgeColor.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = card.roleLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = card.badgeColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                OutlinedTextField(
                                    value = adminUsernameInput,
                                    onValueChange = { adminUsernameInput = it },
                                    label = { Text("${card.roleLabel} Username") },
                                    placeholder = { Text("Enter username") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_username_input_${card.roleKey}")
                                )

                                OutlinedTextField(
                                    value = adminPasswordInput,
                                    onValueChange = { adminPasswordInput = it },
                                    label = { Text("${card.roleLabel} Password") },
                                    placeholder = { Text("Enter password (min 6 chars)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_password_input_${card.roleKey}")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { triggerExecutiveAdminSignIn(card.roleKey) },
                                        enabled = !isLoading,
                                        colors = ButtonDefaults.buttonColors(containerColor = card.badgeColor),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("admin_submit_button_${card.roleKey}")
                                    ) {
                                        Text(
                                            text = "Sign In as ${card.roleLabel}",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            selectedAdminRoleCard = null
                                            adminUsernameInput = ""
                                            adminPasswordInput = ""
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("Close")
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        selectedAdminRoleCard = card.roleKey
                                        adminUsernameInput = ""
                                        adminPasswordInput = ""
                                        errorMessage = null
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("select_admin_card_button_${card.roleKey}")
                                ) {
                                    Text(
                                        text = card.roleLabel,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Existing Account Sign-In Card
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isHindi) "पहले से अकाउंट है?" else "Already Have a SkillConnect Account?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sign in with Google to resume your existing Customer, Service Provider, or Executive Admin workspace.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { triggerGoogleSignIn(selectedRole) },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = SkillBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "Google के साथ साइन इन करें" else "Sign in with Google",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = SkillSuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Protected by Google Identity Services, Salted SHA-256 Verifiers & Zero-Trust Firestore Security Rules.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
