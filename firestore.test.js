const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_uid_001";
const ADMIN_EMAIL = "kfkhoerul05@gmail.com";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function tsPast(msAgo = 2000) {
  return new Date(Date.now() - msAgo);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("system_settings").doc("config").get());
  await assertFails(unauthDb.collection("residents").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("payments").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("announcements").where("published", "==", true).get());
  await assertFails(unauthDb.collection("monthly_summaries").doc("2026-11").get());
});

test("Residents: self-creation, PII isolation, self-update, and privilege escalation denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const spoofAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  const created = tsPast(5000);

  // Alice cannot self-assign admin or hidmatEnabled=true
  await assertFails(
    aliceDb.collection("residents").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice",
      name: "Alice",
      house: "Blok CE-01",
      phone: "08123456789",
      email: "alice@example.com",
      role: "admin",
      roleLabel: "Admin",
      hidmatEnabled: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice valid member creation
  await assertSucceeds(
    aliceDb.collection("residents").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice",
      name: "Alice",
      house: "Blok CE-01",
      phone: "08123456789",
      email: "alice@example.com",
      role: "member",
      roleLabel: "Warga / Member",
      hidmatEnabled: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice can read own profile and filtered query; cannot list all or read Bob
  await assertSucceeds(aliceDb.collection("residents").doc(ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("residents").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("residents").get());
  await assertFails(bobDb.collection("residents").doc(ALICE_UID).get());

  // Alice can update own house/phone/name/username, cannot escalate role or hidmatEnabled or add ghost field
  await assertSucceeds(
    aliceDb.collection("residents").doc(ALICE_UID).update({
      house: "Blok CE-05",
      phone: "081299998888",
      updatedAt: tsPast(1000),
    })
  );
  await assertFails(
    aliceDb.collection("residents").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: tsPast(500),
    })
  );
  await assertFails(
    aliceDb.collection("residents").doc(ALICE_UID).update({
      hidmatEnabled: true,
      updatedAt: tsPast(500),
    })
  );
  await assertFails(
    aliceDb.collection("residents").doc(ALICE_UID).update({
      ghostField: "hack",
      updatedAt: tsPast(500),
    })
  );

  // Spoofed unverified admin email cannot list residents or toggle hidmat
  await assertFails(spoofAdminDb.collection("residents").get());
  await assertFails(
    spoofAdminDb.collection("residents").doc(ALICE_UID).update({
      hidmatEnabled: true,
      updatedAt: tsPast(500),
    })
  );

  // Verified Admin can list all residents and toggle hidmatEnabled
  await assertSucceeds(adminDb.collection("residents").get());
  await assertSucceeds(
    adminDb.collection("residents").doc(ALICE_UID).update({
      hidmatEnabled: true,
      updatedAt: tsPast(500),
    })
  );
});

test("Payments: member pending submission, isolation, and admin verification", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  const created = tsPast(5000);

  // Seed Alice resident record
  await assertSucceeds(
    aliceDb.collection("residents").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice",
      name: "Alice",
      house: "Blok CE-01",
      phone: "081234",
      email: "alice@example.com",
      role: "member",
      roleLabel: "Warga / Member",
      hidmatEnabled: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  const payId = `${ALICE_UID}_2026-11_iuran`;

  // Member cannot self-approve as paid
  await assertFails(
    aliceDb.collection("payments").doc(payId).set({
      userId: ALICE_UID,
      month: "2026-11",
      type: "iuran",
      amount: 50000,
      status: "paid",
      dateLabel: "16 Nov 2026",
      method: "Transfer DANA",
      note: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Member can create pending payment
  await assertSucceeds(
    aliceDb.collection("payments").doc(payId).set({
      userId: ALICE_UID,
      month: "2026-11",
      type: "iuran",
      amount: 50000,
      status: "pending",
      dateLabel: "16 Nov 2026",
      method: "Transfer DANA",
      note: "Transfer via DANA",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Member filtered query succeeds; unfiltered and cross-user reads fail
  await assertSucceeds(aliceDb.collection("payments").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("payments").get());
  await assertFails(bobDb.collection("payments").doc(payId).get());

  // Member cannot self-verify or delete
  await assertFails(
    aliceDb.collection("payments").doc(payId).update({
      status: "paid",
      updatedAt: tsPast(500),
    })
  );
  await assertFails(aliceDb.collection("payments").doc(payId).delete());

  // Admin can list, verify, and delete
  await assertSucceeds(adminDb.collection("payments").get());
  await assertSucceeds(
    adminDb.collection("payments").doc(payId).update({
      status: "paid",
      updatedAt: tsPast(500),
    })
  );
  await assertSucceeds(adminDb.collection("payments").doc(payId).delete());
});

test("System settings, announcements, and monthly summaries: member read, admin write", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  const created = tsPast(5000);

  await assertSucceeds(
    adminDb.collection("system_settings").doc("config").set({
      blokName: "Blok CE PSA",
      subtitle: "Paguyuban Warga Perumahan Silih Asih",
      danaNumber: "085263195072",
      danaAccountName: "Bendahara Blok CE PSA",
      customQrisData: "",
      iuranAmount: 50000,
      hidmatAmount: 25000,
      reminderEnabled: true,
      reminderTitle: "Himbauan Pembayaran",
      reminderMessage: "Mohon segera melunasi iuran bulan ini.",
      pushBlastId: 0,
      pushTargetUserId: "all_unpaid",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(aliceDb.collection("system_settings").doc("config").get());
  await assertFails(
    aliceDb.collection("system_settings").doc("config").update({
      iuranAmount: 0,
      updatedAt: tsPast(500),
    })
  );

  await assertSucceeds(
    adminDb.collection("announcements").doc("info_1").set({
      title: "Kerja Bakti Minggu",
      category: "Berita Kegiatan",
      imageUrl: "",
      content: "Berkumpul di pos keamanan jam 07.00 WIB.",
      urgent: true,
      published: true,
      displayDate: "16 Nov 2026",
      authorId: ADMIN_UID,
      authorName: "Admin Blok CE",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(aliceDb.collection("announcements").where("published", "==", true).get());
  await assertFails(aliceDb.collection("announcements").get());
  await assertFails(aliceDb.collection("announcements").doc("info_1").delete());

  await assertSucceeds(
    adminDb.collection("monthly_summaries").doc("2026-11").set({
      month: "2026-11",
      totalMembers: 12,
      paidIuranCount: 8,
      pendingCount: 2,
      updatedAt: created,
    })
  );
  await assertSucceeds(aliceDb.collection("monthly_summaries").doc("2026-11").get());
  await assertFails(
    aliceDb.collection("monthly_summaries").doc("2026-11").update({
      paidIuranCount: 99,
      updatedAt: tsPast(500),
    })
  );
});

test("Admin invites & delegated admins: only primary admin creates invites, atomic batch redemption succeeds, forgery fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  const created = tsPast(5000);
  const code = "CE-ADM-7741";

  // Regular member cannot create or list admin_invites
  await assertFails(
    aliceDb.collection("admin_invites").doc(code).set({
      code,
      targetEmail: "",
      label: "Bendahara",
      used: false,
      usedByUid: "",
      usedByEmail: "",
      createdByUid: ALICE_UID,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(aliceDb.collection("admin_invites").get());

  // Regular member cannot self-create /admins/{uid} without a valid invite batch
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      roleLabel: "Admin",
      inviteCode: code,
      grantedByUid: ADMIN_UID,
      createdAt: created,
    })
  );

  // Primary admin creates a targeted invite for alice@example.com
  await assertSucceeds(
    adminDb.collection("admin_invites").doc(code).set({
      code,
      targetEmail: "alice@example.com",
      label: "Bendahara Blok CE",
      used: false,
      usedByUid: "",
      usedByEmail: "",
      createdByUid: ADMIN_UID,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Wrong user (Bob) cannot redeem Alice's targeted invite
  const bobBatch = bobDb.batch();
  bobBatch.update(bobDb.collection("admin_invites").doc(code), {
    used: true,
    usedByUid: BOB_UID,
    usedByEmail: "bob@example.com",
    updatedAt: tsPast(1000),
  });
  bobBatch.set(bobDb.collection("admins").doc(BOB_UID), {
    userId: BOB_UID,
    email: "bob@example.com",
    roleLabel: "Bendahara Blok CE",
    inviteCode: code,
    grantedByUid: ADMIN_UID,
    createdAt: tsPast(1000),
  });
  await assertFails(bobBatch.commit());

  // Alice redeems invite atomically with /admins/{uid} and /residents/{uid}
  const aliceBatch = aliceDb.batch();
  aliceBatch.update(aliceDb.collection("admin_invites").doc(code), {
    used: true,
    usedByUid: ALICE_UID,
    usedByEmail: "alice@example.com",
    updatedAt: tsPast(1000),
  });
  aliceBatch.set(aliceDb.collection("admins").doc(ALICE_UID), {
    userId: ALICE_UID,
    email: "alice@example.com",
    roleLabel: "Bendahara Blok CE",
    inviteCode: code,
    grantedByUid: ADMIN_UID,
    createdAt: tsPast(1000),
  });
  aliceBatch.set(aliceDb.collection("residents").doc(ALICE_UID), {
    userId: ALICE_UID,
    username: "alice",
    name: "Alice Bendahara",
    house: "Blok CE-03",
    phone: "081234",
    email: "alice@example.com",
    role: "admin",
    roleLabel: "Bendahara Blok CE",
    hidmatEnabled: false,
    createdAt: tsPast(1000),
    updatedAt: tsPast(1000),
  });
  await assertSucceeds(aliceBatch.commit());

  // Delegated Pengurus Blok (Alice) CANNOT update system_settings/config (Admin Utama only)
  await assertSucceeds(
    adminDb.collection("system_settings").doc("config").set({
      blokName: "Blok CE PSA",
      subtitle: "Paguyuban Warga",
      danaNumber: "085263195072",
      danaAccountName: "Bendahara",
      customQrisData: "",
      iuranAmount: 50000,
      hidmatAmount: 25000,
      reminderEnabled: true,
      reminderTitle: "Info",
      reminderMessage: "Bayar",
      pushBlastId: 0,
      pushTargetUserId: "all_unpaid",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("system_settings").doc("config").update({
      danaNumber: "081234567890",
      updatedAt: tsPast(500),
    })
  );
  await assertSucceeds(
    adminDb.collection("system_settings").doc("config").update({
      danaNumber: "081234567890",
      updatedAt: tsPast(500),
    })
  );
});

test("Auto-generated monthly unpaid bills: admin creates unpaid, resident confirms to pending without self-approving paid, others denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  const created = tsPast(5000);
  const autoPayId = `${ALICE_UID}_2050-12_iuran`;

  // Admin creates an auto-generated unpaid bill for December 2050
  await assertSucceeds(
    adminDb.collection("payments").doc(autoPayId).set({
      userId: ALICE_UID,
      month: "2050-12",
      type: "iuran",
      amount: 50000,
      status: "unpaid",
      dateLabel: "01 Des 2050",
      method: "Tagihan Otomatis",
      note: "Tagihan otomatis tgl 1 (Desember 2050)",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob cannot update Alice's auto-generated unpaid bill
  await assertFails(
    bobDb.collection("payments").doc(autoPayId).update({
      status: "pending",
      method: "Transfer DANA",
      note: "Hijack",
      updatedAt: tsPast(1000),
    })
  );

  // Alice cannot self-approve unpaid bill to paid
  await assertFails(
    aliceDb.collection("payments").doc(autoPayId).update({
      status: "paid",
      method: "Transfer DANA",
      updatedAt: tsPast(1000),
    })
  );

  // Alice can submit payment confirmation with ML Kit scanned receipt (unpaid -> pending)
  await assertSucceeds(
    aliceDb.collection("payments").doc(autoPayId).update({
      status: "pending",
      amount: 50000,
      dateLabel: "02 Des 2050",
      method: "DANA QRIS",
      note: "Sudah transfer QRIS",
      receiptImageUrl: "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      receiptOcrText: "Scan Struk: Rp 50.000 • Transfer Berhasil DANA",
      updatedAt: tsPast(1000),
    })
  );

  // Once confirmed (status == pending, method != Tagihan Otomatis), Alice cannot overwrite it again
  await assertFails(
    aliceDb.collection("payments").doc(autoPayId).update({
      amount: 1000,
      method: "Transfer DANA",
      updatedAt: tsPast(500),
    })
  );
});

test("Two-tier Admin: Admin Utama full control, Pengurus Blok restricted to member edits & max 3 delegated admins", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const u3Db = testEnv.authenticatedContext("uid_3", { email: "u3@example.com", email_verified: true }).firestore();
  const u4Db = testEnv.authenticatedContext("uid_4", { email: "u4@example.com", email_verified: true }).firestore();
  const u5Db = testEnv.authenticatedContext("uid_5", { email: "u5@example.com", email_verified: true }).firestore();

  const created = tsPast(5000);

  // Seed Primary Admin resident profile + 5 members
  await assertSucceeds(
    adminDb.collection("residents").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      username: "kfkhoerul",
      name: "Admin Utama",
      house: "Pos Admin CE",
      phone: "085263195072",
      email: ADMIN_EMAIL,
      role: "admin",
      roleLabel: "Admin Utama Blok CE",
      hidmatEnabled: false,
      createdAt: created,
      updatedAt: created,
    })
  );
  for (const [uid, db, email] of [
    [ALICE_UID, aliceDb, "alice@example.com"],
    [BOB_UID, bobDb, "bob@example.com"],
    ["uid_3", u3Db, "u3@example.com"],
    ["uid_4", u4Db, "u4@example.com"],
    ["uid_5", u5Db, "u5@example.com"],
  ]) {
    await assertSucceeds(
      db.collection("residents").doc(uid).set({
        userId: uid,
        username: uid,
        name: uid,
        house: "Blok CE-01",
        phone: "08111",
        email,
        role: "member",
        roleLabel: "Warga / Member",
        hidmatEnabled: false,
        createdAt: created,
        updatedAt: created,
      })
    );
  }

  // Non-admin Bob cannot promote Alice or list all residents
  await assertFails(bobDb.collection("residents").get());
  await assertFails(
    bobDb.collection("residents").doc(ALICE_UID).update({
      role: "admin",
      roleLabel: "Bendahara",
      updatedAt: tsPast(1000),
    })
  );

  // Primary Admin promotes Alice & Bob to Pengurus Blok (delegated count = 2)
  const pBatch1 = adminDb.batch();
  pBatch1.update(adminDb.collection("residents").doc(ALICE_UID), {
    role: "admin",
    roleLabel: "Bendahara Blok CE",
    updatedAt: tsPast(1000),
  });
  pBatch1.set(adminDb.collection("admins").doc(ALICE_UID), {
    userId: ALICE_UID,
    email: "alice@example.com",
    roleLabel: "Bendahara Blok CE",
    inviteCode: "DIRECT_ASSIGN",
    grantedByUid: ADMIN_UID,
    createdAt: tsPast(1000),
  });
  pBatch1.update(adminDb.collection("residents").doc(BOB_UID), {
    role: "admin",
    roleLabel: "Sekretaris Blok CE",
    updatedAt: tsPast(1000),
  });
  pBatch1.set(adminDb.collection("admins").doc(BOB_UID), {
    userId: BOB_UID,
    email: "bob@example.com",
    roleLabel: "Sekretaris Blok CE",
    inviteCode: "DIRECT_ASSIGN",
    grantedByUid: ADMIN_UID,
    createdAt: tsPast(1000),
  });
  pBatch1.set(adminDb.collection("admin_meta").doc("delegated_counter"), {
    count: 2,
    updatedAt: tsPast(1000),
  });
  await assertSucceeds(pBatch1.commit());

  // Pengurus Blok (Alice) CAN edit regular member (uid_3) profile & hidmat
  await assertSucceeds(
    aliceDb.collection("residents").doc("uid_3").update({
      name: "Warga 3 Updated",
      hidmatEnabled: true,
      updatedAt: tsPast(500),
    })
  );

  // Pengurus Blok (Alice) CANNOT edit another Pengurus Blok's (Bob) profile/hidmat or delete ANY resident
  await assertFails(
    aliceDb.collection("residents").doc(BOB_UID).update({
      name: "Edited By Alice",
      hidmatEnabled: true,
      updatedAt: tsPast(500),
    })
  );
  await assertFails(aliceDb.collection("residents").doc("uid_3").delete());

  // Primary Admin CAN edit Pengurus Blok (Bob) profile & hidmatEnabled AND delete non-primary residents
  await assertSucceeds(
    adminDb.collection("residents").doc(BOB_UID).update({
      name: "Bob Updated By Utama",
      hidmatEnabled: true,
      updatedAt: tsPast(500),
    })
  );

  // Pengurus Blok (Alice) CAN promote 1 more member (uid_3 -> slot 3/3 succeeds)
  const alicePromote3 = aliceDb.batch();
  alicePromote3.update(aliceDb.collection("residents").doc("uid_3"), {
    role: "admin",
    roleLabel: "Pengurus Blok",
    updatedAt: tsPast(450),
  });
  alicePromote3.set(aliceDb.collection("admins").doc("uid_3"), {
    userId: "uid_3",
    email: "u3@example.com",
    roleLabel: "Pengurus Blok",
    inviteCode: "DIRECT_ASSIGN",
    grantedByUid: ALICE_UID,
    createdAt: tsPast(450),
  });
  alicePromote3.set(aliceDb.collection("admin_meta").doc("delegated_counter"), {
    count: 3,
    updatedAt: tsPast(450),
  });
  await assertSucceeds(alicePromote3.commit());

  // Pengurus Blok (Alice) CANNOT exceed max 3 delegated admins (uid_4 fails!)
  const alicePromote4 = aliceDb.batch();
  alicePromote4.update(aliceDb.collection("residents").doc("uid_4"), {
    role: "admin",
    roleLabel: "Pengurus Blok",
    updatedAt: tsPast(400),
  });
  alicePromote4.set(aliceDb.collection("admins").doc("uid_4"), {
    userId: "uid_4",
    email: "u4@example.com",
    roleLabel: "Pengurus Blok",
    inviteCode: "DIRECT_ASSIGN",
    grantedByUid: ALICE_UID,
    createdAt: tsPast(400),
  });
  alicePromote4.set(aliceDb.collection("admin_meta").doc("delegated_counter"), {
    count: 4,
    updatedAt: tsPast(400),
  });
  await assertFails(alicePromote4.commit());

  // Primary Admin HAS NO 3-slot limit (can promote uid_4 too)
  const adminPromote4 = adminDb.batch();
  adminPromote4.update(adminDb.collection("residents").doc("uid_4"), {
    role: "admin",
    roleLabel: "Pengurus Tambahan",
    updatedAt: tsPast(350),
  });
  adminPromote4.set(adminDb.collection("admins").doc("uid_4"), {
    userId: "uid_4",
    email: "u4@example.com",
    roleLabel: "Pengurus Tambahan",
    inviteCode: "DIRECT_ASSIGN",
    grantedByUid: ADMIN_UID,
    createdAt: tsPast(350),
  });
  adminPromote4.set(adminDb.collection("admin_meta").doc("delegated_counter"), {
    count: 4,
    updatedAt: tsPast(350),
  });
  await assertSucceeds(adminPromote4.commit());

  // Pengurus Blok (Alice) CANNOT demote or delete Primary Admin
  await assertFails(
    aliceDb.collection("residents").doc(ADMIN_UID).update({
      role: "member",
      roleLabel: "Warga / Member",
      updatedAt: tsPast(300),
    })
  );
  await assertFails(aliceDb.collection("residents").doc(ADMIN_UID).delete());

  // Primary Admin CAN delete Pengurus Blok (uid_4) atomically
  const deleteBatch = adminDb.batch();
  deleteBatch.delete(adminDb.collection("residents").doc("uid_4"));
  deleteBatch.delete(adminDb.collection("admins").doc("uid_4"));
  await assertSucceeds(deleteBatch.commit());
});

test("DANA webhook config & events + Live Presence & Activity Logs: strictly Primary Admin Only", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // Promote Alice to Pengurus Blok to prove even Pengurus Blok is blocked from Webhook & Live Monitoring
  await assertSucceeds(
    adminDb.collection("admins").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      roleLabel: "Pengurus Blok",
      inviteCode: "DIRECT_ASSIGN",
      grantedByUid: ADMIN_UID,
      createdAt: tsPast(3000),
    })
  );

  // Unauth and Pengurus Blok (Alice) CANNOT read or write webhook config or events
  await assertFails(unauthDb.collection("admin_configs").doc("dana_webhook").get());
  await assertFails(aliceDb.collection("admin_configs").doc("dana_webhook").get());
  await assertFails(unauthDb.collection("dana_webhook_events").get());
  await assertFails(aliceDb.collection("dana_webhook_events").get());

  // Primary Admin can create & update webhook config
  await assertSucceeds(
    adminDb.collection("admin_configs").doc("dana_webhook").set({
      webhookUrl: "https://webhook.blokcepsa.id/webhook/dana",
      webhookSecret: "sec_123456",
      relayFromAndroidEnabled: true,
      autoVerifyWebhookEvents: true,
      updatedByUid: ADMIN_UID,
      createdAt: tsPast(2000),
      updatedAt: tsPast(2000),
    })
  );
  await assertSucceeds(
    adminDb.collection("admin_configs").doc("dana_webhook").update({
      webhookUrl: "https://webhook2.blokcepsa.id/webhook/dana",
      relayFromAndroidEnabled: false,
      updatedByUid: ADMIN_UID,
      updatedAt: tsPast(1000),
    })
  );

  // Pengurus Blok STILL cannot read secret webhook config
  await assertFails(aliceDb.collection("admin_configs").doc("dana_webhook").get());

  // Primary Admin manages incoming webhook events; Pengurus Blok cannot
  await assertSucceeds(
    adminDb.collection("dana_webhook_events").doc("DANA_WH_001").set({
      referenceId: "DANA_WH_001",
      source: "android_notification_relay",
      amount: 50000,
      senderName: "Budi",
      senderPhone: "",
      houseHint: "Blok CE-05",
      monthHint: "2026-11",
      paymentTypeHint: "iuran",
      rawPayload: "Saldo masuk Rp 50.000 dari Budi",
      status: "RECEIVED",
      matchedPaymentId: "",
      matchedSummary: "Menunggu verifikasi",
      verifiedByUid: "",
      createdAt: tsPast(1500),
      updatedAt: tsPast(1500),
    })
  );
  await assertSucceeds(adminDb.collection("dana_webhook_events").get());
  await assertFails(aliceDb.collection("dana_webhook_events").doc("DANA_WH_001").get());
  await assertSucceeds(adminDb.collection("dana_webhook_events").doc("DANA_WH_001").delete());

  // Live User Presence & Activity Logs
  await assertSucceeds(
    aliceDb.collection("user_presence").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice",
      house: "Blok CE-01",
      email: "alice@example.com",
      role: "admin",
      roleLabel: "Pengurus Blok",
      online: true,
      activeScreen: "Home",
      lastSeenMillis: Date.now(),
      updatedAt: tsPast(500),
    })
  );
  await assertFails(aliceDb.collection("user_presence").get());
  await assertSucceeds(adminDb.collection("user_presence").get());

  await assertSucceeds(
    aliceDb.collection("activity_logs").doc("log_alice_1").set({
      userId: ALICE_UID,
      userName: "Alice",
      userEmail: "alice@example.com",
      userRole: "Pengurus Blok",
      house: "Blok CE-01",
      actionType: "LOGIN",
      screen: "Home",
      detail: "Masuk portal Blok CE PSA",
      createdAt: tsPast(500),
    })
  );
  // Alice cannot forge another user's activity log or read global logs
  await assertFails(
    aliceDb.collection("activity_logs").doc("log_spoof").set({
      userId: BOB_UID,
      userName: "Bob",
      userEmail: "bob@example.com",
      userRole: "Warga",
      house: "Blok CE-02",
      actionType: "LOGIN",
      screen: "Home",
      detail: "Spoof",
      createdAt: tsPast(500),
    })
  );
  await assertFails(aliceDb.collection("activity_logs").get());
  await assertSucceeds(adminDb.collection("activity_logs").get());
});

test("Finance entries & Budget plans: Warga and Pengurus Blok read-only, Primary Admin full CRUD", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // Promote Alice to Pengurus Blok
  await assertSucceeds(
    adminDb.collection("admins").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      roleLabel: "Pengurus Blok",
      inviteCode: "DIRECT_ASSIGN",
      grantedByUid: ADMIN_UID,
      createdAt: tsPast(3000),
    })
  );

  // Admin Utama creates finance entries (masuk, keluar, kas_blok) & budget plan
  await assertSucceeds(
    adminDb.collection("finance_entries").doc("fin_1").set({
      kind: "kas_blok",
      title: "Saldo Awal Kas Blok CE",
      category: "Saldo Kas",
      amount: 2500000,
      month: "2026-11",
      dateLabel: "01 Nov 2026",
      note: "Saldo pembukuan kas blok",
      createdByUid: ADMIN_UID,
      createdByName: "Admin Utama",
      createdAt: tsPast(2000),
      updatedAt: tsPast(2000),
    })
  );
  await assertSucceeds(
    adminDb.collection("budget_plans").doc("plan_1").set({
      title: "Renovasi Lampu Jalan Blok CE",
      category: "Infrastruktur",
      month: "2026-11",
      plannedAmount: 1500000,
      realizedAmount: 500000,
      status: "in_progress",
      note: "Pengadaan 4 unit lampu LED",
      createdByUid: ADMIN_UID,
      updatedByName: "Admin Utama",
      createdAt: tsPast(2000),
      updatedAt: tsPast(2000),
    })
  );

  // Unauthenticated reads fail
  await assertFails(unauthDb.collection("finance_entries").get());
  await assertFails(unauthDb.collection("budget_plans").get());

  // Warga (Bob) & Pengurus Blok (Alice) CAN read
  await assertSucceeds(bobDb.collection("finance_entries").get());
  await assertSucceeds(aliceDb.collection("finance_entries").get());
  await assertSucceeds(bobDb.collection("budget_plans").get());
  await assertSucceeds(aliceDb.collection("budget_plans").get());

  // Warga (Bob) & Pengurus Blok (Alice) CANNOT create, update, or delete finance entries or budget plans
  await assertFails(
    aliceDb.collection("finance_entries").doc("fin_alice").set({
      kind: "keluar",
      title: "Pengeluaran Pengurus",
      category: "Operasional",
      amount: 100000,
      month: "2026-11",
      dateLabel: "18 Nov 2026",
      note: "",
      createdByUid: ALICE_UID,
      createdByName: "Alice",
      createdAt: tsPast(1000),
      updatedAt: tsPast(1000),
    })
  );
  await assertFails(
    bobDb.collection("budget_plans").doc("plan_1").update({
      realizedAmount: 999999,
      updatedAt: tsPast(500),
    })
  );
  await assertFails(aliceDb.collection("finance_entries").doc("fin_1").delete());
  await assertFails(aliceDb.collection("budget_plans").doc("plan_1").delete());

  // Admin Utama CAN update and delete
  await assertSucceeds(
    adminDb.collection("budget_plans").doc("plan_1").update({
      realizedAmount: 1500000,
      status: "completed",
      updatedAt: tsPast(300),
    })
  );
  await assertSucceeds(adminDb.collection("finance_entries").doc("fin_1").delete());
  await assertSucceeds(adminDb.collection("budget_plans").doc("plan_1").delete());
});




