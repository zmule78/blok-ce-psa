package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminDashboardAnalytics
import com.example.data.model.MonthlyTrendPoint
import com.example.data.model.computeAdminDashboardAnalytics
import com.example.data.model.formatCompactRupiah
import com.example.data.model.formatRupiah
import com.example.data.model.monthLabel
import com.example.ui.components.AnimatedPercentText
import com.example.ui.components.AnimatedRupiahText
import com.example.ui.components.LivePulseDot
import com.example.ui.components.StaggeredEntrance
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber600
import com.example.ui.theme.DanaBlue
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald950
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Rose100
import com.example.ui.theme.Rose50
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.MainUiState
import kotlin.math.roundToInt

private enum class TrendChartMode {
    AREA,
    BAR
}

@Composable
fun AdminAnalyticsDashboardCard(
    state: MainUiState,
    onExportRecapCsv: () -> Unit
) {
    var windowMonths by remember { mutableIntStateOf(6) }
    var chartMode by remember { mutableStateOf(TrendChartMode.AREA) }

    val analytics: AdminDashboardAnalytics = remember(
        state.selectedMonth,
        state.allResidents,
        state.allPayments,
        state.settings,
        windowMonths
    ) {
        computeAdminDashboardAnalytics(
            selectedMonth = state.selectedMonth,
            residents = state.allResidents,
            payments = state.allPayments,
            settings = state.settings,
            monthWindowSize = windowMonths
        )
    }

    val trends = analytics.monthlyTrends
    val defaultIndex = remember(trends, state.selectedMonth) {
        trends.indexOfFirst { it.monthCode == state.selectedMonth }
            .takeIf { it >= 0 }
            ?: trends.lastIndex.coerceAtLeast(0)
    }
    var selectedIdx by remember(trends.size, defaultIndex) { mutableIntStateOf(defaultIndex) }
    val clampedIdx = selectedIdx.coerceIn(0, (trends.size - 1).coerceAtLeast(0))
    val activePoint = trends.getOrNull(clampedIdx)
    val recap = analytics.selectedMonthRecap

    StaggeredEntrance {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_analytics_dashboard")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
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
                            color = Emerald50,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Analitik Kas Visual",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Slate900,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                LivePulseDot(color = Emerald500, size = 6.dp)
                            }
                            Text(
                                text = "Tren bulanan • ${monthLabel(state.selectedMonth)}",
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
                            .testTag("dashboard_export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Ekspor rekap CSV",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CSV", fontWeight = FontWeight.Bold)
                    }
                }

                // Dual Animated Hero KPI Cards: Dana Masuk vs Belum Terbayar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Dana Masuk Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(Emerald950, Emerald800, Emerald700)))
                            .drawBehind {
                                drawCircle(
                                    color = Emerald500.copy(alpha = 0.22f),
                                    radius = size.maxDimension * 0.55f,
                                    center = Offset(size.width * 0.92f, size.height * 0.12f)
                                )
                            }
                            .padding(14.dp)
                            .testTag("kpi_total_dana_masuk")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Emerald100,
                                    modifier = Modifier.size(16.dp)
                                )
                                Surface(
                                    color = Color.White.copy(alpha = 0.16f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${recap.collectionPercent}% Lunas",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "DANA MASUK",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald100
                            )
                            AnimatedRupiahText(
                                targetAmount = recap.totalCollectedMonth,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                useMonospace = true
                            )
                            Text(
                                text = "Total Kas: ${formatCompactRupiah(analytics.totalCollectedAllTime)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald100.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Belum Terbayar Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(Rose50, Color.White)))
                            .drawBehind {
                                drawCircle(
                                    color = Rose600.copy(alpha = 0.09f),
                                    radius = size.maxDimension * 0.5f,
                                    center = Offset(size.width * 0.9f, size.height * 0.15f)
                                )
                            }
                            .padding(1.dp)
                    ) {
                        Surface(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Rose100),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kpi_belum_terbayar")
                        ) {
                            Column(
                                modifier = Modifier.padding(13.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.WarningAmber,
                                        contentDescription = null,
                                        tint = Rose600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Surface(
                                        color = if (analytics.totalPendingSelectedMonth > 0L) Amber50 else Rose100,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = if (analytics.totalPendingSelectedMonth > 0L) {
                                                "Verif ${formatCompactRupiah(analytics.totalPendingSelectedMonth)}"
                                            } else {
                                                "Tertunggak"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (analytics.totalPendingSelectedMonth > 0L) Amber600 else Rose600,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "BELUM TERBAYAR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Rose600
                                )
                                AnimatedRupiahText(
                                    targetAmount = analytics.totalUnpaidSelectedMonth,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Slate900,
                                    useMonospace = true
                                )
                                Text(
                                    text = "Total Unpaid: ${formatCompactRupiah(analytics.totalRecordedUnpaidAllPeriods)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate500,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Chart Mode & Timeframe Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChartToggleChip(
                            label = "6 Bln",
                            selected = windowMonths == 6,
                            onClick = { windowMonths = 6 },
                            tag = "chart_window_6m"
                        )
                        ChartToggleChip(
                            label = "12 Bln",
                            selected = windowMonths == 12,
                            onClick = { windowMonths = 12 },
                            tag = "chart_window_12m"
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChartModeIconChip(
                            icon = Icons.Filled.ShowChart,
                            label = "Area",
                            selected = chartMode == TrendChartMode.AREA,
                            onClick = { chartMode = TrendChartMode.AREA },
                            tag = "chart_mode_area"
                        )
                        ChartModeIconChip(
                            icon = Icons.Filled.BarChart,
                            label = "Batang",
                            selected = chartMode == TrendChartMode.BAR,
                            onClick = { chartMode = TrendChartMode.BAR },
                            tag = "chart_mode_bar"
                        )
                    }
                }

                // Interactive Recharts-style Tooltip Banner
                if (activePoint != null) {
                    AnimatedContent(
                        targetState = activePoint,
                        transitionSpec = {
                            fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                        },
                        label = "tooltip_transition"
                    ) { pt ->
                        Surface(
                            color = Slate900,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chart_tooltip_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pt.fullLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Geser / ketuk grafik",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate400
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    TooltipValueItem(
                                        dotColor = Emerald500,
                                        label = "Masuk",
                                        value = formatRupiah(pt.totalPaid),
                                        modifier = Modifier.weight(1f)
                                    )
                                    TooltipValueItem(
                                        dotColor = Rose600,
                                        label = "Belum Bayar",
                                        value = formatRupiah(pt.unpaidAmount),
                                        modifier = Modifier.weight(1f)
                                    )
                                    TooltipValueItem(
                                        dotColor = Amber600,
                                        label = "Pending",
                                        value = formatRupiah(pt.pendingAmount),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Recharts-style Interactive Monthly Trend Chart
                Surface(
                    color = Slate50,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChartLegendDot(color = Emerald600, label = "Dana Masuk (Lunas)")
                            ChartLegendDot(color = Rose600, label = "Belum Terbayar")
                        }

                        if (trends.isEmpty()) {
                            Text(
                                text = "Belum ada data transaksi untuk ditampilkan pada grafik.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        } else {
                            InteractiveMonthlyTrendCanvas(
                                trends = trends,
                                chartMode = chartMode,
                                selectedIndex = clampedIdx,
                                onSelectIndex = { selectedIdx = it }
                            )
                        }
                    }
                }

                // Animated Donut Composition Card
                Surface(
                    color = Slate50,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CollectionDonutGauge(
                            paidAmount = recap.totalCollectedMonth,
                            pendingAmount = recap.pendingVerificationAmount,
                            unpaidAmount = recap.unpaidOutstandingAmount,
                            percent = recap.collectionPercent
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Komposisi ${monthLabel(state.selectedMonth)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Slate900,
                                fontWeight = FontWeight.ExtraBold
                            )
                            CompositionLegendRow(
                                color = Emerald600,
                                label = "Lunas (Iuran + Hidmat)",
                                value = formatRupiah(recap.totalCollectedMonth)
                            )
                            CompositionLegendRow(
                                color = Amber600,
                                label = "Menunggu Verifikasi",
                                value = formatRupiah(recap.pendingVerificationAmount)
                            )
                            CompositionLegendRow(
                                color = Rose600,
                                label = "Belum Terbayar",
                                value = formatRupiah(recap.unpaidOutstandingAmount)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveMonthlyTrendCanvas(
    trends: List<MonthlyTrendPoint>,
    chartMode: TrendChartMode,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val maxY = remember(trends) {
        val rawMax = trends.maxOfOrNull { maxOf(it.totalPaid, it.unpaidAmount, it.expectedAmount) } ?: 0L
        rawMax.coerceAtLeast(100_000L)
    }

    val revealAnim = remember { Animatable(0f) }
    LaunchedEffect(trends, chartMode) {
        revealAnim.snapTo(0f)
        revealAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 780, easing = FastOutSlowInEasing)
        )
    }

    val midY = maxY / 2L
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Y-Axis Labels
            Column(
                modifier = Modifier
                    .width(50.dp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatCompactRupiah(maxY),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = Slate400,
                    maxLines = 1
                )
                Text(
                    text = formatCompactRupiah(midY),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = Slate400,
                    maxLines = 1
                )
                Text(
                    text = "Rp0",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = Slate400
                )
            }

            // Main Chart Plot Area
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(trends.size) {
                        detectTapGestures { offset ->
                            if (trends.size <= 1) {
                                onSelectIndex(0)
                            } else {
                                val step = size.width.toFloat() / (trends.size - 1).coerceAtLeast(1)
                                val idx = (offset.x / step).roundToInt().coerceIn(0, trends.lastIndex)
                                onSelectIndex(idx)
                            }
                        }
                    }
                    .pointerInput(trends.size) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            if (trends.size <= 1) {
                                onSelectIndex(0)
                            } else {
                                val step = size.width.toFloat() / (trends.size - 1).coerceAtLeast(1)
                                val idx = (change.position.x / step).roundToInt().coerceIn(0, trends.lastIndex)
                                onSelectIndex(idx)
                            }
                        }
                    }
                    .testTag("monthly_trend_chart_canvas")
            ) {
                val padTop = 10.dp.toPx()
                val padBottom = 10.dp.toPx()
                val padH = 8.dp.toPx()
                val plotW = (size.width - padH * 2).coerceAtLeast(1f)
                val plotH = (size.height - padTop - padBottom).coerceAtLeast(1f)
                val progress = revealAnim.value.coerceIn(0f, 1f)

                // Dashed Cartesian Grid Lines (0%, 50%, 100%)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                listOf(0f, 0.5f, 1f).forEach { ratio ->
                    val y = padTop + plotH * ratio
                    drawLine(
                        color = Slate200,
                        start = Offset(padH, y),
                        end = Offset(padH + plotW, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                val count = trends.size
                fun xFor(i: Int): Float = if (count <= 1) {
                    padH + plotW / 2f
                } else {
                    padH + (plotW * i.toFloat() / (count - 1).toFloat())
                }

                fun yFor(value: Long): Float {
                    val norm = (value.toFloat() / maxY.toFloat()).coerceIn(0f, 1f)
                    return padTop + plotH * (1f - norm)
                }

                if (chartMode == TrendChartMode.AREA) {
                    val paidPts = trends.mapIndexed { idx, pt -> Offset(xFor(idx), yFor(pt.totalPaid)) }
                    val unpaidPts = trends.mapIndexed { idx, pt -> Offset(xFor(idx), yFor(pt.unpaidAmount)) }

                    clipRect(right = padH + plotW * progress + 8.dp.toPx()) {
                        // Unpaid Area & Curve (background layer)
                        drawSmoothAreaAndLine(
                            points = unpaidPts,
                            bottomY = padTop + plotH,
                            lineColor = Rose600,
                            fillTopColor = Rose600.copy(alpha = 0.16f),
                            strokePx = 2.dp.toPx()
                        )
                        // Paid Area & Curve (foreground hero layer)
                        drawSmoothAreaAndLine(
                            points = paidPts,
                            bottomY = padTop + plotH,
                            lineColor = Emerald600,
                            fillTopColor = Emerald500.copy(alpha = 0.34f),
                            strokePx = 3.dp.toPx()
                        )
                    }

                    // Active Selected Crosshair & Halo Points
                    val activeX = xFor(selectedIndex.coerceIn(0, trends.lastIndex))
                    drawLine(
                        color = Slate800.copy(alpha = 0.45f),
                        start = Offset(activeX, padTop),
                        end = Offset(activeX, padTop + plotH),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                    val activePaidY = paidPts.getOrNull(selectedIndex)?.y ?: (padTop + plotH)
                    val activeUnpaidY = unpaidPts.getOrNull(selectedIndex)?.y ?: (padTop + plotH)

                    // Rose active point
                    drawCircle(color = Rose600.copy(alpha = 0.22f), radius = 9.dp.toPx(), center = Offset(activeX, activeUnpaidY))
                    drawCircle(color = Rose600, radius = 4.5.dp.toPx(), center = Offset(activeX, activeUnpaidY))
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(activeX, activeUnpaidY))

                    // Emerald active point
                    drawCircle(color = Emerald500.copy(alpha = 0.28f), radius = 10.dp.toPx(), center = Offset(activeX, activePaidY))
                    drawCircle(color = Emerald600, radius = 5.5.dp.toPx(), center = Offset(activeX, activePaidY))
                    drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(activeX, activePaidY))
                } else {
                    val slotW = plotW / count.coerceAtLeast(1)
                    val pairW = (slotW * 0.72f).coerceAtMost(32.dp.toPx())
                    val barW = (pairW * 0.44f).coerceAtLeast(4.dp.toPx())
                    val gap = (pairW * 0.12f).coerceAtLeast(2.dp.toPx())

                    trends.forEachIndexed { idx, pt ->
                        val centerX = padH + slotW * (idx + 0.5f)
                        if (idx == selectedIndex) {
                            drawRoundRect(
                                color = Emerald500.copy(alpha = 0.12f),
                                topLeft = Offset(centerX - slotW * 0.44f, padTop),
                                size = Size(slotW * 0.88f, plotH),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }

                        val paidH = ((pt.totalPaid.toFloat() / maxY.toFloat()).coerceIn(0f, 1f)) * plotH * progress
                        val unpaidH = ((pt.unpaidAmount.toFloat() / maxY.toFloat()).coerceIn(0f, 1f)) * plotH * progress

                        val leftX = centerX - (barW + gap / 2f)
                        val rightX = centerX + gap / 2f

                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(Emerald500, Emerald700)),
                            topLeft = Offset(leftX, padTop + plotH - paidH),
                            size = Size(barW, paidH.coerceAtLeast(2.dp.toPx())),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(Rose600, Rose600.copy(alpha = 0.75f))),
                            topLeft = Offset(rightX, padTop + plotH - unpaidH),
                            size = Size(barW, unpaidH.coerceAtLeast(2.dp.toPx())),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }
        }

        // X-Axis Month Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 50.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val step = if (trends.size > 7) 2 else 1
            trends.forEachIndexed { idx, pt ->
                val showLabel = idx == selectedIndex || idx % step == 0 || idx == trends.lastIndex
                Text(
                    text = if (showLabel) pt.shortLabel else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (idx == selectedIndex) Emerald700 else Slate400,
                    fontWeight = if (idx == selectedIndex) FontWeight.Black else FontWeight.Medium,
                    modifier = Modifier.clickable { onSelectIndex(idx) }
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmoothAreaAndLine(
    points: List<Offset>,
    bottomY: Float,
    lineColor: Color,
    fillTopColor: Color,
    strokePx: Float
) {
    if (points.isEmpty()) return
    val linePath = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 0 until points.lastIndex) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val ctrlX = (p0.x + p1.x) / 2f
            cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
        }
    }
    val areaPath = Path().apply {
        addPath(linePath)
        lineTo(points.last().x, bottomY)
        lineTo(points.first().x, bottomY)
        close()
    }
    drawPath(
        path = areaPath,
        brush = Brush.verticalGradient(
            colors = listOf(fillTopColor, Color.Transparent),
            startY = 0f,
            endY = bottomY
        )
    )
    drawPath(
        path = linePath,
        color = lineColor,
        style = Stroke(width = strokePx, cap = StrokeCap.Round)
    )
}

@Composable
private fun CollectionDonutGauge(
    paidAmount: Long,
    pendingAmount: Long,
    unpaidAmount: Long,
    percent: Int
) {
    val total = (paidAmount + pendingAmount + unpaidAmount).coerceAtLeast(1L).toFloat()
    val sweepAnim = remember { Animatable(0f) }
    LaunchedEffect(paidAmount, pendingAmount, unpaidAmount) {
        sweepAnim.snapTo(0f)
        sweepAnim.animateTo(1f, animationSpec = tween(750, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier.size(104.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            // Track ring
            drawArc(
                color = Slate200,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            val prog = sweepAnim.value
            val paidSweep = (paidAmount / total) * 360f * prog
            val pendingSweep = (pendingAmount / total) * 360f * prog
            val unpaidSweep = (unpaidAmount / total) * 360f * prog

            var start = -90f
            if (paidSweep > 0.5f) {
                drawArc(
                    color = Emerald600,
                    startAngle = start,
                    sweepAngle = paidSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                start += paidSweep
            }
            if (pendingSweep > 0.5f) {
                drawArc(
                    color = Amber600,
                    startAngle = start,
                    sweepAngle = pendingSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                start += pendingSweep
            }
            if (unpaidSweep > 0.5f) {
                drawArc(
                    color = Rose600,
                    startAngle = start,
                    sweepAngle = unpaidSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedPercentText(
                targetPercent = percent,
                style = MaterialTheme.typography.titleMedium,
                color = Slate900
            )
            Text(
                text = "Lunas",
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )
        }
    }
}

@Composable
private fun ChartToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        color = if (selected) Emerald700 else Slate100,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color.White else Slate600,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ChartModeIconChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        color = if (selected) DanaBlue else Slate100,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else Slate600,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color.White else Slate600,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Slate600
        )
    }
}

@Composable
private fun TooltipValueItem(
    dotColor: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Slate400
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompositionLegendRow(
    color: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Slate600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
            color = Slate900,
            fontWeight = FontWeight.Bold
        )
    }
}
