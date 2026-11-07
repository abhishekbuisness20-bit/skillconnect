package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.*
import com.example.ui.MarketplaceUiState
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    uiState: MarketplaceUiState,
    onSearchChange: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onProviderClick: (ServiceProvider) -> Unit,
    onExploreAllClick: () -> Unit,
    onRefreshMarketplace: () -> Unit = {},
    onServiceScopeChange: (ServiceScopeFilter) -> Unit = {},
    onToggleAvailableOnly: () -> Unit = {},
    onToggleVerifiedOnly: () -> Unit = {},
    onSortOptionChange: (ProviderSortOption) -> Unit = {},
    onToggleFavouriteProvider: (String) -> Unit = {},
    onOpenAiAssistant: () -> Unit = {},
    onNavigateRoute: (String) -> Unit = {}
) {
    var isInitialCategoryFetch by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(650L)
        isInitialCategoryFetch = false
    }

    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = uiState.isMarketplaceRefreshing,
        onRefresh = onRefreshMarketplace,
        state = pullRefreshState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("marketplace_feed_pull_to_refresh"),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullRefreshState,
                isRefreshing = uiState.isMarketplaceRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                color = CyanBrand,
                containerColor = NavySurfaceVariant
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("customer_home_screen"),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // 1. Welcome, Search & Discovery Header
            item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = if (uiState.currentLanguage == AppLanguage.HINDI) "नमस्ते, ${uiState.currentUser.name}" else "Welcome, ${uiState.currentUser.name}",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (uiState.currentLanguage == AppLanguage.HINDI)
                        "आपका कौशल। आपका लोकल नेटवर्क।"
                    else
                        SkillConnectBranding.TAGLINE,
                    color = CyanBrand,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Prominent Search Bar: "Search providers or local services"
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            text = if (uiState.currentLanguage == AppLanguage.HINDI)
                                "प्रदाता या लोकल सेवाएं खोजें"
                            else
                                "Search providers or local services",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search providers or local services",
                            tint = CyanBrand
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NavySurfaceVariant,
                        unfocusedContainerColor = NavySurface,
                        focusedBorderColor = CyanBrand,
                        unfocusedBorderColor = NavyCardBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_bar_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Useful Search Suggestions
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_suggestions_row")
                ) {
                    items(uiState.searchSuggestions) { suggestion ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NavySurfaceVariant)
                                .border(1.dp, NavyCardBorder, RoundedCornerShape(8.dp))
                                .clickable { onSearchChange(suggestion) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = suggestion,
                                color = TextLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // All Services vs Local Services + Availability + Verified + Sort Filters
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = uiState.serviceScopeFilter == ServiceScopeFilter.ALL_SERVICES,
                            onClick = { onServiceScopeChange(ServiceScopeFilter.ALL_SERVICES) },
                            label = { Text("All Services", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = NavySurfaceVariant,
                                selectedContainerColor = CyanBrand,
                                selectedLabelColor = Color(0xFF001F2B),
                                labelColor = TextLight
                            ),
                            modifier = Modifier.testTag("filter_all_services")
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.serviceScopeFilter == ServiceScopeFilter.LOCAL_SERVICES,
                            onClick = { onServiceScopeChange(ServiceScopeFilter.LOCAL_SERVICES) },
                            label = {
                                Text(
                                    text = "Local Services (${uiState.selectedCity ?: uiState.currentUser.city})",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = NavySurfaceVariant,
                                selectedContainerColor = GreenBrand,
                                selectedLabelColor = Color(0xFF00210E),
                                selectedLeadingIconColor = Color(0xFF00210E),
                                labelColor = TextLight
                            ),
                            modifier = Modifier.testTag("filter_local_services")
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.verifiedOnlyFilter,
                            onClick = onToggleVerifiedOnly,
                            label = { Text("Verified Only", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                            leadingIcon = {
                                Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = NavySurfaceVariant,
                                selectedContainerColor = GreenBrand,
                                selectedLabelColor = Color(0xFF00210E),
                                selectedLeadingIconColor = Color(0xFF00210E),
                                labelColor = TextLight
                            ),
                            modifier = Modifier.testTag("filter_verified_only")
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.availableOnlyFilter,
                            onClick = onToggleAvailableOnly,
                            label = { Text("Available Now", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = NavySurfaceVariant,
                                selectedContainerColor = CyanBrand,
                                selectedLabelColor = Color(0xFF001F2B),
                                labelColor = TextLight
                            ),
                            modifier = Modifier.testTag("filter_available_only")
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.sortOption == ProviderSortOption.RELEVANCE,
                            onClick = { onSortOptionChange(ProviderSortOption.RELEVANCE) },
                            label = { Text("Sort: Relevance", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = NavySurfaceVariant,
                                selectedContainerColor = BlueBrand,
                                selectedLabelColor = Color.White,
                                labelColor = TextLight
                            ),
                            modifier = Modifier.testTag("sort_by_relevance")
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.sortOption == ProviderSortOption.RATING_HIGH_TO_LOW,
                            onClick = { onSortOptionChange(ProviderSortOption.RATING_HIGH_TO_LOW) },
                            label = { Text("Sort: Top Rated ★", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = NavySurfaceVariant,
                                selectedContainerColor = GoldStar,
                                selectedLabelColor = Color.Black,
                                labelColor = TextLight
                            ),
                            modifier = Modifier.testTag("sort_by_rating")
                        )
                    }
                }
            }
        }

        // 2. SkillConnect Promotional Banner with authentic brand asset
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(CyanBrand.copy(alpha = 0.8f), GreenBrand.copy(alpha = 0.8f))),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(361f / 177f)
                            .background(Color(0xFF000130)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.skillconnect_banner),
                            contentDescription = "SkillConnect Banner",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🇮🇳 Verified Indian Freelance Talent",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "0% Platform Commission",
                            color = GreenBrand,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // 3. Ask Gemini AI Concierge Card
        item {
            GlassCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("home_ask_gemini_card"),
                borderColor = CyanBrand.copy(alpha = 0.6f),
                onClick = onOpenAiAssistant
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CyanBrand.copy(alpha = 0.16f))
                                .border(1.dp, CyanBrand, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanBrand)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Not sure who to hire? Ask Gemini AI",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Get instant recommendations for verified specialists & ₹ INR pricing",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CyanBrand)
                }
            }
        }

        // 4. Recently Viewed Providers (shown when user has viewed at least 1 provider)
        if (uiState.recentlyViewedProviders.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag("recently_viewed_providers_section")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recently Viewed Providers",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.recentlyViewedProviders.size} Viewed",
                            color = CyanBrand,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.recentlyViewedProviders) { prov ->
                            CompactProviderPreviewCard(
                                provider = prov,
                                onClick = { onProviderClick(prov) }
                            )
                        }
                    }
                }
            }
        }

        // 5. Service Categories Section (LazyVerticalGrid)
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (uiState.currentLanguage == AppLanguage.HINDI) "सेवा श्रेणियाँ" else "Professional Skill Sectors",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.currentLanguage == AppLanguage.HINDI)
                                "विशेषज्ञों को खोजने के लिए किसी सेक्टर को चुनें"
                            else
                                "Browse verified Indian specialists by domain",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    TextButton(
                        onClick = onExploreAllClick,
                        modifier = Modifier.testTag("view_all_categories_button")
                    ) {
                        Text(
                            text = if (uiState.currentLanguage == AppLanguage.HINDI) "सभी देखें" else "Explore All",
                            color = CyanBrand,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                ServiceCategoryGrid(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategory,
                    currentLanguage = uiState.currentLanguage,
                    isLoading = uiState.isCategoriesLoading || isInitialCategoryFetch || uiState.categories.isEmpty(),
                    columns = 2,
                    userScrollEnabled = false,
                    onCategoryClick = { category -> onCategoryClick(category.id) }
                )
            }
        }

        // 6. Featured Indian Talent Section
        item {
            Column(modifier = Modifier.padding(top = 22.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (uiState.currentLanguage == AppLanguage.HINDI) "शीर्ष सत्यापित प्रदाता" else "Featured Indian Providers",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = CyanBrand,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Pull down to refresh feed",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${uiState.filteredProviders.size} Available",
                            color = GreenBrand,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onRefreshMarketplace,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("refresh_marketplace_feed_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh marketplace feed",
                                tint = CyanBrand,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        if (uiState.filteredProviders.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching providers found", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Try clearing your search query or switching to All Services.", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(uiState.filteredProviders) { provider ->
                ProviderCard(
                    provider = provider,
                    isFavourite = provider.id in uiState.favouriteProviderIds,
                    onToggleFavourite = { onToggleFavouriteProvider(provider.id) },
                    onClick = { onProviderClick(provider) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // 7. Trending Skills
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Trending Skills in India",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                val skills = listOf(
                    "Figma UI/UX", "Jetpack Compose", "Brand Logo", "4K Reels Editing",
                    "SEO Growth", "Next.js", "Motion Graphics", "AI Integration"
                )

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    skills.forEach { skill ->
                        SkillChip(skill = skill, onSearch = onSearchChange)
                    }
                }
            }
        }

        // 8. Complete Restored SkillConnect Footer
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SkillConnectFooter(onNavigateRoute = onNavigateRoute)
        }
    }
    }
}

@Composable
private fun CompactProviderPreviewCard(
    provider: ServiceProvider,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant),
        border = BorderStroke(1.dp, CyanBrand.copy(alpha = 0.4f)),
        modifier = Modifier
            .width(210.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NavyBackground)
                        .border(1.dp, CyanBrand, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = provider.name.take(2).uppercase(),
                        color = CyanBrand,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = provider.name,
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (provider.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            VerificationBadge(isVerified = true)
                        }
                    }
                    Text(
                        text = provider.city,
                        color = TextMuted,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = provider.professionalTitle,
                color = TextLight,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IndianRupeeText(amount = provider.hourlyRateInr, suffix = "/hr", fontSize = 12)
                RatingStars(rating = provider.rating, starSize = 12)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceExploreScreen(
    uiState: MarketplaceUiState,
    onSearchChange: (String) -> Unit,
    onCategoryFilter: (String?) -> Unit,
    onCityFilter: (String?) -> Unit,
    onProviderClick: (ServiceProvider) -> Unit,
    onRefreshMarketplace: () -> Unit = {},
    onServiceScopeChange: (ServiceScopeFilter) -> Unit = {},
    onToggleAvailableOnly: () -> Unit = {},
    onToggleVerifiedOnly: () -> Unit = {},
    onSortOptionChange: (ProviderSortOption) -> Unit = {},
    onToggleFavouriteProvider: (String) -> Unit = {},
    onClearAllFilters: () -> Unit = {},
    onNavigateRoute: (String) -> Unit = {}
) {
    val indianCities = listOf("All", "Bengaluru", "Mumbai", "Delhi-NCR", "Hyderabad", "Pune", "Chennai")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("marketplace_explore_screen")
    ) {
        // Search and Filters Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavySurface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search providers or local services", color = TextMuted, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanBrand) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = TextMuted)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NavySurfaceVariant,
                    unfocusedContainerColor = NavySurfaceVariant,
                    focusedBorderColor = CyanBrand,
                    unfocusedBorderColor = NavyCardBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Scope, Verification, Availability & Sort Strip
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = uiState.serviceScopeFilter == ServiceScopeFilter.ALL_SERVICES,
                        onClick = { onServiceScopeChange(ServiceScopeFilter.ALL_SERVICES) },
                        label = { Text("All Services", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = NavySurfaceVariant,
                            selectedContainerColor = CyanBrand,
                            selectedLabelColor = Color(0xFF001F2B),
                            labelColor = TextLight
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.serviceScopeFilter == ServiceScopeFilter.LOCAL_SERVICES,
                        onClick = { onServiceScopeChange(ServiceScopeFilter.LOCAL_SERVICES) },
                        label = { Text("Local Services", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = NavySurfaceVariant,
                            selectedContainerColor = GreenBrand,
                            selectedLabelColor = Color(0xFF00210E),
                            labelColor = TextLight
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.verifiedOnlyFilter,
                        onClick = onToggleVerifiedOnly,
                        label = { Text("Verified", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = NavySurfaceVariant,
                            selectedContainerColor = GreenBrand,
                            selectedLabelColor = Color(0xFF00210E),
                            labelColor = TextLight
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.availableOnlyFilter,
                        onClick = onToggleAvailableOnly,
                        label = { Text("Available", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = NavySurfaceVariant,
                            selectedContainerColor = CyanBrand,
                            selectedLabelColor = Color(0xFF001F2B),
                            labelColor = TextLight
                        )
                    )
                }
                items(ProviderSortOption.entries) { opt ->
                    FilterChip(
                        selected = uiState.sortOption == opt,
                        onClick = { onSortOptionChange(opt) },
                        label = { Text("Sort: ${opt.label}", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = NavySurfaceVariant,
                            selectedContainerColor = BlueBrand,
                            selectedLabelColor = Color.White,
                            labelColor = TextLight
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // City Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(indianCities) { city ->
                    val isSelected = if (city == "All") uiState.selectedCity == null else uiState.selectedCity == city
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCityFilter(if (city == "All") null else city) },
                        label = { Text(city, fontSize = 12.sp, maxLines = 1, softWrap = false) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = NavySurfaceVariant,
                            selectedContainerColor = CyanBrand,
                            selectedLabelColor = Color(0xFF001F2B),
                            labelColor = TextLight
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = NavyCardBorder,
                            selectedBorderColor = CyanBrand
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category Filter Chips + Sectors Grid Toggle
            var showCategoryGrid by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCategory == null,
                            onClick = { onCategoryFilter(null) },
                            label = { Text("All Categories", fontSize = 12.sp, maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = NavySurfaceVariant,
                                selectedContainerColor = GreenBrand,
                                selectedLabelColor = Color(0xFF00210E),
                                labelColor = TextLight
                            )
                        )
                    }
                    items(uiState.categories) { cat ->
                        val isSelected = uiState.selectedCategory == cat.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategoryFilter(cat.id) },
                            label = { Text(cat.nameEn, fontSize = 12.sp, maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = NavySurfaceVariant,
                                selectedContainerColor = GreenBrand,
                                selectedLabelColor = Color(0xFF00210E),
                                labelColor = TextLight
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { showCategoryGrid = !showCategoryGrid },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (showCategoryGrid) CyanBrand.copy(alpha = 0.2f) else NavySurfaceVariant)
                        .border(1.dp, if (showCategoryGrid) CyanBrand else NavyCardBorder, RoundedCornerShape(10.dp))
                        .testTag("toggle_category_grid_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Toggle Sector Grid",
                        tint = if (showCategoryGrid) CyanBrand else TextLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (showCategoryGrid) {
                Spacer(modifier = Modifier.height(10.dp))
                ServiceCategoryGrid(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategory,
                    currentLanguage = uiState.currentLanguage,
                    isLoading = uiState.isCategoriesLoading || uiState.categories.isEmpty(),
                    columns = 2,
                    userScrollEnabled = true,
                    contentPadding = PaddingValues(vertical = 4.dp),
                    onCategoryClick = { cat ->
                        onCategoryFilter(cat.id)
                        showCategoryGrid = false
                    }
                )
            }
        }

        // Results with Pull-To-Refresh
        val explorePullState = rememberPullToRefreshState()

        PullToRefreshBox(
            isRefreshing = uiState.isMarketplaceRefreshing,
            onRefresh = onRefreshMarketplace,
            state = explorePullState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("explore_feed_pull_to_refresh"),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = explorePullState,
                    isRefreshing = uiState.isMarketplaceRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = CyanBrand,
                    containerColor = NavySurfaceVariant
                )
            }
        ) {
            if (uiState.filteredProviders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = TextDim,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No providers found",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try adjusting your search terms or filters.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onClearAllFilters,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Reset All Filters", color = Color(0xFF001F2B), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Showing ${uiState.filteredProviders.size} verified specialists • Sorted by ${uiState.sortOption.label}",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onRefreshMarketplace,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("refresh_explore_feed_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh listings",
                                    tint = CyanBrand,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    items(uiState.filteredProviders) { provider ->
                        ProviderCard(
                            provider = provider,
                            isFavourite = provider.id in uiState.favouriteProviderIds,
                            onToggleFavourite = { onToggleFavouriteProvider(provider.id) },
                            onClick = { onProviderClick(provider) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        SkillConnectFooter(onNavigateRoute = onNavigateRoute)
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: ServiceCategory,
    isHindi: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CyanBrand.copy(alpha = 0.2f) else NavySurfaceVariant
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) CyanBrand else NavyCardBorder
        ),
        modifier = Modifier
            .width(135.dp)
            .clickable(onClick = onClick)
            .testTag("category_card_${category.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyanBrand.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (category.iconName) {
                    "Palette" -> Icons.Default.Palette
                    "Code" -> Icons.Default.Code
                    "Videocam" -> Icons.Default.Videocam
                    "TrendingUp" -> Icons.AutoMirrored.Filled.TrendingUp
                    "Edit" -> Icons.Default.Edit
                    else -> Icons.Default.BusinessCenter
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyanBrand,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = if (isHindi) category.nameHi else category.nameEn,
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${category.subcategories.size} skills",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ProviderCard(
    provider: ServiceProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavourite: Boolean = false,
    onToggleFavourite: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant),
        border = BorderStroke(1.dp, NavyCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("provider_card_${provider.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar Initials + Availability dot
                    Box {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(NavyBackground)
                                .border(1.5.dp, CyanBrand, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = provider.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                                color = CyanBrand,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (provider.isAvailable) GreenBrand else TextDim)
                                .border(1.5.dp, NavySurfaceVariant, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = provider.name,
                                color = TextWhite,
                                fontSize = 15.sp,
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
                            color = TextLight,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${provider.city}, ${provider.state}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IndianRupeeText(amount = provider.hourlyRateInr, suffix = "/hr", fontSize = 14)
                        if (onToggleFavourite != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onToggleFavourite,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("favourite_provider_button_${provider.id}")
                            ) {
                                Icon(
                                    imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (isFavourite) "Remove from Favourites" else "Save Provider",
                                    tint = if (isFavourite) ErrorRed else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    RatingStars(rating = provider.rating, reviewCount = provider.reviewCount)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Skills chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                provider.skills.take(3).forEach { skill ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NavyBackground)
                            .border(0.8.dp, CyanBrand.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(skill, color = CyanBrand, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
                if (provider.skills.size > 3) {
                    Text(
                        "+${provider.skills.size - 3} more",
                        color = TextMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${provider.completedProjects} jobs • ${if (provider.isAvailable) "Available Now" else "Busy"}",
                    color = if (provider.isAvailable) GreenBrand else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "View Profile →",
                    color = CyanBrand,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SkillChip(
    skill: String,
    onSearch: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(NavySurfaceVariant)
            .border(1.dp, NavyCardBorder, RoundedCornerShape(8.dp))
            .clickable { onSearch(skill) }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = skill,
            color = TextLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}
