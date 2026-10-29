package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.FavouriteItem
import com.example.data.model.ServiceListing
import com.example.data.model.ServiceReview
import com.example.data.model.UserProfile
import com.example.ui.theme.SkillBlue
import com.example.ui.theme.SkillBrandGradient
import com.example.ui.theme.SkillCyan
import com.example.ui.theme.SkillErrorRed
import com.example.ui.theme.SkillNavy
import com.example.ui.theme.SkillPurple
import com.example.ui.theme.SkillSuccessGreen
import com.example.ui.theme.SkillWarningAmber
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServiceDetailScreen(
    service: ServiceListing?,
    allServices: List<ServiceListing>,
    providerProfile: UserProfile?,
    reviews: List<ServiceReview>,
    favourites: List<FavouriteItem>,
    onBack: () -> Unit,
    onOpenProviderProfile: (String) -> Unit,
    onOpenServiceDetail: (String) -> Unit,
    onSubmitRequest: (
        service: ServiceListing,
        requirements: String,
        preferredDate: String,
        preferredTimeSlot: String,
        attachmentNote: String,
        requestType: String,
        paymentMethod: String
    ) -> Unit,
    onToggleFavourite: (targetId: String, targetType: String, title: String, subtitle: String, location: String, price: Long) -> Unit,
    onMessageProvider: (providerId: String, providerName: String, serviceTitle: String) -> Unit,
    onReportListing: (targetId: String, targetType: String, reason: String, details: String) -> Unit
) {
    val context = LocalContext.current
    var showRequestModal by remember { mutableStateOf(false) }
    var requestType by remember { mutableStateOf("booking") }
    var showReportDialog by remember { mutableStateOf(false) }

    if (service == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Service listing not found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Back to Marketplace") }
            }
        }
        return
    }

    val isSaved = favourites.any { it.targetId == service.id && it.targetType == "service" }
    val serviceReviews = reviews.filter { it.serviceId == service.id }
    val similarServices = allServices.filter { it.category == service.category && it.id != service.id }.take(3)
    val strictVerified = providerProfile?.isStrictVerifiedBadgeActive ?: service.providerVerified

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("service_detail_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = {
                            onToggleFavourite(
                                service.id,
                                "service",
                                service.title,
                                service.providerName,
                                "${service.city} ${service.locality}".trim(),
                                service.startingPrice
                            )
                        },
                        modifier = Modifier.testTag("service_detail_save_btn")
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save to Favourites",
                            tint = if (isSaved) SkillBlue else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Check out '${service.title}' by ${service.providerName} (${service.city}) on SkillConnect India!"
                                )
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Service"))
                        },
                        modifier = Modifier.testTag("service_detail_share_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Service")
                    }
                    IconButton(
                        onClick = { showReportDialog = true },
                        modifier = Modifier.testTag("service_detail_report_btn")
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = "Report Listing", tint = SkillErrorRed)
                    }
                }
            }
        }

        // Gallery & Header Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            ) {
                Column {
                    Image(
                        painter = painterResource(id = R.drawable.img_provider_banner_1793072728889),
                        contentDescription = service.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(color = SkillBlue.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = service.category,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SkillBlue,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            if (service.subcategory.isNotBlank()) {
                                Surface(color = SkillPurple.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = service.subcategory,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SkillPurple,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = service.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = service.shortSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = when (service.pricingModel) {
                                        "quote" -> "Custom Quote Pricing"
                                        "hourly" -> "₹${service.startingPrice} / hour"
                                        "project" -> "₹${service.startingPrice} / project"
                                        else -> "Fixed Starting Price: ₹${service.startingPrice}"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SkillBlue,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Service Area: ${service.city} ${service.locality}".trim(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (service.reviewCount > 0L) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = SkillWarningAmber)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.1f (%d reviews)", service.averageRating, service.reviewCount),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Primary CTAs: Send Booking Request & Request a Quote
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    requestType = "booking"
                                    showRequestModal = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("send_booking_request_button")
                            ) {
                                Text("Send Booking Request", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    requestType = "quote"
                                    showRequestModal = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("request_quote_button")
                            ) {
                                Text("Request a Quote", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Provider Summary Card
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clickable { onOpenProviderProfile(service.providerId) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(SkillBrandGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = service.providerName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = service.providerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (strictVerified) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified by SkillConnect",
                                    tint = SkillSuccessGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = providerProfile?.verificationBadgeLabel ?: if (strictVerified) "Verified by SkillConnect" else "Verification Pending",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (strictVerified) SkillSuccessGreen else SkillWarningAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = providerProfile?.headline?.ifBlank { "Service Professional in ${service.city}" }
                                ?: "Service Professional in ${service.city}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { onMessageProvider(service.providerId, service.providerName, service.title) }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Message")
                    }
                }
            }
        }

        // Full Service Description, Deliverables, Turnaround & Terms
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Full Service Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(service.description, style = MaterialTheme.typography.bodyMedium)

                    if (service.deliverables.isNotBlank()) {
                        HorizontalDivider()
                        Text("Deliverables & Scope", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(service.deliverables, style = MaterialTheme.typography.bodyMedium)
                    }

                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = SkillBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Turnaround: ${service.turnaround.ifBlank { "As per scope" }}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = SkillPurple, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(service.availability.ifBlank { "Mon–Sat" }, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (service.skills.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            service.skills.forEach { tag ->
                                AssistChip(onClick = {}, label = { Text(tag) })
                            }
                        }
                    }

                    if (service.terms.isNotBlank()) {
                        Text(
                            text = "Terms & Limitations: ${service.terms}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Genuine Customer Reviews
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Genuine Customer Reviews (${serviceReviews.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (serviceReviews.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    ) {
                        Text(
                            text = "No verified completed-job reviews for this listing yet. Reviews are only published after a booking is completed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    serviceReviews.forEach { rev ->
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
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(rev.customerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = SkillWarningAmber, modifier = Modifier.size(16.dp))
                                        Text("${rev.rating}/5", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(rev.comment, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        // Similar Services
        if (similarServices.isNotEmpty()) {
            item {
                Text("Similar Services in ${service.category}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(similarServices, key = { it.id }) { sim ->
                ServiceListingCard(
                    service = sim,
                    isSaved = favourites.any { it.targetId == sim.id && it.targetType == "service" },
                    compact = true,
                    onViewDetails = { onOpenServiceDetail(sim.id) },
                    onViewProvider = { onOpenProviderProfile(sim.providerId) },
                    onToggleSave = {
                        onToggleFavourite(sim.id, "service", sim.title, sim.providerName, sim.city, sim.startingPrice)
                    }
                )
            }
        }
    }

    // Request Booking / Quote Modal with Time Slot & Payment Method (Online UPI/Card/NetBanking + Offline Direct)
    if (showRequestModal) {
        var requirements by remember { mutableStateOf("") }
        var preferredDate by remember { mutableStateOf("Within Next 3 Days") }
        var preferredTimeSlot by remember { mutableStateOf("Morning (9 AM - 12 PM)") }
        var paymentMethod by remember { mutableStateOf("upi") }
        var attachmentNote by remember { mutableStateOf("") }
        var validationError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showRequestModal = false },
            title = {
                Text(
                    text = if (requestType == "quote") "Request a Custom Quote" else "Send Booking Request",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = SkillBlue.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Booking Summary", style = MaterialTheme.typography.labelLarge, color = SkillBlue, fontWeight = FontWeight.Bold)
                            Text("Service: ${service.title}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Text("Provider: ${service.providerName} (${service.city})", style = MaterialTheme.typography.bodySmall)
                            Text("Estimated Amount: ₹${service.startingPrice} (${service.pricingModel})", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    OutlinedTextField(
                        value = requirements,
                        onValueChange = { requirements = it },
                        label = { Text("Project Requirements & Scope *") },
                        placeholder = { Text("Describe what you need, address/locality if on-site, and any specifics...") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("booking_requirements_input")
                    )

                    OutlinedTextField(
                        value = preferredDate,
                        onValueChange = { preferredDate = it },
                        label = { Text("Preferred Date *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("booking_preferred_date_input")
                    )

                    Text("Preferred Time Slot", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "Morning (9 AM - 12 PM)",
                            "Afternoon (12 PM - 4 PM)",
                            "Evening (4 PM - 8 PM)",
                            "Flexible"
                        ).forEach { slot ->
                            FilterChip(
                                selected = preferredTimeSlot == slot,
                                onClick = { preferredTimeSlot = slot },
                                label = { Text(slot, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Text("Preferred Payment Option", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "upi" to "Online UPI (GPay / PhonePe)",
                            "card" to "Card Checkout",
                            "netbanking" to "NetBanking",
                            "cash_offline" to "Offline Direct Settlement"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = paymentMethod == key,
                                onClick = { paymentMethod = key },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = attachmentNote,
                        onValueChange = { attachmentNote = it },
                        label = { Text("Reference Link / Attachment Note (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = SkillWarningAmber, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Note: Submitting this request sets its status to 'Pending'. You receive a unique SC-BKG Reference ID and can complete online or offline settlement in your Bookings tracker.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (validationError != null) {
                        Text(validationError!!, color = SkillErrorRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (requirements.trim().length < 5) {
                            validationError = "Please describe your requirements (at least 5 characters)."
                            return@Button
                        }
                        if (preferredDate.trim().isEmpty()) {
                            validationError = "Please enter your preferred date."
                            return@Button
                        }
                        showRequestModal = false
                        onSubmitRequest(
                            service,
                            requirements,
                            preferredDate,
                            preferredTimeSlot,
                            attachmentNote,
                            requestType,
                            paymentMethod
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                    modifier = Modifier.testTag("confirm_booking_request_btn")
                ) {
                    Text("Submit Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRequestModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showReportDialog) {
        ReportDialog(
            title = "Report Service Listing",
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, details ->
                showReportDialog = false
                onReportListing(service.id, "service", reason, details)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProviderPublicProfileScreen(
    provider: UserProfile?,
    providerServices: List<ServiceListing>,
    reviews: List<ServiceReview>,
    favourites: List<FavouriteItem>,
    onBack: () -> Unit,
    onOpenServiceDetail: (String) -> Unit,
    onMessageProvider: (providerId: String, providerName: String) -> Unit,
    onToggleFavourite: (targetId: String, targetType: String, title: String, subtitle: String, location: String, price: Long) -> Unit,
    onReportProfile: (targetId: String, targetType: String, reason: String, details: String) -> Unit
) {
    var showReportDialog by remember { mutableStateOf(false) }

    val displayProvider = provider ?: UserProfile(
        id = providerServices.firstOrNull()?.providerId.orEmpty(),
        userId = providerServices.firstOrNull()?.providerId.orEmpty(),
        displayName = providerServices.firstOrNull()?.providerName ?: "SkillConnect Provider",
        headline = "Local Service Professional",
        bio = "Offering services on SkillConnect India.",
        role = "provider",
        city = providerServices.firstOrNull()?.city ?: "Bengaluru",
        isVerified = providerServices.firstOrNull()?.providerVerified == true,
        verificationStatus = if (providerServices.firstOrNull()?.providerVerified == true) "approved" else "pending_verification",
        startingPrice = providerServices.minOfOrNull { it.startingPrice } ?: 0L
    )

    val isSaved = favourites.any { it.targetId == displayProvider.userId && it.targetType == "provider" }
    val providerReviews = reviews.filter { it.providerId == displayProvider.userId }
    val strictVerified = displayProvider.isStrictVerifiedBadgeActive
    val memberSince = remember(displayProvider.createdAt) {
        val date = displayProvider.createdAt?.toDate()
        if (date != null) SimpleDateFormat("MMM yyyy", Locale.US).format(date) else "2026"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("provider_public_profile_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back")
                }
                Row {
                    IconButton(
                        onClick = {
                            onToggleFavourite(
                                displayProvider.userId,
                                "provider",
                                displayProvider.displayName,
                                displayProvider.headline,
                                displayProvider.city,
                                displayProvider.startingPrice
                            )
                        }
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save Provider",
                            tint = if (isSaved) SkillBlue else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(Icons.Default.Flag, contentDescription = "Report Profile", tint = SkillErrorRed)
                    }
                }
            }
        }

        // Profile Header Card (PII safe: never exposes private email, home address, or ID documents)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SkillNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
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
                                text = "ID: ${displayProvider.effectivePlatformId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkillCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = if (strictVerified) SkillSuccessGreen.copy(alpha = 0.22f) else SkillWarningAmber.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (strictVerified) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified by SkillConnect",
                                        tint = SkillSuccessGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = displayProvider.verificationBadgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (strictVerified) SkillSuccessGreen else SkillWarningAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(SkillBrandGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayProvider.displayName.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = displayProvider.displayName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                if (strictVerified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified by SkillConnect",
                                        tint = SkillSuccessGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = displayProvider.headline.ifBlank { "Local Professional" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "${displayProvider.city} ${displayProvider.locality}".trim() + " • Member since $memberSince",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = displayProvider.bio.ifBlank { "Service professional registered on SkillConnect India." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    if (displayProvider.skills.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            displayProvider.skills.forEach { skill ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = skill,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Starting Price", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text("₹${displayProvider.startingPrice}", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Experience", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text("${displayProvider.experienceYears} yrs", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Completed Jobs", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text("${displayProvider.completedJobsCount}", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Service Mode", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            val modeLabel = when {
                                displayProvider.remoteAvailable && displayProvider.inPersonAvailable -> "Hybrid"
                                displayProvider.remoteAvailable -> "Online"
                                else -> "On-Site"
                            }
                            Text(modeLabel, style = MaterialTheme.typography.titleSmall, color = SkillCyan, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (displayProvider.languagesSpoken.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Languages: ${displayProvider.languagesSpoken.joinToString(", ")} • Working Hours: ${displayProvider.availability}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    if (displayProvider.publicPhone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Public Contact (Consented): ${displayProvider.publicPhone}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val firstService = providerServices.firstOrNull()
                                if (firstService != null) {
                                    onOpenServiceDetail(firstService.id)
                                } else {
                                    onMessageProvider(displayProvider.userId, displayProvider.displayName)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Request Service")
                        }
                        OutlinedButton(
                            onClick = { onMessageProvider(displayProvider.userId, displayProvider.displayName) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ask for a Quote", color = Color.White)
                        }
                    }
                }
            }
        }

        // Portfolio Showcase if present
        val parsedPortfolio = displayProvider.parsePortfolioProjects()
        if (parsedPortfolio.isNotEmpty()) {
            item {
                Text(
                    text = "Portfolio & Work Samples (${parsedPortfolio.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(parsedPortfolio, key = { it.id }) { proj ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(proj.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (proj.skillsUsed.isNotBlank()) {
                            Text(proj.skillsUsed, style = MaterialTheme.typography.labelSmall, color = SkillBlue)
                        }
                        Text(proj.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Services Offered
        item {
            Text(
                text = "Services Offered (${providerServices.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(providerServices, key = { it.id }) { srv ->
            ServiceListingCard(
                service = srv,
                isSaved = favourites.any { it.targetId == srv.id && it.targetType == "service" },
                onViewDetails = { onOpenServiceDetail(srv.id) },
                onViewProvider = {},
                onToggleSave = {
                    onToggleFavourite(srv.id, "service", srv.title, srv.providerName, srv.city, srv.startingPrice)
                }
            )
        }

        // Genuine Customer Reviews
        item {
            Text(
                text = "Customer Reviews (${providerReviews.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (providerReviews.isEmpty()) {
            item {
                Text(
                    text = "No completed-job reviews yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(providerReviews, key = { it.id }) { rev ->
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
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(rev.customerName, fontWeight = FontWeight.Bold)
                            Text("${rev.rating} ★", color = SkillWarningAmber, fontWeight = FontWeight.Bold)
                        }
                        Text(rev.comment, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        ReportDialog(
            title = "Report Provider Profile",
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, details ->
                showReportDialog = false
                onReportProfile(displayProvider.userId, "provider", reason, details)
            }
        )
    }
}

@Composable
fun ReportDialog(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("Inaccurate pricing or misleading scope") }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Inaccurate pricing or misleading scope",
                    "Spam or duplicate listing",
                    "Impersonation or fake credentials",
                    "Abusive or unsafe behaviour"
                ).forEach { reason ->
                    FilterChip(
                        selected = selectedReason == reason,
                        onClick = { selectedReason = reason },
                        label = { Text(reason) }
                    )
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Additional Details") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason, details) },
                colors = ButtonDefaults.buttonColors(containerColor = SkillErrorRed)
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
