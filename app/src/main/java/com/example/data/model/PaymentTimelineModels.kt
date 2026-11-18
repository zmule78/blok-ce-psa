package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class TimelineFilter(val label: String) {
    ALL("Semua"),
    PAID("Lunas"),
    PENDING("Menunggu"),
    UNPAID("Belum Bayar")
}

data class PaymentTimelineEntry(
    val payment: Payment,
    val title: String,
    val timestampLabel: String,
    val status: DuesStatus,
    val sortEpochMillis: Long
)

fun paymentStatusToDuesStatus(status: String): DuesStatus = when (status.lowercase(Locale.ROOT)) {
    "paid" -> DuesStatus.PAID
    "unpaid" -> DuesStatus.UNPAID
    else -> DuesStatus.PENDING
}

fun formatPaymentTimestamp(payment: Payment): String {
    val date = (payment.updatedAt ?: payment.createdAt)?.toDate()
    if (date != null) {
        val formatter = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID"))
        return formatter.format(date)
    }
    return payment.dateLabel.takeIf { it.isNotBlank() } ?: monthLabel(payment.month)
}

fun approximateEpochFromPeriodCode(periodCode: String): Long {
    val (year, month) = parsePeriodCode(periodCode)
    val cal = Calendar.getInstance(Locale("id", "ID")).apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

fun buildResidentPaymentTimeline(
    payments: List<Payment>,
    filter: TimelineFilter = TimelineFilter.ALL
): List<PaymentTimelineEntry> {
    return payments
        .map { payment ->
            val duesStatus = paymentStatusToDuesStatus(payment.status)
            val typeLabel = if (payment.type == "hidmat") "Hidmat" else "Iuran"
            val sortMillis = (payment.updatedAt ?: payment.createdAt)?.toDate()?.time
                ?: approximateEpochFromPeriodCode(payment.month)
            PaymentTimelineEntry(
                payment = payment,
                title = "$typeLabel • ${monthLabel(payment.month)}",
                timestampLabel = formatPaymentTimestamp(payment),
                status = duesStatus,
                sortEpochMillis = sortMillis
            )
        }
        .filter { entry ->
            when (filter) {
                TimelineFilter.ALL -> true
                TimelineFilter.PAID -> entry.status == DuesStatus.PAID
                TimelineFilter.PENDING -> entry.status == DuesStatus.PENDING
                TimelineFilter.UNPAID -> entry.status == DuesStatus.UNPAID
            }
        }
        .sortedWith(
            compareByDescending<PaymentTimelineEntry> { it.sortEpochMillis }
                .thenByDescending { it.payment.month }
                .thenByDescending { it.payment.id }
        )
}
