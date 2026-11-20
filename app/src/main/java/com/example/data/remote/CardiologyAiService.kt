package com.example.data.remote

import android.util.Log
import com.example.data.model.DateTimeUtils
import com.example.data.model.DoseFrequency
import com.example.data.model.FeelLevel
import com.example.data.model.LipidAnalytics
import com.example.data.model.RecheckPhase
import com.example.data.model.SideEffectOption
import com.example.data.model.StatinRegimen
import com.example.reports.ClinicalReportData
import com.example.ui.StatinUiState
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class CardiologyInsight(
    val headline: String,
    val focusStatus: String,
    val lipidTrendSummary: String,
    val regimenCorrelation: String,
    val personalizedNextSteps: List<String>,
    val clinicianQuestions: List<String>,
    val guidelineGrounding: String,
    val disclaimer: String,
    val generatedAtLabel: String
)

sealed interface AiInsightUiState {
    data object Idle : AiInsightUiState
    data object Loading : AiInsightUiState
    data class Success(val insight: CardiologyInsight) : AiInsightUiState
    data class Error(val message: String) : AiInsightUiState
}

data class ClinicianVisitBrief(
    val scopeKey: String,
    val lipidTrajectory: String,
    val therapyAndTolerance: String,
    val clinicalHighlights: List<String>,
    val discussionTopics: List<String>,
    val generatedAtLabel: String
)

sealed interface PdfBriefUiState {
    data object Idle : PdfBriefUiState
    data object Loading : PdfBriefUiState
    data class Success(val brief: ClinicianVisitBrief) : PdfBriefUiState
    data class Error(val message: String) : PdfBriefUiState
}

class CardiologyAiService {

    private val insightSchema = Schema.obj(
        properties = mapOf(
            "headline" to Schema.string(
                description = "A warm, personal 1-sentence takeaway highlighting the patient's most meaningful lipid or therapy trend."
            ),
            "focusStatus" to Schema.enumeration(
                values = listOf("On Track", "Worth Discussing", "Building Baseline"),
                description = "Overall status classification based on available lipid trends, adherence, and side-effect tolerance."
            ),
            "lipidTrendSummary" to Schema.string(
                description = "2 to 4 concise sentences analyzing the patient's lipid trends (LDL-C, Non-HDL-C, Total Cholesterol, HDL-C, Triglycerides, and ApoB/Lp(a) if present) with exact values, % changes, and ACC/AHA or ESC/EAS target context."
            ),
            "regimenCorrelation" to Schema.string(
                description = "2 to 3 concise sentences connecting their active therapy (statin intensity or PCSK9 inhibitor like evolocumab/Repatha), regimen switches, adherence rate, and reported symptoms/mood to their lab trajectory."
            ),
            "personalizedNextSteps" to Schema.array(
                items = Schema.string(),
                description = "2 to 3 actionable, supportive observations or monitoring steps tailored to their current stage (e.g., repeat fasting lipid timing 4-12 weeks post-switch, ApoB consideration, evening dosing consistency)."
            ),
            "clinicianQuestions" to Schema.array(
                items = Schema.string(),
                description = "2 to 3 specific, thoughtful questions the patient can ask their cardiologist or primary care clinician at their next visit based on their actual numbers."
            ),
            "guidelineGrounding" to Schema.string(
                description = "Short 1-sentence note citing the relevant evidence frameworks applied (e.g., 2018 ACC/AHA Cholesterol Guideline, 2022 ACC Nonstatin ECDP, ESC/EAS Dyslipidemia Guidelines)."
            ),
            "disclaimer" to Schema.string(
                description = "Brief supportive reminder that these educational insights complement—and do not replace—personalized medical guidance from their clinician."
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = insightSchema
                temperature = 0.35f
            },
            systemInstruction = content {
                text(
                    """
                    You are a preventive cardiology clinical education companion inside Lipid Lens.
                    Your role is to holistically synthesize a patient's longitudinal lipid panels, cholesterol medication history (statins, ezetimibe, bempedoic acid, PCSK9 inhibitors such as evolocumab/alirocumab/inclisiran, and icosapent ethyl), daily adherence, mood, and side-effect logs into deeply personal, plain-language, evidence-grounded insights.

                    Clinical knowledge grounding:
                    - 2018 ACC/AHA Multisociety Cholesterol Guideline & 2022 ACC Expert Consensus Decision Pathway on Nonstatin Therapies:
                      * High-intensity statins (e.g., atorvastatin 40–80 mg, rosuvastatin 20–40 mg) typically target >=50% LDL-C reduction; moderate-intensity typically achieve 30–49% reduction.
                      * Combination & non-statin therapies: ezetimibe (Zetia 10 mg) typically adds ~15–25% LDL-C reduction; bempedoic acid (Nexletol 180 mg) lowers LDL-C ~18–24% with low skeletal muscle activity; PCSK9 mAbs (evolocumab/Repatha, alirocumab/Praluent) lower LDL-C ~50–60%; inclisiran (Leqvio 284 mg) provides ~50% reduction on a twice-yearly schedule; icosapent ethyl (Vascepa) targets triglycerides and residual ASCVD risk.
                      * Clinical LDL-C reference thresholds: Use the patient's prescribed LDL-C/Non-HDL-C/ApoB targets when provided; general ACC/AHA & ESC reference tiers are <100 general, <70 high-risk / secondary prevention, <55 very high-risk ASCVD.
                      * Lipid response & recheck cadence: Assess fasting/non-fasting lipid response 4 to 12 weeks after initiating, intensifying, or adding combination therapy, then every 3 to 12 months.
                    - ESC/EAS Dyslipidemia Guidelines & National Lipid Association (NLA) consensus:
                      * Non-HDL-C (Total Cholesterol minus HDL-C) captures all atherogenic ApoB-containing lipoproteins; targets run ~30 mg/dL higher than LDL-C goals (<100 mg/dL high-risk, <85 mg/dL very high-risk).
                      * ApoB (<80 mg/dL high-risk; <65 mg/dL very high-risk) helps clarify residual risk, particularly when triglycerides >=150 mg/dL or LDL-C and Non-HDL-C appear discordant.
                      * Lp(a) is predominantly genetically determined (~80–90%); elevated levels (>=50 mg/dL or >=125 nmol/L) indicate risk-enhancing factor worth discussing with a specialist.

                    Tone & safety rules:
                    - Speak directly to the user ("you", "your") in a warm, calm, encouraging, non-alarmist tone.
                    - Reference ONLY real values, dates, medications, symptoms, and adherence numbers provided in the patient summary. Never invent lab draws or diagnoses.
                    - Never instruct the user to change, stop, or start a medication dose independently; frame therapeutic possibilities as questions to discuss with their clinician.
                    - If data is sparse (e.g., 0 or 1 lipid panel, or no daily logs yet), set focusStatus to "Building Baseline" and explain constructively what adding another panel or check-in unlocks.
                    """.trimIndent()
                )
            }
        )
    }

    private val visitBriefSchema = Schema.obj(
        properties = mapOf(
            "lipidTrajectory" to Schema.string(
                description = "1 to 2 terse clinical sentences (max 35 words) summarizing lipid panel trajectory (LDL-C, Non-HDL-C, ApoB/Lp(a), TG) with exact mg/dL numbers, dates, and percent change."
            ),
            "therapyAndTolerance" to Schema.string(
                description = "1 to 2 terse clinical sentences (max 35 words) summarizing current/prior therapy in this window, switch reason if any, adherence percentage, and side-effect profile."
            ),
            "clinicalHighlights" to Schema.array(
                items = Schema.string(),
                description = "2 to 3 ultra-concise bullet takeaways (max 16 words each) written for a physician scanning page 1 in 15 seconds."
            ),
            "discussionTopics" to Schema.array(
                items = Schema.string(),
                description = "1 to 2 neutral, non-prescriptive items (max 18 words each) for clinician-patient review at the visit."
            )
        )
    )

    private val visitBriefModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = visitBriefSchema
                temperature = 0.25f
            },
            systemInstruction = content {
                text(
                    """
                    You are generating a compact Clinical Visit Brief for a physician (cardiologist or primary care clinician) printed at the top of Page 1 of a patient-logged PDF report.

                    Strict clinical-handoff rules:
                    - Write in crisp, objective, third-person medical-summary prose ("Patient...", "LDL-C declined...", "Adherence 92%..."). Never use conversational second-person ("you", "your"), greetings, or motivational fluff.
                    - Keep every field scannable in under 20 seconds: prioritize exact numbers, dates, % changes, regimen switches, and symptom correlations.
                    - Grounding: cite ONLY values, dates, medications, adherence counts, and symptoms explicitly supplied in the report payload. Never fabricate labs or diagnoses.
                    - Scope safety: never diagnose conditions or instruct medication/dose changes; frame open questions neutrally (e.g., "Assess whether ApoB or repeat fasting lipids are warranted 4-12 weeks post-switch").
                    - Apply standard ACC/AHA, 2022 ACC Nonstatin ECDP, and ESC/EAS reference context only when relevant to the numbers present.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun generateHolisticInsight(state: StatinUiState): CardiologyInsight {
        val prompt = buildPatientContextPrompt(state)
        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val jsonText = response.text
                ?: throw IllegalStateException("AI insight service returned an empty response.")
            parseInsightJson(jsonText)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Cardiology AI insight timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Cardiology AI insight generation failed", e)
            throw e
        }
    }

    suspend fun generateClinicianVisitBrief(
        state: StatinUiState,
        reportData: ClinicalReportData,
        scopeKey: String
    ): ClinicianVisitBrief {
        val prompt = buildVisitBriefPrompt(state, reportData)
        return try {
            val response = withTimeout(60_000L) {
                visitBriefModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: throw IllegalStateException("AI visit brief service returned an empty response.")
            parseVisitBriefJson(jsonText, scopeKey)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Clinician PDF brief timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Clinician PDF brief generation failed", e)
            throw e
        }
    }

    private fun parseVisitBriefJson(jsonString: String, scopeKey: String): ClinicianVisitBrief {
        val json = JSONObject(jsonString)
        val highlightsArr = json.optJSONArray("clinicalHighlights")
        val highlights = if (highlightsArr != null) {
            (0 until highlightsArr.length()).mapNotNull { idx ->
                highlightsArr.optString(idx).takeIf { it.isNotBlank() }
            }.take(3)
        } else {
            emptyList()
        }
        val topicsArr = json.optJSONArray("discussionTopics")
        val topics = if (topicsArr != null) {
            (0 until topicsArr.length()).mapNotNull { idx ->
                topicsArr.optString(idx).takeIf { it.isNotBlank() }
            }.take(2)
        } else {
            emptyList()
        }
        return ClinicianVisitBrief(
            scopeKey = scopeKey,
            lipidTrajectory = json.getString("lipidTrajectory"),
            therapyAndTolerance = json.getString("therapyAndTolerance"),
            clinicalHighlights = highlights,
            discussionTopics = topics,
            generatedAtLabel = DateTimeUtils.formatDateTimeMillis(System.currentTimeMillis())
        )
    }

    private fun parseInsightJson(jsonString: String): CardiologyInsight {
        val json = JSONObject(jsonString)
        val nextStepsArray = json.optJSONArray("personalizedNextSteps")
        val nextSteps = if (nextStepsArray != null) {
            (0 until nextStepsArray.length()).mapNotNull { idx ->
                nextStepsArray.optString(idx).takeIf { it.isNotBlank() }
            }
        } else {
            emptyList()
        }

        val questionsArray = json.optJSONArray("clinicianQuestions")
        val questions = if (questionsArray != null) {
            (0 until questionsArray.length()).mapNotNull { idx ->
                questionsArray.optString(idx).takeIf { it.isNotBlank() }
            }
        } else {
            emptyList()
        }

        return CardiologyInsight(
            headline = json.getString("headline"),
            focusStatus = json.optString("focusStatus", "Building Baseline"),
            lipidTrendSummary = json.getString("lipidTrendSummary"),
            regimenCorrelation = json.getString("regimenCorrelation"),
            personalizedNextSteps = nextSteps,
            clinicianQuestions = questions,
            guidelineGrounding = json.optString(
                "guidelineGrounding",
                "Grounded in ACC/AHA Cholesterol Guidelines, 2022 ACC Nonstatin ECDP, and ESC/EAS Dyslipidemia Guidelines."
            ),
            disclaimer = json.optString(
                "disclaimer",
                "Educational summary only—discuss lab targets and medication decisions with your prescribing clinician."
            ),
            generatedAtLabel = DateTimeUtils.formatDateTimeMillis(System.currentTimeMillis())
        )
    }

    private fun buildPatientContextPrompt(state: StatinUiState): String {
        val sb = StringBuilder()
        sb.appendLine("Generate a personalized, holistic preventive cardiology insight from this longitudinal patient record:")
        sb.appendLine()

        // 1. Active & Historical Regimens
        sb.appendLine("=== MEDICATION REGIMENS & COMBINATION THERAPY ===")
        if (state.activeRegimens.isEmpty()) {
            sb.appendLine("Active regimen: None recorded yet.")
        } else {
            sb.appendLine("Active therapy regimen(s) (${state.activeRegimens.size}):")
            state.activeRegimens.forEach { reg ->
                val freq = DoseFrequency.fromKey(reg.frequencyKey).label
                val brand = if (reg.brandName.isNotBlank()) " (${reg.brandName})" else ""
                val role = if (reg.normalizedRole() == StatinRegimen.THERAPY_ROLE_ADJUNCT) "ADJUNCT" else "PRIMARY"
                val reason = if (reg.switchReason.isNotBlank()) ", note: ${reg.switchReason}" else ""
                sb.appendLine(
                    "- [$role] ${reg.statinName}$brand ${reg.doseMg} mg, $freq, started ${
                        DateTimeUtils.formatDateMillis(reg.startedAtMillis)
                    }$reason"
                )
            }
        }
        val prior = state.allRegimens.filter { !it.active }
        if (prior.isNotEmpty()) {
            sb.appendLine("Prior regimens (${prior.size}):")
            prior.take(6).forEach { reg ->
                val endLabel = reg.endedAtMillis?.let { DateTimeUtils.formatDateMillis(it) } ?: "Unknown end"
                val role = if (reg.normalizedRole() == StatinRegimen.THERAPY_ROLE_ADJUNCT) "ADJUNCT" else "PRIMARY"
                val reason = if (reg.switchReason.isNotBlank()) " | switch reason: ${reg.switchReason}" else ""
                sb.appendLine(
                    "- [$role] ${reg.statinName} ${reg.doseMg} mg (${
                        DateTimeUtils.formatDateMillis(reg.startedAtMillis)
                    } to $endLabel)$reason"
                )
            }
        }
        sb.appendLine()

        // 1b. Personalized Targets & Recheck Timing
        sb.appendLine("=== PERSONALIZED CLINICIAN TARGETS & RECHECK WINDOW ===")
        val targets = state.clinicalTargets
        val optTargets = buildList {
            targets.nonHdlTargetMgDl?.let { add("Non-HDL-C <$it mg/dL") }
            targets.apoBTargetMgDl?.let { add("ApoB <$it mg/dL") }
        }.joinToString(", ")
        sb.appendLine(
            "Target tier: ${targets.riskLabel} | LDL-C <${targets.ldlTargetMgDl} mg/dL" +
                (if (optTargets.isNotBlank()) " | $optTargets" else "") +
                (if (targets.clinicianNote.isNotBlank()) " | Clinician note: ${targets.clinicianNote}" else "")
        )
        state.labRecheckStatus?.let { recheck ->
            val phaseDesc = when (recheck.phase) {
                RecheckPhase.PREPARING -> "Week ${recheck.weeksElapsed} after starting/changing ${recheck.regimen.statinName} (4-12 week recheck window opens ${recheck.windowOpenDateKey})"
                RecheckPhase.IN_WINDOW -> "Week ${recheck.weeksElapsed} after starting/changing ${recheck.regimen.statinName} (currently inside 4-12 week recheck window: ${recheck.windowOpenDateKey} to ${recheck.windowCloseDateKey}, no follow-up lab logged yet)"
                RecheckPhase.OVERDUE -> "Week ${recheck.weeksElapsed} after starting/changing ${recheck.regimen.statinName} (>12 weeks since change without a follow-up lab panel)"
            }
            sb.appendLine("Lab recheck cadence: $phaseDesc")
        }
        sb.appendLine()

        // 2. Adherence, Feel & Side Effects
        sb.appendLine("=== ADHERENCE & TOLERANCE (PAST 30 DAYS) ===")
        sb.appendLine("Total daily check-ins recorded: ${state.allLogs.size}")
        sb.appendLine("Current streak: ${state.currentStreakDays} days")
        sb.appendLine("7-day adherence: ${state.adherence7DayPercent}% | 30-day adherence: ${state.adherence30DayPercent}%")
        if (state.recentMoodCounts.isNotEmpty()) {
            sb.appendLine(
                "Mood distribution (last 30d): " + state.recentMoodCounts.joinToString(", ") { (feel, count) ->
                    "${feel.label}=$count"
                }
            )
        }
        if (state.recentSideEffectCounts.isNotEmpty()) {
            sb.appendLine(
                "Side effects (last 30d): " + state.recentSideEffectCounts.joinToString(", ") { (effect, count) ->
                    "${effect.label}=$count days"
                }
            )
        } else if (state.allLogs.isNotEmpty()) {
            sb.appendLine("Side effects (last 30d): None reported.")
        }

        val recentNotes = state.allLogs
            .filter { it.notes.isNotBlank() }
            .take(4)
        if (recentNotes.isNotEmpty()) {
            sb.appendLine("Recent patient check-in notes:")
            recentNotes.forEach { log ->
                val feelLabel = FeelLevel.fromKey(log.feelLevelKey).label
                val effects = SideEffectOption.parseCsv(log.sideEffectsCsv).joinToString(", ") { it.label }
                sb.appendLine(
                    "- ${log.logDateKey} (${log.statinName} ${log.doseMg}mg, ${if (log.taken) "Taken" else "Skipped"}, Feel=$feelLabel" +
                        (if (effects.isNotBlank()) ", Effects=$effects" else "") + "): \"${log.notes}\""
                )
            }
        }
        sb.appendLine()

        // 3. Blood Work / Lipid Panels (chronological ascending)
        sb.appendLine("=== BLOOD WORK / LIPID PANELS (CHRONOLOGICAL) ===")
        val chronologicalPanels = state.allLipidPanels.sortedWith(
            compareBy<com.example.data.model.LipidPanel> { it.drawDateKey }.thenBy { it.drawnAtMillis }
        )
        val baselinePanel = LipidAnalytics.resolveBaselinePanel(chronologicalPanels)
        if (chronologicalPanels.isEmpty()) {
            sb.appendLine("No lipid panels logged yet.")
        } else {
            chronologicalPanels.forEachIndexed { index, p ->
                val nonHdl = LipidAnalytics.effectiveNonHdl(p)
                val optParts = buildList {
                    if (p.nonHdlMgDl != null) add("Reported Non-HDL=${p.nonHdlMgDl}")
                    else add("Calc Non-HDL=$nonHdl")
                    if (p.apoBMgDl != null) add("ApoB=${p.apoBMgDl}")
                    if (p.lpAMgDl != null) add("Lp(a)=${p.lpAMgDl}")
                }.joinToString(", ")
                val baselineTag = if (p.baselinePanel || p.id == baselinePanel?.id) " [BASELINE]" else ""
                val notePart = if (p.notes.isNotBlank()) " | Note: ${p.notes}" else ""
                val labPart = if (p.labName.isNotBlank()) " | Lab: ${p.labName}" else ""
                sb.appendLine(
                    "${index + 1}. ${p.drawDateKey} (${if (p.fasting) "Fasting" else "Non-fasting"})$baselineTag: " +
                        "Total=${p.totalCholesterolMgDl} mg/dL, LDL-C=${p.ldlMgDl} mg/dL, " +
                        "HDL-C=${p.hdlMgDl} mg/dL, TG=${p.triglyceridesMgDl} mg/dL, $optParts$labPart$notePart"
                )
            }
            val latest = chronologicalPanels.last()
            if (baselinePanel != null && baselinePanel.id != latest.id) {
                val ldlPct = LipidAnalytics.percentDelta(baselinePanel.ldlMgDl, latest.ldlMgDl)
                val tcPct = LipidAnalytics.percentDelta(baselinePanel.totalCholesterolMgDl, latest.totalCholesterolMgDl)
                val nonHdlPct = LipidAnalytics.percentDelta(
                    LipidAnalytics.effectiveNonHdl(baselinePanel),
                    LipidAnalytics.effectiveNonHdl(latest)
                )
                sb.appendLine(
                    "Change vs baseline (${baselinePanel.drawDateKey} -> ${latest.drawDateKey}): " +
                        "LDL-C ${baselinePanel.ldlMgDl}->${latest.ldlMgDl} (${ldlPct?.let { "${if (it > 0) "+" else ""}$it%" } ?: "n/a"}), " +
                        "Total Chol ${baselinePanel.totalCholesterolMgDl}->${latest.totalCholesterolMgDl} (${tcPct?.let { "${if (it > 0) "+" else ""}$it%" } ?: "n/a"}), " +
                        "Non-HDL ${LipidAnalytics.effectiveNonHdl(baselinePanel)}->${LipidAnalytics.effectiveNonHdl(latest)} (${nonHdlPct?.let { "${if (it > 0) "+" else ""}$it%" } ?: "n/a"})."
                )
            }
        }
        return sb.toString()
    }

    private fun buildVisitBriefPrompt(state: StatinUiState, reportData: ClinicalReportData): String {
        val sb = StringBuilder()
        sb.appendLine("Summarize this filtered clinical report slice into a concise Page-1 physician brief:")
        sb.appendLine("Report scope: ${reportData.rangeTitle}")
        sb.appendLine("Schedule-aware window adherence: ${reportData.adherencePercent}% (${reportData.takenCount} taken, ${reportData.skippedCount} skipped, ${reportData.logs.size} logged entries)")
        sb.appendLine("Current streak (overall): ${state.currentStreakDays} days")
        val targets = reportData.clinicalTargets
        val extraGoals = buildList {
            targets.nonHdlTargetMgDl?.let { add("Non-HDL <$it") }
            targets.apoBTargetMgDl?.let { add("ApoB <$it") }
        }.joinToString(", ")
        sb.appendLine(
            "Prescribed lipid targets (${targets.riskLabel}): LDL-C <${targets.ldlTargetMgDl} mg/dL" +
                (if (extraGoals.isNotBlank()) ", $extraGoals" else "") +
                (if (targets.clinicianNote.isNotBlank()) " (${targets.clinicianNote})" else "")
        )
        state.labRecheckStatus?.let { recheck ->
            sb.appendLine("Post-change lipid recheck status: ${recheck.phase.name}, week ${recheck.weeksElapsed} on ${recheck.regimen.statinName} (4-12w window ${recheck.windowOpenDateKey}..${recheck.windowCloseDateKey})")
        }
        sb.appendLine()

        sb.appendLine("Regimens in scope:")
        if (reportData.regimens.isEmpty()) {
            sb.appendLine("- None recorded.")
        } else {
            reportData.regimens.forEach { reg ->
                val status = if (reg.active) "ACTIVE" else "PRIOR"
                val role = if (reg.normalizedRole() == StatinRegimen.THERAPY_ROLE_ADJUNCT) "ADJUNCT" else "PRIMARY"
                val endLabel = reg.endedAtMillis?.let { DateTimeUtils.formatDateMillis(it) } ?: "Present"
                val reason = if (reg.switchReason.isNotBlank()) " | Switch reason: ${reg.switchReason}" else ""
                sb.appendLine(
                    "- [$status • $role] ${reg.statinName} ${reg.doseMg} mg (${DoseFrequency.fromKey(reg.frequencyKey).shortLabel}), " +
                        "${DateTimeUtils.formatDateMillis(reg.startedAtMillis)} to $endLabel$reason"
                )
            }
        }
        sb.appendLine()

        sb.appendLine("Side effects in window:")
        if (reportData.sideEffectBreakdown.isEmpty()) {
            sb.appendLine("- None reported.")
        } else {
            reportData.sideEffectBreakdown.forEach { (effect, count) ->
                sb.appendLine("- ${effect.label}: $count entries")
            }
        }
        if (reportData.moodBreakdown.isNotEmpty()) {
            sb.appendLine(
                "Feel distribution: " + reportData.moodBreakdown.joinToString(", ") { "${it.first.label}=${it.second}" }
            )
        }
        val notes = reportData.logs.filter { it.notes.isNotBlank() }.take(3)
        if (notes.isNotEmpty()) {
            sb.appendLine("Notable log notes in window:")
            notes.forEach { log ->
                sb.appendLine("- ${log.logDateKey} (${log.statinName} ${log.doseMg}mg): \"${log.notes}\"")
            }
        }
        sb.appendLine()

        sb.appendLine("Lipid panels (chronological):")
        val panelsAsc = reportData.lipidPanels.sortedWith(
            compareBy<com.example.data.model.LipidPanel> { it.drawDateKey }.thenBy { it.drawnAtMillis }
        )
        val baseline = reportData.baselinePanel ?: LipidAnalytics.resolveBaselinePanel(panelsAsc)
        if (panelsAsc.isEmpty()) {
            sb.appendLine("- None logged.")
        } else {
            panelsAsc.forEach { p ->
                val nonHdl = LipidAnalytics.effectiveNonHdl(p)
                val baselineTag = if (p.baselinePanel || p.id == baseline?.id) " [BASELINE]" else ""
                val opt = buildList {
                    if (p.apoBMgDl != null) add("ApoB=${p.apoBMgDl}")
                    if (p.lpAMgDl != null) add("Lp(a)=${p.lpAMgDl}")
                }.joinToString(", ")
                val optSuffix = if (opt.isNotBlank()) ", $opt" else ""
                sb.appendLine(
                    "- ${p.drawDateKey} (${if (p.fasting) "Fasting" else "Non-fasting"})$baselineTag: " +
                        "LDL-C=${p.ldlMgDl}, Total=${p.totalCholesterolMgDl}, HDL-C=${p.hdlMgDl}, TG=${p.triglyceridesMgDl}, Non-HDL=$nonHdl$optSuffix"
                )
            }
            val latest = panelsAsc.last()
            if (baseline != null && baseline.id != latest.id) {
                val ldlPct = LipidAnalytics.percentDelta(baseline.ldlMgDl, latest.ldlMgDl)
                val nonHdlPct = LipidAnalytics.percentDelta(
                    LipidAnalytics.effectiveNonHdl(baseline),
                    LipidAnalytics.effectiveNonHdl(latest)
                )
                sb.appendLine(
                    "Delta vs baseline (${baseline.drawDateKey} -> ${latest.drawDateKey}): " +
                        "LDL-C ${baseline.ldlMgDl}->${latest.ldlMgDl} (${ldlPct?.let { "${if (it > 0) "+" else ""}$it%" } ?: "n/a"}), " +
                        "Non-HDL ${LipidAnalytics.effectiveNonHdl(baseline)}->${LipidAnalytics.effectiveNonHdl(latest)} (${nonHdlPct?.let { "${if (it > 0) "+" else ""}$it%" } ?: "n/a"})."
                )
            }
        }
        return sb.toString()
    }

    private companion object {
        const val TAG = "CardiologyAiService"
    }
}
