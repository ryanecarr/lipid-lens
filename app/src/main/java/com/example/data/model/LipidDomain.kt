package com.example.data.model

import kotlin.math.roundToInt

enum class LipidMetric(
    val label: String,
    val shortLabel: String,
    val unit: String,
    val guidelineTargetText: String,
    val targetThresholdMgDl: Int?,
    val secondaryTargetMgDl: Int?,
    val secondaryTargetLabel: String?,
    val lowerIsBetter: Boolean
) {
    LDL_C(
        label = "LDL Cholesterol",
        shortLabel = "LDL-C",
        unit = "mg/dL",
        guidelineTargetText = "ACC/AHA & ESC: <100 general • <70 high-risk • <55 very high-risk ASCVD",
        targetThresholdMgDl = 70,
        secondaryTargetMgDl = 55,
        secondaryTargetLabel = "<55 very high-risk",
        lowerIsBetter = true
    ),
    TOTAL_CHOLESTEROL(
        label = "Total Cholesterol",
        shortLabel = "Total Chol",
        unit = "mg/dL",
        guidelineTargetText = "Desirable total cholesterol typically <200 mg/dL",
        targetThresholdMgDl = 200,
        secondaryTargetMgDl = null,
        secondaryTargetLabel = null,
        lowerIsBetter = true
    ),
    NON_HDL_C(
        label = "Non-HDL Cholesterol",
        shortLabel = "Non-HDL-C",
        unit = "mg/dL",
        guidelineTargetText = "ACC/AHA & ESC target typically <100 mg/dL (high-risk) / <85 mg/dL (very high-risk)",
        targetThresholdMgDl = 100,
        secondaryTargetMgDl = 85,
        secondaryTargetLabel = "<85 very high-risk",
        lowerIsBetter = true
    ),
    HDL_C(
        label = "HDL Cholesterol",
        shortLabel = "HDL-C",
        unit = "mg/dL",
        guidelineTargetText = "Higher protective range typically ≥40 mg/dL (men) or ≥50 mg/dL (women)",
        targetThresholdMgDl = 40,
        secondaryTargetMgDl = 50,
        secondaryTargetLabel = "≥50 optimal",
        lowerIsBetter = false
    ),
    TRIGLYCERIDES(
        label = "Triglycerides",
        shortLabel = "Triglycerides",
        unit = "mg/dL",
        guidelineTargetText = "Normal fasting <150 mg/dL (borderline 150–199)",
        targetThresholdMgDl = 150,
        secondaryTargetMgDl = null,
        secondaryTargetLabel = null,
        lowerIsBetter = true
    ),
    APOB(
        label = "Apolipoprotein B",
        shortLabel = "ApoB",
        unit = "mg/dL",
        guidelineTargetText = "ESC/NLA atherogenic particle target <80 mg/dL (high-risk) / <65 mg/dL (very high-risk)",
        targetThresholdMgDl = 80,
        secondaryTargetMgDl = 65,
        secondaryTargetLabel = "<65 very high-risk",
        lowerIsBetter = true
    )
}

enum class LabTimeRange(val label: String, val monthsBack: Int?) {
    THREE_MONTHS("3M", 3),
    SIX_MONTHS("6M", 6),
    ONE_YEAR("1Y", 12),
    ALL("All", null)
}

enum class TargetPreset(
    val riskLabel: String,
    val shortLabel: String,
    val ldlTargetMgDl: Int,
    val nonHdlTargetMgDl: Int,
    val apoBTargetMgDl: Int
) {
    GENERAL("General (<100)", "General <100", 100, 130, 90),
    HIGH_RISK("High Risk (<70)", "High Risk <70", 70, 100, 80),
    VERY_HIGH_RISK("Very High Risk (<55)", "Very High Risk <55", 55, 85, 65),
    CUSTOM("Custom Goal", "Custom", 70, 100, 80);

    companion object {
        fun matchPreset(targets: ClinicalTargets): TargetPreset {
            return entries.firstOrNull { preset ->
                preset != CUSTOM &&
                    preset.ldlTargetMgDl == targets.ldlTargetMgDl &&
                    preset.riskLabel.equals(targets.riskLabel, ignoreCase = true)
            } ?: CUSTOM
        }
    }
}

object LipidAnalytics {
    fun effectiveNonHdl(panel: LipidPanel): Int =
        panel.nonHdlMgDl ?: (panel.totalCholesterolMgDl - panel.hdlMgDl).coerceAtLeast(1)

    fun valueForMetric(panel: LipidPanel, metric: LipidMetric): Int? = when (metric) {
        LipidMetric.LDL_C -> panel.ldlMgDl
        LipidMetric.TOTAL_CHOLESTEROL -> panel.totalCholesterolMgDl
        LipidMetric.NON_HDL_C -> effectiveNonHdl(panel)
        LipidMetric.HDL_C -> panel.hdlMgDl
        LipidMetric.TRIGLYCERIDES -> panel.triglyceridesMgDl
        LipidMetric.APOB -> panel.apoBMgDl
    }

    fun filterPanelsByRange(
        panels: List<LipidPanel>,
        range: LabTimeRange,
        nowMillis: Long = System.currentTimeMillis()
    ): List<LipidPanel> {
        val sortedAsc = panels.sortedWith(
            compareBy<LipidPanel> { it.drawDateKey }
                .thenBy { it.drawnAtMillis }
        )
        val months = range.monthsBack ?: return sortedAsc
        val cutoffDateKey = DateTimeUtils.monthsAgoDateKey(months, nowMillis)
        return sortedAsc.filter { it.drawDateKey >= cutoffDateKey }
    }

    fun resolveBaselinePanel(panels: List<LipidPanel>): LipidPanel? {
        if (panels.isEmpty()) return null
        val sortedAsc = panels.sortedWith(
            compareBy<LipidPanel> { it.drawDateKey }
                .thenBy { it.drawnAtMillis }
        )
        return sortedAsc.firstOrNull { it.baselinePanel }
            ?: sortedAsc.firstOrNull()?.takeIf { sortedAsc.size >= 2 }
    }

    fun targetsForMetric(
        targets: ClinicalTargets,
        metric: LipidMetric
    ): Pair<Int?, Int?> = when (metric) {
        LipidMetric.LDL_C -> targets.ldlTargetMgDl to null
        LipidMetric.NON_HDL_C -> (targets.nonHdlTargetMgDl ?: (targets.ldlTargetMgDl + 30)) to null
        LipidMetric.APOB -> (targets.apoBTargetMgDl ?: metric.targetThresholdMgDl) to null
        LipidMetric.TOTAL_CHOLESTEROL,
        LipidMetric.HDL_C,
        LipidMetric.TRIGLYCERIDES -> metric.targetThresholdMgDl to metric.secondaryTargetMgDl
    }

    fun percentDelta(baseline: Int, latest: Int): Int? {
        if (baseline <= 0) return null
        return (((latest - baseline).toDouble() / baseline.toDouble()) * 100.0).roundToInt()
    }
}

data class ScheduleAdherenceResult(
    val adherencePercent: Int,
    val takenCount: Int,
    val skippedCount: Int,
    val expectedDoses: Int
)

object AdherenceAnalytics {
    private val EPOCH_2000_MILLIS by lazy { DateTimeUtils.utcMillisFromDateKey("2000-01-01") }

    fun isDueOnDate(regimen: StatinRegimen, dateKey: String): Boolean {
        val startKey = if (regimen.startedAtMillis > EPOCH_2000_MILLIS) {
            DateTimeUtils.dateKeyForMillis(regimen.startedAtMillis)
        } else {
            return true
        }
        val endKey = regimen.endedAtMillis?.let { DateTimeUtils.dateKeyForMillis(it) }
        if (dateKey < startKey) return false
        if (endKey != null && dateKey > endKey) return false

        val freq = DoseFrequency.fromKey(regimen.frequencyKey)
        return when (freq) {
            DoseFrequency.ONCE_DAILY_EVENING,
            DoseFrequency.ONCE_DAILY_MORNING,
            DoseFrequency.CUSTOM -> true

            DoseFrequency.EVERY_OTHER_DAY -> {
                val delta = DateTimeUtils.epochDaysFromDateKey(dateKey) -
                    DateTimeUtils.epochDaysFromDateKey(startKey)
                delta >= 0 && delta % 2L == 0L
            }

            DoseFrequency.EVERY_2_WEEKS -> {
                val delta = DateTimeUtils.epochDaysFromDateKey(dateKey) -
                    DateTimeUtils.epochDaysFromDateKey(startKey)
                delta >= 0 && delta % 14L == 0L
            }

            DoseFrequency.ONCE_MONTHLY -> {
                val startDayOfMonth = DateTimeUtils.dayOfMonthFromDateKey(startKey)
                val maxDayInMonth = DateTimeUtils.daysInMonthForDateKey(dateKey)
                val targetDay = minOf(startDayOfMonth, maxDayInMonth)
                DateTimeUtils.dayOfMonthFromDateKey(dateKey) == targetDay
            }
        }
    }

    fun scheduledDueDates(
        regimen: StatinRegimen,
        startDateKey: String,
        endDateKey: String
    ): List<String> {
        if (startDateKey > endDateKey) return emptyList()
        val freq = DoseFrequency.fromKey(regimen.frequencyKey)
        if (freq == DoseFrequency.CUSTOM) return emptyList()

        val startDay = DateTimeUtils.epochDaysFromDateKey(startDateKey)
        val endDay = DateTimeUtils.epochDaysFromDateKey(endDateKey)
        val dates = mutableListOf<String>()
        for (day in startDay..endDay) {
            val key = DateTimeUtils.dateKeyFromEpochDays(day)
            if (isDueOnDate(regimen, key)) {
                dates.add(key)
            }
        }
        return dates
    }

    fun computeWindowAdherence(
        regimens: List<StatinRegimen>,
        logs: List<DoseLog>,
        startDateKey: String,
        endDateKey: String,
        statinNameFilter: String? = null
    ): ScheduleAdherenceResult {
        val windowLogs = logs.filter { log ->
            log.logDateKey in startDateKey..endDateKey &&
                (statinNameFilter == null || log.statinName == statinNameFilter)
        }
        val takenCount = windowLogs.count { it.taken }
        val skippedCount = windowLogs.count { !it.taken }

        val relevantRegimens = regimens.filter { reg ->
            (statinNameFilter == null || reg.statinName == statinNameFilter) &&
                reg.startedAtMillis > EPOCH_2000_MILLIS
        }

        if (relevantRegimens.isEmpty()) {
            val denom = windowLogs.size
            val pct = if (denom == 0) 0 else ((takenCount * 100f) / denom).roundToInt().coerceIn(0, 100)
            return ScheduleAdherenceResult(pct, takenCount, skippedCount, denom)
        }

        var expectedTotal = 0
        relevantRegimens.forEach { reg ->
            val freq = DoseFrequency.fromKey(reg.frequencyKey)
            val regLogsCount = windowLogs.count { it.regimenId == reg.id }
            val regDueCount = if (freq == DoseFrequency.CUSTOM) {
                regLogsCount
            } else {
                scheduledDueDates(reg, startDateKey, endDateKey).size
            }
            expectedTotal += maxOf(regDueCount, regLogsCount)
        }

        val orphanLogs = windowLogs.count { log ->
            relevantRegimens.none { it.id == log.regimenId }
        }
        expectedTotal += orphanLogs

        val pct = when {
            expectedTotal <= 0 && windowLogs.isEmpty() -> {
                val hasActivePeriodic = relevantRegimens.any {
                    it.active && DoseFrequency.fromKey(it.frequencyKey).isPeriodic()
                }
                if (hasActivePeriodic) 100 else 0
            }
            expectedTotal <= 0 -> 0
            else -> ((takenCount * 100f) / expectedTotal).roundToInt().coerceIn(0, 100)
        }
        return ScheduleAdherenceResult(
            adherencePercent = pct,
            takenCount = takenCount,
            skippedCount = skippedCount,
            expectedDoses = expectedTotal
        )
    }
}

enum class RecheckPhase {
    PREPARING,
    IN_WINDOW,
    OVERDUE
}

data class LabRecheckStatus(
    val regimen: StatinRegimen,
    val startDateKey: String,
    val windowOpenDateKey: String,
    val windowCloseDateKey: String,
    val daysElapsed: Int,
    val weeksElapsed: Int,
    val phase: RecheckPhase
)

object LabRecheckCalculator {
    private val EPOCH_2000_MILLIS by lazy { DateTimeUtils.utcMillisFromDateKey("2000-01-01") }
    private const val MIN_FOLLOWUP_POST_START_DAYS = 21
    private const val WINDOW_START_DAYS = 28 // 4 weeks
    private const val WINDOW_END_DAYS = 84 // 12 weeks
    private const val MAX_STALE_WINDOW_DAYS = 210 // ~30 weeks

    fun evaluate(
        activeRegimens: List<StatinRegimen>,
        panels: List<LipidPanel>,
        todayDateKey: String = DateTimeUtils.todayDateKey()
    ): LabRecheckStatus? {
        val newestActive = activeRegimens
            .filter { it.startedAtMillis > EPOCH_2000_MILLIS }
            .maxByOrNull { it.startedAtMillis }
            ?: return null

        val startDateKey = DateTimeUtils.dateKeyForMillis(newestActive.startedAtMillis)
        val startEpochDay = DateTimeUtils.epochDaysFromDateKey(startDateKey)
        val todayEpochDay = DateTimeUtils.epochDaysFromDateKey(todayDateKey)
        val elapsedDays = (todayEpochDay - startEpochDay).toInt()
        if (elapsedDays < 0 || elapsedDays > MAX_STALE_WINDOW_DAYS) return null

        val followupCutoffKey = DateTimeUtils.addDaysToDateKey(startDateKey, MIN_FOLLOWUP_POST_START_DAYS)
        val hasPostSwitchPanel = panels.any { panel ->
            !panel.baselinePanel && panel.drawDateKey >= followupCutoffKey
        }
        if (hasPostSwitchPanel) return null

        val phase = when {
            elapsedDays < WINDOW_START_DAYS -> RecheckPhase.PREPARING
            elapsedDays <= WINDOW_END_DAYS -> RecheckPhase.IN_WINDOW
            else -> RecheckPhase.OVERDUE
        }

        return LabRecheckStatus(
            regimen = newestActive,
            startDateKey = startDateKey,
            windowOpenDateKey = DateTimeUtils.addDaysToDateKey(startDateKey, WINDOW_START_DAYS),
            windowCloseDateKey = DateTimeUtils.addDaysToDateKey(startDateKey, WINDOW_END_DAYS),
            daysElapsed = elapsedDays,
            weeksElapsed = elapsedDays / 7,
            phase = phase
        )
    }
}
