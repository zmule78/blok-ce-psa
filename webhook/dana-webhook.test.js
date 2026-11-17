"use strict";

const { test } = require("node:test");
const assert = require("node:assert/strict");
const crypto = require("node:crypto");
const {
  verifyHmacSignature,
  extractRupiahAmount,
  parseDanaWebhookPayload,
  matchPaymentsForWebhook,
  createWebhookServer,
} = require("./dana-webhook-server");

function signBody(bodyStr, secret) {
  return (
    "sha256=" +
    crypto.createHmac("sha256", secret).update(bodyStr, "utf8").digest("hex")
  );
}

test("verifyHmacSignature accepts valid signature and rejects tampered payload", () => {
  const secret = "blokce_secret_2026";
  const payload = JSON.stringify({ amount: 50000, senderName: "Budi" });
  const validSig = signBody(payload, secret);

  assert.equal(verifyHmacSignature(payload, validSig, secret), true);
  assert.equal(
    verifyHmacSignature(payload + " ", validSig, secret),
    false
  );
  assert.equal(verifyHmacSignature(payload, "sha256=deadbeef", secret), false);
});

test("parseDanaWebhookPayload normalizes merchant webhook and Android relay text", () => {
  const merchant = parseDanaWebhookPayload({
    originalPartnerReferenceNo: "TRX-DANA-9912",
    latestTransactionStatus: "00",
    amount: { value: "75000.00", currency: "IDR" },
    payerName: "Budi Santoso",
  });
  assert.equal(merchant.valid, true);
  assert.equal(merchant.referenceId, "TRX-DANA-9912");
  assert.equal(merchant.amount, 75000);
  assert.equal(merchant.senderName, "Budi Santoso");
  assert.equal(merchant.source, "dana_merchant_webhook");

  const outgoing = parseDanaWebhookPayload({
    rawText: "Anda mengirim Rp 50.000 ke Ahmad",
  });
  assert.equal(outgoing.valid, false);
});

test("matchPaymentsForWebhook matches single pending, combined bundle, and hinted unpaid bill", () => {
  const residents = [
    { id: "u1", userId: "u1", name: "Budi Santoso", username: "budi", house: "Blok CE-05" },
    { id: "u2", userId: "u2", name: "Siti Aminah", username: "siti", house: "Blok CE-09" },
  ];
  const payments = [
    { id: "u1_2026-11_iuran", userId: "u1", month: "2026-11", type: "iuran", amount: 50000, status: "pending", method: "Transfer DANA" },
    { id: "u1_2026-11_hidmat", userId: "u1", month: "2026-11", type: "hidmat", amount: 25000, status: "pending", method: "Transfer DANA" },
    { id: "u2_2026-11_iuran", userId: "u2", month: "2026-11", type: "iuran", amount: 50000, status: "unpaid", method: "Tagihan Otomatis" },
  ];

  // Combined pending bundle Rp 75.000
  const bundleMatch = matchPaymentsForWebhook(
    { amount: 75000, senderName: "Budi" },
    residents,
    payments
  );
  assert.equal(bundleMatch.status, "AUTO_VERIFIED");
  assert.equal(bundleMatch.targets.length, 2);

  // Unpaid bill matched by resident name + amount
  const unpaidMatch = matchPaymentsForWebhook(
    { amount: 50000, senderName: "Siti Aminah" },
    residents,
    payments.filter((p) => p.userId === "u2")
  );
  assert.equal(unpaidMatch.status, "AUTO_VERIFIED");
  assert.equal(unpaidMatch.targets[0].id, "u2_2026-11_iuran");
  assert.equal(extractRupiahAmount("Rp 50.000,00"), 50000);
});

test("HTTP webhook server verifies HMAC, processes payment, and rejects invalid signature", async () => {
  const secret = "test_secret_key";
  let committed = null;
  const mockStore = {
    async loadResidentsAndPayments() {
      return {
        residents: [{ id: "u1", userId: "u1", name: "Budi", house: "Blok CE-05" }],
        payments: [
          {
            id: "u1_2026-11_iuran",
            userId: "u1",
            month: "2026-11",
            type: "iuran",
            amount: 50000,
            status: "pending",
            method: "DANA QRIS",
          },
        ],
      };
    },
    async commitVerification(args) {
      committed = args;
    },
  };

  const server = createWebhookServer({ secret, store: mockStore });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const addr = server.address();
  const baseUrl = `http://127.0.0.1:${addr.port}`;

  try {
    // 1. Invalid signature -> 401
    const badRes = await fetch(`${baseUrl}/webhook/dana`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-DANA-Signature": "sha256=bad" },
      body: JSON.stringify({ amount: 50000 }),
    });
    assert.equal(badRes.status, 401);

    // 2. Valid PING -> 200 pong
    const pingBody = JSON.stringify({ eventType: "PING", source: "webhook_test_ping" });
    const pingRes = await fetch(`${baseUrl}/webhook/dana`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-DANA-Signature": signBody(pingBody, secret),
      },
      body: pingBody,
    });
    assert.equal(pingRes.status, 200);
    const pingJson = await pingRes.json();
    assert.equal(pingJson.pong, true);

    // 3. Valid incoming DANA webhook -> AUTO_VERIFIED
    const payBody = JSON.stringify({
      referenceId: "DANA-REF-001",
      amount: 50000,
      senderName: "Budi",
      latestTransactionStatus: "00",
    });
    const payRes = await fetch(`${baseUrl}/webhook/dana`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-DANA-Signature": signBody(payBody, secret),
      },
      body: payBody,
    });
    assert.equal(payRes.status, 200);
    const payJson = await payRes.json();
    assert.equal(payJson.verificationStatus, "AUTO_VERIFIED");
    assert.equal(payJson.verifiedCount, 1);
    assert.equal(committed.targets[0].id, "u1_2026-11_iuran");
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});
