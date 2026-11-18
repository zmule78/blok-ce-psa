package com.example.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.util.Locale

data class ParsedReceiptInfo(
    val rawText: String,
    val detectedAmount: Long?,
    val detectedMethod: String?,
    val summaryText: String
)

object ReceiptOcrParser {
    private val rupiahsRegex = Regex(
        """(?i)(?:rp\.?\s*|idr\s*)(\d{1,3}(?:[.,]\d{3})+|\d{4,9})(?:[.,]\d{2})?"""
    )
    private val plainTotalLineRegex = Regex(
        """(?i)(?:total|nominal|jumlah|bayar|transfer)\D{0,14}(\d{1,3}(?:[.,]\d{3})+|\d{4,9})"""
    )

    fun parse(rawText: String): ParsedReceiptInfo {
        val cleanedLines = rawText
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val amountCandidates = mutableListOf<Long>()
        rupiahsRegex.findAll(rawText).forEach { match ->
            parseNumberDigits(match.groupValues[1])?.let { amountCandidates.add(it) }
        }
        if (amountCandidates.isEmpty()) {
            plainTotalLineRegex.findAll(rawText).forEach { match ->
                parseNumberDigits(match.groupValues[1])?.let { amountCandidates.add(it) }
            }
        }
        val detectedAmount = amountCandidates
            .filter { it in 1_000L..50_000_000L }
            .maxOrNull()

        val upper = rawText.uppercase(Locale.ROOT)
        val detectedMethod = when {
            upper.contains("QRIS") -> "DANA QRIS"
            upper.contains("DANA") -> "Transfer DANA"
            upper.contains("BCA") || upper.contains("BRI") || upper.contains("MANDIRI") ||
                upper.contains("BNI") || upper.contains("BSI") || upper.contains("TRANSFER") -> "Transfer Bank"
            else -> null
        }

        val summaryLines = cleanedLines.take(5).joinToString(" • ")
        val summaryText = buildString {
            if (detectedAmount != null) {
                append("Nominal terdeteksi Rp ")
                append(detectedAmount)
                append(" • ")
            }
            if (!detectedMethod.isNullOrBlank()) {
                append(detectedMethod)
                append(" • ")
            }
            append(summaryLines)
        }.trim().trimEnd('•', ' ').take(500)

        return ParsedReceiptInfo(
            rawText = rawText.take(2000),
            detectedAmount = detectedAmount,
            detectedMethod = detectedMethod,
            summaryText = summaryText
        )
    }

    private fun parseNumberDigits(raw: String): Long? {
        val digits = raw.filter { it.isDigit() }
        return digits.toLongOrNull()
    }

    fun compressBitmapToDataUri(bitmap: Bitmap, maxDim: Int = 720): String {
        val scaled = scaleBitmapDown(bitmap, maxDim)
        var quality = 78
        val baos = ByteArrayOutputStream()
        do {
            baos.reset()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            quality -= 10
        } while (baos.size() > 140_000 && quality >= 28)

        val encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$encoded"
    }

    fun decodeUriBitmap(context: Context, uri: Uri, maxDim: Int = 960): Bitmap? {
        return try {
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            val rotation = try {
                context.contentResolver.openInputStream(uri)?.use { exifStream ->
                    when (
                        ExifInterface(exifStream).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    ) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } ?: 0f
            } catch (_: Exception) {
                0f
            }

            val rotated = if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            } else {
                rawBitmap
            }
            scaleBitmapDown(rotated, maxDim)
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        return Bitmap.createScaledBitmap(
            bitmap,
            (w * ratio).toInt().coerceAtLeast(1),
            (h * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}
