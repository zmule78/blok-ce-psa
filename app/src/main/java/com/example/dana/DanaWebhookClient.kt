package com.example.dana

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object DanaWebhookSigner {
    private val secureRandom = SecureRandom()

    fun generateSecretHex(byteCount: Int = 24): String {
        val bytes = ByteArray(byteCount.coerceIn(12, 64))
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun computeHmacSha256Hex(payload: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)
        val digest = mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun buildPingJson(source: String = "webhook_test_ping"): String {
        val cleanSource = escapeJson(source)
        return """{"eventType":"PING","source":"$cleanSource","sentAtMillis":${System.currentTimeMillis()}}"""
    }

    fun buildNotificationRelayJson(
        referenceId: String,
        amount: Long,
        senderHint: String,
        isQris: Boolean,
        rawText: String
    ): String {
        val cleanRef = referenceId.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(128)
        val source = if (isQris) "android_dana_qris_relay" else "android_dana_notif_relay"
        return buildString {
            append('{')
            append("\"referenceId\":\"").append(escapeJson(cleanRef)).append("\",")
            append("\"source\":\"").append(source).append("\",")
            append("\"status\":\"SUCCESS\",")
            append("\"amount\":").append(amount.coerceAtLeast(0L)).append(',')
            append("\"senderName\":\"").append(escapeJson(senderHint.take(120))).append("\",")
            append("\"rawText\":\"").append(escapeJson(rawText.take(1500))).append("\"")
            append('}')
        }
    }

    fun buildCurlTemplate(webhookUrl: String, secret: String): String {
        val targetUrl = webhookUrl.trim().ifBlank { "https://WEBHOOK_ANDA/webhook/dana" }
        val sampleBody =
            """{"referenceId":"DANA-TEST-001","source":"dana_merchant_webhook","amount":50000,"senderName":"Budi Santoso","houseHint":"CE-05","status":"SUCCESS"}"""
        val sigHeader = if (secret.isNotBlank()) {
            " -H \"X-DANA-Signature: sha256=${computeHmacSha256Hex(sampleBody, secret)}\""
        } else {
            ""
        }
        return "curl -X POST \"$targetUrl\" -H \"Content-Type: application/json\"$sigHeader -d '$sampleBody'"
    }

    private fun escapeJson(value: String): String {
        val sb = StringBuilder(value.length + 8)
        for (ch in value) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> {
                    if (ch.code < 0x20) {
                        sb.append("\\u%04x".format(ch.code))
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }
        return sb.toString()
    }
}

data class WebhookDispatchResult(
    val statusCode: Int,
    val responseBody: String
)

object DanaWebhookDispatcher {

    suspend fun sendPing(webhookUrl: String, secret: String): Result<WebhookDispatchResult> =
        sendSignedWebhook(
            webhookUrl = webhookUrl,
            secret = secret,
            jsonPayload = DanaWebhookSigner.buildPingJson()
        )

    suspend fun sendSignedWebhook(
        webhookUrl: String,
        secret: String,
        jsonPayload: String
    ): Result<WebhookDispatchResult> = withContext(Dispatchers.IO) {
        val trimmedUrl = webhookUrl.trim()
        if (!trimmedUrl.startsWith("https://", ignoreCase = true) &&
            !trimmedUrl.startsWith("http://", ignoreCase = true)
        ) {
            return@withContext Result.failure(
                IllegalArgumentException("URL Webhook harus diawali https:// atau http://")
            )
        }

        var conn: HttpURLConnection? = null
        try {
            val url = URL(trimmedUrl)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 12_000
                doOutput = true
                instanceFollowRedirects = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "BlokCE-PSA-Android/1.0")
                val cleanSecret = secret.trim()
                if (cleanSecret.isNotEmpty()) {
                    val sig = DanaWebhookSigner.computeHmacSha256Hex(jsonPayload, cleanSecret)
                    setRequestProperty("X-DANA-Signature", "sha256=$sig")
                    setRequestProperty("X-Webhook-Secret", cleanSecret)
                }
            }

            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(jsonPayload)
                writer.flush()
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = if (stream != null) {
                BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { reader ->
                    reader.readText().take(1000)
                }
            } else {
                ""
            }

            if (code in 200..299) {
                Result.success(WebhookDispatchResult(statusCode = code, responseBody = body))
            } else {
                Result.failure(
                    IllegalStateException("Webhook merespons HTTP $code${if (body.isNotBlank()) ": $body" else ""}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }
}
