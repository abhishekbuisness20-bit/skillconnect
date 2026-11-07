package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WorkRequest
import com.example.model.WorkRequestStatus
import com.example.ui.components.*
import com.example.ui.theme.*

private enum class BookingGroupFilter(val label: String) {
    ALL("All Bookings"),
    UPCOMING("Upcoming"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

@Composable
fun CustomerRequestsScreen(
    requests: List<WorkRequest>,
    currentCustomerId: String,
    onOpenChat: (providerId: String, providerName: String) -> Unit,
    onLeaveReview: (requestId: String, providerId: String) -> Unit,
    onCancelBooking: (requestId: String) -> Unit = {},
    onNavigateRoute: (String) -> Unit = {}
) {
    var selectedGroup by remember { mutableStateOf(BookingGroupFilter.ALL) }

    val myRequests = requests.filter { it.customerId == currentCustomerId }.ifEmpty { requests }
    val filteredRequests = when (selectedGroup) {
        BookingGroupFilter.ALL -> myRequests
        BookingGroupFilter.UPCOMING -> myRequests.filter {
            it.status in listOf(
                WorkRequestStatus.PENDING,
                WorkRequestStatus.ACCEPTED,
                WorkRequestStatus.IN_PROGRESS
            )
        }
        BookingGroupFilter.COMPLETED -> myRequests.filter { it.status == WorkRequestStatus.COMPLETED }
        BookingGroupFilter.CANCELLED -> myRequests.filter {
            it.status in listOf(WorkRequestStatus.CANCELLED, WorkRequestStatus.REJECTED)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customer_requests_screen"),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavySurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Bookings & Service Requests (${myRequests.size})",
                    color = TextWhite,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Track upcoming, in-progress, completed and cancelled bookings",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BookingGroupFilter.entries) { group ->
                        val isSelected = selectedGroup == group
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGroup = group },
                            label = {
                                Text(
                                    text = group.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = NavySurfaceVariant,
                                selectedContainerColor = CyanBrand,
                                selectedLabelColor = Color(0xFF001F2B),
                                labelColor = TextLight
                            ),
                            modifier = Modifier.testTag("bookings_filter_${group.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (filteredRequests.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AssignmentLate,
                            contentDescription = null,
                            tint = TextDim,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No ${selectedGroup.label.lowercase()} found",
                            color = TextWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Explore verified local professionals and book a service with 0% commission.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onNavigateRoute("explore") },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Explore Services", color = Color(0xFF001F2B), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(filteredRequests) { req ->
                GlassCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("booking_card_${req.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = req.title,
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Specialist: ${req.providerName} • ${req.category}",
                                color = CyanBrand,
                                fontSize = 12.sp
                            )
                        }
                        StatusBadge(status = req.status)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = req.description,
                        color = TextLight,
                        fontSize = 12.sp,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Booking Status Tracking Stepper
                    BookingStatusTracker(status = req.status)

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = NavyCardBorder)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            IndianRupeeText(amount = req.budgetInr, fontSize = 14)
                            Text("Deadline: ${req.deadline}", color = TextMuted, fontSize = 11.sp)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onOpenChat(req.providerId, req.providerName) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, CyanBrand.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = CyanBrand, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chat", color = CyanBrand, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }

                            if (req.status == WorkRequestStatus.PENDING || req.status == WorkRequestStatus.ACCEPTED) {
                                OutlinedButton(
                                    onClick = { onCancelBooking(req.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .height(40.dp)
                                        .testTag("cancel_booking_${req.id}")
                                ) {
                                    Text("Cancel", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }

                            if (req.status == WorkRequestStatus.COMPLETED) {
                                Button(
                                    onClick = { onLeaveReview(req.id, req.providerId) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldStar),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Review", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
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
private fun BookingStatusTracker(status: WorkRequestStatus) {
    val steps = listOf("Requested", "Accepted", "In Progress", "Completed")
    val activeStepIndex = when (status) {
        WorkRequestStatus.PENDING -> 0
        WorkRequestStatus.ACCEPTED -> 1
        WorkRequestStatus.IN_PROGRESS -> 2
        WorkRequestStatus.COMPLETED -> 3
        WorkRequestStatus.CANCELLED, WorkRequestStatus.REJECTED -> -1
    }

    if (activeStepIndex == -1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(ErrorRed.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Cancel, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Booking ${status.labelEn}",
                color = ErrorRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(NavyBackground)
                .border(1.dp, NavyCardBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { idx, label ->
                val isDone = idx <= activeStepIndex
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isDone) GreenBrand else NavySurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF00210E),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        color = if (isDone) GreenBrand else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
