package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.ClinicalTargets
import com.example.data.model.DoseFrequency
import com.example.data.model.DoseLog
import com.example.data.model.FeelLevel
import com.example.data.model.LipidPanel
import com.example.data.model.ReminderSettings
import com.example.data.model.SideEffectOption
import com.example.data.model.StatinRegimen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

enum class TherapyAddMode {
    SWITCH_PRIMARY,
    ADD_ADJUNCT
}

class StatinCloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUserId != null) {
            check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user write." }
        }
        return uid
    }

    fun observeRegimens(userId: String): Flow<List<StatinRegimen>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val uid = requireUserId(userId)
        val path = "users/$uid/regimens"
        emitAll(
            db.collection("users").document(uid).collection("regimens")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StatinRegimen::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<StatinRegimen> { it.startedAtMillis }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeLogs(userId: String): Flow<List<DoseLog>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val uid = requireUserId(userId)
        val path = "users/$uid/logs"
        emitAll(
            db.collection("users").document(uid).collection("logs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DoseLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<DoseLog> { it.logDateKey }
                            .thenByDescending { it.takenAtMinutes }
                            .thenByDescending { it.loggedAtMillis }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeReminderSettings(userId: String): Flow<ReminderSettings> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val uid = requireUserId(userId)
        val path = "users/$uid/settings/reminders"
        emitAll(
            db.collection("users").document(uid).collection("settings").document("reminders")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        ReminderSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: ReminderSettings(userId = uid)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeClinicalTargets(userId: String): Flow<ClinicalTargets> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val uid = requireUserId(userId)
        val path = "users/$uid/settings/targets"
        emitAll(
            db.collection("users").document(uid).collection("settings").document("targets")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        ClinicalTargets::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: ClinicalTargets(userId = uid)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeLipidPanels(userId: String): Flow<List<LipidPanel>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val uid = requireUserId(userId)
        val path = "users/$uid/lipid_panels"
        emitAll(
            db.collection("users").document(uid).collection("lipid_panels")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LipidPanel::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<LipidPanel> { it.drawDateKey }
                            .thenByDescending { it.drawnAtMillis }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun startOrSwitchRegimen(
        expectedUserId: String,
        statinName: String,
        brandName: String,
        doseMg: Int,
        frequency: DoseFrequency,
        scheduledTimeMinutes: Int,
        therapyMode: TherapyAddMode = TherapyAddMode.SWITCH_PRIMARY,
        switchReason: String = "",
        notes: String = ""
    ): Result<StatinRegimen> {
        var opPath = "users/$expectedUserId/regimens"
        return try {
            val uid = requireUserId(expectedUserId)
            val cleanName = statinName.trim()
            val cleanBrand = brandName.trim()
            val cleanReason = switchReason.trim()
            val cleanNotes = notes.trim()
            require(cleanName.length in 1..60) { "Medication name must be 1 to 60 characters." }
            require(cleanBrand.length <= 60) { "Brand name must be at most 60 characters." }
            require(doseMg in 1..4000) { "Dose must be between 1 and 4000 mg." }
            require(scheduledTimeMinutes in 0..1439) { "Scheduled time out of bounds." }
            require(cleanReason.length <= 120) { "Switch reason must be at most 120 characters." }
            require(cleanNotes.length <= 200) { "Notes must be at most 200 characters." }

            val role = if (therapyMode == TherapyAddMode.ADD_ADJUNCT) {
                StatinRegimen.THERAPY_ROLE_ADJUNCT
            } else {
                StatinRegimen.THERAPY_ROLE_PRIMARY
            }

            val now = System.currentTimeMillis()
            val regimensCol = db.collection("users").document(uid).collection("regimens")
            val activeSnapshot = regimensCol.whereEqualTo("active", true).get().await()

            val newDocRef = regimensCol.document()
            opPath = newDocRef.path

            val createPayload = mapOf(
                "userId" to uid,
                "statinName" to cleanName,
                "brandName" to cleanBrand,
                "doseMg" to doseMg,
                "therapyRole" to role,
                "frequencyKey" to frequency.storageKey,
                "scheduledTimeMinutes" to scheduledTimeMinutes,
                "active" to true,
                "startedAtMillis" to now,
                "switchReason" to cleanReason,
                "notes" to cleanNotes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            activeSnapshot.documents.forEach { oldDoc ->
                val oldRole = oldDoc.getString("therapyRole") ?: StatinRegimen.THERAPY_ROLE_PRIMARY
                val oldName = oldDoc.getString("statinName").orEmpty()
                val shouldDeactivate = when (therapyMode) {
                    TherapyAddMode.SWITCH_PRIMARY -> oldRole == StatinRegimen.THERAPY_ROLE_PRIMARY
                    TherapyAddMode.ADD_ADJUNCT -> {
                        oldRole == StatinRegimen.THERAPY_ROLE_ADJUNCT &&
                            oldName.equals(cleanName, ignoreCase = true)
                    }
                }
                if (shouldDeactivate) {
                    batch.update(
                        oldDoc.reference,
                        mapOf(
                            "active" to false,
                            "endedAtMillis" to now,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.set(newDocRef, createPayload)
            batch.commit().await()

            Result.success(
                StatinRegimen(
                    id = newDocRef.id,
                    userId = uid,
                    statinName = cleanName,
                    brandName = cleanBrand,
                    doseMg = doseMg,
                    therapyRole = role,
                    frequencyKey = frequency.storageKey,
                    scheduledTimeMinutes = scheduledTimeMinutes,
                    active = true,
                    startedAtMillis = now,
                    endedAtMillis = null,
                    switchReason = cleanReason,
                    notes = cleanNotes
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, opPath)
            Result.failure(e)
        }
    }

    suspend fun deactivateRegimen(expectedUserId: String, regimenId: String): Result<Unit> {
        var opPath = "users/$expectedUserId/regimens/$regimenId"
        return try {
            val uid = requireUserId(expectedUserId)
            require(regimenId.isNotBlank()) { "Regimen ID must not be blank." }
            val docRef = db.collection("users").document(uid).collection("regimens").document(regimenId)
            opPath = docRef.path
            docRef.update(
                mapOf(
                    "active" to false,
                    "endedAtMillis" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, opPath)
            Result.failure(e)
        }
    }

    suspend fun saveDoseLog(
        expectedUserId: String,
        existingLogId: String = "",
        regimen: StatinRegimen,
        taken: Boolean,
        logDateKey: String,
        takenAtMinutes: Int,
        feelLevel: FeelLevel,
        sideEffects: Set<SideEffectOption>,
        notes: String
    ): Result<Unit> {
        var opPath = "users/$expectedUserId/logs"
        return try {
            val uid = requireUserId(expectedUserId)
            val cleanNotes = notes.trim()
            val csv = SideEffectOption.toCsv(sideEffects)
            require(regimen.id.isNotBlank()) { "Active regimen must have a valid document ID." }
            require(regimen.statinName.trim().length in 1..60) { "Invalid medication name." }
            require(regimen.brandName.trim().length <= 60) { "Invalid brand name." }
            require(regimen.doseMg in 1..4000) { "Invalid dose strength." }
            require(DATE_KEY_REGEX.matches(logDateKey)) { "Invalid date key format." }
            require(takenAtMinutes in 0..1439) { "Time taken out of bounds." }
            require(csv.length <= 200) { "Side effects payload too large." }
            require(cleanNotes.length <= 400) { "Notes must be at most 400 characters." }

            val now = System.currentTimeMillis()
            val logsCol = db.collection("users").document(uid).collection("logs")

            if (existingLogId.isBlank()) {
                val newRef = logsCol.document()
                opPath = newRef.path
                val payload = mapOf(
                    "userId" to uid,
                    "regimenId" to regimen.id,
                    "statinName" to regimen.statinName.trim(),
                    "brandName" to regimen.brandName.trim(),
                    "doseMg" to regimen.doseMg,
                    "frequencyKey" to DoseFrequency.fromKey(regimen.frequencyKey).storageKey,
                    "taken" to taken,
                    "logDateKey" to logDateKey,
                    "takenAtMinutes" to takenAtMinutes,
                    "loggedAtMillis" to now,
                    "feelLevelKey" to feelLevel.storageKey,
                    "sideEffectsCsv" to csv,
                    "notes" to cleanNotes,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                newRef.set(payload).await()
            } else {
                val docRef = logsCol.document(existingLogId)
                opPath = docRef.path
                val updatePayload = mapOf(
                    "taken" to taken,
                    "logDateKey" to logDateKey,
                    "takenAtMinutes" to takenAtMinutes,
                    "loggedAtMillis" to now,
                    "feelLevelKey" to feelLevel.storageKey,
                    "sideEffectsCsv" to csv,
                    "notes" to cleanNotes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (existingLogId.isBlank()) OperationType.CREATE else OperationType.UPDATE,
                opPath
            )
            Result.failure(e)
        }
    }

    suspend fun deleteDoseLog(expectedUserId: String, logId: String): Result<Unit> {
        var opPath = "users/$expectedUserId/logs/$logId"
        return try {
            val uid = requireUserId(expectedUserId)
            require(logId.isNotBlank()) { "Log ID must not be blank for delete." }
            val docRef = db.collection("users").document(uid).collection("logs").document(logId)
            opPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, opPath)
            Result.failure(e)
        }
    }

    suspend fun updateReminderSettings(
        expectedUserId: String,
        enabled: Boolean,
        hour: Int,
        minute: Int
    ): Result<ReminderSettings> {
        var opPath = "users/$expectedUserId/settings/reminders"
        var opType = OperationType.WRITE
        return try {
            val uid = requireUserId(expectedUserId)
            val safeHour = hour.coerceIn(0, 23)
            val safeMinute = minute.coerceIn(0, 59)
            val docRef = db.collection("users").document(uid)
                .collection("settings").document("reminders")
            opPath = docRef.path

            val existing = docRef.get().await()
            if (existing.exists()) {
                opType = OperationType.UPDATE
                docRef.update(
                    mapOf(
                        "enabled" to enabled,
                        "hour" to safeHour,
                        "minute" to safeMinute,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                opType = OperationType.CREATE
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "enabled" to enabled,
                        "hour" to safeHour,
                        "minute" to safeMinute,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(
                ReminderSettings(
                    id = "reminders",
                    userId = uid,
                    enabled = enabled,
                    hour = safeHour,
                    minute = safeMinute
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, opType, opPath)
            Result.failure(e)
        }
    }

    suspend fun saveClinicalTargets(
        expectedUserId: String,
        ldlTargetMgDl: Int,
        nonHdlTargetMgDl: Int?,
        apoBTargetMgDl: Int?,
        riskLabel: String,
        clinicianNote: String
    ): Result<ClinicalTargets> {
        var opPath = "users/$expectedUserId/settings/targets"
        var opType = OperationType.WRITE
        return try {
            val uid = requireUserId(expectedUserId)
            val cleanLabel = riskLabel.trim().ifBlank { "Custom Goal" }
            val cleanNote = clinicianNote.trim()
            require(ldlTargetMgDl in 20..300) { "LDL-C target must be 20–300 mg/dL." }
            require(nonHdlTargetMgDl == null || nonHdlTargetMgDl in 30..400) {
                "Non-HDL-C target must be 30–400 mg/dL."
            }
            require(apoBTargetMgDl == null || apoBTargetMgDl in 20..250) {
                "ApoB target must be 20–250 mg/dL."
            }
            require(cleanLabel.length in 1..60) { "Target tier label must be 1 to 60 characters." }
            require(cleanNote.length <= 160) { "Clinician note must be at most 160 characters." }

            val docRef = db.collection("users").document(uid)
                .collection("settings").document("targets")
            opPath = docRef.path

            val existing = docRef.get().await()
            if (existing.exists()) {
                opType = OperationType.UPDATE
                docRef.update(
                    mapOf(
                        "ldlTargetMgDl" to ldlTargetMgDl,
                        "nonHdlTargetMgDl" to (nonHdlTargetMgDl ?: FieldValue.delete()),
                        "apoBTargetMgDl" to (apoBTargetMgDl ?: FieldValue.delete()),
                        "riskLabel" to cleanLabel,
                        "clinicianNote" to cleanNote,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                opType = OperationType.CREATE
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "ldlTargetMgDl" to ldlTargetMgDl,
                    "riskLabel" to cleanLabel,
                    "clinicianNote" to cleanNote,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (nonHdlTargetMgDl != null) payload["nonHdlTargetMgDl"] = nonHdlTargetMgDl
                if (apoBTargetMgDl != null) payload["apoBTargetMgDl"] = apoBTargetMgDl
                docRef.set(payload).await()
            }

            Result.success(
                ClinicalTargets(
                    id = "targets",
                    userId = uid,
                    ldlTargetMgDl = ldlTargetMgDl,
                    nonHdlTargetMgDl = nonHdlTargetMgDl,
                    apoBTargetMgDl = apoBTargetMgDl,
                    riskLabel = cleanLabel,
                    clinicianNote = cleanNote
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, opType, opPath)
            Result.failure(e)
        }
    }

    suspend fun saveLipidPanel(
        expectedUserId: String,
        existingPanelId: String = "",
        drawDateKey: String,
        drawnAtMillis: Long,
        totalCholesterolMgDl: Int,
        ldlMgDl: Int,
        hdlMgDl: Int,
        triglyceridesMgDl: Int,
        nonHdlMgDl: Int?,
        apoBMgDl: Int?,
        lpAMgDl: Int?,
        fasting: Boolean,
        baselinePanel: Boolean = false,
        labName: String,
        notes: String
    ): Result<Unit> {
        var opPath = "users/$expectedUserId/lipid_panels"
        return try {
            val uid = requireUserId(expectedUserId)
            val cleanLab = labName.trim()
            val cleanNotes = notes.trim()
            require(DATE_KEY_REGEX.matches(drawDateKey)) { "Draw date must be yyyy-MM-dd." }
            require(drawnAtMillis >= 0L) { "Invalid draw date timestamp." }
            require(totalCholesterolMgDl in 1..2000) { "Total cholesterol must be 1–2000 mg/dL." }
            require(ldlMgDl in 1..2000) { "LDL-C must be 1–2000 mg/dL." }
            require(hdlMgDl in 1..500) { "HDL-C must be 1–500 mg/dL." }
            require(triglyceridesMgDl in 1..5000) { "Triglycerides must be 1–5000 mg/dL." }
            require(nonHdlMgDl == null || nonHdlMgDl in 1..2000) { "Non-HDL-C must be 1–2000 mg/dL." }
            require(apoBMgDl == null || apoBMgDl in 1..500) { "ApoB must be 1–500 mg/dL." }
            require(lpAMgDl == null || lpAMgDl in 1..1000) { "Lp(a) must be 1–1000 mg/dL." }
            require(cleanLab.length <= 80) { "Lab name must be at most 80 characters." }
            require(cleanNotes.length <= 400) { "Notes must be at most 400 characters." }

            val panelsCol = db.collection("users").document(uid).collection("lipid_panels")
            if (existingPanelId.isBlank()) {
                val newRef = panelsCol.document()
                opPath = newRef.path
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "drawDateKey" to drawDateKey,
                    "drawnAtMillis" to drawnAtMillis,
                    "totalCholesterolMgDl" to totalCholesterolMgDl,
                    "ldlMgDl" to ldlMgDl,
                    "hdlMgDl" to hdlMgDl,
                    "triglyceridesMgDl" to triglyceridesMgDl,
                    "fasting" to fasting,
                    "baselinePanel" to baselinePanel,
                    "labName" to cleanLab,
                    "notes" to cleanNotes,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (nonHdlMgDl != null) payload["nonHdlMgDl"] = nonHdlMgDl
                if (apoBMgDl != null) payload["apoBMgDl"] = apoBMgDl
                if (lpAMgDl != null) payload["lpAMgDl"] = lpAMgDl
                newRef.set(payload).await()
            } else {
                val docRef = panelsCol.document(existingPanelId)
                opPath = docRef.path
                val updatePayload = mapOf<String, Any>(
                    "drawDateKey" to drawDateKey,
                    "drawnAtMillis" to drawnAtMillis,
                    "totalCholesterolMgDl" to totalCholesterolMgDl,
                    "ldlMgDl" to ldlMgDl,
                    "hdlMgDl" to hdlMgDl,
                    "triglyceridesMgDl" to triglyceridesMgDl,
                    "nonHdlMgDl" to (nonHdlMgDl ?: FieldValue.delete()),
                    "apoBMgDl" to (apoBMgDl ?: FieldValue.delete()),
                    "lpAMgDl" to (lpAMgDl ?: FieldValue.delete()),
                    "fasting" to fasting,
                    "baselinePanel" to baselinePanel,
                    "labName" to cleanLab,
                    "notes" to cleanNotes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (existingPanelId.isBlank()) OperationType.CREATE else OperationType.UPDATE,
                opPath
            )
            Result.failure(e)
        }
    }

    suspend fun deleteLipidPanel(expectedUserId: String, panelId: String): Result<Unit> {
        var opPath = "users/$expectedUserId/lipid_panels/$panelId"
        return try {
            val uid = requireUserId(expectedUserId)
            require(panelId.isNotBlank()) { "Panel ID must not be blank for delete." }
            val docRef = db.collection("users").document(uid)
                .collection("lipid_panels").document(panelId)
            opPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, opPath)
            Result.failure(e)
        }
    }

    private companion object {
        val DATE_KEY_REGEX = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
    }
}
