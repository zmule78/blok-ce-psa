package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DuesStatus
import com.example.data.model.computeMonthSummary
import com.example.data.model.monthLabel
import com.example.ui.components.AnimatedRupiahText
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.StatusBadge
import com.example.ui.components.pressScale
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Purple100
import com.example.ui.theme.Purple700
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.MainUiState

@Composable
fun HomeTab(
    state: MainUiState,
    onNavigateTab: (MainTab) -> Unit,
    onOpenQrisScanner: () -> Unit
) {
    val resident = state.currentResident ?: return
    val summary = computeMonthSummary(resident, state.userPayments, state.selectedMonth)

    val (paidCount, totalMembers) = if (resident.adminRole) {
        val members = state.allResidents.filter { !it.adminRole }
        val paid = members.count { member ->
            val pays = state.allPayments.filter { it.userId == member.id }
            computeMonthSummary(member, pays, state.selectedMonth).iuranStatus == DuesStatus.PAID
        }
        paid to members.size
    } else {
        val ms = state.monthlySummary
        (ms?.paidIuranCount?.toInt() ?: 0) to (ms?.totalMembers?.toInt() ?: 0)
    }
    val rawProgress = if (totalMembers > 0) paidCount.toFloat() / totalMembers.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
        label = "home_progress"
    )
    val pct = (animatedProgress * 100).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (resident.needsProfileCompletion) {
            StaggeredEntrance(delayMillis = 0) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Amber600),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lengkapi Profil Anda",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Isi nomor rumah & nomor HP aktif Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                        Button(
                            onClick = { onNavigateTab(MainTab.ACCOUNT) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Amber600
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("complete_profile_cta")
                        ) {
                            Text("Isi", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // Main Resident Status & Progress Card
        StaggeredEntrance(delayMillis = 40) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(Emerald500, Color(0xFF0EA5E9), Emerald600, Emerald500)
                                    )
                                )
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Emerald700),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = resident.name.firstOrNull()?.uppercase() ?: "W",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Assalamualaikum,",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = resident.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = resident.house,
                                style = MaterialTheme.typography.labelMedium,
                                color = Slate500
                            )
                        }
                    }

                    Surface(
                        color = if (resident.adminRole) Purple100 else Emerald100,
                        shape = CircleShape
                    ) {
                        Text(
                            text = resident.roleLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (resident.adminRole) Purple700 else Emerald700,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Iuran & Hidmat Status Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DuesInfoMiniCard(
                        title = "IURAN",
                        amount = state.settings.iuranAmount,
                        status = summary.iuranStatus,
                        modifier = Modifier.weight(1f)
                    )
                    DuesInfoMiniCard(
                        title = "HIDMAT",
                        amount = state.settings.hidmatAmount,
                        status = summary.hidmatStatus,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Community Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Slate900)
                        .drawBehind {
                            drawCircle(
                                color = Emerald500.copy(alpha = 0.16f),
                                radius = size.maxDimension * 0.45f,
                                center = Offset(size.width * 0.92f, size.height * 0.15f)
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PROGRES IURAN WARGA",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                            Text(
                                text = if (totalMembers > 0) "$paidCount/$totalMembers" else "—",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            color = Emerald500,
                            trackColor = Color.White.copy(alpha = 0.16f),
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                        )
                        Text(
                            text = if (totalMembers > 0) {
                                "$pct% warga lunas ${monthLabel(state.selectedMonth)}"
                            } else {
                                "Menunggu pembaruan progres ${monthLabel(state.selectedMonth)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }
        }

        // Billing Status Banner
        StaggeredEntrance(delayMillis = 100) {
        if (summary.overall != DuesStatus.PAID) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Rose600),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (summary.overall == DuesStatus.PENDING) {
                                "Menunggu Verifikasi"
                            } else {
                                "Tagihan Belum Lunas"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = monthLabel(state.selectedMonth),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        IconButton(
                            onClick = onOpenQrisScanner,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_scan_qris_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = "Scan QRIS pembayaran",
                                tint = Color.White
                            )
                        }
                    }
                    Button(
                        onClick = { onNavigateTab(MainTab.PAY) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Rose600
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("home_pay_now_button")
                    ) {
                        Text("Bayar", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Emerald600),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Column {
                        Text(
                            text = "Alhamdulillah, Lunas!",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Tagihan ${monthLabel(state.selectedMonth)} sudah lunas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                }
            }
        }
        }

        // Latest Announcement Preview
        val latest = state.announcements.firstOrNull()
        if (latest != null) {
            val previewInteraction = remember { MutableInteractionSource() }
            StaggeredEntrance(delayMillis = 160) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.pressScale(previewInteraction)
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
                            text = "Info Terbaru",
                            style = MaterialTheme.typography.titleSmall,
                            color = Slate900
                        )
                        TextButton(
                            onClick = { onNavigateTab(MainTab.INFO) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("home_see_all_info_button")
                        ) {
                            Text(
                                text = "Lihat Semua",
                                color = Emerald700,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Surface(
                        color = Slate50,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = previewInteraction,
                                indication = null
                            ) { onNavigateTab(MainTab.INFO) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = latest.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = Slate900,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = latest.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
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
private fun DuesInfoMiniCard(
    title: String,
    amount: Long,
    status: DuesStatus,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Slate50,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Slate200),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )
            AnimatedRupiahText(
                targetAmount = amount,
                style = MaterialTheme.typography.titleSmall,
                color = Slate900,
                fontWeight = FontWeight.ExtraBold,
                useMonospace = true
            )
            StatusBadge(status = status)
        }
    }
}
