package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DateTimeUtils
import com.example.data.model.DoseFrequency
import com.example.data.model.DoseLog
import com.example.data.model.FeelLevel
import com.example.data.model.KnownStatinCatalog
import com.example.data.model.ReminderSettings
import com.example.data.model.SideEffectOption
import com.example.data.model.StatinRegimen
import com.example.ui.ReportRange
import com.example.ui.StatinViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Lipid Lens and custom firestore database id is configured`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Lipid Lens", context.getString(R.string.app_name))
        assertTrue(context.getString(R.string.firestore_database_id).isNotBlank())
    }

    @Test
    fun `regimen catalog includes Repatha and UI state and report aggregation work across switches`() {
        val repatha = KnownStatinCatalog.findByName("Repatha")
        assertNotNull(repatha)
        assertEquals("Evolocumab", repatha?.genericName)
        assertEquals(listOf(140, 420), repatha?.commonDosesMg)
        assertEquals(DoseFrequency.EVERY_2_WEEKS, repatha?.preferredFrequency)

        val priorRegimen = StatinRegimen(
            id = "reg_atorva",
            userId = "user_1",
            statinName = "Atorvastatin",
            brandName = "Lipitor",
            doseMg = 20,
            frequencyKey = DoseFrequency.ONCE_DAILY_EVENING.storageKey,
            scheduledTimeMinutes = 20 * 60,
            active = false,
            startedAtMillis = 1000L,
            endedAtMillis = 2000L,
            switchReason = "Muscle soreness or aches"
        )
        val activeRepatha = StatinRegimen(
            id = "reg_repatha",
            userId = "user_1",
            statinName = "Evolocumab",
            brandName = "Repatha",
            doseMg = 420,
            frequencyKey = DoseFrequency.ONCE_MONTHLY.storageKey,
            scheduledTimeMinutes = 9 * 60,
            active = true,
            startedAtMillis = 2000L,
            endedAtMillis = null,
            switchReason = "Muscle soreness or aches"
        )
        val logs = listOf(
            DoseLog(
                id = "log_1",
                userId = "user_1",
                regimenId = "reg_atorva",
                statinName = "Atorvastatin",
                brandName = "Lipitor",
                doseMg = 20,
                frequencyKey = DoseFrequency.ONCE_DAILY_EVENING.storageKey,
                taken = true,
                logDateKey = DateTimeUtils.todayDateKey(),
                takenAtMinutes = 20 * 60 + 10,
                loggedAtMillis = 1500L,
                feelLevelKey = FeelLevel.OKAY.storageKey,
                sideEffectsCsv = SideEffectOption.toCsv(setOf(SideEffectOption.MUSCLE_ACHE)),
                notes = "Mild calf tightness"
            )
        )

        val uiState = StatinViewModel.computeUiState(
            regimens = listOf(activeRepatha, priorRegimen),
            logs = logs,
            reminders = ReminderSettings(userId = "user_1", enabled = true, hour = 20, minute = 0),
            reportRange = ReportRange.ALL_HISTORY
        )
        assertEquals("Evolocumab", uiState.activeRegimen?.statinName)
        assertEquals(420, uiState.activeRegimen?.doseMg)
        assertEquals(2, uiState.allRegimens.size)
        assertEquals(1, uiState.currentStreakDays)
        assertEquals(100, uiState.adherence7DayPercent)

        val reportData = StatinViewModel.buildClinicalReportData(uiState)
        assertEquals(100, reportData.adherencePercent)
        assertEquals(1, reportData.takenCount)
        assertEquals(0, reportData.skippedCount)
        assertEquals(2, reportData.regimens.size)
        assertEquals(SideEffectOption.MUSCLE_ACHE, reportData.sideEffectBreakdown.first().first)
    }

    @Test
    fun `lipid analytics computes Non-HDL, timeframe filtering, and percentage changes accurately`() {
        val now = 1_760_000_000_000L
        val oneDayMs = 86_400_000L
        val oldPanel = com.example.data.model.LipidPanel(
            id = "p_old",
            userId = "user_1",
            drawDateKey = DateTimeUtils.dateKeyForMillis(now - 220 * oneDayMs),
            drawnAtMillis = now - 220 * oneDayMs,
            totalCholesterolMgDl = 240,
            ldlMgDl = 160,
            hdlMgDl = 48,
            triglyceridesMgDl = 160
        )
        val midPanel = com.example.data.model.LipidPanel(
            id = "p_mid",
            userId = "user_1",
            drawDateKey = DateTimeUtils.dateKeyForMillis(now - 100 * oneDayMs),
            drawnAtMillis = now - 100 * oneDayMs,
            totalCholesterolMgDl = 185,
            ldlMgDl = 105,
            hdlMgDl = 50,
            triglyceridesMgDl = 150
        )
        val recentPanel = com.example.data.model.LipidPanel(
            id = "p_recent",
            userId = "user_1",
            drawDateKey = DateTimeUtils.dateKeyForMillis(now - 15 * oneDayMs),
            drawnAtMillis = now - 15 * oneDayMs,
            totalCholesterolMgDl = 142,
            ldlMgDl = 64,
            hdlMgDl = 54,
            triglyceridesMgDl = 120,
            apoBMgDl = 62
        )

        val panels = listOf(recentPanel, oldPanel, midPanel)
        assertEquals(192, com.example.data.model.LipidAnalytics.effectiveNonHdl(oldPanel))
        assertEquals(-60, com.example.data.model.LipidAnalytics.percentDelta(160, 64))

        val threeMonth = com.example.data.model.LipidAnalytics.filterPanelsByRange(
            panels,
            com.example.data.model.LabTimeRange.THREE_MONTHS,
            nowMillis = now
        )
        assertEquals(listOf("p_recent"), threeMonth.map { it.id })

        val sixMonth = com.example.data.model.LipidAnalytics.filterPanelsByRange(
            panels,
            com.example.data.model.LabTimeRange.SIX_MONTHS,
            nowMillis = now
        )
        assertEquals(listOf("p_mid", "p_recent"), sixMonth.map { it.id })

        val all = com.example.data.model.LipidAnalytics.filterPanelsByRange(
            panels,
            com.example.data.model.LabTimeRange.ALL,
            nowMillis = now
        )
        assertEquals(listOf("p_old", "p_mid", "p_recent"), all.map { it.id })

        val uiState = StatinViewModel.computeUiState(
            regimens = emptyList(),
            logs = emptyList(),
            lipidPanels = panels,
            reminders = ReminderSettings(userId = "user_1")
        )
        val report = StatinViewModel.buildClinicalReportData(uiState)
        assertEquals(3, report.lipidPanels.size)
    }

    @Test
    fun `date picker utc conversion preserves exact calendar day across negative and positive timezones`() {
        val originalTz = java.util.TimeZone.getDefault()
        val originalLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.US)
            listOf("America/Los_Angeles", "UTC", "Asia/Tokyo").forEach { zoneId ->
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(zoneId))
                val chosenDateKey = "2026-11-19"
                val pickerUtcMillis = DateTimeUtils.utcMillisFromDateKey(chosenDateKey)
                val roundTripped = DateTimeUtils.dateKeyFromUtcMillis(pickerUtcMillis)
                assertEquals("Timezone $zoneId shifted date key", chosenDateKey, roundTripped)
                assertEquals("Thu, Nov 19", DateTimeUtils.formatDateKeyShort(chosenDateKey))
            }

            // Even if drawnAtMillis came from another timezone, drawDateKey ordering & 3M filtering remain consistent
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Los_Angeles"))
            val nov19Utc = DateTimeUtils.utcMillisFromDateKey("2026-11-19")
            val panelOld = com.example.data.model.LipidPanel(
                id = "p1",
                drawDateKey = "2026-06-01",
                drawnAtMillis = 0L
            )
            val panelRecent = com.example.data.model.LipidPanel(
                id = "p2",
                drawDateKey = "2026-10-15",
                drawnAtMillis = 0L
            )
            val filtered3M = com.example.data.model.LipidAnalytics.filterPanelsByRange(
                panels = listOf(panelRecent, panelOld),
                range = com.example.data.model.LabTimeRange.THREE_MONTHS,
                nowMillis = nov19Utc + 12 * 3_600_000L
            )
            assertEquals(listOf("p2"), filtered3M.map { it.id })
        } finally {
            java.util.TimeZone.setDefault(originalTz)
            java.util.Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `clinician visit brief scope key tracks filter and data changes and attaches to ClinicalReportData`() {
        val panels = listOf(
            com.example.data.model.LipidPanel(
                id = "p2",
                userId = "user_1",
                drawDateKey = "2026-11-10",
                drawnAtMillis = DateTimeUtils.utcMillisFromDateKey("2026-11-10"),
                totalCholesterolMgDl = 165,
                ldlMgDl = 88,
                hdlMgDl = 54,
                triglyceridesMgDl = 115,
                apoBMgDl = 79
            ),
            com.example.data.model.LipidPanel(
                id = "p1",
                userId = "user_1",
                drawDateKey = "2026-07-01",
                drawnAtMillis = DateTimeUtils.utcMillisFromDateKey("2026-07-01"),
                totalCholesterolMgDl = 240,
                ldlMgDl = 162,
                hdlMgDl = 48,
                triglyceridesMgDl = 150
            )
        )
        val baseState = StatinViewModel.computeUiState(
            regimens = listOf(
                StatinRegimen(
                    id = "reg_1",
                    userId = "user_1",
                    statinName = "Evolocumab",
                    brandName = "Repatha",
                    doseMg = 140,
                    frequencyKey = DoseFrequency.EVERY_2_WEEKS.storageKey,
                    active = true,
                    startedAtMillis = DateTimeUtils.utcMillisFromDateKey("2026-07-15"),
                    switchReason = "Statin-associated myalgia"
                )
            ),
            logs = emptyList(),
            lipidPanels = panels,
            reminders = ReminderSettings(userId = "user_1"),
            reportRange = ReportRange.LAST_30_DAYS
        )
        val report30 = StatinViewModel.buildClinicalReportData(baseState)
        val key30 = StatinViewModel.reportScopeKey(baseState, report30)

        val filteredState = baseState.copy(reportStatinFilter = "Evolocumab")
        val reportFiltered = StatinViewModel.buildClinicalReportData(filteredState)
        val keyFiltered = StatinViewModel.reportScopeKey(filteredState, reportFiltered)

        val allState = baseState.copy(reportRange = ReportRange.ALL_HISTORY)
        val reportAll = StatinViewModel.buildClinicalReportData(allState)
        val keyAll = StatinViewModel.reportScopeKey(allState, reportAll)

        assertTrue(key30 != keyFiltered)
        assertTrue(key30 != keyAll)

        val brief = com.example.data.remote.ClinicianVisitBrief(
            scopeKey = keyAll,
            lipidTrajectory = "LDL-C declined 46% (162 -> 88 mg/dL) from Jul to Nov 2026; ApoB 79 mg/dL.",
            therapyAndTolerance = "Evolocumab 140 mg q2w started Jul 15 after statin-associated myalgia; 0 side effects logged.",
            clinicalHighlights = listOf(
                "Substantial LDL-C reduction on PCSK9 monotherapy.",
                "ApoB <80 mg/dL suggests low residual atherogenic particle burden."
            ),
            discussionTopics = listOf(
                "Confirm ongoing LDL-C target and routine recheck cadence."
            ),
            generatedAtLabel = "Nov 20, 2026 • 2:50 AM"
        )
        val attached = StatinViewModel.buildClinicalReportData(allState, aiVisitBrief = brief)
        assertEquals(brief, attached.aiVisitBrief)
        assertEquals(2, attached.lipidPanels.size)
        assertEquals(1, attached.regimens.size)
    }

    @Test
    fun `expanded lipid therapy catalog includes non-statins and Vascepa high-dose ceiling`() {
        val zetia = KnownStatinCatalog.findByName("Zetia")
        val nexletol = KnownStatinCatalog.findByName("Bempedoic acid")
        val praluent = KnownStatinCatalog.findByName("Praluent")
        val leqvio = KnownStatinCatalog.findByName("Inclisiran")
        val vascepa = KnownStatinCatalog.findByName("Vascepa")

        assertNotNull(zetia)
        assertEquals(StatinRegimen.THERAPY_ROLE_ADJUNCT, zetia?.defaultRole)
        assertNotNull(nexletol)
        assertEquals(180, nexletol?.defaultDoseMg)
        assertNotNull(praluent)
        assertEquals(DoseFrequency.EVERY_2_WEEKS, praluent?.preferredFrequency)
        assertNotNull(leqvio)
        assertEquals(DoseFrequency.CUSTOM, leqvio?.preferredFrequency)
        assertNotNull(vascepa)
        assertEquals(2000, vascepa?.defaultDoseMg)
        assertEquals(listOf(1000, 2000), vascepa?.commonDosesMg)
    }

    @Test
    fun `combination primary and adjunct regimens remain active concurrently in UI state and report`() {
        val startMillis = DateTimeUtils.utcMillisFromDateKey("2026-11-01") + 12 * 3_600_000L
        val rosuva = StatinRegimen(
            id = "reg_rosuva",
            userId = "user_1",
            statinName = "Rosuvastatin",
            brandName = "Crestor",
            doseMg = 20,
            therapyRole = StatinRegimen.THERAPY_ROLE_PRIMARY,
            frequencyKey = DoseFrequency.ONCE_DAILY_EVENING.storageKey,
            active = true,
            startedAtMillis = startMillis
        )
        val ezetimibe = StatinRegimen(
            id = "reg_zetia",
            userId = "user_1",
            statinName = "Ezetimibe",
            brandName = "Zetia",
            doseMg = 10,
            therapyRole = StatinRegimen.THERAPY_ROLE_ADJUNCT,
            frequencyKey = DoseFrequency.ONCE_DAILY_MORNING.storageKey,
            active = true,
            startedAtMillis = startMillis + 86_400_000L
        )
        val today = DateTimeUtils.todayDateKey()
        val logs = listOf(
            DoseLog(
                id = "l1",
                userId = "user_1",
                regimenId = "reg_rosuva",
                statinName = "Rosuvastatin",
                doseMg = 20,
                taken = true,
                logDateKey = today
            ),
            DoseLog(
                id = "l2",
                userId = "user_1",
                regimenId = "reg_zetia",
                statinName = "Ezetimibe",
                doseMg = 10,
                taken = true,
                logDateKey = today
            )
        )

        val state = StatinViewModel.computeUiState(
            regimens = listOf(ezetimibe, rosuva),
            logs = logs,
            reminders = ReminderSettings(userId = "user_1"),
            todayDateKey = today,
            reportRange = ReportRange.CURRENT_REGIMEN
        )

        assertEquals(2, state.activeRegimens.size)
        assertEquals("Rosuvastatin", state.activePrimaryRegimen?.statinName)
        assertEquals(listOf("Ezetimibe"), state.activeAdjunctRegimens.map { it.statinName })
        assertEquals(2, state.todayLogsByRegimenId.size)

        val report = StatinViewModel.buildClinicalReportData(state, todayDateKey = today)
        assertEquals(2, report.regimens.size)
        assertEquals(2, report.takenCount)
    }

    @Test
    fun `schedule-aware adherence and reminder trigger calculation respect periodic frequencies`() {
        val originalTz = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
            val startKey = "2026-10-01"
            val startMillis = DateTimeUtils.utcMillisFromDateKey(startKey) + 9 * 3_600_000L

            val repatha = StatinRegimen(
                id = "reg_q2w",
                userId = "user_1",
                statinName = "Evolocumab",
                brandName = "Repatha",
                doseMg = 140,
                frequencyKey = DoseFrequency.EVERY_2_WEEKS.storageKey,
                active = true,
                startedAtMillis = startMillis
            )
            // Due dates in Oct 1 .. Oct 30: Oct 1, Oct 15, Oct 29 (3 doses)
            val dueQ2w = com.example.data.model.AdherenceAnalytics.scheduledDueDates(
                repatha,
                "2026-10-01",
                "2026-10-30"
            )
            assertEquals(listOf("2026-10-01", "2026-10-15", "2026-10-29"), dueQ2w)

            val q2wLogs = listOf(
                DoseLog(id = "d1", regimenId = "reg_q2w", statinName = "Evolocumab", taken = true, logDateKey = "2026-10-01"),
                DoseLog(id = "d2", regimenId = "reg_q2w", statinName = "Evolocumab", taken = true, logDateKey = "2026-10-15"),
                DoseLog(id = "d3", regimenId = "reg_q2w", statinName = "Evolocumab", taken = true, logDateKey = "2026-10-29")
            )
            val q2wAdherence = com.example.data.model.AdherenceAnalytics.computeWindowAdherence(
                regimens = listOf(repatha),
                logs = q2wLogs,
                startDateKey = "2026-10-01",
                endDateKey = "2026-10-30"
            )
            assertEquals(3, q2wAdherence.expectedDoses)
            assertEquals(100, q2wAdherence.adherencePercent)

            // Every other day over 6 days (Oct 1, 3, 5 -> 3 due doses; 2 taken -> 67%)
            val eod = repatha.copy(
                id = "reg_eod",
                frequencyKey = DoseFrequency.EVERY_OTHER_DAY.storageKey
            )
            val eodDue = com.example.data.model.AdherenceAnalytics.scheduledDueDates(
                eod,
                "2026-10-01",
                "2026-10-06"
            )
            assertEquals(listOf("2026-10-01", "2026-10-03", "2026-10-05"), eodDue)
            val eodAdherence = com.example.data.model.AdherenceAnalytics.computeWindowAdherence(
                regimens = listOf(eod),
                logs = listOf(
                    DoseLog(id = "e1", regimenId = "reg_eod", statinName = "Evolocumab", taken = true, logDateKey = "2026-10-01"),
                    DoseLog(id = "e2", regimenId = "reg_eod", statinName = "Evolocumab", taken = true, logDateKey = "2026-10-03")
                ),
                startDateKey = "2026-10-01",
                endDateKey = "2026-10-06"
            )
            assertEquals(67, eodAdherence.adherencePercent)

            // Monthly anchored on Oct 1 -> next trigger after Oct 15 09:00 UTC is Nov 1 09:00 UTC
            val oct15 = DateTimeUtils.utcMillisFromDateKey("2026-10-15") + 10 * 3_600_000L
            val nextMonthly = com.example.notifications.ReminderScheduler.computeNextTriggerMillis(
                hour = 9,
                minute = 0,
                frequency = DoseFrequency.ONCE_MONTHLY,
                startedAtMillis = startMillis,
                nowMillis = oct15
            )
            assertEquals("2026-11-01", DateTimeUtils.dateKeyFromUtcMillis(nextMonthly))
        } finally {
            java.util.TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun `lab recheck calculator tracks 4 to 12 week window and clears on post-switch non-baseline panel`() {
        val originalTz = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
            val startKey = "2026-08-01"
            val regimen = StatinRegimen(
                id = "reg_1",
                statinName = "Rosuvastatin",
                doseMg = 20,
                active = true,
                startedAtMillis = DateTimeUtils.utcMillisFromDateKey(startKey) + 12 * 3_600_000L
            )
            val baselineOnly = listOf(
                com.example.data.model.LipidPanel(
                    id = "p_base",
                    drawDateKey = "2026-09-05",
                    ldlMgDl = 155,
                    baselinePanel = true
                )
            )

            val week2 = com.example.data.model.LabRecheckCalculator.evaluate(
                activeRegimens = listOf(regimen),
                panels = baselineOnly,
                todayDateKey = DateTimeUtils.addDaysToDateKey(startKey, 14)
            )
            assertEquals(com.example.data.model.RecheckPhase.PREPARING, week2?.phase)

            val week6 = com.example.data.model.LabRecheckCalculator.evaluate(
                activeRegimens = listOf(regimen),
                panels = baselineOnly,
                todayDateKey = DateTimeUtils.addDaysToDateKey(startKey, 42)
            )
            assertEquals(com.example.data.model.RecheckPhase.IN_WINDOW, week6?.phase)

            val week14 = com.example.data.model.LabRecheckCalculator.evaluate(
                activeRegimens = listOf(regimen),
                panels = baselineOnly,
                todayDateKey = DateTimeUtils.addDaysToDateKey(startKey, 98)
            )
            assertEquals(com.example.data.model.RecheckPhase.OVERDUE, week14?.phase)

            val withFollowUp = baselineOnly + com.example.data.model.LipidPanel(
                id = "p_followup",
                drawDateKey = DateTimeUtils.addDaysToDateKey(startKey, 35),
                ldlMgDl = 72,
                baselinePanel = false
            )
            val cleared = com.example.data.model.LabRecheckCalculator.evaluate(
                activeRegimens = listOf(regimen),
                panels = withFollowUp,
                todayDateKey = DateTimeUtils.addDaysToDateKey(startKey, 45)
            )
            assertEquals(null, cleared)
        } finally {
            java.util.TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun `clinical targets and baseline panel anchor override defaults accurately`() {
        val targets = com.example.data.model.ClinicalTargets(
            userId = "user_1",
            ldlTargetMgDl = 55,
            nonHdlTargetMgDl = 85,
            apoBTargetMgDl = 65,
            riskLabel = "Very High Risk (<55)"
        )
        assertEquals(55 to null, com.example.data.model.LipidAnalytics.targetsForMetric(targets, com.example.data.model.LipidMetric.LDL_C))
        assertEquals(85 to null, com.example.data.model.LipidAnalytics.targetsForMetric(targets, com.example.data.model.LipidMetric.NON_HDL_C))
        assertEquals(65 to null, com.example.data.model.LipidAnalytics.targetsForMetric(targets, com.example.data.model.LipidMetric.APOB))

        val earliest = com.example.data.model.LipidPanel(id = "p1", drawDateKey = "2026-01-10", ldlMgDl = 140, baselinePanel = false)
        val explicitBase = com.example.data.model.LipidPanel(id = "p2", drawDateKey = "2026-05-10", ldlMgDl = 175, baselinePanel = true)
        val latest = com.example.data.model.LipidPanel(id = "p3", drawDateKey = "2026-08-10", ldlMgDl = 52, baselinePanel = false)

        val resolved = com.example.data.model.LipidAnalytics.resolveBaselinePanel(listOf(latest, earliest, explicitBase))
        assertEquals("p2", resolved?.id)
    }
}
