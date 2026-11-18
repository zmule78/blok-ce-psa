package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dana.DanaWebhookSigner
import com.example.data.model.AUTO_BILL_METHOD
import com.example.data.model.DanaWebhookEvent
import com.example.data.model.Payment
import com.example.data.model.formatRupiah
import com.example.data.model.monthLabel
import com.example.ui.theme.DanaBlue
import com.example.ui.theme.Emerald700
import com.example.ui.theme.adaptiveColors
import com.example.ui.theme.adaptiveFieldColors
import com.example.ui.viewmodel.MainUiState

@Composable
fun DanaWebhookCard(
    state: MainUiState,
    onSaveWebhookConfig: (
        webhookUrl: String,
        webhookSecret: String,
        relayEnabled: Boolean,
        autoVerifyEnabled: Boolean
    ) -> Unit,
    onTestWebhookPing: (webhookUrl: String, webhookSecret: String) -> Unit,
    onVerifyWebhookEventManual: (eventId: String, paymentId: String, senderName: String) -> Unit,
    onDeleteWebhookEvent: (eventId: String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val ac = adaptiveColors
    val cfg = state.danaWebhookConfig

    var webhookUrl by remember { mutableStateOf(cfg.webhookUrl) }
    var webhookSecret by remember { mutableStateOf(cfg.webhookSecret) }
    var relayEnabled by remember { mutableStateOf(cfg.relayFromAndroidEnabled) }
    var autoVerifyEvents by remember { mutableStateOf(cfg.autoVerifyWebhookEvents) }
    var matchingEvent by remember { mutableStateOf<DanaWebhookEvent?>(null) }

    LaunchedEffect(
        cfg.webhookUrl,
        cfg.webhookSecret,
        cfg.relayFromAndroidEnabled,
        cfg.autoVerifyWebhookEvents
    ) {
        webhookUrl = cfg.webhookUrl
        webhookSecret = cfg.webhookSecret
        relayEnabled = cfg.relayFromAndroidEnabled
        autoVerifyEvents = cfg.autoVerifyWebhookEvents
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
        border = BorderStroke(1.dp, ac.danaBorder),
        modifier = Modifier.testTag("dana_webhook_card")
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
                    Surface(
                        color = ac.danaSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Webhook,
                                contentDescription = null,
                                tint = ac.danaText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Webhook DANA Real-Time",
                            style = MaterialTheme.typography.titleSmall,
                            color = ac.textPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Node.js HMAC-SHA256 / Google Apps Script -> Firestore Live",
                            style = MaterialTheme.typography.bodySmall,
                            color = ac.textMuted
                        )
                    }
                }
                Surface(
                    color = ac.emeraldSurface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = ac.emeraldText,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            OutlinedTextField(
                value = webhookUrl,
                onValueChange = { webhookUrl = it },
                label = { Text("URL Endpoint Webhook (/webhook/dana)") },
                placeholder = { Text("https://blokce-webhook.example.run.app/webhook/dana") },
                leadingIcon = {
                    Icon(Icons.Filled.Http, contentDescription = null, tint = ac.danaText)
                },
                singleLine = true,
                colors = adaptiveFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("webhook_url_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = webhookSecret,
                    onValueChange = { webhookSecret = it },
                    label = { Text("Secret HMAC-SHA256") },
                    placeholder = { Text("Opsional / generate otomatis") },
                    singleLine = true,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("webhook_secret_input")
                )
                Surface(
                    color = ac.danaSurface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    IconButton(
                        onClick = {
                            webhookSecret = DanaWebhookSigner.generateSecretHex()
                            onShowMessage("Secret HMAC-SHA256 baru dibuat — jangan lupa Simpan.")
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("generate_webhook_secret_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Autorenew,
                            contentDescription = "Generate secret webhook",
                            tint = ac.danaText
                        )
                    }
                }
                Surface(
                    color = ac.subtleContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (webhookSecret.isBlank()) {
                                onShowMessage("Secret masih kosong.")
                            } else {
                                copyPlainText(context, "Secret Webhook", webhookSecret)
                                onShowMessage("Secret HMAC disalin ke clipboard!")
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("copy_webhook_secret_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Salin secret webhook",
                            tint = ac.textSecondary
                        )
                    }
                }
            }

            Surface(
                color = ac.cardElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ac.borderSubtle)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Verifikasi Event Webhook Masuk",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ac.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Proses otomatis event berstatus RECEIVED dari Apps Script / server",
                                style = MaterialTheme.typography.labelSmall,
                                color = ac.textMuted
                            )
                        }
                        Switch(
                            checked = autoVerifyEvents,
                            onCheckedChange = { autoVerifyEvents = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Emerald700),
                            modifier = Modifier.testTag("webhook_autoverify_switch")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Teruskan Notif DANA Android ke Webhook",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ac.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Kirim payload signed ke URL server saat notif DANA masuk di HP",
                                style = MaterialTheme.typography.labelSmall,
                                color = ac.textMuted
                            )
                        }
                        Switch(
                            checked = relayEnabled,
                            onCheckedChange = { relayEnabled = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = DanaBlue),
                            modifier = Modifier.testTag("webhook_relay_switch")
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onSaveWebhookConfig(webhookUrl, webhookSecret, relayEnabled, autoVerifyEvents)
                    },
                    enabled = !state.isMutating,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("save_webhook_config_button")
                ) {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onTestWebhookPing(webhookUrl, webhookSecret) },
                    enabled = !state.isMutating && webhookUrl.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("test_webhook_ping_button")
                ) {
                    Icon(Icons.Filled.Send, null, modifier = Modifier.size(16.dp), tint = ac.danaText)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Ping", fontWeight = FontWeight.Bold, color = ac.textPrimary)
                }
            }

            OutlinedButton(
                onClick = {
                    val curlCmd = DanaWebhookSigner.buildCurlTemplate(webhookUrl, webhookSecret)
                    copyPlainText(context, "cURL Webhook DANA", curlCmd)
                    onShowMessage("Contoh perintah cURL + HMAC-SHA256 disalin!")
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("copy_webhook_curl_button")
            ) {
                Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp), tint = ac.emeraldText)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Salin Contoh cURL Webhook (HMAC-SHA256)", fontWeight = FontWeight.Bold, color = ac.textPrimary)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stream Event Webhook (${state.danaWebhookEvents.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = ac.textPrimary
                )
                Text(
                    text = "Firestore Live",
                    style = MaterialTheme.typography.labelSmall,
                    color = ac.danaText,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.danaWebhookEvents.isEmpty()) {
                Surface(
                    color = ac.cardElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Belum ada transaksi DANA masuk via webhook. Jalankan skrip Node.js / Google Apps Script untuk menerima event secara real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ac.textMuted,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                state.danaWebhookEvents.take(6).forEach { ev ->
                    val statusColors = when (ev.status) {
                        "AUTO_VERIFIED" -> Triple(ac.emeraldSurface, ac.emeraldText, "LUNAS OTOMATIS")
                        "NEED_REVIEW" -> Triple(ac.amberSurface, ac.amberText, "PILIH MANUAL")
                        "RECEIVED" -> Triple(ac.danaSurface, ac.danaText, "PROSES...")
                        else -> Triple(ac.subtleContainer, ac.textSecondary, "BELUM COCOK")
                    }
                    Surface(
                        color = statusColors.first.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, statusColors.second.copy(alpha = 0.28f)),
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
                                    text = formatRupiah(ev.amount),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ac.textPrimary,
                                    fontWeight = FontWeight.Black
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = statusColors.first,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = statusColors.third,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColors.second,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteWebhookEvent(ev.id.ifBlank { ev.referenceId }) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("delete_webhook_event_${ev.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Hapus log webhook",
                                            tint = ac.textMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            val metaLine = buildString {
                                append(ev.source.ifBlank { "webhook" })
                                if (ev.senderName.isNotBlank()) append(" • ${ev.senderName}")
                                if (ev.houseHint.isNotBlank()) append(" (${ev.houseHint})")
                            }
                            Text(
                                text = metaLine,
                                style = MaterialTheme.typography.labelSmall,
                                color = ac.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (ev.matchedSummary.isNotBlank()) {
                                Text(
                                    text = ev.matchedSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ac.textPrimary
                                )
                            }

                            if (ev.status != "AUTO_VERIFIED") {
                                OutlinedButton(
                                    onClick = { matchingEvent = ev },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("match_webhook_event_${ev.id}")
                                ) {
                                    Text("Cocokkan Tagihan Manual", fontWeight = FontWeight.Bold, color = ac.textPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val toMatch = matchingEvent
    if (toMatch != null) {
        MatchWebhookEventDialog(
            event = toMatch,
            state = state,
            onMatchPayment = { pay ->
                onVerifyWebhookEventManual(
                    toMatch.id.ifBlank { toMatch.referenceId },
                    pay.id,
                    toMatch.senderName
                )
                matchingEvent = null
            },
            onDismiss = { matchingEvent = null }
        )
    }
}

@Composable
private fun MatchWebhookEventDialog(
    event: DanaWebhookEvent,
    state: MainUiState,
    onMatchPayment: (Payment) -> Unit,
    onDismiss: () -> Unit
) {
    val ac = adaptiveColors
    val residentById = remember(state.allResidents) { state.allResidents.associateBy { it.id } }
    val openPayments = remember(state.allPayments, event.amount) {
        state.allPayments
            .filter { it.status != "paid" }
            .sortedWith(
                compareByDescending<Payment> { it.amount == event.amount }
                    .thenByDescending { it.status == "pending" && it.method != AUTO_BILL_METHOD }
                    .thenByDescending { it.month }
            )
            .take(15)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ac.cardContainer,
        titleContentColor = ac.textPrimary,
        textContentColor = ac.textPrimary,
        title = { Text("Cocokkan Webhook ${formatRupiah(event.amount)}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (event.senderName.isNotBlank() || event.houseHint.isNotBlank()) {
                    Text(
                        text = "Info pengirim: ${event.senderName.ifBlank { "-" }} (${event.houseHint.ifBlank { "-" }})",
                        style = MaterialTheme.typography.bodySmall,
                        color = ac.danaText,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (openPayments.isEmpty()) {
                    Text(
                        text = "Tidak ada tagihan terbuka (unpaid/pending) yang tersedia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ac.textMuted
                    )
                } else {
                    openPayments.forEach { pay ->
                        val owner = residentById[pay.userId]
                        val exact = pay.amount == event.amount
                        Surface(
                            color = if (exact) ac.emeraldSurface else ac.cardElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (exact) ac.emeraldText else ac.borderSubtle
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMatchPayment(pay) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${owner?.name ?: pay.userId} (${owner?.house ?: "-"})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ac.textPrimary
                                )
                                Text(
                                    text = "${pay.type.uppercase()} • ${monthLabel(pay.month)} • ${formatRupiah(pay.amount)} • ${pay.status}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ac.textSecondary
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
                Text("Tutup", color = ac.textSecondary)
            }
        }
    )
}

private fun copyPlainText(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}
