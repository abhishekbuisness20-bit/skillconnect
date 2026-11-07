package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Review
import com.example.model.ServiceProvider
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ProviderDetailScreen(
    provider: ServiceProvider,
    reviews: List<Review>,
    isFavourite: Boolean = false,
    onToggleFavourite: () -> Unit = {},
    onReportProvider: (String) -> Unit = {},
    onBack: () -> Unit,
    onRequestWork: () -> Unit,
    onSendMessage: () -> Unit
) {
    BackHandler(onBack = onBack)

    val providerReviews = reviews.filter { it.providerId == provider.id }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportConfirmation by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Surface(color = NavySurface, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("provider_detail_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextWhite
                            )
                        }
                        Text(
                            text = "Specialist Profile",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleFavourite,
                            modifier = Modifier.testTag("favourite_provider_detail_button")
                        ) {
                            Icon(
                                imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavourite) "Saved Specialist" else "Save Specialist",
                                tint = if (isFavourite) ErrorRed else TextWhite
                            )
                        }
                        IconButton(
                            onClick = { showReportDialog = true },
                            modifier = Modifier.testTag("report_provider_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Report or Support",
                                tint = TextMuted
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = NavySurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onSendMessage,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyanBrand),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("send_inquiry_button")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = CyanBrand)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Inquiry", color = CyanBrand, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = onRequestWork,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenBrand),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(50.dp)
                            .testTag("hire_provider_button")
                    ) {
                        Icon(Icons.Default.Work, contentDescription = null, tint = Color(0xFF00210E))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hire / Request", color = Color(0xFF00210E), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
        },
        containerColor = NavyBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("provider_detail_content"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (reportConfirmation != null) {
                item {
                    Surface(
                        color = GreenBrand.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GreenBrand.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenBrand, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reportConfirmation!!,
                                color = TextWhite,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Header Profile Card
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(NavyBackground)
                                .border(2.dp, CyanBrand, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = provider.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                                color = CyanBrand,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = provider.name,
                                    color = TextWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (provider.isVerified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    VerificationBadge(isVerified = true)
                                }
                            }
                            Text(
                                text = provider.professionalTitle,
                                color = CyanBrand,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${provider.city}, ${provider.state}",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = if (provider.isAvailable) "● Available Now" else "○ Busy",
                                    color = if (provider.isAvailable) GreenBrand else WarningOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = NavyCardBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IndianRupeeText(amount = provider.hourlyRateInr, suffix = "/hr", fontSize = 16)
                            Text("Standard Rate", color = TextMuted, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RatingStars(rating = provider.rating, reviewCount = provider.reviewCount)
                            Text("Customer Rating", color = TextMuted, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${provider.completedProjects}+",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Jobs Done", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }

            // About / Bio
            item {
                GlassCard {
                    Text(
                        text = "About the Specialist",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = provider.bio,
                        color = TextLight,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Languages: ${provider.languages.joinToString(", ")}",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Skills & Expertise
            item {
                GlassCard {
                    Text(
                        text = "Core Skills & Expertise",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        provider.skills.forEach { skill ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NavyBackground)
                                    .border(1.dp, CyanBrand.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(skill, color = CyanBrand, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Services & Fixed Packages
            item {
                Text(
                    text = "Services & Packages",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(provider.services) { service ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = service.name,
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = service.description,
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            IndianRupeeText(amount = service.startingPriceInr, fontSize = 14)
                            Text(
                                text = "${service.deliveryDays} days delivery",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Portfolio Showcases
            item {
                Text(
                    text = "Featured Portfolio",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(provider.portfolio) { item ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyanBrand.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DesignServices, contentDescription = null, tint = CyanBrand)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = item.title,
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.description,
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Customer Reviews
            item {
                Text(
                    text = "Verified Customer Reviews (${providerReviews.size})",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (providerReviews.isEmpty()) {
                item {
                    GlassCard {
                        Text(
                            text = "No reviews yet. Be the first to hire and review!",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(providerReviews) { rev ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = rev.customerName,
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (rev.isVerifiedPurchase) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "✓ Verified Hire",
                                        color = GreenBrand,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Text(
                                text = rev.date,
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        RatingStars(rating = rev.rating.toDouble())
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = rev.comment,
                            color = TextLight,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Trust, Report & Support Section
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Trust, Safety & Support",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Need assistance or want to report a profile concern? Contact ${com.example.model.SkillConnectBranding.OFFICIAL_EMAIL}",
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { showReportDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f))
                        ) {
                            Text("Report", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        var reportReason by remember { mutableStateOf("Inaccurate skill/portfolio information") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Text("Report or Request Support", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Report ${provider.name} to the SkillConnect Trust & Safety team (${com.example.model.SkillConnectBranding.OFFICIAL_EMAIL}):",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        label = { Text("Reason / Details") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = CyanBrand,
                            unfocusedBorderColor = NavyCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReportProvider(reportReason)
                        reportConfirmation = "Report submitted to SkillConnect Trust & Safety (${com.example.model.SkillConnectBranding.OFFICIAL_EMAIL})."
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    modifier = Modifier.testTag("submit_report_button")
                ) {
                    Text("Submit Report", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = NavySurface
        )
    }
}

@Composable
fun CreateWorkRequestDialog(
    provider: ServiceProvider,
    onDismiss: () -> Unit,
    onSubmit: (title: String, description: String, category: String, budget: Long, deadline: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var budgetStr by remember { mutableStateOf((provider.hourlyRateInr * 8).toString()) }
    var deadline by remember { mutableStateOf("7 Days") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.5.dp, CyanBrand),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("create_work_request_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create Work Request",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "Recipient: ${provider.name} (${provider.professionalTitle})",
                    color = CyanBrand,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Title *") },
                    placeholder = { Text("e.g. Landing Page Design") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = CyanBrand,
                        unfocusedBorderColor = NavyCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Work Scope & Details *") },
                    placeholder = { Text("Describe requirements, deliverables...") },
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = CyanBrand,
                        unfocusedBorderColor = NavyCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = budgetStr,
                        onValueChange = { budgetStr = it },
                        label = { Text("Budget (₹ INR) *") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = GreenBrand,
                            unfocusedBorderColor = NavyCardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = deadline,
                        onValueChange = { deadline = it },
                        label = { Text("Deadline") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = CyanBrand,
                            unfocusedBorderColor = NavyCardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = ErrorRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        if (title.isBlank() || description.isBlank()) {
                            errorMessage = "Please enter project title and description."
                            return@Button
                        }
                        val budget = budgetStr.toLongOrNull()
                        if (budget == null || budget <= 0) {
                            errorMessage = "Please enter a valid INR budget."
                            return@Button
                        }
                        onSubmit(title, description, provider.categoryId, budget, deadline)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenBrand),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_request_button")
                ) {
                    Text(
                        text = "Submit Work Request",
                        color = Color(0xFF00210E),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun SubmitReviewDialog(
    providerName: String,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.5.dp, GoldStar),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("submit_review_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Review & Rate Specialist",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Sharing your feedback for $providerName",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { starIndex ->
                        IconButton(onClick = { rating = starIndex }) {
                            Icon(
                                imageVector = if (starIndex <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Star $starIndex",
                                tint = GoldStar,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Written Review *") },
                    placeholder = { Text("Detail the quality of deliverables, timeliness, and communication...") },
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = GoldStar,
                        unfocusedBorderColor = NavyCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error!!, color = ErrorRed, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        if (comment.isBlank()) {
                            error = "Please write a brief review."
                            return@Button
                        }
                        onSubmit(rating, comment)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldStar),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_review_button")
                ) {
                    Text(
                        text = "Submit Verified Review",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
