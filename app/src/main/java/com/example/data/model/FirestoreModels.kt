package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Calendar
import java.util.Locale

@Keep
@IgnoreExtraProperties
data class SystemSettings(
    @DocumentId val id: String = "config",
    val blokName: String = "Blok CE PSA",
    val subtitle: String = "Paguyuban Warga Perumahan Silih Asih",
    val danaNumber: String = "085263195072",
    val danaAccountName: String = "Bendahara Blok CE PSA",
    val customQrisData: String = "",
    val iuranAmount: Long = 50000L,
    val hidmatAmount: Long = 25000L,
    val reminderEnabled: Boolean = true,
    val reminderTitle: String = "Himbauan Pembayaran Iuran Bulan Ini",
    val reminderMessage: String = "Assalamualaikum Wr. Wb. Bapak/Ibu warga Blok CE PSA, dimohon untuk segera menyelesaikan pembayaran iuran bulan ini melalui akun DANA (085263195072) atau scan QRIS yang tersedia. Terima kasih.",
    val pushBlastId: Long = 0L,
    val pushTargetUserId: String = "all_unpaid",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

const val PRIMARY_ADMIN_EMAIL = "kfkhoerul05@gmail.com"

@Keep
@IgnoreExtraProperties
data class Resident(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val name: String = "",
    val house: String = "Blok CE-??",
    val phone: String = "",
    val email: String = "",
    val role: String = "member",
    val roleLabel: String = "Warga / Member",
    val hidmatEnabled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val adminRole: Boolean get() = role == "admin"

    @get:Exclude
    val isPermanentPrimaryEmail: Boolean
        get() = email.equals(PRIMARY_ADMIN_EMAIL, ignoreCase = true)

    @get:Exclude
    val primaryAdmin: Boolean
        get() = adminRole && isPermanentPrimaryEmail

    @get:Exclude
    val needsProfileCompletion: Boolean
        get() = house.isBlank() || house.contains("??") || phone.isBlank()
}

data class AdminStatusResult(
    val isPrimaryAdmin: Boolean,
    val isDelegatedAdmin: Boolean,
    val effectiveRole: String,
    val effectiveRoleLabel: String
) {
    val isAdmin: Boolean get() = isPrimaryAdmin || isDelegatedAdmin
}

fun resolveAdminStatus(
    email: String?,
    emailVerified: Boolean,
    hasAdminDoc: Boolean,
    existingRoleLabel: String = ""
): AdminStatusResult {
    val isPrimary = emailVerified && email.equals(PRIMARY_ADMIN_EMAIL, ignoreCase = true)
    return when {
        isPrimary -> AdminStatusResult(
            isPrimaryAdmin = true,
            isDelegatedAdmin = false,
            effectiveRole = "admin",
            effectiveRoleLabel = "Admin Utama Blok CE"
        )
        hasAdminDoc -> AdminStatusResult(
            isPrimaryAdmin = false,
            isDelegatedAdmin = true,
            effectiveRole = "admin",
            effectiveRoleLabel = existingRoleLabel.takeIf {
                it.isNotBlank() && it != "Warga / Member"
            } ?: "Admin / Pengurus Blok"
        )
        else -> AdminStatusResult(
            isPrimaryAdmin = false,
            isDelegatedAdmin = false,
            effectiveRole = "member",
            effectiveRoleLabel = "Warga / Member"
        )
    }
}

@Keep
@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val roleLabel: String = "Admin / Pengurus Blok",
    val inviteCode: String = "",
    val grantedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminInvite(
    @DocumentId val id: String = "",
    val code: String = "",
    val targetEmail: String = "",
    val label: String = "Admin / Pengurus Blok",
    val used: Boolean = false,
    val usedByUid: String = "",
    val usedByEmail: String = "",
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

const val MAX_DELEGATED_ADMINS_BY_PENGURUS = 3

@Keep
@IgnoreExtraProperties
data class Payment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val month: String = "2026-11",
    val type: String = "iuran",
    val amount: Long = 0L,
    val status: String = "pending",
    val dateLabel: String = "",
    val method: String = "Transfer DANA",
    val note: String = "",
    val receiptImageUrl: String = "",
    val receiptOcrText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @get:Exclude val offlineQueued: Boolean = false
) {
    @get:Exclude
    val isAutoGeneratedBill: Boolean
        get() = status == "unpaid" || (status == "pending" && method == AUTO_BILL_METHOD)
}

const val AUTO_BILL_METHOD = "Tagihan Otomatis"

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = "Pengumuman",
    val imageUrl: String = "",
    val content: String = "",
    val urgent: Boolean = false,
    val published: Boolean = true,
    val displayDate: String = "",
    val authorId: String = "",
    val authorName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MonthlySummary(
    @DocumentId val id: String = "",
    val month: String = "2026-11",
    val totalMembers: Long = 0L,
    val paidIuranCount: Long = 0L,
    val pendingCount: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DanaWebhookConfig(
    @DocumentId val id: String = "dana_webhook",
    val webhookUrl: String = "",
    val webhookSecret: String = "",
    val relayFromAndroidEnabled: Boolean = false,
    val autoVerifyWebhookEvents: Boolean = true,
    val updatedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DanaWebhookEvent(
    @DocumentId val id: String = "",
    val referenceId: String = "",
    val source: String = "webhook_server",
    val amount: Long = 0L,
    val senderName: String = "",
    val senderPhone: String = "",
    val houseHint: String = "",
    val monthHint: String = "",
    val paymentTypeHint: String = "",
    val rawPayload: String = "",
    val status: String = "RECEIVED",
    val matchedPaymentId: String = "",
    val matchedSummary: String = "",
    val verifiedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserPresence(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val house: String = "",
    val email: String = "",
    val role: String = "member",
    val roleLabel: String = "Warga / Member",
    val online: Boolean = true,
    val activeScreen: String = "Beranda",
    val lastSeenMillis: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @Exclude
    fun isEffectivelyOnline(nowMillis: Long = System.currentTimeMillis()): Boolean =
        online && (nowMillis - lastSeenMillis) <= 150_000L
}

@Keep
@IgnoreExtraProperties
data class ActivityLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userRole: String = "",
    val house: String = "",
    val actionType: String = "NAVIGATE",
    val screen: String = "Beranda",
    val detail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FinanceEntry(
    @DocumentId val id: String = "",
    val kind: String = "masuk",
    val title: String = "",
    val category: String = "Umum",
    val amount: Long = 0L,
    val month: String = "2026-11",
    val dateLabel: String = "",
    val note: String = "",
    val createdByUid: String = "",
    val createdByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BudgetPlan(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = "Infrastruktur",
    val month: String = "2026-11",
    val plannedAmount: Long = 0L,
    val realizedAmount: Long = 0L,
    val status: String = "planned",
    val note: String = "",
    val createdByUid: String = "",
    val updatedByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val progressPercent: Int
        get() = if (plannedAmount > 0L) {
            ((realizedAmount.toDouble() / plannedAmount.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        } else 0
}

enum class DuesStatus {
    PAID,
    PENDING,
    UNPAID,
    NOT_APPLICABLE
}

enum class PaymentPackage {
    IURAN,
    HIDMAT,
    BOTH
}

data class MonthSummary(
    val hidmatActive: Boolean,
    val iuranStatus: DuesStatus,
    val hidmatStatus: DuesStatus,
    val overall: DuesStatus
)

data class MonthOption(val code: String, val label: String)

const val MIN_BILLING_YEAR = 2026
const val MAX_BILLING_YEAR = 2050

val INDONESIAN_MONTHS = listOf(
    "Januari",
    "Februari",
    "Maret",
    "April",
    "Mei",
    "Juni",
    "Juli",
    "Agustus",
    "September",
    "Oktober",
    "November",
    "Desember"
)

val INDONESIAN_MONTHS_SHORT = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
)

val BILLING_YEARS: List<Int> = (MIN_BILLING_YEAR..MAX_BILLING_YEAR).toList()

fun formatPeriodCode(year: Int, month: Int): String =
    String.format(Locale.US, "%04d-%02d", year.coerceIn(MIN_BILLING_YEAR, MAX_BILLING_YEAR), month.coerceIn(1, 12))

fun isWithinBillingHorizon(year: Int, month: Int): Boolean =
    year in MIN_BILLING_YEAR..MAX_BILLING_YEAR && month in 1..12

fun isWithinBillingHorizon(periodCode: String): Boolean {
    val parts = periodCode.split("-")
    if (parts.size != 2) return false
    val year = parts[0].toIntOrNull() ?: return false
    val month = parts[1].toIntOrNull() ?: return false
    return isWithinBillingHorizon(year, month)
}

fun parsePeriodCode(code: String): Pair<Int, Int> {
    val parts = code.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(MIN_BILLING_YEAR, MAX_BILLING_YEAR) ?: MIN_BILLING_YEAR
    val month = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 11
    return year to month
}

fun currentBillingPeriodCode(calendar: Calendar = Calendar.getInstance()): String {
    val rawYear = calendar.get(Calendar.YEAR)
    val rawMonth = calendar.get(Calendar.MONTH) + 1
    return when {
        rawYear < MIN_BILLING_YEAR -> formatPeriodCode(MIN_BILLING_YEAR, 1)
        rawYear > MAX_BILLING_YEAR -> formatPeriodCode(MAX_BILLING_YEAR, 12)
        else -> formatPeriodCode(rawYear, rawMonth)
    }
}

val BILLING_MONTHS: List<MonthOption> = BILLING_YEARS.flatMap { year ->
    (1..12).map { month ->
        MonthOption(
            code = formatPeriodCode(year, month),
            label = "${INDONESIAN_MONTHS[month - 1]} $year"
        )
    }
}

fun monthNameOnly(monthNumber: Int): String =
    INDONESIAN_MONTHS.getOrElse(monthNumber - 1) { "Januari" }

fun monthLabel(code: String): String {
    val parts = code.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull()
    val month = parts.getOrNull(1)?.toIntOrNull()
    return if (year != null && month != null && month in 1..12) {
        "${INDONESIAN_MONTHS[month - 1]} $year"
    } else {
        code
    }
}

fun formatRupiah(amount: Long): String {
    val str = amount.toString()
    val sb = StringBuilder()
    var count = 0
    for (i in str.length - 1 downTo 0) {
        sb.append(str[i])
        count++
        if (count % 3 == 0 && i != 0) sb.append('.')
    }
    return "Rp ${sb.reverse()}"
}

fun computeMonthSummary(
    resident: Resident?,
    payments: List<Payment>,
    month: String
): MonthSummary {
    val hidmatActive = resident?.hidmatEnabled == true
    val monthPays = payments.filter { it.month == month }
    val iuranStatus = when {
        monthPays.any { it.type == "iuran" && it.status == "paid" } -> DuesStatus.PAID
        monthPays.any { it.type == "iuran" && it.status == "pending" && !it.isAutoGeneratedBill } -> DuesStatus.PENDING
        else -> DuesStatus.UNPAID
    }
    val hidmatStatus = when {
        !hidmatActive -> DuesStatus.NOT_APPLICABLE
        monthPays.any { it.type == "hidmat" && it.status == "paid" } -> DuesStatus.PAID
        monthPays.any { it.type == "hidmat" && it.status == "pending" && !it.isAutoGeneratedBill } -> DuesStatus.PENDING
        else -> DuesStatus.UNPAID
    }
    val overall = when {
        !hidmatActive -> iuranStatus
        iuranStatus == DuesStatus.PAID && hidmatStatus == DuesStatus.PAID -> DuesStatus.PAID
        iuranStatus == DuesStatus.PENDING || hidmatStatus == DuesStatus.PENDING ||
            iuranStatus == DuesStatus.PAID || hidmatStatus == DuesStatus.PAID -> DuesStatus.PENDING
        else -> DuesStatus.UNPAID
    }
    return MonthSummary(hidmatActive, iuranStatus, hidmatStatus, overall)
}

data class FundMethodBreakdown(
    val method: String,
    val count: Int,
    val totalAmount: Long
)

data class FundRecap(
    val month: String,
    val totalResidentsCount: Int,
    val paidIuranAmount: Long,
    val paidHidmatAmount: Long,
    val totalCollectedMonth: Long,
    val pendingVerificationAmount: Long,
    val expectedMonthTotal: Long,
    val unpaidOutstandingAmount: Long,
    val collectionPercent: Int,
    val yearTotalCollected: Long,
    val allTimeCollected: Long,
    val methodBreakdowns: List<FundMethodBreakdown>
)

fun computeFundRecap(
    month: String,
    residents: List<Resident>,
    payments: List<Payment>,
    settings: SystemSettings
): FundRecap {
    val activeMembers = residents.filter { !it.adminRole }
    val expectedIuran = activeMembers.size.toLong() * settings.iuranAmount
    val expectedHidmat = activeMembers.count { it.hidmatEnabled }.toLong() * settings.hidmatAmount
    val expectedTotal = expectedIuran + expectedHidmat

    val monthPaid = payments.filter { it.month == month && it.status == "paid" }
    val paidIuran = monthPaid.filter { it.type == "iuran" }.sumOf { it.amount }
    val paidHidmat = monthPaid.filter { it.type == "hidmat" }.sumOf { it.amount }
    val collectedMonth = paidIuran + paidHidmat

    val pendingVerif = payments
        .filter { it.month == month && it.status == "pending" && it.method != AUTO_BILL_METHOD }
        .sumOf { it.amount }

    val outstanding = (expectedTotal - collectedMonth).coerceAtLeast(0L)
    val pct = if (expectedTotal > 0L) {
        ((collectedMonth.toDouble() / expectedTotal.toDouble()) * 100.0).toInt().coerceIn(0, 100)
    } else 0

    val selectedYearPrefix = month.take(4)
    val yearCollected = payments
        .filter { it.status == "paid" && it.month.startsWith(selectedYearPrefix) }
        .sumOf { it.amount }
    val allTimeCollected = payments
        .filter { it.status == "paid" }
        .sumOf { it.amount }

    val breakdowns = monthPaid
        .groupBy { it.method.ifBlank { "Lainnya" } }
        .map { (method, list) ->
            FundMethodBreakdown(
                method = method,
                count = list.size,
                totalAmount = list.sumOf { it.amount }
            )
        }
        .sortedByDescending { it.totalAmount }

    return FundRecap(
        month = month,
        totalResidentsCount = activeMembers.size,
        paidIuranAmount = paidIuran,
        paidHidmatAmount = paidHidmat,
        totalCollectedMonth = collectedMonth,
        pendingVerificationAmount = pendingVerif,
        expectedMonthTotal = expectedTotal,
        unpaidOutstandingAmount = outstanding,
        collectionPercent = pct,
        yearTotalCollected = yearCollected,
        allTimeCollected = allTimeCollected,
        methodBreakdowns = breakdowns
    )
}

data class MonthlyTrendPoint(
    val monthCode: String,
    val shortLabel: String,
    val fullLabel: String,
    val iuranPaid: Long,
    val hidmatPaid: Long,
    val totalPaid: Long,
    val pendingAmount: Long,
    val unpaidAmount: Long,
    val expectedAmount: Long
)

data class AdminDashboardAnalytics(
    val selectedMonthRecap: FundRecap,
    val totalCollectedAllTime: Long,
    val totalCollectedSelectedYear: Long,
    val totalUnpaidSelectedMonth: Long,
    val totalPendingSelectedMonth: Long,
    val totalRecordedUnpaidAllPeriods: Long,
    val monthlyTrends: List<MonthlyTrendPoint>
)

fun formatCompactRupiah(amount: Long): String = when {
    amount >= 1_000_000_000L -> {
        val v = amount / 1_000_000_000.0
        String.format(Locale.US, "Rp%.1fM", v).replace(".0", "")
    }
    amount >= 1_000_000L -> {
        val v = amount / 1_000_000.0
        String.format(Locale.US, "Rp%.1fjt", v).replace(".0", "")
    }
    amount >= 1_000L -> {
        val v = amount / 1_000L
        "Rp${v}rb"
    }
    else -> "Rp$amount"
}

fun computeAdminDashboardAnalytics(
    selectedMonth: String,
    residents: List<Resident>,
    payments: List<Payment>,
    settings: SystemSettings,
    monthWindowSize: Int = 6
): AdminDashboardAnalytics {
    val selectedRecap = computeFundRecap(selectedMonth, residents, payments, settings)
    val activeMembers = residents.filter { !it.adminRole }
    val expectedBaseline = activeMembers.size.toLong() * settings.iuranAmount +
        activeMembers.count { it.hidmatEnabled }.toLong() * settings.hidmatAmount

    val (selYear, selMonthNum) = parsePeriodCode(selectedMonth)
    val periodCodes: List<String> = if (monthWindowSize >= 12) {
        (1..12).map { m -> formatPeriodCode(selYear, m) }
    } else {
        val count = monthWindowSize.coerceIn(3, 12)
        val rawOffsets = (-(count - 1)..0).map { offset ->
            val totalIdx = (selYear * 12 + (selMonthNum - 1)) + offset
            val y = totalIdx / 12
            val m = (totalIdx % 12) + 1
            y to m
        }
        val valid = rawOffsets.filter { (y, m) -> isWithinBillingHorizon(y, m) }
        val finalPairs = if (valid.size >= 3) {
            valid
        } else {
            (1..count).map { m -> MIN_BILLING_YEAR to m }
        }
        finalPairs.map { (y, m) -> formatPeriodCode(y, m) }
    }

    val trends = periodCodes.map { code ->
        val (_, mNum) = parsePeriodCode(code)
        val monthPays = payments.filter { it.month == code }
        val paidIuran = monthPays.filter { it.status == "paid" && it.type == "iuran" }.sumOf { it.amount }
        val paidHidmat = monthPays.filter { it.status == "paid" && it.type == "hidmat" }.sumOf { it.amount }
        val totalPaid = paidIuran + paidHidmat
        val pendingAmt = monthPays
            .filter { it.status == "pending" && it.method != AUTO_BILL_METHOD }
            .sumOf { it.amount }
        val explicitUnpaidAmt = monthPays
            .filter { it.isAutoGeneratedBill }
            .sumOf { it.amount }
        val computedGap = (expectedBaseline - totalPaid - pendingAmt).coerceAtLeast(0L)
        val unpaidAmt = maxOf(explicitUnpaidAmt, if (code <= selectedMonth) computedGap else explicitUnpaidAmt)

        MonthlyTrendPoint(
            monthCode = code,
            shortLabel = INDONESIAN_MONTHS_SHORT.getOrElse(mNum - 1) { "Jan" },
            fullLabel = monthLabel(code),
            iuranPaid = paidIuran,
            hidmatPaid = paidHidmat,
            totalPaid = totalPaid,
            pendingAmount = pendingAmt,
            unpaidAmount = unpaidAmt,
            expectedAmount = expectedBaseline
        )
    }

    val recordedUnpaidAll = payments
        .filter { it.isAutoGeneratedBill }
        .sumOf { it.amount }

    return AdminDashboardAnalytics(
        selectedMonthRecap = selectedRecap,
        totalCollectedAllTime = selectedRecap.allTimeCollected,
        totalCollectedSelectedYear = selectedRecap.yearTotalCollected,
        totalUnpaidSelectedMonth = selectedRecap.unpaidOutstandingAmount,
        totalPendingSelectedMonth = selectedRecap.pendingVerificationAmount,
        totalRecordedUnpaidAllPeriods = maxOf(recordedUnpaidAll, selectedRecap.unpaidOutstandingAmount),
        monthlyTrends = trends
    )
}

data class CurrentMonthStatusSummary(
    val month: String,
    val totalActiveResidents: Int,
    val paidResidents: Int,
    val pendingResidents: Int,
    val unpaidResidents: Int
)

fun computeCurrentMonthStatusSummary(
    month: String,
    residents: List<Resident>,
    payments: List<Payment>
): CurrentMonthStatusSummary {
    val activeMembers = residents.filter { !it.adminRole }
    var paidCount = 0
    var pendingCount = 0
    var unpaidCount = 0
    activeMembers.forEach { resident ->
        val mine = payments.filter { it.userId == resident.id && it.month == month }
        when (computeMonthSummary(resident, mine, month).overall) {
            DuesStatus.PAID -> paidCount++
            DuesStatus.PENDING -> pendingCount++
            DuesStatus.UNPAID, DuesStatus.NOT_APPLICABLE -> unpaidCount++
        }
    }
    return CurrentMonthStatusSummary(
        month = month,
        totalActiveResidents = activeMembers.size,
        paidResidents = paidCount,
        pendingResidents = pendingCount,
        unpaidResidents = unpaidCount
    )
}

data class FinanceSummary(
    val selectedMonth: String,
    val totalIuranPaidAllTime: Long,
    val totalKasBlokInitial: Long,
    val totalManualMasuk: Long,
    val totalKeluar: Long,
    val currentKasBalance: Long,
    val monthMasukTotal: Long,
    val monthKeluarTotal: Long,
    val monthKasBlokTotal: Long,
    val totalPlannedBudget: Long,
    val totalRealizedBudget: Long
)

fun computeFinanceSummary(
    selectedMonth: String,
    entries: List<FinanceEntry>,
    payments: List<Payment>,
    budgetPlans: List<BudgetPlan>
): FinanceSummary {
    val paidDuesAllTime = payments.filter { it.status == "paid" }.sumOf { it.amount }
    val paidDuesMonth = payments.filter { it.status == "paid" && it.month == selectedMonth }.sumOf { it.amount }

    val kasBlokAll = entries.filter { it.kind == "kas_blok" }.sumOf { it.amount }
    val manualMasukAll = entries.filter { it.kind == "masuk" }.sumOf { it.amount }
    val keluarAll = entries.filter { it.kind == "keluar" }.sumOf { it.amount }

    val balance = (kasBlokAll + manualMasukAll + paidDuesAllTime) - keluarAll

    val monthEntries = entries.filter { it.month == selectedMonth }
    val monthMasuk = monthEntries.filter { it.kind == "masuk" }.sumOf { it.amount } + paidDuesMonth
    val monthKeluar = monthEntries.filter { it.kind == "keluar" }.sumOf { it.amount }
    val monthKas = monthEntries.filter { it.kind == "kas_blok" }.sumOf { it.amount }

    val plannedTotal = budgetPlans.sumOf { it.plannedAmount }
    val realizedTotal = budgetPlans.sumOf { it.realizedAmount }

    return FinanceSummary(
        selectedMonth = selectedMonth,
        totalIuranPaidAllTime = paidDuesAllTime,
        totalKasBlokInitial = kasBlokAll,
        totalManualMasuk = manualMasukAll,
        totalKeluar = keluarAll,
        currentKasBalance = balance,
        monthMasukTotal = monthMasuk,
        monthKeluarTotal = monthKeluar,
        monthKasBlokTotal = monthKas,
        totalPlannedBudget = plannedTotal,
        totalRealizedBudget = realizedTotal
    )
}

data class MonthlyKasPoint(
    val monthCode: String,
    val shortLabel: String,
    val fullLabel: String,
    val masukAmount: Long,
    val keluarAmount: Long,
    val kasInitialAmount: Long,
    val netDelta: Long,
    val runningBalance: Long
)

fun computeMonthlyKasTimeline(
    selectedMonth: String,
    entries: List<FinanceEntry>,
    payments: List<Payment>,
    monthWindowSize: Int = 6
): List<MonthlyKasPoint> {
    val (selYear, selMonthNum) = parsePeriodCode(selectedMonth)
    val count = monthWindowSize.coerceIn(3, 12)
    val rawOffsets = (-(count - 1)..0).map { offset ->
        val totalIdx = (selYear * 12 + (selMonthNum - 1)) + offset
        val y = totalIdx / 12
        val m = (totalIdx % 12) + 1
        y to m
    }
    val valid = rawOffsets.filter { (y, m) -> isWithinBillingHorizon(y, m) }
    val finalPairs = if (valid.size >= 3) {
        valid
    } else {
        (1..count).map { m -> MIN_BILLING_YEAR to m }
    }
    val periodCodes = finalPairs.map { (y, m) -> formatPeriodCode(y, m) }
    val firstPeriod = periodCodes.first()

    // Cumulative balance strictly before the displayed window
    val priorEntriesMasuk = entries.filter { it.month < firstPeriod && it.kind == "masuk" }.sumOf { it.amount }
    val priorEntriesKas = entries.filter { it.month < firstPeriod && it.kind == "kas_blok" }.sumOf { it.amount }
    val priorEntriesKeluar = entries.filter { it.month < firstPeriod && it.kind == "keluar" }.sumOf { it.amount }
    val priorPaidDues = payments.filter { it.month < firstPeriod && it.status == "paid" }.sumOf { it.amount }
    var running = (priorEntriesKas + priorEntriesMasuk + priorPaidDues) - priorEntriesKeluar

    return periodCodes.map { code ->
        val (_, mNum) = parsePeriodCode(code)
        val mEntries = entries.filter { it.month == code }
        val kasInit = mEntries.filter { it.kind == "kas_blok" }.sumOf { it.amount }
        val manualIn = mEntries.filter { it.kind == "masuk" }.sumOf { it.amount }
        val duesIn = payments.filter { it.month == code && it.status == "paid" }.sumOf { it.amount }
        val totalIn = manualIn + duesIn
        val totalOut = mEntries.filter { it.kind == "keluar" }.sumOf { it.amount }
        val net = (kasInit + totalIn) - totalOut
        running += net
        MonthlyKasPoint(
            monthCode = code,
            shortLabel = INDONESIAN_MONTHS_SHORT.getOrElse(mNum - 1) { "Jan" },
            fullLabel = monthLabel(code),
            masukAmount = totalIn,
            keluarAmount = totalOut,
            kasInitialAmount = kasInit,
            netDelta = net,
            runningBalance = running
        )
    }
}


