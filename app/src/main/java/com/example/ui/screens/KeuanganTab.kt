package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.export.FinanceReportExporter
import com.example.data.model.BudgetPlan
import com.example.data.model.FinanceEntry
import com.example.data.model.MonthlyKasPoint
import com.example.data.model.computeFinanceSummary
import com.example.data.model.computeMonthlyKasTimeline
import com.example.data.model.formatCompactRupiah
import com.example.data.model.formatRupiah
import com.example.data.model.monthLabel
import com.example.ui.components.AnimatedRupiahText
import com.example.ui.components.StaggeredEntrance
import com.example.ui.theme.DanaBlue
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald950
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Rose600
import com.example.ui.theme.Teal800
import com.example.ui.theme.adaptiveColors
import com.example.ui.theme.adaptiveFieldColors
import com.example.ui.viewmodel.MainUiState
import kotlin.math.roundToInt

private enum class FinanceFilterTab(val label: String) {
    ALL("Semua"),
    MASUK("Masuk"),
    KELUAR("Keluar"),
    KAS_BLOK("Kas Blok"),
    ANGGARAN("Anggaran")
}

private enum class KasChartMode {
    BALANCE,
    FLOW
}

private enum class FinanceEntryDialogType {
    NONE,
    INCOMING,
    OUTGOING
}

@Composable
fun KeuanganTab(
    state: MainUiState,
    onSaveFinanceEntry: (
        existingId: String?,
        kind: String,
        title: String,
        category: String,
        amount: Long,
        month: String,
        note: String
    ) -> Unit,
    onDeleteFinanceEntry: (String) -> Unit,
    onSaveBudgetPlan: (
        existingId: String?,
        title: String,
        category: String,
        month: String,
        plannedAmount: Long,
        realizedAmount: Long,
        status: String,
        note: String
    ) -> Unit,
    onDeleteBudgetPlan: (String) -> Unit,
    onExportCompleted: (formatLabel: String, message: String) -> Unit = { _, _ -> }
) {
    val ac = adaptiveColors
    val context = LocalContext.current
    val resident = state.currentResident ?: return
    val isPrimaryAdmin = resident.primaryAdmin

    var filterTab by remember { mutableStateOf(FinanceFilterTab.ALL) }
    var filterMonthOnly by remember { mutableStateOf(false) }
    var financeSearchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }

    var entryDialogType by remember { mutableStateOf(FinanceEntryDialogType.NONE) }
    var editingEntry by remember { mutableStateOf<FinanceEntry?>(null) }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetPlan?>(null) }

    val paymentSource = remember(resident.adminRole, state.allPayments, state.userPayments) {
        if (resident.adminRole) state.allPayments else state.userPayments
    }

    val summary = remember(
        state.selectedMonth,
        state.financeEntries,
        paymentSource,
        state.budgetPlans
    ) {
        computeFinanceSummary(
            selectedMonth = state.selectedMonth,
            entries = state.financeEntries,
            payments = paymentSource,
            budgetPlans = state.budgetPlans
        )
    }

    val kasTimeline = remember(state.selectedMonth, state.financeEntries, paymentSource) {
        computeMonthlyKasTimeline(
            selectedMonth = state.selectedMonth,
            entries = state.financeEntries,
            payments = paymentSource,
            monthWindowSize = 6
        )
    }

    val financeCsvExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csv = FinanceReportExporter.buildFinancialSummaryCsv(
                    selectedMonth = state.selectedMonth,
                    blokName = state.settings.blokName,
                    summary = summary,
                    kasTimeline = kasTimeline,
                    entries = state.financeEntries,
                    budgetPlans = state.budgetPlans,
                    adminName = resident.name.ifBlank { resident.email }
                )
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
                onExportCompleted("CSV", "Laporan ringkasan keuangan CSV berhasil diekspor!")
            } catch (e: Exception) {
                onExportCompleted("CSV", e.localizedMessage ?: "Gagal mengekspor CSV keuangan.")
            }
        }
    }

    val financePdfExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    FinanceReportExporter.writeFinancialSummaryPdf(
                        outputStream = out,
                        selectedMonth = state.selectedMonth,
                        blokName = state.settings.blokName,
                        summary = summary,
                        kasTimeline = kasTimeline,
                        entries = state.financeEntries,
                        budgetPlans = state.budgetPlans,
                        adminName = resident.name.ifBlank { resident.email }
                    )
                }
                onExportCompleted("PDF", "Laporan ringkasan keuangan PDF berhasil diekspor!")
            } catch (e: Exception) {
                onExportCompleted("PDF", e.localizedMessage ?: "Gagal mengekspor PDF keuangan.")
            }
        }
    }

    val entryCategories = remember(state.financeEntries, state.budgetPlans) {
        val cats = (state.financeEntries.map { it.category } + state.budgetPlans.map { it.category })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        listOf("Semua") + cats
    }

    val cleanFinQuery = financeSearchQuery.trim().lowercase()
    val scopedEntries = remember(
        state.financeEntries,
        state.selectedMonth,
        filterMonthOnly,
        filterTab,
        cleanFinQuery,
        selectedCategory
    ) {
        state.financeEntries
            .filter { !filterMonthOnly || it.month == state.selectedMonth }
            .filter { selectedCategory == "Semua" || it.category.equals(selectedCategory, ignoreCase = true) }
            .filter {
                cleanFinQuery.isBlank() ||
                    it.title.lowercase().contains(cleanFinQuery) ||
                    it.category.lowercase().contains(cleanFinQuery) ||
                    it.dateLabel.lowercase().contains(cleanFinQuery) ||
                    monthLabel(it.month).lowercase().contains(cleanFinQuery) ||
                    it.note.lowercase().contains(cleanFinQuery)
            }
            .filter { entry ->
                when (filterTab) {
                    FinanceFilterTab.ALL -> true
                    FinanceFilterTab.MASUK -> entry.kind == "masuk"
                    FinanceFilterTab.KELUAR -> entry.kind == "keluar"
                    FinanceFilterTab.KAS_BLOK -> entry.kind == "kas_blok"
                    FinanceFilterTab.ANGGARAN -> false
                }
            }
    }

    val scopedBudgets = remember(state.budgetPlans, state.selectedMonth, filterMonthOnly, cleanFinQuery, selectedCategory) {
        state.budgetPlans
            .filter { !filterMonthOnly || it.month == state.selectedMonth }
            .filter { selectedCategory == "Semua" || it.category.equals(selectedCategory, ignoreCase = true) }
            .filter {
                cleanFinQuery.isBlank() ||
                    it.title.lowercase().contains(cleanFinQuery) ||
                    it.category.lowercase().contains(cleanFinQuery) ||
                    monthLabel(it.month).lowercase().contains(cleanFinQuery) ||
                    it.note.lowercase().contains(cleanFinQuery)
            }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.testTag("keuangan_tab_root")
    ) {
        // 1. Primary Admin Control Banner (hidden for Warga & Pengurus Blok)
        if (isPrimaryAdmin) {
            Surface(
                color = ac.emeraldSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ac.emeraldBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("finance_admin_utama_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = ac.emeraldText,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kendali Penuh Admin Utama",
                            style = MaterialTheme.typography.labelLarge,
                            color = ac.emeraldText,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Catat keuangan masuk, keluar, saldo kas blok, serta rencana anggaran secara manual.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ac.textSecondary
                        )
                    }
                }
            }
        }

        // 2. Hero Kas Blok Card
        StaggeredEntrance(delayMillis = 20) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Emerald950, Emerald800, Teal800)))
                    .drawBehind {
                        drawCircle(
                            color = Emerald500.copy(alpha = 0.2f),
                            radius = size.maxDimension * 0.45f,
                            center = Offset(size.width * 0.92f, size.height * 0.14f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.06f),
                            radius = size.maxDimension * 0.28f,
                            center = Offset(size.width * 0.08f, size.height * 0.92f)
                        )
                    }
                    .padding(18.dp)
                    .testTag("finance_hero_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SALDO KAS BLOK SAAT INI",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald100
                            )
                            AnimatedRupiahText(
                                targetAmount = summary.currentKasBalance,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                useMonospace = true
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalance,
                                    contentDescription = null,
                                    tint = Emerald100,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeroStatBox(
                            label = "Kas Blok Awal",
                            value = formatCompactRupiah(summary.totalKasBlokInitial),
                            accent = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        HeroStatBox(
                            label = "Masuk (${monthLabel(state.selectedMonth).take(3)})",
                            value = formatCompactRupiah(summary.monthMasukTotal),
                            accent = Emerald100,
                            modifier = Modifier.weight(1f)
                        )
                        HeroStatBox(
                            label = "Keluar (${monthLabel(state.selectedMonth).take(3)})",
                            value = formatCompactRupiah(summary.monthKeluarTotal),
                            accent = Color(0xFFFDA4AF),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Admin Utama Distinct Quick Input Buttons (Dana Masuk vs Dana Keluar vs Anggaran)
        if (isPrimaryAdmin) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            editingEntry = null
                            entryDialogType = FinanceEntryDialogType.INCOMING
                        },
                        enabled = !state.isMutating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald700,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("add_incoming_fund_button")
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kas Masuk", fontWeight = FontWeight.ExtraBold)
                    }

                    Button(
                        onClick = {
                            editingEntry = null
                            entryDialogType = FinanceEntryDialogType.OUTGOING
                        },
                        enabled = !state.isMutating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Rose600,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("add_outgoing_fund_button")
                    ) {
                        Icon(Icons.Filled.RemoveCircleOutline, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kas Keluar", fontWeight = FontWeight.ExtraBold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        editingBudget = null
                        showBudgetDialog = true
                    },
                    enabled = !state.isMutating,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DanaBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("add_budget_plan_button")
                ) {
                    Icon(Icons.Filled.Assignment, null, tint = ac.danaText, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Rencana Anggaran (RAB)", color = ac.danaText, fontWeight = FontWeight.ExtraBold)
                }

                // Direct PDF & CSV Financial Summary Export for Admin Utama
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            financePdfExporter.launch("Ringkasan-Keuangan-${state.selectedMonth}.pdf")
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ac.roseBorder),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("export_finance_pdf_button")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, null, tint = ac.roseText, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ekspor PDF", color = ac.roseText, fontWeight = FontWeight.ExtraBold)
                    }

                    OutlinedButton(
                        onClick = {
                            financeCsvExporter.launch("Ringkasan-Keuangan-${state.selectedMonth}.csv")
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ac.emeraldBorder),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("export_finance_csv_button")
                    ) {
                        Icon(Icons.Filled.Download, null, tint = ac.emeraldText, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ekspor CSV", color = ac.emeraldText, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // 4. Interactive Kas Balance & Cashflow Chart Card
        KasBalanceChartCard(
            points = kasTimeline,
            selectedMonth = state.selectedMonth,
            totalInflow = summary.totalKasBlokInitial + summary.totalManualMasuk + summary.totalIuranPaidAllTime,
            totalOutflow = summary.totalKeluar,
            plannedBudget = summary.totalPlannedBudget,
            realizedBudget = summary.totalRealizedBudget
        )

        // 5. Filter Chips + Search Bar + Period Toggle
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
            border = BorderStroke(1.dp, ac.borderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cari & Filter Transaksi",
                        style = MaterialTheme.typography.labelLarge,
                        color = ac.textPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Surface(
                        color = if (filterMonthOnly) Emerald700 else ac.subtleContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { filterMonthOnly = !filterMonthOnly }
                            .testTag("finance_month_filter_toggle")
                    ) {
                        Text(
                            text = if (filterMonthOnly) monthLabel(state.selectedMonth) else "Semua Periode",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (filterMonthOnly) Color.White else ac.textSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = financeSearchQuery,
                    onValueChange = { financeSearchQuery = it },
                    placeholder = { Text("Cari judul, tanggal, kategori, atau catatan...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = ac.textMuted)
                    },
                    trailingIcon = {
                        if (financeSearchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { financeSearchQuery = "" },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Clear, contentDescription = "Hapus pencarian", tint = ac.textMuted)
                            }
                        }
                    },
                    singleLine = true,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("finance_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FinanceFilterTab.entries.forEach { tab ->
                        val active = filterTab == tab
                        Surface(
                            color = if (active) Emerald700 else ac.subtleContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { filterTab = tab }
                                .testTag("finance_filter_${tab.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) Color.White else ac.textSecondary,
                                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                if (entryCategories.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        entryCategories.forEach { cat ->
                            val active = selectedCategory == cat
                            Surface(
                                color = if (active) ac.danaSurface else ac.cardElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (active) ac.danaBorder else ac.borderSubtle),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) ac.danaText else ac.textSecondary,
                                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Rencana Anggaran (RAB) Section
        if (filterTab == FinanceFilterTab.ALL || filterTab == FinanceFilterTab.ANGGARAN) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
                border = BorderStroke(1.dp, ac.borderSubtle),
                modifier = Modifier.testTag("budget_plans_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rencana Anggaran (${scopedBudgets.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = ac.textPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = formatCompactRupiah(scopedBudgets.sumOf { it.plannedAmount }),
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = ac.danaText,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (scopedBudgets.isEmpty()) {
                        Surface(
                            color = ac.cardElevated,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Belum ada rencana anggaran pada filter ini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ac.textMuted,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        scopedBudgets.forEach { plan ->
                            val (statusBg, statusFg, statusLabel) = when (plan.status) {
                                "completed" -> Triple(ac.emeraldSurface, ac.emeraldText, "Selesai")
                                "in_progress" -> Triple(ac.amberSurface, ac.amberText, "Berjalan")
                                else -> Triple(ac.danaSurface, ac.danaText, "Direncanakan")
                            }
                            Surface(
                                color = ac.cardElevated,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, ac.borderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = plan.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = ac.textPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${plan.category} • ${monthLabel(plan.month)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ac.textMuted
                                            )
                                        }
                                        Surface(color = statusBg, shape = CircleShape) {
                                            Text(
                                                text = statusLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = statusFg,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    LinearProgressIndicator(
                                        progress = { (plan.progressPercent / 100f).coerceIn(0f, 1f) },
                                        color = if (plan.status == "completed") Emerald600 else DanaBlue,
                                        trackColor = ac.borderSubtle,
                                        strokeCap = StrokeCap.Round,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(CircleShape)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Realisasi ${formatRupiah(plan.realizedAmount)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ac.textSecondary
                                        )
                                        Text(
                                            text = "Target ${formatRupiah(plan.plannedAmount)} (${plan.progressPercent}%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ac.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (plan.note.isNotBlank()) {
                                        Text(
                                            text = plan.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ac.textSecondary
                                        )
                                    }

                                    if (isPrimaryAdmin) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    editingBudget = plan
                                                    showBudgetDialog = true
                                                },
                                                modifier = Modifier
                                                    .heightIn(min = 48.dp)
                                                    .testTag("edit_budget_${plan.id}")
                                            ) {
                                                Icon(Icons.Filled.Edit, null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Ubah")
                                            }
                                            IconButton(
                                                onClick = { onDeleteBudgetPlan(plan.id) },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("delete_budget_${plan.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Hapus anggaran ${plan.title}",
                                                    tint = ac.roseText
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Mutasi Keuangan Masuk / Keluar / Kas Blok
        if (filterTab != FinanceFilterTab.ANGGARAN) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
                border = BorderStroke(1.dp, ac.borderSubtle),
                modifier = Modifier.testTag("finance_entries_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Catatan Mutasi Keuangan (${scopedEntries.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = ac.textPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )

                    if (scopedEntries.isEmpty()) {
                        Surface(
                            color = ac.cardElevated,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isPrimaryAdmin) {
                                    "Belum ada catatan keuangan. Gunakan tombol 'Catat Keuangan' untuk mencatat kas masuk, kas keluar, atau saldo awal kas blok."
                                } else {
                                    "Belum ada catatan mutasi keuangan manual pada periode ini."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = ac.textMuted,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        scopedEntries.forEachIndexed { idx, entry ->
                            val (badgeBg, badgeFg, kindLabel, icon) = when (entry.kind) {
                                "keluar" -> QuadEntry(ac.roseSurface, ac.roseText, "KELUAR", Icons.AutoMirrored.Filled.TrendingDown)
                                "kas_blok" -> QuadEntry(ac.danaSurface, ac.danaText, "KAS BLOK", Icons.Filled.AccountBalance)
                                else -> QuadEntry(ac.emeraldSurface, ac.emeraldText, "MASUK", Icons.AutoMirrored.Filled.TrendingUp)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = badgeBg,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = badgeFg,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(color = badgeBg, shape = CircleShape) {
                                            Text(
                                                text = kindLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = badgeFg,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = entry.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ac.textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = entry.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ac.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${monthLabel(entry.month)} • ${entry.dateLabel.ifBlank { "Tercatat" }}${if (entry.note.isNotBlank()) " • ${entry.note}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ac.textMuted
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = (if (entry.kind == "keluar") "- " else "+ ") + formatRupiah(entry.amount),
                                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                                        color = badgeFg,
                                        fontWeight = FontWeight.Black
                                    )
                                    if (isPrimaryAdmin) {
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editingEntry = entry
                                                    entryDialogType = if (entry.kind == "keluar") {
                                                        FinanceEntryDialogType.OUTGOING
                                                    } else {
                                                        FinanceEntryDialogType.INCOMING
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("edit_finance_${entry.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = "Edit ${entry.title}",
                                                    tint = ac.danaText,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteFinanceEntry(entry.id) },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("delete_finance_${entry.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Hapus ${entry.title}",
                                                    tint = ac.roseText,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (idx < scopedEntries.lastIndex) {
                                HorizontalDivider(color = ac.divider)
                            }
                        }
                    }
                }
            }
        }
    }

    if (isPrimaryAdmin && entryDialogType == FinanceEntryDialogType.INCOMING) {
        IncomingFundFormDialog(
            existing = editingEntry,
            defaultMonth = state.selectedMonth,
            isMutating = state.isMutating,
            onSave = { kind, title, category, amount, month, note ->
                onSaveFinanceEntry(editingEntry?.id, kind, title, category, amount, month, note)
                entryDialogType = FinanceEntryDialogType.NONE
                editingEntry = null
            },
            onDismiss = {
                entryDialogType = FinanceEntryDialogType.NONE
                editingEntry = null
            }
        )
    }

    if (isPrimaryAdmin && entryDialogType == FinanceEntryDialogType.OUTGOING) {
        OutgoingFundFormDialog(
            existing = editingEntry,
            defaultMonth = state.selectedMonth,
            isMutating = state.isMutating,
            onSave = { title, category, amount, month, note ->
                onSaveFinanceEntry(editingEntry?.id, "keluar", title, category, amount, month, note)
                entryDialogType = FinanceEntryDialogType.NONE
                editingEntry = null
            },
            onDismiss = {
                entryDialogType = FinanceEntryDialogType.NONE
                editingEntry = null
            }
        )
    }

    if (showBudgetDialog && isPrimaryAdmin) {
        BudgetPlanFormDialog(
            existing = editingBudget,
            defaultMonth = state.selectedMonth,
            isMutating = state.isMutating,
            onSave = { title, category, month, planned, realized, status, note ->
                onSaveBudgetPlan(editingBudget?.id, title, category, month, planned, realized, status, note)
                showBudgetDialog = false
            },
            onDismiss = { showBudgetDialog = false }
        )
    }
}

private data class QuadEntry<A, B, C, D>(val bg: A, val fg: B, val label: C, val icon: D)

@Composable
private fun HeroStatBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.24f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                color = accent,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun KasBalanceChartCard(
    points: List<MonthlyKasPoint>,
    selectedMonth: String,
    totalInflow: Long,
    totalOutflow: Long,
    plannedBudget: Long,
    realizedBudget: Long
) {
    val ac = adaptiveColors
    var chartMode by remember { mutableStateOf(KasChartMode.BALANCE) }
    val defaultIndex = remember(points, selectedMonth) {
        points.indexOfFirst { it.monthCode == selectedMonth }
            .takeIf { it >= 0 }
            ?: points.lastIndex.coerceAtLeast(0)
    }
    var selectedIdx by remember(points.size, defaultIndex) { mutableIntStateOf(defaultIndex) }
    val activePoint = points.getOrNull(selectedIdx.coerceIn(0, (points.size - 1).coerceAtLeast(0)))

    val anim = remember { Animatable(0f) }
    LaunchedEffect(points, chartMode) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
        border = BorderStroke(1.dp, ac.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("kas_balance_chart_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Grafik Saldo Kas & Arus Dana",
                        style = MaterialTheme.typography.titleSmall,
                        color = ac.textPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Ketuk atau geser grafik untuk melihat rincian 6 bulan",
                        style = MaterialTheme.typography.bodySmall,
                        color = ac.textMuted
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        KasChartMode.BALANCE to Icons.Filled.ShowChart,
                        KasChartMode.FLOW to Icons.Filled.BarChart
                    ).forEach { (mode, icon) ->
                        val active = chartMode == mode
                        Surface(
                            color = if (active) Emerald700 else ac.subtleContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { chartMode = mode }
                                .testTag("kas_chart_mode_${mode.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = mode.name,
                                    tint = if (active) Color.White else ac.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (activePoint != null) {
                Surface(
                    color = ac.cardElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ac.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = activePoint.fullLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = ac.textPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Masuk ${formatCompactRupiah(activePoint.masukAmount)} • Keluar ${formatCompactRupiah(activePoint.keluarAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ac.textSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "SALDO KAS",
                                style = MaterialTheme.typography.labelSmall,
                                color = ac.emeraldText
                            )
                            Text(
                                text = formatRupiah(activePoint.runningBalance),
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                                color = if (activePoint.runningBalance >= 0L) ac.emeraldText else ac.roseText,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            if (points.isNotEmpty()) {
                val maxBalance = maxOf(
                    points.maxOf { maxOf(it.runningBalance, 0L) },
                    points.maxOf { maxOf(it.masukAmount, it.keluarAmount) },
                    100_000L
                ).toFloat()

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .pointerInput(points) {
                            detectTapGestures { offset ->
                                if (points.isNotEmpty() && size.width > 0) {
                                    val step = size.width.toFloat() / points.size.coerceAtLeast(1)
                                    val idx = (offset.x / step).roundToInt().coerceIn(0, points.lastIndex)
                                    selectedIdx = idx
                                }
                            }
                        }
                        .pointerInput(points) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                if (points.isNotEmpty() && size.width > 0) {
                                    val step = size.width.toFloat() / points.size.coerceAtLeast(1)
                                    val idx = (change.position.x / step).roundToInt().coerceIn(0, points.lastIndex)
                                    selectedIdx = idx
                                }
                            }
                        }
                ) {
                    val topPad = 14.dp.toPx()
                    val bottomPad = 12.dp.toPx()
                    val chartH = (size.height - topPad - bottomPad).coerceAtLeast(20f)
                    val p = anim.value
                    val gridColor = ac.borderSubtle.copy(alpha = 0.65f)

                    // Horizontal dashed reference lines
                    for (i in 0..3) {
                        val gy = topPad + chartH * (i / 3f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, gy),
                            end = Offset(size.width, gy),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }

                    val count = points.size
                    val slotW = size.width / count.coerceAtLeast(1)

                    if (chartMode == KasChartMode.BALANCE) {
                        val coords = points.mapIndexed { idx, pt ->
                            val x = idx * slotW + slotW / 2f
                            val ratio = (pt.runningBalance.coerceAtLeast(0L) / maxBalance).coerceIn(0f, 1f)
                            val y = topPad + chartH * (1f - ratio)
                            Offset(x, y)
                        }

                        val linePath = Path().apply {
                            coords.forEachIndexed { idx, pt ->
                                if (idx == 0) moveTo(pt.x, pt.y)
                                else {
                                    val prev = coords[idx - 1]
                                    val midX = (prev.x + pt.x) / 2f
                                    cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                                }
                            }
                        }

                        val areaPath = Path().apply {
                            addPath(linePath)
                            lineTo(coords.last().x, topPad + chartH)
                            lineTo(coords.first().x, topPad + chartH)
                            close()
                        }

                        clipRect(right = size.width * p) {
                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    listOf(Emerald500.copy(alpha = 0.34f), Color.Transparent),
                                    startY = topPad,
                                    endY = topPad + chartH
                                )
                            )
                            drawPath(
                                path = linePath,
                                color = Emerald600,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        coords.forEachIndexed { idx, pt ->
                            val isSel = idx == selectedIdx
                            drawCircle(
                                color = if (isSel) DanaBlue else Emerald600,
                                radius = if (isSel) 6.dp.toPx() else 3.5.dp.toPx(),
                                center = pt
                            )
                            if (isSel) {
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.5.dp.toPx(),
                                    center = pt
                                )
                            }
                        }
                    } else {
                        val barW = (slotW * 0.26f).coerceIn(8.dp.toPx(), 18.dp.toPx())
                        val gap = 4.dp.toPx()
                        points.forEachIndexed { idx, pt ->
                            val centerX = idx * slotW + slotW / 2f
                            val inH = ((pt.masukAmount / maxBalance).coerceIn(0f, 1f)) * chartH * p
                            val outH = ((pt.keluarAmount / maxBalance).coerceIn(0f, 1f)) * chartH * p

                            drawRoundRect(
                                brush = Brush.verticalGradient(listOf(Emerald500, Emerald700)),
                                topLeft = Offset(centerX - barW - gap / 2f, topPad + chartH - inH),
                                size = Size(barW, inH.coerceAtLeast(if (pt.masukAmount > 0) 4.dp.toPx() else 0f)),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                            drawRoundRect(
                                color = Rose600,
                                topLeft = Offset(centerX + gap / 2f, topPad + chartH - outH),
                                size = Size(barW, outH.coerceAtLeast(if (pt.keluarAmount > 0) 4.dp.toPx() else 0f)),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }

                // Month axis labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    points.forEachIndexed { idx, pt ->
                        val isSel = idx == selectedIdx
                        Text(
                            text = pt.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSel) ac.emeraldText else ac.textMuted,
                            fontWeight = if (isSel) FontWeight.Black else FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedIdx = idx }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = ac.divider)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Masuk: ${formatRupiah(totalInflow)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ac.emeraldText,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total Keluar: ${formatRupiah(totalOutflow)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ac.roseText,
                    fontWeight = FontWeight.Bold
                )
            }
            if (plannedBudget > 0L) {
                Text(
                    text = "Total Target RAB: ${formatRupiah(plannedBudget)} • Terealisasi: ${formatRupiah(realizedBudget)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ac.textMuted
                )
            }
        }
    }
}

@Composable
private fun IncomingFundFormDialog(
    existing: FinanceEntry?,
    defaultMonth: String,
    isMutating: Boolean,
    onSave: (kind: String, title: String, category: String, amount: Long, month: String, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    val ac = adaptiveColors
    var kind by remember(existing) {
        mutableStateOf(if (existing?.kind == "kas_blok") "kas_blok" else "masuk")
    }
    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var category by remember(existing) { mutableStateOf(existing?.category ?: "Donasi / Pemasukan") }
    var amountText by remember(existing) { mutableStateOf(existing?.amount?.toString() ?: "") }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }
    val month = existing?.month?.ifBlank { defaultMonth } ?: defaultMonth

    val quickCategories = listOf("Iuran Tunai", "Donasi Warga", "Saldo Kas Blok", "Sumbangan Lain")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ac.cardContainer,
        titleContentColor = ac.textPrimary,
        textContentColor = ac.textPrimary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(color = ac.emeraldSurface, shape = RoundedCornerShape(10.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = ac.emeraldText,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Text(if (existing != null) "Ubah Kas Masuk" else "Input Dana Masuk / Kas Blok")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tipe Pemasukan • Periode ${monthLabel(month)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ac.emeraldText,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "masuk" to "Pemasukan Kas",
                        "kas_blok" to "Saldo Kas Blok"
                    ).forEach { (key, label) ->
                        val selected = kind == key
                        Surface(
                            color = if (selected) Emerald700 else ac.subtleContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { kind = key }
                                .testTag("finance_kind_$key")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) Color.White else ac.textSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Sumber / Keterangan Masuk") },
                    singleLine = true,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("finance_title_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickCategories.forEach { cat ->
                        val active = category == cat
                        Surface(
                            color = if (active) ac.emeraldSurface else ac.subtleContainer,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, if (active) ac.emeraldText else ac.borderSubtle),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) ac.emeraldText else ac.textSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori Pemasukan") },
                    singleLine = true,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Nominal Masuk (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("finance_amount_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan tambahan (opsional)") },
                    minLines = 2,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amountVal = amountText.toLongOrNull() ?: 0L
            Button(
                onClick = { onSave(kind, title, category, amountVal, month, note) },
                enabled = !isMutating && title.isNotBlank() && amountVal > 0L,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                modifier = Modifier.testTag("save_incoming_fund_button")
            ) {
                Text("Simpan Masuk")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = ac.textSecondary)
            }
        }
    )
}

@Composable
private fun OutgoingFundFormDialog(
    existing: FinanceEntry?,
    defaultMonth: String,
    isMutating: Boolean,
    onSave: (title: String, category: String, amount: Long, month: String, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    val ac = adaptiveColors
    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var category by remember(existing) { mutableStateOf(existing?.category ?: "Operasional Blok") }
    var amountText by remember(existing) { mutableStateOf(existing?.amount?.toString() ?: "") }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }
    val month = existing?.month?.ifBlank { defaultMonth } ?: defaultMonth

    val quickCategories = listOf("Keamanan", "Kebersihan", "Perbaikan Fasilitas", "Operasional Blok", "Sosial")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ac.cardContainer,
        titleContentColor = ac.textPrimary,
        textContentColor = ac.textPrimary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(color = ac.roseSurface, shape = RoundedCornerShape(10.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = ac.roseText,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Text(if (existing != null) "Ubah Kas Keluar" else "Input Pengeluaran / Kas Keluar")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pengeluaran Kas • Periode ${monthLabel(month)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ac.roseText,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Keperluan / Keterangan Keluar") },
                    singleLine = true,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("outgoing_title_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickCategories.forEach { cat ->
                        val active = category == cat
                        Surface(
                            color = if (active) ac.roseSurface else ac.subtleContainer,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, if (active) ac.roseText else ac.borderSubtle),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) ac.roseText else ac.textSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori Pengeluaran") },
                    singleLine = true,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Nominal Keluar (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("outgoing_amount_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan / Penerima Dana (opsional)") },
                    minLines = 2,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amountVal = amountText.toLongOrNull() ?: 0L
            Button(
                onClick = { onSave(title, category, amountVal, month, note) },
                enabled = !isMutating && title.isNotBlank() && amountVal > 0L,
                colors = ButtonDefaults.buttonColors(containerColor = Rose600, contentColor = Color.White),
                modifier = Modifier.testTag("save_outgoing_fund_button")
            ) {
                Text("Simpan Keluar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = ac.textSecondary)
            }
        }
    )
}

@Composable
private fun BudgetPlanFormDialog(
    existing: BudgetPlan?,
    defaultMonth: String,
    isMutating: Boolean,
    onSave: (
        title: String,
        category: String,
        month: String,
        plannedAmount: Long,
        realizedAmount: Long,
        status: String,
        note: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val ac = adaptiveColors
    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var category by remember(existing) { mutableStateOf(existing?.category ?: "Infrastruktur") }
    var plannedText by remember(existing) { mutableStateOf(existing?.plannedAmount?.toString() ?: "") }
    var realizedText by remember(existing) { mutableStateOf(existing?.realizedAmount?.toString() ?: "0") }
    var status by remember(existing) { mutableStateOf(existing?.status ?: "planned") }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }
    val month = existing?.month?.ifBlank { defaultMonth } ?: defaultMonth

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ac.cardContainer,
        titleContentColor = ac.textPrimary,
        textContentColor = ac.textPrimary,
        title = { Text(if (existing != null) "Ubah Rencana Anggaran" else "Tambah Rencana Anggaran") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Program / Kegiatan") },
                    singleLine = true,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_title_input")
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori (Keamanan / Kebersihan / Sosial)") },
                    singleLine = true,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = plannedText,
                        onValueChange = { plannedText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = adaptiveFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("budget_planned_input")
                    )
                    OutlinedTextField(
                        value = realizedText,
                        onValueChange = { realizedText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Realisasi (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = adaptiveFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Status Pelaksanaan",
                    style = MaterialTheme.typography.labelMedium,
                    color = ac.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "planned" to "Rencana",
                        "in_progress" to "Berjalan",
                        "completed" to "Selesai"
                    ).forEach { (key, label) ->
                        val selected = status == key
                        Surface(
                            color = if (selected) DanaBlue else ac.subtleContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { status = key }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) Color.White else ac.textSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Rincian / Catatan") },
                    minLines = 2,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val plannedVal = plannedText.toLongOrNull() ?: 0L
            val realizedVal = realizedText.toLongOrNull() ?: 0L
            Button(
                onClick = { onSave(title, category, month, plannedVal, realizedVal, status, note) },
                enabled = !isMutating && title.isNotBlank() && plannedVal > 0L,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DanaBlue,
                    contentColor = Color.White,
                    disabledContainerColor = ac.subtleContainer,
                    disabledContentColor = ac.textMuted
                ),
                modifier = Modifier.testTag("save_budget_plan_button")
            ) {
                Text("Simpan RAB")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = ac.textSecondary)
            }
        }
    )
}
