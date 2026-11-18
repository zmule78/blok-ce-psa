package com.example

import com.example.data.model.DuesStatus
import com.example.data.model.Payment
import com.example.data.model.TimelineFilter
import com.example.data.model.buildResidentPaymentTimeline
import com.example.data.model.formatPaymentTimestamp
import com.example.data.model.paymentStatusToDuesStatus
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class PaymentTimelineTest {

    @Test
    fun timeline_sortsNewestFirstUsingTimestampOrMonthFallback() {
        val older = Payment(
            id = "p1",
            month = "2026-09",
            type = "iuran",
            amount = 50000L,
            status = "paid",
            dateLabel = "10 Sep 2026",
            createdAt = Timestamp(Date(1_789_000_000_000L))
        )
        val newest = Payment(
            id = "p2",
            month = "2026-11",
            type = "hidmat",
            amount = 25000L,
            status = "pending",
            dateLabel = "18 Nov 2026",
            updatedAt = Timestamp(Date(1_795_000_000_000L))
        )
        val middle = Payment(
            id = "p3",
            month = "2026-10",
            type = "iuran",
            amount = 50000L,
            status = "unpaid",
            dateLabel = "01 Okt 2026",
            createdAt = Timestamp(Date(1_792_000_000_000L))
        )

        val entries = buildResidentPaymentTimeline(listOf(older, newest, middle))
        assertEquals(listOf("p2", "p3", "p1"), entries.map { it.payment.id })
        assertEquals("Hidmat • November 2026", entries[0].title)
        assertEquals(DuesStatus.PENDING, entries[0].status)
        assertEquals(DuesStatus.UNPAID, entries[1].status)
        assertEquals(DuesStatus.PAID, entries[2].status)
    }

    @Test
    fun timeline_filtersByStatusCorrectly() {
        val payments = listOf(
            Payment(id = "a", month = "2026-11", status = "paid", amount = 50000L),
            Payment(id = "b", month = "2026-10", status = "pending", amount = 50000L),
            Payment(id = "c", month = "2026-09", status = "unpaid", amount = 25000L)
        )

        assertEquals(3, buildResidentPaymentTimeline(payments, TimelineFilter.ALL).size)
        assertEquals(listOf("a"), buildResidentPaymentTimeline(payments, TimelineFilter.PAID).map { it.payment.id })
        assertEquals(listOf("b"), buildResidentPaymentTimeline(payments, TimelineFilter.PENDING).map { it.payment.id })
        assertEquals(listOf("c"), buildResidentPaymentTimeline(payments, TimelineFilter.UNPAID).map { it.payment.id })
    }

    @Test
    fun formatPaymentTimestamp_formatsTimestampOrFallsBackToDateLabel() {
        val withDateLabelOnly = Payment(
            id = "x",
            month = "2026-11",
            dateLabel = "15 Nov 2026"
        )
        assertEquals("15 Nov 2026", formatPaymentTimestamp(withDateLabelOnly))

        val withTimestamp = Payment(
            id = "y",
            month = "2026-11",
            updatedAt = Timestamp(Date(1_795_000_000_000L))
        )
        val formatted = formatPaymentTimestamp(withTimestamp)
        assertTrue(formatted.contains("•"))
        assertEquals(DuesStatus.UNPAID, paymentStatusToDuesStatus("unpaid"))
    }
}
