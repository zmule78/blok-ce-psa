package com.example.data.export

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.BudgetPlan
import com.example.data.model.FinanceEntry
import com.example.data.model.FinanceSummary
import com.example.data.model.MonthlyKasPoint
import com.example.data.model.formatRupiah
import com.example.data.model.monthLabel
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FinanceReportExporter {

    private fun csvEscape(raw: String): String = "\"" + raw.replace("\"", "\"\"") + "\""

    fun buildFinancialSummaryCsv(
        selectedMonth: String,
        blokName: String,
        summary: FinanceSummary,
        kasTimeline: List<MonthlyKasPoint>,
        entries: List<FinanceEntry>,
        budgetPlans: List<BudgetPlan>,
        adminName: String
    ): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        val periodLabel = monthLabel(selectedMonth)
        val generatedAt = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date())

        sb.appendLine(csvEscape("LAPORAN RINGKASAN KEUANGAN & KAS $blokName"))
        sb.appendLine("Periode Laporan,${csvEscape(periodLabel)}")
        sb.appendLine("Dicetak Oleh,${csvEscape(adminName.ifBlank { "Admin Utama" })}")
        sb.appendLine("Waktu Ekspor,${csvEscape(generatedAt)}")
        sb.appendLine()

        sb.appendLine("Metrik Ringkasan Kas,Nilai (IDR)")
        sb.appendLine("Saldo Kas Blok Saat Ini,${summary.currentKasBalance}")
        sb.appendLine("Saldo Awal Kas Blok (All Time),${summary.totalKasBlokInitial}")
        sb.appendLine("Total Iuran & Hidmat Lunas (All Time),${summary.totalIuranPaidAllTime}")
        sb.appendLine("Total Kas Masuk Manual (All Time),${summary.totalManualMasuk}")
        sb.appendLine("Total Kas Keluar (All Time),${summary.totalKeluar}")
        sb.appendLine("Pemasukan Bulan $periodLabel,${summary.monthMasukTotal}")
        sb.appendLine("Pengeluaran Bulan $periodLabel,${summary.monthKeluarTotal}")
        sb.appendLine("Total Rencana Anggaran (RAB),${summary.totalPlannedBudget}")
        sb.appendLine("Total Realisasi Anggaran (RAB),${summary.totalRealizedBudget}")
        sb.appendLine()

        sb.appendLine("Tren Saldo Kas 6 Bulan,Periode,Kas Awal,Masuk,Keluar,Delta Bersih,Saldo Akhir")
        kasTimeline.forEach { pt ->
            sb.appendLine(
                listOf(
                    csvEscape(pt.shortLabel),
                    csvEscape(pt.fullLabel),
                    pt.kasInitialAmount.toString(),
                    pt.masukAmount.toString(),
                    pt.keluarAmount.toString(),
                    pt.netDelta.toString(),
                    pt.runningBalance.toString()
                ).joinToString(",")
            )
        }
        sb.appendLine()

        sb.appendLine("Catatan Mutasi Keuangan (${entries.size})")
        sb.appendLine("Tanggal,Periode,Jenis,Kategori,Judul,Nominal,Catatan,Dicatat Oleh")
        entries.forEach { entry ->
            val kindLabel = when (entry.kind) {
                "keluar" -> "Kas Keluar"
                "kas_blok" -> "Kas Blok"
                else -> "Kas Masuk"
            }
            sb.appendLine(
                listOf(
                    csvEscape(entry.dateLabel.ifBlank { "-" }),
                    csvEscape(monthLabel(entry.month)),
                    csvEscape(kindLabel),
                    csvEscape(entry.category),
                    csvEscape(entry.title),
                    entry.amount.toString(),
                    csvEscape(entry.note),
                    csvEscape(entry.createdByName)
                ).joinToString(",")
            )
        }
        sb.appendLine()

        sb.appendLine("Rencana Anggaran / RAB (${budgetPlans.size})")
        sb.appendLine("Periode,Program,Kategori,Target Anggaran,Realisasi,Progres %,Status,Catatan")
        budgetPlans.forEach { plan ->
            val statusText = when (plan.status) {
                "completed" -> "Selesai"
                "in_progress" -> "Berjalan"
                else -> "Direncanakan"
            }
            sb.appendLine(
                listOf(
                    csvEscape(monthLabel(plan.month)),
                    csvEscape(plan.title),
                    csvEscape(plan.category),
                    plan.plannedAmount.toString(),
                    plan.realizedAmount.toString(),
                    "${plan.progressPercent}%",
                    csvEscape(statusText),
                    csvEscape(plan.note)
                ).joinToString(",")
            )
        }

        return sb.toString()
    }

    fun writeFinancialSummaryPdf(
        outputStream: OutputStream,
        selectedMonth: String,
        blokName: String,
        summary: FinanceSummary,
        kasTimeline: List<MonthlyKasPoint>,
        entries: List<FinanceEntry>,
        budgetPlans: List<BudgetPlan>,
        adminName: String
    ) {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val contentWidth = pageWidth - margin * 2f

        val doc = PdfDocument()
        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin

        val headerBgPaint = Paint().apply { color = Color.rgb(6, 78, 59) }
        val cardBgPaint = Paint().apply { color = Color.rgb(241, 245, 249) }
        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(209, 250, 229)
            textSize = 9.5f
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 9f
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
        }
        val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val emeraldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(4, 120, 87)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(225, 29, 72)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val generatedAt = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date())
        val periodLabel = monthLabel(selectedMonth)

        fun drawFooter() {
            canvas.drawLine(margin, pageHeight - 36f, pageWidth - margin, pageHeight - 36f, borderPaint)
            canvas.drawText(
                "$blokName • Diekspor oleh ${adminName.ifBlank { "Admin Utama" }} • $generatedAt",
                margin,
                pageHeight - 20f,
                labelPaint
            )
            val pageStr = "Hal $pageNumber"
            canvas.drawText(
                pageStr,
                pageWidth - margin - labelPaint.measureText(pageStr),
                pageHeight - 20f,
                labelPaint
            )
        }

        fun ensureSpace(neededHeight: Float) {
            if (y + neededHeight > pageHeight - 50f) {
                drawFooter()
                doc.finishPage(page)
                pageNumber++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = margin
            }
        }

        // 1. Header Banner
        canvas.drawRoundRect(margin, y, pageWidth - margin, y + 66f, 12f, 12f, headerBgPaint)
        canvas.drawText("LAPORAN RINGKASAN KEUANGAN & KAS", margin + 14f, y + 26f, titlePaint)
        canvas.drawText(
            "$blokName • Periode $periodLabel",
            margin + 14f,
            y + 42f,
            subtitlePaint
        )
        canvas.drawText(
            "Saldo Kas Saat Ini: ${formatRupiah(summary.currentKasBalance)}",
            margin + 14f,
            y + 56f,
            subtitlePaint
        )
        y += 82f

        // 2. KPI Summary Box
        canvas.drawRoundRect(margin, y, pageWidth - margin, y + 92f, 10f, 10f, cardBgPaint)
        canvas.drawRoundRect(margin, y, pageWidth - margin, y + 92f, 10f, 10f, borderPaint)
        val col2X = margin + contentWidth * 0.52f
        canvas.drawText("Saldo Kas Awal (All Time):", margin + 12f, y + 20f, labelPaint)
        canvas.drawText(formatRupiah(summary.totalKasBlokInitial), margin + 12f, y + 33f, boldBodyPaint)
        canvas.drawText("Masuk Bulan Ini (Manual + Iuran):", col2X, y + 20f, labelPaint)
        canvas.drawText(formatRupiah(summary.monthMasukTotal), col2X, y + 33f, emeraldPaint)

        canvas.drawText("Iuran & Hidmat Lunas (All Time):", margin + 12f, y + 52f, labelPaint)
        canvas.drawText(formatRupiah(summary.totalIuranPaidAllTime), margin + 12f, y + 65f, emeraldPaint)
        canvas.drawText("Keluar Bulan Ini:", col2X, y + 52f, labelPaint)
        canvas.drawText(formatRupiah(summary.monthKeluarTotal), col2X, y + 65f, rosePaint)

        canvas.drawText(
            "RAB: Realisasi ${formatRupiah(summary.totalRealizedBudget)} dari Target ${formatRupiah(summary.totalPlannedBudget)}",
            margin + 12f,
            y + 82f,
            boldBodyPaint
        )
        y += 108f

        // 3. 6-Month Kas Timeline
        ensureSpace(40f + kasTimeline.size * 18f)
        canvas.drawText("Ringkasan Tren Saldo Kas (${kasTimeline.size} Bulan)", margin, y, sectionPaint)
        y += 14f
        canvas.drawRect(margin, y, pageWidth - margin, y + 18f, cardBgPaint)
        canvas.drawText("Bulan", margin + 6f, y + 13f, boldBodyPaint)
        canvas.drawText("Masuk", margin + 125f, y + 13f, boldBodyPaint)
        canvas.drawText("Keluar", margin + 235f, y + 13f, boldBodyPaint)
        canvas.drawText("Delta", margin + 335f, y + 13f, boldBodyPaint)
        canvas.drawText("Saldo Akhir", margin + 430f, y + 13f, boldBodyPaint)
        y += 22f

        kasTimeline.forEach { pt ->
            ensureSpace(20f)
            canvas.drawText(pt.fullLabel.take(18), margin + 6f, y + 11f, bodyPaint)
            canvas.drawText(formatRupiah(pt.masukAmount), margin + 125f, y + 11f, emeraldPaint)
            canvas.drawText(formatRupiah(pt.keluarAmount), margin + 235f, y + 11f, rosePaint)
            val deltaPrefix = if (pt.netDelta >= 0) "+" else ""
            canvas.drawText("$deltaPrefix${formatRupiah(pt.netDelta)}", margin + 335f, y + 11f, bodyPaint)
            canvas.drawText(formatRupiah(pt.runningBalance), margin + 430f, y + 11f, boldBodyPaint)
            y += 16f
        }
        y += 14f

        // 4. Mutasi Keuangan Table
        ensureSpace(42f)
        canvas.drawText("Catatan Mutasi Keuangan (${entries.size})", margin, y, sectionPaint)
        y += 14f
        if (entries.isEmpty()) {
            canvas.drawText("Belum ada mutasi keuangan tercatat.", margin, y + 12f, labelPaint)
            y += 24f
        } else {
            canvas.drawRect(margin, y, pageWidth - margin, y + 18f, cardBgPaint)
            canvas.drawText("Tanggal", margin + 6f, y + 13f, boldBodyPaint)
            canvas.drawText("Jenis", margin + 88f, y + 13f, boldBodyPaint)
            canvas.drawText("Kategori", margin + 150f, y + 13f, boldBodyPaint)
            canvas.drawText("Judul", margin + 235f, y + 13f, boldBodyPaint)
            canvas.drawText("Nominal", margin + 425f, y + 13f, boldBodyPaint)
            y += 22f

            entries.forEach { entry ->
                ensureSpace(20f)
                val kindLabel = when (entry.kind) {
                    "keluar" -> "KELUAR"
                    "kas_blok" -> "KAS BLOK"
                    else -> "MASUK"
                }
                val amountPaint = if (entry.kind == "keluar") rosePaint else emeraldPaint
                val amountStr = (if (entry.kind == "keluar") "- " else "+ ") + formatRupiah(entry.amount)
                canvas.drawText(entry.dateLabel.ifBlank { entry.month }.take(13), margin + 6f, y + 11f, bodyPaint)
                canvas.drawText(kindLabel, margin + 88f, y + 11f, boldBodyPaint)
                canvas.drawText(entry.category.take(14), margin + 150f, y + 11f, bodyPaint)
                canvas.drawText(entry.title.take(30), margin + 235f, y + 11f, bodyPaint)
                canvas.drawText(amountStr, margin + 425f, y + 11f, amountPaint)
                y += 17f
            }
        }
        y += 14f

        // 5. Budget Plans Table
        ensureSpace(42f)
        canvas.drawText("Rencana Anggaran / RAB (${budgetPlans.size})", margin, y, sectionPaint)
        y += 14f
        if (budgetPlans.isEmpty()) {
            canvas.drawText("Belum ada rencana anggaran tercatat.", margin, y + 12f, labelPaint)
            y += 24f
        } else {
            canvas.drawRect(margin, y, pageWidth - margin, y + 18f, cardBgPaint)
            canvas.drawText("Program", margin + 6f, y + 13f, boldBodyPaint)
            canvas.drawText("Kategori", margin + 180f, y + 13f, boldBodyPaint)
            canvas.drawText("Target", margin + 275f, y + 13f, boldBodyPaint)
            canvas.drawText("Realisasi", margin + 375f, y + 13f, boldBodyPaint)
            canvas.drawText("Progres", margin + 468f, y + 13f, boldBodyPaint)
            y += 22f

            budgetPlans.forEach { plan ->
                ensureSpace(20f)
                canvas.drawText(plan.title.take(28), margin + 6f, y + 11f, bodyPaint)
                canvas.drawText(plan.category.take(15), margin + 180f, y + 11f, bodyPaint)
                canvas.drawText(formatRupiah(plan.plannedAmount), margin + 275f, y + 11f, bodyPaint)
                canvas.drawText(formatRupiah(plan.realizedAmount), margin + 375f, y + 11f, emeraldPaint)
                canvas.drawText("${plan.progressPercent}%", margin + 468f, y + 11f, boldBodyPaint)
                y += 17f
            }
        }

        drawFooter()
        doc.finishPage(page)
        try {
            doc.writeTo(outputStream)
        } finally {
            doc.close()
        }
    }
}
