package com.example.data.repository

import com.example.data.local.BlokCeDao
import com.example.data.local.DanaLiveEventEntity
import com.example.data.local.PendingPaymentEntity
import com.example.data.local.toEntity
import com.example.data.model.AUTO_BILL_METHOD
import com.example.data.model.AdminInvite
import com.example.data.model.AdminRecord
import com.example.data.model.Announcement
import com.example.data.model.DanaWebhookConfig
import com.example.data.model.DanaWebhookEvent
import com.example.data.model.DuesStatus
import com.example.data.model.MonthlySummary
import com.example.data.model.PRIMARY_ADMIN_EMAIL
import com.example.data.model.Payment
import com.example.data.model.Resident
import com.example.data.model.SystemSettings
import com.example.data.model.computeFundRecap
import com.example.data.model.computeMonthSummary
import com.example.data.model.formatRupiah
import com.example.data.model.isWithinBillingHorizon
import com.example.data.model.monthLabel
import com.example.data.model.resolveAdminStatus
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class SubmissionOutcome {
    SENT_CLOUD,
    QUEUED_OFFLINE
}

class BlokCeRepository(
    private val db: FirebaseFirestore,
    private val dao: BlokCeDao,
    private val connectivity: ConnectivityObserver,
    private val auth: FirebaseAuth = Firebase.auth
) {
    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = PRIMARY_ADMIN_EMAIL
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus login dengan Google terlebih dahulu.")
    }

    private fun isBootstrappedAdmin(user: FirebaseUser?): Boolean {
        return user != null &&
            user.isEmailVerified &&
            user.email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)
    }

    private fun formattedToday(): String =
        SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date())

    fun observeOnlineStatus(): Flow<Boolean> = connectivity.observeOnlineStatus()

    fun observeCachedSettings(): Flow<SystemSettings> =
        dao.observeSettings().map { it?.toDomain() ?: SystemSettings() }

    fun observeCachedResident(uid: String): Flow<Resident?> =
        dao.observeResident(uid).map { it?.toDomain() }

    fun observeCachedAllResidents(): Flow<List<Resident>> =
        dao.observeAllResidents().map { list -> list.map { it.toDomain() } }

    fun observeCachedUserPayments(uid: String): Flow<List<Payment>> =
        dao.observeUserPayments(uid).map { list -> list.map { it.toDomain() } }

    fun observeCachedAllPayments(): Flow<List<Payment>> =
        dao.observeAllPayments().map { list -> list.map { it.toDomain() } }

    fun observeCachedAnnouncements(): Flow<List<Announcement>> =
        dao.observeAnnouncements().map { list -> list.map { it.toDomain() } }

    fun observeCachedMonthlySummary(month: String): Flow<MonthlySummary?> =
        dao.observeMonthlySummary(month).map { it?.toDomain() }

    fun observePendingOfflinePayments(uid: String): Flow<List<Payment>> =
        dao.observePendingPayments(uid).map { list -> list.map { it.toDomain() } }

    fun observeDanaLiveEvents(): Flow<List<DanaLiveEventEntity>> =
        dao.observeDanaLiveEvents()

    suspend fun clearDanaLiveEvents() {
        dao.clearDanaLiveEvents()
    }

    suspend fun clearSensitiveLocalCaches() {
        dao.clearAllResidentsCache()
        dao.clearAllPaymentsCache()
    }

    fun observeSystemSettings(): Flow<SystemSettings> = flow {
        requireUserId()
        val path = "system_settings/config"
        emitAll(
            db.collection("system_settings").document("config")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        SystemSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: SystemSettings()
                }
                .onEach { settings -> dao.upsertSettings(settings.toEntity()) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser != null) {
                        handleFirestoreError(ex, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeCurrentResident(uid: String): Flow<Resident?> = flow {
        val activeUid = requireUserId()
        val path = "residents/$uid"
        emitAll(
            db.collection("residents").document(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        Resident::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .onEach { resident ->
                    if (resident != null) dao.upsertResident(resident.toEntity())
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == activeUid) {
                        handleFirestoreError(ex, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeAllResidents(): Flow<List<Resident>> = flow {
        val activeUid = requireUserId()
        val path = "residents"
        emitAll(
            db.collection("residents")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Resident::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(compareBy({ it.house }, { it.name }))
                }
                .onEach { list -> dao.replaceAllResidents(list.map { it.toEntity() }) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == activeUid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeUserPayments(uid: String): Flow<List<Payment>> = flow {
        val activeUid = requireUserId()
        val path = "payments"
        emitAll(
            db.collection("payments")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Payment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.month }
                }
                .onEach { list -> dao.replaceUserPayments(uid, list.map { it.toEntity() }) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == activeUid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAllPayments(): Flow<List<Payment>> = flow {
        val activeUid = requireUserId()
        val path = "payments"
        emitAll(
            db.collection("payments")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Payment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.month }
                }
                .onEach { list -> dao.replaceAllPayments(list.map { it.toEntity() }) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == activeUid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAnnouncements(): Flow<List<Announcement>> = flow {
        val activeUid = requireUserId()
        val path = "announcements"
        emitAll(
            db.collection("announcements")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Announcement::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .onEach { list -> dao.replaceAnnouncements(list.map { it.toEntity() }) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == activeUid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMonthlySummary(month: String): Flow<MonthlySummary?> = flow {
        val activeUid = requireUserId()
        val path = "monthly_summaries/$month"
        emitAll(
            db.collection("monthly_summaries").document(month)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        MonthlySummary::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .onEach { summary ->
                    if (summary != null) dao.upsertMonthlySummary(summary.toEntity())
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == activeUid) {
                        handleFirestoreError(ex, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureSessionBootstrapped(user: FirebaseUser): Result<Resident> {
        val path = "residents/${user.uid}"
        return try {
            val isPrimary = isBootstrappedAdmin(user)
            val hasAdminDoc = if (!isPrimary) {
                try {
                    db.collection("admins").document(user.uid).get().await().exists()
                } catch (_: Exception) {
                    false
                }
            } else {
                false
            }
            val residentRef = db.collection("residents").document(user.uid)
            val snap = residentRef.get().await()
            val existingObj = if (snap.exists()) {
                snap.toObject(Resident::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
            val status = resolveAdminStatus(
                email = user.email,
                emailVerified = user.isEmailVerified,
                hasAdminDoc = hasAdminDoc,
                existingRoleLabel = existingObj?.roleLabel.orEmpty()
            )
            val isAdmin = status.isAdmin

            val resident: Resident = if (!snap.exists()) {
                val email = (user.email ?: "${user.uid}@blokcepsa.id").take(120)
                val rawUsername = email.substringBefore("@")
                    .lowercase(Locale.ROOT)
                    .replace(Regex("[^a-z0-9._-]"), "")
                    .ifBlank { "warga" }
                    .take(60)
                val displayName = (user.displayName ?: rawUsername).take(120)
                val role = status.effectiveRole
                val roleLabel = status.effectiveRoleLabel
                val payload = mapOf(
                    "userId" to user.uid,
                    "username" to rawUsername,
                    "name" to displayName,
                    "house" to if (isAdmin) "Pos Admin CE" else "Blok CE-??",
                    "phone" to "",
                    "email" to email,
                    "role" to role,
                    "roleLabel" to roleLabel,
                    "hidmatEnabled" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                residentRef.set(payload).await()
                Resident(
                    id = user.uid,
                    userId = user.uid,
                    username = rawUsername,
                    name = displayName,
                    house = if (isAdmin) "Pos Admin CE" else "Blok CE-??",
                    phone = "",
                    email = email,
                    role = role,
                    roleLabel = roleLabel,
                    hidmatEnabled = false
                )
            } else {
                val existing = existingObj ?: Resident(id = user.uid, userId = user.uid)
                if (existing.role != status.effectiveRole) {
                    try {
                        residentRef.update(
                            mapOf(
                                "role" to status.effectiveRole,
                                "roleLabel" to status.effectiveRoleLabel,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    } catch (_: Exception) {
                        // Fail-safe: UI still enforces computed status
                    }
                    existing.copy(role = status.effectiveRole, roleLabel = status.effectiveRoleLabel)
                } else {
                    existing
                }
            }

            dao.upsertResident(resident.toEntity())

            if (isPrimary) {
                seedInitialAdminDocsIfMissing(user)
            }

            Result.success(resident)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun seedInitialAdminDocsIfMissing(user: FirebaseUser) {
        val configRef = db.collection("system_settings").document("config")
        val configSnap = configRef.get().await()
        if (!configSnap.exists()) {
            val defaults = SystemSettings()
            val payload = mapOf(
                "blokName" to defaults.blokName,
                "subtitle" to defaults.subtitle,
                "danaNumber" to defaults.danaNumber,
                "danaAccountName" to defaults.danaAccountName,
                "customQrisData" to defaults.customQrisData,
                "iuranAmount" to defaults.iuranAmount,
                "hidmatAmount" to defaults.hidmatAmount,
                "reminderEnabled" to defaults.reminderEnabled,
                "reminderTitle" to defaults.reminderTitle,
                "reminderMessage" to defaults.reminderMessage,
                "pushBlastId" to defaults.pushBlastId,
                "pushTargetUserId" to defaults.pushTargetUserId,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            configRef.set(payload).await()
            dao.upsertSettings(defaults.toEntity())
        }

        val infoSnap = db.collection("announcements")
            .whereEqualTo("published", true)
            .limit(1)
            .get()
            .await()
        if (infoSnap.isEmpty) {
            val info1 = mapOf(
                "title" to "Kerja Bakti & Pembersihan Saluran Air Blok CE",
                "category" to "Berita Kegiatan",
                "imageUrl" to "",
                "content" to "Dihimbau kepada seluruh warga Blok CE PSA untuk berpartisipasi dalam kerja bakti rutin hari Minggu pukul 07.00 WIB. Titik kumpul di Pos Keamanan Blok CE.",
                "urgent" to true,
                "published" to true,
                "displayDate" to formattedToday(),
                "authorId" to user.uid,
                "authorName" to (user.displayName ?: "Admin Blok CE PSA"),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("announcements").document("info_welcome_1").set(info1).await()
        }
    }

    suspend fun submitPayment(
        month: String,
        types: List<Pair<String, Long>>,
        method: String,
        note: String
    ): Result<SubmissionOutcome> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (types.isEmpty()) {
            return Result.failure(IllegalArgumentException("Tidak ada tagihan yang dipilih."))
        }
        val dateLabel = formattedToday()
        val cleanMethod = method.trim().take(50).ifBlank { "Transfer DANA" }
        val cleanNote = note.trim().take(200)

        if (!connectivity.isCurrentlyOnline()) {
            for ((type, amount) in types) {
                val payId = "${uid}_${month}_${type}"
                dao.upsertPendingPayment(
                    PendingPaymentEntity(
                        id = payId,
                        userId = uid,
                        month = month,
                        type = type,
                        amount = amount,
                        dateLabel = dateLabel,
                        method = cleanMethod,
                        note = cleanNote
                    )
                )
            }
            return Result.success(SubmissionOutcome.QUEUED_OFFLINE)
        }

        return try {
            for ((type, amount) in types) {
                val payId = "${uid}_${month}_${type}"
                val docRef = db.collection("payments").document(payId)
                val existing = docRef.get().await()
                val noteValue = cleanNote.ifBlank { "Konfirmasi warga" }
                if (existing.exists()) {
                    val existingStatus = existing.getString("status") ?: ""
                    val existingMethod = existing.getString("method") ?: ""
                    if (existingStatus == "paid") continue
                    if (existingStatus == "pending" && existingMethod != AUTO_BILL_METHOD) continue
                    docRef.update(
                        mapOf(
                            "status" to "pending",
                            "amount" to amount,
                            "dateLabel" to dateLabel,
                            "method" to cleanMethod,
                            "note" to noteValue,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    val payload = mapOf(
                        "userId" to uid,
                        "month" to month,
                        "type" to type,
                        "amount" to amount,
                        "status" to "pending",
                        "dateLabel" to dateLabel,
                        "method" to cleanMethod,
                        "note" to noteValue,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload).await()
                }
            }
            Result.success(SubmissionOutcome.SENT_CLOUD)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                for ((type, amount) in types) {
                    val payId = "${uid}_${month}_${type}"
                    dao.upsertPendingPayment(
                        PendingPaymentEntity(
                            id = payId,
                            userId = uid,
                            month = month,
                            type = type,
                            amount = amount,
                            dateLabel = dateLabel,
                            method = cleanMethod,
                            note = cleanNote
                        )
                    )
                }
                Result.success(SubmissionOutcome.QUEUED_OFFLINE)
            } else {
                handleFirestoreError(e, OperationType.CREATE, "payments")
                Result.failure(e)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "payments")
            Result.failure(e)
        }
    }

    suspend fun flushOfflinePayments(): Result<Int> {
        val uid = auth.currentUser?.uid ?: return Result.success(0)
        if (!connectivity.isCurrentlyOnline()) return Result.success(0)
        val pending = dao.getPendingPaymentsOnce(uid)
        if (pending.isEmpty()) return Result.success(0)

        var synced = 0
        for (item in pending) {
            try {
                val docRef = db.collection("payments").document(item.id)
                val existing = docRef.get().await()
                val noteValue = item.note.ifBlank { "Sinkronisasi antrean offline" }
                if (!existing.exists()) {
                    val payload = mapOf(
                        "userId" to uid,
                        "month" to item.month,
                        "type" to item.type,
                        "amount" to item.amount,
                        "status" to "pending",
                        "dateLabel" to item.dateLabel,
                        "method" to item.method,
                        "note" to noteValue,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload).await()
                } else {
                    val existingStatus = existing.getString("status") ?: ""
                    val existingMethod = existing.getString("method") ?: ""
                    if (existingStatus == "unpaid" ||
                        (existingStatus == "pending" && existingMethod == AUTO_BILL_METHOD)
                    ) {
                        docRef.update(
                            mapOf(
                                "status" to "pending",
                                "amount" to item.amount,
                                "dateLabel" to item.dateLabel,
                                "method" to item.method,
                                "note" to noteValue,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
                dao.deletePendingPayment(item.id)
                synced++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, "payments/${item.id}")
            }
        }
        return Result.success(synced)
    }

    suspend fun updateOwnProfile(
        username: String,
        name: String,
        house: String,
        phone: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanUsername = username.trim().lowercase(Locale.ROOT).replace(" ", "").take(60)
        val cleanName = name.trim().take(120)
        val cleanHouse = house.trim().take(40)
        val cleanPhone = phone.trim().take(30)
        if (cleanUsername.length < 3 || cleanName.isBlank() || cleanHouse.isBlank()) {
            return Result.failure(IllegalArgumentException("Username min 3 huruf, nama & nomor rumah wajib diisi."))
        }
        val path = "residents/$uid"
        return try {
            db.collection("residents").document(uid).update(
                mapOf(
                    "username" to cleanUsername,
                    "name" to cleanName,
                    "house" to cleanHouse,
                    "phone" to cleanPhone,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun verifyPayment(paymentId: String): Result<Unit> {
        val path = "payments/$paymentId"
        return try {
            requireUserId()
            db.collection("payments").document(paymentId).update(
                mapOf(
                    "status" to "paid",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun rejectPayment(paymentId: String): Result<Unit> {
        val path = "payments/$paymentId"
        return try {
            requireUserId()
            db.collection("payments").document(paymentId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun markPaymentPaid(
        targetUid: String,
        month: String,
        type: String,
        amount: Long
    ): Result<Unit> {
        val paymentId = "${targetUid}_${month}_${type}"
        val path = "payments/$paymentId"
        return try {
            requireUserId()
            val docRef = db.collection("payments").document(paymentId)
            val snap = docRef.get().await()
            val dateLabel = formattedToday()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "status" to "paid",
                        "amount" to amount,
                        "dateLabel" to dateLabel,
                        "method" to "Verifikasi Admin / Tunai",
                        "note" to "Dicatat lunas oleh Admin",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to targetUid,
                        "month" to month,
                        "type" to type,
                        "amount" to amount,
                        "status" to "paid",
                        "dateLabel" to dateLabel,
                        "method" to "Verifikasi Admin / Tunai",
                        "note" to "Dicatat lunas oleh Admin",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleResidentHidmat(targetUid: String, enabled: Boolean): Result<Unit> {
        val path = "residents/$targetUid"
        return try {
            requireUserId()
            db.collection("residents").document(targetUid).update(
                mapOf(
                    "hidmatEnabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteResident(targetUid: String): Result<Unit> {
        val path = "residents/$targetUid"
        return try {
            val uid = requireUserId()
            require(targetUid != uid) { "Tidak dapat menghapus akun admin aktif." }
            db.collection("residents").document(targetUid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAnnouncement(
        existingId: String?,
        title: String,
        category: String,
        imageUrl: String,
        content: String,
        urgent: Boolean,
        authorName: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanTitle = title.trim().take(200)
        val cleanCat = category.trim().take(60).ifBlank { "Pengumuman" }
        val cleanImg = imageUrl.trim().take(250000)
        val cleanContent = content.trim().take(2000)
        if (cleanTitle.isBlank() || cleanContent.isBlank()) {
            return Result.failure(IllegalArgumentException("Judul dan isi pengumuman wajib diisi."))
        }
        val docId = existingId?.takeIf { it.isNotBlank() } ?: "info_${System.currentTimeMillis()}"
        val path = "announcements/$docId"
        return try {
            val docRef = db.collection("announcements").document(docId)
            val dateLabel = formattedToday()
            if (existingId != null && docRef.get().await().exists()) {
                docRef.update(
                    mapOf(
                        "title" to cleanTitle,
                        "category" to cleanCat,
                        "imageUrl" to cleanImg,
                        "content" to cleanContent,
                        "urgent" to urgent,
                        "published" to true,
                        "displayDate" to dateLabel,
                        "authorName" to authorName.take(120).ifBlank { "Admin" },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "title" to cleanTitle,
                        "category" to cleanCat,
                        "imageUrl" to cleanImg,
                        "content" to cleanContent,
                        "urgent" to urgent,
                        "published" to true,
                        "displayDate" to dateLabel,
                        "authorId" to uid,
                        "authorName" to authorName.take(120).ifBlank { "Admin" },
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(id: String): Result<Unit> {
        val path = "announcements/$id"
        return try {
            requireUserId()
            db.collection("announcements").document(id).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveSystemSettings(updated: SystemSettings): Result<Unit> {
        val path = "system_settings/config"
        return try {
            requireUserId()
            val docRef = db.collection("system_settings").document("config")
            val exists = docRef.get().await().exists()
            val clean = updated.copy(
                blokName = updated.blokName.trim().take(80).ifBlank { "Blok CE PSA" },
                subtitle = updated.subtitle.trim().take(150),
                danaNumber = updated.danaNumber.trim().take(30).ifBlank { "085263195072" },
                danaAccountName = updated.danaAccountName.trim().take(100).ifBlank { "Bendahara Blok CE PSA" },
                customQrisData = updated.customQrisData.trim().take(250000),
                iuranAmount = updated.iuranAmount.coerceIn(0L, 10000000L),
                hidmatAmount = updated.hidmatAmount.coerceIn(0L, 10000000L),
                reminderTitle = updated.reminderTitle.trim().take(150),
                reminderMessage = updated.reminderMessage.trim().take(500),
                pushTargetUserId = updated.pushTargetUserId.trim().take(128).ifBlank { "all_unpaid" }
            )
            if (exists) {
                docRef.update(
                    mapOf(
                        "blokName" to clean.blokName,
                        "subtitle" to clean.subtitle,
                        "danaNumber" to clean.danaNumber,
                        "danaAccountName" to clean.danaAccountName,
                        "customQrisData" to clean.customQrisData,
                        "iuranAmount" to clean.iuranAmount,
                        "hidmatAmount" to clean.hidmatAmount,
                        "reminderEnabled" to clean.reminderEnabled,
                        "reminderTitle" to clean.reminderTitle,
                        "reminderMessage" to clean.reminderMessage,
                        "pushBlastId" to clean.pushBlastId,
                        "pushTargetUserId" to clean.pushTargetUserId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "blokName" to clean.blokName,
                        "subtitle" to clean.subtitle,
                        "danaNumber" to clean.danaNumber,
                        "danaAccountName" to clean.danaAccountName,
                        "customQrisData" to clean.customQrisData,
                        "iuranAmount" to clean.iuranAmount,
                        "hidmatAmount" to clean.hidmatAmount,
                        "reminderEnabled" to clean.reminderEnabled,
                        "reminderTitle" to clean.reminderTitle,
                        "reminderMessage" to clean.reminderMessage,
                        "pushBlastId" to clean.pushBlastId,
                        "pushTargetUserId" to clean.pushTargetUserId,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun syncMonthlySummary(
        month: String,
        residents: List<Resident>,
        payments: List<Payment>
    ): Result<Unit> {
        val path = "monthly_summaries/$month"
        return try {
            requireUserId()
            val members = residents.filter { !it.adminRole }
            val paidIuran = members.count { member ->
                val memberPays = payments.filter { it.userId == member.id }
                computeMonthSummary(member, memberPays, month).iuranStatus == DuesStatus.PAID
            }
            val pending = payments.count {
                it.month == month && it.status == "pending" && it.method != AUTO_BILL_METHOD
            }
            db.collection("monthly_summaries").document(month).set(
                mapOf(
                    "month" to month,
                    "totalMembers" to members.size.toLong(),
                    "paidIuranCount" to paidIuran.toLong(),
                    "pendingCount" to pending.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun buildMonthlyCsv(
        month: String,
        residents: List<Resident>,
        payments: List<Payment>
    ): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.appendLine("Nama Warga,Nomor Rumah,No HP,Bulan,Status Iuran,Status Hidmat")
        val label = monthLabel(month)
        residents.filter { !it.adminRole }.forEach { u ->
            val userPays = payments.filter { it.userId == u.id }
            val sum = computeMonthSummary(u, userPays, month)
            val iuranTxt = when (sum.iuranStatus) {
                DuesStatus.PAID -> "LUNAS"
                DuesStatus.PENDING -> "MENUNGGU"
                else -> "BELUM BAYAR"
            }
            val hidmatTxt = when {
                !u.hidmatEnabled -> "TIDAK AKTIF"
                sum.hidmatStatus == DuesStatus.PAID -> "LUNAS"
                sum.hidmatStatus == DuesStatus.PENDING -> "MENUNGGU"
                else -> "BELUM BAYAR"
            }
            val safeName = u.name.replace("\"", "\"\"")
            val safeHouse = u.house.replace("\"", "\"\"")
            val safePhone = u.phone.replace("\"", "\"\"")
            sb.appendLine("\"$safeName\",\"$safeHouse\",\"$safePhone\",\"$label\",\"$iuranTxt\",\"$hidmatTxt\"")
        }
        return sb.toString()
    }

    fun buildFundRecapCsv(
        month: String,
        residents: List<Resident>,
        payments: List<Payment>,
        settings: SystemSettings
    ): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        val recap = computeFundRecap(month, residents, payments, settings)
        val label = monthLabel(month)
        sb.appendLine("REKAPITULASI DANA BLOK CE PSA - $label")
        sb.appendLine("Total Warga Aktif,${recap.totalResidentsCount}")
        sb.appendLine("Dana Iuran Masuk (Lunas),${recap.paidIuranAmount}")
        sb.appendLine("Dana Hidmat Masuk (Lunas),${recap.paidHidmatAmount}")
        sb.appendLine("Total Dana Masuk Bulan Ini,${recap.totalCollectedMonth}")
        sb.appendLine("Menunggu Verifikasi,${recap.pendingVerificationAmount}")
        sb.appendLine("Sisa Tagihan Belum Lunas,${recap.unpaidOutstandingAmount}")
        sb.appendLine("Akumulasi Lunas Tahun ${month.take(4)},${recap.yearTotalCollected}")
        sb.appendLine("Persentase Realisasi,${recap.collectionPercent}%")
        sb.appendLine()
        sb.appendLine("Rincian Metode Pembayaran,Jumlah Transaksi,Total Nominal")
        recap.methodBreakdowns.forEach { m ->
            val safeMethod = m.method.replace("\"", "\"\"")
            sb.appendLine("\"$safeMethod\",${m.count},${m.totalAmount}")
        }
        sb.appendLine()
        sb.appendLine("Nama Warga,Nomor Rumah,Nominal Iuran Lunas,Nominal Hidmat Lunas,Total Lunas")
        residents.filter { !it.adminRole }.forEach { u ->
            val paidUser = payments.filter { it.userId == u.id && it.month == month && it.status == "paid" }
            val iuranSum = paidUser.filter { it.type == "iuran" }.sumOf { it.amount }
            val hidmatSum = paidUser.filter { it.type == "hidmat" }.sumOf { it.amount }
            val safeName = u.name.replace("\"", "\"\"")
            val safeHouse = u.house.replace("\"", "\"\"")
            sb.appendLine("\"$safeName\",\"$safeHouse\",$iuranSum,$hidmatSum,${iuranSum + hidmatSum}")
        }
        return sb.toString()
    }

    fun observeAdminInvites(): Flow<List<AdminInvite>> = flow {
        val activeUid = requireUserId()
        val path = "admin_invites"
        emitAll(
            db.collection("admin_invites")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AdminInvite::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == activeUid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeDelegatedAdmins(): Flow<List<AdminRecord>> = flow {
        val activeUid = requireUserId()
        val path = "admins"
        emitAll(
            db.collection("admins")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AdminRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == activeUid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createAdminInvite(label: String, targetEmail: String): Result<AdminInvite> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanLabel = label.trim().take(60).ifBlank { "Admin / Pengurus Blok" }
        val cleanTargetEmail = targetEmail.trim().lowercase(Locale.ROOT).take(120)
        val suffix = (1..4).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"[Random.nextInt(32)] }.joinToString("")
        val code = "CE-ADM-$suffix"
        val path = "admin_invites/$code"
        return try {
            val payload = mapOf(
                "code" to code,
                "targetEmail" to cleanTargetEmail,
                "label" to cleanLabel,
                "used" to false,
                "usedByUid" to "",
                "usedByEmail" to "",
                "createdByUid" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("admin_invites").document(code).set(payload).await()
            Result.success(
                AdminInvite(
                    id = code,
                    code = code,
                    targetEmail = cleanTargetEmail,
                    label = cleanLabel,
                    used = false,
                    createdByUid = uid
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAdminInvite(code: String): Result<Unit> {
        val path = "admin_invites/$code"
        return try {
            requireUserId()
            db.collection("admin_invites").document(code).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun setResidentRole(
        targetUid: String,
        makeAdmin: Boolean,
        roleLabel: String = "Admin / Pengurus Blok"
    ): Result<Unit> {
        val actorUid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (targetUid == actorUid && !makeAdmin) {
            return Result.failure(IllegalArgumentException("Tidak dapat menurunkan role akun admin Anda sendiri."))
        }
        val path = "residents/$targetUid"
        return try {
            val residentRef = db.collection("residents").document(targetUid)
            val snap = residentRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalArgumentException("Dokumen profil warga tidak ditemukan."))
            }
            val target = snap.toObject(
                Resident::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalArgumentException("Data warga tidak valid."))

            if (target.isPermanentPrimaryEmail && !makeAdmin) {
                return Result.failure(
                    IllegalStateException("Admin Utama ($BOOTSTRAPPED_ADMIN_EMAIL) bersifat tetap dan tidak dapat diturunkan.")
                )
            }

            val cleanRole = if (makeAdmin) "admin" else "member"
            val cleanLabel = if (makeAdmin) {
                roleLabel.trim().take(60).ifBlank { "Admin / Pengurus Blok" }
            } else {
                "Warga / Member"
            }

            val batch = db.batch()
            batch.update(
                residentRef,
                mapOf(
                    "role" to cleanRole,
                    "roleLabel" to cleanLabel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val adminRef = db.collection("admins").document(targetUid)
            if (makeAdmin) {
                batch.set(
                    adminRef,
                    mapOf(
                        "userId" to targetUid,
                        "email" to target.email.ifBlank { "$targetUid@blokcepsa.id" }.take(120),
                        "roleLabel" to cleanLabel,
                        "inviteCode" to "DIRECT_ASSIGN",
                        "grantedByUid" to actorUid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.delete(adminRef)
            }
            batch.commit().await()
            dao.upsertResident(target.copy(role = cleanRole, roleLabel = cleanLabel).toEntity())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminEditResidentProfile(
        targetUid: String,
        username: String,
        name: String,
        house: String,
        phone: String,
        hidmatEnabled: Boolean,
        makeAdmin: Boolean,
        roleLabel: String
    ): Result<Unit> {
        val actorUid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanUsername = username.trim().lowercase(Locale.ROOT).replace(" ", "").take(60)
        val cleanName = name.trim().take(120)
        val cleanHouse = house.trim().take(40)
        val cleanPhone = phone.trim().take(30)
        if (cleanUsername.length < 3 || cleanName.isBlank() || cleanHouse.isBlank()) {
            return Result.failure(IllegalArgumentException("Username min 3 huruf, nama & nomor rumah wajib diisi."))
        }
        val path = "residents/$targetUid"
        return try {
            val residentRef = db.collection("residents").document(targetUid)
            val snap = residentRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalArgumentException("Data warga tidak ditemukan."))
            }
            val target = snap.toObject(
                Resident::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalArgumentException("Data warga tidak valid."))

            if (target.isPermanentPrimaryEmail && !makeAdmin) {
                return Result.failure(
                    IllegalStateException("Admin Utama ($BOOTSTRAPPED_ADMIN_EMAIL) bersifat tetap dan tidak dapat diturunkan.")
                )
            }
            if (targetUid == actorUid && !makeAdmin) {
                return Result.failure(IllegalArgumentException("Tidak dapat menurunkan role akun Anda sendiri."))
            }

            val finalRole = if (target.isPermanentPrimaryEmail || makeAdmin) "admin" else "member"
            val finalLabel = when {
                target.isPermanentPrimaryEmail -> "Admin Utama Blok CE"
                finalRole == "admin" -> roleLabel.trim().take(60).ifBlank { "Admin / Pengurus Blok" }
                else -> "Warga / Member"
            }

            val batch = db.batch()
            batch.update(
                residentRef,
                mapOf(
                    "username" to cleanUsername,
                    "name" to cleanName,
                    "house" to cleanHouse,
                    "phone" to cleanPhone,
                    "hidmatEnabled" to hidmatEnabled,
                    "role" to finalRole,
                    "roleLabel" to finalLabel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (!target.isPermanentPrimaryEmail) {
                val adminRef = db.collection("admins").document(targetUid)
                if (finalRole == "admin") {
                    batch.set(
                        adminRef,
                        mapOf(
                            "userId" to targetUid,
                            "email" to target.email.ifBlank { "$targetUid@blokcepsa.id" }.take(120),
                            "roleLabel" to finalLabel,
                            "inviteCode" to "DIRECT_ASSIGN",
                            "grantedByUid" to actorUid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    batch.delete(adminRef)
                }
            }
            batch.commit().await()
            dao.upsertResident(
                target.copy(
                    username = cleanUsername,
                    name = cleanName,
                    house = cleanHouse,
                    phone = cleanPhone,
                    hidmatEnabled = hidmatEnabled,
                    role = finalRole,
                    roleLabel = finalLabel
                ).toEntity()
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun revokeDelegatedAdmin(targetUid: String): Result<Unit> {
        return setResidentRole(targetUid, makeAdmin = false, roleLabel = "Warga / Member")
    }

    suspend fun redeemAdminInvite(rawCode: String): Result<String> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Silakan masuk dengan Google terlebih dahulu."))
        if (!user.isEmailVerified || user.email.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Email Google harus terverifikasi untuk aktivasi admin."))
        }
        val code = rawCode.trim().uppercase(Locale.ROOT)
        if (!code.matches(Regex("^[A-Z0-9\\-]{6,32}$"))) {
            return Result.failure(IllegalArgumentException("Format kode undangan admin tidak valid."))
        }
        val userEmail = user.email!!
        val path = "admin_invites/$code"
        return try {
            val inviteRef = db.collection("admin_invites").document(code)
            val inviteSnap = inviteRef.get().await()
            if (!inviteSnap.exists()) {
                return Result.failure(IllegalArgumentException("Kode undangan admin tidak ditemukan."))
            }
            val invite = inviteSnap.toObject(AdminInvite::class.java)
                ?: return Result.failure(IllegalArgumentException("Data kode undangan tidak valid."))
            if (invite.used) {
                return Result.failure(IllegalStateException("Kode undangan ini sudah pernah digunakan."))
            }
            if (invite.targetEmail.isNotBlank() && !invite.targetEmail.equals(userEmail, ignoreCase = true)) {
                return Result.failure(IllegalStateException("Kode ini khusus untuk akun ${invite.targetEmail}."))
            }

            val residentRef = db.collection("residents").document(user.uid)
            val residentSnap = residentRef.get().await()
            val adminRef = db.collection("admins").document(user.uid)
            val assignedLabel = invite.label.ifBlank { "Admin / Pengurus Blok" }.take(60)

            val batch = db.batch()
            batch.update(
                inviteRef,
                mapOf(
                    "used" to true,
                    "usedByUid" to user.uid,
                    "usedByEmail" to userEmail,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                adminRef,
                mapOf(
                    "userId" to user.uid,
                    "email" to userEmail,
                    "roleLabel" to assignedLabel,
                    "inviteCode" to code,
                    "grantedByUid" to invite.createdByUid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            val updatedResident: Resident? = if (residentSnap.exists()) {
                batch.update(
                    residentRef,
                    mapOf(
                        "role" to "admin",
                        "roleLabel" to assignedLabel,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                residentSnap.toObject(
                    Resident::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.copy(role = "admin", roleLabel = assignedLabel)
            } else {
                val rawUsername = userEmail.substringBefore("@")
                    .lowercase(Locale.ROOT)
                    .replace(Regex("[^a-z0-9._-]"), "")
                    .ifBlank { "admin" }
                    .take(60)
                val displayName = (user.displayName ?: rawUsername).take(120)
                batch.set(
                    residentRef,
                    mapOf(
                        "userId" to user.uid,
                        "username" to rawUsername,
                        "name" to displayName,
                        "house" to "Pos Admin CE",
                        "phone" to "",
                        "email" to userEmail,
                        "role" to "admin",
                        "roleLabel" to assignedLabel,
                        "hidmatEnabled" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                Resident(
                    id = user.uid,
                    userId = user.uid,
                    username = rawUsername,
                    name = displayName,
                    house = "Pos Admin CE",
                    phone = "",
                    email = userEmail,
                    role = "admin",
                    roleLabel = assignedLabel,
                    hidmatEnabled = false
                )
            }
            batch.commit().await()
            if (updatedResident != null) {
                dao.upsertResident(updatedResident.toEntity())
            }
            Result.success(assignedLabel)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Generates `'unpaid'` monthly payment records (`iuran` and, when enabled, `hidmat`)
     * for the given `YYYY-MM` period (2026..2050) based on current [SystemSettings].
     *
     * - When executed under Primary Admin credentials, generates missing `'unpaid'` records
     *   for ALL active residents (`role != "admin"`).
     * - When executed under a regular resident session, ensures their own `'unpaid'` records
     *   exist without touching other residents (respecting zero-trust Firestore rules).
     */
    suspend fun generateMonthlyUnpaidPayments(targetMonth: String): Result<Int> {
        val user = auth.currentUser ?: return Result.success(0)
        if (!isWithinBillingHorizon(targetMonth)) {
            return Result.failure(IllegalArgumentException("Periode di luar rentang 2026-2050: $targetMonth"))
        }
        val path = "payments"
        return try {
            val settingsSnap = db.collection("system_settings").document("config").get().await()
            val settings = settingsSnap.toObject(
                SystemSettings::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: SystemSettings()
            dao.upsertSettings(settings.toEntity())

            val dateLabel = formattedToday()
            val autoNote = "Tagihan otomatis tgl 1 (${monthLabel(targetMonth)})".take(200)
            val isPrimary = isBootstrappedAdmin(user)
            val isAdmin = isPrimary || try {
                db.collection("admins").document(user.uid).get().await().exists()
            } catch (_: Exception) {
                false
            }

            val targetResidents: List<Resident>
            val existingPaymentIds: Set<String>
            val existingMonthPayments: List<Payment>

            if (isAdmin) {
                val resSnap = db.collection("residents").get().await()
                val allRes = resSnap.toObjects(
                    Resident::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                dao.replaceAllResidents(allRes.map { it.toEntity() })
                targetResidents = allRes.filter { !it.adminRole && it.userId.isNotBlank() }

                val paySnap = db.collection("payments")
                    .whereEqualTo("month", targetMonth)
                    .get()
                    .await()
                existingMonthPayments = paySnap.toObjects(
                    Payment::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                existingPaymentIds = existingMonthPayments.map { it.id }.toSet()
            } else {
                val ownSnap = db.collection("residents").document(user.uid).get().await()
                val ownResident = ownSnap.toObject(
                    Resident::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                targetResidents = if (ownResident != null && !ownResident.adminRole) {
                    listOf(ownResident)
                } else {
                    emptyList()
                }
                val ownPaySnap = db.collection("payments")
                    .whereEqualTo("userId", user.uid)
                    .get()
                    .await()
                existingMonthPayments = ownPaySnap.toObjects(
                    Payment::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).filter { it.month == targetMonth }
                existingPaymentIds = existingMonthPayments.map { it.id }.toSet()
            }

            data class UnpaidBillSpec(
                val docId: String,
                val userId: String,
                val type: String,
                val amount: Long
            )

            val toCreate = mutableListOf<UnpaidBillSpec>()
            for (resident in targetResidents) {
                val residentUid = resident.id.ifBlank { resident.userId }
                val iuranId = "${residentUid}_${targetMonth}_iuran"
                if (iuranId !in existingPaymentIds) {
                    toCreate.add(UnpaidBillSpec(iuranId, residentUid, "iuran", settings.iuranAmount))
                }
                if (resident.hidmatEnabled) {
                    val hidmatId = "${residentUid}_${targetMonth}_hidmat"
                    if (hidmatId !in existingPaymentIds) {
                        toCreate.add(UnpaidBillSpec(hidmatId, residentUid, "hidmat", settings.hidmatAmount))
                    }
                }
            }

            if (toCreate.isNotEmpty()) {
                val createdDomain = mutableListOf<Payment>()
                // Firestore WriteBatch limit is 500 operations; chunk at 450
                for (chunk in toCreate.chunked(450)) {
                    val batch = db.batch()
                    for (spec in chunk) {
                        val payload = mapOf(
                            "userId" to spec.userId,
                            "month" to targetMonth,
                            "type" to spec.type,
                            "amount" to spec.amount,
                            "status" to "unpaid",
                            "dateLabel" to dateLabel,
                            "method" to AUTO_BILL_METHOD,
                            "note" to autoNote,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        batch.set(db.collection("payments").document(spec.docId), payload)
                        createdDomain.add(
                            Payment(
                                id = spec.docId,
                                userId = spec.userId,
                                month = targetMonth,
                                type = spec.type,
                                amount = spec.amount,
                                status = "unpaid",
                                dateLabel = dateLabel,
                                method = AUTO_BILL_METHOD,
                                note = autoNote
                            )
                        )
                    }
                    batch.commit().await()
                }
                dao.upsertPayments(createdDomain.map { it.toEntity() })
            }

            if (isAdmin && targetResidents.isNotEmpty()) {
                val paidIuran = targetResidents.count { member ->
                    val memberUid = member.id.ifBlank { member.userId }
                    existingMonthPayments.any {
                        it.userId == memberUid && it.type == "iuran" && it.status == "paid"
                    }
                }
                val pendingVerificationCount = existingMonthPayments.count {
                    it.status == "pending" && it.method != AUTO_BILL_METHOD
                }
                db.collection("monthly_summaries").document(targetMonth).set(
                    mapOf(
                        "month" to targetMonth,
                        "totalMembers" to targetResidents.size.toLong(),
                        "paidIuranCount" to paidIuran.toLong(),
                        "pendingCount" to pendingVerificationCount.toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(toCreate.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Matches an incoming live DANA app notification against open resident payments.
     * Supports:
     * 1. Unique pending confirmation matching exact `amount` or matching resident `senderHint`.
     * 2. Unambiguous `'unpaid'` bill for `senderHint` with matching `amount`.
     * Updates matched Firestore payment(s) to `"paid"` automatically and logs the result in Room.
     */
    suspend fun processLiveDanaNotification(
        eventId: Long,
        amount: Long,
        senderHint: String,
        methodLabel: String,
        autoVerifyEnabled: Boolean
    ): Result<String> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Belum login."))
        return try {
            val isPrimary = isBootstrappedAdmin(user)
            val hasAdminDoc = if (!isPrimary) {
                db.collection("admins").document(user.uid).get().await().exists()
            } else {
                false
            }
            if (!isPrimary && !hasAdminDoc) {
                dao.updateDanaLiveEventStatus(eventId, "IGNORED", "Akun aktif bukan Admin")
                return Result.success("Akun bukan Admin.")
            }

            if (!autoVerifyEnabled) {
                dao.updateDanaLiveEventStatus(
                    eventId,
                    "NEED_REVIEW",
                    "Auto-verifikasi nonaktif • ${formatRupiah(amount)}"
                )
                return Result.success("Menunggu konfirmasi manual admin.")
            }

            val resSnap = db.collection("residents").get().await()
            val residents = resSnap.toObjects(
                Resident::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            val resById = residents.associateBy { it.id }

            val pendingSnap = db.collection("payments")
                .whereEqualTo("status", "pending")
                .get()
                .await()
            val openPending = pendingSnap.toObjects(
                Payment::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).filter { it.method != AUTO_BILL_METHOD }

            val cleanHint = senderHint.trim().lowercase(Locale.ROOT)
            fun matchesSender(r: Resident?): Boolean {
                if (cleanHint.length < 3 || r == null) return false
                val rName = r.name.lowercase(Locale.ROOT)
                val rUser = r.username.lowercase(Locale.ROOT)
                return rName.contains(cleanHint) || cleanHint.contains(rName) ||
                    (rUser.length >= 3 && cleanHint.contains(rUser))
            }

            // 1. Check single pending doc with matching amount (preferring sender hint match)
            val exactSingleMatches = openPending.filter { it.amount == amount }
            val chosenSingle = when {
                exactSingleMatches.size == 1 -> exactSingleMatches.first()
                exactSingleMatches.size > 1 && cleanHint.isNotBlank() -> {
                    val hintFiltered = exactSingleMatches.filter { matchesSender(resById[it.userId]) }
                    if (hintFiltered.size == 1) hintFiltered.first() else null
                }
                else -> null
            }

            // 2. Check combined Iuran + Hidmat pending bundle for one resident in same month
            val chosenBundle: List<Payment>? = if (chosenSingle == null) {
                val bundles = openPending.groupBy { it.userId to it.month }
                    .values
                    .filter { group -> group.size >= 2 && group.sumOf { it.amount } == amount }
                when {
                    bundles.size == 1 -> bundles.first()
                    bundles.size > 1 && cleanHint.isNotBlank() -> {
                        val hintBundles = bundles.filter { group ->
                            matchesSender(resById[group.first().userId])
                        }
                        if (hintBundles.size == 1) hintBundles.first() else null
                    }
                    else -> null
                }
            } else {
                null
            }

            // 3. Fallback: unambiguous `'unpaid'` bill if senderHint identifies resident + exact amount
            val chosenUnpaid: Payment? = if (chosenSingle == null && chosenBundle == null && cleanHint.length >= 3) {
                val unpaidSnap = db.collection("payments")
                    .whereEqualTo("status", "unpaid")
                    .get()
                    .await()
                val unpaidMatches = unpaidSnap.toObjects(
                    Payment::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).filter { it.amount == amount && matchesSender(resById[it.userId]) }
                if (unpaidMatches.size == 1) unpaidMatches.first() else null
            } else {
                null
            }

            val targets = chosenBundle ?: listOfNotNull(chosenSingle ?: chosenUnpaid)
            if (targets.isEmpty()) {
                val statusLabel = if (exactSingleMatches.size > 1) "NEED_REVIEW" else "UNMATCHED"
                val detail = if (exactSingleMatches.size > 1) {
                    "${exactSingleMatches.size} konfirmasi nominal ${formatRupiah(amount)} cocok — pilih manual"
                } else {
                    "Belum ada tagihan cocok untuk ${formatRupiah(amount)}"
                }
                dao.updateDanaLiveEventStatus(eventId, statusLabel, detail)
                return Result.success(detail)
            }

            val noteText = buildString {
                append("Verifikasi DANA Live otomatis")
                if (senderHint.isNotBlank()) append(" • Dari: $senderHint")
            }.take(200)
            val dateLabel = formattedToday()

            val batch = db.batch()
            for (pay in targets) {
                batch.update(
                    db.collection("payments").document(pay.id),
                    mapOf(
                        "status" to "paid",
                        "dateLabel" to dateLabel,
                        "method" to methodLabel.take(50),
                        "note" to noteText,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()

            val residentName = resById[targets.first().userId]?.name ?: targets.first().userId
            val summaryMsg = "LUNAS OTOMATIS: $residentName (${targets.joinToString("+") { it.type.uppercase() }} ${monthLabel(targets.first().month)})"
            dao.updateDanaLiveEventStatus(eventId, "AUTO_VERIFIED", summaryMsg)
            Result.success(summaryMsg)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            dao.updateDanaLiveEventStatus(eventId, "ERROR", e.localizedMessage ?: "Gagal sinkron DANA live")
            handleFirestoreError(e, OperationType.WRITE, "payments")
            Result.failure(e)
        }
    }

    suspend fun verifyLiveDanaForPayment(eventId: Long, paymentId: String, senderHint: String): Result<Unit> {
        val path = "payments/$paymentId"
        return try {
            requireUserId()
            val noteText = buildString {
                append("Verifikasi DANA Live (Konfirmasi Admin)")
                if (senderHint.isNotBlank()) append(" • Dari: $senderHint")
            }.take(200)
            db.collection("payments").document(paymentId).update(
                mapOf(
                    "status" to "paid",
                    "dateLabel" to formattedToday(),
                    "method" to "DANA Live",
                    "note" to noteText,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            dao.updateDanaLiveEventStatus(eventId, "AUTO_VERIFIED", "Diverifikasi manual dari mutasi DANA live")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private data class WebhookMatchOutcome(
        val status: String,
        val targets: List<Payment>,
        val summary: String
    )

    private fun matchOpenPayments(
        amount: Long,
        senderName: String,
        senderPhone: String,
        houseHint: String,
        monthHint: String,
        paymentTypeHint: String,
        residents: List<Resident>,
        allOpenPayments: List<Payment>
    ): WebhookMatchOutcome {
        val resById = residents.associateBy { it.id }
        val cleanName = senderName.trim().lowercase(Locale.ROOT)
        val cleanHouse = houseHint.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), "")
        val cleanPhone = senderPhone.filter { it.isDigit() }
        val cleanMonth = monthHint.trim()
        val cleanType = paymentTypeHint.trim().lowercase(Locale.ROOT)

        fun matchesResident(r: Resident?): Boolean {
            if (r == null) return false
            if (cleanName.length >= 3) {
                val rName = r.name.lowercase(Locale.ROOT)
                val rUser = r.username.lowercase(Locale.ROOT)
                if ((rName.isNotBlank() && (rName.contains(cleanName) || cleanName.contains(rName))) ||
                    (rUser.length >= 3 && cleanName.contains(rUser))
                ) {
                    return true
                }
            }
            if (cleanHouse.length >= 2) {
                val rHouse = r.house.lowercase(Locale.ROOT).replace(Regex("\\s+"), "")
                if (rHouse.isNotBlank() && (rHouse.contains(cleanHouse) || cleanHouse.contains(rHouse))) {
                    return true
                }
            }
            if (cleanPhone.length >= 6) {
                val rPhone = r.phone.filter { it.isDigit() }
                if (rPhone.isNotBlank() && rPhone.endsWith(cleanPhone.takeLast(6))) {
                    return true
                }
            }
            return false
        }

        fun applyHints(list: List<Payment>): List<Payment> = list.filter { p ->
            val monthOk = cleanMonth.isBlank() || p.month == cleanMonth
            val typeOk = cleanType.isBlank() || cleanType == "both" || p.type == cleanType
            monthOk && typeOk
        }

        val openPending = applyHints(
            allOpenPayments.filter { it.status == "pending" && it.method != AUTO_BILL_METHOD }
        )
        val openUnpaid = applyHints(
            allOpenPayments.filter {
                it.status == "unpaid" || (it.status == "pending" && it.method == AUTO_BILL_METHOD)
            }
        )

        val exactSingle = openPending.filter { it.amount == amount }
        val chosenSingle = when {
            exactSingle.size == 1 -> exactSingle.first()
            exactSingle.size > 1 -> {
                val hinted = exactSingle.filter { matchesResident(resById[it.userId]) }
                if (hinted.size == 1) hinted.first() else null
            }
            else -> null
        }

        val chosenBundle: List<Payment>? = if (chosenSingle == null) {
            val bundles = openPending.groupBy { it.userId to it.month }
                .values
                .filter { grp -> grp.size >= 2 && grp.sumOf { it.amount } == amount }
            when {
                bundles.size == 1 -> bundles.first()
                bundles.size > 1 -> {
                    val hintedBundles = bundles.filter { grp -> matchesResident(resById[grp.first().userId]) }
                    if (hintedBundles.size == 1) hintedBundles.first() else null
                }
                else -> null
            }
        } else {
            null
        }

        val hasIdentityHint = cleanName.length >= 3 || cleanHouse.length >= 2 || cleanPhone.length >= 6
        val chosenUnpaidTargets: List<Payment> = if (chosenSingle == null && chosenBundle == null && hasIdentityHint) {
            val resUnpaid = openUnpaid.filter { matchesResident(resById[it.userId]) }
            val singleUnpaid = resUnpaid.filter { it.amount == amount }
            if (singleUnpaid.size == 1) {
                singleUnpaid
            } else {
                val bundlesUnpaid = resUnpaid.groupBy { it.userId to it.month }
                    .values
                    .filter { grp -> grp.size >= 2 && grp.sumOf { it.amount } == amount }
                if (bundlesUnpaid.size == 1) bundlesUnpaid.first() else emptyList()
            }
        } else {
            emptyList()
        }

        val targets = chosenBundle ?: if (chosenSingle != null) listOf(chosenSingle) else chosenUnpaidTargets
        if (targets.isEmpty()) {
            return if (exactSingle.size > 1) {
                WebhookMatchOutcome(
                    status = "NEED_REVIEW",
                    targets = emptyList(),
                    summary = "${exactSingle.size} konfirmasi nominal ${formatRupiah(amount)} cocok — pilih manual"
                )
            } else {
                WebhookMatchOutcome(
                    status = "UNMATCHED",
                    targets = emptyList(),
                    summary = "Belum ada tagihan cocok untuk ${formatRupiah(amount)}"
                )
            }
        }

        val ownerName = resById[targets.first().userId]?.name ?: targets.first().userId
        val summaryMsg = "LUNAS WEBHOOK: $ownerName (${targets.joinToString("+") { it.type.uppercase() }} ${monthLabel(targets.first().month)})".take(250)
        return WebhookMatchOutcome(
            status = "AUTO_VERIFIED",
            targets = targets,
            summary = summaryMsg
        )
    }

    fun observeDanaWebhookConfig(): Flow<DanaWebhookConfig> = flow {
        val activeUid = requireUserId()
        val path = "admin_configs/dana_webhook"
        emitAll(
            db.collection("admin_configs").document("dana_webhook")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        DanaWebhookConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: DanaWebhookConfig()
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == activeUid) {
                        handleFirestoreError(ex, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveDanaWebhookConfig(config: DanaWebhookConfig): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = "admin_configs/dana_webhook"
        return try {
            val docRef = db.collection("admin_configs").document("dana_webhook")
            val exists = docRef.get().await().exists()
            val cleanUrl = config.webhookUrl.trim().take(500)
            val cleanSecret = config.webhookSecret.trim().take(200)
            if (exists) {
                docRef.update(
                    mapOf(
                        "webhookUrl" to cleanUrl,
                        "webhookSecret" to cleanSecret,
                        "relayFromAndroidEnabled" to config.relayFromAndroidEnabled,
                        "autoVerifyWebhookEvents" to config.autoVerifyWebhookEvents,
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "webhookUrl" to cleanUrl,
                        "webhookSecret" to cleanSecret,
                        "relayFromAndroidEnabled" to config.relayFromAndroidEnabled,
                        "autoVerifyWebhookEvents" to config.autoVerifyWebhookEvents,
                        "updatedByUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeDanaWebhookEvents(): Flow<List<DanaWebhookEvent>> = flow {
        val activeUid = requireUserId()
        val path = "dana_webhook_events"
        emitAll(
            db.collection("dana_webhook_events")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DanaWebhookEvent::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == activeUid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun claimAndVerifyWebhookEvent(event: DanaWebhookEvent): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = event.id.ifBlank { event.referenceId }
        if (docId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID event webhook tidak valid."))
        }
        val path = "dana_webhook_events/$docId"
        return try {
            val resSnap = db.collection("residents").get().await()
            val residents = resSnap.toObjects(
                Resident::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            val openSnap = db.collection("payments")
                .whereIn("status", listOf("pending", "unpaid"))
                .get()
                .await()
            val openPayments = openSnap.toObjects(
                Payment::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )

            val outcome = matchOpenPayments(
                amount = event.amount,
                senderName = event.senderName,
                senderPhone = event.senderPhone,
                houseHint = event.houseHint,
                monthHint = event.monthHint,
                paymentTypeHint = event.paymentTypeHint,
                residents = residents,
                allOpenPayments = openPayments
            )

            val batch = db.batch()
            val dateLabel = formattedToday()
            val noteText = buildString {
                append("Verifikasi DANA Webhook (${event.referenceId.ifBlank { docId }})")
                if (event.senderName.isNotBlank()) append(" • Dari: ${event.senderName}")
            }.take(200)

            for (pay in outcome.targets) {
                batch.update(
                    db.collection("payments").document(pay.id),
                    mapOf(
                        "status" to "paid",
                        "dateLabel" to dateLabel,
                        "method" to "DANA Webhook Live",
                        "note" to noteText,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.update(
                db.collection("dana_webhook_events").document(docId),
                mapOf(
                    "status" to outcome.status,
                    "matchedPaymentId" to (outcome.targets.firstOrNull()?.id ?: ""),
                    "matchedSummary" to outcome.summary.take(250),
                    "verifiedByUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(outcome.summary)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun verifyWebhookEventForPayment(
        eventId: String,
        paymentId: String,
        senderName: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = "dana_webhook_events/$eventId"
        return try {
            val noteText = buildString {
                append("Verifikasi DANA Webhook (Manual Admin)")
                if (senderName.isNotBlank()) append(" • Dari: $senderName")
            }.take(200)
            val batch = db.batch()
            batch.update(
                db.collection("payments").document(paymentId),
                mapOf(
                    "status" to "paid",
                    "dateLabel" to formattedToday(),
                    "method" to "DANA Webhook Live",
                    "note" to noteText,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("dana_webhook_events").document(eventId),
                mapOf(
                    "status" to "AUTO_VERIFIED",
                    "matchedPaymentId" to paymentId.take(128),
                    "matchedSummary" to "Diverifikasi manual dari event webhook ($paymentId)".take(250),
                    "verifiedByUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteDanaWebhookEvent(eventId: String): Result<Unit> {
        val path = "dana_webhook_events/$eventId"
        return try {
            requireUserId()
            db.collection("dana_webhook_events").document(eventId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
