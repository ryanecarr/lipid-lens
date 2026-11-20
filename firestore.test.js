const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validRegimenPayload(uid = ALICE_UID, overrides = {}) {
  const nowTs = new Date(Date.now() - 2000);
  return {
    userId: uid,
    statinName: "Atorvastatin",
    brandName: "Lipitor",
    doseMg: 20,
    frequencyKey: "once_daily_evening",
    scheduledTimeMinutes: 1200,
    active: true,
    startedAtMillis: Date.now() - 5000,
    switchReason: "",
    notes: "Take after dinner",
    createdAt: nowTs,
    updatedAt: nowTs,
    ...overrides,
  };
}

function validDoseLogPayload(uid = ALICE_UID, regimenId = "reg_1", overrides = {}) {
  const nowTs = new Date(Date.now() - 2000);
  return {
    userId: uid,
    regimenId,
    statinName: "Atorvastatin",
    brandName: "Lipitor",
    doseMg: 20,
    frequencyKey: "once_daily_evening",
    taken: true,
    logDateKey: "2026-11-19",
    takenAtMinutes: 1210,
    loggedAtMillis: Date.now() - 3000,
    feelLevelKey: "good",
    sideEffectsCsv: "muscle_ache",
    notes: "Mild calf tightness",
    createdAt: nowTs,
    updatedAt: nowTs,
    ...overrides,
  };
}

function validReminderPayload(uid = ALICE_UID, overrides = {}) {
  const nowTs = new Date(Date.now() - 2000);
  return {
    userId: uid,
    enabled: true,
    hour: 20,
    minute: 0,
    createdAt: nowTs,
    updatedAt: nowTs,
    ...overrides,
  };
}

function validClinicalTargetsPayload(uid = ALICE_UID, overrides = {}) {
  const nowTs = new Date(Date.now() - 2000);
  return {
    userId: uid,
    ldlTargetMgDl: 70,
    riskLabel: "High Risk (<70)",
    clinicianNote: "Cardiology goal",
    createdAt: nowTs,
    updatedAt: nowTs,
    ...overrides,
  };
}

function validLipidPanelPayload(uid = ALICE_UID, overrides = {}) {
  const nowTs = new Date(Date.now() - 2000);
  return {
    userId: uid,
    drawDateKey: "2026-11-10",
    drawnAtMillis: Date.now() - 864000000,
    totalCholesterolMgDl: 158,
    ldlMgDl: 72,
    hdlMgDl: 56,
    triglyceridesMgDl: 112,
    fasting: true,
    labName: "Quest Diagnostics",
    notes: "8 weeks after switching to Repatha",
    createdAt: nowTs,
    updatedAt: nowTs,
    ...overrides,
  };
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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied on all StatinLog subcollections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/regimens`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/logs`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/settings/reminders`).get());
  await assertFails(
    unauthDb
      .collection(`users/${ALICE_UID}/regimens`)
      .doc("reg_1")
      .set(validRegimenPayload())
  );
  await assertFails(
    unauthDb
      .collection(`users/${ALICE_UID}/logs`)
      .doc("log_1")
      .set(validDoseLogPayload())
  );
  await assertFails(
    unauthDb
      .doc(`users/${ALICE_UID}/settings/reminders`)
      .set(validReminderPayload())
  );
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/lipid_panels`).get());
  await assertFails(
    unauthDb
      .collection(`users/${ALICE_UID}/lipid_panels`)
      .doc("panel_1")
      .set(validLipidPanelPayload())
  );
});

test("Regimens: owner can create, list, switch/deactivate, and Repatha 420mg succeeds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const regRef = aliceDb.collection(`users/${ALICE_UID}/regimens`).doc("reg_1");

  await assertSucceeds(regRef.set(validRegimenPayload()));
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/regimens`).get());

  // Switch regimen: deactivate reg_1 and create reg_repatha in a batch
  const repathaRef = aliceDb
    .collection(`users/${ALICE_UID}/regimens`)
    .doc("reg_repatha");
  const batch = aliceDb.batch();
  batch.update(regRef, {
    active: false,
    endedAtMillis: Date.now(),
    updatedAt: new Date(Date.now() - 1000),
  });
  batch.set(
    repathaRef,
    validRegimenPayload(ALICE_UID, {
      statinName: "Evolocumab",
      brandName: "Repatha",
      doseMg: 420,
      frequencyKey: "once_monthly",
      scheduledTimeMinutes: 540,
      switchReason: "Muscle soreness or aches",
    })
  );
  await assertSucceeds(batch.commit());
});

test("Regimens: rejects ghost fields, id key, invalid bounds, future timestamps, and immutable mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection(`users/${ALICE_UID}/regimens`);

  await assertFails(
    col.doc("reg_bad_id_field").set(
      validRegimenPayload(ALICE_UID, { id: "reg_bad_id_field" })
    )
  );
  await assertFails(
    col.doc("reg_ghost").set(
      validRegimenPayload(ALICE_UID, { isVerified: true })
    )
  );
  await assertFails(
    col.doc("reg_spoof").set(validRegimenPayload(BOB_UID))
  );
  await assertFails(
    col.doc("reg_bad_dose").set(validRegimenPayload(ALICE_UID, { doseMg: 4500 }))
  );
  await assertFails(
    col.doc("reg_bad_role").set(validRegimenPayload(ALICE_UID, { therapyRole: "experimental" }))
  );
  await assertFails(
    col.doc("reg_bad_freq").set(
      validRegimenPayload(ALICE_UID, { frequencyKey: "hourly" })
    )
  );
  await assertFails(
    col.doc("reg_future_ts").set(
      validRegimenPayload(ALICE_UID, {
        createdAt: new Date(Date.now() + 60000),
      })
    )
  );

  // Create valid regimen then test invalid updates
  await assertSucceeds(col.doc("reg_valid").set(validRegimenPayload()));
  await assertFails(
    col.doc("reg_valid").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    col.doc("reg_valid").update({
      statinName: "Tampered",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("DoseLogs: owner can create, partial-update, list, and delete; invalid schemas fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const logsCol = aliceDb.collection(`users/${ALICE_UID}/logs`);
  const logRef = logsCol.doc("log_1");

  await assertSucceeds(logRef.set(validDoseLogPayload()));
  await assertSucceeds(logsCol.get());

  // Partial update preserving createdAt, userId, regimenId
  await assertSucceeds(
    logRef.update({
      taken: false,
      logDateKey: "2026-11-19",
      takenAtMinutes: 1230,
      loggedAtMillis: Date.now(),
      feelLevelKey: "okay",
      sideEffectsCsv: "fatigue,headache",
      notes: "Skipped due to headache",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Invalid log payloads
  await assertFails(
    logsCol.doc("log_bad_date").set(
      validDoseLogPayload(ALICE_UID, "reg_1", { logDateKey: "11-19-2026" })
    )
  );
  await assertFails(
    logsCol.doc("log_bad_feel").set(
      validDoseLogPayload(ALICE_UID, "reg_1", { feelLevelKey: "amazing" })
    )
  );
  await assertFails(
    logsCol.doc("log_long_notes").set(
      validDoseLogPayload(ALICE_UID, "reg_1", { notes: "x".repeat(401) })
    )
  );
  await assertFails(
    logsCol.doc("log_extra_id").set(
      validDoseLogPayload(ALICE_UID, "reg_1", { id: "log_extra_id" })
    )
  );
  await assertFails(
    logRef.update({
      regimenId: "reg_other",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(logRef.delete());
});

test("ReminderSettings: owner can create/update reminders doc; rejects other doc IDs and invalid hours/minutes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const remRef = aliceDb.doc(`users/${ALICE_UID}/settings/reminders`);

  await assertSucceeds(remRef.set(validReminderPayload()));
  await assertSucceeds(
    remRef.update({
      enabled: false,
      hour: 8,
      minute: 30,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/settings/arbitrary`).set(validReminderPayload())
  );
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/settings/reminders`).update({
      hour: 25,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/settings/reminders`).update({
      minute: 60,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation: Bob cannot read, list, create, update, or delete Alice's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb
      .doc(`users/${ALICE_UID}/regimens/alice_reg`)
      .set(validRegimenPayload(ALICE_UID));
    await adminDb
      .doc(`users/${ALICE_UID}/logs/alice_log`)
      .set(validDoseLogPayload(ALICE_UID, "alice_reg"));
    await adminDb
      .doc(`users/${ALICE_UID}/settings/reminders`)
      .set(validReminderPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/regimens/alice_reg`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/regimens`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/regimens/bob_inject`).set(validRegimenPayload(BOB_UID))
  );
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/regimens/alice_reg`).update({
      active: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.doc(`users/${ALICE_UID}/regimens/alice_reg`).delete());

  await assertFails(bobDb.doc(`users/${ALICE_UID}/logs/alice_log`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/logs`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/logs/alice_log`).update({
      notes: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.doc(`users/${ALICE_UID}/logs/alice_log`).delete());

  await assertFails(bobDb.doc(`users/${ALICE_UID}/settings/reminders`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/settings/reminders`).update({
      enabled: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .doc(`users/${ALICE_UID}/lipid_panels/alice_panel`)
      .set(validLipidPanelPayload(ALICE_UID));
  });
  await assertFails(bobDb.doc(`users/${ALICE_UID}/lipid_panels/alice_panel`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/lipid_panels`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/lipid_panels/alice_panel`).update({
      ldlMgDl: 250,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.doc(`users/${ALICE_UID}/lipid_panels/alice_panel`).delete());
});

test("LipidPanels: owner can create, update with optional biomarkers, and delete; invalid ranges fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const panelsCol = aliceDb.collection(`users/${ALICE_UID}/lipid_panels`);
  const panelRef = panelsCol.doc("panel_1");

  // Create without optional biomarkers (null omitted)
  await assertSucceeds(panelRef.set(validLipidPanelPayload()));
  await assertSucceeds(panelsCol.get());

  // Create with optional ApoB, Non-HDL, Lp(a), and baselinePanel anchor
  const panelOptRef = panelsCol.doc("panel_opt");
  await assertSucceeds(
    panelOptRef.set(
      validLipidPanelPayload(ALICE_UID, {
        nonHdlMgDl: 102,
        apoBMgDl: 68,
        lpAMgDl: 24,
        baselinePanel: true,
      })
    )
  );

  // Update adding and modifying optional biomarkers and baseline flag
  await assertSucceeds(
    panelRef.update({
      ldlMgDl: 64,
      apoBMgDl: 61,
      nonHdlMgDl: 94,
      baselinePanel: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    panelOptRef.update({
      notes: "Fasting repeat confirmed",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Invalid payloads
  await assertFails(
    panelsCol.doc("panel_bad_ldl").set(
      validLipidPanelPayload(ALICE_UID, { ldlMgDl: 0 })
    )
  );
  await assertFails(
    panelsCol.doc("panel_null_apob").set(
      validLipidPanelPayload(ALICE_UID, { apoBMgDl: null })
    )
  );
  await assertFails(
    panelsCol.doc("panel_bad_date").set(
      validLipidPanelPayload(ALICE_UID, { drawDateKey: "2026/11/10" })
    )
  );
  await assertFails(
    panelsCol.doc("panel_ghost").set(
      validLipidPanelPayload(ALICE_UID, { isFasting: true })
    )
  );
  await assertFails(
    panelRef.update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(panelRef.delete());
});

test("Combination regimens: concurrent primary + adjunct therapy and 2000mg Vascepa succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection(`users/${ALICE_UID}/regimens`);

  await assertSucceeds(
    col.doc("reg_primary").set(
      validRegimenPayload(ALICE_UID, {
        statinName: "Rosuvastatin",
        brandName: "Crestor",
        doseMg: 20,
        therapyRole: "primary",
      })
    )
  );
  await assertSucceeds(
    col.doc("reg_adjunct_zetia").set(
      validRegimenPayload(ALICE_UID, {
        statinName: "Ezetimibe",
        brandName: "Zetia",
        doseMg: 10,
        therapyRole: "adjunct",
        frequencyKey: "once_daily_morning",
        scheduledTimeMinutes: 480,
      })
    )
  );
  await assertSucceeds(
    col.doc("reg_adjunct_vascepa").set(
      validRegimenPayload(ALICE_UID, {
        statinName: "Icosapent ethyl",
        brandName: "Vascepa",
        doseMg: 2000,
        therapyRole: "adjunct",
      })
    )
  );
});

test("ClinicalTargets: owner can create, update, and isolate settings/targets; rejects invalid bounds & cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const targetsRef = aliceDb.doc(`users/${ALICE_UID}/settings/targets`);

  await assertFails(unauthDb.doc(`users/${ALICE_UID}/settings/targets`).get());
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/settings/targets`).set(validClinicalTargetsPayload())
  );

  await assertSucceeds(
    targetsRef.set(
      validClinicalTargetsPayload(ALICE_UID, {
        ldlTargetMgDl: 55,
        nonHdlTargetMgDl: 85,
        apoBTargetMgDl: 65,
        riskLabel: "Very High Risk (<55)",
      })
    )
  );
  await assertSucceeds(targetsRef.get());
  await assertSucceeds(
    targetsRef.update({
      ldlTargetMgDl: 70,
      riskLabel: "High Risk (<70)",
      clinicianNote: "Updated after stress test",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/settings/targets`).update({
      ldlTargetMgDl: 5,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/settings/targets`).update({
      isCustom: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.doc(`users/${ALICE_UID}/settings/targets`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/settings/targets`).update({
      ldlTargetMgDl: 100,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
