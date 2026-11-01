package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.MarketplaceCatalog
import com.example.data.model.ProjectAttachmentMeta
import com.example.data.model.ProjectSubmission
import com.example.data.model.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ALLOWED_EXTENSIONS = setOf(
    "jpg", "jpeg", "png", "webp",
    "mp4", "mov", "webm",
    "pdf", "doc", "docx", "txt", "xls", "xlsx"
)

private const val MAX_FILE_SIZE_BYTES = 25L * 1024L * 1024L // 25 MB

private fun inspectAttachmentUri(
    context: Context,
    uri: Uri,
    uploadedBy: String,
    projectScope: String
): Pair<ProjectAttachmentMeta?, String?> {
    return try {
        val resolver = context.contentResolver
        var displayName = "attachment_${System.currentTimeMillis()}"
        var fileSize = 0L

        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) {
                    val name = cursor.getString(nameIdx)
                    if (!name.isNullOrBlank()) displayName = name
                }
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    fileSize = cursor.getLong(sizeIdx)
                }
            }
        }

        val mimeType = resolver.getType(uri)?.lowercase(Locale.ROOT) ?: ""
        val ext = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)

        val effectiveExt = when {
            ext.isNotBlank() -> ext
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
            mimeType.contains("png") -> "png"
            mimeType.contains("webp") -> "webp"
            mimeType.contains("mp4") -> "mp4"
            mimeType.contains("quicktime") || mimeType.contains("mov") -> "mov"
            mimeType.contains("webm") -> "webm"
            mimeType.contains("pdf") -> "pdf"
            mimeType.contains("msword") -> "doc"
            mimeType.contains("wordprocessingml") -> "docx"
            mimeType.contains("plain") -> "txt"
            mimeType.contains("excel") || mimeType.contains("spreadsheetml") -> "xlsx"
            else -> ""
        }

        if (effectiveExt !in ALLOWED_EXTENSIONS) {
            return null to "Unsupported file format ($displayName). Allowed: JPG, PNG, WEBP, MP4, MOV, WEBM, PDF, DOC, DOCX, TXT, XLS, XLSX."
        }

        if (fileSize > MAX_FILE_SIZE_BYTES) {
            return null to "File '$displayName' exceeds the 25 MB maximum size limit."
        }

        val categoryType = when (effectiveExt) {
            "jpg", "jpeg", "png", "webp" -> "image/$effectiveExt"
            "mp4", "mov", "webm" -> "video/$effectiveExt"
            else -> "document/$effectiveExt"
        }

        val finalFileName = if (displayName.contains('.')) displayName else "$displayName.$effectiveExt"
        val now = System.currentTimeMillis()
        val storagePath = "projects/$projectScope/${uploadedBy.ifBlank { "user" }}/${now}_$finalFileName"

        try {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Ignore if URI does not support persistable permission
        }

        ProjectAttachmentMeta(
            fileName = finalFileName,
            fileType = categoryType,
            fileSizeBytes = if (fileSize > 0L) fileSize else 1024L * 120L,
            storagePath = storagePath,
            downloadURL = uri.toString(),
            uploadedBy = uploadedBy,
            uploadedAtMs = now
        ) to null
    } catch (e: Exception) {
        null to "Could not read selected file: ${e.localizedMessage ?: "Unknown error"}"
    }
}

private fun openOrShareAttachment(context: Context, attachment: ProjectAttachmentMeta) {
    try {
        if (attachment.downloadURL.isNotBlank()) {
            val uri = Uri.parse(attachment.downloadURL)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    uri,
                    when {
                        attachment.isImage -> "image/*"
                        attachment.isVideo -> "video/*"
                        attachment.fileName.endsWith(".pdf", true) -> "application/pdf"
                        else -> "*/*"
                    }
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(viewIntent, "Open ${attachment.fileName}"))
        } else {
            Toast.makeText(
                context,
                "Attachment: ${attachment.fileName} (${attachment.formattedSize})",
                Toast.LENGTH_SHORT
            ).show()
        }
    } catch (_: Exception) {
        Toast.makeText(
            context,
            "File ready: ${attachment.fileName} (${attachment.formattedSize}) • Path: ${attachment.storagePath}",
            Toast.LENGTH_LONG
        ).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProjectSubmissionDialog(
    preselectedProvider: UserProfile?,
    availableProviders: List<UserProfile>,
    initialServiceId: String = "",
    initialServiceTitle: String = "",
    initialCategory: String = "",
    customerUid: String,
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onSubmitProject: (
        providerId: String,
        providerName: String,
        serviceId: String,
        serviceCategory: String,
        projectTitle: String,
        projectDescription: String,
        projectRequirements: String,
        expectedDeliveryDate: String,
        budgetInr: Double,
        additionalInstructions: String,
        attachments: List<ProjectAttachmentMeta>
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedProvider by remember(preselectedProvider, availableProviders) {
        mutableStateOf(preselectedProvider ?: availableProviders.firstOrNull())
    }
    var providerExpanded by remember { mutableStateOf(false) }

    val defaultCategory = MarketplaceCatalog.categories.firstOrNull()?.nameEn ?: "Graphic Design"
    var selectedCategory by remember(initialCategory, selectedProvider) {
        mutableStateOf(
            initialCategory.ifBlank {
                selectedProvider?.primaryCategory?.ifBlank { defaultCategory } ?: defaultCategory
            }
        )
    }
    var categoryExpanded by remember { mutableStateOf(false) }

    var projectTitle by remember(initialServiceTitle) {
        mutableStateOf(if (initialServiceTitle.isNotBlank()) "Project: $initialServiceTitle" else "")
    }
    var projectDescription by remember { mutableStateOf("") }
    var projectRequirements by remember { mutableStateOf("") }
    var expectedDeliveryDate by remember { mutableStateOf("") }
    var budgetText by remember { mutableStateOf("") }
    var additionalInstructions by remember { mutableStateOf("") }

    var attachments by remember { mutableStateOf<List<ProjectAttachmentMeta>>(emptyList()) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var isUploadingFiles by remember { mutableStateOf(false) }
    var previewAttachment by remember { mutableStateOf<ProjectAttachmentMeta?>(null) }

    val visualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isUploadingFiles = true
                uploadProgress = 0.15f
                validationError = null
                val added = mutableListOf<ProjectAttachmentMeta>()
                uris.forEachIndexed { idx, uri ->
                    delay(90)
                    uploadProgress = ((idx + 1).toFloat() / uris.size.toFloat()).coerceIn(0.2f, 1f)
                    val (meta, err) = inspectAttachmentUri(context, uri, customerUid, "customer_submission")
                    if (err != null) {
                        validationError = err
                    } else if (meta != null) {
                        added.add(meta)
                    }
                }
                attachments = attachments + added
                delay(120)
                isUploadingFiles = false
                uploadProgress = 0f
            }
        }
    }

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isUploadingFiles = true
                uploadProgress = 0.15f
                validationError = null
                val added = mutableListOf<ProjectAttachmentMeta>()
                uris.forEachIndexed { idx, uri ->
                    delay(90)
                    uploadProgress = ((idx + 1).toFloat() / uris.size.toFloat()).coerceIn(0.2f, 1f)
                    val (meta, err) = inspectAttachmentUri(context, uri, customerUid, "customer_submission")
                    if (err != null) {
                        validationError = err
                    } else if (meta != null) {
                        added.add(meta)
                    }
                }
                attachments = attachments + added
                delay(120)
                isUploadingFiles = false
                uploadProgress = 0f
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 640.dp)
                .heightIn(max = 760.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(
                                    Icons.Default.AssignmentTurnedIn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(10.dp).size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Submit Your Project",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Send requirements, deadlines & reference files to your provider",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Form Content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Service Provider Selector
                    if (preselectedProvider != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.PersonPin,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Selected Service Provider",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = preselectedProvider.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (preselectedProvider.isStrictVerifiedBadgeActive) {
                                            Icon(
                                                Icons.Default.Verified,
                                                contentDescription = "Verified by SkillConnect",
                                                tint = Color(0xFF1D9BF0),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${preselectedProvider.primaryCategory} • ${preselectedProvider.city}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = providerExpanded,
                            onExpandedChange = { providerExpanded = !providerExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedProvider?.let {
                                    "${it.displayName} (${it.primaryCategory.ifBlank { "Service Provider" }})"
                                } ?: "Select Service Provider",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Service Provider *") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("project_provider_selector"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = providerExpanded,
                                onDismissRequest = { providerExpanded = false }
                            ) {
                                availableProviders.forEach { provider ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(provider.displayName, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    "${provider.primaryCategory} • ${provider.city}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedProvider = provider
                                            if (provider.primaryCategory.isNotBlank()) {
                                                selectedCategory = provider.primaryCategory
                                            }
                                            providerExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Service Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Service Category *") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("project_category_selector"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            MarketplaceCatalog.categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.nameEn) },
                                    onClick = {
                                        selectedCategory = cat.nameEn
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Project Title
                    OutlinedTextField(
                        value = projectTitle,
                        onValueChange = {
                            projectTitle = it
                            validationError = null
                        },
                        label = { Text("Project Title *") },
                        placeholder = { Text("e.g., E-Commerce Mobile App UI & Brand Kit") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Project Description
                    OutlinedTextField(
                        value = projectDescription,
                        onValueChange = {
                            projectDescription = it
                            validationError = null
                        },
                        label = { Text("Project Description *") },
                        placeholder = { Text("Describe your project goals, target audience, and scope...") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_description_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Project Requirements
                    OutlinedTextField(
                        value = projectRequirements,
                        onValueChange = {
                            projectRequirements = it
                            validationError = null
                        },
                        label = { Text("Detailed Project Requirements *") },
                        placeholder = { Text("List specific deliverables, formats, dimensions, or technical requirements...") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_requirements_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Expected Delivery Date & Optional Budget Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = expectedDeliveryDate,
                            onValueChange = {
                                expectedDeliveryDate = it
                                validationError = null
                            },
                            label = { Text("Expected Delivery Date *") },
                            placeholder = { Text("DD/MM/YYYY or 7 Days") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("project_delivery_date_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = budgetText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' }) {
                                    budgetText = input
                                }
                            },
                            label = { Text("Budget ₹ (Optional)") },
                            placeholder = { Text("e.g. 5000") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Icon(Icons.Default.CurrencyRupee, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("project_budget_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Additional Instructions
                    OutlinedTextField(
                        value = additionalInstructions,
                        onValueChange = { additionalInstructions = it },
                        label = { Text("Additional Instructions (Optional)") },
                        placeholder = { Text("Special instructions, preferred communication style, links, or notes...") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_instructions_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Multiple File Upload Section
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Reference Files & Attachments",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Images (JPG, PNG, WEBP) • Videos (MP4, MOV, WEBM) • Docs (PDF, DOC, DOCX, TXT, XLS, XLSX) • Max 25 MB/file",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${attachments.size} attached",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Upload Action Buttons (Responsive 2-button row, single-line text)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        visualMediaLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                        .testTag("attach_photos_videos_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PermMedia,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Photos & Videos",
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        documentLauncher.launch(
                                            arrayOf(
                                                "application/pdf",
                                                "application/msword",
                                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                                "text/plain",
                                                "application/vnd.ms-excel",
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                "image/*",
                                                "video/*"
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                        .testTag("attach_documents_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "PDF & Docs",
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            // Upload Progress Bar
                            AnimatedVisibility(visible = isUploadingFiles) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Validating & preparing files...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${(uploadProgress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { uploadProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }

                            // Attached File Previews
                            if (attachments.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    attachments.forEachIndexed { index, item ->
                                        ProjectAttachmentItemRow(
                                            attachment = item,
                                            onPreviewClick = { previewAttachment = item },
                                            onRemoveClick = {
                                                attachments = attachments.toMutableList().also { it.removeAt(index) }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Validation Error Banner
                    if (validationError != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = validationError ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Bottom Action Footer (Single-Line Responsive Buttons)
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val provider = selectedProvider
                            when {
                                provider == null -> {
                                    validationError = "Please select a Service Provider."
                                }
                                projectTitle.isBlank() -> {
                                    validationError = "Please enter a Project Title."
                                }
                                projectDescription.isBlank() -> {
                                    validationError = "Please enter a Project Description."
                                }
                                projectRequirements.isBlank() -> {
                                    validationError = "Please enter your Project Requirements."
                                }
                                expectedDeliveryDate.isBlank() -> {
                                    validationError = "Please specify an Expected Delivery Date."
                                }
                                else -> {
                                    validationError = null
                                    onSubmitProject(
                                        provider.userId.ifBlank { provider.id },
                                        provider.displayName,
                                        initialServiceId,
                                        selectedCategory,
                                        projectTitle.trim(),
                                        projectDescription.trim(),
                                        projectRequirements.trim(),
                                        expectedDeliveryDate.trim(),
                                        budgetText.toDoubleOrNull() ?: 0.0,
                                        additionalInstructions.trim(),
                                        attachments
                                    )
                                }
                            }
                        },
                        enabled = !isSubmitting && !isUploadingFiles,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("confirm_submit_project_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isSubmitting) "Submitting Project..." else "Submit Your Project",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }

    if (previewAttachment != null) {
        AttachmentPreviewModal(
            attachment = previewAttachment!!,
            onDismiss = { previewAttachment = null }
        )
    }
}

@Composable
fun ProjectAttachmentItemRow(
    attachment: ProjectAttachmentMeta,
    onPreviewClick: () -> Unit,
    onRemoveClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreviewClick() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Thumbnail or File Type Badge
            if (attachment.isImage && attachment.downloadURL.isNotBlank()) {
                AsyncImage(
                    model = attachment.downloadURL,
                    contentDescription = attachment.fileName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        attachment.isImage -> Color(0xFF0EA5E9).copy(alpha = 0.14f)
                        attachment.isVideo -> Color(0xFF8B5CF6).copy(alpha = 0.14f)
                        else -> Color(0xFFF59E0B).copy(alpha = 0.14f)
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                attachment.isImage -> Icons.Default.Image
                                attachment.isVideo -> Icons.Default.PlayCircleFilled
                                attachment.fileName.endsWith(".pdf", true) -> Icons.Default.PictureAsPdf
                                else -> Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = when {
                                attachment.isImage -> Color(0xFF0284C7)
                                attachment.isVideo -> Color(0xFF7C3AED)
                                else -> Color(0xFFD97706)
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${attachment.fileType.uppercase()} • ${attachment.formattedSize}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = { openOrShareAttachment(context, attachment) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = "Open or Download File",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (onRemoveClick != null) {
                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Remove Attachment",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AttachmentPreviewModal(
    attachment: ProjectAttachmentMeta,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = attachment.fileName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (attachment.isImage && attachment.downloadURL.isNotBlank()) {
                    AsyncImage(
                        model = attachment.downloadURL,
                        contentDescription = attachment.fileName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when {
                                    attachment.isVideo -> Icons.Default.OndemandVideo
                                    attachment.fileName.endsWith(".pdf", true) -> Icons.Default.PictureAsPdf
                                    else -> Icons.Default.InsertDriveFile
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (attachment.isVideo) "Video Preview Ready" else "Document Attachment",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Type: ${attachment.fileType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Size: ${attachment.formattedSize}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Cloud Path: ${attachment.storagePath}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    openOrShareAttachment(context, attachment)
                    onDismiss()
                }
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Open / Download", maxLines = 1, softWrap = false)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", maxLines = 1, softWrap = false)
            }
        }
    )
}

@Composable
fun ProjectDeliveryDialog(
    project: ProjectSubmission,
    providerUid: String,
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onDeliverWork: (deliveryNotes: String, deliveryFiles: List<ProjectAttachmentMeta>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var deliveryNotes by remember { mutableStateOf(project.deliveryNotes) }
    var deliveryFiles by remember { mutableStateOf(project.parsedDeliveryFiles) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isUploading = true
                errorText = null
                val added = mutableListOf<ProjectAttachmentMeta>()
                uris.forEach { uri ->
                    delay(70)
                    val (meta, err) = inspectAttachmentUri(context, uri, providerUid, "provider_delivery")
                    if (err != null) errorText = err
                    else if (meta != null) added.add(meta)
                }
                deliveryFiles = deliveryFiles + added
                isUploading = false
            }
        }
    }

    val docsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isUploading = true
                errorText = null
                val added = mutableListOf<ProjectAttachmentMeta>()
                uris.forEach { uri ->
                    delay(70)
                    val (meta, err) = inspectAttachmentUri(context, uri, providerUid, "provider_delivery")
                    if (err != null) errorText = err
                    else if (meta != null) added.add(meta)
                }
                deliveryFiles = deliveryFiles + added
                isUploading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Submit Completed Work • ${project.effectiveProjectRef}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Project: ${project.projectTitle} (Customer: ${project.customerName})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = deliveryNotes,
                    onValueChange = {
                        deliveryNotes = it
                        errorText = null
                    },
                    label = { Text("Delivery Summary & Notes *") },
                    placeholder = { Text("Describe the completed deliverables, instructions, or summary of work...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Attach Final Deliverables (Images, Videos, PDFs, Documents)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            mediaLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PermMedia, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Media", maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = {
                            docsLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "text/plain",
                                    "application/vnd.ms-excel",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "image/*",
                                    "video/*"
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Docs / PDF", maxLines = 1, softWrap = false)
                    }
                }

                if (isUploading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (deliveryFiles.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        deliveryFiles.forEachIndexed { idx, file ->
                            ProjectAttachmentItemRow(
                                attachment = file,
                                onPreviewClick = { openOrShareAttachment(context, file) },
                                onRemoveClick = {
                                    deliveryFiles = deliveryFiles.toMutableList().also { it.removeAt(idx) }
                                }
                            )
                        }
                    }
                }

                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (deliveryNotes.isBlank()) {
                        errorText = "Please enter delivery notes explaining the completed work."
                    } else {
                        onDeliverWork(deliveryNotes.trim(), deliveryFiles)
                    }
                },
                enabled = !isSubmitting && !isUploading
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Deliver Work", maxLines = 1, softWrap = false)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", maxLines = 1, softWrap = false)
            }
        }
    )
}

@Composable
fun ProjectStatusBadge(status: String, label: String) {
    val (bgColor, textColor) = when (status.lowercase()) {
        "new_request" -> Color(0xFF2563EB).copy(alpha = 0.14f) to Color(0xFF1D4ED8)
        "under_review" -> Color(0xFFD97706).copy(alpha = 0.14f) to Color(0xFFB45309)
        "accepted" -> Color(0xFF0D9488).copy(alpha = 0.14f) to Color(0xFF0F766E)
        "in_progress" -> Color(0xFF7C3AED).copy(alpha = 0.14f) to Color(0xFF6D28D9)
        "work_submitted" -> Color(0xFF0284C7).copy(alpha = 0.15f) to Color(0xFF0369A1)
        "revision_requested" -> Color(0xFFEA580C).copy(alpha = 0.15f) to Color(0xFFC2410C)
        "completed" -> Color(0xFF16A34A).copy(alpha = 0.15f) to Color(0xFF15803D)
        "rejected" -> Color(0xFFDC2626).copy(alpha = 0.15f) to Color(0xFFB91C1C)
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun CustomerProjectCard(
    project: ProjectSubmission,
    onRequestRevision: (String) -> Unit,
    onConfirmCompletion: () -> Unit,
    onMessageProvider: () -> Unit
) {
    var showRevisionDialog by remember { mutableStateOf(false) }
    var revisionNote by remember { mutableStateOf("") }
    var previewAttachment by remember { mutableStateOf<ProjectAttachmentMeta?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${project.effectiveProjectRef} • ${project.serviceCategory}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = project.projectTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Service Provider: ${project.providerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                ProjectStatusBadge(status = project.status, label = project.statusDisplayLabel)
            }

            // Key Metadata Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Deadline: ${project.expectedDeliveryDate.ifBlank { "Flexible" }}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (project.budgetInr > 0) {
                    Text(
                        text = "Budget: ₹${project.budgetInr.toInt()}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = project.projectDescription,
                style = MaterialTheme.typography.bodyMedium
            )

            if (project.projectRequirements.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Requirements:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = project.projectRequirements,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Customer Attached Files
            val attachments = project.parsedAttachments
            if (attachments.isNotEmpty()) {
                Text(
                    text = "Submitted Reference Files (${attachments.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    attachments.forEach { file ->
                        ProjectAttachmentItemRow(
                            attachment = file,
                            onPreviewClick = { previewAttachment = file }
                        )
                    }
                }
            }

            // Provider Response Message
            if (project.providerResponseMessage.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Provider Response:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = project.providerResponseMessage,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Delivered Work Files from Provider
            val deliveredFiles = project.parsedDeliveryFiles
            if (project.deliveryNotes.isNotBlank() || deliveredFiles.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0EA5E9).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.TaskAlt,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Completed Work Delivered by Provider",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0369A1)
                            )
                        }
                        if (project.deliveryNotes.isNotBlank()) {
                            Text(
                                text = project.deliveryNotes,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (deliveredFiles.isNotEmpty()) {
                            deliveredFiles.forEach { file ->
                                ProjectAttachmentItemRow(
                                    attachment = file,
                                    onPreviewClick = { previewAttachment = file }
                                )
                            }
                        }
                    }
                }
            }

            // Revision History
            val revisions = project.parsedRevisions
            if (revisions.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEA580C).copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Revision Requests (${revisions.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC2410C)
                        )
                        revisions.forEach { rev ->
                            Text(
                                text = "• ${rev.note}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Text(
                text = "Submitted: ${dateFormat.format(Date(project.createdAtMs))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Customer Action Buttons (Stacked cleanly to avoid vertical text wrapping)
            if (project.status.equals("work_submitted", ignoreCase = true)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showRevisionDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Request Revision",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = onConfirmCompletion,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Confirm Completed",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onMessageProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 42.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Discuss Project with ${project.providerName}",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (showRevisionDialog) {
        AlertDialog(
            onDismissRequest = { showRevisionDialog = false },
            title = { Text("Request Project Revision", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = revisionNote,
                    onValueChange = { revisionNote = it },
                    label = { Text("What changes or adjustments are needed?") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (revisionNote.isNotBlank()) {
                            onRequestRevision(revisionNote.trim())
                            revisionNote = ""
                            showRevisionDialog = false
                        }
                    }
                ) {
                    Text("Send Revision Request", maxLines = 1, softWrap = false)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevisionDialog = false }) {
                    Text("Cancel", maxLines = 1, softWrap = false)
                }
            }
        )
    }

    if (previewAttachment != null) {
        AttachmentPreviewModal(
            attachment = previewAttachment!!,
            onDismiss = { previewAttachment = null }
        )
    }
}

@Composable
fun ProviderProjectCard(
    project: ProjectSubmission,
    providerUid: String,
    isBusy: Boolean = false,
    onUpdateStatus: (newStatus: String, responseMessage: String) -> Unit,
    onDeliverWork: (deliveryNotes: String, deliveryFiles: List<ProjectAttachmentMeta>) -> Unit,
    onMessageCustomer: () -> Unit
) {
    var responseNote by remember(project.providerResponseMessage) {
        mutableStateOf(project.providerResponseMessage)
    }
    var showDeliveryModal by remember { mutableStateOf(false) }
    var previewAttachment by remember { mutableStateOf<ProjectAttachmentMeta?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${project.effectiveProjectRef} • ${project.serviceCategory}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = project.projectTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Customer: ${project.customerName}" +
                            if (project.customerPhone.isNotBlank()) " • ${project.customerPhone}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                ProjectStatusBadge(status = project.status, label = project.statusDisplayLabel)
            }

            // Deadline & Budget
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Expected Delivery: ${project.expectedDeliveryDate.ifBlank { "Flexible" }}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (project.budgetInr > 0) {
                    Text(
                        text = "Budget: ₹${project.budgetInr.toInt()}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = project.projectDescription,
                style = MaterialTheme.typography.bodyMedium
            )

            if (project.projectRequirements.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Customer Requirements:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = project.projectRequirements,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (project.additionalInstructions.isNotBlank()) {
                Text(
                    text = "Additional Instructions: ${project.additionalInstructions}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Attached Reference Files from Customer
            val attachments = project.parsedAttachments
            if (attachments.isNotEmpty()) {
                Text(
                    text = "Customer Reference Attachments (${attachments.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    attachments.forEach { file ->
                        ProjectAttachmentItemRow(
                            attachment = file,
                            onPreviewClick = { previewAttachment = file }
                        )
                    }
                }
            }

            // Revision Requests from Customer
            val revisions = project.parsedRevisions
            if (revisions.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEA580C).copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Customer Revision Requests (${revisions.size}):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC2410C)
                        )
                        revisions.forEach { rev ->
                            Text("• ${rev.note}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Provider Response Input
            OutlinedTextField(
                value = responseNote,
                onValueChange = { responseNote = it },
                label = { Text("Response Note to Customer") },
                placeholder = { Text("Add a message about timeline, acceptance, or progress...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            // Provider Workflow Actions (Responsive 2-Column Rows + Full-Width Primary Button)
            val statusLower = project.status.lowercase()
            if (statusLower in setOf("new_request", "under_review")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (statusLower == "new_request") {
                        OutlinedButton(
                            onClick = { onUpdateStatus("under_review", responseNote.trim()) },
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text("Mark Review", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    OutlinedButton(
                        onClick = { onUpdateStatus("rejected", responseNote.trim()) },
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text("Reject", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                }

                Button(
                    onClick = { onUpdateStatus("accepted", responseNote.trim()) },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Accept Project Request", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            } else if (statusLower in setOf("accepted", "in_progress", "revision_requested", "work_submitted")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (statusLower != "in_progress") {
                        OutlinedButton(
                            onClick = { onUpdateStatus("in_progress", responseNote.trim()) },
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("In Progress", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Button(
                        onClick = { showDeliveryModal = true },
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (statusLower == "work_submitted") "Update Delivery" else "Submit Work / Deliver",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onMessageCustomer,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 42.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Message Customer (${project.customerName})",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "Received: ${dateFormat.format(Date(project.createdAtMs))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDeliveryModal) {
        ProjectDeliveryDialog(
            project = project,
            providerUid = providerUid,
            isSubmitting = isBusy,
            onDismiss = { showDeliveryModal = false },
            onDeliverWork = { notes, files ->
                onDeliverWork(notes, files)
                showDeliveryModal = false
            }
        )
    }

    if (previewAttachment != null) {
        AttachmentPreviewModal(
            attachment = previewAttachment!!,
            onDismiss = { previewAttachment = null }
        )
    }
}
