package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BlokCeDao {
    @Query("SELECT * FROM system_settings WHERE id = 'config' LIMIT 1")
    fun observeSettings(): Flow<SystemSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: SystemSettingsEntity)

    @Query("SELECT * FROM residents WHERE userId = :uid LIMIT 1")
    fun observeResident(uid: String): Flow<ResidentEntity?>

    @Query("SELECT * FROM residents ORDER BY house ASC, name ASC")
    fun observeAllResidents(): Flow<List<ResidentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResident(resident: ResidentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResidents(residents: List<ResidentEntity>)

    @Query("DELETE FROM residents")
    suspend fun clearResidents()

    @Transaction
    suspend fun replaceAllResidents(residents: List<ResidentEntity>) {
        clearResidents()
        upsertResidents(residents)
    }

    @Query("SELECT * FROM payments WHERE userId = :uid ORDER BY month DESC")
    fun observeUserPayments(uid: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY month DESC")
    fun observeAllPayments(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayments(payments: List<PaymentEntity>)

    @Query("DELETE FROM payments WHERE userId = :uid")
    suspend fun clearUserPayments(uid: String)

    @Query("DELETE FROM payments")
    suspend fun clearAllPayments()

    @Transaction
    suspend fun replaceUserPayments(uid: String, payments: List<PaymentEntity>) {
        clearUserPayments(uid)
        upsertPayments(payments)
    }

    @Transaction
    suspend fun replaceAllPayments(payments: List<PaymentEntity>) {
        clearAllPayments()
        upsertPayments(payments)
    }

    @Query("SELECT * FROM announcements ORDER BY createdAtMillis DESC")
    fun observeAnnouncements(): Flow<List<AnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnnouncements(items: List<AnnouncementEntity>)

    @Query("DELETE FROM announcements")
    suspend fun clearAnnouncements()

    @Transaction
    suspend fun replaceAnnouncements(items: List<AnnouncementEntity>) {
        clearAnnouncements()
        upsertAnnouncements(items)
    }

    @Query("SELECT * FROM monthly_summaries WHERE month = :month LIMIT 1")
    fun observeMonthlySummary(month: String): Flow<MonthlySummaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMonthlySummary(summary: MonthlySummaryEntity)

    @Query("SELECT * FROM pending_payments WHERE userId = :uid ORDER BY queuedAtMillis DESC")
    fun observePendingPayments(uid: String): Flow<List<PendingPaymentEntity>>

    @Query("SELECT * FROM pending_payments WHERE userId = :uid ORDER BY queuedAtMillis ASC")
    suspend fun getPendingPaymentsOnce(uid: String): List<PendingPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPendingPayment(item: PendingPaymentEntity)

    @Query("DELETE FROM pending_payments WHERE id = :id")
    suspend fun deletePendingPayment(id: String)

    @Query("DELETE FROM residents")
    suspend fun clearAllResidentsCache()

    @Query("DELETE FROM payments")
    suspend fun clearAllPaymentsCache()

    @Query("SELECT * FROM dana_live_events ORDER BY receivedAtMillis DESC LIMIT 50")
    fun observeDanaLiveEvents(): Flow<List<DanaLiveEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDanaLiveEvent(event: DanaLiveEventEntity): Long

    @Query("UPDATE dana_live_events SET status = :status, matchedSummary = :matchedSummary WHERE id = :id")
    suspend fun updateDanaLiveEventStatus(id: Long, status: String, matchedSummary: String)

    @Query("DELETE FROM dana_live_events")
    suspend fun clearDanaLiveEvents()
}
