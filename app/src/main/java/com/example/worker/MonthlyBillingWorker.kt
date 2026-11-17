package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.currentBillingPeriodCode
import com.example.data.model.isWithinBillingHorizon
import com.example.data.repository.BlokCeRepository
import com.example.data.repository.ConnectivityObserver
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException

class MonthlyBillingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private companion object {
        const val TAG = "MonthlyBillingWorker"
        const val MAX_RETRY_ATTEMPTS = 4
    }

    override suspend fun doWork(): Result {
        val targetMonth = inputData.getString(MonthlyBillingScheduler.KEY_TARGET_MONTH)
            ?.takeIf { isWithinBillingHorizon(it) }
            ?: currentBillingPeriodCode()
        val isScheduledRun = inputData.getBoolean(MonthlyBillingScheduler.KEY_IS_SCHEDULED_RUN, true)

        if (!isWithinBillingHorizon(targetMonth)) {
            Log.i(TAG, "Skipping billing for $targetMonth: outside 2026-2050 horizon.")
            return Result.success(
                workDataOf(
                    MonthlyBillingScheduler.KEY_TARGET_MONTH to targetMonth,
                    MonthlyBillingScheduler.KEY_GENERATED_COUNT to 0
                )
            )
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
                Log.w(TAG, "FirebaseApp unavailable during worker run for $targetMonth.", e)
                null
            }
            if (user == null) {
                Log.i(TAG, "Skipping auto-billing for $targetMonth: no authenticated Google session.")
                if (isScheduledRun) {
                    MonthlyBillingScheduler.scheduleNextFirstOfMonth(applicationContext, forceReplace = true)
                }
                return Result.success(
                    workDataOf(
                        MonthlyBillingScheduler.KEY_TARGET_MONTH to targetMonth,
                        MonthlyBillingScheduler.KEY_GENERATED_COUNT to 0
                    )
                )
            }

            val databaseId = applicationContext.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            val dao = AppDatabase.getInstance(applicationContext).blokCeDao()
            val connectivity = ConnectivityObserver(applicationContext)
            val repository = BlokCeRepository(db, dao, connectivity, Firebase.auth)

            val genResult = repository.generateMonthlyUnpaidPayments(targetMonth)
            if (genResult.isSuccess) {
                val count = genResult.getOrDefault(0)
                MonthlyBillingScheduler.recordRunResult(applicationContext, targetMonth, count)
                if (isScheduledRun) {
                    MonthlyBillingScheduler.scheduleNextFirstOfMonth(applicationContext, forceReplace = true)
                }
                Log.i(TAG, "Monthly billing completed for $targetMonth: $count unpaid records created.")
                Result.success(
                    workDataOf(
                        MonthlyBillingScheduler.KEY_TARGET_MONTH to targetMonth,
                        MonthlyBillingScheduler.KEY_GENERATED_COUNT to count
                    )
                )
            } else {
                val err = genResult.exceptionOrNull()
                if (isTransientFirestoreError(err) && runAttemptCount < MAX_RETRY_ATTEMPTS) {
                    Log.w(TAG, "Transient error generating monthly bills, scheduling retry.", err)
                    Result.retry()
                } else {
                    if (isScheduledRun) {
                        MonthlyBillingScheduler.scheduleNextFirstOfMonth(applicationContext, forceReplace = true)
                    }
                    Log.e(TAG, "Failed generating monthly bills for $targetMonth.", err)
                    Result.failure()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isTransientFirestoreError(e) && runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Log.w(TAG, "Transient exception in MonthlyBillingWorker, retrying.", e)
                Result.retry()
            } else {
                if (isScheduledRun) {
                    MonthlyBillingScheduler.scheduleNextFirstOfMonth(applicationContext, forceReplace = true)
                }
                Log.e(TAG, "MonthlyBillingWorker failed permanently.", e)
                Result.failure()
            }
        }
    }

    private fun isTransientFirestoreError(error: Throwable?): Boolean {
        val fsErr = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        return fsErr?.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            fsErr?.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
    }
}
