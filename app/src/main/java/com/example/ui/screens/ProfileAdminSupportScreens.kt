package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.UserPreferences
import com.example.data.model.AdminAccount
import com.example.data.model.AdminAuditLog
import com.example.data.model.BookingRequest
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.JobOpportunityItem
import com.example.data.model.MarketplaceCatalog
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
import com.example.ui.AppDestination
import com.example.ui.components.AllIndiaLocationSelectorField
import com.example.ui.components.SkillConnectBrandLogo
import com.example.ui.components.SkillConnectOfficialLogoBanner
import com.example.ui.theme.SkillBlue
import com.example.ui.theme.SkillCyan
import com.example.ui.theme.SkillErrorRed
import com.example.ui.theme.SkillNavy
import com.example.ui.theme.SkillPurple
import com.example.ui.theme.SkillSuccessGreen
import com.example.ui.theme.SkillWarningAmber

// Permanent Official Merchant UPI ID for SkillConnect Subscriptions
const val MERCHANT_UPI_ID = "6393481228@ybl"
const val MERCHANT_UPI_NAME = "SkillConnect India"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileAndSettingsScreen(
    profile: UserProfile?,
    userEmail: String,
    fallbackName: String,
    preferences: UserPreferences,
    isAdminUser: Boolean,
    activeExecutiveRole: String = "",
    myAdminAccount: AdminAccount? = null,
    myVerification: VerificationSubmission? = null,
    myBookings: List<BookingRequest> = emptyList(),
    myConversations: List<Conversation> = emptyList(),
    myPayments: List<PaymentRecord> = emptyList(),
    myDisputes: List<PaymentDispute> = emptyList(),
    mySupportTickets: List<SupportTicket> = emptyList(),
    onSaveProfile: (UserProfile) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onChangeLanguage: (String) -> Unit,
    onUpdateNotificationPrivacy: (Boolean, Boolean, Boolean) -> Unit,
    onRequestRoleChangeTicket: (targetRole: String, reason: String) -> Unit = { _, _ -> },
    onRequestDeletionTicket: () -> Unit,
    onRequestLocationPermission: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onToggleJobApplication: (JobOpportunityItem) -> Unit = {},
    onToggleCourseEnrollment: (SkillCourseItem) -> Unit = {},
    onFileDispute: (booking: BookingRequest, paymentId: String, reason: String, statement: String, evidenceNote: String) -> Unit = { _, _, _, _, _ -> },
    onLoginExecutiveAdmin: (role: String, username: String, password: String) -> Unit = { _, _, _ -> },
    onLogoutExecutiveAdmin: () -> Unit = {},
    onNavigate: (AppDestination) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val baseProfile = profile ?: UserProfile(
        displayName = fallbackName,
        role = "customer",
        city = preferences.selectedCity.takeIf { it != "All India" } ?: "Bengaluru",
        locality = preferences.selectedLocality,
        preferredLanguage = preferences.language
    )

    var displayName by remember(baseProfile) { mutableStateOf(baseProfile.displayName) }
    var headline by remember(baseProfile) { mutableStateOf(baseProfile.headline) }
    var bio by remember(baseProfile) { mutableStateOf(baseProfile.bio) }
    val lockedRole = baseProfile.role
    var city by remember(baseProfile) { mutableStateOf(baseProfile.city) }
    var locality by remember(baseProfile) { mutableStateOf(baseProfile.locality) }
    var skillsText by remember(baseProfile) { mutableStateOf(baseProfile.skills.joinToString(", ")) }
    var startingPriceText by remember(baseProfile) { mutableStateOf(baseProfile.startingPrice.toString()) }
    var availability by remember(baseProfile) { mutableStateOf(baseProfile.availability) }
    var publicPhone by remember(baseProfile) { mutableStateOf(baseProfile.publicPhone) }

    var cityExpanded by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showRoleChangeDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var disputeBookingTarget by remember { mutableStateOf<BookingRequest?>(null) }

    // Unified Admin Gmail Portal state inside Profile
    var showUnifiedAdminInput by remember { mutableStateOf(false) }
    var adminPasswordInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Role-Aware Header with Unique Platform ID (SC-CUS-XXXXXXXX or SC-PRO-XXXXXXXX)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SkillNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SkillConnectBrandLogo(compact = true, onNavyHeader = true)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = SkillCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "PLATFORM ID: ${baseProfile.effectivePlatformId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkillCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = if (lockedRole == "provider") SkillPurple.copy(alpha = 0.3f) else SkillBlue.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "ROLE: ${lockedRole.uppercase()} • ${baseProfile.accountStatus.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = displayName.ifBlank { "SkillConnect Member" },
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        if (lockedRole == "provider" && baseProfile.isStrictVerifiedBadgeActive) {
                            Surface(
                                color = SkillBlue.copy(alpha = 0.28f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .border(1.dp, SkillCyan, RoundedCornerShape(50))
                                    .testTag("profile_header_blue_tick_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Blue Tick Verified by SkillConnect",
                                        tint = SkillCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Verified by SkillConnect",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "Email: $userEmail • City: $city ($locality)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.82f)
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (lockedRole == "customer" || isAdminUser) {
                            Button(
                                onClick = { onNavigate(AppDestination.CUSTOMER_DASHBOARD) },
                                colors = ButtonDefaults.buttonColors(containerColor = SkillBlue)
                            ) {
                                Text("Customer Dashboard")
                            }
                        }
                        if (lockedRole == "provider" || isAdminUser) {
                            Button(
                                onClick = { onNavigate(AppDestination.PROVIDER_DASHBOARD) },
                                colors = ButtonDefaults.buttonColors(containerColor = SkillPurple)
                            ) {
                                Text("Provider Workspace")
                            }
                            OutlinedButton(onClick = { onNavigate(AppDestination.PROVIDER_ONBOARDING_WIZARD) }) {
                                Text("8-Step Wizard", color = Color.White)
                            }
                            OutlinedButton(onClick = { onNavigate(AppDestination.PROVIDER_VERIFICATION) }) {
                                Text("Verification", color = Color.White)
                            }
                        }
                        OutlinedButton(
                            onClick = { onNavigate(AppDestination.SUBSCRIPTIONS) },
                            modifier = Modifier.testTag("profile_open_subscriptions_btn")
                        ) {
                            Text("Subscription Plans", color = Color.White)
                        }
                        if (isAdminUser) {
                            Button(
                                onClick = { onNavigate(AppDestination.ADMIN_DASHBOARD) },
                                colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen),
                                modifier = Modifier.testTag("open_admin_dashboard_btn")
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Executive Admin (${activeExecutiveRole.ifBlank { "Portal" }.uppercase()})")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Direct Logout Option right below every Customer and Service Provider Profile Card
            Button(
                onClick = { showLogoutConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = SkillErrorRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("profile_top_logout_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Logout from ${if (lockedRole == "provider") "Service Provider" else "Customer"} Profile",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // =========================================================================
        // SECTION 1 OF 9: 1. Profile information
        // =========================================================================
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .testTag("profile_section_1_info")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "1. Profile information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkillBlue
                    )
                    Text(
                        text = "Account Status: ${baseProfile.accountStatus.uppercase()} • Role: ${lockedRole.uppercase()} • Completed Jobs: ${baseProfile.completedJobsCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Full Name / Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (lockedRole == "provider") {
                        OutlinedTextField(
                            value = headline,
                            onValueChange = { headline = it },
                            label = { Text("Professional Headline & Specialization") },
                            placeholder = { Text("e.g. Certified Electrician & Home Technician") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Bio, Experience & Portfolio Summary") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    AllIndiaLocationSelectorField(
                        city = city,
                        locality = locality,
                        stateName = baseProfile.serviceState.ifBlank { preferences.selectedState },
                        pinCode = preferences.selectedPinCode,
                        label = "All-India Location (State, City, Locality & PIN)",
                        allowAllIndia = false,
                        testTag = "profile_all_india_location_selector",
                        onLocationChanged = { _, newCity, newLocality, _ ->
                            city = newCity
                            if (newLocality.isNotBlank()) {
                                locality = newLocality
                            }
                        }
                    )

                    OutlinedTextField(
                        value = locality,
                        onValueChange = { locality = it },
                        label = { Text("Locality / Area") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (lockedRole == "provider") {
                        OutlinedTextField(
                            value = skillsText,
                            onValueChange = { skillsText = it },
                            label = { Text("Skills & Certifications (comma-separated, max 10)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = startingPriceText,
                                onValueChange = { startingPriceText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Starting Price (₹)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = availability,
                                onValueChange = { availability = it },
                                label = { Text("Working Hours / Availability") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val parsedSkills = skillsText.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(10)
                            onSaveProfile(
                                baseProfile.copy(
                                    displayName = displayName,
                                    headline = headline,
                                    bio = bio,
                                    role = lockedRole,
                                    city = city,
                                    locality = locality,
                                    skills = parsedSkills,
                                    startingPrice = startingPriceText.toLongOrNull() ?: 0L,
                                    availability = availability,
                                    isPublicProvider = lockedRole == "provider",
                                    publicPhone = if (preferences.showPhonePublicly) publicPhone else ""
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_profile_button")
                    ) {
                        Text("Save Profile Information", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 2 OF 9: 2. Verification status
        // =========================================================================
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .testTag("profile_section_2_verification")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Verification status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SkillBlue
                        )
                        Surface(
                            color = if (baseProfile.isStrictVerifiedBadgeActive) SkillBlue.copy(alpha = 0.16f) else SkillWarningAmber.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (baseProfile.isStrictVerifiedBadgeActive) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Blue Tick Verified by SkillConnect",
                                        tint = SkillBlue,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = baseProfile.verificationBadgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (baseProfile.isStrictVerifiedBadgeActive) SkillBlue else SkillWarningAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = if (lockedRole == "provider") {
                            "Provider KYC & Document Status: ${myVerification?.status?.uppercase() ?: baseProfile.verificationStatus.uppercase()}"
                        } else {
                            "Customer Account Verification: Verified Google Identity (${baseProfile.effectivePlatformId})"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!myVerification?.adminNote.isNullOrBlank() || baseProfile.verificationRejectionReason.isNotBlank()) {
                        Text(
                            text = "Co-Founder / Admin Review Note: ${myVerification?.adminNote?.ifBlank { baseProfile.verificationRejectionReason } ?: baseProfile.verificationRejectionReason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SkillCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (lockedRole == "provider") {
                        OutlinedButton(
                            onClick = { onNavigate(AppDestination.PROVIDER_VERIFICATION) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Manage Documents & Verification Submission")
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 3 OF 9: 3. Jobs, services, or bookings (includes Jobs & Courses Hub)
        // =========================================================================
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .testTag("profile_section_3_jobs_bookings")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "3. Jobs, services, or bookings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkillBlue
                    )

                    val activeCount = myBookings.count { it.status in listOf("requested", "accepted", "awaiting_payment", "confirmed", "in_progress") }
                    val completedCount = myBookings.count { it.status == "completed" }

                    Text(
                        text = "Bookings Summary: ${myBookings.size} Total • $activeCount Active • $completedCount Completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onNavigate(AppDestination.BOOKINGS) },
                            colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open Bookings (${myBookings.size})")
                        }
                        if (lockedRole == "provider") {
                            OutlinedButton(
                                onClick = { onNavigate(AppDestination.CREATE_EDIT_SERVICE) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Add Service")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onNavigate(AppDestination.EXPLORE) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Explore Services")
                            }
                        }
                    }

                    HorizontalDivider()

                    // Jobs & Earning Opportunities Hub (Preserved Core Feature)
                    Text(
                        text = "Jobs & Earning Opportunities (${preferences.appliedJobIds.size} Applied)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    MarketplaceCatalog.jobOpportunities.take(3).forEach { job ->
                        val isApplied = job.id in preferences.appliedJobIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(job.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text("${job.companyOrClient} • ${job.city} • ${job.payoutRangeInr}", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = { onToggleJobApplication(job) }) {
                                Text(if (isApplied) "Applied ✓" else "Apply")
                            }
                        }
                    }

                    // Skills & Courses Hub (Preserved Core Feature)
                    Text(
                        text = "Skills & Courses (${preferences.enrolledCourseIds.size} Enrolled)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    MarketplaceCatalog.skillCourses.take(3).forEach { course ->
                        val isEnrolled = course.id in preferences.enrolledCourseIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(course.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text("${course.instructorName} • ${course.durationHours}h • ${course.badgeEarned}", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = { onToggleCourseEnrollment(course) }) {
                                Text(if (isEnrolled) "Enrolled ✓" else "Enroll")
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 4 OF 9: 4. Booking Notices & Self Login Status (Admin Read/Delete for Messages)
        // =========================================================================
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .testTag("profile_section_4_chat")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isAdminUser) {
                            "4. Admin Messages & Notices (Read & Delete Enabled)"
                        } else {
                            "4. My Booking Notices & Self Login Status"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkillBlue
                    )
                    Text(
                        text = "My Self Login Status (Private to You): Signed in as ${displayName.ifBlank { "Member" }} (${baseProfile.effectivePlatformId}) • Role: ${lockedRole.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SkillSuccessGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isAdminUser) {
                        val unreadThreads = myConversations.count { it.unreadCustomerCount > 0 || it.unreadProviderCount > 0 }
                        Text(
                            text = "Admin Message Governance: Only Admins can read and delete platform messages. Active Threads: ${myConversations.size} (Unread: $unreadThreads)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Personal Booking Notices: ${myBookings.size} Booking Updates • Only your own booking notices and self login status are shown here. Reading & deleting messages is restricted to Administration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { onNavigate(AppDestination.MESSAGES) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isAdminUser) "Open Admin Messages & Notices Console"
                            else "Open My Booking & Self Login Notices"
                        )
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 5 OF 9: 5. Payments and transaction history (6 states + Disputes)
        // =========================================================================
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .testTag("profile_section_5_payments")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "5. Payments and transaction history",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkillBlue
                    )
                    Text(
                        text = "Tracked States: Pending • Processing • Successful • Failed • Refunded • Disputed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (myPayments.isEmpty()) {
                        Text(
                            text = "No payment or payout transactions recorded yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        myPayments.take(5).forEach { pay ->
                            val statusDisplay = when (pay.status) {
                                "succeeded", "paid_out" -> "SUCCESSFUL"
                                "refund_pending" -> "PROCESSING REFUND"
                                else -> pay.status.uppercase()
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${pay.transactionRef} • ₹${pay.amountInr}", fontWeight = FontWeight.Bold)
                                    Text("${pay.paymentType.uppercase()} via ${pay.method.uppercase()} • $statusDisplay", style = MaterialTheme.typography.bodySmall)
                                }
                                if (pay.bookingId.isNotBlank()) {
                                    val linkedBk = myBookings.firstOrNull { it.id == pay.bookingId }
                                    if (linkedBk != null) {
                                        TextButton(onClick = { disputeBookingTarget = linkedBk }) {
                                            Text("Raise Dispute", color = SkillErrorRed)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (myDisputes.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Active / Resolved Payment Disputes (${myDisputes.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        myDisputes.forEach { dsp ->
                            Surface(
                                color = SkillWarningAmber.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("${dsp.id} • ₹${dsp.disputedAmountInr} • ${dsp.status.uppercase()}", fontWeight = FontWeight.Bold)
                                    Text("Reason: ${dsp.reason} • Assigned Tier: ${dsp.assignedRole.uppercase()}", style = MaterialTheme.typography.bodySmall)
                                    if (dsp.adminDecisionNote.isNotBlank()) {
                                        Text("Decision: ${dsp.adminDecisionNote}", style = MaterialTheme.typography.bodySmall, color = SkillBlue)
                                    }
                                }
                            }
                        }
                    }

                    if (myBookings.isNotEmpty() && disputeBookingTarget == null) {
                        OutlinedButton(
                            onClick = { disputeBookingTarget = myBookings.first() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("File a Booking / Payment Dispute")
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 6 OF 9: 6. Help and support (includes Role Switch Request & Executive Admin Card)
        // =========================================================================
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .testTag("profile_section_6_support")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "6. Help and support",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkillBlue
                    )
                    Text(
                        text = "Submitted Support Tickets: ${mySupportTickets.size} • Strict Role Lock Active (${lockedRole.uppercase()})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onNavigate(AppDestination.HELP_AND_LEGAL) },
                            colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Help & Support Centre")
                        }
                        OutlinedButton(
                            onClick = { showRoleChangeDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Request Role Switch")
                        }
                    }

                    HorizontalDivider()

                    // Unified Admin Gmail Portal Quick Access (Founder, Co-Founder, CEO)
                    Text(
                        text = if (activeExecutiveRole.isNotBlank()) {
                            "Active Executive Admin Session: ${activeExecutiveRole.uppercase()} (${myAdminAccount?.canonicalAccountId ?: preferences.activeAdminCanonicalId})"
                        } else {
                            "Unified Admin Gmail Portal (Founder • Co-Founder • CEO)"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (activeExecutiveRole.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onNavigate(AppDestination.ADMIN_DASHBOARD) },
                                colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Open ${activeExecutiveRole.uppercase()} Console",
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = onLogoutExecutiveAdmin,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "End Admin Session",
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Your administrative role (FOUNDER, CO-FOUNDER, or CEO) is automatically assigned by your authenticated Google email. Unauthorized Gmail accounts are rejected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!showUnifiedAdminInput) {
                            OutlinedButton(
                                onClick = {
                                    showUnifiedAdminInput = true
                                    adminPasswordInput = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Open Unified Admin Gmail Login",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Unified Admin Gmail Auth", style = MaterialTheme.typography.labelMedium, color = SkillPurple, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Strict 3-Attempt Limit (60s Lockout)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkillWarningAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedTextField(
                                value = adminPasswordInput,
                                onValueChange = { adminPasswordInput = it },
                                label = { Text("Secondary Admin Password (Max 3 Attempts)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        onLoginExecutiveAdmin(
                                            "",
                                            "",
                                            adminPasswordInput
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SkillPurple),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Verify Admin Gmail & Open Console",
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        showUnifiedAdminInput = false
                                        adminPasswordInput = ""
                                    }
                                ) {
                                    Text("Cancel", maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 7 OF 9: 7. Notifications and settings
        // =========================================================================
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .testTag("profile_section_7_notifications_settings")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "7. Notifications and settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkillBlue
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Theme Mode")
                        Switch(
                            checked = preferences.isDarkMode,
                            onCheckedChange = onToggleDarkMode,
                            modifier = Modifier.testTag("settings_dark_mode_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Preferred Language")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = preferences.language == "en",
                                onClick = { onChangeLanguage("en") },
                                label = { Text("English") }
                            )
                            FilterChip(
                                selected = preferences.language == "hi",
                                onClick = { onChangeLanguage("hi") },
                                label = { Text("हिन्दी") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Push & Booking Alert Notifications")
                        Switch(
                            checked = preferences.pushNotificationsEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateNotificationPrivacy(preferences.emailNotifications, enabled, preferences.showPhonePublicly)
                                if (enabled) onRequestNotificationPermission()
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Nearby City Location Access")
                        OutlinedButton(onClick = onRequestLocationPermission) {
                            Text("Configure Location")
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 8 OF 9: 8. Privacy and account deletion
        // =========================================================================
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .testTag("profile_section_8_privacy")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "8. Privacy and account deletion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkillBlue
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Consent to Display Public Contact Number", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Only shown on your public profile if you explicitly toggle this on.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.showPhonePublicly,
                            onCheckedChange = {
                                onUpdateNotificationPrivacy(preferences.emailNotifications, preferences.pushNotificationsEnabled, it)
                            }
                        )
                    }

                    if (preferences.showPhonePublicly) {
                        OutlinedTextField(
                            value = publicPhone,
                            onValueChange = { publicPhone = it },
                            label = { Text("Indian Contact Number (+91 ...)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                val exportSummary = """
                                    SkillConnect Account Data Export
                                    Platform ID: ${baseProfile.effectivePlatformId}
                                    Name: $displayName
                                    Role: $lockedRole
                                    City: $city ($locality)
                                    Verification Status: ${baseProfile.verificationStatus}
                                """.trimIndent()
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, exportSummary)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Export Account Data"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export Data")
                        }

                        OutlinedButton(
                            onClick = { showDeleteAccountDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Request Deletion", color = SkillErrorRed)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 9 OF 9: 9. Logout
        // =========================================================================
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SkillErrorRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .testTag("profile_section_9_logout")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "9. Logout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkillErrorRed
                    )
                    Text(
                        text = "Sign out of Firebase Authentication, clear CredentialManager state and cached role sessions, and return to the login screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { showLogoutConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SkillErrorRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("logout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Confirm Logout", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to log out of SkillConnect (${baseProfile.effectivePlatformId})? Your active session and cached credentials will be cleared.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillErrorRed),
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text("Yes, Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (disputeBookingTarget != null) {
        val bk = disputeBookingTarget!!
        var reason by remember { mutableStateOf("Service Deliverable / Payment Issue") }
        var statement by remember { mutableStateOf("") }
        var evidence by remember { mutableStateOf("Booking Ref: ${bk.effectiveBookingRef}") }
        AlertDialog(
            onDismissRequest = { disputeBookingTarget = null },
            title = { Text("File Payment / Booking Dispute", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Booking: ${bk.serviceTitle} (${bk.effectiveBookingRef}) • ₹${bk.effectiveTotalInr}")
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Dispute Reason *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = statement,
                        onValueChange = { statement = it },
                        label = { Text("Your Statement *") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = evidence,
                        onValueChange = { evidence = it },
                        label = { Text("Evidence / Reference Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onFileDispute(
                            bk,
                            bk.paymentReferenceId,
                            reason,
                            statement.ifBlank { reason },
                            evidence
                        )
                        disputeBookingTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillErrorRed)
                ) {
                    Text("Submit Dispute")
                }
            },
            dismissButton = {
                TextButton(onClick = { disputeBookingTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (showRoleChangeDialog) {
        val targetRole = if (lockedRole == "customer") "provider" else "customer"
        var reason by remember { mutableStateOf("I would like to switch my account role to $targetRole.") }
        AlertDialog(
            onDismissRequest = { showRoleChangeDialog = false },
            title = { Text("Request Account Role Change", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "To protect marketplace integrity, account role changes (${lockedRole.uppercase()} → ${targetRole.uppercase()}) require an Admin-reviewed Support Ticket."
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason for Role Switch *") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRoleChangeDialog = false
                        onRequestRoleChangeTicket(targetRole, reason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillBlue)
                ) {
                    Text("Submit Role Change Ticket")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRoleChangeDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Request Account Deletion?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will submit a verified account deletion request ticket to SkillConnect Trust & Safety and sign you out.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        onRequestDeletionTicket()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillErrorRed)
                ) {
                    Text("Submit Deletion Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProviderVerificationScreen(
    profile: UserProfile?,
    submission: VerificationSubmission?,
    onSubmitVerification: (
        legalName: String,
        businessName: String,
        category: String,
        city: String,
        experienceYears: Long,
        portfolioSummary: String,
        documentReferenceNote: String
    ) -> Unit
) {
    var legalName by remember(submission, profile) { mutableStateOf(submission?.legalName ?: profile?.displayName.orEmpty()) }
    var businessName by remember(submission, profile) { mutableStateOf(submission?.businessName ?: profile?.businessName.orEmpty()) }
    var category by remember(submission, profile) { mutableStateOf(submission?.category ?: profile?.primaryCategory?.ifBlank { "Graphic Design" } ?: "Graphic Design") }
    var city by remember(submission, profile) { mutableStateOf(submission?.city ?: profile?.city ?: "Bengaluru") }
    var experienceYearsText by remember(submission, profile) { mutableStateOf((submission?.experienceYears ?: profile?.experienceYears ?: 2L).toString()) }
    var portfolioSummary by remember(submission, profile) { mutableStateOf(submission?.portfolioSummary ?: profile?.bio.orEmpty()) }
    var documentReferenceNote by remember(submission) { mutableStateOf(submission?.documentReferenceNote ?: "Aadhaar Masked: XXXX-XXXX-4821") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val strictVerified = profile?.isStrictVerifiedBadgeActive == true
    val badgeLabel = profile?.verificationBadgeLabel ?: "Verification Not Submitted"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("provider_verification_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SkillNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = if (strictVerified) SkillSuccessGreen else SkillWarningAmber)
                        Text(
                            text = "Provider Verification Workflow",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "6-Stage Status: $badgeLabel",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (strictVerified) SkillSuccessGreen else SkillWarningAmber
                    )
                    if (!submission?.adminNote.isNullOrBlank()) {
                        Text(
                            text = "Admin Reviewer Note: ${submission?.adminNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SkillCyan
                        )
                    }
                    Text(
                        text = "Your verification details are stored privately in an isolated collection restricted to you and authorized platform administrators. The 'Verified by SkillConnect' badge appears ONLY after explicit Admin approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Submit Professional Verification Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = legalName,
                        onValueChange = { legalName = it },
                        label = { Text("Full Legal Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Studio / Business Name (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Primary Category *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = experienceYearsText,
                            onValueChange = { experienceYearsText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Years of Exp.") },
                            singleLine = true,
                            modifier = Modifier.weight(0.6f)
                        )
                    }

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Operating City in India *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = portfolioSummary,
                        onValueChange = { portfolioSummary = it },
                        label = { Text("Professional Work & Portfolio Summary *") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = documentReferenceNote,
                        onValueChange = { documentReferenceNote = it },
                        label = { Text("Masked Government ID / Trade Certificate Reference *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (validationError != null) {
                        Text(validationError!!, color = SkillErrorRed, style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            if (legalName.trim().length < 2) {
                                validationError = "Please enter your full legal name."
                                return@Button
                            }
                            if (portfolioSummary.trim().length < 5) {
                                validationError = "Please provide a brief summary of your professional work."
                                return@Button
                            }
                            validationError = null
                            onSubmitVerification(
                                legalName,
                                businessName,
                                category,
                                city,
                                experienceYearsText.toLongOrNull() ?: 1L,
                                portfolioSummary,
                                documentReferenceNote
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_verification_btn")
                    ) {
                        Text("Submit for Verification Review", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionsScreen(
    profile: UserProfile?,
    globalSettings: PlatformSetting?,
    myPayments: List<PaymentRecord> = emptyList(),
    onSubscribePlan: (planName: String, amountInr: Long, paymentMethod: String, utrReference: String, payerUpiNote: String) -> Unit = { _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    val freeLimit = globalSettings?.freeListingLimit ?: 3L
    val providerPremiumPrice = globalSettings?.premiumPriceInr ?: 499L
    val customerPremiumPrice = (providerPremiumPrice / 2L).coerceAtLeast(199L)
    val period = globalSettings?.billingPeriod ?: "Monthly"

    val userRole = profile?.role ?: "customer"
    var selectedRoleTab by remember(userRole) { mutableStateOf(userRole) }
    var checkoutPlan by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var upiLaunchFeedback by remember { mutableStateOf<String?>(null) }

    val subscriptionPayments = remember(myPayments) {
        myPayments.filter { it.paymentType == "subscription" }
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }
    val hasPendingSubscription = subscriptionPayments.any { it.status == "pending" }
    val currentPlan = profile?.subscriptionPlan ?: "free"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("subscriptions_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SkillNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkillConnectBrandLogo(compact = true, onNavyHeader = true)
                        Surface(
                            color = if (currentPlan == "premium") SkillSuccessGreen else SkillCyan.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "ACTIVE PLAN: ${currentPlan.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "SkillConnect Direct UPI Subscriptions",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Available for both Customer and Service Provider profiles. Pay via any installed UPI app, submit your 12-digit UTR, and our CEO/Founder team will activate Premium after verifying the merchant statement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedRoleTab == "customer",
                            onClick = { selectedRoleTab = "customer" },
                            label = {
                                Text(
                                    text = if (userRole == "customer") "Customer Plans (Your Role)" else "Customer Plans",
                                    color = if (selectedRoleTab == "customer") SkillNavy else Color.White
                                )
                            },
                            modifier = Modifier.testTag("sub_tab_customer")
                        )
                        FilterChip(
                            selected = selectedRoleTab == "provider",
                            onClick = { selectedRoleTab = "provider" },
                            label = {
                                Text(
                                    text = if (userRole == "provider") "Provider Plans (Your Role)" else "Provider Plans",
                                    color = if (selectedRoleTab == "provider") SkillNavy else Color.White
                                )
                            },
                            modifier = Modifier.testTag("sub_tab_provider")
                        )
                    }
                }
            }
        }

        // Permanent Official Merchant Payment Detail Card (Shown to Everyone Purchasing a Subscription)
        item {
            val activeTabPrice = if (selectedRoleTab == "customer") customerPremiumPrice else providerPremiumPrice
            var pageUpiFeedback by remember { mutableStateOf<String?>(null) }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, SkillBlue, RoundedCornerShape(16.dp))
                    .testTag("merchant_payment_detail_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Merchant Payment Details",
                                tint = SkillBlue
                            )
                            Text(
                                text = "Official Merchant Payment Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = SkillSuccessGreen.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "VERIFIED UPI",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkillSuccessGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        color = SkillBlue.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Merchant Name: $MERCHANT_UPI_NAME",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Permanent UPI ID: $MERCHANT_UPI_ID",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = SkillBlue,
                                modifier = Modifier.testTag("merchant_upi_id_display")
                            )
                            Text(
                                text = "Selected Plan Amount: ₹$activeTabPrice / $period (${if (selectedRoleTab == "customer") "Customer Plus" else "Provider Pro"})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Merchant UPI ID", MERCHANT_UPI_ID))
                                pageUpiFeedback = "Copied $MERCHANT_UPI_ID to clipboard."
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("page_copy_merchant_upi_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy UPI ID",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy UPI ID", style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = {
                                val uri = Uri.parse(
                                    "upi://pay?pa=${Uri.encode(MERCHANT_UPI_ID)}" +
                                        "&pn=${Uri.encode(MERCHANT_UPI_NAME)}" +
                                        "&am=$activeTabPrice&cu=INR" +
                                        "&tn=${Uri.encode("SkillConnect ${selectedRoleTab.uppercase()} Premium")}"
                                )
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                runCatching {
                                    context.startActivity(intent)
                                    pageUpiFeedback = "Opened UPI app for $MERCHANT_UPI_ID (₹$activeTabPrice). After payment, tap Upgrade below to submit your UTR."
                                }.onFailure {
                                    pageUpiFeedback = "No UPI app detected on this device. Send ₹$activeTabPrice to $MERCHANT_UPI_ID from any UPI app and tap Upgrade below."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("page_open_upi_app_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = "Open UPI App",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open UPI App", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (pageUpiFeedback != null) {
                        Text(
                            text = pageUpiFeedback!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = SkillPurple,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Pending or Historical Subscription Requests Banner
        if (subscriptionPayments.isNotEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (hasPendingSubscription) SkillWarningAmber else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Your Subscription Payment Requests (${subscriptionPayments.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (hasPendingSubscription) {
                            Text(
                                text = "Your Direct UPI UTR submission is currently PENDING verification. Premium is never activated from the Android client—an active CEO or Founder will verify the credit in the merchant bank/UPI statement.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SkillWarningAmber
                            )
                        }
                        subscriptionPayments.take(5).forEach { pay ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${pay.transactionRef} • ₹${pay.amountInr}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = pay.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = when (pay.status) {
                                        "succeeded" -> SkillSuccessGreen.copy(alpha = 0.15f)
                                        "pending" -> SkillWarningAmber.copy(alpha = 0.15f)
                                        else -> SkillErrorRed.copy(alpha = 0.15f)
                                    },
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = pay.status.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (pay.status) {
                                            "succeeded" -> SkillSuccessGreen
                                            "pending" -> SkillWarningAmber
                                            else -> SkillErrorRed
                                        },
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedRoleTab == "customer") {
            // Customer Free Plan Card
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, SkillBlue, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Free Customer Plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("₹0 / Forever", style = MaterialTheme.typography.titleMedium, color = SkillBlue, fontWeight = FontWeight.Bold)
                        }
                        Text("• Discover & hire verified local professionals across India")
                        Text("• Unlimited service enquiries, custom quote requests & in-app chat")
                        Text("• Access to SkillConnect Jobs, Earning Opportunities & Courses hub")
                        Text("• Standard booking protection & Co-Founder/CEO dispute resolution")
                        Surface(color = SkillSuccessGreen.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = if (currentPlan == "free") "CURRENT ACTIVE PLAN" else "INCLUDED IN ACCOUNT",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkillSuccessGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Customer Premium Plan Card
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, SkillPurple, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = SkillPurple)
                            Text("SkillConnect Plus (Customer Premium)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Text("₹$customerPremiumPrice / $period", style = MaterialTheme.typography.titleMedium, color = SkillPurple, fontWeight = FontWeight.Bold)
                        Text("• Zero platform convenience fee (save 5% on every confirmed service booking)")
                        Text("• Priority matching with top-rated Verified by SkillConnect professionals")
                        Text("• Dedicated priority support & fast-track dispute assistance")
                        Text("• Direct UPI payment with CEO/Founder bank statement verification")
                        Button(
                            onClick = { checkoutPlan = "premium" to customerPremiumPrice },
                            colors = ButtonDefaults.buttonColors(containerColor = SkillPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("subscribe_customer_plus_btn")
                        ) {
                            Text(
                                when {
                                    currentPlan == "premium" -> "Submit Renewal Request (₹$customerPremiumPrice)"
                                    hasPendingSubscription -> "Submit Updated UPI UTR (₹$customerPremiumPrice)"
                                    else -> "Upgrade via Direct UPI (₹$customerPremiumPrice)"
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // Provider Free Plan Card
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, SkillBlue, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Free Provider Plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("₹0 / Forever", style = MaterialTheme.typography.titleMedium, color = SkillBlue, fontWeight = FontWeight.Bold)
                        }
                        Text("• Complete public provider profile & skill portfolio")
                        Text("• Up to $freeLimit active service listings")
                        Text("• Direct customer enquiries, quote requests, and in-app messaging")
                        Text("• Eligible for Verified by SkillConnect badge upon admin document review")
                        Surface(color = SkillSuccessGreen.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = if (currentPlan == "free") "CURRENT ACTIVE PLAN" else "INCLUDED IN ACCOUNT",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkillSuccessGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Provider Pro Plan Card
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, SkillPurple, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = SkillPurple)
                            Text("SkillConnect Pro (Provider Premium)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Text("₹$providerPremiumPrice / $period", style = MaterialTheme.typography.titleMedium, color = SkillPurple, fontWeight = FontWeight.Bold)
                        Text("• Up to 15 active service listings & 'Featured' placement eligibility")
                        Text("• Enhanced portfolio showcase (up to 6 rich projects) & priority enquiry filters")
                        Text("• Transparent disclosure: Paid plans never manipulate ratings or claim higher trustworthiness.")
                        Button(
                            onClick = { checkoutPlan = "premium" to providerPremiumPrice },
                            colors = ButtonDefaults.buttonColors(containerColor = SkillPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("subscribe_pro_plan_btn")
                        ) {
                            Text(
                                when {
                                    currentPlan == "premium" -> "Submit Renewal Request (₹$providerPremiumPrice)"
                                    hasPendingSubscription -> "Submit Updated UPI UTR (₹$providerPremiumPrice)"
                                    else -> "Upgrade to Pro via Direct UPI (₹$providerPremiumPrice)"
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (checkoutPlan != null) {
        val (planKey, price) = checkoutPlan!!
        var utrInput by remember { mutableStateOf("") }
        var payerNoteInput by remember { mutableStateOf("") }
        var validationError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                checkoutPlan = null
                upiLaunchFeedback = null
            },
            title = {
                Text(
                    text = "Direct UPI Subscription (₹$price / $period)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = SkillBlue.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Step 1: Pay Official Merchant UPI ID", fontWeight = FontWeight.Bold, color = SkillBlue)
                            Text("Merchant Name: $MERCHANT_UPI_NAME", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "Permanent UPI ID: $MERCHANT_UPI_ID",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = SkillBlue,
                                modifier = Modifier.testTag("dialog_merchant_upi_id")
                            )
                            Text(
                                text = "Amount Payable: ₹$price / $period",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("Merchant UPI ID", MERCHANT_UPI_ID))
                                        upiLaunchFeedback = "Copied $MERCHANT_UPI_ID to clipboard."
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dialog_copy_upi_id_btn")
                                ) {
                                    Text("Copy UPI ID", style = MaterialTheme.typography.labelSmall)
                                }
                                Button(
                                    onClick = {
                                        val uri = Uri.parse(
                                            "upi://pay?pa=${Uri.encode(MERCHANT_UPI_ID)}" +
                                                "&pn=${Uri.encode(MERCHANT_UPI_NAME)}" +
                                                "&am=$price&cu=INR" +
                                                "&tn=${Uri.encode("SkillConnect ${selectedRoleTab.uppercase()} Premium")}"
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        runCatching {
                                            context.startActivity(intent)
                                            upiLaunchFeedback = "Opened UPI app for $MERCHANT_UPI_ID. Complete payment and enter your 12-digit UTR below."
                                        }.onFailure {
                                            upiLaunchFeedback = "No installed UPI app found on this device. Pay ₹$price to $MERCHANT_UPI_ID from any UPI app and enter the UTR below."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("launch_upi_app_btn")
                                ) {
                                    Text("Open UPI App", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (upiLaunchFeedback != null) {
                                Text(
                                    text = upiLaunchFeedback!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkillPurple
                                )
                            }
                        }
                    }

                    Text("Step 2: Submit Your UPI UTR / Reference", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = utrInput,
                        onValueChange = {
                            utrInput = it
                            validationError = null
                        },
                        label = { Text("12-Digit UPI UTR / Transaction Ref *") },
                        placeholder = { Text("e.g. 430219876543") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subscription_utr_input")
                    )

                    OutlinedTextField(
                        value = payerNoteInput,
                        onValueChange = { payerNoteInput = it },
                        label = { Text("Your UPI App / Payer VPA Note (Optional)") },
                        placeholder = { Text("e.g. Paid via GPay from name@okaxis") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Security Notice: Submitting a UTR creates a PENDING request and leaves your profile on Free until an active CEO or Founder confirms the credit in the merchant bank statement.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (validationError != null) {
                        Text(
                            text = validationError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = SkillErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanUtr = utrInput.trim()
                        if (cleanUtr.length < 6) {
                            validationError = "Please enter a valid UPI UTR / reference number (at least 6 characters)."
                            return@Button
                        }
                        checkoutPlan = null
                        upiLaunchFeedback = null
                        onSubscribePlan(planKey, price, "upi", cleanUtr, payerNoteInput.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen),
                    modifier = Modifier.testTag("submit_upi_subscription_btn")
                ) {
                    Text("Submit Pending UPI Request")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        checkoutPlan = null
                        upiLaunchFeedback = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardScreen(
    isAdminUser: Boolean,
    activeExecutiveRole: String = "founder",
    myAdminAccount: AdminAccount? = null,
    allAdmins: List<AdminAccount> = emptyList(),
    allDisputes: List<PaymentDispute> = emptyList(),
    allUsers: List<UserProfile> = emptyList(),
    allServices: List<ServiceListing>,
    publicProviders: List<UserProfile>,
    allVerifications: List<VerificationSubmission> = emptyList(),
    allBookings: List<BookingRequest> = emptyList(),
    allPayments: List<PaymentRecord> = emptyList(),
    allConversations: List<Conversation> = emptyList(),
    allMessages: List<ChatMessage> = emptyList(),
    reviews: List<ServiceReview>,
    reports: List<SafetyReport>,
    supportTickets: List<SupportTicket> = emptyList(),
    auditLogs: List<AdminAuditLog> = emptyList(),
    globalSettings: PlatformSetting?,
    onSaveGlobalSettings: (
        announcementText: String,
        announcementActive: Boolean,
        freeListingLimit: Long,
        premiumPriceInr: Long,
        billingPeriod: String,
        paymentsDemoMode: Boolean
    ) -> Unit,
    onApproveProvider: (userId: String, approve: Boolean) -> Unit,
    onVerificationDecision: (providerId: String, decision: String, reasonOrNotes: String) -> Unit = { uid, dec, _ ->
        onApproveProvider(uid, dec == "approve")
    },
    onModerateService: (serviceId: String, isPublished: Boolean, isFeatured: Boolean, status: String, reason: String) -> Unit,
    onSetUserAccountStatus: (userId: String, status: String, reason: String) -> Unit = { _, _, _ -> },
    onProcessPaymentAction: (payment: PaymentRecord, newStatus: String, note: String) -> Unit = { _, _, _ -> },
    onReviewSubscriptionPayment: (payment: PaymentRecord, approve: Boolean, verificationNote: String) -> Unit = { _, _, _ -> },
    onResolveSafetyReport: (reportId: String, status: String, note: String) -> Unit = { _, _, _ -> },
    onRespondSupportTicket: (ticket: SupportTicket, status: String, responseNote: String, approveRoleChangeTo: String?) -> Unit = { _, _, _, _ -> },
    onHandleDispute: (dispute: PaymentDispute, newStatus: String, assignedRole: String, decisionNote: String) -> Unit = { _, _, _, _ -> },
    onDeleteMessage: (messageId: String) -> Unit = {},
    onDeleteConversation: (conversationId: String) -> Unit = {},
    onLoginExecutiveAdmin: (role: String, username: String, password: String) -> Unit = { _, _, _ -> },
    onLogoutExecutiveAdmin: () -> Unit = {},
    onChangeAdminPassword: (currentPassword: String, newPassword: String) -> Unit = { _, _ -> },
    onFounderManageAdmin: (targetAdminUid: String, newRole: String, tempPassword: String) -> Unit = { _, _, _ -> }
) {
    val effectiveRole = (myAdminAccount?.adminRole?.ifBlank { activeExecutiveRole } ?: activeExecutiveRole)
        .lowercase()
        .ifBlank { if (isAdminUser) "founder" else "" }

    val isCoFounder = effectiveRole == "cofounder"
    val isCeo = effectiveRole == "ceo"
    val isFounder = effectiveRole == "founder"
    val isFounderAccessGroup = isFounder || isCoFounder

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var selectedInspectUser by remember { mutableStateOf<UserProfile?>(null) }

    if (!isAdminUser && effectiveRole.isBlank()) {
        var passwordInput by remember { mutableStateOf("") }
        var gateAttemptsUsed by remember { mutableStateOf(0) }
        var gateLockedUntilMs by remember { mutableStateOf(0L) }
        val nowMs = System.currentTimeMillis()
        val isGateLocked = gateLockedUntilMs > nowMs
        val attemptsLeft = (3 - gateAttemptsUsed).coerceAtLeast(0)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .testTag("admin_unauthorized_gate"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = SkillPurple, modifier = Modifier.size(48.dp))
            Text("Unified Admin Gmail Login Page", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Strict 3-Attempt Security Policy: Only 3 login attempts are permitted before a 60-second lockout. Your administrative role (FOUNDER, CO-FOUNDER, or CEO) is automatically assigned by your authenticated Google email.",
                style = MaterialTheme.typography.bodySmall,
                color = SkillWarningAmber,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                color = SkillPurple.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, SkillPurple, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Authorized Admin Gmail Verification", fontWeight = FontWeight.Bold, color = SkillPurple)
                        Text(
                            text = if (isGateLocked) "LOCKED (0/3 left)" else "Attempts left: $attemptsLeft/3",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isGateLocked) SkillErrorRed else SkillWarningAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "• FOUNDER: Founder-level access, Founder identity & permissions.\n• CO-FOUNDER: Founder-level Admin Access Group with separate Co-Founder identity & role.\n• CEO: Separate CEO role & CEO-specific dashboard permissions.\n• All unauthorized Gmail accounts are immediately rejected.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Secondary Admin Password (Max 3 Attempts)") },
                        singleLine = true,
                        enabled = !isGateLocked,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val currentNow = System.currentTimeMillis()
                            if (gateLockedUntilMs > currentNow) return@Button
                            val nextCount = gateAttemptsUsed + 1
                            gateAttemptsUsed = nextCount
                            if (nextCount >= 3) {
                                gateLockedUntilMs = currentNow + 60_000L
                            }
                            onLoginExecutiveAdmin("", "", passwordInput)
                        },
                        enabled = !isGateLocked,
                        colors = ButtonDefaults.buttonColors(containerColor = SkillPurple),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isGateLocked) "Locked for 60s (3/3 Attempts Used)"
                            else "Verify Authorized Admin Gmail ($attemptsLeft/3 left)",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        return
    }

    var activeSection by remember(effectiveRole) {
        mutableStateOf("overview")
    }
    var userSearchQuery by remember { mutableStateOf("") }
    var decisionReasonInput by remember { mutableStateOf("Verified identity & portfolio details.") }
    var announcementText by remember(globalSettings) {
        mutableStateOf(globalSettings?.announcementText ?: "Welcome to SkillConnect India — Verified Local Professionals")
    }
    var announcementActive by remember(globalSettings) { mutableStateOf(globalSettings?.announcementActive ?: true) }
    var freeLimitText by remember(globalSettings) { mutableStateOf((globalSettings?.freeListingLimit ?: 3L).toString()) }
    var premiumPriceText by remember(globalSettings) { mutableStateOf((globalSettings?.premiumPriceInr ?: 499L).toString()) }
    var decisionNoteInput by remember { mutableStateOf("Reviewed with full transaction & booking evidence.") }
    var subscriptionVerificationNote by remember {
        mutableStateOf("Verified UTR credit in merchant bank/UPI statement.")
    }

    val effectiveUsers = if (allUsers.isNotEmpty()) allUsers else publicProviders
    val customersList = effectiveUsers.filter { it.role == "customer" }
    val providersList = effectiveUsers.filter { it.role == "provider" }

    // Role-scoped navigation tabs
    val adminSections = buildList {
        add("overview" to "1. Executive Overview")
        add("verification_queue" to "2. Verification Queue (${allVerifications.size.coerceAtLeast(providersList.size)})")
        add("disputes" to "3. Payment Disputes (${allDisputes.size})")
        add("tickets" to "4. Support Tickets (${supportTickets.size})")
        add("reports" to "5. Safety Reports (${reports.size})")
        add("users" to "6. Users (${effectiveUsers.size})")
        add("customers" to "7. Customers (${customersList.size})")
        add("providers" to "8. Providers (${providersList.size})")
        add("listings" to "9. Services (${allServices.size})")
        add("categories" to "10. Categories (${MarketplaceCatalog.categories.size})")
        add("bookings" to "11. Bookings (${allBookings.size})")
        add("reviews" to "12. Reviews (${reviews.size})")
        if (isCeo || isFounderAccessGroup) {
            add("payments" to "13. Payments & Refunds (${allPayments.size})")
            add("payouts" to "14. Provider Payouts")
            add("subscriptions" to "15. Subscriptions")
            add("announcements" to "16. Announcements")
            add("settings" to "17. Platform Settings")
        }
        if (isFounder) {
            add("executive_admins" to "18. Founder Admin Manager (${allAdmins.size})")
        }
        add("audit_logs" to "19. Audit Logs (${auditLogs.size})")
        add("messages" to "20. Messages — Admin Read & Delete (${allMessages.size})")
    }

    val roleDisplayBadge = when (effectiveRole) {
        "cofounder" -> "CO-FOUNDER (ADM-COF-001)"
        "ceo" -> "CEO (ADM-CEO-001)"
        else -> "FOUNDER (ADM-FND-001)"
    }
    val accessGroupBadge = when {
        isFounder -> "Access Group: Founder-Level Admin Access (Founder Identity & Full Authority)"
        isCoFounder -> "Access Group: Founder-Level Admin Access Group (Co-Founder Identity & Role)"
        else -> "Access Group: CEO Role & CEO-Specific Dashboard Permissions"
    }
    val sessionPolicyBadge = if (isFounder) {
        "Session Policy: Persistent Executive Session (Founder 15-Min Timeout Exception Active)"
    } else {
        "Session Policy: 15-Minute Inactivity / Background Timeout Enforced"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SkillNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkillConnectBrandLogo(compact = true, onNavyHeader = true)
                        Surface(
                            color = when {
                                isFounder -> SkillPurple
                                isCoFounder -> SkillCyan.copy(alpha = 0.35f)
                                else -> SkillBlue
                            },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = when {
                                    isCoFounder -> "CO-FOUNDER"
                                    isCeo -> "CEO"
                                    else -> "FOUNDER"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = roleDisplayBadge,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = accessGroupBadge,
                        style = MaterialTheme.typography.labelMedium,
                        color = SkillSuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = sessionPolicyBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = SkillCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when {
                            isCoFounder -> "Scope: Founder-Level Admin Access Group with separate Co-Founder identity — Full Verification, Bookings, Refunds, Platform Settings, Moderation & Audit Logs. (Authorized Admin Account updates remain exclusive to Founder)"
                            isCeo -> "Scope: Separate CEO Role & CEO-Specific Dashboard Permissions — Business Analytics, Bookings, Verifications, Operational Settings & Refund Approvals up to ₹10,000 INR."
                            else -> "Scope: Highest Founder Authority — Founder-Level Access, Exclusive Admin Account Governance, Unlimited Refund Approvals, Commission Reports, Emergency Maintenance & Immutable Audit Logs."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showChangePasswordDialog = true },
                            modifier = Modifier.testTag("admin_change_password_btn")
                        ) {
                            Text("Change Password", color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = onLogoutExecutiveAdmin,
                            modifier = Modifier.testTag("admin_logout_session_btn")
                        ) {
                            Text("End Admin Session", color = SkillWarningAmber, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                        }
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        adminSections.forEach { (key, label) ->
                            FilterChip(
                                selected = activeSection == key,
                                onClick = { activeSection = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        when (activeSection) {
            "overview", "announcements", "subscriptions", "settings" -> {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        DashboardStatCard(Modifier.weight(1f), "Total Users", "${effectiveUsers.size}", SkillBlue)
                        DashboardStatCard(Modifier.weight(1f), "Published Listings", "${allServices.size}", SkillSuccessGreen)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        DashboardStatCard(Modifier.weight(1f), "Bookings", "${allBookings.size}", SkillPurple)
                        DashboardStatCard(Modifier.weight(1f), "Open Disputes", "${allDisputes.count { it.status != "resolved" && it.status != "rejected" }}", SkillWarningAmber)
                    }
                }

                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                if (isFounderAccessGroup) "Founder-Level Admin Access Group: Platform, Subscription & Emergency Controls (${if (isCoFounder) "CO-FOUNDER" else "FOUNDER"})"
                                else "CEO Operational & Announcement Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                                OutlinedTextField(
                                    value = announcementText,
                                    onValueChange = { announcementText = it },
                                    label = { Text("Homepage Announcement Banner") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Show Announcement Bar on Homepage")
                                    Switch(checked = announcementActive, onCheckedChange = { announcementActive = it })
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = freeLimitText,
                                        onValueChange = { freeLimitText = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Free Plan Listing Limit") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = premiumPriceText,
                                        onValueChange = { premiumPriceText = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Pro Plan Price (₹)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Button(
                                    onClick = {
                                        onSaveGlobalSettings(
                                            announcementText,
                                            announcementActive,
                                            freeLimitText.toLongOrNull() ?: 3L,
                                            premiumPriceText.toLongOrNull() ?: 499L,
                                            "Monthly",
                                            false
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save Platform Settings")
                                }
                            }
                        }
                    }
            }

            "disputes" -> {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tiered Dispute & Refund Resolution Policy", fontWeight = FontWeight.Bold, color = SkillBlue)
                            Text(
                                "• Co-Founder: Reviews customer/provider statements & flags/escalates disputes to CEO.\n" +
                                    "• CEO: Investigates disputes & approves refunds up to ₹10,000 INR (<= ₹10,000). Escalates > ₹10,000 INR to Founder.\n" +
                                    "• Founder: Unlimited refund approval authority (> ₹10,000 INR and all amounts).",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedTextField(
                                value = decisionNoteInput,
                                onValueChange = { decisionNoteInput = it },
                                label = { Text("Admin Resolution / Escalation Note *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (allDisputes.isEmpty()) {
                    item {
                        Text("No payment or booking disputes filed yet.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    items(allDisputes, key = { it.id }) { disp ->
                        val overCeoCap = disp.disputedAmountInr > 10000L
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Booking: ${disp.bookingId} • ₹${disp.disputedAmountInr}", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = disp.status.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (disp.status.startsWith("resolved")) SkillSuccessGreen else SkillWarningAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text("Reason: ${disp.reason} • Assigned Tier: ${disp.assignedRole.uppercase()}", style = MaterialTheme.typography.bodySmall)
                                if (disp.customerStatement.isNotBlank()) {
                                    Text("Customer Statement: ${disp.customerStatement}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (disp.providerStatement.isNotBlank()) {
                                    Text("Provider Statement: ${disp.providerStatement}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (disp.evidenceNote.isNotBlank()) {
                                    Text("Evidence: ${disp.evidenceNote}", style = MaterialTheme.typography.labelSmall, color = SkillBlue)
                                }
                                if (disp.adminDecisionNote.isNotBlank()) {
                                    Text("Decision Note: ${disp.adminDecisionNote}", style = MaterialTheme.typography.labelSmall, color = SkillSuccessGreen)
                                }

                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (isCeo) {
                                        if (!overCeoCap) {
                                            Button(
                                                onClick = {
                                                    onHandleDispute(disp, "resolved_refunded", "ceo", "CEO Refund Approved (₹${disp.disputedAmountInr}): $decisionNoteInput")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen)
                                            ) {
                                                Text("CEO Approve Refund (₹${disp.disputedAmountInr} <= ₹10k)")
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    onHandleDispute(disp, "escalated_to_founder", "founder", "Escalated > ₹10,000 INR to Founder Group: $decisionNoteInput")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SkillPurple)
                                            ) {
                                                Text("Escalate > ₹10,000 to Founder")
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onHandleDispute(disp, "resolved_rejected", "ceo", decisionNoteInput)
                                            }
                                        ) {
                                            Text("Reject Dispute", color = SkillErrorRed)
                                        }
                                    }
                                    if (isFounderAccessGroup) {
                                        val actorTitle = if (isCoFounder) "Co-Founder" else "Founder"
                                        Button(
                                            onClick = {
                                                onHandleDispute(disp, "resolved_refunded", effectiveRole, "$actorTitle Unlimited Approval (₹${disp.disputedAmountInr}): $decisionNoteInput")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen)
                                        ) {
                                            Text("$actorTitle Approve Refund (₹${disp.disputedAmountInr})")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onHandleDispute(disp, "resolved_rejected", effectiveRole, decisionNoteInput)
                                            }
                                        ) {
                                            Text("Reject Dispute", color = SkillErrorRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "executive_admins" -> {
                if (!isFounder) {
                    item {
                        Text("Only the Founder can manage administrative accounts and roles.", color = SkillErrorRed)
                    }
                } else {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SkillPurple, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Founder Executive Account & Role Governance", fontWeight = FontWeight.Bold, color = SkillPurple)
                                Text(
                                    "Canonical Executive Accounts:\n" +
                                        "• ADM-COF-001 → CO-FOUNDER\n" +
                                        "• ADM-CEO-001 → CEO\n" +
                                        "• ADM-FND-001 → FOUNDER",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    items(allAdmins, key = { it.adminUid.ifBlank { it.canonicalAccountId } }) { adm ->
                        var resetPassInput by remember { mutableStateOf("") }
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${adm.displayName} • ${adm.canonicalAccountId}", fontWeight = FontWeight.Bold)
                                Text(
                                    "Role: ${adm.adminRole.uppercase()} • Username: ${adm.username} • Persistent Session: ${adm.founderSessionPersistent}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedTextField(
                                    value = resetPassInput,
                                    onValueChange = { resetPassInput = it },
                                    label = { Text("Optional New Temporary Password (min 8 chars)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("cofounder", "ceo", "founder").forEach { targetRole ->
                                        OutlinedButton(
                                            onClick = {
                                                onFounderManageAdmin(adm.adminUid, targetRole, resetPassInput)
                                                resetPassInput = ""
                                            }
                                        ) {
                                            Text("Set ${targetRole.uppercase()}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "users", "customers", "providers" -> {
                val targetList = when (activeSection) {
                    "customers" -> customersList
                    "providers" -> providersList
                    else -> effectiveUsers
                }.filter { u ->
                    val q = userSearchQuery.trim().lowercase()
                    q.isEmpty() ||
                        u.displayName.lowercase().contains(q) ||
                        u.effectivePlatformId.lowercase().contains(q) ||
                        u.city.lowercase().contains(q) ||
                        u.publicPhone.lowercase().contains(q)
                }

                item {
                    OutlinedTextField(
                        value = userSearchQuery,
                        onValueChange = { userSearchQuery = it },
                        label = { Text("Search by Name, City, Phone, or SC-CUS / SC-PRO ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                items(targetList, key = { it.userId.ifBlank { it.id } }) { usr ->
                    val isProvider = usr.role == "provider"
                    val isBlueTickActive = usr.isStrictVerifiedBadgeActive
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("${usr.displayName} • ${usr.effectivePlatformId}", fontWeight = FontWeight.Bold)
                                    if (isProvider && isBlueTickActive) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Blue Tick Verified by SkillConnect",
                                            tint = SkillBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                if (isProvider) {
                                    Surface(
                                        color = if (isBlueTickActive) SkillBlue.copy(alpha = 0.14f) else SkillWarningAmber.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isBlueTickActive) {
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = "Blue Tick",
                                                    tint = SkillBlue,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Text(
                                                text = usr.verificationBadgeLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isBlueTickActive) SkillBlue else SkillWarningAmber,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "Role: ${usr.role.uppercase()} • City: ${usr.city} (${usr.locality.ifBlank { "All Areas" }}) • Account Status: ${usr.accountStatus.uppercase()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { selectedInspectUser = usr },
                                    colors = ButtonDefaults.buttonColors(containerColor = SkillNavy),
                                    modifier = Modifier.testTag("admin_view_full_details_${usr.userId}")
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("View Full Details")
                                }
                                if (isProvider) {
                                    Button(
                                        onClick = {
                                            onVerificationDecision(
                                                usr.userId,
                                                "approve",
                                                "Approved by ${effectiveRole.uppercase()} — Blue Tick Verified by SkillConnect"
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                                        modifier = Modifier.testTag("admin_approve_blue_tick_${usr.userId}")
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isBlueTickActive) "Blue Tick Active (Verified)" else "Approve Profile (Blue Tick)")
                                    }
                                }
                                if (usr.accountStatus != "suspended") {
                                    OutlinedButton(onClick = { onSetUserAccountStatus(usr.userId, "suspended", "Suspended by ${effectiveRole.uppercase()}") }) {
                                        Text("Suspend Account", color = SkillErrorRed)
                                    }
                                } else {
                                    Button(
                                        onClick = { onSetUserAccountStatus(usr.userId, "active", "Reactivated by ${effectiveRole.uppercase()}") },
                                        colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen)
                                    ) {
                                        Text("Reactivate Account")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "verification_queue" -> {
                item {
                    OutlinedTextField(
                        value = decisionReasonInput,
                        onValueChange = { decisionReasonInput = it },
                        label = { Text("Admin Verification Decision Note / Rejection Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(providersList, key = { "vq_${it.userId}" }) { prov ->
                    val isBlueTickActive = prov.isStrictVerifiedBadgeActive
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("${prov.displayName} (${prov.effectivePlatformId}) • ${prov.city}", fontWeight = FontWeight.Bold)
                                    if (isBlueTickActive) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Blue Tick Verified by SkillConnect",
                                            tint = SkillBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Surface(
                                    color = if (isBlueTickActive) SkillBlue.copy(alpha = 0.14f) else SkillWarningAmber.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isBlueTickActive) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "Blue Tick",
                                                tint = SkillBlue,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(
                                            text = prov.verificationBadgeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isBlueTickActive) SkillBlue else SkillWarningAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Current Badge Status: ${prov.verificationBadgeLabel} • Account: ${prov.accountStatus.uppercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBlueTickActive) SkillBlue else SkillWarningAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { onVerificationDecision(prov.userId, "approve", decisionReasonInput) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                                    modifier = Modifier.testTag("vq_approve_blue_tick_${prov.userId}")
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Approve Profile (Blue Tick)")
                                }
                                Button(
                                    onClick = { selectedInspectUser = prov },
                                    colors = ButtonDefaults.buttonColors(containerColor = SkillNavy),
                                    modifier = Modifier.testTag("vq_view_full_details_${prov.userId}")
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("View Full Details")
                                }
                                OutlinedButton(
                                    onClick = { onVerificationDecision(prov.userId, "reject", decisionReasonInput) }
                                ) {
                                    Text("Reject with Reason", color = SkillErrorRed)
                                }
                                OutlinedButton(
                                    onClick = { onVerificationDecision(prov.userId, "request_info", decisionReasonInput) }
                                ) {
                                    Text("Request Resubmission", color = SkillWarningAmber)
                                }
                                OutlinedButton(
                                    onClick = { onVerificationDecision(prov.userId, "suspend", decisionReasonInput) }
                                ) {
                                    Text("Suspend Listing", color = SkillErrorRed)
                                }
                                OutlinedButton(
                                    onClick = { onVerificationDecision(prov.userId, "revoke", decisionReasonInput) }
                                ) {
                                    Text("Revoke Badge")
                                }
                            }
                        }
                    }
                }
            }

            "listings" -> {
                items(allServices, key = { it.id }) { srv ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${srv.title} by ${srv.providerName}", fontWeight = FontWeight.Bold)
                            Text("${srv.category} • ₹${srv.startingPrice} • Featured: ${srv.isFeatured}", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        onModerateService(srv.id, srv.isPublished, !srv.isFeatured, "approved", "Updated by admin")
                                    }
                                ) {
                                    Text(if (srv.isFeatured) "Remove Featured" else "Mark Featured")
                                }
                                OutlinedButton(
                                    onClick = {
                                        onModerateService(srv.id, false, false, "rejected", "Removed by admin moderation")
                                    }
                                ) {
                                    Text("Unpublish", color = SkillErrorRed)
                                }
                            }
                        }
                    }
                }
            }

            "categories" -> {
                items(MarketplaceCatalog.categories, key = { it.id }) { cat ->
                    val count = allServices.count { it.category.equals(cat.id, ignoreCase = true) }
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${cat.nameEn} (${cat.nameHi})", fontWeight = FontWeight.Bold)
                            Text("$count active listings", color = SkillBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            "bookings" -> {
                items(allBookings, key = { it.id }) { bk ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${bk.effectiveBookingRef} • ${bk.serviceTitle}", fontWeight = FontWeight.Bold)
                            Text("Customer: ${bk.customerName} → Provider: ${bk.providerName}", style = MaterialTheme.typography.bodySmall)
                            Text("Status: ${bk.status.uppercase()} • Payment: ${bk.paymentStatus.uppercase()} (₹${bk.totalAmountInr})", style = MaterialTheme.typography.labelSmall, color = SkillBlue)
                        }
                    }
                }
            }

            "reviews" -> {
                items(reviews, key = { it.id }) { rev ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${rev.customerName} (${rev.rating} ★)", fontWeight = FontWeight.Bold)
                            Text(rev.comment, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            "reports" -> {
                if (reports.isEmpty()) {
                    item { Text("No open safety reports.") }
                } else {
                    items(reports, key = { it.id }) { rep ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Target: ${rep.targetType} (${rep.targetId}) • Status: ${rep.status.uppercase()}", fontWeight = FontWeight.Bold)
                                Text("Reason: ${rep.reason}", style = MaterialTheme.typography.bodySmall)
                                Text("Details: ${rep.details}", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onResolveSafetyReport(rep.id, "resolved", "Reviewed & actioned by ${effectiveRole.uppercase()}") },
                                        colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen)
                                    ) {
                                        Text("Mark Resolved")
                                    }
                                    OutlinedButton(
                                        onClick = { onResolveSafetyReport(rep.id, "dismissed", "Dismissed after review") }
                                    ) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "payments", "payouts" -> {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SkillPurple, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Founder / Co-Founder / CEO Direct UPI Subscription Verification",
                                fontWeight = FontWeight.Bold,
                                color = SkillPurple
                            )
                            Text(
                                "Verify the user's submitted 12-digit UTR, amount, and date against the merchant bank/UPI statement before approving. Approving marks the payment SUCCEEDED and activates subscriptionPlan = 'premium'.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedTextField(
                                value = subscriptionVerificationNote,
                                onValueChange = { subscriptionVerificationNote = it },
                                label = { Text("Merchant Statement Verification Note (min 8 chars) *") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_subscription_verification_note_input")
                            )
                        }
                    }
                }

                val filteredPayments = if (activeSection == "payouts") {
                    allPayments.filter { it.paymentType == "payout" || it.status == "pending" }
                } else {
                    allPayments
                }
                if (filteredPayments.isEmpty()) {
                    item { Text("No payment or payout records in this view yet.") }
                } else {
                    items(filteredPayments, key = { it.id }) { pay ->
                        val exceedsCeoThreshold = pay.amountInr > 10000L
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${pay.notes.ifBlank { pay.paymentType }} (${pay.transactionRef})", fontWeight = FontWeight.Bold)
                                Text("Type: ${pay.paymentType.uppercase()} • Method: ${pay.method.uppercase()} • Amount: ₹${pay.amountInr} • User: ${pay.userId}", style = MaterialTheme.typography.bodySmall)
                                Text("Status: ${pay.status.uppercase()} • Key: ${pay.id}", style = MaterialTheme.typography.labelSmall, color = SkillBlue)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (pay.paymentType == "subscription" && pay.status == "pending" && (isCeo || isFounderAccessGroup)) {
                                        Button(
                                            onClick = {
                                                onReviewSubscriptionPayment(pay, true, subscriptionVerificationNote)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen),
                                            modifier = Modifier.testTag("approve_subscription_payment_${pay.id}")
                                        ) {
                                            Text("Approve & Activate Premium")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onReviewSubscriptionPayment(pay, false, subscriptionVerificationNote)
                                            },
                                            modifier = Modifier.testTag("reject_subscription_payment_${pay.id}")
                                        ) {
                                            Text("Reject UTR", color = SkillErrorRed)
                                        }
                                    }
                                    if (pay.status == "refund_pending" || pay.status == "disputed") {
                                        if (isFounderAccessGroup || (isCeo && !exceedsCeoThreshold)) {
                                            Button(
                                                onClick = { onProcessPaymentAction(pay, "refunded", "Refund approved by ${effectiveRole.uppercase()}") },
                                                colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen)
                                            ) {
                                                Text(if (isCeo) "CEO Approve Refund (<= ₹10k)" else "${if (isCoFounder) "Co-Founder" else "Founder"} Approve Refund")
                                            }
                                        } else if (isCeo && exceedsCeoThreshold) {
                                            Text(
                                                "Requires Founder / Co-Founder approval (> ₹10,000 INR)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SkillWarningAmber,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    if (pay.paymentType == "payout" && pay.status == "pending") {
                                        Button(
                                            onClick = { onProcessPaymentAction(pay, "paid_out", "Payout settled by ${effectiveRole.uppercase()}") },
                                            colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen)
                                        ) {
                                            Text("Mark Payout Settled")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "tickets" -> {
                if (supportTickets.isEmpty()) {
                    item { Text("No support tickets submitted yet.") }
                } else {
                    items(supportTickets, key = { it.id }) { tkt ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${tkt.category} • ${tkt.name} (${tkt.contactEmail})", fontWeight = FontWeight.Bold)
                                Text(tkt.description, style = MaterialTheme.typography.bodySmall)
                                Text("Status: ${tkt.status.uppercase()}", style = MaterialTheme.typography.labelSmall, color = SkillBlue)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onRespondSupportTicket(tkt, "resolved", "Resolved by SkillConnect ${effectiveRole.uppercase()}", null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen)
                                    ) {
                                        Text("Resolve Ticket")
                                    }
                                    if ((isCeo || isFounderAccessGroup) && tkt.category.contains("Role Change", ignoreCase = true)) {
                                        OutlinedButton(
                                            onClick = { onRespondSupportTicket(tkt, "resolved", "Role switched to Provider", "provider") }
                                        ) {
                                            Text("Approve → Provider")
                                        }
                                        OutlinedButton(
                                            onClick = { onRespondSupportTicket(tkt, "resolved", "Role switched to Customer", "customer") }
                                        ) {
                                            Text("Approve → Customer")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "audit_logs" -> {
                if (auditLogs.isEmpty()) {
                    item { Text("No admin audit log entries recorded yet.") }
                } else {
                    items(auditLogs, key = { it.id }) { log ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${log.actionType.uppercase()} → ${log.targetCollection} (${log.targetId})", fontWeight = FontWeight.Bold)
                                Text("${log.reason} (${log.previousState} → ${log.newState})", style = MaterialTheme.typography.bodySmall)
                                Text("By: ${log.adminEmail}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            "messages" -> {
                item {
                    Surface(
                        color = SkillBlue.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SkillBlue.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Admin Exclusive Message Governance (Read & Delete)",
                                fontWeight = FontWeight.Bold,
                                color = SkillBlue
                            )
                            Text(
                                text = "Only Administrators can read and delete platform messages and conversations. Customers and Service Providers only see their own Booking Notices and Self Login Status.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (allConversations.isNotEmpty()) {
                    item {
                        Text(
                            text = "All Conversations (${allConversations.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(allConversations, key = { "adm_conv_${it.id}" }) { conv ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${conv.customerName} ↔ ${conv.providerName}", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Service: ${conv.serviceTitle.ifBlank { "General" }} • Last: ${conv.lastMessage.ifBlank { "No messages" }}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onDeleteConversation(conv.id) },
                                    modifier = Modifier.testTag("admin_portal_delete_conv_${conv.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = SkillErrorRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete Thread", color = SkillErrorRed)
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "All Platform Messages (${allMessages.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (allMessages.isEmpty()) {
                    item {
                        Text("No messages recorded in Firestore.", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    items(allMessages, key = { "adm_msg_${it.id}" }) { msg ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${msg.senderName} (${msg.senderId}) • Thread: ${msg.conversationId}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SkillBlue
                                    )
                                    Text(msg.text, style = MaterialTheme.typography.bodyMedium)
                                }
                                OutlinedButton(
                                    onClick = { onDeleteMessage(msg.id) },
                                    modifier = Modifier.testTag("admin_portal_delete_msg_${msg.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = SkillErrorRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", color = SkillErrorRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedInspectUser != null) {
        val inspectUser = selectedInspectUser!!
        val userVerification = allVerifications.firstOrNull { it.userId == inspectUser.userId }
        val userBookings = allBookings.filter { it.customerId == inspectUser.userId || it.providerId == inspectUser.userId }
        val userPayments = allPayments.filter { it.userId == inspectUser.userId }
        val userServices = allServices.filter { it.providerId == inspectUser.userId }
        val isBlueTick = inspectUser.isStrictVerifiedBadgeActive

        AlertDialog(
            onDismissRequest = { selectedInspectUser = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Full User Details: ${inspectUser.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (inspectUser.role == "provider" && isBlueTick) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Blue Tick Verified by SkillConnect",
                            tint = SkillBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .testTag("admin_full_user_details_dialog")
                ) {
                    item {
                        Surface(
                            color = if (isBlueTick) SkillBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Platform ID: ${inspectUser.effectivePlatformId}",
                                    fontWeight = FontWeight.Bold,
                                    color = SkillBlue
                                )
                                Text("Firebase UID: ${inspectUser.userId}", style = MaterialTheme.typography.bodySmall)
                                Text("Role: ${inspectUser.role.uppercase()} • Account Status: ${inspectUser.accountStatus.uppercase()}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = "Verification Badge: ${inspectUser.verificationBadgeLabel} (${inspectUser.verificationStatus.uppercase()})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isBlueTick) SkillBlue else SkillWarningAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    item {
                        Text("Location & Contact Details", fontWeight = FontWeight.Bold, color = SkillBlue)
                        Text("State / City / Locality: ${inspectUser.serviceState.ifBlank { "India" }} • ${inspectUser.city} • ${inspectUser.locality.ifBlank { "All Areas" }}", style = MaterialTheme.typography.bodySmall)
                        Text("Public Phone: ${inspectUser.publicPhone.ifBlank { "Not Shared / Private" }}", style = MaterialTheme.typography.bodySmall)
                        Text("Preferred Language: ${inspectUser.preferredLanguage.uppercase()} • Subscription Plan: ${inspectUser.subscriptionPlan.uppercase()}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (inspectUser.role == "provider") {
                        item {
                            HorizontalDivider()
                            Text("Service Provider Professional Details", fontWeight = FontWeight.Bold, color = SkillBlue)
                            Text("Business Name: ${inspectUser.businessName.ifBlank { userVerification?.businessName ?: "Individual Professional" }}", style = MaterialTheme.typography.bodySmall)
                            Text("Headline: ${inspectUser.headline.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                            Text("Bio / Portfolio: ${inspectUser.bio.ifBlank { userVerification?.portfolioSummary ?: "N/A" }}", style = MaterialTheme.typography.bodySmall)
                            Text("Primary Category: ${inspectUser.primaryCategory.ifBlank { "General" }} • Experience: ${inspectUser.experienceYears} yrs", style = MaterialTheme.typography.bodySmall)
                            Text("Skills: ${inspectUser.skills.joinToString(", ").ifBlank { "None listed" }}", style = MaterialTheme.typography.bodySmall)
                            Text("Starting Price: ₹${inspectUser.startingPrice} • Availability: ${inspectUser.availability.ifBlank { "Flexible" }}", style = MaterialTheme.typography.bodySmall)
                            Text("Completed Jobs: ${inspectUser.completedJobsCount} • Rating: ${inspectUser.averageRating} (${inspectUser.reviewCount} reviews)", style = MaterialTheme.typography.bodySmall)
                            if (userVerification != null) {
                                Text("Submitted KYC Document Note: ${userVerification.documentReferenceNote}", style = MaterialTheme.typography.bodySmall, color = SkillPurple)
                            }
                        }
                    }
                    item {
                        HorizontalDivider()
                        Text(
                            text = "Platform Activity Summary",
                            fontWeight = FontWeight.Bold,
                            color = SkillBlue
                        )
                        Text(
                            text = "• Published Services: ${userServices.size}\n" +
                                "• Bookings Involved: ${userBookings.size}\n" +
                                "• Payments / Transactions: ${userPayments.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (inspectUser.role == "provider") {
                        Button(
                            onClick = {
                                onVerificationDecision(
                                    inspectUser.userId,
                                    "approve",
                                    "Approved by ${effectiveRole.uppercase()} — Blue Tick Verified by SkillConnect"
                                )
                                selectedInspectUser = inspectUser.copy(
                                    isVerified = true,
                                    verificationStatus = "approved",
                                    accountStatus = "active"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SkillBlue)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBlueTick) "Verified by SkillConnect ✓" else "Approve (Blue Tick)")
                        }
                    }
                    TextButton(onClick = { selectedInspectUser = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        var currentPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Change Executive Admin Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Passwords are salted and hashed with SHA-256 before storage. Raw passwords are never stored in source code or Firestore.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = currentPass,
                        onValueChange = { currentPass = it },
                        label = { Text("Current Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New Password (min 8 chars)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showChangePasswordDialog = false
                        onChangeAdminPassword(currentPass, newPass)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillBlue)
                ) {
                    Text("Update Password Hash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun HelpCentreAndLegalScreen(
    isHindi: Boolean,
    defaultName: String,
    defaultEmail: String,
    supportTickets: List<SupportTicket>,
    onSubmitSupportTicket: (
        name: String,
        email: String,
        category: String,
        relatedBookingId: String,
        description: String
    ) -> Unit
) {
    var activeTab by remember { mutableStateOf("support") }
    var name by remember(defaultName) { mutableStateOf(defaultName) }
    var email by remember(defaultEmail) { mutableStateOf(defaultEmail) }
    var category by remember { mutableStateOf("Booking or Enquiry Issue") }
    var relatedBookingId by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("help_and_legal_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SkillConnectOfficialLogoBanner(
                modifier = Modifier.fillMaxWidth(),
                height = 96.dp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isHindi) "हेल्प सेंटर और कानूनी नीतियां" else "Help Centre, Support & Legal Policies",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = activeTab == "support",
                    onClick = { activeTab = "support" },
                    label = { Text("Contact Support") }
                )
                FilterChip(
                    selected = activeTab == "faqs",
                    onClick = { activeTab = "faqs" },
                    label = { Text("FAQs") }
                )
                FilterChip(
                    selected = activeTab == "legal",
                    onClick = { activeTab = "legal" },
                    label = { Text("Legal & Policies") }
                )
            }
        }

        when (activeTab) {
            "support" -> {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Submit a Support Request", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Your Name *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Contact Email *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                label = { Text("Issue Category *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = relatedBookingId,
                                onValueChange = { relatedBookingId = it },
                                label = { Text("Related Booking ID (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Describe the issue in detail *") },
                                minLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("support_description_input")
                            )

                            if (validationError != null) {
                                Text(validationError!!, color = SkillErrorRed, style = MaterialTheme.typography.bodySmall)
                            }

                            Button(
                                onClick = {
                                    if (description.trim().length < 5) {
                                        validationError = "Please describe your issue (at least 5 characters)."
                                        return@Button
                                    }
                                    validationError = null
                                    onSubmitSupportTicket(name, email, category, relatedBookingId, description)
                                    description = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_support_ticket_btn")
                            ) {
                                Text("Submit Support Ticket", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (supportTickets.isNotEmpty()) {
                    item {
                        Text("Your Submitted Tickets (${supportTickets.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(supportTickets, key = { it.id }) { tkt ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(tkt.category, fontWeight = FontWeight.Bold)
                                    Text(tkt.status.uppercase(), color = SkillBlue, fontWeight = FontWeight.Bold)
                                }
                                Text(tkt.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            "faqs" -> {
                items(MarketplaceCatalog.faqs) { faq ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (isHindi) faq.questionHi else faq.questionEn, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (isHindi) faq.answerHi else faq.answerEn,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            "legal" -> {
                items(MarketplaceCatalog.legalPolicies) { policy ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (isHindi) policy.titleHi else policy.titleEn,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = policy.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = SkillBlue
                            )
                            policy.clauses.forEach { clause ->
                                Text(clause, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
