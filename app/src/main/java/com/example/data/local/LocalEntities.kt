package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Announcement
import com.example.data.model.MonthlySummary
import com.example.data.model.Payment
import com.example.data.model.Resident
import com.example.data.model.SystemSettings

@Entity(tableName = "system_settings")
data class SystemSettingsEntity(
    @PrimaryKey val id: String = "config",
    val blokName: String,
    val subtitle: String,
    val danaNumber: String,
    val danaAccountName: String,
    val customQrisData: String,
    val iuranAmount: Long,
    val hidmatAmount: Long,
    val reminderEnabled: Boolean,
    val reminderTitle: String,
    val reminderMessage: String,
    val pushBlastId: Long,
    val pushTargetUserId: String,
    val syncedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain() = SystemSettings(
        id = id,
        blokName = blokName,
        subtitle = subtitle,
        danaNumber = danaNumber,
        danaAccountName = danaAccountName,
        customQrisData = customQrisData,
        iuranAmount = iuranAmount,
        hidmatAmount = hidmatAmount,
        reminderEnabled = reminderEnabled,
        reminderTitle = reminderTitle,
        reminderMessage = reminderMessage,
        pushBlastId = pushBlastId,
        pushTargetUserId = pushTargetUserId
    )
}

fun SystemSettings.toEntity() = SystemSettingsEntity(
    id = id.ifBlank { "config" },
    blokName = blokName,
    subtitle = subtitle,
    danaNumber = danaNumber,
    danaAccountName = danaAccountName,
    customQrisData = customQrisData,
    iuranAmount = iuranAmount,
    hidmatAmount = hidmatAmount,
    reminderEnabled = reminderEnabled,
    reminderTitle = reminderTitle,
    reminderMessage = reminderMessage,
    pushBlastId = pushBlastId,
    pushTargetUserId = pushTargetUserId
)

@Entity(tableName = "residents")
data class ResidentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val username: String,
    val name: String,
    val house: String,
    val phone: String,
    val email: String,
    val role: String,
    val roleLabel: String,
    val hidmatEnabled: Boolean
) {
    fun toDomain() = Resident(
        id = id,
        userId = userId,
        username = username,
        name = name,
        house = house,
        phone = phone,
        email = email,
        role = role,
        roleLabel = roleLabel,
        hidmatEnabled = hidmatEnabled
    )
}

fun Resident.toEntity() = ResidentEntity(
    id = id.ifBlank { userId },
    userId = userId,
    username = username,
    name = name,
    house = house,
    phone = phone,
    email = email,
    role = role,
    roleLabel = roleLabel,
    hidmatEnabled = hidmatEnabled
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val month: String,
    val type: String,
    val amount: Long,
    val status: String,
    val dateLabel: String,
    val method: String,
    val note: String
) {
    fun toDomain() = Payment(
        id = id,
        userId = userId,
        month = month,
        type = type,
        amount = amount,
        status = status,
        dateLabel = dateLabel,
        method = method,
        note = note,
        offlineQueued = false
    )
}

fun Payment.toEntity() = PaymentEntity(
    id = id,
    userId = userId,
    month = month,
    type = type,
    amount = amount,
    status = status,
    dateLabel = dateLabel,
    method = method,
    note = note
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val imageUrl: String,
    val content: String,
    val urgent: Boolean,
    val published: Boolean,
    val displayDate: String,
    val authorId: String,
    val authorName: String,
    val createdAtMillis: Long
) {
    fun toDomain() = Announcement(
        id = id,
        title = title,
        category = category,
        imageUrl = imageUrl,
        content = content,
        urgent = urgent,
        published = published,
        displayDate = displayDate,
        authorId = authorId,
        authorName = authorName
    )
}

fun Announcement.toEntity() = AnnouncementEntity(
    id = id,
    title = title,
    category = category,
    imageUrl = imageUrl,
    content = content,
    urgent = urgent,
    published = published,
    displayDate = displayDate,
    authorId = authorId,
    authorName = authorName,
    createdAtMillis = createdAt?.toDate()?.time ?: System.currentTimeMillis()
)

@Entity(tableName = "monthly_summaries")
data class MonthlySummaryEntity(
    @PrimaryKey val month: String,
    val totalMembers: Long,
    val paidIuranCount: Long,
    val pendingCount: Long
) {
    fun toDomain() = MonthlySummary(
        id = month,
        month = month,
        totalMembers = totalMembers,
        paidIuranCount = paidIuranCount,
        pendingCount = pendingCount
    )
}

fun MonthlySummary.toEntity() = MonthlySummaryEntity(
    month = month,
    totalMembers = totalMembers,
    paidIuranCount = paidIuranCount,
    pendingCount = pendingCount
)

@Entity(tableName = "pending_payments")
data class PendingPaymentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val month: String,
    val type: String,
    val amount: Long,
    val dateLabel: String,
    val method: String,
    val note: String,
    val queuedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain() = Payment(
        id = id,
        userId = userId,
        month = month,
        type = type,
        amount = amount,
        status = "pending",
        dateLabel = dateLabel,
        method = method,
        note = note,
        offlineQueued = true
    )
}

@Entity(tableName = "dana_live_events")
data class DanaLiveEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val title: String,
    val content: String,
    val parsedAmount: Long,
    val senderHint: String,
    val status: String,
    val matchedSummary: String,
    val receivedAtMillis: Long = System.currentTimeMillis()
)
