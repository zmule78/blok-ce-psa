package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DuesStatus
import com.example.data.model.Payment
import com.example.data.model.PaymentPackage
import com.example.data.model.TimelineFilter
import com.example.data.model.buildResidentPaymentTimeline
import com.example.data.model.computeMonthSummary
import com.example.data.model.formatRupiah
import com.example.data.model.monthLabel
import com.example.ui.components.EmphasizedDecelerate
import com.example.ui.components.ReceiptPreviewDialog
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.StatusBadge
import com.example.ui.components.decodeDataUriBitmap
import com.example.ui.theme.Amber600
import com.example.ui.theme.DanaBlue
import com.example.ui.theme.DanaDark
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Rose600
import com.example.ui.theme.adaptiveColors
import com.example.ui.theme.adaptiveFieldColors
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
    onOpenReceiptScanner: () -> Unit = {},
    onClearScannedReceipt: () -> Unit = {},
    requireBiometricConfirm: Boolean = false,
    onRequestBiometricForPayment: (amount: Long, onVerified: () -> Unit) -> Unit = { _, onVerified -> onVerified() },
    onSubmitPayment: (method: String, note: String) -> Unit
) {
    val resident = state.currentResident ?: return
    val ac = adaptiveColors
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
    var previewPaymentReceipt by remember { mutableStateOf<Payment?>(null) }

    LaunchedEffect(state.scannedQris) {
        val scanned = state.scannedQris
        if (scanned != null) {
            selectedMethod = "DANA QRIS"
            note = scanned.formatConfirmationNote()
        }
    }

    LaunchedEffect(state.scannedReceipt) {
        val receipt = state.scannedReceipt
        if (receipt != null) {
            receipt.parsed.detectedMethod?.let { m ->
                if (m in methods) selectedMethod = m
            }
            if (note.isBlank() && receipt.parsed.summaryText.isNotBlank()) {
                note = "Struk OCR: ${receipt.parsed.summaryText}"
            }
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

        // Prominent Quick-Access OCR Receipt Scanner Card (easy reach right under DANA card)
        AnimatedContent(
            targetState = state.scannedReceipt,
            transitionSpec = {
                (fadeIn(tween(260, easing = EmphasizedDecelerate)) +
                    scaleIn(initialScale = 0.97f, animationSpec = tween(260, easing = EmphasizedDecelerate))) togetherWith
                    (fadeOut(tween(170)) + scaleOut(targetScale = 0.98f, animationSpec = tween(170)))
            },
            label = "ocr_scanner_card_transition"
        ) { scannedReceipt ->
        if (scannedReceipt == null) {
            Surface(
                color = ac.cardContainer,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, ac.danaBorder),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(enabled = !state.isMutating, onClick = onOpenReceiptScanner)
                    .testTag("open_receipt_scanner_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Brush.linearGradient(listOf(DanaBlue, DanaDark))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DocumentScanner,
                            contentDescription = "Scan struk bukti transfer OCR",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scan Struk Transfer (OCR)",
                            style = MaterialTheme.typography.titleSmall,
                            color = ac.textPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Foto struk atau pilih galeri — isi otomatis metode & catatan",
                            style = MaterialTheme.typography.bodySmall,
                            color = ac.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = ac.danaSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ac.danaBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Scan",
                                style = MaterialTheme.typography.labelMedium,
                                color = ac.danaText,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = ac.danaText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        } else {
            val thumb = remember(scannedReceipt.receiptImageUrl) {
                decodeDataUriBitmap(scannedReceipt.receiptImageUrl)
            }
            Surface(
                color = ac.danaSurface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, ac.danaBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scanned_receipt_summary_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (thumb != null) {
                            Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = "Pratinjau struk bukti transfer",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DanaBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Struk Digital Terpasang",
                                style = MaterialTheme.typography.labelLarge,
                                color = ac.textPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = scannedReceipt.parsed.summaryText.ifBlank {
                                    "Foto struk siap dikirim ke admin"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = ac.textSecondary
                            )
                        }
                        IconButton(
                            onClick = onClearScannedReceipt,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("clear_scanned_receipt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Hapus struk bukti",
                                tint = ac.textMuted
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenReceiptScanner,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DocumentScanner,
                                contentDescription = null,
                                tint = ac.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Ulang", color = ac.textPrimary)
                        }
                        if (scannedReceipt.parsed.summaryText.isNotBlank()) {
                            Button(
                                onClick = {
                                    scannedReceipt.parsed.detectedMethod?.let { m ->
                                        if (m in methods) selectedMethod = m
                                    }
                                    note = "Struk OCR: ${scannedReceipt.parsed.summaryText}"
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DanaBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("apply_receipt_ocr_button")
                            ) {
                                Text("Terapkan OCR", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        }

        // Package & Form Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
            border = BorderStroke(1.dp, ac.borderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Pilih Tagihan (${monthLabel(state.selectedMonth)})",
                    style = MaterialTheme.typography.titleSmall,
                    color = ac.textPrimary
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
                        activeBorder = ac.emeraldText,
                        activeBg = ac.emeraldSurface,
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
                        activeBorder = ac.danaText,
                        activeBg = ac.danaSurface,
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
                        activeBorder = ac.purpleText,
                        activeBg = ac.purpleSurface,
                        onClick = { onSelectPackage(PaymentPackage.BOTH) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pkg_both_button")
                    )
                }

                Surface(
                    color = ac.emeraldSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ac.emeraldBorder),
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
                            color = ac.textSecondary
                        )
                        Text(
                            text = formatRupiah(totalSelected),
                            style = MaterialTheme.typography.titleLarge,
                            color = ac.emeraldText,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                AnimatedVisibility(
                    visible = state.scannedQris != null,
                    enter = expandVertically(tween(240, easing = EmphasizedDecelerate)) + fadeIn(tween(220)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(150))
                ) {
                val scannedQris = state.scannedQris
                if (scannedQris != null) {
                    Surface(
                        color = ac.emeraldSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ac.emeraldBorder),
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
                                tint = ac.emeraldText,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "QRIS Terdeteksi: ${scannedQris.merchantName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ac.textPrimary,
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
                                    color = ac.textSecondary
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
                                    tint = ac.textMuted
                                )
                            }
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
                        colors = adaptiveFieldColors(),
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
                    trailingIcon = {
                        if (state.scannedReceipt == null) {
                            IconButton(
                                onClick = onOpenReceiptScanner,
                                enabled = !state.isMutating,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("inline_receipt_scanner_icon")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DocumentScanner,
                                    contentDescription = "Scan struk transfer dengan OCR",
                                    tint = ac.danaText
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_note_input")
                )

                AnimatedVisibility(
                    visible = requireBiometricConfirm,
                    enter = expandVertically(tween(220, easing = EmphasizedDecelerate)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
                ) {
                    Surface(
                        color = ac.emeraldSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ac.emeraldBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fingerprint,
                                contentDescription = null,
                                tint = ac.emeraldText,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Pembayaran ini memerlukan verifikasi sidik jari / wajah.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ac.emeraldText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val methodSnapshot = selectedMethod
                        val noteSnapshot = note
                        if (requireBiometricConfirm) {
                            onRequestBiometricForPayment(totalSelected) {
                                onSubmitPayment(methodSnapshot, noteSnapshot)
                                note = ""
                            }
                        } else {
                            onSubmitPayment(methodSnapshot, noteSnapshot)
                            note = ""
                        }
                    },
                    enabled = (canIuran || canHidmat) && totalSelected > 0 && !state.isMutating,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("submit_payment_button")
                ) {
                    Icon(
                        imageVector = if (requireBiometricConfirm) Icons.Filled.Fingerprint else Icons.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isOnline) "Kirim Konfirmasi Pembayaran" else "Simpan ke Antrean Offline",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Read-Only Historical Payment Timeline for Residents
        var timelineFilter by remember { mutableStateOf(TimelineFilter.ALL) }
        var showAllTimeline by remember { mutableStateOf(false) }
        val filteredTimeline = remember(state.userPayments, timelineFilter) {
            buildResidentPaymentTimeline(state.userPayments, timelineFilter)
        }
        val visibleTimeline = remember(filteredTimeline, showAllTimeline) {
            if (showAllTimeline) filteredTimeline else filteredTimeline.take(10)
        }

        StaggeredEntrance(delayMillis = 40) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
            border = BorderStroke(1.dp, ac.borderSubtle),
            modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
                )
                .testTag("resident_payment_timeline_card")
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = ac.emeraldSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null,
                                    tint = ac.emeraldText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Timeline Riwayat Pembayaran",
                                style = MaterialTheme.typography.titleSmall,
                                color = ac.textPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Catatan histori transaksi Anda",
                                style = MaterialTheme.typography.bodySmall,
                                color = ac.textMuted
                            )
                        }
                    }
                    Surface(
                        color = ac.subtleContainer,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, ac.borderSubtle)
                    ) {
                        Text(
                            text = "${state.userPayments.size} Catatan",
                            style = MaterialTheme.typography.labelSmall,
                            color = ac.textSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                if (state.userPayments.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimelineFilter.entries.forEach { option ->
                            val selected = timelineFilter == option
                            Surface(
                                color = if (selected) ac.emeraldSurface else ac.cardElevated,
                                shape = CircleShape,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) ac.emeraldBorder else ac.borderSubtle
                                ),
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .clip(CircleShape)
                                    .clickable { timelineFilter = option }
                                    .testTag("timeline_filter_${option.name.lowercase()}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) ac.emeraldText else ac.textSecondary,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                if (visibleTimeline.isEmpty()) {
                    Surface(
                        color = ac.subtleContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (state.userPayments.isEmpty()) {
                                "Belum ada riwayat pembayaran"
                            } else {
                                "Tidak ada catatan untuk filter ${timelineFilter.label}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ac.textMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 22.dp, horizontal = 14.dp)
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        visibleTimeline.forEachIndexed { idx, entry ->
                            val p = entry.payment
                            val nodeColor = when (entry.status) {
                                DuesStatus.PAID -> Emerald600
                                DuesStatus.UNPAID -> Rose600
                                else -> Amber600
                            }
                            val isFirst = idx == 0
                            val isLast = idx == visibleTimeline.lastIndex

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                            ) {
                                // Vertical Timeline Rail + Status Node
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    if (!isFirst) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(14.dp)
                                                .background(ac.borderSubtle)
                                                .align(Alignment.TopCenter)
                                        )
                                    }
                                    if (!isLast) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .fillMaxHeight()
                                                .padding(top = 24.dp)
                                                .background(ac.borderSubtle)
                                                .align(Alignment.TopCenter)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 12.dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(nodeColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(nodeColor)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Read-Only Timeline Entry Card
                                Surface(
                                    color = ac.cardElevated,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, ac.borderSubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(bottom = if (isLast) 0.dp else 10.dp)
                                        .testTag("timeline_item_${p.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = entry.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = ac.textPrimary,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(end = 8.dp)
                                            )
                                            StatusBadge(
                                                status = entry.status,
                                                offlineQueued = p.offlineQueued,
                                                isAutoBill = p.isAutoGeneratedBill && p.status != "unpaid"
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Schedule,
                                                contentDescription = null,
                                                tint = ac.textMuted,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "${entry.timestampLabel} • ${p.method}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ac.textMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (p.note.isNotBlank()) {
                                            Text(
                                                text = p.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ac.textSecondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = formatRupiah(p.amount),
                                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                                color = ac.textPrimary,
                                                fontWeight = FontWeight.Black
                                            )
                                            if (p.receiptImageUrl.isNotBlank() || p.receiptOcrText.isNotBlank()) {
                                                Surface(
                                                    color = ac.danaSurface,
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, ac.danaBorder),
                                                    modifier = Modifier
                                                        .heightIn(min = 36.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable { previewPaymentReceipt = p }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.ReceiptLong,
                                                            contentDescription = null,
                                                            tint = ac.danaText,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = "Lihat Struk OCR",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = ac.danaText,
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
                    }

                    if (filteredTimeline.size > 10) {
                        TextButton(
                            onClick = { showAllTimeline = !showAllTimeline },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("timeline_toggle_all_button")
                        ) {
                            Text(
                                text = if (showAllTimeline) {
                                    "Tampilkan Lebih Ringkas"
                                } else {
                                    "Lihat Semua (${filteredTimeline.size} Transaksi)"
                                },
                                color = ac.emeraldText,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
        }
    }

    val previewPay = previewPaymentReceipt
    if (previewPay != null) {
        ReceiptPreviewDialog(
            receiptImageUrl = previewPay.receiptImageUrl,
            receiptOcrText = previewPay.receiptOcrText,
            payerLabel = "${monthLabel(previewPay.month)} • ${formatRupiah(previewPay.amount)}",
            onDismiss = { previewPaymentReceipt = null }
        )
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
    val ac = adaptiveColors
    Surface(
        color = if (selected) activeBg else ac.cardElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) activeBorder else ac.borderSubtle),
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
                color = if (!enabled) ac.textMuted else if (selected) activeBorder else ac.textSecondary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (!enabled) ac.textMuted else ac.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
