"use strict";

const http = require("node:http");
const crypto = require("node:crypto");

const DEFAULT_PROJECT_ID = process.env.FIREBASE_PROJECT_ID || "gen-lang-client-0300234321";
const DEFAULT_DATABASE_ID =
  process.env.FIRESTORE_DATABASE_ID ||
  "ai-studio-android-blokceps-3d267df8-34f4-4b1b-ad95-9ac915d88fe0";
const AUTO_BILL_METHOD = "Tagihan Otomatis";

const OUTGOING_KEYWORDS = [
  "berhasil dikirim",
  "kamu mengirim",
  "anda mengirim",
  "transfer keluar",
  "beli pulsa",
  "bayar tagihan",
];

const INCOMING_KEYWORDS = [
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
  "top up masuk",
  "success",
  "paid",
];

/**
 * Constant-time HMAC-SHA256 verification.
 * Accepts either hex or `sha256=<hex>` format in `X-DANA-Signature`.
 */
function verifyHmacSignature(rawBody, signatureHeader, secret) {
  if (!secret || typeof secret !== "string") return true;
  if (!signatureHeader || typeof signatureHeader !== "string") return false;

  const cleanSig = signatureHeader.trim().replace(/^sha256=/i, "").toLowerCase();
  if (!/^[0-9a-f]{64}$/.test(cleanSig)) return false;

  const expectedHex = crypto
    .createHmac("sha256", secret)
    .update(rawBody, "utf8")
    .digest("hex")
    .toLowerCase();

  return crypto.timingSafeEqual(
    Buffer.from(cleanSig, "hex"),
    Buffer.from(expectedHex, "hex")
  );
}

/**
 * Extracts positive IDR amount from numeric values, DANA Business `amount.value`,
 * or Rupiah text strings like "Rp 50.000,00".
 */
function extractRupiahAmount(input) {
  if (typeof input === "number" && Number.isFinite(input) && input > 0) {
    return Math.round(input);
  }
  if (input && typeof input === "object" && input.value !== undefined) {
    return extractRupiahAmount(input.value);
  }
  if (typeof input !== "string") return 0;

  const trimmed = input.trim();
  if (/^\d+(\.\d{1,2})?$/.test(trimmed)) {
    return Math.round(parseFloat(trimmed));
  }

  const rupiahMatch = trimmed.match(
    /(?:rp\.?|idr)?\s*([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{4,9})(?:[.,]00)?/i
  );
  if (!rupiahMatch) return 0;
  const digits = rupiahMatch[1].replace(/[.,]/g, "");
  const parsed = Number.parseInt(digits, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
}

/**
 * Normalizes DANA Business Merchant Webhook, Gateway (Midtrans/Xendit),
 * or Android Notification Relay JSON into a canonical event object.
 */
function parseDanaWebhookPayload(body) {
  if (!body || typeof body !== "object") {
    return { valid: false, reason: "Payload JSON kosong atau tidak valid." };
  }

  // Ping / connectivity verification event
  if (body.eventType === "PING" || body.source === "webhook_test_ping") {
    return { valid: true, isPing: true, source: body.source || "webhook_test_ping" };
  }

  // Reject explicit failed/pending/outgoing statuses
  const statusStr = String(
    body.latestTransactionStatus || body.transactionStatus || body.status || ""
  ).toLowerCase();
  if (
    statusStr &&
    !["00", "success", "settled", "settlement", "paid", "completed", "credit"].includes(
      statusStr
    )
  ) {
    return { valid: false, reason: `Status transaksi bukan sukses: ${statusStr}` };
  }

  const rawText = String(body.rawText || body.message || body.description || "").trim();
  const rawLower = rawText.toLowerCase();
  if (rawText && OUTGOING_KEYWORDS.some((kw) => rawLower.includes(kw))) {
    return { valid: false, reason: "Notifikasi transfer keluar diabaikan." };
  }

  const amount = extractRupiahAmount(
    body.amount ?? body.gross_amount ?? body.paidAmount ?? rawText
  );
  if (amount <= 0) {
    return { valid: false, reason: "Nominal transaksi Rupiah tidak valid." };
  }

  if (
    body.amount === undefined &&
    body.gross_amount === undefined &&
    rawText &&
    !INCOMING_KEYWORDS.some((kw) => rawLower.includes(kw))
  ) {
    return { valid: false, reason: "Pesan teks tidak mengindikasikan dana masuk." };
  }

  const referenceId = String(
    body.referenceId ||
      body.originalPartnerReferenceNo ||
      body.originalReferenceNo ||
      body.transaction_id ||
      body.order_id ||
      `DANA-WH-${Date.now()}`
  )
    .replace(/[^a-zA-Z0-9_-]/g, "_")
    .slice(0, 128);

  const senderFromTextMatch = rawText.match(
    /(?:dari|from|oleh)\s+([A-Za-z0-9 .,'_-]{2,40})/i
  );
  const senderName = String(
    body.senderName ||
      body.senderHint ||
      body.payerName ||
      body.customerName ||
      (senderFromTextMatch ? senderFromTextMatch[1].trim() : "")
  )
    .trim()
    .slice(0, 120);

  const senderPhone = String(body.senderPhone || body.payerPhone || "")
    .trim()
    .slice(0, 30);

  const houseHint = String(
    body.houseHint ||
      body.house ||
      (rawText.match(/blok\s*ce[-\s]?(\d+[a-z]?)/i)?.[0] || "")
  )
    .trim()
    .slice(0, 40);

  const monthHint = String(body.monthHint || body.month || "")
    .trim()
    .slice(0, 7);

  const paymentTypeHint = String(body.paymentTypeHint || body.type || "")
    .trim()
    .toLowerCase()
    .slice(0, 20);

  const source = String(
    body.source ||
      (body.originalPartnerReferenceNo ? "dana_merchant_webhook" : "gateway_webhook")
  )
    .trim()
    .slice(0, 60);

  return {
    valid: true,
    isPing: false,
    referenceId,
    source,
    amount,
    senderName,
    senderPhone,
    houseHint,
    monthHint,
    paymentTypeHint,
    rawPayload: (rawText || JSON.stringify(body)).slice(0, 2000),
  };
}

/**
 * Matches parsed webhook transaction against resident profiles and open payments.
 */
function matchPaymentsForWebhook(event, residents, payments) {
  const resById = new Map(residents.map((r) => [r.id || r.userId, r]));
  const cleanName = (event.senderName || "").trim().toLowerCase();
  const cleanHouse = (event.houseHint || "").trim().toLowerCase().replace(/\s+/g, "");
  const cleanPhone = (event.senderPhone || "").replace(/\D/g, "");

  function matchesResident(resident) {
    if (!resident) return false;
    let matched = false;
    if (cleanName.length >= 3) {
      const rName = (resident.name || "").toLowerCase();
      const rUser = (resident.username || "").toLowerCase();
      if (
        (rName && (rName.includes(cleanName) || cleanName.includes(rName))) ||
        (rUser.length >= 3 && cleanName.includes(rUser))
      ) {
        matched = true;
      }
    }
    if (cleanHouse.length >= 2) {
      const rHouse = (resident.house || "").toLowerCase().replace(/\s+/g, "");
      if (rHouse && (rHouse.includes(cleanHouse) || cleanHouse.includes(rHouse))) {
        matched = true;
      }
    }
    if (cleanPhone.length >= 6) {
      const rPhone = (resident.phone || "").replace(/\D/g, "");
      if (rPhone && rPhone.endsWith(cleanPhone.slice(-6))) {
        matched = true;
      }
    }
    return matched;
  }

  function applyHints(list) {
    return list.filter((p) => {
      if (event.monthHint && p.month !== event.monthHint) return false;
      if (
        event.paymentTypeHint &&
        event.paymentTypeHint !== "both" &&
        p.type !== event.paymentTypeHint
      ) {
        return false;
      }
      return true;
    });
  }

  const openManualPending = applyHints(
    payments.filter((p) => p.status === "pending" && p.method !== AUTO_BILL_METHOD)
  );
  const openUnpaid = applyHints(
    payments.filter(
      (p) =>
        p.status === "unpaid" ||
        (p.status === "pending" && p.method === AUTO_BILL_METHOD)
    )
  );

  // 1. Exact single manual confirmation
  const exactPending = openManualPending.filter((p) => p.amount === event.amount);
  if (exactPending.length === 1) {
    return { status: "AUTO_VERIFIED", targets: exactPending };
  }
  if (exactPending.length > 1) {
    const hinted = exactPending.filter((p) => matchesResident(resById.get(p.userId)));
    if (hinted.length === 1) return { status: "AUTO_VERIFIED", targets: hinted };
  }

  // 2. Combined pending bundle (Iuran + Hidmat in same month)
  const pendingBundles = Object.values(
    openManualPending.reduce((acc, p) => {
      const key = `${p.userId}_${p.month}`;
      acc[key] = acc[key] || [];
      acc[key].push(p);
      return acc;
    }, {})
  ).filter((grp) => grp.length >= 2 && grp.reduce((s, x) => s + x.amount, 0) === event.amount);

  if (pendingBundles.length === 1) {
    return { status: "AUTO_VERIFIED", targets: pendingBundles[0] };
  }
  if (pendingBundles.length > 1) {
    const hintedBundles = pendingBundles.filter((grp) =>
      matchesResident(resById.get(grp[0].userId))
    );
    if (hintedBundles.length === 1) {
      return { status: "AUTO_VERIFIED", targets: hintedBundles[0] };
    }
  }

  // 3. Direct auto-generated `unpaid` bill match when resident identity hint is present
  const hasIdentityHint =
    cleanName.length >= 3 || cleanHouse.length >= 2 || cleanPhone.length >= 6;
  if (hasIdentityHint) {
    const unpaidResident = openUnpaid.filter((p) =>
      matchesResident(resById.get(p.userId))
    );
    const unpaidSingle = unpaidResident.filter((p) => p.amount === event.amount);
    if (unpaidSingle.length === 1) {
      return { status: "AUTO_VERIFIED", targets: unpaidSingle };
    }
    const unpaidBundles = Object.values(
      unpaidResident.reduce((acc, p) => {
        const key = `${p.userId}_${p.month}`;
        acc[key] = acc[key] || [];
        acc[key].push(p);
        return acc;
      }, {})
    ).filter(
      (grp) => grp.length >= 2 && grp.reduce((s, x) => s + x.amount, 0) === event.amount
    );
    if (unpaidBundles.length === 1) {
      return { status: "AUTO_VERIFIED", targets: unpaidBundles[0] };
    }
  }

  return {
    status: exactPending.length > 1 ? "NEED_REVIEW" : "UNMATCHED",
    targets: [],
  };
}

function createFirestoreAdminStore() {
  const admin = require("firebase-admin");
  const { getFirestore, FieldValue } = require("firebase-admin/firestore");
  if (!admin.apps.length) {
    admin.initializeApp({ projectId: DEFAULT_PROJECT_ID });
  }
  const db = getFirestore(admin.app(), DEFAULT_DATABASE_ID);

  return {
    async loadResidentsAndPayments() {
      const [resSnap, paySnap] = await Promise.all([
        db.collection("residents").get(),
        db.collection("payments").where("status", "in", ["pending", "unpaid"]).get(),
      ]);
      const residents = resSnap.docs.map((d) => ({ id: d.id, ...d.data() }));
      const payments = paySnap.docs.map((d) => ({ id: d.id, ...d.data() }));
      return { residents, payments };
    },
    async commitVerification({ event, targets, status, matchedSummary }) {
      const batch = db.batch();
      const nowLabel = new Intl.DateTimeFormat("id-ID", {
        day: "2-digit",
        month: "short",
        year: "numeric",
      }).format(new Date());

      for (const pay of targets) {
        const payRef = db.collection("payments").doc(pay.id);
        batch.update(payRef, {
          status: "paid",
          dateLabel: nowLabel,
          method: "DANA Webhook Live",
          note: `Verifikasi DANA Webhook (${event.referenceId})${
            event.senderName ? ` • Dari: ${event.senderName}` : ""
          }`.slice(0, 200),
          updatedAt: FieldValue.serverTimestamp(),
        });
      }

      const eventRef = db.collection("dana_webhook_events").doc(event.referenceId);
      batch.set(
        eventRef,
        {
          referenceId: event.referenceId,
          source: event.source,
          amount: event.amount,
          senderName: event.senderName,
          senderPhone: event.senderPhone,
          houseHint: event.houseHint,
          monthHint: event.monthHint,
          paymentTypeHint: event.paymentTypeHint,
          rawPayload: event.rawPayload,
          status,
          matchedPaymentId: targets[0]?.id || "",
          matchedSummary,
          verifiedByUid: "webhook_server",
          createdAt: FieldValue.serverTimestamp(),
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
      await batch.commit();
    },
  };
}

function createWebhookServer({
  secret = process.env.DANA_WEBHOOK_SECRET || "",
  store = null,
} = {}) {
  let resolvedStore = store;
  function getStore() {
    if (!resolvedStore) {
      resolvedStore = createFirestoreAdminStore();
    }
    return resolvedStore;
  }

  return http.createServer(async (req, res) => {
    res.setHeader("Content-Type", "application/json; charset=utf-8");

    if (req.method === "GET" && req.url === "/health") {
      res.statusCode = 200;
      res.end(
        JSON.stringify({
          ok: true,
          service: "blok-ce-psa-dana-webhook",
          projectId: DEFAULT_PROJECT_ID,
          databaseId: DEFAULT_DATABASE_ID,
        })
      );
      return;
    }

    if (req.method !== "POST" || !req.url?.startsWith("/webhook/dana")) {
      res.statusCode = 404;
      res.end(JSON.stringify({ ok: false, error: "Endpoint tidak ditemukan." }));
      return;
    }

    const chunks = [];
    for await (const chunk of req) {
      chunks.push(chunk);
    }
    const rawBody = Buffer.concat(chunks).toString("utf8");

    const sigHeader = req.headers["x-dana-signature"];
    const secretHeader = req.headers["x-webhook-secret"];
    if (secret) {
      const hmacOk = verifyHmacSignature(rawBody, sigHeader, secret);
      const tokenOk =
        typeof secretHeader === "string" &&
        secretHeader.length === secret.length &&
        crypto.timingSafeEqual(Buffer.from(secretHeader), Buffer.from(secret));
      if (!hmacOk && !tokenOk) {
        res.statusCode = 401;
        res.end(JSON.stringify({ ok: false, error: "Signature webhook tidak valid." }));
        return;
      }
    }

    let jsonBody;
    try {
      jsonBody = JSON.parse(rawBody);
    } catch {
      res.statusCode = 400;
      res.end(JSON.stringify({ ok: false, error: "Body JSON tidak valid." }));
      return;
    }

    const parsed = parseDanaWebhookPayload(jsonBody);
    if (!parsed.valid) {
      res.statusCode = 202;
      res.end(JSON.stringify({ ok: true, ignored: true, reason: parsed.reason }));
      return;
    }

    if (parsed.isPing) {
      res.statusCode = 200;
      res.end(
        JSON.stringify({
          ok: true,
          pong: true,
          source: parsed.source,
          timestamp: new Date().toISOString(),
        })
      );
      return;
    }

    try {
      const activeStore = getStore();
      const { residents, payments } = await activeStore.loadResidentsAndPayments();
      const match = matchPaymentsForWebhook(parsed, residents, payments);
      const owner = match.targets[0]
        ? residents.find((r) => (r.id || r.userId) === match.targets[0].userId)
        : null;
      const matchedSummary =
        match.status === "AUTO_VERIFIED"
          ? `LUNAS WEBHOOK: ${owner?.name || match.targets[0].userId} (${match.targets
              .map((t) => `${t.type.toUpperCase()} ${t.month}`)
              .join("+")})`
          : match.status === "NEED_REVIEW"
          ? "Nominal cocok >1 tagihan pending — perlu review Admin di aplikasi"
          : `Belum ada tagihan cocok untuk Rp ${parsed.amount.toLocaleString("id-ID")}`;

      await activeStore.commitVerification({
        event: parsed,
        targets: match.targets,
        status: match.status,
        matchedSummary,
      });

      res.statusCode = 200;
      res.end(
        JSON.stringify({
          ok: true,
          referenceId: parsed.referenceId,
          verificationStatus: match.status,
          verifiedCount: match.targets.length,
          matchedSummary,
        })
      );
    } catch (err) {
      res.statusCode = 500;
      res.end(
        JSON.stringify({
          ok: false,
          error: err instanceof Error ? err.message : "Internal server error",
        })
      );
    }
  });
}

if (require.main === module) {
  const port = Number.parseInt(process.env.PORT || "8080", 10);
  const server = createWebhookServer();
  server.listen(port, () => {
    console.log(
      `[Blok CE PSA] DANA Webhook listening on :${port} (DB: ${DEFAULT_DATABASE_ID})`
    );
  });
}

module.exports = {
  verifyHmacSignature,
  extractRupiahAmount,
  parseDanaWebhookPayload,
  matchPaymentsForWebhook,
  createWebhookServer,
};
