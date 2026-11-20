package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class StatinRegimen(
    @DocumentId val id: String = "",
    val userId: String = "",
    val statinName: String = "",
    val brandName: String = "",
    val doseMg: Int = 0,
    val therapyRole: String = THERAPY_ROLE_PRIMARY,
    val frequencyKey: String = DoseFrequency.ONCE_DAILY_EVENING.storageKey,
    val scheduledTimeMinutes: Int = 20 * 60,
    val active: Boolean = true,
    val startedAtMillis: Long = 0L,
    val endedAtMillis: Long? = null,
    val switchReason: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun normalizedRole(): String =
        if (therapyRole == THERAPY_ROLE_ADJUNCT) THERAPY_ROLE_ADJUNCT else THERAPY_ROLE_PRIMARY

    companion object {
        const val THERAPY_ROLE_PRIMARY = "primary"
        const val THERAPY_ROLE_ADJUNCT = "adjunct"
    }
}

@Keep
@IgnoreExtraProperties
data class DoseLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val regimenId: String = "",
    val statinName: String = "",
    val brandName: String = "",
    val doseMg: Int = 0,
    val frequencyKey: String = DoseFrequency.ONCE_DAILY_EVENING.storageKey,
    val taken: Boolean = true,
    val logDateKey: String = "",
    val takenAtMinutes: Int = 20 * 60,
    val loggedAtMillis: Long = 0L,
    val feelLevelKey: String = FeelLevel.GOOD.storageKey,
    val sideEffectsCsv: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReminderSettings(
    @DocumentId val id: String = "reminders",
    val userId: String = "",
    val enabled: Boolean = false,
    val hour: Int = 20,
    val minute: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ClinicalTargets(
    @DocumentId val id: String = "targets",
    val userId: String = "",
    val ldlTargetMgDl: Int = 70,
    val nonHdlTargetMgDl: Int? = null,
    val apoBTargetMgDl: Int? = null,
    val riskLabel: String = "High Risk (<70)",
    val clinicianNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LipidPanel(
    @DocumentId val id: String = "",
    val userId: String = "",
    val drawDateKey: String = "",
    val drawnAtMillis: Long = 0L,
    val totalCholesterolMgDl: Int = 0,
    val ldlMgDl: Int = 0,
    val hdlMgDl: Int = 0,
    val triglyceridesMgDl: Int = 0,
    val nonHdlMgDl: Int? = null,
    val apoBMgDl: Int? = null,
    val lpAMgDl: Int? = null,
    val fasting: Boolean = true,
    val baselinePanel: Boolean = false,
    val labName: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
