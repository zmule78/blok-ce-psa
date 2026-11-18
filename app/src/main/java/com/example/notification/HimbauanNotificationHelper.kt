package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Payment
import com.example.data.model.Resident
import com.example.data.model.SystemSettings
import com.example.data.model.formatRupiah
import com.example.data.model.monthLabel

data class PendingNotificationPayload(
    val alertKey: String,
    val title: String,
    val message: String,
    val pendingCount: Int,
    val pendingAmount: Long,
    val targetTabName: String
)

fun evaluateResidentPendingNotification(
    resident: Resident?,
    monthPayments: List<Payment>,
    currentMonth: String,
    dayKey: String,
    settings: SystemSettings
): PendingNotificationPayload? {
    if (resident == null || resident.adminRole) return null

    val pendingList = monthPayments.filter {
        it.userId == resident.id &&
            it.month == currentMonth &&
            it.status.equals("pending", ignoreCase = true)
    }
    if (pendingList.isEmpty()) return null

    val totalPending = pendingList.sumOf { p ->
        if (p.amount > 0L) p.amount
        else if (p.type == "hidmat") settings.hidmatAmount
        else settings.iuranAmount
    }
    val itemLabels = pendingList
        .map { if (it.type == "hidmat") "Hidmat" else "Iuran" }
        .distinct()
        .joinToString(" & ")

    val periodName = monthLabel(currentMonth)
    val title = "Pembayaran Pending • $periodName"
    val body = "Tagihan $itemLabels (${resident.house}) sebesar ${formatRupiah(totalPending)} " +
        "masih berstatus PENDING bulan ini. Segera selesaikan atau konfirmasi agar tercatat lunas."

    return PendingNotificationPayload(
        alertKey = "pending_res_${currentMonth}_${dayKey}_${resident.id}_${totalPending}_${pendingList.size}",
        title = title,
        message = body,
        pendingCount = pendingList.size,
        pendingAmount = totalPending,
        targetTabName = "PAY"
    )
}

fun evaluateAdminPendingOverviewNotification(
    monthPayments: List<Payment>,
    currentMonth: String,
    dayKey: String
): PendingNotificationPayload? {
    val pendingList = monthPayments.filter {
        it.month == currentMonth &&
            it.status.equals("pending", ignoreCase = true)
    }
    if (pendingList.isEmpty()) return null

    val distinctResidents = pendingList.map { it.userId }.distinct().size
    val totalPending = pendingList.sumOf { it.amount }
    val periodName = monthLabel(currentMonth)

    return PendingNotificationPayload(
        alertKey = "pending_adm_${currentMonth}_${dayKey}_${distinctResidents}_${totalPending}",
        title = "$distinctResidents Warga Berstatus Pending • $periodName",
        message = "Terdapat ${pendingList.size} pembayaran status pending senilai ${formatRupiah(totalPending)}. " +
            "Ketuk untuk meninjau & mempercepat pelunasan bulan ini.",
        pendingCount = distinctResidents,
        pendingAmount = totalPending,
        targetTabName = "ADMIN_WARGA"
    )
}

object HimbauanNotificationHelper {
    const val CHANNEL_ID = "blok_ce_himbauan_channel"
    const val PENDING_CHANNEL_ID = "blok_ce_pending_reminder_channel"
    const val EXTRA_OPEN_TAB = "extra_open_tab"
    private const val PREFS_NAME = "blok_ce_himbauan_notif_prefs"
    private const val KEY_LAST_SHOWN_KEYS = "shown_alert_keys"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_himbauan_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_himbauan_desc)
                    enableVibration(true)
                    enableLights(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }
            if (manager.getNotificationChannel(PENDING_CHANNEL_ID) == null) {
                val pendingChannel = NotificationChannel(
                    PENDING_CHANNEL_ID,
                    context.getString(R.string.notification_channel_pending_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_pending_desc)
                    enableVibration(true)
                    enableLights(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(pendingChannel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun dispatchHimbauanIfNew(
        context: Context,
        alertKey: String,
        title: String,
        message: String,
        targetTabName: String = "PAY"
    ): Boolean = dispatchInternal(
        context = context,
        channelId = CHANNEL_ID,
        alertKey = alertKey,
        title = title,
        message = message,
        targetTabName = targetTabName,
        forceNotify = false
    )

    fun dispatchPendingStatusNotification(
        context: Context,
        alertKey: String,
        title: String,
        message: String,
        targetTabName: String = "PAY",
        forceNotify: Boolean = false
    ): Boolean = dispatchInternal(
        context = context,
        channelId = PENDING_CHANNEL_ID,
        alertKey = alertKey,
        title = title,
        message = message,
        targetTabName = targetTabName,
        forceNotify = forceNotify
    )

    private fun dispatchInternal(
        context: Context,
        channelId: String,
        alertKey: String,
        title: String,
        message: String,
        targetTabName: String,
        forceNotify: Boolean
    ): Boolean {
        if (alertKey.isBlank() || title.isBlank() || message.isBlank()) return false
        if (!hasNotificationPermission(context)) return false
        ensureChannel(context)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val shownSet = prefs.getStringSet(KEY_LAST_SHOWN_KEYS, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (!forceNotify && alertKey in shownSet) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TAB, targetTabName)
        }
        val reqCode = alertKey.hashCode() and 0x7FFFFFFF
        val pendingIntent = PendingIntent.getActivity(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title.take(100))
            .setContentText(message.take(180))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(reqCode, notification)
            shownSet.add(alertKey)
            val trimmed = if (shownSet.size > 60) {
                shownSet.toList().takeLast(60).toSet()
            } else {
                shownSet
            }
            prefs.edit().putStringSet(KEY_LAST_SHOWN_KEYS, trimmed).apply()
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
