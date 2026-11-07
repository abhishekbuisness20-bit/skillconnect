package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppLanguage
import com.example.model.SkillConnectBranding
import com.example.model.UserRole
import com.example.model.WorkRequestStatus
import com.example.ui.theme.*

@Composable
fun SkillConnectTopBar(
    title: String,
    currentRole: UserRole,
    currentLanguage: AppLanguage,
    selectedCity: String? = null,
    unreadNotificationCount: Int = 0,
    onRoleSelect: (UserRole) -> Unit,
    onToggleLanguage: () -> Unit,
    onHamburgerClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
    onAiAssistantClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    showRoleSwitcher: Boolean = false
) {
    Surface(
        color = NavySurface,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .testTag("skillconnect_top_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Top Row: Hamburger Menu + Logo & Brand + Quick Action Icons (Search, AI, Notifications, Language)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onHamburgerClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NavySurfaceVariant)
                            .border(1.dp, NavyCardBorder, RoundedCornerShape(10.dp))
                            .testTag("header_hamburger_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Navigation Menu",
                            tint = CyanBrand,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF000130))
                            .border(1.2.dp, CyanBrand, CircleShape)
                            .padding(2.dp)
                            .testTag("app_logo_badge"),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.skillconnect_logo),
                            contentDescription = "SkillConnect Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Skill",
                                color = TextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "Connect",
                                color = GreenBrand,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            text = title,
                            color = TextMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Search Icon Shortcut
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(NavySurfaceVariant)
                            .testTag("header_search_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Providers or Local Services",
                            tint = CyanBrand,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Ask Gemini AI Shortcut
                    IconButton(
                        onClick = onAiAssistantClick,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(CyanBrand.copy(alpha = 0.25f), GreenBrand.copy(alpha = 0.25f))
                                )
                            )
                            .border(1.dp, CyanBrand.copy(alpha = 0.6f), CircleShape)
                            .testTag("header_ai_assistant_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Ask Gemini AI Assistant",
                            tint = CyanBrand,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Notifications Icon
                    Box {
                        IconButton(
                            onClick = onNotificationClick,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NavySurfaceVariant)
                                .testTag("notification_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (unreadNotificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(GreenBrand)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Second Compact Strip: Location Selector + AI Quick Pill + Language Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Location Selection Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NavySurfaceVariant)
                        .border(1.dp, NavyCardBorder, RoundedCornerShape(8.dp))
                        .clickable(onClick = onLocationClick)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("header_location_selector_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Select Location",
                        tint = GreenBrand,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedCity ?: "All India",
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Ask Gemini AI Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyanBrand.copy(alpha = 0.14f))
                            .border(1.dp, CyanBrand.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .clickable(onClick = onAiAssistantClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyanBrand,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ask Gemini AI",
                            color = CyanBrand,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Language Toggle
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavySurfaceVariant)
                            .border(1.dp, CyanBrand.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable(onClick = onToggleLanguage)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("language_toggle_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Change Language",
                            tint = CyanBrand,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.ENGLISH) "हिंदी" else "EN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanBrand,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            if (showRoleSwitcher) {
                Spacer(modifier = Modifier.height(8.dp))
                DemoRoleSwitcher(
                    currentRole = currentRole,
                    onRoleSelect = onRoleSelect
                )
            }
        }
    }
}

@Composable
fun SkillConnectFooter(
    onNavigateRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF050A17),
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NavyCardBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .testTag("skillconnect_footer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Brand Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF000130))
                        .border(1.5.dp, CyanBrand, CircleShape)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.skillconnect_logo),
                        contentDescription = "SkillConnect Original Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Skill",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "Connect",
                            color = GreenBrand,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Text(
                        text = SkillConnectBranding.TAGLINE,
                        color = CyanBrand,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("footer_tagline")
                    )
                }
            }

            Text(
                text = SkillConnectBranding.DESCRIPTION,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.testTag("footer_description")
            )

            HorizontalDivider(color = NavyCardBorder)

            // 2. Footer Links Grid (All Working Navigation Links)
            Text(
                text = "QUICK LINKS & PLATFORM",
                color = TextLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FooterNavLink(
                        label = "Explore Services",
                        icon = Icons.Default.Explore,
                        onClick = { onNavigateRoute("explore") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("footer_link_explore")
                    )
                    FooterNavLink(
                        label = "Become a Provider",
                        icon = Icons.Default.Engineering,
                        onClick = { onNavigateRoute("become_provider") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("footer_link_become_provider")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FooterNavLink(
                        label = "Provider Verification",
                        icon = Icons.Default.VerifiedUser,
                        onClick = { onNavigateRoute("provider_verification") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("footer_link_provider_verification")
                    )
                    FooterNavLink(
                        label = "Subscriptions",
                        icon = Icons.Default.WorkspacePremium,
                        onClick = { onNavigateRoute("subscriptions") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("footer_link_subscriptions")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FooterNavLink(
                        label = "Help Centre",
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        onClick = { onNavigateRoute("help_centre") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("footer_link_help_centre")
                    )
                    FooterNavLink(
                        label = "Legal Policies",
                        icon = Icons.Default.Gavel,
                        onClick = { onNavigateRoute("legal_policies") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("footer_link_legal_policies")
                    )
                }
            }

            HorizontalDivider(color = NavyCardBorder)

            // 3. Contact Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "OFFICIAL CONTACT & COMMUNITY",
                    color = TextLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onNavigateRoute("help_centre") }
                        .testTag("footer_official_community")
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = CyanBrand,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Official Community: ${SkillConnectBranding.OFFICIAL_COMMUNITY}",
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onNavigateRoute("help_centre") }
                        .testTag("footer_official_email")
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = GreenBrand,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Official Email: ${SkillConnectBranding.OFFICIAL_EMAIL}",
                        color = GreenBrand,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(color = NavyCardBorder)

            // 4. Copyright
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SkillConnectBranding.COPYRIGHT,
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("footer_copyright")
                )
                Text(
                    text = "Made for India 🇮🇳",
                    color = CyanBrand,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun FooterNavLink(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(NavySurfaceVariant.copy(alpha = 0.7f))
            .border(1.dp, NavyCardBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyanBrand,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = TextWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DemoRoleSwitcher(
    currentRole: UserRole,
    onRoleSelect: (UserRole) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyBackground),
        border = BorderStroke(1.dp, NavyCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("demo_role_switcher")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RolePill(
                title = "Customer",
                isSelected = currentRole == UserRole.CUSTOMER,
                activeColor = CyanBrand,
                icon = Icons.Default.Person,
                onClick = { onRoleSelect(UserRole.CUSTOMER) },
                modifier = Modifier.weight(1f)
            )

            RolePill(
                title = "Provider",
                isSelected = currentRole == UserRole.SERVICE_PROVIDER,
                activeColor = GreenBrand,
                icon = Icons.Default.Engineering,
                onClick = { onRoleSelect(UserRole.SERVICE_PROVIDER) },
                modifier = Modifier.weight(1f)
            )

            RolePill(
                title = "Admin",
                isSelected = currentRole == UserRole.ADMIN,
                activeColor = BlueBrand,
                icon = Icons.Default.AdminPanelSettings,
                onClick = { onRoleSelect(UserRole.ADMIN) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RolePill(
    title: String,
    isSelected: Boolean,
    activeColor: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) activeColor.copy(alpha = 0.22f) else Color.Transparent,
        label = "pillBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else TextMuted,
        label = "pillContent"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) activeColor.copy(alpha = 0.6f) else Color.Transparent,
                shape = RoundedCornerShape(9.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = NavyCardBorder,
    containerColor: Color = NavySurfaceVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun StatusBadge(status: WorkRequestStatus) {
    val (bgColor, textColor) = when (status) {
        WorkRequestStatus.PENDING -> WarningOrange.copy(alpha = 0.18f) to WarningOrange
        WorkRequestStatus.ACCEPTED -> CyanBrand.copy(alpha = 0.18f) to CyanBrand
        WorkRequestStatus.IN_PROGRESS -> BlueBrand.copy(alpha = 0.18f) to BlueBrand
        WorkRequestStatus.COMPLETED -> SuccessGreen.copy(alpha = 0.18f) to SuccessGreen
        WorkRequestStatus.REJECTED -> ErrorRed.copy(alpha = 0.18f) to ErrorRed
        WorkRequestStatus.CANCELLED -> TextDim.copy(alpha = 0.25f) to TextMuted
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status.labelEn,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun VerificationBadge(
    isVerified: Boolean,
    modifier: Modifier = Modifier
) {
    if (isVerified) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = false)
                .defaultMinSize(minWidth = 64.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(GreenBrand.copy(alpha = 0.15f))
                .border(0.8.dp, GreenBrand.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
                .padding(horizontal = 6.dp, vertical = 2.5.dp)
                .testTag("verified_badge")
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Verified Provider",
                tint = GreenBrand,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "Verified",
                color = GreenBrand,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
fun RatingStars(
    rating: Double,
    reviewCount: Int? = null,
    starSize: Int = 14
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Rating",
            tint = GoldStar,
            modifier = Modifier.size(starSize.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = String.format("%.1f", rating),
            color = TextWhite,
            fontSize = (starSize - 1).sp,
            fontWeight = FontWeight.Bold
        )
        if (reviewCount != null) {
            Text(
                text = " ($reviewCount)",
                color = TextMuted,
                fontSize = (starSize - 2).sp
            )
        }
    }
}

@Composable
fun IndianRupeeText(
    amount: Long,
    suffix: String = "",
    fontSize: Int = 16,
    color: Color = GreenBrand,
    fontWeight: FontWeight = FontWeight.Bold
) {
    Text(
        text = "₹${formatInr(amount)}$suffix",
        color = color,
        fontSize = fontSize.sp,
        fontWeight = fontWeight
    )
}

fun formatInr(amount: Long): String {
    val str = amount.toString()
    if (str.length <= 3) return str
    val lastThree = str.substring(str.length - 3)
    val rest = str.substring(0, str.length - 3)
    val formattedRest = rest.reversed().chunked(2).joinToString(",").reversed()
    return "$formattedRest,$lastThree"
}
