package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNotification
import com.example.data.model.BookingRequest
import com.example.data.model.FavouriteItem
import com.example.data.model.MarketplaceCatalog
import com.example.data.model.PaymentRecord
import com.example.data.model.ServiceListing
import com.example.data.model.ServiceReview
import com.example.data.model.UserProfile
import com.example.ui.AppDestination
import com.example.ui.components.AllIndiaLocationSelectorField
import com.example.ui.components.SkillConnectBrandLogo
import com.example.ui.theme.SkillBlue
import com.example.ui.theme.SkillCyan
import com.example.ui.theme.SkillErrorRed
import com.example.ui.theme.SkillNavy
import com.example.ui.theme.SkillPurple
import com.example.ui.theme.SkillSuccessGreen
import com.example.ui.theme.SkillWarningAmber

@Composable
fun CustomerDashboardScreen(
    profile: UserProfile?,
    fallbackName: String,
    customerBookings: List<BookingRequest>,
    savedItems: List<FavouriteItem>,
    recentlyViewedServices: List<ServiceListing>,
    recommendedServices: List<ServiceListing>,
    notifications: List<AppNotification>,
    onOpenServiceDetail: (String) -> Unit,
    onOpenProviderProfile: (String) -> Unit,
    onCancelBooking: (BookingRequest, String) -> Unit,
    onContactProvider: (BookingRequest) -> Unit,
    onSubmitReview: (BookingRequest, Int, String) -> Unit,
    onPayBooking: (BookingRequest, String) -> Unit = { _, _ -> },
    onRequestRefund: (BookingRequest, String) -> Unit = { _, _ -> },
    onSignOut: () -> Unit = {},
    onNavigate: (AppDestination) -> Unit
) {
    val firstName = (profile?.displayName?.ifBlank { fallbackName } ?: fallbackName)
        .trim()
        .split(" ")
        .firstOrNull()
        ?.ifBlank { "Member" } ?: "Member"

    val totalCount = customerBookings.size
    val pendingCount = customerBookings.count { it.status == "pending" || it.status == "draft" }
    val acceptedCount = customerBookings.count { it.status == "accepted" || it.status == "in_progress" }
    val completedCount = customerBookings.count { it.status == "completed" }

    var reviewTargetBooking by remember { mutableStateOf<BookingRequest?>(null) }
    var cancelTargetBooking by remember { mutableStateOf<BookingRequest?>(null) }
    var paymentTargetBooking by remember { mutableStateOf<BookingRequest?>(null) }
    var refundTargetBooking by remember { mutableStateOf<BookingRequest?>(null) }
    var showCustomerLogoutConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customer_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Customer Header with Unique SC-CUS-XXXXXXXX ID + Direct Logout Option
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SkillNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkillConnectBrandLogo(compact = true, onNavyHeader = true)
                        Surface(
                            color = SkillCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "CUSTOMER ID: ${profile?.effectivePlatformId ?: "SC-CUS-MEMBER"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkillCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Welcome back, $firstName",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customer Profile & Dashboard • Track your service enquiries, accepted bookings, payments, and saved professionals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(AppDestination.EXPLORE) },
                            colors = ButtonDefaults.buttonColors(containerColor = SkillBlue)
                        ) {
                            Text("Explore Services")
                        }
                        OutlinedButton(onClick = { onNavigate(AppDestination.PROVIDER_DIRECTORY) }) {
                            Text("Provider Directory", color = Color.White)
                        }
                        OutlinedButton(onClick = { onNavigate(AppDestination.PROFILE_SETTINGS) }) {
                            Text("Customer Profile", color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Direct Logout Button right below Customer Profile Card
            Button(
                onClick = { showCustomerLogoutConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = SkillErrorRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("customer_dashboard_logout_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Logout from Customer Profile",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4 Genuine Summary Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardStatCard(Modifier.weight(1f), "Total Requests", "$totalCount", SkillBlue)
                    DashboardStatCard(Modifier.weight(1f), "Pending Requests", "$pendingCount", SkillWarningAmber)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardStatCard(Modifier.weight(1f), "Accepted / Active", "$acceptedCount", SkillPurple)
                    DashboardStatCard(Modifier.weight(1f), "Completed Jobs", "$completedCount", SkillSuccessGreen)
                }
            }
        }

        // First-time onboarding / Empty state if no requests yet
        if (customerBookings.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Getting Started on SkillConnect", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "You haven't sent any service requests yet. Browse verified local professionals, compare deliverables, and send your first booking or quote request.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { onNavigate(AppDestination.EXPLORE) },
                            colors = ButtonDefaults.buttonColors(containerColor = SkillBlue)
                        ) {
                            Text("Explore Services")
                        }
                    }
                }
            }
        } else {
            item {
                Text("Recent Booking & Quote Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(customerBookings.take(6), key = { it.id }) { booking ->
                BookingRequestItemCard(
                    booking = booking,
                    isProviderView = false,
                    onContact = { onContactProvider(booking) },
                    onCancel = { cancelTargetBooking = booking },
                    onLeaveReview = { reviewTargetBooking = booking },
                    onPayBooking = { paymentTargetBooking = booking },
                    onRequestRefund = { refundTargetBooking = booking },
                    onProviderAction = { _, _ -> }
                )
            }
        }

        // Saved Providers & Services
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Saved Favourites (${savedItems.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onNavigate(AppDestination.FAVOURITES) }) {
                    Text("Manage All")
                }
            }
        }

        if (recentlyViewedServices.isNotEmpty()) {
            item {
                Text("Recently Viewed Services", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(recentlyViewedServices.take(3), key = { "recent_${it.id}" }) { srv ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable { onOpenServiceDetail(srv.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(srv.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${srv.providerName} • ${srv.city}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("₹${srv.startingPrice}", style = MaterialTheme.typography.titleSmall, color = SkillBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (recommendedServices.isNotEmpty()) {
            item {
                Text("Recommended Services for You", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(recommendedServices.take(3), key = { "rec_${it.id}" }) { srv ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable { onOpenServiceDetail(srv.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(srv.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${srv.category} • ${srv.city}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("View →", color = SkillBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Help & Support Shortcut
        item {
            OutlinedButton(
                onClick = { onNavigate(AppDestination.HELP_AND_LEGAL) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Need Help? Visit Help Centre & Support")
            }
        }
    }

    if (cancelTargetBooking != null) {
        val target = cancelTargetBooking!!
        var cancelReason by remember { mutableStateOf("Schedule changed") }
        AlertDialog(
            onDismissRequest = { cancelTargetBooking = null },
            title = { Text("Cancel Booking Request?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to cancel '${target.serviceTitle}' (${target.effectiveBookingRef})?")
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Reason for cancellation") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        cancelTargetBooking = null
                        onCancelBooking(target, cancelReason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillErrorRed)
                ) {
                    Text("Confirm Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTargetBooking = null }) { Text("Keep Request") }
            }
        )
    }

    if (paymentTargetBooking != null) {
        val target = paymentTargetBooking!!
        var selectedMethod by remember { mutableStateOf(target.paymentMethod.ifBlank { "upi" }) }
        AlertDialog(
            onDismissRequest = { paymentTargetBooking = null },
            title = { Text("Complete Booking Payment", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Booking Ref: ${target.effectiveBookingRef}", style = MaterialTheme.typography.labelMedium, color = SkillBlue)
                    Text("Service: ${target.serviceTitle} • Amount: ₹${target.totalAmountInr.takeIf { it > 0 } ?: 999L}", fontWeight = FontWeight.Bold)
                    listOf(
                        "upi" to "Online UPI (GPay / PhonePe / Paytm)",
                        "card" to "Credit / Debit Card Checkout",
                        "netbanking" to "NetBanking Checkout",
                        "cash_offline" to "Confirm Direct Offline Settlement"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedMethod == key,
                            onClick = { selectedMethod = key },
                            label = { Text(label) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        paymentTargetBooking = null
                        onPayBooking(target, selectedMethod)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen)
                ) {
                    Text("Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentTargetBooking = null }) { Text("Cancel") }
            }
        )
    }

    if (refundTargetBooking != null) {
        val target = refundTargetBooking!!
        var refundReason by remember { mutableStateOf("Service scope could not be fulfilled") }
        AlertDialog(
            onDismissRequest = { refundTargetBooking = null },
            title = { Text("Request Booking Refund", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Submit a refund request for '${target.serviceTitle}' (${target.effectiveBookingRef}):")
                    OutlinedTextField(
                        value = refundReason,
                        onValueChange = { refundReason = it },
                        label = { Text("Reason for Refund Request *") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        refundTargetBooking = null
                        onRequestRefund(target, refundReason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillWarningAmber)
                ) {
                    Text("Submit Refund Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { refundTargetBooking = null }) { Text("Cancel") }
            }
        )
    }

    if (reviewTargetBooking != null) {
        val target = reviewTargetBooking!!
        var rating by remember { mutableStateOf(5) }
        var comment by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { reviewTargetBooking = null },
            title = { Text("Leave a Verified Review", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Service: ${target.serviceTitle} by ${target.providerName}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { star ->
                            FilterChip(
                                selected = rating == star,
                                onClick = { rating = star },
                                label = { Text("$star ★") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Your honest feedback *") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (err != null) {
                        Text(err!!, color = SkillErrorRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (comment.trim().length < 3) {
                            err = "Please write at least 3 characters of feedback."
                            return@Button
                        }
                        reviewTargetBooking = null
                        onSubmitReview(target, rating, comment)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen)
                ) {
                    Text("Publish Review")
                }
            },
            dismissButton = {
                TextButton(onClick = { reviewTargetBooking = null }) { Text("Cancel") }
            }
        )
    }

    if (showCustomerLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showCustomerLogoutConfirm = false },
            title = { Text("Confirm Logout", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to log out of your Customer Profile and return to the initial login page?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCustomerLogoutConfirm = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillErrorRed),
                    modifier = Modifier.testTag("confirm_customer_dashboard_logout_btn")
                ) {
                    Text("Yes, Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomerLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProviderDashboardScreen(
    profile: UserProfile?,
    myServices: List<ServiceListing>,
    providerBookings: List<BookingRequest>,
    reviews: List<ServiceReview>,
    providerPayments: List<PaymentRecord> = emptyList(),
    notifications: List<AppNotification> = emptyList(),
    onAddNewService: () -> Unit,
    onEditService: (ServiceListing) -> Unit,
    onDeleteService: (String) -> Unit,
    onProviderBookingAction: (BookingRequest, newStatus: String, note: String) -> Unit,
    onContactCustomer: (BookingRequest) -> Unit,
    onOpenVerification: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenOnboardingWizard: () -> Unit = {},
    onOpenPublicProfile: () -> Unit = {},
    onRequestPayout: (Long, String) -> Unit = { _, _ -> },
    onSignOut: () -> Unit = {},
    onNavigate: (AppDestination) -> Unit = {}
) {
    val newEnquiries = providerBookings.count { it.status == "pending" }
    val acceptedBookings = providerBookings.count { it.status == "accepted" || it.status == "in_progress" }
    val completedJobs = providerBookings.count { it.status == "completed" }
    val totalRequests = providerBookings.size
    val myReviews = reviews.filter { it.providerId == profile?.userId }

    val hasService = myServices.isNotEmpty()
    val checklist = remember(profile, hasService) {
        (profile ?: UserProfile(role = "provider")).buildCompletionChecklist(hasService)
    }
    val completionPct = remember(profile, hasService) {
        (profile ?: UserProfile(role = "provider")).completionPercentage(hasService)
    }

    var activeSection by remember { mutableStateOf("overview") }
    var deleteServiceConfirm by remember { mutableStateOf<ServiceListing?>(null) }
    var declineBookingTarget by remember { mutableStateOf<BookingRequest?>(null) }
    var payoutAmountText by remember { mutableStateOf("2500") }
    var payoutUpiId by remember { mutableStateOf("provider@upi") }
    var showProviderLogoutConfirm by remember { mutableStateOf(false) }

    // All 19 Provider Workspace Sections
    val providerSections = listOf(
        "overview" to "1. Overview",
        "public_profile" to "2. My Public Profile",
        "verification" to "3. Verification Status",
        "my_services" to "4. My Services (${myServices.size})",
        "add_service" to "5. Add New Service",
        "edit_services" to "6. Edit Services",
        "enquiries" to "7. Customer Enquiries ($newEnquiries)",
        "bookings" to "8. Booking Requests ($totalRequests)",
        "calendar" to "9. Booking Calendar",
        "messages" to "10. Messages",
        "portfolio" to "11. Portfolio Manager",
        "reviews" to "12. Reviews & Ratings (${myReviews.size})",
        "analytics" to "13. Profile Analytics",
        "earnings" to "14. Earnings & Payments",
        "payouts" to "15. Payouts",
        "subscriptions" to "16. Subscription Plans",
        "notifications" to "17. Notifications (${notifications.size})",
        "support" to "18. Help & Support",
        "settings" to "19. Account Settings"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("provider_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Provider Workspace Header & 19-Section Switcher
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
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = SkillCyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "PROVIDER ID: ${profile?.effectivePlatformId ?: "SC-PRO-MEMBER"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkillCyan,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = profile?.displayName?.ifBlank { "Provider Workspace" } ?: "Provider Workspace",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                if (profile?.isStrictVerifiedBadgeActive == true) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = "Blue Tick Verified by SkillConnect",
                                        tint = SkillBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (profile?.isStrictVerifiedBadgeActive == true) {
                                    Surface(
                                        color = SkillBlue,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VerifiedUser,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Verified by SkillConnect",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Badge Status: ${profile?.verificationBadgeLabel ?: "Verification Not Submitted"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SkillWarningAmber
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = onAddNewService,
                            colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                            modifier = Modifier.testTag("provider_add_service_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Service")
                        }
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenOnboardingWizard,
                            colors = ButtonDefaults.buttonColors(containerColor = SkillPurple),
                            modifier = Modifier.testTag("provider_open_wizard_btn")
                        ) {
                            Text("8-Step Onboarding Wizard ($completionPct%)")
                        }
                        OutlinedButton(onClick = onOpenVerification) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verification", color = Color.White)
                        }
                        OutlinedButton(onClick = onOpenSubscriptions) {
                            Text("Plan: ${(profile?.subscriptionPlan ?: "free").uppercase()}", color = Color.White)
                        }
                    }

                    // 19-Section Horizontal Navigation Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        providerSections.forEach { (key, label) ->
                            FilterChip(
                                selected = activeSection == key,
                                onClick = {
                                    when (key) {
                                        "add_service" -> onAddNewService()
                                        "public_profile" -> onOpenPublicProfile()
                                        "verification" -> onOpenVerification()
                                        "messages" -> onNavigate(AppDestination.MESSAGES)
                                        "subscriptions" -> onOpenSubscriptions()
                                        "support" -> onNavigate(AppDestination.HELP_AND_LEGAL)
                                        "settings" -> onNavigate(AppDestination.PROFILE_SETTINGS)
                                        else -> activeSection = key
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        // 7-Point Profile Completion Checklist Card
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Profile Completion Checklist ($completionPct%)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onOpenOnboardingWizard) {
                            Text("Complete in Wizard →")
                        }
                    }
                    LinearProgressIndicator(
                        progress = { completionPct / 100f },
                        color = if (completionPct == 100) SkillSuccessGreen else SkillBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        checklist.forEach { (label, done) ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (done) SkillSuccessGreen else SkillWarningAmber,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Overview Metric Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardStatCard(Modifier.weight(1f), "New Enquiries", "$newEnquiries", SkillWarningAmber)
                    DashboardStatCard(Modifier.weight(1f), "Total Requests", "$totalRequests", SkillBlue)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardStatCard(Modifier.weight(1f), "Accepted Bookings", "$acceptedBookings", SkillPurple)
                    DashboardStatCard(Modifier.weight(1f), "Completed Jobs", "$completedJobs", SkillSuccessGreen)
                }
            }
        }

        when (activeSection) {
            "calendar" -> {
                item {
                    Text("Booking Calendar & Availability Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Working Hours: ${profile?.availability ?: "Mon–Sat, 9:00 AM – 7:00 PM"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val scheduled = providerBookings.filter { it.status in listOf("pending", "accepted", "in_progress") }
                if (scheduled.isEmpty()) {
                    item { Text("No upcoming scheduled jobs on your calendar.") }
                } else {
                    items(scheduled, key = { "cal_${it.id}" }) { bk ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("${bk.preferredDate} • ${bk.preferredTimeSlot}", fontWeight = FontWeight.Bold, color = SkillBlue)
                                Text("${bk.serviceTitle} — Customer: ${bk.customerName}", style = MaterialTheme.typography.titleSmall)
                                Text("Status: ${bk.status.uppercase()} • Ref: ${bk.effectiveBookingRef}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            "portfolio" -> {
                val projects = profile?.parsePortfolioProjects().orEmpty()
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Portfolio Manager (${projects.size}/6)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Button(onClick = onOpenOnboardingWizard) { Text("Add / Edit in Step 5") }
                    }
                }
                if (projects.isEmpty()) {
                    item { Text("No portfolio projects uploaded yet. Use Step 5 of the Onboarding Wizard to attach work samples.") }
                } else {
                    items(projects, key = { it.id }) { proj ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(proj.title, fontWeight = FontWeight.Bold)
                                Text(proj.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            "analytics" -> {
                item {
                    val avgRating = if (myReviews.isNotEmpty()) myReviews.map { it.rating }.average() else 0.0
                    val responseRate = if (totalRequests > 0) ((totalRequests - newEnquiries) * 100) / totalRequests else 100
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Genuine Profile Analytics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("• Profile Completion Score: $completionPct%")
                            Text("• Published Service Listings: ${myServices.count { it.isPublished }}")
                            Text("• Enquiry Response Rate: $responseRate%")
                            Text("• Confirmed Completed Jobs: $completedJobs")
                            Text("• Verified Customer Rating: ${String.format("%.1f", avgRating)} ★ (${myReviews.size} reviews)")
                        }
                    }
                }
            }

            "earnings", "payouts" -> {
                val totalEarned = providerPayments.filter { it.status in listOf("succeeded", "paid_out") && it.paymentType != "payout" }.sumOf { it.amountInr }
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Earnings & Provider Payouts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Settled Net Earnings: ₹$totalEarned", style = MaterialTheme.typography.headlineSmall, color = SkillSuccessGreen, fontWeight = FontWeight.ExtraBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = payoutAmountText,
                                    onValueChange = { payoutAmountText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Payout Amount (₹)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = payoutUpiId,
                                    onValueChange = { payoutUpiId = it },
                                    label = { Text("UPI ID / Bank Ref") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                            Button(
                                onClick = {
                                    val amt = payoutAmountText.toLongOrNull() ?: 0L
                                    onRequestPayout(amt, payoutUpiId)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Request Instant UPI / Bank Payout")
                            }
                        }
                    }
                }
                items(providerPayments, key = { it.id }) { pay ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${pay.notes.ifBlank { pay.paymentType }} (${pay.transactionRef})", fontWeight = FontWeight.Bold)
                                Text("Method: ${pay.method.uppercase()} • Status: ${pay.status.uppercase()}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("₹${pay.amountInr}", fontWeight = FontWeight.Bold, color = SkillBlue)
                        }
                    }
                }
            }

            "notifications" -> {
                item {
                    Text("Provider Alerts & Notifications (${notifications.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(notifications, key = { it.id }) { notif ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(notif.title, fontWeight = FontWeight.Bold)
                            Text(notif.body, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            else -> {
                // Default Overview / Services / Enquiries / Bookings / Reviews
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("My Service Listings (${myServices.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onAddNewService) { Text("+ Create New") }
                    }
                }

                if (myServices.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("No Active Service Listings Yet", fontWeight = FontWeight.Bold)
                                Text(
                                    "Publish your first service with clear deliverables and pricing to start receiving customer enquiries.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(onClick = onAddNewService, colors = ButtonDefaults.buttonColors(containerColor = SkillBlue)) {
                                    Text("Create First Service")
                                }
                            }
                        }
                    }
                } else {
                    items(myServices, key = { it.id }) { srv ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(srv.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${srv.category} • ₹${srv.startingPrice} (${srv.pricingModel}) • ${if (srv.isPublished) "Published" else "Draft"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row {
                                        TextButton(onClick = { onEditService(srv) }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Edit")
                                        }
                                        TextButton(onClick = { deleteServiceConfirm = srv }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SkillErrorRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Customer Enquiries & Booking Schedule
                item {
                    Text("Customer Enquiries & Booking Requests (${providerBookings.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (providerBookings.isEmpty()) {
                    item {
                        Text(
                            "No customer enquiries received yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(providerBookings, key = { it.id }) { booking ->
                        BookingRequestItemCard(
                            booking = booking,
                            isProviderView = true,
                            onContact = { onContactCustomer(booking) },
                            onCancel = {},
                            onLeaveReview = {},
                            onProviderAction = { newStatus, note ->
                                if (newStatus == "rejected" || newStatus == "declined") {
                                    declineBookingTarget = booking
                                } else {
                                    onProviderBookingAction(booking, newStatus, note)
                                }
                            }
                        )
                    }
                }

                // Recent Reviews on Provider's Work
                item {
                    Text("Customer Reviews (${myReviews.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(myReviews.take(5), key = { it.id }) { rev ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(rev.customerName, fontWeight = FontWeight.Bold)
                                Text("${rev.rating} ★", color = SkillWarningAmber, fontWeight = FontWeight.Bold)
                            }
                            Text(rev.comment, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    // Destructive Delete Confirmation Dialog
    if (deleteServiceConfirm != null) {
        val srv = deleteServiceConfirm!!
        AlertDialog(
            onDismissRequest = { deleteServiceConfirm = null },
            title = { Text("Delete Service Listing?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete '${srv.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteServiceConfirm = null
                        onDeleteService(srv.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillErrorRed)
                ) {
                    Text("Delete Listing")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteServiceConfirm = null }) { Text("Cancel") }
            }
        )
    }

    // Destructive Reject Confirmation Dialog
    if (declineBookingTarget != null) {
        val bk = declineBookingTarget!!
        var declineReason by remember { mutableStateOf("Currently booked on requested date") }
        AlertDialog(
            onDismissRequest = { declineBookingTarget = null },
            title = { Text("Reject Customer Request?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rejecting updates this booking request to 'Rejected'. Optionally share a reason with ${bk.customerName}:")
                    OutlinedTextField(
                        value = declineReason,
                        onValueChange = { declineReason = it },
                        label = { Text("Reason for rejecting") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        declineBookingTarget = null
                        onProviderBookingAction(bk, "rejected", declineReason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillErrorRed)
                ) {
                    Text("Confirm Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { declineBookingTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun DashboardStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: Color
) {
    Surface(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accentColor,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookingRequestItemCard(
    booking: BookingRequest,
    isProviderView: Boolean,
    onContact: () -> Unit,
    onCancel: () -> Unit,
    onLeaveReview: () -> Unit,
    onPayBooking: () -> Unit = {},
    onRequestRefund: () -> Unit = {},
    onProviderAction: (newStatus: String, note: String) -> Unit
) {
    val badgeColor = when (booking.status) {
        "completed", "accepted", "closed" -> SkillSuccessGreen
        "pending", "in_progress", "draft", "refund_requested" -> SkillWarningAmber
        "rejected", "declined", "cancelled", "cancelled_by_customer", "cancelled_by_provider", "disputed", "refunded" -> SkillErrorRed
        else -> SkillBlue
    }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.effectiveBookingRef,
                        style = MaterialTheme.typography.labelSmall,
                        color = SkillBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = booking.serviceTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(color = badgeColor.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = booking.status.replace("_", " ").uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = if (isProviderView) {
                    "Customer: ${booking.customerName} • ${booking.preferredDate} (${booking.preferredTimeSlot})"
                } else {
                    "Provider: ${booking.providerName} • ${booking.preferredDate} (${booking.preferredTimeSlot})"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Amount: ₹${booking.totalAmountInr} • Payment: ${booking.paymentMethod.uppercase()} (${booking.paymentStatus.uppercase()})",
                style = MaterialTheme.typography.labelSmall,
                color = if (booking.paymentStatus == "paid") SkillSuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Requirements: ${booking.requirements}",
                style = MaterialTheme.typography.bodySmall
            )

            if (booking.statusNote.isNotBlank()) {
                Text(
                    text = "Status Note: ${booking.statusNote}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SkillBlue
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(onClick = onContact) {
                    Text(if (isProviderView) "Message Customer" else "Message Provider")
                }

                if (isProviderView) {
                    if (booking.status == "pending") {
                        Button(
                            onClick = { onProviderAction("accepted", "Booking accepted by provider") },
                            colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen)
                        ) {
                            Text("Accept")
                        }
                        OutlinedButton(onClick = { onProviderAction("rejected", "Rejected by provider") }) {
                            Text("Reject", color = SkillErrorRed)
                        }
                    } else if (booking.status == "accepted") {
                        Button(
                            onClick = { onProviderAction("in_progress", "Work started") },
                            colors = ButtonDefaults.buttonColors(containerColor = SkillBlue)
                        ) {
                            Text("Start Work")
                        }
                        OutlinedButton(onClick = { onProviderAction("cancelled_by_provider", "Cancelled by provider") }) {
                            Text("Cancel", color = SkillErrorRed)
                        }
                    } else if (booking.status == "in_progress") {
                        Button(
                            onClick = { onProviderAction("completed", "Job marked completed") },
                            colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen)
                        ) {
                            Text("Mark Completed")
                        }
                    }
                } else {
                    if (booking.paymentStatus != "paid" && booking.status in listOf("pending", "accepted", "in_progress")) {
                        Button(
                            onClick = onPayBooking,
                            colors = ButtonDefaults.buttonColors(containerColor = SkillBlue)
                        ) {
                            Text("Pay Now")
                        }
                    }
                    if (booking.paymentStatus == "paid" && booking.status !in listOf("refund_requested", "refunded")) {
                        OutlinedButton(onClick = onRequestRefund) {
                            Text("Request Refund", color = SkillWarningAmber)
                        }
                    }
                    if (booking.status in listOf("draft", "pending", "accepted")) {
                        OutlinedButton(onClick = onCancel) {
                            Text("Cancel", color = SkillErrorRed)
                        }
                    }
                    if (booking.status == "completed" && !booking.hasReviewed) {
                        Button(
                            onClick = onLeaveReview,
                            colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen)
                        ) {
                            Text("Submit Review")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEditServiceScreen(
    existingService: ServiceListing?,
    defaultCity: String,
    defaultLocality: String,
    onCancel: () -> Unit,
    onSaveService: (
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
    ) -> Unit
) {
    var title by remember(existingService) { mutableStateOf(existingService?.title ?: "") }
    var category by remember(existingService) { mutableStateOf(existingService?.category ?: "Graphic Design") }
    var subcategory by remember(existingService) { mutableStateOf(existingService?.subcategory ?: "") }
    var shortSummary by remember(existingService) { mutableStateOf(existingService?.shortSummary ?: "") }
    var description by remember(existingService) { mutableStateOf(existingService?.description ?: "") }
    var skillsText by remember(existingService) { mutableStateOf(existingService?.skills?.joinToString(", ") ?: "") }
    var startingPriceText by remember(existingService) { mutableStateOf((existingService?.startingPrice ?: 1499L).toString()) }
    var pricingModel by remember(existingService) { mutableStateOf(existingService?.pricingModel ?: "fixed") }
    var deliverables by remember(existingService) { mutableStateOf(existingService?.deliverables ?: "") }
    var turnaround by remember(existingService) { mutableStateOf(existingService?.turnaround ?: "3 Days") }
    var city by remember(existingService) { mutableStateOf(existingService?.city ?: defaultCity.ifBlank { "Bengaluru" }) }
    var locality by remember(existingService) { mutableStateOf(existingService?.locality ?: defaultLocality) }
    var availability by remember(existingService) { mutableStateOf(existingService?.availability ?: "Mon–Sat, 9 AM – 7 PM") }
    var terms by remember(existingService) { mutableStateOf(existingService?.terms ?: "") }

    var catExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }
    var isPreviewMode by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("create_edit_service_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingService == null) "Create New Service Listing" else "Edit Service Listing",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = { isPreviewMode = !isPreviewMode }) {
                    Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPreviewMode) "Edit Form" else "Preview")
                }
            }
        }

        if (isPreviewMode) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, SkillBlue, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("LISTING PREVIEW", style = MaterialTheme.typography.labelSmall, color = SkillBlue, fontWeight = FontWeight.Bold)
                        Text(title.ifBlank { "Untitled Service" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("$category • $city $locality", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(shortSummary.ifBlank { "No short summary entered." }, style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider()
                        Text(description.ifBlank { "No full description entered." }, style = MaterialTheme.typography.bodySmall)
                        Text("Deliverables: $deliverables", style = MaterialTheme.typography.bodySmall)
                        Text("Price: ₹${startingPriceText.toLongOrNull() ?: 0L} ($pricingModel) • Turnaround: $turnaround", fontWeight = FontWeight.Bold, color = SkillBlue)
                    }
                }
            }
        } else {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Service Title (3–120 chars) *") },
                    placeholder = { Text("e.g. Professional Brand Identity & Logo Design") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_form_title_input")
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        MarketplaceCatalog.categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nameEn) },
                                onClick = {
                                    category = cat.id
                                    subcategory = cat.subcategories.firstOrNull().orEmpty()
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = subcategory,
                    onValueChange = { subcategory = it },
                    label = { Text("Subcategory") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = shortSummary,
                    onValueChange = { shortSummary = it },
                    label = { Text("Short Summary (5–200 chars) *") },
                    placeholder = { Text("Concise 1–2 sentence summary shown on search cards") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_form_summary_input")
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Full Service Description (10–2000 chars) *") },
                    minLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_form_description_input")
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pricing Model *", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("fixed" to "Fixed Price", "hourly" to "Hourly Rate", "quote" to "Request a Quote").forEach { (key, label) ->
                            FilterChip(
                                selected = pricingModel == key,
                                onClick = { pricingModel = key },
                                label = { Text(label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = startingPriceText,
                        onValueChange = { startingPriceText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Starting Price in INR (₹) *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_form_price_input")
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = skillsText,
                    onValueChange = { skillsText = it },
                    label = { Text("Skills & Tags (comma-separated, max 10)") },
                    placeholder = { Text("e.g. Figma, Vector Logo, Branding") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = deliverables,
                    onValueChange = { deliverables = it },
                    label = { Text("Deliverables & Scope") },
                    placeholder = { Text("What the customer will receive upon completion") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = turnaround,
                        onValueChange = { turnaround = it },
                        label = { Text("Estimated Turnaround") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = availability,
                        onValueChange = { availability = it },
                        label = { Text("Availability") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                AllIndiaLocationSelectorField(
                    city = city,
                    locality = locality,
                    label = "Service Location in India (State, City, Locality & PIN) *",
                    allowAllIndia = true,
                    testTag = "service_all_india_location_selector",
                    onLocationChanged = { _, newCity, newLocality, _ ->
                        city = newCity
                        if (newLocality.isNotBlank() && newLocality != "All Localities") {
                            locality = newLocality
                        }
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = locality,
                    onValueChange = { locality = it },
                    label = { Text("Service Locality / Neighbourhood") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = terms,
                    onValueChange = { terms = it },
                    label = { Text("Optional Terms & Limitations") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (validationError != null) {
            item {
                Text(validationError!!, color = SkillErrorRed, style = MaterialTheme.typography.bodySmall)
            }
        }

        item {
            fun validateAndSubmit(publish: Boolean) {
                if (title.trim().length < 3) {
                    validationError = "Service title must be at least 3 characters."
                    return
                }
                if (shortSummary.trim().length < 5) {
                    validationError = "Short summary must be at least 5 characters."
                    return
                }
                if (description.trim().length < 10) {
                    validationError = "Full description must be at least 10 characters."
                    return
                }
                val parsedPrice = startingPriceText.toLongOrNull() ?: 0L
                val parsedSkills = skillsText.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(10)
                validationError = null
                onSaveService(
                    existingService?.id,
                    title,
                    category,
                    subcategory,
                    shortSummary,
                    description,
                    parsedSkills,
                    parsedPrice,
                    pricingModel,
                    deliverables,
                    turnaround,
                    city.ifBlank { "Bengaluru" },
                    locality,
                    availability,
                    terms,
                    publish
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                OutlinedButton(
                    onClick = { validateAndSubmit(publish = false) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_service_draft_btn")
                ) {
                    Text("Save Draft")
                }
                Button(
                    onClick = { validateAndSubmit(publish = true) },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("publish_service_btn")
                ) {
                    Text("Publish Listing", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
