package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.FavouriteItem
import com.example.data.model.JobOpportunityItem
import com.example.data.model.MarketplaceCatalog
import com.example.data.model.PlatformSetting
import com.example.data.model.ServiceListing
import com.example.data.model.SkillCourseItem
import com.example.data.model.UserProfile
import com.example.ui.AppDestination
import com.example.ui.ExploreFilterState
import com.example.ui.UiState
import com.example.ui.components.AllIndiaLocationPickerDialog
import com.example.ui.components.AllIndiaLocationSelectorField
import com.example.ui.components.SkillConnectBrandLogo
import com.example.ui.components.SkillConnectOfficialLogoBanner
import com.example.ui.theme.SkillBlue
import com.example.ui.theme.SkillBrandGradient
import com.example.ui.theme.SkillCyan
import com.example.ui.theme.SkillNavy
import com.example.ui.theme.SkillPurple
import com.example.ui.theme.SkillSuccessGreen
import com.example.ui.theme.SkillWarningAmber
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isHindi: Boolean,
    globalSettings: PlatformSetting?,
    publishedServicesState: UiState<List<ServiceListing>>,
    publicProvidersState: UiState<List<UserProfile>>,
    favourites: List<FavouriteItem>,
    selectedCity: String,
    appliedJobIds: List<String> = emptyList(),
    enrolledCourseIds: List<String> = emptyList(),
    onToggleJobApplication: (JobOpportunityItem) -> Unit = {},
    onToggleCourseEnrollment: (SkillCourseItem) -> Unit = {},
    onSearchTriggered: (keyword: String, city: String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onOpenServiceDetail: (String) -> Unit,
    onOpenProviderProfile: (String) -> Unit,
    onNavigate: (AppDestination) -> Unit,
    onToggleFavourite: (targetId: String, targetType: String, title: String, subtitle: String, location: String, price: Long) -> Unit
) {
    var searchKeyword by remember { mutableStateOf("") }
    var searchCity by remember(selectedCity) { mutableStateOf(selectedCity) }
    var topSelectedCategory by remember { mutableStateOf("All") }
    var topSearchScope by remember { mutableStateOf("all") } // "all", "services", "providers"
    var cityDropdownExpanded by remember { mutableStateOf(false) }
    var expandedFaqIndex by remember { mutableStateOf<Int?>(0) }

    val publishedServices = (publishedServicesState as? UiState.Success)?.data.orEmpty()
    val publicProviders = (publicProvidersState as? UiState.Success)?.data.orEmpty()

    val trimmedQuery = searchKeyword.trim().lowercase()
    val isTopSearchActive = trimmedQuery.isNotEmpty() || topSelectedCategory != "All"

    val matchingTopProviders = remember(publicProviders, trimmedQuery, topSelectedCategory, searchCity) {
        if (!isTopSearchActive) {
            emptyList()
        } else {
            publicProviders.filter { prov ->
                val matchesQuery = trimmedQuery.isEmpty() ||
                    prov.displayName.lowercase().contains(trimmedQuery) ||
                    prov.primaryCategory.lowercase().contains(trimmedQuery) ||
                    prov.subcategories.contains(trimmedQuery, ignoreCase = true) ||
                    prov.headline.lowercase().contains(trimmedQuery) ||
                    prov.skills.any { it.lowercase().contains(trimmedQuery) } ||
                    prov.effectivePlatformId.lowercase().contains(trimmedQuery) ||
                    prov.city.lowercase().contains(trimmedQuery)
                val matchesCat = topSelectedCategory == "All" ||
                    prov.primaryCategory.equals(topSelectedCategory, ignoreCase = true) ||
                    prov.subcategories.contains(topSelectedCategory, ignoreCase = true)
                val matchesCity = searchCity == "All India" ||
                    prov.city.equals("All India", ignoreCase = true) ||
                    prov.city.equals(searchCity, ignoreCase = true) ||
                    prov.city.contains(searchCity, ignoreCase = true)
                matchesQuery && matchesCat && matchesCity
            }
        }
    }

    val matchingTopServices = remember(publishedServices, trimmedQuery, topSelectedCategory, searchCity) {
        if (!isTopSearchActive) {
            emptyList()
        } else {
            publishedServices.filter { srv ->
                val matchesQuery = trimmedQuery.isEmpty() ||
                    srv.title.lowercase().contains(trimmedQuery) ||
                    srv.category.lowercase().contains(trimmedQuery) ||
                    srv.providerName.lowercase().contains(trimmedQuery) ||
                    srv.shortSummary.lowercase().contains(trimmedQuery) ||
                    srv.skills.any { it.lowercase().contains(trimmedQuery) } ||
                    srv.city.lowercase().contains(trimmedQuery)
                val matchesCat = topSelectedCategory == "All" ||
                    srv.category.equals(topSelectedCategory, ignoreCase = true)
                val matchesCity = searchCity == "All India" ||
                    srv.city.equals("All India", ignoreCase = true) ||
                    srv.city.equals(searchCity, ignoreCase = true) ||
                    srv.city.contains(searchCity, ignoreCase = true)
                matchesQuery && matchesCat && matchesCity
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Persistent Top Marketplace Search Bar for finding Providers or Local Services by Name or Category
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("marketplace_top_search_bar")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchKeyword,
                    onValueChange = { searchKeyword = it },
                    placeholder = {
                        Text(
                            text = if (isHindi) {
                                stringResource(R.string.marketplace_top_search_placeholder_hi)
                            } else {
                                stringResource(R.string.marketplace_top_search_placeholder_en)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = SkillBlue
                        )
                    },
                    trailingIcon = {
                        if (searchKeyword.isNotEmpty() || topSelectedCategory != "All") {
                            IconButton(
                                onClick = {
                                    searchKeyword = ""
                                    topSelectedCategory = "All"
                                },
                                modifier = Modifier.testTag("marketplace_top_search_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.cd_clear_marketplace_search)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("marketplace_top_search_input")
                )

                // Quick Scope & Category Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "all" to if (isHindi) stringResource(R.string.marketplace_search_scope_all_hi) else stringResource(R.string.marketplace_search_scope_all_en),
                        "services" to if (isHindi) stringResource(R.string.marketplace_search_scope_services_hi) else stringResource(R.string.marketplace_search_scope_services_en),
                        "providers" to if (isHindi) stringResource(R.string.marketplace_search_scope_providers_hi) else stringResource(R.string.marketplace_search_scope_providers_en)
                    ).forEach { (scopeKey, label) ->
                        FilterChip(
                            selected = topSearchScope == scopeKey,
                            onClick = { topSearchScope = scopeKey },
                            label = { Text(label) },
                            modifier = Modifier.testTag("marketplace_search_scope_$scopeKey")
                        )
                    }

                    FilterChip(
                        selected = topSelectedCategory == "All",
                        onClick = { topSelectedCategory = "All" },
                        label = { Text(if (isHindi) "सभी श्रेणियां" else "All Categories") },
                        modifier = Modifier.testTag("marketplace_top_category_all")
                    )

                    MarketplaceCatalog.categories.forEach { cat ->
                        FilterChip(
                            selected = topSelectedCategory.equals(cat.id, ignoreCase = true),
                            onClick = {
                                topSelectedCategory = if (topSelectedCategory.equals(cat.id, ignoreCase = true)) {
                                    "All"
                                } else {
                                    cat.id
                                }
                            },
                            label = { Text(if (isHindi) cat.nameHi else cat.nameEn) },
                            modifier = Modifier.testTag("marketplace_top_category_${cat.id}")
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("home_screen_list"),
            contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Instant Live Results Card when user searches by Name or Category in the Top Search Bar
            if (isTopSearchActive) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .border(1.dp, SkillBlue.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                            .testTag("marketplace_top_search_results_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isHindi) "त्वरित खोज परिणाम" else "Instant Marketplace Search Results",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = buildString {
                                            append("${matchingTopServices.size} local service(s) • ${matchingTopProviders.size} service provider(s)")
                                            if (topSelectedCategory != "All") {
                                                append(" • Category: $topSelectedCategory")
                                            }
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        if (topSelectedCategory != "All" && searchKeyword.isBlank()) {
                                            onCategorySelected(topSelectedCategory)
                                        } else {
                                            onSearchTriggered(
                                                searchKeyword.ifBlank {
                                                    if (topSelectedCategory != "All") topSelectedCategory else ""
                                                },
                                                searchCity
                                            )
                                        }
                                    },
                                    modifier = Modifier.testTag("marketplace_top_search_explore_all_button")
                                ) {
                                    Text(
                                        text = if (isHindi) "सभी देखें →" else "Explore All →",
                                        color = SkillBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            val showProvidersSection = topSearchScope == "all" || topSearchScope == "providers"
                            val showServicesSection = topSearchScope == "all" || topSearchScope == "services"

                            if (showProvidersSection && matchingTopProviders.isNotEmpty()) {
                                Text(
                                    text = if (isHindi) "मैचिंग सर्विस प्रोवाइडर्स (${matchingTopProviders.size})" else "Matching Service Providers (${matchingTopProviders.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SkillBlue,
                                    fontWeight = FontWeight.Bold
                                )
                                matchingTopProviders.take(4).forEach { prov ->
                                    val strictVerified = prov.isStrictVerifiedBadgeActive
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenProviderProfile(prov.userId) }
                                            .testTag("top_search_provider_result_${prov.userId}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(SkillBrandGradient),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = prov.displayName.take(1).uppercase(),
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = prov.displayName,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (strictVerified) {
                                                        Icon(
                                                            imageVector = Icons.Default.Verified,
                                                            contentDescription = "Verified Provider",
                                                            tint = SkillBlue,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "${prov.primaryCategory} • ${prov.city}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = if (prov.startingPrice > 0L) "₹${prov.startingPrice}+" else "View",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = SkillBlue,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            if (showServicesSection && matchingTopServices.isNotEmpty()) {
                                Text(
                                    text = if (isHindi) "मैचिंग लोकल सेवाएं (${matchingTopServices.size})" else "Matching Local Services (${matchingTopServices.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SkillBlue,
                                    fontWeight = FontWeight.Bold
                                )
                                matchingTopServices.take(5).forEach { srv ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenServiceDetail(srv.id) }
                                            .testTag("top_search_service_result_${srv.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = srv.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${srv.category} • by ${srv.providerName} • ${srv.city}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (srv.pricingModel == "quote") "Quote" else "₹${srv.startingPrice}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = SkillBlue,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }

                            if ((showProvidersSection && matchingTopProviders.isEmpty()) &&
                                (showServicesSection && matchingTopServices.isEmpty()) ||
                                (topSearchScope == "providers" && matchingTopProviders.isEmpty()) ||
                                (topSearchScope == "services" && matchingTopServices.isEmpty())
                            ) {
                                Text(
                                    text = if (isHindi) {
                                        "इस नाम या श्रेणी के लिए कोई मिलान नहीं मिला। कृपया अन्य कीवर्ड आज़माएं।"
                                    } else {
                                        "No matching local services or providers found for this name or category."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 1. Optional Announcement Bar (Admin managed) positioned cleanly below the top app bar
        if (globalSettings?.announcementActive != false) {
            item {
                val text = globalSettings?.announcementText?.ifBlank {
                    if (isHindi) "SkillConnect इंडिया में आपका स्वागत है — वेरिफाइड लोकल प्रोफेशनल्स और फ्रीलांसर्स से जुड़ें"
                    else "Welcome to SkillConnect India — Connect directly with verified local professionals & freelancers."
                } ?: "Welcome to SkillConnect India — Verified Local Professionals & Freelancers."

                Surface(
                    color = SkillPurple.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(1.dp, SkillPurple.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Announcement",
                            tint = SkillPurple,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Hero Section with balanced vertical spacing
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SkillNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
                    SkillConnectOfficialLogoBanner(
                        modifier = Modifier.fillMaxWidth(),
                        height = 104.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = SkillCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (isHindi) "भारत का लोकल सर्विसेज़ मार्केटप्लेस" else "India's Local Services Marketplace",
                            style = MaterialTheme.typography.labelMedium,
                            color = SkillCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isHindi) {
                            "सही प्रोफेशनल खोजें। अपना काम पूरा करें।"
                        } else {
                            "Find the Right Professional. Get Things Done."
                        },
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isHindi) {
                            "अपने व्यक्तिगत प्रोजेक्ट्स और बिज़नेस की ज़रूरतों के लिए प्रतिभाशाली प्रोफेशनल्स और भरोसेमंद लोकल सेवाएं खोजें।"
                        } else {
                            "Discover talented professionals and trusted local services for your personal projects and business needs."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Hero Illustration
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_connect_1793072729236),
                        contentDescription = "Customers and service professionals connecting across India",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Search Bar + City Input
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = searchKeyword,
                                onValueChange = { searchKeyword = it },
                                label = {
                                    Text(
                                        if (isHindi) "कौन सी सेवा चाहिए? (जैसे Logo, Electrician, AC)"
                                        else "What service do you need? (e.g. Logo Design, Electrician)"
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = SkillBlue)
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("hero_service_search_input")
                            )

                            AllIndiaLocationSelectorField(
                                city = searchCity,
                                label = if (isHindi) "भारत में स्थान चुनें (राज्य, शहर, लोकैलिटी, पिन)" else "All-India Location (State, City, Locality & PIN)",
                                allowAllIndia = true,
                                isHindi = isHindi,
                                testTag = "hero_location_input",
                                onLocationChanged = { _, newCity, _, _ ->
                                    searchCity = newCity
                                }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onSearchTriggered(searchKeyword, searchCity) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("hero_search_services_button")
                                ) {
                                    Text(
                                        text = if (isHindi) "सेवाएं खोजें" else "Search Services",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onNavigate(AppDestination.PROVIDER_DIRECTORY) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("hero_browse_categories_button")
                                ) {
                                    Text(
                                        text = if (isHindi) "प्रोवाइडर डायरेक्टरी" else "Provider Directory",
                                        color = SkillNavy,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "क्या आप एक स्किल्ड प्रोफेशनल हैं?" else "Browse verified & active providers across India:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        TextButton(
                            onClick = { onNavigate(AppDestination.PROVIDER_DIRECTORY) },
                            modifier = Modifier.testTag("hero_become_provider_cta")
                        ) {
                            Text(
                                text = if (isHindi) "प्रोवाइडर्स देखें →" else "View Directory →",
                                color = SkillCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. Trust Indicators (Genuine metrics when available, otherwise honest value propositions)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isHindi) "पारदर्शी और भरोसेमंद प्लेटफ़ॉर्म" else "Platform Trust & Value Propositions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (publishedServices.isNotEmpty() || publicProviders.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GenuineMetricCard(
                            modifier = Modifier.weight(1f),
                            value = "${publishedServices.size}",
                            label = if (isHindi) "सक्रिय सेवाएं" else "Active Listings"
                        )
                        GenuineMetricCard(
                            modifier = Modifier.weight(1f),
                            value = "${publicProviders.size}",
                            label = if (isHindi) "प्रोवाइडर प्रोफ़ाइल" else "Public Providers"
                        )
                        GenuineMetricCard(
                            modifier = Modifier.weight(1f),
                            value = "${publicProviders.count { it.isStrictVerifiedBadgeActive }}",
                            label = if (isHindi) "वेरिफाइड प्रोवाइडर" else "Verified Pros"
                        )
                    }
                }
                val propositions = if (isHindi) MarketplaceCatalog.valuePropositionsHi else MarketplaceCatalog.valuePropositionsEn
                propositions.forEach { (title, desc) ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SkillSuccessGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Popular Categories (All 15 categories with real counts only when > 0)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "लोकप्रिय सेवा कैटेगरी" else "Popular Service Categories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onNavigate(AppDestination.EXPLORE) }) {
                        Text(if (isHindi) "सभी देखें" else "Explore All")
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 3
                ) {
                    MarketplaceCatalog.categories.forEach { cat ->
                        val activeCount = publishedServices.count { it.category.equals(cat.id, ignoreCase = true) }
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                .clickable { onCategorySelected(cat.id) }
                                .testTag("category_card_${cat.id.lowercase().replace(" ", "_")}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SkillBlue.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = cat.nameEn,
                                        tint = SkillBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isHindi) cat.nameHi else cat.nameEn,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (activeCount > 0) {
                                    Text(
                                        text = "$activeCount active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SkillSuccessGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Featured Professionals & Active Service Listings
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "प्रमुख प्रोफेशनल्स और सेवाएं" else "Featured Professionals & Services",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onNavigate(AppDestination.EXPLORE) }) {
                        Text(if (isHindi) "सभी देखें" else "View All")
                    }
                }

                if (publishedServices.isEmpty() && publicProviders.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isHindi) "पहली सेवा लिस्टिंग प्रकाशित करें" else "Be the First Featured Professional in Your City",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isHindi) {
                                    "SkillConnect पर कोई नकली प्रोफ़ाइल या फ़र्जी रेटिंग नहीं दिखाई जाती। अपनी पहली वास्तविक सेवा लिस्टिंग बनाएं या प्रोवाइडर प्रोफ़ाइल सेट करें।"
                                } else {
                                    "SkillConnect never fabricates fake profiles or ratings. Publish your first real service listing or switch to Provider mode to appear here."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { onNavigate(AppDestination.CREATE_EDIT_SERVICE) },
                                colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                                modifier = Modifier.testTag("empty_featured_create_service_btn")
                            ) {
                                Text(if (isHindi) "नई सेवा जोड़ें" else "Create Service Listing")
                            }
                        }
                    }
                } else {
                    publishedServices.take(4).forEach { service ->
                        val isSaved = favourites.any { it.targetId == service.id && it.targetType == "service" }
                        ServiceListingCard(
                            service = service,
                            isSaved = isSaved,
                            onViewDetails = { onOpenServiceDetail(service.id) },
                            onViewProvider = { onOpenProviderProfile(service.providerId) },
                            onToggleSave = {
                                onToggleFavourite(
                                    service.id,
                                    "service",
                                    service.title,
                                    service.providerName,
                                    "${service.city} ${service.locality}".trim(),
                                    service.startingPrice
                                )
                            }
                        )
                    }
                }
            }
        }

        // 5B. Jobs & Earning Opportunities + Skills & Courses Hub
        item {
            JobsAndCoursesHubCard(
                isHindi = isHindi,
                appliedJobIds = appliedJobIds,
                enrolledCourseIds = enrolledCourseIds,
                onToggleJobApplication = onToggleJobApplication,
                onToggleCourseEnrollment = onToggleCourseEnrollment
            )
        }

        // 6. How SkillConnect Works (4 Steps)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isHindi) "SkillConnect कैसे काम करता है" else "How SkillConnect Works",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                val steps = if (isHindi) MarketplaceCatalog.howItWorksStepsHi else MarketplaceCatalog.howItWorksStepsEn
                steps.forEachIndexed { idx, (stepTitle, stepDesc) ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SkillBrandGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = stepTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stepDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Become a Provider Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SkillNavy)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_provider_banner_1793072728889),
                        contentDescription = "Become a Provider on SkillConnect",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isHindi) "अपने कौशल को अवसरों में बदलें।" else "Turn Your Skills Into Opportunities.",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi) {
                            "अपना प्रोफेशनल पोर्टफोलियो बनाएं, पारदर्शी रूप से ग्राहकों से जुड़ें और एक ही जगह अपनी बुकिंग्स प्रबंधित करें।"
                        } else {
                            "Discover local clients, showcase your portfolio transparently, and manage all service enquiries in one place."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { onNavigate(AppDestination.CREATE_EDIT_SERVICE) },
                        colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("create_free_provider_profile_cta")
                    ) {
                        Text(
                            text = if (isHindi) "अपनी मुफ़्त प्रोवाइडर प्रोफ़ाइल बनाएं" else "Create Your Free Provider Profile",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 8. Clearly Labelled Sample Testimonials (Development Preview)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isHindi) "सैंपल फीडबैक (Development Sample)" else "Customer Stories (Sample Preview)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Clearly labelled sample testimonials for development layout preview. Real customer reviews appear on service pages after confirmed job completion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MarketplaceCatalog.sampleTestimonials.forEach { item ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "“${item.quote}”",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${item.author} • ${item.city} (${item.serviceUsed})",
                                style = MaterialTheme.typography.labelMedium,
                                color = SkillBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 9. Frequently Asked Questions
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isHindi) "अक्सर पूछे जाने वाले प्रश्न (FAQ)" else "Frequently Asked Questions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                MarketplaceCatalog.faqs.forEachIndexed { idx, faq ->
                    val expanded = expandedFaqIndex == idx
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable { expandedFaqIndex = if (expanded) null else idx }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isHindi) faq.questionHi else faq.questionEn,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                            AnimatedVisibility(visible = expanded) {
                                Text(
                                    text = if (isHindi) faq.answerHi else faq.answerEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 10. Navy Footer Section
        item {
            Surface(
                color = SkillNavy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SkillConnectBrandLogo(onNavyHeader = true)
                    Text(
                        text = "“Your Skills. Your Local Network.” • Connecting customers with verified local professionals and freelancers across India.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Explore Services" to AppDestination.EXPLORE,
                            "Become a Provider" to AppDestination.CREATE_EDIT_SERVICE,
                            "Provider Verification" to AppDestination.PROVIDER_VERIFICATION,
                            "Subscriptions" to AppDestination.SUBSCRIPTIONS,
                            "Help Centre & Legal Policies" to AppDestination.HELP_AND_LEGAL
                        ).forEach { (label, dest) ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = SkillCyan,
                                modifier = Modifier
                                    .clickable { onNavigate(dest) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "© 2026 SkillConnect India. Built with Zero-Trust Security & Transparent Community Standards.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun GenuineMetricCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Surface(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = SkillBlue,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun JobsAndCoursesHubCard(
    isHindi: Boolean,
    appliedJobIds: List<String>,
    enrolledCourseIds: List<String>,
    onToggleJobApplication: (JobOpportunityItem) -> Unit,
    onToggleCourseEnrollment: (SkillCourseItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeHubTab by remember { mutableStateOf("jobs") }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("jobs_and_courses_hub"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isHindi) "नौकरियां, कमाई के अवसर और स्किल कोर्सेज" else "Jobs, Earning Opportunities & Skill Courses",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = activeHubTab == "jobs",
                onClick = { activeHubTab = "jobs" },
                label = {
                    Text(
                        if (isHindi) "नौकरियां और कमाई (${MarketplaceCatalog.jobOpportunities.size})"
                        else "Jobs & Earning (${MarketplaceCatalog.jobOpportunities.size})"
                    )
                },
                modifier = Modifier.testTag("hub_jobs_tab")
            )
            FilterChip(
                selected = activeHubTab == "courses",
                onClick = { activeHubTab = "courses" },
                label = {
                    Text(
                        if (isHindi) "स्किल्स और कोर्सेज (${MarketplaceCatalog.skillCourses.size})"
                        else "Skills & Courses (${MarketplaceCatalog.skillCourses.size})"
                    )
                },
                modifier = Modifier.testTag("hub_courses_tab")
            )
        }

        if (activeHubTab == "jobs") {
            MarketplaceCatalog.jobOpportunities.forEach { job ->
                val isApplied = job.id in appliedJobIds
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = job.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = job.payoutRangeInr,
                                style = MaterialTheme.typography.labelMedium,
                                color = SkillSuccessGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${job.companyOrClient} • ${job.category} • ${job.city} (${job.commitmentType})",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkillBlue
                        )
                        Text(
                            text = job.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Skills: ${job.skillsRequired.joinToString(", ")}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { onToggleJobApplication(job) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isApplied) SkillSuccessGreen else SkillBlue
                                )
                            ) {
                                Text(if (isApplied) "Applied ✓" else "Apply Now")
                            }
                        }
                    }
                }
            }
        } else {
            MarketplaceCatalog.skillCourses.forEach { course ->
                val isEnrolled = course.id in enrolledCourseIds
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = course.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (course.priceInr == 0L) "FREE" else "₹${course.priceInr}",
                                style = MaterialTheme.typography.labelMedium,
                                color = SkillPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${course.instructorName} • ${course.category} • ${course.durationHours}h • ${course.level}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkillBlue
                        )
                        Text(
                            text = "${course.summary} (Badge: ${course.badgeEarned})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { onToggleCourseEnrollment(course) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEnrolled) SkillSuccessGreen else SkillPurple
                            )
                        ) {
                            Text(if (isEnrolled) "Enrolled ✓" else "Enroll Course")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreServicesScreen(
    isHindi: Boolean,
    filterState: ExploreFilterState,
    publishedServicesState: UiState<List<ServiceListing>>,
    publicProvidersState: UiState<List<UserProfile>> = UiState.Success(emptyList()),
    filteredServices: List<ServiceListing>,
    favourites: List<FavouriteItem>,
    appliedJobIds: List<String> = emptyList(),
    enrolledCourseIds: List<String> = emptyList(),
    onToggleJobApplication: (JobOpportunityItem) -> Unit = {},
    onToggleCourseEnrollment: (SkillCourseItem) -> Unit = {},
    onUpdateFilter: ((ExploreFilterState) -> ExploreFilterState) -> Unit,
    onClearFilters: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenServiceDetail: (String) -> Unit,
    onOpenProviderProfile: (String) -> Unit,
    onToggleFavourite: (targetId: String, targetType: String, title: String, subtitle: String, location: String, price: Long) -> Unit,
    onOpenCreateService: () -> Unit
) {
    var showFilterDrawer by remember { mutableStateOf(false) }
    var showExploreLocationPicker by remember { mutableStateOf(false) }
    var exploreResultScope by remember { mutableStateOf("all") } // "all", "services", "providers"

    val allPublicProviders = (publicProvidersState as? UiState.Success)?.data.orEmpty()
    val matchingProviders = remember(
        allPublicProviders,
        filterState.keyword,
        filterState.selectedCategory,
        filterState.selectedCity,
        filterState.verifiedOnly
    ) {
        val kw = filterState.keyword.trim().lowercase()
        allPublicProviders.filter { prov ->
            val matchesKw = kw.isEmpty() ||
                prov.displayName.lowercase().contains(kw) ||
                prov.primaryCategory.lowercase().contains(kw) ||
                prov.subcategories.contains(kw, ignoreCase = true) ||
                prov.headline.lowercase().contains(kw) ||
                prov.skills.any { it.lowercase().contains(kw) } ||
                prov.effectivePlatformId.lowercase().contains(kw) ||
                prov.city.lowercase().contains(kw)
            val matchesCat = filterState.selectedCategory == "All" ||
                prov.primaryCategory.equals(filterState.selectedCategory, ignoreCase = true) ||
                prov.subcategories.contains(filterState.selectedCategory, ignoreCase = true)
            val matchesCity = filterState.selectedCity == "All India" ||
                prov.city.equals("All India", ignoreCase = true) ||
                prov.city.equals(filterState.selectedCity, ignoreCase = true) ||
                prov.city.contains(filterState.selectedCity, ignoreCase = true)
            val matchesVerified = !filterState.verifiedOnly || prov.isStrictVerifiedBadgeActive
            matchesKw && matchesCat && matchesCity && matchesVerified
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("explore_services_screen")
    ) {
        // Top Search & Filter Control Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = filterState.keyword,
                        onValueChange = { kw -> onUpdateFilter { it.copy(keyword = kw) } },
                        placeholder = {
                            Text(
                                if (isHindi) stringResource(R.string.marketplace_top_search_placeholder_hi)
                                else stringResource(R.string.marketplace_top_search_placeholder_en)
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (filterState.keyword.isNotEmpty()) {
                                IconButton(onClick = { onUpdateFilter { it.copy(keyword = "") } }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("explore_search_input")
                    )

                    IconButton(
                        onClick = { showFilterDrawer = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SkillBlue.copy(alpha = 0.1f))
                            .testTag("explore_filter_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Open Filters",
                            tint = SkillBlue
                        )
                    }

                    IconButton(
                        onClick = { onUpdateFilter { it.copy(isGridView = !it.isGridView) } },
                        modifier = Modifier.testTag("explore_view_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (filterState.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Switch Grid or List View"
                        )
                    }
                }

                // Category Horizontal Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterState.selectedCategory == "All",
                        onClick = { onUpdateFilter { it.copy(selectedCategory = "All") } },
                        label = { Text(if (isHindi) "सभी" else "All Categories") }
                    )
                    MarketplaceCatalog.categories.forEach { cat ->
                        FilterChip(
                            selected = filterState.selectedCategory.equals(cat.id, ignoreCase = true),
                            onClick = { onUpdateFilter { it.copy(selectedCategory = cat.id) } },
                            label = { Text(if (isHindi) cat.nameHi else cat.nameEn) }
                        )
                    }
                }

                // Active filter summary & Sort chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterState.selectedCity != "All India",
                        onClick = { showExploreLocationPicker = true },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = {
                            Text(
                                if (filterState.selectedLocality.isNotBlank() && filterState.selectedLocality != "All Localities") {
                                    "${filterState.selectedCity}, ${filterState.selectedLocality}"
                                } else {
                                    "Location: ${filterState.selectedCity}"
                                }
                            )
                        },
                        modifier = Modifier.testTag("explore_location_chip")
                    )
                    FilterChip(
                        selected = filterState.verifiedOnly,
                        onClick = { onUpdateFilter { it.copy(verifiedOnly = !it.verifiedOnly) } },
                        leadingIcon = {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(if (isHindi) "केवल वेरिफाइड" else "Verified Only") }
                    )
                    listOf(
                        "relevance" to "Relevance",
                        "newest" to "Newest",
                        "price_low" to "Price: Low–High",
                        "price_high" to "Price: High–Low"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = filterState.sortBy == key,
                            onClick = { onUpdateFilter { it.copy(sortBy = key) } },
                            label = { Text(label) }
                        )
                    }
                    TextButton(
                        onClick = onClearFilters,
                        modifier = Modifier.testTag("clear_all_filters_button")
                    ) {
                        Text(if (isHindi) "फ़िल्टर रीसेट" else "Clear Filters")
                    }
                }
            }
        }

        // Results Area
        when (publishedServicesState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SkillBlue)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Loading verified marketplace services...")
                    }
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Network or database error while loading services",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = publishedServicesState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is UiState.Success -> {
                val showServicesInExplore = exploreResultScope == "all" || exploreResultScope == "services"
                val showProvidersInExplore = exploreResultScope == "all" || exploreResultScope == "providers"
                val hasMatchingServices = showServicesInExplore && filteredServices.isNotEmpty()
                val hasMatchingProviders = showProvidersInExplore && matchingProviders.isNotEmpty()

                if (!hasMatchingServices && !hasMatchingProviders) {
                    // Empty Search State + Search Suggestions
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = if (isHindi) "कोई सेवा या प्रोवाइडर नहीं मिला" else "No Matching Services or Providers Found",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try clearing active filters, broadening your city selection to 'All India', or explore popular suggestions below:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Logo Design", "Website Development", "Electrician", "Home Cleaning", "AC Repair", "Photography").forEach { suggestion ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            onUpdateFilter {
                                                ExploreFilterState(selectedCategory = suggestion)
                                            }
                                        },
                                        label = { Text(suggestion) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = onClearFilters) {
                                    Text("Reset All Filters")
                                }
                                Button(
                                    onClick = onOpenCreateService,
                                    colors = ButtonDefaults.buttonColors(containerColor = SkillBlue)
                                ) {
                                    Text("List a Service")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = exploreResultScope == "all",
                                    onClick = { exploreResultScope = "all" },
                                    label = {
                                        Text(
                                            if (isHindi) "सभी (${filteredServices.size + matchingProviders.size})"
                                            else "All (${filteredServices.size + matchingProviders.size})"
                                        )
                                    },
                                    modifier = Modifier.testTag("explore_scope_all_chip")
                                )
                                FilterChip(
                                    selected = exploreResultScope == "services",
                                    onClick = { exploreResultScope = "services" },
                                    label = {
                                        Text(
                                            if (isHindi) "लोकल सेवाएं (${filteredServices.size})"
                                            else "Local Services (${filteredServices.size})"
                                        )
                                    },
                                    modifier = Modifier.testTag("explore_scope_services_chip")
                                )
                                FilterChip(
                                    selected = exploreResultScope == "providers",
                                    onClick = { exploreResultScope = "providers" },
                                    label = {
                                        Text(
                                            if (isHindi) "प्रोवाइडर्स (${matchingProviders.size})"
                                            else "Service Providers (${matchingProviders.size})"
                                        )
                                    },
                                    modifier = Modifier.testTag("explore_scope_providers_chip")
                                )
                            }
                        }

                        if (showProvidersInExplore && matchingProviders.isNotEmpty() &&
                            (exploreResultScope == "providers" || filterState.keyword.isNotBlank() || filterState.selectedCategory != "All")
                        ) {
                            item {
                                Text(
                                    text = if (isHindi) {
                                        "मैचिंग सर्विस प्रोवाइडर्स (${matchingProviders.size})"
                                    } else {
                                        "Matching Service Providers (${matchingProviders.size})"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SkillBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            val displayedProviders = if (exploreResultScope == "providers") matchingProviders else matchingProviders.take(4)
                            items(displayedProviders, key = { "explore_prov_${it.userId}" }) { prov ->
                                val strictVerified = prov.isStrictVerifiedBadgeActive
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                        .clickable { onOpenProviderProfile(prov.userId) }
                                        .testTag("explore_provider_result_${prov.userId}")
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
                                                text = prov.displayName.take(1).uppercase(),
                                                color = Color.White,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = prov.displayName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (strictVerified) {
                                                    Icon(
                                                        imageVector = Icons.Default.Verified,
                                                        contentDescription = "Verified by SkillConnect",
                                                        tint = SkillBlue,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = prov.headline.ifBlank { "${prov.primaryCategory} Professional" },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${prov.primaryCategory} • ${prov.city}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SkillBlue,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { onOpenProviderProfile(prov.userId) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Profile", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        if (showServicesInExplore && filteredServices.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Showing ${filteredServices.size} service listing(s) • City: ${filterState.selectedCity}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (filterState.isGridView) {
                            val chunked = filteredServices.chunked(2)
                            items(chunked) { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    pair.forEach { service ->
                                        val isSaved = favourites.any { it.targetId == service.id && it.targetType == "service" }
                                        Box(modifier = Modifier.weight(1f)) {
                                            ServiceListingCard(
                                                service = service,
                                                isSaved = isSaved,
                                                compact = true,
                                                onViewDetails = { onOpenServiceDetail(service.id) },
                                                onViewProvider = { onOpenProviderProfile(service.providerId) },
                                                onToggleSave = {
                                                    onToggleFavourite(
                                                        service.id,
                                                        "service",
                                                        service.title,
                                                        service.providerName,
                                                        "${service.city} ${service.locality}".trim(),
                                                        service.startingPrice
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            items(filteredServices, key = { it.id }) { service ->
                                val isSaved = favourites.any { it.targetId == service.id && it.targetType == "service" }
                                ServiceListingCard(
                                    service = service,
                                    isSaved = isSaved,
                                    onViewDetails = { onOpenServiceDetail(service.id) },
                                    onViewProvider = { onOpenProviderProfile(service.providerId) },
                                    onToggleSave = {
                                        onToggleFavourite(
                                            service.id,
                                            "service",
                                            service.title,
                                            service.providerName,
                                            "${service.city} ${service.locality}".trim(),
                                            service.startingPrice
                                        )
                                    }
                                )
                            }
                        }

                            if (publishedServicesState.data.size > filteredServices.size) {
                                item {
                                    OutlinedButton(
                                        onClick = onLoadMore,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("explore_load_more_button")
                                    ) {
                                        Text("Load More Services")
                                    }
                                }
                            }
                        }

                        item {
                            JobsAndCoursesHubCard(
                                isHindi = isHindi,
                                appliedJobIds = appliedJobIds,
                                enrolledCourseIds = enrolledCourseIds,
                                onToggleJobApplication = onToggleJobApplication,
                                onToggleCourseEnrollment = onToggleCourseEnrollment,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Mobile Filter Drawer Bottom Sheet
    if (showFilterDrawer) {
        ModalBottomSheet(
            onDismissRequest = { showFilterDrawer = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Filter Marketplace Services",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // All-India Location Selector (State, District/City, Locality, PIN & GPS)
                Text("All-India Location (28 States, 8 UTs, Locality & PIN)", style = MaterialTheme.typography.labelLarge)
                AllIndiaLocationSelectorField(
                    city = filterState.selectedCity,
                    locality = filterState.selectedLocality,
                    label = "State, District/City, Locality or PIN",
                    allowAllIndia = true,
                    showLocalityAndPinInputs = true,
                    isHindi = isHindi,
                    testTag = "explore_drawer_location_selector",
                    onLocationChanged = { _, newCity, newLocality, _ ->
                        onUpdateFilter {
                            it.copy(
                                selectedCity = newCity,
                                selectedLocality = if (newLocality == "All Localities") "" else newLocality
                            )
                        }
                    }
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MarketplaceCatalog.indianCities.keys.take(16).forEach { city ->
                        FilterChip(
                            selected = filterState.selectedCity == city,
                            onClick = { onUpdateFilter { it.copy(selectedCity = city, selectedLocality = "") } },
                            label = { Text(city) }
                        )
                    }
                }

                // Max Price Slider
                Text(
                    text = "Max Starting Price: ₹${filterState.maxPriceInr}",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = filterState.maxPriceInr.toFloat(),
                    onValueChange = { v -> onUpdateFilter { it.copy(maxPriceInr = v.toLong()) } },
                    valueRange = 500f..100000f
                )

                // Minimum Rating Filter
                Text("Minimum Genuine Rating", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.0 to "Any", 3.0 to "3.0★+", 4.0 to "4.0★+", 4.5 to "4.5★+").forEach { (r, label) ->
                        FilterChip(
                            selected = filterState.minRating == r,
                            onClick = { onUpdateFilter { it.copy(minRating = r) } },
                            label = { Text(label) }
                        )
                    }
                }

                // Pricing Model
                Text("Pricing Model", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All" to "All", "fixed" to "Fixed Price", "hourly" to "Hourly", "quote" to "Request Quote").forEach { (m, label) ->
                        FilterChip(
                            selected = filterState.pricingModelFilter == m,
                            onClick = { onUpdateFilter { it.copy(pricingModelFilter = m) } },
                            label = { Text(label) }
                        )
                    }
                }

                // Verified Only Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Verified Providers Only", style = MaterialTheme.typography.titleSmall)
                    Switch(
                        checked = filterState.verifiedOnly,
                        onCheckedChange = { v -> onUpdateFilter { it.copy(verifiedOnly = v) } }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onClearFilters()
                            showFilterDrawer = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All")
                    }
                    Button(
                        onClick = { showFilterDrawer = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply Filters")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showExploreLocationPicker) {
        AllIndiaLocationPickerDialog(
            initialCity = filterState.selectedCity,
            initialLocality = filterState.selectedLocality,
            allowAllIndia = true,
            isHindi = isHindi,
            onDismiss = { showExploreLocationPicker = false },
            onLocationSelected = { _, newCity, newLocality, _ ->
                showExploreLocationPicker = false
                onUpdateFilter {
                    it.copy(
                        selectedCity = newCity,
                        selectedLocality = if (newLocality == "All Localities") "" else newLocality
                    )
                }
            }
        )
    }
}

@Composable
fun ServiceListingCard(
    service: ServiceListing,
    isSaved: Boolean,
    compact: Boolean = false,
    onViewDetails: () -> Unit,
    onViewProvider: () -> Unit,
    onToggleSave: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable { onViewDetails() }
            .testTag("service_card_${service.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = SkillBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = service.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = SkillBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (service.isFeatured) {
                        Surface(
                            color = SkillPurple.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Featured",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkillPurple,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove from Favourites" else "Save to Favourites",
                        tint = if (isSaved) SkillBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = service.title,
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onViewProvider() }
            ) {
                Text(
                    text = "by ${service.providerName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (service.providerVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified by SkillConnect",
                        tint = SkillBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Verified by SkillConnect",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkillBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!compact) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = service.shortSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (service.locality.isNotBlank()) "${service.city}, ${service.locality}" else service.city,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (service.reviewCount > 0L) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = SkillWarningAmber,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f (%d)", service.averageRating, service.reviewCount),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "New Listing",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (service.pricingModel) {
                            "quote" -> "Request a Quote"
                            "hourly" -> "₹${service.startingPrice} / hr"
                            else -> "Starts at ₹${service.startingPrice}"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = SkillBlue,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = service.availability.ifBlank { "Available" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onViewDetails,
                    colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("View Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProviderDirectoryScreen(
    isHindi: Boolean,
    publicProvidersState: UiState<List<UserProfile>>,
    favourites: List<FavouriteItem>,
    onOpenProviderProfile: (String) -> Unit,
    onMessageProvider: (providerId: String, providerName: String) -> Unit,
    onToggleFavourite: (targetId: String, targetType: String, title: String, subtitle: String, location: String, price: Long) -> Unit,
    onSubmitProjectToProvider: (UserProfile) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("All India") }
    var selectedCategory by remember { mutableStateOf("All") }
    var verifiedOnly by remember { mutableStateOf(false) }

    val allProviders = (publicProvidersState as? UiState.Success)?.data.orEmpty()
    val filteredProviders = remember(allProviders, searchQuery, selectedCity, selectedCategory, verifiedOnly) {
        allProviders.filter { prov ->
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                prov.displayName.lowercase().contains(q) ||
                prov.headline.lowercase().contains(q) ||
                prov.effectivePlatformId.lowercase().contains(q) ||
                prov.primaryCategory.lowercase().contains(q) ||
                prov.skills.any { it.lowercase().contains(q) } ||
                prov.city.lowercase().contains(q)
            val matchesCity = selectedCity == "All India" || prov.city.equals(selectedCity, ignoreCase = true)
            val matchesCat = selectedCategory == "All" ||
                prov.primaryCategory.equals(selectedCategory, ignoreCase = true) ||
                prov.subcategories.contains(selectedCategory, ignoreCase = true)
            val matchesVerified = !verifiedOnly || prov.isStrictVerifiedBadgeActive
            matchesQuery && matchesCity && matchesCat && matchesVerified
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("provider_directory_screen")
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isHindi) "पब्लिक सर्विस प्रोवाइडर डायरेक्टरी" else "Public Service Provider Directory",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Name, Skill, Category, City, or SC-PRO ID...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("directory_search_input")
                )

    var showDirectoryLocationPicker by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = verifiedOnly,
                        onClick = { verifiedOnly = !verifiedOnly },
                        leadingIcon = { Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Verified by SkillConnect Only") }
                    )
                    FilterChip(
                        selected = selectedCity != "All India",
                        onClick = { showDirectoryLocationPicker = true },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("All-India Picker: $selectedCity") },
                        modifier = Modifier.testTag("directory_all_india_picker_chip")
                    )
                    MarketplaceCatalog.indianCities.keys.take(20).forEach { city ->
                        FilterChip(
                            selected = selectedCity == city,
                            onClick = { selectedCity = city },
                            label = { Text(city) }
                        )
                    }
                }
                if (showDirectoryLocationPicker) {
                    AllIndiaLocationPickerDialog(
                        initialCity = selectedCity,
                        allowAllIndia = true,
                        isHindi = isHindi,
                        onDismiss = { showDirectoryLocationPicker = false },
                        onLocationSelected = { _, newCity, _, _ ->
                            showDirectoryLocationPicker = false
                            selectedCity = newCity
                        }
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == "All",
                        onClick = { selectedCategory = "All" },
                        label = { Text("All Categories") }
                    )
                    MarketplaceCatalog.categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory.equals(cat.id, ignoreCase = true),
                            onClick = { selectedCategory = cat.id },
                            label = { Text(cat.nameEn) }
                        )
                    }
                }
            }
        }

        if (filteredProviders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No Service Providers Match Your Filter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Try clearing filters or searching across All India. Only active providers who consented to public listing appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            searchQuery = ""
                            selectedCity = "All India"
                            selectedCategory = "All"
                            verifiedOnly = false
                        }
                    ) {
                        Text("Reset Directory Filters")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Showing ${filteredProviders.size} active service provider(s) • Transparent 6-Stage Verification Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(filteredProviders, key = { it.userId }) { prov ->
                    val strictVerified = prov.isStrictVerifiedBadgeActive
                    val isSaved = favourites.any { it.targetId == prov.userId && it.targetType == "provider" }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                            .clickable { onOpenProviderProfile(prov.userId) }
                            .testTag("directory_provider_card_${prov.userId}")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = SkillBlue.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = prov.effectivePlatformId,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SkillBlue,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    color = if (strictVerified) SkillBlue.copy(alpha = 0.14f) else SkillWarningAmber.copy(alpha = 0.14f),
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
                                                contentDescription = "Blue Tick Verified by SkillConnect",
                                                tint = SkillBlue,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Text(
                                            text = prov.verificationBadgeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (strictVerified) SkillBlue else SkillWarningAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Row(
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
                                        text = prov.displayName.take(1).uppercase(),
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(prov.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        if (strictVerified) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "Blue Tick Verified by SkillConnect",
                                                tint = SkillBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = prov.headline.ifBlank { "${prov.primaryCategory} Professional" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val modeText = when {
                                        prov.remoteAvailable && prov.inPersonAvailable -> "Hybrid"
                                        prov.remoteAvailable -> "Online"
                                        else -> "On-Site"
                                    }
                                    Text(
                                        text = "${prov.city} ${prov.locality}".trim() + " • ${prov.experienceYears} yrs exp • $modeText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onToggleFavourite(
                                            prov.userId,
                                            "provider",
                                            prov.displayName,
                                            prov.headline,
                                            prov.city,
                                            prov.startingPrice
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Save Provider",
                                        tint = if (isSaved) SkillBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (prov.skills.isNotEmpty()) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    prov.skills.take(5).forEach { tag ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = tag,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Starts at ₹${prov.startingPrice}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SkillBlue,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onMessageProvider(prov.userId, prov.displayName) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("Message", maxLines = 1, softWrap = false)
                                    }
                                    Button(
                                        onClick = { onOpenProviderProfile(prov.userId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("View Profile", maxLines = 1, softWrap = false)
                                    }
                                }
                            }

                            Button(
                                onClick = { onSubmitProjectToProvider(prov) },
                                colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("directory_submit_project_${prov.userId}")
                            ) {
                                Text(
                                    text = "Submit Your Project",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

