package com.example

import com.example.data.export.FinanceReportExporter
import com.example.data.model.AUTO_BILL_METHOD
import com.example.data.model.BudgetPlan
import com.example.data.model.FinanceEntry
import com.example.data.model.PRIMARY_ADMIN_EMAIL
import com.example.data.model.Payment
import com.example.data.model.Resident
import com.example.data.model.SystemSettings
import com.example.data.model.TransactionDateFilter
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionTypeFilter
import com.example.data.model.buildUnifiedAdminTransactions
import com.example.data.model.computeFinanceSummary
import com.example.data.model.computeMonthlyKasTimeline
import com.example.data.model.extractTransactionCategories
import com.example.data.model.filterAdminTransactions
import com.example.notification.evaluateAdminPendingOverviewNotification
import com.example.notification.evaluateResidentPendingNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationAndExportFilterTest {

    @Test
    fun pendingNotificationEvaluator_notifiesResidentOnlyWhenPendingExistsInCurrentMonth() {
        val currentMonth = "2026-11"
        val settings = SystemSettings(iuranAmount = 50_000L, hidmatAmount = 25_000L)
        val resident = Resident(id = "u1", name = "Budi", house = "Blok CE-04", role = "member")

        val pendingPayments = listOf(
            Payment(
                id = "p1",
                userId = "u1",
                month = currentMonth,
                type = "iuran",
                amount = 50_000L,
                status = "pending",
                method = "DANA QRIS"
            ),
            Payment(
                id = "p2",
                userId = "u1",
                month = "2026-10",
                type = "iuran",
                amount = 50_000L,
                status = "paid"
            )
        )

        val payload = evaluateResidentPendingNotification(
            resident = resident,
            monthPayments = pendingPayments,
            currentMonth = currentMonth,
            dayKey = "2026-11-18",
            settings = settings
        )
        assertNotNull(payload)
        assertEquals(1, payload!!.pendingCount)
        assertEquals(50_000L, payload.pendingAmount)
        assertEquals("PAY", payload.targetTabName)
        assertTrue(payload.message.contains("Blok CE-04"))
        assertTrue(payload.message.contains("PENDING"))

        // Paid resident should not receive a pending reminder
        val paidOnly = listOf(
            Payment(id = "p3", userId = "u1", month = currentMonth, type = "iuran", amount = 50_000L, status = "paid")
        )
        assertNull(
            evaluateResidentPendingNotification(resident, paidOnly, currentMonth, "2026-11-18", settings)
        )

        // Admin account is excluded from resident evaluator
        val admin = Resident(id = "adm", email = PRIMARY_ADMIN_EMAIL, role = "admin")
        assertNull(
            evaluateResidentPendingNotification(admin, pendingPayments, currentMonth, "2026-11-18", settings)
        )
    }

    @Test
    fun adminPendingOverviewEvaluator_countsDistinctResidentsAndNominal() {
        val month = "2026-11"
        val payments = listOf(
            Payment(id = "p1", userId = "u1", month = month, type = "iuran", amount = 50_000L, status = "pending"),
            Payment(id = "p2", userId = "u1", month = month, type = "hidmat", amount = 25_000L, status = "pending"),
            Payment(id = "p3", userId = "u2", month = month, type = "iuran", amount = 50_000L, status = "pending"),
            Payment(id = "p4", userId = "u3", month = month, type = "iuran", amount = 50_000L, status = "paid")
        )

        val overview = evaluateAdminPendingOverviewNotification(payments, month, "2026-11-18")
        assertNotNull(overview)
        assertEquals(2, overview!!.pendingCount)
        assertEquals(125_000L, overview.pendingAmount)
        assertEquals("ADMIN_WARGA", overview.targetTabName)
    }

    @Test
    fun financeReportExporter_buildsCompleteCsvWithKasSummaryTimelineAndPlans() {
        val month = "2026-11"
        val entries = listOf(
            FinanceEntry(id = "e1", kind = "kas_blok", title = "Saldo Awal Kas", category = "Kas Awal", amount = 2_000_000L, month = "2026-10", dateLabel = "01 Okt 2026"),
            FinanceEntry(id = "e2", kind = "keluar", title = "Perbaikan Gerbang \"Utama\"", category = "Infrastruktur", amount = 350_000L, month = month, dateLabel = "15 Nov 2026")
        )
        val payments = listOf(
            Payment(id = "p1", userId = "u1", month = month, type = "iuran", amount = 50_000L, status = "paid")
        )
        val plans = listOf(
            BudgetPlan(id = "b1", title = "Lampu Jalan LED", category = "Keamanan", month = month, plannedAmount = 1_000_000L, realizedAmount = 500_000L, status = "in_progress")
        )
        val summary = computeFinanceSummary(month, entries, payments, plans)
        val timeline = computeMonthlyKasTimeline(month, entries, payments, 6)

        val csv = FinanceReportExporter.buildFinancialSummaryCsv(
            selectedMonth = month,
            blokName = "Blok CE PSA",
            summary = summary,
            kasTimeline = timeline,
            entries = entries,
            budgetPlans = plans,
            adminName = "Admin Utama"
        )

        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("LAPORAN RINGKASAN KEUANGAN & KAS Blok CE PSA"))
        assertTrue(csv.contains("Saldo Kas Blok Saat Ini,1700000"))
        assertTrue(csv.contains("Perbaikan Gerbang \"\"Utama\"\""))
        assertTrue(csv.contains("Lampu Jalan LED"))
        assertTrue(csv.contains("50%"))
    }

    @Test
    fun transactionHistoryFilter_filtersByResidentCategoryTypeAndDate() {
        val residents = listOf(
            Resident(id = "u1", name = "Budi Santoso", house = "Blok CE-01", role = "member"),
            Resident(id = "u2", name = "Siti Aminah", house = "Blok CE-08", role = "member")
        )
        val payments = listOf(
            Payment(
                id = "pay1",
                userId = "u1",
                month = "2026-11",
                type = "iuran",
                amount = 50_000L,
                status = "paid",
                dateLabel = "15 Nov 2026",
                method = "Transfer DANA"
            ),
            Payment(
                id = "pay2",
                userId = "u2",
                month = "2026-10",
                type = "hidmat",
                amount = 25_000L,
                status = "pending",
                dateLabel = "10 Okt 2026",
                method = "DANA QRIS"
            ),
            Payment(
                id = "auto1",
                userId = "u2",
                month = "2026-11",
                type = "iuran",
                amount = 50_000L,
                status = "unpaid",
                method = AUTO_BILL_METHOD
            )
        )
        val entries = listOf(
            FinanceEntry(
                id = "fin1",
                kind = "keluar",
                title = "Honor Petugas Sampah",
                category = "Kebersihan",
                amount = 300_000L,
                month = "2026-11",
                dateLabel = "17 Nov 2026",
                createdByName = "Admin Utama"
            )
        )

        val unified = buildUnifiedAdminTransactions(payments, entries, residents)
        // Auto unpaid placeholder should not clutter transaction history
        assertEquals(3, unified.size)

        val categories = extractTransactionCategories(unified)
        assertEquals(listOf("Semua", "Hidmat", "Iuran", "Kebersihan"), categories)

        // Filter by resident name
        val byResident = filterAdminTransactions(
            items = unified,
            query = "Budi",
            typeFilter = TransactionTypeFilter.ALL,
            categoryFilter = "Semua",
            dateFilter = TransactionDateFilter.ALL,
            selectedMonth = "2026-11"
        )
        assertEquals(1, byResident.size)
        assertEquals("pay_pay1", byResident[0].id)

        // Filter by category + expenses
        val expensesOnly = filterAdminTransactions(
            items = unified,
            query = "",
            typeFilter = TransactionTypeFilter.EXPENSES,
            categoryFilter = "Kebersihan",
            dateFilter = TransactionDateFilter.SELECTED_MONTH,
            selectedMonth = "2026-11"
        )
        assertEquals(1, expensesOnly.size)
        assertEquals(TransactionDirection.OUTGOING, expensesOnly[0].direction)

        // Filter by date text query
        val byDateQuery = filterAdminTransactions(
            items = unified,
            query = "10 Okt",
            typeFilter = TransactionTypeFilter.PENDING,
            categoryFilter = "Semua",
            dateFilter = TransactionDateFilter.ALL,
            selectedMonth = "2026-11"
        )
        assertEquals(1, byDateQuery.size)
        assertEquals("Siti Aminah", byDateQuery[0].residentName)
    }
}
