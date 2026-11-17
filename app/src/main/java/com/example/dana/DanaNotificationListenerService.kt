package com.example.dana

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.DanaLiveEventEntity
import com.example.data.repository.BlokCeRepository
import com.example.data.repository.ConnectivityObserver
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DanaNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.packageName != DanaNotificationParser.DANA_PACKAGE_NAME) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()

        val parsed = DanaNotificationParser.parse(title, text) ?: return
        val appCtx = applicationContext

        serviceScope.launch {
            try {
                val dao = AppDatabase.getInstance(appCtx).blokCeDao()
                val eventTime = sbn.postTime.takeIf { it > 0L } ?: System.currentTimeMillis()
                val eventId = dao.insertDanaLiveEvent(
                    DanaLiveEventEntity(
                        packageName = sbn.packageName,
                        title = title.take(120),
                        content = text.take(400),
                        parsedAmount = parsed.amount,
                        senderHint = parsed.senderHint,
                        status = "RECEIVED",
                        matchedSummary = "Notifikasi DANA masuk terdeteksi",
                        receivedAtMillis = eventTime
                    )
                )

                val webhookUrl = DanaLiveBridge.getWebhookUrl(appCtx)
                if (DanaLiveBridge.isWebhookRelayEnabled(appCtx) && webhookUrl.isNotBlank()) {
                    val relayJson = DanaWebhookSigner.buildNotificationRelayJson(
                        referenceId = "ANDROID_${eventTime}_$eventId",
                        amount = parsed.amount,
                        senderHint = parsed.senderHint,
                        isQris = parsed.isQris,
                        rawText = "$title $text"
                    )
                    DanaWebhookDispatcher.sendSignedWebhook(
                        webhookUrl = webhookUrl,
                        secret = DanaLiveBridge.getWebhookSecret(appCtx),
                        jsonPayload = relayJson
                    ).onFailure { err ->
                        Log.w("DanaLiveService", "Webhook relay warning: ${err.message}")
                    }
                }

                val autoEnabled = DanaLiveBridge.isAutoVerifyEnabled(appCtx)
                if (FirebaseApp.getApps(appCtx).isEmpty()) {
                    FirebaseApp.initializeApp(appCtx)
                }
                if (FirebaseApp.getApps(appCtx).isEmpty() || Firebase.auth.currentUser == null) {
                    dao.updateDanaLiveEventStatus(
                        id = eventId,
                        status = "UNAUTHENTICATED",
                        matchedSummary = "Login Admin diperlukan untuk sinkronisasi cloud"
                    )
                    return@launch
                }

                val db = FirebaseFirestore.getInstance(appCtx.getString(R.string.firestore_database_id))
                val repo = BlokCeRepository(db, dao, ConnectivityObserver(appCtx), Firebase.auth)
                val methodLabel = if (parsed.isQris) "DANA QRIS (Live)" else "DANA Live"
                repo.processLiveDanaNotification(
                    eventId = eventId,
                    amount = parsed.amount,
                    senderHint = parsed.senderHint,
                    methodLabel = methodLabel,
                    autoVerifyEnabled = autoEnabled
                )
            } catch (e: Exception) {
                Log.w("DanaLiveService", "Error processing DANA notification", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
