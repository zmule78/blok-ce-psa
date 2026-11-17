package com.example.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.model.MAX_BILLING_YEAR
import com.example.data.model.currentBillingPeriodCode
import com.example.data.model.isWithinBillingHorizon
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class BillingWorkerStatus(
    val lastRunPeriod: String = "",
    val lastRunAtMillis: Long = 0L,
    val lastGeneratedCount: Int = 0,
    val nextRunAtMillis: Long = 0L
)

object MonthlyBillingScheduler {
    const val UNIQUE_MONTHLY_WORK_NAME = "blokce_monthly_billing_first_day"
    const val UNIQUE_CATCHUP_WORK_NAME = "blokce_monthly_billing_catchup"
    const val TAG_MONTHLY_BILLING = "monthly_billing_first_day"

    const val KEY_TARGET_MONTH = "target_month"
    const val KEY_IS_SCHEDULED_RUN = "is_scheduled_run"
    const val KEY_GENERATED_COUNT = "generated_count"

    private const val PREFS_NAME = "blokce_billing_worker_prefs"
    private const val PREF_LAST_PERIOD = "last_run_period"
    private const val PREF_LAST_RUN_MILLIS = "last_run_at_millis"
    private const val PREF_LAST_COUNT = "last_generated_count"
    private const val PREF_NEXT_RUN_MILLIS = "next_run_at_millis"

    /**
     * Computes the exact epoch millis for the 1st day of the next month at 00:05:00 local time.
     * Using calendar-aware month math avoids fixed 30-day drift across 28/29/30/31-day months
     * across all months through the year 2050.
     */
    fun computeNextFirstOfMonthMillis(
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }

    fun computeDelayUntilNextFirstOfMonthMillis(
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val nextMillis = computeNextFirstOfMonthMillis(nowMillis, timeZone)
        return (nextMillis - nowMillis).coerceAtLeast(60_000L)
    }

    fun isFirstDayOfMonth(
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        return cal.get(Calendar.DAY_OF_MONTH) == 1
    }

    fun ensureMonthlyBillingScheduled(context: Context) {
        val appContext = context.applicationContext
        scheduleNextFirstOfMonth(appContext, forceReplace = false)

        val nowCal = Calendar.getInstance()
        val nowYear = nowCal.get(Calendar.YEAR)
        val nowMonth = nowCal.get(Calendar.MONTH) + 1
        val currentPeriod = currentBillingPeriodCode(nowCal)
        val status = getStatus(appContext)
        if (isFirstDayOfMonth(nowCal.timeInMillis) &&
            isWithinBillingHorizon(nowYear, nowMonth) &&
            status.lastRunPeriod != currentPeriod
        ) {
            enqueueImmediateRun(
                context = appContext,
                targetMonth = currentPeriod,
                uniqueWorkName = UNIQUE_CATCHUP_WORK_NAME,
                policy = ExistingWorkPolicy.KEEP,
                isScheduledRun = false
            )
        }
    }

    fun scheduleNextFirstOfMonth(context: Context, forceReplace: Boolean) {
        val appContext = context.applicationContext
        val now = System.currentTimeMillis()
        val nextRunMillis = computeNextFirstOfMonthMillis(now)
        val nextCal = Calendar.getInstance().apply { timeInMillis = nextRunMillis }
        val nextYear = nextCal.get(Calendar.YEAR)
        val nextMonth = nextCal.get(Calendar.MONTH) + 1

        if (nextYear > MAX_BILLING_YEAR || !isWithinBillingHorizon(nextYear, nextMonth)) {
            WorkManager.getInstance(appContext).cancelUniqueWork(UNIQUE_MONTHLY_WORK_NAME)
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(PREF_NEXT_RUN_MILLIS, 0L)
                .apply()
            return
        }

        val delayMillis = (nextRunMillis - now).coerceAtLeast(60_000L)
        val targetPeriod = currentBillingPeriodCode(nextCal)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<MonthlyBillingWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .addTag(TAG_MONTHLY_BILLING)
            .setInputData(
                workDataOf(
                    KEY_TARGET_MONTH to targetPeriod,
                    KEY_IS_SCHEDULED_RUN to true
                )
            )
            .build()

        val policy = if (forceReplace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            UNIQUE_MONTHLY_WORK_NAME,
            policy,
            request
        )

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingNext = prefs.getLong(PREF_NEXT_RUN_MILLIS, 0L)
        if (forceReplace || existingNext <= now) {
            prefs.edit().putLong(PREF_NEXT_RUN_MILLIS, nextRunMillis).apply()
        }
    }

    fun enqueueImmediateRun(
        context: Context,
        targetMonth: String,
        uniqueWorkName: String = UNIQUE_CATCHUP_WORK_NAME,
        policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE,
        isScheduledRun: Boolean = false
    ) {
        val appContext = context.applicationContext
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<MonthlyBillingWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .addTag(TAG_MONTHLY_BILLING)
            .setInputData(
                workDataOf(
                    KEY_TARGET_MONTH to targetMonth,
                    KEY_IS_SCHEDULED_RUN to isScheduledRun
                )
            )
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(uniqueWorkName, policy, request)
    }

    fun recordRunResult(
        context: Context,
        period: String,
        generatedCount: Int,
        runAtMillis: Long = System.currentTimeMillis()
    ) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_LAST_PERIOD, period)
            .putLong(PREF_LAST_RUN_MILLIS, runAtMillis)
            .putInt(PREF_LAST_COUNT, generatedCount)
            .apply()
    }

    fun getStatus(context: Context): BillingWorkerStatus {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedNext = prefs.getLong(PREF_NEXT_RUN_MILLIS, 0L)
        val effectiveNext = if (savedNext > System.currentTimeMillis()) {
            savedNext
        } else {
            computeNextFirstOfMonthMillis()
        }
        return BillingWorkerStatus(
            lastRunPeriod = prefs.getString(PREF_LAST_PERIOD, "") ?: "",
            lastRunAtMillis = prefs.getLong(PREF_LAST_RUN_MILLIS, 0L),
            lastGeneratedCount = prefs.getInt(PREF_LAST_COUNT, 0),
            nextRunAtMillis = effectiveNext
        )
    }

    fun formatTimestamp(millis: Long): String {
        if (millis <= 0L) return "Belum pernah"
        return SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date(millis))
    }
}
