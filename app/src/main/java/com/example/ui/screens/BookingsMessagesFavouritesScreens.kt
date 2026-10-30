package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNotification
import com.example.data.model.BookingRequest
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.FavouriteItem
import com.example.data.model.ServiceListing
import com.example.ui.theme.SkillBlue
import com.example.ui.theme.SkillBrandGradient
import com.example.ui.theme.SkillErrorRed
import com.example.ui.theme.SkillNavy
import com.example.ui.theme.SkillPurple
import com.example.ui.theme.SkillSuccessGreen
import com.example.ui.theme.SkillWarningAmber
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BookingsAndEnquiriesScreen(
    activeRole: String = "customer",
    customerBookings: List<BookingRequest>,
    providerBookings: List<BookingRequest>,
    onUpdateCustomerBooking: (BookingRequest, newStatus: String, note: String) -> Unit,
    onUpdateProviderBooking: (BookingRequest, newStatus: String, note: String) -> Unit,
    onSubmitReview: (BookingRequest, Int, String) -> Unit,
    onPayBooking: (BookingRequest, String) -> Unit = { _, _ -> },
    onRequestRefund: (BookingRequest, String) -> Unit = { _, _ -> },
    onMessageParty: (BookingRequest, isCustomerView: Boolean) -> Unit,
    onExploreServices: () -> Unit
) {
    var activeTab by remember(activeRole) {
        mutableStateOf(if (activeRole == "provider") "provider" else "customer")
    }
    var statusFilter by remember { mutableStateOf("All") }
    var reviewTarget by remember { mutableStateOf<BookingRequest?>(null) }
    var paymentTarget by remember { mutableStateOf<BookingRequest?>(null) }
    var refundTarget by remember { mutableStateOf<BookingRequest?>(null) }

    val sourceList = if (activeTab == "customer") customerBookings else providerBookings
    val filteredList = sourceList.filter { bk ->
        statusFilter == "All" || bk.status.equals(statusFilter, ignoreCase = true)
    }

    // All 12 booking states supported
    val bookingStateFilters = listOf(
        "All",
        "draft",
        "pending",
        "accepted",
        "rejected",
        "cancelled_by_customer",
        "cancelled_by_provider",
        "in_progress",
        "completed",
        "disputed",
        "refund_requested",
        "refunded",
        "closed"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("bookings_screen")
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Bookings & Enquiry Tracker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = activeTab == "customer",
                        onClick = { activeTab = "customer" },
                        label = { Text("My Requests (${customerBookings.size})") },
                        modifier = Modifier.testTag("bookings_customer_tab")
                    )
                    if (activeRole == "provider" || providerBookings.isNotEmpty()) {
                        FilterChip(
                            selected = activeTab == "provider",
                            onClick = { activeTab = "provider" },
                            label = { Text("Provider Jobs (${providerBookings.size})") },
                            modifier = Modifier.testTag("bookings_provider_tab")
                        )
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bookingStateFilters.forEach { st ->
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = st },
                            label = { Text(st.replace("_", " ").replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (activeTab == "customer") "No Customer Enquiries in This View" else "No Incoming Provider Jobs in This View",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enquiries start as 'Pending' and transition through Accepted, In Progress, Completed, or Refunded states.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onExploreServices,
                        colors = ButtonDefaults.buttonColors(containerColor = SkillBlue)
                    ) {
                        Text("Explore Services")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { it.id }) { booking ->
                    BookingRequestItemCard(
                        booking = booking,
                        isProviderView = activeTab == "provider",
                        onContact = { onMessageParty(booking, activeTab == "customer") },
                        onCancel = { onUpdateCustomerBooking(booking, "cancelled_by_customer", "Cancelled by customer") },
                        onLeaveReview = { reviewTarget = booking },
                        onPayBooking = { paymentTarget = booking },
                        onRequestRefund = { refundTarget = booking },
                        onProviderAction = { newStatus, note ->
                            onUpdateProviderBooking(booking, newStatus, note)
                        }
                    )
                }
            }
        }
    }

    if (paymentTarget != null) {
        val target = paymentTarget!!
        var selectedMethod by remember { mutableStateOf(target.paymentMethod.ifBlank { "upi" }) }
        var upiVpa by remember { mutableStateOf("customer@okicici") }
        AlertDialog(
            onDismissRequest = { paymentTarget = null },
            title = { Text("Complete Booking Payment", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    if (selectedMethod == "upi") {
                        OutlinedTextField(
                            value = upiVpa,
                            onValueChange = { upiVpa = it },
                            label = { Text("UPI ID / VPA") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        paymentTarget = null
                        onPayBooking(target, selectedMethod)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen),
                    modifier = Modifier.testTag("confirm_payment_checkout_btn")
                ) {
                    Text("Pay ₹${target.totalAmountInr.takeIf { it > 0 } ?: 999L}")
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (refundTarget != null) {
        val target = refundTarget!!
        var refundReason by remember { mutableStateOf("Service scope could not be completed") }
        AlertDialog(
            onDismissRequest = { refundTarget = null },
            title = { Text("Request Booking Refund", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Request a refund for '${target.serviceTitle}' (${target.effectiveBookingRef}):")
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
                        refundTarget = null
                        onRequestRefund(target, refundReason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillWarningAmber)
                ) {
                    Text("Submit Refund Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { refundTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (reviewTarget != null) {
        val target = reviewTarget!!
        var rating by remember { mutableStateOf(5) }
        var comment by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { reviewTarget = null },
            title = { Text("Review Completed Service", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${target.serviceTitle} • ${target.providerName}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { s ->
                            FilterChip(
                                selected = rating == s,
                                onClick = { rating = s },
                                label = { Text("$s ★") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Your review *") },
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
                            err = "Please enter at least 3 characters."
                            return@Button
                        }
                        reviewTarget = null
                        onSubmitReview(target, rating, comment)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen)
                ) {
                    Text("Submit Review")
                }
            },
            dismissButton = {
                TextButton(onClick = { reviewTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MessagesAndNotificationsScreen(
    currentUserId: String,
    isAdminUser: Boolean = false,
    userDisplayName: String = "",
    userEmail: String = "",
    userRole: String = "customer",
    userPlatformId: String = "",
    conversations: List<Conversation>,
    activeConversation: Conversation?,
    messages: List<ChatMessage>,
    notifications: List<AppNotification>,
    adminAllConversations: List<Conversation> = emptyList(),
    adminAllMessages: List<ChatMessage> = emptyList(),
    onSelectConversation: (Conversation?) -> Unit,
    onSendMessage: (String) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onMarkAllNotificationsRead: () -> Unit = {},
    onToggleBlockConversation: (String, Boolean) -> Unit = { _, _ -> },
    onReportConversation: (targetId: String, reason: String, details: String) -> Unit,
    onAdminDeleteMessage: (String) -> Unit = {},
    onAdminDeleteConversation: (String) -> Unit = {}
) {
    var activeTab by remember(isAdminUser) {
        mutableStateOf(if (isAdminUser) "messages" else "notifications")
    }
    var messageInput by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }

    // Strict self-only notifications for Customers and Service Providers
    val selfNotifications = remember(notifications, currentUserId) {
        notifications.filter { it.userId == currentUserId || it.userId.isBlank() }
    }

    val effectiveConversations = remember(isAdminUser, adminAllConversations, conversations) {
        if (isAdminUser && adminAllConversations.isNotEmpty()) adminAllConversations else conversations
    }

    if (isAdminUser && activeConversation != null) {
        val threadMessages = remember(messages, adminAllMessages, activeConversation.id) {
            val adminFiltered = adminAllMessages.filter { it.conversationId == activeConversation.id }
                .sortedBy { it.createdAt?.seconds ?: 0L }
            if (adminFiltered.isNotEmpty()) adminFiltered else messages
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("chat_thread_screen")
        ) {
            Surface(
                color = SkillNavy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = { onSelectConversation(null) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Column {
                            Text(
                                text = "${activeConversation.customerName} ↔ ${activeConversation.providerName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (activeConversation.serviceTitle.isNotBlank()) {
                                    "Admin Message Inspector • Topic: ${activeConversation.serviceTitle}"
                                } else {
                                    "Admin Message Inspector (Read & Delete Enabled)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Row {
                        IconButton(
                            onClick = {
                                onAdminDeleteConversation(activeConversation.id)
                            },
                            modifier = Modifier.testTag("admin_delete_conversation_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Entire Conversation",
                                tint = SkillErrorRed
                            )
                        }
                        IconButton(
                            onClick = {
                                onToggleBlockConversation(activeConversation.id, !activeConversation.isBlocked)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = if (activeConversation.isBlocked) "Unblock Conversation" else "Block Conversation",
                                tint = if (activeConversation.isBlocked) SkillErrorRed else Color.White
                            )
                        }
                    }
                }
            }

            if (activeConversation.isBlocked) {
                Surface(color = SkillErrorRed.copy(alpha = 0.14f), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "This conversation is currently blocked. Tap the Block icon in the top bar to unblock.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkillErrorRed,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (threadMessages.isEmpty()) {
                    item {
                        Text(
                            text = "No messages in this conversation thread.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(threadMessages, key = { it.id }) { msg ->
                        val isMe = msg.senderId == currentUserId
                        val timeText = remember(msg.createdAt) {
                            val d = msg.createdAt?.toDate()
                            if (d != null) SimpleDateFormat("hh:mm a", Locale.US).format(d) else "Just now"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (isMe) SkillBlue else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .then(
                                        if (!isMe) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                        else Modifier
                                    )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Sender: ${msg.senderName.ifBlank { msg.senderId.take(8) }}",
                                        color = if (isMe) Color.White.copy(alpha = 0.85f) else SkillBlue,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = msg.text,
                                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = timeText,
                                        color = if (isMe) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onAdminDeleteMessage(msg.id) },
                                modifier = Modifier.testTag("admin_delete_msg_${msg.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Admin Delete Message",
                                    tint = SkillErrorRed
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    // Main Screen: Self Booking & Login Notices (for Customers & Providers) + Admin Message Moderation (for Admins)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("messages_and_notifications_screen")
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAdminUser) {
                            "Admin Message Governance & Notices"
                        } else {
                            "My Booking Notices & Self Login Status"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (activeTab == "notifications" && selfNotifications.any { !it.isRead }) {
                        TextButton(onClick = onMarkAllNotificationsRead) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark All Read")
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = activeTab == "notifications",
                        onClick = { activeTab = "notifications" },
                        label = {
                            val unread = selfNotifications.count { !it.isRead }
                            Text(
                                if (unread > 0) "My Booking & Login Notices ($unread)"
                                else "My Booking & Login Notices (${selfNotifications.size})"
                            )
                        }
                    )
                    FilterChip(
                        selected = activeTab == "messages",
                        onClick = { activeTab = "messages" },
                        label = {
                            Text(
                                if (isAdminUser) "Admin Messages (${effectiveConversations.size})"
                                else "Messages (Admin Only)"
                            )
                        }
                    )
                }
            }
        }

        if (activeTab == "messages") {
            if (!isAdminUser) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, SkillBlue, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = SkillBlue,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Messages Read & Delete Restricted to Admins",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SkillBlue
                            )
                            Text(
                                text = "Only SkillConnect Administrators (Co-Founder, CEO, Founder) can read and delete messages. Customers and Service Providers receive their own Booking Notices and Self Login status in the 'My Booking & Login Notices' tab.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { activeTab = "notifications" },
                                colors = ButtonDefaults.buttonColors(containerColor = SkillBlue)
                            ) {
                                Text("View My Booking & Login Notices")
                            }
                        }
                    }
                }
            } else if (effectiveConversations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No platform conversations recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(effectiveConversations, key = { it.id }) { conv ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                .clickable { onSelectConversation(conv) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(SkillBrandGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = conv.customerName.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${conv.customerName} ↔ ${conv.providerName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (conv.serviceTitle.isNotBlank()) {
                                        Text(conv.serviceTitle, style = MaterialTheme.typography.labelSmall, color = SkillBlue)
                                    }
                                    Text(
                                        text = conv.lastMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                                IconButton(
                                    onClick = { onAdminDeleteConversation(conv.id) },
                                    modifier = Modifier.testTag("admin_delete_conv_${conv.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Conversation",
                                        tint = SkillErrorRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Self-Only Login Status Card (Shown to the signed-in Customer or Service Provider)
                item {
                    Surface(
                        color = SkillNavy,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("self_login_notice_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MY SELF LOGIN STATUS (PRIVATE TO YOU)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkillSuccessGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = SkillSuccessGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "ACTIVE SELF SESSION",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SkillSuccessGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${userDisplayName.ifBlank { "Signed-In Member" }} (${userRole.uppercase()})",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Platform ID: ${userPlatformId.ifBlank { "SC-${userRole.take(3).uppercase()}" }} • Email: ${userEmail.ifBlank { "Verified Google Sign-In" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "Privacy Guarantee: Your login session and booking notices are visible only to you.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    }
                }

                if (selfNotifications.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "No Booking Notices Yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "When a booking request is sent, accepted, updated, or completed, your personal booking notice will appear here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(selfNotifications, key = { it.id }) { notif ->
                        Surface(
                            color = if (!notif.isRead) SkillBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                .clickable {
                                    if (!notif.isRead) onMarkNotificationRead(notif.id)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = if (!notif.isRead) SkillBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(notif.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(notif.body, style = MaterialTheme.typography.bodySmall)
                                }
                                if (!notif.isRead) {
                                    Surface(color = SkillBlue, shape = CircleShape, modifier = Modifier.size(10.dp)) {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavouritesAndCompareScreen(
    favourites: List<FavouriteItem>,
    allServices: List<ServiceListing>,
    comparedServiceIds: Set<String>,
    onToggleCompare: (String) -> Unit,
    onOpenServiceDetail: (String) -> Unit,
    onOpenProviderProfile: (String) -> Unit,
    onRemoveFavourite: (FavouriteItem) -> Unit,
    onExploreServices: () -> Unit
) {
    val comparedServices = allServices.filter { it.id in comparedServiceIds }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("favourites_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Saved Services & Providers",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your private saved shortlist. Select up to 3 services to compare pricing and turnaround side-by-side.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (comparedServices.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, SkillBlue, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CompareArrows, contentDescription = null, tint = SkillBlue)
                            Text(
                                text = "Manual Service Comparison (${comparedServices.size}/3)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            comparedServices.forEach { srv ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(srv.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 2)
                                        Text("By ${srv.providerName}", style = MaterialTheme.typography.bodySmall)
                                        Text("₹${srv.startingPrice} (${srv.pricingModel})", style = MaterialTheme.typography.titleSmall, color = SkillBlue, fontWeight = FontWeight.Bold)
                                        Text("Turnaround: ${srv.turnaround.ifBlank { "Standard" }}", style = MaterialTheme.typography.labelSmall)
                                        Text("City: ${srv.city}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (favourites.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = SkillBlue,
                            modifier = Modifier.size(40.dp)
                        )
                        Text("No Saved Favourites Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Tap the bookmark icon on any service listing or provider profile to save it here across sessions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onExploreServices,
                            colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                            modifier = Modifier.testTag("empty_favourites_explore_cta")
                        ) {
                            Text("Explore Services")
                        }
                    }
                }
            }
        } else {
            items(favourites, key = { it.id }) { fav ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .clickable {
                            if (fav.targetType == "provider") onOpenProviderProfile(fav.targetId)
                            else onOpenServiceDetail(fav.targetId)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fav.targetType.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (fav.targetType == "provider") SkillPurple else SkillBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(fav.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${fav.subtitle} • ${fav.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (fav.price > 0L) {
                                Text("Starts at ₹${fav.price}", style = MaterialTheme.typography.labelMedium, color = SkillBlue, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (fav.targetType == "service") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = fav.targetId in comparedServiceIds,
                                        onCheckedChange = { onToggleCompare(fav.targetId) }
                                    )
                                    Text("Compare", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            IconButton(onClick = { onRemoveFavourite(fav) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Favourite", tint = SkillErrorRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
