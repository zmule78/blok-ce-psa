package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dana.DanaLiveBridge
import com.example.data.local.DanaLiveEventEntity
import com.example.data.model.AUTO_BILL_METHOD
import com.example.data.model.PRIMARY_ADMIN_EMAIL
import com.example.data.model.Payment
import com.example.data.model.Resident
import com.example.data.model.SystemSettings
import com.example.data.model.computeFundRecap
import com.example.data.model.formatRupiah
import com.example.data.model.monthLabel
import com.example.ui.components.uriToCompressedDataUri
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber600
import com.example.ui.theme.DanaBlue
import com.example.ui.theme.DanaDark
import com.example.ui.theme.DanaLight
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Purple100
import com.example.ui.theme.Purple700
import com.example.ui.theme.Rose100
import com.example.ui.theme.Rose50
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.MainUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FundRecapCard(
    state: MainUiState,
    onExportRecapCsv: () -> Unit
) {
    val recap = remember(state.selectedMonth, state.allResidents, state.allPayments, state.settings) {
        computeFundRecap(state.selectedMonth, state.allResidents, state.allPayments, state.settings)
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rekap_dana_card")
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Emerald700,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Rekapitulasi Dana",
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate900,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = monthLabel(state.selectedMonth),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                }
                OutlinedButton(
                    onClick = onExportRecapCsv,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("export_recap_csv_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ekspor", fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(Emerald900, Emerald700)),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "TOTAL DANA MASUK (LUNAS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Emerald100
                    )
                    Text(
                        text = formatRupiah(recap.totalCollectedMonth),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                    LinearProgressIndicator(
                        progress = { (recap.collectionPercent / 100f).coerceIn(0f, 1f) },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "Realisasi ${recap.collectionPercent}% dari target ${formatRupiah(recap.expectedMonthTotal)} (${recap.totalResidentsCount} warga)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Emerald100
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecapMetricPill(
                    label = "Iuran Lunas",
                    value = formatRupiah(recap.paidIuranAmount),
                    bg = Emerald50,
                    fg = Emerald700,
                    modifier = Modifier.weight(1f)
                )
                RecapMetricPill(
                    label = "Hidmat Lunas",
                    value = formatRupiah(recap.paidHidmatAmount),
                    bg = DanaLight,
                    fg = DanaDark,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecapMetricPill(
                    label = "Pending Verif",
                    value = formatRupiah(recap.pendingVerificationAmount),
                    bg = Amber50,
                    fg = Amber600,
                    modifier = Modifier.weight(1f)
                )
                RecapMetricPill(
                    label = "Sisa Tagihan",
                    value = formatRupiah(recap.unpaidOutstandingAmount),
                    bg = Rose50,
                    fg = Rose600,
                    modifier = Modifier.weight(1f)
                )
            }

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
                        Text(
                            text = "Akumulasi Tahun ${state.selectedMonth.take(4)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                        Text(
                            text = formatRupiah(recap.yearTotalCollected),
                            style = MaterialTheme.typography.bodySmall,
                            color = Emerald700,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Kas Masuk Keseluruhan",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                        Text(
                            text = formatRupiah(recap.allTimeCollected),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate900,
                            fontWeight = FontWeight.Black
                        )
                    }
                    if (recap.methodBreakdowns.isNotEmpty()) {
                        HorizontalDivider(color = Slate200)
                        Text(
                            text = "Rincian Metode Pembayaran (${monthLabel(state.selectedMonth)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate600,
                            fontWeight = FontWeight.Bold
                        )
                        recap.methodBreakdowns.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.method} (${item.count}x)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                                Text(
                                    text = formatRupiah(item.totalAmount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate900,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecapMetricPill(
    label: String,
    value: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = Slate900,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun AdminRoleManagementCard(
    state: MainUiState,
    onOpenPromoteDialog: () -> Unit,
    onDemoteAdmin: (targetUid: String) -> Unit,
    onEditAdminProfile: (Resident) -> Unit
) {
    val currentUid = state.currentResident?.id.orEmpty()
    val delegatedResidentMap = remember(state.allResidents) {
        state.allResidents.associateBy { it.id }
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

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_role_management_card")
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
                    imageVector = Icons.Filled.AdminPanelSettings,
                    contentDescription = null,
                    tint = Emerald700,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Kelola Role & Akses Admin",
                    style = MaterialTheme.typography.titleSmall,
                    color = Slate900,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = "Admin dapat mengangkat warga menjadi Admin (role: admin) dan menurunkannya kembali menjadi Warga. Admin Utama bersifat permanen dan tidak bisa diturunkan.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
            )

            // Permanent Primary Admin Card
            Surface(
                color = Emerald50,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Emerald100),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ADMIN UTAMA & TETAP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Emerald700,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = PRIMARY_ADMIN_EMAIL,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate900,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = Emerald700,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Admin permanen terkunci",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "TETAP",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onOpenPromoteDialog,
                enabled = !state.isMutating,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("open_promote_admin_dialog")
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pilih Warga Jadi Admin", fontWeight = FontWeight.ExtraBold)
            }

            if (delegatedResidents.isNotEmpty() || extraAdminRecords.isNotEmpty()) {
                HorizontalDivider(color = Slate100)
                Text(
                    text = "Daftar Pengurus (role: admin)",
                    style = MaterialTheme.typography.labelLarge,
                    color = Slate900
                )
                delegatedResidents.forEach { adm ->
                    Surface(
                        color = Slate50,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = adm.name.ifBlank { adm.email },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Slate900,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${adm.roleLabel} • ${adm.house}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Emerald700
                                    )
                                    Text(
                                        text = adm.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    color = Purple100,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Purple700,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onEditAdminProfile(adm) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("edit_admin_resident_${adm.id}")
                                ) {
                                    Icon(Icons.Filled.Edit, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Edit Profil", style = MaterialTheme.typography.labelMedium)
                                }
                                if (adm.id != currentUid) {
                                    OutlinedButton(
                                        onClick = { onDemoteAdmin(adm.id) },
                                        enabled = !state.isMutating,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose600),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("demote_admin_${adm.id}")
                                    ) {
                                        Icon(Icons.Filled.PersonRemove, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Turunkan jadi Warga", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
                extraAdminRecords.forEach { rec ->
                    val linkedRes = delegatedResidentMap[rec.userId]
                    Surface(
                        color = Slate50,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = linkedRes?.name ?: rec.email,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Slate900
                                )
                                Text(
                                    text = "${rec.roleLabel} • ${rec.email}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500
                                )
                            }
                            if (rec.userId != currentUid) {
                                TextButton(
                                    onClick = { onDemoteAdmin(rec.userId) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("demote_admin_${rec.userId}")
                                ) {
                                    Text("Turunkan", color = Rose600, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Belum ada pengurus tambahan selain Admin Utama.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
            }
        }
    }
}

@Composable
fun DanaLiveIntegrationCard(
    state: MainUiState,
    onToggleAutoVerify: (Boolean) -> Unit,
    onVerifyLiveDanaManual: (eventId: Long, paymentId: String, senderHint: String) -> Unit,
    onClearEvents: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var matchingEvent by remember { mutableStateOf<DanaLiveEventEntity?>(null) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dana_live_card")
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
                        imageVector = Icons.Filled.Sync,
                        contentDescription = null,
                        tint = DanaBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Verifikasi DANA Live",
                        style = MaterialTheme.typography.titleSmall,
                        color = Slate900,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Surface(
                    color = if (state.danaListenerEnabled) Emerald50 else Amber50,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (state.danaListenerEnabled) Emerald100 else Amber100)
                ) {
                    Text(
                        text = if (state.danaListenerEnabled) "LIVE AKTIF" else "PERLU IZIN",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.danaListenerEnabled) Emerald700 else Amber600,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Terhubung otomatis ke notifikasi resmi aplikasi DANA (id.dana) di perangkat Admin. Setiap saldo masuk yang cocok nominal/nama pengirim otomatis memverifikasi tagihan warga menjadi LUNAS secara live.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
            )

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Verifikasi Pembayaran Live",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate900,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Otomatis ubah status ke Lunas saat notif DANA masuk",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                    Switch(
                        checked = state.danaAutoVerifyEnabled,
                        onCheckedChange = onToggleAutoVerify,
                        colors = SwitchDefaults.colors(checkedTrackColor = DanaBlue),
                        modifier = Modifier.testTag("dana_auto_verify_switch")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { DanaLiveBridge.openNotificationListenerSettings(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.danaListenerEnabled) Slate900 else DanaBlue
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("dana_listener_settings_button")
                ) {
                    Icon(Icons.Filled.NotificationsActive, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.danaListenerEnabled) "Pengaturan Notif" else "Izinkan Akses DANA",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = {
                        val opened = DanaLiveBridge.openDanaApp(context)
                        if (!opened) {
                            onShowMessage("Membuka Play Store untuk menginstal aplikasi DANA.")
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("admin_open_dana_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buka DANA", fontWeight = FontWeight.Bold)
                }
            }

            if (state.danaLiveEvents.isNotEmpty()) {
                HorizontalDivider(color = Slate100)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mutasi Notifikasi DANA (${state.danaLiveEvents.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = Slate900
                    )
                    TextButton(
                        onClick = onClearEvents,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.ClearAll, null, modifier = Modifier.size(16.dp), tint = Slate500)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bersihkan", color = Slate500)
                    }
                }
                val fmt = remember { SimpleDateFormat("dd/MM HH:mm", Locale("id", "ID")) }
                state.danaLiveEvents.take(5).forEach { ev ->
                    val statusColor = when (ev.status) {
                        "AUTO_VERIFIED" -> Emerald700
                        "NEED_REVIEW" -> Amber600
                        "ERROR" -> Rose600
                        else -> Slate600
                    }
                    Surface(
                        color = Slate50,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatRupiah(ev.parsedAmount),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DanaDark,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${ev.status} • ${fmt.format(Date(ev.receivedAtMillis))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (ev.senderHint.isNotBlank()) {
                                Text(
                                    text = "Pengirim: ${ev.senderHint}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate900,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = ev.matchedSummary.ifBlank { ev.content },
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (ev.status == "NEED_REVIEW" || ev.status == "UNMATCHED") {
                                OutlinedButton(
                                    onClick = { matchingEvent = ev },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("match_dana_event_${ev.id}")
                                ) {
                                    Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cocokkan Tagihan Manual")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val evToMatch = matchingEvent
    if (evToMatch != null) {
        MatchDanaEventDialog(
            event = evToMatch,
            state = state,
            onMatchPayment = { pay ->
                onVerifyLiveDanaManual(evToMatch.id, pay.id, evToMatch.senderHint)
                matchingEvent = null
            },
            onDismiss = { matchingEvent = null }
        )
    }
}

@Composable
fun AdminSystemToolsCard(
    settings: SystemSettings,
    onOpenEditSettings: () -> Unit,
    onOpenAnnouncements: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_system_tools_card")
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
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = Emerald700,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Sistem Admin Blok",
                    style = MaterialTheme.typography.titleSmall,
                    color = Slate900,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Surface(
                color = Slate50,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${settings.blokName} • DANA ${settings.danaNumber}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Slate900
                    )
                    Text(
                        text = "Tarif Iuran ${formatRupiah(settings.iuranAmount)} • Tarif Hidmat ${formatRupiah(settings.hidmatAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenEditSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("open_admin_system_settings_dialog")
                ) {
                    Icon(Icons.Filled.Settings, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Sistem", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onOpenAnnouncements,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("open_admin_announcements_button")
                ) {
                    Icon(Icons.Filled.Newspaper, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kelola Info", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun EditResidentDialog(
    resident: Resident,
    currentAdminUid: String,
    isMutating: Boolean,
    onSave: (
        username: String,
        name: String,
        house: String,
        phone: String,
        hidmatEnabled: Boolean,
        makeAdmin: Boolean,
        roleLabel: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember(resident.id) { mutableStateOf(resident.username) }
    var name by remember(resident.id) { mutableStateOf(resident.name) }
    var house by remember(resident.id) { mutableStateOf(resident.house) }
    var phone by remember(resident.id) { mutableStateOf(resident.phone) }
    var hidmatEnabled by remember(resident.id) { mutableStateOf(resident.hidmatEnabled) }
    var makeAdmin by remember(resident.id) { mutableStateOf(resident.adminRole) }
    var roleLabel by remember(resident.id) {
        mutableStateOf(
            resident.roleLabel.takeIf { it.isNotBlank() && it != "Warga / Member" }
                ?: "Admin / Pengurus Blok"
        )
    }

    val isLockedPrimary = resident.isPermanentPrimaryEmail
    val isSelf = resident.id == currentAdminUid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profil Warga") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = resident.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_resident_name_input")
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_resident_username_input")
                )
                OutlinedTextField(
                    value = house,
                    onValueChange = { house = it },
                    label = { Text("No. Rumah") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_resident_house_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. HP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_resident_phone_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Peserta Hidmat", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = hidmatEnabled,
                        onCheckedChange = { hidmatEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = DanaBlue)
                    )
                }
                if (isLockedPrimary) {
                    Surface(
                        color = Emerald50,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Emerald100),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Admin Utama (${PRIMARY_ADMIN_EMAIL}) bersifat tetap dan role tidak dapat diubah.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Emerald700,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Role Admin ('role: admin')", style = MaterialTheme.typography.bodyMedium)
                            if (isSelf) {
                                Text(
                                    text = "Tidak bisa menurunkan akun sendiri",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                        }
                        Switch(
                            checked = makeAdmin,
                            enabled = !isSelf,
                            onCheckedChange = { makeAdmin = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Emerald700),
                            modifier = Modifier.testTag("edit_resident_role_switch")
                        )
                    }
                    if (makeAdmin) {
                        OutlinedTextField(
                            value = roleLabel,
                            onValueChange = { roleLabel = it },
                            label = { Text("Jabatan / Label Admin") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(username, name, house, phone, hidmatEnabled, makeAdmin, roleLabel)
                },
                enabled = !isMutating && username.trim().length >= 3 && name.isNotBlank() && house.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                modifier = Modifier.testTag("save_edit_resident_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun PromoteResidentDialog(
    residents: List<Resident>,
    isMutating: Boolean,
    onPromote: (targetUid: String, roleLabel: String) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var roleLabel by remember { mutableStateOf("Admin / Pengurus Blok") }
    val candidates = remember(residents, search) {
        val q = search.trim().lowercase()
        residents.filter { !it.adminRole && !it.isPermanentPrimaryEmail }.filter {
            q.isBlank() || it.name.lowercase().contains(q) ||
                it.house.lowercase().contains(q) || it.email.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Warga Jadi Admin") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = roleLabel,
                    onValueChange = { roleLabel = it },
                    label = { Text("Label Jabatan Admin") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Cari nama / rumah / email...") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (candidates.isEmpty()) {
                        Text(
                            text = "Tidak ada warga yang memenuhi filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    } else {
                        candidates.forEach { candidate ->
                            Surface(
                                color = Slate50,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isMutating) {
                                        onPromote(candidate.id, roleLabel)
                                    }
                                    .testTag("promote_candidate_${candidate.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = candidate.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "${candidate.house} • ${candidate.email}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate500,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "Angkat",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Emerald700,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun EditSystemSettingsDialog(
    settings: SystemSettings,
    isMutating: Boolean,
    onSave: (SystemSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var blokName by remember(settings) { mutableStateOf(settings.blokName) }
    var subtitle by remember(settings) { mutableStateOf(settings.subtitle) }
    var danaNumber by remember(settings) { mutableStateOf(settings.danaNumber) }
    var danaOwner by remember(settings) { mutableStateOf(settings.danaAccountName) }
    var iuranAmount by remember(settings) { mutableStateOf(settings.iuranAmount.toString()) }
    var hidmatAmount by remember(settings) { mutableStateOf(settings.hidmatAmount.toString()) }
    var customQris by remember(settings) { mutableStateOf(settings.customQrisData) }

    val qrisPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = uriToCompressedDataUri(context, uri, maxDim = 512)
            if (encoded != null) customQris = encoded
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengaturan Sistem Blok") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = blokName,
                    onValueChange = { blokName = it },
                    label = { Text("Nama Blok") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subjudul") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = danaNumber,
                    onValueChange = { danaNumber = it },
                    label = { Text("Nomor DANA Resmi") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = danaOwner,
                    onValueChange = { danaOwner = it },
                    label = { Text("Atas Nama DANA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = iuranAmount,
                    onValueChange = { iuranAmount = it.filter { c -> c.isDigit() } },
                    label = { Text("Tarif Iuran (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hidmatAmount,
                    onValueChange = { hidmatAmount = it.filter { c -> c.isDigit() } },
                    label = { Text("Tarif Hidmat (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            qrisPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DanaBlue),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.QrCode2, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (customQris.isBlank()) "Pilih QRIS" else "Ganti QRIS")
                    }
                    if (customQris.isNotBlank()) {
                        OutlinedButton(
                            onClick = { customQris = "" },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Reset")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        settings.copy(
                            blokName = blokName,
                            subtitle = subtitle,
                            danaNumber = danaNumber,
                            danaAccountName = danaOwner,
                            iuranAmount = iuranAmount.toLongOrNull() ?: settings.iuranAmount,
                            hidmatAmount = hidmatAmount.toLongOrNull() ?: settings.hidmatAmount,
                            customQrisData = customQris
                        )
                    )
                },
                enabled = !isMutating,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                modifier = Modifier.testTag("save_system_dialog_button")
            ) {
                Text("Simpan Sistem")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun MatchDanaEventDialog(
    event: DanaLiveEventEntity,
    state: MainUiState,
    onMatchPayment: (Payment) -> Unit,
    onDismiss: () -> Unit
) {
    val residentById = remember(state.allResidents) { state.allResidents.associateBy { it.id } }
    val openPayments = remember(state.allPayments, event.parsedAmount) {
        state.allPayments
            .filter { it.status != "paid" }
            .sortedWith(
                compareByDescending<Payment> { it.amount == event.parsedAmount }
                    .thenByDescending { it.status == "pending" && it.method != AUTO_BILL_METHOD }
                    .thenByDescending { it.month }
            )
            .take(15)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cocokkan Mutasi ${formatRupiah(event.parsedAmount)}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (event.senderHint.isNotBlank()) {
                    Text(
                        text = "Pengirim terdeteksi: ${event.senderHint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DanaDark,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (openPayments.isEmpty()) {
                    Text(
                        text = "Tidak ada tagihan terbuka (unpaid/pending) yang tersedia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                } else {
                    openPayments.forEach { pay ->
                        val owner = residentById[pay.userId]
                        Surface(
                            color = if (pay.amount == event.parsedAmount) Emerald50 else Slate50,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (pay.amount == event.parsedAmount) Emerald600 else Slate200
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMatchPayment(pay) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${owner?.name ?: pay.userId} (${owner?.house ?: "-"})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Slate900
                                )
                                Text(
                                    text = "${pay.type.uppercase()} • ${monthLabel(pay.month)} • ${formatRupiah(pay.amount)} • ${pay.status}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
