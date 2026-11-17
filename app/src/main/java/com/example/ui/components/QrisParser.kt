package com.example.ui.components

import com.example.data.model.formatRupiah

data class ScannedQrisInfo(
    val rawPayload: String,
    val isEmvQris: Boolean,
    val merchantName: String,
    val merchantCity: String? = null,
    val nmidOrId: String? = null,
    val amountRupiah: Long? = null,
    val referenceLabel: String? = null,
    val providerLabel: String? = null
) {
    fun formatConfirmationNote(): String {
        val parts = mutableListOf<String>()
        parts.add(if (isEmvQris) "QRIS: $merchantName" else "Scan QR: $merchantName")
        if (!providerLabel.isNullOrBlank()) {
            parts.add(providerLabel)
        }
        if (amountRupiah != null && amountRupiah > 0L) {
            parts.add(formatRupiah(amountRupiah))
        }
        if (!referenceLabel.isNullOrBlank()) {
            parts.add("Ref $referenceLabel")
        }
        return parts.joinToString(" • ").take(120)
    }
}

object QrisParser {

    fun parse(rawValue: String): ScannedQrisInfo {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) {
            return ScannedQrisInfo(
                rawPayload = rawValue,
                isEmvQris = false,
                merchantName = "Kode QR Kosong"
            )
        }

        val rootTags = parseTlv(trimmed)
        val looksLikeEmv = rootTags != null &&
            rootTags["00"] == "01" &&
            (rootTags.containsKey("59") || (26..51).any { rootTags.containsKey(it.toString().padStart(2, '0')) })

        if (looksLikeEmv && rootTags != null) {
            val merchantName = rootTags["59"]?.trim()?.takeIf { it.isNotEmpty() } ?: "Merchant QRIS"
            val merchantCity = rootTags["60"]?.trim()?.takeIf { it.isNotEmpty() }
            val amountRupiah = rootTags["54"]
                ?.trim()
                ?.replace(",", ".")
                ?.toDoubleOrNull()
                ?.toLong()
                ?.takeIf { it > 0L }

            var nmidOrId: String? = rootTags["51"]?.let { parseTlv(it)?.get("02") }
            var providerLabel: String? = null

            for (tagNum in 26..51) {
                val key = tagNum.toString().padStart(2, '0')
                val subRaw = rootTags[key] ?: continue
                val sub = parseTlv(subRaw) ?: continue
                val gui = sub["00"]?.uppercase()
                if (!gui.isNullOrBlank() && providerLabel == null) {
                    providerLabel = when {
                        gui.contains("DANA") -> "DANA"
                        gui.contains("GOPAY") || gui.contains("GOJEK") -> "GoPay"
                        gui.contains("OVO") -> "OVO"
                        gui.contains("SHOPEE") -> "ShopeePay"
                        gui.contains("QRIS") -> "QRIS"
                        else -> gui.substringBefore('.').takeIf { it.isNotBlank() } ?: gui
                    }
                }
                if (nmidOrId.isNullOrBlank()) {
                    nmidOrId = sub["02"] ?: sub["01"]
                }
            }

            val additionalFields = rootTags["62"]?.let { parseTlv(it) }
            val refLabel = additionalFields?.get("05")?.trim()?.takeIf { it.isNotEmpty() }
                ?: additionalFields?.get("01")?.trim()?.takeIf { it.isNotEmpty() }

            return ScannedQrisInfo(
                rawPayload = trimmed,
                isEmvQris = true,
                merchantName = merchantName,
                merchantCity = merchantCity,
                nmidOrId = nmidOrId,
                amountRupiah = amountRupiah,
                referenceLabel = refLabel,
                providerLabel = providerLabel ?: "QRIS"
            )
        }

        // Fallback for DANA deep links, URLs, or plain text QR codes
        val displayLabel = when {
            trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("http://", ignoreCase = true) -> {
                val host = trimmed.substringAfter("://").substringBefore("/")
                if (host.contains("dana", ignoreCase = true)) {
                    "Tautan Pembayaran DANA"
                } else {
                    host.ifBlank { "Tautan QR" }
                }
            }
            else -> trimmed.take(40)
        }

        val provider = if (trimmed.contains("dana", ignoreCase = true)) "DANA" else null

        return ScannedQrisInfo(
            rawPayload = trimmed,
            isEmvQris = false,
            merchantName = displayLabel,
            referenceLabel = trimmed.takeLast(8).takeIf { trimmed.length > 8 },
            providerLabel = provider
        )
    }

    private fun parseTlv(payload: String): Map<String, String>? {
        val result = linkedMapOf<String, String>()
        var index = 0
        while (index + 4 <= payload.length) {
            val tag = payload.substring(index, index + 2)
            if (!tag.all { it.isDigit() }) return null
            val lenStr = payload.substring(index + 2, index + 4)
            val length = lenStr.toIntOrNull() ?: return null
            val valueStart = index + 4
            val valueEnd = valueStart + length
            if (valueEnd > payload.length) return null
            result[tag] = payload.substring(valueStart, valueEnd)
            index = valueEnd
        }
        return if (index == payload.length && result.isNotEmpty()) result else null
    }
}
