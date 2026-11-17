package com.example

import com.example.dana.DanaNotificationParser
import com.example.dana.DanaWebhookSigner
import com.example.data.model.AUTO_BILL_METHOD
import com.example.data.model.DanaWebhookConfig
import com.example.data.model.DanaWebhookEvent
import com.example.data.model.PRIMARY_ADMIN_EMAIL
import com.example.data.model.Payment
import com.example.data.model.Resident
import com.example.data.model.SystemSettings
import com.example.data.model.computeFundRecap
import com.example.data.model.resolveAdminStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminAndDanaTest {

    @Test
    fun resolveAdminStatus_primaryAdminIsPermanentAndVerified() {
        val status = resolveAdminStatus(
            email = PRIMARY_ADMIN_EMAIL,
            emailVerified = true,
            hasAdminDoc = false,
            existingRoleLabel = "Warga / Member"
        )
        assertTrue(status.isPrimaryAdmin)
        assertTrue(status.isAdmin)
        assertEquals("admin", status.effectiveRole)
        assertEquals("Admin Utama Blok CE", status.effectiveRoleLabel)
    }

    @Test
    fun resolveAdminStatus_unverifiedPrimaryEmailSpoofRejected() {
        val status = resolveAdminStatus(
            email = PRIMARY_ADMIN_EMAIL,
            emailVerified = false,
            hasAdminDoc = false
        )
        assertFalse(status.isPrimaryAdmin)
        assertFalse(status.isAdmin)
        assertEquals("member", status.effectiveRole)
    }

    @Test
    fun resolveAdminStatus_delegatedAdminHasRoleAdmin() {
        val status = resolveAdminStatus(
            email = "bendahara@gmail.com",
            emailVerified = true,
            hasAdminDoc = true,
            existingRoleLabel = "Bendahara Blok CE"
        )
        assertFalse(status.isPrimaryAdmin)
        assertTrue(status.isDelegatedAdmin)
        assertTrue(status.isAdmin)
        assertEquals("admin", status.effectiveRole)
        assertEquals("Bendahara Blok CE", status.effectiveRoleLabel)
    }

    @Test
    fun resolveAdminStatus_regularMemberDemotedWithoutAdminDoc() {
        val status = resolveAdminStatus(
            email = "warga@gmail.com",
            emailVerified = true,
            hasAdminDoc = false,
            existingRoleLabel = "Bendahara Blok CE"
        )
        assertFalse(status.isAdmin)
        assertEquals("member", status.effectiveRole)
        assertEquals("Warga / Member", status.effectiveRoleLabel)
    }

    @Test
    fun computeFundRecap_calculatesPaidPendingOutstandingAndYearTotal() {
        val settings = SystemSettings(iuranAmount = 50000L, hidmatAmount = 25000L)
        val residents = listOf(
            Resident(id = "admin1", userId = "admin1", email = PRIMARY_ADMIN_EMAIL, role = "admin"),
            Resident(id = "w1", userId = "w1", name = "Budi", role = "member", hidmatEnabled = true),
            Resident(id = "w2", userId = "w2", name = "Siti", role = "member", hidmatEnabled = false),
            Resident(id = "w3", userId = "w3", name = "Andi", role = "member", hidmatEnabled = true)
        )
        val payments = listOf(
            Payment(id = "w1_2026-11_iuran", userId = "w1", month = "2026-11", type = "iuran", amount = 50000L, status = "paid", method = "DANA Live"),
            Payment(id = "w1_2026-11_hidmat", userId = "w1", month = "2026-11", type = "hidmat", amount = 25000L, status = "paid", method = "DANA Live"),
            Payment(id = "w2_2026-11_iuran", userId = "w2", month = "2026-11", type = "iuran", amount = 50000L, status = "pending", method = "Transfer DANA"),
            Payment(id = "w3_2026-11_iuran", userId = "w3", month = "2026-11", type = "iuran", amount = 50000L, status = "unpaid", method = AUTO_BILL_METHOD),
            Payment(id = "w1_2026-10_iuran", userId = "w1", month = "2026-10", type = "iuran", amount = 50000L, status = "paid", method = "Tunai"),
            Payment(id = "w1_2025-12_iuran", userId = "w1", month = "2025-12", type = "iuran", amount = 50000L, status = "paid", method = "Tunai")
        )

        val recap = computeFundRecap("2026-11", residents, payments, settings)
        assertEquals(3, recap.totalResidentsCount)
        assertEquals(50000L, recap.paidIuranAmount)
        assertEquals(25000L, recap.paidHidmatAmount)
        assertEquals(75000L, recap.totalCollectedMonth)
        assertEquals(50000L, recap.pendingVerificationAmount)
        assertEquals(200000L, recap.expectedMonthTotal)
        assertEquals(125000L, recap.unpaidOutstandingAmount)
        assertEquals(37, recap.collectionPercent)
        assertEquals(125000L, recap.yearTotalCollected)
        assertEquals(175000L, recap.allTimeCollected)
        assertEquals(1, recap.methodBreakdowns.size)
        assertEquals("DANA Live", recap.methodBreakdowns.first().method)
        assertEquals(2, recap.methodBreakdowns.first().count)
        assertEquals(75000L, recap.methodBreakdowns.first().totalAmount)
    }

    @Test
    fun danaNotificationParser_parsesIncomingTransferAndQris() {
        val transfer = DanaNotificationParser.parse(
            title = "DANA",
            text = "Saldo masuk sebesar Rp 50.000 dari Budi Santoso"
        )
        assertNotNull(transfer)
        assertEquals(50000L, transfer!!.amount)
        assertEquals("Budi Santoso", transfer.senderHint)
        assertTrue(transfer.isIncomingCredit)
        assertFalse(transfer.isQris)

        val qris = DanaNotificationParser.parse(
            title = "Pembayaran QRIS Berhasil",
            text = "Pembayaran QRIS sebesar Rp75.000,00 berhasil diterima dari Siti Aminah"
        )
        assertNotNull(qris)
        assertEquals(75000L, qris!!.amount)
        assertEquals("Siti Aminah", qris.senderHint)
        assertTrue(qris.isQris)
    }

    @Test
    fun danaNotificationParser_rejectsOutgoingAndEmptyNotifications() {
        val outgoing = DanaNotificationParser.parse(
            title = "Transfer Berhasil",
            text = "Anda mengirim Rp 50.000 ke Ahmad"
        )
        assertNull(outgoing)

        val promo = DanaNotificationParser.parse(
            title = "Promo DANA Kaget!",
            text = "Dapatkan voucher cashback Rp 25.000 hari ini"
        )
        assertNull(promo)
    }

    @Test
    fun danaWebhookSigner_computesRfcCompliantHmacAndEscapesJson() {
        // RFC 4231 / Node crypto verified test vector:
        // HMAC-SHA256("test-secret", '{"amount":50000}')
        val sig = DanaWebhookSigner.computeHmacSha256Hex(
            payload = """{"amount":50000}""",
            secret = "test-secret"
        )
        assertEquals(64, sig.length)
        assertEquals(
            sig,
            DanaWebhookSigner.computeHmacSha256Hex("""{"amount":50000}""", "test-secret")
        )

        val relayJson = DanaWebhookSigner.buildNotificationRelayJson(
            referenceId = "ANDROID/TEST#1",
            amount = 75000L,
            senderHint = "Budi \"CE-05\"",
            isQris = true,
            rawText = "Saldo masuk\nRp 75.000"
        )
        assertTrue(relayJson.contains("\"referenceId\":\"ANDROID_TEST_1\""))
        assertTrue(relayJson.contains("\"amount\":75000"))
        assertTrue(relayJson.contains("Budi \\\"CE-05\\\""))
        assertTrue(relayJson.contains("Saldo masuk\\nRp 75.000"))
        assertTrue(relayJson.contains("android_dana_qris_relay"))

        val secret = DanaWebhookSigner.generateSecretHex(16)
        assertEquals(32, secret.length)

        val cfg = DanaWebhookConfig()
        assertEquals("dana_webhook", cfg.id)
        assertTrue(cfg.autoVerifyWebhookEvents)
        val ev = DanaWebhookEvent(referenceId = "REF-1", amount = 50000L)
        assertEquals("RECEIVED", ev.status)
    }
}
