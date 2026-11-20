package com.example.data.model

enum class TherapyClass(
    val label: String,
    val badgeLabel: String
) {
    STATIN("Statin", "Statin"),
    CHOLESTEROL_ABSORPTION("Absorption Inhibitor", "Non-Statin Oral"),
    ACL_INHIBITOR("ACL Inhibitor", "Non-Statin Oral"),
    PCSK9("PCSK9 Therapy", "PCSK9"),
    TRIGLYCERIDE_THERAPY("Prescription EPA", "Triglyceride")
}

data class KnownStatin(
    val id: String,
    val genericName: String,
    val brandName: String,
    val commonDosesMg: List<Int>,
    val defaultDoseMg: Int,
    val timingGuidance: String,
    val preferredFrequency: DoseFrequency,
    val clinicalNote: String,
    val therapyClass: TherapyClass = TherapyClass.STATIN,
    val defaultRole: String = StatinRegimen.THERAPY_ROLE_PRIMARY
)

enum class DoseFrequency(
    val storageKey: String,
    val label: String,
    val shortLabel: String,
    val defaultTimeMinutes: Int,
    val cadenceDays: Int,
    val reminderSummary: String
) {
    ONCE_DAILY_EVENING(
        storageKey = "once_daily_evening",
        label = "Once daily (Evening)",
        shortLabel = "Daily • Evening",
        defaultTimeMinutes = 20 * 60, // 8:00 PM
        cadenceDays = 1,
        reminderSummary = "Once every evening"
    ),
    ONCE_DAILY_MORNING(
        storageKey = "once_daily_morning",
        label = "Once daily (Morning)",
        shortLabel = "Daily • Morning",
        defaultTimeMinutes = 8 * 60, // 8:00 AM
        cadenceDays = 1,
        reminderSummary = "Once every morning"
    ),
    EVERY_OTHER_DAY(
        storageKey = "every_other_day",
        label = "Every other day",
        shortLabel = "Every 2nd day",
        defaultTimeMinutes = 20 * 60,
        cadenceDays = 2,
        reminderSummary = "Every 2nd day"
    ),
    EVERY_2_WEEKS(
        storageKey = "every_2_weeks",
        label = "Every 2 weeks",
        shortLabel = "Every 2 weeks",
        defaultTimeMinutes = 9 * 60, // 9:00 AM
        cadenceDays = 14,
        reminderSummary = "Every 14 days"
    ),
    ONCE_MONTHLY(
        storageKey = "once_monthly",
        label = "Once monthly",
        shortLabel = "Monthly",
        defaultTimeMinutes = 9 * 60,
        cadenceDays = 30,
        reminderSummary = "Once every month"
    ),
    CUSTOM(
        storageKey = "custom",
        label = "Custom schedule",
        shortLabel = "Custom",
        defaultTimeMinutes = 20 * 60,
        cadenceDays = 1,
        reminderSummary = "Custom schedule"
    );

    fun isPeriodic(): Boolean =
        this == EVERY_OTHER_DAY || this == EVERY_2_WEEKS || this == ONCE_MONTHLY

    companion object {
        fun fromKey(key: String?): DoseFrequency =
            entries.firstOrNull { it.storageKey == key || it.name == key } ?: ONCE_DAILY_EVENING
    }
}

enum class FeelLevel(
    val storageKey: String,
    val label: String,
    val score: Int
) {
    GREAT("great", "Great", 5),
    GOOD("good", "Good", 4),
    OKAY("okay", "Okay", 3),
    SLUGGISH("sluggish", "Sluggish", 2),
    ROUGH("rough", "Rough", 1);

    companion object {
        fun fromKey(key: String?): FeelLevel =
            entries.firstOrNull { it.storageKey == key || it.name == key } ?: GOOD
    }
}

enum class SideEffectOption(
    val storageKey: String,
    val label: String
) {
    NONE("none", "No side effects"),
    MUSCLE_ACHE("muscle_ache", "Muscle ache"),
    JOINT_STIFFNESS("joint_stiffness", "Joint stiffness"),
    FATIGUE("fatigue", "Fatigue"),
    HEADACHE("headache", "Headache"),
    NAUSEA_GI("nausea_gi", "Nausea / GI"),
    BRAIN_FOG("brain_fog", "Brain fog"),
    DIZZINESS("dizziness", "Dizziness"),
    SLEEP_TROUBLE("sleep_trouble", "Sleep trouble");

    companion object {
        fun fromKey(key: String?): SideEffectOption? =
            entries.firstOrNull { it.storageKey == key || it.name == key }

        fun parseCsv(csv: String): List<SideEffectOption> {
            if (csv.isBlank()) return emptyList()
            return csv.split(",")
                .mapNotNull { fromKey(it.trim()) }
                .distinct()
        }

        fun toCsv(options: Collection<SideEffectOption>): String =
            options.filter { it != NONE }
                .distinct()
                .joinToString(",") { it.storageKey }
    }
}

object KnownStatinCatalog {
    val statins: List<KnownStatin> = listOf(
        KnownStatin(
            id = "atorvastatin",
            genericName = "Atorvastatin",
            brandName = "Lipitor",
            commonDosesMg = listOf(10, 20, 40, 80),
            defaultDoseMg = 20,
            timingGuidance = "Anytime daily (long-acting)",
            preferredFrequency = DoseFrequency.ONCE_DAILY_EVENING,
            clinicalNote = "Long half-life; works consistently morning or evening."
        ),
        KnownStatin(
            id = "rosuvastatin",
            genericName = "Rosuvastatin",
            brandName = "Crestor",
            commonDosesMg = listOf(5, 10, 20, 40),
            defaultDoseMg = 10,
            timingGuidance = "Anytime daily (long-acting)",
            preferredFrequency = DoseFrequency.ONCE_DAILY_EVENING,
            clinicalNote = "Hydrophilic statin often tried when switching for muscle comfort."
        ),
        KnownStatin(
            id = "simvastatin",
            genericName = "Simvastatin",
            brandName = "Zocor",
            commonDosesMg = listOf(10, 20, 40),
            defaultDoseMg = 20,
            timingGuidance = "Evening preferred (short half-life)",
            preferredFrequency = DoseFrequency.ONCE_DAILY_EVENING,
            clinicalNote = "Best taken in the evening when cholesterol synthesis peaks."
        ),
        KnownStatin(
            id = "pravastatin",
            genericName = "Pravastatin",
            brandName = "Pravachol",
            commonDosesMg = listOf(10, 20, 40, 80),
            defaultDoseMg = 40,
            timingGuidance = "Evening or bedtime preferred",
            preferredFrequency = DoseFrequency.ONCE_DAILY_EVENING,
            clinicalNote = "Hydrophilic profile with fewer drug-to-drug interactions."
        ),
        KnownStatin(
            id = "lovastatin",
            genericName = "Lovastatin",
            brandName = "Mevacor",
            commonDosesMg = listOf(10, 20, 40),
            defaultDoseMg = 20,
            timingGuidance = "With evening meal",
            preferredFrequency = DoseFrequency.ONCE_DAILY_EVENING,
            clinicalNote = "Absorption improves when taken alongside an evening meal."
        ),
        KnownStatin(
            id = "fluvastatin",
            genericName = "Fluvastatin",
            brandName = "Lescol",
            commonDosesMg = listOf(20, 40, 80),
            defaultDoseMg = 40,
            timingGuidance = "Evening or bedtime",
            preferredFrequency = DoseFrequency.ONCE_DAILY_EVENING,
            clinicalNote = "Lower-intensity option sometimes chosen for sensitivity."
        ),
        KnownStatin(
            id = "pitavastatin",
            genericName = "Pitavastatin",
            brandName = "Livalo",
            commonDosesMg = listOf(1, 2, 4),
            defaultDoseMg = 2,
            timingGuidance = "Anytime daily",
            preferredFrequency = DoseFrequency.ONCE_DAILY_EVENING,
            clinicalNote = "Minimal CYP interaction; low mg dosing."
        ),
        KnownStatin(
            id = "ezetimibe",
            genericName = "Ezetimibe",
            brandName = "Zetia",
            commonDosesMg = listOf(10),
            defaultDoseMg = 10,
            timingGuidance = "Once daily, morning or evening",
            preferredFrequency = DoseFrequency.ONCE_DAILY_MORNING,
            clinicalNote = "Blocks intestinal cholesterol absorption; commonly added alongside a statin or bempedoic acid.",
            therapyClass = TherapyClass.CHOLESTEROL_ABSORPTION,
            defaultRole = StatinRegimen.THERAPY_ROLE_ADJUNCT
        ),
        KnownStatin(
            id = "bempedoic_acid",
            genericName = "Bempedoic acid",
            brandName = "Nexletol",
            commonDosesMg = listOf(180),
            defaultDoseMg = 180,
            timingGuidance = "Once daily, with or without food",
            preferredFrequency = DoseFrequency.ONCE_DAILY_MORNING,
            clinicalNote = "Oral liver-selective ACL inhibitor with low skeletal muscle activity; used solo or with ezetimibe/statin.",
            therapyClass = TherapyClass.ACL_INHIBITOR,
            defaultRole = StatinRegimen.THERAPY_ROLE_ADJUNCT
        ),
        KnownStatin(
            id = "evolocumab",
            genericName = "Evolocumab",
            brandName = "Repatha",
            commonDosesMg = listOf(140, 420),
            defaultDoseMg = 140,
            timingGuidance = "Every 2 weeks (140 mg) or Monthly (420 mg)",
            preferredFrequency = DoseFrequency.EVERY_2_WEEKS,
            clinicalNote = "PCSK9 inhibitor injection often prescribed alongside or as an alternative for statin intolerance.",
            therapyClass = TherapyClass.PCSK9,
            defaultRole = StatinRegimen.THERAPY_ROLE_PRIMARY
        ),
        KnownStatin(
            id = "alirocumab",
            genericName = "Alirocumab",
            brandName = "Praluent",
            commonDosesMg = listOf(75, 150, 300),
            defaultDoseMg = 75,
            timingGuidance = "Every 2 weeks (75/150 mg) or Monthly (300 mg)",
            preferredFrequency = DoseFrequency.EVERY_2_WEEKS,
            clinicalNote = "PCSK9 monoclonal antibody injection used alongside maximally tolerated statin or as monotherapy.",
            therapyClass = TherapyClass.PCSK9,
            defaultRole = StatinRegimen.THERAPY_ROLE_PRIMARY
        ),
        KnownStatin(
            id = "inclisiran",
            genericName = "Inclisiran",
            brandName = "Leqvio",
            commonDosesMg = listOf(284),
            defaultDoseMg = 284,
            timingGuidance = "In-clinic injection: Day 1, Month 3, then every 6 months",
            preferredFrequency = DoseFrequency.CUSTOM,
            clinicalNote = "Twice-yearly siRNA PCSK9 therapy administered in clinic alongside oral lipid therapy.",
            therapyClass = TherapyClass.PCSK9,
            defaultRole = StatinRegimen.THERAPY_ROLE_ADJUNCT
        ),
        KnownStatin(
            id = "icosapent_ethyl",
            genericName = "Icosapent ethyl",
            brandName = "Vascepa",
            commonDosesMg = listOf(1000, 2000),
            defaultDoseMg = 2000,
            timingGuidance = "With meals (typically 2000 mg twice daily = 4000 mg/day)",
            preferredFrequency = DoseFrequency.ONCE_DAILY_EVENING,
            clinicalNote = "Purified prescription EPA for elevated triglycerides and cardiovascular risk reduction alongside a statin.",
            therapyClass = TherapyClass.TRIGLYCERIDE_THERAPY,
            defaultRole = StatinRegimen.THERAPY_ROLE_ADJUNCT
        )
    )

    val switchReasons: List<String> = listOf(
        "Doctor prescribed titration",
        "Added non-statin adjunct",
        "Muscle soreness or aches",
        "Fatigue or low energy",
        "Sleep or headache issues",
        "Formulary / pharmacy change",
        "Routine follow-up adjustment"
    )

    fun findByName(name: String): KnownStatin? =
        statins.firstOrNull {
            it.genericName.equals(name.trim(), ignoreCase = true) ||
                it.brandName.equals(name.trim(), ignoreCase = true)
        }
}
