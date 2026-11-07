package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*
import com.example.ui.MarketplaceUiState
import com.example.ui.components.GlassCard
import com.example.ui.components.SkillConnectFooter
import com.example.ui.theme.*

@Composable
fun GeminiAiAssistantDialog(
    messages: List<AiChatMessage>,
    aiUiState: AiAssistantUiState,
    onSendPrompt: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputPrompt by remember { mutableStateOf("") }
    val quickPrompts = listOf(
        "Best UI/UX designers",
        "Android app developer rates",
        "Verified video editors",
        "SEO & marketing experts"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = NavyBackground,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.5.dp, CyanBrand.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.82f)
                .testTag("gemini_ai_assistant_modal")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(NavySurface, NavySurfaceVariant)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CyanBrand.copy(alpha = 0.18f))
                                .border(1.dp, CyanBrand, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CyanBrand,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Ask Gemini AI",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "SkillConnect Smart Talent Concierge",
                                color = CyanBrand,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_gemini_ai_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close AI Assistant", tint = TextMuted)
                    }
                }

                // Quick Prompt Suggestions
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavySurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickPrompts) { prompt ->
                        AssistChip(
                            onClick = { onSendPrompt(prompt) },
                            label = {
                                Text(
                                    text = prompt,
                                    color = CyanBrand,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(containerColor = NavySurfaceVariant),
                            border = BorderStroke(1.dp, CyanBrand.copy(alpha = 0.35f))
                        )
                    }
                }

                // Fallback / Status Banner if AI service returned an error
                if (aiUiState is AiAssistantUiState.Error) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WarningOrange.copy(alpha = 0.14f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("gemini_ai_fallback_notice"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = aiUiState.message,
                            color = TextWhite,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Messages List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { msg ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (msg.isFromUser) Alignment.End else Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 290.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (msg.isFromUser) CyanBrand
                                        else NavySurfaceVariant
                                    )
                                    .border(
                                        1.dp,
                                        if (msg.isFromUser) CyanBrand else NavyCardBorder,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = if (msg.isFromUser) Color(0xFF001F2B) else TextWhite,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    if (aiUiState is AiAssistantUiState.Streaming) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NavySurfaceVariant)
                                    .border(1.dp, CyanBrand.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = aiUiState.partialText,
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    if (aiUiState is AiAssistantUiState.Loading) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = CyanBrand,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gemini is analyzing SkillConnect providers…",
                                    color = CyanBrand,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavySurface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputPrompt,
                        onValueChange = { inputPrompt = it },
                        placeholder = {
                            Text(
                                text = "Ask about services, rates or providers…",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = NavySurfaceVariant,
                            unfocusedContainerColor = NavySurfaceVariant,
                            focusedBorderColor = CyanBrand,
                            unfocusedBorderColor = NavyCardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gemini_ai_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputPrompt.isNotBlank()) {
                                onSendPrompt(inputPrompt)
                                inputPrompt = ""
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CyanBrand)
                            .testTag("gemini_ai_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Ask Gemini",
                            tint = Color(0xFF001F2B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LocationSelectorDialog(
    selectedCity: String?,
    onSelectCity: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val cities = listOf(
        null to "All India (National Marketplace)",
        "Bengaluru" to "Bengaluru, Karnataka",
        "Mumbai" to "Mumbai, Maharashtra",
        "Delhi-NCR" to "Delhi-NCR",
        "Hyderabad" to "Hyderabad, Telangana",
        "Pune" to "Pune, Maharashtra",
        "Chennai" to "Chennai, Tamil Nadu",
        "Kolkata" to "Kolkata, West Bengal"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = GreenBrand)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Service Location", color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("location_selector_dialog")
            ) {
                Text(
                    text = "Filter verified local professionals or browse across all Indian cities:",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                cities.forEach { (cityKey, displayLabel) ->
                    val isSelected = selectedCity == cityKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CyanBrand.copy(alpha = 0.18f) else NavySurfaceVariant)
                            .border(
                                1.dp,
                                if (isSelected) CyanBrand else NavyCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onSelectCity(cityKey)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = displayLabel,
                            color = if (isSelected) CyanBrand else TextWhite,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyanBrand, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = CyanBrand, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun MessagesAndNotificationsScreen(
    uiState: MarketplaceUiState,
    onOpenChat: (String, String) -> Unit,
    onMarkAllNotificationsRead: () -> Unit,
    onExploreProviders: () -> Unit,
    onNavigateRoute: (String) -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Messages, 1 = Notifications

    // Build unique conversation partners from messages and work requests
    val conversationPartners = remember(uiState.messages, uiState.workRequests, uiState.allProviders) {
        val map = linkedMapOf<String, Pair<String, String>>()
        uiState.workRequests.forEach { req ->
            map[req.providerId] = req.providerName to "Booking: ${req.title} (${req.status.labelEn})"
        }
        uiState.messages.forEach { msg ->
            if (msg.senderId != uiState.currentUser.id) {
                map[msg.senderId] = msg.senderName to msg.text
            }
        }
        if (map.isEmpty()) {
            uiState.allProviders.take(3).forEach { prov ->
                map[prov.id] = prov.name to "${prov.professionalTitle} • Tap to start direct chat"
            }
        }
        map.entries.toList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("messages_and_notifications_screen"),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavySurface)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Messages & Notifications",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Direct customer-provider communication & real-time platform alerts",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedSubTab == 0,
                        onClick = { selectedSubTab = 0 },
                        label = {
                            Text(
                                text = "Direct Chats (${conversationPartners.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = NavySurfaceVariant,
                            selectedContainerColor = CyanBrand,
                            selectedLabelColor = Color(0xFF001F2B),
                            selectedLeadingIconColor = Color(0xFF001F2B),
                            labelColor = TextWhite
                        ),
                        modifier = Modifier.testTag("messages_subtab_chats")
                    )

                    FilterChip(
                        selected = selectedSubTab == 1,
                        onClick = { selectedSubTab = 1 },
                        label = {
                            Text(
                                text = "Notifications (${uiState.notifications.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = NavySurfaceVariant,
                            selectedContainerColor = GreenBrand,
                            selectedLabelColor = Color(0xFF00210E),
                            selectedLeadingIconColor = Color(0xFF00210E),
                            labelColor = TextWhite
                        ),
                        modifier = Modifier.testTag("messages_subtab_notifications")
                    )
                }
            }
        }

        if (selectedSubTab == 0) {
            items(conversationPartners) { entry ->
                val partnerId = entry.key
                val (partnerName, previewText) = entry.value
                GlassCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("conversation_item_$partnerId"),
                    onClick = { onOpenChat(partnerId, partnerName) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(NavyBackground)
                                    .border(1.5.dp, CyanBrand, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = partnerName.take(2).uppercase(),
                                    color = CyanBrand,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = partnerName,
                                    color = TextWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = previewText,
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Open Chat",
                            tint = CyanBrand,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Alerts & Updates",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onMarkAllNotificationsRead) {
                        Text("Mark all read", color = CyanBrand, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(uiState.notifications) { notif ->
                GlassCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    borderColor = if (notif.isRead) NavyCardBorder else GreenBrand.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (notif.isRead) CyanBrand else GreenBrand,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = notif.title,
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = notif.category,
                                    color = CyanBrand,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = notif.message,
                                color = TextLight,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            SkillConnectFooter(onNavigateRoute = onNavigateRoute)
        }
    }
}

@Composable
fun FooterDestinationScreen(
    routeKey: String,
    uiState: MarketplaceUiState,
    onBack: () -> Unit,
    onSwitchToProviderRole: () -> Unit,
    onNavigateRoute: (String) -> Unit,
    onSubmitSupportTicket: (String, String) -> Unit
) {
    BackHandler(onBack = onBack)

    var supportSubject by remember { mutableStateOf("") }
    var supportMessage by remember { mutableStateOf("") }
    var ticketSubmitted by remember { mutableStateOf(false) }
    var verificationSubmitted by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf("Starter (0% Commission)") }

    val (screenTitle, screenSubtitle) = when (routeKey) {
        "become_provider" -> "Become a SkillConnect Provider" to "Join India's verified freelance & local services network with 0% commission"
        "provider_verification" -> "Provider Verification & Trust" to "Earn the SkillConnect Verified Badge for higher client conversion"
        "subscriptions" -> "SkillConnect Subscriptions" to "Transparent plans for Indian freelancers, studios, and enterprises"
        "help_centre" -> "Help Centre & Official Support" to "24/7 assistance • ${SkillConnectBranding.OFFICIAL_EMAIL}"
        else -> "Legal Policies & Terms" to "Privacy Policy, Terms of Service & Zero-Commission Guidelines"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("footer_destination_$routeKey"),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(color = NavySurface, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("footer_page_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyanBrand)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(screenTitle, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(screenSubtitle, color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        when (routeKey) {
            "become_provider" -> {
                item {
                    GlassCard(modifier = Modifier.padding(horizontal = 16.dp), borderColor = GreenBrand) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Why Join SkillConnect as a Service Provider?", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("• Keep 100% of your earnings with 0% platform commission.", color = GreenBrand, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("• Get discovered by local clients in your city and across India.", color = TextLight, fontSize = 12.sp)
                            Text("• Direct chat, instant booking requests, and verified portfolio showcase.", color = TextLight, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = onSwitchToProviderRole,
                                colors = ButtonDefaults.buttonColors(containerColor = GreenBrand),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("activate_provider_studio_button")
                            ) {
                                Icon(Icons.Default.Engineering, contentDescription = null, tint = Color(0xFF00210E))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Provider Studio Workspace", color = Color(0xFF00210E), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            "provider_verification" -> {
                item {
                    GlassCard(modifier = Modifier.padding(horizontal = 16.dp), borderColor = CyanBrand) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = GreenBrand)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("3-Step SkillConnect Verification", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("1. Identity & Location Check (Indian City & Contact verification)", color = TextLight, fontSize = 12.sp)
                            Text("2. Portfolio & Past Work Review by SkillConnect Admins", color = TextLight, fontSize = 12.sp)
                            Text("3. Instant Verified Badge activation on all search results", color = TextLight, fontSize = 12.sp)
                            if (verificationSubmitted) {
                                Text(
                                    text = "✓ Your verification review request has been sent to SkillConnect Management (${SkillConnectBranding.OFFICIAL_EMAIL}).",
                                    color = GreenBrand,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Button(
                                    onClick = { verificationSubmitted = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Apply for Verified Badge", color = Color(0xFF001F2B), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            "subscriptions" -> {
                val plans = listOf(
                    Triple("Starter (0% Commission)", "₹0 / month", "Unlimited profile listing, direct chat & local discovery"),
                    Triple("SkillConnect Verified Pro", "₹499 / month", "Priority search placement, Verified badge fast-track & analytics"),
                    Triple("Agency & Enterprise", "₹1,499 / month", "Multi-city featured placement & dedicated account manager")
                )
                items(plans) { (name, price, desc) ->
                    val isCurrent = selectedPlan == name
                    GlassCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        borderColor = if (isCurrent) GreenBrand else NavyCardBorder,
                        onClick = { selectedPlan = name }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(price, color = GreenBrand, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(desc, color = TextMuted, fontSize = 12.sp)
                            }
                            if (isCurrent) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Selected Plan", tint = GreenBrand)
                            }
                        }
                    }
                }
            }

            "help_centre" -> {
                item {
                    GlassCard(modifier = Modifier.padding(horizontal = 16.dp), borderColor = CyanBrand) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Official Support & Community", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Official Email: ${SkillConnectBranding.OFFICIAL_EMAIL}", color = GreenBrand, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Official Community: ${SkillConnectBranding.OFFICIAL_COMMUNITY}", color = CyanBrand, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                            HorizontalDivider(color = NavyCardBorder)

                            Text("Submit a Support or Safety Ticket", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = supportSubject,
                                onValueChange = { supportSubject = it },
                                label = { Text("Subject / Topic") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = supportMessage,
                                onValueChange = { supportMessage = it },
                                label = { Text("Describe how we can help") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (ticketSubmitted) {
                                Text(
                                    text = "✓ Ticket logged! Our team at ${SkillConnectBranding.OFFICIAL_EMAIL} will respond shortly.",
                                    color = GreenBrand,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = {
                                    if (supportSubject.isNotBlank() || supportMessage.isNotBlank()) {
                                        onSubmitSupportTicket(
                                            supportSubject.ifBlank { "General Support" },
                                            supportMessage.ifBlank { "Support inquiry" }
                                        )
                                        ticketSubmitted = true
                                        supportSubject = ""
                                        supportMessage = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Send to SkillConnect Support", color = Color(0xFF001F2B), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            else -> {
                item {
                    GlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("SkillConnect India Legal & Trust Policies", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("1. Zero-Commission Marketplace Policy: SkillConnect connects customers directly with verified providers without charging platform commission.", color = TextLight, fontSize = 12.sp, lineHeight = 17.sp)
                            Text("2. Data Privacy & Security: User profiles and bookings are protected via Firebase Authentication and role-based access rules.", color = TextLight, fontSize = 12.sp, lineHeight = 17.sp)
                            Text("3. Official Grievance & Contact Officer: Reach out to ${SkillConnectBranding.OFFICIAL_EMAIL} or ${SkillConnectBranding.OFFICIAL_COMMUNITY}.", color = CyanBrand, fontSize = 12.sp, lineHeight = 17.sp)
                            Text(SkillConnectBranding.COPYRIGHT, color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            SkillConnectFooter(onNavigateRoute = onNavigateRoute)
        }
    }
}
