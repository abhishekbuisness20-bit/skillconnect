package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AdminRoleDoc
import com.example.data.MarketplaceRepository
import com.example.model.*
import com.example.ui.MarketplaceUiState
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// =========================================================================
// 1. FOUNDER DASHBOARD (SUPER ADMIN)
// =========================================================================
@Composable
fun FounderDashboardScreen(
    uiState: MarketplaceUiState,
    onVerifyProvider: (providerId: String, verified: Boolean) -> Unit,
    onAddCategory: (ServiceCategory) -> Unit,
    onBroadcastAnnouncement: (String) -> Unit,
    onUpdatePermissions: (targetUid: String, newPermissions: AdminPermissions) -> Unit,
    onRevokeAdmin: (targetUid: String) -> Unit,
    onToggleAdminActive: (targetUid: String) -> Unit,
    onDeleteEntity: (id: String, isProvider: Boolean, reason: String) -> Unit,
    onProvisionAccounts: (founderPass: String, ceoPass: String, adminPass: String) -> Unit
) {
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var showProvisionDialog by remember { mutableStateOf(false) }
    var editingAdminDoc by remember { mutableStateOf<AdminRoleDoc?>(null) }
    var pendingConfirmationAction by remember { mutableStateOf<ConfirmationPayload?>(null) }

    val pendingProviders = uiState.allProviders.filter { !it.isVerified }
    val verifiedProviders = uiState.allProviders.filter { it.isVerified }
    val openComplaints = uiState.complaintsList.filter { it.status != ComplaintStatus.RESOLVED && it.status != ComplaintStatus.DISMISSED }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("founder_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Management Authorization Header
        item {
            GlassCard(borderColor = CyanBrand) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(NavyBackground)
                            .border(2.dp, CyanBrand, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = CyanBrand, modifier = Modifier.size(26.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Founder Command Center",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SUPER ADMIN",
                                color = Color(0xFF001F2B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyanBrand)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Founder: Abhishek Singh (Super Admin)",
                            color = CyanBrand,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Admins: Abhay (CEO), Adarsh/Abhay (Admin)",
                            color = TextLight,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Authenticated: ${uiState.currentUser.email}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    IconButton(
                        onClick = { showProvisionDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NavySurfaceVariant)
                            .testTag("founder_provision_wizard_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Provision Admin Accounts",
                            tint = CyanBrand,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("admin_session_timeout_indicator")
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = GreenBrand,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Zero-Trust Security: 15-min inactivity timeout • UID Verification Active",
                        color = GreenBrand,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Platform KPIs
        item {
            Text(
                text = "Marketplace Health & Platform Governance",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminMetricCard(
                        title = "Platform Users",
                        value = "${uiState.adminMetrics.totalUsers}",
                        color = CyanBrand,
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = "Service Providers",
                        value = "${uiState.allProviders.size}",
                        color = GreenBrand,
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = "Admin Accounts",
                        value = "${uiState.adminRolesList.size}",
                        color = BlueBrand,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminMetricCard(
                        title = "Active Work Orders",
                        value = "${uiState.adminMetrics.activeWorkRequests}",
                        color = WarningOrange,
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = "Open Disputes",
                        value = "${openComplaints.size}",
                        color = if (openComplaints.isNotEmpty()) ErrorRed else SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = "Audit Events",
                        value = "${uiState.auditLogsList.size}",
                        color = CyanBrand,
                        modifier = Modifier.weight(1f)
                    )
                }

                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Marketplace Gross Volume", color = TextMuted, fontSize = 11.sp)
                            IndianRupeeText(amount = uiState.adminMetrics.totalVolumeInr, fontSize = 20)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showBroadcastDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Broadcast", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        }

        // Section: Administrator Accounts & Roles Governance (FOUNDER ONLY)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manage CEO & Admin Accounts",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showProvisionDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NavySurfaceVariant),
                    border = BorderStroke(1.dp, CyanBrand),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("founder_provision_account_btn")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = CyanBrand, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Provision Wizard", color = CyanBrand, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(uiState.adminRolesList) { roleDoc ->
            val isFounderAccount = roleDoc.role == "FOUNDER"
            val permissions = roleDoc.toDomainPermissions()
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, if (isFounderAccount) CyanBrand else if (roleDoc.isActive) NavyCardBorder else ErrorRed.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_account_card_${roleDoc.userId}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = roleDoc.email,
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = roleDoc.accessLevel,
                                    color = if (isFounderAccount) CyanBrand else GreenBrand,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background((if (isFounderAccount) CyanBrand else GreenBrand).copy(alpha = 0.18f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = "UID: ${roleDoc.userId} • Status: ${if (roleDoc.isActive) "Active" else "Revoked / Suspended"}",
                                color = if (roleDoc.isActive) TextMuted else ErrorRed,
                                fontSize = 10.sp
                            )
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (roleDoc.isActive) SuccessGreen.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f))
                                .border(1.dp, if (roleDoc.isActive) SuccessGreen else ErrorRed, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (roleDoc.isActive) "Active" else "Suspended",
                                color = if (roleDoc.isActive) SuccessGreen else ErrorRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Key Permission Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PermissionBadge("Admins", permissions.canManageAdmins)
                        PermissionBadge("Roles", permissions.canManageRoles)
                        PermissionBadge("Approve", permissions.canApproveProviders)
                        PermissionBadge("Complaints", permissions.canResolveComplaints)
                        PermissionBadge("Logs", permissions.canAccessSecurityLogs)
                        PermissionBadge("Delete", permissions.canDeletePlatformData)
                    }

                    // Action Buttons (Founder has full control; cannot revoke Founder self)
                    if (!isFounderAccount) {
                        HorizontalDivider(color = NavyCardBorder.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { editingAdminDoc = roleDoc },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("edit_permissions_${roleDoc.userId}")
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = CyanBrand, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Permissions", color = CyanBrand, fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = {
                                    pendingConfirmationAction = ConfirmationPayload(
                                        title = if (roleDoc.isActive) "Suspend Administrator?" else "Re-activate Administrator?",
                                        message = "Are you sure you want to ${if (roleDoc.isActive) "suspend" else "activate"} access for ${roleDoc.email}?",
                                        actionLabel = if (roleDoc.isActive) "Suspend" else "Activate",
                                        isDestructive = roleDoc.isActive,
                                        onConfirm = { onToggleAdminActive(roleDoc.userId) }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (roleDoc.isActive) WarningOrange.copy(alpha = 0.2f) else SuccessGreen.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = if (roleDoc.isActive) "Suspend" else "Activate",
                                    color = if (roleDoc.isActive) WarningOrange else SuccessGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = {
                                    pendingConfirmationAction = ConfirmationPayload(
                                        title = "Revoke Administrator Access?",
                                        message = "This will immediately revoke management privileges for ${roleDoc.email} (${roleDoc.role}).",
                                        actionLabel = "Revoke",
                                        isDestructive = true,
                                        onConfirm = { onRevokeAdmin(roleDoc.userId) }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("revoke_admin_${roleDoc.userId}")
                            ) {
                                Text("Revoke", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section: Provider Verification Queue
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Provider Verification Queue (${pendingProviders.size})",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${verifiedProviders.size} Verified",
                    color = GreenBrand,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (pendingProviders.isEmpty()) {
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenBrand)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "All service providers are verified. No pending items.",
                            color = TextLight,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(pendingProviders) { prov ->
                GlassCard(borderColor = WarningOrange.copy(alpha = 0.5f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = prov.name, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = prov.professionalTitle, color = CyanBrand, fontSize = 12.sp)
                            Text(text = "${prov.city}, ${prov.state} • ${prov.skills.joinToString(", ")}", color = TextMuted, fontSize = 11.sp)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { onVerifyProvider(prov.id, true) },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenBrand),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(38.dp).testTag("approve_verification_${prov.id}")
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF00210E), modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Approve", color = Color(0xFF00210E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    pendingConfirmationAction = ConfirmationPayload(
                                        title = "Delete Service Provider?",
                                        message = "Are you sure you want to delete ${prov.name}'s profile from SkillConnect?",
                                        actionLabel = "Delete",
                                        isDestructive = true,
                                        onConfirm = { onDeleteEntity(prov.id, true, "Rejected by Founder") }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text("Delete", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section: System Security Audit Logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Security Audit Logs (${uiState.auditLogsList.size})",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Immutable Trail",
                    color = CyanBrand,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        items(uiState.auditLogsList.take(6)) { log ->
            AuditLogCard(log = log)
        }

        // Section: Category Management
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Service Categories (${uiState.categories.size})",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { showAddCategoryDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = CyanBrand, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Category", color = CyanBrand, fontSize = 12.sp)
                }
            }
        }

        items(uiState.categories) { cat ->
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cat.nameEn, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(cat.descriptionEn, color = TextMuted, fontSize = 11.sp)
                        Text("Subcategories: ${cat.subcategories.take(3).joinToString(", ")}...", color = CyanBrand, fontSize = 10.sp)
                    }
                    Text("${cat.subcategories.size} skills", color = GreenBrand, fontSize = 11.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSubmit = { newCat ->
                onAddCategory(newCat)
                showAddCategoryDialog = false
            }
        )
    }

    if (showBroadcastDialog) {
        BroadcastDialog(
            onDismiss = { showBroadcastDialog = false },
            onSubmit = { msg ->
                onBroadcastAnnouncement(msg)
                showBroadcastDialog = false
            }
        )
    }

    if (showProvisionDialog) {
        AdminAccountProvisioningDialog(
            onDismiss = { showProvisionDialog = false },
            onSubmit = { fPass, cPass, aPass ->
                onProvisionAccounts(fPass, cPass, aPass)
                showProvisionDialog = false
            }
        )
    }

    editingAdminDoc?.let { doc ->
        EditPermissionsDialog(
            adminDoc = doc,
            onDismiss = { editingAdminDoc = null },
            onSubmit = { updatedPermissions ->
                onUpdatePermissions(doc.userId, updatedPermissions)
                editingAdminDoc = null
            }
        )
    }

    pendingConfirmationAction?.let { conf ->
        ConfirmationActionDialog(
            payload = conf,
            onDismiss = { pendingConfirmationAction = null }
        )
    }
}

// =========================================================================
// 2. CEO DASHBOARD (EXECUTIVE ADMIN)
// =========================================================================
@Composable
fun CeoDashboardScreen(
    uiState: MarketplaceUiState,
    onVerifyProvider: (providerId: String, verified: Boolean) -> Unit,
    onResolveComplaint: (complaintId: String, status: ComplaintStatus, notes: String) -> Unit,
    onBroadcastPromotion: (String) -> Unit
) {
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var resolvingComplaint by remember { mutableStateOf<PlatformComplaint?>(null) }

    val pendingProviders = uiState.allProviders.filter { !it.isVerified }
    val escalatedComplaints = uiState.complaintsList.filter {
        it.status == ComplaintStatus.ESCALATED_TO_CEO || it.status == ComplaintStatus.INVESTIGATING || it.status == ComplaintStatus.PENDING
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ceo_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // CEO Executive Header
        item {
            GlassCard(borderColor = GreenBrand) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(NavyBackground)
                            .border(2.dp, GreenBrand, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = GreenBrand, modifier = Modifier.size(26.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CEO Executive Operations",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "EXECUTIVE ADMIN",
                                color = Color(0xFF00210E),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GreenBrand)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Executive: Abhay (CEO)",
                            color = GreenBrand,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Authenticated: ${uiState.currentUser.email}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = GreenBrand, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("15-Min Inactivity Auto-Logout Active", color = GreenBrand, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Business Operations KPIs
        item {
            Text("Business Operations & Dispute Management", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminMetricCard(
                        title = "Gross Volume",
                        value = "₹${uiState.adminMetrics.totalVolumeInr / 1000}k",
                        color = CyanBrand,
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = "Active Contracts",
                        value = "${uiState.adminMetrics.activeWorkRequests}",
                        color = GreenBrand,
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = "Pending Approvals",
                        value = "${pendingProviders.size}",
                        color = WarningOrange,
                        modifier = Modifier.weight(1f)
                    )
                }

                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Marketing & Platform Growth", color = TextMuted, fontSize = 11.sp)
                            Text("Broadcast promotions to all users", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showBroadcastDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenBrand),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Promote", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Escalated Disputes & Complaints Queue
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customer & Provider Disputes (${escalatedComplaints.size})",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("CEO Oversight", color = WarningOrange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (escalatedComplaints.isEmpty()) {
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenBrand)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("No pending or escalated complaints. Operations healthy.", color = TextLight, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(escalatedComplaints) { complaint ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = BorderStroke(1.dp, if (complaint.status == ComplaintStatus.ESCALATED_TO_CEO) WarningOrange else NavyCardBorder),
                    modifier = Modifier.fillMaxWidth().testTag("complaint_card_${complaint.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = complaint.reason,
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (complaint.status == ComplaintStatus.ESCALATED_TO_CEO) WarningOrange.copy(alpha = 0.2f) else CyanBrand.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = complaint.status.label,
                                    color = if (complaint.status == ComplaintStatus.ESCALATED_TO_CEO) WarningOrange else CyanBrand,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = complaint.details,
                            color = TextLight,
                            fontSize = 12.sp
                        )

                        Text(
                            text = "Reporter: ${complaint.reporterName} • Target: ${complaint.targetProviderName ?: "General"}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )

                        if (complaint.resolutionNotes.isNotBlank()) {
                            Text(
                                text = "Current Notes: ${complaint.resolutionNotes}",
                                color = GreenBrand,
                                fontSize = 11.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { resolvingComplaint = complaint },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenBrand),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp).testTag("ceo_resolve_complaint_${complaint.id}")
                            ) {
                                Text("Take Action", color = Color(0xFF00210E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Provider Approval Queue
        item {
            Text("Provider Approval Queue (${pendingProviders.size})", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (pendingProviders.isEmpty()) {
            item {
                GlassCard {
                    Text("All provider applicants approved.", color = TextLight, fontSize = 12.sp)
                }
            }
        } else {
            items(pendingProviders) { prov ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prov.name, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(prov.professionalTitle, color = GreenBrand, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { onVerifyProvider(prov.id, true) },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenBrand),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Approve", color = Color(0xFF00210E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    if (showBroadcastDialog) {
        BroadcastDialog(
            onDismiss = { showBroadcastDialog = false },
            onSubmit = { msg ->
                onBroadcastPromotion(msg)
                showBroadcastDialog = false
            }
        )
    }

    resolvingComplaint?.let { comp ->
        ResolveComplaintDialog(
            complaint = comp,
            isCeo = true,
            onDismiss = { resolvingComplaint = null },
            onSubmit = { status, notes ->
                onResolveComplaint(comp.id, status, notes)
                resolvingComplaint = null
            }
        )
    }
}

// =========================================================================
// 3. ADMIN DASHBOARD (LIMITED ADMIN)
// =========================================================================
@Composable
fun AdminDashboardScreen(
    uiState: MarketplaceUiState,
    onVerifyProvider: (providerId: String, verified: Boolean) -> Unit,
    onAddCategory: (ServiceCategory) -> Unit,
    onBroadcastAnnouncement: (String) -> Unit,
    onResolveComplaint: (complaintId: String, status: ComplaintStatus, notes: String) -> Unit = { _, _, _ -> }
) {
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var resolvingComplaint by remember { mutableStateOf<PlatformComplaint?>(null) }

    val pendingProviders = uiState.allProviders.filter { !it.isVerified }
    val assignedComplaints = uiState.complaintsList.filter { it.status == ComplaintStatus.PENDING || it.status == ComplaintStatus.INVESTIGATING }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Management Authorization Header
        item {
            GlassCard(borderColor = BlueBrand) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(NavyBackground)
                            .border(1.5.dp, BlueBrand, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = BlueBrand, modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Admin Operations Desk",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIMITED ADMIN",
                                color = BlueBrand,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BlueBrand.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Text(
                            text = "Admin: Abhay / Adarsh",
                            color = CyanBrand,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Authenticated: ${uiState.currentUser.email}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("admin_session_timeout_indicator")
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = GreenBrand, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("15-min inactivity auto-logout active", color = GreenBrand, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Support & Moderation Metrics
        item {
            Text("Operations & Provider Verification", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminMetricCard(
                    title = "Pending Verification",
                    value = "${pendingProviders.size}",
                    color = if (pendingProviders.isNotEmpty()) ErrorRed else SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                AdminMetricCard(
                    title = "Assigned Inquiries",
                    value = "${assignedComplaints.size}",
                    color = WarningOrange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Provider Verification Queue
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Provider Verification Queue (${pendingProviders.size})",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (pendingProviders.isEmpty()) {
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenBrand)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("All providers verified. Queue clear.", color = TextLight, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(pendingProviders) { prov ->
                GlassCard(borderColor = WarningOrange.copy(alpha = 0.5f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prov.name, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(prov.professionalTitle, color = CyanBrand, fontSize = 12.sp)
                            Text("${prov.city}, ${prov.state} • ${prov.skills.joinToString(", ")}", color = TextMuted, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { onVerifyProvider(prov.id, true) },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenBrand),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(42.dp).testTag("approve_verification_${prov.id}")
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF00210E), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Approve", color = Color(0xFF00210E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Assigned Support Inquiries & Complaints
        item {
            Text("Assigned Inquiries & User Complaints (${assignedComplaints.size})", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        items(assignedComplaints) { comp ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavyCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(comp.reason, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(comp.details, color = TextLight, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { resolvingComplaint = comp },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Handle Ticket", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(onDismiss = { showAddCategoryDialog = false }, onSubmit = { onAddCategory(it); showAddCategoryDialog = false })
    }

    if (showBroadcastDialog) {
        BroadcastDialog(onDismiss = { showBroadcastDialog = false }, onSubmit = { onBroadcastAnnouncement(it); showBroadcastDialog = false })
    }

    resolvingComplaint?.let { comp ->
        ResolveComplaintDialog(
            complaint = comp,
            isCeo = false,
            onDismiss = { resolvingComplaint = null },
            onSubmit = { status, notes ->
                onResolveComplaint(comp.id, status, notes)
                resolvingComplaint = null
            }
        )
    }
}

// =========================================================================
// 4. DIALOGS & HELPER COMPONENTS
// =========================================================================

data class ConfirmationPayload(
    val title: String,
    val message: String,
    val actionLabel: String,
    val isDestructive: Boolean = false,
    val onConfirm: () -> Unit
)

@Composable
fun ConfirmationActionDialog(
    payload: ConfirmationPayload,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (payload.isDestructive) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (payload.isDestructive) ErrorRed else CyanBrand
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(payload.title, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(payload.message, color = TextLight, fontSize = 13.sp, lineHeight = 18.sp)
        },
        confirmButton = {
            Button(
                onClick = {
                    payload.onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (payload.isDestructive) ErrorRed else CyanBrand
                ),
                modifier = Modifier.testTag("confirm_action_dialog_btn")
            ) {
                Text(
                    text = payload.actionLabel,
                    color = if (payload.isDestructive) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextLight)
            }
        }
    )
}

@Composable
fun EditPermissionsDialog(
    adminDoc: AdminRoleDoc,
    onDismiss: () -> Unit,
    onSubmit: (AdminPermissions) -> Unit
) {
    val initial = adminDoc.toDomainPermissions()
    var pManageAdmins by remember { mutableStateOf(initial.canManageAdmins) }
    var pManageRoles by remember { mutableStateOf(initial.canManageRoles) }
    var pCategories by remember { mutableStateOf(initial.canManageCategories) }
    var pAnnouncements by remember { mutableStateOf(initial.canBroadcastAnnouncements) }
    var pApprove by remember { mutableStateOf(initial.canApproveProviders) }
    var pModerate by remember { mutableStateOf(initial.canModerateContent) }
    var pComplaints by remember { mutableStateOf(initial.canResolveComplaints) }
    var pLogs by remember { mutableStateOf(initial.canAccessSecurityLogs) }
    var pFinancials by remember { mutableStateOf(initial.canViewFinancialAnalytics) }
    var pDelete by remember { mutableStateOf(initial.canDeletePlatformData) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, CyanBrand),
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Configure Permissions: ${adminDoc.role}",
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = adminDoc.email,
                    color = CyanBrand,
                    fontSize = 11.sp
                )
                HorizontalDivider(color = NavyCardBorder)

                PermissionToggleRow("Manage Admins", pManageAdmins) { pManageAdmins = it }
                PermissionToggleRow("Manage Roles", pManageRoles) { pManageRoles = it }
                PermissionToggleRow("Broadcast Announcements", pAnnouncements) { pAnnouncements = it }
                PermissionToggleRow("Approve Providers", pApprove) { pApprove = it }
                PermissionToggleRow("Moderate Content", pModerate) { pModerate = it }
                PermissionToggleRow("Resolve Complaints", pComplaints) { pComplaints = it }
                PermissionToggleRow("Access Audit Logs", pLogs) { pLogs = it }
                PermissionToggleRow("View Financial Reports", pFinancials) { pFinancials = it }
                PermissionToggleRow("Delete Platform Entities", pDelete) { pDelete = it }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel", color = TextLight) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSubmit(
                                AdminPermissions(
                                    canManageAdmins = pManageAdmins,
                                    canManageRoles = pManageRoles,
                                    canManageCategories = pCategories,
                                    canBroadcastAnnouncements = pAnnouncements,
                                    canApproveProviders = pApprove,
                                    canModerateContent = pModerate,
                                    canResolveComplaints = pComplaints,
                                    canAccessSecurityLogs = pLogs,
                                    canViewFinancialAnalytics = pFinancials,
                                    canDeletePlatformData = pDelete
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                        modifier = Modifier.testTag("save_permissions_btn")
                    ) {
                        Text("Save Permissions", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextLight, fontSize = 12.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = CyanBrand
            )
        )
    }
}

@Composable
fun AdminAccountProvisioningDialog(
    onDismiss: () -> Unit,
    onSubmit: (founderPass: String, ceoPass: String, adminPass: String) -> Unit
) {
    var founderPass by remember { mutableStateOf("") }
    var ceoPass by remember { mutableStateOf("") }
    var adminPass by remember { mutableStateOf("") }
    var isFVis by remember { mutableStateOf(false) }
    var isCVis by remember { mutableStateOf(false) }
    var isAVis by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, CyanBrand),
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = CyanBrand)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Official Account Provisioning",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Initialize the 3 official SkillConnect admin accounts in Firebase Auth with verified UID authorization.",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                val submitProvisioning = {
                    if (founderPass.length < 6 || ceoPass.length < 6 || adminPass.length < 6) {
                        errorMsg = "All 3 passwords must be at least 6 characters."
                    } else {
                        onSubmit(founderPass, ceoPass, adminPass)
                    }
                }

                OutlinedTextField(
                    value = founderPass,
                    onValueChange = { founderPass = it; errorMsg = null },
                    label = { Text("Founder Password (${MarketplaceRepository.OFFICIAL_FOUNDER_EMAIL})") },
                    visualTransformation = if (isFVis) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    trailingIcon = {
                        IconButton(onClick = { isFVis = !isFVis }) {
                            Icon(if (isFVis) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = CyanBrand)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("provision_founder_pass_input")
                )

                OutlinedTextField(
                    value = ceoPass,
                    onValueChange = { ceoPass = it; errorMsg = null },
                    label = { Text("CEO Password (${MarketplaceRepository.OFFICIAL_CEO_EMAIL})") },
                    visualTransformation = if (isCVis) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    trailingIcon = {
                        IconButton(onClick = { isCVis = !isCVis }) {
                            Icon(if (isCVis) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = GreenBrand)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("provision_ceo_pass_input")
                )

                OutlinedTextField(
                    value = adminPass,
                    onValueChange = { adminPass = it; errorMsg = null },
                    label = { Text("Admin Password (${MarketplaceRepository.OFFICIAL_ADMIN_EMAIL})") },
                    visualTransformation = if (isAVis) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitProvisioning() }),
                    trailingIcon = {
                        IconButton(onClick = { isAVis = !isAVis }) {
                            Icon(if (isAVis) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = BlueBrand)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("provision_admin_pass_input")
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = ErrorRed, fontSize = 11.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel", color = TextLight) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { submitProvisioning() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                        modifier = Modifier.testTag("submit_provision_btn")
                    ) {
                        Text("Provision Accounts", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ResolveComplaintDialog(
    complaint: PlatformComplaint,
    isCeo: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (ComplaintStatus, String) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(ComplaintStatus.INVESTIGATING) }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, CyanBrand),
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Resolve Complaint #${complaint.id.takeLast(4)}", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Reason: ${complaint.reason}", color = CyanBrand, fontSize = 12.sp)

                Text("Update Status:", color = TextLight, fontSize = 12.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ComplaintStatusOption("Investigating", selectedStatus == ComplaintStatus.INVESTIGATING) { selectedStatus = ComplaintStatus.INVESTIGATING }
                    if (!isCeo) {
                        ComplaintStatusOption("Escalate to CEO", selectedStatus == ComplaintStatus.ESCALATED_TO_CEO) { selectedStatus = ComplaintStatus.ESCALATED_TO_CEO }
                    }
                    ComplaintStatusOption("Resolved", selectedStatus == ComplaintStatus.RESOLVED) { selectedStatus = ComplaintStatus.RESOLVED }
                    ComplaintStatusOption("Dismissed", selectedStatus == ComplaintStatus.DISMISSED) { selectedStatus = ComplaintStatus.DISMISSED }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Resolution Notes") },
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit(selectedStatus, notes) }),
                    modifier = Modifier.fillMaxWidth().testTag("complaint_notes_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel", color = TextLight) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSubmit(selectedStatus, notes) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                        modifier = Modifier.testTag("submit_complaint_resolution_btn")
                    ) {
                        Text("Submit Resolution", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComplaintStatusOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) CyanBrand.copy(alpha = 0.2f) else NavySurfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = CyanBrand))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = if (isSelected) CyanBrand else TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AuditLogCard(log: AdminAuditLog) {
    val dateStr = remember(log.timestampMillis) {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(log.timestampMillis))
    }
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant),
        border = BorderStroke(1.dp, NavyCardBorder),
        modifier = Modifier.fillMaxWidth().testTag("audit_log_item_${log.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(NavySurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (log.roleType) {
                        AdminRoleType.FOUNDER -> Icons.Default.Shield
                        AdminRoleType.CEO -> Icons.Default.BusinessCenter
                        AdminRoleType.ADMIN -> Icons.Default.Security
                    },
                    contentDescription = null,
                    tint = when (log.roleType) {
                        AdminRoleType.FOUNDER -> CyanBrand
                        AdminRoleType.CEO -> GreenBrand
                        AdminRoleType.ADMIN -> BlueBrand
                    },
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = log.action, color = CyanBrand, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = dateStr, color = TextMuted, fontSize = 9.sp)
                }
                Text(text = log.details, color = TextLight, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(text = "Admin: ${log.adminEmail} • ${log.roleType.name}", color = TextMuted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun PermissionBadge(label: String, hasPermission: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (hasPermission) GreenBrand.copy(alpha = 0.15f) else TextMuted.copy(alpha = 0.15f))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = if (hasPermission) GreenBrand else TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AdminMetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(title, color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onSubmit: (ServiceCategory) -> Unit
) {
    var nameEn by remember { mutableStateOf("") }
    var nameHi by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subcategoriesStr by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, CyanBrand),
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Add Service Category", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                val submitCategoryForm = {
                    if (nameEn.isNotBlank()) {
                        val subs = subcategoriesStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        onSubmit(
                            ServiceCategory(
                                id = "cat_${System.currentTimeMillis()}",
                                nameEn = nameEn,
                                nameHi = nameHi.ifBlank { nameEn },
                                descriptionEn = description,
                                descriptionHi = description,
                                iconName = "BusinessCenter",
                                subcategories = if (subs.isNotEmpty()) subs else listOf("General")
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text("Category Name (English)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nameHi,
                    onValueChange = { nameHi = it },
                    label = { Text("श्रेणी का नाम (Hindi)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short Description") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = subcategoriesStr,
                    onValueChange = { subcategoriesStr = it },
                    label = { Text("Subcategories (comma separated)") },
                    placeholder = { Text("Skill 1, Skill 2, Skill 3") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitCategoryForm() }),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { submitCategoryForm() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Category", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BroadcastDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, CyanBrand),
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Broadcast Platform Announcement", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Announcement Text") },
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (text.isNotBlank()) {
                                onSubmit(text)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSubmit(text)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Broadcast to App", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
