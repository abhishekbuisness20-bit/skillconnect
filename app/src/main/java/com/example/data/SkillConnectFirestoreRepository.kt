package com.example.data

import android.content.Context
import com.example.R
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.WorkRequest
import com.example.model.WorkRequestStatus
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@IgnoreExtraProperties
data class UserProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "CUSTOMER",
    val city: String = "",
    val state: String = "",
    val avatarInitials: String = "",
    val profilePhotoUri: String? = null,
    val coverPhotoUri: String? = null,
    val professionalHeadline: String = "",
    val bio: String = "",
    val skills: List<String> = emptyList(),
    val experienceYears: Long = 0L,
    val education: String = "",
    val portfolioLinks: List<String> = emptyList(),
    val socialGithub: String = "",
    val socialLinkedin: String = "",
    val socialTwitter: String = "",
    val contactPreference: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomainModel(): UserProfile {
        val parsedRole = try {
            UserRole.valueOf(role)
        } catch (_: Exception) {
            UserRole.CUSTOMER
        }
        return UserProfile(
            id = userId.ifBlank { id },
            name = name,
            email = email,
            phone = phone,
            role = parsedRole,
            city = city,
            state = state,
            avatarInitials = avatarInitials.ifBlank { name.take(2).uppercase() },
            profilePhotoUri = profilePhotoUri,
            coverPhotoUri = coverPhotoUri,
            professionalHeadline = professionalHeadline,
            bio = bio,
            skills = skills,
            experienceYears = experienceYears.toInt(),
            education = education,
            portfolioLinks = portfolioLinks,
            socialGithub = socialGithub,
            socialLinkedin = socialLinkedin,
            socialTwitter = socialTwitter,
            contactPreference = contactPreference.ifBlank { "In-App Chat & Email" }
        )
    }
}

@IgnoreExtraProperties
data class WorkRequestDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val budgetInr: Long = 0L,
    val deadline: String = "",
    val status: String = "PENDING",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomainModel(): WorkRequest {
        val parsedStatus = try {
            WorkRequestStatus.valueOf(status)
        } catch (_: Exception) {
            WorkRequestStatus.PENDING
        }
        return WorkRequest(
            id = id,
            customerId = userId,
            customerName = customerName,
            providerId = providerId,
            providerName = providerName,
            title = title,
            description = description,
            category = category,
            budgetInr = budgetInr,
            deadline = deadline,
            status = parsedStatus,
            notes = notes
        )
    }
}

@IgnoreExtraProperties
data class AdminRoleDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val role: String = "ADMIN",
    val accessLevel: String = "LIMITED ADMIN",
    val isActive: Boolean = true,
    val permissions: Map<String, Boolean> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomainPermissions(): com.example.model.AdminPermissions {
        return com.example.model.AdminPermissions(
            canManageAdmins = permissions["canManageAdmins"] ?: false,
            canManageRoles = permissions["canManageRoles"] ?: false,
            canManageCategories = permissions["canManageCategories"] ?: false,
            canBroadcastAnnouncements = permissions["canBroadcastAnnouncements"] ?: false,
            canApproveProviders = permissions["canApproveProviders"] ?: true,
            canModerateContent = permissions["canModerateContent"] ?: true,
            canResolveComplaints = permissions["canResolveComplaints"] ?: true,
            canAccessSecurityLogs = permissions["canAccessSecurityLogs"] ?: false,
            canViewFinancialAnalytics = permissions["canViewFinancialAnalytics"] ?: false,
            canDeletePlatformData = permissions["canDeletePlatformData"] ?: false
        )
    }

    fun toDomainRoleType(): com.example.model.AdminRoleType {
        return try {
            com.example.model.AdminRoleType.valueOf(role)
        } catch (_: Exception) {
            com.example.model.AdminRoleType.ADMIN
        }
    }
}

@IgnoreExtraProperties
data class AdminAuditLogDoc(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val adminRole: String = "ADMIN",
    val action: String = "",
    val targetEntityId: String = "",
    val details: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
) {
    fun toDomainModel(): com.example.model.AdminAuditLog {
        val roleType = try {
            com.example.model.AdminRoleType.valueOf(adminRole)
        } catch (_: Exception) {
            com.example.model.AdminRoleType.ADMIN
        }
        return com.example.model.AdminAuditLog(
            id = id,
            adminUid = adminUid,
            adminEmail = adminEmail,
            roleType = roleType,
            action = action,
            targetEntityId = targetEntityId,
            details = details,
            timestampMillis = timestamp?.toDate()?.time ?: System.currentTimeMillis()
        )
    }
}

@IgnoreExtraProperties
data class PlatformComplaintDoc(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val targetProviderId: String = "",
    val targetProviderName: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "PENDING",
    val assignedRoleLevel: String = "ADMIN",
    val resolutionNotes: String = "",
    val resolvedByUid: String = "",
    val resolvedByEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomainModel(): com.example.model.PlatformComplaint {
        val st = try {
            com.example.model.ComplaintStatus.valueOf(status)
        } catch (_: Exception) {
            com.example.model.ComplaintStatus.PENDING
        }
        val assigned = try {
            com.example.model.AdminRoleType.valueOf(assignedRoleLevel)
        } catch (_: Exception) {
            com.example.model.AdminRoleType.ADMIN
        }
        return com.example.model.PlatformComplaint(
            id = id,
            reporterId = reporterId,
            reporterName = reporterName,
            targetProviderId = targetProviderId.ifBlank { null },
            targetProviderName = targetProviderName.ifBlank { null },
            reason = reason,
            details = details,
            status = st,
            assignedRoleLevel = assigned,
            resolutionNotes = resolutionNotes,
            timestampMillis = createdAt?.toDate()?.time ?: System.currentTimeMillis()
        )
    }
}

class SkillConnectFirestoreRepository(private val db: FirebaseFirestore) {

    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users")
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
        return try {
            val existingSnap = docRef.get().await()
            val safeRole = if (profile.role == UserRole.ADMIN) "CUSTOMER" else profile.role.name
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "name" to profile.name.ifBlank { "SkillConnect User" }.take(100),
                "email" to profile.email.ifBlank { auth.currentUser?.email ?: "user@skillconnect.in" }.take(120),
                "phone" to profile.phone.take(30),
                "role" to safeRole,
                "city" to profile.city.take(80),
                "state" to profile.state.take(80),
                "avatarInitials" to profile.avatarInitials.take(10),
                "professionalHeadline" to profile.professionalHeadline.take(150),
                "bio" to profile.bio.take(1000),
                "skills" to profile.skills.take(20).map { it.take(60) },
                "experienceYears" to profile.experienceYears.coerceIn(0, 80).toLong(),
                "education" to profile.education.take(200),
                "portfolioLinks" to profile.portfolioLinks.take(10).map { it.take(300) },
                "socialGithub" to profile.socialGithub.take(100),
                "socialLinkedin" to profile.socialLinkedin.take(100),
                "socialTwitter" to profile.socialTwitter.take(100),
                "contactPreference" to profile.contactPreference.take(100),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            profile.profilePhotoUri?.take(500)?.let { payload["profilePhotoUri"] = it }
            profile.coverPhotoUri?.take(500)?.let { payload["coverPhotoUri"] = it }

            if (existingSnap.exists()) {
                val existingCreatedAt = existingSnap.getTimestamp(
                    "createdAt",
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existingCreatedAt != null) {
                    payload["createdAt"] = existingCreatedAt
                } else {
                    payload["createdAt"] = FieldValue.serverTimestamp()
                }
                docRef.set(payload).await()
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            }
            Result.success(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(userId: String = requireUserId()): Result<UserProfileDoc?> {
        val docRef = db.collection("users").document(userId)
        return try {
            val snap = docRef.get().await()
            val doc = snap.toObject(
                UserProfileDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(doc)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String = requireUserId()): Flow<UserProfileDoc?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    snap.toObject(
                        UserProfileDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun createWorkRequest(
        customerName: String,
        providerId: String,
        providerName: String,
        title: String,
        description: String,
        category: String,
        budgetInr: Long,
        deadline: String
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "work_requests")
            return Result.failure(e)
        }
        val collectionRef = db.collection("work_requests")
        val docRef = collectionRef.document()
        val payload = mapOf(
            "userId" to uid,
            "customerName" to customerName.ifBlank { "Customer" }.take(100),
            "providerId" to providerId.ifBlank { "prov_default" }.take(128),
            "providerName" to providerName.ifBlank { "Provider" }.take(100),
            "title" to title.ifBlank { "Work Request" }.take(150),
            "description" to description.ifBlank { "Project details" }.take(1000),
            "category" to category.ifBlank { "General" }.take(100),
            "budgetInr" to budgetInr.coerceIn(0L, 100000000L),
            "deadline" to deadline.ifBlank { "Flexible" }.take(80),
            "status" to WorkRequestStatus.PENDING.name,
            "notes" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserWorkRequests(): Result<List<WorkRequestDoc>> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "work_requests")
            return Result.failure(e)
        }
        return try {
            val snapshot = db.collection("work_requests")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val list = snapshot.toObjects(
                WorkRequestDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "work_requests")
            Result.failure(e)
        }
    }

    suspend fun getWorkRequestById(requestId: String): Result<WorkRequestDoc> {
        val docRef = db.collection("work_requests").document(requestId)
        return try {
            val snap = docRef.get().await()
            val doc = snap.toObject(
                WorkRequestDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw NoSuchElementException("Work request not found: $requestId")
            Result.success(doc)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserWorkRequests(): Flow<List<WorkRequestDoc>> = flow {
        val uid = auth.currentUser?.uid ?: ""
        val path = "work_requests"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WorkRequestDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // --- Admin Roles Collection (/admin_roles/{uid}) ---

    suspend fun getAdminRole(uid: String): Result<AdminRoleDoc?> {
        val docRef = db.collection("admin_roles").document(uid)
        return try {
            val snap = docRef.get().await()
            val doc = snap.toObject(
                AdminRoleDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(doc)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeAdminRoles(): Flow<List<AdminRoleDoc>> = flow {
        val path = "admin_roles"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AdminRoleDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveAdminRole(roleDoc: AdminRoleDoc): Result<Unit> {
        val targetUid = roleDoc.userId.ifBlank { roleDoc.id }
        val docRef = db.collection("admin_roles").document(targetUid)
        val payload = mapOf(
            "userId" to targetUid,
            "email" to roleDoc.email.trim().lowercase(),
            "role" to roleDoc.role,
            "accessLevel" to roleDoc.accessLevel,
            "isActive" to roleDoc.isActive,
            "permissions" to roleDoc.permissions,
            "createdAt" to (roleDoc.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Admin Audit Logs Collection (/admin_audit_logs/{logId}) ---

    suspend fun createAuditLog(
        adminUid: String,
        adminEmail: String,
        adminRole: String,
        action: String,
        targetEntityId: String,
        details: String
    ): Result<String> {
        val collectionRef = db.collection("admin_audit_logs")
        val docRef = collectionRef.document()
        val payload = mapOf(
            "adminUid" to adminUid,
            "adminEmail" to adminEmail.trim().lowercase(),
            "adminRole" to adminRole,
            "action" to action,
            "targetEntityId" to targetEntityId,
            "details" to details,
            "timestamp" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeAuditLogs(): Flow<List<AdminAuditLogDoc>> = flow {
        val path = "admin_audit_logs"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AdminAuditLogDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // --- Platform Complaints Collection (/complaints/{complaintId}) ---

    fun observeComplaints(): Flow<List<PlatformComplaintDoc>> = flow {
        val path = "complaints"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PlatformComplaintDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateComplaintStatus(
        complaintId: String,
        status: String,
        notes: String,
        resolvedByUid: String,
        resolvedByEmail: String
    ): Result<Unit> {
        val docRef = db.collection("complaints").document(complaintId)
        val payload = mapOf(
            "status" to status,
            "resolutionNotes" to notes,
            "resolvedByUid" to resolvedByUid,
            "resolvedByEmail" to resolvedByEmail,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }
}
