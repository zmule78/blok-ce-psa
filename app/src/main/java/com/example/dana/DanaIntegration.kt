package com.example.dana

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

data class ParsedDanaNotification(
    val amount: Long,
    val senderHint: String,
    val isIncomingCredit: Boolean,
    val isQris: Boolean
)

object DanaNotificationParser {
    const val DANA_PACKAGE_NAME = "id.dana"

    private val outgoingKeywords = listOf(
        "berhasil dikirim",
        "kamu mengirim",
        "anda mengirim",
        "pembayaran ke",
        "bayar ke",
        "kirim uang ke",
        "transfer keluar",
        "beli pulsa",
        "bayar tagihan"
    )

    private val incomingKeywords = listOf(
        "saldo masuk",
        "menerima",
        "diterima",
        "uang masuk",
        "transfer masuk",
        "terima transfer",
        "terima dana",
        "pembayaran qris",
        "qris berhasil",
        "masuk sebesar",
        "top up masuk"
    )

    private val rupiahRegex = Regex(
        pattern = "(?:rp\\.?|idr)\\s*([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{4,9})(?:[.,]00)?",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val senderFromRegex = Regex(
        pattern = "(?:dari|from|oleh)\\s+([A-Za-z0-9 .,'_-]{2,40})",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    fun parse(title: String, text: String): ParsedDanaNotification? {
        val combined = "$title $text".trim()
        if (combined.isBlank()) return null
        val lower = combined.lowercase()

        val isOutgoing = outgoingKeywords.any { lower.contains(it) }
        val isIncoming = incomingKeywords.any { lower.contains(it) }
        if (!isIncoming || isOutgoing) return null

        val match = rupiahRegex.find(combined) ?: return null
        val rawDigits = match.groupValues[1].replace(".", "").replace(",", "")
        val amount = rawDigits.toLongOrNull() ?: return null
        if (amount <= 0L) return null

        val sender = senderFromRegex.find(combined)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.trimEnd('.', ',', '!', ')')
            .orEmpty()

        return ParsedDanaNotification(
            amount = amount,
            senderHint = sender,
            isIncomingCredit = true,
            isQris = lower.contains("qris")
        )
    }
}

object DanaLiveBridge {
    private const val PREFS_NAME = "blokce_dana_live_prefs"
    private const val KEY_AUTO_VERIFY = "auto_verify_enabled"
    private const val KEY_WEBHOOK_URL = "dana_webhook_url"
    private const val KEY_WEBHOOK_SECRET = "dana_webhook_secret"
    private const val KEY_WEBHOOK_RELAY_ENABLED = "dana_webhook_relay_enabled"

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context.applicationContext)
        if (context.packageName in enabledPackages) return true

        val flat = Settings.Secure.getString(
            context.applicationContext.contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        val cn = ComponentName(context.applicationContext, DanaNotificationListenerService::class.java)
        return flat.split(':').any { entry ->
            entry.equals(cn.flattenToString(), ignoreCase = true) ||
                entry.equals(cn.flattenToShortString(), ignoreCase = true)
        }
    }

    fun isAutoVerifyEnabled(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_VERIFY, true)

    fun setAutoVerifyEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_VERIFY, enabled)
            .apply()
    }

    fun saveWebhookLocalPrefs(
        context: Context,
        webhookUrl: String,
        webhookSecret: String,
        relayEnabled: Boolean
    ) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WEBHOOK_URL, webhookUrl.trim())
            .putString(KEY_WEBHOOK_SECRET, webhookSecret.trim())
            .putBoolean(KEY_WEBHOOK_RELAY_ENABLED, relayEnabled)
            .apply()
    }

    fun getWebhookUrl(context: Context): String =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WEBHOOK_URL, "")
            .orEmpty()

    fun getWebhookSecret(context: Context): String =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WEBHOOK_SECRET, "")
            .orEmpty()

    fun isWebhookRelayEnabled(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_WEBHOOK_RELAY_ENABLED, false)

    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun openDanaApp(context: Context): Boolean {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(DanaNotificationParser.DANA_PACKAGE_NAME)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return true
        }
        return try {
            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=${DanaNotificationParser.DANA_PACKAGE_NAME}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
            false
        } catch (_: ActivityNotFoundException) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=${DanaNotificationParser.DANA_PACKAGE_NAME}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            false
        }
    }
}
