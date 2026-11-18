package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.example.ui.components.directionalTabTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DuesStatus
import com.example.data.model.MAX_DELEGATED_ADMINS_BY_PENGURUS
import com.example.data.model.PRIMARY_ADMIN_EMAIL
import com.example.data.model.Payment
import com.example.data.model.Resident
import com.example.data.model.SystemSettings
import com.example.data.model.TransactionDateFilter
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionTypeFilter
import com.example.data.model.buildUnifiedAdminTransactions
import com.example.data.model.computeMonthSummary
import com.example.data.model.currentBillingPeriodCode
import com.example.data.model.extractTransactionCategories
import com.example.data.model.filterAdminTransactions
import com.example.data.model.formatRupiah
import com.example.data.model.monthLabel
import com.example.ui.components.LivePulseDot
import com.example.ui.components.ReceiptPreviewDialog
import com.example.ui.components.StatusBadge
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
import com.example.ui.viewmodel.AdminFilter
import com.example.ui.viewmodel.MainUiState
import com.example.worker.MonthlyBillingScheduler
import com.example.worker.PendingPaymentNotificationScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AdminSubTab(val label: String, val icon: ImageVector) {
    RINGKASAN("Ringkasan", Icons.Filled.BarChart),
    WARGA("Warga & Tagihan", Icons.Filled.Group),
    PERAN("Peran", Icons.Filled.AdminPanelSettings),
    SISTEM("Sistem & DANA", Icons.Filled.Settings),
    LIVE_MONITOR("Live Monitor", Icons.Filled.Analytics)
}

@Composable
fun AdminWargaTab(
    state: MainUiState,
    onSetFilter: (AdminFilter) -> Unit,
    onSetSearch: (String) -> Unit,
    onVerifyPayment: (String) -> Unit,
    onRejectPayment: (String) -> Unit,
    onMarkPaid: (targetUid: String, type: String) -> Unit,
    onToggleHidmat: (targetUid: String, enabled: Boolean) -> Unit,
    onEditResidentProfile: (
        targetUid: String,
        username: String,
        name: String,
        house: String,
        phone: String,
        hidmatEnabled: Boolean,
        makeAdmin: Boolean,
        roleLabel: String
    ) -> Unit,
    onSetResidentRole: (targetUid: String, makeAdmin: Boolean, roleLabel: String) -> Unit,
    onDeleteResident: (String) -> Unit,
    onSaveSettings: (SystemSettings) -> Unit,
    onNavigateToInfo: () -> Unit,
    onPushBlast: () -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onTogglePendingNotifier: (Boolean) -> Unit = {},
    onTriggerPendingNotifications: () -> Unit = {},
    onGenerateMonthlyBills: () -> Unit,
    onToggleDanaAutoVerify: (Boolean) -> Unit,
    onVerifyLiveDanaManual: (eventId: Long, paymentId: String, senderHint: String) -> Unit,
    onClearDanaLiveEvents: () -> Unit,
    onSaveWebhookConfig: (String, String, Boolean, Boolean) -> Unit,
    onTestWebhookPing: (String, String) -> Unit,
    onVerifyWebhookEventManual: (eventId: String, paymentId: String, senderName: String) -> Unit,
    onDeleteWebhookEvent: (eventId: String) -> Unit,
    onBuildCsv: () -> String,
    onBuildFundRecapCsv: () -> String,
    onShowMessage: (String) -> Unit
) {
    val currentResident = state.currentResident ?: return
    if (!currentResident.adminRole) return

    val isPrimaryAdmin = currentResident.primaryAdmin
    val ac = adaptiveColors
    val context = LocalContext.current

    val availableTabs = remember(isPrimaryAdmin) {
        if (isPrimaryAdmin) {
            AdminSubTab.entries
        } else {
            listOf(AdminSubTab.RINGKASAN, AdminSubTab.WARGA, AdminSubTab.PERAN)
        }
    }
    var activeSubTab by remember(isPrimaryAdmin) { mutableStateOf(AdminSubTab.RINGKASAN) }
    LaunchedEffect(availableTabs, activeSubTab) {
        if (activeSubTab !in availableTabs) {
            activeSubTab = AdminSubTab.RINGKASAN
        }
    }

    var pendingDeleteResident by remember { mutableStateOf<Resident?>(null) }
    var editingResident by remember { mutableStateOf<Resident?>(null) }
    var previewReceiptPayment by remember { mutableStateOf<Payment?>(null) }
    var showPromoteDialog by remember { mutableStateOf(false) }
    var showSystemSettingsDialog by remember { mutableStateOf(false) }

    val csvExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csvText = onBuildCsv()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csvText.toByteArray(Charsets.UTF_8))
                }
                onShowMessage("Laporan CSV warga berhasil diekspor!")
            } catch (e: Exception) {
                onShowMessage(e.localizedMessage ?: "Gagal mengekspor CSV.")
            }
        }
    }

    val recapCsvExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csvText = onBuildFundRecapCsv()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csvText.toByteArray(Charsets.UTF_8))
                }
                onShowMessage("Rekapitulasi Dana CSV berhasil diekspor!")
            } catch (e: Exception) {
                onShowMessage(e.localizedMessage ?: "Gagal mengekspor Rekap Dana CSV.")
            }
        }
    }

    val delegatedResidents = remember(state.allResidents) {
        state.allResidents.filter { it.adminRole && !it.isPermanentPrimaryEmail }
    }
    val extraAdminRecords = remember(state.delegatedAdmins, delegatedResidents) {
        val knownIds = delegatedResidents.map { it.id }.toSet()
        state.delegatedAdmins.filter {
            it.userId !in knownIds && !it.email.equals(PRIMARY_ADMIN_EMAIL, ignoreCase = true)
        }
    }
    val totalDelegatedCount = delegatedResidents.size + extraAdminRecords.size
    val canPromoteMore = isPrimaryAdmin || totalDelegatedCount < MAX_DELEGATED_ADMINS_BY_PENGURUS
    val remainingSlots = if (isPrimaryAdmin) null else (MAX_DELEGATED_ADMINS_BY_PENGURUS - totalDelegatedCount).coerceAtLeast(0)

    val members = remember(state.allResidents) { state.allResidents.filter { !it.adminRole } }
    val managedAccounts = remember(state.allResidents, isPrimaryAdmin) {
        if (isPrimaryAdmin) {
            state.allResidents.filter { !it.isPermanentPrimaryEmail }
        } else {
            state.allResidents.filter { !it.adminRole }
        }
    }

    val query = state.adminSearchQuery.trim().lowercase()
    val filtered = remember(managedAccounts, query, state.adminFilter, state.allPayments, state.selectedMonth) {
        managedAccounts.filter { u ->
            val matchesQuery = query.isBlank() ||
                u.name.lowercase().contains(query) ||
                u.house.lowercase().contains(query) ||
                u.phone.contains(query) ||
                u.email.lowercase().contains(query)
            if (!matchesQuery) return@filter false
            if (state.adminFilter == AdminFilter.ALL) return@filter true
            val userPays = state.allPayments.filter { it.userId == u.id }
            val overall = computeMonthSummary(u, userPays, state.selectedMonth).overall
            when (state.adminFilter) {
                AdminFilter.ALL -> true
                AdminFilter.UNPAID -> overall == DuesStatus.UNPAID
                AdminFilter.PENDING -> overall == DuesStatus.PENDING
                AdminFilter.PAID -> overall == DuesStatus.PAID
            }
        }
    }

    val totalPaid = remember(members, state.allPayments, state.selectedMonth) {
        members.count { u ->
            val pays = state.allPayments.filter { it.userId == u.id }
            computeMonthSummary(u, pays, state.selectedMonth).iuranStatus == DuesStatus.PAID
        }
    }
    val pendingPays = remember(state.allPayments) {
        state.allPayments.filter { it.status == "pending" && !it.isAutoGeneratedBill }
    }
    val unpaidBillCount = remember(state.allPayments, state.selectedMonth) {
        state.allPayments.count { it.month == state.selectedMonth && it.isAutoGeneratedBill }
    }
    val residentLookup = remember(state.allResidents) {
        state.allResidents.associateBy { it.id }
    }
    val onlineNowCount = remember(state.onlinePresence) {
        state.onlinePresence.count { it.isEffectivelyOnline() }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.testTag("admin_warga_tab_root")
    ) {
        // Gradient Role Hero Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        if (isPrimaryAdmin) {
                            listOf(Emerald950, Emerald800, Teal800)
                        } else {
                            listOf(Color(0xFF0F2F4A), Color(0xFF134E4A), Emerald800)
                        }
                    )
                )
                .drawBehind {
                    drawCircle(
                        color = Emerald500.copy(alpha = 0.18f),
                        radius = size.maxDimension * 0.45f,
                        center = Offset(size.width * 0.92f, size.height * 0.15f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = size.maxDimension * 0.25f,
                        center = Offset(size.width * 0.08f, size.height * 0.88f)
                    )
                }
                .padding(16.dp)
                .testTag("admin_role_hero_header")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPrimaryAdmin) Icons.Filled.VerifiedUser else Icons.Filled.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Emerald100,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (isPrimaryAdmin) "ADMIN UTAMA BLOK CE" else "PENGURUS BLOK",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald100,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = currentResident.name.ifBlank { currentResident.email },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.28f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LivePulseDot(color = Emerald500, size = 7.dp)
                            Text(
                                text = if (isPrimaryAdmin) "Akses Penuh" else "Maks $MAX_DELEGATED_ADMINS_BY_PENGURUS Admin",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroQuickBadge("Warga", members.size.toString(), Modifier.weight(1f))
                    HeroQuickBadge("Perlu Verif", pendingPays.size.toString(), Modifier.weight(1f))
                    if (isPrimaryAdmin) {
                        HeroQuickBadge("Online Live", onlineNowCount.toString(), Modifier.weight(1f))
                    } else {
                        HeroQuickBadge("Kuota Admin", "$totalDelegatedCount/$MAX_DELEGATED_ADMINS_BY_PENGURUS", Modifier.weight(1f))
                    }
                }
            }
        }

        // Clean Horizontally Scrollable Segmented Sub-Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableTabs.forEach { tab ->
                val selected = activeSubTab == tab
                Surface(
                    color = if (selected) Emerald700 else ac.cardContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (selected) Emerald700 else ac.borderSubtle),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { activeSubTab = tab }
                        .testTag("admin_subtab_${tab.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = if (selected) Color.White else ac.emeraldText,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else ac.textPrimary,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Sub-Tab Content with directional slide + fade transition
        AnimatedContent(
            targetState = activeSubTab,
            transitionSpec = {
                directionalTabTransition(forward = targetState.ordinal >= initialState.ordinal)
            },
            modifier = Modifier.animateContentSize(
                animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
            ),
            label = "admin_subtab_content"
        ) { tab ->
            when (tab) {
                AdminSubTab.RINGKASAN -> {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        AdminAnalyticsDashboardCard(
                            state = state,
                            onExportRecapCsv = {
                                recapCsvExporter.launch("Rekap-Dana-${state.selectedMonth}.csv")
                            }
                        )
                        FundRecapCard(
                            state = state,
                            onExportRecapCsv = {
                                recapCsvExporter.launch("Rekap-Dana-${state.selectedMonth}.csv")
                            }
                        )
                        AdminTransactionHistoryCard(state = state)
                    }
                }

                AdminSubTab.WARGA -> {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Overview, Filter & Export CSV Card
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
                            border = BorderStroke(1.dp, ac.borderSubtle)
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
                                            text = if (isPrimaryAdmin) "Data Warga & Pengurus" else "Data Warga",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ac.textPrimary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = if (isPrimaryAdmin) {
                                                "Edit profil, aktifkan Hidmat, atau hapus akun warga & pengurus blok"
                                            } else {
                                                "Verifikasi iuran & kelola data warga"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ac.textMuted
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { csvExporter.launch("Laporan-Iuran-${state.selectedMonth}.csv") },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("export_csv_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Download,
                                            contentDescription = null,
                                            tint = ac.emeraldText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("CSV", fontWeight = FontWeight.Bold, color = ac.textPrimary)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AdminCounterBox("Warga", members.size.toString(), ac.emeraldSurface, ac.emeraldText, Modifier.weight(1f))
                                    AdminCounterBox("Lunas", totalPaid.toString(), ac.danaSurface, ac.danaText, Modifier.weight(1f))
                                    AdminCounterBox("Pending", pendingPays.size.toString(), ac.amberSurface, ac.amberText, Modifier.weight(1f))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val filters = listOf(
                                        AdminFilter.ALL to "Semua",
                                        AdminFilter.UNPAID to "Belum",
                                        AdminFilter.PENDING to "Verif",
                                        AdminFilter.PAID to "Lunas"
                                    )
                                    filters.forEach { (filter, label) ->
                                        val active = state.adminFilter == filter
                                        Surface(
                                            color = if (active) Emerald600 else ac.subtleContainer,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onSetFilter(filter) }
                                                .testTag("admin_filter_${filter.name.lowercase()}")
                                        ) {
                                            BoxIconText(label = label, active = active)
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = state.adminSearchQuery,
                                    onValueChange = onSetSearch,
                                    placeholder = { Text("Cari nama / rumah / HP...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = null,
                                            tint = ac.textMuted
                                        )
                                    },
                                    singleLine = true,
                                    colors = adaptiveFieldColors(),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_search_input")
                                )
                            }
                        }

                        // Pending Verification Queue
                        if (pendingPays.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = ac.amberSurface),
                                border = BorderStroke(1.dp, ac.amberBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Perlu Verifikasi (${pendingPays.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ac.amberText,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    pendingPays.forEach { pay ->
                                        val owner = residentLookup[pay.userId]
                                        val hasReceipt = pay.receiptImageUrl.isNotBlank() || pay.receiptOcrText.isNotBlank()
                                        Surface(
                                            color = ac.cardContainer,
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, ac.amberBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "${owner?.name ?: "Warga (${pay.userId.take(6)})"} • ${owner?.house ?: ""}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = ac.textPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${pay.type.uppercase()} • ${monthLabel(pay.month)} • ${formatRupiah(pay.amount)}${if (pay.note.isNotBlank()) " • ${pay.note}" else ""}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = ac.textSecondary
                                                )
                                                if (pay.receiptOcrText.isNotBlank()) {
                                                    Surface(
                                                        color = ac.emeraldSurface,
                                                        shape = RoundedCornerShape(10.dp),
                                                        border = BorderStroke(1.dp, ac.emeraldBorder)
                                                    ) {
                                                        Text(
                                                            text = "OCR Struk: ${pay.receiptOcrText}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = ac.emeraldText,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.padding(8.dp)
                                                        )
                                                    }
                                                }
                                                if (hasReceipt) {
                                                    OutlinedButton(
                                                        onClick = { previewReceiptPayment = pay },
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(min = 48.dp)
                                                            .testTag("admin_view_receipt_${pay.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.ReceiptLong,
                                                            contentDescription = null,
                                                            tint = ac.danaText,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Lihat Bukti Struk", color = ac.danaText, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedButton(
                                                        onClick = { onRejectPayment(pay.id) },
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ac.roseText),
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .heightIn(min = 48.dp)
                                                            .testTag("reject_payment_${pay.id}")
                                                    ) {
                                                        Text("Tolak", fontWeight = FontWeight.Bold)
                                                    }
                                                    Button(
                                                        onClick = { onVerifyPayment(pay.id) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Emerald600,
                                                            contentColor = Color.White
                                                        ),
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .heightIn(min = 48.dp)
                                                            .testTag("verify_payment_${pay.id}")
                                                    ) {
                                                        Text("Verifikasi", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Resident & (for Primary Admin) Pengurus List
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
                            border = BorderStroke(1.dp, ac.borderSubtle)
                        ) {
                            if (filtered.isEmpty()) {
                                Text(
                                    text = "Tidak ada data akun sesuai filter",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ac.textMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                )
                            } else {
                                Column {
                                    filtered.forEachIndexed { idx, u ->
                                        val userPays = state.allPayments.filter { it.userId == u.id }
                                        val sum = computeMonthSummary(u, userPays, state.selectedMonth)

                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = u.name,
                                                            style = MaterialTheme.typography.labelLarge,
                                                            color = ac.textPrimary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        if (u.adminRole) {
                                                            Surface(
                                                                color = ac.purpleSurface,
                                                                shape = CircleShape
                                                            ) {
                                                                Text(
                                                                    text = "PENGURUS",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = ac.purpleText,
                                                                    fontWeight = FontWeight.Black,
                                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        text = "${u.house}${if (u.phone.isNotBlank()) " • ${u.phone}" else ""}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = ac.textSecondary
                                                    )
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Surface(
                                                        color = ac.emeraldSurface,
                                                        shape = RoundedCornerShape(10.dp)
                                                    ) {
                                                        IconButton(
                                                            onClick = { editingResident = u },
                                                            modifier = Modifier
                                                                .size(48.dp)
                                                                .testTag("edit_resident_${u.id}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.Edit,
                                                                contentDescription = "Edit profil ${u.name}",
                                                                tint = ac.emeraldText,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                    // Only Primary Admin may delete non-primary accounts
                                                    if (isPrimaryAdmin && !u.isPermanentPrimaryEmail) {
                                                        Surface(
                                                            color = ac.roseSurface,
                                                            shape = RoundedCornerShape(10.dp)
                                                        ) {
                                                            IconButton(
                                                                onClick = { pendingDeleteResident = u },
                                                                modifier = Modifier
                                                                    .size(48.dp)
                                                                    .testTag("delete_resident_${u.id}")
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.Delete,
                                                                    contentDescription = "Hapus akun ${u.name}",
                                                                    tint = ac.roseText,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    color = ac.cardElevated,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "IURAN",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = ac.textSecondary
                                                        )
                                                        StatusBadge(status = sum.iuranStatus)
                                                    }
                                                }
                                                Surface(
                                                    color = ac.cardElevated,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "HIDMAT",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = ac.textSecondary
                                                        )
                                                        StatusBadge(status = sum.hidmatStatus)
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (!u.adminRole && sum.iuranStatus == DuesStatus.UNPAID) {
                                                    Button(
                                                        onClick = { onMarkPaid(u.id, "iuran") },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Emerald600,
                                                            contentColor = Color.White
                                                        ),
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .heightIn(min = 48.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Iuran", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                                if (!u.adminRole && sum.hidmatActive && sum.hidmatStatus == DuesStatus.UNPAID) {
                                                    Button(
                                                        onClick = { onMarkPaid(u.id, "hidmat") },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = DanaBlue,
                                                            contentColor = Color.White
                                                        ),
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .heightIn(min = 48.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Hidmat", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                                OutlinedButton(
                                                    onClick = { onToggleHidmat(u.id, !u.hidmatEnabled) },
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                        .testTag("toggle_hidmat_${u.id}")
                                                ) {
                                                    Text(
                                                        text = if (u.hidmatEnabled) "Matikan Hidmat" else "Aktifkan Hidmat",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = ac.textPrimary,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                        if (idx < filtered.lastIndex) {
                                            HorizontalDivider(color = ac.divider)
                                        }
                                    }
                                }
                            }
                        }

                        // Quick Monthly Bill Generation (available to both Pengurus Blok & Admin Utama)
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
                            border = BorderStroke(1.dp, ac.borderSubtle)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Tagihan Unpaid Periode ${monthLabel(state.selectedMonth)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ac.textPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "$unpaidBillCount record tagihan unpaid tercatat untuk periode ini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ac.textMuted
                                )
                                Button(
                                    onClick = onGenerateMonthlyBills,
                                    enabled = !state.isMutating,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Emerald700,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("generate_monthly_bills_button")
                                ) {
                                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Buat Tagihan ${monthLabel(state.selectedMonth)} Sekarang",
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                AdminSubTab.PERAN -> {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        AdminRoleManagementCard(
                            state = state,
                            onOpenPromoteDialog = { showPromoteDialog = true },
                            onDemoteAdmin = { uid ->
                                onSetResidentRole(uid, false, "Warga / Member")
                            },
                            onEditAdminProfile = { adm -> editingResident = adm }
                        )
                    }
                }

                AdminSubTab.SISTEM -> {
                    if (isPrimaryAdmin) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            AdminSystemToolsCard(
                                settings = state.settings,
                                onOpenEditSettings = { showSystemSettingsDialog = true },
                                onOpenAnnouncements = onNavigateToInfo
                            )
                            DanaLiveIntegrationCard(
                                state = state,
                                onToggleAutoVerify = onToggleDanaAutoVerify,
                                onVerifyLiveDanaManual = onVerifyLiveDanaManual,
                                onClearEvents = onClearDanaLiveEvents,
                                onShowMessage = onShowMessage
                            )
                            DanaWebhookCard(
                                state = state,
                                onSaveWebhookConfig = onSaveWebhookConfig,
                                onTestWebhookPing = onTestWebhookPing,
                                onVerifyWebhookEventManual = onVerifyWebhookEventManual,
                                onDeleteWebhookEvent = onDeleteWebhookEvent,
                                onShowMessage = onShowMessage
                            )

                            // Push Reminder Blast Control
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
                                border = BorderStroke(1.dp, ac.borderSubtle)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Push Notif Himbauan",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ac.textPrimary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Button(
                                        onClick = onPushBlast,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Rose600,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("push_blast_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.NotificationsActive,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Kirim ke Semua Warga Belum Bayar", fontWeight = FontWeight.ExtraBold)
                                    }
                                    Surface(
                                        color = ac.cardElevated,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, ac.borderSubtle)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Himbauan otomatis saat login",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = ac.textPrimary
                                            )
                                            Switch(
                                                checked = state.settings.reminderEnabled,
                                                onCheckedChange = onToggleReminder,
                                                colors = SwitchDefaults.colors(checkedTrackColor = Emerald700)
                                            )
                                        }
                                    }
                                }
                            }

                            // Automated Pending Payment Notification Service Card
                            PendingPaymentNotifierCard(
                                state = state,
                                onToggleService = onTogglePendingNotifier,
                                onTriggerNow = onTriggerPendingNotifications
                            )

                            // Automated 1st-of-Month Billing Worker Control Card
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
                                border = BorderStroke(1.dp, ac.borderSubtle)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Schedule,
                                            contentDescription = null,
                                            tint = ac.emeraldText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Tagihan Otomatis Tanggal 1",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = ac.textPrimary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Text(
                                        text = "Background service WorkManager berjalan otomatis setiap tanggal 1 pukul 00:05 untuk membuat tagihan unpaid Iuran & Hidmat bagi seluruh warga aktif (periode 2026 s.d. 2050).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ac.textSecondary
                                    )
                                    Surface(
                                        color = ac.cardElevated,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, ac.borderSubtle),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Jadwal Berikutnya", style = MaterialTheme.typography.bodySmall, color = ac.textMuted)
                                                Text(
                                                    text = MonthlyBillingScheduler.formatTimestamp(state.billingWorkerStatus.nextRunAtMillis),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = ac.emeraldText,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Terakhir Dijalankan", style = MaterialTheme.typography.bodySmall, color = ac.textMuted)
                                                val lastLabel = if (state.billingWorkerStatus.lastRunAtMillis > 0L) {
                                                    "${monthLabel(state.billingWorkerStatus.lastRunPeriod)} (${state.billingWorkerStatus.lastGeneratedCount} tagihan)"
                                                } else {
                                                    "Menunggu tanggal 1"
                                                }
                                                Text(
                                                    text = lastLabel,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = ac.textPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Tagihan Unpaid (${monthLabel(state.selectedMonth)})", style = MaterialTheme.typography.bodySmall, color = ac.textMuted)
                                                Text(
                                                    text = "$unpaidBillCount record",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = ac.textPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Button(
                                        onClick = onGenerateMonthlyBills,
                                        enabled = !state.isMutating,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Emerald700,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("generate_monthly_bills_system_button")
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Buat Tagihan ${monthLabel(state.selectedMonth)} Sekarang",
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AdminSubTab.LIVE_MONITOR -> {
                    if (isPrimaryAdmin) {
                        LiveMonitorSection(state = state)
                    }
                }
            }
        }
    }

    val toEdit = editingResident
    if (toEdit != null) {
        EditResidentDialog(
            resident = toEdit,
            currentAdminUid = currentResident.id,
            isPrimaryAdmin = isPrimaryAdmin,
            canPromoteMore = canPromoteMore,
            isMutating = state.isMutating,
            onSave = { username, name, house, phone, hidmatEnabled, makeAdmin, roleLabel ->
                onEditResidentProfile(
                    toEdit.id,
                    username,
                    name,
                    house,
                    phone,
                    hidmatEnabled,
                    makeAdmin,
                    roleLabel
                )
                editingResident = null
            },
            onDismiss = { editingResident = null }
        )
    }

    if (showPromoteDialog) {
        PromoteResidentDialog(
            residents = state.allResidents,
            remainingSlots = remainingSlots,
            isMutating = state.isMutating,
            onPromote = { uid, roleLabel ->
                onSetResidentRole(uid, true, roleLabel)
                showPromoteDialog = false
            },
            onDismiss = { showPromoteDialog = false }
        )
    }

    if (showSystemSettingsDialog && isPrimaryAdmin) {
        EditSystemSettingsDialog(
            settings = state.settings,
            isMutating = state.isMutating,
            onSave = { updated ->
                onSaveSettings(updated)
                showSystemSettingsDialog = false
            },
            onDismiss = { showSystemSettingsDialog = false }
        )
    }

    val toPreview = previewReceiptPayment
    if (toPreview != null) {
        val owner = residentLookup[toPreview.userId]
        ReceiptPreviewDialog(
            receiptImageUrl = toPreview.receiptImageUrl,
            receiptOcrText = toPreview.receiptOcrText,
            payerLabel = "${owner?.name ?: "Warga"} (${owner?.house ?: "-"}) • ${formatRupiah(toPreview.amount)}",
            onDismiss = { previewReceiptPayment = null }
        )
    }

    val toDelete = pendingDeleteResident
    if (toDelete != null && isPrimaryAdmin) {
        AlertDialog(
            onDismissRequest = { pendingDeleteResident = null },
            containerColor = ac.cardContainer,
            titleContentColor = ac.textPrimary,
            textContentColor = ac.textPrimary,
            title = { Text(if (toDelete.adminRole) "Hapus Akun Pengurus?" else "Hapus Akun Warga?") },
            text = {
                Text("${toDelete.name} (${toDelete.house}) akan dihapus dari sistem Blok CE PSA.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteResident(toDelete.id)
                        pendingDeleteResident = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600, contentColor = Color.White)
                ) {
                    Text("Ya, Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteResident = null }) {
                    Text("Batal", color = ac.textSecondary)
                }
            }
        )
    }
}

private enum class ActivityLogFilter(val label: String) {
    ALL("Semua"),
    NAVIGATION("Navigasi & Login"),
    PAYMENT("Pembayaran"),
    ADMIN("Admin & Sistem"),
    FINANCE("Keuangan");

    fun matches(actionType: String): Boolean = when (this) {
        ALL -> true
        NAVIGATION -> actionType in setOf("LOGIN", "NAVIGATE", "INFO_POST")
        PAYMENT -> actionType in setOf("PAYMENT_SUBMIT", "RECEIPT_SCAN")
        ADMIN -> actionType in setOf("ADMIN_ACTION", "SYSTEM_UPDATE")
        FINANCE -> actionType == "FINANCE_UPDATE"
    }
}

@Composable
private fun LiveMonitorSection(state: MainUiState) {
    val ac = adaptiveColors
    val fmt = remember { SimpleDateFormat("dd/MM HH:mm:ss", Locale("id", "ID")) }
    var selectedLogFilter by remember { mutableStateOf(ActivityLogFilter.ALL) }

    val sortedPresence = remember(state.onlinePresence) {
        val now = System.currentTimeMillis()
        state.onlinePresence.sortedWith(
            compareByDescending<com.example.data.model.UserPresence> { it.isEffectivelyOnline(now) }
                .thenByDescending { it.lastSeenMillis }
        )
    }
    val onlineCount = remember(sortedPresence) {
        val now = System.currentTimeMillis()
        sortedPresence.count { it.isEffectivelyOnline(now) }
    }
    val actionCounts = remember(state.activityLogs) {
        state.activityLogs.groupingBy { it.actionType.ifBlank { "AKTIVITAS" } }.eachCount()
    }
    val screenCounts = remember(state.activityLogs) {
        state.activityLogs
            .groupingBy { it.screen.ifBlank { "Beranda" } }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
    }
    val topScreen = screenCounts.firstOrNull()?.key ?: "-"
    val filteredLogs = remember(state.activityLogs, selectedLogFilter) {
        state.activityLogs.filter { selectedLogFilter.matches(it.actionType) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Live Online Presence Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
            border = BorderStroke(1.dp, ac.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("live_online_presence_card")
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = ac.emeraldText,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Pengguna Online Live",
                                style = MaterialTheme.typography.titleSmall,
                                color = ac.textPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Pantauan akun aktif & layar yang sedang diakses (${sortedPresence.size} tercatat)",
                                style = MaterialTheme.typography.bodySmall,
                                color = ac.textMuted
                            )
                        }
                    }
                    Surface(
                        color = ac.emeraldSurface,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, ac.emeraldBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LivePulseDot(color = Emerald500, size = 7.dp)
                            Text(
                                text = "$onlineCount Online",
                                style = MaterialTheme.typography.labelSmall,
                                color = ac.emeraldText,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                if (sortedPresence.isEmpty()) {
                    Surface(
                        color = ac.cardElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Belum ada sesi pengguna yang tercatat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ac.textMuted,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    sortedPresence.take(20).forEach { presence ->
                        val isOnline = presence.isEffectivelyOnline()
                        Surface(
                            color = if (isOnline) ac.emeraldSurface.copy(alpha = 0.55f) else ac.cardElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isOnline) ac.emeraldBorder else ac.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (isOnline) {
                                    LivePulseDot(color = Emerald500, size = 10.dp)
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(ac.textMuted.copy(alpha = 0.45f))
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = presence.name.ifBlank { presence.email.ifBlank { presence.userId } },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = ac.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (presence.role == "admin") {
                                            Surface(color = ac.purpleSurface, shape = CircleShape) {
                                                Text(
                                                    text = "ADMIN",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ac.purpleText,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${presence.house} • ${if (isOnline) "Sedang aktif" else "Offline"} • ${fmt.format(Date(presence.lastSeenMillis.coerceAtLeast(1L)))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ac.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    color = if (isOnline) ac.danaSurface else ac.subtleContainer,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = presence.activeScreen.ifBlank { "Beranda" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOnline) ac.danaText else ac.textSecondary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Live Activity Analytics Report Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
            border = BorderStroke(1.dp, ac.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("live_activity_report_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Analytics,
                        contentDescription = null,
                        tint = ac.danaText,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Analitik Live Report Aktivitas",
                            style = MaterialTheme.typography.titleSmall,
                            color = ac.textPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Ringkasan interaksi aplikasi, layar populer & audit event real-time",
                            style = MaterialTheme.typography.bodySmall,
                            color = ac.textMuted
                        )
                    }
                }

                // Interaction Summary KPI Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminCounterBox(
                        label = "Total Interaksi",
                        value = "${state.activityLogs.size}",
                        bg = ac.danaSurface,
                        fg = ac.danaText,
                        modifier = Modifier.weight(1f)
                    )
                    AdminCounterBox(
                        label = "Online Kini",
                        value = "$onlineCount",
                        bg = ac.emeraldSurface,
                        fg = ac.emeraldText,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = ac.purpleSurface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Layar Teratas",
                                style = MaterialTheme.typography.labelSmall,
                                color = ac.purpleText,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = topScreen,
                                style = MaterialTheme.typography.titleSmall,
                                color = ac.textPrimary,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Top Active Screens Proportion Bars
                if (screenCounts.isNotEmpty() && state.activityLogs.isNotEmpty()) {
                    val totalLogs = state.activityLogs.size.toFloat().coerceAtLeast(1f)
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
                            Text(
                                text = "Distribusi Akses Menu",
                                style = MaterialTheme.typography.labelMedium,
                                color = ac.textPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            screenCounts.take(4).forEach { (screenName, count) ->
                                val ratio = (count / totalLogs).coerceIn(0f, 1f)
                                val pct = (ratio * 100f).toInt()
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = screenName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ac.textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "$count ($pct%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ac.textSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(CircleShape),
                                        color = DanaBlue,
                                        trackColor = ac.divider
                                    )
                                }
                            }
                        }
                    }
                }

                if (actionCounts.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        actionCounts.entries.sortedByDescending { it.value }.forEach { (action, count) ->
                            Surface(
                                color = ac.danaSurface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ac.danaBorder)
                            ) {
                                Text(
                                    text = "$action: $count",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ac.danaText,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActivityLogFilter.entries.forEach { filterOption ->
                        val selected = selectedLogFilter == filterOption
                        FilterChip(
                            selected = selected,
                            onClick = { selectedLogFilter = filterOption },
                            label = {
                                Text(
                                    text = filterOption.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ac.danaSurface,
                                selectedLabelColor = ac.danaText,
                                containerColor = ac.cardElevated,
                                labelColor = ac.textSecondary
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("activity_filter_${filterOption.name.lowercase()}")
                        )
                    }
                }

                if (filteredLogs.isEmpty()) {
                    Surface(
                        color = ac.cardElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (state.activityLogs.isEmpty()) {
                                "Belum ada log aktivitas yang masuk."
                            } else {
                                "Tidak ada aktivitas pada kategori ${selectedLogFilter.label}."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ac.textMuted,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    val visibleLogs = filteredLogs.take(25)
                    visibleLogs.forEachIndexed { idx, log ->
                        val badgeColor = when (log.actionType) {
                            "PAYMENT_SUBMIT", "RECEIPT_SCAN" -> ac.emeraldSurface to ac.emeraldText
                            "ADMIN_ACTION", "SYSTEM_UPDATE" -> ac.purpleSurface to ac.purpleText
                            "FINANCE_UPDATE" -> ac.amberSurface to ac.amberText
                            else -> ac.danaSurface to ac.danaText
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(color = badgeColor.first, shape = CircleShape) {
                                        Text(
                                            text = log.actionType,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = badgeColor.second,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "${log.userName.ifBlank { log.userEmail }} (${log.house})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ac.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${log.screen} • ${log.detail}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ac.textSecondary
                                )
                            }
                            val tsText = log.createdAt?.toDate()?.let { fmt.format(it) } ?: "Baru saja"
                            Text(
                                text = tsText,
                                style = MaterialTheme.typography.labelSmall,
                                color = ac.textMuted
                            )
                        }
                        if (idx < visibleLogs.lastIndex) {
                            HorizontalDivider(color = ac.divider)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroQuickBadge(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color.Black.copy(alpha = 0.24f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Emerald100.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun AdminCounterBox(
    label: String,
    value: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    val ac = adaptiveColors
    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = ac.textPrimary,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun PendingPaymentNotifierCard(
    state: MainUiState,
    onToggleService: (Boolean) -> Unit,
    onTriggerNow: () -> Unit
) {
    val ac = adaptiveColors
    val currentMonth = remember { currentBillingPeriodCode() }
    val pendingPaymentsNow = remember(state.allPayments, currentMonth) {
        state.allPayments.filter {
            it.month == currentMonth && it.status.equals("pending", ignoreCase = true)
        }
    }
    val pendingResidentCount = remember(pendingPaymentsNow) {
        pendingPaymentsNow.map { it.userId }.distinct().size
    }
    val pendingTotalNominal = remember(pendingPaymentsNow) {
        pendingPaymentsNow.sumOf { it.amount }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
        border = BorderStroke(1.dp, ac.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_payment_notifier_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = ac.amberText,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Layanan Notifikasi Otomatis Pending",
                        style = MaterialTheme.typography.titleSmall,
                        color = ac.textPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Pengingat otomatis WorkManager (tiap 12 jam) untuk warga berstatus pending bulan ${monthLabel(currentMonth)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ac.textMuted
                    )
                }
            }

            Surface(
                color = ac.cardElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ac.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Status Pending Bulan Ini", style = MaterialTheme.typography.bodySmall, color = ac.textMuted)
                        Text(
                            text = "$pendingResidentCount warga (${formatRupiah(pendingTotalNominal)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = ac.amberText,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Terakhir Diperiksa", style = MaterialTheme.typography.bodySmall, color = ac.textMuted)
                        Text(
                            text = PendingPaymentNotificationScheduler.formatLastCheck(
                                state.pendingNotifierStatus.lastCheckAtMillis
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = ac.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Surface(
                color = ac.cardElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ac.borderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pengingat Otomatis Berkala (12 Jam)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ac.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.pendingNotifierStatus.enabled) "Layanan aktif di latar belakang" else "Layanan dijeda",
                            style = MaterialTheme.typography.labelSmall,
                            color = ac.textMuted
                        )
                    }
                    Switch(
                        checked = state.pendingNotifierStatus.enabled,
                        onCheckedChange = onToggleService,
                        colors = SwitchDefaults.colors(checkedTrackColor = Emerald700),
                        modifier = Modifier.testTag("pending_notifier_switch")
                    )
                }
            }

            Button(
                onClick = onTriggerNow,
                enabled = !state.isMutating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DanaBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("trigger_pending_notif_button")
            ) {
                Icon(Icons.Filled.NotificationsActive, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kirim Push Notif Warga Pending Sekarang",
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun AdminTransactionHistoryCard(state: MainUiState) {
    val ac = adaptiveColors
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf(TransactionTypeFilter.ALL) }
    var dateFilter by remember { mutableStateOf(TransactionDateFilter.ALL) }
    var categoryFilter by remember { mutableStateOf("Semua") }
    var showAll by remember { mutableStateOf(false) }
    var dateMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val allTransactions = remember(state.allPayments, state.financeEntries, state.allResidents) {
        buildUnifiedAdminTransactions(
            payments = state.allPayments,
            financeEntries = state.financeEntries,
            residents = state.allResidents
        )
    }
    val categories = remember(allTransactions) {
        extractTransactionCategories(allTransactions)
    }
    val categoryCounts = remember(allTransactions) {
        allTransactions.groupingBy { it.category.trim().lowercase() }.eachCount()
    }
    val filteredTransactions = remember(
        allTransactions,
        query,
        typeFilter,
        categoryFilter,
        dateFilter,
        state.selectedMonth
    ) {
        filterAdminTransactions(
            items = allTransactions,
            query = query,
            typeFilter = typeFilter,
            categoryFilter = categoryFilter,
            dateFilter = dateFilter,
            selectedMonth = state.selectedMonth
        )
    }
    val hasActiveFilter = query.isNotBlank() ||
        typeFilter != TransactionTypeFilter.ALL ||
        dateFilter != TransactionDateFilter.ALL ||
        categoryFilter != "Semua"

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
        border = BorderStroke(1.dp, ac.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .testTag("admin_transaction_history_card")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = ac.emeraldText,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Riwayat Transaksi & Pengeluaran",
                            style = MaterialTheme.typography.titleSmall,
                            color = ac.textPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Cari & saring transaksi berdasarkan jenis, periode, atau kategori",
                            style = MaterialTheme.typography.bodySmall,
                            color = ac.textMuted
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AnimatedVisibility(
                        visible = hasActiveFilter,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            color = ac.subtleContainer,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, ac.borderSubtle),
                            modifier = Modifier
                                .heightIn(min = 32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    query = ""
                                    typeFilter = TransactionTypeFilter.ALL
                                    dateFilter = TransactionDateFilter.ALL
                                    categoryFilter = "Semua"
                                }
                                .testTag("admin_tx_reset_filters")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Reset filter",
                                    tint = ac.textSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Reset",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ac.textSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Surface(
                        color = ac.emeraldSurface,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${filteredTransactions.size} item",
                            style = MaterialTheme.typography.labelSmall,
                            color = ac.emeraldText,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Search Input Bar
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Cari nama warga, kategori, tanggal...") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = ac.textMuted)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { query = "" },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Clear, contentDescription = "Reset pencarian", tint = ac.textMuted)
                        }
                    }
                },
                singleLine = true,
                colors = adaptiveFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_tx_search_input")
            )

            // 1. Segmented Pill Rail for Transaction Type / Status
            Surface(
                color = ac.subtleContainer,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ac.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TransactionTypeFilter.entries.forEach { option ->
                        val selected = typeFilter == option
                        val pillContainer by animateColorAsState(
                            targetValue = if (selected) Emerald700 else Color.Transparent,
                            label = "tx_type_bg"
                        )
                        val pillTextColor by animateColorAsState(
                            targetValue = if (selected) Color.White else ac.textSecondary,
                            label = "tx_type_fg"
                        )
                        Surface(
                            color = pillContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { typeFilter = option }
                                .testTag("admin_tx_type_${option.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = pillTextColor,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 2. Minimalist Dual Dropdown Selectors (Periode & Kategori)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateActive = dateFilter != TransactionDateFilter.ALL
                val currentDateLabel = when (dateFilter) {
                    TransactionDateFilter.ALL -> "Semua Periode"
                    TransactionDateFilter.SELECTED_MONTH -> monthLabel(state.selectedMonth)
                    else -> dateFilter.label
                }

                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = if (dateActive) ac.emeraldSurface else ac.cardElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (dateActive) ac.emeraldBorder else ac.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { dateMenuExpanded = true }
                            .testTag("admin_tx_date_dropdown")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = if (dateActive) ac.emeraldText else ac.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = currentDateLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (dateActive) ac.emeraldText else ac.textPrimary,
                                fontWeight = if (dateActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Pilih periode",
                                tint = if (dateActive) ac.emeraldText else ac.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = dateMenuExpanded,
                        onDismissRequest = { dateMenuExpanded = false },
                        containerColor = ac.cardContainer
                    ) {
                        TransactionDateFilter.entries.forEach { option ->
                            val selected = dateFilter == option
                            val optionLabel = if (option == TransactionDateFilter.SELECTED_MONTH) {
                                monthLabel(state.selectedMonth)
                            } else {
                                option.label
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = optionLabel,
                                        color = if (selected) ac.emeraldText else ac.textPrimary,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                                    )
                                },
                                trailingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = ac.emeraldText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                onClick = {
                                    dateFilter = option
                                    dateMenuExpanded = false
                                },
                                modifier = Modifier.testTag("admin_tx_date_${option.name.lowercase()}")
                            )
                        }
                    }
                }

                val categoryActive = categoryFilter != "Semua"
                val categoryButtonLabel = if (categoryActive) categoryFilter else "Semua Kategori"

                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = if (categoryActive) ac.emeraldSurface else ac.cardElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (categoryActive) ac.emeraldBorder else ac.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = categories.size > 1) { categoryMenuExpanded = true }
                            .testTag("admin_tx_category_dropdown")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Category,
                                contentDescription = null,
                                tint = if (categoryActive) ac.emeraldText else ac.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = categoryButtonLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (categoryActive) ac.emeraldText else ac.textPrimary,
                                fontWeight = if (categoryActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Pilih kategori",
                                tint = if (categoryActive) ac.emeraldText else ac.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                        containerColor = ac.cardContainer
                    ) {
                        categories.forEach { cat ->
                            val selected = categoryFilter == cat
                            val count = if (cat == "Semua") {
                                allTransactions.size
                            } else {
                                categoryCounts[cat.trim().lowercase()] ?: 0
                            }
                            val display = if (cat == "Semua") "Semua Kategori ($count)" else "$cat ($count)"
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = display,
                                        color = if (selected) ac.emeraldText else ac.textPrimary,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                                    )
                                },
                                trailingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = ac.emeraldText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                onClick = {
                                    categoryFilter = cat
                                    categoryMenuExpanded = false
                                },
                                modifier = Modifier.testTag("admin_tx_cat_${cat.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = ac.divider)

            if (filteredTransactions.isEmpty()) {
                Surface(
                    color = ac.cardElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tidak ada transaksi yang cocok dengan filter atau kata kunci saat ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ac.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                val visible = if (showAll) filteredTransactions else filteredTransactions.take(15)
                visible.forEachIndexed { idx, item ->
                    val isOutgoing = item.direction == TransactionDirection.OUTGOING
                    val badgeColors = when {
                        isOutgoing -> ac.roseSurface to ac.roseText
                        item.status == "pending" -> ac.amberSurface to ac.amberText
                        else -> ac.emeraldSurface to ac.emeraldText
                    }
                    val statusLabel = when {
                        isOutgoing -> "KELUAR"
                        item.status == "pending" -> "PENDING"
                        item.status == "paid" -> "LUNAS"
                        else -> "MASUK"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = badgeColors.first,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isOutgoing) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = badgeColors.second,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(color = badgeColors.first, shape = CircleShape) {
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = badgeColors.second,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ac.danaText,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${item.residentName} (${item.house})",
                                style = MaterialTheme.typography.labelLarge,
                                color = ac.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.title} • ${item.dateLabel} • ${item.methodOrKind}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ac.textMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = (if (isOutgoing) "- " else "+ ") + formatRupiah(item.amount),
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = badgeColors.second,
                            fontWeight = FontWeight.Black
                        )
                    }
                    if (idx < visible.lastIndex) {
                        HorizontalDivider(color = ac.divider)
                    }
                }

                if (filteredTransactions.size > 15) {
                    TextButton(
                        onClick = { showAll = !showAll },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("admin_tx_toggle_show_all")
                    ) {
                        Text(
                            text = if (showAll) {
                                "Tampilkan Lebih Sedikit"
                            } else {
                                "Tampilkan Semua (${filteredTransactions.size} Transaksi)"
                            },
                            color = ac.danaText,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxIconText(label: String, active: Boolean) {
    val ac = adaptiveColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) Color.White else ac.textSecondary,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
