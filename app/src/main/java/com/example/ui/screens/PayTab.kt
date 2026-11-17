package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.DuesStatus
import com.example.data.model.PaymentPackage
import com.example.data.model.computeMonthSummary
import com.example.data.model.formatRupiah
import com.example.data.model.monthLabel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DanaBlue
import com.example.ui.theme.DanaDark
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Purple100
import com.example.ui.theme.Purple700
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayTab(
    state: MainUiState,
    onSelectPackage: (PaymentPackage) -> Unit,
    onCopyDana: (String) -> Unit,
    onOpenDanaApp: (String) -> Unit,
    onOpenQrisZoom: () -> Unit,
    onOpenQrisScanner: () -> Unit,
    onClearScannedQris: () -> Unit,
    onSubmitPayment: (method: String, note: String) -> Unit
) {
    val resident = state.currentResident ?: return
    val summary = computeMonthSummary(resident, state.userPayments, state.selectedMonth)
    val canIuran = summary.iuranStatus == DuesStatus.UNPAID
    val canHidmat = summary.hidmatActive && summary.hidmatStatus == DuesStatus.UNPAID

    val effectivePkg = when {
        state.selectedPayPackage == PaymentPackage.IURAN && !canIuran && canHidmat -> PaymentPackage.HIDMAT
        state.selectedPayPackage == PaymentPackage.HIDMAT && !canHidmat && canIuran -> PaymentPackage.IURAN
        else -> state.selectedPayPackage
    }

    val totalSelected = (if (effectivePkg == PaymentPackage.IURAN || effectivePkg == PaymentPackage.BOTH) {
        if (canIuran) state.settings.iuranAmount else 0L
    } else 0L) + (if (effectivePkg == PaymentPackage.HIDMAT || effectivePkg == PaymentPackage.BOTH) {
        if (canHidmat) state.settings.hidmatAmount else 0L
    } else 0L)

    val methods = listOf("Transfer DANA", "DANA QRIS", "Tunai ke Bendahara", "Transfer Bank")
    var selectedMethod by remember { mutableStateOf(if (state.scannedQris != null) "DANA QRIS" else methods.first()) }
    var methodExpanded by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf(state.scannedQris?.formatConfirmationNote().orEmpty()) }

    LaunchedEffect(state.scannedQris) {
        val scanned = state.scannedQris
        if (scanned != null) {
            selectedMethod = "DANA QRIS"
            note = scanned.formatConfirmationNote()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // DANA Destination Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(DanaBlue, DanaDark)),
                    RoundedCornerShape(24.dp)
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "DANA RESMI",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    text = state.settings.danaNumber,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "a.n. ${state.settings.danaAccountName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onCopyDana(state.settings.danaNumber) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.22f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("copy_dana_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin")
                    }
                    Button(
                        onClick = onOpenQrisZoom,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = DanaBlue
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("open_qris_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("QRIS", fontWeight = FontWeight.ExtraBold)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onOpenDanaApp(state.settings.danaNumber) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.22f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("open_dana_app_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka DANA", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onOpenQrisScanner,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald700,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("scan_qris_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan QRIS", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // Package & Form Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Pilih Tagihan (${monthLabel(state.selectedMonth)})",
                    style = MaterialTheme.typography.titleSmall,
                    color = Slate900
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PackageCardButton(
                        title = "Iuran",
                        subtitle = formatRupiah(state.settings.iuranAmount),
                        selected = effectivePkg == PaymentPackage.IURAN && canIuran,
                        enabled = canIuran,
                        activeBorder = Emerald700,
                        activeBg = Emerald50,
                        onClick = { onSelectPackage(PaymentPackage.IURAN) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pkg_iuran_button")
                    )
                    PackageCardButton(
                        title = "Hidmat",
                        subtitle = if (resident.hidmatEnabled) formatRupiah(state.settings.hidmatAmount) else "Nonaktif",
                        selected = effectivePkg == PaymentPackage.HIDMAT && canHidmat,
                        enabled = canHidmat,
                        activeBorder = DanaBlue,
                        activeBg = Color(0xFFEFF6FF),
                        onClick = { onSelectPackage(PaymentPackage.HIDMAT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pkg_hidmat_button")
                    )
                    PackageCardButton(
                        title = "Keduanya",
                        subtitle = "Iuran + Hidmat",
                        selected = effectivePkg == PaymentPackage.BOTH && (canIuran || canHidmat),
                        enabled = canIuran || canHidmat,
                        activeBorder = Purple700,
                        activeBg = Purple100.copy(alpha = 0.45f),
                        onClick = { onSelectPackage(PaymentPackage.BOTH) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pkg_both_button")
                    )
                }

                Surface(
                    color = Emerald50,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Emerald100),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Tagihan",
                            style = MaterialTheme.typography.labelLarge,
                            color = Slate600
                        )
                        Text(
                            text = formatRupiah(totalSelected),
                            style = MaterialTheme.typography.titleLarge,
                            color = Emerald700,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                val scannedQris = state.scannedQris
                if (scannedQris != null) {
                    Surface(
                        color = Emerald50,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scanned_qris_summary_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "QRIS Terdeteksi: ${scannedQris.merchantName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Slate900,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                val detailText = buildList {
                                    scannedQris.providerLabel?.let { add(it) }
                                    scannedQris.merchantCity?.let { add(it) }
                                    if (scannedQris.amountRupiah != null) {
                                        add("Nominal QRIS ${formatRupiah(scannedQris.amountRupiah)}")
                                    } else {
                                        add("QRIS Statis")
                                    }
                                }.joinToString(" • ")
                                Text(
                                    text = detailText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                            }
                            IconButton(
                                onClick = onClearScannedQris,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("clear_scanned_qris_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Hapus hasil scan QRIS",
                                    tint = Slate500
                                )
                            }
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = methodExpanded,
                    onExpandedChange = { methodExpanded = !methodExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Metode Pembayaran") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = methodExpanded,
                        onDismissRequest = { methodExpanded = false }
                    ) {
                        methods.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedMethod = option
                                    methodExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan transfer (opsional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_note_input")
                )

                Button(
                    onClick = {
                        onSubmitPayment(selectedMethod, note)
                        note = ""
                    },
                    enabled = (canIuran || canHidmat) && totalSelected > 0 && !state.isMutating,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("submit_payment_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isOnline) "Kirim Konfirmasi Pembayaran" else "Simpan ke Antrean Offline",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Payment History
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Riwayat Pembayaran",
                    style = MaterialTheme.typography.titleSmall,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(10.dp))
                val list = state.userPayments.take(10)
                if (list.isEmpty()) {
                    Text(
                        text = "Belum ada riwayat pembayaran",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp)
                    )
                } else {
                    list.forEachIndexed { idx, p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${if (p.type == "hidmat") "Hidmat" else "Iuran"} - ${monthLabel(p.month)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Slate900
                                )
                                Text(
                                    text = "${p.dateLabel} • ${p.method}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = formatRupiah(p.amount),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Slate900
                                )
                                val badgeStatus = when (p.status) {
                                    "paid" -> DuesStatus.PAID
                                    "unpaid" -> DuesStatus.UNPAID
                                    else -> DuesStatus.PENDING
                                }
                                StatusBadge(
                                    status = badgeStatus,
                                    offlineQueued = p.offlineQueued,
                                    isAutoBill = p.isAutoGeneratedBill && p.status != "unpaid"
                                )
                            }
                        }
                        if (idx < list.lastIndex) {
                            HorizontalDivider(color = Slate100)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageCardButton(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    activeBorder: Color,
    activeBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) activeBg else Slate50,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) activeBorder else Slate200),
        modifier = modifier
            .heightIn(min = 60.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = if (!enabled) Slate400 else if (selected) activeBorder else Slate600,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (!enabled) Slate400 else Slate600,
                textAlign = TextAlign.Center
            )
        }
    }
}
