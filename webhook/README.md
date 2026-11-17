# Blok CE PSA — DANA Real-Time Webhook Integration

Tersedia dua skrip siap pakai untuk menerima webhook transaksi DANA masuk secara real-time dan memperbarui status pembayaran warga di Cloud Firestore:

1. **`dana-webhook-server.js` (Node.js / Cloud Run / VPS)**
   - Mendengarkan `POST /webhook/dana` (atau `GET /health`).
   - Memverifikasi tanda tangan HMAC-SHA256 pada header `X-DANA-Signature` atau token `X-Webhook-Secret`.
   - Mendukung payload **DANA Business Merchant API**, **Payment Gateway**, dan **Android Notification Forwarder**.
   - Langsung mencatat dokumen ke koleksi `dana_webhook_events` dan memverifikasi tagihan cocok pada `payments` menjadi `"paid"` di database:
     - Project ID: `gen-lang-client-0300234321`
     - Database ID: `ai-studio-android-blokceps-3d267df8-34f4-4b1b-ad95-9ac915d88fe0`

2. **`dana-gmail-apps-script.gs` (Google Apps Script Serverless)**
   - Alternatif tanpa server (gratis) untuk menerima POST webhook atau memantau email notifikasi resmi dari DANA secara otomatis lalu menulis event ke `dana_webhook_events` di Firestore, yang langsung diproses real-time oleh perangkat Admin.

## Menjalankan Lokal / Cloud Run

```bash
cd webhook
npm install
DANA_WEBHOOK_SECRET="rahasia_anda" PORT=8080 npm start
```

## Menjalankan Pengujian Skrip Webhook

```bash
node --test webhook/dana-webhook.test.js
```
