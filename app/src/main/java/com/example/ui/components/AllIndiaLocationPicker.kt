package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.IndiaLocationDirectory
import com.example.ui.theme.SkillBlue
import com.example.ui.theme.SkillNavy
import com.example.ui.theme.SkillPurple
import com.example.ui.theme.SkillSuccessGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllIndiaLocationPickerDialog(
    initialCity: String,
    initialLocality: String = "",
    initialState: String = "",
    initialPinCode: String = "",
    allowAllIndia: Boolean = true,
    isHindi: Boolean = false,
    onDismiss: () -> Unit,
    onLocationSelected: (state: String, city: String, locality: String, pinCode: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedState by remember(initialCity, initialState) {
        mutableStateOf(
            when {
                initialCity.equals("All India", ignoreCase = true) -> "All States & UTs"
                initialState.isNotBlank() -> initialState
                else -> IndiaLocationDirectory.stateForCity(initialCity)
            }
        )
    }
    var selectedCity by remember(initialCity) {
        mutableStateOf(initialCity.ifBlank { if (allowAllIndia) "All India" else "Bengaluru" })
    }
    var selectedLocality by remember(initialLocality) {
        mutableStateOf(initialLocality)
    }
    var selectedPinCode by remember(initialCity, initialPinCode) {
        mutableStateOf(
            when {
                initialPinCode.isNotBlank() -> initialPinCode
                initialCity.equals("All India", ignoreCase = true) -> ""
                else -> IndiaLocationDirectory.defaultPinForCity(initialCity)
            }
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var stateFilterChip by remember { mutableStateOf("All States & UTs") }
    var pinSearchInput by remember { mutableStateOf(selectedPinCode) }
    var resolvedPostOffices by remember { mutableStateOf<List<String>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isResolvingGps by remember { mutableStateOf(false) }
    var isResolvingPin by remember { mutableStateOf(false) }

    fun triggerLiveGpsResolution() {
        isResolvingGps = true
        errorMessage = null
        statusMessage = if (isHindi) "GPS से स्थान खोजा जा रहा है..." else "Detecting Indian city, locality & PIN via GPS..."
        scope.launch {
            val result = IndiaLocationDirectory.detectCurrentIndiaLocation(context)
            isResolvingGps = false
            result.onSuccess { resolved ->
                selectedState = resolved.state
                selectedCity = resolved.city
                selectedLocality = resolved.locality
                selectedPinCode = resolved.pinCode
                pinSearchInput = resolved.pinCode
                resolvedPostOffices = resolved.postOffices
                statusMessage = "Detected: ${resolved.city}, ${resolved.locality} (${resolved.pinCode}) • ${resolved.state}"
            }.onFailure { err ->
                statusMessage = null
                errorMessage = err.localizedMessage ?: "Could not detect GPS location."
            }
        }
    }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            triggerLiveGpsResolution()
        } else {
            errorMessage = "Location permission denied. You can search any Indian State, City, Locality, or PIN code below."
        }
    }

    fun triggerPinLookup(pinToLookup: String) {
        val clean = pinToLookup.filter { it.isDigit() }.take(6)
        if (clean.length != 6) {
            errorMessage = "Please enter a valid 6-digit Indian PIN code."
            return
        }
        isResolvingPin = true
        errorMessage = null
        scope.launch {
            val result = IndiaLocationDirectory.resolvePinCode(clean)
            isResolvingPin = false
            result.onSuccess { resolved ->
                selectedState = resolved.state
                selectedCity = resolved.city
                selectedLocality = resolved.locality
                selectedPinCode = resolved.pinCode
                resolvedPostOffices = resolved.postOffices
                statusMessage = "PIN ${resolved.pinCode}: ${resolved.city}, ${resolved.state} (${resolved.source})"
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Could not resolve PIN code."
            }
        }
    }

    val matchingCities = remember(searchQuery, stateFilterChip) {
        IndiaLocationDirectory.searchCities(
            query = searchQuery,
            selectedState = stateFilterChip
        )
    }

    val currentCityLocalities = remember(selectedCity, resolvedPostOffices) {
        if (resolvedPostOffices.isNotEmpty()) {
            (resolvedPostOffices + IndiaLocationDirectory.localitiesForCity(selectedCity)).distinct()
        } else {
            IndiaLocationDirectory.localitiesForCity(selectedCity)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("all_india_location_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = SkillBlue,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = if (isHindi) "अखिल भारतीय स्थान चुनें (All-India Location)" else "Select All-India Location",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "28 States • 8 Union Territories • Live PIN & GPS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "Close Location Picker")
                    }
                }

                // Active Selection Banner + GPS Auto-Detect Button
                Surface(
                    color = SkillBlue.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SkillBlue.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedCity.equals("All India", ignoreCase = true)) {
                                        "Selected: All India (Nationwide)"
                                    } else {
                                        buildString {
                                            append(selectedCity)
                                            if (selectedLocality.isNotBlank() && selectedLocality != "All Localities") {
                                                append(" • $selectedLocality")
                                            }
                                            if (selectedState.isNotBlank() && selectedState != "All States & UTs") {
                                                append(", $selectedState")
                                            }
                                            if (selectedPinCode.isNotBlank()) {
                                                append(" - $selectedPinCode")
                                            }
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SkillBlue
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val hasCoarse = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    val hasFine = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasCoarse || hasFine) {
                                        triggerLiveGpsResolution()
                                    } else {
                                        gpsPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                enabled = !isResolvingGps,
                                colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("detect_gps_location_button")
                            ) {
                                if (isResolvingGps) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Detecting...", style = MaterialTheme.typography.labelMedium)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "GPS से मेरा स्थान पहचानें" else "Auto-Detect via GPS",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (allowAllIndia) {
                                OutlinedButton(
                                    onClick = {
                                        selectedState = ""
                                        selectedCity = "All India"
                                        selectedLocality = "All Localities"
                                        selectedPinCode = ""
                                        pinSearchInput = ""
                                        statusMessage = "Set to All India (Nationwide services & jobs)"
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(40.dp)
                                        .testTag("select_all_india_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("All India", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                if (statusMessage != null) {
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = SkillSuccessGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Search by State, District/City, Locality + 6-Digit PIN Code Lookup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { q ->
                            searchQuery = q
                            // If user types a 6-digit PIN directly in the search bar, resolve it automatically
                            val digits = q.trim().filter { it.isDigit() }
                            if (digits.length == 6 && q.trim().length == 6) {
                                pinSearchInput = digits
                                triggerPinLookup(digits)
                            }
                        },
                        label = {
                            Text(
                                if (isHindi) "राज्य, शहर, लोकैलिटी या पिन कोड खोजें"
                                else "Search State, City, Locality or PIN"
                            )
                        },
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
                            .weight(1.35f)
                            .testTag("all_india_search_input")
                    )

                    OutlinedTextField(
                        value = pinSearchInput,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() }.take(6)
                            pinSearchInput = clean
                            selectedPinCode = clean
                            if (clean.length == 6) {
                                triggerPinLookup(clean)
                            }
                        },
                        label = { Text("6-Digit PIN") },
                        trailingIcon = {
                            if (isResolvingPin) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                IconButton(
                                    onClick = { triggerPinLookup(pinSearchInput) },
                                    modifier = Modifier.testTag("lookup_pincode_button")
                                ) {
                                    Icon(
                                        Icons.Default.PinDrop,
                                        contentDescription = "Lookup PIN",
                                        tint = SkillPurple
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("all_india_pincode_input")
                    )
                }

                // State / Union Territory Horizontal Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = stateFilterChip == "All States & UTs",
                        onClick = { stateFilterChip = "All States & UTs" },
                        label = { Text("All 28 States & 8 UTs") }
                    )
                    IndiaLocationDirectory.allStatesAndUTs.forEach { stateEntry ->
                        FilterChip(
                            selected = stateFilterChip.equals(stateEntry.name, ignoreCase = true),
                            onClick = {
                                stateFilterChip = stateEntry.name
                                selectedState = stateEntry.name
                            },
                            label = {
                                Text(
                                    if (stateEntry.isUnionTerritory) "${stateEntry.name} (UT)"
                                    else stateEntry.name
                                )
                            }
                        )
                    }
                }

                // Scrollable List of Matching Indian Cities / Districts
                Text(
                    text = "Select District / City (${matchingCities.size} available):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 195.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentPadding = PaddingValues(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(matchingCities, key = { "${it.stateCode}_${it.name}" }) { cityEntry ->
                        val isSelected = selectedCity.equals(cityEntry.name, ignoreCase = true)
                        Surface(
                            color = if (isSelected) SkillBlue.copy(alpha = 0.12f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCity = cityEntry.name
                                    selectedState = cityEntry.state
                                    selectedPinCode = cityEntry.defaultPin
                                    pinSearchInput = cityEntry.defaultPin
                                    resolvedPostOffices = emptyList()
                                    if (selectedLocality.isBlank() ||
                                        selectedLocality == "All Localities" ||
                                        selectedLocality !in cityEntry.localities
                                    ) {
                                        selectedLocality = cityEntry.localities.firstOrNull().orEmpty()
                                    }
                                }
                                .testTag("city_option_${cityEntry.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${cityEntry.name}, ${cityEntry.state}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) SkillBlue else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Popular: ${cityEntry.localities.take(4).joinToString(", ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "PIN ${cityEntry.defaultPin}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SkillPurple,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = SkillBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Locality Chips + Custom Locality Input
                if (!selectedCity.equals("All India", ignoreCase = true)) {
                    if (currentCityLocalities.isNotEmpty()) {
                        Text(
                            text = "Select Locality / Post Office in $selectedCity:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currentCityLocalities.forEach { loc ->
                                FilterChip(
                                    selected = selectedLocality.equals(loc, ignoreCase = true),
                                    onClick = { selectedLocality = loc },
                                    label = { Text(loc) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = selectedLocality,
                        onValueChange = { selectedLocality = it },
                        label = { Text("Locality / Area / Neighbourhood in $selectedCity") },
                        placeholder = { Text("Enter or pick locality...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("all_india_locality_input")
                    )
                }

                HorizontalDivider()

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_all_india_location_button")
                    ) {
                        Text(if (isHindi) "रद्द करें" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalState = if (selectedCity.equals("All India", ignoreCase = true)) {
                                ""
                            } else {
                                selectedState.takeIf { it.isNotBlank() && it != "All States & UTs" }
                                    ?: IndiaLocationDirectory.stateForCity(selectedCity)
                            }
                            val finalPin = if (selectedCity.equals("All India", ignoreCase = true)) {
                                ""
                            } else {
                                selectedPinCode.ifBlank { IndiaLocationDirectory.defaultPinForCity(selectedCity) }
                            }
                            onLocationSelected(
                                finalState,
                                selectedCity,
                                selectedLocality.trim(),
                                finalPin
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                        modifier = Modifier.testTag("confirm_all_india_location_button")
                    ) {
                        Text(
                            text = if (isHindi) "स्थान लागू करें" else "Apply Location",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable inline All-India Location Selector Field for forms (Home, Explore, Auth, Onboarding, Profile, Create Service).
 */
@Composable
fun AllIndiaLocationSelectorField(
    city: String,
    locality: String = "",
    stateName: String = "",
    pinCode: String = "",
    label: String = "Location in India (State, City, Locality & PIN)",
    allowAllIndia: Boolean = false,
    showLocalityAndPinInputs: Boolean = false,
    isHindi: Boolean = false,
    testTag: String = "all_india_location_selector_field",
    onLocationChanged: (state: String, city: String, locality: String, pinCode: String) -> Unit
) {
    var showPickerDialog by remember { mutableStateOf(false) }

    val resolvedState = remember(city, stateName) {
        if (city.equals("All India", ignoreCase = true)) ""
        else stateName.ifBlank { IndiaLocationDirectory.stateForCity(city) }
    }
    val resolvedPin = remember(city, pinCode) {
        if (city.equals("All India", ignoreCase = true)) ""
        else pinCode.ifBlank { IndiaLocationDirectory.defaultPinForCity(city) }
    }

    val displaySummary = remember(city, locality, resolvedState, resolvedPin) {
        if (city.equals("All India", ignoreCase = true)) {
            "All India (All 28 States & 8 Union Territories)"
        } else {
            buildString {
                append(city)
                if (locality.isNotBlank() && locality != "All Localities") {
                    append(", $locality")
                }
                if (resolvedState.isNotBlank()) {
                    append(" • $resolvedState")
                }
                if (resolvedPin.isNotBlank()) {
                    append(" ($resolvedPin)")
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = displaySummary,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = SkillPurple
                    )
                },
                trailingIcon = {
                    TextButton(onClick = { showPickerDialog = true }) {
                        Text(
                            text = if (isHindi) "बदलें" else "Change",
                            color = SkillBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag)
            )
            // Transparent clickable overlay on the main body so tapping anywhere opens the All-India Picker Dialog
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showPickerDialog = true }
            )
        }

        if (showLocalityAndPinInputs && !city.equals("All India", ignoreCase = true)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = locality,
                    onValueChange = { newLoc ->
                        onLocationChanged(resolvedState, city, newLoc, resolvedPin)
                    },
                    label = { Text(if (isHindi) "लोकैलिटी / एरिया" else "Locality / Area") },
                    singleLine = true,
                    modifier = Modifier.weight(1.25f)
                )
                OutlinedTextField(
                    value = pinCode,
                    onValueChange = { newPin ->
                        val cleanPin = newPin.filter { it.isDigit() }.take(6)
                        onLocationChanged(resolvedState, city, locality, cleanPin)
                    },
                    label = { Text("PIN Code") },
                    singleLine = true,
                    modifier = Modifier.weight(0.75f)
                )
            }
        }
    }

    if (showPickerDialog) {
        AllIndiaLocationPickerDialog(
            initialCity = city,
            initialLocality = locality,
            initialState = resolvedState,
            initialPinCode = resolvedPin,
            allowAllIndia = allowAllIndia,
            isHindi = isHindi,
            onDismiss = { showPickerDialog = false },
            onLocationSelected = { newState, newCity, newLocality, newPin ->
                showPickerDialog = false
                onLocationChanged(newState, newCity, newLocality, newPin)
            }
        )
    }
}
