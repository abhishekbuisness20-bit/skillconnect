package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.ui.MarketplaceUiState
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ProviderDashboardScreen(
    uiState: MarketplaceUiState,
    onStatusChange: (requestId: String, newStatus: WorkRequestStatus) -> Unit,
    onToggleAvailability: () -> Unit,
    onOpenChat: (customerId: String, customerName: String) -> Unit,
    onAddService: (ServiceOffering) -> Unit
) {
    // Current provider profile (matches demoProvider or first provider)
    val provider = uiState.allProviders.find { it.userId == uiState.currentUser.id }
        ?: uiState.allProviders.firstOrNull()

    var showAddServiceDialog by remember { mutableStateOf(false) }

    val providerRequests = uiState.workRequests.filter { it.providerId == provider?.id }
    val pendingRequests = providerRequests.filter { it.status == WorkRequestStatus.PENDING }
    val activeRequests = providerRequests.filter { it.status == WorkRequestStatus.ACCEPTED || it.status == WorkRequestStatus.IN_PROGRESS }
    val completedRequests = providerRequests.filter { it.status == WorkRequestStatus.COMPLETED }

    val totalEarnings = completedRequests.sumOf { it.budgetInr } + 48000L

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("provider_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Provider Studio",
                            color = CyanBrand,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = provider?.name ?: "Professional",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = provider?.professionalTitle ?: "Specialist",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    // Availability Switch
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (provider?.isAvailable == true) "Available" else "Busy",
                            color = if (provider?.isAvailable == true) GreenBrand else WarningOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = provider?.isAvailable == true,
                            onCheckedChange = { onToggleAvailability() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00210E),
                                checkedTrackColor = GreenBrand,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = NavySurface
                            ),
                            modifier = Modifier.testTag("provider_availability_toggle")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = NavyCardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Verification Status Card
                if (provider?.isVerified == true) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GreenBrand.copy(alpha = 0.12f))
                            .border(1.dp, GreenBrand.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = GreenBrand, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Verified SkillConnect Specialist",
                                color = GreenBrand,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Your profile is verified by management. You rank higher in search.",
                                color = TextLight,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarningOrange.copy(alpha = 0.12f))
                            .border(1.dp, WarningOrange.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Verification In Progress",
                                color = WarningOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Founder Abhishek Singh & Admins (Abhay, Adarsh) will review your portfolio within 24h.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricBox(
                    label = "Incoming",
                    value = "${pendingRequests.size}",
                    color = WarningOrange,
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    label = "Active Work",
                    value = "${activeRequests.size}",
                    color = CyanBrand,
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    label = "Completed",
                    value = "${completedRequests.size + (provider?.completedProjects ?: 0)}",
                    color = GreenBrand,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Platform Earnings", color = TextMuted, fontSize = 12.sp)
                        IndianRupeeText(amount = totalEarnings, fontSize = 22)
                    }
                    Button(
                        onClick = { showAddServiceDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("add_service_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Service",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Incoming Work Requests Section
        item {
            Text(
                text = "Incoming Client Requests (${pendingRequests.size})",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (pendingRequests.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = "No pending requests at the moment. Keep your portfolio updated!",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(pendingRequests) { req ->
                GlassCard(borderColor = WarningOrange.copy(alpha = 0.5f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(req.title, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Client: ${req.customerName}", color = CyanBrand, fontSize = 12.sp)
                        }
                        StatusBadge(status = req.status)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(req.description, color = TextLight, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IndianRupeeText(amount = req.budgetInr, fontSize = 14)
                        Text("Deadline: ${req.deadline}", color = TextMuted, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onStatusChange(req.id, WorkRequestStatus.REJECTED) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            border = BorderStroke(1.dp, ErrorRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text("Decline", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        Button(
                            onClick = { onStatusChange(req.id, WorkRequestStatus.ACCEPTED) },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenBrand),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp)
                        ) {
                            Text("Accept Project", color = Color(0xFF00210E), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        // Active Projects
        item {
            Text(
                text = "Active Contracts (${activeRequests.size})",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (activeRequests.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = "No ongoing contracts.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(activeRequests) { req ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(req.title, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Client: ${req.customerName}", color = CyanBrand, fontSize = 12.sp)
                        }
                        StatusBadge(status = req.status)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IndianRupeeText(amount = req.budgetInr, fontSize = 14)
                        Button(
                            onClick = { onStatusChange(req.id, WorkRequestStatus.COMPLETED) },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenBrand),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Complete", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showAddServiceDialog && provider != null) {
        AddServiceDialog(
            onDismiss = { showAddServiceDialog = false },
            onSubmit = { newService ->
                onAddService(newService)
                showAddServiceDialog = false
            }
        )
    }
}

@Composable
private fun MetricBox(
    label: String,
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
            Text(label, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
fun AddServiceDialog(
    onDismiss: () -> Unit,
    onSubmit: (ServiceOffering) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("5000") }
    var daysStr by remember { mutableStateOf("3") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.5.dp, CyanBrand),
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Service Offering",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service Title") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = CyanBrand
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Scope / Deliverables") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = CyanBrand
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price (₹)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = GreenBrand
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = daysStr,
                        onValueChange = { daysStr = it },
                        label = { Text("Days") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = CyanBrand
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        val price = priceStr.toLongOrNull() ?: 3000L
                        val days = daysStr.toIntOrNull() ?: 2
                        if (name.isNotBlank()) {
                            onSubmit(
                                ServiceOffering(
                                    id = "serv_${System.currentTimeMillis()}",
                                    name = name,
                                    description = description,
                                    startingPriceInr = price,
                                    deliveryDays = days
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("Publish Service", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
