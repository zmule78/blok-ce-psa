package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.DuesStatus
import com.example.data.model.SystemSettings
import com.example.data.model.formatRupiah
import com.example.data.model.monthLabel
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber600
import com.example.ui.theme.DanaBlue
import com.example.ui.theme.DanaLight
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Rose100
import com.example.ui.theme.Rose50
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import java.io.ByteArrayOutputStream
import kotlin.math.min

@Composable
fun StatusBadge(status: DuesStatus, offlineQueued: Boolean = false, isAutoBill: Boolean = false) {
    val (bg, fg, icon, text) = when {
        offlineQueued -> Quad(Amber100, Amber600, Icons.Filled.WifiOff, "Antrean Offline")
        status == DuesStatus.PAID -> Quad(Emerald100, Emerald700, Icons.Filled.CheckCircle, "Lunas")
        isAutoBill -> Quad(Amber100, Amber600, Icons.Filled.Schedule, "Tagihan Aktif")
        status == DuesStatus.PENDING -> Quad(Amber100, Amber600, Icons.Filled.Schedule, "Verifikasi")
        status == DuesStatus.NOT_APPLICABLE -> Quad(Slate100, Slate500, Icons.Filled.Remove, "Nonaktif")
        else -> Quad(Rose100, Rose600, Icons.Filled.Error, "Belum Bayar")
    }
    Surface(
        color = bg,
        shape = CircleShape,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun QrisGraphic(
    customQrisData: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "qris_scan")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_y"
    )

    val decodedBitmap = remember(customQrisData) {
        if (customQrisData.startsWith("data:image")) decodeDataUriBitmap(customQrisData) else null
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Slate200, RoundedCornerShape(18.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            decodedBitmap != null -> {
                Image(
                    bitmap = decodedBitmap.asImageBitmap(),
                    contentDescription = "QRIS DANA Resmi",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            customQrisData.startsWith("http") -> {
                AsyncImage(
                    model = customQrisData,
                    contentDescription = "QRIS DANA Resmi",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val grid = 21
                    val pad = size.minDimension * 0.04f
                    val cell = (size.minDimension - pad * 2f) / grid
                    for (r in 0 until grid) {
                        for (c in 0 until grid) {
                            val tl = r < 7 && c < 7
                            val tr = r < 7 && c >= 14
                            val bl = r >= 14 && c < 7
                            val filled = if (tl || tr || bl) {
                                val lr = if (bl) r - 14 else r
                                val lc = if (tr) c - 14 else c
                                lr == 0 || lr == 6 || lc == 0 || lc == 6 ||
                                    (lr in 2..4 && lc in 2..4)
                            } else {
                                ((r * 7 + c * 13 + 5) % 3 == 0) || ((r + c) % 4 == 0)
                            }
                            if (filled) {
                                drawRoundRect(
                                    color = Slate900,
                                    topLeft = Offset(pad + c * cell, pad + r * cell),
                                    size = Size(cell * 0.88f, cell * 0.88f),
                                    cornerRadius = CornerRadius(cell * 0.15f, cell * 0.15f)
                                )
                            }
                        }
                    }
                    // DANA center badge
                    val badgeW = size.width * 0.30f
                    val badgeH = size.height * 0.18f
                    drawRoundRect(
                        color = DanaBlue,
                        topLeft = Offset((size.width - badgeW) / 2f, (size.height - badgeH) / 2f),
                        size = Size(badgeW, badgeH),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
                Surface(
                    color = DanaBlue,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "DANA",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Animated scan line
        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = size.height * scanProgress
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Emerald500, Color.Transparent)
                ),
                start = Offset(size.width * 0.05f, y),
                end = Offset(size.width * 0.95f, y),
                strokeWidth = 5f
            )
        }
    }
}

@Composable
fun QrisZoomDialog(
    settings: SystemSettings,
    onCopyDana: () -> Unit,
    onScanQris: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QRIS DANA RESMI",
                        style = MaterialTheme.typography.labelLarge,
                        color = DanaBlue,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_qris_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup dialog QRIS",
                            tint = Slate500
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(DanaLight)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    QrisGraphic(
                        customQrisData = settings.customQrisData,
                        modifier = Modifier.size(220.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = settings.danaAccountName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Slate900,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "DANA: ${settings.danaNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DanaBlue,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onCopyDana,
                    colors = ButtonDefaults.buttonColors(containerColor = DanaBlue),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("qris_copy_dana_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salin Nomor DANA")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onScanQris,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("qris_dialog_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = null,
                        tint = Emerald700,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan QRIS dengan Kamera", color = Emerald700, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReminderBlastDialog(
    settings: SystemSettings,
    house: String,
    selectedMonth: String,
    totalDue: Long,
    onPayNow: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Rose600),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = Rose50,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Rose100)
                ) {
                    Text(
                        text = "NOTIFIKASI • ${monthLabel(selectedMonth).uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Rose600,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = settings.reminderTitle.ifBlank { "Himbauan Pembayaran Iuran" },
                    style = MaterialTheme.typography.titleLarge,
                    color = Slate900,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = settings.reminderMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = Rose50,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Rose100),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Tagihan ($house)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                        Text(
                            text = formatRupiah(totalDue),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Rose600
                        )
                        Text(
                            text = "DANA: ${settings.danaNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("reminder_later_button")
                    ) {
                        Text("Nanti Saja")
                    }
                    Button(
                        onClick = onPayNow,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("reminder_pay_now_button")
                    ) {
                        Text("Bayar Sekarang")
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementImage(imageUrl: String, modifier: Modifier = Modifier) {
    if (imageUrl.isBlank()) return
    val decoded = remember(imageUrl) {
        if (imageUrl.startsWith("data:image")) decodeDataUriBitmap(imageUrl) else null
    }
    if (decoded != null) {
        Image(
            bitmap = decoded.asImageBitmap(),
            contentDescription = "Gambar pengumuman",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else if (imageUrl.startsWith("http")) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Gambar pengumuman",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

fun uriToCompressedDataUri(context: Context, uri: Uri, maxDim: Int = 512): String? {
    return try {
        val stream = context.contentResolver.openInputStream(uri) ?: return null
        val original = stream.use { BitmapFactory.decodeStream(it) } ?: return null
        val scale = min(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height)
            .coerceAtMost(1f)
        val w = (original.width * scale).toInt().coerceAtLeast(1)
        val h = (original.height * scale).toInt().coerceAtLeast(1)
        val resized = if (scale < 1f) Bitmap.createScaledBitmap(original, w, h, true) else original
        val out = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 78, out)
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        "data:image/jpeg;base64,$b64"
    } catch (_: Exception) {
        null
    }
}

fun decodeDataUriBitmap(dataUri: String): Bitmap? {
    return try {
        val base64Part = dataUri.substringAfter("base64,", "")
        if (base64Part.isBlank()) return null
        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}
