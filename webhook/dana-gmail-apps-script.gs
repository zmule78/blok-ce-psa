/**
 * Blok CE PSA - Google Apps Script Serverless DANA Webhook & Gmail Trigger
 *
 * Opsi serverless gratis tanpa VPS/Cloud Run:
 * 1. Buka https://script.google.com -> Buat Project baru.
 * 2. Tempel script ini dan isi Script Properties:
 *    - WEBHOOK_SECRET: (samakan dengan Secret di tab Admin aplikasi)
 *    - FIRESTORE_SA_EMAIL & FIRESTORE_SA_KEY (atau deploy URL Web App ke aplikasi Android)
 * 3. Buat Time-driven trigger (1 menit sekali) pada `pollDanaEmailNotifications`
 *    atau Deploy sebagai Web App (`doPost`) untuk menerima forwarder MacroDroid/Gateway.
 */

const PROJECT_ID = "gen-lang-client-0300234321";
const DATABASE_ID = "ai-studio-android-blokceps-3d267df8-34f4-4b1b-ad95-9ac915d88fe0";

function doPost(e) {
  try {
    const rawBody = e.postData ? e.postData.contents : "{}";
    const payload = JSON.parse(rawBody);
    const secret = PropertiesService.getScriptProperties().getProperty("WEBHOOK_SECRET") || "";
    const incomingSecret = (e.parameter && e.parameter.secret) || payload.secret || "";

    if (secret && incomingSecret !== secret) {
      return jsonResponse({ ok: false, error: "Secret tidak valid" }, 401);
    }

    if (payload.eventType === "PING" || payload.source === "webhook_test_ping") {
      return jsonResponse({ ok: true, pong: true, timestamp: new Date().toISOString() }, 200);
    }

    const parsed = parseIncomingDanaText(payload.rawText || "", payload.amount, payload.senderName);
    if (!parsed) {
      return jsonResponse({ ok: true, ignored: true }, 202);
    }

    const refId = sanitizeDocId(payload.referenceId || "GAS_" + Date.now());
    writeWebhookEventToFirestore(refId, {
      referenceId: refId,
      source: payload.source || "apps_script_webhook",
      amount: parsed.amount,
      senderName: parsed.senderName,
      senderPhone: payload.senderPhone || "",
      houseHint: payload.houseHint || "",
      monthHint: payload.monthHint || "",
      paymentTypeHint: payload.paymentTypeHint || "",
      rawPayload: (payload.rawText || rawBody).substring(0, 2000),
      status: "RECEIVED",
      matchedPaymentId: "",
      matchedSummary: "Diterima via Google Apps Script — menunggu verifikasi live Admin",
      verifiedByUid: "apps_script",
    });

    return jsonResponse({ ok: true, referenceId: refId, amount: parsed.amount }, 200);
  } catch (err) {
    return jsonResponse({ ok: false, error: String(err) }, 500);
  }
}

function pollDanaEmailNotifications() {
  const threads = GmailApp.search('from:(dana.id) ("Saldo masuk" OR "diterima" OR "QRIS") is:unread', 0, 10);
  threads.forEach(function (thread) {
    thread.getMessages().forEach(function (msg) {
      if (!msg.isUnread()) return;
      const body = msg.getPlainBody() || "";
      const subject = msg.getSubject() || "";
      const parsed = parseIncomingDanaText(subject + " " + body, 0, "");
      if (parsed) {
        const refId = sanitizeDocId("GMAIL_" + msg.getId());
        writeWebhookEventToFirestore(refId, {
          referenceId: refId,
          source: "gmail_dana_trigger",
          amount: parsed.amount,
          senderName: parsed.senderName,
          senderPhone: "",
          houseHint: "",
          monthHint: "",
          paymentTypeHint: "",
          rawPayload: (subject + "\n" + body).substring(0, 1500),
          status: "RECEIVED",
          matchedPaymentId: "",
          matchedSummary: "Email DANA masuk — diproses otomatis oleh perangkat Admin",
          verifiedByUid: "gmail_trigger",
        });
      }
      msg.markRead();
    });
  });
}

function parseIncomingDanaText(text, explicitAmount, explicitSender) {
  let amount = Number(explicitAmount || 0);
  if (!amount || amount <= 0) {
    const m = String(text).match(/(?:rp\.?|idr)\s*([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{4,9})/i);
    if (!m) return null;
    amount = parseInt(m[1].replace(/[.,]/g, ""), 10);
  }
  if (!amount || amount <= 0) return null;
  const senderMatch = String(text).match(/(?:dari|from|oleh)\s+([A-Za-z0-9 .,'_-]{2,40})/i);
  const senderName = (explicitSender || (senderMatch ? senderMatch[1].trim() : "")).substring(0, 120);
  return { amount: amount, senderName: senderName };
}

function writeWebhookEventToFirestore(docId, fields) {
  const nowIso = new Date().toISOString();
  const url =
    "https://firestore.googleapis.com/v1/projects/" +
    PROJECT_ID +
    "/databases/" +
    DATABASE_ID +
    "/documents/dana_webhook_events?documentId=" +
    encodeURIComponent(docId);

  const firestoreFields = {
    referenceId: { stringValue: String(fields.referenceId) },
    source: { stringValue: String(fields.source) },
    amount: { integerValue: String(Math.round(fields.amount)) },
    senderName: { stringValue: String(fields.senderName || "") },
    senderPhone: { stringValue: String(fields.senderPhone || "") },
    houseHint: { stringValue: String(fields.houseHint || "") },
    monthHint: { stringValue: String(fields.monthHint || "") },
    paymentTypeHint: { stringValue: String(fields.paymentTypeHint || "") },
    rawPayload: { stringValue: String(fields.rawPayload || "") },
    status: { stringValue: String(fields.status) },
    matchedPaymentId: { stringValue: String(fields.matchedPaymentId || "") },
    matchedSummary: { stringValue: String(fields.matchedSummary || "") },
    verifiedByUid: { stringValue: String(fields.verifiedByUid || "apps_script") },
    createdAt: { timestampValue: nowIso },
    updatedAt: { timestampValue: nowIso },
  };

  UrlFetchApp.fetch(url, {
    method: "post",
    contentType: "application/json",
    headers: {
      Authorization: "Bearer " + ScriptApp.getOAuthToken(),
    },
    payload: JSON.stringify({ fields: firestoreFields }),
    muteHttpExceptions: true,
  });
}

function sanitizeDocId(id) {
  return String(id).replace(/[^a-zA-Z0-9_-]/g, "_").substring(0, 128);
}

function jsonResponse(obj, code) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(
    ContentService.MimeType.JSON
  );
}
