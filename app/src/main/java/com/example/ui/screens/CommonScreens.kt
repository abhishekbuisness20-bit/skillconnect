package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AppLanguage
import com.example.model.ChatMessage
import com.example.model.ServiceCategory
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.GlassCard
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    currentUserId: String,
    partnerId: String,
    partnerName: String,
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val conversationMessages = messages.filter {
        (it.senderId == currentUserId) || (it.senderId == partnerId)
    }

    LaunchedEffect(conversationMessages.size) {
        if (conversationMessages.isNotEmpty()) {
            listState.animateScrollToItem(conversationMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Surface(color = NavySurface, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NavyBackground)
                            .border(1.5.dp, CyanBrand, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = partnerName.take(2).uppercase(),
                            color = CyanBrand,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = partnerName,
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(GreenBrand)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Online • Direct Chat", color = GreenBrand, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = NavySurface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val submitChatMsg = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText.trim())
                            inputText = ""
                        }
                    }
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type message...", color = TextMuted, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = NavySurfaceVariant,
                            unfocusedContainerColor = NavySurfaceVariant,
                            focusedBorderColor = CyanBrand,
                            unfocusedBorderColor = NavyCardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submitChatMsg() }),
                        modifier = Modifier
                            .weight(1f)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    submitChatMsg()
                                    true
                                } else {
                                    false
                                }
                            }
                            .testTag("chat_input_field")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { submitChatMsg() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CyanBrand)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color(0xFF001F2B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        containerColor = NavyBackground
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "End-to-end encrypted messaging via SkillConnect",
                        color = TextDim,
                        fontSize = 11.sp
                    )
                }
            }

            items(conversationMessages) { msg ->
                val isMe = msg.senderId == currentUserId
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val timeStr = timeFormat.format(Date(msg.timestampMillis))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomStart = if (isMe) 14.dp else 2.dp,
                                    bottomEnd = if (isMe) 2.dp else 14.dp
                                )
                            )
                            .background(if (isMe) CyanBrand else NavySurfaceVariant)
                            .border(
                                1.dp,
                                if (isMe) CyanBrand else NavyCardBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (isMe) Color(0xFF001F2B) else TextWhite,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeStr,
                        color = TextDim,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    user: UserProfile,
    activeRole: UserRole,
    isDarkTheme: Boolean,
    currentLanguage: AppLanguage,
    favouriteProviders: List<com.example.model.ServiceProvider> = emptyList(),
    onProviderClick: (com.example.model.ServiceProvider) -> Unit = {},
    onNavigateFooterRoute: (String) -> Unit = {},
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onSwitchRole: (UserRole) -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToPreviewProfile: () -> Unit,
    onLogout: () -> Unit
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Header with Custom Photo & Cover Support
        item {
            GlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Optional Cover Photo
                    if (user.coverPhotoUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = user.coverPhotoUri,
                                contentDescription = "Cover Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Avatar (Custom Photo or Default Initials)
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.dp, CyanBrand, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (user.profilePhotoUri != null) {
                                AsyncImage(
                                    model = user.profilePhotoUri,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = user.avatarInitials.ifBlank { user.name.take(2).uppercase() },
                                    color = CyanBrand,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.testTag("profile_user_name")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified Member",
                                    tint = GreenBrand,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = user.email,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.testTag("profile_user_email")
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val roleBadgeColor = when (activeRole) {
                                    UserRole.ADMIN -> BlueBrand
                                    UserRole.SERVICE_PROVIDER -> GreenBrand
                                    UserRole.CUSTOMER -> CyanBrand
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(roleBadgeColor.copy(alpha = 0.16f))
                                        .border(1.dp, roleBadgeColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .testTag("profile_user_role")
                                ) {
                                    Text(
                                        text = when (activeRole) {
                                            UserRole.ADMIN -> "ADMIN • Administrator"
                                            UserRole.SERVICE_PROVIDER -> "SERVICE PROVIDER"
                                            UserRole.CUSTOMER -> "CUSTOMER"
                                        },
                                        color = roleBadgeColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Text(
                                    text = user.professionalHeadline.ifBlank {
                                        when (activeRole) {
                                            UserRole.ADMIN -> "Authorized Platform Administrator"
                                            UserRole.SERVICE_PROVIDER -> "Verified Skill Specialist"
                                            UserRole.CUSTOMER -> "Project Creator & Client"
                                        }
                                    },
                                    color = CyanBrand,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${user.city}, ${user.state} • ${user.phone}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Profile Action Buttons: Edit Profile, Customize, Preview Public Profile
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToEditProfile,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("edit_profile_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF001F2B), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Profile", color = Color(0xFF001F2B), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = onNavigateToEditProfile,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("customize_profile_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenBrand),
                            border = BorderStroke(1.dp, GreenBrand),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = GreenBrand, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Customize", color = GreenBrand, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FilledTonalButton(
                        onClick = onNavigateToPreviewProfile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("preview_public_profile_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = CyanBrand, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Preview Public Profile", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        // Professional Summary & Bio
        if (user.bio.isNotBlank()) {
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Professional Bio",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = user.bio,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // Skills & Expertise
        if (user.skills.isNotEmpty()) {
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Core Skills & Expertise",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        OptInFlowRow(skills = user.skills)
                    }
                }
            }
        }

        // Experience & Education
        if (user.experienceYears > 0 || user.education.isNotBlank()) {
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Experience & Credentials",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (user.experienceYears > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Work, contentDescription = null, tint = GreenBrand, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${user.experienceYears}+ Years of Industry Experience", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                            }
                        }
                        if (user.education.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.School, contentDescription = null, tint = CyanBrand, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(user.education, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Account Workspace Mode Switcher
        item {
            Text(
                text = "Account Role & Workspace",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            GlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val workspaceTitle = when (activeRole) {
                        UserRole.ADMIN -> "Admin Management Workspace"
                        UserRole.SERVICE_PROVIDER -> "Provider Studio Workspace"
                        UserRole.CUSTOMER -> "Customer Workspace"
                    }
                    val workspaceSubtitle = when (activeRole) {
                        UserRole.ADMIN -> "Active: Platform governance, verification & security"
                        UserRole.SERVICE_PROVIDER -> "Active: Managing services & client inquiries"
                        UserRole.CUSTOMER -> "Active: Exploring skills & booking specialists"
                    }
                    val workspaceColor = when (activeRole) {
                        UserRole.ADMIN -> BlueBrand
                        UserRole.SERVICE_PROVIDER -> GreenBrand
                        UserRole.CUSTOMER -> CyanBrand
                    }
                    val workspaceBadge = when (activeRole) {
                        UserRole.ADMIN -> "ADMIN"
                        UserRole.SERVICE_PROVIDER -> "STUDIO"
                        UserRole.CUSTOMER -> "CUSTOMER"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = workspaceTitle,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = workspaceSubtitle,
                                color = workspaceColor,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(workspaceColor.copy(alpha = 0.15f))
                                .border(1.dp, workspaceColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = workspaceBadge,
                                color = workspaceColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (user.role == UserRole.ADMIN && activeRole != UserRole.ADMIN) {
                        Button(
                            onClick = { onSwitchRole(UserRole.ADMIN) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("return_to_admin_workspace_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BlueBrand),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Return to Admin Management Hub",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            val targetRole = if (activeRole == UserRole.SERVICE_PROVIDER) UserRole.CUSTOMER else UserRole.SERVICE_PROVIDER
                            onSwitchRole(targetRole)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("switch_workspace_role_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeRole == UserRole.SERVICE_PROVIDER) CyanBrand else GreenBrand
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = Color(0xFF070E1E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (activeRole == UserRole.SERVICE_PROVIDER) "Switch to Customer Workspace" else "Switch to Provider Studio",
                            color = Color(0xFF070E1E),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your open tabs, drafts, and inquiries are preserved across workspace switches.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Settings Options (Preferences, Theme Mode & Regional Settings)
        item {
            Text(
                text = "Preferences & Regional Settings",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            GlassCard {
                // Theme Mode Switch (Functional Light / Dark theme)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = CyanBrand
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Theme Mode / थीम मोड", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (isDarkTheme) "Premium Dark Navy (Active)" else "Pearl Light (Active)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleTheme() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF001F2B),
                            checkedTrackColor = CyanBrand
                        ),
                        modifier = Modifier.testTag("theme_mode_switch")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleLanguage)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = CyanBrand)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Language / भाषा", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Text(if (currentLanguage == AppLanguage.ENGLISH) "English (Default)" else "हिंदी (सक्रिय)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                    Text(
                        text = if (currentLanguage == AppLanguage.ENGLISH) "Change to हिंदी" else "Change to EN",
                        color = CyanBrand,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = CyanBrand)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Push Notifications", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = CyanBrand)
                    )
                }
            }
        }

        // Saved / Favourite Providers Section
        item {
            Text(
                text = "Saved Specialists (${favouriteProviders.size})",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            GlassCard(modifier = Modifier.testTag("saved_providers_section")) {
                if (favouriteProviders.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = CyanBrand, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tap the heart icon on any specialist profile to save them here for quick hiring.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        favouriteProviders.forEach { prov ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onProviderClick(prov) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = prov.name,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (prov.isVerified) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            VerificationBadge(isVerified = true)
                                        }
                                    }
                                    Text(
                                        text = "${prov.professionalTitle} • ${prov.city}",
                                        color = CyanBrand,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "★ ${String.format("%.1f", prov.rating)}",
                                    color = GoldStar,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Platform & About Information
        item {
            Text(
                text = "About SkillConnect",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Website: https://skillconnectofficial.cloud.run",
                        color = CyanBrand,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Official Community: ${com.example.model.SkillConnectBranding.OFFICIAL_COMMUNITY}",
                        color = CyanBrand,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("profile_official_community")
                    )
                    Text(
                        text = "Official Email: ${com.example.model.SkillConnectBranding.OFFICIAL_EMAIL}",
                        color = GreenBrand,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("profile_official_email")
                    )
                    Text(
                        text = "Founder: Abhishek Singh • Admins: Abhay, Adarsh",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "SkillConnect connects skilled Indian professionals with clients and enterprises across Graphic Design, Web Development, Video, Marketing, Writing, and Business Services.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Account Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onLogout,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }

                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Request Account Deletion", color = ErrorRed, fontSize = 12.sp)
                }
            }
        }

        // Restored Professional Footer
        item {
            com.example.ui.components.SkillConnectFooter(
                onNavigateRoute = onNavigateFooterRoute
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Account Deletion Request", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("In accordance with data safety guidelines, your deletion request will be processed within 48 hours.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onLogout() }) {
                    Text("Confirm", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun OptInFlowRow(skills: List<String>) {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        skills.forEach { skill ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyanBrand.copy(alpha = 0.12f))
                    .border(1.dp, CyanBrand.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = skill,
                    color = CyanBrand,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun EditProfileScreen(
    user: UserProfile,
    categories: List<ServiceCategory>,
    onBack: () -> Unit,
    onSave: (UserProfile) -> Unit,
    onPreview: () -> Unit
) {
    BackHandler(onBack = onBack)

    var name by remember { mutableStateOf(user.name) }
    var headline by remember { mutableStateOf(user.professionalHeadline) }
    var city by remember { mutableStateOf(user.city) }
    var state by remember { mutableStateOf(user.state) }
    var phone by remember { mutableStateOf(user.phone) }
    var bio by remember { mutableStateOf(user.bio) }
    var experienceYears by remember { mutableStateOf(user.experienceYears.toString()) }
    var education by remember { mutableStateOf(user.education) }
    var contactPreference by remember { mutableStateOf(user.contactPreference) }

    var profilePhotoUri by remember { mutableStateOf(user.profilePhotoUri) }
    var coverPhotoUri by remember { mutableStateOf(user.coverPhotoUri) }

    val skillsList = remember { mutableStateListOf<String>().apply { addAll(user.skills) } }
    var newSkillInput by remember { mutableStateOf("") }

    val portfolioLinksList = remember { mutableStateListOf<String>().apply { addAll(user.portfolioLinks) } }
    var newLinkInput by remember { mutableStateOf("") }

    var github by remember { mutableStateOf(user.socialGithub) }
    var linkedin by remember { mutableStateOf(user.socialLinkedin) }
    var twitter by remember { mutableStateOf(user.socialTwitter) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            profilePhotoUri = uri.toString()
        }
    }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coverPhotoUri = uri.toString()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("edit_profile_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            text = "Edit & Customize Profile",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val updated = user.copy(
                                name = name.trim(),
                                professionalHeadline = headline.trim(),
                                city = city.trim(),
                                state = state.trim(),
                                phone = phone.trim(),
                                bio = bio.trim(),
                                experienceYears = experienceYears.toIntOrNull() ?: 0,
                                education = education.trim(),
                                contactPreference = contactPreference,
                                profilePhotoUri = profilePhotoUri,
                                coverPhotoUri = coverPhotoUri,
                                skills = skillsList.toList(),
                                portfolioLinks = portfolioLinksList.toList(),
                                socialGithub = github.trim(),
                                socialLinkedin = linkedin.trim(),
                                socialTwitter = twitter.trim()
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_profile_top_button")
                    ) {
                        Text("Save", color = Color(0xFF001F2B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Cover Photo & Profile Avatar Upload Section
            item {
                GlassCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Cover Photo Preview & Upload
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, CyanBrand.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable {
                                    coverPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (coverPhotoUri != null) {
                                AsyncImage(
                                    model = coverPhotoUri,
                                    contentDescription = "Cover Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Surface(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (coverPhotoUri == null) "Add Cover Photo" else "Change Cover Photo",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Avatar Picker
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.dp, CyanBrand, CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profilePhotoUri != null) {
                                AsyncImage(
                                    model = profilePhotoUri,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = user.avatarInitials.ifBlank { name.take(2).uppercase() },
                                    color = CyanBrand,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("upload_profile_photo_button")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = CyanBrand, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload / Replace Profile Photo", color = CyanBrand, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (profilePhotoUri != null) {
                            TextButton(onClick = { profilePhotoUri = null }) {
                                Text("Remove Custom Photo", color = ErrorRed, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Section 1: Basic Information
            item {
                Text("Basic Information", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth().testTag("edit_name_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = headline,
                            onValueChange = { headline = it },
                            label = { Text("Professional Headline") },
                            placeholder = { Text("e.g. Senior Mobile App Architect & UI Designer") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth().testTag("edit_headline_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("City") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f).testTag("edit_city_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = state,
                                onValueChange = { state = it },
                                label = { Text("State") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f).testTag("edit_state_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Mobile Phone") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth().testTag("edit_phone_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Section 2: Bio & Description
            item {
                Text("About & Professional Bio", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Bio / Description") },
                            placeholder = { Text("Introduce your experience, what problems you solve, or what projects you hire for...") },
                            modifier = Modifier.fillMaxWidth().height(120.dp).testTag("edit_bio_input"),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 5
                        )
                    }
                }
            }

            // Section 3: Skills & Expertise Editor
            item {
                Text("Skills & Expertise", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val commitSkillInput = {
                            if (newSkillInput.isNotBlank() && !skillsList.contains(newSkillInput.trim())) {
                                skillsList.add(newSkillInput.trim())
                                newSkillInput = ""
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newSkillInput,
                                onValueChange = { newSkillInput = it },
                                placeholder = { Text("Add a skill (e.g. Jetpack Compose, Figma)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { commitSkillInput() }),
                                modifier = Modifier
                                    .weight(1f)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            commitSkillInput()
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    .testTag("add_skill_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { commitSkillInput() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyanBrand)
                                    .testTag("add_skill_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Skill", tint = Color(0xFF001F2B))
                            }
                        }

                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            skillsList.forEach { skill ->
                                Surface(
                                    color = CyanBrand.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, CyanBrand.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(skill, color = CyanBrand, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = CyanBrand,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { skillsList.remove(skill) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Experience & Education
            item {
                Text("Experience & Credentials", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = experienceYears,
                            onValueChange = { experienceYears = it },
                            label = { Text("Years of Experience") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth().testTag("edit_experience_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = education,
                            onValueChange = { education = it },
                            label = { Text("Education / Certifications") },
                            placeholder = { Text("e.g. Master of Design, NID or IIT Madras") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth().testTag("edit_education_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Section 5: Portfolio Links & Social Handles
            item {
                Text("Portfolio & Social Links", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val commitLinkInput = {
                            if (newLinkInput.isNotBlank() && !portfolioLinksList.contains(newLinkInput.trim())) {
                                portfolioLinksList.add(newLinkInput.trim())
                                newLinkInput = ""
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newLinkInput,
                                onValueChange = { newLinkInput = it },
                                placeholder = { Text("Add portfolio URL (https://...)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { commitLinkInput() }),
                                modifier = Modifier
                                    .weight(1f)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            commitLinkInput()
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    .testTag("add_link_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { commitLinkInput() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GreenBrand)
                                    .testTag("add_link_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Link", tint = Color(0xFF00210E))
                            }
                        }

                        portfolioLinksList.forEach { link ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(link, color = CyanBrand, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { portfolioLinksList.remove(link) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = ErrorRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))

                        OutlinedTextField(
                            value = github,
                            onValueChange = { github = it },
                            label = { Text("GitHub Username / Handle") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = linkedin,
                            onValueChange = { linkedin = it },
                            label = { Text("LinkedIn Profile / Username") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = twitter,
                            onValueChange = { twitter = it },
                            label = { Text("Twitter / X Handle") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Bottom Actions (Save Changes & Preview Public Profile)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val updated = user.copy(
                                name = name.trim(),
                                professionalHeadline = headline.trim(),
                                city = city.trim(),
                                state = state.trim(),
                                phone = phone.trim(),
                                bio = bio.trim(),
                                experienceYears = experienceYears.toIntOrNull() ?: 0,
                                education = education.trim(),
                                contactPreference = contactPreference,
                                profilePhotoUri = profilePhotoUri,
                                coverPhotoUri = coverPhotoUri,
                                skills = skillsList.toList(),
                                portfolioLinks = portfolioLinksList.toList(),
                                socialGithub = github.trim(),
                                socialLinkedin = linkedin.trim(),
                                socialTwitter = twitter.trim()
                            )
                            onSave(updated)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_profile_changes_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color(0xFF001F2B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Changes", color = Color(0xFF001F2B), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = onPreview,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("editor_preview_profile_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenBrand),
                        border = BorderStroke(1.dp, GreenBrand),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = GreenBrand)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Preview Public Profile", fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun PublicProfilePreviewScreen(
    user: UserProfile,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("preview_profile_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            text = "Public Profile Preview",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("preview_edit_shortcut_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF001F2B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", color = Color(0xFF001F2B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    color = CyanBrand.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CyanBrand.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = CyanBrand, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Public View Mode: This is how clients and specialists on SkillConnect see your profile.",
                            color = CyanBrand,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Public Card Header with Cover & Avatar
            item {
                GlassCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (user.coverPhotoUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = user.coverPhotoUri,
                                    contentDescription = "Cover Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(2.5.dp, CyanBrand, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (user.profilePhotoUri != null) {
                                    AsyncImage(
                                        model = user.profilePhotoUri,
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = user.avatarInitials.ifBlank { user.name.take(2).uppercase() },
                                        color = CyanBrand,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.Verified, contentDescription = "Verified", tint = GreenBrand, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = user.professionalHeadline.ifBlank {
                                        if (user.role == UserRole.SERVICE_PROVIDER) "Verified Skill Specialist"
                                        else "Active Marketplace Customer"
                                    },
                                    color = CyanBrand,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "📍 ${user.city}, ${user.state} • India",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Public Stats Pill
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Rating", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = GoldStar, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("4.9", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Experience", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Text("${user.experienceYears}+ Yrs", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Security", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Text("Verified", color = GreenBrand, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Bio
            if (user.bio.isNotBlank()) {
                item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("About Me", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(user.bio, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }

            // Skills
            if (user.skills.isNotEmpty()) {
                item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Verified Skills", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            OptInFlowRow(skills = user.skills)
                        }
                    }
                }
            }

            // Portfolio Showcase
            if (user.portfolioLinks.isNotEmpty()) {
                item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Portfolio & Live Work", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            user.portfolioLinks.forEach { link ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Public, contentDescription = null, tint = CyanBrand, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(link, color = CyanBrand, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            // Contact / Inquiry Action
            item {
                Button(
                    onClick = { /* Public profile interactive button */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenBrand),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF00210E))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (user.role == UserRole.SERVICE_PROVIDER) "Request Work from ${user.name.split(" ").first()}"
                        else "Send Inquiry to ${user.name.split(" ").first()}",
                        color = Color(0xFF00210E),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

