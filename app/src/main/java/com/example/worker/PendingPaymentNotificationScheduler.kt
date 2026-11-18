package com.example.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class PendingNotifierStatus(
    val enabled: Boolean = true,
    val lastCheckAtMillis: Long = 0L,
    val lastNotifiedPeriod: String = "",
    val lastPendingCount: Int = 0
)

object PendingPaymentNotificationScheduler {
    const val UNIQUE_PERIODIC_WORK_NAME = "blokce_pending_payment_periodic_notifier"
    const val UNIQUE_IMMEDIATE_WORK_NAME = "blokce_pending_payment_immediate_notifier"
    const val TAG_PENDING_NOTIFIER = "pending_payment_notifier"
    const val KEY_FORCE_NOTIFY = "force_notify"

    private const val PREFS_NAME = "blokce_pending_notifier_prefs"
    private const val PREF_ENABLED = "service_enabled"
    private const val PREF_LAST_CHECK_MILLIS = "last_check_at_millis"
    private const val PREF_LAST_PERIOD = "last_notified_period"
    private const val PREF_LAST_PENDING_COUNT = "last_pending_count"

    fun isServiceEnabled(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREF_ENABLED, true)

    fun setServiceEnabled(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_ENABLED, enabled)
            .apply()
        if (enabled) {
            ensureScheduled(appContext)
        } else {
            WorkManager.getInstance(appContext).cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
        }
    }

    fun ensureScheduled(context: Context) {
        val appContext = context.applicationContext
        if (!isServiceEnabled(appContext)) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<PendingPaymentNotificationWorker>(
            12, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .addTag(TAG_PENDING_NOTIFIER)
            .setInputData(workDataOf(KEY_FORCE_NOTIFY to false))
            .build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    fun enqueueImmediateCheck(context: Context, forceNotify: Boolean = true) {
        val appContext = context.applicationContext
        val request = OneTimeWorkRequestBuilder<PendingPaymentNotificationWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .addTag(TAG_PENDING_NOTIFIER)
            .setInputData(workDataOf(KEY_FORCE_NOTIFY to forceNotify))
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            UNIQUE_IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun recordCheckResult(
        context: Context,
        period: String,
        pendingCount: Int,
        checkedAtMillis: Long = System.currentTimeMillis()
    ) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(PREF_LAST_CHECK_MILLIS, checkedAtMillis)
            .putString(PREF_LAST_PERIOD, period)
            .putInt(PREF_LAST_PENDING_COUNT, pendingCount)
            .apply()
    }

    fun getStatus(context: Context): PendingNotifierStatus {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return PendingNotifierStatus(
            enabled = prefs.getBoolean(PREF_ENABLED, true),
            lastCheckAtMillis = prefs.getLong(PREF_LAST_CHECK_MILLIS, 0L),
            lastNotifiedPeriod = prefs.getString(PREF_LAST_PERIOD, "") ?: "",
            lastPendingCount = prefs.getInt(PREF_LAST_PENDING_COUNT, 0)
        )
    }

    fun formatLastCheck(millis: Long): String {
        if (millis <= 0L) return "Belum berjalan"
        return SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date(millis))
    }
}
