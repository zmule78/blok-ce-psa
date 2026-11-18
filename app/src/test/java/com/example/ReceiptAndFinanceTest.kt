package com.example

import com.example.data.model.AUTO_BILL_METHOD
import com.example.data.model.BudgetPlan
import com.example.data.model.FinanceEntry
import com.example.data.model.MAX_DELEGATED_ADMINS_BY_PENGURUS
import com.example.data.model.PRIMARY_ADMIN_EMAIL
import com.example.data.model.Payment
import com.example.data.model.Resident
import com.example.data.model.UserPresence
import com.example.data.model.computeCurrentMonthStatusSummary
import com.example.data.model.computeFinanceSummary
import com.example.data.model.computeMonthlyKasTimeline
import com.example.data.model.resolveAdminStatus
import com.example.scanner.ReceiptOcrParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptAndFinanceTest {

    @Test
    fun receiptOcrParser_extractsDanaAmountAndMethod() {
        val sample = """
            DANA INDONESIA
            Transaksi Berhasil
            Kirim Uang ke Bendahara Blok CE
            Total Bayar Rp 75.000
            ID Transaksi 2026111800912
        """.trimIndent()

        val parsed = ReceiptOcrParser.parse(sample)
        assertEquals(75_000L, parsed.detectedAmount)
        assertEquals("Transfer DANA", parsed.detectedMethod)
        assertTrue(parsed.summaryText.contains("Nominal terdeteksi Rp 75000"))
        assertTrue(parsed.summaryText.contains("Transfer DANA"))
    }

    @Test
    fun receiptOcrParser_extractsBankOrQrisAndPlainTotalLine() {
        val qrisSample = """
            Pembayaran QRIS Sukses
            Merchant Blok CE PSA
            Jumlah 50.000
        """.trimIndent()
        val parsedQris = ReceiptOcrParser.parse(qrisSample)
        assertEquals(50_000L, parsedQris.detectedAmount)
        assertEquals("DANA QRIS", parsedQris.detectedMethod)

        val bankSample = """
            m-BCA Transfer Berhasil
            IDR 150.000,00
            Ke Rekening Bendahara
        """.trimIndent()
        val parsedBank = ReceiptOcrParser.parse(bankSample)
        assertEquals(150_000L, parsedBank.detectedAmount)
        assertEquals("Transfer Bank", parsedBank.detectedMethod)
    }

    @Test
    fun computeCurrentMonthStatusSummary_countsPaidPendingUnpaidExcludingAdmins() {
        val month = "2026-11"
        val residents = listOf(
            Resident(id = "admin_utama", name = "Admin", email = PRIMARY_ADMIN_EMAIL, role = "admin", hidmatEnabled = false),
            Resident(id = "warga_paid", name = "Budi", role = "member", hidmatEnabled = true),
            Resident(id = "warga_pending", name = "Siti", role = "member", hidmatEnabled = false),
            Resident(id = "warga_unpaid", name = "Andi", role = "member", hidmatEnabled = false)
        )
        val payments = listOf(
            Payment(id = "p1", userId = "warga_paid", month = month, type = "iuran", amount = 50_000L, status = "paid", method = "Transfer DANA"),
            Payment(id = "p2", userId = "warga_paid", month = month, type = "hidmat", amount = 25_000L, status = "paid", method = "Transfer DANA"),
            Payment(id = "p3", userId = "warga_pending", month = month, type = "iuran", amount = 50_000L, status = "pending", method = "DANA QRIS"),
            Payment(id = "p4", userId = "warga_unpaid", month = month, type = "iuran", amount = 50_000L, status = "unpaid", method = AUTO_BILL_METHOD)
        )

        val summary = computeCurrentMonthStatusSummary(month, residents, payments)
        assertEquals(3, summary.totalActiveResidents)
        assertEquals(1, summary.paidResidents)
        assertEquals(1, summary.pendingResidents)
        assertEquals(1, summary.unpaidResidents)
    }

    @Test
    fun computeFinanceSummary_calculatesKasBlokMasukPaidDuesKeluarAndBudget() {
        val month = "2026-11"
        val entries = listOf(
            FinanceEntry(id = "e1", kind = "kas_blok", title = "Saldo Awal Kas", amount = 2_000_000L, month = "2026-10"),
            FinanceEntry(id = "e2", kind = "masuk", title = "Donasi Warga", amount = 500_000L, month = month),
            FinanceEntry(id = "e3", kind = "keluar", title = "Perbaikan Lampu Jalan", amount = 350_000L, month = month)
        )
        val payments = listOf(
            Payment(id = "p1", userId = "w1", month = month, type = "iuran", amount = 50_000L, status = "paid"),
            Payment(id = "p2", userId = "w2", month = "2026-10", type = "iuran", amount = 50_000L, status = "paid"),
            Payment(id = "p3", userId = "w3", month = month, type = "iuran", amount = 50_000L, status = "pending")
        )
        val plans = listOf(
            BudgetPlan(id = "b1", title = "Portal Gerbang", plannedAmount = 1_500_000L, realizedAmount = 750_000L, month = month),
            BudgetPlan(id = "b2", title = "Cat Pos Ronda", plannedAmount = 500_000L, realizedAmount = 500_000L, month = month)
        )

        val summary = computeFinanceSummary(month, entries, payments, plans)
        assertEquals(100_000L, summary.totalIuranPaidAllTime)
        assertEquals(2_000_000L, summary.totalKasBlokInitial)
        assertEquals(500_000L, summary.totalManualMasuk)
        assertEquals(350_000L, summary.totalKeluar)
        // Balance = 2_000_000 + 500_000 + 100_000 - 350_000 = 2_250_000
        assertEquals(2_250_000L, summary.currentKasBalance)
        // Month Masuk = manual masuk (500_000) + paid dues in 2026-11 (50_000) = 550_000
        assertEquals(550_000L, summary.monthMasukTotal)
        assertEquals(350_000L, summary.monthKeluarTotal)
        assertEquals(2_000_000L, summary.totalPlannedBudget)
        assertEquals(1_250_000L, summary.totalRealizedBudget)
        assertEquals(50, plans[0].progressPercent)
        assertEquals(100, plans[1].progressPercent)
    }

    @Test
    fun twoTierAdminRules_quotaAndPresenceCheck() {
        assertEquals(3, MAX_DELEGATED_ADMINS_BY_PENGURUS)

        val primary = resolveAdminStatus(PRIMARY_ADMIN_EMAIL, emailVerified = true, hasAdminDoc = false)
        assertTrue(primary.isPrimaryAdmin)
        assertFalse(primary.isDelegatedAdmin)

        val pengurus = resolveAdminStatus("pengurus1@gmail.com", emailVerified = true, hasAdminDoc = true, existingRoleLabel = "Pengurus Blok")
        assertFalse(pengurus.isPrimaryAdmin)
        assertTrue(pengurus.isDelegatedAdmin)
        assertTrue(pengurus.isAdmin)

        val now = 1_700_000_000_000L
        val onlineUser = UserPresence(online = true, lastSeenMillis = now - 30_000L)
        val staleUser = UserPresence(online = true, lastSeenMillis = now - 300_000L)
        val offlineUser = UserPresence(online = false, lastSeenMillis = now - 5_000L)
        assertTrue(onlineUser.isEffectivelyOnline(now))
        assertFalse(staleUser.isEffectivelyOnline(now))
        assertFalse(offlineUser.isEffectivelyOnline(now))
    }

    @Test
    fun computeMonthlyKasTimeline_tracksRollingBalanceAndNetDeltaAcrossMonths() {
        val entries = listOf(
            FinanceEntry(id = "e0", kind = "kas_blok", title = "Saldo Awal Tahun", amount = 1_000_000L, month = "2026-01"),
            FinanceEntry(id = "e1", kind = "masuk", title = "Donasi Agust", amount = 200_000L, month = "2026-08"),
            FinanceEntry(id = "e2", kind = "keluar", title = "Lampu Jalan", amount = 150_000L, month = "2026-09"),
            FinanceEntry(id = "e3", kind = "kas_blok", title = "Setoran Kas Blok", amount = 500_000L, month = "2026-10"),
            FinanceEntry(id = "e4", kind = "keluar", title = "Pos Keamanan", amount = 300_000L, month = "2026-11")
        )
        val payments = listOf(
            Payment(id = "p0", userId = "w1", month = "2026-05", type = "iuran", amount = 50_000L, status = "paid"),
            Payment(id = "p1", userId = "w1", month = "2026-10", type = "iuran", amount = 50_000L, status = "paid"),
            Payment(id = "p2", userId = "w2", month = "2026-11", type = "iuran", amount = 75_000L, status = "paid"),
            Payment(id = "p3", userId = "w3", month = "2026-11", type = "iuran", amount = 50_000L, status = "pending")
        )

        val timeline = computeMonthlyKasTimeline(
            selectedMonth = "2026-11",
            entries = entries,
            payments = payments,
            monthWindowSize = 6
        )

        assertEquals(listOf("2026-06", "2026-07", "2026-08", "2026-09", "2026-10", "2026-11"), timeline.map { it.monthCode })
        // Prior to 2026-06: 1_000_000 (kas_blok 2026-01) + 50_000 (paid 2026-05) = 1_050_000
        assertEquals(1_050_000L, timeline[0].runningBalance)
        assertEquals(1_050_000L, timeline[1].runningBalance)
        // 2026-08: +200_000 -> 1_250_000
        assertEquals(200_000L, timeline[2].masukAmount)
        assertEquals(1_250_000L, timeline[2].runningBalance)
        // 2026-09: -150_000 -> 1_100_000
        assertEquals(150_000L, timeline[3].keluarAmount)
        assertEquals(1_100_000L, timeline[3].runningBalance)
        // 2026-10: +500_000 kas_blok + 50_000 iuran -> +550_000 net -> 1_650_000
        assertEquals(550_000L, timeline[4].netDelta)
        assertEquals(1_650_000L, timeline[4].runningBalance)
        // 2026-11: +75_000 iuran - 300_000 keluar -> -225_000 net -> 1_425_000
        assertEquals(75_000L, timeline[5].masukAmount)
        assertEquals(300_000L, timeline[5].keluarAmount)
        assertEquals(-225_000L, timeline[5].netDelta)
        assertEquals(1_425_000L, timeline[5].runningBalance)
    }
}
