package com.example

import com.example.ui.components.QrisParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrisParserTest {

    private fun tlv(tag: String, value: String): String {
        val len = value.length.toString().padStart(2, '0')
        return "$tag$len$value"
    }

    @Test
    fun parse_validEmvQrisWithDanaAndAmount_extractsAllFields() {
        val merchantAccount = tlv("00", "ID.CO.DANA.WWW") + tlv("02", "ID1023456789012")
        val additionalData = tlv("05", "BLOKCE-NOV26")
        val payload = buildString {
            append(tlv("00", "01"))
            append(tlv("01", "12"))
            append(tlv("26", merchantAccount))
            append(tlv("52", "8398"))
            append(tlv("53", "360"))
            append(tlv("54", "50000"))
            append(tlv("58", "ID"))
            append(tlv("59", "KAS BLOK CE PSA"))
            append(tlv("60", "BEKASI"))
            append(tlv("62", additionalData))
            append(tlv("63", "ABCD"))
        }

        val parsed = QrisParser.parse(payload)

        assertTrue(parsed.isEmvQris)
        assertEquals("KAS BLOK CE PSA", parsed.merchantName)
        assertEquals("BEKASI", parsed.merchantCity)
        assertEquals(50_000L, parsed.amountRupiah)
        assertEquals("DANA", parsed.providerLabel)
        assertEquals("ID1023456789012", parsed.nmidOrId)
        assertEquals("BLOKCE-NOV26", parsed.referenceLabel)
        assertTrue(parsed.formatConfirmationNote().contains("QRIS: KAS BLOK CE PSA"))
        assertTrue(parsed.formatConfirmationNote().contains("DANA"))
    }

    @Test
    fun parse_staticEmvQrisWithoutAmount_returnsNullAmount() {
        val merchantAccount = tlv("00", "ID.CO.QRIS.WWW")
        val payload = buildString {
            append(tlv("00", "01"))
            append(tlv("01", "11"))
            append(tlv("26", merchantAccount))
            append(tlv("58", "ID"))
            append(tlv("59", "BENDAHARA CE"))
            append(tlv("60", "JAKARTA"))
            append(tlv("63", "1234"))
        }

        val parsed = QrisParser.parse(payload)

        assertTrue(parsed.isEmvQris)
        assertEquals("BENDAHARA CE", parsed.merchantName)
        assertEquals("JAKARTA", parsed.merchantCity)
        assertNull(parsed.amountRupiah)
        assertEquals("QRIS", parsed.providerLabel)
    }

    @Test
    fun parse_danaDeepLink_fallsBackGracefully() {
        val parsed = QrisParser.parse("https://link.dana.id/qr/abc12345")

        assertFalse(parsed.isEmvQris)
        assertEquals("Tautan Pembayaran DANA", parsed.merchantName)
        assertEquals("DANA", parsed.providerLabel)
        assertEquals("abc12345", parsed.referenceLabel)
    }
}
