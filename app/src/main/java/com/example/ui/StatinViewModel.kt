package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdherenceAnalytics
import com.example.data.model.ClinicalTargets
import com.example.data.model.DateTimeUtils
import com.example.data.model.DoseFrequency
import com.example.data.model.DoseLog
import com.example.data.model.FeelLevel
import com.example.data.model.LabRecheckCalculator
import com.example.data.model.LabRecheckStatus
import com.example.data.model.LabTimeRange
import com.example.data.model.LipidAnalytics
import com.example.data.model.LipidMetric
import com.example.data.model.LipidPanel
import com.example.data.model.ReminderSettings
import com.example.data.model.SideEffectOption
import com.example.data.model.StatinRegimen
import com.example.data.remote.AiInsightUiState
import com.example.data.remote.CardiologyAiService
import com.example.data.remote.ClinicianVisitBrief
import com.example.data.remote.PdfBriefUiState
import com.example.data.remote.StatinCloudRepository
import com.example.data.remote.TherapyAddMode
import com.example.notifications.ReminderCachePrefs
import com.example.notifications.ReminderScheduler
import com.example.reports.ClinicalPdfGenerator
import com.example.reports.ClinicalReportData
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AppTab {
    SUMMARY,
    LOGS,
    LABS,
    MEDICATIONS,
    REPORTS
}

enum class LogFilter {
    ALL,
    TAKEN,
    SKIPPED,
    WITH_SIDE_EFFECTS
}

enum class ReportRange(val label: String) {
    LAST_14_DAYS("Last 14 Days"),
    LAST_30_DAYS("Last 30 Days"),
    CURRENT_REGIMEN("Current Therapy"),
    ALL_HISTORY("All History")
}

data class DayStatus(
    val dateKey: String,
    val dayLetter: String,
    val log: DoseLog?,
    val scheduledDue: Boolean = true
)

data class StatinUiState(
    val isLoading: Boolean = false,
    val activeRegimen: StatinRegimen? = null,
    val activeRegimens: List<StatinRegimen> = emptyList(),
    val allRegimens: List<StatinRegimen> = emptyList(),
    val allLogs: List<DoseLog> = emptyList(),
    val allLipidPanels: List<LipidPanel> = emptyList(),
    val todayLog: DoseLog? = null,
    val todayLogsByRegimenId: Map<String, DoseLog> = emptyMap(),
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val clinicalTargets: ClinicalTargets = ClinicalTargets(),
    val labRecheckStatus: LabRecheckStatus? = null,
    val currentStreakDays: Int = 0,
    val adherence7DayPercent: Int = 0,
    val adherence30DayPercent: Int = 0,
    val last7Days: List<DayStatus> = emptyList(),
    val recentMoodCounts: List<Pair<FeelLevel, Int>> = emptyList(),
    val recentSideEffectCounts: List<Pair<SideEffectOption, Int>> = emptyList(),
    val logFilter: LogFilter = LogFilter.ALL,
    val logStatinFilter: String? = null,
    val reportRange: ReportRange = ReportRange.LAST_30_DAYS,
    val reportStatinFilter: String? = null,
    val selectedLabMetric: LipidMetric = LipidMetric.LDL_C,
    val selectedLabRange: LabTimeRange = LabTimeRange.SIX_MONTHS
) {
    val activePrimaryRegimen: StatinRegimen?
        get() = activeRegimens.firstOrNull { it.normalizedRole() == StatinRegimen.THERAPY_ROLE_PRIMARY }

    val activeAdjunctRegimens: List<StatinRegimen>
        get() = activeRegimens.filter { it.normalizedRole() == StatinRegimen.THERAPY_ROLE_ADJUNCT }

    val isPeriodicSchedule: Boolean
        get() = activeRegimens.isNotEmpty() &&
            activeRegimens.all { DoseFrequency.fromKey(it.frequencyKey).isPeriodic() }

    val filteredLogs: List<DoseLog>
        get() = allLogs.filter { log ->
            val matchesStatus = when (logFilter) {
                LogFilter.ALL -> true
                LogFilter.TAKEN -> log.taken
                LogFilter.SKIPPED -> !log.taken
                LogFilter.WITH_SIDE_EFFECTS -> SideEffectOption.parseCsv(log.sideEffectsCsv).isNotEmpty()
            }
            val matchesStatin = logStatinFilter == null || log.statinName == logStatinFilter
            matchesStatus && matchesStatin
        }

    val distinctLoggedStatins: List<String>
        get() = (allRegimens.map { it.statinName } + allLogs.map { it.statinName })
            .distinct()
            .sorted()
}

class StatinViewModel(
    private val repository: StatinCloudRepository,
    private val currentUserId: String,
    private val aiService: CardiologyAiService = CardiologyAiService()
) : ViewModel() {

    private val _logFilter = MutableStateFlow(LogFilter.ALL)
    private val _logStatinFilter = MutableStateFlow<String?>(null)
    private val _reportRange = MutableStateFlow(ReportRange.LAST_30_DAYS)
    private val _reportStatinFilter = MutableStateFlow<String?>(null)
    private val _selectedLabMetric = MutableStateFlow(LipidMetric.LDL_C)
    private val _selectedLabRange = MutableStateFlow(LabTimeRange.SIX_MONTHS)

    private val _aiInsightState = MutableStateFlow<AiInsightUiState>(AiInsightUiState.Idle)
    val aiInsightState: StateFlow<AiInsightUiState> = _aiInsightState.asStateFlow()

    private val _includeAiInPdf = MutableStateFlow(false)
    val includeAiInPdf: StateFlow<Boolean> = _includeAiInPdf.asStateFlow()

    private val _pdfBriefState = MutableStateFlow<PdfBriefUiState>(PdfBriefUiState.Idle)
    val pdfBriefState: StateFlow<PdfBriefUiState> = _pdfBriefState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    private val filterTuple = combine(
        _logFilter,
        _logStatinFilter,
        _reportRange,
        _reportStatinFilter,
        combine(_selectedLabMetric, _selectedLabRange) { metric, range -> metric to range }
    ) { logFilter, logStatin, reportRange, reportStatin, labPair ->
        FilterConfig(
            logFilter = logFilter,
            logStatin = logStatin,
            reportRange = reportRange,
            reportStatin = reportStatin,
            labMetric = labPair.first,
            labRange = labPair.second
        )
    }

    private val settingsTuple = combine(
        repository.observeReminderSettings(currentUserId),
        repository.observeClinicalTargets(currentUserId)
    ) { reminders, targets ->
        reminders to targets
    }

    val screenState: StateFlow<UiState<StatinUiState>> = combine(
        repository.observeRegimens(currentUserId),
        repository.observeLogs(currentUserId),
        repository.observeLipidPanels(currentUserId),
        settingsTuple,
        filterTuple
    ) { regimens, logs, panels, settingsPair, filters ->
        computeUiState(
            regimens = regimens,
            logs = logs,
            lipidPanels = panels,
            reminders = settingsPair.first,
            clinicalTargets = settingsPair.second,
            logFilter = filters.logFilter,
            logStatinFilter = filters.logStatin,
            reportRange = filters.reportRange,
            reportStatinFilter = filters.reportStatin,
            selectedLabMetric = filters.labMetric,
            selectedLabRange = filters.labRange
        )
    }
        .map<StatinUiState, UiState<StatinUiState>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing Firestore data", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Failed to sync your medication logs."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private fun currentData(): StatinUiState? = (screenState.value as? UiState.Success)?.data

    fun setLogFilter(filter: LogFilter) {
        _logFilter.value = filter
    }

    fun setLogStatinFilter(statinName: String?) {
        _logStatinFilter.value = statinName
    }

    fun setReportRange(range: ReportRange) {
        _reportRange.value = range
    }

    fun setReportStatinFilter(statinName: String?) {
        _reportStatinFilter.value = statinName
    }

    fun setLabMetric(metric: LipidMetric) {
        _selectedLabMetric.value = metric
    }

    fun setLabRange(range: LabTimeRange) {
        _selectedLabRange.value = range
    }

    fun generatePersonalInsight() {
        val snapshot = currentData() ?: return
        viewModelScope.launch {
            _aiInsightState.value = AiInsightUiState.Loading
            try {
                val insight = aiService.generateHolisticInsight(snapshot)
                _aiInsightState.value = AiInsightUiState.Success(insight)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Cardiology AI insight timed out", e)
                _aiInsightState.value = AiInsightUiState.Error(
                    "Analysis timed out after 60 seconds. Please check your connection and try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Cardiology AI insight failed", e)
                _aiInsightState.value = AiInsightUiState.Error(
                    e.localizedMessage ?: "Unable to generate personal cardiology insights right now."
                )
            }
        }
    }

    fun setIncludeAiInPdf(enabled: Boolean) {
        _includeAiInPdf.value = enabled
        if (!enabled) return
        val snapshot = currentData() ?: return
        val reportData = buildClinicalReportData(snapshot)
        val scopeKey = reportScopeKey(snapshot, reportData)
        val existing = (_pdfBriefState.value as? PdfBriefUiState.Success)?.brief
        if (existing == null || existing.scopeKey != scopeKey) {
            generatePdfVisitBrief()
        }
    }

    fun generatePdfVisitBrief() {
        val snapshot = currentData() ?: return
        val reportData = buildClinicalReportData(snapshot)
        val scopeKey = reportScopeKey(snapshot, reportData)
        viewModelScope.launch {
            _pdfBriefState.value = PdfBriefUiState.Loading
            try {
                val brief = aiService.generateClinicianVisitBrief(snapshot, reportData, scopeKey)
                _pdfBriefState.value = PdfBriefUiState.Success(brief)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Clinician PDF brief timed out", e)
                _pdfBriefState.value = PdfBriefUiState.Error(
                    "Brief generation timed out after 60 seconds. You can still export the data-only PDF or retry."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Clinician PDF brief failed", e)
                _pdfBriefState.value = PdfBriefUiState.Error(
                    e.localizedMessage ?: "Unable to generate AI visit brief right now."
                )
            }
        }
    }

    private fun activeMatchedVisitBrief(
        state: StatinUiState,
        reportData: ClinicalReportData
    ): ClinicianVisitBrief? {
        if (!_includeAiInPdf.value) return null
        val brief = (_pdfBriefState.value as? PdfBriefUiState.Success)?.brief ?: return null
        val expectedKey = reportScopeKey(state, reportData)
        return brief.takeIf { it.scopeKey == expectedKey }
    }

    fun saveRegimen(
        statinName: String,
        brandName: String,
        doseMg: Int,
        frequency: DoseFrequency,
        scheduledTimeMinutes: Int,
        therapyMode: TherapyAddMode = TherapyAddMode.SWITCH_PRIMARY,
        switchReason: String,
        notes: String
    ) {
        viewModelScope.launch {
            val hadPrimary = currentData()?.activePrimaryRegimen != null
            val result = repository.startOrSwitchRegimen(
                expectedUserId = currentUserId,
                statinName = statinName,
                brandName = brandName,
                doseMg = doseMg,
                frequency = frequency,
                scheduledTimeMinutes = scheduledTimeMinutes,
                therapyMode = therapyMode,
                switchReason = switchReason,
                notes = notes
            )
            result.onSuccess {
                val msg = when {
                    therapyMode == TherapyAddMode.ADD_ADJUNCT ->
                        "Added $statinName $doseMg mg adjunct therapy"
                    hadPrimary ->
                        "Switched primary therapy to $statinName $doseMg mg"
                    else ->
                        "Started $statinName $doseMg mg regimen"
                }
                _messages.tryEmit(msg)
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Could not save medication regimen")
            }
        }
    }

    fun deactivateRegimen(regimenId: String) {
        viewModelScope.launch {
            val result = repository.deactivateRegimen(currentUserId, regimenId)
            result.onSuccess {
                _messages.tryEmit("Therapy moved to history")
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Could not stop therapy")
            }
        }
    }

    fun saveDoseLog(
        existingLogId: String = "",
        regimen: StatinRegimen,
        taken: Boolean,
        logDateKey: String,
        takenAtMinutes: Int,
        feelLevel: FeelLevel,
        sideEffects: Set<SideEffectOption>,
        notes: String
    ) {
        viewModelScope.launch {
            val result = repository.saveDoseLog(
                expectedUserId = currentUserId,
                existingLogId = existingLogId,
                regimen = regimen,
                taken = taken,
                logDateKey = logDateKey,
                takenAtMinutes = takenAtMinutes,
                feelLevel = feelLevel,
                sideEffects = sideEffects,
                notes = notes
            )
            result.onSuccess {
                _messages.tryEmit(
                    if (existingLogId.isNotBlank()) "Check-in updated"
                    else if (taken) "${regimen.statinName} logged for ${DateTimeUtils.formatDateKeyShort(logDateKey)}"
                    else "Skipped dose recorded"
                )
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Could not save daily log")
            }
        }
    }

    fun deleteDoseLog(logId: String) {
        viewModelScope.launch {
            val result = repository.deleteDoseLog(currentUserId, logId)
            result.onSuccess {
                _messages.tryEmit("Log entry deleted")
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Could not delete log entry")
            }
        }
    }

    fun saveLipidPanel(
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
    ) {
        viewModelScope.launch {
            val result = repository.saveLipidPanel(
                expectedUserId = currentUserId,
                existingPanelId = existingPanelId,
                drawDateKey = drawDateKey,
                drawnAtMillis = drawnAtMillis,
                totalCholesterolMgDl = totalCholesterolMgDl,
                ldlMgDl = ldlMgDl,
                hdlMgDl = hdlMgDl,
                triglyceridesMgDl = triglyceridesMgDl,
                nonHdlMgDl = nonHdlMgDl,
                apoBMgDl = apoBMgDl,
                lpAMgDl = lpAMgDl,
                fasting = fasting,
                baselinePanel = baselinePanel,
                labName = labName,
                notes = notes
            )
            result.onSuccess {
                _messages.tryEmit(
                    if (existingPanelId.isNotBlank()) "Lipid panel updated"
                    else "Lipid panel logged for ${DateTimeUtils.formatDateKeyShort(drawDateKey)}"
                )
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Could not save lipid panel")
            }
        }
    }

    fun deleteLipidPanel(panelId: String) {
        viewModelScope.launch {
            val result = repository.deleteLipidPanel(currentUserId, panelId)
            result.onSuccess {
                _messages.tryEmit("Lipid panel removed")
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Could not delete lipid panel")
            }
        }
    }

    fun saveClinicalTargets(
        ldlTargetMgDl: Int,
        nonHdlTargetMgDl: Int?,
        apoBTargetMgDl: Int?,
        riskLabel: String,
        clinicianNote: String
    ) {
        viewModelScope.launch {
            val result = repository.saveClinicalTargets(
                expectedUserId = currentUserId,
                ldlTargetMgDl = ldlTargetMgDl,
                nonHdlTargetMgDl = nonHdlTargetMgDl,
                apoBTargetMgDl = apoBTargetMgDl,
                riskLabel = riskLabel,
                clinicianNote = clinicianNote
            )
            result.onSuccess { saved ->
                _messages.tryEmit("Lipid target updated: LDL-C <${saved.ldlTargetMgDl} mg/dL")
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Could not save lipid targets")
            }
        }
    }

    fun saveReminderSettings(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            val appCtx = context.applicationContext
            val active = currentData()?.activeRegimen
            val freq = DoseFrequency.fromKey(active?.frequencyKey)
            val result = repository.updateReminderSettings(currentUserId, enabled, hour, minute)
            result.onSuccess { updated ->
                ReminderCachePrefs.saveSettings(appCtx, updated)
                ReminderCachePrefs.saveActiveRegimen(appCtx, active)
                if (enabled) {
                    val triggerMillis = ReminderScheduler.scheduleReminder(
                        context = appCtx,
                        hour = updated.hour,
                        minute = updated.minute,
                        frequency = freq,
                        startedAtMillis = active?.startedAtMillis ?: -1L
                    )
                    val timeLabel = DateTimeUtils.formatMinutesOfDay(updated.hour * 60 + updated.minute)
                    val msg = if (freq.isPeriodic()) {
                        "${freq.reminderSummary} reminder scheduled (${DateTimeUtils.formatDateMillis(triggerMillis)} at $timeLabel)"
                    } else {
                        "Reminder set for $timeLabel"
                    }
                    _messages.tryEmit(msg)
                } else {
                    ReminderScheduler.cancelDailyReminder(appCtx)
                    _messages.tryEmit("Therapy reminder paused")
                }
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Could not save reminder settings")
            }
        }
    }

    fun sendTestNotification(context: Context) {
        val sent = ReminderScheduler.showReminderNotification(
            context.applicationContext,
            currentData()?.activeRegimen
        )
        _messages.tryEmit(
            if (sent) "Test reminder notification sent"
            else "Enable notification permission to preview reminders"
        )
    }

    fun syncReminderCache(context: Context, state: StatinUiState) {
        val appCtx = context.applicationContext
        val active = state.activeRegimen
        val freq = DoseFrequency.fromKey(active?.frequencyKey)
        ReminderCachePrefs.saveSettings(appCtx, state.reminderSettings)
        ReminderCachePrefs.saveActiveRegimen(appCtx, active)
        if (state.reminderSettings.enabled) {
            ReminderScheduler.scheduleReminder(
                context = appCtx,
                hour = state.reminderSettings.hour,
                minute = state.reminderSettings.minute,
                frequency = freq,
                startedAtMillis = active?.startedAtMillis ?: -1L
            )
        } else {
            ReminderScheduler.cancelDailyReminder(appCtx)
        }
    }

    fun exportSharePdf(context: Context, state: StatinUiState, onReady: (Uri) -> Unit) {
        val baseReport = buildClinicalReportData(state)
        val reportData = baseReport.copy(aiVisitBrief = activeMatchedVisitBrief(state, baseReport))
        viewModelScope.launch {
            try {
                val uri = withContext(Dispatchers.IO) {
                    ClinicalPdfGenerator.createShareablePdfUri(context.applicationContext, reportData)
                }
                onReady(uri)
            } catch (e: Exception) {
                _messages.tryEmit("Could not generate PDF: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun savePdfToUri(context: Context, state: StatinUiState, destinationUri: Uri) {
        val baseReport = buildClinicalReportData(state)
        val reportData = baseReport.copy(aiVisitBrief = activeMatchedVisitBrief(state, baseReport))
        viewModelScope.launch {
            try {
                val ok = withContext(Dispatchers.IO) {
                    ClinicalPdfGenerator.writePdfToUri(context.applicationContext, destinationUri, reportData)
                }
                _messages.tryEmit(
                    if (ok) "Clinical PDF report saved to device"
                    else "Could not write PDF file"
                )
            } catch (e: Exception) {
                _messages.tryEmit("Failed to save PDF: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    private data class FilterConfig(
        val logFilter: LogFilter,
        val logStatin: String?,
        val reportRange: ReportRange,
        val reportStatin: String?,
        val labMetric: LipidMetric,
        val labRange: LabTimeRange
    )

    companion object {
        private const val TAG = "StatinVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
        private val EPOCH_2000_MILLIS by lazy { DateTimeUtils.utcMillisFromDateKey("2000-01-01") }

        fun computeUiState(
            regimens: List<StatinRegimen>,
            logs: List<DoseLog>,
            lipidPanels: List<LipidPanel> = emptyList(),
            reminders: ReminderSettings,
            clinicalTargets: ClinicalTargets = ClinicalTargets(),
            logFilter: LogFilter = LogFilter.ALL,
            logStatinFilter: String? = null,
            reportRange: ReportRange = ReportRange.LAST_30_DAYS,
            reportStatinFilter: String? = null,
            selectedLabMetric: LipidMetric = LipidMetric.LDL_C,
            selectedLabRange: LabTimeRange = LabTimeRange.SIX_MONTHS,
            todayDateKey: String = DateTimeUtils.todayDateKey(),
            last7Keys: List<String> = DateTimeUtils.lastNDaysDateKeys(7),
            last30Keys: List<String> = DateTimeUtils.lastNDaysDateKeys(30)
        ): StatinUiState {
            val activeRegimens = regimens
                .filter { it.active }
                .sortedWith(
                    compareBy<StatinRegimen> {
                        if (it.normalizedRole() == StatinRegimen.THERAPY_ROLE_PRIMARY) 0 else 1
                    }.thenByDescending { it.startedAtMillis }
                )
            val activeRegimen = activeRegimens.firstOrNull()

            val logsByDate = logs.groupBy { it.logDateKey }
            val todayLogsByRegimen = logsByDate[todayDateKey].orEmpty().associateBy { it.regimenId }
            val todayLog = activeRegimen?.let { todayLogsByRegimen[it.id] }
                ?: logsByDate[todayDateKey]?.firstOrNull()

            val last7Days = last7Keys.map { key ->
                val due = activeRegimens.isEmpty() || activeRegimens.any { AdherenceAnalytics.isDueOnDate(it, key) }
                DayStatus(
                    dateKey = key,
                    dayLetter = DateTimeUtils.dayShortLetter(key),
                    log = logsByDate[key]?.firstOrNull(),
                    scheduledDue = due
                )
            }

            val adh7Result = AdherenceAnalytics.computeWindowAdherence(
                regimens = regimens,
                logs = logs,
                startDateKey = last7Keys.firstOrNull() ?: todayDateKey,
                endDateKey = last7Keys.lastOrNull() ?: todayDateKey
            )
            val adh30Result = AdherenceAnalytics.computeWindowAdherence(
                regimens = regimens,
                logs = logs,
                startDateKey = last30Keys.firstOrNull() ?: todayDateKey,
                endDateKey = last30Keys.lastOrNull() ?: todayDateKey
            )

            val last30KeySet = last30Keys.toSet()
            val last30Logs = logs.filter { it.logDateKey in last30KeySet }

            var streak = 0
            val earliestActiveStartKey = activeRegimens
                .filter { it.startedAtMillis > EPOCH_2000_MILLIS }
                .minOfOrNull { DateTimeUtils.dateKeyForMillis(it.startedAtMillis) }
            val checkKeys = DateTimeUtils.lastNDaysDateKeys(365).reversed()
            for ((index, key) in checkKeys.withIndex()) {
                val dayLogs = logsByDate[key].orEmpty()
                val anyTaken = dayLogs.any { it.taken }
                if (anyTaken) {
                    streak++
                } else if (index == 0 && dayLogs.isEmpty()) {
                    continue
                } else if (
                    dayLogs.isEmpty() &&
                    earliestActiveStartKey != null &&
                    key >= earliestActiveStartKey &&
                    activeRegimens.none { AdherenceAnalytics.isDueOnDate(it, key) }
                ) {
                    continue
                } else {
                    break
                }
            }

            val moodCounts = FeelLevel.entries.mapNotNull { level ->
                val count = last30Logs.count { FeelLevel.fromKey(it.feelLevelKey) == level }
                if (count > 0) level to count else null
            }

            val effectMap = mutableMapOf<SideEffectOption, Int>()
            last30Logs.forEach { log ->
                SideEffectOption.parseCsv(log.sideEffectsCsv).forEach { effect ->
                    if (effect != SideEffectOption.NONE) {
                        effectMap[effect] = (effectMap[effect] ?: 0) + 1
                    }
                }
            }
            val sideEffectCounts = effectMap.entries
                .sortedByDescending { it.value }
                .map { it.key to it.value }

            val recheckStatus = LabRecheckCalculator.evaluate(
                activeRegimens = activeRegimens,
                panels = lipidPanels,
                todayDateKey = todayDateKey
            )

            return StatinUiState(
                isLoading = false,
                activeRegimen = activeRegimen,
                activeRegimens = activeRegimens,
                allRegimens = regimens,
                allLogs = logs,
                allLipidPanels = lipidPanels,
                todayLog = todayLog,
                todayLogsByRegimenId = todayLogsByRegimen,
                reminderSettings = reminders,
                clinicalTargets = clinicalTargets,
                labRecheckStatus = recheckStatus,
                currentStreakDays = streak,
                adherence7DayPercent = adh7Result.adherencePercent,
                adherence30DayPercent = adh30Result.adherencePercent,
                last7Days = last7Days,
                recentMoodCounts = moodCounts,
                recentSideEffectCounts = sideEffectCounts,
                logFilter = logFilter,
                logStatinFilter = logStatinFilter,
                reportRange = reportRange,
                reportStatinFilter = reportStatinFilter,
                selectedLabMetric = selectedLabMetric,
                selectedLabRange = selectedLabRange
            )
        }

        fun reportScopeKey(state: StatinUiState, reportData: ClinicalReportData): String {
            val latestPanel = reportData.lipidPanels.firstOrNull()
            return listOf(
                state.reportRange.name,
                state.reportStatinFilter ?: "ALL",
                reportData.logs.size.toString(),
                reportData.takenCount.toString(),
                reportData.regimens.size.toString(),
                state.activeRegimens.size.toString(),
                reportData.lipidPanels.size.toString(),
                latestPanel?.drawDateKey.orEmpty(),
                latestPanel?.ldlMgDl?.toString().orEmpty(),
                state.clinicalTargets.ldlTargetMgDl.toString(),
                state.clinicalTargets.nonHdlTargetMgDl?.toString().orEmpty(),
                state.clinicalTargets.apoBTargetMgDl?.toString().orEmpty()
            ).joinToString("|")
        }

        fun buildClinicalReportData(
            state: StatinUiState,
            aiVisitBrief: ClinicianVisitBrief? = null,
            todayDateKey: String = DateTimeUtils.todayDateKey()
        ): ClinicalReportData {
            val activeIds = state.activeRegimens.map { it.id }.filter { it.isNotBlank() }.toSet()
            val keys14 = DateTimeUtils.lastNDaysDateKeys(14)
            val keys30 = DateTimeUtils.lastNDaysDateKeys(30)

            val filteredLogs = when (state.reportRange) {
                ReportRange.LAST_14_DAYS -> {
                    val keySet = keys14.toSet()
                    state.allLogs.filter { it.logDateKey in keySet }
                }
                ReportRange.LAST_30_DAYS -> {
                    val keySet = keys30.toSet()
                    state.allLogs.filter { it.logDateKey in keySet }
                }
                ReportRange.CURRENT_REGIMEN -> {
                    if (activeIds.isNotEmpty()) state.allLogs.filter { it.regimenId in activeIds }
                    else state.allLogs
                }
                ReportRange.ALL_HISTORY -> state.allLogs
            }.filter { log ->
                state.reportStatinFilter == null || log.statinName == state.reportStatinFilter
            }

            val relevantRegimens = state.allRegimens.filter { reg ->
                (state.reportRange != ReportRange.CURRENT_REGIMEN || reg.active) &&
                    (state.reportStatinFilter == null || reg.statinName == state.reportStatinFilter)
            }

            val windowStartKey = when (state.reportRange) {
                ReportRange.LAST_14_DAYS -> keys14.firstOrNull() ?: todayDateKey
                ReportRange.LAST_30_DAYS -> keys30.firstOrNull() ?: todayDateKey
                ReportRange.CURRENT_REGIMEN,
                ReportRange.ALL_HISTORY -> {
                    val regStarts = relevantRegimens
                        .filter { it.startedAtMillis > EPOCH_2000_MILLIS }
                        .map { DateTimeUtils.dateKeyForMillis(it.startedAtMillis) }
                    val logStarts = filteredLogs.map { it.logDateKey }
                    (regStarts + logStarts).minOrNull() ?: todayDateKey
                }
            }
            val windowEndKey = (filteredLogs.map { it.logDateKey } + todayDateKey).maxOrNull() ?: todayDateKey

            val adherenceResult = AdherenceAnalytics.computeWindowAdherence(
                regimens = relevantRegimens,
                logs = filteredLogs,
                startDateKey = windowStartKey,
                endDateKey = windowEndKey,
                statinNameFilter = state.reportStatinFilter
            )

            val moodBreakdown = FeelLevel.entries.mapNotNull { feel ->
                val count = filteredLogs.count { FeelLevel.fromKey(it.feelLevelKey) == feel }
                if (count > 0) feel to count else null
            }

            val effectCounts = mutableMapOf<SideEffectOption, Int>()
            filteredLogs.forEach { log ->
                SideEffectOption.parseCsv(log.sideEffectsCsv).forEach { effect ->
                    if (effect != SideEffectOption.NONE) {
                        effectCounts[effect] = (effectCounts[effect] ?: 0) + 1
                    }
                }
            }
            val sortedEffects = effectCounts.entries
                .sortedByDescending { it.value }
                .map { it.key to it.value }

            val scopeTitle = buildString {
                append(state.reportRange.label)
                if (state.reportStatinFilter != null) {
                    append(" (${state.reportStatinFilter})")
                }
            }

            return ClinicalReportData(
                rangeTitle = scopeTitle,
                generatedAtLabel = DateTimeUtils.formatDateTimeMillis(System.currentTimeMillis()),
                regimens = relevantRegimens,
                logs = filteredLogs,
                lipidPanels = state.allLipidPanels,
                clinicalTargets = state.clinicalTargets,
                baselinePanel = LipidAnalytics.resolveBaselinePanel(state.allLipidPanels),
                takenCount = adherenceResult.takenCount,
                skippedCount = adherenceResult.skippedCount,
                adherencePercent = adherenceResult.adherencePercent,
                moodBreakdown = moodBreakdown,
                sideEffectBreakdown = sortedEffects,
                aiVisitBrief = aiVisitBrief
            )
        }

        fun factory(
            repository: StatinCloudRepository,
            currentUserId: String
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StatinViewModel(repository, currentUserId) as T
                }
            }
    }
}
