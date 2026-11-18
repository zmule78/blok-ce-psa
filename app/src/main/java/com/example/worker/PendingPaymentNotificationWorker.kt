package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.Payment
import com.example.data.model.Resident
import com.example.data.model.SystemSettings
import com.example.data.model.currentBillingPeriodCode
import com.example.notification.HimbauanNotificationHelper
import com.example.notification.evaluateAdminPendingOverviewNotification
import com.example.notification.evaluateResidentPendingNotification
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PendingPaymentNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private companion object {
        const val TAG = "PendingPayNotifWorker"
    }

    override suspend fun doWork(): Result {
        val forceNotify = inputData.getBoolean(PendingPaymentNotificationScheduler.KEY_FORCE_NOTIFY, false)
        if (!forceNotify && !PendingPaymentNotificationScheduler.isServiceEnabled(applicationContext)) {
            return Result.success()
        }

        return try {
            val user = try {
                if (FirebaseApp.getApps(applicationContext).isEmpty()) {
                    FirebaseApp.initializeApp(applicationContext)
                }
                if (FirebaseApp.getApps(applicationContext).isNotEmpty()) {
                    Firebase.auth.currentUser
                } else {
                    null
                }
            } catch (e: IllegalStateException) {
                Log.w(TAG, "FirebaseApp unavailable during pending notification check.", e)
                null
            }

            if (user == null) {
                return Result.success()
            }

            val dao = AppDatabase.getInstance(applicationContext).blokCeDao()
            val databaseId = applicationContext.getString(R.string.firestore_database_id)
            val db = runCatching { FirebaseFirestore.getInstance(databaseId) }.getOrNull()

            val currentMonth = currentBillingPeriodCode()
            val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            val settings = runCatching {
                db?.collection("system_settings")?.document("config")?.get()?.await()
                    ?.toObject(SystemSettings::class.java)
            }.getOrNull()
                ?: dao.observeSettings().firstOrNull()?.toDomain()
                ?: SystemSettings()

            val resident = runCatching {
                db?.collection("residents")?.document(user.uid)?.get()?.await()
                    ?.toObject(Resident::class.java)
            }.getOrNull()
                ?: dao.observeResident(user.uid).firstOrNull()?.toDomain()

            if (resident == null) {
                PendingPaymentNotificationScheduler.recordCheckResult(applicationContext, currentMonth, 0)
                return Result.success()
            }

            val payload = if (resident.adminRole) {
                val monthPayments = runCatching {
                    db?.collection("payments")
                        ?.whereEqualTo("month", currentMonth)
                        ?.get()?.await()
                        ?.toObjects(Payment::class.java)
                }.getOrNull()
                    ?: dao.observeAllPayments().firstOrNull()?.map { it.toDomain() }.orEmpty()

                evaluateAdminPendingOverviewNotification(
                    monthPayments = monthPayments,
                    currentMonth = currentMonth,
                    dayKey = dayKey
                )
            } else {
                val userMonthPayments = runCatching {
                    db?.collection("payments")
                        ?.whereEqualTo("userId", user.uid)
                        ?.whereEqualTo("month", currentMonth)
                        ?.get()?.await()
                        ?.toObjects(Payment::class.java)
                }.getOrNull()
                    ?: dao.observeUserPayments(user.uid).firstOrNull()?.map { it.toDomain() }.orEmpty()

                evaluateResidentPendingNotification(
                    resident = resident,
                    monthPayments = userMonthPayments,
                    currentMonth = currentMonth,
                    dayKey = dayKey,
                    settings = settings
                )
            }

            if (payload != null) {
                HimbauanNotificationHelper.dispatchPendingStatusNotification(
                    context = applicationContext,
                    alertKey = payload.alertKey,
                    title = payload.title,
                    message = payload.message,
                    targetTabName = payload.targetTabName,
                    forceNotify = forceNotify
                )
            }

            PendingPaymentNotificationScheduler.recordCheckResult(
                context = applicationContext,
                period = currentMonth,
                pendingCount = payload?.pendingCount ?: 0
            )

            Result.success(
                workDataOf(
                    "period" to currentMonth,
                    "pending_count" to (payload?.pendingCount ?: 0)
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Error checking pending payments for push notification.", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
