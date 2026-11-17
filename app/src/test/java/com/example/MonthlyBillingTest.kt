package com.example

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.model.AUTO_BILL_METHOD
import com.example.data.model.BILLING_MONTHS
import com.example.data.model.BILLING_YEARS
import com.example.data.model.DuesStatus
import com.example.data.model.MAX_BILLING_YEAR
import com.example.data.model.MIN_BILLING_YEAR
import com.example.data.model.Payment
import com.example.data.model.Resident
import com.example.data.model.computeMonthSummary
import com.example.data.model.formatPeriodCode
import com.example.data.model.isWithinBillingHorizon
import com.example.data.model.monthLabel
import com.example.data.model.parsePeriodCode
import com.example.worker.MonthlyBillingScheduler
import com.example.worker.MonthlyBillingWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MonthlyBillingTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun periods_cover2026Through2050_byYearAndMonth() {
        assertEquals(2026, MIN_BILLING_YEAR)
        assertEquals(2050, MAX_BILLING_YEAR)
        assertEquals(25, BILLING_YEARS.size)
        assertEquals(300, BILLING_MONTHS.size)
        assertEquals("2026-01", BILLING_MONTHS.first().code)
        assertEquals("Januari 2026", BILLING_MONTHS.first().label)
        assertEquals("2050-12", BILLING_MONTHS.last().code)
        assertEquals("Desember 2050", BILLING_MONTHS.last().label)

        assertTrue(isWithinBillingHorizon("2026-01"))
        assertTrue(isWithinBillingHorizon("2050-12"))
        assertFalse(isWithinBillingHorizon("2025-12"))
        assertFalse(isWithinBillingHorizon("2051-01"))
        assertFalse(isWithinBillingHorizon("2050-13"))

        assertEquals("Desember 2050", monthLabel("2050-12"))
        assertEquals(2050 to 7, parsePeriodCode("2050-07"))
        assertEquals("2050-07", formatPeriodCode(2050, 7))
    }

    @Test
    fun scheduler_alwaysTargetsNextFirstOfMonthAt0005Through2050() {
        val tz = TimeZone.getTimeZone("Asia/Jakarta")

        // From Nov 17, 2026 -> Dec 1, 2026 00:05:00
        val novCal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 17, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextDec = Calendar.getInstance(tz).apply {
            timeInMillis = MonthlyBillingScheduler.computeNextFirstOfMonthMillis(novCal.timeInMillis, tz)
        }
        assertEquals(2026, nextDec.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, nextDec.get(Calendar.MONTH))
        assertEquals(1, nextDec.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, nextDec.get(Calendar.HOUR_OF_DAY))
        assertEquals(5, nextDec.get(Calendar.MINUTE))

        // From Nov 1, 2050 -> Dec 1, 2050 00:05:00 (still inside 2050 horizon)
        val nov2050Cal = Calendar.getInstance(tz).apply {
            set(2050, Calendar.NOVEMBER, 1, 0, 5, 10)
            set(Calendar.MILLISECOND, 0)
        }
        val nextDec2050 = Calendar.getInstance(tz).apply {
            timeInMillis = MonthlyBillingScheduler.computeNextFirstOfMonthMillis(nov2050Cal.timeInMillis, tz)
        }
        assertEquals(2050, nextDec2050.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, nextDec2050.get(Calendar.MONTH))
        assertEquals(1, nextDec2050.get(Calendar.DAY_OF_MONTH))
        assertTrue(isWithinBillingHorizon(nextDec2050.get(Calendar.YEAR), nextDec2050.get(Calendar.MONTH) + 1))

        // From Dec 1, 2050 -> Jan 1, 2051 is outside 2050 horizon
        val dec2050Cal = Calendar.getInstance(tz).apply {
            set(2050, Calendar.DECEMBER, 1, 0, 5, 10)
            set(Calendar.MILLISECOND, 0)
        }
        val nextJan2051 = Calendar.getInstance(tz).apply {
            timeInMillis = MonthlyBillingScheduler.computeNextFirstOfMonthMillis(dec2050Cal.timeInMillis, tz)
        }
        assertEquals(2051, nextJan2051.get(Calendar.YEAR))
        assertFalse(isWithinBillingHorizon(nextJan2051.get(Calendar.YEAR), nextJan2051.get(Calendar.MONTH) + 1))
    }

    @Test
    fun unpaidAutoGeneratedBill_mapsToUnpaidSummaryUntilResidentConfirms() {
        val resident = Resident(id = "u1", userId = "u1", role = "member", hidmatEnabled = true)
        val autoIuran = Payment(
            id = "u1_2050-05_iuran",
            userId = "u1",
            month = "2050-05",
            type = "iuran",
            amount = 50_000L,
            status = "unpaid",
            method = AUTO_BILL_METHOD
        )
        assertTrue(autoIuran.isAutoGeneratedBill)

        val autoOnlySummary = computeMonthSummary(resident, listOf(autoIuran), "2050-05")
        assertEquals(DuesStatus.UNPAID, autoOnlySummary.iuranStatus)
        assertEquals(DuesStatus.UNPAID, autoOnlySummary.hidmatStatus)
        assertEquals(DuesStatus.UNPAID, autoOnlySummary.overall)

        val confirmedIuran = autoIuran.copy(status = "pending", method = "DANA QRIS")
        assertFalse(confirmedIuran.isAutoGeneratedBill)
        val pendingVerifSummary = computeMonthSummary(resident, listOf(confirmedIuran), "2050-05")
        assertEquals(DuesStatus.PENDING, pendingVerifSummary.iuranStatus)

        val paidIuran = confirmedIuran.copy(status = "paid")
        assertEquals(DuesStatus.PAID, computeMonthSummary(resident, listOf(paidIuran), "2050-05").iuranStatus)
    }

    @Test
    fun workManager_enqueuesUniqueFirstOfMonthWork_andWorkerSkipsGracefullyWhenSignedOut() {
        MonthlyBillingScheduler.ensureMonthlyBillingScheduled(context)
        shadowOf(Looper.getMainLooper()).idle()
        val wm = WorkManager.getInstance(context)
        val infos = wm.getWorkInfosForUniqueWork(MonthlyBillingScheduler.UNIQUE_MONTHLY_WORK_NAME).get()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos.first().state)

        val worker = TestListenableWorkerBuilder<MonthlyBillingWorker>(context).build()
        val result = runBlocking { worker.doWork() }
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue("Expected Success but got $result", result is ListenableWorker.Result.Success)
    }
}
