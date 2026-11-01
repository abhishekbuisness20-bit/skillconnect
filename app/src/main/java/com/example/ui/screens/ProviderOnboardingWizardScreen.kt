package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MarketplaceCatalog
import com.example.data.model.PortfolioProjectItem
import com.example.data.model.UserProfile
import com.example.data.model.VerificationSubmission
import com.example.data.model.encodePortfolioProjects
import com.example.ui.components.AllIndiaLocationSelectorField
import com.example.ui.components.SkillConnectBrandLogo
import com.example.ui.theme.SkillBlue
import com.example.ui.theme.SkillCyan
import com.example.ui.theme.SkillErrorRed
import com.example.ui.theme.SkillNavy
import com.example.ui.theme.SkillSuccessGreen
import com.example.ui.theme.SkillWarningAmber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProviderOnboardingWizardScreen(
    profile: UserProfile?,
    userEmail: String,
    fallbackName: String,
    existingVerification: VerificationSubmission?,
    onSaveStepDraft: (UserProfile, Int) -> Unit,
    onCompleteWizard: (
        finalProfile: UserProfile,
        idDocumentType: String,
        idDocumentNumberMasked: String,
        portfolioSummary: String,
        firstServiceTitle: String,
        firstServiceDesc: String
    ) -> Unit,
    onExitWizard: () -> Unit
) {
    val baseProfile = profile ?: UserProfile(
        displayName = fallbackName,
        role = "provider",
        city = "Bengaluru",
        serviceState = "Karnataka"
    )

    var currentStep by remember(baseProfile.onboardingStep) {
        mutableIntStateOf(baseProfile.onboardingStep.toInt().coerceIn(1, 8))
    }
    var isPreviewOpen by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Step 1 — Account Details
    var legalName by remember(baseProfile, existingVerification) {
        mutableStateOf(
            existingVerification?.legalName?.ifBlank { baseProfile.displayName.ifBlank { fallbackName } }
                ?: baseProfile.displayName.ifBlank { fallbackName }
        )
    }
    var displayName by remember(baseProfile) {
        mutableStateOf(baseProfile.displayName.ifBlank { fallbackName })
    }
    var mobileNumber by remember(baseProfile) { mutableStateOf(baseProfile.publicPhone) }
    var acceptedTerms by remember { mutableStateOf(true) }

    // Step 2 — Professional Details
    var headline by remember(baseProfile) { mutableStateOf(baseProfile.headline) }
    var primaryCategory by remember(baseProfile) {
        mutableStateOf(baseProfile.primaryCategory.ifBlank { "Graphic Design" })
    }
    var secondaryCategoriesText by remember(baseProfile) {
        mutableStateOf(baseProfile.subcategories)
    }
    var skillsText by remember(baseProfile) {
        mutableStateOf(baseProfile.skills.joinToString(", "))
    }
    var experienceYearsText by remember(baseProfile) {
        mutableStateOf(baseProfile.experienceYears.coerceAtLeast(1L).toString())
    }
    var bio by remember(baseProfile) { mutableStateOf(baseProfile.bio) }
    var languagesSpokenText by remember(baseProfile) {
        mutableStateOf(baseProfile.languagesSpoken.joinToString(", ").ifBlank { "English, Hindi" })
    }

    // Step 3 — Service Information
    var serviceTitle by remember { mutableStateOf("") }
    var serviceDescription by remember { mutableStateOf("") }
    var startingPriceText by remember(baseProfile) {
        mutableStateOf((baseProfile.startingPrice.takeIf { it > 0L } ?: 999L).toString())
    }
    var pricingType by remember(baseProfile) {
        mutableStateOf(baseProfile.pricingModel.ifBlank { "fixed" })
    }
    var turnaroundTime by remember { mutableStateOf("2–3 Days") }
    var serviceMode by remember(baseProfile) {
        mutableStateOf(
            when {
                baseProfile.remoteAvailable && baseProfile.inPersonAvailable -> "Hybrid"
                baseProfile.remoteAvailable -> "Online"
                else -> "At Customer Location"
            }
        )
    }

    // Step 4 — Service Location
    var stateName by remember(baseProfile) {
        mutableStateOf(baseProfile.serviceState.ifBlank { "Karnataka" })
    }
    var city by remember(baseProfile) {
        mutableStateOf(baseProfile.city.ifBlank { "Bengaluru" })
    }
    var locality by remember(baseProfile) {
        mutableStateOf(baseProfile.locality.ifBlank { "Indiranagar" })
    }
    var pincode by remember(baseProfile) {
        mutableStateOf(baseProfile.pinCode.ifBlank { "560038" })
    }
    var serviceRadiusText by remember(baseProfile) {
        mutableStateOf(baseProfile.serviceAreaRadius.ifBlank { "Within 15 km" })
    }
    var remoteAvailable by remember(baseProfile) {
        mutableStateOf(baseProfile.remoteAvailable)
    }

    // Step 5 — Profile Photo & Portfolio (Zero-permission Android Photo Picker)
    var profilePhotoUri by remember(baseProfile) { mutableStateOf(baseProfile.avatarUri) }
    var coverBannerUri by remember(baseProfile) { mutableStateOf(baseProfile.coverUri) }
    val portfolioList = remember(baseProfile.portfolioSummaryJson) {
        mutableStateListOf<PortfolioProjectItem>().apply {
            addAll(baseProfile.parsePortfolioProjects())
        }
    }
    var newProjectTitle by remember { mutableStateOf("") }
    var newProjectDesc by remember { mutableStateOf("") }
    var newProjectImageUri by remember { mutableStateOf("") }

    val profilePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            profilePhotoUri = uri.toString()
        }
    }

    val coverBannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coverBannerUri = uri.toString()
        }
    }

    val portfolioImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            newProjectImageUri = uri.toString()
        }
    }

    // Step 6 — Contact & Business Information
    var businessName by remember(baseProfile) { mutableStateOf(baseProfile.businessName) }
    var businessType by remember(baseProfile) {
        mutableStateOf(baseProfile.businessType.ifBlank { "Individual Freelancer" })
    }
    var qualifications by remember(baseProfile) { mutableStateOf(baseProfile.qualifications) }
    var workingDays by remember { mutableStateOf("Mon–Sat") }
    var workingHours by remember { mutableStateOf("9:00 AM – 7:00 PM") }
    var preferredContactMethod by remember { mutableStateOf("In-App Chat") }
    var publicPhoneConsent by remember(baseProfile) {
        mutableStateOf(baseProfile.publicPhone.isNotBlank())
    }

    // Step 7 — Identity & Verification
    var govIdType by remember { mutableStateOf("Aadhaar Masked") }
    var govIdMaskedRef by remember(existingVerification) {
        mutableStateOf(existingVerification?.documentReferenceNote ?: "XXXX-XXXX-4821")
    }
    var selfieConfirmed by remember { mutableStateOf(true) }
    var addressConfirmed by remember { mutableStateOf(true) }
    var certificateRef by remember { mutableStateOf("") }
    var verificationConsent by remember { mutableStateOf(true) }

    var catExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }

    fun buildCurrentDraftProfile(): UserProfile {
        val parsedSkills = skillsText.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(10)
        val parsedLanguages = languagesSpokenText.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(6)
        val encodedPortfolio = encodePortfolioProjects(portfolioList)
        return baseProfile.copy(
            displayName = displayName.trim().ifBlank { legalName.trim() },
            headline = headline.trim(),
            bio = bio.trim(),
            role = "provider",
            primaryCategory = primaryCategory,
            subcategories = secondaryCategoriesText.trim(),
            skills = parsedSkills,
            experienceYears = experienceYearsText.toLongOrNull() ?: 1L,
            languagesSpoken = parsedLanguages,
            startingPrice = startingPriceText.toLongOrNull() ?: 0L,
            pricingModel = pricingType,
            remoteAvailable = remoteAvailable || serviceMode in listOf("Online", "Hybrid"),
            inPersonAvailable = serviceMode in listOf("At Customer Location", "At Provider Location", "Hybrid"),
            serviceState = stateName.trim().ifBlank { "Karnataka" },
            city = city.trim().ifBlank { "Bengaluru" },
            locality = locality.trim(),
            pinCode = pincode.trim(),
            serviceAreaRadius = serviceRadiusText.trim().ifBlank { "Within 15 km" },
            avatarUri = profilePhotoUri,
            coverUri = coverBannerUri,
            portfolioSummaryJson = encodedPortfolio,
            businessName = businessName.trim(),
            businessType = businessType.trim(),
            qualifications = qualifications.trim(),
            availability = "$workingDays, $workingHours",
            publicPhone = if (publicPhoneConsent) mobileNumber.trim() else "",
            publicListingConsent = true,
            isPublicProvider = true,
            onboardingStep = currentStep.toLong()
        )
    }

    fun validateCurrentStep(): Boolean {
        validationError = null
        when (currentStep) {
            1 -> {
                if (legalName.trim().length < 2) {
                    validationError = "Please enter your full legal name (at least 2 characters)."
                    return false
                }
                if (displayName.trim().length < 2) {
                    validationError = "Please enter your public display name."
                    return false
                }
                if (!acceptedTerms) {
                    validationError = "Please accept the Provider Terms & Privacy Consent."
                    return false
                }
            }
            2 -> {
                if (headline.trim().length < 4) {
                    validationError = "Please enter a professional headline (at least 4 characters)."
                    return false
                }
                if (skillsText.split(",").map { it.trim() }.none { it.isNotBlank() }) {
                    validationError = "Please enter at least one skill tag."
                    return false
                }
                if (bio.trim().length < 15) {
                    validationError = "Please write a professional bio (at least 15 characters)."
                    return false
                }
            }
            3 -> {
                if (serviceTitle.trim().length < 3) {
                    validationError = "Please enter your first service title (at least 3 characters)."
                    return false
                }
                if (serviceDescription.trim().length < 10) {
                    validationError = "Please describe your first service (at least 10 characters)."
                    return false
                }
            }
            4 -> {
                if (city.trim().isEmpty()) {
                    validationError = "Please select your primary city in India."
                    return false
                }
                if (locality.trim().isEmpty()) {
                    validationError = "Please enter your service area or locality."
                    return false
                }
            }
            7 -> {
                if (govIdMaskedRef.trim().length < 4) {
                    validationError = "Please enter a masked ID reference (last 4 digits)."
                    return false
                }
                if (!verificationConsent) {
                    validationError = "Please confirm the Verification Consent checkbox."
                    return false
                }
            }
        }
        return true
    }

    val stepTitles = listOf(
        "1. Account Details",
        "2. Professional Details",
        "3. Service Information",
        "4. Service Location",
        "5. Profile Photo & Portfolio",
        "6. Contact & Business Info",
        "7. Identity & Verification",
        "8. Review & Submit"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("provider_onboarding_wizard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card with Progress & Save Draft
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SkillNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SkillConnectBrandLogo(compact = true, onNavyHeader = true)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = SkillCyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "PROVIDER ID: ${baseProfile.effectivePlatformId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkillCyan,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "8-Step Provider Onboarding Wizard",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Step $currentStep of 8 • ${stepTitles[currentStep - 1]}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                onSaveStepDraft(buildCurrentDraftProfile(), currentStep)
                                onExitWizard()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Resume Later",
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { currentStep / 8f },
                        color = SkillCyan,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )

                    // Horizontal Step Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        stepTitles.forEachIndexed { index, label ->
                            val stepNum = index + 1
                            val isSelected = currentStep == stepNum
                            val isDone = stepNum < currentStep
                            Surface(
                                color = when {
                                    isSelected -> SkillBlue
                                    isDone -> SkillSuccessGreen.copy(alpha = 0.28f)
                                    else -> Color.White.copy(alpha = 0.1f)
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.clickable { currentStep = stepNum }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Step Form Body
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (currentStep) {
                        1 -> {
                            Text("Step 1 — Account Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Your legal name and private email are kept confidential for verification and administrative safety.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = legalName,
                                onValueChange = { legalName = it },
                                label = { Text("Full Legal Name *") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wizard_legal_name_input")
                            )
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Public Display Name *") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wizard_display_name_input")
                            )
                            OutlinedTextField(
                                value = userEmail,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Verified Google Account Email (Private)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = mobileNumber,
                                onValueChange = { mobileNumber = it },
                                label = { Text("Indian Mobile Number (+91 ...)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { acceptedTerms = !acceptedTerms }
                            ) {
                                Checkbox(checked = acceptedTerms, onCheckedChange = { acceptedTerms = it })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "I accept the SkillConnect Service Provider Terms, Zero-Fake-Review Policy, and Privacy Policy.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        2 -> {
                            Text("Step 2 — Professional Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = headline,
                                onValueChange = { headline = it },
                                label = { Text("Professional Title / Headline *") },
                                placeholder = { Text("e.g. Certified Electrician & Smart Home Installer") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wizard_headline_input")
                            )

                            ExposedDropdownMenuBox(
                                expanded = catExpanded,
                                onExpandedChange = { catExpanded = !catExpanded }
                            ) {
                                OutlinedTextField(
                                    value = primaryCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Primary Skill Category *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = catExpanded,
                                    onDismissRequest = { catExpanded = false }
                                ) {
                                    MarketplaceCatalog.categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat.nameEn) },
                                            onClick = {
                                                primaryCategory = cat.id
                                                catExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = secondaryCategoriesText,
                                onValueChange = { secondaryCategoriesText = it },
                                label = { Text("Secondary Categories / Subcategories (optional)") },
                                placeholder = { Text("e.g. Plumbing, Appliance Repair") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = skillsText,
                                onValueChange = { skillsText = it },
                                label = { Text("Skills Tags (comma-separated, max 10) *") },
                                placeholder = { Text("e.g. Wiring, MCB Repair, Inverter Setup") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wizard_skills_input")
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = experienceYearsText,
                                    onValueChange = { txt -> experienceYearsText = txt.filter { ch -> ch.isDigit() } },
                                    label = { Text("Years of Experience *") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = languagesSpokenText,
                                    onValueChange = { languagesSpokenText = it },
                                    label = { Text("Languages Spoken *") },
                                    placeholder = { Text("English, Hindi, Kannada") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.4f)
                                )
                            }

                            OutlinedTextField(
                                value = bio,
                                onValueChange = { bio = it },
                                label = { Text("Professional Bio & Experience Summary *") },
                                minLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wizard_bio_input")
                            )
                        }

                        3 -> {
                            Text("Step 3 — First Service Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = serviceTitle,
                                onValueChange = { serviceTitle = it },
                                label = { Text("First Service Title *") },
                                placeholder = { Text("e.g. Complete Home Electrical Inspection & Fault Repair") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wizard_service_title_input")
                            )
                            OutlinedTextField(
                                value = serviceDescription,
                                onValueChange = { serviceDescription = it },
                                label = { Text("Service Description & Deliverables *") },
                                minLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wizard_service_desc_input")
                            )
                            Text("Pricing Type *", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "fixed" to "Fixed Price",
                                    "hourly" to "Hourly",
                                    "project" to "Per Project",
                                    "quote" to "Custom Quote"
                                ).forEach { (key, label) ->
                                    FilterChip(
                                        selected = pricingType == key,
                                        onClick = { pricingType = key },
                                        label = { Text(label) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = startingPriceText,
                                    onValueChange = { txt -> startingPriceText = txt.filter { ch -> ch.isDigit() } },
                                    label = { Text("Starting Price in INR (₹) *") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = turnaroundTime,
                                    onValueChange = { turnaroundTime = it },
                                    label = { Text("Estimated Completion Time") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text("Service Mode *", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "Online",
                                    "At Customer Location",
                                    "At Provider Location",
                                    "Hybrid"
                                ).forEach { mode ->
                                    FilterChip(
                                        selected = serviceMode == mode,
                                        onClick = { serviceMode = mode },
                                        label = { Text(mode) }
                                    )
                                }
                            }
                        }

                        4 -> {
                            Text("Step 4 — Service Location", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = "India",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Country") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.8f)
                                )
                                OutlinedTextField(
                                    value = stateName,
                                    onValueChange = { stateName = it },
                                    label = { Text("State *") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                            AllIndiaLocationSelectorField(
                                city = city,
                                locality = locality,
                                stateName = stateName,
                                pinCode = pincode,
                                label = "All-India Operating City, State, Locality & PIN *",
                                allowAllIndia = false,
                                testTag = "wizard_all_india_location_selector",
                                onLocationChanged = { newState, newCity, newLocality, newPin ->
                                    if (newState.isNotBlank()) stateName = newState
                                    city = newCity
                                    if (newLocality.isNotBlank()) locality = newLocality
                                    if (newPin.isNotBlank()) pincode = newPin
                                }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = locality,
                                    onValueChange = { locality = it },
                                    label = { Text("Area / Locality *") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                                OutlinedTextField(
                                    value = pincode,
                                    onValueChange = { txt -> pincode = txt.filter { ch -> ch.isDigit() }.take(6) },
                                    label = { Text("Pincode") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.8f)
                                )
                            }
                            OutlinedTextField(
                                value = serviceRadiusText,
                                onValueChange = { serviceRadiusText = it },
                                label = { Text("On-Site Service Area Radius") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Available for Remote / All-India Online Work")
                                Switch(checked = remoteAvailable, onCheckedChange = { remoteAvailable = it })
                            }
                        }

                        5 -> {
                            Text("Step 5 — Profile Photo & Portfolio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Select media using the zero-permission Android Photo Picker or add portfolio project highlights (up to 6 items).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = {
                                        profilePhotoLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (profilePhotoUri.isBlank()) "Profile Photo" else "Photo Selected ✓",
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        coverBannerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (coverBannerUri.isBlank()) "Cover Banner" else "Banner Selected ✓",
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            HorizontalDivider()
                            Text("Add Portfolio Sample (${portfolioList.size}/6)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = newProjectTitle,
                                onValueChange = { newProjectTitle = it },
                                label = { Text("Project Title") },
                                placeholder = { Text("e.g. E-Commerce Brand Redesign for Local Retailer") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newProjectDesc,
                                onValueChange = { newProjectDesc = it },
                                label = { Text("Project Description & Outcome") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = {
                                        portfolioImageLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = if (newProjectImageUri.isBlank()) "Attach Photo" else "Photo Attached ✓",
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (newProjectTitle.isNotBlank() && portfolioList.size < 6) {
                                            portfolioList.add(
                                                PortfolioProjectItem(
                                                    id = "proj_${System.currentTimeMillis()}",
                                                    title = newProjectTitle.trim(),
                                                    description = newProjectDesc.trim(),
                                                    skillsUsed = primaryCategory,
                                                    imageUri = newProjectImageUri.ifBlank { "sample_portfolio_asset" },
                                                    projectLink = ""
                                                )
                                            )
                                            newProjectTitle = ""
                                            newProjectDesc = ""
                                            newProjectImageUri = ""
                                        }
                                    },
                                    enabled = newProjectTitle.isNotBlank() && portfolioList.size < 6,
                                    colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Add to Portfolio",
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            portfolioList.forEachIndexed { idx, item ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${idx + 1}. ${item.title}", fontWeight = FontWeight.Bold)
                                            Text(item.description, style = MaterialTheme.typography.bodySmall)
                                        }
                                        IconButton(onClick = { portfolioList.removeAt(idx) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = SkillErrorRed)
                                        }
                                    }
                                }
                            }
                        }

                        6 -> {
                            Text("Step 6 — Contact & Business Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = businessName,
                                onValueChange = { businessName = it },
                                label = { Text("Business / Studio / Brand Name (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = businessType,
                                onValueChange = { businessType = it },
                                label = { Text("Business Type (Individual / Agency / Firm)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = qualifications,
                                onValueChange = { qualifications = it },
                                label = { Text("Qualifications / Website / Portfolio Link (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = workingDays,
                                    onValueChange = { workingDays = it },
                                    label = { Text("Working Days") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = workingHours,
                                    onValueChange = { workingHours = it },
                                    label = { Text("Working Hours") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text("Preferred Contact Method", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("In-App Chat", "Booking Request", "Public Phone (Consented)").forEach { method ->
                                    FilterChip(
                                        selected = preferredContactMethod == method,
                                        onClick = { preferredContactMethod = method },
                                        label = { Text(method) }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Display Contact Phone on Public Profile", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Private by default. Only shown publicly if you consent.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(checked = publicPhoneConsent, onCheckedChange = { publicPhoneConsent = it })
                            }
                        }

                        7 -> {
                            Text("Step 7 — Identity & Verification", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Verification details are stored in a private collection accessible only to you and the platform administrator. They are never exposed on public profiles.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("Government ID Type *", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Aadhaar Masked", "PAN", "Driving Licence", "Passport").forEach { idType ->
                                    FilterChip(
                                        selected = govIdType == idType,
                                        onClick = { govIdType = idType },
                                        label = { Text(idType) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = govIdMaskedRef,
                                onValueChange = { govIdMaskedRef = it },
                                label = { Text("Masked ID Reference (Last 4 Digits Only) *") },
                                placeholder = { Text("e.g. XXXX-XXXX-4821") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wizard_masked_id_input")
                            )
                            OutlinedTextField(
                                value = certificateRef,
                                onValueChange = { certificateRef = it },
                                label = { Text("Skill Certificate / GST / Trade Proof Reference (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { selfieConfirmed = !selfieConfirmed }
                            ) {
                                Checkbox(checked = selfieConfirmed, onCheckedChange = { selfieConfirmed = it })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Live Selfie / Profile Photo Identity Match Confirmed", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { addressConfirmed = !addressConfirmed }
                            ) {
                                Checkbox(checked = addressConfirmed, onCheckedChange = { addressConfirmed = it })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Local Presence & Operating City ($city) Confirmed", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { verificationConsent = !verificationConsent }
                            ) {
                                Checkbox(checked = verificationConsent, onCheckedChange = { verificationConsent = it })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "I consent to SkillConnect Admin reviewing my verification submission. I understand the 'Verified by SkillConnect' badge is granted only after admin approval.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        8 -> {
                            val draft = buildCurrentDraftProfile()
                            val checklist = draft.buildCompletionChecklist(hasPublishedService = serviceTitle.isNotBlank())
                            val pct = draft.completionPercentage(hasPublishedService = serviceTitle.isNotBlank())

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Step 8 — Review & Submit",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { isPreviewOpen = !isPreviewOpen },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPreviewOpen) "Hide Preview" else "Preview Profile",
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Surface(
                                color = SkillBlue.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Profile Completion Checklist",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SkillBlue,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            color = if (pct == 100) SkillSuccessGreen.copy(alpha = 0.16f) else SkillBlue.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "$pct%",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (pct == 100) SkillSuccessGreen else SkillBlue,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    LinearProgressIndicator(
                                        progress = { pct / 100f },
                                        color = if (pct == 100) SkillSuccessGreen else SkillBlue,
                                        trackColor = SkillBlue.copy(alpha = 0.18f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(50))
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        checklist.forEach { check ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = if (check.isDone) SkillSuccessGreen else SkillWarningAmber,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = check.label,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (check.isDone) FontWeight.SemiBold else FontWeight.Normal,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (isPreviewOpen) {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = SkillNavy),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("PUBLIC DIRECTORY PREVIEW", style = MaterialTheme.typography.labelSmall, color = SkillCyan, fontWeight = FontWeight.Bold)
                                        Text("${draft.displayName} • ${draft.effectivePlatformId}", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(draft.headline.ifBlank { "Professional on SkillConnect" }, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                                        Text("${draft.primaryCategory} • ${draft.city}, ${draft.locality} ($serviceMode)", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
                                        Text("First Service: ${serviceTitle.ifBlank { "Not specified" }} — ₹$startingPriceText ($pricingType)", style = MaterialTheme.typography.bodySmall, color = SkillCyan)
                                        Text("Status Badge Upon Submission: Verification Pending (Publicly Discoverable)", style = MaterialTheme.typography.labelSmall, color = SkillWarningAmber)
                                    }
                                }
                            }

                            Text(
                                text = "Upon clicking 'Submit for Verification', your provider profile and first service listing will be saved to Firestore, your verification status will transition to 'Pending Verification', and your profile will be discoverable in the Public Provider Directory with a transparent 'Verification Pending' status label until approved by Admin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (validationError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = validationError!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    // Bottom Wizard Action Buttons: Stack Primary Submit/Next Full-Width Below Previous & Save Draft Row
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (currentStep > 1) {
                                OutlinedButton(
                                    onClick = {
                                        validationError = null
                                        currentStep -= 1
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("wizard_previous_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Previous",
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    onSaveStepDraft(buildCurrentDraftProfile(), currentStep)
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("wizard_save_draft_btn")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Save Draft",
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (currentStep < 8) {
                            Button(
                                onClick = {
                                    if (validateCurrentStep()) {
                                        val next = currentStep + 1
                                        onSaveStepDraft(buildCurrentDraftProfile(), next)
                                        currentStep = next
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SkillBlue),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("wizard_next_step_btn")
                            ) {
                                Text(
                                    text = "Next Step",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (validateCurrentStep()) {
                                        val finalDraft = buildCurrentDraftProfile()
                                        val portfolioSummaryText = portfolioList.joinToString("; ") { "${it.title}: ${it.description}" }
                                            .ifBlank { finalDraft.bio }
                                        onCompleteWizard(
                                            finalDraft,
                                            govIdType,
                                            govIdMaskedRef,
                                            portfolioSummaryText,
                                            serviceTitle.ifBlank { "$primaryCategory Professional Service" },
                                            serviceDescription.ifBlank { bio.ifBlank { "Professional $primaryCategory service in $city." } }
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SkillSuccessGreen),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("wizard_submit_verification_btn")
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Submit for Verification",
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
