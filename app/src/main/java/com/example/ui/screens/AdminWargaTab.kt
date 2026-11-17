package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.DuesStatus
import com.example.data.model.Resident
import com.example.data.model.SystemSettings
import com.example.data.model.computeMonthSummary
import com.example.data.model.formatRupiah
import com.example.data.model.monthLabel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber600
import com.example.ui.theme.DanaBlue
import com.example.ui.theme.DanaLight
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Rose600
import com.example.ui.theme.Rose50
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.AdminFilter
import com.example.ui.viewmodel.MainUiState
import com.example.worker.MonthlyBillingScheduler

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
    if (state.currentResident?.adminRole != true) return

    val context = LocalContext.current
    var pendingDeleteResident by remember { mutableStateOf<Resident?>(null) }
    var editingResident by remember { mutableStateOf<Resident?>(null) }
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

    val members = state.allResidents.filter { !it.adminRole }
    val query = state.adminSearchQuery.trim().lowercase()
    val filtered = members.filter { u ->
        val matchesQuery = query.isBlank() ||
            u.name.lowercase().contains(query) ||
            u.house.lowercase().contains(query) ||
            u.phone.contains(query)
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

    val totalPaid = members.count { u ->
        val pays = state.allPayments.filter { it.userId == u.id }
        computeMonthSummary(u, pays, state.selectedMonth).iuranStatus == DuesStatus.PAID
    }
    val pendingPays = state.allPayments.filter { it.status == "pending" && !it.isAutoGeneratedBill }
    val unpaidBillCount = state.allPayments.count { it.month == state.selectedMonth && it.isAutoGeneratedBill }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Rekapitulasi Dana
        FundRecapCard(
            state = state,
            onExportRecapCsv = {
                recapCsvExporter.launch("Rekap-Dana-${state.selectedMonth}.csv")
            }
        )

        // 2. DANA Live Automatic Notification Verification
        DanaLiveIntegrationCard(
            state = state,
            onToggleAutoVerify = onToggleDanaAutoVerify,
            onVerifyLiveDanaManual = onVerifyLiveDanaManual,
            onClearEvents = onClearDanaLiveEvents,
            onShowMessage = onShowMessage
        )

        // 2b. DANA Real-Time Webhook Integration (Node.js / Google Apps Script)
        DanaWebhookCard(
            state = state,
            onSaveWebhookConfig = onSaveWebhookConfig,
            onTestWebhookPing = onTestWebhookPing,
            onVerifyWebhookEventManual = onVerifyWebhookEventManual,
            onDeleteWebhookEvent = onDeleteWebhookEvent,
            onShowMessage = onShowMessage
        )

        // 3. Overview, Filter & Export CSV Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200)
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
                    Text(
                        text = "Data Warga",
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate900
                    )
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
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CSV", fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminCounterBox("Total", members.size.toString(), Emerald50, Emerald700, Modifier.weight(1f))
                    AdminCounterBox("Lunas", totalPaid.toString(), DanaLight, DanaBlue, Modifier.weight(1f))
                    AdminCounterBox("Pending", pendingPays.size.toString(), Amber50, Amber600, Modifier.weight(1f))
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
                            color = if (active) Emerald600 else Slate100,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
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
                            tint = Slate400
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_search_input")
                )
            }
        }

        // 4. Pending Verification Queue
        if (pendingPays.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Amber50),
                border = BorderStroke(1.dp, Amber100)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Perlu Verifikasi (${pendingPays.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = Amber600,
                        fontWeight = FontWeight.ExtraBold
                    )
                    pendingPays.forEach { pay ->
                        val owner = state.allResidents.firstOrNull { it.id == pay.userId }
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Amber100),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = owner?.name ?: "Warga (${pay.userId.take(6)})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Slate900
                                )
                                Text(
                                    text = "${pay.type.uppercase()} • ${monthLabel(pay.month)} • ${formatRupiah(pay.amount)} • ${pay.note}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onRejectPayment(pay.id) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose600),
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
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
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

        // 5. Resident List with Edit & Delete
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200)
        ) {
            if (filtered.isEmpty()) {
                Text(
                    text = "Tidak ada data warga sesuai filter",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400,
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
                                    Text(
                                        text = u.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "${u.house}${if (u.phone.isNotBlank()) " • ${u.phone}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        color = Emerald50,
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
                                                tint = Emerald700,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        color = Rose50,
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
                                                contentDescription = "Hapus warga ${u.name}",
                                                tint = Rose600,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = Slate50,
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
                                            color = Slate600
                                        )
                                        StatusBadge(status = sum.iuranStatus)
                                    }
                                }
                                Surface(
                                    color = Slate50,
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
                                            color = Slate600
                                        )
                                        StatusBadge(status = sum.hidmatStatus)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (sum.iuranStatus == DuesStatus.UNPAID) {
                                    Button(
                                        onClick = { onMarkPaid(u.id, "iuran") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
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
                                if (sum.hidmatActive && sum.hidmatStatus == DuesStatus.UNPAID) {
                                    Button(
                                        onClick = { onMarkPaid(u.id, "hidmat") },
                                        colors = ButtonDefaults.buttonColors(containerColor = DanaBlue),
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
                                ) {
                                    Text(
                                        text = if (u.hidmatEnabled) "Matikan Hidmat" else "Aktifkan Hidmat",
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        if (idx < filtered.lastIndex) {
                            HorizontalDivider(color = Slate100)
                        }
                    }
                }
            }
        }

        // 6. Kelola Role Admin (Angkat Warga / Turunkan Admin; Admin Utama Tetap)
        AdminRoleManagementCard(
            state = state,
            onOpenPromoteDialog = { showPromoteDialog = true },
            onDemoteAdmin = { uid ->
                onSetResidentRole(uid, false, "Warga / Member")
            },
            onEditAdminProfile = { adm -> editingResident = adm }
        )

        // 7. Sistem Admin Blok
        AdminSystemToolsCard(
            settings = state.settings,
            onOpenEditSettings = { showSystemSettingsDialog = true },
            onOpenAnnouncements = onNavigateToInfo
        )

        // 8. Push Reminder Blast Control
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Push Notif Himbauan",
                    style = MaterialTheme.typography.titleSmall,
                    color = Slate900
                )
                Button(
                    onClick = onPushBlast,
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600),
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
                    color = Slate50,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Slate200)
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
                            color = Slate900
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

        // 9. Automated 1st-of-Month Billing Worker Control Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200)
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
                        tint = Emerald700,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Tagihan Otomatis Tanggal 1",
                        style = MaterialTheme.typography.titleSmall,
                        color = Slate900
                    )
                }
                Text(
                    text = "Background service WorkManager berjalan otomatis setiap tanggal 1 pukul 00:05 untuk membuat tagihan unpaid Iuran & Hidmat bagi seluruh warga aktif (periode 2026 s.d. 2050).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
                Surface(
                    color = Slate50,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Slate200),
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
                            Text("Jadwal Berikutnya", style = MaterialTheme.typography.bodySmall, color = Slate500)
                            Text(
                                text = MonthlyBillingScheduler.formatTimestamp(state.billingWorkerStatus.nextRunAtMillis),
                                style = MaterialTheme.typography.bodySmall,
                                color = Emerald700,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Terakhir Dijalankan", style = MaterialTheme.typography.bodySmall, color = Slate500)
                            val lastLabel = if (state.billingWorkerStatus.lastRunAtMillis > 0L) {
                                "${monthLabel(state.billingWorkerStatus.lastRunPeriod)} (${state.billingWorkerStatus.lastGeneratedCount} tagihan)"
                            } else {
                                "Menunggu tanggal 1"
                            }
                            Text(
                                text = lastLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate900,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tagihan Unpaid (${monthLabel(state.selectedMonth)})", style = MaterialTheme.typography.bodySmall, color = Slate500)
                            Text(
                                text = "$unpaidBillCount record",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate900,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Button(
                    onClick = onGenerateMonthlyBills,
                    enabled = !state.isMutating,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("generate_monthly_bills_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Buat Tagihan ${monthLabel(state.selectedMonth)} Sekarang",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }

    val toEdit = editingResident
    if (toEdit != null) {
        EditResidentDialog(
            resident = toEdit,
            currentAdminUid = state.currentResident.id,
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
            isMutating = state.isMutating,
            onPromote = { uid, roleLabel ->
                onSetResidentRole(uid, true, roleLabel)
                showPromoteDialog = false
            },
            onDismiss = { showPromoteDialog = false }
        )
    }

    if (showSystemSettingsDialog) {
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

    val toDelete = pendingDeleteResident
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteResident = null },
            title = { Text("Hapus Warga?") },
            text = {
                Text("${toDelete.name} (${toDelete.house}) akan dihapus dari daftar warga.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteResident(toDelete.id)
                        pendingDeleteResident = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600)
                ) {
                    Text("Ya, Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteResident = null }) {
                    Text("Batal")
                }
            }
        )
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
                color = fg
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = Slate900,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun BoxIconText(label: String, active: Boolean) {
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
            color = if (active) Color.White else Slate600,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
